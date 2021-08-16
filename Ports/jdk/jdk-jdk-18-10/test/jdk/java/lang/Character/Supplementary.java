/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4533872 4985214 4985217 4993841 5017268 5017280
 * @summary Unit tests for supplementary character support (JSR-204)
 * @compile Supplementary.java
 * @run main/timeout=600 Supplementary
 */

public class Supplementary {
    private static final char MIN_HIGH = '\uD800';
    private static final char MAX_HIGH = '\uDBFF';
    private static final char MIN_LOW = MAX_HIGH + 1;
    private static final char MAX_LOW = '\uDFFF';
    private static final int MIN_CODE_POINT = 0x000000;
    private static final int MIN_SUPPLEMENTARY = 0x010000;
    private static final int MAX_SUPPLEMENTARY = 0x10ffff;

    public static void main(String[] args) {
        // Do not change the order of test method calls since there
        // are some interdependencies.

        testConstants();

        test00();

        // Store all Unicode code points, except for surrogate code
        // points, in cu[] through the loops below. Then, use the data
        // for code point/code unit conversion and other tests later.
        char[] cu = new char[(MAX_SUPPLEMENTARY+1) * 2];
        int length = test01(cu);

        String str = new String(cu, 0, length);
        cu = null;
        test02(str);
        test03(str.toCharArray());
        test04(str);
        test05(str);

        // Test for toString(int)
        test06();

        // Test unpaired surrogates
        testUnpaired();

        // Test exceptions
        testExceptions00();
        testExceptions01(str);
        testExceptions02(str.toCharArray());
    }

    static void testConstants() {
        if (Character.MIN_HIGH_SURROGATE != MIN_HIGH) {
            constantError("MIN_HIGH_SURROGATE", Character.MIN_HIGH_SURROGATE, MIN_HIGH);
        }
        if (Character.MAX_HIGH_SURROGATE != MAX_HIGH) {
            constantError("MAX_HIGH_SURROGATE", Character.MAX_HIGH_SURROGATE, MAX_HIGH);
        }
        if (Character.MIN_LOW_SURROGATE != MIN_LOW) {
            constantError("MIN_LOW_SURROGATE", Character.MIN_LOW_SURROGATE, MIN_LOW);
        }
        if (Character.MAX_LOW_SURROGATE != MAX_LOW) {
            constantError("MAX_LOW_SURROGATE", Character.MAX_LOW_SURROGATE, MAX_LOW);
        }
        if (Character.MIN_SURROGATE != MIN_HIGH) {
            constantError("MIN_SURROGATE", Character.MIN_SURROGATE, MIN_HIGH);
        }
        if (Character.MAX_SURROGATE != MAX_LOW) {
            constantError("MAX_SURROGATE", Character.MAX_SURROGATE, MAX_LOW);
        }
        if (Character.MIN_SUPPLEMENTARY_CODE_POINT != MIN_SUPPLEMENTARY) {
            constantError("MIN_SUPPLEMENTARY_CODE_POINT",
                          Character.MIN_SUPPLEMENTARY_CODE_POINT, MIN_SUPPLEMENTARY);
        }
        if (Character.MIN_CODE_POINT != MIN_CODE_POINT) {
            constantError("MIN_CODE_POINT", Character.MIN_CODE_POINT, MIN_CODE_POINT);
        }
        if (Character.MAX_CODE_POINT != MAX_SUPPLEMENTARY) {
            constantError("MAX_CODE_POINT", Character.MAX_CODE_POINT, MAX_SUPPLEMENTARY);
        }
    }

    static void constantError(String name, int value, int expectedValue) {
        throw new RuntimeException("Character." + name + " has a wrong value: got "
                                   + toHexString(value)
                                   + ", expected " + toHexString(expectedValue));
    }

