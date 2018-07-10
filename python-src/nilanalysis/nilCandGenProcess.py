import copy
from urllib import parse
import nilanalysis.utils as utils
import utils.constants as constants


# Eg: Africa this subsumes all of Africa/* timezones
# HighLevelTimezonesToKeep = ['Africa']
HighLevelTimezonesToKeep = []
# Eg: 'Africa/Oromo'
LowLevelTimezonesToKeep = []
#LowLevelTimezonesToKeep = []

Inlink_threshold = 1000


EIDSTOKEEP = set()
EIDSTONOTKEEP = set()

FEATURE_CODE_NAME_GOLD=['independent political entity', 'first-order administrative division', 'capital of a political entity', 'sea', 'continent', 'seat of a first-order administrative division', 'populated place', 'administrative division', 'second-order administrative division', 'region', 'desert', 'zone', 'seat of a second-order administrative division', 'third-order administrative division', 'building(s)', 'island', 'airport', 'lake', 'ocean', 'dependent political entity', 'pier']



def nilClustering_Token2ClusterIdx(docta, nilClusteringDict, nilClusterIdx):
    candGenView = copy.deepcopy(docta.get_view("PRUNED_CANDGEN"))

    for i, cons in enumerate(candGenView.cons_list):
        if cons['label'].startswith("NULL") or cons['label'].startswith("NIL"):
            mentionstr = cons['tokens']
            if mentionstr not in nilClusteringDict:
                nilClusteringDict[mentionstr] = nilClusterIdx
                nilClusterIdx += 1

    return nilClusteringDict, nilClusterIdx



def nilClusterSingleDoc(docta, nilClusteringDict):
    nilClusterCandGenView = copy.deepcopy(docta.get_view("PRUNED_CANDGEN"))

    # Find NIL mentions
    for i, cons in enumerate(nilClusterCandGenView.cons_list):
        if cons['label'].startswith("NULL") or cons['label'].startswith("NIL"):
            mentionstr = cons['tokens']
            clusterIdx = nilClusteringDict[mentionstr]
            cons['label'] = 'NIL' + str(clusterIdx)

    nilClusterCandGenView.view_name = 'NILCLUSTER_CANDGEN'
    docta.view_dictionary['NILCLUSTER_CANDGEN'] = nilClusterCandGenView

    docta_json = docta.as_json

    return docta_json


def nilSingletonClustering(docta, viewName, singletonClusterIdx):
    nilClusterCandGenView = copy.deepcopy(docta.get_view(viewName))

    for i, cons in enumerate(nilClusterCandGenView.cons_list):
        if cons['label'].startswith("NULL") or cons['label'].startswith("NIL"):
            cons['label'] = 'NIL' + str(singletonClusterIdx)
            singletonClusterIdx += 1

    docta.view_dictionary[viewName] = nilClusterCandGenView

    docta_json = docta.as_json

    return docta_json, singletonClusterIdx


def nilLinkCandGenSingleDoc(docta, geonames, title_normalizer, inlinks):
    prunedCandGenView = copy.deepcopy(docta.get_view("CANDGEN"))

    for cons in prunedCandGenView.cons_list:
        labelScoreMap = {}

        if 'labelScoreMap' in cons:
            labelScoreMap = cons['labelScoreMap']
        else:
            continue

        pruned_labelScoreMap = processCandidateLabelScoreMap(
            labelScoreMap, geonames, title_normalizer, inlinks)

        # IF CAND LIST IS NOT EMPTY
        if pruned_labelScoreMap:
            cons['labelScoreMap'] = pruned_labelScoreMap
            maxScore = -1
            maxLabel = ''
            for k, v in pruned_labelScoreMap.items():
                if v > maxScore:
                    maxScore = v
                    maxLabel = k
            cons['label'] = maxLabel
            cons['score'] = maxScore

        # IF CAND LIST IS EMPTY
        if not pruned_labelScoreMap:
            cons['label'] = "NIL"
            cons['score'] = 1.0
            cons.pop('labelScoreMap', None)

    prunedCandGenView.view_name = 'PRUNED_CANDGEN'
    docta.view_dictionary['PRUNED_CANDGEN'] = prunedCandGenView

    return docta


