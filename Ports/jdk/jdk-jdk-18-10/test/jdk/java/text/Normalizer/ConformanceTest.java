/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug  4221795 6565620 6959267 7070436 7198195 8032446 8174270 8221431 8239383
 * @summary Confirm Normalizer's fundamental behavior
 * @library /lib/testlibrary/java/lang
 * @modules java.base/sun.text java.base/jdk.internal.icu.text
 * @compile -XDignore.symbol.file ConformanceTest.java
 * @run main/timeout=3000 ConformanceTest
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.BitSet;
import java.util.StringTokenizer;

import jdk.internal.icu.text.NormalizerBase;

/*
 * Conformance test for java.text.Normalizer and sun.text.Normalizer.
 */
public class ConformanceTest {

    //
    // Options to be used with sun.text.Normalizer
    //

    /*
     * Default Unicode 3.2.0 normalization. (Provided for IDNA/StringPrep)
     *
     *   - Without Corrigendum 4 fix
     *     (Different from ICU4J 3.2's Normalizer.)
     *   - Without Public Review Issue #29 fix
     *     (Different from ICU4J 3.2's Normalizer.)
     */
    private static final int UNICODE_3_2_0 = sun.text.Normalizer.UNICODE_3_2;

    /*
     * Original Unicode 3.2.0 normalization. (Provided for testing only)
     *
     *   - With Corrigendum 4 fix
     *   - With Public Revilew Issue #29 fix
     */
    private static final int UNICODE_3_2_0_ORIGINAL =
                                 NormalizerBase.UNICODE_3_2;

    /*
     * Default normalization. In JDK 6,
     *   - Unicode 4.0.0
     *   - With Corrigendum 4 fix
     *   - Without Public Review Issue #29 fix
     *
     * In JDK 7,
     *   - Unicode 5.1.0
     *     (Different from ICU4J 3.2's Normalizer.)
     *   - With Corrigendum 4 fix
     *   - With Public Review Issue #29 fix
     *
     * In JDK 8,
     *   - Unicode 6.1.0
     *   - With Corrigendum 4 fix
     *   - With Public Review Issue #29 fix
     *
     *  When we support Unicode 4.1.0 or later, we need to do normalization
     *  with Public Review Issue #29 fix. For more details of PRI #29, see
     *  http://unicode.org/review/pr-29.html .
     */
    private static final int UNICODE_LATEST = NormalizerBase.UNICODE_LATEST;

    //
    // Conformance test datafiles
    //

    /*
     * Conformance test datafile for Unicode 3.2.0 with Corrigendum4
     * corrections.
     * This testdata is for sun.text.Normalize(UNICODE_3_2)
     *
     * This is NOT an original Conformace test data. Some inconvenient test
     * cases are commented out. About corrigendum 4, please refer
     *   http://www.unicode.org/review/resolved-pri.html#pri29
     *
     */
    static final String DATA_3_2_0_CORRIGENDUM =
                            "NormalizationTest-3.2.0.Corrigendum4.txt";

    /*
     * Conformance test datafile for Unicode 3.2.0 without Corrigendum4
     * corrections. This is the original Conformace test data.
     *
     * This testdata is for sun.text.Normalize(UNICODE_3_2_IDNA)
     */
    static final String DATA_3_2_0 = "NormalizationTest-3.2.0.txt";

    /*
     * Conformance test datafile for the latest Unicode which is supported
     * by J2SE.
     * Unicode 4.0.0 is the latest version in JDK 5.0 and JDK 6. Unicode 5.1.0
     * in JDK 7, and 6.1.0 in JDK 8. This Unicode can be used via both
     * java.text.Normalizer and sun.text.Normalizer.
     *
     * This testdata is for sun.text.Normalize(UNICODE_LATEST)
     */
    static final String DATA_LATEST = "NormalizationTest.txt";

