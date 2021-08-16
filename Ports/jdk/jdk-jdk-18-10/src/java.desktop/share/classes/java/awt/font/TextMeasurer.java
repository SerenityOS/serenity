/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * (C) Copyright Taligent, Inc. 1996 - 1997, All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998, All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by Taligent, Inc., a wholly-owned subsidiary
 * of IBM. These materials are provided under terms of a License
 * Agreement between Taligent and Sun. This technology is protected
 * by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.awt.font;

import java.awt.Font;

import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.Bidi;
import java.text.BreakIterator;
import java.text.CharacterIterator;

import java.awt.font.FontRenderContext;

import java.util.Hashtable;
import java.util.Map;

import sun.font.AttributeValues;
import sun.font.BidiUtils;
import sun.font.TextLineComponent;
import sun.font.TextLabelFactory;
import sun.font.FontResolver;

/**
 * The {@code TextMeasurer} class provides the primitive operations
 * needed for line break: measuring up to a given advance, determining the
 * advance of a range of characters, and generating a
 * {@code TextLayout} for a range of characters. It also provides
 * methods for incremental editing of paragraphs.
 * <p>
 * A {@code TextMeasurer} object is constructed with an
 * {@link java.text.AttributedCharacterIterator AttributedCharacterIterator}
 * representing a single paragraph of text.  The value returned by the
 * {@link AttributedCharacterIterator#getBeginIndex() getBeginIndex}
 * method of {@code AttributedCharacterIterator}
 * defines the absolute index of the first character.  The value
 * returned by the
 * {@link AttributedCharacterIterator#getEndIndex() getEndIndex}
 * method of {@code AttributedCharacterIterator} defines the index
 * past the last character.  These values define the range of indexes to
 * use in calls to the {@code TextMeasurer}.  For example, calls to
 * get the advance of a range of text or the line break of a range of text
 * must use indexes between the beginning and end index values.  Calls to
 * {@link #insertChar(java.text.AttributedCharacterIterator, int) insertChar}
 * and
 * {@link #deleteChar(java.text.AttributedCharacterIterator, int) deleteChar}
 * reset the {@code TextMeasurer} to use the beginning index and end
 * index of the {@code AttributedCharacterIterator} passed in those calls.
 * <p>
 * Most clients will use the more convenient {@code LineBreakMeasurer},
 * which implements the standard line break policy (placing as many words
 * as will fit on each line).
 *
 * @author John Raley
 * @see LineBreakMeasurer
 * @since 1.3
 */

public final class TextMeasurer implements Cloneable {

    // Number of lines to format to.
    private static float EST_LINES = (float) 2.1;

    /*
    static {
        String s = System.getProperty("estLines");
        if (s != null) {
            try {
                Float f = Float.valueOf(s);
                EST_LINES = f.floatValue();
            }
            catch(NumberFormatException e) {
            }
        }
        //System.out.println("EST_LINES="+EST_LINES);
    }
    */

    private FontRenderContext fFrc;

    private int fStart;

    // characters in source text
    private char[] fChars;

    // Bidi for this paragraph
    private Bidi fBidi;

    // Levels array for chars in this paragraph - needed to reorder
    // trailing counterdirectional whitespace
    private byte[] fLevels;

    // line components in logical order
    private TextLineComponent[] fComponents;

    // index where components begin
    private int fComponentStart;

    // index where components end
    private int fComponentLimit;

    private boolean haveLayoutWindow;

    // used to find valid starting points for line components
    private BreakIterator fLineBreak = null;
    private CharArrayIterator charIter = null;
    int layoutCount = 0;
    int layoutCharCount = 0;

    // paragraph, with resolved fonts and styles
    private StyledParagraph fParagraph;

    // paragraph data - same across all layouts
    private boolean fIsDirectionLTR;
    private byte fBaseline;
    private float[] fBaselineOffsets;
    private float fJustifyRatio = 1;

