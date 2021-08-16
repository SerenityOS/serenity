/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.nio.ByteBuffer;
import java.util.Locale;

public class TrueTypeGlyphMapper extends CharToGlyphMapper {

    static final char REVERSE_SOLIDUS = 0x005c; // the backslash char.
    static final char JA_YEN = 0x00a5;

    /* if running on Solaris and default Locale is ja_JP then
     * we map need to remap reverse solidus (backslash) to Yen as
     * apparently expected there.
     */
    static final boolean isJAlocale = Locale.JAPAN.equals(Locale.getDefault());

    TrueTypeFont font;
    CMap cmap;
    int numGlyphs;

    public TrueTypeGlyphMapper(TrueTypeFont font) {
        this.font = font;
        try {
            cmap = CMap.initialize(font);
        } catch (Exception e) {
            cmap = null;
        }
        if (cmap == null) {
            handleBadCMAP();
        }
        missingGlyph = 0; /* standard for TrueType fonts */
        ByteBuffer buffer = font.getTableBuffer(TrueTypeFont.maxpTag);
        if (buffer != null && buffer.capacity() >= 6) {
            numGlyphs = buffer.getChar(4); // offset 4 bytes in MAXP table.
        } else {
            handleBadCMAP();
        }
    }

    public int getNumGlyphs() {
        return numGlyphs;
    }

    private char getGlyphFromCMAP(int charCode) {
        try {
            char glyphCode = cmap.getGlyph(charCode);
            if (glyphCode < numGlyphs ||
                glyphCode >= FileFontStrike.INVISIBLE_GLYPHS) {
                return glyphCode;
            } else {
                if (FontUtilities.isLogging()) {
                    FontUtilities.logWarning(font + " out of range glyph id=" +
                             Integer.toHexString((int)glyphCode) +
                             " for char " + Integer.toHexString(charCode));
                }
                return (char)missingGlyph;
            }
        } catch(Exception e) {
            handleBadCMAP();
            return (char) missingGlyph;
        }
    }

    private char getGlyphFromCMAP(int charCode, int variationSelector) {
        if (variationSelector == 0) {
            return getGlyphFromCMAP(charCode);
        }
        try {
            char glyphCode = cmap.getVariationGlyph(charCode,
                                                    variationSelector);
            if (glyphCode < numGlyphs ||
                glyphCode >= FileFontStrike.INVISIBLE_GLYPHS) {
                return glyphCode;
            } else {
                if (FontUtilities.isLogging()) {
                    FontUtilities.logWarning(font + " out of range glyph id=" +
                         Integer.toHexString((int)glyphCode) +
                         " for char " + Integer.toHexString(charCode) +
                         " for vs " + Integer.toHexString(variationSelector));
                }
                return (char)missingGlyph;
            }
        } catch (Exception e) {
             handleBadCMAP();
             return (char) missingGlyph;
        }
    }

    private void handleBadCMAP() {
        if (FontUtilities.isLogging()) {
            FontUtilities.logSevere("Null Cmap for " + font +
                                    "substituting for this font");
        }

        SunFontManager.getInstance().deRegisterBadFont(font);
        /* The next line is not really a solution, but might
         * reduce the exceptions until references to this font2D
         * are gone.
         */
        cmap = CMap.theNullCmap;
    }

    private char remapJAChar(char unicode) {
        return (unicode == REVERSE_SOLIDUS) ? JA_YEN : unicode;
    }

    private int remapJAIntChar(int unicode) {
        return (unicode == REVERSE_SOLIDUS) ? JA_YEN : unicode;
    }

    public int charToGlyph(char unicode) {
        int glyph = getGlyphFromCMAP(unicode);
        return glyph;
    }

    public int charToGlyph(int unicode) {
        int glyph = getGlyphFromCMAP(unicode);
        return glyph;
    }

    @Override
    public int charToVariationGlyph(int unicode, int variationSelector) {
        int glyph = getGlyphFromCMAP(unicode, variationSelector);
        return glyph;
    }

    public void charsToGlyphs(int count, int[] unicodes, int[] glyphs) {
        for (int i=0;i<count;i++) {
            glyphs[i] = getGlyphFromCMAP(unicodes[i]);
        }
    }

    public void charsToGlyphs(int count, char[] unicodes, int[] glyphs) {

        for (int i=0; i<count; i++) {
            int code = unicodes[i]; // char is unsigned.

            if (code >= HI_SURROGATE_START &&
                code <= HI_SURROGATE_END && i < count - 1) {
                char low = unicodes[i + 1];

                if (low >= LO_SURROGATE_START &&
                    low <= LO_SURROGATE_END) {
                    code = (code - HI_SURROGATE_START) *
                        0x400 + low - LO_SURROGATE_START + 0x10000;

                    glyphs[i] = getGlyphFromCMAP(code);
                    i += 1; // Empty glyph slot after surrogate
                    glyphs[i] = INVISIBLE_GLYPH_ID;
                    continue;
                }
            }
            glyphs[i] = getGlyphFromCMAP(code);

        }
    }

    /* This variant checks if shaping is needed and immediately
     * returns true if it does. A caller of this method should be expecting
     * to check the return type because it needs to know how to handle
     * the character data for display.
     */
    public boolean charsToGlyphsNS(int count, char[] unicodes, int[] glyphs) {

        for (int i=0; i<count; i++) {
            int code = unicodes[i]; // char is unsigned.

            if (code >= HI_SURROGATE_START &&
                code <= HI_SURROGATE_END && i < count - 1) {
                char low = unicodes[i + 1];

                if (low >= LO_SURROGATE_START &&
                    low <= LO_SURROGATE_END) {
                    code = (code - HI_SURROGATE_START) *
                        0x400 + low - LO_SURROGATE_START + 0x10000;
                    glyphs[i + 1] = INVISIBLE_GLYPH_ID;
                }
            }

            glyphs[i] = getGlyphFromCMAP(code);

            if (code < FontUtilities.MIN_LAYOUT_CHARCODE) {
                continue;
            }
            else if (FontUtilities.isComplexCharCode(code) ||
                     CharToGlyphMapper.isVariationSelector(code)) {
                return true;
            }
            else if (code >= 0x10000) {
                i += 1; // Empty glyph slot after surrogate
                continue;
            }
        }

        return false;
    }

    /* A pretty good heuristic is that the cmap we are using
     * supports 32 bit character codes.
     */
    boolean hasSupplementaryChars() {
        return
            cmap instanceof CMap.CMapFormat8 ||
            cmap instanceof CMap.CMapFormat10 ||
            cmap instanceof CMap.CMapFormat12;
    }
}
