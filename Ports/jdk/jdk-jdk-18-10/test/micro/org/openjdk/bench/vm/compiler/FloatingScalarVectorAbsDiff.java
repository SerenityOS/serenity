/*
 * Copyright (c) 2020, Huawei Technologies Co., Ltd. All rights reserved.
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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;

import java.util.concurrent.TimeUnit;
import java.util.Random;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class FloatingScalarVectorAbsDiff {
    @Param({"1024"})
    public int count;

    private float[]  floatsA,  floatsB,  floatsD;
    private double[] doublesA, doublesB, doublesD;

    @Param("316731")
    private int seed;
    private Random r = new Random(seed);

    @Setup
    public void init() {
        floatsA  = new float[count];
        doublesA = new double[count];

        floatsB  = new float[count];
        doublesB = new double[count];

        floatsD  = new float[count];
        doublesD = new double[count];

        for (int i = 0; i < count; i++) {
            floatsA[i]  = r.nextFloat();
            doublesB[i] = r.nextDouble();

            floatsB[i]  = r.nextFloat();
            doublesB[i] = r.nextDouble();
        }
    }

    @Benchmark
    public void testVectorAbsDiffFloat() {
        for (int i = 0; i < count; i++) {
            floatsD[i] = Math.abs(floatsA[i] - floatsB[i]);
        }
    }

    @Benchmark
    public void testVectorAbsDiffDouble() {
        for (int i = 0; i < count; i++) {
            doublesD[i] = Math.abs(doublesA[i] - doublesB[i]);
        }
    }

    @Benchmark
    public void testScalarAbsDiffFloat(Blackhole bh) {
        float a = r.nextFloat();
        float b = r.nextFloat();

        for (int i = 0; i < count; i++) {
            a = Math.abs(a - b);
            b = Math.abs(b - a);
        }

        bh.consume(a + b);
    }

    @Benchmark
    public void testScalarAbsDiffDouble(Blackhole bh) {
        double a = r.nextDouble();
        double b = r.nextDouble();

        for (int i = 0; i < count; i++) {
            a = Math.abs(a - b);
            b = Math.abs(b - a);
        }

        bh.consume(a + b);
    }
}
