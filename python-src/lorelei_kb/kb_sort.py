import argparse
import codecs
import time

import goslate
from googletrans import Translator

from lorelei_kb.load_geonames_kb_v2 import GeoNamesLoader

if __name__ == '__main__':
    # print(inpath+"\t"+outpath)
    parser = argparse.ArgumentParser(
        description='kb_pipline parser.\nThe output format might look like these:\nDefault: counts <tab> surface <tab> kbid\nTranslate Only: counts <tab> surface <tab> translation <tab> kbid\nGeoname Only: counts <tab> surface <tab> kbid <tab> kbname\nBoth Translate and Geoname: counts <tab> surface <tab> translation <tab> kbid <tab> kbname')
    parser.add_argument('Inpath', action="store", help='Input file path')
    parser.add_argument('Outpath', action="store", help='Output file path')
    parser.add_argument('--SrcLang', action="store", required=False, help='Source language')
    parser.add_argument('TarLang', action="store", help='Target language')
    parser.add_argument('--Options', default="", action="store", required=False,
                        help='Two options, t:translate using Google Translate; g:load geoname mapping.Default with option c')
    parser.add_argument('--Format', default="raw", action="store", required=False,
                        help='Format of your file. Default raw tab file. Support raw:raw tab,th:three basic column, ts: translated file, geo:geonamed file')
    parser.add_argument('--Thresh', default=0, action="store", required=False,
                        help='Threshold for translation. Only translate words whose frequency is greater than or equal to the threshold')
    parser.add_argument('--Trans', default="google", action="store", required=False,
                        help='Translation method. Default using google. Support google and goslate')
    args = vars(parser.parse_args())
    inpath = args['Inpath']
    outpath = args['Outpath']
    inlang = args['SrcLang']
    outlang = args['TarLang']
    options = args['Options']
    format = args['Format']
    threshold = int(args['Thresh'])
    trans_method = args['Trans']

    # print(outlang)
    # lines = []
    # with codecs.open(inpath,encoding = 'UTF-8') as f:
    #     lines = f.readlines()
    mydict = {}
    for line in codecs.open(inpath, encoding='UTF-8'):
        line = str(line)
        words = line.strip().split('\t')
        if 'raw' in format:
            surf = words[2]
            eid = words[4]
            if surf not in mydict:
                mydict[surf] = {"Count": 0, "ID": eid, "Translation": "NIL", "Mapping": "NIL"}
            count = mydict[surf]["Count"] + 1
            mydict[surf]["Count"] = count
        elif 'th' in format:
            count, surf, eid = words
            mydict[surf] = {"Count": count, "ID": eid, "Translation": "NIL", "Mapping": "NIL"}
        elif 'ts' in format:
            count, surf, trans, eid = words
            mydict[surf] = {"Count": count, "ID": eid, "Translation": trans, "Mapping": "NIL"}
        elif 'geo' in format:
            count, surf, trans, eid, geo = words
            mydict[surf] = {"Count": count, "ID": eid, "Translation": trans, "Mapping": geo}

    if 't' in options:
        if 'google' in trans_method:
            translator = Translator()
        else:
            translator = goslate.Goslate()
        print(len(mydict))
        for idx, entry in enumerate(mydict):
            row = mydict[entry]
            if row["Translation"] != "NIL" or int(row["Count"]) < threshold:
                continue
            try:
                if 'google' in trans_method:
                    translation = translator.translate(entry, dest=outlang).text
                else:
                    translation = translator.translate(entry, outlang)
                mydict[entry] = {"Count": row["Count"], "ID": row["ID"],
                                 "Translation": translation, "Mapping": row["Mapping"]}
                print("translating:" + str(idx))
                if idx > 0 and idx % 100 == 0:
                    print('wait')
                    time.sleep(10)
            except:
                print("NIL")
                # finally:
                #    print (str(dict[entry]["Count"])+"\t"+entry)

    if 'g' in options:
        geonames = GeoNamesLoader()
        for entry in mydict:
            try:
                name = geonames.get(eid=str(mydict[entry]["ID"]))['name']
                mydict[entry] = {"Count": mydict[entry]["Count"], "ID": mydict[entry]["ID"],
                                 "Translation": mydict[entry]["Translation"], "Mapping": name}

            except:
                mydict[entry] = {"Count": mydict[entry]["Count"], "ID": mydict[entry]["ID"],
                                 "Translation": mydict[entry]["Translation"], "Mapping": "NIL"}

        geonames.finish()
    mydict = sorted(mydict.items(), key=lambda d: int(d[1]["Count"]), reverse=True)
    with codecs.open(outpath, encoding='UTF-8', mode='w') as out:
        for outline in mydict:
            if 'g' in options:
                if 't' in options or 'ts' in format or 'geo' in format:
                    out.write(
                        str(outline[1]["Count"]) + "\t" + outline[0] + "\t" + outline[1]["Translation"] + "\t" + str(
                            outline[1]["ID"]) + "\t" + outline[1]['Mapping'] + "\n")
                else:
                    out.write(
                        str(outline[1]["Count"]) + "\t" + outline[0] + "\t" + outline[0]+ "\t" + str(outline[1]["ID"]) + "\t" + outline[1][
                            'Mapping'] + "\n")
            else:
                if 't' in options or 'ts' in format:
                    out.write(
                        str(outline[1]["Count"]) + "\t" + outline[0] + "\t" + outline[1]["Translation"] + "\t" + str(
                            outline[1]["ID"]) + "\n")
                else:
                    out.write(str(outline[1]["Count"]) + "\t" + outline[0] + "\t" +outline[0]+"\t"+ str(outline[1]["ID"]) + "\n")