    /**
     * Constructs a {@code TextMeasurer} from the source text.
     * The source text should be a single entire paragraph.
     * @param text the source paragraph.  Cannot be null.
     * @param frc the information about a graphics device which is needed
     *       to measure the text correctly.  Cannot be null.
     */
    public TextMeasurer(AttributedCharacterIterator text, FontRenderContext frc) {

        fFrc = frc;
        initAll(text);
    }

    protected Object clone() {
        TextMeasurer other;
        try {
            other = (TextMeasurer) super.clone();
        }
        catch(CloneNotSupportedException e) {
            throw new Error();
        }
        if (fComponents != null) {
            other.fComponents = fComponents.clone();
        }
        return other;
    }

    private void invalidateComponents() {
        fComponentStart = fComponentLimit = fChars.length;
        fComponents = null;
        haveLayoutWindow = false;
    }

    /**
     * Initialize state, including fChars array, direction, and
     * fBidi.
     */
    private void initAll(AttributedCharacterIterator text) {

        fStart = text.getBeginIndex();

        // extract chars
        fChars = new char[text.getEndIndex() - fStart];

        int n = 0;
        for (char c = text.first();
             c != CharacterIterator.DONE;
             c = text.next())
        {
            fChars[n++] = c;
        }

        text.first();

        fBidi = new Bidi(text);
        if (fBidi.isLeftToRight()) {
            fBidi = null;
        }

        text.first();
        Map<? extends Attribute, ?> paragraphAttrs = text.getAttributes();
        NumericShaper shaper = AttributeValues.getNumericShaping(paragraphAttrs);
        if (shaper != null) {
            shaper.shape(fChars, 0, fChars.length);
        }

        fParagraph = new StyledParagraph(text, fChars);

        // set paragraph attributes
        {
            // If there's an embedded graphic at the start of the
            // paragraph, look for the first non-graphic character
            // and use it and its font to initialize the paragraph.
            // If not, use the first graphic to initialize.
            fJustifyRatio = AttributeValues.getJustification(paragraphAttrs);

            boolean haveFont = TextLine.advanceToFirstFont(text);

            if (haveFont) {
                Font defaultFont = TextLine.getFontAtCurrentPos(text);
                int charsStart = text.getIndex() - text.getBeginIndex();
                LineMetrics lm = defaultFont.getLineMetrics(fChars, charsStart, charsStart+1, fFrc);
                fBaseline = (byte) lm.getBaselineIndex();
                fBaselineOffsets = lm.getBaselineOffsets();
            }
            else {
                // hmmm what to do here?  Just try to supply reasonable
                // values I guess.

                GraphicAttribute graphic = (GraphicAttribute)
                                paragraphAttrs.get(TextAttribute.CHAR_REPLACEMENT);
                fBaseline = TextLayout.getBaselineFromGraphic(graphic);
                Hashtable<Attribute, ?> fmap = new Hashtable<>(5, (float)0.9);
                Font dummyFont = new Font(fmap);
                LineMetrics lm = dummyFont.getLineMetrics(" ", 0, 1, fFrc);
                fBaselineOffsets = lm.getBaselineOffsets();
            }
            fBaselineOffsets = TextLine.getNormalizedOffsets(fBaselineOffsets, fBaseline);
        }

        invalidateComponents();
    }

