from urllib import parse
import utils.constants as constants


# def get_geoname_property(geonames, eid, property_name):
#     if eid in geonames.kb:
#         if property_name in geonames[eid]:
#             return geonames[eid][property_name]
#         else:
#             return constants.NULL_GEONAME_PROP
#     else:
#         return constants.NULL_TITLE


def get_geoname_property(geonames, eid, property_name):
    ''' For PYMONGO. '''
    eid_doc = geonames.get(eid=eid)
    if eid_doc is None:
        return constants.NULL_TITLE
    else:
        if property_name in eid_doc:
            return eid_doc[property_name]
        else:
            return constants.NULL_GEONAME_PROP


# def get_wikititle(geonames, gold_eid):
#     if "external_link" in geonames[gold_eid]:
#         links = geonames[gold_eid]["external_link"].split("|")
#         wiki_link = [link for link in links if "en.wikipedia" in link]
#         if len(wiki_link) == 0:
#             return None
#         else:
#             return wiki_link
#     return None


def get_wikititle(geonames, gold_eid):
    ''' For PyMongo. '''
    # eid_doc = geonames.get(eid=gold_eid)
    if "external_link" in geonames[gold_eid]:
        links = geonames[gold_eid]["external_link"].split("|")
        wiki_link = [link for link in links if "en.wikipedia" in link]
        if len(wiki_link) == 0:
            return None
        else:
            return wiki_link
    return None


def get_normalized_wikititle(geonames, title_normalizer, gold_eid):
    wikititle = get_wikititle(geonames, gold_eid)
    if wikititle is None:
        return constants.NULL_TITLE
    goldtitle = wikititle[0].rsplit('/', 1)[-1]
    goldtitle = parse.unquote(goldtitle)
    goldtitle = title_normalizer.normalize(goldtitle)
    return goldtitle


def get_wikititle_kbentry(kbentry):
    ''' For PyMongo. '''
    if "external_link" in kbentry:
        links = kbentry["external_link"].split("|")
        wiki_link = [link for link in links if "en.wikipedia" in link]
        if len(wiki_link) == 0:
            return None
        else:
            return wiki_link
    return None


def get_normalized_wikititle_kbentry(title_normalizer, kbentry):
    wikititle = get_wikititle_kbentry(kbentry)
    if wikititle is None:
        return constants.NULL_TITLE
    goldtitle = wikititle[0].rsplit('/', 1)[-1]
    goldtitle = parse.unquote(goldtitle)
    goldtitle = title_normalizer.normalize(goldtitle)
    return goldtitle
