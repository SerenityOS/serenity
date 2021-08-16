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

import sun.hotspot.WhiteBox;

/**
 * This application is part of PLAB promotion test.
 * The application fills a part of young gen with a number of small objects.
 * Then it calls young GC twice to promote objects from eden to survivor, and from survivor to old.
 * The test which running the application is responsible to set up test parameters
 * and VM flags including flags turning GC logging on. The test will then check the produced log.
 */
final public class AppPLABPromotion {

    private final static WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    /**
     * AppPLABPromotion is used for testing PLAB promotion.
     * Expects the following properties to be set:
     * - chunk.size - size of one object (byte array)
     * - mem.to.fill - amount of memory to be consumed
     * - reachable - memory should be consumed by live or dead objects
     *
     * @param args
     */
    public static void main(String[] args) {
        long chunkSize = Long.getLong("chunk.size");
        long memToFill = Long.getLong("mem.to.fill");
        boolean reachable = Boolean.getBoolean("reachable");
        if (chunkSize == 0) {
            throw new IllegalArgumentException("Chunk size must be not 0");
        }
        if (memToFill <= 0) {
            throw new IllegalArgumentException("mem.to.fill property should be above 0");
        }
        // Fill requested amount of memory
        allocate(reachable, memToFill, chunkSize);
        // Promote all allocated objects from Eden to Survivor
        WHITE_BOX.youngGC();
        // Promote all allocated objects from Survivor to Old
        WHITE_BOX.youngGC();
    }

    /**
     *
     * @param reachable - should allocate reachable object
     * @param memSize - Memory to fill
     * @param chunkSize - requested bytes per objects.
     * Actual size of bytes per object will be greater
     */
    private static void allocate(boolean reachable, long memSize, long chunkSize) {
        long realSize = WHITE_BOX.getObjectSize(new byte[(int) chunkSize]);
        int items = (int) ((memSize - 1) / (realSize)) + 1;
        MemoryConsumer storage;
        if (reachable) {
            storage = new MemoryConsumer(items, (int) chunkSize);
        } else {
            storage = new MemoryConsumer(1, (int) chunkSize);
        }
        // Make all young gen available.
        WHITE_BOX.fullGC();
        storage.consume(items * chunkSize);
    }
}