    /**
     * Generate components for the paragraph.  fChars, fBidi should have been
     * initialized already.
     */
    private void generateComponents(int startingAt, int endingAt) {

        if (collectStats) {
            formattedChars += (endingAt-startingAt);
        }
        int layoutFlags = 0; // no extra info yet, bidi determines run and line direction
        TextLabelFactory factory = new TextLabelFactory(fFrc, fChars, fBidi, layoutFlags);

        int[] charsLtoV = null;

        if (fBidi != null) {
            fLevels = BidiUtils.getLevels(fBidi);
            int[] charsVtoL = BidiUtils.createVisualToLogicalMap(fLevels);
            charsLtoV = BidiUtils.createInverseMap(charsVtoL);
            fIsDirectionLTR = fBidi.baseIsLeftToRight();
        }
        else {
            fLevels = null;
            fIsDirectionLTR = true;
        }

        try {
            fComponents = TextLine.getComponents(
                fParagraph, fChars, startingAt, endingAt, charsLtoV, fLevels, factory);
        }
        catch(IllegalArgumentException e) {
            System.out.println("startingAt="+startingAt+"; endingAt="+endingAt);
            System.out.println("fComponentLimit="+fComponentLimit);
            throw e;
        }

        fComponentStart = startingAt;
        fComponentLimit = endingAt;
        //debugFormatCount += (endingAt-startingAt);
    }

    private int calcLineBreak(final int pos, final float maxAdvance) {

        // either of these statements removes the bug:
        //generateComponents(0, fChars.length);
        //generateComponents(pos, fChars.length);

        int startPos = pos;
        float width = maxAdvance;

        int tlcIndex;
        int tlcStart = fComponentStart;

        for (tlcIndex = 0; tlcIndex < fComponents.length; tlcIndex++) {
            int gaLimit = tlcStart + fComponents[tlcIndex].getNumCharacters();
            if (gaLimit > startPos) {
                break;
            }
            else {
                tlcStart = gaLimit;
            }
        }

        // tlcStart is now the start of the tlc at tlcIndex

        for (; tlcIndex < fComponents.length; tlcIndex++) {

            TextLineComponent tlc = fComponents[tlcIndex];
            int numCharsInGa = tlc.getNumCharacters();

            int lineBreak = tlc.getLineBreakIndex(startPos - tlcStart, width);
            if (lineBreak == numCharsInGa && tlcIndex < fComponents.length) {
                width -= tlc.getAdvanceBetween(startPos - tlcStart, lineBreak);
                tlcStart += numCharsInGa;
                startPos = tlcStart;
            }
            else {
                return tlcStart + lineBreak;
            }
        }

        if (fComponentLimit < fChars.length) {
            // format more text and try again
            //if (haveLayoutWindow) {
            //    outOfWindow++;
            //}

            generateComponents(pos, fChars.length);
            return calcLineBreak(pos, maxAdvance);
        }

        return fChars.length;
    }

    /**
     * According to the Unicode Bidirectional Behavior specification
     * (Unicode Standard 2.0, section 3.11), whitespace at the ends
     * of lines which would naturally flow against the base direction
     * must be made to flow with the line direction, and moved to the
     * end of the line.  This method returns the start of the sequence
     * of trailing whitespace characters to move to the end of a
     * line taken from the given range.
     */
    private int trailingCdWhitespaceStart(int startPos, int limitPos) {

        if (fLevels != null) {
            // Back up over counterdirectional whitespace
            final byte baseLevel = (byte) (fIsDirectionLTR? 0 : 1);
            for (int cdWsStart = limitPos; --cdWsStart >= startPos;) {
                if ((fLevels[cdWsStart] % 2) == baseLevel ||
                        Character.getDirectionality(fChars[cdWsStart]) != Character.DIRECTIONALITY_WHITESPACE) {
                    return ++cdWsStart;
                }
            }
        }

        return startPos;
    }

