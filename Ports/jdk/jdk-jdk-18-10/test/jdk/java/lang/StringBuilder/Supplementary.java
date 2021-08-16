/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @test
 * @bug 4533872 4915683 4985217 5017280
 * @summary Unit tests for supplementary character support (JSR-204)
 */

public class Supplementary {

    public static void main(String[] args) {
        test1();        // Test for codePointAt(int index)
        test2();        // Test for codePointBefore(int index)
        test3();        // Test for reverse()
        test4();        // Test for appendCodePoint(int codePoint)
        test5();        // Test for codePointCount(int beginIndex, int endIndex)
        test6();        // Test for offsetByCodePoints(int index, int offset)
        testDontReadOutOfBoundsTrailingSurrogate();
    }

    /* Text strings which are used as input data.
     * The comment above each text string means the index of each 16-bit char
     * for convenience.
     */
    static final String[] input = {
      /*                               111     1     111111     22222
         0123     4     5678     9     012     3     456789     01234 */
        "abc\uD800\uDC00def\uD800\uD800ab\uD800\uDC00cdefa\uDC00bcdef",
      /*                          1     1111     1111     1     222
         0     12345     6789     0     1234     5678     9     012     */
        "\uD800defg\uD800hij\uD800\uDC00klm\uDC00nop\uDC00\uD800rt\uDC00",
      /*                          11     1     1111     1     112     222
         0     12345     6     78901     2     3456     7     890     123     */
        "\uDC00abcd\uDBFF\uDFFFefgh\uD800\uDC009ik\uDC00\uDC00lm\uDC00no\uD800",
      /*                                    111     111111     1 22     2
         0     1     2345     678     9     012     345678     9 01     2     */
        "\uD800\uDC00!#$\uD800%&\uD800\uDC00;+\uDC00<>;=^\uDC00\\@\uD800\uDC00",

        // includes an undefined supplementary character in Unicode 4.0.0
      /*                                    1     11     1     1111     1
         0     1     2345     6     789     0     12     3     4567     8     */
        "\uDB40\uDE00abc\uDE01\uDB40de\uDB40\uDE02f\uDB40\uDE03ghi\uDB40\uDE02",
    };


    /* Expected results for:
     *     test1(): for codePointAt()
     *
     * Each character in each array is the golden data for each text string
     * in the above input data. For example, the first data in each array is
     * for the first input string.
     */
    static final int[][] golden1 = {
        {'a',    0xD800, 0xDC00,  0x10000, 0xE0200}, // codePointAt(0)
        {0xD800, 0x10000, 'g',    0xDC00,  0xE0202}, // codePointAt(9)
        {'f',    0xDC00,  0xD800, 0xDC00,  0xDE02},  // codePointAt(length-1)
    };

    /*
     * Test for codePointAt(int index) method
     */
    static void test1() {

        for (int i = 0; i < input.length; i++) {
            StringBuilder sb = new StringBuilder(input[i]);

            /*
             * Normal case
             */
            testCodePoint(At, sb, 0, golden1[0][i]);
            testCodePoint(At, sb, 9, golden1[1][i]);
            testCodePoint(At, sb, sb.length()-1, golden1[2][i]);

            /*
             * Abnormal case - verify that an exception is thrown.
             */
            testCodePoint(At, sb, -1);
            testCodePoint(At, sb, sb.length());
        }
    }


    /* Expected results for:
     *     test2(): for codePointBefore()
     *
     * Each character in each array is the golden data for each text string
     * in the above input data. For example, the first data in each array is
     * for the first input string.
     */
    static final int[][] golden2 = {
        {'a',    0xD800, 0xDC00,  0xD800,  0xDB40},  // codePointBefore(1)
        {0xD800, 'l',    0x10000, 0xDC00,  0xDB40},  // codePointBefore(13)
        {'f',    0xDC00, 0xD800,  0x10000, 0xE0202}, // codePointBefore(length)
    };

    /*
     * Test for codePointBefore(int index) method
     */
    static void test2() {

        for (int i = 0; i < input.length; i++) {
            StringBuilder sb = new StringBuilder(input[i]);

            /*
             * Normal case
             */
            testCodePoint(Before, sb, 1, golden2[0][i]);
            testCodePoint(Before, sb, 13, golden2[1][i]);
            testCodePoint(Before, sb, sb.length(), golden2[2][i]);

            /*
             * Abnormal case - verify that an exception is thrown.
             */
            testCodePoint(Before, sb, 0);
            testCodePoint(Before, sb, sb.length()+1);
        }
    }


    /* Expected results for:
     *     test3(): for reverse()
     *
     * Unlike golden1 and golden2, each array is the golden data for each text
     * string in the above input data. For example, the first array is  for
     * the first input string.
     */
    static final String[] golden3 = {
        "fedcb\uDC00afedc\uD800\uDC00ba\uD800\uD800fed\uD800\uDC00cba",
        "\uDC00tr\uD800\uDC00pon\uDC00mlk\uD800\uDC00jih\uD800gfed\uD800",
        "\uD800on\uDC00ml\uDC00\uDC00ki9\uD800\uDC00hgfe\uDBFF\uDFFFdcba\uDC00",
        "\uD800\uDC00@\\\uDC00^=;><\uDC00+;\uD800\uDC00&%\uD800$#!\uD800\uDC00",

        // includes an undefined supplementary character in Unicode 4.0.0
        "\uDB40\uDE02ihg\uDB40\uDE03f\uDB40\uDE02ed\uDB40\uDE01cba\uDB40\uDE00",
    };

