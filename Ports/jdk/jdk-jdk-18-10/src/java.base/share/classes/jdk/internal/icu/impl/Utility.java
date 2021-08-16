/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
/*
 *******************************************************************************
 * Copyright (C) 1996-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package jdk.internal.icu.impl;

import jdk.internal.icu.lang.UCharacter;
import jdk.internal.icu.text.UTF16;

import java.io.IOException;
import java.util.Locale;

public final class Utility {

    /**
     * Convert characters outside the range U+0020 to U+007F to
     * Unicode escapes, and convert backslash to a double backslash.
     */
    public static final String escape(String s) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<s.length(); ) {
            int c = Character.codePointAt(s, i);
            i += UTF16.getCharCount(c);
            if (c >= ' ' && c <= 0x007F) {
                if (c == '\\') {
                    buf.append("\\\\"); // That is, "\\"
                } else {
                    buf.append((char)c);
                }
            } else {
                boolean four = c <= 0xFFFF;
                buf.append(four ? "\\u" : "\\U");
                buf.append(hex(c, four ? 4 : 8));
            }
        }
        return buf.toString();
    }

    /* This map must be in ASCENDING ORDER OF THE ESCAPE CODE */
    private static final char[] UNESCAPE_MAP = {
        /*"   0x22, 0x22 */
        /*'   0x27, 0x27 */
        /*?   0x3F, 0x3F */
        /*\   0x5C, 0x5C */
        /*a*/ 0x61, 0x07,
        /*b*/ 0x62, 0x08,
        /*e*/ 0x65, 0x1b,
        /*f*/ 0x66, 0x0c,
        /*n*/ 0x6E, 0x0a,
        /*r*/ 0x72, 0x0d,
        /*t*/ 0x74, 0x09,
        /*v*/ 0x76, 0x0b
    };

    /**
     * Convert an escape to a 32-bit code point value.  We attempt
     * to parallel the icu4c unescapeAt() function.
     * @param offset16 an array containing offset to the character
     * <em>after</em> the backslash.  Upon return offset16[0] will
     * be updated to point after the escape sequence.
     * @return character value from 0 to 10FFFF, or -1 on error.
     */
    public static int unescapeAt(String s, int[] offset16) {
        int c;
        int result = 0;
        int n = 0;
        int minDig = 0;
        int maxDig = 0;
        int bitsPerDigit = 4;
        int dig;
        int i;
        boolean braces = false;

        /* Check that offset is in range */
        int offset = offset16[0];
        int length = s.length();
        if (offset < 0 || offset >= length) {
            return -1;
        }

        /* Fetch first UChar after '\\' */
        c = Character.codePointAt(s, offset);
        offset += UTF16.getCharCount(c);

        /* Convert hexadecimal and octal escapes */
        switch (c) {
        case 'u':
            minDig = maxDig = 4;
            break;
        case 'U':
            minDig = maxDig = 8;
            break;
        case 'x':
            minDig = 1;
            if (offset < length && UTF16.charAt(s, offset) == 0x7B /*{*/) {
                ++offset;
                braces = true;
                maxDig = 8;
            } else {
                maxDig = 2;
          }
            break;
        default:
            dig = UCharacter.digit(c, 8);
            if (dig >= 0) {
                minDig = 1;
                maxDig = 3;
                n = 1; /* Already have first octal digit */
                bitsPerDigit = 3;
                result = dig;
            }
            break;
        }
        if (minDig != 0) {
            while (offset < length && n < maxDig) {
                c = UTF16.charAt(s, offset);
                dig = UCharacter.digit(c, (bitsPerDigit == 3) ? 8 : 16);
                if (dig < 0) {
                    break;
                }
                result = (result << bitsPerDigit) | dig;
                offset += UTF16.getCharCount(c);
                ++n;
            }
            if (n < minDig) {
                return -1;
            }
            if (braces) {
                if (c != 0x7D /*}*/) {
                    return -1;
                }
                ++offset;
          }
            if (result < 0 || result >= 0x110000) {
                return -1;
            }
            // If an escape sequence specifies a lead surrogate, see
            // if there is a trail surrogate after it, either as an
            // escape or as a literal.  If so, join them up into a
            // supplementary.
            if (offset < length &&
                    UTF16.isLeadSurrogate((char) result)) {
                int ahead = offset+1;
                c = s.charAt(offset); // [sic] get 16-bit code unit
                if (c == '\\' && ahead < length) {
                    int o[] = new int[] { ahead };
                    c = unescapeAt(s, o);
                    ahead = o[0];
                }
                if (UTF16.isTrailSurrogate((char) c)) {
                    offset = ahead;
                    result = UCharacterProperty.getRawSupplementary(
                            (char) result, (char) c);
                }
            }
            offset16[0] = offset;
            return result;
        }

        /* Convert C-style escapes in table */
        for (i=0; i<UNESCAPE_MAP.length; i+=2) {
            if (c == UNESCAPE_MAP[i]) {
                offset16[0] = offset;
                return UNESCAPE_MAP[i+1];
            } else if (c < UNESCAPE_MAP[i]) {
                break;
            }
        }

        /* Map \cX to control-X: X & 0x1F */
        if (c == 'c' && offset < length) {
            c = UTF16.charAt(s, offset);
            offset16[0] = offset + UTF16.getCharCount(c);
            return 0x1F & c;
        }

        /* If no special forms are recognized, then consider
         * the backslash to generically escape the next character. */
        offset16[0] = offset;
        return c;
    }

    /**
     * Supplies a zero-padded hex representation of an integer (without 0x)
     */
    public static String hex(long i, int places) {
        if (i == Long.MIN_VALUE) return "-8000000000000000";
        boolean negative = i < 0;
        if (negative) {
            i = -i;
        }
        String result = Long.toString(i, 16).toUpperCase(Locale.ENGLISH);
        if (result.length() < places) {
            result = "0000000000000000".substring(result.length(),places) + result;
        }
        if (negative) {
            return '-' + result;
        }
        return result;
    }

    static final char DIGITS[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    /**
     * Return true if the character is NOT printable ASCII.  The tab,
     * newline and linefeed characters are considered unprintable.
     */
    public static boolean isUnprintable(int c) {
        //0x20 = 32 and 0x7E = 126
        return !(c >= 0x20 && c <= 0x7E);
    }

    /**
     * Escape unprintable characters using <backslash>uxxxx notation
     * for U+0000 to U+FFFF and <backslash>Uxxxxxxxx for U+10000 and
     * above.  If the character is printable ASCII, then do nothing
     * and return FALSE.  Otherwise, append the escaped notation and
     * return TRUE.
     */
    public static <T extends Appendable> boolean escapeUnprintable(T result, int c) {
        try {
            if (isUnprintable(c)) {
                result.append('\\');
                if ((c & ~0xFFFF) != 0) {
                    result.append('U');
                    result.append(DIGITS[0xF&(c>>28)]);
                    result.append(DIGITS[0xF&(c>>24)]);
                    result.append(DIGITS[0xF&(c>>20)]);
                    result.append(DIGITS[0xF&(c>>16)]);
                } else {
                    result.append('u');
                }
                result.append(DIGITS[0xF&(c>>12)]);
                result.append(DIGITS[0xF&(c>>8)]);
                result.append(DIGITS[0xF&(c>>4)]);
                result.append(DIGITS[0xF&c]);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
