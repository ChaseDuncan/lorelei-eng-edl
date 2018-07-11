package edu.illinois.cs.cogcomp.loreleiengedl.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class WikipediaResourceUtils {
    // path to map of Freebase IDs to Wikipedia page IDs
    protected static final String MID2WID =
            "/shared/preprocessed/upadhya3/enwiki-datamachine/mid.wikipedia_en_id";
    // path to map of Wikipedia page IDs to Wikipedia page titles
    static final String WID2TITLE =
            "/shared/preprocessed/upadhya3/enwiki-datamachine/idmap/enwiki-20170520.id2t";
    static HashMap<String,String> mid2WidMap;

    /**
     * Instantiates map from Wikipedia ids to Wikipedia titles.
     * @return HashMap from ids to titles
     * @throws IOException
     */
    public static HashMap<String, String> initializeWid2TitleMap() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(WID2TITLE));
        HashMap<String,String> wid2titleMap = new HashMap<>();
        String line;
        while((line = br.readLine()) != null){
            String[] spline = line.trim().split("\t");
            wid2titleMap.put(spline[0],spline[1]);
        }
        return wid2titleMap;
    }

    /**
     * Instantiates a map which maps Wikipedia titles to Wikipedia ids.
     * @return map of titles to wids
     * @throws IOException
     */
    public static HashMap<String, String> initializeTitle2WidMap() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(WID2TITLE));
        HashMap<String,String> title2WidMap = new HashMap<>();
        String line;
        while((line = br.readLine()) != null){
            String[] spline = line.trim().split("\t");
            title2WidMap.put(spline[1],spline[0]);
        }
        return title2WidMap;
    }

    public static HashMap<String, String> initializeTitle2MidMap() throws IOException {
        HashMap<String, String> wid2title = initializeWid2TitleMap();
        HashMap<String,String> title2MidMap = new HashMap<>();
        String line;
        BufferedReader br = new BufferedReader(new FileReader(MID2WID));
        while((line = br.readLine()) != null){
            String[] spline = line.trim().split("\t");
            title2MidMap.put(wid2title.get(spline[1]), spline[0]);
        }
        return title2MidMap;
    }
}
