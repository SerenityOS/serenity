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
package org.openjdk.bench.java.util.stream.tasks.IntegerDuplicate;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This benchmark assesses flatMap() performance.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class Bulk {

    /**
     * Implementation notes:
     *   - parallel versions need to use special sink to get the values
     */

    private IntegerDuplicateProblem problem;
    private LongAdder adder;

    @Setup
    public void setup() {
        problem = new IntegerDuplicateProblem();
        adder = new LongAdder();
    }

    @Benchmark
    public long hm_seq() {
        for (Integer i : problem.get()) {
            adder.add(i);
            adder.add(i);
        }
        return adder.sum();
    }

    @Benchmark
    public long hm_par() {
        new Task(problem.get(), adder, 0, problem.get().length).invoke();
        return adder.sum();
    }

    @Benchmark
    public long bulk_seq_inner() {
        return Arrays.stream(problem.get()).flatMap(new Function<Integer, Stream<Integer>>() {
            @Override
            public Stream<Integer> apply(Integer integer) {
                return Collections.nCopies(2, integer).stream();
            }
        }).collect(LongAdder::new, LongAdder::add, (la1, la2) -> la1.add(la2.sum())).sum();
    }

    @Benchmark
    public long bulk_par_inner() {
        return Arrays.stream(problem.get()).parallel().flatMap(new Function<Integer, Stream<Integer>>() {
            @Override
            public Stream<Integer> apply(Integer integer) {
                return Collections.nCopies(2, integer).stream();
            }
        }).collect(LongAdder::new, LongAdder::add, (la1, la2) -> la1.add(la2.sum())).sum();
    }

    public static class Task extends RecursiveAction {
        private static final int FORK_LIMIT = 500;

        private final Integer[] data;
        private final LongAdder sink;
        private final int start;
        private final int end;

        Task(Integer[] w, LongAdder sink, int start, int end) {
            this.data = w;
            this.sink = sink;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int size = end - start;
            if (size > FORK_LIMIT) {
                int mid = start + size / 2;
                Task t1 = new Task(data, sink, start, mid);
                Task t2 = new Task(data, sink, mid, end);
                t1.fork();
                t2.invoke();
                t1.join();
            } else {
                for (int i = start; i < end; i++) {
                    sink.add(i);
                    sink.add(i);
                }
            }
        }
    }

}
