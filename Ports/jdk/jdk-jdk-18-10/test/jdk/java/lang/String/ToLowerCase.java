/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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
    @test
    @bug 4217441 4533872 4900935 8020037 8032012 8041791 8042589 8054307
    @summary toLowerCase should lower-case Greek Sigma correctly depending
             on the context (final/non-final).  Also it should handle
             Locale specific (lt, tr, and az) lowercasings and supplementary
             characters correctly.
*/

import java.util.Locale;

public class ToLowerCase {

    public static void main(String[] args) {
        Locale turkish = new Locale("tr", "TR");
        Locale lt = new Locale("lt"); // Lithanian
        Locale az = new Locale("az"); // Azeri

        // Greek Sigma final/non-final tests
        test("\u03A3", Locale.US, "\u03C3");
        test("LAST\u03A3", Locale.US, "last\u03C2");
        test("MID\u03A3DLE", Locale.US, "mid\u03C3dle");
        test("WORD1 \u03A3 WORD3", Locale.US, "word1 \u03C3 word3");
        test("WORD1 LAST\u03A3 WORD3", Locale.US, "word1 last\u03C2 word3");
        test("WORD1 MID\u03A3DLE WORD3", Locale.US, "word1 mid\u03C3dle word3");
        test("\u0399\u0395\u03a3\u03a5\u03a3 \u03a7\u03a1\u0399\u03a3\u03a4\u039f\u03a3", Locale.US,
             "\u03b9\u03b5\u03c3\u03c5\u03c2 \u03c7\u03c1\u03b9\u03c3\u03c4\u03bf\u03c2"); // "IESUS XRISTOS"

        // Explicit dot above for I's and J's whenever there are more accents above (Lithanian)
        test("I", lt, "i");
        test("I\u0300", lt, "i\u0307\u0300"); // "I" followed by COMBINING GRAVE ACCENT (cc==230)
        test("I\u0316", lt, "i\u0316"); // "I" followed by COMBINING GRAVE ACCENT BELOW (cc!=230)
        test("J", lt, "j");
        test("J\u0300", lt, "j\u0307\u0300"); // "J" followed by COMBINING GRAVE ACCENT (cc==230)
        test("J\u0316", lt, "j\u0316"); // "J" followed by COMBINING GRAVE ACCENT BELOW (cc!=230)
        test("\u012E", lt, "\u012F");
        test("\u012E\u0300", lt, "\u012F\u0307\u0300"); // "I (w/ OGONEK)" followed by COMBINING GRAVE ACCENT (cc==230)
        test("\u012E\u0316", lt, "\u012F\u0316"); // "I (w/ OGONEK)" followed by COMBINING GRAVE ACCENT BELOW (cc!=230)
        test("\u00CC", lt, "i\u0307\u0300");
        test("\u00CD", lt, "i\u0307\u0301");
        test("\u0128", lt, "i\u0307\u0303");
        test("I\u0300", Locale.US, "i\u0300"); // "I" followed by COMBINING GRAVE ACCENT (cc==230)
        test("J\u0300", Locale.US, "j\u0300"); // "J" followed by COMBINING GRAVE ACCENT (cc==230)
        test("\u012E\u0300", Locale.US, "\u012F\u0300"); // "I (w/ OGONEK)" followed by COMBINING GRAVE ACCENT (cc==230)
        test("\u00CC", Locale.US, "\u00EC");
        test("\u00CD", Locale.US, "\u00ED");
        test("\u0128", Locale.US, "\u0129");

        // I-dot tests
        test("\u0130", turkish, "i");
        test("\u0130", az, "i");
        test("\u0130", lt, "\u0069\u0307");
        test("\u0130", Locale.US, "\u0069\u0307");
        test("\u0130", Locale.JAPAN, "\u0069\u0307");
        test("\u0130", Locale.ROOT, "\u0069\u0307");

        // Remove dot_above in the sequence I + dot_above (Turkish and Azeri)
        test("I\u0307", turkish, "i");
        test("I\u0307", az, "i");
        test("J\u0307", turkish, "j\u0307");
        test("J\u0307", az, "j\u0307");

        // Unless an I is before a dot_above, it turns into a dotless i (Turkish and Azeri)
        test("I", turkish, "\u0131");
        test("I", az, "\u0131");
        test("I", Locale.US, "i");
        test("IABC", turkish, "\u0131abc");
        test("IABC", az, "\u0131abc");
        test("IABC", Locale.US, "iabc");

        // Supplementary character tests
        //
        // U+10400 ("\uD801\uDC00"): DESERET CAPITAL LETTER LONG I
        // U+10401 ("\uD801\uDC01"): DESERET CAPITAL LETTER LONG E
        // U+10402 ("\uD801\uDC02"): DESERET CAPITAL LETTER LONG A
        // U+10428 ("\uD801\uDC28"): DESERET SMALL LETTER LONG I
        // U+10429 ("\uD801\uDC29"): DESERET SMALL LETTER LONG E
        // U+1042A ("\uD801\uDC2A"): DESERET SMALL LETTER LONG A
        //
        // valid code point tests:
        test("\uD801\uDC00\uD801\uDC01\uD801\uDC02", Locale.US, "\uD801\uDC28\uD801\uDC29\uD801\uDC2A");
        test("\uD801\uDC00A\uD801\uDC01B\uD801\uDC02C", Locale.US, "\uD801\uDC28a\uD801\uDC29b\uD801\uDC2Ac");
        // invalid code point tests:
        test("\uD800\uD800\uD801A\uDC00\uDC00\uDC00B", Locale.US, "\uD800\uD800\uD801a\uDC00\uDC00\uDC00b");

        // lower/uppercase + surrogates
        test("a\uD801\uDC1c", Locale.ROOT, "a\uD801\uDC44");
        test("A\uD801\uDC1c", Locale.ROOT, "a\uD801\uDC44");
        test("a\uD801\uDC00\uD801\uDC01\uD801\uDC02", Locale.US, "a\uD801\uDC28\uD801\uDC29\uD801\uDC2A");
        test("A\uD801\uDC00\uD801\uDC01\uD801\uDC02", Locale.US, "a\uD801\uDC28\uD801\uDC29\uD801\uDC2A");

        // test bmp + supp1
        StringBuilder src = new StringBuilder(0x20000);
        StringBuilder exp = new StringBuilder(0x20000);
        for (int cp = 0; cp < 0x20000; cp++) {
            if (cp >= Character.MIN_HIGH_SURROGATE && cp <= Character.MAX_HIGH_SURROGATE) {
                continue;
            }
            if (cp == 0x0130) {
                // Although UnicodeData.txt has the lower case char as \u0069, it should be
                // handled with the rules in SpecialCasing.txt, i.e., \u0069\u0307 in
                // non Turkic locales.
                continue;
            }
            int lowerCase = Character.toLowerCase(cp);
            if (lowerCase == -1) {    //Character.ERROR
                continue;
            }
            src.appendCodePoint(cp);
            exp.appendCodePoint(lowerCase);
        }
        test(src.toString(), Locale.US, exp.toString());

        // test latin1
        src = new StringBuilder(0x100);
        exp = new StringBuilder(0x100);
        for (int cp = 0; cp < 0x100; cp++) {
            int lowerCase = Character.toLowerCase(cp);
            if (lowerCase == -1) {    //Character.ERROR
                continue;
            }
            src.appendCodePoint(cp);
            exp.appendCodePoint(lowerCase);
        }
        test(src.toString(), Locale.US, exp.toString());

        // test non-latin1 -> latin1
        src = new StringBuilder(0x100).append("abc");
        exp = new StringBuilder(0x100).append("abc");
        for (int cp = 0x100; cp < 0x10000; cp++) {
            int lowerCase  = Character.toLowerCase(cp);
            if (lowerCase < 0x100 && cp != '\u0130') {
                src.appendCodePoint(cp);
                exp.appendCodePoint(lowerCase);
            }
        }
        test(src.toString(), Locale.US, exp.toString());
    }

