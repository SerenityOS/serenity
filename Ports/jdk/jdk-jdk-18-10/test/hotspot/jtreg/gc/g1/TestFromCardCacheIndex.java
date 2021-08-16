/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestFromCardCacheIndex.java
 * @bug 8196485
 * @summary Ensure that G1 does not miss a remembered set entry due to from card cache default value indices.
 * @requires vm.gc.G1
 * @requires vm.debug
 * @requires vm.bits != "32"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. -Xms20M -Xmx20M -XX:+UseCompressedOops -XX:G1HeapRegionSize=1M -XX:HeapBaseMinAddress=2199011721216 -XX:+UseG1GC -verbose:gc gc.g1.TestFromCardCacheIndex
 */
package gc.g1;

import sun.hotspot.WhiteBox;

/**
 * Repeatedly tries to generate references from objects that contained a card with the same index
 * of the from card cache default value.
 */
public class TestFromCardCacheIndex {
    private static WhiteBox WB;

    // Shift value to calculate card indices from addresses.
    private static final int CardSizeShift = 9;

    /**
     * Returns the last address on the heap within the object.
     *
     * @param The Object array to get the last address from.
     */
    private static long getObjectLastAddress(Object[] o) {
        return WB.getObjectAddress(o) + WB.getObjectSize(o) - 1;
    }

    /**
     * Returns the (truncated) 32 bit card index for the given address.
     *
     * @param The address to get the 32 bit card index from.
     */
    private static int getCardIndex32bit(long address) {
        return (int)(address >> CardSizeShift);
    }

    // The source arrays that are placed on the heap in old gen.
    private static int numArrays = 7000;
    private static int arraySize = 508;
    // Size of a humongous byte array, a bit less than a 1M region. This makes sure
    // that we always create a cross-region reference when referencing it.
    private static int byteArraySize = 1024*1023;

    public static void main(String[] args) {
        WB = sun.hotspot.WhiteBox.getWhiteBox();
        for (int i = 0; i < 5; i++) {
          runTest();
          WB.fullGC();
        }
    }

    public static void runTest() {
        System.out.println("Starting test");

        // Spray the heap with random object arrays in the hope that we get one
        // at the proper place.
        Object[][] arrays = new Object[numArrays][];
        for (int i = 0; i < numArrays; i++) {
            arrays[i] = new Object[arraySize];
        }

        // Make sure that everything is in old gen.
        WB.fullGC();

        // Find if we got an allocation at the right spot.
        Object[] arrayWithCardMinus1 = findArray(arrays);

        if (arrayWithCardMinus1 == null) {
            System.out.println("Array with card -1 not found. Trying again.");
            return;
        } else {
            System.out.println("Array with card -1 found.");
        }

        System.out.println("Modifying the last card in the array with a new object in a different region...");
        // Create a target object that is guaranteed to be in a different region.
        byte[] target = new byte[byteArraySize];

        // Modify the last entry of the object we found.
        arrayWithCardMinus1[arraySize - 1] = target;

        target = null;
        // Make sure that the dirty cards are flushed by doing a GC.
        System.out.println("Doing a GC.");
        WB.youngGC();

        System.out.println("The crash didn't reproduce. Trying again.");
    }

    /**
     * Finds an returns an array that contains a (32 bit truncated) card with value -1.
     */
    private static Object[] findArray(Object[][] arrays) {
        for (int i = 0; i < arrays.length; i++) {
            Object[] target = arrays[i];
            if (target == null) {
                continue;
            }
            WB.getObjectAddress(target); // startAddress not used
            final long lastAddress = getObjectLastAddress(target);
            final int card = getCardIndex32bit(lastAddress);
            if (card == -1) {
                Object[] foundArray = target;
                return foundArray;
            }
        }
        return null;
    }
}

