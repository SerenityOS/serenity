/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.utils;

/**
 * This class defines the basic XML character properties. The data
 * in this class can be used to verify that a character is a valid
 * XML character or if the character is a space, name start, or name
 * character.
 * <p>
 * A series of convenience methods are supplied to ease the burden
 * of the developer. Because inlining the checks can improve per
 * character performance, the tables of character properties are
 * public. Using the character as an index into the <code>CHARS</code>
 * array and applying the appropriate mask flag (e.g.
 * <code>MASK_VALID</code>), yields the same results as calling the
 * convenience methods. There is one exception: check the comments
 * for the <code>isValid</code> method for details.
 *
 * @author Glenn Marcy, IBM
 * @author Andy Clark, IBM
 * @author Eric Ye, IBM
 * @author Arnaud  Le Hors, IBM
 * @author Rahul Srivastava, Sun Microsystems Inc.
 *
 */
public class XMLChar {

    //
    // Constants
    //

    /** Character flags. */
    private static final byte[] CHARS = new byte[1 << 16];

    /** Valid character mask. */
    public static final int MASK_VALID = 0x01;

    /** Space character mask. */
    public static final int MASK_SPACE = 0x02;

    /** Name start character mask. */
    public static final int MASK_NAME_START = 0x04;

    /** Name character mask. */
    public static final int MASK_NAME = 0x08;

    /** Pubid character mask. */
    public static final int MASK_PUBID = 0x10;

    /**
     * Content character mask. Special characters are those that can
     * be considered the start of markup, such as '&lt;' and '&amp;'.
     * The various newline characters are considered special as well.
     * All other valid XML characters can be considered content.
     * <p>
     * This is an optimization for the inner loop of character scanning.
     */
    public static final int MASK_CONTENT = 0x20;

    /** NCName start character mask. */
    public static final int MASK_NCNAME_START = 0x40;

    /** NCName character mask. */
    public static final int MASK_NCNAME = 0x80;

    //
    // Static initialization
    //

