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
package org.openjdk.bench.java.util.stream.pipeline;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

/**
 * Benchmark tests the pipeline construction costs.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class PipelineSetup {

    /**
     * This is one of the few benchmarks where measuring running time makes sense.
     */

    @Param("100000")
    private int size;

    @Benchmark
    public Object baseline_newObject() {
        return new Object();
    }

    @Benchmark
    public LongStream seq_test00() {
        return LongStream.range(0, size);
    }

    @Benchmark
    public LongStream seq_test01() {
        return LongStream.range(0, size)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream seq_test02() {
        return LongStream.range(0, size)
                .filter((x) -> false)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream seq_test04() {
        return LongStream.range(0, size)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream seq_test08() {
        return LongStream.range(0, size)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream seq_test16() {
        return LongStream.range(0, size)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream par_test00() {
        return LongStream.range(0, size).parallel();
    }

    @Benchmark
    public LongStream par_test01() {
        return LongStream.range(0, size).parallel()
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream par_test02() {
        return LongStream.range(0, size).parallel()
                .filter((x) -> false)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream par_test04() {
        return LongStream.range(0, size).parallel()
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream par_test08() {
        return LongStream.range(0, size).parallel()
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false);
    }

    @Benchmark
    public LongStream par_test16() {
        return LongStream.range(0, size).parallel()
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)

                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false)
                .filter((x) -> false);
    }

}
