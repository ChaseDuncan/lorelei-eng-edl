import os
import sys
import json
import argparse
from os import listdir
from os.path import isfile, join
from ccg_nlpy.core.text_annotation import TextAnnotation
from wiki_kb.inlinks_v2 import Inlinks
from lorelei_kb.load_geonames_kb_v2 import GeoNamesLoader
from wiki_kb.title_normalizer_v2 import TitleNormalizer
import nilanalysis.nilCandGenProcess as nilCandGenProcess
import nilanalysis.utils as utils


def sortDictOnValues(d, reverse):
    sorted_tuples = sorted(d.items(), key=lambda x: x[1], reverse=reverse)
    return sorted_tuples


def readJsonTA(jsonfp):
    with open(jsonfp, 'r') as f:
        tajsonstr = f.read()
    ta = TextAnnotation(json_str=tajsonstr)
    return ta


def getAllFilesInDir(dirpath):
    onlyfiles = [f for f in listdir(dirpath) if isfile(join(dirpath, f))]
    filefps = [os.path.join(dirpath, fname) for fname in onlyfiles]
    return filefps


def writeProcessedCandGenTAs(intadir, outtadir, geonames, inlinks,
                             title_normalizer, mode, viewName=None):
    jsonfps = getAllFilesInDir(intadir)

    maxNER = 0
    maxNERFile = ''

    print("Total number of files: {}".format(len(jsonfps)))

    if mode == 'nillink' or mode == 'nilsinglecluster':
        singletonClusterIdx = 0

        print("TIMEZONES TO KEEP")
        print("LowLevelTimezones: {}".format(
            nilCandGenProcess.LowLevelTimezonesToKeep))
        print("HighLevelTimezones: {}".format(
            nilCandGenProcess.HighLevelTimezonesToKeep))

        for i, jsonfp in enumerate(jsonfps):

            ta = readJsonTA(jsonfp)

            numNer = len(ta.get_view("NER_CONLL").cons_list)
            if numNer > maxNER:
                maxNER = numNer
                maxNERFile = jsonfp

            dirpath, outfilename = os.path.split(jsonfp)
            outfilepath = os.path.join(outtadir, outfilename)

            if mode == 'nillink':
                modified_ta = nilCandGenProcess.nilLinkCandGenSingleDoc(
                    ta, geonames, title_normalizer, inlinks)

                modified_ta, singletonClusterIdx = \
                    nilCandGenProcess.nilSingletonClustering(
                        docta=modified_ta, viewName='PRUNED_CANDGEN',
                        singletonClusterIdx=singletonClusterIdx)

                json.dump(modified_ta, open(outfilepath, "w"), indent=True)

            elif mode == 'nilsinglecluster':
                modified_ta, singletonClusterIdx = \
                    nilCandGenProcess.nilSingletonClustering(
                        docta=ta, viewName=viewName,
                        singletonClusterIdx=singletonClusterIdx)

                json.dump(modified_ta, open(outfilepath, "w"), indent=True)

            if i % 50 == 0:
                print("Files done: {}".format(i))


    elif mode == 'nilcluster':
        print("Running Nil Clustering")
        # { key : tokens,  value: clusterIdx }
        nilClusteringDict = {}
        nilClusterIdx = 0

        for i, jsonfp in enumerate(jsonfps):
            ta = readJsonTA(jsonfp)

            nilClusteringDict, nilClusterIdx = \
                nilCandGenProcess.nilClustering_Token2ClusterIdx(
                    ta, nilClusteringDict, nilClusterIdx)
        print("Tokens2ClusterIdx made. Size: {}".format(len(nilClusteringDict)))


        for i, jsonfp in enumerate(jsonfps):
            ta = readJsonTA(jsonfp)

            dirpath, outfilename = os.path.split(jsonfp)
            outfilepath = os.path.join(outtadir, outfilename)

            modified_ta = nilCandGenProcess.nilClusterSingleDoc(
                ta, nilClusteringDict)

            json.dump(modified_ta, open(outfilepath, "w"), indent=True)

            if i % 50 == 0:
                print("Files done: {}".format(i))

    print("All TAs with Cand Gen Pruning written")
    print("Max NER: {}  File: {}".format(maxNER, maxNERFile))


def main(args):
    print("Loading Geonames")
    geonames = GeoNamesLoader(ilcode=args.ilcode)
    if args.ilcode == '10' and geonames.get(eid='1248991') is None:
        print("GEONAMES not loaded propery. Exiting")
        sys.exit()
    print("Geonames Loaded")

    print("Loading Inlinks")
    inlinks = Inlinks()

    print("Loading Title normalizer")
    title_normalizer = TitleNormalizer()

    if args.ilcode == '9':
        nilCandGenProcess.HighLevelTimezonesToKeep = ['Africa']
        nilCandGenProcess.LowLevelTimezonesToKeep = []

    if args.ilcode == '10':
        nilCandGenProcess.HighLevelTimezonesToKeep = []
        nilCandGenProcess.LowLevelTimezonesToKeep = ['Asia/Colombo']

    if args.mode == 'nillink' or args.mode == 'nilcluster' or \
            args.mode == 'nilsinglecluster':

        if args.mode == 'nilsinglecluster':
            if args.viewname is None:
                print("FOR NIL SINGLETON CLUSTERING, SPECIFY VIEWNAME")
                sys.exit()

        print("MODE: {}".format(args.mode))
        print("Running nil linking for: {}".format(args.intadir))
        print("Writing  output in: {}".format(args.outtadir))
        writeProcessedCandGenTAs(intadir=args.intadir, outtadir=args.outtadir,
                                 geonames=geonames, inlinks=inlinks,
                                 title_normalizer=title_normalizer,
                                 mode=args.mode, viewName=args.viewname)

    else:
        print("WRONG MODE: {}".format(args.mode))
        sys.exit()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Short sample app')
    parser.add_argument('--ilcode', default=None, required=True)
    parser.add_argument('--intadir', default=None, required=True)
    parser.add_argument('--outtadir', default=None, required=True)
    parser.add_argument('--mode', default=None, required=True)
    parser.add_argument('--viewname', default=None)

    args = parser.parse_args()

    main(args)
