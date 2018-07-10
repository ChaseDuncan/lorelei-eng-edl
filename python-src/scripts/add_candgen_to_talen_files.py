import sys
import os

import copy
import json

# put parent directory in path so that imports work
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from utils.io_utils import get_ta_dir

"""
We need a blank View in the text annotations to add manually added
Wikipedia titles. This script adds it.
"""

def _add_candgen_view(ta):
    """ Adds empty candgen view to TA"""
    candgen_view = copy.deepcopy(ta.get_ner_conll)
    for constituent in candgen_view.cons_list:
        constituent['label'] = ""

    candgen_view.view_name = "CANDGEN"
    ta.view_dictionary["CANDGEN"] = candgen_view


def add_candgen_view(ta_dir):
    """Adds TA of empty constituents called CANDGEN for each
    NER annotated constituent in the TA dir.

    @param: ta_dir: directory of TAs
    """
    tas = get_ta_dir(ta_dir)
    for ta in tas:
        # if there is an NER view without constituents, skip it
        if ta.get_ner_conll.cons_list == None:
            continue
        print("Adding CANDGEN view to " + ta.id)
        _add_candgen_view(ta)
        ta_json = ta.as_json
        json.dump(ta_json, open(ta_dir + "/" + ta.id + ".json", "w"), indent=True)
        
if __name__=="__main__":
    ta_dir="/shared/preprocessed/cddunca2/lorelei/il9/talen-data/data/il9/setS-annotation-user"
    add_candgen_view(ta_dir)

