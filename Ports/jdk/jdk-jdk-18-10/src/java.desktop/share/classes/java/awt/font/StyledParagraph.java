/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * (C) Copyright IBM Corp. 1999,  All rights reserved.
 */
package java.awt.font;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.im.InputMethodHighlight;
import java.text.Annotation;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;
import sun.font.CodePointIterator;
import sun.font.Decoration;
import sun.font.FontResolver;

/**
 * This class stores Font, GraphicAttribute, and Decoration intervals
 * on a paragraph of styled text.
 * <p>
 * Currently, this class is optimized for a small number of intervals
 * (preferably 1).
 */
final class StyledParagraph {

    // the length of the paragraph
    private int length;

    // If there is a single Decoration for the whole paragraph, it
    // is stored here.  Otherwise this field is ignored.

    private Decoration decoration;

    // If there is a single Font or GraphicAttribute for the whole
    // paragraph, it is stored here.  Otherwise this field is ignored.
    private Object font;

    // If there are multiple Decorations in the paragraph, they are
    // stored in this Vector, in order.  Otherwise this vector and
    // the decorationStarts array are null.
    private Vector<Decoration> decorations;
    // If there are multiple Decorations in the paragraph,
    // decorationStarts[i] contains the index where decoration i
    // starts.  For convenience, there is an extra entry at the
    // end of this array with the length of the paragraph.
    int[] decorationStarts;

    // If there are multiple Fonts/GraphicAttributes in the paragraph,
    // they are
    // stored in this Vector, in order.  Otherwise this vector and
    // the fontStarts array are null.
    private Vector<Object> fonts;
    // If there are multiple Fonts/GraphicAttributes in the paragraph,
    // fontStarts[i] contains the index where decoration i
    // starts.  For convenience, there is an extra entry at the
    // end of this array with the length of the paragraph.
    int[] fontStarts;

    private static int INITIAL_SIZE = 8;

    /**
     * Create a new StyledParagraph over the given styled text.
     * @param aci an iterator over the text
     * @param chars the characters extracted from aci
     */
    public StyledParagraph(AttributedCharacterIterator aci,
                           char[] chars) {

        int start = aci.getBeginIndex();
        int end = aci.getEndIndex();
        length = end - start;

        int index = start;
        aci.first();

        do {
            final int nextRunStart = aci.getRunLimit();
            final int localIndex = index-start;

            Map<? extends Attribute, ?> attributes = aci.getAttributes();
            attributes = addInputMethodAttrs(attributes);
            Decoration d = Decoration.getDecoration(attributes);
            addDecoration(d, localIndex);

            Object f = getGraphicOrFont(attributes);
            if (f == null) {
                addFonts(chars, attributes, localIndex, nextRunStart-start);
            }
            else {
                addFont(f, localIndex);
            }

            aci.setIndex(nextRunStart);
            index = nextRunStart;

        } while (index < end);

        // Add extra entries to starts arrays with the length
        // of the paragraph.  'this' is used as a dummy value
        // in the Vector.
        if (decorations != null) {
            decorationStarts = addToVector(this, length, decorations, decorationStarts);
        }
        if (fonts != null) {
            fontStarts = addToVector(this, length, fonts, fontStarts);
        }
    }

    /**
     * Adjust indices in starts to reflect an insertion after pos.
     * Any index in starts greater than pos will be increased by 1.
     */
    private static void insertInto(int pos, int[] starts, int numStarts) {

        while (starts[--numStarts] > pos) {
            starts[numStarts] += 1;
        }
    }

    /**
     * Return a StyledParagraph reflecting the insertion of a single character
     * into the text.  This method will attempt to reuse the given paragraph,
     * but may create a new paragraph.
     * @param aci an iterator over the text.  The text should be the same as the
     *     text used to create (or most recently update) oldParagraph, with
     *     the exception of inserting a single character at insertPos.
     * @param chars the characters in aci
     * @param insertPos the index of the new character in aci
     * @param oldParagraph a StyledParagraph for the text in aci before the
     *     insertion
     */
    public static StyledParagraph insertChar(AttributedCharacterIterator aci,
                                             char[] chars,
                                             int insertPos,
                                             StyledParagraph oldParagraph) {

        // If the styles at insertPos match those at insertPos-1,
        // oldParagraph will be reused.  Otherwise we create a new
        // paragraph.

        char ch = aci.setIndex(insertPos);
        int relativePos = Math.max(insertPos - aci.getBeginIndex() - 1, 0);

        Map<? extends Attribute, ?> attributes =
            addInputMethodAttrs(aci.getAttributes());
        Decoration d = Decoration.getDecoration(attributes);
        if (!oldParagraph.getDecorationAt(relativePos).equals(d)) {
            return new StyledParagraph(aci, chars);
        }
        Object f = getGraphicOrFont(attributes);
        if (f == null) {
            FontResolver resolver = FontResolver.getInstance();
            int fontIndex = resolver.getFontIndex(ch);
            f = resolver.getFont(fontIndex, attributes);
        }
        if (!oldParagraph.getFontOrGraphicAt(relativePos).equals(f)) {
            return new StyledParagraph(aci, chars);
        }

        // insert into existing paragraph
        oldParagraph.length += 1;
        if (oldParagraph.decorations != null) {
            insertInto(relativePos,
                       oldParagraph.decorationStarts,
                       oldParagraph.decorations.size());
        }
        if (oldParagraph.fonts != null) {
            insertInto(relativePos,
                       oldParagraph.fontStarts,
                       oldParagraph.fonts.size());
        }
        return oldParagraph;
    }