    /*
     * Test isValidCodePoint(int)
     *      isSupplementaryCodePoint(int)
     *      charCount(int)
     */
    static void test00() {
        for (int cp = -MAX_SUPPLEMENTARY; cp <= MAX_SUPPLEMENTARY*2; cp++) {
            boolean isValid = cp >= 0 && cp <= MAX_SUPPLEMENTARY;
            if (Character.isValidCodePoint(cp) != isValid) {
                throw new RuntimeException("isValidCodePoint failed with "
                                           + toHexString(cp));
            }
            boolean isSupplementary = cp >= MIN_SUPPLEMENTARY && cp <= MAX_SUPPLEMENTARY;
            if (Character.isSupplementaryCodePoint(cp) != isSupplementary) {
                throw new RuntimeException("isSupplementaryCodePoint failed with "
                                           + toHexString(cp));
            }
            int len = Character.charCount(cp);
            if (isValid) {
                if ((isSupplementary && len != 2)
                    || (!isSupplementary && len != 1)) {
                    throw new RuntimeException("wrong character length "+len+" for "
                                               + toHexString(cp));
                }
            } else if (len != 1 && len != 2) {
                throw new RuntimeException("wrong character length "+len+" for "
                                           + toHexString(cp));
            }
        }
    }

    /**
     * Test toChar(int)
     *      toChar(int, char[], int)
     *      isHighSurrogate(char)
     *      isLowSurrogate(char)
     *      isSurrogatePair(int, int)
     *
     * While testing those methods, this method generates all Unicode
     * code points (except for surrogate code points) and store them
     * in cu.
     *
     * @return the number of code units generated in cu
     */
    static int test01(char[] cu) {
        int index = 0;
        // Test toChar(int)
        //      toChar(int, char[], int)
        //      isHighSurrogate(char)
        //      isLowSurrogate(char)
        // with BMP code points
        for (int i = 0; i <= Character.MAX_VALUE; i++) {
            char[] u = Character.toChars(i);
            if (u.length != 1 || u[0] != i) {
                throw new RuntimeException("wrong toChars(int) result for BMP: "
                                           + toHexString("u", u));
            }
            int n = Character.toChars(i, cu, index);
            if (n != 1 || cu[index] != i) {
                throw new RuntimeException("wrong toChars(int, char[], int) result for BMP:"
                                           + " len=" + n
                                           + ", cu["+index+"]="+toHexString(cu[index]));
            }
            boolean isHigh = i >= MIN_HIGH && i <= MAX_HIGH;
            if (Character.isHighSurrogate((char) i) != isHigh) {
                throw new RuntimeException("wrong high-surrogate test for "
                                           + toHexString(i));
            }
            boolean isLow = i >= MIN_LOW && i <= MAX_LOW;
            if (Character.isLowSurrogate((char)i) != isLow) {
                throw new RuntimeException("wrong low-surrogate test for "
                                           + toHexString(i));
            }
            if (!isHigh && !isLow) {
                index++;
            }
        }

        // Test isSurrogatePair with all surrogate pairs
        // Test toChars(int)
        //      toChars(int, char[], int)
        // with all supplementary characters
        int supplementary = MIN_SUPPLEMENTARY;
        for (int i = Character.MAX_VALUE/2; i <= Character.MAX_VALUE; i++) {
            char hi = (char) i;
            boolean isHigh = Character.isHighSurrogate(hi);

            for (int j = Character.MAX_VALUE/2; j <= Character.MAX_VALUE; j++) {
                char lo = (char) j;
                boolean isLow = Character.isLowSurrogate(lo);
                boolean isSurrogatePair = isHigh && isLow;
                if (Character.isSurrogatePair(hi, lo) != isSurrogatePair) {
                    throw new RuntimeException("wrong surrogate pair test for hi="
                                               + toHexString(hi)
                                               + ", lo="+toHexString(lo));
                }
                if (isSurrogatePair) {
                    int cp = Character.toCodePoint(hi, lo);
                    if (cp != supplementary) {
                        throw new RuntimeException("wrong code point: got "
                                                   + toHexString(cp)
                                                   + ", expected="
                                                   + toHexString(supplementary));
                    }
                    char[] u = Character.toChars(cp);
                    if (u.length != 2 || u[0] != hi || u[1] != lo) {
                        throw new RuntimeException("wrong toChars(int) result for supplementary: "+
                                                   toHexString("u", u));
                    }
                    int n = Character.toChars(cp, cu, index);
                    if (n != 2 || cu[index] != hi || cu[index+1] != lo) {
                        throw new RuntimeException("wrong toChars(int, char[], int) result "
                                           + "for supplementary: len=" + n
                                           + ", cu["+index+"]=" + toHexString(cu[index])
                                           + ", cu["+(index+1)+"]=" + toHexString(cu[index+1]));
                    }
                    index += n;
                    supplementary++;
                }
            }
        }
        if (supplementary != MAX_SUPPLEMENTARY + 1) {
            throw new RuntimeException("wrong supplementary count (supplementary="
                                       + toHexString(supplementary)+")");
        }

        int nCodeUnits = Character.MAX_VALUE + 1 - (MAX_LOW - MIN_HIGH + 1)
                         + ((MAX_SUPPLEMENTARY - MIN_SUPPLEMENTARY + 1) * 2);
        if (index != nCodeUnits) {
            throw new RuntimeException("wrong number of code units: " + index
                                       + ", expected " + nCodeUnits);
        }
        return index;
    }

