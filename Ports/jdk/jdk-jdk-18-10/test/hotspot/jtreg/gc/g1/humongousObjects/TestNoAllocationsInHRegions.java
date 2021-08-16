/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects;

import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

import static gc.testlibrary.Allocation.blackHole;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @test TestNoAllocationsInHRegions
 * @key randomness
 * @summary Checks that no additional allocations are made in humongous regions
 * @requires vm.gc.G1
 * @library /test/lib /
 * @modules java.management java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M -Xms200m -Xmx200m -XX:MaxTenuringThreshold=0
 *                   -Xlog:gc=trace:file=TestNoAllocationsInHRegions10.log
 *                   gc.g1.humongousObjects.TestNoAllocationsInHRegions 30 10
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M -Xms200m -Xmx200m -XX:MaxTenuringThreshold=0
 *                   -Xlog:gc=trace:file=TestNoAllocationsInHRegions50.log
 *                   gc.g1.humongousObjects.TestNoAllocationsInHRegions 30 50
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M -Xms200m -Xmx200m -XX:MaxTenuringThreshold=0
 *                   -Xlog:gc=trace:file=TestNoAllocationsInHRegions70.log
 *                   gc.g1.humongousObjects.TestNoAllocationsInHRegions 30 70
 */
public class TestNoAllocationsInHRegions {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final Random RND = Utils.getRandomInstance();
    private static final int G1_REGION_SIZE = WB.g1RegionSize();
    private static final int[] HUMONGOUS_SIZES = {G1_REGION_SIZE / 2, G1_REGION_SIZE + 1, G1_REGION_SIZE * 2 + 1};
    private static final int ALLOC_THREAD_COUNT = 5;

    // We fill specified part of heap with humongous objects - we need public static to prevent escape analysis to
    // collect this field
    public static LinkedList<byte[]> humongousAllocations = new LinkedList<>();

    private static volatile boolean shouldStop = false;
    private static volatile Error error = null;

    static class Allocator implements Runnable {
        private final Random random;
        private final List<byte[]> liveObjects = new LinkedList<>();
        private int usedMemory = 0;
        public final Runnable[] actions;

        /**
         * Maximum size of simple allocation
         */
        private static final int MAX_ALLOCATION_SIZE = (int) (G1_REGION_SIZE / 2 * 0.9);

        /**
         * Maximum size of dead (i.e. one which is made unreachable right after allocation) object
         */
        private static final int DEAD_OBJECT_MAX_SIZE = G1_REGION_SIZE / 10;

        public Allocator(int maxAllocationMemory) {
            random = new Random(RND.nextLong());
            actions = new Runnable[]{
                    // Allocation
                    () -> {
                        if (maxAllocationMemory - usedMemory != 0) {
                            int arraySize = random.nextInt(Math.min(maxAllocationMemory - usedMemory,
                                    MAX_ALLOCATION_SIZE));

                            if (arraySize != 0) {
                                byte[] allocation = new byte[arraySize];
                                liveObjects.add(allocation);
                                usedMemory += arraySize;

                                // Sanity check
                                if (WB.g1IsHumongous(allocation)) {
                                    String errorMessage = String.format("Test Bug: Byte array of size"
                                                    + " %d is expected to be non-humongous but it is humongous",
                                            allocation.length);

                                    System.out.println(errorMessage);
                                    error = new Error(errorMessage);
                                    shouldStop = true;
                                }

                                // Test check
                                if (WB.g1BelongsToHumongousRegion(WB.getObjectAddress(allocation))) {
                                    String errorMessage = String.format("Non-humongous allocation of byte array of "
                                            + "length %d and size %d with address %d was made in Humongous Region",
                                            allocation.length, WB.getObjectSize(allocation),
                                            WB.getObjectAddress(allocation));

                                    System.out.println(errorMessage);
                                    error = new Error(errorMessage);
                                    shouldStop = true;
                                }
                            }
                        }
                    },

                    // Deallocation
                    () -> {
                        if (liveObjects.size() != 0) {
                            int elementNum = random.nextInt(liveObjects.size());
                            int shouldFree = liveObjects.get(elementNum).length;
                            liveObjects.remove(elementNum);
                            usedMemory -= shouldFree;
                        }
                    },

                    // Dead object allocation
                    () -> {
                        int size = random.nextInt(DEAD_OBJECT_MAX_SIZE);
                        blackHole(new byte[size]);
                    },

                    // Check
                    () -> {
                        List<byte[]> wrongHumongousAllocations = liveObjects.stream()
                                .filter(WB::g1IsHumongous)
                                .collect(Collectors.toList());

                        if (wrongHumongousAllocations.size() > 0) {
                            wrongHumongousAllocations.stream().forEach(a ->
                                    System.out.format("Non-humongous allocation of byte array of length %d and"
                                                    + " size %d with address %d was made in Humongous Region",
                                            a.length, WB.getObjectSize(a), WB.getObjectAddress(a)));
                            error = new Error("Some non-humongous allocations were made to humongous region");
                            shouldStop = true;
                        }
                    }
            };
        }

        @Override
        public void run() {
            while (!shouldStop) {
                actions[random.nextInt(actions.length)].run();
                Thread.yield();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new Error("Test Bug: Expected duration (in seconds) and percent of allocated regions were not "
                    + "provided as command line argument");
        }

        // test duration
        long duration = Integer.parseInt(args[0]) * 1000L;
        // part of heap preallocated with humongous objects (in percents)
        int percentOfAllocatedHeap = Integer.parseInt(args[1]);

        long startTime = System.currentTimeMillis();

        long initialFreeRegionsCount = WB.g1NumFreeRegions();
        int regionsToAllocate = (int) ((double) initialFreeRegionsCount / 100.0 * percentOfAllocatedHeap);
        long freeRegionLeft = initialFreeRegionsCount - regionsToAllocate;

        System.out.println("Regions to allocate: " + regionsToAllocate + "; regions to left free: " + freeRegionLeft);

        int maxMemoryPerAllocThread = (int) ((Runtime.getRuntime().freeMemory() / 100.0
                * (100 - percentOfAllocatedHeap)) / ALLOC_THREAD_COUNT * 0.5);

        System.out.println("Using " + maxMemoryPerAllocThread / 1024 + "KB for each of " + ALLOC_THREAD_COUNT
                + " allocation threads");

        while (WB.g1NumFreeRegions() > freeRegionLeft) {
            try {
                humongousAllocations.add(new byte[HUMONGOUS_SIZES[RND.nextInt(HUMONGOUS_SIZES.length)]]);
            } catch (OutOfMemoryError oom) {
                //We got OOM trying to fill heap with humongous objects
                //It probably means that heap is fragmented which is strange since the test logic should avoid it
                System.out.println("Warning: OOM while allocating humongous objects - it likely means "
                        + "that heap is fragmented");
                break;
            }
        }

        System.out.println("Initial free regions " + initialFreeRegionsCount + "; Free regions left "
                + WB.g1NumFreeRegions());

        LinkedList<Thread> threads = new LinkedList<>();

        for (int i = 0; i < ALLOC_THREAD_COUNT; i++) {
            threads.add(new Thread(new Allocator(maxMemoryPerAllocThread)));
        }

        threads.stream().forEach(Thread::start);

        while ((System.currentTimeMillis() - startTime < duration) && error == null) {
            Thread.yield();
        }

        shouldStop = true;
        System.out.println("Finished test");
        if (error != null) {
            throw error;
        }
    }
}
