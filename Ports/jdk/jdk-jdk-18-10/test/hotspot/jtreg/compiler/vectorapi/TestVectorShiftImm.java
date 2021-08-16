/*
 * Copyright (c) 2021, Huawei Technologies Co., Ltd. All rights reserved.
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

package compiler.vectorapi;

import java.util.Random;
import java.util.Arrays;

import jdk.incubator.vector.*;

/**
 * @test
 * @bug 8261142
 * @summary AArch64: Incorrect instruction encoding when right-shifting vectors with shift amount equals to the element width
 * @modules jdk.incubator.vector
 *
 * @run main/othervm -XX:CompileCommand=print,compiler/vectorapi/TestVectorShiftImm.shift*
 *                   -XX:-TieredCompilation -Dvlen=64 compiler.vectorapi.TestVectorShiftImm
 * @run main/othervm -XX:CompileCommand=print,compiler/vectorapi/TestVectorShiftImm.shift*
 *                   -XX:-TieredCompilation -Dvlen=128 compiler.vectorapi.TestVectorShiftImm
 */

public class TestVectorShiftImm {
    private static final int ARR_LEN = 16;
    private static final int NUM_ITERS = 100000;

    private static final int NUM_OPS          = 5;
    private static final int ACCUMULATE_OP_S  = 3;
    private static final int MAX_TESTS_PER_OP = 7;
    private static final int VLENS            = 2;

    private static byte[]  bytesA,    bytesB;
    private static short[] shortsA,   shortsB;
    private static int[]   integersA, integersB;
    private static long[]  longsA,    longsB;

    private static byte  tBytes[][],    gBytes[][];
    private static short tShorts[][],   gShorts[][];
    private static int   tIntegers[][], gIntegers[][];
    private static long  tLongs[][],    gLongs[][];

    private static Random r = new Random(32781);

    static final VectorSpecies<Byte> byte64SPECIES  = ByteVector.SPECIES_64;
    static final VectorSpecies<Byte> byte128SPECIES = ByteVector.SPECIES_128;

    static final VectorSpecies<Short> short64SPECIES  = ShortVector.SPECIES_64;
    static final VectorSpecies<Short> short128SPECIES = ShortVector.SPECIES_128;

    static final VectorSpecies<Integer> integer64SPECIES  = IntVector.SPECIES_64;
    static final VectorSpecies<Integer> integer128SPECIES = IntVector.SPECIES_128;

    static final VectorSpecies<Long> long128SPECIES = LongVector.SPECIES_128;

    static String[] opNames = {"LSHL", "ASHR", "LSHR", "ASHR_AND_ACCUMULATE", "LSHR_AND_ACCUMULATE"};

    static boolean allTestsPassed = true;
    static StringBuilder errMsg = new StringBuilder();

    public static void main(String args[]) {

        int vlen = Integer.parseInt(System.getProperty("vlen", ""));

        test_init();

        if (vlen == 64) {
            test_vlen64();
        }

        if(vlen == 128) {
            test_vlen128();
        }

        if (allTestsPassed) {
            System.out.println("Test PASSED");
        } else {
            throw new RuntimeException("Test Failed, failed tests:\n" + errMsg.toString());
        }
    }

    static void test_vlen64() {
        for (int i = 0; i < NUM_ITERS; i++) {
            shift_and_accumulate(tBytes, true, byte64SPECIES, 64);
            shift_and_accumulate(tShorts, true, short64SPECIES, 64);
            shift_and_accumulate(tIntegers, true, integer64SPECIES, 64);

            shift(tBytes, true, byte64SPECIES, 64);
            shift(tShorts, true, short64SPECIES, 64);
            shift(tIntegers, true, integer64SPECIES, 64);
        }
    }

    static void test_vlen128() {
        for (int i = 0; i < NUM_ITERS; i++) {
            shift_and_accumulate(tBytes, true, byte128SPECIES, 128);
            shift_and_accumulate(tShorts, true, short128SPECIES, 128);
            shift_and_accumulate(tIntegers, true, integer128SPECIES, 128);
            shift_and_accumulate(tLongs, true, long128SPECIES, 128);

            shift(tBytes, true, byte128SPECIES, 128);
            shift(tShorts, true, short128SPECIES, 128);
            shift(tIntegers, true, integer128SPECIES, 128);
            shift(tLongs, true, long128SPECIES, 128);
        }
    }