    private TextLineComponent[] makeComponentsOnRange(int startPos,
                                                      int limitPos) {

        // sigh I really hate to do this here since it's part of the
        // bidi algorithm.
        // cdWsStart is the start of the trailing counterdirectional
        // whitespace
        final int cdWsStart = trailingCdWhitespaceStart(startPos, limitPos);

        int tlcIndex;
        int tlcStart = fComponentStart;

        for (tlcIndex = 0; tlcIndex < fComponents.length; tlcIndex++) {
            int gaLimit = tlcStart + fComponents[tlcIndex].getNumCharacters();
            if (gaLimit > startPos) {
                break;
            }
            else {
                tlcStart = gaLimit;
            }
        }

        // tlcStart is now the start of the tlc at tlcIndex

        int componentCount;
        {
            boolean split = false;
            int compStart = tlcStart;
            int lim=tlcIndex;
            for (boolean cont=true; cont; lim++) {
                int gaLimit = compStart + fComponents[lim].getNumCharacters();
                if (cdWsStart > Math.max(compStart, startPos)
                            && cdWsStart < Math.min(gaLimit, limitPos)) {
                    split = true;
                }
                if (gaLimit >= limitPos) {
                    cont=false;
                }
                else {
                    compStart = gaLimit;
                }
            }
            componentCount = lim-tlcIndex;
            if (split) {
                componentCount++;
            }
        }

        TextLineComponent[] components = new TextLineComponent[componentCount];
        int newCompIndex = 0;
        int linePos = startPos;

        int breakPt = cdWsStart;

        int subsetFlag;
        if (breakPt == startPos) {
            subsetFlag = fIsDirectionLTR? TextLineComponent.LEFT_TO_RIGHT :
                                          TextLineComponent.RIGHT_TO_LEFT;
            breakPt = limitPos;
        }
        else {
            subsetFlag = TextLineComponent.UNCHANGED;
        }

        while (linePos < limitPos) {

            int compLength = fComponents[tlcIndex].getNumCharacters();
            int tlcLimit = tlcStart + compLength;

            int start = Math.max(linePos, tlcStart);
            int limit = Math.min(breakPt, tlcLimit);

            components[newCompIndex++] = fComponents[tlcIndex].getSubset(
                                                                start-tlcStart,
                                                                limit-tlcStart,
                                                                subsetFlag);
            linePos += (limit-start);
            if (linePos == breakPt) {
                breakPt = limitPos;
                subsetFlag = fIsDirectionLTR? TextLineComponent.LEFT_TO_RIGHT :
                                              TextLineComponent.RIGHT_TO_LEFT;
            }
            if (linePos == tlcLimit) {
                tlcIndex++;
                tlcStart = tlcLimit;
            }
        }

        return components;
    }

    private TextLine makeTextLineOnRange(int startPos, int limitPos) {

        int[] charsLtoV = null;
        byte[] charLevels = null;

        if (fBidi != null) {
            Bidi lineBidi = fBidi.createLineBidi(startPos, limitPos);
            charLevels = BidiUtils.getLevels(lineBidi);
            int[] charsVtoL = BidiUtils.createVisualToLogicalMap(charLevels);
            charsLtoV = BidiUtils.createInverseMap(charsVtoL);
        }

        TextLineComponent[] components = makeComponentsOnRange(startPos, limitPos);

        return new TextLine(fFrc,
                            components,
                            fBaselineOffsets,
                            fChars,
                            startPos,
                            limitPos,
                            charsLtoV,
                            charLevels,
                            fIsDirectionLTR);

    }

    private void ensureComponents(int start, int limit) {

        if (start < fComponentStart || limit > fComponentLimit) {
            generateComponents(start, limit);
        }
    }

    private void makeLayoutWindow(int localStart) {

        int compStart = localStart;
        int compLimit = fChars.length;

        // If we've already gone past the layout window, format to end of paragraph
        if (layoutCount > 0 && !haveLayoutWindow) {
            float avgLineLength = Math.max(layoutCharCount / layoutCount, 1);
            compLimit = Math.min(localStart + (int)(avgLineLength*EST_LINES), fChars.length);
        }

        if (localStart > 0 || compLimit < fChars.length) {
            if (charIter == null) {
                charIter = new CharArrayIterator(fChars);
            }
            else {
                charIter.reset(fChars);
            }
            if (fLineBreak == null) {
                fLineBreak = BreakIterator.getLineInstance();
            }
            fLineBreak.setText(charIter);
            if (localStart > 0) {
                if (!fLineBreak.isBoundary(localStart)) {
                    compStart = fLineBreak.preceding(localStart);
                }
            }
            if (compLimit < fChars.length) {
                if (!fLineBreak.isBoundary(compLimit)) {
                    compLimit = fLineBreak.following(compLimit);
                }
            }
        }

        ensureComponents(compStart, compLimit);
        haveLayoutWindow = true;
    }

