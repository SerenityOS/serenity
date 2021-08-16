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
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;

/**
 * Benchmark tests the pipeline fusion abilities.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class PipelineSeqMultiple {

    @Param("100000")
    private int size;

    @Benchmark
    public Object bulk_into_anon() {
        return LongStream.range(0, size)
                .filter((x) -> true)
                .filter((x) -> true)
                .filter((x) -> true)
                .filter((x) -> true)
                .filter((x) -> true)
                .filter((x) -> true)
                .filter((x) -> true)
                .filter((x) -> false)
                .collect(LongAdder::new, LongAdder::add, (la1, la2) -> la1.add(la2.sum())).sum();
    }

    @Benchmark
    public Object bulk_into_named() {
        LongPredicate t = (x) -> true;
        LongPredicate f = (x) -> false;
        return LongStream.range(0, size)
                .filter(t)
                .filter(t)
                .filter(t)
                .filter(t)
                .filter(t)
                .filter(t)
                .filter(t)
                .filter(f)
                .collect(LongAdder::new, LongAdder::add, (la1, la2) -> la1.add(la2.sum())).sum();
    }


    @Benchmark
    public Object bulk_foreach_anon() {
        LongAdder adder = new LongAdder();
        LongStream.range(0, size).forEach((l) -> {
            if (((LongPredicate) (x) -> true).test(l))
            if (((LongPredicate) (x) -> true).test(l))
            if (((LongPredicate) (x) -> true).test(l))
            if (((LongPredicate) (x) -> true).test(l))
            if (((LongPredicate) (x) -> true).test(l))
            if (((LongPredicate) (x) -> true).test(l))
            if (((LongPredicate) (x) -> true).test(l))
            if (((LongPredicate) (x) -> false).test(l))
                adder.add(l);
        });
        return adder.sum();
    }


    @Benchmark
    public Object bulk_foreach_named() {
        LongAdder adder = new LongAdder();
        LongPredicate t = (x) -> true;
        LongPredicate f = (x) -> false;
        LongStream.range(0, size).forEach((l) -> {
            if (t.test(l))
            if (t.test(l))
            if (t.test(l))
            if (t.test(l))
            if (t.test(l))
            if (t.test(l))
            if (t.test(l))
            if (f.test(l))
                adder.add(l);
        });
        return adder.sum();
    }

    @Benchmark
    public Object bulk_ifs() {
        LongAdder adder = new LongAdder();
        LongStream.range(0, size).forEach((l) -> {
            if (true)
            if (true)
            if (true)
            if (true)
            if (true)
            if (true)
            if (true)
            if (false)
                adder.add(l);
        });
        return adder.sum();
    }

}
