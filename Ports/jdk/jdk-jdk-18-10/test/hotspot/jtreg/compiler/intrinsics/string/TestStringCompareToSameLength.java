/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, BELLSOFT. All rights reserved.
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
 * @requires os.arch=="aarch64"
 * @summary String::compareTo implementation uses different algorithms for
 *          different string length. This test creates various strings of
 *          specified size, which are different at all possible index values and
 *          compares them. Expecting separately calculated result to be returned.
 *          String size is specified via commandline. Various size values can
 *          be specified during intrinsic development in order to test cases
 *          specific for new or modified intrinsic implementation. Aarch64
 *          implementation has 1, 4, 8 -bytes loops for length < 72 and
 *          16, 32, 64 -bytes loops for string length >= 72. Code is also
 *          affected by SoftwarePrefetchHintDistance flag value.
 *          Test class can also accept "-fullmode" parameter
 *          with maxLength paramter after it. Then it will iterate through all
 *          string length values up to maxLength parameter (inclusive). It takes
 *          a lot of time but is useful for development.
 * @run main/othervm -XX:SoftwarePrefetchHintDistance=192 compiler.intrinsics.string.TestStringCompareToSameLength 2 5 10 13 17 20 25 35 36 37 71 72 73 88 90 192 193 208 209
 * @run main/othervm -XX:SoftwarePrefetchHintDistance=16 compiler.intrinsics.string.TestStringCompareToSameLength 2 5 10 13 17 20 25 35 36 37 71 72 73 88 90
 * @run main/othervm -XX:SoftwarePrefetchHintDistance=-1 compiler.intrinsics.string.TestStringCompareToSameLength 2 5 10 13 17 20 25 35 36 37 71 72 73 88 90
 */

package compiler.intrinsics.string;

public class TestStringCompareToSameLength {
    private final int size;

    public static void main(String args[]) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: $testClass $testLength1"
                    + " [$testLength2 [...]] | -fullmode $maxLength");
        }
        if (args.length == 2 && "-fullmode".equals(args[0])) {
            int maxLength = Integer.parseInt(args[1]);
            for (int length = 1; length <= maxLength; length++) {
                TestStringCompareToSameLength test = new TestStringCompareToSameLength(length);
                for (int mismatchIdx = 0; mismatchIdx <= length; mismatchIdx++) {
                    test.testCompareTo(mismatchIdx);
                }
            }
        } else {
            for (String arg : args) {
                int size = Integer.parseInt(arg);
                TestStringCompareToSameLength test = new TestStringCompareToSameLength(size);
                for (int mismatchIdx = 0; mismatchIdx <= size; mismatchIdx++) {
                    test.testCompareTo(mismatchIdx);
                }
            }
        }
    }

    private TestStringCompareToSameLength(int size) {
        this.size = size;
    }

    private void testCompareTo(int mismatchIdx) {
        // Create Latin1 strings: latin1, latin2, which are different at index.
        // Case of index == size is a case of equal strings
        char latinSrc[] = new char[size];
        // generate ASCII string
        for (int i = 0; i < size; i++) {
            latinSrc[i] = (char) ('a' + (i % 26));
        }
        String latinStr1 = new String(latinSrc);
        if (mismatchIdx != size) latinSrc[mismatchIdx] = (char) ('a' - 1);
        String latinStr2 = new String(latinSrc);

        // Create 3 utf strings: utfStr1, utfStr2: same as latinStr1, but has UTF-16 character
        // utfStr1 and utfStr2 are different at requested index and character value is greater
        // than same index character in latinStr1.
        // utfStr3 is different at requested index and character value is less than same
        // index character in latinStr1. Will be a Latin1-encoded string in case difference
        // is requested at last character. This case not applicable and is skipped below.
        char cArray[] = latinStr1.toCharArray();
        cArray[cArray.length - 1] = '\uBEEF'; // at least last character is UTF-16
        if (mismatchIdx != size) cArray[mismatchIdx] = '\u1234';
        String utfStr1 = new String(cArray);
        if (mismatchIdx != size) cArray[mismatchIdx] = '\u5678';
        String utfStr2 = new String(cArray);
        if (mismatchIdx != size) cArray[mismatchIdx] = (char) ('a' - 2); // less than Latin1 index position
        // utfStr3 will be Latin1 if last character differ. Will skip this case
        String utfStr3 = new String(cArray);

        for (int i = 0; i < 10000; i++) {
            checkCase(mismatchIdx, latinStr1, latinStr2, "LL"); // compare Latin1 with Latin1

            checkCase(mismatchIdx, utfStr1, utfStr2, "UU"); // compare UTF-16 vs UTF-16

            if (size != mismatchIdx) { // UTF-16 and Latin1 strings can't be equal. Then skip this case
                // compare UTF16 string, which is expected to be > than Latin1
                checkCase(mismatchIdx, latinStr1, utfStr1, "U(large)L");
                if (mismatchIdx != size - 1) {
                    // compare UTF16 string, which is expected to be < than Latin1
                    checkCase(mismatchIdx,  latinStr1, utfStr3, "U(small)L");
                }
            }
        }
    }

    private void checkCase(int mismatchIdx, String str1, String str2, String caseName) {
        int expected;
        if (mismatchIdx != size) {
            expected = str1.charAt(mismatchIdx) - str2.charAt(mismatchIdx);
        } else {
            expected = str1.length() - str2.length();
        }
        int result = str1.compareTo(str2);
        int reversedResult = str2.compareTo(str1);
        if (expected != result || result != -reversedResult) {
            throw new AssertionError(String.format("%s CASE FAILED: size = %d, "
                    + "mismatchIdx = %d, expected = %d, but got result = %d, "
                    + "reversedResult = %d for string1 = '%s', string2 = '%s'",
                    caseName, size, mismatchIdx, expected, result,
                    reversedResult, str1, str2));
        }
    }
}

