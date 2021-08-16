/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/*
 * @test TestMaxMinHeapFreeRatioFlags
 * @summary Verify that heap size changes according to max and min heap free ratios.
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver/timeout=240 gc.arguments.TestMaxMinHeapFreeRatioFlags
 */

import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collections;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;
import jdk.internal.misc.Unsafe;

public class TestMaxMinHeapFreeRatioFlags {

    public static final long M = 1024 * 1024;
    public static final long MAX_HEAP_SIZE = 200 * M;
    public static final long HEAP_SIZE = 10 * M;
    public static final long MAX_NEW_SIZE = 20 * M;
    public static final long NEW_SIZE = 5 * M;

    public static void main(String args[]) throws Exception {
        LinkedList<String> options = new LinkedList<>(
                Arrays.asList(Utils.getFilteredTestJavaOpts("-XX:[^ ]*HeapFreeRatio","-XX:\\+ExplicitGCInvokesConcurrent"))
        );

        negativeTest(20, false, 10, true, options);
        negativeTest(100, true, 0, false, options);
        negativeTest(101, false, 50, false, options);
        negativeTest(49, true, 102, true, options);
        negativeTest(-1, false, 50, false, options);
        negativeTest(50, true, -1, true, options);

        positiveTest(10, false, 90, false, true, options);
        positiveTest(10, true, 80, false, true, options);
        positiveTest(20, false, 70, true, true, options);
        positiveTest(25, true, 65, true, true, options);
        positiveTest(40, false, 50, false, true, options);
    }

