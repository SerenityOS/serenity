/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8054307
 * @summary Validates StringCoding.hasNegatives intrinsic with a small range of tests.
 * @library /compiler/patches
 *
 * @build java.base/java.lang.Helper
 * @run main compiler.intrinsics.string.TestHasNegatives
 */

package compiler.intrinsics.string;

/*
 * @summary Validates StringCoding.hasNegatives intrinsic with a small
 *          range of tests.
 */
public class TestHasNegatives {

    private static byte[] tBa = new byte[4096 + 16];

    /**
     * Completely initialize the test array, preparing it for tests of the
     * StringCoding.hasNegatives method with a given array segment offset,
     * length, and number of negative bytes.
     */
    public static void initialize(int off, int len, int neg) {
        assert (len + off <= tBa.length);
        // insert "canary" (negative) values before offset
        for (int i = 0; i < off; ++i) {
            tBa[i] = (byte) (((i + 15) & 0x7F) | 0x80);
        }
        // fill the array segment
        for (int i = off; i < len + off; ++i) {
            tBa[i] = (byte) (((i - off + 15) & 0x7F));
        }
        if (neg != 0) {
            // modify a number (neg) disparate array bytes inside
            // segment to be negative.
            int div = (neg > 1) ? (len - 1) / (neg - 1) : 0;
            int idx;
            for (int i = 0; i < neg; ++i) {
                idx = off + (len - 1) - div * i;
                tBa[idx] = (byte) (0x80 | tBa[idx]);
            }
        }
        // insert "canary" negative values after array segment
        for (int i = len + off; i < tBa.length; ++i) {
            tBa[i] = (byte) (((i + 15) & 0x7F) | 0x80);
        }
    }

    /** Sizes of array segments to test. */
    private static int sizes[] = { 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 13, 17, 19, 23, 37, 61, 131,
            4099 };

    /**
     * Test different array segment sizes, offsets, and number of negative
     * bytes.
     */
    public static void test_hasNegatives() throws Exception {
        int len, off;
        int ng;
        boolean r;

        for (ng = 0; ng < 57; ++ng) { // number of negatives in array segment
            for (off = 0; off < 8; ++off) { // starting offset of array segment
                for (int i = 0; i < sizes.length; ++i) { // array segment size
                                                         // choice
                    len = sizes[i];
                    if (len + off > tBa.length)
                        continue;
                    initialize(off, len, ng);
                    r = Helper.StringCodingHasNegatives(tBa, off, len);
                    if (r ^ ((ng == 0) ? false : true)) {
                        throw new Exception("Failed test hasNegatives " + "offset: " + off + " "
                                + "length: " + len + " " + "return: " + r + " " + "negatives: "
                                + ng);
                    }
                }
            }
        }
    }

    public void run() throws Exception {
        // iterate to eventually get intrinsic inlined
        for (int j = 0; j < 1000; ++j) {
            test_hasNegatives();
        }
    }

    public static void main(String[] args) throws Exception {
        (new TestHasNegatives()).run();
        System.out.println("hasNegatives validated");
    }
}
