package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.annotation.BasicTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.lorelei.xml.*;

import cz.jirutka.unidecode.Unidecode;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import edu.illinois.cs.cogcomp.nlp.tokenizer.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This reads LTF files into TextAnnotation. All functions are static.
 * Created by mayhew on 3/29/18.
 */
@SuppressWarnings("Duplicates")
public class Ltf2TextAnnotation {

    private static Logger logger = LoggerFactory.getLogger( Ltf2TextAnnotation.class );

    public static boolean verbose = false;

    /**
     * Given a directory path, this will open all .ltf.zip files and convert all contained .ltf.xml files into TextAnnotation. You can
     * use this to skip unzipping all of the .ltf.zip files.
     * @param ltfzipdir
     * @return list of TextAnnotations, one for each compressed LTF file
     * @throws IOException
     * @throws XMLException
     */
    public static List<TextAnnotation> readLTFZip(String ltfzipdir) throws IOException, XMLException {
        return readLTFZip(null, ltfzipdir);
    }

    /**
     * Given a directory path, this will open all .ltf.zip files and convert .ltf.xml files into TextAnnotation if they are present
     * in idlist. If idlist is null, it will read all ltf files. Use this if you want to read a select few LTF files from the zip files.
     * @param ltfzipdir
     * @return list of TextAnnotations, one for each compressed LTF file
     * @throws IOException
     * @throws XMLException
     */
    public static List<TextAnnotation> readLTFZip(List<String> idlist, String ltfzipdir) throws IOException, XMLException {
        FilenameFilter ltfzip_filter = (File dir, String name) -> {
            return name.endsWith(".ltf.zip");
        };

        List<TextAnnotation> tas = new ArrayList<>();

        String[] ltfzip_files = (new File(ltfzipdir)).list(ltfzip_filter);
        for(String ltfzip_fname : ltfzip_files) {

            ZipFile zip = new ZipFile(ltfzipdir + "/" + ltfzip_fname);

            for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (!entry.isDirectory()) {
                    // This assumes that the filename looks like ltf/DOCID.ltf.xml
                    String docid = entry.getName().replace(".ltf.xml","").split("/")[1];

                    if(idlist != null && !idlist.contains(docid)){
                        continue;
                    }

                    InputStream ins = zip.getInputStream(entry);

                    TextAnnotation ta = readLTFFileToTA(ins, getDtdpath());
                    tas.add(ta);
                }
            }
        }

