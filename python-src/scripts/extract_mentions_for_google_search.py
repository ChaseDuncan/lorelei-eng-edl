import sys
import os


#### INCOMPLETE NEVER USED! ####

# put parent directory in path so that imports work
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from utils.io_utils import get_ta_dir

"""
Extracts all unique surface forms of mentions found by NER into a file.
"""

from utils.io_utils import get_ta_dir

def extract_mentions(ta_dir, outpath):
    """Primary functionality. Presumes ta_dir is a directory of json
    annotated text annotations which includs an NER_LORELEI view.
    Iterates over this directory and the constituents in each TA's
    NER_LORELEI view. Creates a file of the unique surface forms found
    therein.

    @param: ta_dir: directory of json tas
    @param: out_path: path to write file to
    """
    tas = get_ta_dir(ta_dir)
    unique_surfaces = set()
    for ta in tas:
        ner_view = ta.get_view("NER_LORELEI")
        # nothing to do in cases where no entities are detected
        if new_view.cons_list == None:
            continue
        for constituent in ner_view.cons_list:
            hash_tokens.add(constituent.)

    with open(outpath, "w") as o: 
        for hash_token in hash_tokens:
            o.write("%s\n"%hash_token)

if __name__=='__main__':
    #ta_dir="/shared/corpora/corporaWeb/lorelei/evaluation-2018/il9/processed/setE/eng/"
    #outpath="/home/cddunca2/lorelei-eng-edl/data/il9_hashtags"
    ta_dir="/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/setE/eng/"
    outpath="/home/cddunca2/lorelei-eng-edl/data/il10_hashtags"

    extract_hashtags(ta_dir,outpath)
