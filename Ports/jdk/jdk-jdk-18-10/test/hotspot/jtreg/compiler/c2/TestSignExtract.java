/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Arm Limited. All rights reserved.
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
 * @bug 8242429
 * @summary Better implementation for sign extract.
 *
 * @run main/othervm -XX:-TieredCompilation -XX:CompileCommand=dontinline,compiler.c2.TestSignExtract::signExtract*
 *      compiler.c2.TestSignExtract
 */
package compiler.c2;

public class TestSignExtract {

    private static final long[] LONG_VALUES = {0L, 0xFFFFFFFFL, 0x12L, -1L, -123L, -0x12L, Long.MAX_VALUE, Long.MIN_VALUE};
    private static final int[] INT_VALUES = {0, 0x1234, -1, -0x12345678, Integer.MAX_VALUE, Integer.MIN_VALUE};


    private static int signExtractInt1(int x) {
        return (x >> 1) >>> 31;
    }

    private static int signExtractInt2(int x) {
        return (x >> 32) >>> 31;
    }

    private static int signExtractInt3(int x) {
        return (x >> 31) >>> 31;
    }

    private static int signExtractInt4(int x) {
        return 0 - (x >> 31);
    }

    private static long signExtractLong1(long x) {
        return (x >> 1) >>> 63;
    }

    private static long signExtractLong2(long x) {
        return (x >> 54) >>> 63;
    }

    private static long signExtractLong3(long x) {
        return (x >> 63) >>> 63;
    }

    private static long signExtractLong4(long x) {
        return 0 - (x >> 63);
    }

    private static int WARMUP = 5000;

    public static void main(String[] args) {
        for (int i = 0; i < WARMUP; i++) {
            for (int e : INT_VALUES) {
                // "(A >> n) >>> 31" => "(A >>> 31)"
                assert e >>> 31 == signExtractInt1(e);
                assert e >>> 31 == signExtractInt2(e);
                assert e >>> 31 == signExtractInt3(e);
                // "0 - (A >> 31)" => "(A >>> 31)"
                assert e >>> 31 == signExtractInt4(e);
            }

            for (long e : LONG_VALUES) {
                // "(A >> n) >>> 63" => "(A >>> 63)"
                assert e >>> 63 == signExtractLong1(e);
                assert e >>> 63 == signExtractLong2(e);
                assert e >>> 63 == signExtractLong3(e);
                // "0 - (A >> 63)" => "(A >>> 63)"
                assert e >>> 63 == signExtractLong4(e);
            }
        }
    }
}
