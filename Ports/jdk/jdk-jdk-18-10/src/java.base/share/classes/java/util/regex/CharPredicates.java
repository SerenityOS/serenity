/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern.CharPredicate;
import java.util.regex.Pattern.BmpCharPredicate;

class CharPredicates {

    static final CharPredicate ALPHABETIC() {
        return Character::isAlphabetic;
    }

    // \p{gc=Decimal_Number}
    static final CharPredicate DIGIT() {
        return Character::isDigit;
    }

    static final CharPredicate LETTER() {
        return Character::isLetter;
    }

    static final CharPredicate IDEOGRAPHIC() {
        return Character::isIdeographic;
    }

    static final CharPredicate LOWERCASE() {
        return Character::isLowerCase;
    }

    static final CharPredicate UPPERCASE() {
        return Character::isUpperCase;
    }

    static final CharPredicate TITLECASE() {
        return Character::isTitleCase;
    }

    // \p{Whitespace}
    static final CharPredicate WHITE_SPACE() {
        return ch ->
            ((((1 << Character.SPACE_SEPARATOR) |
               (1 << Character.LINE_SEPARATOR) |
               (1 << Character.PARAGRAPH_SEPARATOR)) >> Character.getType(ch)) & 1)
            != 0 || (ch >= 0x9 && ch <= 0xd) || (ch == 0x85);
    }

    // \p{gc=Control}
    static final CharPredicate CONTROL() {
        return ch -> Character.getType(ch) == Character.CONTROL;
    }

    // \p{gc=Punctuation}
    static final CharPredicate PUNCTUATION() {
        return ch ->
            ((((1 << Character.CONNECTOR_PUNCTUATION) |
               (1 << Character.DASH_PUNCTUATION) |
               (1 << Character.START_PUNCTUATION) |
               (1 << Character.END_PUNCTUATION) |
               (1 << Character.OTHER_PUNCTUATION) |
               (1 << Character.INITIAL_QUOTE_PUNCTUATION) |
               (1 << Character.FINAL_QUOTE_PUNCTUATION)) >> Character.getType(ch)) & 1)
            != 0;
    }

    // \p{gc=Decimal_Number}
    // \p{Hex_Digit}    -> PropList.txt: Hex_Digit
    static final CharPredicate HEX_DIGIT() {
        return DIGIT().union(ch -> (ch >= 0x0030 && ch <= 0x0039) ||
                (ch >= 0x0041 && ch <= 0x0046) ||
                (ch >= 0x0061 && ch <= 0x0066) ||
                (ch >= 0xFF10 && ch <= 0xFF19) ||
                (ch >= 0xFF21 && ch <= 0xFF26) ||
                (ch >= 0xFF41 && ch <= 0xFF46));
    }

    static final CharPredicate ASSIGNED() {
        return ch -> Character.getType(ch) != Character.UNASSIGNED;
    }

    // PropList.txt:Noncharacter_Code_Point
    static final CharPredicate NONCHARACTER_CODE_POINT() {
        return ch -> (ch & 0xfffe) == 0xfffe || (ch >= 0xfdd0 && ch <= 0xfdef);
    }

    // \p{alpha}
    // \p{digit}
    static final CharPredicate ALNUM() {
        return ALPHABETIC().union(DIGIT());
    }

    // \p{Whitespace} --
    // [\N{LF} \N{VT} \N{FF} \N{CR} \N{NEL}  -> 0xa, 0xb, 0xc, 0xd, 0x85
    //  \p{gc=Line_Separator}
    //  \p{gc=Paragraph_Separator}]
    static final CharPredicate BLANK() {
        return ch ->
            Character.getType(ch) == Character.SPACE_SEPARATOR ||
            ch == 0x9; // \N{HT}
    }

    // [^
    //  \p{space}
    //  \p{gc=Control}
    //  \p{gc=Surrogate}
    //  \p{gc=Unassigned}]
    static final CharPredicate GRAPH() {
        return ch ->
            ((((1 << Character.SPACE_SEPARATOR) |
               (1 << Character.LINE_SEPARATOR) |
               (1 << Character.PARAGRAPH_SEPARATOR) |
               (1 << Character.CONTROL) |
               (1 << Character.SURROGATE) |
               (1 << Character.UNASSIGNED)) >> Character.getType(ch)) & 1)
            == 0;
    }

    // \p{graph}
    // \p{blank}
    // -- \p{cntrl}
    static final CharPredicate PRINT() {
        return GRAPH().union(BLANK()).and(CONTROL().negate());
    }

    //  200C..200D    PropList.txt:Join_Control
    static final CharPredicate JOIN_CONTROL() {
        return ch -> ch == 0x200C || ch == 0x200D;
    }

