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
package org.openjdk.bench.java.util.stream.ops.ref;

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
public class SliceToList {

    @Param("10000")
    private int size;

    @Benchmark
    public List<String> seq_baseline() {
        return IntStream.range(0, size)
                .mapToObj(x -> "x")
                .toList();
    }

    @Benchmark
    public List<String> seq_limit() {
        return IntStream.range(0, size * 2)
                .mapToObj(x -> "x")
                .limit(size)
                .toList();
    }

    @Benchmark
    public List<String> seq_skipLimit() {
        return IntStream.range(0, size * 2)
                .mapToObj(x -> "x")
                .skip(1)
                .limit(size)
                .toList();
    }

    @Benchmark
    public List<String> par_baseline() {
        return IntStream.range(0, size)
                .parallel()
                .mapToObj(x -> "x")
                .toList();
    }

    @Benchmark
    public List<String> par_limit() {
        return IntStream.range(0, size * 2)
                .parallel()
                .mapToObj(x -> "x")
                .limit(size)
                .toList();
    }

    @Benchmark
    public List<String> par_skipLimit() {
        return IntStream.range(0, size * 2)
                .parallel()
                .mapToObj(x -> "x")
                .skip(1)
                .limit(size)
                .toList();
    }
}
