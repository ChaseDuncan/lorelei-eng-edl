package edu.illinois.cs.cogcomp.loreleiengedl.utils;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;

public class ExtractUniqueSurfaceForms {

    public static void main(String[] args) throws Exception {
        String ilcode = "9";
        String sourceDirectory = "/shared/preprocessed/cddunca2/lorelei/il"
                + ilcode + "/cp2-setE/";
        String outfile = "/shared/experiments/cddunca2/lorelei/unique-surface-forms/il" +
                ilcode + "-sf.tsv";

        // get list of serialized TAs
        File dir = new File(sourceDirectory);
        File[] directoryListing = dir.listFiles();

        HashSet<String> uniqueSurfaceForms = new HashSet<>();
        SerializationHelper sh = new SerializationHelper();
        int numProcessedFiles = 0;
        int numEmptyViews = 0;
        // aggregate all unique surface forms
        for(File serializedTA : directoryListing){
            String taPath = sourceDirectory + serializedTA.getName();
            TextAnnotation ta = sh.deserializeTextAnnotationFromFile(taPath, true);
            numProcessedFiles++;
            View nerView = ta.getView("NER_LORELEI");
            for(Constituent constituent : nerView.getConstituents()){
                uniqueSurfaceForms.add(constituent.getSurfaceForm());
            }
            if(nerView.getNumberOfConstituents() == 0)
                numEmptyViews++;
        }

        System.out.println("Number of unique surface forms: " + uniqueSurfaceForms.size());
        System.out.println("Number of files processed: " + numProcessedFiles);
        System.out.println("Number of empty views: " + numEmptyViews);

        // write the surface forms out to the file
        BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));

        for(String surface : uniqueSurfaceForms){
            bw.write(surface+"\n");
        }
    }
}
