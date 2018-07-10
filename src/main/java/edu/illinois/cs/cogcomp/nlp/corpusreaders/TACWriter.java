package edu.illinois.cs.cogcomp.nlp.corpusreaders;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.XmlTextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.StringTransformation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import edu.illinois.cs.cogcomp.loreleiengedl.utils.WikipediaResourceUtils;

/**
 * Created by cddunca2 on 2/28/18.
 */
public class TACWriter {
    static int mentionID = 0;
    static HashMap<String, String> title2mid = null;

    static {
        try {
            title2mid = WikipediaResourceUtils.initializeTitle2MidMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeTACSubmissionFormatFile(String viewName, XmlTextAnnotation xmlTextAnnotation,
                                                    BufferedWriter bw,
                                                    String systemName) throws IOException {
        TextAnnotation ta = xmlTextAnnotation.getTextAnnotation();
        View wikiView = ta.getView(viewName);
        StringTransformation st = xmlTextAnnotation.getXmlSt();
        List<Constituent> constituents = wikiView.getConstituents();
        IntPair origOffsets;
        for(Constituent c : constituents){
            c.getLabel();
            origOffsets = st.getOriginalOffsets(c.getStartCharOffset(),c.getEndCharOffset());
                    //    if(m.getType().equals("TTL")) continue;
            //View nerView = ta.getView("English_NERVIEW");
            View nerView = ta.getView("MENTION");

            String type = nerView.getConstituentsCoveringCharSpan(c.getStartCharOffset(),c.getEndCharOffset())
                    .get(0).getLabel();
            String label = title2mid.get(c.getLabel());
            if (label == null || label.contains("unk_wid"))
                label = "NIL";
            String[] types = type.split("-");
            bw.write(String.format("%s\t%d\t%s\t%s\t%s\t%s\t%s\t%f\n",
                                    systemName,
                                    mentionID,
                                    cleanSurface(c.getSurfaceForm()),
                                    docId(ta.getId(),origOffsets.getFirst(),origOffsets.getSecond()-1),
                                    label,types[1],types[0],
                                    c.getConstituentScore()));

            mentionID++;
        }
    }

    private static String docId(String id, int startOffset, int endOffset){
        return String.format("%s:%d-%d",
                FilenameUtils.removeExtension(id).split("\\.")[0],startOffset,endOffset);
    }

    private static String cleanSurface(String surface){
        return surface.replaceAll("[\\t\\n\\r]+"," ").trim();
    }
}
