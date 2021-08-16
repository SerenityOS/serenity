/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test UsageThresholdIncreasedTest
 * @summary verifying that threshold hasn't been hit after allocation smaller
 *     than threshold value and that threshold value can be changed
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing  -XX:-MethodFlushing
 *     -XX:CompileCommand=compileonly,null::*
 *     -XX:-SegmentedCodeCache
 *     compiler.codecache.jmx.UsageThresholdIncreasedTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing  -XX:-MethodFlushing
 *     -XX:CompileCommand=compileonly,null::*
 *     -XX:+SegmentedCodeCache
 *     compiler.codecache.jmx.UsageThresholdIncreasedTest
 */

package compiler.codecache.jmx;

import sun.hotspot.code.BlobType;

import java.lang.management.MemoryPoolMXBean;

public class UsageThresholdIncreasedTest {

    private static final int ALLOCATION_STEP = 5;
    private static final long THRESHOLD_STEP = ALLOCATION_STEP
            * CodeCacheUtils.MIN_ALLOCATION;
    private final BlobType btype;

    public UsageThresholdIncreasedTest(BlobType btype) {
        this.btype = btype;
    }

    public static void main(String[] args) {
        for (BlobType btype : BlobType.getAvailable()) {
            new UsageThresholdIncreasedTest(btype).runTest();
        }
    }

    private void checkUsageThresholdCount(MemoryPoolMXBean bean, long count){
        CodeCacheUtils.assertEQorGTE(btype, bean.getUsageThresholdCount(), count,
                String.format("Usage threshold was hit: %d times for %s "
                        + "Threshold value: %d with current usage: %d",
                        bean.getUsageThresholdCount(), bean.getName(),
                        bean.getUsageThreshold(), bean.getUsage().getUsed()));
    }

    protected void runTest() {
        long headerSize = CodeCacheUtils.getHeaderSize(btype);
        long allocationUnit = Math.max(0, CodeCacheUtils.MIN_ALLOCATION - headerSize);
        MemoryPoolMXBean bean = btype.getMemoryPool();
        long initialCount = bean.getUsageThresholdCount();
        long initialSize = bean.getUsage().getUsed();
        bean.setUsageThreshold(initialSize + THRESHOLD_STEP);
        for (int i = 0; i < ALLOCATION_STEP - 1; i++) {
            CodeCacheUtils.WB.allocateCodeBlob(allocationUnit, btype.id);
        }
        // Usage threshold check is triggered by GC cycle, so, call it
        CodeCacheUtils.WB.fullGC();
        checkUsageThresholdCount(bean, initialCount);
        long filledSize = bean.getUsage().getUsed();
        bean.setUsageThreshold(filledSize + THRESHOLD_STEP);
        for (int i = 0; i < ALLOCATION_STEP - 1; i++) {
            CodeCacheUtils.WB.allocateCodeBlob(allocationUnit, btype.id);
        }
        CodeCacheUtils.WB.fullGC();
        checkUsageThresholdCount(bean, initialCount);
        System.out.println("INFO: Case finished successfully for " + bean.getName());
    }
}
