import os
import sys
from os import listdir
from os.path import isfile, join
from ccg_nlpy.core.text_annotation import TextAnnotation
from wiki_kb.inlinks_v2 import Inlinks
from lorelei_kb.load_geonames_kb_v2 import GeoNamesLoader
from wiki_kb.title_normalizer_v2 import TitleNormalizer
import nilanalysis.nilCandGenProcess as nilCandGenProcess
import nilanalysis.utils as utils

# LORELEI KB
# /shared/corpora/corporaWeb/lorelei/evaluation-20170804/LDC2017E19_LORELEI_EDL_Knowledge_Base_V1.0/kb/data

print("Loading Geonames")
geonames = GeoNamesLoader(kbfile='/shared/bronte/upadhya3/tac2018/geonames.kb')
if geonames.get(eid='71000119') is None:
    print("GEONAMES not loaded propery. Exiting")
    sys.exit()
print("Geonames Loaded")

print("Loading Inlinks")
# inlinks = Inlinks(pkl_path1="/shared/bronte/upadhya3/tac2018/inlinks.pkl")
inlinks = Inlinks()
print("Loading Title normalizer")
# id2t_path = '/shared/preprocessed/upadhya3/enwiki-datamachine/' \
#             'idmap/enwiki-20170520.id2t'
# rd_map_path = '/shared/preprocessed/upadhya3/enwiki-datamachine/' \
#               'idmap/enwiki-20170520.r2t'
title_normalizer = TitleNormalizer()

wikititle2eid = {utils.constants.NULL_TITLE: utils.constants.NULL_TITLE_ID}


def sortDictOnValues(d, reverse):
    sorted_tuples = sorted(d.items(), key=lambda x: x[1], reverse=reverse)
    return sorted_tuples


def readJsonTA(jsonfp):
    with open(jsonfp, 'r') as f:
        tajsonstr = f.read()
    ta = TextAnnotation(json_str=tajsonstr)
    return ta


def getNilMentionNER(ta, nil_indices):
    ner_view = ta.get_view('NER_CONLL')
    nilmentionners = []
    for i in nil_indices:
        cons = ner_view.cons_list[i]
        nilmentionners.append((i, cons['label'], cons['tokens']))

    return nilmentionners


def getNERView(ta):
    ner_view = ta.get_view('NER_CONLL')
    for i, cons in enumerate(ner_view.cons_list):
        print(cons)


def getNILMentionsAndGoldEIDSFromWikifierView(ta):
    global wikititle2eid
    wikifier_view = ta.get_view('WIKIFIER')
    nil_indices = []
    gold_eids = []
    for i, cons in enumerate(wikifier_view.cons_list):
        label = cons['label']
        if label[0:3] == 'NIL':
            nil_indices.append(i)
        else:
            gold_eids.append(label.split("|")[0])

    gold_titles = [utils.get_normalized_wikititle(
                   geonames=geonames,
                   title_normalizer=title_normalizer,
                   gold_eid=x) for x in gold_eids]

    for (eid, title) in zip(gold_eids, gold_titles):
        if title not in wikititle2eid:
            wikititle2eid[title] = eid


    return (nil_indices, gold_eids, gold_titles)


def getNILEntityCands(ta, nil_indices):
    candgen_view = ta.get_view('CANDGEN')
    nilmentionCands = []
    for i in nil_indices:
        if 'labelScoreMap' in candgen_view.cons_list[i]:
            labelScoreMap = candgen_view.cons_list[i]['labelScoreMap']
            # id_pipe_cand : 9165721|Oromia_Zone
            cands = [id_pipe_cand.split('|')[1]
                     for id_pipe_cand in labelScoreMap]
            for id_pipe_cand in labelScoreMap:
                if id_pipe_cand.split('|')[1] not in wikititle2eid:
                    wikititle2eid[id_pipe_cand.split('|')[1]] = \
                        id_pipe_cand.split('|')[0]

            nilmentionCands.append(cands)
        else:
            nilmentionCands.append([])


    return nilmentionCands


def getAllFilesInDir(dirpath):
    onlyfiles = [f for f in listdir(dirpath) if isfile(join(dirpath, f))]
    filefps = [os.path.join(dirpath, fname) for fname in onlyfiles]
    return filefps


def makeNilCandsCountDict(allNilMentionsCands):
    candCountDict = {}
    for cands in allNilMentionsCands:
        for cand in cands:
            if cand not in candCountDict:
                candCountDict[cand] = 0
            candCountDict[cand] += 1

    return candCountDict


def makeNilNERTokensCountDict(allNilMentionsNER):
    nerCountDict = {}
    nerlabelCountDict = {}
    for ner in allNilMentionsNER:
        tokens = ner[2]
        label = ner[1]
        if tokens not in nerCountDict:
            nerCountDict[tokens] = 0
        nerCountDict[tokens] += 1

        if label not in nerlabelCountDict:
            nerlabelCountDict[label] = 0
        nerlabelCountDict[label] += 1


    return (nerCountDict, nerlabelCountDict)


def getAllNilNERAndCands(tadirpath):
    allNilMentionsNER = []
    allNilMentionsCands = []
    allGoldEids = []
    allGoldTitles = []

    jsonfps = getAllFilesInDir(tadirpath)
    for jsonfp in jsonfps:
        ta = readJsonTA(jsonfp)

        # gold_eids: [gold_eid, ... ]
        (nil_indices,
         gold_eids, gold_titles) = getNILMentionsAndGoldEIDSFromWikifierView(ta)
        allGoldEids.extend(gold_eids)
        allGoldTitles.extend(gold_titles)

        # nilmentionsNER: [(i, label, tokens), ..]
        nilmentionsNER = getNilMentionNER(ta, nil_indices)
        allNilMentionsNER.extend(nilmentionsNER)

        # nilmentionCands: [ [candidites], ..] (aligned with nilmentionsNER)
        nilmentionCands = getNILEntityCands(ta, nil_indices)
        allNilMentionsCands.extend(nilmentionCands)

    print("Number of NIL mentions : {}".format(len(allNilMentionsNER)))
    print("Number of NIL mentions : {}".format(len(allNilMentionsCands)))

    return (allNilMentionsNER, allNilMentionsCands, allGoldEids, allGoldTitles)