    /**
     * Verify that heap size will be changed to conform
     * min and max heap free ratios.
     *
     * @param minRatio value of MinHeapFreeRatio option
     * @param useXminf used Xminf option instead of MinHeapFreeRatio
     * @param maxRatio value of MaxHeapFreeRatio option
     * @param useXmaxf used Xmaxf option instead of MaxHeapFreeRatio
     * @param options additional options for JVM
     */
    public static void positiveTest(int minRatio, boolean useXminf,
            int maxRatio, boolean useXmaxf, boolean shrinkHeapInSteps,
            LinkedList<String> options) throws Exception {

        LinkedList<String> vmOptions = new LinkedList<>(options);
        Collections.addAll(vmOptions,
                (useXminf ? "-Xminf" + minRatio / 100.0 : "-XX:MinHeapFreeRatio=" + minRatio),
                (useXmaxf ? "-Xmaxf" + maxRatio / 100.0 : "-XX:MaxHeapFreeRatio=" + maxRatio),
                "-Xmx" + MAX_HEAP_SIZE,
                "-Xms" + HEAP_SIZE,
                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                "-XX:NewSize=" + NEW_SIZE,
                "-XX:MaxNewSize=" + MAX_NEW_SIZE,
                "-XX:" + (shrinkHeapInSteps ? '+' : '-') + "ShrinkHeapInSteps",
                RatioVerifier.class.getName(),
                Integer.toString(minRatio),
                Integer.toString(maxRatio),
                Boolean.toString(shrinkHeapInSteps)
        );

        ProcessBuilder procBuilder = GCArguments.createJavaProcessBuilder(vmOptions);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());
        analyzer.shouldHaveExitValue(0);
    }

    /**
     * Verify that VM will fail to start with specified ratios.
     *
     * @param minRatio value of MinHeapFreeRatio option
     * @param useXminf used Xminf option instead of MinHeapFreeRatio
     * @param maxRatio value of MaxHeapFreeRatio option
     * @param useXmaxf used Xmaxf option instead of MaxHeapFreeRatio
     * @param options additional options for JVM
     */
    public static void negativeTest(int minRatio, boolean useXminf,
            int maxRatio, boolean useXmaxf,
            LinkedList<String> options) throws Exception {

        LinkedList<String> vmOptions = new LinkedList<>(options);
        Collections.addAll(vmOptions,
                (useXminf ? "-Xminf" + minRatio / 100.0 : "-XX:MinHeapFreeRatio=" + minRatio),
                (useXmaxf ? "-Xmaxf" + maxRatio / 100.0 : "-XX:MaxHeapFreeRatio=" + maxRatio),
                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                "-version"
        );
        ProcessBuilder procBuilder = GCArguments.createJavaProcessBuilder(vmOptions);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());
        analyzer.shouldHaveExitValue(1);
        analyzer.shouldContain("Error: Could not create the Java Virtual Machine.");
    }

    /**
     * RatioVerifier will be executed in the tested VM.
     * It will check that real heap usage after collection lies between MinHeapFreeRatio and MaxHeapFreeRatio.
     */
    public static class RatioVerifier {

        private static final Unsafe unsafe = Unsafe.getUnsafe();

        // Size of byte array that will be allocated
        public static final int CHUNK_SIZE = 1024;
        // Length of byte array, that will be added to "garbage" list.
        public static final int ARRAY_LENGTH = CHUNK_SIZE - Unsafe.ARRAY_BYTE_BASE_OFFSET;
        // Amount of tries to force heap shrinking/expansion using GC
        public static final int GC_TRIES = 10;

        // Value that will be added/substracted from expected min/max heap free ratio
        // during memory allocation to make sure that specified limit will be exceeded.
        public static final double OVERLOAD = 0.05;
        // Acceptable heap free ratio limit exceedance: verification will fail if
        // actual ratio is lower than expected min heap free ratio - VARIANCE or
        // higher than expected max heap free ratio + VARIANCE.
        public static final double VARIANCE = 0.025;

        public static LinkedList<Object> garbage = new LinkedList<>();

        public static void main(String args[]) throws Exception {
            if (args.length != 3) {
                throw new IllegalArgumentException("Expected 3 args: <minRatio> <maxRatio> <shrinkHeapInSteps>");
            }
            if (GCTypes.OldGCType.getOldGCType() == GCTypes.OldGCType.PSOld ||
                GCTypes.OldGCType.getOldGCType() == GCTypes.OldGCType.G1) {
                System.out.println("Test is not applicable to parallel full GCs");
                return;
            }

            double minRatio = Integer.valueOf(args[0]) / 100.0;
            double maxRatio = Integer.valueOf(args[1]) / 100.0;
            boolean shrinkHeapInSteps = Boolean.valueOf(args[2]);

            long maxHeapSize = getMax();
            int gcTries = (shrinkHeapInSteps ? GC_TRIES : 1);

            // Initial checks. This also links up everything in these helper methods,
            // in case it brings more garbage.
            forceGC(gcTries);
            verifyRatio(minRatio, maxRatio);

            // commit 0.5 of total heap size to have enough space
            // to both shink and expand
            while (getCommitted() < maxHeapSize / 2) {
                garbage.add(new byte[ARRAY_LENGTH]);
            }

            forceGC(gcTries);
            // Verify that current heap free ratio lies between specified limits
            verifyRatio(minRatio, maxRatio);

            // Estimate how much memory we have to allocate to force expansion
            long memoryToFill = (long) (getCommitted() * (1 - minRatio + OVERLOAD))
                    - getUsed();

            long previouslyCommitted = getCommitted();

            while (memoryToFill > 0) {
                garbage.add(new byte[CHUNK_SIZE]);
                memoryToFill -= CHUNK_SIZE;
            }

            forceGC(gcTries);
            // Verify that after memory allocation heap free ratio is still conforming specified limits
            verifyRatio(minRatio, maxRatio);
            // Verify that heap was actually expanded
            if (previouslyCommitted >= getCommitted()) {
                throw new RuntimeException("Heap was not expanded.");
            }

            // Estimate how much memory we have to free to force shrinking
            long memoryToFree = getUsed()
                    - (long) (getCommitted() * (1 - maxRatio - OVERLOAD));

            previouslyCommitted = getCommitted();

            while (memoryToFree > 0 && garbage.size() > 0) {
                garbage.remove(garbage.size() - 1);
                memoryToFree -= CHUNK_SIZE;
            }

            forceGC(gcTries);
            // Verify that heap free ratio is still conforming specified limits
            verifyRatio(minRatio, maxRatio);
            // Verify that heap was actually shrinked
            if (previouslyCommitted <= getCommitted()) {
                throw new RuntimeException("Heap was not shrinked.");
            }
        }

        public static void forceGC(int gcTries) {
            for (int i = 0; i < gcTries; i++) {
                System.gc();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                }
            }
        }

        /**
         * Verify that heap free ratio is conforming specified limits.
         * Actual heap free ratio may be very close to one of specified limits,
         * but exceed for more then VARIANCE.
         * Verification will also pass if actual ratio is not conforming limits,
         * but it is not possible to shrink/expand heap.
         */
        public static void verifyRatio(double minRatio, double maxRatio) {
            double ratio = getHeapFreeRatio();
            System.out.println(minRatio + " " + ratio + " " + maxRatio);
            if (minRatio - ratio > VARIANCE
                    && getCommitted() < getMax()) {
                throw new RuntimeException("Current heap free ratio is lower than "
                        + "MinHeapFreeRatio (" + ratio + " vs " + minRatio + ").");
            }
            if (ratio - maxRatio > VARIANCE
                    && getUsed() > getInit()) {
                throw new RuntimeException("Current heap free ratio is higher than "
                        + "MaxHeapFreeRatio (" + ratio + " vs " + maxRatio + ").");
            }
        }

        /*
         * Obtain information about heap size.
         *
         * For G1 information summed up for all type of regions,
         * because tested options affect overall heap sizing.
         *
         * For all other GCs return information only for old gen.
         */
        public static long getMax() {
            return HeapRegionUsageTool.getOldUsage().getMax();
        }

        public static long getInit() {
            if (GCTypes.OldGCType.getOldGCType() == GCTypes.OldGCType.G1) {
                return HeapRegionUsageTool.getEdenUsage().getInit()
                        + HeapRegionUsageTool.getSurvivorUsage().getInit()
                        + HeapRegionUsageTool.getOldUsage().getInit();
            } else {
                return HeapRegionUsageTool.getOldUsage().getInit();
            }
        }

        public static long getUsed() {
            if (GCTypes.OldGCType.getOldGCType() == GCTypes.OldGCType.G1) {
                return HeapRegionUsageTool.getEdenUsage().getUsed()
                        + HeapRegionUsageTool.getSurvivorUsage().getUsed()
                        + HeapRegionUsageTool.getOldUsage().getUsed();
            } else {
                return HeapRegionUsageTool.getOldUsage().getUsed();
            }
        }

        public static long getCommitted() {
            if (GCTypes.OldGCType.getOldGCType() == GCTypes.OldGCType.G1) {
                return HeapRegionUsageTool.getEdenUsage().getCommitted()
                        + HeapRegionUsageTool.getSurvivorUsage().getCommitted()
                        + HeapRegionUsageTool.getOldUsage().getCommitted();
            } else {
                return HeapRegionUsageTool.getOldUsage().getCommitted();
            }
        }

        public static long getFree() {
            return getCommitted() - getUsed();
        }

        public static double getHeapFreeRatio() {
            return getFree() / (double) getCommitted();
        }
    }
}
