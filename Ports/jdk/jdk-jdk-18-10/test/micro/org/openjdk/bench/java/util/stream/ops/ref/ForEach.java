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
package org.openjdk.bench.java.util.stream.ops.ref;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.LongStream;

/**
 * Benchmark for forEach() operations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ForEach {

    /**
     * Implementation notes:
     *   - parallel version requires thread-safe sink, we use the same for sequential version for better comparison
     *   - operations are explicit inner classes to untangle unwanted lambda effects
     */

    @Param("100000")
    private int size;

    private LongAdder sink;
    private Consumer<Long> b1, b2, b3;

    @Setup
    public void setup() {
        sink = new LongAdder();
        b1 = new Consumer<Long>() {
            @Override
            public void accept(Long v) {
                sink.add(v);
            }
        };
        b2 = new Consumer<Long>() {
            @Override
            public void accept(Long v) {
                sink.add(v);
            }
        };
        b3 = new Consumer<Long>() {
            @Override
            public void accept(Long v) {
                sink.add(v);
            }
        };
    }

    @Benchmark
    public long seq_invoke() {
        LongStream.range(0, size).boxed().forEach(b1);
        return sink.sum();
    }

    @Benchmark
    public long seq_chain111() {
        LongStream.range(0, size).boxed().forEach(b1);
        LongStream.range(0, size).boxed().forEach(b1);
        LongStream.range(0, size).boxed().forEach(b1);
        return sink.sum();
    }

    @Benchmark
    public long seq_chain123() {
        LongStream.range(0, size).boxed().forEach(b1);
        LongStream.range(0, size).boxed().forEach(b2);
        LongStream.range(0, size).boxed().forEach(b3);
        return sink.sum();
    }

    @Benchmark
    public long par_invoke() {
        LongStream.range(0, size).parallel().boxed().forEach(b1);
        return sink.sum();
    }

    @Benchmark
    public long par_chain111() {
        LongStream.range(0, size).parallel().boxed().forEach(b1);
        LongStream.range(0, size).parallel().boxed().forEach(b1);
        LongStream.range(0, size).parallel().boxed().forEach(b1);
        return sink.sum();
    }

    @Benchmark
    public long par_chain123() {
        LongStream.range(0, size).parallel().boxed().forEach(b1);
        LongStream.range(0, size).parallel().boxed().forEach(b2);
        LongStream.range(0, size).parallel().boxed().forEach(b3);
        return sink.sum();
    }

}
