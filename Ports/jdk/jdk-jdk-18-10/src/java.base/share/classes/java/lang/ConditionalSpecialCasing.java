/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.text.BreakIterator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import sun.text.Normalizer;


/**
 * This is a utility class for {@code String.toLowerCase()} and
 * {@code String.toUpperCase()}, that handles special casing with
 * conditions.  In other words, it handles the mappings with conditions
 * that are defined in
 * <a href="http://www.unicode.org/Public/UNIDATA/SpecialCasing.txt">Special
 * Casing Properties</a> file.
 * <p>
 * Note that the unconditional case mappings (including 1:M mappings)
 * are handled in {@code Character.toLower/UpperCase()}.
 */
final class ConditionalSpecialCasing {

    // context conditions.
    static final int FINAL_CASED =              1;
    static final int AFTER_SOFT_DOTTED =        2;
    static final int MORE_ABOVE =               3;
    static final int AFTER_I =                  4;
    static final int NOT_BEFORE_DOT =           5;

    // combining class definitions
    static final int COMBINING_CLASS_ABOVE = 230;

    // Special case mapping entries
    static Entry[] entry = {
        //# ================================================================================
        //# Conditional mappings
        //# ================================================================================
        new Entry(0x03A3, new char[]{0x03C2}, new char[]{0x03A3}, null, FINAL_CASED), // # GREEK CAPITAL LETTER SIGMA
        new Entry(0x0130, new char[]{0x0069, 0x0307}, new char[]{0x0130}, null, 0), // # LATIN CAPITAL LETTER I WITH DOT ABOVE

        //# ================================================================================
        //# Locale-sensitive mappings
        //# ================================================================================
        //# Lithuanian
        new Entry(0x0307, new char[]{0x0307}, new char[]{}, "lt",  AFTER_SOFT_DOTTED), // # COMBINING DOT ABOVE
        new Entry(0x0049, new char[]{0x0069, 0x0307}, new char[]{0x0049}, "lt", MORE_ABOVE), // # LATIN CAPITAL LETTER I
        new Entry(0x004A, new char[]{0x006A, 0x0307}, new char[]{0x004A}, "lt", MORE_ABOVE), // # LATIN CAPITAL LETTER J
        new Entry(0x012E, new char[]{0x012F, 0x0307}, new char[]{0x012E}, "lt", MORE_ABOVE), // # LATIN CAPITAL LETTER I WITH OGONEK
        new Entry(0x00CC, new char[]{0x0069, 0x0307, 0x0300}, new char[]{0x00CC}, "lt", 0), // # LATIN CAPITAL LETTER I WITH GRAVE
        new Entry(0x00CD, new char[]{0x0069, 0x0307, 0x0301}, new char[]{0x00CD}, "lt", 0), // # LATIN CAPITAL LETTER I WITH ACUTE
        new Entry(0x0128, new char[]{0x0069, 0x0307, 0x0303}, new char[]{0x0128}, "lt", 0), // # LATIN CAPITAL LETTER I WITH TILDE

        //# ================================================================================
        //# Turkish and Azeri
        new Entry(0x0130, new char[]{0x0069}, new char[]{0x0130}, "tr", 0), // # LATIN CAPITAL LETTER I WITH DOT ABOVE
        new Entry(0x0130, new char[]{0x0069}, new char[]{0x0130}, "az", 0), // # LATIN CAPITAL LETTER I WITH DOT ABOVE
        new Entry(0x0307, new char[]{}, new char[]{0x0307}, "tr", AFTER_I), // # COMBINING DOT ABOVE
        new Entry(0x0307, new char[]{}, new char[]{0x0307}, "az", AFTER_I), // # COMBINING DOT ABOVE
        new Entry(0x0049, new char[]{0x0131}, new char[]{0x0049}, "tr", NOT_BEFORE_DOT), // # LATIN CAPITAL LETTER I
        new Entry(0x0049, new char[]{0x0131}, new char[]{0x0049}, "az", NOT_BEFORE_DOT), // # LATIN CAPITAL LETTER I
        new Entry(0x0069, new char[]{0x0069}, new char[]{0x0130}, "tr", 0), // # LATIN SMALL LETTER I
        new Entry(0x0069, new char[]{0x0069}, new char[]{0x0130}, "az", 0)  // # LATIN SMALL LETTER I
    };

