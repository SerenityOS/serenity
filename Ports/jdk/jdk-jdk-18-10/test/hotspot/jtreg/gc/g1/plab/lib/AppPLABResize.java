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
package gc.g1.plab.lib;

import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;

/**
 * This application is part of PLAB Resize test.
 * The application allocates objects in 3 iterations:
 * 1. Objects of fixed size
 * 2. Objects of decreasing size
 * 3. Objects of increasing size
 * The application doesn't have any assumptions about expected behavior.
 * It's supposed to be executed by a test which should set up test parameters (object sizes, number of allocations, etc)
 * and VM flags including flags turning GC logging on. The test will then check the produced log.
 *
 * Expects the following properties to be set:
 * - iterations - amount of iteration per cycle.
 * - chunk.size - size of objects to be allocated
 */
final public class AppPLABResize {

    // Memory to be promoted by PLAB for one thread.
    private static final long MEM_ALLOC_WORDS = 32768;
    // Defined by properties.
    private static final int ITERATIONS = Integer.getInteger("iterations");
    private static final long CHUNK = Long.getLong("chunk.size");

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    /**
     * Main method for AppPLABResizing. Application expect for next properties:
     * iterations, chunk.size and threads.
     *
     * @param args
     */
    public static void main(String[] args) {

        if (ITERATIONS == 0 || CHUNK == 0) {
            throw new IllegalArgumentException("Properties should be set");
        }

        long wordSize = Platform.is32bit() ? 4l : 8l;
        // PLAB size is shared between threads.
        long initialMemorySize = wordSize * MEM_ALLOC_WORDS;

        // Expect changing memory to half during all iterations.
        long memChangeStep = initialMemorySize / 2 / ITERATIONS;

        WHITE_BOX.fullGC();

        // Warm the PLAB. Fill memory ITERATIONS times without changing memory size.
        iterateAllocation(initialMemorySize, 0);
        // Fill memory ITERATIONS times.
        // Initial size is initialMemorySize and step is -memChangeStep
        iterateAllocation(initialMemorySize, -memChangeStep);
        // Fill memory ITERATIONS times.
        // Initial size is memoryAfterChanging, step is memChangeStep.
        // Memory size at start should be greater then last size on previous step.
        // Last size on previous step is initialMemorySize - memChangeStep*(ITERATIONS - 1)
        long memoryAfterChanging = initialMemorySize - memChangeStep * (ITERATIONS - 2);
        iterateAllocation(memoryAfterChanging, memChangeStep);
    }

    private static void iterateAllocation(long memoryToFill, long change) {
        int items;
        if (change > 0) {
            items = (int) ((memoryToFill + change * ITERATIONS) / CHUNK) + 1;
        } else {
            items = (int) (memoryToFill / CHUNK) + 1;
        }

        long currentMemToFill = memoryToFill;
        for (int iteration = 0; iteration < ITERATIONS; ++iteration) {
            MemoryConsumer storage = new MemoryConsumer(items, (int) CHUNK);
            storage.consume(currentMemToFill);
            // Promote all objects to survivor
            WHITE_BOX.youngGC();
            storage.clear();
            currentMemToFill += change;
        }
    }
}