def candGenScore(tadirpath, useCandPruning=False):
    (prior_correct, nil_correct, total_hits, total) = (0, 0, 0, 0)
    jsonfps = getAllFilesInDir(tadirpath)
    print("Total number of files: {}".format(len(jsonfps)))
    for i, jsonfp in enumerate(jsonfps):
        ta = readJsonTA(jsonfp)

        (pr_corr, nil_corr,
         tot_hits, tot) = nilCandGenProcess.scoreCandGenSingleDoc(
            ta, geonames, title_normalizer, inlinks, useCandPruning)

        [prior_correct,
         nil_correct,
         total_hits,
         total] = [x + y for x, y in zip([prior_correct, nil_correct,
                                          total_hits, total],
                                         [pr_corr, nil_corr, tot_hits, tot])]
        if i % 50 == 0:
            print("Files done: {}".format(i))


    el_correct = prior_correct + nil_correct
    print("total_hits {}/{}={}".format(total_hits, total, total_hits / total))
    print("prior correct {}/{}={}".format(prior_correct, total, prior_correct / total))
    print("nil correct {}/{}={}".format(nil_correct, total, nil_correct / total))
    print("edl correct {}/{}={}".format(el_correct, total, el_correct / total))



def titleGeonamePropAnalysis(sortedGoldTitleCounts, sortedNilCandCounts):
    feature_code_name_goldcount = dict()
    feature_code_name_nilcount = dict()

    print("Gold titles")
    for i in range(0, len(sortedGoldTitleCounts)):
        title = sortedGoldTitleCounts[i][0]
        if title == utils.constants.NULL_TITLE:
            continue
        if title in wikititle2eid:
            eid = wikititle2eid[title]

            feature_code_name = utils.get_geoname_property(geonames, eid, "feature_code_name")
            timezone = utils.get_geoname_property(geonames, eid, "timezone")
            feature_class_name = utils.get_geoname_property(geonames, eid, "feature_class_name")
            if title in inlinks.inlinks:
                inlinks_count = len(inlinks.inlinks[title])
            else:
                inlinks_count = -1

            feature_code_name_goldcount[feature_code_name] = \
                feature_code_name_goldcount.get(feature_code_name, 0) + 1

            print("Wikititle: {} || {} | {} | {} | {}".format(title,
                  feature_code_name, timezone, feature_class_name,
                  inlinks_count))

    print("\nNILL Candidates")
    for i in range(0, len(sortedNilCandCounts)):
        title = sortedNilCandCounts[i][0]
        if title == utils.constants.NULL_TITLE:
            continue
        if title in wikititle2eid:
            eid = wikititle2eid[title]

            feature_code_name = utils.get_geoname_property(geonames, eid, "feature_code_name")
            timezone = utils.get_geoname_property(geonames, eid, "timezone")
            feature_class_name = utils.get_geoname_property(geonames, eid, "feature_class_name")
            if title in inlinks.inlinks:
                inlinks_count = len(inlinks.inlinks[title])
            else:
                inlinks_count = -1

            feature_code_name_nilcount[feature_code_name] = \
                feature_code_name_nilcount.get(feature_code_name, 0) + 1

            print("Wikititle: {} || {} | {} | {} | {}".format(title,
                  feature_code_name, timezone, feature_class_name,
                  inlinks_count))

    print("Feature code name for gold entities")
    print(feature_code_name_goldcount)
    print(list(feature_code_name_goldcount.keys()))

    print("Feature code name for nil candidates")
    print(feature_code_name_nilcount)


def main():
    # tadirpath = "/shared/bronte/upadhya3/tac2018/il5_gold_mentions_trans10"
    tadirpath = "/shared/bronte/upadhya3/tac2018/il6_gold_mentions_namematch_cands"

    # allNilMentionsNER : [ (i, label, tokens) ]
    print("\nFinding nil mentions and candidates in Corpus")
    (allNilMentionsNER,
     allNilMentionsCands,
     allGoldEids, allGoldTitles) = getAllNilNERAndCands(tadirpath=tadirpath)

    print("\nMaking count dicts for Candidates")
    candCountDict = makeNilCandsCountDict(allNilMentionsCands)
    sortedCandCountTuples = sortDictOnValues(candCountDict, reverse=True)
    print(sortedCandCountTuples[0:50])

    (nerCountDict,
     nerlabelCountDict) = makeNilNERTokensCountDict(allNilMentionsNER)
    sortNerCounts = sortDictOnValues(nerCountDict, reverse=True)
    sortNerLabelsCounts = sortDictOnValues(nerlabelCountDict, reverse=True)

    print(sortNerCounts[0:50])
    print(sortNerLabelsCounts)

    goldTitleCountDict = dict()
    for i in allGoldTitles:
        goldTitleCountDict[i] = goldTitleCountDict.get(i, 0) + 1
    sortGoldTitleCounts = sortDictOnValues(goldTitleCountDict, reverse=True)
    print(sortGoldTitleCounts[0:50])

    # print("\nRunning property analysis")
    # titleGeonamePropAnalysis(sortGoldTitleCounts, sortedCandCountTuples)

    print("\nComputing EL score from candidate generation")
    candGenScore(tadirpath=tadirpath, useCandPruning=False)


if __name__ == '__main__':
    main()
