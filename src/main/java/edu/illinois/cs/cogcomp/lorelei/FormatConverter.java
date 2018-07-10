package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.lorelei.xml.XMLException;
import edu.illinois.cs.cogcomp.loreleiengedl.utils.LinkUtils;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is intended to be a command line script. This should be changed very rarely.
 */
public class FormatConverter {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option help = new Option( "help", "print this message" );

        Option fromformat = Option.builder("from")
                .hasArg()
                .required()
                .build();

        Option toformat = Option.builder("to")
                .hasArg()
                .required()
                .build();

        Option infolder = Option.builder("infolder")
                .hasArg()
                .required()
                .build();

        Option outfolder = Option.builder("outfolder")
                .hasArg()
                .required()
                .build();

        // This is a folder containing filenames to exclude.
        Option excludeopt = Option.builder("exclude")
                .hasArg()
                .build();

        Option verboseoption = Option.builder("verbose")
                .build();

        options.addOption(help);
        options.addOption(fromformat);
        options.addOption(toformat);
        options.addOption(infolder);
        options.addOption(outfolder);
        options.addOption(excludeopt);
        options.addOption(verboseoption);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if(cmd.hasOption("verbose")){
            Ltf2TextAnnotation.verbose = true;
        }

        String excludepath = null;
        if(cmd.hasOption("exclude")){
            excludepath = cmd.getOptionValue("exclude");
        }

        String from = cmd.getOptionValue("from").toLowerCase();
        String to = cmd.getOptionValue("to").toLowerCase();
        String inpath = cmd.getOptionValue("infolder");
        String outpath = cmd.getOptionValue("outfolder");

        // allow either txt or text
        if(to.equals("txt")){
            to = "text";
        }