    /**
     * Test codePointAt(CharSequence, int)
     *      codePointBefore(CharSequence, int)
     */
    static void test02(CharSequence cs) {
        int cp = 0;
        int ch;
        for (int i = 0; i < cs.length(); i += Character.charCount(ch)) {
            ch = Character.codePointAt(cs, i);
            if (ch != cp) {
                throw new RuntimeException("wrong codePointAt(CharSequence, "+i+") value: got "
                                           + toHexString(ch)
                                           + ", expected "+toHexString(cp));
            }
            cp++;
            // Skip surrogates
            if (cp == MIN_HIGH) {
                cp = MAX_LOW + 1;
            }
        }

        cp--;
        for (int i = cs.length(); i > 0; i -= Character.charCount(ch)) {
            ch = Character.codePointBefore(cs, i);
            if (ch != cp) {
                throw new RuntimeException("codePointBefore(CharSequence, "+i+") returned "
                                           + toHexString(ch)
                                           + ", expected " + toHexString(cp));
            }
            cp--;
            // Skip surrogates
            if (cp == MAX_LOW) {
                cp = MIN_HIGH - 1;
            }
        }
    }

    /**
     * Test codePointAt(char[], int)
     *      codePointAt(char[], int, int)
     *      codePointBefore(char[], int)
     *      codePointBefore(char[], int, int)
     */
    static void test03(char[] a) {
        int cp = 0;
        int ch;
        for (int i = 0; i < a.length; i += Character.charCount(ch)) {
            ch = Character.codePointAt(a, i);
            if (ch != cp) {
                throw new RuntimeException("codePointAt(char[], "+i+") returned "
                                           + toHexString(ch)
                                           + ", expected "+toHexString(cp));
            }
            int x = Character.codePointAt(a, i, i+1);
            if (x != a[i]) {
                throw new RuntimeException(String.format(
                                 "codePointAt(char[], %d, %d) returned 0x%04x, expected 0x%04x\n",
                                 i, i+1, x, (int)a[i]));
            }
            cp++;
            // Skip surrogates
            if (cp == MIN_HIGH) {
                cp = MAX_LOW + 1;
            }
        }

        cp--;
        for (int i = a.length; i > 0; i -= Character.charCount(ch)) {
            ch = Character.codePointBefore(a, i);
            if (ch != cp) {
                throw new RuntimeException("codePointBefore(char[], "+i+") returned "
                                           + toHexString(ch)
                                           + ", expected " + toHexString(cp));
            }
            int x = Character.codePointBefore(a, i, i-1);
            if (x != a[i-1]) {
                throw new RuntimeException(String.format(
                                 "codePointAt(char[], %d, %d) returned 0x%04x, expected 0x%04x\n",
                                 i, i-1, x, (int)a[i-1]));
            }
            cp--;
            // Skip surrogates
            if (cp == MAX_LOW) {
                cp = MIN_HIGH - 1;
            }
        }
    }

