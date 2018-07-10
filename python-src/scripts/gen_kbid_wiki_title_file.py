from lorelei_kb.load_geonames_kb_v2 import GeoNamesLoader
import urllib

"""
    Creates a tab separated file LORELEI KB ids to Wikipedia titles.
"""
# external_link and entityid
def gen_kbid_wiki_title_file(outfile, ilcode):
    geonames = GeoNamesLoader(ilcode=ilcode)
    with open(outfile, "w") as o:
        for e in geonames.all_iterator():
            try:
                ext_link = e['external_link']
                eid = e['entityid']
                if "en.wikipedia.org" in ext_link:
                    ext_link_split = ext_link.split("|")
                    if len(ext_link_split) > 1:
                        ext_link_split = [link for link in ext_link_split if "en.wikipedia.org" in link]
                    title = ext_link_split[0].split("/")[-1]
                    # decode escaped characters
                    title = urllib.parse.unquote(title)

                    o.write("%s\t%s\n"%(eid,title))
            except KeyError:
                continue


    geonames.finish()

if __name__=='__main__':
    import sys
    outfile = sys.argv[1]
    ilcode = sys.argv[2]
    gen_kbid_wiki_title_file(outfile, ilcode)

