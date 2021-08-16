/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.codecache.jmx;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.BlobType;
import sun.hotspot.code.CodeBlob;

import javax.management.Notification;
import java.lang.management.MemoryPoolMXBean;

public final class CodeCacheUtils {

    /**
    * Returns the value to be used for code heap allocation
    */
    public static final int ALLOCATION_SIZE
            = Integer.getInteger("codecache.allocation.size", 100);
    public static final WhiteBox WB = WhiteBox.getWhiteBox();
    public static final long SEGMENT_SIZE
            = WhiteBox.getWhiteBox().getUintxVMFlag("CodeCacheSegmentSize");
    public static final long MIN_BLOCK_LENGTH
            = WhiteBox.getWhiteBox().getUintxVMFlag("CodeCacheMinBlockLength");
    public static final long MIN_ALLOCATION = SEGMENT_SIZE * MIN_BLOCK_LENGTH;

    private CodeCacheUtils() {
        // To prevent from instantiation
    }

    public static final void hitUsageThreshold(MemoryPoolMXBean bean,
            BlobType btype) {
        long initialSize = bean.getUsage().getUsed();
        bean.setUsageThreshold(initialSize + 1);
        long usageThresholdCount = bean.getUsageThresholdCount();
        long addr = WB.allocateCodeBlob(1, btype.id);
        WB.fullGC();
        Utils.waitForCondition(()
                -> bean.getUsageThresholdCount() == usageThresholdCount + 1);
        WB.freeCodeBlob(addr);
    }

    public static final long getHeaderSize(BlobType btype) {
        long addr = WB.allocateCodeBlob(0, btype.id);
        int size = CodeBlob.getCodeBlob(addr).size;
        WB.freeCodeBlob(addr);
        return size;
    }

    public static String getPoolNameFromNotification(
            Notification notification) {
        return ((javax.management.openmbean.CompositeDataSupport)
                notification.getUserData()).get("poolName").toString();
    }

    public static boolean isAvailableCodeHeapPoolName(String name) {
        return BlobType.getAvailable().stream()
                .map(BlobType::getMemoryPool)
                .map(MemoryPoolMXBean::getName)
                .filter(name::equals)
                .findAny().isPresent();
    }

    /**
     * Checks if the usage of the code heap corresponding to 'btype' can be
     * predicted at runtime if we disable compilation. The usage of the
     * 'NonNMethod' code heap can not be predicted because we generate adapters
     * and buffers at runtime. The 'MethodNonProfiled' code heap is also not
     * predictable because we may generate compiled versions of method handle
     * intrinsics while resolving methods at runtime. Same applies to 'All'.
     *
     * @param btype BlobType to be checked
     * @return boolean value, true if respective code heap is predictable
     */
    public static boolean isCodeHeapPredictable(BlobType btype) {
        return btype == BlobType.MethodProfiled;
    }

    /**
     * Verifies that 'newValue' is equal to 'oldValue' if usage of the
     * corresponding code heap is predictable. Checks the weaker condition
     * 'newValue >= oldValue' if usage is not predictable because intermediate
     * allocations may happen.
     *
     * @param btype BlobType of the code heap to be checked
     * @param newValue New value to be verified
     * @param oldValue Old value to be verified
     * @param msg Error message if verification fails
     */
    public static void assertEQorGTE(BlobType btype, long newValue, long oldValue, String msg) {
        if (CodeCacheUtils.isCodeHeapPredictable(btype)) {
            // Usage is predictable, check strong == condition
            Asserts.assertEQ(newValue, oldValue, msg);
        } else {
            // Usage is not predictable, check weaker >= condition
            Asserts.assertGTE(newValue, oldValue, msg);
        }
    }

    /**
     * Verifies that 'newValue' is equal to 'oldValue' if usage of the
     * corresponding code heap is predictable. Checks the weaker condition
     * 'newValue <= oldValue' if usage is not predictable because intermediate
     * allocations may happen.
     *
     * @param btype BlobType of the code heap to be checked
     * @param newValue New value to be verified
     * @param oldValue Old value to be verified
     * @param msg Error message if verification fails
     */
    public static void assertEQorLTE(BlobType btype, long newValue, long oldValue, String msg) {
        if (CodeCacheUtils.isCodeHeapPredictable(btype)) {
            // Usage is predictable, check strong == condition
            Asserts.assertEQ(newValue, oldValue, msg);
        } else {
            // Usage is not predictable, check weaker <= condition
            Asserts.assertLTE(newValue, oldValue, msg);
        }
    }


    public static void disableCollectionUsageThresholds() {
        BlobType.getAvailable().stream()
                .map(BlobType::getMemoryPool)
                .filter(MemoryPoolMXBean::isCollectionUsageThresholdSupported)
                .forEach(b -> b.setCollectionUsageThreshold(0L));
    }
}
