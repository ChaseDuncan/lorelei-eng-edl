package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.lorelei.xml.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mayhew2 on 8/8/17.
 */
@SuppressWarnings("Duplicates")
@Deprecated
public class Ltf2Conll {


    private static Logger logger = LoggerFactory.getLogger( Ltf2Conll.class );

    private static List<String> excludetags = Arrays.asList("TTL", "NAN", "TIME");

    public static String makeline(String label, int start_offset, int end_offset, int ind, String word){
        return label+"\t"+ind+"\t"+start_offset+"\t"+end_offset+"\tO\t"+word+"\tx\tx\t0";
    }


    /**
     * This creates a CONLL formatted file from laf formatted data (excluding twitter data)
     * @param outdir directory to write output file to
     * @throws XMLException
     * @throws IOException
     */
    public static void readFiles(String dtdpath, String ltfdir, String outdir) throws XMLException, IOException {

        String[] ltf_files = (new File(ltfdir)).list();

        System.out.println("Number of ltf files: " + ltf_files.length);

        for(String ltf_fname : ltf_files) {
            String docname = ltf_fname.split("\\.")[0];

            File ltf_file = new File(ltfdir + "/" + ltf_fname);
            if(!ltf_file.exists()){
                logger.debug("skipping...");
                continue;
            }
            Document ltf = SimpleXMLParser.getDocument(ltf_file, dtdpath);

            List<String> conlllines = new ArrayList<>();

            // each segment is a sentence.
            NodeList segs = ltf.getElementsByTagName("SEG");
            for (int s = 0; s < segs.getLength(); s++) {
                Node seg = segs.item(s);
                NamedNodeMap segattr = seg.getAttributes();

                NodeList segchildren = seg.getChildNodes();
                for (int n = 0; n < segchildren.getLength(); n++) {
                    Node child = segchildren.item(n);

                    if (child.getNodeName() == "TOKEN") {

                        String word = child.getTextContent();
                        NamedNodeMap childattr = child.getAttributes();
                        String id = childattr.getNamedItem("id").getNodeValue();

                        String[] idsplit = id.split("-");
                        int tokenid = Integer.parseInt(idsplit[2]);

                        int start_char = Integer.parseInt(childattr.getNamedItem("start_char").getNodeValue());
                        int end_char = Integer.parseInt(childattr.getNamedItem("end_char").getNodeValue());

                        String outline = makeline("O", start_char, end_char, tokenid, word);
                        conlllines.add(outline);
                    }
                }

                // empty line between sentences.
                conlllines.add("");
            }

            LineIO.write(outdir + docname + ".conll", conlllines);
        }

    }

    public static void main(String[] args) throws XMLException, IOException {

        String dtdpath = "/shared/corpora/corporaWeb/lorelei/data/LDC2016E87_LORELEI_Amharic_Representative_Language_Pack_Translation_Annotation_Grammar_Lexicon_and_Tools_V2.0/dtds/";

        // Directory where conll output files will be written.
        String ltfdir = "/shared/corpora/corporaWeb/lorelei/evaluation-20170804/set0/data/monolingual_text/ltf/";
        String outdir = "/shared/corpora/corporaWeb/lorelei/evaluation-20170804/set0/data/monolingual_text/conll/";



        readFiles(dtdpath, ltfdir, outdir);
//        getTwitter(nerdir, dtdpath, twdir, outdir);

    }

}

