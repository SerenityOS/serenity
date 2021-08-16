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
 * @test PeakUsageTest
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *     -XX:+SegmentedCodeCache
 *     compiler.codecache.jmx.PeakUsageTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *     -XX:-SegmentedCodeCache
 *     compiler.codecache.jmx.PeakUsageTest
 * @summary testing of getPeakUsage() and resetPeakUsage for
 *     segmented code cache
 */

package compiler.codecache.jmx;

import sun.hotspot.code.BlobType;

import java.lang.management.MemoryPoolMXBean;


public class PeakUsageTest {

    private final BlobType btype;

    public PeakUsageTest(BlobType btype) {
        this.btype = btype;
    }

    public static void main(String[] args) {
        for (BlobType btype : BlobType.getAvailable()) {
            new PeakUsageTest(btype).runTest();
        }
    }

    protected void runTest() {
        MemoryPoolMXBean bean = btype.getMemoryPool();
        bean.resetPeakUsage();
        long addr = CodeCacheUtils.WB.allocateCodeBlob(
                CodeCacheUtils.ALLOCATION_SIZE, btype.id);

        try {
            /*
            Always save peakUsage after saving currentUsage. Reversing the order
            can lead to inconsistent results (currentUsage > peakUsage) because
            of intermediate allocations.
            */
            long currUsage = bean.getUsage().getUsed();
            long peakUsage = bean.getPeakUsage().getUsed();
            CodeCacheUtils.assertEQorLTE(btype, currUsage,
                    peakUsage,
                    "Peak usage does not match usage after allocation for "
                    + bean.getName());
        } finally {
            if (addr != 0) {
                CodeCacheUtils.WB.freeCodeBlob(addr);
            }
        }
        bean.resetPeakUsage();
        long currUsage = bean.getUsage().getUsed();
        long peakUsage = bean.getPeakUsage().getUsed();
        CodeCacheUtils.assertEQorLTE(btype, currUsage,
                peakUsage,
                "Code cache peak usage is not equal to usage after reset for "
                + bean.getName());
        long addr2 = CodeCacheUtils.WB.allocateCodeBlob(
                CodeCacheUtils.ALLOCATION_SIZE, btype.id);
        try {
            currUsage = bean.getUsage().getUsed();
            peakUsage = bean.getPeakUsage().getUsed();

            CodeCacheUtils.assertEQorLTE(btype, currUsage,
                    peakUsage,
                    "Code cache peak usage is not equal to usage after fresh "
                    + "allocation for " + bean.getName());
        } finally {
            if (addr2 != 0) {
                CodeCacheUtils.WB.freeCodeBlob(addr2);
            }
        }
        System.out.printf("INFO: Scenario finished successfully for %s%n",
                bean.getName());
    }
}
