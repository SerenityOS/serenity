/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test TestHumongousThreshold
 * @summary Checks that objects larger than half a region are allocated as humongous
 * @requires vm.gc.G1
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=1M
 * gc.g1.humongousObjects.TestHumongousThreshold
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=2M
 * gc.g1.humongousObjects.TestHumongousThreshold
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=4M
 * gc.g1.humongousObjects.TestHumongousThreshold
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=8M
 * gc.g1.humongousObjects.TestHumongousThreshold
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -Xms128M -XX:G1HeapRegionSize=16M
 * gc.g1.humongousObjects.TestHumongousThreshold
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -Xms200M -XX:G1HeapRegionSize=32M
 * gc.g1.humongousObjects.TestHumongousThreshold
 *
 */

public class TestHumongousThreshold {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int REGION_SIZE = WHITE_BOX.g1RegionSize();
    private static final int MAX_CONTINUOUS_SIZE_CHECK = 129;
    private static final int NON_HUMONGOUS_STEPS = 10;

    /**
     * The method allocates byte[] with specified size and checks that:
     * 1. byte[] is allocated as we specified in expectedHumongous.
     * 2. byte[] is allocated as humongous if its size is large than a half of region and non-humongous otherwise.
     * It uses WB to obtain the size of created byte[]. Only objects larger than half of region are expected
     * to be humongous.
     *
     * @param arraySize size of allocation
     * @param expectedHumongous expected humongous/non-humongous allocation
     * @return allocated byte array
     */

    private static void allocateAndCheck(int arraySize, boolean expectedHumongous) {
        byte[] storage = new byte[arraySize];
        long objectSize = WHITE_BOX.getObjectSize(storage);
        boolean shouldBeHumongous = objectSize > (REGION_SIZE / 2);

        Asserts.assertEquals(expectedHumongous, shouldBeHumongous, "Despite we expected this object to be "
                + (expectedHumongous ? "humongous" : "non-humongous") + " it appeared otherwise when we checked "
                + "object size - likely test bug; Allocation size = " + arraySize + "; Object size = " + objectSize
                + "; region size = " + REGION_SIZE);

        Asserts.assertEquals(WHITE_BOX.g1IsHumongous(storage), shouldBeHumongous,
                "Object should be allocated as " + (shouldBeHumongous ? "humongous"
                        : "non-humongous") + " but it wasn't; Allocation size = " + arraySize + "; Object size = "
                        + objectSize + "; region size = " + REGION_SIZE);
    }

    public static void main(String[] args) {
        int byteArrayMemoryOverhead = Helpers.detectByteArrayAllocationOverhead();

        // Largest non-humongous byte[]
        int maxByteArrayNonHumongousSize = (REGION_SIZE / 2) - byteArrayMemoryOverhead;

        // Increment for non-humongous testing
        int nonHumongousStep = maxByteArrayNonHumongousSize / NON_HUMONGOUS_STEPS;

        // Maximum byte[] that takes one region
        int maxByteArrayOneRegionSize = REGION_SIZE - byteArrayMemoryOverhead;

        // Sizes in regions
        // i,e, 1.0f means one region, 1.5f means one and half region etc
        float[] humongousFactors = {0.8f, 1.0f, 1.2f, 1.5f, 1.7f, 2.0f, 2.5f};

        // Some diagnostic output
        System.out.format("%s started%n", TestHumongousThreshold.class.getName());
        System.out.format("Actual G1 region size %d%n", REGION_SIZE);
        System.out.format("byte[] memory overhead %d%n", byteArrayMemoryOverhead);

        // Non-humongous allocations
        System.out.format("Doing non-humongous allocations%n");

        // Testing allocations with byte[] with length from 0 to MAX_CONTINUOUS_SIZE_CHECK
        System.out.format("Testing allocations with byte[] with length from 0 to %d%n", MAX_CONTINUOUS_SIZE_CHECK);
        for (int i = 0; i < MAX_CONTINUOUS_SIZE_CHECK; ++i) {
            allocateAndCheck(i, false);
        }

        // Testing allocations with byte[] with length from 0 to nonHumongousStep * NON_HUMONGOUS_STEPS
        System.out.format("Testing allocations with byte[] with length from 0 to %d with step %d%n",
                nonHumongousStep * NON_HUMONGOUS_STEPS, nonHumongousStep);
        for (int i = 0; i < NON_HUMONGOUS_STEPS; ++i) {
            allocateAndCheck(i * nonHumongousStep, false);
        }

        // Testing allocations with byte[] of maximum non-humongous length
        System.out.format("Testing allocations with byte[] of maximum non-humongous length %d%n",
                maxByteArrayNonHumongousSize);
        allocateAndCheck(maxByteArrayNonHumongousSize, false);

        // Humongous allocations
        System.out.format("Doing humongous allocations%n");
        // Testing with minimum humongous object
        System.out.format("Testing with byte[] of minimum humongous object %d%n", maxByteArrayNonHumongousSize + 1);
        allocateAndCheck(maxByteArrayNonHumongousSize + 1, true);

        // Testing allocations with byte[] with length from (maxByteArrayNonHumongousSize + 1) to
        // (maxByteArrayNonHumongousSize + 1 + MAX_CONTINUOUS_SIZE_CHECK)
        System.out.format("Testing allocations with byte[] with length from %d to %d%n",
                maxByteArrayNonHumongousSize + 1, maxByteArrayNonHumongousSize + 1 + MAX_CONTINUOUS_SIZE_CHECK);
        for (int i = 0; i < MAX_CONTINUOUS_SIZE_CHECK; ++i) {
            allocateAndCheck(maxByteArrayNonHumongousSize + 1 + i, true);
        }

        // Checking that large (more than a half of region size) objects are humongous
        System.out.format("Checking that large (more than a half of region size) objects are humongous%n");
        for (float factor : humongousFactors) {
            allocateAndCheck((int) (maxByteArrayOneRegionSize * factor), true);
        }
    }
}
