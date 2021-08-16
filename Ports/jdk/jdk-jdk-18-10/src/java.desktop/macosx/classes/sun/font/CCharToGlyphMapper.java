/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;

public class CCharToGlyphMapper extends CharToGlyphMapper {
    private static native int countGlyphs(final long nativeFontPtr);

    private Cache cache = new Cache();
    CFont fFont;
    int numGlyphs = -1;

    public CCharToGlyphMapper(CFont font) {
        fFont = font;
        missingGlyph = 0; // for getMissingGlyphCode()
    }

    public int getNumGlyphs() {
        if (numGlyphs == -1) {
            numGlyphs = countGlyphs(fFont.getNativeFontPtr());
        }
        return numGlyphs;
    }

    public boolean canDisplay(char ch) {
        int glyph = charToGlyph(ch);
        return glyph != missingGlyph;
    }

    public boolean canDisplay(int cp) {
        int glyph = charToGlyph(cp);
        return glyph != missingGlyph;
    }

    public synchronized boolean charsToGlyphsNS(int count,
                                                char[] unicodes, int[] glyphs)
    {
        charsToGlyphs(count, unicodes, glyphs);

        // The following shaping checks are from either
        // TrueTypeGlyphMapper or Type1GlyphMapper
        for (int i = 0; i < count; i++) {
            int code = unicodes[i];

            if (code >= HI_SURROGATE_START && code <= HI_SURROGATE_END && i < count - 1) {
                char low = unicodes[i + 1];

                if (low >= LO_SURROGATE_START && low <= LO_SURROGATE_END) {
                    code = (code - HI_SURROGATE_START) * 0x400 + low - LO_SURROGATE_START + 0x10000;
                    glyphs[i + 1] = INVISIBLE_GLYPH_ID;
                }
            }

            if (code < FontUtilities.MIN_LAYOUT_CHARCODE) {
                continue;
            } else if (FontUtilities.isComplexCharCode(code)) {
                return true;
            } else if (code >= 0x10000) {
                i += 1; // Empty glyph slot after surrogate
                continue;
            }
        }

        return false;
    }

    public synchronized int charToGlyph(char unicode) {
        final int glyph = cache.get(unicode);
        if (glyph != 0) return glyph;

        final char[] unicodeArray = new char[] { unicode };
        final int[] glyphArray = new int[1];

        nativeCharsToGlyphs(fFont.getNativeFontPtr(), 1, unicodeArray, glyphArray);
        cache.put(unicode, glyphArray[0]);

        return glyphArray[0];
    }

    public synchronized int charToGlyph(int unicode) {
        if (unicode >= 0x10000) {
            int[] glyphs = new int[2];
            char[] surrogates = new char[2];
            int base = unicode - 0x10000;
            surrogates[0] = (char)((base >>> 10) + HI_SURROGATE_START);
            surrogates[1] = (char)((base % 0x400) + LO_SURROGATE_START);
            charsToGlyphs(2, surrogates, glyphs);
            return glyphs[0];
         } else {
             return charToGlyph((char)unicode);
         }
    }

    public synchronized void charsToGlyphs(int count, char[] unicodes, int[] glyphs) {
        cache.get(count, unicodes, glyphs);
    }

    public synchronized void charsToGlyphs(int count, int[] unicodes, int[] glyphs) {
        for (int i = 0; i < count; i++) {
            glyphs[i] = charToGlyph(unicodes[i]);
        };
    }

    // This mapper returns either the glyph code, or if the character can be
    // replaced on-the-fly using CoreText substitution; the negative unicode
    // value. If this "glyph code int" is treated as an opaque code, it will
    // strike and measure exactly as a real glyph code - whether the character
    // is present or not. Missing characters for any font on the system will
    // be returned as 0, as the getMissingGlyphCode() function above indicates.
    private static native void nativeCharsToGlyphs(final long nativeFontPtr,
                                                   int count, char[] unicodes,
                                                   int[] glyphs);

    private class Cache {
        private static final int FIRST_LAYER_SIZE = 256;
        private static final int SECOND_LAYER_SIZE = 16384; // 16384 = 128x128

        private final int[] firstLayerCache = new int[FIRST_LAYER_SIZE];
        private SparseBitShiftingTwoLayerArray secondLayerCache;
        private HashMap<Integer, Integer> generalCache;

        Cache() {
            // <rdar://problem/5331678> need to prevent getting '-1' stuck in the cache
            firstLayerCache[1] = 1;
        }

