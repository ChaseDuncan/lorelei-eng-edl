import sys
import os

# put parent directory in path so that imports work
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from utils.io_utils import get_ta_dir

"""
Extracts all unique tokens from the Twitter data which begin
with a hashtag. Creates a file with one of these tokens per line.
"""

from utils.io_utils import get_ta_dir

def extract_hashtags(ta_dir, outpath):
    """Primary functionality. Presumes ta_dir is a directory of json
    annotated text annotations. Iterates over this directory and the
    tokens in each TA. Creates a file of these.

    @param: ta_dir: directory of json tas
    @param: out_path: path to write file to
    """
    tas = get_ta_dir(ta_dir)
    hash_tokens = set()
    for ta in tas:
        token_view = ta.get_tokens
        for token in token_view:
            if token[0] == '#':
                hash_tokens.add(token)

    with open(outpath, "w") as o: 
        for hash_token in hash_tokens:
            o.write("%s\n"%hash_token)

if __name__=='__main__':
    #ta_dir="/shared/corpora/corporaWeb/lorelei/evaluation-2018/il9/processed/setE/eng/"
    #outpath="/home/cddunca2/lorelei-eng-edl/data/il9_hashtags"
    ta_dir="/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/setE/eng/"
    outpath="/home/cddunca2/lorelei-eng-edl/data/il10_hashtags"

    extract_hashtags(ta_dir,outpath)
