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
 * @test InitialAndMaxUsageTest
 * @summary testing of initial and max usage
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:-UseCodeCacheFlushing
 *     -XX:-MethodFlushing -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:CompileCommand=compileonly,null::* -XX:-UseLargePages
 *     -XX:+SegmentedCodeCache
 *     compiler.codecache.jmx.InitialAndMaxUsageTest
 * @run main/othervm -Xbootclasspath/a:. -XX:-UseCodeCacheFlushing
 *     -XX:-MethodFlushing -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:CompileCommand=compileonly,null::* -XX:-UseLargePages
 *     -XX:-SegmentedCodeCache
 *     compiler.codecache.jmx.InitialAndMaxUsageTest
 */

package compiler.codecache.jmx;

import jdk.test.lib.Asserts;
import sun.hotspot.code.BlobType;

import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.List;

public class InitialAndMaxUsageTest {

    private static final double CACHE_USAGE_COEF = 0.95d;
    private final BlobType btype;
    private final boolean lowerBoundIsZero;
    private final long maxSize;

    public InitialAndMaxUsageTest(BlobType btype) {
        this.btype = btype;
        this.maxSize = btype.getSize();
        /* Only profiled code cache initial size should be 0, because of
         -XX:CompileCommand=compileonly,null::* non-methods might be not empty,
         as well as non-profiled methods, because it's used as fallback in
         case non-methods is full */
        lowerBoundIsZero = btype == BlobType.MethodProfiled;
    }

    public static void main(String[] args) {
        for (BlobType btype : BlobType.getAvailable()) {
            new InitialAndMaxUsageTest(btype).runTest();
        }
    }

    private boolean canAllocate(double size, long maxSize, MemoryPoolMXBean bean) {
        // Don't fill too much to have space for adapters. So, stop after crossing 95% and
        // don't allocate in case we'll cross 97% on next allocation.
        double used = bean.getUsage().getUsed();
        return (used <= CACHE_USAGE_COEF * maxSize) &&
               (used + size <= (CACHE_USAGE_COEF + 0.02d)  * maxSize);
    }

    protected void runTest() {
        long headerSize = CodeCacheUtils.getHeaderSize(btype);
        MemoryPoolMXBean bean = btype.getMemoryPool();
        long initialUsage = btype.getMemoryPool().getUsage().getUsed();
        System.out.printf("INFO: trying to test %s of max size %d and initial"
                + " usage %d%n", bean.getName(), maxSize, initialUsage);
        Asserts.assertLT(initialUsage + headerSize + 1L, maxSize,
                "Initial usage is close to total size for " + bean.getName());
        if (lowerBoundIsZero) {
            Asserts.assertEQ(initialUsage, 0L, "Unexpected initial usage");
        }
        ArrayList<Long> blobs = new ArrayList<>();
        long minAllocationUnit = Math.max(1, CodeCacheUtils.MIN_ALLOCATION - headerSize);
        /* now filling code cache with large-sized allocation first, since
         lots of small allocations takes too much time, so, just a small
         optimization */
        try {
            for (long size = 100_000 * minAllocationUnit; size > 0; size /= 10) {
                long blob = 0;
                while (canAllocate(size, maxSize, bean) &&
                       (blob = CodeCacheUtils.WB.allocateCodeBlob(size, btype.id)) != 0) {
                    blobs.add(blob);
                }
            }
            Asserts.assertGT((double) bean.getUsage().getUsed(),
                    CACHE_USAGE_COEF * maxSize, String.format("Unable to fill "
                            + "more than %f of %s. Reported usage is %d ",
                            CACHE_USAGE_COEF, bean.getName(),
                            bean.getUsage().getUsed()));
        } finally {
            for (long entry : blobs) {
                CodeCacheUtils.WB.freeCodeBlob(entry);
            }
        }
        System.out.printf("INFO: Scenario finished successfully for %s%n",
                bean.getName());
    }

}