    static {

        //
        // [2] Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] |
        //              [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        //

        int charRange[] = {
            0x0009, 0x000A, 0x000D, 0x000D, 0x0020, 0xD7FF, 0xE000, 0xFFFD,
        };

        //
        // [3] S ::= (#x20 | #x9 | #xD | #xA)+
        //

        int spaceChar[] = {
            0x0020, 0x0009, 0x000D, 0x000A,
        };

        //
        // [4] NameChar ::= Letter | Digit | '.' | '-' | '_' | ':' |
        //                  CombiningChar | Extender
        //

        int nameChar[] = {
            0x002D, 0x002E, // '-' and '.'
        };

        //
        // [5] Name ::= (Letter | '_' | ':') (NameChar)*
        //

        int nameStartChar[] = {
            0x003A, 0x005F, // ':' and '_'
        };

        //
        // [13] PubidChar ::= #x20 | 0xD | 0xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]
        //

        int pubidChar[] = {
            0x000A, 0x000D, 0x0020, 0x0021, 0x0023, 0x0024, 0x0025, 0x003D,
            0x005F
        };

        int pubidRange[] = {
            0x0027, 0x003B, 0x003F, 0x005A, 0x0061, 0x007A
        };

        //
        // [84] Letter ::= BaseChar | Ideographic
        //

        int letterRange[] = {
            // BaseChar
            0x0041, 0x005A, 0x0061, 0x007A, 0x00C0, 0x00D6, 0x00D8, 0x00F6,
            0x00F8, 0x0131, 0x0134, 0x013E, 0x0141, 0x0148, 0x014A, 0x017E,
            0x0180, 0x01C3, 0x01CD, 0x01F0, 0x01F4, 0x01F5, 0x01FA, 0x0217,
            0x0250, 0x02A8, 0x02BB, 0x02C1, 0x0388, 0x038A, 0x038E, 0x03A1,
            0x03A3, 0x03CE, 0x03D0, 0x03D6, 0x03E2, 0x03F3, 0x0401, 0x040C,
            0x040E, 0x044F, 0x0451, 0x045C, 0x045E, 0x0481, 0x0490, 0x04C4,
            0x04C7, 0x04C8, 0x04CB, 0x04CC, 0x04D0, 0x04EB, 0x04EE, 0x04F5,
            0x04F8, 0x04F9, 0x0531, 0x0556, 0x0561, 0x0586, 0x05D0, 0x05EA,
            0x05F0, 0x05F2, 0x0621, 0x063A, 0x0641, 0x064A, 0x0671, 0x06B7,
            0x06BA, 0x06BE, 0x06C0, 0x06CE, 0x06D0, 0x06D3, 0x06E5, 0x06E6,
            0x0905, 0x0939, 0x0958, 0x0961, 0x0985, 0x098C, 0x098F, 0x0990,
            0x0993, 0x09A8, 0x09AA, 0x09B0, 0x09B6, 0x09B9, 0x09DC, 0x09DD,
            0x09DF, 0x09E1, 0x09F0, 0x09F1, 0x0A05, 0x0A0A, 0x0A0F, 0x0A10,
            0x0A13, 0x0A28, 0x0A2A, 0x0A30, 0x0A32, 0x0A33, 0x0A35, 0x0A36,
            0x0A38, 0x0A39, 0x0A59, 0x0A5C, 0x0A72, 0x0A74, 0x0A85, 0x0A8B,
            0x0A8F, 0x0A91, 0x0A93, 0x0AA8, 0x0AAA, 0x0AB0, 0x0AB2, 0x0AB3,
            0x0AB5, 0x0AB9, 0x0B05, 0x0B0C, 0x0B0F, 0x0B10, 0x0B13, 0x0B28,
            0x0B2A, 0x0B30, 0x0B32, 0x0B33, 0x0B36, 0x0B39, 0x0B5C, 0x0B5D,
            0x0B5F, 0x0B61, 0x0B85, 0x0B8A, 0x0B8E, 0x0B90, 0x0B92, 0x0B95,
            0x0B99, 0x0B9A, 0x0B9E, 0x0B9F, 0x0BA3, 0x0BA4, 0x0BA8, 0x0BAA,
            0x0BAE, 0x0BB5, 0x0BB7, 0x0BB9, 0x0C05, 0x0C0C, 0x0C0E, 0x0C10,
            0x0C12, 0x0C28, 0x0C2A, 0x0C33, 0x0C35, 0x0C39, 0x0C60, 0x0C61,
            0x0C85, 0x0C8C, 0x0C8E, 0x0C90, 0x0C92, 0x0CA8, 0x0CAA, 0x0CB3,
            0x0CB5, 0x0CB9, 0x0CE0, 0x0CE1, 0x0D05, 0x0D0C, 0x0D0E, 0x0D10,
            0x0D12, 0x0D28, 0x0D2A, 0x0D39, 0x0D60, 0x0D61, 0x0E01, 0x0E2E,
            0x0E32, 0x0E33, 0x0E40, 0x0E45, 0x0E81, 0x0E82, 0x0E87, 0x0E88,
            0x0E94, 0x0E97, 0x0E99, 0x0E9F, 0x0EA1, 0x0EA3, 0x0EAA, 0x0EAB,
            0x0EAD, 0x0EAE, 0x0EB2, 0x0EB3, 0x0EC0, 0x0EC4, 0x0F40, 0x0F47,
            0x0F49, 0x0F69, 0x10A0, 0x10C5, 0x10D0, 0x10F6, 0x1102, 0x1103,
            0x1105, 0x1107, 0x110B, 0x110C, 0x110E, 0x1112, 0x1154, 0x1155,
            0x115F, 0x1161, 0x116D, 0x116E, 0x1172, 0x1173, 0x11AE, 0x11AF,
            0x11B7, 0x11B8, 0x11BC, 0x11C2, 0x1E00, 0x1E9B, 0x1EA0, 0x1EF9,
            0x1F00, 0x1F15, 0x1F18, 0x1F1D, 0x1F20, 0x1F45, 0x1F48, 0x1F4D,
            0x1F50, 0x1F57, 0x1F5F, 0x1F7D, 0x1F80, 0x1FB4, 0x1FB6, 0x1FBC,
            0x1FC2, 0x1FC4, 0x1FC6, 0x1FCC, 0x1FD0, 0x1FD3, 0x1FD6, 0x1FDB,
            0x1FE0, 0x1FEC, 0x1FF2, 0x1FF4, 0x1FF6, 0x1FFC, 0x212A, 0x212B,
            0x2180, 0x2182, 0x3041, 0x3094, 0x30A1, 0x30FA, 0x3105, 0x312C,
            0xAC00, 0xD7A3,
            // Ideographic
            0x3021, 0x3029, 0x4E00, 0x9FA5,
        };
        int letterChar[] = {
            // BaseChar
            0x0386, 0x038C, 0x03DA, 0x03DC, 0x03DE, 0x03E0, 0x0559, 0x06D5,
            0x093D, 0x09B2, 0x0A5E, 0x0A8D, 0x0ABD, 0x0AE0, 0x0B3D, 0x0B9C,
            0x0CDE, 0x0E30, 0x0E84, 0x0E8A, 0x0E8D, 0x0EA5, 0x0EA7, 0x0EB0,
            0x0EBD, 0x1100, 0x1109, 0x113C, 0x113E, 0x1140, 0x114C, 0x114E,
            0x1150, 0x1159, 0x1163, 0x1165, 0x1167, 0x1169, 0x1175, 0x119E,
            0x11A8, 0x11AB, 0x11BA, 0x11EB, 0x11F0, 0x11F9, 0x1F59, 0x1F5B,
            0x1F5D, 0x1FBE, 0x2126, 0x212E,
            // Ideographic
            0x3007,
        };

        //
        // [87] CombiningChar ::= ...
        //

        int combiningCharRange[] = {
            0x0300, 0x0345, 0x0360, 0x0361, 0x0483, 0x0486, 0x0591, 0x05A1,
            0x05A3, 0x05B9, 0x05BB, 0x05BD, 0x05C1, 0x05C2, 0x064B, 0x0652,
            0x06D6, 0x06DC, 0x06DD, 0x06DF, 0x06E0, 0x06E4, 0x06E7, 0x06E8,
            0x06EA, 0x06ED, 0x0901, 0x0903, 0x093E, 0x094C, 0x0951, 0x0954,
            0x0962, 0x0963, 0x0981, 0x0983, 0x09C0, 0x09C4, 0x09C7, 0x09C8,
            0x09CB, 0x09CD, 0x09E2, 0x09E3, 0x0A40, 0x0A42, 0x0A47, 0x0A48,
            0x0A4B, 0x0A4D, 0x0A70, 0x0A71, 0x0A81, 0x0A83, 0x0ABE, 0x0AC5,
            0x0AC7, 0x0AC9, 0x0ACB, 0x0ACD, 0x0B01, 0x0B03, 0x0B3E, 0x0B43,
            0x0B47, 0x0B48, 0x0B4B, 0x0B4D, 0x0B56, 0x0B57, 0x0B82, 0x0B83,
            0x0BBE, 0x0BC2, 0x0BC6, 0x0BC8, 0x0BCA, 0x0BCD, 0x0C01, 0x0C03,
            0x0C3E, 0x0C44, 0x0C46, 0x0C48, 0x0C4A, 0x0C4D, 0x0C55, 0x0C56,
            0x0C82, 0x0C83, 0x0CBE, 0x0CC4, 0x0CC6, 0x0CC8, 0x0CCA, 0x0CCD,
            0x0CD5, 0x0CD6, 0x0D02, 0x0D03, 0x0D3E, 0x0D43, 0x0D46, 0x0D48,
            0x0D4A, 0x0D4D, 0x0E34, 0x0E3A, 0x0E47, 0x0E4E, 0x0EB4, 0x0EB9,
            0x0EBB, 0x0EBC, 0x0EC8, 0x0ECD, 0x0F18, 0x0F19, 0x0F71, 0x0F84,
            0x0F86, 0x0F8B, 0x0F90, 0x0F95, 0x0F99, 0x0FAD, 0x0FB1, 0x0FB7,
            0x20D0, 0x20DC, 0x302A, 0x302F,
        };

        int combiningCharChar[] = {
            0x05BF, 0x05C4, 0x0670, 0x093C, 0x094D, 0x09BC, 0x09BE, 0x09BF,
            0x09D7, 0x0A02, 0x0A3C, 0x0A3E, 0x0A3F, 0x0ABC, 0x0B3C, 0x0BD7,
            0x0D57, 0x0E31, 0x0EB1, 0x0F35, 0x0F37, 0x0F39, 0x0F3E, 0x0F3F,
            0x0F97, 0x0FB9, 0x20E1, 0x3099, 0x309A,
        };

        //
        // [88] Digit ::= ...
        //

        int digitRange[] = {
            0x0030, 0x0039, 0x0660, 0x0669, 0x06F0, 0x06F9, 0x0966, 0x096F,
            0x09E6, 0x09EF, 0x0A66, 0x0A6F, 0x0AE6, 0x0AEF, 0x0B66, 0x0B6F,
            0x0BE7, 0x0BEF, 0x0C66, 0x0C6F, 0x0CE6, 0x0CEF, 0x0D66, 0x0D6F,
            0x0E50, 0x0E59, 0x0ED0, 0x0ED9, 0x0F20, 0x0F29,
        };

        //
        // [89] Extender ::= ...
        //

        int extenderRange[] = {
            0x3031, 0x3035, 0x309D, 0x309E, 0x30FC, 0x30FE,
        };

        int extenderChar[] = {
            0x00B7, 0x02D0, 0x02D1, 0x0387, 0x0640, 0x0E46, 0x0EC6, 0x3005,
        };

        //
        // SpecialChar ::= '<', '&', '\n', '\r', ']'
        //

        int specialChar[] = {
            '<', '&', '\n', '\r', ']',
        };

        //
        // Initialize
        //

        // set valid characters
        for (int i = 0; i < charRange.length; i += 2) {
            for (int j = charRange[i]; j <= charRange[i + 1]; j++) {
                CHARS[j] |= MASK_VALID | MASK_CONTENT;
            }
        }

        // remove special characters
        for (int i = 0; i < specialChar.length; i++) {
            CHARS[specialChar[i]] = (byte)(CHARS[specialChar[i]] & ~MASK_CONTENT);
        }

        // set space characters
        for (int i = 0; i < spaceChar.length; i++) {
            CHARS[spaceChar[i]] |= MASK_SPACE;
        }

        // set name start characters
        for (int i = 0; i < nameStartChar.length; i++) {
            CHARS[nameStartChar[i]] |= MASK_NAME_START | MASK_NAME |
                                       MASK_NCNAME_START | MASK_NCNAME;
        }
        for (int i = 0; i < letterRange.length; i += 2) {
            for (int j = letterRange[i]; j <= letterRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME_START | MASK_NAME |
                            MASK_NCNAME_START | MASK_NCNAME;
            }
        }
        for (int i = 0; i < letterChar.length; i++) {
            CHARS[letterChar[i]] |= MASK_NAME_START | MASK_NAME |
                                    MASK_NCNAME_START | MASK_NCNAME;
        }

        // set name characters
        for (int i = 0; i < nameChar.length; i++) {
            CHARS[nameChar[i]] |= MASK_NAME | MASK_NCNAME;
        }
        for (int i = 0; i < digitRange.length; i += 2) {
            for (int j = digitRange[i]; j <= digitRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME | MASK_NCNAME;
            }
        }
        for (int i = 0; i < combiningCharRange.length; i += 2) {
            for (int j = combiningCharRange[i]; j <= combiningCharRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME | MASK_NCNAME;
            }
        }
        for (int i = 0; i < combiningCharChar.length; i++) {
            CHARS[combiningCharChar[i]] |= MASK_NAME | MASK_NCNAME;
        }
        for (int i = 0; i < extenderRange.length; i += 2) {
            for (int j = extenderRange[i]; j <= extenderRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME | MASK_NCNAME;
            }
        }
        for (int i = 0; i < extenderChar.length; i++) {
            CHARS[extenderChar[i]] |= MASK_NAME | MASK_NCNAME;
        }

        // remove ':' from allowable MASK_NCNAME_START and MASK_NCNAME chars
        CHARS[':'] &= ~(MASK_NCNAME_START | MASK_NCNAME);

        // set Pubid characters
        for (int i = 0; i < pubidChar.length; i++) {
            CHARS[pubidChar[i]] |= MASK_PUBID;
        }
        for (int i = 0; i < pubidRange.length; i += 2) {
            for (int j = pubidRange[i]; j <= pubidRange[i + 1]; j++) {
                CHARS[j] |= MASK_PUBID;
            }
        }

    } // <clinit>()