    /*
     * Conformance test datafile in ICU4J 3.2.
     */
    static final String DATA_ICU = "ICUNormalizationTest.txt";

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
    private static final java.text.Normalizer.Form NFC  =
        java.text.Normalizer.Form.NFC;
    private static final java.text.Normalizer.Form NFD  =
        java.text.Normalizer.Form.NFD;
    private static final java.text.Normalizer.Form NFKC =
        java.text.Normalizer.Form.NFKC;
    private static final java.text.Normalizer.Form NFKD =
        java.text.Normalizer.Form.NFKD;
    static final java.text.Normalizer.Form[] forms = {NFC, NFD, NFKC, NFKD};


    static TestNormalizer normalizer;

    public static void main(String[] args) throws Exception {
        ConformanceTest ct = new ConformanceTest();
        ct.test();
    }

    void test() throws Exception {
        normalizer = new testJavaNormalizer();
        test(DATA_LATEST, UNICODE_LATEST);

        normalizer = new testSunNormalizer();
        test(DATA_3_2_0_CORRIGENDUM, UNICODE_3_2_0);
        test(DATA_LATEST, UNICODE_LATEST);
        test(DATA_ICU, UNICODE_LATEST);

        /* Unconformity test */
//      test(DATA_3_2_0, UNICODE_LATEST);
//      test(DATA_LATEST, UNICODE_3_2_0);
    }