    static void test(String in, Locale locale, String expected) {
        test0(in, locale,expected);
        for (String[] ss :  new String[][] {
                                new String[] {"abc",      "abc"},
                                new String[] {"aBc",      "abc"},
                                new String[] {"ABC",      "abc"},
                                new String[] {"ab\u4e00", "ab\u4e00"},
                                new String[] {"aB\u4e00", "ab\u4e00"},
                                new String[] {"AB\u4e00", "ab\u4e00"},
                                new String[] {"ab\uD800\uDC00", "ab\uD800\uDC00"},
                                new String[] {"aB\uD800\uDC00", "ab\uD800\uDC00"},
                                new String[] {"AB\uD800\uDC00", "ab\uD800\uDC00"},
                                new String[] {"ab\uD801\uDC1C", "ab\uD801\uDC44"},
                                new String[] {"aB\uD801\uDC1C", "ab\uD801\uDC44"},
                                new String[] {"AB\uD801\uDC1C", "ab\uD801\uDC44"},

                            }) {
            test0(ss[0] + " " + in, locale, ss[1] + " " + expected);
            test0(in + " " + ss[0], locale, expected + " " + ss[1]);
        }
    }

    static void test0(String in, Locale locale, String expected) {
        String result = in.toLowerCase(locale);
        if (!result.equals(expected)) {
            System.err.println("input: " + in + ", locale: " + locale +
                    ", expected: " + expected + ", actual: " + result);
            throw new RuntimeException();
        }
    }
}
