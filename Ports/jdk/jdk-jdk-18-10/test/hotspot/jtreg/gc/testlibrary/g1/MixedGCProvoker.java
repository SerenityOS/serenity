/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.testlibrary.g1;

import static java.util.stream.IntStream.range;
import static jdk.test.lib.Asserts.assertTrue;
import static sun.hotspot.WhiteBox.getWhiteBox;

import java.util.ArrayList;
import java.util.List;

import gc.testlibrary.Helpers;
import jdk.test.lib.Asserts;

/**
 * Utility class to reliably provoke a mixed GC. The class allocates several arrays and
 * promotes them to the old generation. All GCs are triggered by Whitebox to ensure that they occur.
 * Must use G1.
 */
public class MixedGCProvoker {

    /**
     * Allocate a few objects that are supposed to end up in the old generation as they are held live by the
     * given array. Caller must make sure by running this test with e.g. -XX:+AlwaysTenure that the objects
     * to actually be tenured.
     * Mixes live and dead objects, allocating about two regions worth of objects.
     * @param liveOldObjects Array receiving the live objects after this method.
     * @param g1HeapRegionSize Size of a G1 heap region.
     * @param arraySize Size of the byte arrays to use.
     */
    public static void allocateOldObjects(
            List<byte[]> liveOldObjects,
            int g1HeapRegionSize,
            int arraySize) {

        var toUnreachable = new ArrayList<byte[]>();

        // Allocates buffer and promotes it to the old gen. Mix live and dead old objects.
        // allocate about two regions of old memory. At least one full old region will guarantee
        // mixed collection in the future
        range(0, g1HeapRegionSize/arraySize).forEach(n -> {
            liveOldObjects.add(new byte[arraySize]);
            toUnreachable.add(new byte[arraySize]);
        });

        // Do one young collection, AlwaysTenure will force promotion.
        getWhiteBox().youngGC();

        // Check it is promoted & keep alive
        Asserts.assertTrue(getWhiteBox().isObjectInOldGen(liveOldObjects), "List of the objects is suppose to be in OldGen");
        Asserts.assertTrue(getWhiteBox().isObjectInOldGen(toUnreachable), "List of the objects is suppose to be in OldGen");
    }

    /**
     * Provoke at least one mixed gc by starting a marking cycle, waiting for its end and triggering two GCs.
     * @param liveOldObjects The objects supposed to survive this marking cycle.
     */
    public static void provokeMixedGC(List<byte[]> liveOldObjects) {
        Helpers.waitTillCMCFinished(getWhiteBox(), 10);
        getWhiteBox().g1StartConcMarkCycle();
        Helpers.waitTillCMCFinished(getWhiteBox(), 10);
        getWhiteBox().youngGC(); // the "Prepare Mixed" gc
        getWhiteBox().youngGC(); // the "Mixed" gc

        // check that liveOldObjects still alive
        assertTrue(getWhiteBox().isObjectInOldGen(liveOldObjects), "List of the objects is suppose to be in OldGen");
    }

    /**
     * Provoke a mixed GC on a G1 heap with the given heap region size.
     * @param g1HeapRegionSize The size of your regions in bytes
     */
    public static void allocateAndProvokeMixedGC(int g1HeapRegionSize) {
        final var arraySize = 20_000;
        var liveOldObjects = new ArrayList<byte[]>();

        getWhiteBox().fullGC(); // Make sure the heap is in a known state.
        allocateOldObjects(liveOldObjects, g1HeapRegionSize, arraySize);
        provokeMixedGC(liveOldObjects);
    }
}
