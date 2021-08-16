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
package org.openjdk.bench.java.util.stream.tasks.PrimesFilter.t100;

import org.openjdk.bench.java.util.stream.tasks.PrimesFilter.PrimesProblem;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * This benchmark evaluates find all prime numbers in a range.
 *
 * filter()...into() actions are benchmarked.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class Bulk {

    private final long RANGE_START  = 1000_000_000_000_000L;
    private final long RANGE_END = RANGE_START + 100;

    @Benchmark
    public List<Long> hm_seq() {
        List<Long> results = new ArrayList<>();
        for (long i = RANGE_START; i < RANGE_END; i++) {
            if (PrimesProblem.isPrime(i)) {
                results.add(i);
            }
        }
        return results;
    }

    @Benchmark
    public List<Long> hm_par() {
        return new FactoringTask(RANGE_START, RANGE_END).invoke();
    }

    @Benchmark
    public List<Long> bulk_seq_inner() {
        return LongStream.range(RANGE_START, RANGE_END)
                .boxed()
                .filter(new Predicate<Long>() {
                            @Override
                            public boolean test(Long o) {
                                return PrimesProblem.isPrime(o);
                            }
                        }
                ).collect(Collectors.<Long>toList());
    }

    @Benchmark
    public List<Long> bulk_par_inner() {
        return LongStream.range(RANGE_START, RANGE_END).parallel()
                .boxed()
                .filter(new Predicate<Long>() {
                            @Override
                            public boolean test(Long o) {
                                return PrimesProblem.isPrime(o);
                            }
                        }
                ).collect(Collectors.<Long>toList());
    }

    @Benchmark
    public List<Long> bulk_parseq_inner() {
        return LongStream.range(RANGE_START, RANGE_END).parallel()
                .boxed()
                .filter(new Predicate<Long>() {
                            @Override
                            public boolean test(Long o) {
                                return PrimesProblem.isPrime(o);
                            }
                        }
                ).sequential().collect(Collectors.<Long>toList());
    }

    public static class FactoringTask extends RecursiveTask<List<Long>> {
        final long low;
        final long high;

        @Override
        protected List<Long> compute() {
            if (high - low == 1L) {
                if (PrimesProblem.isPrime(low))
                    return Collections.singletonList(low);
                else
                    return Collections.emptyList();
            }

            long mid = (low + high) / 2L;
            FactoringTask t1 = new FactoringTask(low, mid);
            FactoringTask t2 = new FactoringTask(mid, high);

            List<Long> results;

            // The right way to do it. Forks off one task and
            // continues the other task in this thread. I've
            // seen up to 8x speed up on 16-way Intel and 32-way
            // SPARC boxes (which probably matches the actual number
            // of cores they have, as opposed to the number of threads)
            t2.fork();
            results = new ArrayList<>(t1.compute());
            results.addAll(t2.join());

            return results;
        }

        FactoringTask(long low, long high) {
            this.low = low;
            this.high = high;
        }
    }

}