        return tas;
    }



    /**
     * This creates a list of TextAnnotation files from LTF format files. (This calls readLTF(dtdpath, ltfdir, -1)
     * @param ltfdir path containing LTF files
     * @param limit number of files you want to read usually for debugging. -1 for no limit
     * @return a list of TextAnnotations, one per file.
     * @throws XMLException
     */
    public static List<TextAnnotation> readLTF(String ltfdir, int limit) throws XMLException, FileNotFoundException {
        return readLTF(null, ltfdir, limit);
    }

    /**
     * This creates a list of TextAnnotation files from LTF format files. (This calls readLTF(dtdpath, ltfdir, -1)
     * @param ltfdir path containing LTF files
     * @return a list of TextAnnotations, one per file.
     * @throws XMLException
     */
    public static List<TextAnnotation> readLTF(String ltfdir) throws XMLException, FileNotFoundException {
        return readLTF(null, ltfdir, -1);
    }


    /**
     * Convert an individual LTF file into a TextAnnotation
     * @param ltffile an inputstream, so that it can start as a File or as a String (already read)
     * @param dtdpath
     * @return
     * @throws XMLException
     */
    private static TextAnnotation readLTFFileToTA(InputStream ltffile, String dtdpath) throws XMLException {

        List<String> tokensList = new ArrayList<>();
        List<IntPair> charOffsetsList = new ArrayList<>();
        List<Integer> sentEndIndicesList = new ArrayList<>();

        StringBuilder textbuilder = new StringBuilder();

        Document ltf = SimpleXMLParser.getDocument(ltffile, dtdpath);

        Node doc = ltf.getElementsByTagName("DOC").item(0);
        String docname = doc.getAttributes().getNamedItem("id").getNodeValue();

        int totaltoks = 0;

        // each segment is a sentence.
        NodeList segs = ltf.getElementsByTagName("SEG");
        for (int s = 0; s < segs.getLength(); s++) {
            Node seg = segs.item(s);
            NamedNodeMap segattr = seg.getAttributes();

            // sometimes the segment starts at 1, but we will just hallucinate an extra character in those cases...
            int seg_start_char = Integer.parseInt(segattr.getNamedItem("start_char").getNodeValue());

            while(textbuilder.length() < seg_start_char){
                textbuilder.append(" ");
            }
            
            int offset = 0;
            
            NodeList segchildren = seg.getChildNodes();
            for (int n = 0; n < segchildren.getLength(); n++) {
                Node child = segchildren.item(n);

                if(child.getNodeName() == "ORIGINAL_TEXT"){
                    // get that text
                    String ot = child.getTextContent();
                    textbuilder.append(ot);
                }

                if (child.getNodeName() == "TOKEN") {
                    totaltoks++;

                    String word = child.getTextContent();
                    
                    NamedNodeMap childattr = child.getAttributes();
                    String id = childattr.getNamedItem("id").getNodeValue();

                    String[] idsplit = id.split("-");
                    int sentid = Integer.parseInt(idsplit[1]);
                    int tokenid = Integer.parseInt(idsplit[2]);

                    // these indices are inclusive, so we add 1 to the last index to make it match standard systems.
                    int start_char = Integer.parseInt(childattr.getNamedItem("start_char").getNodeValue()) + offset;
                    int end_char = Integer.parseInt(childattr.getNamedItem("end_char").getNodeValue())+1 + offset;

                    String textsofar = textbuilder.toString();
                    String textword = textsofar.substring(start_char, end_char);

                    int newoffset = 0;
                    while(!textword.equals(word)){
                        newoffset += 1;
                        textword = textsofar.substring(start_char, end_char+newoffset);
                    }
                    offset += newoffset;
                    end_char += newoffset;
                    
                    if(!textword.equals(word)){
                        logger.error("Tokenization is not happening correctly! " + docname);                        
                    }

                    tokensList.add(word);
                    charOffsetsList.add(new IntPair(start_char, end_char));
                }
            }
            sentEndIndicesList.add(totaltoks);
        }

        int[] sentenceEndIndexes = new int[sentEndIndicesList.size()];
        for(int i =0; i < sentEndIndicesList.size(); i++){
            sentenceEndIndexes[i] = sentEndIndicesList.get(i);
        }

        String[] tokens = new String[tokensList.size()];
        for (int i = 0; i < tokensList.size(); i++)
            tokens[i] = tokensList.get(i);

        IntPair[] charOffsets = new IntPair[charOffsetsList.size()];
        for (int i = 0; i < charOffsetsList.size(); i++)
            charOffsets[i] = charOffsetsList.get(i);

        Tokenizer.Tokenization tok = new Tokenizer.Tokenization(tokens, charOffsets, sentenceEndIndexes);
        String corpusId = "";
        String text = textbuilder.toString();
        BasicTextAnnotationBuilder btab = new BasicTextAnnotationBuilder();
        TextAnnotation ta = btab.createTextAnnotation(corpusId, docname, text, tok);


        boolean romanization = true;
        if(romanization) {
            Unidecode unidecode = Unidecode.toAscii();
            View roman = new View(ViewNames.TRANSLITERATION, "Ltf2TextAnnotation", ta, 1.0);

            for(int i = 0; i < ta.getTokens().length; i++){
                String romantok = unidecode.decode(ta.getToken(i));
                Constituent cons = new Constituent(romantok, ViewNames.TRANSLITERATION, ta, i, i+1);
                roman.addConstituent(cons);
            }
            ta.addView(ViewNames.TRANSLITERATION, roman);
        }

        return ta;
    }

    public static String getDtdpath(){
        //Get file from resources folder
        //ClassLoader classLoader = Ltf2TextAnnotation.class.getClassLoader();
        //File file = new File(classLoader.getResource("dtds/").getFile());
        //String dtdpath = file.getAbsolutePath() + "/";
        String dtdpath = "/shared/corpora/corporaWeb/lorelei/" +
                "LDC2016E30_LORELEI_Mandarin_Incident_Language_Pack_V2.0/set0/dtds/";
        return dtdpath;
    }


    /**
     * This creates a list of TextAnnotation files from LTF format files.
     * @param idlist list of file ids to open. if this is null, all will loaded. if this is non-null, limit is ignored.
     * @param ltfdir path containing LTF files
     * @param limit number of files you want to read usually for debugging. -1 for no limit
     * @return a list of TextAnnotations, one per file.
     * @throws XMLException
     */
    public static List<TextAnnotation> readLTF(List<String> idlist, String ltfdir, int limit) throws XMLException, FileNotFoundException {

        FilenameFilter ltf_filter = (File dir, String name) -> {
            return name.endsWith("");
        };

        String[] ltf_files = (new File(ltfdir)).list(ltf_filter);

        System.out.println("Number of ltf files: " + ltf_files.length);
        if(limit > -1) {
            System.out.println(String.format("Only reading %d files", limit));
        }

        List<TextAnnotation> tas = new ArrayList<>();

        int i = 0;
        for(String ltf_fname : ltf_files) {

            if(i%1000 == 0){
                System.out.println("Read: " + i + "/" + ltf_files.length +" files.");
            }
            i++;
            
            String docname = ltf_fname.replace(".ltf.xml","");

            if(idlist != null && !idlist.contains(docname)){
                continue;
            }

            if(verbose) {
                System.out.println(docname);
            }

            File ltf_file = new File(ltfdir + "/" + ltf_fname);
            if(!ltf_file.exists()){
                System.err.println("Missing ltf file? " + ltf_file.getName());
                continue;
            }

            TextAnnotation ta = readLTFFileToTA(new FileInputStream(ltf_file), getDtdpath());
            tas.add(ta);

            if(tas.size() == limit){
                break;
            }

        }

        System.out.println(String.format("Read %d ltf files into ta", tas.size()));
        return tas;
    }

    /**
     * This function takes a list of TextAnnotations (probably read from readLTF function), and also reads a tab separated
     * submission format file (from LORELEI evaluation) and adds the labels from the tab file. The tab file
     * need not be linked to the Knowledge base.
     *
     * @param tas list of TextAnnotations
     * @param tabfilepath path to tab file (something like il6_edl.tab)
     * @throws FileNotFoundException
     */
    public static void addAnnoFromTab(List<TextAnnotation> tas, String tabfilepath) throws FileNotFoundException {

        HashMap<String, TextAnnotation> id2ta = new HashMap<>();
        for(TextAnnotation ta : tas){
            id2ta.put(ta.getId(), ta);
        }

        HashSet<String> annotatedtas = new HashSet<>();
        List<String> lines = LineIO.read(tabfilepath);

        for(String line : lines){
            String[] sline = line.split("\t");
            if(sline[0].equals("system_run_id")){
                // this is a header line.
                continue;
            }

            String mention = sline[2];
            String id = sline[3];

            String[] idsplit = id.split(":");
            String docid = idsplit[0];
            String[] offsets = idsplit[1].split("-");
            int start_char = Integer.parseInt(offsets[0]);
            int end_char = Integer.parseInt(offsets[1]);
            String label = sline[5];

            if(!id2ta.containsKey(docid)){
                continue;
            }

            annotatedtas.add(docid);

            TextAnnotation ta = id2ta.get(docid);

            View ner;
            if(ta.hasView(ViewNames.NER_CONLL)){
                ner = ta.getView(ViewNames.NER_CONLL);
            }else{
                ner = new View(ViewNames.NER_CONLL, "Ltf2TextAnnotation",ta,1.0);
                ta.addView(ViewNames.NER_CONLL, ner);
            }

            // this function uses inclusive indices (last index is index of last char in word)
            int start_tok = ta.getTokenIdFromCharacterOffset(start_char);
            int end_tok = ta.getTokenIdFromCharacterOffset(end_char);

            // since end_char is an inclusive index, we add 1 to get the substring.
            String mytext = ta.getText().substring(start_char, end_char+1);
            if(!mytext.equals(mention)) {
                System.out.println("mismatch! perhaps an error in the tab file?");
                System.out.println(mention + " " + mytext);
            }

            // need to add one to end_tok because of end offsets.
            Constituent cons = new Constituent(label, ViewNames.NER_CONLL, ta, start_tok, end_tok+1);
            ner.addConstituent(cons);
        }

        System.out.println(annotatedtas.size() + " tas annotated");
    }



    public static void main(String[] args) throws Exception {

        String ltfdir="/shared/corpora/corporaWeb/lorelei/data/LDC2016E90_LORELEI_Somali_Representative_Language_Pack_Monolingual_Text_V1.1/data/monolingual_text/ltf/";
        String tweetsdir = "/shared/corpora/corporaWeb/lorelei/data/LDC2016E90_LORELEI_Somali_Representative_Language_Pack_Monolingual_Text_V1.1/data/monolingual_text/tweets/";
        String lafdir = "/shared/corpora/corporaWeb/lorelei/data/LDC2016E91_LORELEI_Somali_Representative_Language_Pack_Translation_Annotation_Grammar_Lexicon_and_Tools_V1.0/data/annotation/entity/simple/";


        Ltf2TextAnnotation.readLTF("/home/mayhew/yor/");
    }
}