    // A hash table that contains the above entries
    static Hashtable<Integer, HashSet<Entry>> entryTable = new Hashtable<>();
    static {
        // create hashtable from the entry
        for (Entry cur : entry) {
            Integer cp = cur.getCodePoint();
            HashSet<Entry> set = entryTable.get(cp);
            if (set == null) {
                set = new HashSet<>();
                entryTable.put(cp, set);
            }
            set.add(cur);
        }
    }

    static int toLowerCaseEx(String src, int index, Locale locale) {
        char[] result = lookUpTable(src, index, locale, true);

        if (result != null) {
            if (result.length == 1) {
                return result[0];
            } else {
                return Character.ERROR;
            }
        } else {
            // default to Character class' one
            return Character.toLowerCase(src.codePointAt(index));
        }
    }

    static int toUpperCaseEx(String src, int index, Locale locale) {
        char[] result = lookUpTable(src, index, locale, false);

        if (result != null) {
            if (result.length == 1) {
                return result[0];
            } else {
                return Character.ERROR;
            }
        } else {
            // default to Character class' one
            return Character.toUpperCaseEx(src.codePointAt(index));
        }
    }

    static char[] toLowerCaseCharArray(String src, int index, Locale locale) {
        return lookUpTable(src, index, locale, true);
    }

    static char[] toUpperCaseCharArray(String src, int index, Locale locale) {
        char[] result = lookUpTable(src, index, locale, false);
        if (result != null) {
            return result;
        } else {
            return Character.toUpperCaseCharArray(src.codePointAt(index));
        }
    }

    private static char[] lookUpTable(String src, int index, Locale locale, boolean bLowerCasing) {
        HashSet<Entry> set = entryTable.get(src.codePointAt(index));
        char[] ret = null;

        if (set != null) {
            Iterator<Entry> iter = set.iterator();
            String currentLang = locale.getLanguage();
            while (iter.hasNext()) {
                Entry entry = iter.next();
                String conditionLang = entry.getLanguage();
                if (((conditionLang == null) || (conditionLang.equals(currentLang))) &&
                        isConditionMet(src, index, locale, entry.getCondition())) {
                    ret = bLowerCasing ? entry.getLowerCase() : entry.getUpperCase();
                    if (conditionLang != null) {
                        break;
                    }
                }
            }
        }

        return ret;
    }

    private static boolean isConditionMet(String src, int index, Locale locale, int condition) {
        return switch (condition) {
            case FINAL_CASED       -> isFinalCased(src, index, locale);
            case AFTER_SOFT_DOTTED -> isAfterSoftDotted(src, index);
            case MORE_ABOVE        -> isMoreAbove(src, index);
            case AFTER_I           -> isAfterI(src, index);
            case NOT_BEFORE_DOT    -> !isBeforeDot(src, index);
            default -> true;
        };
    }

