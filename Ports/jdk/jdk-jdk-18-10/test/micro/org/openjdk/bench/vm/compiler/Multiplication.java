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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Tests speed of multiplication calculations with constants.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Multiplication {

    @Param("500")
    private int arraySize;

    private long[] longArraySmall, longArrayBig;

    @Setup
    public void setupSubclass() {
        longArraySmall = new long[arraySize];
        longArrayBig = new long[arraySize];

        /*
         * small values always have higher 32 bits cleared. big values always
         * have higher 32 bits set.
         */
        for (int i = 0; i < arraySize; i++) {
            longArraySmall[i] = 100L * i + i;
            longArrayBig[i] = ((100L * i + i) << 32) + 4543 + i * 4;
        }
    }

    /* helper for small constant benchmarks. */
    private static long smallConstantHelper(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum += value * 453543L;
        }
        return sum;
    }

    /* helper for big constant benchmarks. */
    private static long bigConstantHelper(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum += value * 4554345533543L;
        }
        return sum;
    }

    /**
     * Test multiplications of longs. One of the operands is a small constant and the other is a variable that always is
     * small.
     */
    @Benchmark
    public long testLongSmallVariableSmallConstantMul() {
        return smallConstantHelper(longArraySmall);
    }

    /**
     * Test multiplications of longs. One of the operands is a big constant and the other is a variable that always is
     * small.
     */
    @Benchmark
    public long testLongSmallVariableBigConstantMul() {
        return bigConstantHelper(longArraySmall);
    }

    /**
     * Test multiplications of longs. One of the operands is a small constant and the other is a variable that always is
     * big.
     */
    @Benchmark
    public long testLongBigVariableSmallConstantMul() {
        return smallConstantHelper(longArrayBig);
    }

    /**
     * Test multiplications of longs. One of the operands is a big constant and the other is a variable that always is
     * big.
     */
    @Benchmark
    public long testLongBigVariableBigConstantMul() {
        return bigConstantHelper(longArrayBig);
    }

}