    /*
     * Main routine of conformance test
     */
    private static void test(String filename, int unicodeVer) throws Exception {

        File  f = filename.equals(DATA_LATEST) ?
            UCDFiles.NORMALIZATION_TEST.toFile() :
            new File(System.getProperty("test.src", "."), filename);
        FileInputStream fis = new FileInputStream(f);
        BufferedReader in =
            new BufferedReader(new InputStreamReader(fis, decoder));

        System.out.println("\nStart testing for " + normalizer.name +
            " with " + filename + " for options: " +
            (((unicodeVer & NormalizerBase.UNICODE_3_2) != 0) ?
                "Unicode 3.2.0" : "the latest Unicode"));

        int lineNo = 0;
        String text;
        boolean part1test = false;
        boolean part1testExists = false;
        String[] columns = new String[6];

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
                        part1testExists = true;
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

        if (part1testExists) {
            System.out.println("# Testing characters which are not listed in Part1");
            testRemainingChars(filename, unicodeVer);
            part1testExists = false;
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

                for (int j = 0; j < forms.length; j++) {
                    java.text.Normalizer.Form form = forms[j];

                    to = normalizer.normalize(from, form, unicodeVer);
                    if (!from.equals(to)) {
                        error(form, from, from, to, file, -1);
//                  } else {
//                      okay(form, from, from, to, file, -1);
                    }

                    if (!normalizer.isNormalized(from, form, unicodeVer)) {
                        error(form, from, file, -1);
//                  } else {
//                      okay(form, from, file, -1);
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
                             java.text.Normalizer.Form form, int unicodeVer,
                             String file, int line) throws Exception {
        for (int i = FROM; i <= TO; i++) {
            String got = normalizer.normalize(c[i], form, unicodeVer);
            if (!c[col].equals(got)) {
                error(form, c[i], c[col], got, file, line);
//          } else {
//              okay(form, c[i], c[col], got, file, line);
            }

            /*
             * If the original String equals its normalized String, it means
             * that the original String is normalizerd. Thus, isNormalized()
             * should return true. And, vice versa!
             */
            if (c[col].equals(c[i])) {
                if (!normalizer.isNormalized(c[i], form, unicodeVer)) {
                    error(form, c[i], file, line);
//              } else {
//                  okay(form, c[i], file, line);
                }
            } else {
                if (normalizer.isNormalized(c[i], form, unicodeVer)) {
                    error(form, c[i], file, line);
//              } else {
//                  okay(form, c[i], file, line);
                }
            }
        }
    }

    /*
     * Generate an array of String from a line of conformance datafile.
     */
    private static void prepareColumns(String[] cols, String text,
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

            cols[i] = sb.toString();
            sb.setLength(0);
        }

        if (part1test) {
            charList.set(cols[1].codePointAt(0));
        }
    }

    /*
     * Show an error message when normalize() didn't return the expected value.
     * (An exception is sometimes convenient. Therefore, it is commented out
     * for the moment.)
     */
    private static void error(java.text.Normalizer.Form form,
                              String from, String to, String got,
                              String file, int line) throws Exception {
        System.err.println("-\t" + form.toString() + ": normalize(" +
            toHexString(from) + ") doesn't equal <" + toHexString(to) +
            "> at line " + line + " in " + file + ". Got [" +
            toHexString(got) + "]");
        throw new RuntimeException("Normalization(" + form.toString() + ") failed");
    }

    /*
     * Show an error message when isNormalize() didn't return the expected
     * value.
     * (An exception is sometimes convenient. Therefore, it is commented out
     * for the moment.)
     */
    private static void error(java.text.Normalizer.Form form, String s,
                              String file, int line) throws Exception {
        System.err.println("\t" + form.toString() + ": isNormalized(" +
            toHexString(s) + ") returned the wrong value at line " + line +
            " in " + file);
        throw new RuntimeException("Normalization(" + form.toString() +") failed");
    }

    /*
     * (For debugging)
     * Shows a message when normalize() returned the expected value.
     */
    private static void okay(java.text.Normalizer.Form form,
                             String from, String to, String got,
                             String file, int line) {
        System.out.println("\t" + form.toString() + ": normalize(" +
            toHexString(from) + ") equals <" + toHexString(to) +
            "> at line " + line + " in " + file + ". Got [" +
            toHexString(got) + "]");
    }

    /*
     * (For debugging)
     * Shows a message when isNormalized() returned the expected value.
     */
    private static void okay(java.text.Normalizer.Form form, String s,
                             String file, int line) {
        System.out.println("\t" + form.toString() + ": isNormalized(" +
            toHexString(s) + ") returned the correct value at line " +
            line + " in " + file);
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
     * Abstract class to call each Normalizer in java.text or sun.text.
     */
    private abstract class TestNormalizer {
        String name;

        TestNormalizer(String str) {
            name = str;
        }

        String getNormalizerName() {
            return name;
        }

        abstract String normalize(CharSequence cs,
                                  java.text.Normalizer.Form form,
                                  int option);

        abstract boolean isNormalized(CharSequence cs,
                                     java.text.Normalizer.Form form,
                                     int option);
    }

    /*
     * For java.text.Normalizer
     *   - normalize(CharSequence, Normalizer.Form)
     *   - isNormalized(CharSequence, Normalizer.Form)
     */
    private class testJavaNormalizer extends TestNormalizer {
        testJavaNormalizer() {
            super("java.text.Normalizer");
        }

        String normalize(CharSequence cs,
                         java.text.Normalizer.Form form,
                         int option) {
            return java.text.Normalizer.normalize(cs, form);
        }

        boolean isNormalized(CharSequence cs,
                             java.text.Normalizer.Form form,
                             int option) {
            return java.text.Normalizer.isNormalized(cs, form);
        }
    }

    /*
     * For sun.text.Normalizer
     *   - normalize(CharSequence, Normalizer.Form, int)
     *   - isNormalized(CharSequence, Normalizer.Form, int)
     */
    private class testSunNormalizer extends TestNormalizer {
        testSunNormalizer() {
            super("sun.text.Normalizer");
        }

        String normalize(CharSequence cs,
                         java.text.Normalizer.Form form,
                         int option) {
            return sun.text.Normalizer.normalize(cs, form, option);
        }

        boolean isNormalized(CharSequence cs,
                             java.text.Normalizer.Form form,
                             int option) {
            return sun.text.Normalizer.isNormalized(cs, form, option);
        }
    }
}
