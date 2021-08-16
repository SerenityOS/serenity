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
package org.openjdk.bench.java.util.stream.tasks.PrimesSieve;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Arrays;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

/**
 * Bulk scenario: filter out candidate primes.
 *
 * This test covers bulk infrastructure only. Refer to other tests for lambda-specific cases.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class Bulk {

    private PrimesSieveProblem problem;

    @Setup(Level.Trial)
    public void populateData(){
        problem = new PrimesSieveProblem();
    }

    @Benchmark
    public int hm_seq() {
        int max = Integer.MIN_VALUE;
        for (int d : problem.get()) {
            if (PrimesSieveProblem.isNotDivisible(d, 2)
                    && PrimesSieveProblem.isNotDivisible(d, 3)
                    && PrimesSieveProblem.isNotDivisible(d, 5)
                    && PrimesSieveProblem.isNotDivisible(d, 7)
                    && PrimesSieveProblem.isNotDivisible(d, 11)
                    && PrimesSieveProblem.isNotDivisible(d, 13)
                    && PrimesSieveProblem.isNotDivisible(d, 17)
                    && PrimesSieveProblem.isNotDivisible(d, 19)
                ) {
                if (d > max) {
                    max = d;
                }
            }
        }
        return max;
    }

    @Benchmark
    public int hm_par() {
        return new FilterTask(problem.get()).invoke();
    }

    @Benchmark
    public int bulk_seq_inner() {
        return Arrays.stream(problem.get())
                .filter(new FilterOp(2))
                .filter(new FilterOp(3))
                .filter(new FilterOp(5))
                .filter(new FilterOp(7))
                .filter(new FilterOp(11))
                .filter(new FilterOp(13))
                .filter(new FilterOp(17))
                .filter(new FilterOp(19))
                .reduce(Integer.MIN_VALUE, new ReduceOp());
    }

    @Benchmark
    public int bulk_par_inner() {
        return Arrays.stream(problem.get()).parallel()
                .filter(new FilterOp(2))
                .filter(new FilterOp(3))
                .filter(new FilterOp(5))
                .filter(new FilterOp(7))
                .filter(new FilterOp(11))
                .filter(new FilterOp(13))
                .filter(new FilterOp(17))
                .filter(new FilterOp(19))
                .reduce(Integer.MIN_VALUE, new ReduceOp());
    }

    static class FilterOp implements Predicate<Integer> {
        private final int divisor;

        public FilterOp(int divisor) {
            this.divisor = divisor;
        }

        @Override
        public boolean test(Integer value) {
            return PrimesSieveProblem.isNotDivisible(value, divisor);
        }
    }

    static class ReduceOp implements BinaryOperator<Integer> {
        @Override
        public Integer apply(Integer left, Integer right) {
            return (left > right) ? left : right;
        }
    }

    static class FilterTask extends RecursiveTask<Integer> {
        private static final int FORK_LIMIT = 1000;
        final Integer[] data;
        final int start, end;

        FilterTask(Integer[] data) {
            this(data, 0, data.length);
        }

        FilterTask(Integer[] data, int start, int end) {
            this.data = data;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Integer compute() {
            int size = end - start;
            if (size > FORK_LIMIT) {
                int mid = start + size / 2;
                FilterTask t1 = new FilterTask(data, start, mid);
                FilterTask t2 = new FilterTask(data, mid, end);
                t1.fork();
                Integer r1 = t2.invoke();
                Integer r2 = t1.join();
                return (r1 > r2) ? r1 : r2;
            } else {
                int max = Integer.MIN_VALUE;
                for (int i = start; i < end; i++) {
                    int d = data[i];
                    if (PrimesSieveProblem.isNotDivisible(d, 2)
                            && PrimesSieveProblem.isNotDivisible(d, 3)
                            && PrimesSieveProblem.isNotDivisible(d, 5)
                            && PrimesSieveProblem.isNotDivisible(d, 7)
                            && PrimesSieveProblem.isNotDivisible(d, 11)
                            && PrimesSieveProblem.isNotDivisible(d, 13)
                            && PrimesSieveProblem.isNotDivisible(d, 17)
                            && PrimesSieveProblem.isNotDivisible(d, 19)
                            ) {
                        if (d > max) {
                            max = d;
                        }
                    }
                }
                return max;
            }
        }
    }

}