    /**
     * Test codePointCount(CharSequence, int, int)
     *      codePointCount(char[], int, int, int, int)
     */
    static void test04(String str) {
        int length = str.length();
        char[] a = str.toCharArray();

        for (int i = 0; i <= length; i += 99, length -= 29999) {
            int n = Character.codePointCount(str, i, length);
            int m = codePointCount(str.substring(i, length));
            checkCodePointCount(str, n, m);
            n = Character.codePointCount(a, i, length - i);
            checkCodePointCount(a, n, m);
        }

        // test special cases
        length = str.length();
        int n = Character.codePointCount(str, 0, 0);
        checkCodePointCount(str, n, 0);
        n = Character.codePointCount(str, length, length);
        checkCodePointCount(str, n, 0);
        n = Character.codePointCount(a, 0, 0);
        checkCodePointCount(a, n, 0);
        n = Character.codePointCount(a, length, 0);
        checkCodePointCount(a, n, 0);
    }

    // This method assumes that Character.codePointAt() and
    // Character.charCount() work correctly.
    private static int codePointCount(CharSequence seq) {
        int n = 0, len;
        for (int i = 0; i < seq.length(); i += len) {
            int codepoint = Character.codePointAt(seq, i);
            n++;
            len = Character.charCount(codepoint);
        }
        return n;
    }

    private static void checkCodePointCount(Object data, int n, int expected) {
        String type = getType(data);
        if (n != expected) {
            throw new RuntimeException("codePointCount(" + type + "...) returned " + n
                                       + ", expected " + expected);
        }
    }

    /**
     * Test offsetByCodePoints(CharSequence, int, int)
     *      offsetByCodePoints(char[], int, int, int, int)
     *
     * This test case assumes that Character.codePointCount()s work
     * correctly.
     */
    static void test05(String str) {
        int length = str.length();
        char[] a = str.toCharArray();

        for (int i = 0; i <= length; i += 99, length -= 29999) {
            int nCodePoints = Character.codePointCount(a, i, length - i);
            int index;

            // offsetByCodePoints(CharSequence, int, int)

            int expectedHighIndex = length;
            // For forward CharSequence scan, we need to adjust the
            // expected index in case the last char in the text range
            // is a high surrogate and forms a valid supplementary
            // code point with the next char.
            if (length < a.length) {
                int cp = Character.codePointAt(a, length - 1);
                if (Character.isSupplementaryCodePoint(cp)) {
                    expectedHighIndex++;
                }
            }
            index = Character.offsetByCodePoints(str, i, nCodePoints);
            checkNewIndex(str, nCodePoints, index, expectedHighIndex);
            int expectedLowIndex = i;
            if (i > 0) {
                int cp = Character.codePointBefore(a, i + 1);
                if (Character.isSupplementaryCodePoint(cp)) {
                    expectedLowIndex--;
                }
            }
            index = Character.offsetByCodePoints(str, length, -nCodePoints);
            checkNewIndex(str, -nCodePoints, index, expectedLowIndex);

            // offsetByCodePoints(char[], int, int, int, int)

            int start = Math.max(0, i-1);
            int limit = Math.min(a.length, length+1);
            index = Character.offsetByCodePoints(a, start, limit - start,
                                                 i, nCodePoints);
            checkNewIndex(a, nCodePoints, index, expectedHighIndex);
            if (length != expectedHighIndex) {
                index = Character.offsetByCodePoints(a, start, length - start,
                                                     i, nCodePoints);
                checkNewIndex(a, nCodePoints, index, length);
            }
            index = Character.offsetByCodePoints(a, start, limit - start,
                                                 length, -nCodePoints);
            checkNewIndex(a, -nCodePoints, index, expectedLowIndex);
            if (i != expectedLowIndex) {
                index = Character.offsetByCodePoints(a, i, limit - i,
                                                     length, -nCodePoints);
                checkNewIndex(a, -nCodePoints, index, i);
            }
        }

        // test special cases for 0-length text ranges.
        length = str.length();
        int index = Character.offsetByCodePoints(str, 0, 0);
        checkNewIndex(str, 0, index, 0);
        index = Character.offsetByCodePoints(str, length, 0);
        checkNewIndex(str, 0, index, length);
        index = Character.offsetByCodePoints(a, 0, 0, 0, 0);
        checkNewIndex(a, 0, index, 0);
        index = Character.offsetByCodePoints(a, 0, length, 0, 0);
        checkNewIndex(a, 0, index, 0);
        index = Character.offsetByCodePoints(a, 0, length, length, 0);
        checkNewIndex(a, 0, index, length);
        index = Character.offsetByCodePoints(a, length, 0, length, 0);
        checkNewIndex(a, 0, index, length);
    }

