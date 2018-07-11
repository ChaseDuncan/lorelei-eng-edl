package edu.illinois.cs.cogcomp.loreleiengedl.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;

import java.io.FileNotFoundException;
import java.util.*;

public class LinkUtils {
    /**
     * Return a map of hashtag tokens to a list of Wikipedia titles for IL9
     */
    public static HashMap<String, List<String>>
    initTweetToWikipediaIL9() throws FileNotFoundException {
        return initTweetToWikipedia("data/wiki-links/p_il9_wiki_links.txt");
    }

    /**
     * Return a map of hashtag tokens to a list of Wikipedia titles for IL9
     */
    public static HashMap<String, List<String>>
    initTweetToWikipediaIL10() throws FileNotFoundException {
        return initTweetToWikipedia("data/wiki-links/p_il10_wiki_links.txt");
    }

    /**
     * Return a map of hashtag tokens to a list of Wikipedia titles for a file of links
     *
     * @param linkFile path to file of links
     */
    private static HashMap<String, List<String>>
    initTweetToWikipedia(String linkFile) throws FileNotFoundException {
        HashMap<String, List<String>> tweetToWiki =
                new HashMap<String, List<String>>();
        String fileline = LineIO.slurp(linkFile);
        ArrayList<String> lines = new ArrayList(Arrays.asList(fileline.split("\n")));
        for (String line : lines){
            String[] spline = line.split("\t");

            if (spline.length < 2)
                continue;

            String surface = spline[0];
            ArrayList<String> wikiTitles =
                    new ArrayList(Arrays.asList(Arrays.copyOfRange(spline, 1,spline.length)));
            System.out.println(wikiTitles);
            tweetToWiki.put(spline[0], wikiTitles);
        }
        return tweetToWiki;
    }

    /**
     * Return a map of NEs to a list of Wikipedia titles for IL9
     */
    public static HashMap<String, List<String>>
    initTextToWikipediaIL9() throws FileNotFoundException {
        //TODO: file!
        return initTextToWikipedia("");
    }

    /**
     * Return a map of hashtag tokens to a list of Wikipedia titles for IL9
     */
    public static HashMap<String, List<String>>
    initTextToWikipediaIL10() throws FileNotFoundException {

        //TODO: file!
        return initTextToWikipedia("");
    }

    /**
     * Return a map of hashtag tokens to a list of Wikipedia titles for a file of links
     *
     * @param linkFile path to file of links
     */
    private static HashMap<String, List<String>>
    initTextToWikipedia(String linkFile) throws FileNotFoundException {
        HashMap<String, List<String>> textToWiki =
                new HashMap<String, List<String>>();
        String fileline = LineIO.slurp(linkFile);
        ArrayList<String> lines = new ArrayList(Arrays.asList(fileline.split("\n")));
        for (String line : lines){
            String[] spline = line.split("\t");

            if (spline.length < 2)
                continue;

            String surface = spline[0];
            ArrayList<String> wikiTitles =
                    new ArrayList(Arrays.asList(Arrays.copyOfRange(spline, 1,spline.length)));
            textToWiki.put(spline[0], wikiTitles);
        }
        return textToWiki;
    }

    /**
     * Initializes a map which takes Wikipedia titles to KB ids.
     *
     * @param entity2WikipediaTitle tsv file of LORELEI KB ids to Wikipedia titles.
     * @return a hashmap of kb ids to titles
     * @throws FileNotFoundException
     */
    public static HashMap<String,String>
    initWiki2LORELEIMap(String entity2WikipediaTitle) throws FileNotFoundException {
        HashMap<String, String> wiki2lorelei = new HashMap<>();
        ArrayList<String> idsToTitles = LineIO.read(entity2WikipediaTitle);
        for(String i2t : idsToTitles){
            String[] sp = i2t.split("\t");
            if(sp.length < 2)
                continue;
            wiki2lorelei.put(sp[1],sp[0]);
        }
        return wiki2lorelei;
    }
}
