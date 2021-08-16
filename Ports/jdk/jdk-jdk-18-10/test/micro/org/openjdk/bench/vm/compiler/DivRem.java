/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Tests speed of division and remainder calculations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class DivRem {

    private static final int ARRAYSIZE = 500;

    /* instance fields for the constant int division tests. */
    public int[] intValues, intValues2;

    /* instance fields for the constant long division tests. */
    public long[] longValues, longValues2;

    /* instance fields for the tests using the testdr-method. */
    public long[] drLongValues1, drLongValues2;

    public long[] drLongValuesAsInts1, drLongValuesAsInts2;

    @Setup
    public void setupSubclass() {
        Random r = new Random(4711);

        intValues = new int[ARRAYSIZE];
        intValues2 = new int[ARRAYSIZE];
        longValues = new long[ARRAYSIZE];
        longValues2 = new long[ARRAYSIZE];

        for (int i = 0; i < ARRAYSIZE; i++) {
            intValues[i] = r.nextInt();
            if (intValues[i] == 0) {
                intValues[i] = 5353;
            }
            intValues2[i] = r.nextInt();

            longValues[i] = r.nextLong();
            if (longValues[i] == 0) {
                longValues[i] = 5353L;
            }
            longValues2[i] = r.nextLong();
        }

        /* generate random longs for 32-64 tests */

        drLongValues1 = new long[ARRAYSIZE];
        drLongValues2 = new long[ARRAYSIZE];
        drLongValuesAsInts1 = new long[ARRAYSIZE];
        drLongValuesAsInts2 = new long[ARRAYSIZE];
        for (int i = 0; i < ARRAYSIZE; i++) {
            long l = r.nextLong();
            if (l == 0L) {
                l++;
            }
            drLongValues1[i] = l;
            drLongValuesAsInts1[i] = (long) (int) l;
            l = r.nextLong();
            if (l == 0L) {
                l++;
            }
            drLongValues2[i] = l;
            drLongValuesAsInts2[i] = (long) (int) l;
        }
    }

    /**
     * Tests integer division with a constant divisor. Hopefully the JVM will do a Granlund-Montgomery and convert it to
     * a multiplication instead.
     */
    @Benchmark
    public int testIntDivConstantDivisor() {
        int dummy = 0;
        for (int i = 0; i < intValues.length; i++) {
            dummy += intValues[i] / 49;
        }
        return dummy;
    }

    /**
     * Tests long division with a constant divisor. Hopefully the JVM will do a Granlund-Montgomery and convert it to a
     * multiplication instead.
     */
    @Benchmark
    public long testLongDivConstantDivisor() {
        long dummy = 0;
        for (int i = 0; i < longValues.length; i++) {
            dummy += longValues[i] / 49L + longValues[i] / 0x4949494949L;
        }
        return dummy;
    }

    /**
     * Tests integer remainder with a constant divisor. Hopefully the JVM will do a Granlund-Montgomery and convert it to
     * two multiplications instead.
     */
    @Benchmark
    public int testIntRemConstantDivisor() {
        int dummy = 0;
        for (int i = 0; i < intValues.length; i++) {
            dummy += intValues[i] % 49;
        }
        return dummy;
    }

    /**
     * Tests long division with a constant divisor. Hopefully the JVM will do a Granlund-Montgomery and convert it to a
     * multiplication instead.
     */
    @Benchmark
    public long testLongRemConstantDivisor() {
        long dummy = 0;
        for (int i = 0; i < longValues.length; i++) {
            dummy += longValues[i] % 49L + longValues[i] % 0x4949494949L;
        }
        return dummy;
    }

    /**
     * Tests integer division with a variable divisor. This benchmark is mainly here to be a comparison against the
     * benchmark that performs both divisions and remainder calculations.
     */
    @Benchmark
    public int testIntDivVariableDivisor() {
        int dummy = 0;
        for (int i = 0; i < intValues.length; i++) {
            dummy += intValues2[i] / intValues[i];
        }
        return dummy;
    }

    /**
     * Tests integer division and remainder with a variable divisor. Both calculations are performed with the same
     * divisor, so a JVM should not have to perform two complex calculations. Either a division followed by a
     * multiplication, or on X86 using idiv, where the reminder is also returned from the idiv instruction.
     */
    @Benchmark
    public int testIntDivRemVariableDivisor() {
        int dummy = 0;
        for (int i = 0; i < intValues.length; i++) {
            dummy += intValues2[i] / intValues[i];
            dummy += intValues2[i] % intValues[i];
        }
        return dummy;
    }

    @Benchmark
    public long test64DivRem64() {
        long dummy = 0;
        for (int i = 0; i < drLongValues1.length; i++) {
            long l1 = drLongValues1[i];
            long l2 = drLongValues2[i];
            dummy += l1 / l2;
            dummy += l1 % l2;
        }
        return dummy;
    }

    @Benchmark
    public long test32DivRem32() {
        long dummy = 0;
        for (int i = 0; i < drLongValuesAsInts1.length; i++) {
            long l1 = drLongValuesAsInts1[i];
            long l2 = drLongValuesAsInts2[i];
            dummy += l1 / l2;
            dummy += l1 % l2;
        }
        return dummy;
    }

    @Benchmark
    public long test64DivRem32() {
        long dummy = 0;
        for (int i = 0; i < drLongValues1.length; i++) {
            long l1 = drLongValues1[i];
            long l2 = drLongValuesAsInts2[i];
            dummy += l1 / l2;
            dummy += l1 % l2;
        }
        return dummy;
    }
}
