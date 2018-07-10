package edu.illinois.cs.cogcomp.loreleiengedl;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Basic and incomplete tests for Lorelei edl system java component
 *
 * @author mssammon
 */
public class TestLoreleiEnglishEDL {

    @Test
    public void testNomOverlap() throws Exception {
        String file = "src/test/resources/ENG_NW_020119_20151124_H0040LIOS.json";
        boolean useJson = true;
        TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(file, useJson);

        View neView = ta.getView(LORELEIEnglishEDL.NERVIEW);
        View mdView = ta.getView(LORELEIEnglishEDL.MDVIEW);

        List<Constituent> badMents = new ArrayList<>();

        for (Constituent ment : mdView)
            if (LORELEIEnglishEDL.neOverlaps(ment, neView))
                badMents.add(ment);

        assertEquals(3, badMents.size());

        for (Constituent bm : badMents) {
            System.err.println("Bad mention: " + bm.toString());
        }
    }
}
