# coding: utf-8
import logging
import argparse
from lorelei_kb.load_geonames_kb_v2 import GeoNamesLoader
# logging.basicConfig(format='%(asctime)s: %(filename)s:%(lineno)d: %(message)s', level=logging.INFO)
from utils.mongo_backed_dict import MongoBackedDict

logging.basicConfig(format=':%(levelname)s: %(message)s', level=logging.INFO)
import time


def add_to_dict(names, eid, tmpdict):
    for name in names:
        if name not in tmpdict:
            tmpdict[name] = set([])
        tmpdict[name].add(eid)


def ngrams(s, n):
    ans = []
    for i in range(len(s) - n + 1):
        ans.append(s[i:i + n])
    return ans


def getngrams(s, ngram):
    ans = []
    if ngram == 0:
        ans.append(s)
    if ngram == 1:
        ans += s.split(" ")
    if ngram == 4:
        ans += ngrams(s, 4)
    if ngram == 3:
        ans += ngrams(s, 3)
    if ngram == 2:
        ans += ngrams(s, 2)
    if ngram == 5:
        v = s.toLowerCase().replaceAll("[aeiou]", "")
        ans.append(v)
        ans += ngrams(v, 4)
    return ans


class AbstractIndex:
    def __init__(self, index_name, kbfile, overwrite=False, ngramorders=[]):
        self.name2ent = MongoBackedDict(dbname=index_name + ".phrase")
        self.word2ent = MongoBackedDict(dbname=index_name + ".word")
        self.ngram2ent = {}
        self.kbfile = kbfile
        self.ngramorders = ngramorders

        for i in self.ngramorders:
            self.ngram2ent[i] = MongoBackedDict(dbname=index_name + ".ngram-{}".format(i))
        index_type = None
        indices = []
        all_empty = all([i.size() == 0 for i in indices])
        if overwrite or all_empty:
            self.name2ent.drop_collection()
            self.word2ent.drop_collection()
            for i in self.ngramorders:
                self.ngram2ent[i].drop_collection()
            index_type = "all"
        else:
            # TODO The logic here is messed up
            if self.name2ent.size() == 0:
                self.name2ent.drop_collection()
                index_type = "name2ent"

            if self.word2ent.size() == 0:
                self.word2ent.drop_collection()
                index_type = "word2ent"

            for i in self.ngramorders:
                if self.ngram2ent[i].size() == 0:
                    self.ngram2ent[i].drop_collection()
                    index_type = "ngram2ent"

        if index_type is not None:
            start = time.time()
            logging.info("loading from file %s", index_name)
            self.load_kb(index_type=index_type)
            logging.info("created in %d secs", time.time() - start)
        logging.info("%s loaded", index_name)

    def process_kb(self):
        raise NotImplementedError

    def load_kb(self, index_type):
        name_map = {}
        word_map = {}
        ngram_map = {}
        logging.info("index type:%s", index_type)
        for i in self.ngramorders:
            ngram_map[i] = {}
        try:
            for names, eid in self.process_kb():

                names = set(names)
                if index_type in ["all", "name2ent"]:
                    add_to_dict(names, eid, name_map)

                if index_type in ["all", "word2ent"]:
                    toks = set([tok for n in names for tok in n.split(" ")])
                    add_to_dict(toks, eid, word_map)

                if index_type in ["all", "ngram2ent"]:
                    for i in self.ngramorders:
                        ngramset = set([gram for n in names for gram in getngrams(n, ngram=i)])
                        add_to_dict(ngramset, eid, ngram_map[i])

            self.put_in_mongo(index_type, name_map, word_map, ngram_map)
        except KeyboardInterrupt:
            logging.info("ending prematurely.")
            self.put_in_mongo(index_type, name_map, word_map, ngram_map)

    def put_in_mongo(self, index_type, name_map, word_map, ngram_map):
        if index_type in ["all", "name2ent"]:
            self.name2ent.bulk_insert(name_map,
                                      insert_freq=len(name_map),
                                      value_func=lambda x: list(x))
        if index_type in ["all", "word2ent"]:
            self.word2ent.bulk_insert(word_map,
                                      insert_freq=len(word_map),
                                      value_func=lambda x: list(x))
        if index_type in ["all", "ngram2ent"]:
            for i in self.ngramorders:
                ngram_map[i] = self.prune_map(ngram_map[i])
                self.ngram2ent[i].bulk_insert(ngram_map[i],
                                              insert_freq=len(ngram_map[i]),
                                              value_func=lambda x: list(x))

    def prune_map(self, nmap):
        # dict changes during iteration, so take care
        for k in list(nmap.keys()):
            if len(nmap[k]) > 10000:
                logging.info("pruning entry for %s len=%d", k, len(nmap[k]))
                del nmap[k]
        return nmap


class GeoNameIndex(AbstractIndex):
    def __init__(self, index_name, kbfile, overwrite, ngramorders):
        super().__init__(index_name, kbfile, overwrite, ngramorders)

    def process_kb(self):
        logging.info("processing geonames ...")
        for idx, line in enumerate(open(self.kbfile)):
            if idx > 0 and idx % 1000000 == 0:
                logging.info("read %d lines", idx)
            parts = line.strip().split('\t')
            if len(parts) < 4:
                logging.info("bad line %s", line)
                logging.info(parts)
                continue
            eid, name = parts[2], parts[3]
            names = [name, name.lower()]
            if len(parts) > 4:
                asciiname = parts[4]

            else:
                # TODO make this ascii
                asciiname = name
            if len(asciiname.strip()) != 0:
                names.append(asciiname)
                names.append(asciiname.lower())
            yield names, eid