    // Additional input data & expected result for test3()
    static final String[][] testdata1 = {
        {"a\uD800\uDC00", "\uD800\uDC00a"},
        {"a\uDC00\uD800", "\uD800\uDC00a"},
        {"\uD800\uDC00a", "a\uD800\uDC00"},
        {"\uDC00\uD800a", "a\uD800\uDC00"},
        {"\uDC00\uD800\uD801", "\uD801\uD800\uDC00"},
        {"\uDC00\uD800\uDC01", "\uD800\uDC01\uDC00"},
        {"\uD801\uD800\uDC00", "\uD800\uDC00\uD801"},
        {"\uD800\uDC01\uDC00", "\uDC00\uD800\uDC01"},
        {"\uD800\uDC00\uDC01\uD801", "\uD801\uDC01\uD800\uDC00"},
    };

    /*
     * Test for reverse() method
     */
    static void test3() {
        for (int i = 0; i < input.length; i++) {
            StringBuilder sb = new StringBuilder(input[i]).reverse();

            check(!golden3[i].equals(sb.toString()),
                 "reverse() for <" + toHexString(input[i]) + ">",
                 sb, golden3[i]);
        }

        for (int i = 0; i < testdata1.length; i++) {
            StringBuilder sb = new StringBuilder(testdata1[i][0]).reverse();

            check(!testdata1[i][1].equals(sb.toString()),
                 "reverse() for <" + toHexString(testdata1[i][0]) + ">",
                 sb, testdata1[i][1]);
        }
    }

    /**
     * Test for appendCodePoint() method
     */
    static void test4() {
        for (int i = 0; i < input.length; i++) {
            String s = input[i];
            StringBuilder sb = new StringBuilder();
            int c;
            for (int j = 0; j < s.length(); j += Character.charCount(c)) {
                c = s.codePointAt(j);
                StringBuilder rsb = sb.appendCodePoint(c);
                check(sb != rsb, "appendCodePoint returned a wrong object");
                int sbc = sb.codePointAt(j);
                check(sbc != c, "appendCodePoint("+j+") != c", sbc, c);
            }
            check(!s.equals(sb.toString()),
                  "appendCodePoint() produced a wrong result with input["+i+"]");
        }

        // test exception
        testAppendCodePoint(-1, IllegalArgumentException.class);
        testAppendCodePoint(Character.MAX_CODE_POINT+1, IllegalArgumentException.class);
    }

    /**
     * Test codePointCount(int, int)
     *
     * This test case assumes that
     * Character.codePointCount(CharSequence, int, int) works
     * correctly.
     */
    static void test5() {
        for (int i = 0; i < input.length; i++) {
            String s = input[i];
            StringBuilder sb = new StringBuilder(s);
            int length = sb.length();
            for (int j = 0; j <= length; j++) {
                int result = sb.codePointCount(j, length);
                int expected = Character.codePointCount(sb, j, length);
                check(result != expected, "codePointCount(input["+i+"], "+j+", "+length+")",
                      result, expected);
            }
            for (int j = length; j >= 0; j--) {
                int result = sb.codePointCount(0, j);
                int expected = Character.codePointCount(sb, 0, j);
                check(result != expected, "codePointCount(input["+i+"], 0, "+j+")",
                      result, expected);
            }

            // test exceptions
            testCodePointCount(null, 0, 0, NullPointerException.class);
            testCodePointCount(sb, -1, length, IndexOutOfBoundsException.class);
            testCodePointCount(sb, 0, length+1, IndexOutOfBoundsException.class);
            testCodePointCount(sb, length, length-1, IndexOutOfBoundsException.class);
        }
    }

