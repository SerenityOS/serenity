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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses general ForkJoinPool performance with simple tasks
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ForkJoinPoolRawCallable {

    /**
     * Implementation notes:
     *
     * This test submits empty callables.
     * Callables are submitted in batches, to prevent convoying by driver threads.
     * One driver thread can saturate up to BATCH_SIZE threads.
     *
     * One baseline includes raw throughput, without submissions to executors.
     * This is not considered as fair comparison, but left around as basic compute baseline.
     * Executors could not possibly be faster than that.
     *
     * Another baseline includes ThreadPoolExecutor.
     * Note that this baseline is inherently non-scalable with ABQ backing TPE.
     * The size of ABQ is chosen to accommodate tons of threads, which can also suffer due to cache effects.
     *
     * Tasks are reading public volatile field to break opportunistic optimizations in loops.
     * Tasks are pre-allocated to negate instantiation costs.
     */

    @Param("0")
    private int workers;

    @Param("1000")
    private int batchSize;

    private ThreadPoolExecutor tpe;
    private ForkJoinPool fjpSync;
    private ForkJoinPool fjpAsync;
    private List<SampleTask> tasks;

    public volatile int arg = 42;

    @Setup
    public void setup() {
        SampleTask task = new SampleTask();

        tasks = new ArrayList<>();
        for (int c = 0; c < batchSize; c++) {
            tasks.add(task);
        }

        if (workers == 0) {
            workers = Runtime.getRuntime().availableProcessors();
        }

        tpe = new ThreadPoolExecutor(workers, workers, 1, TimeUnit.HOURS, new ArrayBlockingQueue<>(batchSize * batchSize));
        fjpSync = new ForkJoinPool(workers, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, false);
        fjpAsync = new ForkJoinPool(workers, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @TearDown
    public void teardown() {
        tpe.shutdownNow();
        fjpSync.shutdownNow();
        fjpAsync.shutdownNow();
    }

    @Benchmark
    public int baseline_raw() throws Exception {
        int s = 0;
        for (SampleTask t : tasks) {
            s += t.call();
        }
        return s;
    }

    @Benchmark
    public int baseline_TPE() throws Exception {
        return doWork(tpe);
    }

    @Benchmark
    public int testSync() throws ExecutionException, InterruptedException {
        return doWork(fjpSync);
    }

    @Benchmark
    public int testAsync() throws ExecutionException, InterruptedException {
        return doWork(fjpAsync);
    }

    public int doWork(ExecutorService service) throws ExecutionException, InterruptedException {
        List<Future<Integer>> futures = new ArrayList<>(tasks.size());
        for (SampleTask task : tasks) {
            futures.add(service.submit(task));
        }

        int s = 0;
        for (Future<Integer> future : futures) {
            s += future.get();
        }
        return s;
    }

    public class SampleTask implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            return arg;
        }
    }

}