class AltNameIndex(AbstractIndex):
    def __init__(self, index_name, kbfile, overwrite, ngramorders):
        super().__init__(index_name, kbfile, overwrite, ngramorders)

    def process_kb(self):
        logging.info("processing alternate names ...")
        for idx, line in enumerate(open(self.kbfile)):
            if idx > 0 and idx % 1000000 == 0:
                logging.info("read %d lines", idx)
            parts = line.strip().split('\t')
            if len(parts) != 2:
                continue
            eid, name = parts
            names = [name, name.lower()]
            yield names, eid


# class AltNameIndex:
#     def __init__(self, index_name, kbfile=None, overwrite=False, ngramorders=[]):
#         self.name2ent = MongoBackedDict(dbname=index_name + ".phrase")
#         self.word2ent = MongoBackedDict(dbname=index_name + ".word")
#         self.ngram2ent = {}
#         self.ngramorders = ngramorders
#         for i in self.ngramorders:
#             self.ngram2ent[i] = MongoBackedDict(dbname=index_name + ".ngram-{}".format(i))
#         my_maps = [self.ngram2ent[i] for i in self.ngramorders] + [self.name2ent, self.word2ent]
#         if any([m.size() == 0 for m in my_maps]) or overwrite:
#             self.name2ent.drop_collection()
#             self.word2ent.drop_collection()
#             for i in self.ngramorders:
#                 self.ngram2ent[i].drop_collection()
#             start = time.time()
#             logging.info("loading from file %s", index_name)
#             self.load_kb(kbfile=kbfile)
#             logging.info("created in %d secs", time.time() - start)
#
#     def process_kb(self, kbfile):
#         logging.info("processing alternate names ...")
#         for idx, line in enumerate(open(kbfile)):
#             if idx > 0 and idx % 1000000 == 0:
#                 logging.info("read %d lines", idx)
#             parts = line.strip().split('\t')
#             if len(parts) != 2:
#                 continue
#             eid, name = parts
#             names = [name, name.lower()]
#             yield names, eid
#
#     def load_kb(self, kbfile="data/alternate_names.kb"):
#         name_map = {}
#         word_map = {}
#         ngram_map = {}
#         for i in self.ngramorders:
#             ngram_map[i] = {}
#         try:
#             for names, eid in self.process_kb(kbfile):
#                 names = set(names)
#                 add_to_dict(names, eid, name_map)
#
#                 toks = set([tok for n in names for tok in n.split(" ")])
#                 add_to_dict(toks, eid, word_map)
#                 ##
#                 for i in self.ngramorders:
#                     ngramset = set([gram for n in names for gram in getngrams(n, ngram=i)])
#                     # ngramset = set(getngrams(n, ngram=i) for n in names)
#                     add_to_dict(ngramset, eid, ngram_map[i])
#                     ##
#             self.put_in_mongo(name_map, word_map, ngram_map)
#         except KeyboardInterrupt:
#             logging.info("ending prematurely.")
#             self.put_in_mongo(name_map, word_map, ngram_map)
#
#     def put_in_mongo(self, name_map, word_map, ngram_map):
#         self.name2ent.bulk_insert(name_map, insert_freq=len(name_map), value_func=lambda x: list(x))
#         self.word2ent.bulk_insert(word_map, insert_freq=len(word_map), value_func=lambda x: list(x))
#         for i in self.ngramorders:
#             ngram_map[i] = self.prune_ngrams(ngram_map[i])
#             self.ngram2ent[i].bulk_insert(ngram_map[i], insert_freq=len(ngram_map[i]), value_func=lambda x: list(x))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Short sample app')
    parser.add_argument('--geoind', default="geonames_index", action="store")
    parser.add_argument('--altind', default="altnames_index", action="store")
    parser.add_argument('--orders', default="2,3,4", action="store")
    parser.add_argument('--write', action="store_true")
    args = parser.parse_args()
    args = vars(args)
    args["orders"] = list(map(int, args["orders"].split(",")))
    geoname_index = GeoNameIndex(index_name=args["geoind"],
                                 kbfile="data/geonames.kb",
                                 overwrite=args["write"],
                                 ngramorders=args["orders"])
    altname_index = AltNameIndex(index_name=args["altind"],
                                 kbfile="data/alternate_names.kb",
                                 overwrite=args["write"],
                                 ngramorders=args["orders"])
    geonames = GeoNamesLoader("data/geonames.kb")
    try:
        while True:
            surface = input("enter surface:")
            if surface in geoname_index.name2ent:
                eids = geoname_index.name2ent[surface]
                print("geocands found:", len(eids))
                for idx, cand in enumerate(eids):
                    logging.info("eid %s %s", cand, geonames[cand]['name'])
            if surface in altname_index.name2ent:
                eids = altname_index.name2ent[surface]
                print("altcands found:", len(eids))
                for idx, cand in enumerate(eids):
                    logging.info("eid %s %s", cand, geonames[cand]['name'])

    except KeyboardInterrupt:
        print('interrupted!')
