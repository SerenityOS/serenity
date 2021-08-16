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
 *          different string length. This test creates string with specified
 *          size and longer string, which is same at beginning.
 *          Expecting length delta to be returned. Test class takes 2
 *          parameters: <string length>, <maximum string length delta>
 *          Input parameters for this test are set according to Aarch64
 *          String::compareTo intrinsic implementation specifics. Aarch64
 *          implementation has 1, 4, 8 -bytes loops for length < 72 and
 *          16, 32, 64 -characters loops for length >= 72. Code is also affected
 *          by SoftwarePrefetchHintDistance vm flag value.
 * @run main/othervm -XX:SoftwarePrefetchHintDistance=192 compiler.intrinsics.string.TestStringCompareToDifferentLength 4 2 5 10 13 17 20 23 24 25 71 72 73 88 90 192 193 208 209
 * @run main/othervm -XX:SoftwarePrefetchHintDistance=16 compiler.intrinsics.string.TestStringCompareToDifferentLength 4 2 5 10 13 17 20 23 24 25 71 72 73 88 90
 * @run main/othervm -XX:SoftwarePrefetchHintDistance=-1 compiler.intrinsics.string.TestStringCompareToDifferentLength 4 2 5 10 13 17 20 23 24 25 71 72 73 88 90
 */

package compiler.intrinsics.string;

public class TestStringCompareToDifferentLength {
    private final int size;

    public static void main(String args[]) {
        if (args.length > 1) {
            int maxLengthDelta = Integer.parseInt(args[0]);
            for (int i = 1; i < args.length; i++) {
                int  size = Integer.parseInt(args[i]);
                TestStringCompareToDifferentLength test
                        = new TestStringCompareToDifferentLength(size);
                for (int delta = 1; delta <= maxLengthDelta; delta++) {
                    test.testCompareTo(delta);
                }
            }
        } else {
            System.out.println("Usage: $testClass $maxLengthDelta $testLength [$testLength2 [$testLength3 [...]]]");
        }
    }

    private TestStringCompareToDifferentLength(int size) {
        this.size = size;
    }

    private void testCompareTo(int delta) {
        char strsrc[] = new char[size + delta];
        // generate ASCII string
        for (int i = 0; i < size + delta; i++) {
            strsrc[i] = (char) ('a' + (i % 26));
        }

        String longLatin1 = new String(strsrc);
        String shortLatin1 = longLatin1.substring(0, size);

        String longUTF16LastChar = longLatin1.substring(0, longLatin1.length() - 1) + '\uBEEF';
        String longUTF16FirstChar = '\uBEEF' + longLatin1.substring(1, longLatin1.length());
        String shortUTF16FirstChar = longUTF16FirstChar.substring(0, size);

        for (int i = 0; i < 10000; i++) {
            checkCase(longLatin1, shortLatin1, delta, "LL"); // Latin1-Latin1.
            checkCase(longUTF16LastChar, shortLatin1, delta, "UL"); // Latin1-UTF-16 case.
            checkCase(longUTF16FirstChar, shortUTF16FirstChar, delta, "UU"); // UTF-16-UTF-16 case
        }
    }

    private void checkCase(String str2, String str1, int expected, String caseName) {
        int result = str2.compareTo(str1);
        int reversedResult = str1.compareTo(str2);
        if (expected != result || result != -reversedResult) {
            throw new AssertionError(String.format("%s CASE FAILED: size = %d, "
                    + "expected = %d, but got result = %d, "
                    + "reversedResult = %d for string1 = '%s', string2 = '%s'",
                    caseName, size, expected, result,
                    reversedResult, str1, str2));
        }
    }
}

