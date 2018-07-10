package edu.illinois.cs.cogcomp.loreleiengedl;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.loreleiengedl.utils.LinkUtils;
import edu.illinois.cs.cogcomp.ner.config.NerBaseConfigurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import edu.illinois.cs.cogcomp.ner.NERAnnotator;
import org.cogcomp.md.MentionAnnotator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LORELEIEnglishEDL {
    // for reading in serialized TAs and serializing TAs to json
    private static SerializationHelper sh;
    private static String NERVIEW = "NER_LORELEI";
    private static String ELVIEW = "NEUREL";
    private static String MDVIEW = "MENTION";
    private static String NLVIEW = "NOMLINK";
    private static String CANDGENVIEW = "CANDGEN";
    private static String GOOGLEVIEW= "GOOGLE";

    // map of Wikipedia titles to kb ids. instatiated when needed.
    HashMap<String, String> wiki2lorelei = null;

    public LORELEIEnglishEDL() {
        sh = new SerializationHelper();
    }

    /**
     * Annotate LRLP using NER. Serialize resulting text annotations to file.
     * Expects that input directory is a directory of json serialized text annotations
     * that were generated from LORELEI LTF files.
     *
     * Creates a directory of json serialized text annotations with an NER and MENTIONview added
     * to them.
     *
     * @param sourceDirectory input directory of json serialized text annotations
     * @param nerAnnotatedDirectory output directory of json serialized text annotations
     *
     */
    public void annotateNER(String sourceDirectory, String nerAnnotatedDirectory) throws Exception {
        String configFile = "config/sf.config";

        ResourceManager rm = null;
        try {
            rm = new ResourceManager(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        String viewName = rm.getString(NerBaseConfigurator.VIEW_NAME);

        NERAnnotator nerAnnotator = null;
        nerAnnotator = new NERAnnotator(new NerBaseConfigurator().getConfig(rm), viewName);

        // get list of serialized TAs
        File dir = new File(sourceDirectory);
        File[] directoryListing = dir.listFiles();

        // iterate through TAs, add NER annotation and create JSON serialization
        for(File serializedTA : directoryListing){
            String ltfPath = sourceDirectory + serializedTA.getName();

            TextAnnotation ta = sh.deserializeTextAnnotationFromFile(ltfPath, true);
            nerAnnotator.getView(ta);

            String taPath = nerAnnotatedDirectory + serializedTA.getName();
            // overwrites existing json serializations in JSON_DIR
            sh.serializeTextAnnotationToFile(ta, taPath, true, true);
        }
    }

    /**
     *  Annotates a directory of json serialized TextAnnotations with a 'Google' view. This
     *  is a view which maps the surface form of a mention to the first Wikipedia page returned
     *  by a Google search of the surface form.
     *
     * @param taDir the directory of TAs to annotate
     */
    public void addGoogleView(String taDir, String ilCode) throws Exception {

        File dir = new File(taDir);
        File[] directoryListing = dir.listFiles();
        // TODO: this doesn't belong here...
        HashMap<String, List<String>> tweetToWiki = null;
        if(ilCode.equals("9"))
            tweetToWiki = LinkUtils.initTweetToWikipediaIL9();
        if(ilCode.equals("10"))
            tweetToWiki = LinkUtils.initTweetToWikipediaIL9();

        for(File serializedTA : directoryListing){

            //TODO: currently only operating on Tweets
            if(!serializedTA.getName().contains("SN"))
                continue;
            String taPath = taDir + serializedTA.getName();

            TextAnnotation ta = sh.deserializeTextAnnotationFromFile(taPath, true);
            View googleView  = new View(GOOGLEVIEW, "lorelei-eng-edl", ta, 1.0);
            View tokens = ta.getView("TOKENS");
            for(Constituent token : tokens.getConstituents()){
                if(token.getSurfaceForm().charAt(0) != '#')
                    continue;
                String cleanHash = token.getSurfaceForm().substring(1,token.length());
                List<String> links = tweetToWiki.get(cleanHash);

                if(links == null)
                    continue;

                String link = links.get(0);
                Constituent googleCon =
                        new Constituent(link, 1.0, GOOGLEVIEW, ta,
                                token.getStartSpan(), token.getEndSpan());
                googleView.addConstituent(googleCon);
            }
            // overwrites existing json serializations in JSON_DIR
            sh.serializeTextAnnotationToFile(ta, taPath, true, true);
        }
    }

    /**
     * Annotate LRLP using Mention Detection. Serialize resulting text annotations to file.
     * Expects that input directory is a directory of json serialized text annotations
     * that were generated from LORELEI LTF files.
     *
     * Creates a directory of json serialized text annotations with an NER and MENTIONview added
     * to them.
     *
     * @param taDir input/output directory of json serialized text annotations
     *
     */
    public void addMentionView(String taDir) throws Exception {
        String nomModelPath = "/home/cddunca2/lorelei-eng-edl/md-models/TAC_HEAD_TYPE/TAC_NOM";

        MentionAnnotator mentionAnnotator =
                new MentionAnnotator("", nomModelPath,
                        "", "", "");

        // mention annotator requires a POS view which may not have been added at this point
        POSAnnotator posannotator = new POSAnnotator();
        posannotator.doInitialize();

        // get list of serialized TAs
        File dir = new File(taDir);
        File[] directoryListing = dir.listFiles();

        // iterate through TAs, add NER annotation and create JSON serialization
        for(File serializedTA : directoryListing){
            String ltfPath = taDir + serializedTA.getName();

            TextAnnotation ta = sh.deserializeTextAnnotationFromFile(ltfPath, true);
            posannotator.addView(ta);
            mentionAnnotator.addView(ta);

            String taPath = taDir + serializedTA.getName();
            // overwrites existing json serializations in JSON_DIR
            sh.serializeTextAnnotationToFile(ta, taPath, true, true);
        }
    }

    /**
     * This function adds a CANDGEN view to the each TA in a directory, the path of which is
     * passed in as a parameter. The CANDGEN view is identical to the NEUREL view except that
     * prepends the LORELEI kb id and a pipe delimiter to each label in the view.
     *
     * For example, "Real_Page" will become "123456|Real_Page". This operation is applied to
     * the labels in the labels to scores map as well as the top label in the constituent.
     *
     * If there isn't a KB id for a given Wikipedia title then it is not added to the View.
     *
     * This view is required in order for the nil analysis part of the system to work. The nil
     * analysis is done in Python.
     *
     * @param: jsonTADir directory of json serialized text annotations
     */
    public void addCandGenViewToDir(String jsonTADir,
                                    String entity2WikipediaTitle) throws Exception {
        // create hash map which maps Wikipedia titles to LORELEI KB ids
        wiki2lorelei = LinkUtils.initWiki2LORELEIMap(entity2WikipediaTitle);
        // get list of files in the directory
        File dir = new File(jsonTADir);
        File[] directoryListing = dir.listFiles();

        // iterate over TAs in directory adding a CANDGEN view to each
        for(File jsonSerializedTA : directoryListing) {
            String jsonTaPath = jsonTADir + jsonSerializedTA.getName();
            TextAnnotation ta = sh.deserializeTextAnnotationFromFile(jsonTaPath, true);

            if(!ta.hasView("NEUREL")) {
                // TODO: logging!
                System.out.println(ta.getId() + " no NEUREL view.");
                continue;
            }

            addCandGenView(ta);
            sh.serializeTextAnnotationToFile(ta, jsonTaPath, true, true);
        }
    }

    /**
     * Add CANDGEN view to a TextAnnotation
     *
     * @param ta textannotation to add view to
     */
    public void addCandGenView(TextAnnotation ta){
        View elView = ta.getView(ELVIEW);
        View candgenView  = new View(CANDGENVIEW, "lorelei-eng-edl", ta, 1.0);
        for(Constituent constituent : elView.getConstituents()){
            // Constituent(Map<String,Double> labelsToScores, String viewName,
            // TextAnnotation text, int start, int end)
            Map<String, Double> extendedLabelsToScores =
                    extendLabelsToScores(constituent.getLabelsToScores());
            Constituent candgenConstituent;
            if(extendedLabelsToScores.isEmpty()) {
                candgenConstituent =
                        new Constituent("NIL", 1.0, CANDGENVIEW, ta,
                                constituent.getStartSpan(), constituent.getEndSpan());
            }else {
                candgenConstituent = new Constituent(extendedLabelsToScores,
                        CANDGENVIEW, ta, constituent.getStartSpan(), constituent.getEndSpan());
            }
            candgenView.addConstituent(candgenConstituent);
        }
        ta.addView(CANDGENVIEW, candgenView);
    }

    /**
     * Turns a Wikipedia label into "LORELEI|Wikipedia" form.
     *
     * @param wikiLabel label to be transformed
     */
    private String extendLabel(String wikiLabel){
        String id = wiki2lorelei.get(wikiLabel);
        if(id == null)
            return null;
        return String.format("%s|%s",id,wikiLabel);
    }

    /**
     * Transforms labels to scores to labels used in nil analysis
     * @param labelsToScores map to transform
     * @return labelsToScores map with transformed labels
     */
    private Map<String, Double> extendLabelsToScores(Map<String, Double> labelsToScores){
        Map<String, Double> extendedLabelsToScores = new HashMap<>();
        for(Map.Entry<String, Double> labelScore : labelsToScores.entrySet()){
            String label = labelScore.getKey();
            String extendedLabel = extendLabel(label);
            if(extendedLabel == null)
                continue;
            Double score = labelScore.getValue();
            extendedLabelsToScores.put(extendedLabel, score);
        }
        return extendedLabelsToScores;
    }

    /**
     * Iterates through a directory of json serialized text annotations and links the nominals
     * in each document. Preconditions: TextAnnotation has a MENTION view which includes nominals.
     * Also, obviously, there must be an EL view.
     *
     * Note: the TAs in jsonTADir will be overwritten by this function. This shouldn't be consequential
     * since all of the data that was there is maintained.
     *
     * @param jsonTADir directory of json serialized TextAnnotations
     * @throws Exception
     */
    public void annotateNominals(String jsonTADir) throws Exception {
        addMentionView(jsonTADir);
        // get list of files in the directory
        File dir = new File(jsonTADir);
        File[] directoryListing = dir.listFiles();

        // iterate over TAs in directory adding a NOMLINK view to each
        for(File jsonSerializedTA : directoryListing) {
            String jsonTaPath = jsonTADir + jsonSerializedTA.getName();
            System.out.println(jsonTaPath);
            TextAnnotation ta = sh.	deserializeTextAnnotationFromFile(jsonTaPath, true);

            //if(!ta.hasView("NEUREL")) {
            //    // TODO: logging!
            //    System.out.println(ta.getId() + " no NEUREL view.");
            //    continue;
            //}

            linkNominals(ta);
            sh.serializeTextAnnotationToFile(ta, jsonTaPath, true, true);
        }
    }

    /**
     * Links nominal mention to nearest entity to its left in text. The start offset of the
     * target mention is strictly less than the start offset of the nominal.
     *
     * The Java TreeSet data structure is used to find the nearest linked NAM to the left
     * of a given NOM in the text. The NOM is then linked to that entity.
     *
     * @param ta the document to link
     */
    private void linkNominals(TextAnnotation ta){
        // get view on nominal mentions; get view of linked, named mentions
        View nerView = ta.getView(NERVIEW);
        View mentionView = ta.getView(MDVIEW);
        View elView = ta.getView(ELVIEW);

        // instatiate nominal link view
        View nomLinkView = new View(NLVIEW, "lorelei-eng-edl", ta, 1.0);

        // build map and treeset from EL view constituents
        HashMap<Integer, Constituent> spanStart2Con = new HashMap<>();
        TreeSet<Integer> spanStarts = new TreeSet<>();

        for(Constituent constituent : nerView.getConstituents()){
            spanStart2Con.put(constituent.getStartSpan(),constituent);
            spanStarts.add(constituent.getStartSpan());
        }

        // iterate through nominal cons, left linking each to its nearest NE link
        for(Constituent constituent : mentionView.getConstituents()){
            // only left link NOM mention types
            if(constituent.getAttribute("EntityMentionType").equals("NAM")) {
                System.out.println("There's a NAM in the nominals kill it.");
                System.exit(-1);
            }

            // only link when the extent does not include a NAM
            if(namInside(constituent, mentionView)) {
                continue;
            }

            // get spans of nominal
            Integer startSpan = constituent.getStartSpan();
            Integer endSpan = constituent.getEndSpan();

            // find span of first NAM which is strictly less than the start span of the NOM
            Integer leftLinkSpanStart = spanStarts.lower(startSpan);

            // get the NAM constituent corresponding to the offset
            Constituent leftLink = spanStart2Con.get(leftLinkSpanStart);
            // TODO: check typing?

            // we don't link if nominal is first mention in doc
            if(leftLink == null)
                continue;

            // link NOM with entity linked to by NAM
            Constituent nomLink = new Constituent(leftLink.getLabel(), leftLink.getConstituentScore(),
                    NLVIEW, ta, startSpan, endSpan);
            nomLinkView.addConstituent(nomLink);
        }
        ta.addView(NLVIEW, nomLinkView);
    }

    /**
     * Check if a NOM mention includes a NAM. In these cases, according to the specification
     * of the task, we are only to link the NAM and discard the nominal group.
     *
     * @param nomMention the nominal group to check for NAMs
     * @param mentionView the view from a mention detection system
     * @return true or false depending on whether a NAM is detected
     */
    public boolean namInside(Constituent nomMention, View mentionView){
        // get all constituents which share span
        List<Constituent> otherCons =
                mentionView.getConstituentsOverlappingCharSpan(nomMention.getStartCharOffset(),
                        nomMention.getEndCharOffset());

        // iterate over overlapping Constituents if one is a NAM, return true
        for(Constituent overlappingMention : otherCons){
            if(overlappingMention.getAttribute("EntityMentionType").equals("NAM"))
                return true;
        }

        return false;
    }
}
