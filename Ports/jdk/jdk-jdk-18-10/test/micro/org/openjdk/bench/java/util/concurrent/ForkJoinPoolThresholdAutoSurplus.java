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
package org.openjdk.bench.java.util.concurrent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses ForkJoinPool performance with dependence on threshold.
 */
@OutputTimeUnit(TimeUnit.MINUTES)
@State(Scope.Benchmark)
public class ForkJoinPoolThresholdAutoSurplus {

    /**
     * Implementation notes:
     *
     * This test solves the problem with threshold = 1, and adaptive heuristics. The optimal level is static,
     * and lies somewhere in 1..2 interval. Note the test degrades significantly when heuristic starts to fail,
     * and the throughput is buried under FJP overheads.
     *
     * Baseline includes solving problem sequentially. Hence, each test provides the speedup for parallel execution
     * versus sequential version.
     */

    @Param("0")
    private int workers;

    @Param("10000000")
    private int size;

    @Param({"1", "2", "3", "4", "5", "6", "7", "8"})
    private int threshold;

    private ForkJoinPool fjp;
    private Problem problem;

    @Setup
    public void setup() {
        if (workers == 0) {
            workers = Runtime.getRuntime().availableProcessors();
        }

        problem = new Problem(size);
        fjp = new ForkJoinPool(workers);
    }

    @TearDown
    public void teardown() {
        fjp.shutdownNow();
    }

    @Benchmark
    public long baselineRaw() {
        return problem.solve();
    }

    @Benchmark
    public Long test() throws ExecutionException, InterruptedException {
        return fjp.invoke(new AutoQueuedTask(threshold, problem, 0, problem.size()));
    }

    private static class AutoQueuedTask extends RecursiveTask<Long> {
        private final int thr;
        private final Problem problem;
        private final int l;
        private final int r;

        public AutoQueuedTask(int thr, Problem p, int l, int r) {
            this.thr = thr;
            this.problem = p;
            this.l = l;
            this.r = r;
        }

        @Override
        protected Long compute() {
            if (r - l <= 1 || getSurplusQueuedTaskCount() >= thr) {
                return problem.solve(l, r);
            }

            int mid = (l + r) >>> 1;
            ForkJoinTask<Long> t1 = new AutoQueuedTask(thr, problem, l, mid);
            ForkJoinTask<Long> t2 = new AutoQueuedTask(thr, problem, mid, r);

            t2.fork();

            long res = 0;
            res += t1.invoke();
            res += t2.join();
            return res;
        }
    }



}