        public synchronized int get(final int index) {
            if (index < FIRST_LAYER_SIZE) {
                // catch common glyphcodes
                return firstLayerCache[index];
            }

            if (index < SECOND_LAYER_SIZE) {
                // catch common unicodes
                if (secondLayerCache == null) return 0;
                return secondLayerCache.get(index);
            }

            if (generalCache == null) return 0;
            final Integer value = generalCache.get(index);
            if (value == null) return 0;
            return value.intValue();
        }

        public synchronized void put(final int index, final int value) {
            if (index < FIRST_LAYER_SIZE) {
                // catch common glyphcodes
                firstLayerCache[index] = value;
                return;
            }

            if (index < SECOND_LAYER_SIZE) {
                // catch common unicodes
                if (secondLayerCache == null) {
                    secondLayerCache = new SparseBitShiftingTwoLayerArray(SECOND_LAYER_SIZE, 7); // 128x128
                }
                secondLayerCache.put(index, value);
                return;
            }

            if (generalCache == null) {
                generalCache = new HashMap<Integer, Integer>();
            }

            generalCache.put(index, value);
        }

        private class SparseBitShiftingTwoLayerArray {
            final int[][] cache;
            final int shift;
            final int secondLayerLength;

            public SparseBitShiftingTwoLayerArray(final int size,
                                                  final int shift)
            {
                this.shift = shift;
                this.cache = new int[1 << shift][];
                this.secondLayerLength = size >> shift;
            }

            public int get(final int index) {
                final int firstIndex = index >> shift;
                final int[] firstLayerRow = cache[firstIndex];
                if (firstLayerRow == null) return 0;
                return firstLayerRow[index - (firstIndex * (1 << shift))];
            }

            public void put(final int index, final int value) {
                final int firstIndex = index >> shift;
                int[] firstLayerRow = cache[firstIndex];
                if (firstLayerRow == null) {
                    cache[firstIndex] = firstLayerRow = new int[secondLayerLength];
                }
                firstLayerRow[index - (firstIndex * (1 << shift))] = value;
            }
        }

        public synchronized void get(int count, char[] indicies, int[] values)
        {
            // "missed" is the count of 'char' that are not mapped.
            // Surrogates count for 2.
            // unmappedChars is the unique list of these chars.
            // unmappedCharIndices is the location in the original array
            int missed = 0;
            char[] unmappedChars = null;
            int [] unmappedCharIndices = null;

            for (int i = 0; i < count; i++){
                int code = indicies[i];
                if (code >= HI_SURROGATE_START &&
                    code <= HI_SURROGATE_END && i < count - 1)
                {
                    char low = indicies[i + 1];
                    if (low >= LO_SURROGATE_START && low <= LO_SURROGATE_END) {
                        code = (code - HI_SURROGATE_START) * 0x400 +
                            low - LO_SURROGATE_START + 0x10000;
                    }
                }

                final int value = get(code);
                if (value != 0 && value != -1) {
                    values[i] = value;
                    if (code >= 0x10000) {
                        values[i+1] = INVISIBLE_GLYPH_ID;
                        i++;
                    }
                } else {
                    values[i] = 0;
                    put(code, -1);
                    if (unmappedChars == null) {
                        // This is likely to be longer than we need,
                        // but is the simplest and cheapest option.
                        unmappedChars = new char[indicies.length];
                        unmappedCharIndices = new int[indicies.length];
                    }
                    unmappedChars[missed] = indicies[i];
                    unmappedCharIndices[missed] = i;
                    if (code >= 0x10000) { // was a surrogate pair
                        unmappedChars[++missed] = indicies[++i];
                    }
                    missed++;
                }
            }

            if (missed == 0) {
                return;
            }

            final int[] glyphCodes = new int[missed];

            // bulk call to fill in the unmapped code points.
            nativeCharsToGlyphs(fFont.getNativeFontPtr(),
                                missed, unmappedChars, glyphCodes);

            for (int m = 0; m < missed; m++){
                int i = unmappedCharIndices[m];
                int code = unmappedChars[m];
                if (code >= HI_SURROGATE_START &&
                    code <= HI_SURROGATE_END && m < missed - 1)
                {
                    char low = unmappedChars[m + 1];
                    if (low >= LO_SURROGATE_START && low <= LO_SURROGATE_END) {
                        code = (code - HI_SURROGATE_START) * 0x400 +
                            low - LO_SURROGATE_START + 0x10000;
                    }
                }
               values[i] = glyphCodes[m];
               put(code, values[i]);
               if (code >= 0x10000) {
                   m++;
                   values[i + 1] = INVISIBLE_GLYPH_ID;
                }
            }
        }
    }
}
