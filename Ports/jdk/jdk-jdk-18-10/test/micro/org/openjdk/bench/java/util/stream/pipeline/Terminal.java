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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;

/**
 * Benchmark for forEach()/iterator()/into() operations;
 * Testing which one is faster for semantically-equvalent operations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class Terminal {

    /**
     * Implementation notes:
     *   - parallel version requires thread-safe sink, we use the same for sequential version for better comparison
     *   - operations are explicit inner classes to untangle unwanted lambda effects
     */

    @Param("100000")
    private int size;

    private LongAdder sink;
    private LongConsumer block;

    @Setup
    public void setup() {
        sink = new LongAdder();
        block = new LongConsumer() {
            @Override
            public void accept(long v) {
                sink.add(v);
            }
        };
    }

    @Benchmark
    public long baseline_prim_acc() {
        long s = 0;
        for (long l = 0L; l < size; l++) {
            s += l;
        }
        sink.add(s);
        return sink.sum();
    }

    @Benchmark
    public long baseline_prim_sink() {
        for (long l = 0L; l < size; l++) {
            sink.add(l);
        }
        return sink.sum();
    }

    @Benchmark
    public long baseline_iterator_acc() {
        long s = 0;
        for (Iterator<Long> iterator = LongStream.range(0, size).boxed().iterator(); iterator.hasNext(); ) {
            Long l = iterator.next();
            s += l;
        }
        sink.add(s);
        return sink.sum();
    }

    @Benchmark
    public long baseline_iterator_sink() {
        for (Iterator<Long> iterator = LongStream.range(0, size).boxed().iterator(); iterator.hasNext(); ) {
            sink.add(iterator.next());
        }
        return sink.sum();
    }

    @Benchmark
    public long seq_iterator() {
        Iterator<Long> i = LongStream.range(0, size).boxed().iterator();
        while (i.hasNext()) {
            sink.add(i.next());
        }
        return sink.sum();
    }

    @Benchmark
    public long par_iterator() {
        Iterator<Long> i = LongStream.range(0, size).parallel().boxed().iterator();
        while (i.hasNext()) {
            sink.add(i.next());
        }
        return sink.sum();
    }

    @Benchmark
    public long seq_forEach() {
        LongStream.range(1, size).forEach(block);
        return sink.sum();
    }

    @Benchmark
    public long par_forEach() {
        LongStream.range(1, size).parallel().forEach(block);
        return sink.sum();
    }

    @Benchmark
    public long seq_into() {
        return LongStream.range(1, size)
                .collect(LongAdder::new, LongAdder::add, (la1, la2) -> la1.add(la2.sum())).sum();
    }

    @Benchmark
    public long par_into() {
        return LongStream.range(1, size).parallel()
                .collect(LongAdder::new, LongAdder::add, (la1, la2) -> la1.add(la2.sum())).sum();
    }

}
