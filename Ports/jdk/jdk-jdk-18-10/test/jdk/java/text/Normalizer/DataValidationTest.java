/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * test
 * bug  4221795
 * summary Confirm *.icu data using ICU4J Normalizer
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.BitSet;
import java.util.StringTokenizer;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.impl.NormalizerImpl;

/**
 * This is not a test program but a data validation utility.
 * Two datafiles for Normalizer, unorm.icu and uprops.icu under
 * sun/text/resouces, are generated using generators in ICU4C 3.2 on a
 * BIG-ENDIAN machine. Before using them with java.text.Normalizer and
 * sun.text.Normalizer, you may want to check these test datafile's validation.
 * You can test datafiles using Normalizer in ICU4J 3.2. Download ICU4J 3.2 and
 * run this test program with -cp <ICU4J 3.2>.
 */
public class DataValidationTest {

    //
    // Options to be used with com.ibm.icu.text.Normalizer
    //

    /*
     * Default Unicode 3.2.0 normalization.
     *
     *   - With Corrigendum 4 fix
     *     (Different from Mustang's Normalizer.)
     *   - With Public Review Issue #29 fix
     *     (Different from Mustang's Normalizer.)
     */
    private static final int UNICODE_3_2_0 = Normalizer.UNICODE_3_2;

    /*
     * *Incomplete* Unicode 3.2.0 normalization for IDNA/StringPrep.
     *
     *   - With Corrigendum 4 fix
     *   - Without Public Review Issue #29 fix
     *
     * ICU4J's Normalizer itself doesn't support normalization for Unicode 3.2.0
     * without Corrigendum 4 fix, which is necessary for IDNA/StringPrep. It is
     * done in StringPrep. Therefore, we don't test the normlaization in this
     * test program. We merely test normalization for Unicode 3.2.0 without
     * Public Review Issue #29 fix with this test program.
     */
    private static final int UNICODE_3_2_0_BEFORE_PRI_29 =
                                 Normalizer.UNICODE_3_2 |
                                 NormalizerImpl.BEFORE_PRI_29;

    /*
     * Default normalization.
     *
     *   - Unicode 4.0.1
     *     (Different from Mustang's Normalizer.)
     *   - With Corrigendum 4 fix
     *   - With Public Review Issue #29 fix
     *     (Different from Mustang's Normalizer.)
     *
     * Because Public Review Issue #29 is fixed in Unicode 4.1.0. I think that
     * IUC4J 3.2 should not support it. But it actually supports PRI #29 fix
     * as default....
     */
    private static final int UNICODE_LATEST = 0x00;

    /*
     * Normalization without Public Review Issue #29 fix.
     *
     *   - Unicode 4.0.1
     *   - Without Corrigendum 4 fix
     *   - Without Public Review Issue #29 fix
     */
    static final int UNICODE_LATEST_BEFORE_PRI_29 =
                         NormalizerImpl.BEFORE_PRI_29;

    //
    // Conformance test datafiles
    //

    /*
     * Conformance test datafile for normalization for Unicode 3.2.0 with
     * Corrigendum 4 corrections. This is NOT an original Conformace test
     * data. Some inconvenient test cases are commented out.
     * About corrigendum 4, please refer
     *   http://www.unicode.org/versions/corrigendum4.html
     *
     * ICU4J 3.2's Normalizer itself doesn't support normalization for Unicode
     * 3.2.0 without Corrigendum 4 corrections. StringPrep helps it. So, we
     * don't test the normalization with this test program.
     */
    static final String DATA_3_2_0 = "NormalizationTest-3.2.0.Corrigendum4.txt";

    /*
     * Conformance test datafile for the latest Unicode which is supported
     * by J2SE.
     */
    static final String DATA_LATEST = "NormalizationTest-Latest.txt";

   /*
    * Decorder
    */
    static final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

   /*
    * List to pick up characters which are not listed in Part1
    */
    static BitSet charList = new BitSet(Character.MAX_CODE_POINT+1);

   /*
    * Shortcuts
    */
    static final Normalizer.Mode NFC  = com.ibm.icu.text.Normalizer.NFC;
    static final Normalizer.Mode NFD  = com.ibm.icu.text.Normalizer.NFD;
    static final Normalizer.Mode NFKC = com.ibm.icu.text.Normalizer.NFKC;
    static final Normalizer.Mode NFKD = com.ibm.icu.text.Normalizer.NFKD;
    static final Normalizer.Mode[] modes = {NFC, NFD, NFKC, NFKD};