    /**
     * Adjust indices in starts to reflect a deletion after deleteAt.
     * Any index in starts greater than deleteAt will be increased by 1.
     * It is the caller's responsibility to make sure that no 0-length
     * runs result.
     */
    private static void deleteFrom(int deleteAt, int[] starts, int numStarts) {

        while (starts[--numStarts] > deleteAt) {
            starts[numStarts] -= 1;
        }
    }

    /**
     * Return a StyledParagraph reflecting the insertion of a single character
     * into the text.  This method will attempt to reuse the given paragraph,
     * but may create a new paragraph.
     * @param aci an iterator over the text.  The text should be the same as the
     *     text used to create (or most recently update) oldParagraph, with
     *     the exception of deleting a single character at deletePos.
     * @param chars the characters in aci
     * @param deletePos the index where a character was removed
     * @param oldParagraph a StyledParagraph for the text in aci before the
     *     insertion
     */
    public static StyledParagraph deleteChar(AttributedCharacterIterator aci,
                                             char[] chars,
                                             int deletePos,
                                             StyledParagraph oldParagraph) {

        // We will reuse oldParagraph unless there was a length-1 run
        // at deletePos.  We could do more work and check the individual
        // Font and Decoration runs, but we don't right now...
        deletePos -= aci.getBeginIndex();

        if (oldParagraph.decorations == null && oldParagraph.fonts == null) {
            oldParagraph.length -= 1;
            return oldParagraph;
        }

        if (oldParagraph.getRunLimit(deletePos) == deletePos+1) {
            if (deletePos == 0 || oldParagraph.getRunLimit(deletePos-1) == deletePos) {
                return new StyledParagraph(aci, chars);
            }
        }

        oldParagraph.length -= 1;
        if (oldParagraph.decorations != null) {
            deleteFrom(deletePos,
                       oldParagraph.decorationStarts,
                       oldParagraph.decorations.size());
        }
        if (oldParagraph.fonts != null) {
            deleteFrom(deletePos,
                       oldParagraph.fontStarts,
                       oldParagraph.fonts.size());
        }
        return oldParagraph;
    }

    /**
     * Return the index at which there is a different Font, GraphicAttribute, or
     * Decoration than at the given index.
     * @param index a valid index in the paragraph
     * @return the first index where there is a change in attributes from
     *      those at index
     */
    public int getRunLimit(int index) {

        if (index < 0 || index >= length) {
            throw new IllegalArgumentException("index out of range");
        }
        int limit1 = length;
        if (decorations != null) {
            int run = findRunContaining(index, decorationStarts);
            limit1 = decorationStarts[run+1];
        }
        int limit2 = length;
        if (fonts != null) {
            int run = findRunContaining(index, fontStarts);
            limit2 = fontStarts[run+1];
        }
        return Math.min(limit1, limit2);
    }

    /**
     * Return the Decoration in effect at the given index.
     * @param index a valid index in the paragraph
     * @return the Decoration at index.
     */
    public Decoration getDecorationAt(int index) {

        if (index < 0 || index >= length) {
            throw new IllegalArgumentException("index out of range");
        }
        if (decorations == null) {
            return decoration;
        }
        int run = findRunContaining(index, decorationStarts);
        return decorations.elementAt(run);
    }

    /**
     * Return the Font or GraphicAttribute in effect at the given index.
     * The client must test the type of the return value to determine what
     * it is.
     * @param index a valid index in the paragraph
     * @return the Font or GraphicAttribute at index.
     */
    public Object getFontOrGraphicAt(int index) {

        if (index < 0 || index >= length) {
            throw new IllegalArgumentException("index out of range");
        }
        if (fonts == null) {
            return font;
        }
        int run = findRunContaining(index, fontStarts);
        return fonts.elementAt(run);
    }

