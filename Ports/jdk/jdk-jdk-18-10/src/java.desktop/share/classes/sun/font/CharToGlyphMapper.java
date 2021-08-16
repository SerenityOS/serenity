/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

/*
 * NB the versions that take a char as an int are used by the opentype
 * layout engine. If that remains in native these methods may not be
 * needed in the Java class.
 */
public abstract class CharToGlyphMapper {

    public static final int HI_SURROGATE_START = 0xD800;
    public static final int HI_SURROGATE_END = 0xDBFF;
    public static final int LO_SURROGATE_START = 0xDC00;
    public static final int LO_SURROGATE_END = 0xDFFF;
    public static final int VS_START = 0xFE00;
    public static final int VS_END = 0xFE0F;
    public static final int VSS_START = 0xE0100;
    public static final int VSS_END = 0xE01FF;

    public static final int UNINITIALIZED_GLYPH = -1;
    public static final int INVISIBLE_GLYPH_ID = 0xffff;
    public static final int INVISIBLE_GLYPHS   = 0xfffe; // and above

    protected int missingGlyph = CharToGlyphMapper.UNINITIALIZED_GLYPH;

    public int getMissingGlyphCode() {
        return missingGlyph;
    }

    /* Default implementations of these methods may be overridden by
     * subclasses which have special requirements or optimisations
     */

    public boolean canDisplay(char ch) {
        int glyph = charToGlyph(ch);
        return glyph != missingGlyph;
    }

    public boolean canDisplay(int cp) {
        int glyph = charToGlyph(cp);
        return glyph != missingGlyph;
    }

    public int charToGlyph(char unicode) {
        char[] chars = new char[1];
        int[] glyphs = new int[1];
        chars[0] = unicode;
        charsToGlyphs(1, chars, glyphs);
        return glyphs[0];
    }

    public int charToGlyph(int unicode) {
        int[] chars = new int[1];
        int [] glyphs = new int[1];
        chars[0] = unicode;
        charsToGlyphs(1, chars, glyphs);
        return glyphs[0];
    }

    public int charToVariationGlyph(int unicode, int variationSelector) {
        // Override this if variation selector is supported.
        return charToGlyph(unicode);
    }

    public abstract int getNumGlyphs();

    public abstract void charsToGlyphs(int count,
                                       char[] unicodes, int[] glyphs);

    public abstract boolean charsToGlyphsNS(int count,
                                            char[] unicodes, int[] glyphs);

    public abstract void charsToGlyphs(int count,
                                       int[] unicodes, int[] glyphs);

    public static boolean isVariationSelector(int charCode) {
        return ((charCode >= VSS_START && charCode <= VSS_END) ||
                (charCode >= VS_START && charCode <= VS_END));
    }

}