    /**
     * Test toString(int)
     *
     * This test case assumes that Character.toChars()/String(char[]) work
     * correctly.
     */
    static void test06() {
        for (int cp = Character.MIN_CODE_POINT; cp <= Character.MAX_CODE_POINT; cp++) {
            String result = Character.toString(cp);
            String expected = new String(Character.toChars(cp));
            if (!result.equals(expected)) {
                throw new RuntimeException("Wrong string is created. code point: " +
                    cp + ", result: " + result + ", expected: " + expected);
            }
        }
    }

    private static void checkNewIndex(Object data, int offset, int result, int expected) {
        String type = getType(data);
        String offsetType = (offset > 0) ? "positive" : (offset < 0) ? "negative" : "0";
        if (result != expected) {
            throw new RuntimeException("offsetByCodePoints(" + type + ", ...) ["
                                       + offsetType + " offset]"
                                       + " returned " + result
                                       + ", expected " + expected);
        }
    }

    // Test codePointAt(CharSequence, int)
    //      codePointBefore(CharSequence, int)
    //      codePointAt(char[], int)
    //      codePointBefore(char[], int)
    //      toChar(int)
    //      toChar(int, char[], int)
    // with unpaired surrogates
    static void testUnpaired() {
        testCodePoint("\uD800", new int[] { 0xD800 });
        testCodePoint("\uDC00", new int[] { 0xDC00 });
        testCodePoint("a\uD800", new int[] { 'a', 0xD800 });
        testCodePoint("a\uDC00", new int[] { 'a', 0xDC00 });
        testCodePoint("\uD800a", new int[] { 0xD800, 'a' });
        testCodePoint("\uDBFFa", new int[] { 0xDBFF, 'a' });
        testCodePoint("a\uD800\uD801", new int[] { 'a', 0xD800, 0xD801 });
        testCodePoint("a\uD800x\uDC00", new int[] { 'a', 0xD800, 'x', 0xDC00 });
        testCodePoint("\uDC00\uD800", new int[] { 0xDC00, 0xD800 });
        testCodePoint("\uD800\uDC00\uDC00", new int[] { 0x10000, 0xDC00 });
        testCodePoint("\uD800\uD800\uDC00", new int[] { 0xD800, 0x10000 });
        testCodePoint("\uD800\uD800\uD800\uD800\uDC00\uDC00\uDC00\uDC00",
                      new int[] { 0xD800, 0xD800, 0xD800, 0x10000, 0xDC00, 0xDC00, 0xDC00});
    }

