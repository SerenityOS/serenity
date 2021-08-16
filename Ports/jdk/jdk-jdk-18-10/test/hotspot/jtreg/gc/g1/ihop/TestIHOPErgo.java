/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

 /*
 * @test TestIHOPErgo
 * @bug 8148397
 * @key stress
 * @summary Test checks that behavior of Adaptive and Static IHOP at concurrent cycle initiation
 * @requires vm.gc.G1
 * @requires !vm.flightRecorder
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.opt.MaxGCPauseMillis == "null"
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @run driver/timeout=480 gc.g1.ihop.TestIHOPErgo
 */
package gc.g1.ihop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import gc.g1.ihop.lib.IhopUtils;

/**
 * The test starts the AppIHOP multiple times varying settings of MaxHeapSize.
 * The test parses GC log from AppIHOP to check:
 * - occupancy is not less than threshold for Adaptive and Static IHOP at
 * concurrent cycle initiation
 * - Adaptive IHOP prediction was started during AppIHOP executing
 * - log contains ergonomic messages in log
 */
public class TestIHOPErgo {

    // Common GC tune and logging options for test.
    private final static String[] COMMON_OPTIONS = {
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:G1MixedGCLiveThresholdPercent=100",
        "-XX:G1HeapWastePercent=0",
        "-XX:MaxGCPauseMillis=30000",
        "-XX:G1MixedGCCountTarget=1",
        "-XX:+UseG1GC",
        "-XX:G1HeapRegionSize=1m",
        "-XX:+G1UseAdaptiveIHOP",
        "-Xlog:gc+ihop=debug,gc+ihop+ergo=debug,gc+ergo=debug",
        "-XX:+AlwaysTenure",
        "-XX:G1AdaptiveIHOPNumInitialSamples=1",
        "-XX:InitiatingHeapOccupancyPercent=30"
    };

    public static void main(String[] args) throws Throwable {

        // heap size MB, sleep time for allocator, true/false for adaptive/static
        runTest(64, 0, false);
        runTest(64, 100, false);
        runTest(128, 100, false);
        runTest(256, 50, false);
        runTest(512, 30, false);
        runTest(64, 50, true);
        runTest(128, 200, true);
        runTest(256, 100, true);
        runTest(512, 50, true);
    }

    /**
     * Runs AppIHOP in separate VM and checks GC log.
     *
     * @param heapSize       heap size
     * @param sleepTime      sleep time between memory allocations.
     * @param isIhopAdaptive true forAdaptive IHOP, false for Static
     *
     * @throws Throwable
     */
    private static void runTest(int heapSize, int sleepTime, boolean isIhopAdaptive) throws Throwable {
        System.out.println("IHOP test:");
        System.out.println("  MaxHeapSize : " + heapSize);

        List<String> options = new ArrayList<>();
        Collections.addAll(options,
                "-Dheap.size=" + heapSize,
                "-Dsleep.time=" + sleepTime,
                "-XX:MaxHeapSize=" + heapSize + "M",
                "-XX:NewSize=" + heapSize / 8 + "M",
                "-XX:MaxNewSize=" + heapSize / 8 + "M",
                "-XX:InitialHeapSize=" + heapSize + "M",
                "-XX:" + (isIhopAdaptive ? "+" : "-") + "G1UseAdaptiveIHOP"
        );

        Collections.addAll(options, COMMON_OPTIONS);
        options.add(AppIHOP.class.getName());
        OutputAnalyzer out = executeTest(options);

        // Checks that log contains message which indicates that IHOP prediction is active
        if (isIhopAdaptive) {
            IhopUtils.checkAdaptiveIHOPWasActivated(out);
        }
        // Checks that log contains messages which indicates that VM initiates/checks heap occupancy
        // and tries to start concurrent cycle.
        IhopUtils.checkErgoMessagesExist(out);

        // Checks threshold and occupancy values
        IhopUtils.checkIhopLogValues(out);
    }

    private static OutputAnalyzer executeTest(List<String> options) throws Throwable, RuntimeException {
        OutputAnalyzer out = ProcessTools.executeTestJvm(options);
        if (out.getExitValue() != 0) {
            System.out.println(out.getOutput());
            throw new RuntimeException("AppIHOP failed with exit code" + out.getExitValue());
        }
        return out;
    }

    /**
     * The AppIHOP fills 60% of heap and allocates and frees 30% of existing
     * heap 'iterations' times to achieve IHOP activation. To be executed in
     * separate VM. Expected properties:
     * heap.size - heap size which is used to calculate amount of memory
     *             to be allocated and freed
     * sleep.time - short pause between filling each MB
     */
    public static class AppIHOP {

        public final static LinkedList<Object> GARBAGE = new LinkedList<>();

        private final int ITERATIONS = 10;
        private final int OBJECT_SIZE = 100000;
        // 60% of the heap will be filled before test cycles.
        // 30% of the heap will be filled and freed during test cycle.
        private final long HEAP_PREALLOC_PCT = 60;
        private final long HEAP_ALLOC_PCT = 30;
        private final long HEAP_SIZE;
        // Amount of memory to be allocated before iterations start
        private final long HEAP_PREALLOC_SIZE;
        // Amount of memory to be allocated and freed during iterations
        private final long HEAP_ALLOC_SIZE;
        private final int SLEEP_TIME;

        public static void main(String[] args) throws InterruptedException {
            new AppIHOP().start();
        }

        AppIHOP() {
            HEAP_SIZE = Integer.getInteger("heap.size") * 1024 * 1024;
            SLEEP_TIME = Integer.getInteger("sleep.time");

            HEAP_PREALLOC_SIZE = HEAP_SIZE * HEAP_PREALLOC_PCT / 100;
            HEAP_ALLOC_SIZE = HEAP_SIZE * HEAP_ALLOC_PCT / 100;
        }

        public void start() throws InterruptedException {
            fill(HEAP_PREALLOC_SIZE);
            fillAndFree(HEAP_ALLOC_SIZE, ITERATIONS);
        }

        /**
         * Fills allocationSize bytes of garbage.
         *
         * @param allocationSize amount of garbage
         */
        private void fill(long allocationSize) {
            long allocated = 0;
            while (allocated < allocationSize) {
                GARBAGE.addFirst(new byte[OBJECT_SIZE]);
                allocated += OBJECT_SIZE;
            }
        }

        /**
         * Allocates allocationSize bytes of garbage. Performs a short pauses
         * during allocation. Frees allocated garbage.
         *
         * @param allocationSize amount of garbage per iteration
         * @param iterations     iteration count
         *
         * @throws InterruptedException
         */
        private void fillAndFree(long allocationSize, int iterations) throws InterruptedException {

            for (int i = 0; i < iterations; ++i) {
                System.out.println("Iteration:" + i);
                long allocated = 0;
                long counter = 0;
                while (allocated < allocationSize) {
                    GARBAGE.addFirst(new byte[OBJECT_SIZE]);
                    allocated += OBJECT_SIZE;
                    counter += OBJECT_SIZE;
                    if (counter > 1024 * 1024) {
                        counter = 0;
                        if (SLEEP_TIME != 0) {
                            Thread.sleep(SLEEP_TIME);
                        }
                    }
                }
                long removed = 0;
                while (removed < allocationSize) {
                    GARBAGE.removeLast();
                    removed += OBJECT_SIZE;
                }
            }
        }
    }
}
