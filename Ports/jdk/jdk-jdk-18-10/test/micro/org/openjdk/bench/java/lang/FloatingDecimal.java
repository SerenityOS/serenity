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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Tests for sun.misc.FloatingDecimal. Performs floating point number to String conversions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class FloatingDecimal {

    private double[] randomArray, twoDecimalsArray, integerArray;
    private static final int TESTSIZE = 1000;

    @Setup
    public void setup() {
        Random r = new Random(1123);
        randomArray = new double[TESTSIZE];
        twoDecimalsArray = new double[TESTSIZE];
        integerArray = new double[TESTSIZE];
        for (int i = 0; i < TESTSIZE; i++) {
            randomArray[i] = r.nextDouble() * 10000.0D;
            twoDecimalsArray[i] = ((double) (10000 - r.nextInt(20000))) / 100;
            integerArray[i] = (double) (100 - r.nextInt(200));
        }
    }

    /** Tests Double.toString on double values generated from Random.nextDouble() */
    @Benchmark
    @OperationsPerInvocation(TESTSIZE)
    public void randomDoubleToString(Blackhole bh) {
        for (double d : randomArray) {
            bh.consume(Double.toString(d));
        }
    }

    /** Tests Double.toString on double values that are integers between -100 and 100. */
    @Benchmark
    @OperationsPerInvocation(TESTSIZE)
    public void integerDoubleToString(Blackhole bh) {
        for (double d : integerArray) {
            bh.consume(Double.toString(d));
        }
    }

    /** Tests Double.toString on double values that are between -100 and 100 and have two decimal digits. */
    @Benchmark
    @OperationsPerInvocation(TESTSIZE)
    public void twoDecimalsDoubleToString(Blackhole bh) {
        for (double d : twoDecimalsArray) {
            bh.consume(Double.toString(d));
        }
    }

}
