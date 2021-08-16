/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class maps an individual character to a Font family which can
 * display it.  The character-to-Font mapping does not depend on the
 * character's context, so a particular character will be mapped to the
 * same font family each time.
 * <p>
 * Typically, clients will call getIndexFor(char) for each character
 * in a style run.  When getIndexFor() returns a different value from
 * ones seen previously, the characters up to that point will be assigned
 * a font obtained from getFont().
 */
public final class FontResolver {

    // An array of all fonts available to the runtime.  The fonts
    // will be searched in order.
    private Font[] allFonts;
    private Font[] supplementaryFonts;
    private int[]  supplementaryIndices;

    // Default size of Fonts (if created from an empty Map, for instance).
    private static final int DEFAULT_SIZE = 12; // from Font

    private Font defaultFont = new Font(Font.DIALOG, Font.PLAIN, DEFAULT_SIZE);

    // The results of previous lookups are cached in a two-level
    // table.  The value for a character c is found in:
    //     blocks[c>>SHIFT][c&MASK]
    // although the second array is only allocated when needed.
    // A 0 value means the character's font has not been looked up.
    // A positive value means the character's font is in the allFonts
    // array at index (value-1).
    private static final int SHIFT = 9;
    private static final int BLOCKSIZE = 1<<(16-SHIFT);
    private static final int MASK = BLOCKSIZE-1;
    private int[][] blocks = new int[1<<SHIFT][];

    private FontResolver() {
    }

    private Font[] getAllFonts() {
        if (allFonts == null) {
            allFonts =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            for (int i=0; i < allFonts.length; i++) {
                allFonts[i] = allFonts[i].deriveFont((float)DEFAULT_SIZE);
            }
        }
        return allFonts;
    }

    /**
     * Search fonts in order, and return "1" to indicate its in the default
     * font, (or not found at all),  or the index of the first font
     * which can display the given character, plus 2, if it is not
     * in the default font.
     */
    private int getIndexFor(char c) {

        if (defaultFont.canDisplay(c)) {
            return 1;
        }
        for (int i=0; i < getAllFonts().length; i++) {
            if (allFonts[i].canDisplay(c)) {
                return i+2;
            }
        }
        return 1;
    }

    private Font [] getAllSCFonts() {

        if (supplementaryFonts == null) {
            ArrayList<Font> fonts = new ArrayList<Font>();
            ArrayList<Integer> indices = new ArrayList<Integer>();

            for (int i=0; i<getAllFonts().length; i++) {
                Font font = allFonts[i];
                Font2D font2D = FontUtilities.getFont2D(font);
                if (font2D.hasSupplementaryChars()) {
                    fonts.add(font);
                    indices.add(Integer.valueOf(i));
                }
            }

            int len = fonts.size();
            supplementaryIndices = new int[len];
            for (int i=0; i<len; i++) {
                supplementaryIndices[i] = indices.get(i);
            }
            supplementaryFonts = fonts.toArray(new Font[len]);
        }
        return supplementaryFonts;
    }

    /* This method is called only for character codes >= 0x10000 - which
     * are assumed to be legal supplementary characters.
     * It looks first at the default font (to avoid calling getAllFonts if at
     * all possible) and if that doesn't map the code point, it scans
     * just the fonts that may contain supplementary characters.
     * The index that is returned is into the "allFonts" array so that
     * callers see the same value for both supplementary and base chars.
     */
    private int getIndexFor(int cp) {

        if (defaultFont.canDisplay(cp)) {
            return 1;
        }

        for (int i = 0; i < getAllSCFonts().length; i++) {
            if (supplementaryFonts[i].canDisplay(cp)) {
                return supplementaryIndices[i]+2;
            }
        }
        return 1;
    }

    /**
     * Return an index for the given character.  The index identifies a
     * font family to getFont(), and has no other inherent meaning.
     * @param c the character to map
     * @return a value for consumption by getFont()
     * @see #getFont
     */
    public int getFontIndex(char c) {

        int blockIndex = c>>SHIFT;
        int[] block = blocks[blockIndex];
        if (block == null) {
            block = new int[BLOCKSIZE];
            blocks[blockIndex] = block;
        }

        int index = c & MASK;
        if (block[index] == 0) {
            block[index] = getIndexFor(c);
        }
        return block[index];
    }

    public int getFontIndex(int cp) {
        if (cp < 0x10000) {
            return getFontIndex((char)cp);
        }
        return getIndexFor(cp);
    }

    /**
     * Determines the font index for the code point at the current position in the
     * iterator, then advances the iterator to the first code point that has
     * a different index or until the iterator is DONE, and returns the font index.
     * @param iter a code point iterator, this will be advanced past any code
     *             points that have the same font index
     * @return the font index for the initial code point found, or 1 if the iterator
     * was empty.
     */
    public int nextFontRunIndex(CodePointIterator iter) {
        int cp = iter.next();
        int fontIndex = 1;
        if (cp != CodePointIterator.DONE) {
            fontIndex = getFontIndex(cp);

            while ((cp = iter.next()) != CodePointIterator.DONE) {
                if (getFontIndex(cp) != fontIndex) {
                    iter.prev();
                    break;
                }
            }
        }
        return fontIndex;
    }

    /**
     * Return a Font from a given font index with properties
     * from attributes.  The font index, which should have been produced
     * by getFontIndex(), determines a font family.  The size and style
     * of the Font reflect the properties in attributes.  Any Font or
     * font family specifications in attributes are ignored, on the
     * assumption that clients have already handled them.
     * @param index an index from getFontIndex() which determines the
     *        font family
     * @param attributes a Map from which the size and style of the Font
     *        are determined.  The default size is 12 and the default style
     *        is Font.PLAIN
     * @see #getFontIndex
     */
    public Font getFont(int index,
                        Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        Font font = defaultFont;

        if (index >= 2) {
            font = allFonts[index-2];
        }

        return font.deriveFont(attributes);
    }

    private static FontResolver INSTANCE;

    /**
     * Return a shared instance of FontResolver.
     */
    public static FontResolver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FontResolver();
        }
        return INSTANCE;
    }
}
