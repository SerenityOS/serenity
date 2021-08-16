/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
package java.net;

import java.io.InputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import jdk.internal.icu.impl.Punycode;
import jdk.internal.icu.text.StringPrep;
import jdk.internal.icu.text.UCharacterIterator;

/**
 * Provides methods to convert internationalized domain names (IDNs) between
 * a normal Unicode representation and an ASCII Compatible Encoding (ACE) representation.
 * Internationalized domain names can use characters from the entire range of
 * Unicode, while traditional domain names are restricted to ASCII characters.
 * ACE is an encoding of Unicode strings that uses only ASCII characters and
 * can be used with software (such as the Domain Name System) that only
 * understands traditional domain names.
 *
 * <p>Internationalized domain names are defined in <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>.
 * RFC 3490 defines two operations: ToASCII and ToUnicode. These 2 operations employ
 * <a href="http://www.ietf.org/rfc/rfc3491.txt">Nameprep</a> algorithm, which is a
 * profile of <a href="http://www.ietf.org/rfc/rfc3454.txt">Stringprep</a>, and
 * <a href="http://www.ietf.org/rfc/rfc3492.txt">Punycode</a> algorithm to convert
 * domain name string back and forth.
 *
 * <p>The behavior of aforementioned conversion process can be adjusted by various flags:
 *   <ul>
 *     <li>If the ALLOW_UNASSIGNED flag is used, the domain name string to be converted
 *         can contain code points that are unassigned in Unicode 3.2, which is the
 *         Unicode version on which IDN conversion is based. If the flag is not used,
 *         the presence of such unassigned code points is treated as an error.
 *     <li>If the USE_STD3_ASCII_RULES flag is used, ASCII strings are checked against <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC 1122</a> and <a href="http://www.ietf.org/rfc/rfc1123.txt">RFC 1123</a>.
 *         It is an error if they don't meet the requirements.
 *   </ul>
 * These flags can be logically OR'ed together.
 *
 * <p>The security consideration is important with respect to internationalization
 * domain name support. For example, English domain names may be <i>homographed</i>
 * - maliciously misspelled by substitution of non-Latin letters.
 * <a href="http://www.unicode.org/reports/tr36/">Unicode Technical Report #36</a>
 * discusses security issues of IDN support as well as possible solutions.
 * Applications are responsible for taking adequate security measures when using
 * international domain names.
 *
 * @author Edward Wang
 * @since 1.6
 *
 */
public final class IDN {
    /**
     * Flag to allow processing of unassigned code points
     */
    public static final int ALLOW_UNASSIGNED = 0x01;

    /**
     * Flag to turn on the check against STD-3 ASCII rules
     */
    public static final int USE_STD3_ASCII_RULES = 0x02;


    /**
     * Translates a string from Unicode to ASCII Compatible Encoding (ACE),
     * as defined by the ToASCII operation of <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>.
     *
     * <p>ToASCII operation can fail. ToASCII fails if any step of it fails.
     * If ToASCII operation fails, an IllegalArgumentException will be thrown.
     * In this case, the input string should not be used in an internationalized domain name.
     *
     * <p> A label is an individual part of a domain name. The original ToASCII operation,
     * as defined in RFC 3490, only operates on a single label. This method can handle
     * both label and entire domain name, by assuming that labels in a domain name are
     * always separated by dots. The following characters are recognized as dots:
     * &#0092;u002E (full stop), &#0092;u3002 (ideographic full stop), &#0092;uFF0E (fullwidth full stop),
     * and &#0092;uFF61 (halfwidth ideographic full stop). if dots are
     * used as label separators, this method also changes all of them to &#0092;u002E (full stop)
     * in output translated string.
     *
     * @param input     the string to be processed
     * @param flag      process flag; can be 0 or any logical OR of possible flags
     *
     * @return          the translated {@code String}
     *
     * @throws IllegalArgumentException   if the input string doesn't conform to RFC 3490 specification
     */
    public static String toASCII(String input, int flag)
    {
        int p = 0, q = 0;
        StringBuilder out = new StringBuilder();

        if (isRootLabel(input)) {
            return ".";
        }

        while (p < input.length()) {
            q = searchDots(input, p);
            out.append(toASCIIInternal(input.substring(p, q),  flag));
            if (q != (input.length())) {
               // has more labels, or keep the trailing dot as at present
               out.append('.');
            }
            p = q + 1;
        }

        return out.toString();
    }


    /**
     * Translates a string from Unicode to ASCII Compatible Encoding (ACE),
     * as defined by the ToASCII operation of <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>.
     *
     * <p> This convenience method works as if by invoking the
     * two-argument counterpart as follows:
     * <blockquote>
     * {@link #toASCII(String, int) toASCII}(input,&nbsp;0);
     * </blockquote>
     *
     * @param input     the string to be processed
     *
     * @return          the translated {@code String}
     *
     * @throws IllegalArgumentException   if the input string doesn't conform to RFC 3490 specification
     */
    public static String toASCII(String input) {
        return toASCII(input, 0);
    }