    //
    // Public static methods
    //

    /**
     * Returns true if the specified character is a supplemental character.
     *
     * @param c The character to check.
     */
    public static boolean isSupplemental(int c) {
        return (c >= 0x10000 && c <= 0x10FFFF);
    }

    /**
     * Returns true the supplemental character corresponding to the given
     * surrogates.
     *
     * @param h The high surrogate.
     * @param l The low surrogate.
     */
    public static int supplemental(char h, char l) {
        return (h - 0xD800) * 0x400 + (l - 0xDC00) + 0x10000;
    }

    /**
     * Returns the high surrogate of a supplemental character
     *
     * @param c The supplemental character to "split".
     */
    public static char highSurrogate(int c) {
        return (char) (((c - 0x00010000) >> 10) + 0xD800);
    }

    /**
     * Returns the low surrogate of a supplemental character
     *
     * @param c The supplemental character to "split".
     */
    public static char lowSurrogate(int c) {
        return (char) (((c - 0x00010000) & 0x3FF) + 0xDC00);
    }

    /**
     * Returns whether the given character is a high surrogate
     *
     * @param c The character to check.
     */
    public static boolean isHighSurrogate(int c) {
        return (0xD800 <= c && c <= 0xDBFF);
    }

    /**
     * Returns whether the given character is a low surrogate
     *
     * @param c The character to check.
     */
    public static boolean isLowSurrogate(int c) {
        return (0xDC00 <= c && c <= 0xDFFF);
    }


