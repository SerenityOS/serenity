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
 * @test GetUsageTest
 * @summary testing of getUsage() for segmented code cache
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *     -XX:-UseCodeCacheFlushing -XX:-MethodFlushing
 *     -XX:+SegmentedCodeCache
 *     compiler.codecache.jmx.GetUsageTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *     -XX:-UseCodeCacheFlushing -XX:-MethodFlushing
 *     -XX:-SegmentedCodeCache
 *     compiler.codecache.jmx.GetUsageTest
 */

package compiler.codecache.jmx;

import jdk.test.lib.Asserts;
import sun.hotspot.code.BlobType;

import java.lang.management.MemoryPoolMXBean;
import java.util.HashMap;
import java.util.Map;

public class GetUsageTest {

    private final BlobType btype;
    private final int allocateSize;

    public GetUsageTest(BlobType btype, int allocSize) {
        this.btype = btype;
        this.allocateSize = allocSize;
    }

    public static void main(String[] args) throws Exception {
        for (BlobType btype : BlobType.getAvailable()) {
            for (int allocSize = 10; allocSize < 100000; allocSize *= 10) {
                new GetUsageTest(btype, allocSize).runTest();
            }
        }
    }

    protected final Map<MemoryPoolMXBean, Long> getBeanUsages() {
        Map<MemoryPoolMXBean, Long> beanUsages = new HashMap<>();
        for (BlobType bt : BlobType.getAvailable()) {
            beanUsages.put(bt.getMemoryPool(),
                    bt.getMemoryPool().getUsage().getUsed());
        }
        return beanUsages;
    }

    protected void runTest() {
        MemoryPoolMXBean[] predictableBeans = BlobType.getAvailable().stream()
                .filter(CodeCacheUtils::isCodeHeapPredictable)
                .map(BlobType::getMemoryPool)
                .toArray(MemoryPoolMXBean[]::new);
        Map<MemoryPoolMXBean, Long> initial = getBeanUsages();
        long addr = 0;
        try {
            addr = CodeCacheUtils.WB.allocateCodeBlob(allocateSize, btype.id);
            Map<MemoryPoolMXBean, Long> current = getBeanUsages();
            long blockCount = Math.floorDiv(allocateSize
                    + CodeCacheUtils.getHeaderSize(btype)
                    + CodeCacheUtils.SEGMENT_SIZE - 1, CodeCacheUtils.SEGMENT_SIZE);
            long usageUpperEstimate = Math.max(blockCount,
                    CodeCacheUtils.MIN_BLOCK_LENGTH) * CodeCacheUtils.SEGMENT_SIZE;
            for (MemoryPoolMXBean entry : predictableBeans) {
                long diff = current.get(entry) - initial.get(entry);
                if (entry.equals(btype.getMemoryPool())) {
                    if (CodeCacheUtils.isCodeHeapPredictable(btype)) {
                        Asserts.assertFalse(diff <= 0L || diff > usageUpperEstimate,
                                String.format("Pool %s usage increase was reported "
                                        + "unexpectedly as increased by %d using "
                                        + "allocation size %d", entry.getName(),
                                        diff, allocateSize));
                    }
                } else {
                    CodeCacheUtils.assertEQorGTE(btype, diff, 0L,
                            String.format("Pool %s usage changed unexpectedly while"
                                    + " trying to increase: %s using allocation "
                                    + "size %d", entry.getName(),
                                    btype.getMemoryPool().getName(), allocateSize));
                }
            }
        } finally {
            if (addr != 0) {
                CodeCacheUtils.WB.freeCodeBlob(addr);
            }
        }
        System.out.printf("INFO: Scenario finished successfully for %s%n",
                btype.getMemoryPool().getName());
    }
}
