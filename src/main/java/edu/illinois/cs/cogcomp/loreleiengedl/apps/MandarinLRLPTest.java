package edu.illinois.cs.cogcomp.loreleiengedl.apps;

import edu.illinois.cs.cogcomp.lorelei.FormatConverter;
import edu.illinois.cs.cogcomp.loreleiengedl.LORELEIEnglishEDL;

import java.io.File;

public class MandarinLRLPTest {
    static String LTF_DIR = "/shared/corpora/corporaWeb/lorelei/" +
            "LDC2016E30_LORELEI_Mandarin_Incident_Language_Pack_V2.0/" +
            "setS/data/monolingual_text/ltf/";
    static String TA_DIR = "/shared/experiments/cddunca2/mandarin-text-annotation/";
    static String JSON_DIR = "/shared/experiments/cddunca2/mandarin-text-annotation-json/";

    public static void main(String[] args) throws Exception {
        //String task = args[0];
        //String task = "ner";
        //task = "nomlink";

        /*
        String task = "submission";
        LORELEIEnglishEDL edl = new LORELEIEnglishEDL();

        // if mandarin ta directory doesn't exist, create it and convert ltf to ta
        File directory = new File(TA_DIR);
        if (! directory.exists()){
            directory.mkdir();
            // had to hack Ltf2TextAnnotation.getDtdpath() to make this work
            FormatConverter.Ltf2Json(LTF_DIR, TA_DIR);
        }

        if(task.equals("ner"))
            edl.annotateNER(TA_DIR);
        if(task.equals("nomlink"))
            edl.annotateNominals(TA_DIR);
        if(task.equals("submission"))
            FormatConverter.EnglishEDL2Tab(TA_DIR, "submissionFileTest");
            */
   }
}