    /**
     * Return i such that starts[i] &lt;= index &lt; starts[i+1].  starts
     * must be in increasing order, with at least one element greater
     * than index.
     */
    private static int findRunContaining(int index, int[] starts) {

        for (int i=1; true; i++) {
            if (starts[i] > index) {
                return i-1;
            }
        }
    }

    /**
     * Append the given Object to the given Vector.  Add
     * the given index to the given starts array.  If the
     * starts array does not have room for the index, a
     * new array is created and returned.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int[] addToVector(Object obj,
                                     int index,
                                     Vector v,
                                     int[] starts) {

        if (!v.lastElement().equals(obj)) {
            v.addElement(obj);
            int count = v.size();
            if (starts.length == count) {
                int[] temp = new int[starts.length*2];
                System.arraycopy(starts, 0, temp, 0, starts.length);
                starts = temp;
            }
            starts[count-1] = index;
        }
        return starts;
    }

    /**
     * Add a new Decoration run with the given Decoration at the
     * given index.
     */
    private void addDecoration(Decoration d, int index) {

        if (decorations != null) {
            decorationStarts = addToVector(d,
                                           index,
                                           decorations,
                                           decorationStarts);
        }
        else if (decoration == null) {
            decoration = d;
        }
        else {
            if (!decoration.equals(d)) {
                decorations = new Vector<Decoration>(INITIAL_SIZE);
                decorations.addElement(decoration);
                decorations.addElement(d);
                decorationStarts = new int[INITIAL_SIZE];
                decorationStarts[0] = 0;
                decorationStarts[1] = index;
            }
        }
    }

    /**
     * Add a new Font/GraphicAttribute run with the given object at the
     * given index.
     */
    private void addFont(Object f, int index) {

        if (fonts != null) {
            fontStarts = addToVector(f, index, fonts, fontStarts);
        }
        else if (font == null) {
            font = f;
        }
        else {
            if (!font.equals(f)) {
                fonts = new Vector<Object>(INITIAL_SIZE);
                fonts.addElement(font);
                fonts.addElement(f);
                fontStarts = new int[INITIAL_SIZE];
                fontStarts[0] = 0;
                fontStarts[1] = index;
            }
        }
    }

    /**
     * Resolve the given chars into Fonts using FontResolver, then add
     * font runs for each.
     */
    private void addFonts(char[] chars, Map<? extends Attribute, ?> attributes,
                          int start, int limit) {

        FontResolver resolver = FontResolver.getInstance();
        CodePointIterator iter = CodePointIterator.create(chars, start, limit);
        for (int runStart = iter.charIndex(); runStart < limit; runStart = iter.charIndex()) {
            int fontIndex = resolver.nextFontRunIndex(iter);
            addFont(resolver.getFont(fontIndex, attributes), runStart);
        }
    }

    /**
     * Return a Map with entries from oldStyles, as well as input
     * method entries, if any.
     */
    static Map<? extends Attribute, ?>
           addInputMethodAttrs(Map<? extends Attribute, ?> oldStyles) {

        Object value = oldStyles.get(TextAttribute.INPUT_METHOD_HIGHLIGHT);

        try {
            if (value != null) {
                if (value instanceof Annotation) {
                    value = ((Annotation)value).getValue();
                }

                InputMethodHighlight hl;
                hl = (InputMethodHighlight) value;

                Map<? extends Attribute, ?> imStyles = null;
                try {
                    imStyles = hl.getStyle();
                } catch (NoSuchMethodError e) {
                }

                if (imStyles == null) {
                    Toolkit tk = Toolkit.getDefaultToolkit();
                    imStyles = tk.mapInputMethodHighlight(hl);
                }

                if (imStyles != null) {
                    HashMap<Attribute, Object>
                        newStyles = new HashMap<>(5, (float)0.9);
                    newStyles.putAll(oldStyles);

                    newStyles.putAll(imStyles);

                    return newStyles;
                }
            }
        }
        catch(ClassCastException e) {
        }

        return oldStyles;
    }

    /**
     * Extract a GraphicAttribute or Font from the given attributes.
     * If attributes does not contain a GraphicAttribute, Font, or
     * Font family entry this method returns null.
     */
    private static Object getGraphicOrFont(
            Map<? extends Attribute, ?> attributes) {

        Object value = attributes.get(TextAttribute.CHAR_REPLACEMENT);
        if (value != null) {
            return value;
        }
        value = attributes.get(TextAttribute.FONT);
        if (value != null) {
            return value;
        }

        if (attributes.get(TextAttribute.FAMILY) != null) {
            return Font.getFont(attributes);
        }
        else {
            return null;
        }
    }
}