    static void testCodePoint(String str, int[] codepoints) {
        int c;
        // Test Character.codePointAt/Before(CharSequence, int)
        int j = 0;
        for (int i = 0; i < str.length(); i += Character.charCount(c)) {
            c = Character.codePointAt(str, i);
            if (c != codepoints[j++]) {
                throw new RuntimeException("codePointAt(CharSequence, " + i + ") returned "
                                           + toHexString(c)
                                           + ", expected " + toHexString(codepoints[j-1]));
            }
        }
        if (j != codepoints.length) {
            throw new RuntimeException("j != codepoints.length after codePointAt(CharSequence, int)"
                                       + " (j=" + j + ")"
                                       + ", expected: " + codepoints.length);
        }

        j = codepoints.length;
        for (int i = str.length(); i > 0 ; i -= Character.charCount(c)) {
            c = Character.codePointBefore(str, i);
            if (c != codepoints[--j]) {
                throw new RuntimeException("codePointBefore(CharSequence, " + i + ") returned "
                                           + toHexString(c)
                                           + ", expected " + toHexString(codepoints[j]));
            }
        }
        if (j != 0) {
            throw new RuntimeException("j != 0 after codePointBefore(CharSequence, int)"
                                       + " (j=" + j + ")");
        }

        // Test Character.codePointAt/Before(char[], int)
        char[] a = str.toCharArray();
        j = 0;
        for (int i = 0; i < a.length; i += Character.charCount(c)) {
            c = Character.codePointAt(a, i);
            if (c != codepoints[j++]) {
                throw new RuntimeException("codePointAt(char[], " + i + ") returned "
                                           + toHexString(c)
                                           + ", expected " + toHexString(codepoints[j-1]));
            }
        }
        if (j != codepoints.length) {
            throw new RuntimeException("j != codepoints.length after codePointAt(char[], int)"
                                       + " (j=" + j + ")"
                                       + ", expected: " + codepoints.length);
        }

        j = codepoints.length;
        for (int i = a.length; i > 0 ; i -= Character.charCount(c)) {
            c = Character.codePointBefore(a, i);
            if (c != codepoints[--j]) {
                throw new RuntimeException("codePointBefore(char[], " + i + ") returned "
                                           + toHexString(c)
                                           + ", expected " + toHexString(codepoints[j]));
            }
        }
        if (j != 0) {
            throw new RuntimeException("j != 0 after codePointBefore(char[], int)"
                                       + " (j=" + j + ")");
        }

        // Test toChar(int)
        j = 0;
        for (int i = 0; i < codepoints.length; i++) {
            a = Character.toChars(codepoints[i]);
            for (int k = 0; k < a.length; k++) {
                if (str.charAt(j++) != a[k]) {
                    throw new RuntimeException("toChars(int) returned " + toHexString("result", a)
                                               + " from codepoint=" + toHexString(codepoints[i]));
                }
            }
        }

        // Test toChars(int, char[], int)
        a = new char[codepoints.length * 2];
        j = 0;
        for (int i = 0; i < codepoints.length; i++) {
            int n = Character.toChars(codepoints[i], a, j);
            j += n;
        }
        String s = new String(a, 0, j);
        if (!str.equals(s)) {
            throw new RuntimeException("toChars(int, char[], int) returned "
                                       + toHexString("dst", s.toCharArray())
                                       + ", expected " + toHexString("data", str.toCharArray()));
        }
    }

    // Test toChar(int)
    //      toChar(int, char[], int)
    //      toString(int)
    // for exceptions
    static void testExceptions00() {
        callToChars1(-1, IllegalArgumentException.class);
        callToChars1(MAX_SUPPLEMENTARY + 1, IllegalArgumentException.class);

        callToChars3(MAX_SUPPLEMENTARY, null, 0, NullPointerException.class);
        callToChars3(-MIN_SUPPLEMENTARY,    new char[2], 0, IllegalArgumentException.class);
        callToChars3(MAX_SUPPLEMENTARY + 1, new char[2], 0, IllegalArgumentException.class);
        callToChars3('A', new char[0],  0, IndexOutOfBoundsException.class);
        callToChars3('A', new char[1], -1, IndexOutOfBoundsException.class);
        callToChars3('A', new char[1],  1, IndexOutOfBoundsException.class);
        callToChars3(MIN_SUPPLEMENTARY, new char[0],  0, IndexOutOfBoundsException.class);
        callToChars3(MIN_SUPPLEMENTARY, new char[1],  0, IndexOutOfBoundsException.class);
        callToChars3(MIN_SUPPLEMENTARY, new char[2], -1, IndexOutOfBoundsException.class);
        callToChars3(MIN_SUPPLEMENTARY, new char[2],  1, IndexOutOfBoundsException.class);

        callToString(Character.MIN_CODE_POINT - 1, IllegalArgumentException.class);
        callToString(Character.MAX_CODE_POINT + 1, IllegalArgumentException.class);
    }

    static final boolean At = true, Before = false;

