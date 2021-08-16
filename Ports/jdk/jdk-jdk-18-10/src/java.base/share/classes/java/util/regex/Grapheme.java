/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.util.regex;

import java.util.Objects;

final class Grapheme {

    /**
     * Look for the next extended grapheme cluster boundary in a CharSequence.
     * It assumes the start of the char sequence at offset {@code off} is a boundary.
     * <p>
     * See Unicode Standard Annex #29 Unicode Text Segmentation for the specification
     * for the extended grapheme cluster boundary rules. The following implementation
     * is based on version 12.0 of the annex.
     * (http://www.unicode.org/reports/tr29/tr29-35.html)
     *
     * @param src the {@code CharSequence} to be scanned
     * @param off offset to start looking for the next boundary in the src
     * @param limit limit offset in the src (exclusive)
     * @return the next grapheme boundary
     */
    static int nextBoundary(CharSequence src, int off, int limit) {
        Objects.checkFromToIndex(off, limit, src.length());

        int ch0 = Character.codePointAt(src, off);
        int ret = off + Character.charCount(ch0);
        // indicates whether gb11 or gb12 is underway
        int t0 = getType(ch0);
        int riCount = t0 == RI ? 1 : 0;
        boolean gb11 = t0 == EXTENDED_PICTOGRAPHIC;
        while (ret < limit) {
            int ch1 = Character.codePointAt(src, ret);
            int t1 = getType(ch1);

            if (gb11 && t0 == ZWJ && t1 == EXTENDED_PICTOGRAPHIC) {
                // continue for gb11
            } else if (riCount % 2 == 1 && t0 == RI && t1 == RI) {
                // continue for gb12
            } else if (rules[t0][t1]) {
                if (ret > off) {
                    break;
                } else {
                    gb11 = t1 == EXTENDED_PICTOGRAPHIC;
                    riCount = 0;
                }
            }

            riCount += (t1 == RI) ? 1 : 0;
            t0 = t1;

            ret += Character.charCount(ch1);
        }
        return ret;
    }

    // types
    private static final int OTHER = 0;
    private static final int CR = 1;
    private static final int LF = 2;
    private static final int CONTROL = 3;
    private static final int EXTEND = 4;
    private static final int ZWJ = 5;
    private static final int RI = 6;
    private static final int PREPEND = 7;
    private static final int SPACINGMARK = 8;
    private static final int L = 9;
    private static final int V = 10;
    private static final int T = 11;
    private static final int LV = 12;
    private static final int LVT = 13;
    private static final int EXTENDED_PICTOGRAPHIC = 14;

    private static final int FIRST_TYPE = 0;
    private static final int LAST_TYPE = 14;

    private static boolean[][] rules;
    static {
        rules = new boolean[LAST_TYPE + 1][LAST_TYPE + 1];
        // GB 999 Any + Any  -> default
        for (int i = FIRST_TYPE; i <= LAST_TYPE; i++)
            for (int j = FIRST_TYPE; j <= LAST_TYPE; j++)
                rules[i][j] = true;
        // GB 6 L x (L | V | LV | VT)
        rules[L][L] = false;
        rules[L][V] = false;
        rules[L][LV] = false;
        rules[L][LVT] = false;
        // GB 7 (LV | V) x (V | T)
        rules[LV][V] = false;
        rules[LV][T] = false;
        rules[V][V] = false;
        rules[V][T] = false;
        // GB 8 (LVT | T) x T
        rules[LVT][T] = false;
        rules[T][T] = false;
        // GB 9 x (Extend|ZWJ)
        // GB 9a x Spacing Mark
        // GB 9b Prepend x
        for (int i = FIRST_TYPE; i <= LAST_TYPE; i++) {
            rules[i][EXTEND] = false;
            rules[i][ZWJ] = false;
            rules[i][SPACINGMARK] = false;
            rules[PREPEND][i] = false;
        }
        // GB 4  (Control | CR | LF) +
        // GB 5  + (Control | CR | LF)
        for (int i = FIRST_TYPE; i <= LAST_TYPE; i++)
            for (int j = CR; j <= CONTROL; j++) {
                rules[i][j] = true;
                rules[j][i] = true;
            }
        // GB 3 CR x LF
        rules[CR][LF] = false;
        // GB 11 Exended_Pictographic x (Extend|ZWJ)
        rules[EXTENDED_PICTOGRAPHIC][EXTEND] = false;
        rules[EXTENDED_PICTOGRAPHIC][ZWJ] = false;
    }

    // Hangul syllables
    private static final int SYLLABLE_BASE = 0xAC00;
    private static final int LCOUNT = 19;
    private static final int VCOUNT = 21;
    private static final int TCOUNT = 28;
    private static final int NCOUNT = VCOUNT * TCOUNT; // 588
    private static final int SCOUNT = LCOUNT * NCOUNT; // 11172

