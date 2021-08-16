/*
 * Copyright (c) 2021, Huawei Technologies Co. Ltd. All rights reserved.
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
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class MaxMinOptimizeTest {
    private static final int COUNT = 100000;

    private float[] floats_a = new float[COUNT];
    private float[] floats_b = new float[COUNT];
    private double[] doubles_a = new double[COUNT];
    private double[] doubles_b = new double[COUNT];

    private Random r = new Random();

    @Setup
    public void init() {
        for (int i=0; i<COUNT; i++) {
            floats_a[i] = r.nextFloat();
            floats_b[i] = r.nextFloat();
            doubles_a[i] = r.nextDouble();
            doubles_b[i] = r.nextDouble();
        }
    }

    @Benchmark
    public void fAdd(Blackhole bh) {
        float sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += fAddBench(floats_a[i], floats_b[i]);
        bh.consume(sum);
    }

    @Benchmark
    public void fMul(Blackhole bh) {
        float sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += fMulBench(floats_a[i], floats_b[i]);
        bh.consume(sum);
    }

    @Benchmark
    public void fMax(Blackhole bh) {
        float sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += fMaxBench(floats_a[i], floats_b[i]);
        bh.consume(sum);
    }

    @Benchmark
    public void fMin(Blackhole bh) {
        float sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += fMinBench(floats_a[i], floats_b[i]);
        bh.consume(sum);
    }

    private float fAddBench(float a, float b) {
        return Math.max(a, b) + Math.min(a, b);
    }

    private float fMulBench(float a, float b) {
        return Math.max(a, b) * Math.min(a, b);
    }

    private float fMaxBench(float a, float b) {
        return Math.max(Math.max(a, b), Math.min(a, b));
    }

    private float fMinBench(float a, float b) {
        return Math.min(Math.max(a, b), Math.min(a, b));
    }


    @Benchmark
    public void dAdd(Blackhole bh) {
        double sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += dAddBench(doubles_a[i], doubles_b[i]);
        bh.consume(sum);
    }

    @Benchmark
    public void dMul(Blackhole bh) {
        double sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += dMulBench(doubles_a[i], doubles_b[i]);
        bh.consume(sum);
    }

    @Benchmark
    public void dMax(Blackhole bh) {
        double sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += dMaxBench(doubles_a[i], doubles_b[i]);
        bh.consume(sum);
    }

    @Benchmark
    public void dMin(Blackhole bh) {
        double sum = 0;
        for (int i=0; i<COUNT; i++)
            sum += dMinBench(doubles_a[i], doubles_b[i]);
        bh.consume(sum);
    }

    private double dAddBench(double a, double b) {
        return Math.max(a, b) + Math.min(a, b);
    }

    private double dMulBench(double a, double b) {
        return Math.max(a, b) * Math.min(a, b);
    }

    private double dMaxBench(double a, double b) {
        return Math.max(Math.max(a, b), Math.min(a, b));
    }

    private double dMinBench(double a, double b) {
        return Math.min(Math.max(a, b), Math.min(a, b));
    }
}
