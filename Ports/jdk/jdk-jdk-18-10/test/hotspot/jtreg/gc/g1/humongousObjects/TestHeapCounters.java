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

import gc.testlibrary.Helpers;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @test TestHeapCounters
 * @summary Checks that heap counters work as expected after humongous allocations/deallocations
 * @requires vm.gc.G1
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xmx128m -Xms128m
 *                   -XX:G1HeapRegionSize=1M -XX:InitiatingHeapOccupancyPercent=100 -XX:-G1UseAdaptiveIHOP
 *                   -Xlog:gc -Xlog:gc:file=TestHeapCountersRuntime.gc.log
 *                    gc.g1.humongousObjects.TestHeapCounters RUNTIME_COUNTER
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xmx128m -Xms128m
 *                   -XX:G1HeapRegionSize=1M -XX:InitiatingHeapOccupancyPercent=100 -XX:-G1UseAdaptiveIHOP
 *                   -Xlog:gc -Xlog:gc:file=TestHeapCountersMXBean.gc.log
 *                    gc.g1.humongousObjects.TestHeapCounters MX_BEAN_COUNTER
 */
public class TestHeapCounters {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int G1_REGION_SIZE = WHITE_BOX.g1RegionSize();
    private static final int HALF_G1_REGION_SIZE = G1_REGION_SIZE / 2;

    // Since during deallocation GC could free (very unlikely) some non-humongous data this value relaxes amount of
    // memory we expect to be freed.
    private static final double ALLOCATION_SIZE_TOLERANCE_FACTOR = 0.85D;

    private enum MemoryCounter {
        MX_BEAN_COUNTER {
            @Override
            public long getUsedMemory() {
                return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            }
        },
        RUNTIME_COUNTER {
            @Override
            public long getUsedMemory() {
                return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            }
        };

        public abstract long getUsedMemory();
    }

    private static class Allocation {
        private byte[] allocation;
        public final long expectedSize;

        public Allocation(int allocationSize, long allocationExpectedSize) {
            allocation = new byte[allocationSize];
            expectedSize = allocationExpectedSize;

            System.out.println(String.format("Object size is %d; Object is %shumongous",
                    WHITE_BOX.getObjectSize(allocation),
                    (WHITE_BOX.g1IsHumongous(allocation) ? "" : "non-")));

            selfTest();
        }

        private void selfTest() {
            boolean isHumongous = WHITE_BOX.getObjectSize(allocation) > HALF_G1_REGION_SIZE;
            boolean shouldBeHumongous = WHITE_BOX.g1IsHumongous(allocation);

            // Sanity check
            Asserts.assertEquals(isHumongous, shouldBeHumongous,
                    String.format("Test Bug: Object of size %d is expected to be %shumongous but it is not",
                            WHITE_BOX.getObjectSize(allocation), (shouldBeHumongous ? "" : "non-")));
        }

        public void forgetAllocation() {
            allocation = null;
        }
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            throw new Error("Expected memory counter name wasn't provided as command line argument");
        }
        MemoryCounter memoryCounter = MemoryCounter.valueOf(args[0].toUpperCase());

        int byteArrayMemoryOverhead = Helpers.detectByteArrayAllocationOverhead();

        // Largest non-humongous byte[]
        int maxByteArrayNonHumongousSize = HALF_G1_REGION_SIZE - byteArrayMemoryOverhead;

        // Maximum byte[] that takes one region
        int maxByteArrayOneRegionSize = G1_REGION_SIZE - byteArrayMemoryOverhead;

        List<Integer> allocationSizes = Arrays.asList(
                (int) maxByteArrayNonHumongousSize + 1,
                (int) (0.8f * maxByteArrayOneRegionSize),
                (int) (maxByteArrayOneRegionSize),
                (int) (1.2f * maxByteArrayOneRegionSize),
                (int) (1.5f * maxByteArrayOneRegionSize),
                (int) (1.7f * maxByteArrayOneRegionSize),
                (int) (2.0f * maxByteArrayOneRegionSize),
                (int) (2.5f * maxByteArrayOneRegionSize)
        );

        List<Allocation> allocations = new ArrayList<>();
        List<GarbageCollectorMXBean> gcBeans =
                ManagementFactory.getGarbageCollectorMXBeans();

        long gcCountBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();


        System.out.println("Starting allocations - no GC should happen until we finish them");

        for (int allocationSize : allocationSizes) {

            long usedMemoryBefore = memoryCounter.getUsedMemory();
            long expectedAllocationSize = (long) Math.ceil((double) allocationSize / G1_REGION_SIZE) * G1_REGION_SIZE;
            allocations.add(new Allocation(allocationSize, expectedAllocationSize));
            long usedMemoryAfter = memoryCounter.getUsedMemory();

            System.out.format("Expected allocation size: %d\nUsed memory before allocation: %d\n"
                            + "Used memory after allocation: %d\n",
                    expectedAllocationSize, usedMemoryBefore, usedMemoryAfter);

            long gcCountNow = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();

            if (gcCountNow == gcCountBefore) {
                // We should allocate at least allocation.expectedSize
                Asserts.assertGreaterThanOrEqual(usedMemoryAfter - usedMemoryBefore, expectedAllocationSize,
                        "Counter of type " + memoryCounter.getClass().getSimpleName() +
                                " returned wrong allocation size");
            } else {
                System.out.println("GC happened during allocation so the check is skipped");
                gcCountBefore = gcCountNow;
            }
        }

        System.out.println("Finished allocations - no GC should have happened before this line");


        allocations.stream().forEach(allocation -> {
            long usedMemoryBefore = memoryCounter.getUsedMemory();
            allocation.forgetAllocation();

            WHITE_BOX.fullGC();

            long usedMemoryAfter = memoryCounter.getUsedMemory();

            // We should free at least allocation.expectedSize * ALLOCATION_SIZE_TOLERANCE_FACTOR
            Asserts.assertGreaterThanOrEqual(usedMemoryBefore - usedMemoryAfter,
                    (long) (allocation.expectedSize * ALLOCATION_SIZE_TOLERANCE_FACTOR),
                    "Counter of type " + memoryCounter.getClass().getSimpleName() + " returned wrong allocation size");
        });
    }
}
