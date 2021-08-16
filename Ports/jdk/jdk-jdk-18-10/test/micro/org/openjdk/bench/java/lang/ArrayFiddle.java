/*
 * Copyright 2019 Google Inc.  All Rights Reserved.
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

import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Explores the cost of copying the contents of one array to another, as is
 * commonly seen in collection classes that need to resize their backing
 * array, like ArrayList.
 *
 * We have multiple variations on copying, and we explore the cost of
 * clearing the old array, which might help for generational GCs.
 *
 * Benchmarks the operations in the ancient
 * JDK-6428387: array clone() much slower than Arrays.copyOf
 *
 * 2019 results on x86:
 *
 * The "simple" benchmarks below have the same performance, except that
 * simple_copyLoop is surprisingly 5x slower.  The array copying intrinsics
 * are very effective and a naive loop does not get optimized the same way.
 * OTOH there is no intrinsic for Arrays.fill but the naive array zeroing loop
 * *does* get optimized to something a little faster than than arraycopy.
 *
 * System.arraycopy and Arrays.fill have such outstanding performance that
 * one should use them to replace handwritten loops whenever possible.
 *
 * This benchmark is great for measuring cache effects, e.g. size=10^6 has 5x
 * the per-element cost of size=10^3 (See "The Myth of RAM".)
 *
 * (cd $(git rev-parse --show-toplevel) && for size in 3 16 999 999999; do make test TEST='micro:java.lang.ArrayFiddle' MICRO="FORK=2;WARMUP_ITER=4;ITER=4;OPTIONS=-opi $size -p size=$size" |& perl -ne 'print if /^Benchmark/ .. /^Finished running test/'; done)
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 1)
@Measurement(iterations = 4)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ArrayFiddle {
    @Param("999")
    public int size;

    public int largerSize;
    public Object[] data;
    public Object[] copy;

    @Setup
    public void setup() {
        largerSize = size + (size >> 1);

        data = new Object[size];
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = data.length; i--> 0; )
            data[i] = rnd.nextInt(256);

        copy = data.clone();
    }

    // --- "simple" benchmarks just make an array clone

    @Benchmark
    public Object[] simple_clone() {
        return data.clone();
    }

    @Benchmark
    public Object[] simple_copyOf() {
        return Arrays.copyOf(data, data.length);
    }

    @Benchmark
    public Object[] simple_arraycopy() {
        Object[] out = new Object[data.length];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }

    @Benchmark
    public Object[] simple_copyLoop() {
        final Object[] data = this.data;
        int len = data.length;
        Object[] out = new Object[len];
        for (int i = 0; i < len; i++)
            out[i] = data[i];
        return out;
    }

    // --- "grow" benchmarks have an output array that is larger

    private Object[] input_array() {
        System.arraycopy(data, 0, copy, 0, size);
        return copy;
    }

    @Benchmark
    public Object[] grow_copyLoop() {
        Object[] in = input_array();
        Object[] out = new Object[largerSize];
        for (int i = 0, len = in.length; i < len; i++)
            out[i] = in[i];
        return out;
    }

    @Benchmark
    public Object[] grow_copyZeroLoop() {
        Object[] in = input_array();
        Object[] out = new Object[largerSize];
        for (int i = 0, len = in.length; i < len; i++) {
            out[i] = in[i];
            in[i] = null;
        }
        return out;
    }

    @Benchmark
    public Object[] grow_arraycopy() {
        Object[] in = input_array();
        Object[] out = new Object[largerSize];
        System.arraycopy(in, 0, out, 0, size);
        return out;
    }

    @Benchmark
    public Object[] grow_arraycopy_fill() {
        Object[] in = input_array();
        Object[] out = new Object[largerSize];
        System.arraycopy(in, 0, out, 0, size);
        Arrays.fill(in, null);
        return out;
    }

    @Benchmark
    public Object[] grow_arraycopy_zeroLoop() {
        Object[] in = input_array();
        Object[] out = new Object[largerSize];
        System.arraycopy(in, 0, out, 0, size);
        for (int i = 0, len = in.length; i < len; i++)
            in[i] = null;
        return out;
    }

    @Benchmark
    public Object[] grow_copyOf() {
        Object[] in = input_array();
        Object[] out = Arrays.copyOf(in, largerSize);
        return out;
    }

    @Benchmark
    public Object[] grow_copyOf_fill() {
        Object[] in = input_array();
        Object[] out = Arrays.copyOf(in, largerSize);
        Arrays.fill(in, null);
        return out;
    }

    @Benchmark
    public Object[] grow_copyOf_zeroLoop() {
        Object[] in = input_array();
        Object[] out = Arrays.copyOf(in, largerSize);
        for (int i = 0, len = in.length; i < len; i++)
            in[i] = null;
        return out;
    }

}
