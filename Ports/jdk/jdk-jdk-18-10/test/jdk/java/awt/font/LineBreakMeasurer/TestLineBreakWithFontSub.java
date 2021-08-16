/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 4175418 8158924
 * @author John Raley
 * @summary This insures that bug 4175418: Font substitution in TextLayout /
 * LineBreakMeasurer is inconsistent has been fixed.  The problem was
 * that text was measured in one Font, but lines were produced
 * in a different font.
 */

/*
 * (C) Copyright IBM Corp. 1999, All Rights Reserved
 */

import java.text.AttributedString;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;

/**
 * This insures that bug 4175418: Font substitution in TextLayout /
 * LineBreakMeasurer is inconsistent has been fixed.  The problem was
 * that text was measured in one Font, but lines were produced
 * in a different font.  One symptom of this problem is that lines are
 * either too short or too long.  This test line-breaks a paragraph
 * and checks the line lengths to make sure breaks were chosen well.
 * This can be checked because the paragraph is so simple.
 */
public class TestLineBreakWithFontSub {

    public static void main(String[] args) {

        new TestLineBreakWithFontSub().test();
        System.out.println("Line break / font substitution test PASSED");
    }

    private static final String WORD = "word";
    private static final String SPACING = " ";
    // The Hebrew character in this string can trigger font substitution
    private static final String MIXED = "A\u05D0";

    private static final int NUM_WORDS = 12;

    private static final FontRenderContext DEFAULT_FRC =
                            new FontRenderContext(null, false, false);

    public void test() {

        // construct a paragraph as follows: MIXED + [SPACING + WORD] + ...
        StringBuffer text = new StringBuffer(MIXED);
        for (int i=0; i < NUM_WORDS; i++) {
            text.append(SPACING);
            text.append(WORD);
        }

        AttributedString attrString = new AttributedString(text.toString());
        attrString.addAttribute(TextAttribute.SIZE, new Float(24.0));

        LineBreakMeasurer measurer = new LineBreakMeasurer(attrString.getIterator(),
                                                           DEFAULT_FRC);

        // get width of a space-word sequence, in context
        int sequenceLength = WORD.length()+SPACING.length();
        measurer.setPosition(text.length() - sequenceLength);

        TextLayout layout = measurer.nextLayout(10000.0f);

        if (layout.getCharacterCount() != sequenceLength) {
            throw new Error("layout length is incorrect!");
        }

        final float sequenceAdvance = layout.getVisibleAdvance();

        float wrappingWidth = sequenceAdvance * 2;

        // now run test with a variety of widths
        while (wrappingWidth < (sequenceAdvance*NUM_WORDS)) {
            measurer.setPosition(0);
            checkMeasurer(measurer,
                          wrappingWidth,
                          sequenceAdvance,
                          text.length());
            wrappingWidth += sequenceAdvance / 5;
        }
    }

    /**
     * Iterate through measurer and check that every line is
     * not too long and not too short, but just right.
     */
    private void checkMeasurer(LineBreakMeasurer measurer,
                               float wrappingWidth,
                               float sequenceAdvance,
                               int endPosition) {

        do {
            TextLayout layout = measurer.nextLayout(wrappingWidth);
            float visAdvance = layout.getVisibleAdvance();

            // Check that wrappingWidth-sequenceAdvance < visAdvance
            // Also, if we're not at the end of the paragraph,
            // check that visAdvance <= wrappingWidth

            if (visAdvance > wrappingWidth) {
                // line is too long for given wrapping width
                throw new Error("layout is too long");
            }

            if (measurer.getPosition() < endPosition) {
                if (visAdvance <= wrappingWidth - sequenceAdvance) {
                    // line is too short for given wrapping width;
                    // another word would have fit
                    throw new Error("room for more words on line.  diff=" +
                                    (wrappingWidth - sequenceAdvance - visAdvance));
                }
            }
        } while (measurer.getPosition() != endPosition);
    }
}