    /**
     * Returns the index of the first character which will not fit on
     * on a line beginning at {@code start} and possible
     * measuring up to {@code maxAdvance} in graphical width.
     *
     * @param start the character index at which to start measuring.
     *  {@code start} is an absolute index, not relative to the
     *  start of the paragraph
     * @param maxAdvance the graphical width in which the line must fit
     * @return the index after the last character that will fit
     *  on a line beginning at {@code start}, which is not longer
     *  than {@code maxAdvance} in graphical width
     * @throws IllegalArgumentException if {@code start} is
     *          less than the beginning of the paragraph.
     */
    public int getLineBreakIndex(int start, float maxAdvance) {

        int localStart = start - fStart;

        if (!haveLayoutWindow ||
                localStart < fComponentStart ||
                localStart >= fComponentLimit) {
            makeLayoutWindow(localStart);
        }

        return calcLineBreak(localStart, maxAdvance) + fStart;
    }

    /**
     * Returns the graphical width of a line beginning at {@code start}
     * and including characters up to {@code limit}.
     * {@code start} and {@code limit} are absolute indices,
     * not relative to the start of the paragraph.
     *
     * @param start the character index at which to start measuring
     * @param limit the character index at which to stop measuring
     * @return the graphical width of a line beginning at {@code start}
     *   and including characters up to {@code limit}
     * @throws IndexOutOfBoundsException if {@code limit} is less
     *         than {@code start}
     * @throws IllegalArgumentException if {@code start} or
     *          {@code limit} is not between the beginning of
     *          the paragraph and the end of the paragraph.
     */
    public float getAdvanceBetween(int start, int limit) {

        int localStart = start - fStart;
        int localLimit = limit - fStart;

        ensureComponents(localStart, localLimit);
        TextLine line = makeTextLineOnRange(localStart, localLimit);
        return line.getMetrics().advance;
        // could cache line in case getLayout is called with same start, limit
    }

    /**
     * Returns a {@code TextLayout} on the given character range.
     *
     * @param start the index of the first character
     * @param limit the index after the last character.  Must be greater
     *   than {@code start}
     * @return a {@code TextLayout} for the characters beginning at
     *  {@code start} up to (but not including) {@code limit}
     * @throws IndexOutOfBoundsException if {@code limit} is less
     *         than {@code start}
     * @throws IllegalArgumentException if {@code start} or
     *          {@code limit} is not between the beginning of
     *          the paragraph and the end of the paragraph.
     */
    public TextLayout getLayout(int start, int limit) {

        int localStart = start - fStart;
        int localLimit = limit - fStart;

        ensureComponents(localStart, localLimit);
        TextLine textLine = makeTextLineOnRange(localStart, localLimit);

        if (localLimit < fChars.length) {
            layoutCharCount += limit-start;
            layoutCount++;
        }

        return new TextLayout(textLine,
                              fBaseline,
                              fBaselineOffsets,
                              fJustifyRatio);
    }

    private int formattedChars = 0;
    private static boolean wantStats = false;/*"true".equals(System.getProperty("collectStats"));*/
    private boolean collectStats = false;

    private void printStats() {
        System.out.println("formattedChars: " + formattedChars);
        //formattedChars = 0;
        collectStats = false;
    }