    /**
     * Translates a string from ASCII Compatible Encoding (ACE) to Unicode,
     * as defined by the ToUnicode operation of <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>.
     *
     * <p>ToUnicode never fails. In case of any error, the input string is returned unmodified.
     *
     * <p> A label is an individual part of a domain name. The original ToUnicode operation,
     * as defined in RFC 3490, only operates on a single label. This method can handle
     * both label and entire domain name, by assuming that labels in a domain name are
     * always separated by dots. The following characters are recognized as dots:
     * &#0092;u002E (full stop), &#0092;u3002 (ideographic full stop), &#0092;uFF0E (fullwidth full stop),
     * and &#0092;uFF61 (halfwidth ideographic full stop).
     *
     * @param input     the string to be processed
     * @param flag      process flag; can be 0 or any logical OR of possible flags
     *
     * @return          the translated {@code String}
     */
    public static String toUnicode(String input, int flag) {
        int p = 0, q = 0;
        StringBuilder out = new StringBuilder();

        if (isRootLabel(input)) {
            return ".";
        }

        while (p < input.length()) {
            q = searchDots(input, p);
            out.append(toUnicodeInternal(input.substring(p, q),  flag));
            if (q != (input.length())) {
               // has more labels, or keep the trailing dot as at present
               out.append('.');
            }
            p = q + 1;
        }

        return out.toString();
    }


    /**
     * Translates a string from ASCII Compatible Encoding (ACE) to Unicode,
     * as defined by the ToUnicode operation of <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>.
     *
     * <p> This convenience method works as if by invoking the
     * two-argument counterpart as follows:
     * <blockquote>
     * {@link #toUnicode(String, int) toUnicode}(input,&nbsp;0);
     * </blockquote>
     *
     * @param input     the string to be processed
     *
     * @return          the translated {@code String}
     */
    public static String toUnicode(String input) {
        return toUnicode(input, 0);
    }


    /* ---------------- Private members -------------- */

    // ACE Prefix is "xn--"
    private static final String ACE_PREFIX = "xn--";
    private static final int ACE_PREFIX_LENGTH = ACE_PREFIX.length();

    private static final int MAX_LABEL_LENGTH   = 63;

    // single instance of nameprep
    private static StringPrep namePrep = null;

    static {
        try {
            final String IDN_PROFILE = "/sun/net/idn/uidna.spp";
            @SuppressWarnings("removal")
            InputStream stream = System.getSecurityManager() != null
                    ? AccessController.doPrivileged(new PrivilegedAction<>() {
                            public InputStream run() {
                                return StringPrep.class.getResourceAsStream(IDN_PROFILE);
                            }})
                    : StringPrep.class.getResourceAsStream(IDN_PROFILE);

            namePrep = new StringPrep(stream);
            stream.close();
        } catch (IOException e) {
            // should never reach here
            assert false;
        }
    }


    /* ---------------- Private operations -------------- */


    //
    // to suppress the default zero-argument constructor
    //
    private IDN() {}

