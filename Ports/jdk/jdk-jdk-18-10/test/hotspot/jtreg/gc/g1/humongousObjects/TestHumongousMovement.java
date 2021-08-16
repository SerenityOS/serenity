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
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @test TestHumongousMovement
 * @key randomness
 * @summary Checks that Humongous objects are not moved during GC
 * @requires vm.gc.G1
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M -Xms200m -Xmx200m -XX:InitiatingHeapOccupancyPercent=100
 *                   -Xlog:gc=info:file=TestHumongousMovement.log
 *                   gc.g1.humongousObjects.TestHumongousMovement
 */

public class TestHumongousMovement {

    private static class AllocationData {
        private final byte[] allocation;
        public final BigInteger objectAddress;

        public AllocationData(byte[] byteArray) {
            allocation = byteArray;
            objectAddress = new BigInteger(Long.toUnsignedString((WB.getObjectAddress(allocation))));
        }

        public boolean isAddressChanged() {
            return !new BigInteger(Long.toUnsignedString((WB.getObjectAddress(allocation)))).equals(objectAddress);
        }

        public void printDetails(PrintStream out) {
            BigInteger objectAddressAfterGC =
                    new BigInteger(Long.toUnsignedString((WB.getObjectAddress(allocation))));

            out.println(String.format("Object stored address = %d\nObject address after GC = %d\n"
                            + "They are %sequals", objectAddress,
                    objectAddressAfterGC, !objectAddress.equals(objectAddressAfterGC) ? "not " : ""));
        }

    }

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    // How many allocated humongous objects should be deleted
    private static final double HUMONGOUS_OBJECTS_DELETED_FACTOR = 0.5D;
    // How many of free g1 regions should be used for humongous allocations
    private static final double ALLOCATED_HUMONGOUS_REGIONS_FACTOR = 0.25D;

    public static void main(String[] args) {

        int g1RegionSize = WB.g1RegionSize();
        int byteArrayMemoryOverhead = Helpers.detectByteArrayAllocationOverhead();

        System.out.println("Total " + WB.g1NumMaxRegions() + " regions");
        System.out.println("Total " + WB.g1NumFreeRegions() + " free regions");

        int regionsToAllocate = (int) (WB.g1NumFreeRegions() * ALLOCATED_HUMONGOUS_REGIONS_FACTOR);

        // Sanity check
        Asserts.assertGreaterThan(regionsToAllocate, 0, "Test Bug: no regions to allocate");

        System.out.println("Allocating " + regionsToAllocate + " humongous objects, each 90% of g1 region size");

        List<AllocationData> allocations = new ArrayList<>();

        // 90 % of maximum byte[] that takes one region
        int hSize = (int) ((g1RegionSize - byteArrayMemoryOverhead) * 0.9);

        // Sanity check
        Asserts.assertGreaterThan(hSize, g1RegionSize / 2, "Test Bug: allocation size is not humongous");

        for (int i = 0; i < regionsToAllocate; ++i) {
            allocations.add(new AllocationData(new byte[hSize]));
        }

        Random rnd = Utils.getRandomInstance();

        int toDelete = (int) (allocations.size() * HUMONGOUS_OBJECTS_DELETED_FACTOR);

        for (int i = 0; i < toDelete; ++i) {
            allocations.remove(rnd.nextInt(allocations.size()));
        }

        WB.fullGC();

        List<AllocationData> movedObjects = allocations.stream()
                .filter(AllocationData::isAddressChanged)
                .collect(Collectors.toList());

        if (movedObjects.size() > 0) {
            System.out.println("Test failed - some humongous objects moved after Full GC");
            movedObjects.stream().forEach(a -> a.printDetails(System.out));
            throw new Error("Test failed - some humongous objects moved after Full GC");
        } else {
            System.out.println("Passed");
        }
    }
}
