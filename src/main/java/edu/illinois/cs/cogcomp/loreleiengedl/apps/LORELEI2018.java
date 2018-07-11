package edu.illinois.cs.cogcomp.loreleiengedl.apps;

import edu.illinois.cs.cogcomp.lorelei.FormatConverter;
import edu.illinois.cs.cogcomp.loreleiengedl.LORELEIEnglishEDL;

public class LORELEI2018 {
    public static void main(String[] args) throws Exception {
        // specify which task to perform in the linking pipeline
        String task = args[0];

        LORELEIEnglishEDL edl = new LORELEIEnglishEDL();
        // add view to TAs for NAM/NOM mentions, serialize TAs for EL step
        if(task.equals("ner")) {
            // directory with LORELEI source data in it
            String ltfDir = args[1];
            // directory to store TextAnnotations of source data
            String taDir = args[2];
            edl.annotateNER(ltfDir, taDir);
        }

        // link nominals
        if(task.equals("nomlink")) {
            // directory of json TAs which are NER and EL annotated
            String taDir = args[1];
            edl.annotateNominals(taDir);
        }

        // google links
        if(task.equals("google")){
            String taDir = args[1];
            String ilCode = args[2];
            edl.addGoogleView(taDir,ilCode);
        }

        // write submission file
        if(task.equals("submission")) {
            // directory of json TAs which are NER and EL annotated and NOMs are linked
            String taDir = args[1];
            // path to write submission file
            String submissionFile = args[2];
            // TODO: clean all of this up!
            String ilCode = args[3];
            String entity2wiki = "data/il"  + ilCode + "_i2t.tab";
            // true for KB ids, false for Wiki titles
            FormatConverter.EnglishEDL2Tab(taDir, submissionFile, entity2wiki, true);
            //FormatConverter.EnglishEDL2Tab(taDir, submissionFile, entity2wiki, false);
        }
    }
}
