import glob
import itertools
import re
# from write_data_to_annotate import camel
from ccg_nlpy import TextAnnotation

__author__ = 'Shyam'


def camel_case_split(identifier):
    matches = re.finditer('.+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)', identifier)
    return [m.group(0) for m in matches]


def camel(s):
    return s != s.lower() and s != s.upper()


def correct_surface(q):
    if q.startswith('#'):
        q = q[1:]
    if q == "SSudan":
        q = "South Sudan"
    if q.endswith("…"):
        q = q[:-1]
    if q == "ኣውሮፓ":
        q = "አውሮፓ"
    if camel(q[1:]):  # SaintDenis
        q = " ".join(camel_case_split(q)).lower()
    if q[-1] == ",":  # Eritrea,
        q = q[:-1]
    return q


def remove_suffix(s):
    # make a list of known suffixes and abstract this
    suffixes = ["tti", "tiin", "tii"]
    for suff in suffixes:
        if s.endswith(suff):
            s = s[:-len(suff)]
        return s


def remove_consecutive_duplicates(word):
    return ''.join(i for i, _ in itertools.groupby(word))


def load_ta_from_jsons(json_dir):
    doc2ta = {}
    tafiles = glob.glob(json_dir + "/*")
    for tafile in tafiles:
        ta = TextAnnotation(json_str=open(tafile).read())
        docid = tafile.split("/")[-1]
        doc2ta[docid] = ta
    return doc2ta