    //
    // toASCII operation; should only apply to a single label
    //
    private static String toASCIIInternal(String label, int flag)
    {
        // step 1
        // Check if the string contains code points outside the ASCII range 0..0x7c.
        boolean isASCII  = isAllASCII(label);
        StringBuffer dest;

        // step 2
        // perform the nameprep operation; flag ALLOW_UNASSIGNED is used here
        if (!isASCII) {
            UCharacterIterator iter = UCharacterIterator.getInstance(label);
            try {
                dest = namePrep.prepare(iter, flag);
            } catch (java.text.ParseException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            dest = new StringBuffer(label);
        }

        // step 8, move forward to check the smallest number of the code points
        // the length must be inside 1..63
        if (dest.length() == 0) {
            throw new IllegalArgumentException(
                        "Empty label is not a legal name");
        }

        // step 3
        // Verify the absence of non-LDH ASCII code points
        //   0..0x2c, 0x2e..0x2f, 0x3a..0x40, 0x5b..0x60, 0x7b..0x7f
        // Verify the absence of leading and trailing hyphen
        boolean useSTD3ASCIIRules = ((flag & USE_STD3_ASCII_RULES) != 0);
        if (useSTD3ASCIIRules) {
            for (int i = 0; i < dest.length(); i++) {
                int c = dest.charAt(i);
                if (isNonLDHAsciiCodePoint(c)) {
                    throw new IllegalArgumentException(
                        "Contains non-LDH ASCII characters");
                }
            }

            if (dest.charAt(0) == '-' ||
                dest.charAt(dest.length() - 1) == '-') {

                throw new IllegalArgumentException(
                        "Has leading or trailing hyphen");
            }
        }

        if (!isASCII) {
            // step 4
            // If all code points are inside 0..0x7f, skip to step 8
            if (!isAllASCII(dest.toString())) {
                // step 5
                // verify the sequence does not begin with ACE prefix
                if(!startsWithACEPrefix(dest)){

                    // step 6
                    // encode the sequence with punycode
                    try {
                        dest = Punycode.encode(dest, null);
                    } catch (java.text.ParseException e) {
                        throw new IllegalArgumentException(e);
                    }

                    dest = toASCIILower(dest);

                    // step 7
                    // prepend the ACE prefix
                    dest.insert(0, ACE_PREFIX);
                } else {
                    throw new IllegalArgumentException("The input starts with the ACE Prefix");
                }

            }
        }

        // step 8
        // the length must be inside 1..63
        if (dest.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("The label in the input is too long");
        }

        return dest.toString();
    }

    //
    // toUnicode operation; should only apply to a single label
    //
    private static String toUnicodeInternal(String label, int flag) {
        boolean[] caseFlags = null;
        StringBuffer dest;

        // step 1
        // find out if all the codepoints in input are ASCII
        boolean isASCII = isAllASCII(label);

        if(!isASCII){
            // step 2
            // perform the nameprep operation; flag ALLOW_UNASSIGNED is used here
            try {
                UCharacterIterator iter = UCharacterIterator.getInstance(label);
                dest = namePrep.prepare(iter, flag);
            } catch (Exception e) {
                // toUnicode never fails; if any step fails, return the input string
                return label;
            }
        } else {
            dest = new StringBuffer(label);
        }

        // step 3
        // verify ACE Prefix
        if(startsWithACEPrefix(dest)) {

            // step 4
            // Remove the ACE Prefix
            String temp = dest.substring(ACE_PREFIX_LENGTH, dest.length());

            try {
                // step 5
                // Decode using punycode
                StringBuffer decodeOut = Punycode.decode(new StringBuffer(temp), null);

                // step 6
                // Apply toASCII
                String toASCIIOut = toASCII(decodeOut.toString(), flag);

                // step 7
                // verify
                if (toASCIIOut.equalsIgnoreCase(dest.toString())) {
                    // step 8
                    // return output of step 5
                    return decodeOut.toString();
                }
            } catch (Exception ignored) {
                // no-op
            }
        }

        // just return the input
        return label;
    }


    //
    // LDH stands for "letter/digit/hyphen", with characters restricted to the
    // 26-letter Latin alphabet <A-Z a-z>, the digits <0-9>, and the hyphen
    // <->.
    // Non LDH refers to characters in the ASCII range, but which are not
    // letters, digits or the hyphen.
    //
    // non-LDH = 0..0x2C, 0x2E..0x2F, 0x3A..0x40, 0x5B..0x60, 0x7B..0x7F
    //
    private static boolean isNonLDHAsciiCodePoint(int ch){
        return (0x0000 <= ch && ch <= 0x002C) ||
               (0x002E <= ch && ch <= 0x002F) ||
               (0x003A <= ch && ch <= 0x0040) ||
               (0x005B <= ch && ch <= 0x0060) ||
               (0x007B <= ch && ch <= 0x007F);
    }

    //
    // search dots in a string and return the index of that character;
    // or if there is no dots, return the length of input string
    // dots might be: \u002E (full stop), \u3002 (ideographic full stop), \uFF0E (fullwidth full stop),
    // and \uFF61 (halfwidth ideographic full stop).
    //
    private static int searchDots(String s, int start) {
        int i;
        for (i = start; i < s.length(); i++) {
            if (isLabelSeparator(s.charAt(i))) {
                break;
            }
        }

        return i;
    }

    //
    // to check if a string is a root label, ".".
    //
    private static boolean isRootLabel(String s) {
        return (s.length() == 1 && isLabelSeparator(s.charAt(0)));
    }

    //
    // to check if a character is a label separator, i.e. a dot character.
    //
    private static boolean isLabelSeparator(char c) {
        return (c == '.' || c == '\u3002' || c == '\uFF0E' || c == '\uFF61');
    }

    //
    // to check if a string only contains US-ASCII code point
    //
    private static boolean isAllASCII(String input) {
        boolean isASCII = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c > 0x7F) {
                isASCII = false;
                break;
            }
        }
        return isASCII;
    }

    //
    // to check if a string starts with ACE-prefix
    //
    private static boolean startsWithACEPrefix(StringBuffer input){
        boolean startsWithPrefix = true;

        if(input.length() < ACE_PREFIX_LENGTH){
            return false;
        }
        for(int i = 0; i < ACE_PREFIX_LENGTH; i++){
            if(toASCIILower(input.charAt(i)) != ACE_PREFIX.charAt(i)){
                startsWithPrefix = false;
            }
        }
        return startsWithPrefix;
    }

    private static char toASCIILower(char ch){
        if('A' <= ch && ch <= 'Z'){
            return (char)(ch + 'a' - 'A');
        }
        return ch;
    }

    private static StringBuffer toASCIILower(StringBuffer input){
        StringBuffer dest = new StringBuffer();
        for(int i = 0; i < input.length();i++){
            dest.append(toASCIILower(input.charAt(i)));
        }
        return dest;
    }
}
