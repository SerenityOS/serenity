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
 * @test TestIHOPStatic
 * @bug 8148397
 * @summary Test checks concurrent cycle initiation which depends on IHOP value.
 * @requires vm.gc.G1
 * @requires !vm.flightRecorder
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires !(vm.graal.enabled & vm.compMode == "Xcomp")
 * @requires os.maxMemory > 1G
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @run driver/timeout=240 gc.g1.ihop.TestIHOPStatic
 */
package gc.g1.ihop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;

import gc.g1.ihop.lib.IhopUtils;

/**
 * The test starts the AppIHOP multiple times varying setting of MaxHeapSize,
 * IHOP and amount of memory to allocate. Then the test parses the GC log from
 * the app to check that Concurrent Mark Cycle was initiated only if needed
 * and at the right moment, defined by IHOP setting.
 */
public class TestIHOPStatic {

    final static long YOUNG_SIZE = 8 * 1024 * 1024;

    private final static String[] COMMON_OPTIONS = {
        "-XX:+UseG1GC",
        "-XX:G1HeapRegionSize=1m",
        "-XX:-G1UseAdaptiveIHOP",
        "-XX:NewSize=" + YOUNG_SIZE,
        "-XX:MaxNewSize=" + YOUNG_SIZE,
        "-Xlog:gc+ihop+ergo=debug"
    };

    public static void main(String[] args) throws Throwable {

        // Test case:
        // IHOP value, heap occupancy, heap size, expectation of message
        // Test cases for occupancy is greater than IHOP
        runTest(30, 35, 64, true);
        runTest(50, 55, 256, true);
        runTest(60, 65, 64, true);
        runTest(70, 75, 512, true);

        // Test cases for big difference between occupancy and IHOP
        runTest(30, 50, 256, true);
        runTest(30, 70, 512, true);
        runTest(50, 70, 256, true);

        // Test cases for occupancy is less than IHOP
        runTest(30, 25, 64, false);
        runTest(50, 45, 256, false);
        runTest(70, 65, 64, false);
        runTest(70, 65, 512, false);

        // Test cases for big difference between occupancy and IHOP
        runTest(50, 30, 300, false);
        runTest(70, 50, 160, false);

        // Cases for 0 and 100 IHOP.
        runTest(0, 50, 256, true);
        runTest(0, 95, 512, true);
        runTest(100, 20, 64, false);
        runTest(100, 100, 512, false);
    }

    /**
     * Runs the test case.
     *
     * @param ihop                    IHOP value
     * @param pctToFill               heap percentage to be filled
     * @param heapSize                heap size for test
     * @param expectInitiationMessage
     *                                true - concurrent cycle initiation message is expected
     *                                false - message is not expected
     *
     * @throws Throwable
     */
    private static void runTest(int ihop, long pctToFill, long heapSize, boolean expectInitiationMessage) throws Throwable {
        System.out.println("");
        System.out.println("IHOP test:");
        System.out.println("  InitiatingHeapOccupancyPercent : " + ihop);
        System.out.println("  Part of heap to fill (percentage) : " + pctToFill);
        System.out.println("  MaxHeapSize : " + heapSize);
        System.out.println("  Expect for concurrent cycle initiation message : " + expectInitiationMessage);
        List<String> options = new ArrayList<>();
        Collections.addAll(options, Utils.getTestJavaOpts());
        Collections.addAll(options,
                "-XX:InitiatingHeapOccupancyPercent=" + ihop,
                "-Dmemory.fill=" + (heapSize * 1024 * 1024 * pctToFill / 100),
                "-XX:MaxHeapSize=" + heapSize + "M",
                "-XX:InitialHeapSize=" + heapSize + "M"
        );
        Collections.addAll(options, COMMON_OPTIONS);
        options.add(AppIHOP.class.getName());

        OutputAnalyzer out = ProcessTools.executeTestJvm(options);

        if (out.getExitValue() != 0) {
            System.out.println(out.getOutput());
            throw new RuntimeException("IhopTest failed with exit code " + out.getExitValue());
        }

        checkResult(out, expectInitiationMessage);
    }

    /**
     * Checks execution results to ensure that concurrent cycle was initiated or
     * was not.
     *
     * @param out
     * @param expectInitiationMessage true - test expects for concurrent cycle initiation.
     *                                false - test does not expect for concurrent cycle initiation
     */
    private static void checkResult(OutputAnalyzer out, boolean expectInitiationMessage) {
        // Find expected messages
        List<String> logItems = IhopUtils.getErgoInitiationMessages(out);

        // Concurrent cycle was not initiated but was expected.
        if (logItems.isEmpty() && expectInitiationMessage) {
            System.out.println(out.getOutput());
            throw new RuntimeException("Concurrent cycle was not initiated.");
        }
        IhopUtils.checkIhopLogValues(out);
    }

    static class AppIHOP {

        /**
         * Simple class which fills part of memory and initiates GC.
         * To be executed in separate VM.
         * Expect next VM properties to be set:
         * memory.fill - amount of garbage to be created.
         */
        private static final long MEMORY_TO_FILL = Integer.getInteger("memory.fill");
        private final static int CHUNK_SIZE = 10000;

        public final static ArrayList<Object> STORAGE = new ArrayList<>();

        public static void main(String[] args) throws InterruptedException {

            // Calculate part of heap to be filled to achieve expected occupancy.
            System.out.println("Mem to fill:" + MEMORY_TO_FILL);
            if (MEMORY_TO_FILL <= 0) {
                throw new RuntimeException("Wrong memory size: " + MEMORY_TO_FILL);
            }
            try {
                createGarbage(MEMORY_TO_FILL);
            } catch (OutOfMemoryError oome) {
                return;
            }
            // Concurrent cycle initiation should start at end of Young GC cycle.
            // Will fill entire young gen with garbage to guarantee that Young GC was initiated.
            try {
                createGarbage(TestIHOPStatic.YOUNG_SIZE);
            } catch (OutOfMemoryError oome) {
            }
        }

        private static void createGarbage(long memToFill) {
            for (long i = 0; i < memToFill / CHUNK_SIZE; i++) {
                STORAGE.add(new byte[CHUNK_SIZE]);
            }
        }
    }
}
