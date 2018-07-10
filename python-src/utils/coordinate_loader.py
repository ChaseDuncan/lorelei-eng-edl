from collections import namedtuple, defaultdict
import pickle
import sys
import logging
import os
from utils.mongo_backed_dict import MongoBackedDict
from utils.vocab_utils import get_idx
from pymongo.errors import DocumentTooLarge
import reverse_geocoder as rg
logging.basicConfig(format='%(asctime)s: %(filename)s:%(lineno)d: %(message)s', level=logging.INFO)


def load_coord(kbfile):
    # ="data/enwiki/wid_title_mid_types_counts.txt"
    pkl_path = kbfile + ".coord.pkl"
    if os.path.exists(pkl_path):
        logging.info("pkl found! loading map %s", pkl_path)
        id2cc = load(pkl_path)
    else:
        logging.info("pkl not found! making nekb maps...")
        id2cc = {}
        for line in open(kbfile):
            parts = line.strip().split("\t")
            # print(parts)
            title, ent_id, longitude, latitude, country_name = parts
            coordinates = (float(latitude), float(longitude))
            result = rg.search(coordinates)
            cc = result["cc"]
            id2cc[ent_id] = cc
        obj = id2cc
        save(pkl_path, obj)
    return id2cc
