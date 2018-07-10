from utils.io_utils import get_ta_dir

import copy
import json
"""
    This script turns all of the keys in the labels to scores map
    for the NEUREL view in "LORELEIKBID|WikiTitle".
"""
# tsv of lorelei kb ids to wiki titles
l2t = "../data/l2t.tab"


def wiki_to_lorelei_map():
    """Instatiates a map of LORELEI KB ids to Wikipedia titles.
    Relies on the l2t variable which points to a file with
    this data in it.
    """
    w2t_map = {}
    with open(l2t, "r") as m:
        for line in m.readlines():
            spline = line.strip().split("\t")
            # one of the lines in the file doesn't have a Wikipedia title
            # and I don't know why.
            if len(spline) < 2:
                continue
            lid, title =  spline
            w2t_map[title] = lid
    return w2t_map

def convert_entity_ids(ta_dir):
    """Adds a new View to each TextAnnotation in a directory which has
    a labels to scores map where the keys have the format 
    "LORELEI kb id | Wikipedia title". This map is created based on the output
    from an EL model. If the Wikipedia title that the EL model predicted is not
    in the KB the entry is removed from the map.

    @param ta_dir directory of text annotations
    """
    w2t_map = wiki_to_lorelei_map()

    tas = get_ta_dir(ta_dir)
    for ta in tas:
        candgen_view = copy.deepcopy(ta.get_view("NEUREL"))
        el_cons_list = candgen_view.cons_list

        for constituent in el_cons_list:
            convertedLabelToScoreMap = {}
            for k, v in constituent['labelScoreMap'].items():
                try:
                    kbid = w2t_map[k]
                    convKey = "%s|%s"%(kbid,k)
                    convertedLabelToScoreMap[convKey] = v
                except KeyError:
                    print(k + " not in KB.")
            # overwrite old label score map with new map which has new keys
            constituent['labelScoreMap'] = convertedLabelToScoreMap

        candgen_view.view_name = "CANDGEN"
        ta.view_dictionary["CANDGEN"] = candgen_view
        ta_json = ta.as_json
        json.dump(ta_json, open("test/" + ta.id, "w"), indent=True)

if __name__=='__main__':
    import sys
    ta_dir = sys.argv[1]
    convert_entity_ids(ta_dir)