    /**
     * Test codePointAt(CharSequence, int)
     *      codePointBefore(CharSequence, int)
     *      codePointCount(CharSequence, int, int)
     *      offsetByCodePoints(CharSequence, int, int)
     * for exceptions
     */
    static void testExceptions01(CharSequence cs) {
        CharSequence nullSeq = null;
        // codePointAt
        callCodePoint(At, nullSeq, 0, NullPointerException.class);
        callCodePoint(At, cs, -1, IndexOutOfBoundsException.class);
        callCodePoint(At, cs, cs.length(), IndexOutOfBoundsException.class);
        callCodePoint(At, cs, cs.length()*3, IndexOutOfBoundsException.class);

        // codePointBefore
        callCodePoint(Before, nullSeq, 0, NullPointerException.class);
        callCodePoint(Before, cs, -1, IndexOutOfBoundsException.class);
        callCodePoint(Before, cs, 0, IndexOutOfBoundsException.class);
        callCodePoint(Before, cs, cs.length()+1, IndexOutOfBoundsException.class);

        // codePointCount
        callCodePointCount(nullSeq, 0, 0, NullPointerException.class);
        callCodePointCount(cs, -1, 1, IndexOutOfBoundsException.class);
        callCodePointCount(cs, 0, cs.length()+1, IndexOutOfBoundsException.class);
        callCodePointCount(cs, 3, 1, IndexOutOfBoundsException.class);

        // offsetByCodePoints
        callOffsetByCodePoints(nullSeq, 0, 0, NullPointerException.class);
        callOffsetByCodePoints(cs, -1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(cs, cs.length()+1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(cs, 0, cs.length()*2, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(cs, cs.length(), 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(cs, 0, -1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(cs, cs.length(), -cs.length()*2,
                               IndexOutOfBoundsException.class);
        callOffsetByCodePoints(cs, cs.length(), Integer.MIN_VALUE,
                               IndexOutOfBoundsException.class);
        callOffsetByCodePoints(cs, 0, Integer.MAX_VALUE, IndexOutOfBoundsException.class);
    }

    /**
     * Test codePointAt(char[], int)
     *      codePointAt(char[], int, int)
     *      codePointBefore(char[], int)
     *      codePointBefore(char[], int, int)
     *      codePointCount(char[], int, int)
     *      offsetByCodePoints(char[], int, int, int, int)
     * for exceptions
     */
    static void testExceptions02(char[] a) {
        char[] nullArray = null;
        callCodePoint(At, nullArray, 0, NullPointerException.class);
        callCodePoint(At, a, -1, IndexOutOfBoundsException.class);
        callCodePoint(At, a, a.length, IndexOutOfBoundsException.class);
        callCodePoint(At, a, a.length*3, IndexOutOfBoundsException.class);
        callCodePoint(Before, nullArray, 0, NullPointerException.class);
        callCodePoint(Before, a, -1, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, 0, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, a.length+1, IndexOutOfBoundsException.class);

        // tests for the methods with limit
        callCodePoint(At, nullArray, 0, 1, NullPointerException.class);
        callCodePoint(At, a, 0, -1, IndexOutOfBoundsException.class);
        callCodePoint(At, a, 0, 0, IndexOutOfBoundsException.class);
        callCodePoint(At, a, 0, a.length+1, IndexOutOfBoundsException.class);
        callCodePoint(At, a, 2, 1, IndexOutOfBoundsException.class);
        callCodePoint(At, a, -1, 1, IndexOutOfBoundsException.class);
        callCodePoint(At, a, a.length, 1, IndexOutOfBoundsException.class);
        callCodePoint(At, a, a.length*3, 1, IndexOutOfBoundsException.class);
        callCodePoint(Before, nullArray, 1, 0, NullPointerException.class);
        callCodePoint(Before, a, 2, -1, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, 2, 2, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, 2, 3, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, 2, a.length, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, -1, -1, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, 0, 0, IndexOutOfBoundsException.class);
        callCodePoint(Before, a, a.length+1, a.length-1, IndexOutOfBoundsException.class);

        // codePointCount
        callCodePointCount(nullArray, 0, 0, NullPointerException.class);
        callCodePointCount(a, -1, 1, IndexOutOfBoundsException.class);
        callCodePointCount(a, 0, -1, IndexOutOfBoundsException.class);
        callCodePointCount(a, 0, a.length+1, IndexOutOfBoundsException.class);
        callCodePointCount(a, 1, a.length, IndexOutOfBoundsException.class);
        callCodePointCount(a, a.length, 1, IndexOutOfBoundsException.class);
        callCodePointCount(a, a.length+1, -1, IndexOutOfBoundsException.class);

        // offsetByCodePoints
        callOffsetByCodePoints(nullArray, 0, 0, 0, 0,  NullPointerException.class);
        callOffsetByCodePoints(a, -1, a.length, 1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length+1, 1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 10, a.length, 1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 10, a.length-10, 1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 10, 10, 21, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 20, -10, 15, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 10, 10, 15, 20, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 10, 10, 15, -20, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, -1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, a.length+1, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, 0, a.length*2, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, a.length, 1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, 0, -1, IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, a.length, -a.length*2,
                               IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, a.length, Integer.MIN_VALUE,
                               IndexOutOfBoundsException.class);
        callOffsetByCodePoints(a, 0, a.length, 0, Integer.MAX_VALUE,
                               IndexOutOfBoundsException.class);
    }

    /**
     * Test the 1-arg toChars(int) for exceptions
     */
    private static void callToChars1(int codePoint, Class expectedException) {
        try {
            char[] a = Character.toChars(codePoint);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("toChars(int) didn't throw " + expectedException.getName());
    }

    /**
     * Test the 3-arg toChars(int, char[], int) for exceptions
     */
    private static void callToChars3(int codePoint, char[] dst, int index,
                                     Class expectedException) {
        try {
            int n = Character.toChars(codePoint, dst, index);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("toChars(int,char[],int) didn't throw "
                                   + expectedException.getName());
    }

    private static void callCodePoint(boolean isAt, CharSequence cs, int index,
                                      Class expectedException) {
        try {
            int c = isAt ? Character.codePointAt(cs, index)
                         : Character.codePointBefore(cs, index);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("codePoint" + (isAt ? "At" : "Before")
                                   + " didn't throw " + expectedException.getName());
    }

    private static void callCodePoint(boolean isAt, char[] a, int index,
                                      Class expectedException) {
        try {
            int c = isAt ? Character.codePointAt(a, index)
                         : Character.codePointBefore(a, index);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("codePoint" + (isAt ? "At" : "Before")
                                   + " didn't throw " + expectedException.getName());
    }

    private static void callCodePoint(boolean isAt, char[] a, int index, int limit,
                                      Class expectedException) {
        try {
            int c = isAt ? Character.codePointAt(a, index, limit)
                         : Character.codePointBefore(a, index, limit);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("codePoint" + (isAt ? "At" : "Before")
                                   + " didn't throw " + expectedException.getName());
    }

    private static void callCodePointCount(Object data, int beginIndex, int endIndex,
                                           Class expectedException) {
        String type = getType(data);
        try {
            int n = (data instanceof CharSequence) ?
                  Character.codePointCount((CharSequence) data, beginIndex, endIndex)
                : Character.codePointCount((char[]) data, beginIndex, endIndex);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("codePointCount(" + type + "...) didn't throw "
                                   + expectedException.getName());
    }

    private static void callOffsetByCodePoints(CharSequence seq, int index, int offset,
                                               Class expectedException) {
        try {
            int n = Character.offsetByCodePoints(seq, index, offset);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("offsetCodePointCounts(CharSequnce...) didn't throw "
                                   + expectedException.getName());
    }


    private static void callOffsetByCodePoints(char[] a, int start, int count,
                                               int index, int offset,
                                               Class expectedException) {
        try {
            int n = Character.offsetByCodePoints(a, start, count, index, offset);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("offsetCodePointCounts(char[]...) didn't throw "
                                   + expectedException.getName());
    }

    private static void callToString(int codePoint, Class expectedException) {
        try {
            String s = Character.toString(codePoint);
        } catch (Exception e) {
            if (expectedException.isInstance(e)) {
                return;
            }
            throw new RuntimeException("Unspecified exception", e);
        }
        throw new RuntimeException("toString(int) didn't throw "
                                   + expectedException.getName());
    }

    private static String getType(Object data) {
        return (data instanceof CharSequence) ? "CharSequence" : "char[]";
    }

    private static String toHexString(int c) {
        return "0x" + Integer.toHexString(c);
    }

    private static String toHexString(String name, char[] a) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(name).append('[').append(i).append("]=");
            sb.append(toHexString(a[i]));
        }
        return sb.toString();
    }
}