    /**
     * Tests for type byte.
     */

    static int shift_with_op(VectorOperators.Binary op, ByteVector vbb,
                             byte arr[][], int end, int ind) {
        vbb.lanewise(op, 0).intoArray(arr[end++], ind);
        vbb.lanewise(op, 1).intoArray(arr[end++], ind);
        vbb.lanewise(op, 8).intoArray(arr[end++], ind);
        vbb.lanewise(op, 13).intoArray(arr[end++], ind);
        vbb.lanewise(op, 16).intoArray(arr[end++], ind);
        vbb.lanewise(op, 19).intoArray(arr[end++], ind);
        vbb.lanewise(op, 24).intoArray(arr[end++], ind);
        return end;
    }

    static int shift_with_op_and_add(VectorOperators.Binary op,
                                     ByteVector vba, ByteVector vbb,
                                     byte arr[][], int end, int ind) {
        vba.add(vbb.lanewise(op, 0)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 1)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 8)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 13)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 16)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 19)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 24)).intoArray(arr[end++], ind);
        return end;
    }

    static void shift(byte arrBytes[][], boolean verify,
                      VectorSpecies<Byte> vSpecies, int vlen) {
        int start = vlen / 128 * NUM_OPS * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 8) {
            end = start;
            ByteVector vbb = ByteVector.fromArray(vSpecies, bytesB, i);
            end = shift_with_op(VectorOperators.LSHL, vbb, arrBytes, end, i);
            end = shift_with_op(VectorOperators.ASHR, vbb, arrBytes, end, i);
            end = shift_with_op(VectorOperators.LSHR, vbb, arrBytes, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("BYTE", Arrays.equals(tBytes[i], gBytes[i]), i, vlen);
            }
        }
    }

    static void shift_and_accumulate(byte arrBytes[][], boolean verify,
                                           VectorSpecies<Byte> vSpecies, int vlen) {
        int start = (ACCUMULATE_OP_S + vlen / 128 * NUM_OPS) * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 8) {
            end = start;
            ByteVector vba = ByteVector.fromArray(vSpecies, bytesA, i);
            ByteVector vbb = ByteVector.fromArray(vSpecies, bytesB, i);
            end = shift_with_op_and_add(VectorOperators.ASHR, vba, vbb, arrBytes, end, i);
            end = shift_with_op_and_add(VectorOperators.LSHR, vba, vbb, arrBytes, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("BYTE", Arrays.equals(tBytes[i], gBytes[i]), i, vlen);
            }
        }
    }

    /**
     * Tests for type short.
     */

    static int shift_with_op(VectorOperators.Binary op, ShortVector vbb,
                             short arr[][], int end, int ind) {
        vbb.lanewise(op, 0).intoArray(arr[end++], ind);
        vbb.lanewise(op, 9).intoArray(arr[end++], ind);
        vbb.lanewise(op, 16).intoArray(arr[end++], ind);
        vbb.lanewise(op, 27).intoArray(arr[end++], ind);
        vbb.lanewise(op, 32).intoArray(arr[end++], ind);
        vbb.lanewise(op, 43).intoArray(arr[end++], ind);
        vbb.lanewise(op, 48).intoArray(arr[end++], ind);
        return end;
    }

    static int shift_with_op_and_add(VectorOperators.Binary op,
                                     ShortVector vba, ShortVector vbb,
                                     short arr[][], int end, int ind) {
        vba.add(vbb.lanewise(op, 0)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 9)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 16)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 27)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 32)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 43)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 48)).intoArray(arr[end++], ind);
        return end;
    }

    static void shift(short arrShorts[][], boolean verify,
                      VectorSpecies<Short> vSpecies, int vlen) {
        int start = vlen / 128 * NUM_OPS * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 16) {
            end = start;
            ShortVector vbb = ShortVector.fromArray(vSpecies, shortsB, i);
            end = shift_with_op(VectorOperators.LSHL, vbb, arrShorts, end, i);
            end = shift_with_op(VectorOperators.ASHR, vbb, arrShorts, end, i);
            end = shift_with_op(VectorOperators.LSHR, vbb, arrShorts, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("SHORT", Arrays.equals(tShorts[i], gShorts[i]), i, vlen);
            }
        }
    }

    static void shift_and_accumulate(short arrShorts[][], boolean verify,
                                     VectorSpecies<Short> vSpecies, int vlen) {
        int start = (ACCUMULATE_OP_S + vlen / 128 * NUM_OPS) * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 16) {
            end = start;
            ShortVector vba = ShortVector.fromArray(vSpecies, shortsA, i);
            ShortVector vbb = ShortVector.fromArray(vSpecies, shortsB, i);
            end = shift_with_op_and_add(VectorOperators.ASHR, vba, vbb, arrShorts, end, i);
            end = shift_with_op_and_add(VectorOperators.LSHR, vba, vbb, arrShorts, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("SHORT", Arrays.equals(tShorts[i], gShorts[i]), i, vlen);
            }
        }
    }

    /**
     * Tests for type int.
     */

    static int shift_with_op(VectorOperators.Binary op, IntVector vbb,
                             int arr[][], int end, int ind) {
        vbb.lanewise(op, 0).intoArray(arr[end++], ind);
        vbb.lanewise(op, 17).intoArray(arr[end++], ind);
        vbb.lanewise(op, 32).intoArray(arr[end++], ind);
        vbb.lanewise(op, 53).intoArray(arr[end++], ind);
        vbb.lanewise(op, 64).intoArray(arr[end++], ind);
        vbb.lanewise(op, 76).intoArray(arr[end++], ind);
        vbb.lanewise(op, 96).intoArray(arr[end++], ind);
        return end;
    }

    static int shift_with_op_and_add(VectorOperators.Binary op,
                                     IntVector vba, IntVector vbb,
                                     int arr[][], int end, int ind) {
        vba.add(vbb.lanewise(op, 0)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 17)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 32)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 53)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 64)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 76)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 96)).intoArray(arr[end++], ind);
        return end;
    }

    static void shift(int arrIntegers[][], boolean verify,
                      VectorSpecies<Integer> vSpecies, int vlen) {
        int start = vlen / 128 * NUM_OPS * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 32) {
            end = start;
            IntVector vbb = IntVector.fromArray(vSpecies, integersB, i);
            end = shift_with_op(VectorOperators.LSHL, vbb, arrIntegers, end, i);
            end = shift_with_op(VectorOperators.ASHR, vbb, arrIntegers, end, i);
            end = shift_with_op(VectorOperators.LSHR, vbb, arrIntegers, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("INTEGER", Arrays.equals(tIntegers[i], gIntegers[i]), i, vlen);
            }
        }
    }

    static void shift_and_accumulate(int arrIntegers[][], boolean verify,
                                     VectorSpecies<Integer> vSpecies, int vlen) {
        int start = (ACCUMULATE_OP_S + vlen / 128 * NUM_OPS) * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 32) {
            end = start;
            IntVector vba = IntVector.fromArray(vSpecies, integersA, i);
            IntVector vbb = IntVector.fromArray(vSpecies, integersB, i);
            end = shift_with_op_and_add(VectorOperators.ASHR, vba, vbb, arrIntegers, end, i);
            end = shift_with_op_and_add(VectorOperators.LSHR, vba, vbb, arrIntegers, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("INTEGER", Arrays.equals(tIntegers[i], gIntegers[i]), i, vlen);
            }
        }
    }

    /**
     * Tests for type long.
     */

    static int shift_with_op(VectorOperators.Binary op, LongVector vbb,
                             long arr[][], int end, int ind) {
        vbb.lanewise(op, 0).intoArray(arr[end++], ind);
        vbb.lanewise(op, 37).intoArray(arr[end++], ind);
        vbb.lanewise(op, 64).intoArray(arr[end++], ind);
        vbb.lanewise(op, 99).intoArray(arr[end++], ind);
        vbb.lanewise(op, 128).intoArray(arr[end++], ind);
        vbb.lanewise(op, 157).intoArray(arr[end++], ind);
        vbb.lanewise(op, 192).intoArray(arr[end++], ind);
        return end;
    }

    static int shift_with_op_and_add(VectorOperators.Binary op,
                                     LongVector vba, LongVector vbb,
                                     long arr[][], int end, int ind) {
        vba.add(vbb.lanewise(op, 0)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 37)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 64)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 99)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 128)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 157)).intoArray(arr[end++], ind);
        vba.add(vbb.lanewise(op, 192)).intoArray(arr[end++], ind);
        return end;
    }

    static void shift(long arrLongs[][], boolean verify,
                      VectorSpecies<Long> vSpecies, int vlen) {
        int start = vlen / 128 * NUM_OPS * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 64) {
            end = start;
            LongVector vbb = LongVector.fromArray(vSpecies, longsB, i);
            end = shift_with_op(VectorOperators.LSHL, vbb, arrLongs, end, i);
            end = shift_with_op(VectorOperators.ASHR, vbb, arrLongs, end, i);
            end = shift_with_op(VectorOperators.LSHR, vbb, arrLongs, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("LONG", Arrays.equals(tLongs[i], gLongs[i]), i, vlen);
            }
        }
    }

    static void shift_and_accumulate(long arrLongs[][], boolean verify,
                                     VectorSpecies<Long> vSpecies, int vlen) {
        int start = (ACCUMULATE_OP_S + vlen / 128 * NUM_OPS) * MAX_TESTS_PER_OP, end = 0;

        for (int i = 0; i < ARR_LEN; i += vlen / 64) {
            end = start;
            LongVector vba = LongVector.fromArray(vSpecies, longsA, i);
            LongVector vbb = LongVector.fromArray(vSpecies, longsB, i);
            end = shift_with_op_and_add(VectorOperators.ASHR, vba, vbb, arrLongs, end, i);
            end = shift_with_op_and_add(VectorOperators.LSHR, vba, vbb, arrLongs, end, i);
        }

        if (verify) {
            for (int i = start; i < end; i++) {
                assertTrue("LONG", Arrays.equals(tLongs[i], gLongs[i]), i, vlen);
            }
        }
    }

    static void test_init() {
        int count = ARR_LEN;

        bytesA    = new byte[count];
        shortsA   = new short[count];
        integersA = new int[count];
        longsA    = new long[count];

        bytesB    = new byte[count];
        shortsB   = new short[count];
        integersB = new int[count];
        longsB    = new long[count];

        tBytes    = new byte[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];
        tShorts   = new short[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];
        tIntegers = new int[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];
        tLongs    = new long[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];

        gBytes    = new byte[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];
        gShorts   = new short[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];
        gIntegers = new int[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];
        gLongs    = new long[VLENS * MAX_TESTS_PER_OP * NUM_OPS][count];

        for (int i = 0; i < count; i++) {
            bytesA[i]    = (byte) r.nextInt();
            shortsA[i]   = (short) r.nextInt();
            integersA[i] = r.nextInt();
            longsA[i]    = r.nextLong();

            bytesB[i]    = (byte) r.nextInt();
            shortsB[i]   = (short) r.nextInt();
            integersB[i] = r.nextInt();
            longsB[i]    = r.nextLong();
        }

        shift(gBytes, false, byte64SPECIES,  64);
        shift(gBytes, false, byte128SPECIES, 128);
        shift_and_accumulate(gBytes, false, byte64SPECIES,  64);
        shift_and_accumulate(gBytes, false, byte128SPECIES, 128);

        shift(gShorts, false, short64SPECIES,  64);
        shift(gShorts, false, short128SPECIES, 128);
        shift_and_accumulate(gShorts, false, short64SPECIES,  64);
        shift_and_accumulate(gShorts, false, short128SPECIES, 128);

        shift(gIntegers, false, integer64SPECIES,  64);
        shift(gIntegers, false, integer128SPECIES, 128);
        shift_and_accumulate(gIntegers, false, integer64SPECIES,  64);
        shift_and_accumulate(gIntegers, false, integer128SPECIES, 128);

        shift(gLongs, false, long128SPECIES, 128);
        shift_and_accumulate(gLongs, false, long128SPECIES, 128);
    }

    static void assertTrue(String type, boolean okay, int i, int vlen) {
        int op = i % (MAX_TESTS_PER_OP * NUM_OPS) / MAX_TESTS_PER_OP;
        if (!okay) {
            allTestsPassed = false;
            if (!errMsg.toString().contains("type " + type + " index " + i)) {
                errMsg.append("type " + type + " index " + i + ", operation " + opNames[op] + ", vector length "+ vlen + ".\n");
            }
        }
    }
}