        if(from.equals("ta") && to.equals("json")){
            Ta2Json(inpath, outpath);
        }else if(from.equals("json") && to.equals("ta")){
            Json2Ta(inpath, outpath);
        }else if(from.equals("ltf") && to.equals("json")){
            Ltf2Json(inpath, outpath);
        }else if(from.equals("ltf") && to.equals("ta")){
            Ltf2Ta(inpath, outpath);
        }else if(from.equals("ltf") && to.equals("conll")){
            Ltf2Conll(inpath, outpath);
        }else if(from.equals("ta") && to.equals("text")){
            Ta2Text(inpath, outpath);
        }else if(from.equals("json") && to.equals("text")){
            Json2Text(inpath, outpath);
        }else if(from.equals("ltf") && to.equals("text")){
            Ltf2Text(inpath, outpath);
        }else if(from.equals("ta") && to.equals("column")){
            Ta2column(inpath, outpath);
        }else if(from.equals("json") && to.equals("column")){
            Json2column(inpath, outpath);
        }else if(from.equals("ltfzip") && to.equals("text")){
            LtfZip2Text(inpath, outpath, excludepath);
        }else if(from.equals("conll") && to.equals("json")) {
            Conll2Json(inpath, outpath);
        }else if(from.equals("conll") && to.equals("ta")){
            Conll2Ta(inpath, outpath);
        }
        else if(from.equals("json") && to.equals("tab")){
            //Json2Tab(inpath, outpath);
        }else{
            System.err.println(String.format("Conversion not supported: %s to %s", from, to));
        }
    }

    /**
     * This writes the tokenized text of a TextAnnotation out to a single file (used for training Brown clusters, or a language model
     * or word vectors.
     * @param tas list of text annotations
     * @param outfile filename (usually .txt)
     * @throws Exception
     */
    public static void writeText(List<TextAnnotation> tas, String outfile) throws IOException {
        for(TextAnnotation ta : tas){
            LineIO.append(outfile, ta.getTokenizedText());
        }
    }

    public static void Ltf2Conll(String inpath, String outpath) throws XMLException, IOException {
        System.out.println("Warning! Writing to CoNLL format can be lossy!");
        List<TextAnnotation> tas = Ltf2TextAnnotation.readLTF(inpath);
        CoNLLNerReader.TaToConll(tas, outpath);
        System.out.println(String.format("Wrote %d textannotations to %s", tas.size(), outpath));
    }

    public static void Ltf2Json(String inpath, String outpath) throws XMLException, IOException {
        List<TextAnnotation> tas = Ltf2TextAnnotation.readLTF(inpath);
        for(TextAnnotation ta : tas){
            SerializationHelper.serializeTextAnnotationToFile(ta, outpath + "/" + ta.getId(), true,true);
        }
        System.out.println(String.format("Wrote %d textannotations to %s", tas.size(), outpath));

    }

    public static void Ltf2Ta(String inpath, String outpath) throws XMLException, IOException {
        List<TextAnnotation> tas = Ltf2TextAnnotation.readLTF(inpath);
        for(TextAnnotation ta : tas){
            SerializationHelper.serializeTextAnnotationToFile(ta, outpath + "/" + ta.getId(), true);
        }
        System.out.println(String.format("Wrote %d textannotations to %s", tas.size(), outpath));
    }

    public static void Json2Ta(String inpath, String outpath) throws Exception {
        File tapath = new File(inpath);
        File[] filelist = tapath.listFiles();
        for(File f : filelist){
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath(), true);
            SerializationHelper.serializeTextAnnotationToFile(ta, outpath + "/" + ta.getId(), true);
        }
        System.out.println(String.format("Wrote %d textannotations to %s", filelist.length, outpath));
    }

    public static void Ta2Json(String inpath, String outpath) throws Exception {
        File tapath = new File(inpath);
        File[] filelist = tapath.listFiles();
        for(File f : filelist){
            try {
                TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath());
                SerializationHelper.serializeTextAnnotationToFile(ta, outpath + "/" + ta.getId(), true, true);
            }catch(Exception e){
                System.err.println("Processing failed for: " + f.getAbsolutePath());
            }

        }
        System.out.println(String.format("Wrote %d textannotations to %s", filelist.length, outpath));
    }

    private static List<String> getColumnLinesFromTa(TextAnnotation ta){
        List<String> talines = new ArrayList<>();
        View sentview = ta.getView(ViewNames.SENTENCE);
        View nerview = ta.getView(ViewNames.NER_CONLL);
        for (int i = 0; i < ta.getTokens().length; i++) {

            // Default "outside" label in NER_CONLL
            String label = "O";

            List<Constituent> constituents = nerview.getConstituentsCoveringToken(i);

            // should be just one constituent
            if (constituents.size() > 0) {
                Constituent c = constituents.get(0);
                if (c.getStartSpan() == i) {
                    label = "B-" + c.getLabel();
                } else {
                    label = "I-" + c.getLabel();
                }
            }
            talines.add(String.format("%s %s",ta.getToken(i), label));
            List<Constituent> sents = sentview.getConstituentsCoveringToken(i);
            if (sents.size() > 0) {
                Constituent sent = sents.get(0);

                int end = sent.getEndSpan();
                if (i == end - 1) {
                    talines.add("");
                }
            }
        }
        return talines;
    }

    public static void Ta2column(String inpath, String outfile) throws Exception {
        File tapath = new File(inpath);
        File[] filelist = tapath.listFiles();

        List<String> talines = new ArrayList<>();

        for(File f : filelist){
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath());
            talines.addAll(getColumnLinesFromTa(ta));
            talines.add("");
        }
        FileUtils.writeLines(Paths.get(outfile).toFile(), talines);
        System.out.println(String.format("Wrote %d textannotations to %s", filelist.length, outfile));
    }

    public static void Json2column(String inpath, String outfile) throws Exception {
        File tapath = new File(inpath);
        File[] filelist = tapath.listFiles();

        List<String> talines = new ArrayList<>();

        for(File f : filelist){
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath(), true);
            talines.addAll(getColumnLinesFromTa(ta));
            talines.add("");
        }
        FileUtils.writeLines(Paths.get(outfile).toFile(), talines);
        System.out.println(String.format("Wrote %d textannotations to %s", filelist.length, outfile));
    }

    /**
     * This function can be sped up significantly... no need to go through the middle man.
     * @param inpath path to directory of ltf files.
     * @param outfile
     * @throws XMLException
     * @throws IOException
     */
    private static void Ltf2Text(String inpath, String outfile) throws XMLException, IOException {
        FilenameFilter ltf_filter = (File dir, String name) -> {
            return name.endsWith(".ltf.xml");
        };

        File[] ltf_files = (new File(inpath)).listFiles(ltf_filter);

        // I happen to know that LTF files are structured in this way.
        Pattern p = Pattern.compile(">(.*)</TOKEN>");

        int i = 0;
        for(File f : ltf_files){

            if(i%1000 == 0){
                System.out.println("Read: " + i + "/" + ltf_files.length +" files.");
            }
            i++;

            List<String> lines = LineIO.read(f.getAbsolutePath());
            StringJoiner sj = new StringJoiner(" ");
            for(String line : lines){
                if(line.startsWith("<TOKEN")){
                    Matcher m = p.matcher(line);
                    if(m.find()) {
                        String word = m.group(1);
                        sj.add(word);
                    }
                }
            }
            sj.add("\n");
            LineIO.append(outfile, sj.toString());
        }

        System.out.println(String.format("Wrote %d ltf files to %s", ltf_files.length, outfile));
    }

    /**
     * This reads directory from the zip files.
     * @param inpath
     * @param outfile
     * @throws XMLException
     * @throws IOException
     */
    private static void LtfZip2Text(String inpath, String outfile, String excludepath) throws XMLException, IOException {
        FilenameFilter ltf_filter = (File dir, String name) -> {
            return name.endsWith(".ltf.zip");
        };

        HashSet<String> excludeset = new HashSet<>();
        if(excludepath != null) {
            System.out.println(excludepath);
            for (String ex : (new File(excludepath)).list()) {
                excludeset.add(ex);
            }
            System.out.println("Exclude set has: " + excludeset.size() + " elements.");
        }

        File[] ltfzip_files = (new File(inpath)).listFiles(ltf_filter);

        // I happen to know that LTF files are structured in this way.
        Pattern p = Pattern.compile(">(.*)</TOKEN>");

        int i = 0;
        for(File zipf : ltfzip_files){
            if(i%10 == 0){
                System.out.println("Read: " + i + "/" + ltfzip_files.length +" files.");
            }
            i++;

            StringJoiner sj = new StringJoiner(" ");
            ZipFile zip = new ZipFile(zipf.getAbsolutePath());

            for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (!entry.isDirectory()) {
                    InputStream ins = zip.getInputStream(entry);
                    InputStreamReader input = new InputStreamReader(ins);
                    BufferedReader reader = new BufferedReader(input);

                    String docid = entry.getName().split("\\.")[0].split("/")[1];

                    // exclude these files.
                    if(excludeset.contains(docid)){
                        System.out.println("Skipping: " + docid);
                        continue;
                    }

                    for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                        if(line.startsWith("<TOKEN")){
                            Matcher m = p.matcher(line);
                            if(m.find()) {
                                String word = m.group(1);
                                sj.add(word);
                            }
                        }
                    }
                    reader.close();
                    sj.add("\n");
                }
            }
            LineIO.append(outfile, sj.toString());
        }

        System.out.println(String.format("Wrote %d ltf files to %s", ltfzip_files.length, outfile));
    }


    private static void Json2Text(String inpath, String outfile) throws Exception {
        File tapath = new File(inpath);
        File[] filelist = tapath.listFiles();
        List<TextAnnotation> tas = new ArrayList<>();
        for(File f : filelist){
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath(), true);
            tas.add(ta);
        }
        writeText(tas, outfile);
        System.out.println(String.format("Wrote %d textannotations to %s", filelist.length, outfile));
    }

    private static void Ta2Text(String inpath, String outfile) throws Exception {
        File tapath = new File(inpath);
        File[] filelist = tapath.listFiles();
        List<TextAnnotation> tas = new ArrayList<>();
        for(File f : filelist){
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath());
            tas.add(ta);
        }
        writeText(tas, outfile);
        System.out.println(String.format("Wrote %d textannotations to %s", filelist.length, outfile));
    }

    private static void Conll2Json(String inpath, String outpath) throws IOException {
        CoNLLNerReader cnr = new CoNLLNerReader(inpath);
        while(cnr.hasNext()){
            TextAnnotation ta = cnr.next();
            SerializationHelper.serializeTextAnnotationToFile(ta, outpath + "/" + ta.getId(),
                    true, true);
        }
    }

    private static void Conll2Ta(String inpath, String outpath) throws IOException {
        CoNLLNerReader cnr = new CoNLLNerReader(inpath);
        while(cnr.hasNext()){
            TextAnnotation ta = cnr.next();
            SerializationHelper.serializeTextAnnotationToFile(ta, outpath + "/" + ta.getId(),true);
        }
    }

    /**
     * Creates a LORELEI submission format tab file from a directory of Java serialized
     * TextAnnotations. Preconditions: the TextAnnotations must have three views MENTION,
     * NEUREL, and NOMLINK.
     *
     * @param taDir directory of Java serialized TextAnnotations
     * @param outputFilePath where tab file shall be written
     * @param entity2WikipediaTitle path to map of kb entities to wikititles
     * @param loreleiKB use kb ids
     */
    public static void EnglishEDL2Tab(String taDir, String outputFilePath,
                                      String entity2WikipediaTitle, boolean loreleiKB) throws Exception {
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath));
        File directory = new File(taDir);
        assert directory.exists();
        File[] directoryListing = directory.listFiles();

        // iterate over TAs in directory adding a NOMLINK view to each
        for(File serializedTA : directoryListing) {
            String jsonTaPath = taDir + serializedTA.getName();
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(jsonTaPath, true);
            if(!ta.hasView("NEUREL"))
                continue;
            writeSub(ta,bw,entity2WikipediaTitle,loreleiKB);
        }
        bw.close();
    }

    static String SYSTEM_NAME = "Penn";
    static int mentionID = 0;

    /**
     * // TODO: this converts Wikipedia titles to LORELEI ids need separate function for cases
     * // where we don't want this functionality.
     *
     * Helper for EnglishEDL2Tab which wirtes the TextAnnotation to LORELEI submission formatted
     * lines in a BufferedWriter.
     *
     * @param ta TextAnnotation to write
     * @param bw BufferedWriter to write to
     */
    private static void writeSub(TextAnnotation ta, BufferedWriter bw,
                                 String entity2WikipediaTitle, boolean loreleiKB) throws IOException {
        assert ta.hasView("NEUREL");
        assert ta.hasView("MENTION");
        assert ta.hasView("NOMLINK");

        View namlinkView = ta.getView("NEUREL");
        View mentionView = ta.getView("MENTION");
        View nomlinkView = ta.getView("NOMLINK");

        // create hash map which maps Wikipedia titles to LORELEI KB ids
        HashMap<String, String> wiki2lorelei = LinkUtils.initWiki2LORELEIMap(entity2WikipediaTitle);

        for(Constituent mention : mentionView.getConstituents()){
            String[] types = mention.getLabel().split("-");
            String nounType = types[0];
            String entType = types[1];
            int startCharOff = mention.getStartCharOffset();
            int endCharOff = mention.getEndCharOffset() - 1;

            View typeElView = null;
            // get the appropriate entity links based on noun type of menton
            if(nounType.equals("NOM"))
                typeElView = nomlinkView;
            if(nounType.equals("NAM"))
                typeElView = namlinkView;

            List<Constituent> entityLabel =
                    typeElView.getConstituentsWithSpan(mention.getSpan());

            // some nominal mentions will not be linked
            if (entityLabel.size() == 0 && nounType.equals("NOM"))
                continue;

            // TODO: why was this here?
            //if (entityLabel.size() == 0 )
            //    continue;

            assert entityLabel.size() == 1;
            String entity = entityLabel.get(0).getLabel();
            if(loreleiKB) {
                String kbId = wiki2lorelei.get(entity);

                if (kbId == null)
                    kbId = "NIL";
                entity = kbId;
            }
            String surface = mention.getSurfaceForm();

            bw.write(SYSTEM_NAME + "\t" + mentionID++ + "\t" + surface + "\t" + ta.getId()+":" +
                    startCharOff+ "-" + endCharOff + "\t" + entity + "\t" + entType + "\t" + nounType +
                    "\t" + "1.0" + "\n");
        }
    }

    // TODO: this whole thing shouldn't be here
}
