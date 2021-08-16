/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
public class FpMinMaxIntrinsics {
    private static final int COUNT = 1000;

    private double[] doubles = new double[COUNT];
    private float[] floats = new float[COUNT];

    private int c1, c2, s1, s2;

    private Random r = new Random();

    @Setup
    public void init() {
        c1 = s1 = step();
        c2 = COUNT - (s2 = step());

        for (int i=0; i<COUNT; i++) {
            floats[i] = r.nextFloat();
            doubles[i] = r.nextDouble();
        }
    }

    private int step() {
        return (r.nextInt() & 0xf) + 1;
    }

    @Benchmark
    public void dMax(Blackhole bh) {
        for (int i=0; i<COUNT; i++)
            bh.consume(dMaxBench());
    }

    @Benchmark
    public void dMin(Blackhole bh) {
        for (int i=0; i<COUNT; i++)
            bh.consume(dMinBench());
    }

    @Benchmark
    public void fMax(Blackhole bh) {
        for (int i=0; i<COUNT; i++)
            bh.consume(fMaxBench());
    }

    @Benchmark
    public void fMin(Blackhole bh) {
        for (int i=0; i<COUNT; i++)
            bh.consume(fMinBench());
    }

    private double dMaxBench() {
        inc();
        return Math.max(doubles[c1], doubles[c2]);
    }

    private double dMinBench() {
        inc();
        return Math.min(doubles[c1], doubles[c2]);
    }

    private float fMaxBench() {
        inc();
        return Math.max(floats[c1], floats[c2]);
    }

    private float fMinBench() {
        inc();
        return Math.min(floats[c1], floats[c2]);
    }

    private void inc() {
        c1 = c1 + s1 < COUNT ? c1 + s1 : (s1 = step());
        c2 = c2 - s2 > 0 ? c2 - s2 : COUNT - (s2 = step());
    }

    @Benchmark
    public float fMinReduce() {
        float result = Float.MAX_VALUE;

        for (int i=0; i<COUNT; i++)
            result = Math.min(result, floats[i]);

        return result;
    }

    @Benchmark
    public double dMinReduce() {
        double result = Double.MAX_VALUE;

        for (int i=0; i<COUNT; i++)
            result = Math.min(result, doubles[i]);

        return result;
    }
}
