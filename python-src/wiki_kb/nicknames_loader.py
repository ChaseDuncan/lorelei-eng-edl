import logging

from utils.mongo_backed_dict import MongoBackedDict

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)


def load_nicknames(path="/shared/experiments/upadhya3/ppoudyaltest/wiki_list", overwrite=False):
    nicknames = MongoBackedDict(dbname="nicknames")
    if nicknames.size() == 0 or overwrite:
        nn_map = {}
        # populate nn_map
        for idx,line in enumerate(open(path)):
            parts = line.strip().split('\t')
            if idx > 0 and idx % 10000==0:
                logging.info("read %d lines", idx)
            # if len(parts)!=3:
            #     logging.info("bad line %s",line)
            #     continue
            title, tid = parts[:2]
            fr_strs = parts[2:]
            # print(title,tid,fr_strs)
            for fr_str in fr_strs:
                if fr_str not in nn_map:
                    nn_map[fr_str] = title
        nicknames.bulk_insert(regular_map=nn_map, insert_freq=len(nn_map))
    return nicknames


if __name__ == "__main__":
    load_nicknames()