    /**
     * Returns true if the specified character is valid. This method
     * also checks the surrogate character range from 0x10000 to 0x10FFFF.
     * <p>
     * If the program chooses to apply the mask directly to the
     * <code>CHARS</code> array, then they are responsible for checking
     * the surrogate character range.
     *
     * @param c The character to check.
     */
    public static boolean isValid(int c) {
        return (c < 0x10000 && (CHARS[c] & MASK_VALID) != 0) ||
               (0x10000 <= c && c <= 0x10FFFF);
    } // isValid(int):boolean

    /**
     * Returns true if the specified character is invalid.
     *
     * @param c The character to check.
     */
    public static boolean isInvalid(int c) {
        return !isValid(c);
    } // isInvalid(int):boolean

    /**
     * Returns true if the specified character can be considered content.
     *
     * @param c The character to check.
     */
    public static boolean isContent(int c) {
        return (c < 0x10000 && (CHARS[c] & MASK_CONTENT) != 0) ||
               (0x10000 <= c && c <= 0x10FFFF);
    } // isContent(int):boolean

    /**
     * Returns true if the specified character can be considered markup.
     * Markup characters include '&lt;', '&amp;', and '%'.
     *
     * @param c The character to check.
     */
    public static boolean isMarkup(int c) {
        return c == '<' || c == '&' || c == '%';
    } // isMarkup(int):boolean