    //  \p{alpha}
    //  \p{gc=Mark}
    //  \p{digit}
    //  \p{gc=Connector_Punctuation}
    //  \p{Join_Control}    200C..200D
    static final CharPredicate WORD() {
        return ALPHABETIC().union(ch -> ((((1 << Character.NON_SPACING_MARK) |
                                  (1 << Character.ENCLOSING_MARK) |
                                  (1 << Character.COMBINING_SPACING_MARK) |
                                  (1 << Character.DECIMAL_DIGIT_NUMBER) |
                                  (1 << Character.CONNECTOR_PUNCTUATION))
                                 >> Character.getType(ch)) & 1) != 0,
                         JOIN_CONTROL());
    }

    /////////////////////////////////////////////////////////////////////////////

    private static CharPredicate getPosixPredicate(String name, boolean caseIns) {
        return switch (name) {
            case "ALPHA" -> ALPHABETIC();
            case "LOWER" -> caseIns
                                ? LOWERCASE().union(UPPERCASE(), TITLECASE())
                                : LOWERCASE();
            case "UPPER" -> caseIns
                                ? UPPERCASE().union(LOWERCASE(), TITLECASE())
                                : UPPERCASE();
            case "SPACE" -> WHITE_SPACE();
            case "PUNCT" -> PUNCTUATION();
            case "XDIGIT" -> HEX_DIGIT();
            case "ALNUM" -> ALNUM();
            case "CNTRL" -> CONTROL();
            case "DIGIT" -> DIGIT();
            case "BLANK" -> BLANK();
            case "GRAPH" -> GRAPH();
            case "PRINT" -> PRINT();
            default -> null;
        };
    }

    private static CharPredicate getUnicodePredicate(String name, boolean caseIns) {
        return switch (name) {
            case "ALPHABETIC" -> ALPHABETIC();
            case "ASSIGNED" -> ASSIGNED();
            case "CONTROL" -> CONTROL();
            case "HEXDIGIT", "HEX_DIGIT" -> HEX_DIGIT();
            case "IDEOGRAPHIC" -> IDEOGRAPHIC();
            case "JOINCONTROL", "JOIN_CONTROL" -> JOIN_CONTROL();
            case "LETTER" -> LETTER();
            case "LOWERCASE" -> caseIns
                                    ? LOWERCASE().union(UPPERCASE(), TITLECASE())
                                    : LOWERCASE();
            case "NONCHARACTERCODEPOINT", "NONCHARACTER_CODE_POINT" -> NONCHARACTER_CODE_POINT();
            case "TITLECASE" -> caseIns
                                    ? TITLECASE().union(LOWERCASE(), UPPERCASE())
                                    : TITLECASE();
            case "PUNCTUATION" -> PUNCTUATION();
            case "UPPERCASE" -> caseIns
                                    ? UPPERCASE().union(LOWERCASE(), TITLECASE())
                                    : UPPERCASE();
            case "WHITESPACE", "WHITE_SPACE" -> WHITE_SPACE();
            case "WORD" -> WORD();
            default -> null;
        };
    }

    public static CharPredicate forUnicodeProperty(String propName, boolean caseIns) {
        propName = propName.toUpperCase(Locale.ROOT);
        CharPredicate p = getUnicodePredicate(propName, caseIns);
        if (p != null)
            return p;
        return getPosixPredicate(propName, caseIns);
    }

    public static CharPredicate forPOSIXName(String propName, boolean caseIns) {
        return getPosixPredicate(propName.toUpperCase(Locale.ENGLISH), caseIns);
    }

    /////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a predicate matching all characters belong to a named
     * UnicodeScript.
     */
    static CharPredicate forUnicodeScript(String name) {
        final Character.UnicodeScript script;
        try {
            script = Character.UnicodeScript.forName(name);
            return ch -> script == Character.UnicodeScript.of(ch);
        } catch (IllegalArgumentException iae) {}
        return null;
    }

    /**
     * Returns a predicate matching all characters in a UnicodeBlock.
     */
    static CharPredicate forUnicodeBlock(String name) {
        final Character.UnicodeBlock block;
        try {
            block = Character.UnicodeBlock.forName(name);
            return ch -> block == Character.UnicodeBlock.of(ch);
        } catch (IllegalArgumentException iae) {}
         return null;
    }

    /////////////////////////////////////////////////////////////////////////////

    // unicode categories, aliases, properties, java methods ...