    /**
     * Implements the "Final_Cased" condition
     *
     * Specification: Within the closest word boundaries containing C, there is a cased
     * letter before C, and there is no cased letter after C.
     *
     * Regular Expression:
     *   Before C: [{cased==true}][{wordBoundary!=true}]*
     *   After C: !([{wordBoundary!=true}]*[{cased}])
     */
    private static boolean isFinalCased(String src, int index, Locale locale) {
        BreakIterator wordBoundary = BreakIterator.getWordInstance(locale);
        wordBoundary.setText(src);
        int ch;

        // Look for a preceding 'cased' letter
        for (int i = index; (i >= 0) && !wordBoundary.isBoundary(i);
                i -= Character.charCount(ch)) {

            ch = src.codePointBefore(i);
            if (isCased(ch)) {

                int len = src.length();
                // Check that there is no 'cased' letter after the index
                for (i = index + Character.charCount(src.codePointAt(index));
                        (i < len) && !wordBoundary.isBoundary(i);
                        i += Character.charCount(ch)) {

                    ch = src.codePointAt(i);
                    if (isCased(ch)) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Implements the "After_I" condition
     *
     * Specification: The last preceding base character was an uppercase I,
     * and there is no intervening combining character class 230 (ABOVE).
     *
     * Regular Expression:
     *   Before C: [I]([{cc!=230}&{cc!=0}])*
     */
    private static boolean isAfterI(String src, int index) {
        int ch;
        int cc;

        // Look for the last preceding base character
        for (int i = index; i > 0; i -= Character.charCount(ch)) {

            ch = src.codePointBefore(i);

            if (ch == 'I') {
                return true;
            } else {
                cc = Normalizer.getCombiningClass(ch);
                if ((cc == 0) || (cc == COMBINING_CLASS_ABOVE)) {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Implements the "After_Soft_Dotted" condition
     *
     * Specification: The last preceding character with combining class
     * of zero before C was Soft_Dotted, and there is no intervening
     * combining character class 230 (ABOVE).
     *
     * Regular Expression:
     *   Before C: [{Soft_Dotted==true}]([{cc!=230}&{cc!=0}])*
     */
    private static boolean isAfterSoftDotted(String src, int index) {
        int ch;
        int cc;

        // Look for the last preceding character
        for (int i = index; i > 0; i -= Character.charCount(ch)) {

            ch = src.codePointBefore(i);

            if (isSoftDotted(ch)) {
                return true;
            } else {
                cc = Normalizer.getCombiningClass(ch);
                if ((cc == 0) || (cc == COMBINING_CLASS_ABOVE)) {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Implements the "More_Above" condition
     *
     * Specification: C is followed by one or more characters of combining
     * class 230 (ABOVE) in the combining character sequence.
     *
     * Regular Expression:
     *   After C: [{cc!=0}]*[{cc==230}]
     */
    private static boolean isMoreAbove(String src, int index) {
        int ch;
        int cc;
        int len = src.length();

        // Look for a following ABOVE combining class character
        for (int i = index + Character.charCount(src.codePointAt(index));
                i < len; i += Character.charCount(ch)) {

            ch = src.codePointAt(i);
            cc = Normalizer.getCombiningClass(ch);

            if (cc == COMBINING_CLASS_ABOVE) {
                return true;
            } else if (cc == 0) {
                return false;
            }
        }

        return false;
    }

    /**
     * Implements the "Before_Dot" condition
     *
     * Specification: C is followed by {@code U+0307 COMBINING DOT ABOVE}.
     * Any sequence of characters with a combining class that is
     * neither 0 nor 230 may intervene between the current character
     * and the combining dot above.
     *
     * Regular Expression:
     *   After C: ([{cc!=230}&{cc!=0}])*[\u0307]
     */
    private static boolean isBeforeDot(String src, int index) {
        int ch;
        int cc;
        int len = src.length();

        // Look for a following COMBINING DOT ABOVE
        for (int i = index + Character.charCount(src.codePointAt(index));
                i < len; i += Character.charCount(ch)) {

            ch = src.codePointAt(i);

            if (ch == '\u0307') {
                return true;
            } else {
                cc = Normalizer.getCombiningClass(ch);
                if ((cc == 0) || (cc == COMBINING_CLASS_ABOVE)) {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Examines whether a character is 'cased'.
     *
     * A character C is defined to be 'cased' if and only if at least one of
     * following are true for C: uppercase==true, or lowercase==true, or
     * general_category==titlecase_letter.
     *
     * The uppercase and lowercase property values are specified in the data
     * file DerivedCoreProperties.txt in the Unicode Character Database.
     */
    private static boolean isCased(int ch) {
        int type = Character.getType(ch);
        if (type == Character.LOWERCASE_LETTER ||
                type == Character.UPPERCASE_LETTER ||
                type == Character.TITLECASE_LETTER) {
            return true;
        } else {
            // Check for Other_Lowercase and Other_Uppercase
            //
            if ((ch >= 0x02B0) && (ch <= 0x02B8)) {
                // MODIFIER LETTER SMALL H..MODIFIER LETTER SMALL Y
                return true;
            } else if ((ch >= 0x02C0) && (ch <= 0x02C1)) {
                // MODIFIER LETTER GLOTTAL STOP..MODIFIER LETTER REVERSED GLOTTAL STOP
                return true;
            } else if ((ch >= 0x02E0) && (ch <= 0x02E4)) {
                // MODIFIER LETTER SMALL GAMMA..MODIFIER LETTER SMALL REVERSED GLOTTAL STOP
                return true;
            } else if (ch == 0x0345) {
                // COMBINING GREEK YPOGEGRAMMENI
                return true;
            } else if (ch == 0x037A) {
                // GREEK YPOGEGRAMMENI
                return true;
            } else if ((ch >= 0x1D2C) && (ch <= 0x1D61)) {
                // MODIFIER LETTER CAPITAL A..MODIFIER LETTER SMALL CHI
                return true;
            } else if ((ch >= 0x2160) && (ch <= 0x217F)) {
                // ROMAN NUMERAL ONE..ROMAN NUMERAL ONE THOUSAND
                // SMALL ROMAN NUMERAL ONE..SMALL ROMAN NUMERAL ONE THOUSAND
                return true;
            } else if ((ch >= 0x24B6) && (ch <= 0x24E9)) {
                // CIRCLED LATIN CAPITAL LETTER A..CIRCLED LATIN CAPITAL LETTER Z
                // CIRCLED LATIN SMALL LETTER A..CIRCLED LATIN SMALL LETTER Z
                return true;
            } else {
                return false;
            }
        }
    }

    private static boolean isSoftDotted(int ch) {
        switch (ch) {
        case 0x0069: // Soft_Dotted # L&       LATIN SMALL LETTER I
        case 0x006A: // Soft_Dotted # L&       LATIN SMALL LETTER J
        case 0x012F: // Soft_Dotted # L&       LATIN SMALL LETTER I WITH OGONEK
        case 0x0268: // Soft_Dotted # L&       LATIN SMALL LETTER I WITH STROKE
        case 0x0456: // Soft_Dotted # L&       CYRILLIC SMALL LETTER BYELORUSSIAN-UKRAINIAN I
        case 0x0458: // Soft_Dotted # L&       CYRILLIC SMALL LETTER JE
        case 0x1D62: // Soft_Dotted # L&       LATIN SUBSCRIPT SMALL LETTER I
        case 0x1E2D: // Soft_Dotted # L&       LATIN SMALL LETTER I WITH TILDE BELOW
        case 0x1ECB: // Soft_Dotted # L&       LATIN SMALL LETTER I WITH DOT BELOW
        case 0x2071: // Soft_Dotted # L&       SUPERSCRIPT LATIN SMALL LETTER I
            return true;
        default:
            return false;
        }
    }

    /**
     * An internal class that represents an entry in the Special Casing Properties.
     */
    static class Entry {
        int ch;
        char [] lower;
        char [] upper;
        String lang;
        int condition;

        Entry(int ch, char[] lower, char[] upper, String lang, int condition) {
            this.ch = ch;
            this.lower = lower;
            this.upper = upper;
            this.lang = lang;
            this.condition = condition;
        }

        int getCodePoint() {
            return ch;
        }

        char[] getLowerCase() {
            return lower;
        }

        char[] getUpperCase() {
            return upper;
        }

        String getLanguage() {
            return lang;
        }

        int getCondition() {
            return condition;
        }
    }
}
