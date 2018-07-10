package edu.illinois.cs.cogcomp.loreleiengedl.apps;

import edu.illinois.cs.cogcomp.loreleiengedl.LORELEIEnglishEDL;
import edu.illinois.cs.cogcomp.lorelei.FormatConverter;

public class LORELEI2018Test {
    public static void main(String[] args) throws Exception {
        // specify which task to perform in the linking pipeline
        //String task = "nomlink";
        String task = "ner";
        //String task = "nil";
        String ilCode = "9";

        String taDir = "/shared/preprocessed/cddunca2/lorelei/il" + ilCode + "/setS/";

        LORELEIEnglishEDL edl = new LORELEIEnglishEDL();
        // add view to TAs for NAM/NOM mentions, serialize TAs for EL step
        if(task.equals("ner")) {
            // directory with LORELEI source data in it
            String ltfDir =
                    "/shared/corpora/corporaWeb/lorelei/evaluation-2018/il" + ilCode +
                        "/processed/setE/eng/";
            // directory to store TextAnnotations of source data
            edl.annotateNER(ltfDir, taDir);
        }

        if(task.equals("nil")){
            String entity2wiki = "data/il"  + ilCode + "_i2t.tab";
            edl.addCandGenViewToDir(taDir, entity2wiki);
        }

        // link nominals
        if(task.equals("nomlink")) {
            // directory of json TAs which are NER and EL annotated
            edl.annotateNominals(taDir);
        }
        task = "submission";
        // write submission file
        if(task.equals("submission")) {
            // directory of json TAs which are NER and EL annotated and NOMs are linked
            // path to write submission file
            String submissionFile = "submission-purgatory/il" + ilCode + "_test.tab";
            String entity2wiki = "data/il"  + ilCode + "_i2t.tab";
            FormatConverter.EnglishEDL2Tab(taDir, submissionFile, entity2wiki, false);
        }
    }
}