    static CharPredicate forProperty(String name, boolean caseIns) {
        // Unicode character property aliases, defined in
        // http://www.unicode.org/Public/UNIDATA/PropertyValueAliases.txt
        return switch (name) {
            case "Cn" -> category(1 << Character.UNASSIGNED);
            case "Lu" -> category(caseIns ? (1 << Character.LOWERCASE_LETTER) |
                                            (1 << Character.UPPERCASE_LETTER) |
                                            (1 << Character.TITLECASE_LETTER)
                                          : (1 << Character.UPPERCASE_LETTER));
            case "Ll" -> category(caseIns ? (1 << Character.LOWERCASE_LETTER) |
                                            (1 << Character.UPPERCASE_LETTER) |
                                            (1 << Character.TITLECASE_LETTER)
                                          : (1 << Character.LOWERCASE_LETTER));
            case "Lt" -> category(caseIns ? (1 << Character.LOWERCASE_LETTER) |
                                            (1 << Character.UPPERCASE_LETTER) |
                                            (1 << Character.TITLECASE_LETTER)
                                          : (1 << Character.TITLECASE_LETTER));
            case "Lm" -> category(1 << Character.MODIFIER_LETTER);
            case "Lo" -> category(1 << Character.OTHER_LETTER);
            case "Mn" -> category(1 << Character.NON_SPACING_MARK);
            case "Me" -> category(1 << Character.ENCLOSING_MARK);
            case "Mc" -> category(1 << Character.COMBINING_SPACING_MARK);
            case "Nd" -> category(1 << Character.DECIMAL_DIGIT_NUMBER);
            case "Nl" -> category(1 << Character.LETTER_NUMBER);
            case "No" -> category(1 << Character.OTHER_NUMBER);
            case "Zs" -> category(1 << Character.SPACE_SEPARATOR);
            case "Zl" -> category(1 << Character.LINE_SEPARATOR);
            case "Zp" -> category(1 << Character.PARAGRAPH_SEPARATOR);
            case "Cc" -> category(1 << Character.CONTROL);
            case "Cf" -> category(1 << Character.FORMAT);
            case "Co" -> category(1 << Character.PRIVATE_USE);
            case "Cs" -> category(1 << Character.SURROGATE);
            case "Pd" -> category(1 << Character.DASH_PUNCTUATION);
            case "Ps" -> category(1 << Character.START_PUNCTUATION);
            case "Pe" -> category(1 << Character.END_PUNCTUATION);
            case "Pc" -> category(1 << Character.CONNECTOR_PUNCTUATION);
            case "Po" -> category(1 << Character.OTHER_PUNCTUATION);
            case "Sm" -> category(1 << Character.MATH_SYMBOL);
            case "Sc" -> category(1 << Character.CURRENCY_SYMBOL);
            case "Sk" -> category(1 << Character.MODIFIER_SYMBOL);
            case "So" -> category(1 << Character.OTHER_SYMBOL);
            case "Pi" -> category(1 << Character.INITIAL_QUOTE_PUNCTUATION);
            case "Pf" -> category(1 << Character.FINAL_QUOTE_PUNCTUATION);
            case "L" -> category(((1 << Character.UPPERCASE_LETTER) |
                                  (1 << Character.LOWERCASE_LETTER) |
                                  (1 << Character.TITLECASE_LETTER) |
                                  (1 << Character.MODIFIER_LETTER) |
                                  (1 << Character.OTHER_LETTER)));
            case "M" -> category(((1 << Character.NON_SPACING_MARK) |
                                  (1 << Character.ENCLOSING_MARK) |
                                  (1 << Character.COMBINING_SPACING_MARK)));
            case "N" -> category(((1 << Character.DECIMAL_DIGIT_NUMBER) |
                                  (1 << Character.LETTER_NUMBER) |
                                  (1 << Character.OTHER_NUMBER)));
            case "Z" -> category(((1 << Character.SPACE_SEPARATOR) |
                                  (1 << Character.LINE_SEPARATOR) |
                                  (1 << Character.PARAGRAPH_SEPARATOR)));
            case "C" -> category(((1 << Character.CONTROL) |
                                  (1 << Character.FORMAT) |
                                  (1 << Character.PRIVATE_USE) |
                                  (1 << Character.SURROGATE) |
                                  (1 << Character.UNASSIGNED))); // Other
            case "P" -> category(((1 << Character.DASH_PUNCTUATION) |
                                  (1 << Character.START_PUNCTUATION) |
                                  (1 << Character.END_PUNCTUATION) |
                                  (1 << Character.CONNECTOR_PUNCTUATION) |
                                  (1 << Character.OTHER_PUNCTUATION) |
                                  (1 << Character.INITIAL_QUOTE_PUNCTUATION) |
                                  (1 << Character.FINAL_QUOTE_PUNCTUATION)));
            case "S" -> category(((1 << Character.MATH_SYMBOL) |
                                  (1 << Character.CURRENCY_SYMBOL) |
                                  (1 << Character.MODIFIER_SYMBOL) |
                                  (1 << Character.OTHER_SYMBOL)));
            case "LC" -> category(((1 << Character.UPPERCASE_LETTER) |
                                   (1 << Character.LOWERCASE_LETTER) |
                                   (1 << Character.TITLECASE_LETTER)));
            case "LD" -> category(((1 << Character.UPPERCASE_LETTER) |
                                   (1 << Character.LOWERCASE_LETTER) |
                                   (1 << Character.TITLECASE_LETTER) |
                                   (1 << Character.MODIFIER_LETTER) |
                                   (1 << Character.OTHER_LETTER) |
                                   (1 << Character.DECIMAL_DIGIT_NUMBER)));
            case "L1" -> range(0x00, 0xFF); // Latin-1
            case "all" -> Pattern.ALL();
            // Posix regular expression character classes, defined in
            // http://www.unix.org/onlinepubs/009695399/basedefs/xbd_chap09.html
            case "ASCII" -> range(0x00, 0x7F);    // ASCII
            case "Alnum" -> ctype(ASCII.ALNUM);   // Alphanumeric characters
            case "Alpha" -> ctype(ASCII.ALPHA);   // Alphabetic characters
            case "Blank" -> ctype(ASCII.BLANK);   // Space and tab characters
            case "Cntrl" -> ctype(ASCII.CNTRL);   // Control characters
            case "Digit" -> range('0', '9');      // Numeric characters
            case "Graph" -> ctype(ASCII.GRAPH);   // printable and visible
            case "Lower" -> caseIns ? ctype(ASCII.ALPHA)
                                    : range('a', 'z'); // Lower-case alphabetic
            case "Print" -> range(0x20, 0x7E);    // Printable characters
            case "Punct" -> ctype(ASCII.PUNCT);   // Punctuation characters
            case "Space" -> ctype(ASCII.SPACE);   // Space characters
            case "Upper" -> caseIns ? ctype(ASCII.ALPHA)
                                    : range('A', 'Z'); // Upper-case alphabetic
            case "XDigit" -> ctype(ASCII.XDIGIT); // hexadecimal digits

            // Java character properties, defined by methods in Character.java
            case "javaLowerCase" -> caseIns ? c -> Character.isLowerCase(c) ||
                                                   Character.isUpperCase(c) ||
                                                   Character.isTitleCase(c)
                                            : Character::isLowerCase;
            case "javaUpperCase" -> caseIns ? c -> Character.isUpperCase(c) ||
                                                   Character.isLowerCase(c) ||
                                                   Character.isTitleCase(c)
                                            : Character::isUpperCase;
            case "javaAlphabetic" -> Character::isAlphabetic;
            case "javaIdeographic" -> Character::isIdeographic;
            case "javaTitleCase" -> caseIns ? c -> Character.isTitleCase(c) ||
                                                   Character.isLowerCase(c) ||
                                                   Character.isUpperCase(c)
                                            : Character::isTitleCase;
            case "javaDigit" -> Character::isDigit;
            case "javaDefined" -> Character::isDefined;
            case "javaLetter" -> Character::isLetter;
            case "javaLetterOrDigit" -> Character::isLetterOrDigit;
            case "javaJavaIdentifierStart" -> Character::isJavaIdentifierStart;
            case "javaJavaIdentifierPart" -> Character::isJavaIdentifierPart;
            case "javaUnicodeIdentifierStart" -> Character::isUnicodeIdentifierStart;
            case "javaUnicodeIdentifierPart" -> Character::isUnicodeIdentifierPart;
            case "javaIdentifierIgnorable" -> Character::isIdentifierIgnorable;
            case "javaSpaceChar" -> Character::isSpaceChar;
            case "javaWhitespace" -> Character::isWhitespace;
            case "javaISOControl" -> Character::isISOControl;
            case "javaMirrored" -> Character::isMirrored;
            default -> null;
        };
    }

    private static CharPredicate category(final int typeMask) {
        return ch -> (typeMask & (1 << Character.getType(ch))) != 0;
    }

    private static CharPredicate range(final int lower, final int upper) {
        return (BmpCharPredicate)ch -> lower <= ch && ch <= upper;
    }

    private static CharPredicate ctype(final int ctype) {
        return (BmpCharPredicate)ch -> ch < 128 && ASCII.isType(ch, ctype);
    }

    /////////////////////////////////////////////////////////////////////////////

    /**
     * Posix ASCII variants, not in the lookup map
     */
    static final BmpCharPredicate ASCII_DIGIT() {
        return ch -> ch < 128 && ASCII.isDigit(ch);
    }
    static final BmpCharPredicate ASCII_WORD() {
        return ch -> ch < 128 && ASCII.isWord(ch);
    }
    static final BmpCharPredicate ASCII_SPACE() {
        return ch -> ch < 128 && ASCII.isSpace(ch);
    }

}