    /**
     * Updates the {@code TextMeasurer} after a single character has
     * been inserted
     * into the paragraph currently represented by this
     * {@code TextMeasurer}.  After this call, this
     * {@code TextMeasurer} is equivalent to a new
     * {@code TextMeasurer} created from the text;  however, it will
     * usually be more efficient to update an existing
     * {@code TextMeasurer} than to create a new one from scratch.
     *
     * @param newParagraph the text of the paragraph after performing
     * the insertion.  Cannot be null.
     * @param insertPos the position in the text where the character was
     * inserted.  Must not be less than the start of
     * {@code newParagraph}, and must be less than the end of
     * {@code newParagraph}.
     * @throws IndexOutOfBoundsException if {@code insertPos} is less
     *         than the start of {@code newParagraph} or greater than
     *         or equal to the end of {@code newParagraph}
     * @throws NullPointerException if {@code newParagraph} is
     *         {@code null}
     */
    public void insertChar(AttributedCharacterIterator newParagraph, int insertPos) {

        if (collectStats) {
            printStats();
        }
        if (wantStats) {
            collectStats = true;
        }

        fStart = newParagraph.getBeginIndex();
        int end = newParagraph.getEndIndex();
        if (end - fStart != fChars.length+1) {
            initAll(newParagraph);
        }

        char[] newChars = new char[end-fStart];
        int newCharIndex = insertPos - fStart;
        System.arraycopy(fChars, 0, newChars, 0, newCharIndex);

        char newChar = newParagraph.setIndex(insertPos);
        newChars[newCharIndex] = newChar;
        System.arraycopy(fChars,
                         newCharIndex,
                         newChars,
                         newCharIndex+1,
                         end-insertPos-1);
        fChars = newChars;

        if (fBidi != null || Bidi.requiresBidi(newChars, newCharIndex, newCharIndex + 1) ||
                newParagraph.getAttribute(TextAttribute.BIDI_EMBEDDING) != null) {

            fBidi = new Bidi(newParagraph);
            if (fBidi.isLeftToRight()) {
                fBidi = null;
            }
        }

        fParagraph = StyledParagraph.insertChar(newParagraph,
                                                fChars,
                                                insertPos,
                                                fParagraph);
        invalidateComponents();
    }

    /**
     * Updates the {@code TextMeasurer} after a single character has
     * been deleted
     * from the paragraph currently represented by this
     * {@code TextMeasurer}.  After this call, this
     * {@code TextMeasurer} is equivalent to a new {@code TextMeasurer}
     * created from the text;  however, it will usually be more efficient
     * to update an existing {@code TextMeasurer} than to create a new one
     * from scratch.
     *
     * @param newParagraph the text of the paragraph after performing
     * the deletion.  Cannot be null.
     * @param deletePos the position in the text where the character was removed.
     * Must not be less than
     * the start of {@code newParagraph}, and must not be greater than the
     * end of {@code newParagraph}.
     * @throws IndexOutOfBoundsException if {@code deletePos} is
     *         less than the start of {@code newParagraph} or greater
     *         than the end of {@code newParagraph}
     * @throws NullPointerException if {@code newParagraph} is
     *         {@code null}
     */
    public void deleteChar(AttributedCharacterIterator newParagraph, int deletePos) {

        fStart = newParagraph.getBeginIndex();
        int end = newParagraph.getEndIndex();
        if (end - fStart != fChars.length-1) {
            initAll(newParagraph);
        }

        char[] newChars = new char[end-fStart];
        int changedIndex = deletePos-fStart;

        System.arraycopy(fChars, 0, newChars, 0, deletePos-fStart);
        System.arraycopy(fChars, changedIndex+1, newChars, changedIndex, end-deletePos);
        fChars = newChars;

        if (fBidi != null) {
            fBidi = new Bidi(newParagraph);
            if (fBidi.isLeftToRight()) {
                fBidi = null;
            }
        }

        fParagraph = StyledParagraph.deleteChar(newParagraph,
                                                fChars,
                                                deletePos,
                                                fParagraph);
        invalidateComponents();
    }

    /**
     * NOTE:  This method is only for LineBreakMeasurer's use.  It is package-
     * private because it returns internal data.
     */
    char[] getChars() {

        return fChars;
    }
}
