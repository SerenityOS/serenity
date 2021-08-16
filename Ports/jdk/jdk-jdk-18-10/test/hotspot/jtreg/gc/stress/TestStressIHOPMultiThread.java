/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.stress;

 /*
 * @test TestStressIHOPMultiThread
 * @bug 8148397
 * @key stress
 * @summary Stress test for IHOP
 * @requires vm.gc.G1
 * @run main/othervm/timeout=200 -Xmx128m -XX:G1HeapWastePercent=0 -XX:G1MixedGCCountTarget=1
 *              -XX:+UseG1GC -XX:G1HeapRegionSize=1m -XX:+G1UseAdaptiveIHOP
 *              -Xlog:gc+ihop=debug,gc+ihop+ergo=debug,gc+ergo=debug:TestStressIHOPMultiThread1.log
 *              -Dtimeout=2 -DheapUsageMinBound=30 -DheapUsageMaxBound=80
 *              -Dthreads=2 gc.stress.TestStressIHOPMultiThread
 * @run main/othervm/timeout=200 -Xmx256m -XX:G1HeapWastePercent=0 -XX:G1MixedGCCountTarget=1
 *              -XX:+UseG1GC -XX:G1HeapRegionSize=2m -XX:+G1UseAdaptiveIHOP
 *              -Xlog:gc+ihop=debug,gc+ihop+ergo=debug,gc+ergo=debug:TestStressIHOPMultiThread2.log
 *              -Dtimeout=2 -DheapUsageMinBound=60 -DheapUsageMaxBound=90
 *              -Dthreads=3 gc.stress.TestStressIHOPMultiThread
 * @run main/othervm/timeout=200 -Xmx256m -XX:G1HeapWastePercent=0 -XX:G1MixedGCCountTarget=1
 *              -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:-G1UseAdaptiveIHOP
 *              -Xlog:gc+ihop=debug,gc+ihop+ergo=debug,gc+ergo=debug:TestStressIHOPMultiThread3.log
 *              -Dtimeout=2 -DheapUsageMinBound=40 -DheapUsageMaxBound=90
 *              -Dthreads=5 gc.stress.TestStressIHOPMultiThread
 * @run main/othervm/timeout=200 -Xmx128m -XX:G1HeapWastePercent=0 -XX:G1MixedGCCountTarget=1
 *              -XX:+UseG1GC -XX:G1HeapRegionSize=8m -XX:+G1UseAdaptiveIHOP
 *              -Xlog:gc+ihop=debug,gc+ihop+ergo=debug,gc+ergo=debug:TestStressIHOPMultiThread4.log
 *              -Dtimeout=2 -DheapUsageMinBound=20 -DheapUsageMaxBound=90
 *              -Dthreads=10 gc.stress.TestStressIHOPMultiThread
 * @run main/othervm/timeout=200 -Xmx512m -XX:G1HeapWastePercent=0 -XX:G1MixedGCCountTarget=1
 *              -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+G1UseAdaptiveIHOP
 *              -Xlog:gc+ihop=debug,gc+ihop+ergo=debug,gc+ergo=debug:TestStressIHOPMultiThread5.log
 *              -Dtimeout=2 -DheapUsageMinBound=20 -DheapUsageMaxBound=90
 *              -Dthreads=17 gc.stress.TestStressIHOPMultiThread
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Stress test for Adaptive IHOP. Starts a number of threads that fill and free
 * specified amount of memory. Tests work with enabled IHOP logging.
 *
 */
public class TestStressIHOPMultiThread {

    public final static List<Object> GARBAGE = new LinkedList<>();

    private final long HEAP_SIZE;
    // Amount of memory to be allocated before iterations start
    private final long HEAP_PREALLOC_SIZE;
    // Amount of memory to be allocated and freed during iterations
    private final long HEAP_ALLOC_SIZE;
    private final int CHUNK_SIZE = 100000;

    private final int TIMEOUT;
    private final int THREADS;
    private final int HEAP_LOW_BOUND;
    private final int HEAP_HIGH_BOUND;

    private volatile boolean running = true;
    private final List<AllocationThread> threads;

    public static void main(String[] args) throws InterruptedException {
        new TestStressIHOPMultiThread().start();

    }

    TestStressIHOPMultiThread() {

        TIMEOUT = Integer.getInteger("timeout") * 60;
        THREADS = Integer.getInteger("threads");
        HEAP_LOW_BOUND = Integer.getInteger("heapUsageMinBound");
        HEAP_HIGH_BOUND = Integer.getInteger("heapUsageMaxBound");
        HEAP_SIZE = Runtime.getRuntime().maxMemory();

        HEAP_PREALLOC_SIZE = HEAP_SIZE * HEAP_LOW_BOUND / 100;
        HEAP_ALLOC_SIZE = HEAP_SIZE * (HEAP_HIGH_BOUND - HEAP_LOW_BOUND) / 100;

        threads = new ArrayList<>(THREADS);
    }

    public void start() throws InterruptedException {
        fill();
        createThreads();
        waitForStress();
        stressDone();
        waitForFinish();
    }

    /**
     * Fills HEAP_PREALLOC_SIZE bytes of garbage.
     */
    private void fill() {
        long allocated = 0;
        while (allocated < HEAP_PREALLOC_SIZE) {
            GARBAGE.add(new byte[CHUNK_SIZE]);
            allocated += CHUNK_SIZE;
        }
    }

    /**
     * Creates a number of threads which will fill and free amount of memory.
     */
    private void createThreads() {
        for (int i = 0; i < THREADS; ++i) {
            System.out.println("Create thread " + i);
            AllocationThread thread =new TestStressIHOPMultiThread.AllocationThread(i, HEAP_ALLOC_SIZE / THREADS);
            // Put reference to thread garbage into common garbage for avoiding possible optimization.
            GARBAGE.add(thread.getList());
            threads.add(thread);
        }
        threads.forEach(t -> t.start());
    }

    /**
     * Wait each thread for finishing
     */
    private void waitForFinish() {
        threads.forEach(thread -> {
            thread.silentJoin();
        });
    }

    private boolean isRunning() {
        return running;
    }

    private void stressDone() {
        running = false;
    }

    private void waitForStress() throws InterruptedException {
        Thread.sleep(TIMEOUT * 1000);
    }

    private class AllocationThread extends Thread {

        private final List<Object> garbage;

        private final long amountOfGarbage;
        private final int threadId;

        public AllocationThread(int id, long amount) {
            super("Thread " + id);
            threadId = id;
            amountOfGarbage = amount;
            garbage = new LinkedList<>();
        }

        /**
         * Returns list of garbage.
         * @return List with thread garbage.
         */
        public List<Object> getList(){
            return garbage;
        }

        @Override
        public void run() {
            System.out.println("Start the thread " + threadId);
            while (TestStressIHOPMultiThread.this.isRunning()) {
                try {
                    allocate(amountOfGarbage);
                } catch (OutOfMemoryError e) {
                    free();
                    System.out.println("OutOfMemoryError occurred in thread " + threadId);
                    break;
                }
                free();
            }
        }

        private void silentJoin() {
            System.out.println("Join the thread " + threadId);
            try {
                join();
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }

        /**
         * Allocates thread local garbage
         */
        private void allocate(long amount) {
            long allocated = 0;
            while (allocated < amount && TestStressIHOPMultiThread.this.isRunning()) {
                garbage.add(new byte[CHUNK_SIZE]);
                allocated += CHUNK_SIZE;
            }
        }

        /**
         * Frees thread local garbage
         */
        private void free() {
            garbage.clear();
        }
    }
}
