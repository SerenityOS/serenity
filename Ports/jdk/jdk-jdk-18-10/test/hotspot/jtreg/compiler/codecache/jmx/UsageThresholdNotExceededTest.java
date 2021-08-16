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
 * @test UsageThresholdNotExceededTest
 * @summary verifying that usage threshold not exceeded while allocating less
 *     than usage threshold
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing -XX:-MethodFlushing
 *     -XX:CompileCommand=compileonly,null::*
 *     -XX:+SegmentedCodeCache
 *     compiler.codecache.jmx.UsageThresholdNotExceededTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing -XX:-MethodFlushing
 *     -XX:CompileCommand=compileonly,null::*
 *     -XX:-SegmentedCodeCache
 *     compiler.codecache.jmx.UsageThresholdNotExceededTest
 */

package compiler.codecache.jmx;

import sun.hotspot.code.BlobType;

import java.lang.management.MemoryPoolMXBean;

public class UsageThresholdNotExceededTest {

    private final BlobType btype;

    public UsageThresholdNotExceededTest(BlobType btype) {
        this.btype = btype;
    }

    public static void main(String[] args) {
        for (BlobType btype : BlobType.getAvailable()) {
            new UsageThresholdNotExceededTest(btype).runTest();
        }
    }

    protected void runTest() {
        MemoryPoolMXBean bean = btype.getMemoryPool();
        long initialThresholdCount = bean.getUsageThresholdCount();
        long initialUsage = bean.getUsage().getUsed();

        bean.setUsageThreshold(initialUsage + 1 + CodeCacheUtils.MIN_ALLOCATION);
        long size = CodeCacheUtils.getHeaderSize(btype);

        CodeCacheUtils.WB.allocateCodeBlob(Math.max(0, CodeCacheUtils.MIN_ALLOCATION
                - size), btype.id);
        // a gc cycle triggers usage threshold recalculation
        CodeCacheUtils.WB.fullGC();
        CodeCacheUtils.assertEQorGTE(btype, bean.getUsageThresholdCount(), initialThresholdCount,
                String.format("Usage threshold was hit: %d times for %s. "
                        + "Threshold value: %d with current usage: %d",
                        bean.getUsageThresholdCount(), bean.getName(),
                        bean.getUsageThreshold(), bean.getUsage().getUsed()));
        System.out.println("INFO: Case finished successfully for " + bean.getName());
    }
}