    /**
     * Returns true if the specified character is a space character
     * as defined by production [3] in the XML 1.0 specification.
     *
     * @param c The character to check.
     */
    public static boolean isSpace(int c) {
        return c < 0x10000 && (CHARS[c] & MASK_SPACE) != 0;
    } // isSpace(int):boolean

    /**
     * Returns true if the specified character is a valid name start
     * character as defined by production [5] in the XML 1.0
     * specification.
     *
     * @param c The character to check.
     */
    public static boolean isNameStart(int c) {
        return c < 0x10000 && (CHARS[c] & MASK_NAME_START) != 0;
    } // isNameStart(int):boolean

    /**
     * Returns true if the specified character is a valid name
     * character as defined by production [4] in the XML 1.0
     * specification.
     *
     * @param c The character to check.
     */
    public static boolean isName(int c) {
        return c < 0x10000 && (CHARS[c] & MASK_NAME) != 0;
    } // isName(int):boolean

    /**
     * Returns true if the specified character is a valid NCName start
     * character as defined by production [4] in Namespaces in XML
     * recommendation.
     *
     * @param c The character to check.
     */
    public static boolean isNCNameStart(int c) {
        return c < 0x10000 && (CHARS[c] & MASK_NCNAME_START) != 0;
    } // isNCNameStart(int):boolean

    /**
     * Returns true if the specified character is a valid NCName
     * character as defined by production [5] in Namespaces in XML
     * recommendation.
     *
     * @param c The character to check.
     */
    public static boolean isNCName(int c) {
        return c < 0x10000 && (CHARS[c] & MASK_NCNAME) != 0;
    } // isNCName(int):boolean

