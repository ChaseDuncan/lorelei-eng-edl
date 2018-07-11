package edu.illinois.cs.cogcomp.loreleiengedl.apps;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.XmlTextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TACReader;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TACWriter;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import org.cogcomp.md.MentionAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;

public class TAC2017Eval {

    private static final Logger logger = LoggerFactory.getLogger(TAC2017Eval.class);
    private static final String NAME = TAC2017Eval.class.getCanonicalName();

    //String year = "2016";
    static String year = "2017";
    // location of source data
    static String corpusRoot = "/home/cddunca2/thesis/data/"+year+"/";
    //String outputDir = "/shared/experiments/cddunca2/tac-gold-tas-2016/";
    static String outputDir = "/shared/experiments/cddunca2/tac"+year+"-ace-ner-annotated/";
    //static String inputDir = "/shared/experiments/cddunca2/tac"+year+"-el-annotated/";
    static String inputDir = "/shared/experiments/cddunca2/tac-nitish-pred-tas-mlner-2017/";

    static SerializationHelper sh = new SerializationHelper();
    /**
     * Serializes a TextAnnotation as JSON.
     * @param ta the TextAnnotation to be serialized
     */
    static private void serializeTaAsJson(TextAnnotation ta) throws IOException{
        String output = outputDir + ta.getId();
        BufferedWriter bw = new BufferedWriter(new FileWriter(output, false));
        bw.write(sh.serializeToJson(ta));
        bw.close();
    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // make directory to write output to if it doesn't exist
        boolean annotateNER = false;
        boolean writeSubmissionFile = true;

        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        TACReader tacReader = new TACReader(corpusRoot,
                new TokenizerTextAnnotationBuilder(new StatefulTokenizer()),
                Language.English, true);
        MentionAnnotator mentionAnnotator =
                //new MentionAnnotator("TAC_2016_EN/NAM", "TAC_HEAD_TYPE/TAC_NOM", "", "", "");
                //new MentionAnnotator("TAC_2016_EN/NAM", "", "", "", "");
                new MentionAnnotator("ACE_HEAD_TYPE/ACE_NAM_TYPE", "", "", "", "");
        // mention annotator requires a POS view which may not have been added at this point
        POSAnnotator posannotator = new POSAnnotator();
        posannotator.doInitialize();
        if(annotateNER) {
            while (tacReader.hasNext()) {
                XmlTextAnnotation outputXmlTa = tacReader.next();
                TextAnnotation ta = outputXmlTa.getTextAnnotation();
                posannotator.addView(ta);
                mentionAnnotator.addView(ta);
                serializeTaAsJson(ta);
            }
        }

        if(writeSubmissionFile) {
            TACWriter tacWriter = new TACWriter();
            BufferedWriter bw = new BufferedWriter(new FileWriter("tac-submission-files/submissionfile", false));
            HashMap<String,File> jsonTaMap = new HashMap<>();
            File dir = new File(inputDir);
            File[] directoryListing = dir.listFiles();
            for (File child : directoryListing) {
                jsonTaMap.put(child.getName(),child);
            }
            while (tacReader.hasNext()) {
                XmlTextAnnotation xmlTa = tacReader.next();
                String jsonTa = inputDir + "/"  + xmlTa.getTextAnnotation().getId();
                File jsonTaFile = new File(jsonTa);
                // not all of the files from the reader were able to be annotated
                // if they couldn't be then they do not exist in the directory
                if(!jsonTaFile.exists())
                    continue;
                try {
                    logger.info(jsonTa);
                    TextAnnotation ta = sh.deserializeTextAnnotationFromFile(jsonTa, true);
                    tacWriter.writeTACSubmissionFormatFile("NEUREL",
                            new XmlTextAnnotation(xmlTa.getXmlSt(),ta,xmlTa.getXmlMarkup()),
                            bw, "NITISH");
                } catch(ArrayIndexOutOfBoundsException e){
                    logger.info(jsonTa + " cannot be read.");
                }
            }
            bw.close();
        }
    }
}