    /**
     * Test offsetByCodePoints(int, int)
     *
     * This test case assumes that
     * Character.codePointCount(CharSequence, int, int) works
     * correctly.
     */
    static void test6() {
        for (int i = 0; i < input.length; i++) {
            String s = input[i];
            StringBuilder sb = new StringBuilder(s);
            int length = s.length();
            for (int j = 0; j <= length; j++) {
                int nCodePoints = Character.codePointCount(sb, j, length);
                int result = sb.offsetByCodePoints(j, nCodePoints);
                check(result != length,
                      "offsetByCodePoints(input["+i+"], "+j+", "+nCodePoints+")",
                      result, length);
                result = sb.offsetByCodePoints(length, -nCodePoints);
                int expected = j;
                if (j > 0 && j < length) {
                    int cp = sb.codePointBefore(j+1);
                    if (Character.isSupplementaryCodePoint(cp)) {
                        expected--;
                    }
                }
                check(result != expected,
                      "offsetByCodePoints(input["+i+"], "+j+", "+(-nCodePoints)+")",
                      result, expected);
            }
            for (int j = length; j >= 0; j--) {
                int nCodePoints = Character.codePointCount(sb, 0, j);
                int result = sb.offsetByCodePoints(0, nCodePoints);
                int expected = j;
                if (j > 0 && j < length) {
                    int cp = sb.codePointAt(j-1);
                     if (Character.isSupplementaryCodePoint(cp)) {
                        expected++;
                    }
                }
                check(result != expected,
                      "offsetByCodePoints(input["+i+"], 0, "+nCodePoints+")",
                      result, expected);
                result = sb.offsetByCodePoints(j, -nCodePoints);
                check(result != 0,
                      "offsetBycodePoints(input["+i+"], "+j+", "+(-nCodePoints)+")",
                      result, 0);
            }

            // test exceptions
            testOffsetByCodePoints(null, 0, 0, NullPointerException.class);
            testOffsetByCodePoints(sb, -1, length, IndexOutOfBoundsException.class);
            testOffsetByCodePoints(sb, 0, length+1, IndexOutOfBoundsException.class);
            testOffsetByCodePoints(sb, 1, -2, IndexOutOfBoundsException.class);
            testOffsetByCodePoints(sb, length, length-1, IndexOutOfBoundsException.class);
            testOffsetByCodePoints(sb, length, -(length+1), IndexOutOfBoundsException.class);
        }
    }

    static void testDontReadOutOfBoundsTrailingSurrogate() {
        StringBuilder sb = new StringBuilder();
        int suppl = Character.MIN_SUPPLEMENTARY_CODE_POINT;
        sb.appendCodePoint(suppl);
        check(sb.codePointAt(0) != (int) suppl,
              "codePointAt(0)", sb.codePointAt(0), suppl);
        check(sb.length() != 2, "sb.length()");
        sb.setLength(1);
        check(sb.length() != 1, "sb.length()");
        check(sb.codePointAt(0) != Character.highSurrogate(suppl),
              "codePointAt(0)",
              sb.codePointAt(0), Character.highSurrogate(suppl));
    }

    static final boolean At = true, Before = false;

    static void testCodePoint(boolean isAt, StringBuilder sb, int index, int expected) {
        int c = isAt ? sb.codePointAt(index) : sb.codePointBefore(index);

        check(c != expected,
              "codePoint" + (isAt ? "At" : "Before") + "(" + index + ") for <"
              + sb + ">", c, expected);
    }

    static void testCodePoint(boolean isAt, StringBuilder sb, int index) {
        boolean exceptionOccurred = false;

        try {
            int c = isAt ? sb.codePointAt(index) : sb.codePointBefore(index);
        }
        catch (StringIndexOutOfBoundsException e) {
            exceptionOccurred = true;
        }
        check(!exceptionOccurred,
              "codePoint" + (isAt ? "At" : "Before") + "(" + index + ") for <"
              + sb + "> should throw StringIndexOutOfBoundsPointerException.");
    }

    static void testAppendCodePoint(int codePoint, Class expectedException) {
        try {
            new StringBuilder().appendCodePoint(codePoint);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Error: Unexpected exception", e);
        }
        check(true, "appendCodePoint(" + toHexString(codePoint) + ") didn't throw "
              + expectedException.getName());
    }

    static void testCodePointCount(StringBuilder sb, int beginIndex, int endIndex,
                                   Class expectedException) {
        try {
            int n = sb.codePointCount(beginIndex, endIndex);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Error: Unexpected exception", e);
        }
        check(true, "codePointCount() didn't throw " + expectedException.getName());
    }

    static void testOffsetByCodePoints(StringBuilder sb, int index, int offset,
                                       Class expectedException) {
        try {
            int n = sb.offsetByCodePoints(index, offset);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Error: Unexpected exception", e);
        }
        check(true, "offsetByCodePoints() didn't throw " + expectedException.getName());
    }

    static void check(boolean err, String msg) {
        if (err) {
            throw new RuntimeException("Error: " + msg);
        }
    }

    static void check(boolean err, String s, int got, int expected) {
        if (err) {
            throw new RuntimeException("Error: " + s
                                       + " returned an unexpected value. got "
                                       + toHexString(got)
                                       + ", expected "
                                       + toHexString(expected));
        }
    }

    static void check(boolean err, String s, StringBuilder got, String expected) {
        if (err) {
            throw new RuntimeException("Error: " + s
                                       + " returned an unexpected value. got <"
                                       + toHexString(got.toString())
                                       + ">, expected <"
                                       + toHexString(expected)
                                       + ">");
        }
    }

    private static String toHexString(int c) {
        return "0x" + Integer.toHexString(c);
    }

    private static String toHexString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            sb.append(" 0x");
            if (c < 0x10) sb.append('0');
            if (c < 0x100) sb.append('0');
            if (c < 0x1000) sb.append('0');
            sb.append(Integer.toHexString(c));
        }
        sb.append(' ');
        return sb.toString();
    }
}
