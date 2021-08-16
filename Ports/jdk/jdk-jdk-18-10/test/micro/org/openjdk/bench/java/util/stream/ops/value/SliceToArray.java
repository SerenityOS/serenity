/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util.stream.ops.value;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Benchmark for limit()/skip() operation in sized streams.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class SliceToArray {

    @Param("10000")
    private int size;

    @Benchmark
    public int[] seq_baseline() {
        return IntStream.range(0, size)
                .toArray();
    }

    @Benchmark
    public int[] seq_limit() {
        return IntStream.range(0, size * 2)
                .limit(size)
                .toArray();
    }

    @Benchmark
    public int[] seq_skipLimit() {
        return IntStream.range(0, size * 2)
                .skip(1)
                .limit(size)
                .toArray();
    }

    @Benchmark
    public int[] par_baseline() {
        return IntStream.range(0, size)
                .parallel()
                .toArray();
    }

    @Benchmark
    public int[] par_limit() {
        return IntStream.range(0, size * 2)
                .parallel()
                .limit(size)
                .toArray();
    }

    @Benchmark
    public int[] par_skipLimit() {
        return IntStream.range(0, size * 2)
                .parallel()
                .skip(1)
                .limit(size)
                .toArray();
    }
}
