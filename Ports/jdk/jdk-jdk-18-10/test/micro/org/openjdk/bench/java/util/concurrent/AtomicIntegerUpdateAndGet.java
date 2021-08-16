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
package org.openjdk.bench.java.util.concurrent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 * Benchmarks assesses the performance of new Atomic* API.
 *
 * Implementation notes:
 *   - atomic instances are padded to eliminate false sharing
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class AtomicIntegerUpdateAndGet {

    private PaddedAtomicInteger count;
    private int value = 42;
    private IntUnaryOperator captureOp;
    private IntUnaryOperator noCaptureOp;

    @Setup
    public void setup() {
        count = new PaddedAtomicInteger();
        noCaptureOp = new IntUnaryOperator() {
            public int applyAsInt(int v) {
                return v + 42;
            }
        };
        captureOp = new IntUnaryOperator() {
            public int applyAsInt(int v) {
                return v + value;
            }
        };
    }

    @Benchmark
    public int testAddAndGet() {
        return count.addAndGet(42);
    }

    @Benchmark
    public int testInnerNoCapture() {
        return count.updateAndGet(new IntUnaryOperator() {
            public int applyAsInt(int v) {
                return v + 42;
            }
        });
    }

    @Benchmark
    public int testInnerCapture() {
        return count.updateAndGet(new IntUnaryOperator() {
            public int applyAsInt(int v) {
                return v + value;
            }
        });
    }

    @Benchmark
    public int testInnerCaptureCached() {
        return count.updateAndGet(captureOp);
    }

    @Benchmark
    public int testInnerNoCaptureCached() {
        return count.updateAndGet(noCaptureOp);
    }

    @Benchmark
    public int testLambdaNoCapture() {
        return count.updateAndGet(x -> x + 42);
    }

    @Benchmark
    public int testLambdaCapture() {
        return count.updateAndGet(x -> x + value);
    }

    private static class PaddedAtomicInteger extends AtomicInteger {
        private volatile long pad00, pad01, pad02, pad03, pad04, pad05, pad06, pad07;
        private volatile long pad10, pad11, pad12, pad13, pad14, pad15, pad16, pad17;
    }

}