    public static void main(String[] args) throws Exception {
        test(DATA_3_2_0, UNICODE_3_2_0);
        test(DATA_3_2_0, UNICODE_3_2_0_BEFORE_PRI_29);
        test(DATA_LATEST, UNICODE_LATEST);
        // This test started failing since ICU4J 3.6.
//      test(DATA_LATEST, UNICODE_LATEST_BEFORE_PRI_29);

        /* Unconformity test */
//      test(DATA_3_2_0, UNICODE_LATEST);
//      test(DATA_LATEST, UNICODE_3_2);
    }

    private static void test(String filename, int unicodeVer) throws Exception {

        FileInputStream fis = new FileInputStream(filename);
        BufferedReader in =
            new BufferedReader(new InputStreamReader(fis, decoder));

        System.out.println("\nStart testing with " + filename +
            " for options: " +
            (((unicodeVer & Normalizer.UNICODE_3_2) != 0) ?
                "Unicode 3.2.0" : "the latest Unicode") + ", " +
            (((unicodeVer & NormalizerImpl.BEFORE_PRI_29) != 0) ?
                "with" : "without") + " PRI #29 fix");

        int lineNo = 0;
        String text;
        String[] columns = new String[6];
        boolean part1test = false;

        while ((text = in.readLine()) != null) {
            lineNo ++;

            char c = text.charAt(0);
            if (c == '#') {
                continue;
            } else if (c == '@') {
                if (text.startsWith("@Part")) {
                    System.out.println("# Testing data in " + text);

                    if (text.startsWith("@Part1 ")) {
                        part1test = true;
                    } else {
                        part1test = false;
                    }

                    continue;
                }
            }

            prepareColumns(columns, text, filename, lineNo, part1test);

            testNFC(columns, unicodeVer, filename, lineNo);
            testNFD(columns, unicodeVer, filename, lineNo);
            testNFKC(columns, unicodeVer, filename, lineNo);
            testNFKD(columns, unicodeVer, filename, lineNo);
        }

        in.close();
        fis.close();

        if (unicodeVer == UNICODE_LATEST) {
            System.out.println("# Testing characters which are not listed in Part1");
            testRemainingChars(filename, unicodeVer);
        }
    }

    /*
     * Test for NFC
     *
     *   c2 ==  NFC(c1) ==  NFC(c2) ==  NFC(c3)
     *   c4 ==  NFC(c4) ==  NFC(c5)
     */
    private static void testNFC(String[] c, int unicodeVer,
                                String file, int line) throws Exception {
        test(2, c, 1, 3, NFC, unicodeVer, file, line);
        test(4, c, 4, 5, NFC, unicodeVer, file, line);
    }

    /*
     * Test for NFD
     *
     *   c3 ==  NFD(c1) ==  NFD(c2) ==  NFD(c3)
     *   c5 ==  NFD(c4) ==  NFD(c5)
     */
    private static void testNFD(String[] c, int unicodeVer,
                                String file, int line) throws Exception {
        test(3, c, 1, 3, NFD, unicodeVer, file, line);
        test(5, c, 4, 5, NFD, unicodeVer, file, line);
    }

    /*
     * Test for NFKC
     *
     *   c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)
     */
    private static void testNFKC(String[] c, int unicodeVer,
                                 String file, int line) throws Exception {
        test(4, c, 1, 5, NFKC, unicodeVer, file, line);
    }

    /*
     * Test for NFKD
     *
     *   c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)
     */
    private static void testNFKD(String[] c, int unicodeVer,
                                 String file, int line) throws Exception {
        test(5, c, 1, 5, NFKD, unicodeVer, file, line);
    }

    /*
     * Test for characters which aren't listed in Part1
     *
     *   X == NFC(X) == NFD(X) == NFKC(X) == NFKD(X)
     */
    private static void testRemainingChars(String file,
                                           int unicodeVer) throws Exception {
        for (int i = Character.MIN_CODE_POINT;
             i <= Character.MAX_CODE_POINT;
             i++) {
            if (!charList.get(i)) {
                String from = String.valueOf(Character.toChars(i));
                String to;

                for (int j = 0; j < modes.length; j++) {
                    Normalizer.Mode mode = modes[j];

                    to = Normalizer.normalize(from, mode, unicodeVer);
                    if (!from.equals(to)) {
                        error(mode, from, from, to, file, -1);
//                  } else {
//                      okay(mode, from, from, to, file, -1);
                    }

                    if (!Normalizer.isNormalized(from, mode, unicodeVer)) {
                        error(mode, from, file, -1);
//                  } else {
//                      okay(mode, from, file, -1);
                    }
                }
            }
        }
    }