    /**
     * Returns true if the specified character is a valid Pubid
     * character as defined by production [13] in the XML 1.0
     * specification.
     *
     * @param c The character to check.
     */
    public static boolean isPubid(int c) {
        return c < 0x10000 && (CHARS[c] & MASK_PUBID) != 0;
    } // isPubid(int):boolean

    /*
     * [5] Name ::= (Letter | '_' | ':') (NameChar)*
     */
    /**
     * Check to see if a string is a valid Name according to [5]
     * in the XML 1.0 Recommendation
     *
     * @param name string to check
     * @return true if name is a valid Name
     */
    public static boolean isValidName(String name) {
        if (name.length() == 0)
            return false;
        char ch = name.charAt(0);
        if( isNameStart(ch) == false)
           return false;
        for (int i = 1; i < name.length(); i++ ) {
           ch = name.charAt(i);
           if( isName( ch ) == false ){
              return false;
           }
        }
        return true;
    } // isValidName(String):boolean


    /*
     * from the namespace rec
     * [4] NCName ::= (Letter | '_') (NCNameChar)*
     */
    /**
     * Check to see if a string is a valid NCName according to [4]
     * from the XML Namespaces 1.0 Recommendation
     *
     * @param ncName string to check
     * @return true if name is a valid NCName
     */
    public static boolean isValidNCName(String ncName) {
        if (ncName.length() == 0)
            return false;
        char ch = ncName.charAt(0);
        if( isNCNameStart(ch) == false)
           return false;
        for (int i = 1; i < ncName.length(); i++ ) {
           ch = ncName.charAt(i);
           if( isNCName( ch ) == false ){
              return false;
           }
        }
        return true;
    } // isValidNCName(String):boolean

    /*
     * [7] Nmtoken ::= (NameChar)+
     */
    /**
     * Check to see if a string is a valid Nmtoken according to [7]
     * in the XML 1.0 Recommendation
     *
     * @param nmtoken string to check
     * @return true if nmtoken is a valid Nmtoken
     */
    public static boolean isValidNmtoken(String nmtoken) {
        if (nmtoken.length() == 0)
            return false;
        for (int i = 0; i < nmtoken.length(); i++ ) {
           char ch = nmtoken.charAt(i);
           if(  ! isName( ch ) ){
              return false;
           }
        }
        return true;
    } // isValidName(String):boolean





    // encodings

    /**
     * Returns true if the encoding name is a valid IANA encoding.
     * This method does not verify that there is a decoder available
     * for this encoding, only that the characters are valid for an
     * IANA encoding name.
     *
     * @param ianaEncoding The IANA encoding name.
     */
    public static boolean isValidIANAEncoding(String ianaEncoding) {
        if (ianaEncoding != null) {
            int length = ianaEncoding.length();
            if (length > 0) {
                char c = ianaEncoding.charAt(0);
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                    for (int i = 1; i < length; i++) {
                        c = ianaEncoding.charAt(i);
                        if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') &&
                            (c < '0' || c > '9') && c != '.' && c != '_' &&
                            c != '-') {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    } // isValidIANAEncoding(String):boolean

    /**
     * Returns true if the encoding name is a valid Java encoding.
     * This method does not verify that there is a decoder available
     * for this encoding, only that the characters are valid for an
     * Java encoding name.
     *
     * @param javaEncoding The Java encoding name.
     */
    public static boolean isValidJavaEncoding(String javaEncoding) {
        if (javaEncoding != null) {
            int length = javaEncoding.length();
            if (length > 0) {
                for (int i = 1; i < length; i++) {
                    char c = javaEncoding.charAt(i);
                    if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') &&
                        (c < '0' || c > '9') && c != '.' && c != '_' &&
                        c != '-') {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    } // isValidIANAEncoding(String):boolean

   /**
     * Simple check to determine if qname is legal. If it returns false
     * then <param>str</param> is illegal; if it returns true then
     * <param>str</param> is legal.
     */
    public static boolean isValidQName(String str) {

       final int colon = str.indexOf(':');

       if (colon == 0 || colon == str.length() - 1) {
           return false;
       }

       if (colon > 0) {
           final String prefix = str.substring(0,colon);
           final String localPart = str.substring(colon+1);
           return isValidNCName(prefix) && isValidNCName(localPart);
       }
       else {
           return isValidNCName(str);
       }
    }

} // class XMLChar