    // #tr29: SpacingMark exceptions: The following (which have
    // General_Category = Spacing_Mark and would otherwise be included)
    // are specifically excluded
    private static boolean isExcludedSpacingMark(int cp) {
       return  cp == 0x102B || cp == 0x102C || cp == 0x1038 ||
               cp >= 0x1062 && cp <= 0x1064 ||
               cp >= 0x1062 && cp <= 0x106D ||
               cp == 0x1083 ||
               cp >= 0x1087 && cp <= 0x108C ||
               cp == 0x108F ||
               cp >= 0x109A && cp <= 0x109C ||
               cp == 0x1A61 || cp == 0x1A63 || cp == 0x1A64 ||
               cp == 0xAA7B || cp == 0xAA7D;
    }

    @SuppressWarnings("fallthrough")
    private static int getType(int cp) {
        if (cp < 0x007F) { // ASCII
            if (cp < 32) { // Control characters
                if (cp == 0x000D)
                    return CR;
                if (cp == 0x000A)
                    return LF;
                return CONTROL;
            }
            return OTHER;
        }

        if (EmojiData.isExtendedPictographic(cp)) {
            return EXTENDED_PICTOGRAPHIC;
        }

        int type = Character.getType(cp);
        switch(type) {
        case Character.UNASSIGNED:
            // NOTE: #tr29 lists "Unassigned and Default_Ignorable_Code_Point" as Control
            // but GraphemeBreakTest.txt lists u+0378/reserved-0378 as "Other"
            // so type it as "Other" to make the test happy
            if (cp == 0x0378)
                return OTHER;

        case Character.CONTROL:
        case Character.LINE_SEPARATOR:
        case Character.PARAGRAPH_SEPARATOR:
        case Character.SURROGATE:
            return CONTROL;
        case Character.FORMAT:
            if (cp == 0x200C ||
                cp >= 0xE0020 && cp <= 0xE007F)
                return EXTEND;
            if (cp == 0x200D)
                return ZWJ;
            if (cp >= 0x0600 && cp <= 0x0605 ||
                cp == 0x06DD || cp == 0x070F || cp == 0x08E2 ||
                cp == 0x110BD || cp == 0x110CD)
                return PREPEND;
            return CONTROL;
        case Character.NON_SPACING_MARK:
        case Character.ENCLOSING_MARK:
            // NOTE:
            // #tr29 "plus a few General_Category = Spacing_Mark needed for
            // canonical equivalence."
            // but for "extended grapheme clusters" support, there is no
            // need actually to diff "extend" and "spackmark" given GB9, GB9a
            return EXTEND;
        case  Character.COMBINING_SPACING_MARK:
            if (isExcludedSpacingMark(cp))
                return OTHER;
            // NOTE:
            // 0x11720 and 0x11721 are mentioned in #tr29 as
            // OTHER_LETTER but it appears their category has been updated to
            // COMBING_SPACING_MARK already (verified in ver.8)
            return SPACINGMARK;
        case Character.OTHER_SYMBOL:
            if (cp >= 0x1F1E6 && cp <= 0x1F1FF)
                return RI;
            return OTHER;
        case Character.MODIFIER_LETTER:
        case Character.MODIFIER_SYMBOL:
            // WARNING:
            // not mentioned in #tr29 but listed in GraphemeBreakProperty.txt
            if (cp == 0xFF9E || cp == 0xFF9F ||
                cp >= 0x1F3FB && cp <= 0x1F3FF)
                return EXTEND;
            return OTHER;
        case Character.OTHER_LETTER:
            if (cp == 0x0E33 || cp == 0x0EB3)
                return SPACINGMARK;
            // hangul jamo
            if (cp >= 0x1100 && cp <= 0x11FF) {
                if (cp <= 0x115F)
                    return L;
                if (cp <= 0x11A7)
                    return V;
                return T;
            }
            // hangul syllables
            int sindex = cp - SYLLABLE_BASE;
            if (sindex >= 0 && sindex < SCOUNT) {

                if (sindex % TCOUNT == 0)
                    return LV;
                return LVT;
            }
            //  hangul jamo_extended A
            if (cp >= 0xA960 && cp <= 0xA97C)
                return L;
            //  hangul jamo_extended B
            if (cp >= 0xD7B0 && cp <= 0xD7C6)
                return V;
            if (cp >= 0xD7CB && cp <= 0xD7FB)
                return T;

            // Prepend
            switch (cp) {
                case 0x0D4E:
                case 0x111C2:
                case 0x111C3:
                case 0x1193F:
                case 0x11941:
                case 0x11A3A:
                case 0x11A84:
                case 0x11A85:
                case 0x11A86:
                case 0x11A87:
                case 0x11A88:
                case 0x11A89:
                case 0x11D46:
                    return PREPEND;
            }
        }
        return OTHER;
    }
}
