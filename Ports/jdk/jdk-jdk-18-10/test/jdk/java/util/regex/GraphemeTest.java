/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7071819 8221431 8239383
 * @summary tests Unicode Extended Grapheme support
 * @library /lib/testlibrary/java/lang
 * @run main GraphemeTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GraphemeTest {

    public static void main(String[] args) throws Throwable {
        testProps(UCDFiles.GRAPHEME_BREAK_PROPERTY);
        testProps(UCDFiles.EMOJI_DATA);
    }

    private static void testProps(Path path) throws IOException {
        Files.lines(path)
            .map( ln -> ln.replaceFirst("#.*", "") )
            .filter( ln -> ln.length() != 0 )
            .forEach(ln -> {
                    String[] strs = ln.split("\\s+");
                    int off = strs[0].indexOf("..");
                    int cp0, cp1;
                    String expected = strs[2];
                    if (off != -1) {
                        cp0 = Integer.parseInt(strs[0], 0, off, 16);
                        cp1 = Integer.parseInt(strs[0], off + 2, strs[0].length(), 16);
                    } else {
                        cp0 = cp1 = Integer.parseInt(strs[0], 16);
                    }
                    for (int cp = cp0; cp <=  cp1; cp++) {
                        // Ignore Emoji* for now (only interested in Extended_Pictographic)
                        if (expected.startsWith("Emoji")) {
                            continue;
                        }

                        // NOTE:
                        // #tr29 "plus a few General_Category = Spacing_Mark needed for
                        // canonical equivalence."
                        // For "extended grapheme clusters" support, there is no
                        // need actually to diff "extend" and "spackmark" given GB9, GB9a.
                        if (!expected.equals(types[getType(cp)])) {
                            if ("Extend".equals(expected) &&
                                "SpacingMark".equals(types[getType(cp)]))
                                System.out.printf("[%x]  [%s][%d] -> [%s]%n",
                                    cp, expected, Character.getType(cp), types[getType(cp)]);
                            else
                                throw new RuntimeException(String.format(
                                    "cp=[%x], expeced:[%s] result:[%s]%n",
                                    cp, expected, types[getType(cp)]));
                        }
                    }
                });
    }

    private static final String[] types = {
        "Other", "CR", "LF", "Control", "Extend", "ZWJ", "Regional_Indicator",
        "Prepend", "SpacingMark",
        "L", "V", "T", "LV", "LVT",
        "Extended_Pictographic" };

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // from java.util.regex.Grapheme.java
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
        if (isExtendedPictographic(cp)) {
            return EXTENDED_PICTOGRAPHIC;
        }

        int type = Character.getType(cp);
        switch(type) {
        case Character.CONTROL:
            if (cp == 0x000D)
                return CR;
            if (cp == 0x000A)
                return LF;
            return CONTROL;
        case Character.UNASSIGNED:
            // NOTE: #tr29 lists "Unassigned and Default_Ignorable_Code_Point" as Control
            // but GraphemeBreakTest.txt lists u+0378/reserved-0378 as "Other"
            // so type it as "Other" to make the test happy
            if (cp == 0x0378)
                return OTHER;

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

    // from generated java.util.regex.EmojiData.java
    static boolean isExtendedPictographic(int cp) {
        return
                cp == 0x00A9 ||
                cp == 0x00AE ||
                cp == 0x203C ||
                cp == 0x2049 ||
                cp == 0x2122 ||
                cp == 0x2139 ||
               (cp >= 0x2194 && cp <= 0x2199) ||
                cp == 0x21A9 ||
                cp == 0x21AA ||
                cp == 0x231A ||
                cp == 0x231B ||
                cp == 0x2328 ||
                cp == 0x2388 ||
                cp == 0x23CF ||
               (cp >= 0x23E9 && cp <= 0x23F3) ||
               (cp >= 0x23F8 && cp <= 0x23FA) ||
                cp == 0x24C2 ||
                cp == 0x25AA ||
                cp == 0x25AB ||
                cp == 0x25B6 ||
                cp == 0x25C0 ||
               (cp >= 0x25FB && cp <= 0x25FE) ||
               (cp >= 0x2600 && cp <= 0x2605) ||
               (cp >= 0x2607 && cp <= 0x2612) ||
               (cp >= 0x2614 && cp <= 0x2685) ||
               (cp >= 0x2690 && cp <= 0x2705) ||
               (cp >= 0x2708 && cp <= 0x2712) ||
                cp == 0x2714 ||
                cp == 0x2716 ||
                cp == 0x271D ||
                cp == 0x2721 ||
                cp == 0x2728 ||
                cp == 0x2733 ||
                cp == 0x2734 ||
                cp == 0x2744 ||
                cp == 0x2747 ||
                cp == 0x274C ||
                cp == 0x274E ||
               (cp >= 0x2753 && cp <= 0x2755) ||
                cp == 0x2757 ||
               (cp >= 0x2763 && cp <= 0x2767) ||
               (cp >= 0x2795 && cp <= 0x2797) ||
                cp == 0x27A1 ||
                cp == 0x27B0 ||
                cp == 0x27BF ||
                cp == 0x2934 ||
                cp == 0x2935 ||
               (cp >= 0x2B05 && cp <= 0x2B07) ||
                cp == 0x2B1B ||
                cp == 0x2B1C ||
                cp == 0x2B50 ||
                cp == 0x2B55 ||
                cp == 0x3030 ||
                cp == 0x303D ||
                cp == 0x3297 ||
                cp == 0x3299 ||
               (cp >= 0x1F000 && cp <= 0x1F0FF) ||
               (cp >= 0x1F10D && cp <= 0x1F10F) ||
                cp == 0x1F12F ||
               (cp >= 0x1F16C && cp <= 0x1F171) ||
                cp == 0x1F17E ||
                cp == 0x1F17F ||
                cp == 0x1F18E ||
               (cp >= 0x1F191 && cp <= 0x1F19A) ||
               (cp >= 0x1F1AD && cp <= 0x1F1E5) ||
               (cp >= 0x1F201 && cp <= 0x1F20F) ||
                cp == 0x1F21A ||
                cp == 0x1F22F ||
               (cp >= 0x1F232 && cp <= 0x1F23A) ||
               (cp >= 0x1F23C && cp <= 0x1F23F) ||
               (cp >= 0x1F249 && cp <= 0x1F3FA) ||
               (cp >= 0x1F400 && cp <= 0x1F53D) ||
               (cp >= 0x1F546 && cp <= 0x1F64F) ||
               (cp >= 0x1F680 && cp <= 0x1F6FF) ||
               (cp >= 0x1F774 && cp <= 0x1F77F) ||
               (cp >= 0x1F7D5 && cp <= 0x1F7FF) ||
               (cp >= 0x1F80C && cp <= 0x1F80F) ||
               (cp >= 0x1F848 && cp <= 0x1F84F) ||
               (cp >= 0x1F85A && cp <= 0x1F85F) ||
               (cp >= 0x1F888 && cp <= 0x1F88F) ||
               (cp >= 0x1F8AE && cp <= 0x1F8FF) ||
               (cp >= 0x1F90C && cp <= 0x1F93A) ||
               (cp >= 0x1F93C && cp <= 0x1F945) ||
               (cp >= 0x1F947 && cp <= 0x1FAFF) ||
               (cp >= 0x1FC00 && cp <= 0x1FFFD);
    }
}
