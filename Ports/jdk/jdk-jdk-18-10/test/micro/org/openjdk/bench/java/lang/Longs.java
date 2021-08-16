/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class Longs {

    @Param("500")
    private int size;

    private long[] longArraySmall;
    private long[] longArrayBig;

    @Setup
    public void setup() {
        longArraySmall = new long[size];
        longArrayBig = new long[size];
        for (int i = 0; i < size; i++) {
            longArraySmall[i] = 100L * i + i + 103L;
            longArrayBig[i] = ((100L * i + i) << 32) + 4543 + i * 4;
        }
    }

    /** Performs toString on small values, just a couple of digits. */
    @Benchmark
    public void toStringSmall(Blackhole bh) {
        for (long value : longArraySmall) {
            bh.consume(Long.toString(value));
        }
    }

    /** Performs toString on large values, around 10 digits. */
    @Benchmark
    public void toStringBig(Blackhole bh) {
        for (long value : longArrayBig) {
            bh.consume(Long.toString(value));
        }
    }

    /*
     * Have them public to avoid total unrolling
     */
    public int innerLoops = 1500;

    @Benchmark
    public long repetitiveSubtraction() {
        long x = 127, dx = 0;

        for (int i = 0; i < innerLoops; i++) {
            x -= dx;
            dx = (dx - x);
        }
        return x;
    }
}
