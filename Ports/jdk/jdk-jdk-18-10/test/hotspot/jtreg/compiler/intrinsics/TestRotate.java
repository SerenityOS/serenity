/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8248830 8256823
 * @summary Support for scalar rotates ([Integer/Long].rotate[Left/Right]).
 * @library /test/lib
 * @run main/othervm/timeout=600 -XX:-TieredCompilation -XX:CompileThreshold=1000 -Xbatch
 *      -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure
 *      compiler.intrinsics.TestRotate
 */
package compiler.intrinsics;

public class TestRotate {

    static final int ITERS = 50000;
    static final int[] INT_VALUES = {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 2, 3, 5, 8, 13};
    static final long[] LONG_VALUES = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, 1L, 2L, 3L, 5L, 8L, 13L};

    // expected resules
    static final int[] TEST_ROR_OR_INT_1_EXPECTED = {1073741824, -1073741825, 0, -2147483648, 1, -2147483647, -2147483646, 4, -2147483642};

    static final int[] TEST_ROR_OR_INT_16_EXPECTED = {32768, -32769, 0, 65536, 131072, 196608, 327680, 524288, 851968};

    static final int[] TEST_ROR_OR_INT_31_EXPECTED = {1, -2, 0, 2, 4, 6, 10, 16, 26};

    static final int[] TEST_ROR_OR_INT_32_EXPECTED = {-2147483648, 2147483647, 0, 1, 2, 3, 5, 8, 13};

    static final long[] TEST_ROR_OR_LONG_1_EXPECTED = {4611686018427387904L, -4611686018427387905L, 0L, -9223372036854775808L, 1L, -9223372036854775807L, -9223372036854775806L, 4L, -9223372036854775802L};

    static final long[] TEST_ROR_OR_LONG_16_EXPECTED = {140737488355328L, -140737488355329L, 0L, 281474976710656L, 562949953421312L, 844424930131968L, 1407374883553280L, 2251799813685248L, 3659174697238528L};

    static final long[] TEST_ROR_OR_LONG_63_EXPECTED = {1L, -2L, 0L, 2L, 4L, 6L, 10L, 16L, 26L};

    static final long[] TEST_ROR_OR_LONG_64_EXPECTED = {-9223372036854775808L, 9223372036854775807L, 0L, 1L, 2L, 3L, 5L, 8L, 13L};

    static final int[] TEST_ROR_ADD_INT_1_EXPECTED = TEST_ROR_OR_INT_1_EXPECTED;

    static final int[] TEST_ROR_ADD_INT_16_EXPECTED = TEST_ROR_OR_INT_16_EXPECTED;

    static final int[] TEST_ROR_ADD_INT_31_EXPECTED = TEST_ROR_OR_INT_31_EXPECTED;

    static final int[] TEST_ROR_ADD_INT_32_EXPECTED = {0, -2, 0, 2, 4, 6, 10, 16, 26};

    static final long[] TEST_ROR_ADD_LONG_1_EXPECTED = TEST_ROR_OR_LONG_1_EXPECTED;

    static final long[] TEST_ROR_ADD_LONG_16_EXPECTED = TEST_ROR_OR_LONG_16_EXPECTED;

    static final long[] TEST_ROR_ADD_LONG_63_EXPECTED = TEST_ROR_OR_LONG_63_EXPECTED;

    static final long[] TEST_ROR_ADD_LONG_64_EXPECTED = {0L, -2L, 0L, 2L, 4L, 6L, 10L, 16L, 26L};

    // eor shift expected
    static final int[] TEST_EOR_ROR_SHIFT_1_INT_EXPECTED = {-1073741824, -1073741824, 0, -2147483647, 3, -2147483646, -2147483641, 12, -2147483637};

    static final int[] TEST_EOR_ROR_SHIFT_16_INT_EXPECTED = {-2147450880, -2147450880, 0, 65537, 131074, 196611, 327685, 524296, 851981};

    static final int[] TEST_EOR_ROR_SHIFT_31_INT_EXPECTED = {-2147483647, -2147483647, 0, 3, 6, 5, 15, 24, 23};

    static final int[] TEST_EOR_ROR_SHIFT_32_INT_EXPECTED = {0, 0, 0, 0, 0, 0, 0, 0, 0};

    static final long[] TEST_EOR_ROR_SHIFT_1_LONG_EXPECTED = {-4611686018427387904L, -4611686018427387904L, 0L, -9223372036854775807L, 3L, -9223372036854775806L, -9223372036854775801L, 12L, -9223372036854775797L};

    static final long[] TEST_EOR_ROR_SHIFT_16_LONG_EXPECTED = {-9223231299366420480L, -9223231299366420480L, 0L, 281474976710657L, 562949953421314L, 844424930131971L, 1407374883553285L, 2251799813685256L, 3659174697238541L};

    static final long[] TEST_EOR_ROR_SHIFT_63_LONG_EXPECTED = {-9223372036854775807L, -9223372036854775807L, 0L, 3L, 6L, 5L, 15L, 24L, 23L};

    static final long[] TEST_EOR_ROR_SHIFT_64_LONG_EXPECTED = {0, 0, 0, 0, 0, 0, 0, 0, 0};

    // and shift expected
    static final int[] TEST_AND_ROR_SHIFT_1_INT_EXPECTED = {0, 1073741823, 0, 0, 0, 1, 0, 0, 4};

    static final int[] TEST_AND_ROR_SHIFT_16_INT_EXPECTED = {0, 2147450879, 0, 0, 0, 0, 0, 0, 0};

    static final int[] TEST_AND_ROR_SHIFT_31_INT_EXPECTED = {0, 2147483646, 0, 0, 0, 2, 0, 0, 8};

    static final int[] TEST_AND_ROR_SHIFT_32_INT_EXPECTED = {-2147483648, 2147483647, 0, 1, 2, 3, 5, 8, 13};

    static final long[] TEST_AND_ROR_SHIFT_1_LONG_EXPECTED = {0L, 4611686018427387903L, 0L, 0L, 0L, 1L, 0L, 0L, 4L};

    static final long[] TEST_AND_ROR_SHIFT_16_LONG_EXPECTED = {0L, 9223231299366420479L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};

    static final long[] TEST_AND_ROR_SHIFT_63_LONG_EXPECTED = {0L, 9223372036854775806L, 0L, 0L, 0L, 2L, 0L, 0L, 8L};

    static final long[] TEST_AND_ROR_SHIFT_64_LONG_EXPECTED = {-9223372036854775808L, 9223372036854775807L, 0L, 1L, 2L, 3L, 5L, 8L, 13L};

    // or shift expected
    static final int[] TEST_OR_ROR_SHIFT_1_INT_EXPECTED = {-1073741824, -1, 0, -2147483647, 3, -2147483645, -2147483641, 12, -2147483633};

    static final int[] TEST_OR_ROR_SHIFT_16_INT_EXPECTED = {-2147450880, -1, 0, 65537, 131074, 196611, 327685, 524296, 851981};

    static final int[] TEST_OR_ROR_SHIFT_31_INT_EXPECTED = {-2147483647, -1, 0, 3, 6, 7, 15, 24, 31};

    static final int[] TEST_OR_ROR_SHIFT_32_INT_EXPECTED = {-2147483648, 2147483647, 0, 1, 2, 3, 5, 8, 13};

    static final long[] TEST_OR_ROR_SHIFT_1_LONG_EXPECTED = {-4611686018427387904L, -1L, 0L, -9223372036854775807L, 3L, -9223372036854775805L, -9223372036854775801L, 12L, -9223372036854775793L};

    static final long[] TEST_OR_ROR_SHIFT_16_LONG_EXPECTED = {-9223231299366420480L, -1L, 0L, 281474976710657L, 562949953421314L, 844424930131971L, 1407374883553285L, 2251799813685256L, 3659174697238541L};

    static final long[] TEST_OR_ROR_SHIFT_63_LONG_EXPECTED = {-9223372036854775807L, -1L, 0L, 3L, 6L, 7L, 15L, 24L, 31L};

    static final long[] TEST_OR_ROR_SHIFT_64_LONG_EXPECTED = {-9223372036854775808L, 9223372036854775807L, 0L, 1L, 2L, 3L, 5L, 8L, 13L};

    // eon shift expected
    static final int[] TEST_EON_ROR_SHIFT_1_INT_EXPECTED = {1073741823, 1073741823, -1, 2147483646, -4, 2147483645, 2147483640, -13, 2147483636};

    static final int[] TEST_EON_ROR_SHIFT_16_INT_EXPECTED = {2147450879, 2147450879, -1, -65538, -131075, -196612, -327686, -524297, -851982};

    static final int[] TEST_EON_ROR_SHIFT_31_INT_EXPECTED = {2147483646, 2147483646, -1, -4, -7, -6, -16, -25, -24};

    static final int[] TEST_EON_ROR_SHIFT_32_INT_EXPECTED = {-1, -1, -1, -1, -1, -1, -1, -1, -1};

    static final long[] TEST_EON_ROR_SHIFT_1_LONG_EXPECTED = {4611686018427387903L, 4611686018427387903L, -1L, 9223372036854775806L, -4L, 9223372036854775805L, 9223372036854775800L, -13L, 9223372036854775796L};

    static final long[] TEST_EON_ROR_SHIFT_16_LONG_EXPECTED = {9223231299366420479L, 9223231299366420479L, -1L, -281474976710658L, -562949953421315L, -844424930131972L, -1407374883553286L, -2251799813685257L, -3659174697238542L};

    static final long[] TEST_EON_ROR_SHIFT_63_LONG_EXPECTED = {9223372036854775806L, 9223372036854775806L, -1L, -4L, -7L, -6L, -16L, -25L, -24L};

    static final long[] TEST_EON_ROR_SHIFT_64_LONG_EXPECTED = {-1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L};

    // bic shift expected
    static final int[] TEST_BIC_ROR_SHIFT_1_INT_EXPECTED = {-2147483648, 1073741824, 0, 1, 2, 2, 5, 8, 9};

    static final int[] TEST_BIC_ROR_SHIFT_16_INT_EXPECTED = {-2147483648, 32768, 0, 1, 2, 3, 5, 8, 13};

    static final int[] TEST_BIC_ROR_SHIFT_31_INT_EXPECTED = {-2147483648, 1, 0, 1, 2, 1, 5, 8, 5};

    static final int[] TEST_BIC_ROR_SHIFT_32_INT_EXPECTED = {0, 0, 0, 0, 0, 0, 0, 0, 0};

    static final long[] TEST_BIC_ROR_SHIFT_1_LONG_EXPECTED = {-9223372036854775808L, 4611686018427387904L, 0L, 1L, 2L, 2L, 5L, 8L, 9L};

    static final long[] TEST_BIC_ROR_SHIFT_16_LONG_EXPECTED = {-9223372036854775808L, 140737488355328L, 0L, 1L, 2L, 3L, 5L, 8L, 13L};

    static final long[] TEST_BIC_ROR_SHIFT_63_LONG_EXPECTED = {-9223372036854775808L, 1L, 0L, 1L, 2L, 1L, 5L, 8L, 5L};

    static final long[] TEST_BIC_ROR_SHIFT_64_LONG_EXPECTED = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};

    // orn shift expected
    static final int[] TEST_ORN_ROR_SHIFT_1_INT_EXPECTED = {-1073741825, 2147483647, -1, 2147483647, -2, 2147483647, 2147483645, -5, 2147483645};

    static final int[] TEST_ORN_ROR_SHIFT_16_INT_EXPECTED = {-32769, 2147483647, -1, -65537, -131073, -196609, -327681, -524289, -851969};

    static final int[] TEST_ORN_ROR_SHIFT_31_INT_EXPECTED = {-2, 2147483647, -1, -3, -5, -5, -11, -17, -19};

    static final int[] TEST_ORN_ROR_SHIFT_32_INT_EXPECTED = {-1, -1, -1, -1, -1, -1, -1, -1, -1};

    static final long[] TEST_ORN_ROR_SHIFT_1_LONG_EXPECTED = {-4611686018427387905L, 9223372036854775807L, -1L, 9223372036854775807L, -2L, 9223372036854775807L, 9223372036854775805L, -5L, 9223372036854775805L};

    static final long[] TEST_ORN_ROR_SHIFT_16_LONG_EXPECTED = {-140737488355329L, 9223372036854775807L, -1L, -281474976710657L, -562949953421313L, -844424930131969L, -1407374883553281L, -2251799813685249L, -3659174697238529L};

    static final long[] TEST_ORN_ROR_SHIFT_63_LONG_EXPECTED = {-2L, 9223372036854775807L, -1L, -3L, -5L, -5L, -11L, -17L, -19L};

    static final long[] TEST_ORN_ROR_SHIFT_64_LONG_EXPECTED = {-1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L};

    static final int[] TEST_ROR_INT_API_1_EXPECTED = TEST_ROR_OR_INT_1_EXPECTED;

    static final int[] TEST_ROR_INT_API_16_EXPECTED = TEST_ROR_OR_INT_16_EXPECTED;

    static final int[] TEST_ROR_INT_API_31_EXPECTED = TEST_ROR_OR_INT_31_EXPECTED;

    static final int[] TEST_ROR_INT_API_32_EXPECTED = TEST_ROR_OR_INT_32_EXPECTED;

    static final long[] TEST_ROR_LONG_API_1_EXPECTED = TEST_ROR_OR_LONG_1_EXPECTED;

    static final long[] TEST_ROR_LONG_API_16_EXPECTED = TEST_ROR_OR_LONG_16_EXPECTED;

    static final long[] TEST_ROR_LONG_API_63_EXPECTED = TEST_ROR_OR_LONG_63_EXPECTED;

    static final long[] TEST_ROR_LONG_API_64_EXPECTED = TEST_ROR_OR_LONG_64_EXPECTED;

    static final int[] TEST_ROL_INT_API_1_EXPECTED = {1, -2, 0, 2, 4, 6, 10, 16, 26};

    static final int[] TEST_ROL_INT_API_16_EXPECTED = {32768, -32769, 0, 65536, 131072, 196608, 327680, 524288, 851968};

    static final int[] TEST_ROL_INT_API_31_EXPECTED = {1073741824, -1073741825, 0, -2147483648, 1, -2147483647, -2147483646, 4, -2147483642};

    static final int[] TEST_ROL_INT_API_32_EXPECTED = {-2147483648, 2147483647, 0, 1, 2, 3, 5, 8, 13};

    static final long[] TEST_ROL_LONG_API_1_EXPECTED = {1L, -2L, 0L, 2L, 4L, 6L, 10L, 16L, 26L};

    static final long[] TEST_ROL_LONG_API_16_EXPECTED = {32768L, -32769L, 0L, 65536L, 131072L, 196608L, 327680L, 524288L, 851968L};

    static final long[] TEST_ROL_LONG_API_63_EXPECTED = {4611686018427387904L, -4611686018427387905L, 0L, -9223372036854775808L, 1L, -9223372036854775807L, -9223372036854775806L, 4L, -9223372036854775802L};

    static final long[] TEST_ROL_LONG_API_64_EXPECTED = {-9223372036854775808L, 9223372036854775807L, 0L, 1L, 2L, 3L, 5L, 8L, 13L};

    // verify
    static void verify(String text, long ref, long actual) {
        if (ref != actual) {
            System.err.println(text + " " + ref + " != " + actual);
            throw new Error("Fail");
        }
    }

    static void verify(String text, int ref, int actual) {
        if (ref != actual) {
            System.err.println(text + " " + ref + " != " + actual);
            throw new Error("Fail");
        }
    }

    // ror test constant
    public static int testRorOrInt1(int val) {
        return (val >>> 1) | (val << (32 - 1));
    }

    public static int testRorOrInt16(int val) {
        return (val >>> 16) | (val << (32 - 16));
    }

    public static int testRorOrInt31(int val) {
        return (val >>> 31) | (val << (32 - 31));
    }

    public static int testRorOrInt32(int val) {
        return (val >>> 32) | (val << (32 - 32));
    }

    public static long testRorOrLong1(long val) {
        return (val >>> 1) | (val << (64 - 1));
    }

    public static long testRorOrLong16(long val) {
        return (val >>> 16) | (val << (64 - 16));
    }

    public static long testRorOrLong63(long val) {
        return (val >>> 63) | (val << (64 - 63));
    }

    public static long testRorOrLong64(long val) {
        return (val >>> 64) | (val << (64 - 64));
    }

    public static int testRorAddInt1(int val) {
        return (val >>> 1) + (val << (32 - 1));
    }

    public static int testRorAddInt16(int val) {
        return (val >>> 16) + (val << (32 - 16));
    }

    public static int testRorAddInt31(int val) {
        return (val >>> 31) + (val << (32 - 31));
    }

    public static int testRorAddInt32(int val) {
        return (val >>> 32) + (val << (32 - 32));
    }

    public static long testRorAddLong1(long val) {
        return (val >>> 1) + (val << (64 - 1));
    }

    public static long testRorAddLong16(long val) {
        return (val >>> 16) + (val << (64 - 16));
    }

    public static long testRorAddLong63(long val) {
        return (val >>> 63) + (val << (64 - 63));
    }

    public static long testRorAddLong64(long val) {
        return (val >>> 64) + (val << (64 - 64));
    }

    // eor(ROR shift)
    public static int testRorOrInt1Eor(int val) {
        return val ^ ((val >>> 1) | (val << (32 - 1)));
    }

    public static int testRorOrInt16Eor(int val) {
        return val ^ ((val >>> 16) | (val << (32 - 16)));
    }

    public static int testRorOrInt31Eor(int val) {
        return val ^ ((val >>> 31) | (val << (32 - 31)));
    }

    public static int testRorOrInt32Eor(int val) {
        return val ^ ((val >>> 32) | (val << (32 - 32)));
    }

    public static long testRorOrLong1Eor(long val) {
        return val ^ ((val >>> 1) | (val << (64 - 1)));
    }

    public static long testRorOrLong16Eor(long val) {
        return val ^ ((val >>> 16) | (val << (64 - 16)));
    }

    public static long testRorOrLong63Eor(long val) {
        return val ^ ((val >>> 63) | (val << (64 - 63)));
    }

    public static long testRorOrLong64Eor(long val) {
        return val ^ ((val >>> 64) | (val << (64 - 64)));
    }

    // and(ROR shift)
    public static int testRorOrInt1And(int val) {
        return val & ((val >>> 1) | (val << (32 - 1)));
    }

    public static int testRorOrInt16And(int val) {
        return val & ((val >>> 16) | (val << (32 - 16)));
    }

    public static int testRorOrInt31And(int val) {
        return val & ((val >>> 31) | (val << (32 - 31)));
    }

    public static int testRorOrInt32And(int val) {
        return val & ((val >>> 32) | (val << (32 - 32)));
    }

    public static long testRorOrLong1And(long val) {
        return val & ((val >>> 1) | (val << (64 - 1)));
    }

    public static long testRorOrLong16And(long val) {
        return val & ((val >>> 16) | (val << (64 - 16)));
    }

    public static long testRorOrLong63And(long val) {
        return val & ((val >>> 63) | (val << (64 - 63)));
    }

    public static long testRorOrLong64And(long val) {
        return val & ((val >>> 64) | (val << (64 - 64)));
    }

    // or(ROR shift)
    public static int testRorOrInt1Or(int val) {
        return val | ((val >>> 1) | (val << (32 - 1)));
    }

    public static int testRorOrInt16Or(int val) {
        return val | ((val >>> 16) | (val << (32 - 16)));
    }

    public static int testRorOrInt31Or(int val) {
        return val | ((val >>> 31) | (val << (32 - 31)));
    }

    public static int testRorOrInt32Or(int val) {
        return val | ((val >>> 32) | (val << (32 - 32)));
    }

    public static long testRorOrLong1Or(long val) {
        return val | ((val >>> 1) | (val << (64 - 1)));
    }

    public static long testRorOrLong16Or(long val) {
        return val | ((val >>> 16) | (val << (64 - 16)));
    }

    public static long testRorOrLong63Or(long val) {
        return val | ((val >>> 63) | (val << (64 - 63)));
    }

    public static long testRorOrLong64Or(long val) {
        return val | ((val >>> 64) | (val << (64 - 64)));
    }

    // eon (ROR shift)
    public static int testRorOrInt1Eon(int val) {
        return val ^ (-1 ^ ((val >>> 1) | (val << (32 - 1))));
    }

    public static int testRorOrInt16Eon(int val) {
        return val ^ (-1 ^ ((val >>> 16) | (val << (32 - 16))));
    }

    public static int testRorOrInt31Eon(int val) {
        return val ^ (-1 ^ ((val >>> 31) | (val << (32 - 31))));
    }

    public static int testRorOrInt32Eon(int val) {
        return val ^ (-1 ^ ((val >>> 32) | (val << (32 - 32))));
    }

    public static long testRorOrLong1Eon(long val) {
        return val ^ (-1 ^ ((val >>> 1) | (val << (64 - 1))));
    }

    public static long testRorOrLong16Eon(long val) {
        return val ^ (-1 ^ ((val >>> 16) | (val << (64 - 16))));
    }

    public static long testRorOrLong63Eon(long val) {
        return val ^ (-1 ^ ((val >>> 63) | (val << (64 - 63))));
    }

    public static long testRorOrLong64Eon(long val) {
        return val ^ (-1 ^ ((val >>> 64) | (val << (64 - 64))));
    }

    // and (ROR shift)
    public static int testRorOrInt1Bic(int val) {
        return val & (-1 ^ ((val >>> 1) | (val << (32 - 1))));
    }

    public static int testRorOrInt16Bic(int val) {
        return val & (-1 ^ ((val >>> 16) | (val << (32 - 16))));
    }

    public static int testRorOrInt31Bic(int val) {
        return val & (-1 ^ ((val >>> 31) | (val << (32 - 31))));
    }

    public static int testRorOrInt32Bic(int val) {
        return val & (-1 ^ ((val >>> 32) | (val << (32 - 32))));
    }

    public static long testRorOrLong1Bic(long val) {
        return val & (-1 ^ ((val >>> 1) | (val << (64 - 1))));
    }

    public static long testRorOrLong16Bic(long val) {
        return val & (-1 ^ ((val >>> 16) | (val << (64 - 16))));
    }

    public static long testRorOrLong63Bic(long val) {
        return val & (-1 ^ ((val >>> 63) | (val << (64 - 63))));
    }

    public static long testRorOrLong64Bic(long val) {
        return val & (-1 ^ ((val >>> 64) | (val << (64 - 64))));
    }

    // or (ROR shift)
    public static int testRorOrInt1Orn(int val) {
        return val | (-1 ^ ((val >>> 1) | (val << (32 - 1))));
    }

    public static int testRorOrInt16Orn(int val) {
        return val | (-1 ^ ((val >>> 16) | (val << (32 - 16))));
    }

    public static int testRorOrInt31Orn(int val) {
        return val | (-1 ^ ((val >>> 31) | (val << (32 - 31))));
    }

    public static int testRorOrInt32Orn(int val) {
        return val | (-1 ^ ((val >>> 32) | (val << (32 - 32))));
    }

    public static long testRorOrLong1Orn(long val) {
        return val | (-1 ^ ((val >>> 1) | (val << (64 - 1))));
    }

    public static long testRorOrLong16Orn(long val) {
        return val | (-1 ^ ((val >>> 16) | (val << (64 - 16))));
    }

    public static long testRorOrLong63Orn(long val) {
        return val | (-1 ^ ((val >>> 63) | (val << (64 - 63))));
    }

    public static long testRorOrLong64Orn(long val) {
        return val | (-1 ^ ((val >>> 64) | (val << (64 - 64))));
    }

    // test rotate API
    public static int testRorIntApi(int val, int distance) {
        return Integer.rotateRight(val, distance);
    }

    public static long testRorLongApi(long val, int distance) {
        return Long.rotateRight(val, distance);
    }

    public static int testRolIntApi(int val, int distance) {
        return Integer.rotateLeft(val, distance);
    }

    public static long testRolLongApi(long val, int distance) {
        return Long.rotateLeft(val, distance);
    }

    public static void testRolIntZero(int val) {
        // Count is known to be zero only after loop opts
        int count = 42;
        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                count = 0;
            }
        }
        int res = Integer.rotateLeft(val, count);
        if (res != val) {
            throw new RuntimeException("test_rol_int_zero failed: " + res + " != " + val);
        }
    }

    public static void testRolLongZero(long val) {
        // Count is known to be zero only after loop opts
        int count = 42;
        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                count = 0;
            }
        }
        long res = Long.rotateLeft(val, count);
        if (res != val) {
            throw new RuntimeException("test_rol_long_zero failed: " + res + " != " + val);
        }
    }

    public static void testRorIntZero(int val) {
        // Count is known to be zero only after loop opts
        int count = 42;
        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                count = 0;
            }
        }
        int res = Integer.rotateRight(val, count);
        if (res != val) {
            throw new RuntimeException("test_ror_int_zero failed: " + res + " != " + val);
        }
    }

    public static void testRorLongZero(long val) {
        // Count is known to be zero only after loop opts
        int count = 42;
        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                count = 0;
            }
        }
        long res = Long.rotateRight(val, count);
        if (res != val) {
            throw new RuntimeException("test_ror_long_zero failed: " + res + " != " + val);
        }
    }

    public static void testRorOrInts() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorOrInt1(" + val + ")",  testRorOrInt1(val),  TEST_ROR_OR_INT_1_EXPECTED[i]);
                verify("testRorOrInt16(" + val + ")", testRorOrInt16(val), TEST_ROR_OR_INT_16_EXPECTED[i]);
                verify("testRorOrInt31(" + val + ")", testRorOrInt31(val), TEST_ROR_OR_INT_31_EXPECTED[i]);
                verify("testRorOrInt32(" + val + ")", testRorOrInt32(val), TEST_ROR_OR_INT_32_EXPECTED[i]);
            }
        }
    }

    public static void testRorAddInts() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorAddInt1(" + val + ")",  testRorAddInt1(val),  TEST_ROR_ADD_INT_1_EXPECTED[i]);
                verify("testRorAddInt16(" + val + ")", testRorAddInt16(val), TEST_ROR_ADD_INT_16_EXPECTED[i]);
                verify("testRorAddInt31(" + val + ")", testRorAddInt31(val), TEST_ROR_ADD_INT_31_EXPECTED[i]);
                verify("testRorAddInt32(" + val + ")", testRorAddInt32(val), TEST_ROR_ADD_INT_32_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrIntEors() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorOrInt1Eor(" + val + ")",  testRorOrInt1Eor(val),  TEST_EOR_ROR_SHIFT_1_INT_EXPECTED[i]);
                verify("testRorOrInt16Eor(" + val + ")", testRorOrInt16Eor(val), TEST_EOR_ROR_SHIFT_16_INT_EXPECTED[i]);
                verify("testRorOrInt31Eor(" + val + ")", testRorOrInt31Eor(val), TEST_EOR_ROR_SHIFT_31_INT_EXPECTED[i]);
                verify("testRorOrInt32Eor(" + val + ")", testRorOrInt32Eor(val), TEST_EOR_ROR_SHIFT_32_INT_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrIntAnds() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorOrInt1And(" + val + ")",  testRorOrInt1And(val),  TEST_AND_ROR_SHIFT_1_INT_EXPECTED[i]);
                verify("testRorOrInt16And(" + val + ")", testRorOrInt16And(val), TEST_AND_ROR_SHIFT_16_INT_EXPECTED[i]);
                verify("testRorOrInt31And(" + val + ")", testRorOrInt31And(val), TEST_AND_ROR_SHIFT_31_INT_EXPECTED[i]);
                verify("testRorOrInt32And(" + val + ")", testRorOrInt32And(val), TEST_AND_ROR_SHIFT_32_INT_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrIntOrs() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorOrInt1Or(" + val + ")",  testRorOrInt1Or(val),  TEST_OR_ROR_SHIFT_1_INT_EXPECTED[i]);
                verify("testRorOrInt16Or(" + val + ")", testRorOrInt16Or(val), TEST_OR_ROR_SHIFT_16_INT_EXPECTED[i]);
                verify("testRorOrInt31Or(" + val + ")", testRorOrInt31Or(val), TEST_OR_ROR_SHIFT_31_INT_EXPECTED[i]);
                verify("testRorOrInt32Or(" + val + ")", testRorOrInt32Or(val), TEST_OR_ROR_SHIFT_32_INT_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrIntEons() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorOrInt1Eon(" + val + ")",  testRorOrInt1Eon(val),  TEST_EON_ROR_SHIFT_1_INT_EXPECTED[i]);
                verify("testRorOrInt16Eon(" + val + ")", testRorOrInt16Eon(val), TEST_EON_ROR_SHIFT_16_INT_EXPECTED[i]);
                verify("testRorOrInt31Eon(" + val + ")", testRorOrInt31Eon(val), TEST_EON_ROR_SHIFT_31_INT_EXPECTED[i]);
                verify("testRorOrInt32Eon(" + val + ")", testRorOrInt32Eon(val), TEST_EON_ROR_SHIFT_32_INT_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrIntBics() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorOrInt1Bic(" + val + ")",  testRorOrInt1Bic(val),  TEST_BIC_ROR_SHIFT_1_INT_EXPECTED[i]);
                verify("testRorOrInt16Bic(" + val + ")", testRorOrInt16Bic(val), TEST_BIC_ROR_SHIFT_16_INT_EXPECTED[i]);
                verify("testRorOrInt31Bic(" + val + ")", testRorOrInt31Bic(val), TEST_BIC_ROR_SHIFT_31_INT_EXPECTED[i]);
                verify("testRorOrInt32Bic(" + val + ")", testRorOrInt32Bic(val), TEST_BIC_ROR_SHIFT_32_INT_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrIntOrns() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorOrInt1Orn(" + val + ")",  testRorOrInt1Orn(val),  TEST_ORN_ROR_SHIFT_1_INT_EXPECTED[i]);
                verify("testRorOrInt16Orn(" + val + ")", testRorOrInt16Orn(val), TEST_ORN_ROR_SHIFT_16_INT_EXPECTED[i]);
                verify("testRorOrInt31Orn(" + val + ")", testRorOrInt31Orn(val), TEST_ORN_ROR_SHIFT_31_INT_EXPECTED[i]);
                verify("testRorOrInt32Orn(" + val + ")", testRorOrInt32Orn(val), TEST_ORN_ROR_SHIFT_32_INT_EXPECTED[i]);
            }
        }
    }

    public static void testRorIntApis() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRorIntApi(" + val + ", 1)",  testRorIntApi(val, 1),  TEST_ROR_INT_API_1_EXPECTED[i]);
                verify("testRorIntApi(" + val + ", 16)", testRorIntApi(val, 16), TEST_ROR_INT_API_16_EXPECTED[i]);
                verify("testRorIntApi(" + val + ", 31)", testRorIntApi(val, 31), TEST_ROR_INT_API_31_EXPECTED[i]);
                verify("testRorIntApi(" + val + ", 32)", testRorIntApi(val, 32), TEST_ROR_INT_API_32_EXPECTED[i]);
            }
        }
    }

    public static void testRolIntApis() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                verify("testRolIntApi(" + val + ", 1)",  testRolIntApi(val, 1),  TEST_ROL_INT_API_1_EXPECTED[i]);
                verify("testRolIntApi(" + val + ", 16)", testRolIntApi(val, 16), TEST_ROL_INT_API_16_EXPECTED[i]);
                verify("testRolIntApi(" + val + ", 31)", testRolIntApi(val, 31), TEST_ROL_INT_API_31_EXPECTED[i]);
                verify("testRolIntApi(" + val + ", 32)", testRolIntApi(val, 32), TEST_ROL_INT_API_32_EXPECTED[i]);
            }
        }
    }

    public static void testRolrIntZeros() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < INT_VALUES.length; i++) {
                int val = INT_VALUES[i];
                testRolIntZero(val);
                testRorIntZero(val);
            }
        }
    }

    public static void testRorOrLongs() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorOrLong1(" + val + ")",  testRorOrLong1(val),  TEST_ROR_OR_LONG_1_EXPECTED[i]);
                verify("testRorOrLong16(" + val + ")", testRorOrLong16(val), TEST_ROR_OR_LONG_16_EXPECTED[i]);
                verify("testRorOrLong63(" + val + ")", testRorOrLong63(val), TEST_ROR_OR_LONG_63_EXPECTED[i]);
                verify("testRorOrLong64(" + val + ")", testRorOrLong64(val), TEST_ROR_OR_LONG_64_EXPECTED[i]);
            }
        }
    }

    public static void testRorAddLongs() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorAddLong1(" + val + ")",  testRorAddLong1(val),  TEST_ROR_ADD_LONG_1_EXPECTED[i]);
                verify("testRorAddLong16(" + val + ")", testRorAddLong16(val), TEST_ROR_ADD_LONG_16_EXPECTED[i]);
                verify("testRorAddLong63(" + val + ")", testRorAddLong63(val), TEST_ROR_ADD_LONG_63_EXPECTED[i]);
                verify("testRorAddLong64(" + val + ")", testRorAddLong64(val), TEST_ROR_ADD_LONG_64_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrLongEors() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorOrLong1Eor(" + val + ")",  testRorOrLong1Eor(val),  TEST_EOR_ROR_SHIFT_1_LONG_EXPECTED[i]);
                verify("testRorOrLong16Eor(" + val + ")", testRorOrLong16Eor(val), TEST_EOR_ROR_SHIFT_16_LONG_EXPECTED[i]);
                verify("testRorOrLong63Eor(" + val + ")", testRorOrLong63Eor(val), TEST_EOR_ROR_SHIFT_63_LONG_EXPECTED[i]);
                verify("testRorOrLong64Eor(" + val + ")", testRorOrLong64Eor(val), TEST_EOR_ROR_SHIFT_64_LONG_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrLongAnds() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorOrLong1And(" + val + ")",  testRorOrLong1And(val),  TEST_AND_ROR_SHIFT_1_LONG_EXPECTED[i]);
                verify("testRorOrLong16And(" + val + ")", testRorOrLong16And(val), TEST_AND_ROR_SHIFT_16_LONG_EXPECTED[i]);
                verify("testRorOrLong63And(" + val + ")", testRorOrLong63And(val), TEST_AND_ROR_SHIFT_63_LONG_EXPECTED[i]);
                verify("testRorOrLong64And(" + val + ")", testRorOrLong64And(val), TEST_AND_ROR_SHIFT_64_LONG_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrLongOrs() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorOrLong1Or(" + val + ")",  testRorOrLong1Or(val),  TEST_OR_ROR_SHIFT_1_LONG_EXPECTED[i]);
                verify("testRorOrLong16Or(" + val + ")", testRorOrLong16Or(val), TEST_OR_ROR_SHIFT_16_LONG_EXPECTED[i]);
                verify("testRorOrLong63Or(" + val + ")", testRorOrLong63Or(val), TEST_OR_ROR_SHIFT_63_LONG_EXPECTED[i]);
                verify("testRorOrLong64Or(" + val + ")", testRorOrLong64Or(val), TEST_OR_ROR_SHIFT_64_LONG_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrLongEons() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorOrLong1Eon(" + val + ")",  testRorOrLong1Eon(val),  TEST_EON_ROR_SHIFT_1_LONG_EXPECTED[i]);
                verify("testRorOrLong16Eon(" + val + ")", testRorOrLong16Eon(val), TEST_EON_ROR_SHIFT_16_LONG_EXPECTED[i]);
                verify("testRorOrLong63Eon(" + val + ")", testRorOrLong63Eon(val), TEST_EON_ROR_SHIFT_63_LONG_EXPECTED[i]);
                verify("testRorOrLong64Eon(" + val + ")", testRorOrLong64Eon(val), TEST_EON_ROR_SHIFT_64_LONG_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrLongBics() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorOrLong1Bic(" + val + ")",  testRorOrLong1Bic(val),  TEST_BIC_ROR_SHIFT_1_LONG_EXPECTED[i]);
                verify("testRorOrLong16Bic(" + val + ")", testRorOrLong16Bic(val), TEST_BIC_ROR_SHIFT_16_LONG_EXPECTED[i]);
                verify("testRorOrLong63Bic(" + val + ")", testRorOrLong63Bic(val), TEST_BIC_ROR_SHIFT_63_LONG_EXPECTED[i]);
                verify("testRorOrLong64Bic(" + val + ")", testRorOrLong64Bic(val), TEST_BIC_ROR_SHIFT_64_LONG_EXPECTED[i]);
            }
        }
    }

    public static void testRorOrLongOrns() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorOrLong1Orn(" + val + ")",  testRorOrLong1Orn(val),  TEST_ORN_ROR_SHIFT_1_LONG_EXPECTED[i]);
                verify("testRorOrLong16Orn(" + val + ")", testRorOrLong16Orn(val), TEST_ORN_ROR_SHIFT_16_LONG_EXPECTED[i]);
                verify("testRorOrLong63Orn(" + val + ")", testRorOrLong63Orn(val), TEST_ORN_ROR_SHIFT_63_LONG_EXPECTED[i]);
                verify("testRorOrLong64Orn(" + val + ")", testRorOrLong64Orn(val), TEST_ORN_ROR_SHIFT_64_LONG_EXPECTED[i]);
            }
        }
    }

    public static void testRorLongApis() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRorLongApi(" + val + ", 1)",  testRorLongApi(val, 1),  TEST_ROR_LONG_API_1_EXPECTED[i]);
                verify("testRorLongApi(" + val + ", 16)", testRorLongApi(val, 16), TEST_ROR_LONG_API_16_EXPECTED[i]);
                verify("testRorLongApi(" + val + ", 63)", testRorLongApi(val, 63), TEST_ROR_LONG_API_63_EXPECTED[i]);
                verify("testRorLongApi(" + val + ", 64)", testRorLongApi(val, 64), TEST_ROR_LONG_API_64_EXPECTED[i]);
            }
        }
    }

    public static void testRolLongApis() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                verify("testRolLongApi(" + val + ", 1)",  testRolLongApi(val, 1),  TEST_ROL_LONG_API_1_EXPECTED[i]);
                verify("testRolLongApi(" + val + ", 16)", testRolLongApi(val, 16), TEST_ROL_LONG_API_16_EXPECTED[i]);
                verify("testRolLongApi(" + val + ", 63)", testRolLongApi(val, 63), TEST_ROL_LONG_API_63_EXPECTED[i]);
                verify("testRolLongApi(" + val + ", 64)", testRolLongApi(val, 64), TEST_ROL_LONG_API_64_EXPECTED[i]);
            }
        }
    }

    public static void testRolrLongZeros() {
        for (int count = 0; count < ITERS; count++) {
            for (int i = 0; i < LONG_VALUES.length; i++) {
                long val = LONG_VALUES[i];
                testRolLongZero(i);
                testRorLongZero(i);
            }
        }
    }

    public static void main(String[] args) {
        testRorOrInts();
        testRorAddInts();
        testRorOrIntEors();
        testRorOrIntAnds();
        testRorOrIntOrs();
        testRorOrIntEons();
        testRorOrIntBics();
        testRorOrIntOrns();
        testRorIntApis();
        testRolIntApis();
        testRolrIntZeros();

        testRorOrLongs();
        testRorAddLongs();
        testRorOrLongEors();
        testRorOrLongAnds();
        testRorOrLongOrs();
        testRorOrLongEons();
        testRorOrLongBics();
        testRorOrLongOrns();
        testRorLongApis();
        testRolLongApis();
        testRolrLongZeros();
    }

}
