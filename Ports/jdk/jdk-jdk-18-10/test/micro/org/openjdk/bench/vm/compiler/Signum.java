/*
 * Copyright (c) Intel, 2021 All rights reserved.
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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class Signum {

    private final int ITERATIONS = 15000;

    private double doubleValue = 1D;
    private float floatValue = 1F;

    private static final float[] float_values = {
        123.4f,
        -56.7f,
        7e30f,
        -0.3e30f,
        Float.MAX_VALUE,
        -Float.MAX_VALUE,
        Float.MIN_VALUE,
        -Float.MIN_VALUE,
        0.0f,
        -0.0f,
        Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY,
        Float.NaN,
        Float.MIN_NORMAL,
        -Float.MIN_NORMAL,
        0x0.0002P-126f,
        -0x0.0002P-126f
    };

    private static final double[] double_values = {
        123.4d,
        -56.7d,
        7e30d,
        -0.3e30d,
        Double.MAX_VALUE,
        -Double.MAX_VALUE,
        Double.MIN_VALUE,
        -Double.MIN_VALUE,
        0.0d,
        -0.0d,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.NaN,
        Double.MIN_NORMAL,
        -Double.MIN_NORMAL,
        0x0.00000001P-1022,
        -0x0.00000001P-1022,
    };

    private static double Signum_Kernel(double data)
    {
        return Math.signum(data);
    }

    private static float Signum_Kernel(float data)
    {
        return Math.signum(data);
    }

    @Benchmark
    @OperationsPerInvocation(ITERATIONS * 17)
    public void _1_signumFloatTest(Blackhole bh) {
        for (int i = 0; i < ITERATIONS; i++) {
            for (float f : float_values) {
                bh.consume(Signum_Kernel(f));
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(ITERATIONS * 17)
    public void _2_overheadFloat(Blackhole bh) {
        for (int i = 0; i < ITERATIONS; i++) {
            for (float f : float_values) {
                bh.consume(f);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(ITERATIONS * 17)
    public void _3_signumDoubleTest(Blackhole bh) {
        for (int i = 0; i < ITERATIONS; i++) {
            for (double d : double_values) {
                bh.consume(Signum_Kernel(d));
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(ITERATIONS * 17)
    public void _4_overheadDouble(Blackhole bh) {
        for (int i = 0; i < ITERATIONS; i++) {
            for (double d : double_values) {
                bh.consume(d);
            }
        }
    }
}
