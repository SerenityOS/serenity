/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;

import java.util.concurrent.TimeUnit;
import java.util.Random;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class VectorReductionFloatingMinMax {
    @Param({"512"})
    public int COUNT_DOUBLE;

    @Param({"3"})
    public int COUNT_FLOAT;

    private float[]  floatsA;
    private float[]  floatsB;
    private double[] doublesA;
    private double[] doublesB;

    @Param("0")
    private int seed;
    private Random r = new Random(seed);

    @Setup
    public void init() {
        floatsA = new float[COUNT_FLOAT];
        floatsB = new float[COUNT_FLOAT];
        doublesA = new double[COUNT_DOUBLE];
        doublesB = new double[COUNT_DOUBLE];

        for (int i = 0; i < COUNT_FLOAT; i++) {
            floatsA[i] = r.nextFloat();
            floatsB[i] = r.nextFloat();
        }
        for (int i = 0; i < COUNT_DOUBLE; i++) {
            doublesA[i] = r.nextDouble();
            doublesB[i] = r.nextDouble();
        }
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:-SuperWordLoopUnrollAnalysis"})
    public void maxRedF(Blackhole bh) {
        float max = 0.0f;
        for (int i = 0; i < COUNT_FLOAT; i++) {
            max = Math.max(max, Math.abs(floatsA[i] - floatsB[i]));
        }
        bh.consume(max);
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:-SuperWordLoopUnrollAnalysis"})
    public void minRedF(Blackhole bh) {
        float min = 0.0f;
        for (int i = 0; i < COUNT_FLOAT; i++) {
            min = Math.min(min, Math.abs(floatsA[i] - floatsB[i]));
        }
        bh.consume(min);
    }

    @Benchmark
    public void maxRedD(Blackhole bh) {
        double max = 0.0d;
        for (int i = 0; i < COUNT_DOUBLE; i++) {
            max = Math.max(max, Math.abs(doublesA[i] - doublesB[i]));
        }
        bh.consume(max);
    }

    @Benchmark
    public void minRedD(Blackhole bh) {
        double min = 0.0d;
        for (int i = 0; i < COUNT_DOUBLE; i++) {
            min = Math.min(min, Math.abs(doublesA[i] - doublesB[i]));
        }
        bh.consume(min);
    }
}