    /*
     * Test normalize() and isNormalized()
     */
    private static void test(int col, String[] c,
                             int FROM, int TO,
                             Normalizer.Mode mode, int unicodeVer,
                             String file, int line) throws Exception {
        for (int i = FROM; i <= TO; i++) {
            String got = Normalizer.normalize(c[i], mode, unicodeVer);
            if (!c[col].equals(got)) {
                error(mode, c[i], c[col], got, file, line);
//          } else {
//              okay(mode, c[i], c[col], got, file, line);
            }

            /*
             * If the original String equals its normalized String, it means
             * that the original String is normalizerd. Thus, isNormalized()
             * should return true. And, vice versa!
             */
            if (c[col].equals(c[i])) {
                if (!Normalizer.isNormalized(c[i], mode, unicodeVer)) {
                    error(mode, c[i], file, line);
//              } else {
//                  okay(mode, c[i], file, line);
                }
            } else {
                if (Normalizer.isNormalized(c[i], mode, unicodeVer)) {
                    error(mode, c[i], file, line);
//              } else {
//                  okay(mode, c[i], file, line);
                }
            }
        }
    }

    /*
     * Generate an array of String from a line of conformance datafile.
     */
    private static void prepareColumns(String[] col, String text,
                                       String file, int line,
                                       boolean part1test) throws Exception {
        int index = text.indexOf('#');
        if (index != -1) {
            text = text.substring(0, index);
        }

        StringTokenizer st = new StringTokenizer(text, ";");
        int tokenCount = st.countTokens();
        if (tokenCount < 5) {
             throw new RuntimeException("# of tokens in datafile should be 6, but got: " + tokenCount + " at line " + line + " in " + file);
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 1; i <= 5; i++) {
            StringTokenizer tst = new StringTokenizer(st.nextToken(), " ");

            while (tst.hasMoreTokens()) {
                int code = Integer.parseInt(tst.nextToken(), 16);
                sb.append(Character.toChars(code));
            }

            col[i] = sb.toString();
            sb.setLength(0);
        }

        if (part1test) {
            charList.set(col[1].codePointAt(0));
        }
    }

    /*
     * Show an error message when normalize() didn't return the expected value.
     * (An exception is sometimes convenient. Therefore, it is commented out
     * for the moment.)
     */
    private static void error(Normalizer.Mode mode,
                              String from, String to, String got,
                              String file, int line) throws Exception {
        System.err.println("\t" + toString(mode) + ": normalize(" +
            toHexString(from) + ") doesn't equal <" + toHexString(to) +
            "> at line " + line + " in " + file + ". Got <" +
            toHexString(got) + ">.");
//      throw new RuntimeException("Normalization(" + toString(mode) + ") failed");
    }

    /*
     * Show an error message when isNormalize() didn't return the expected value.
     * (An exception is sometimes convenient. Therefore, it is commented out
     * for the moment.)
     */
    private static void error(Normalizer.Mode mode, String orig,
                              String file, int line) throws Exception {
        System.err.println("\t" + toString(mode) + ": isNormalized(" +
            toHexString(orig) + ") returned the wrong value at line " + line +
            " in " + file + ".");
//      throw new RuntimeException("Normalization(" + toString(mode) +") failed");
    }

    /*
     * (For debugging)
     * Shows a message when normalize() returned the expected value.
     */
    private static void okay(Normalizer.Mode mode,
                             String from, String to, String got,
                             String file, int line) {
        System.out.println("\t" + toString(mode) + ": normalize(" +
            toHexString(from) + ") equals <" + toHexString(to) +
            "> at line " + line + " in " + file + ". Got <" +
            toHexString(got) + ">.");
    }

    /*
     * (For debugging)
     * Shows a message when isNormalized() returned the expected value.
     */
    private static void okay(Normalizer.Mode mode, String orig,
                             String file, int line) {
        System.out.println("\t" + toString(mode) + ": isNormalized(" +
            toHexString(orig) + ") returned the correct value at line " +
            line + " in " + file + ".");
    }

    /*
     * Returns a spece-delimited hex String
     */
    private static String toHexString(String s) {
        StringBuffer sb = new StringBuffer(" ");

        for (int i = 0; i < s.length(); i++) {
            sb.append(Integer.toHexString(s.charAt(i)));
            sb.append(' ');
        }

        return sb.toString();
    }

   /*
    * Returns the name of Normalizer.Mode
    */
    private static String toString(Normalizer.Mode mode) {
        if (mode == Normalizer.NFC) {
            return "NFC";
        } else if (mode == Normalizer.NFD) {
            return "NFD";
        } else if (mode == Normalizer.NFKC) {
            return "NFKC";
        } else if (mode == Normalizer.NFKD) {
            return "NFKD";
        }

        return "unknown";
    }
}