def scoreCandGenSingleDoc(docta, geonames, title_normalizer, inlinks,
                          useCandPruning):
    gold_entities = docta.get_view("WIKIFIER").cons_list

    # [cons['labelScoreMap'] if 'labelScoreMap' in cons else {} ]
    cands2scores = []

    for cons in docta.get_view("CANDGEN").cons_list:
        if 'labelScoreMap' in cons:
            cands2scores.append(cons['labelScoreMap'])
        else:
            cands2scores.append({})



    # cands2scores = docta.get_view("CANDGEN").labels_to_scores_array
    total = 0
    nil_correct = 0
    total_hits = 0
    prior_correct = 0
    for gold_eid, labelScoreMap in zip(gold_entities, cands2scores):
        candeid2score = {}
        candtitle2score = {}

        if useCandPruning:
            pruned_labelScoreMap = processCandidateLabelScoreMap(
                labelScoreMap, geonames, title_normalizer, inlinks)
        else:
            pruned_labelScoreMap = labelScoreMap

        for k in pruned_labelScoreMap:
            candeid2score[k.split('|')[0]] = pruned_labelScoreMap[k]
            candtitle2score[k.split('|')[1]] = pruned_labelScoreMap[k]

        total += 1
        gold_eid = gold_eid["label"]

        # pruned_candtitle2score = processCandTitleScoreMap(candtitle2score,
        #                                                   inlinks)

        if gold_eid.startswith("NIL"):
            if len(candtitle2score) == 0:
                nil_correct += 1
            continue
        else:
            gold_eid = gold_eid.split("|")[0]
            # print(gold_eid)
            wikititle = utils.get_wikititle(geonames, gold_eid)
            if wikititle is None:
                continue
            goldtitle = wikititle[0].rsplit('/', 1)[-1]
            goldtitle = parse.unquote(goldtitle)
            goldtitle = title_normalizer.normalize(goldtitle)

            # if gold_eid in candeid2score:
            #     total_hits += 1
            # if len(candeid2score.keys()) > 0:
            #     prior_argmax_eid = max(candeid2score.keys(), key=lambda key: candeid2score[key])
            #     # print("prior_argmax",prior_argmax)
            #     if prior_argmax_eid == gold_eid:
            #         prior_correct += 1

            if goldtitle in candtitle2score:
                total_hits += 1
            if len(candtitle2score.keys()) > 0:
                prior_argmax_title = max(candtitle2score.keys(),
                                         key=lambda
                                         key: candtitle2score[key])
                # print("prior_argmax",prior_argmax)
                if prior_argmax_title == goldtitle:
                    prior_correct += 1


    return (prior_correct, nil_correct, total_hits, total)



def inTimezonesToKeep(timezone):
    if timezone == constants.NULL_GEONAME_PROP:
        return True

    if LowLevelTimezonesToKeep:
        if timezone in LowLevelTimezonesToKeep:
            return True

    if HighLevelTimezonesToKeep:
        if timezone.split("/")[0] in HighLevelTimezonesToKeep:
            return True


def crossesInlinksThreshold(inlinks_count):
    if inlinks_count < 0 or inlinks_count > Inlink_threshold:
        return True
    else:
        return False


def keepEntity(eid, title, geonames, title_normalizer, inlinks):
    if eid in EIDSTOKEEP:
        return True
    if eid in EIDSTONOTKEEP:
        return False

    # feature_code_name = utils.get_geoname_property(geonames, eid, "feature_code_name")
    timezone = utils.get_geoname_property(geonames, eid, "timezone")
    # feature_class_name = utils.get_geoname_property(geonames, eid, "feature_class_name")

    if inTimezonesToKeep(timezone=timezone):
        EIDSTOKEEP.add(eid)
        return True
        # pruned_labelScoreMap[id_pipe_title] = score
    else:
        if title in inlinks.inlinks:
            inlinks_count = len(inlinks.inlinks[title])
        else:
            inlinks_count = -1
        # if feature_code_name not in FEATURE_CODE_NAME_GOLD:
        #     continue
        if (crossesInlinksThreshold(inlinks_count)):
            EIDSTOKEEP.add(eid)
            return True
            # pruned_labelScoreMap[id_pipe_title] = score
        else:
            EIDSTONOTKEEP.add(eid)
            return False


def processCandidateLabelScoreMap(labelScoreMap, geonames, title_normalizer,
                                  inlinks):
    pruned_labelScoreMap = {}

    for (id_pipe_title, score) in labelScoreMap.items():
        eid, title = id_pipe_title.split("|")
        if keepEntity(eid, title, geonames, title_normalizer, inlinks):
            pruned_labelScoreMap[id_pipe_title] = score

    return pruned_labelScoreMap
