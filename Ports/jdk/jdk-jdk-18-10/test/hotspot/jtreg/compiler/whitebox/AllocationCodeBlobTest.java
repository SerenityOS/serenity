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
 * @test AllocationCodeBlobTest
 * @summary testing of WB::allocate/freeCodeBlob()
 * @bug 8059624 8064669
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *                   -XX:-SegmentedCodeCache
 *                   compiler.whitebox.AllocationCodeBlobTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:CompileCommand=compileonly,null::*
 *                   -XX:+SegmentedCodeCache
 *                   compiler.whitebox.AllocationCodeBlobTest
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import jdk.test.lib.InfiniteLoop;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.BlobType;

import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.EnumSet;

public class AllocationCodeBlobTest {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final long CODE_CACHE_SIZE
            = WHITE_BOX.getUintxVMFlag("ReservedCodeCacheSize");
    private static final int SIZE = 1;

    public static void main(String[] args) {
        // check that Sweeper handels dummy blobs correctly
        Thread t = new Thread(
                new InfiniteLoop(WHITE_BOX::forceNMethodSweep, 1L),
                "ForcedSweeper");
        t.setDaemon(true);
        System.out.println("Starting " + t.getName());
        t.start();

        EnumSet<BlobType> blobTypes = BlobType.getAvailable();
        for (BlobType type : blobTypes) {
            new AllocationCodeBlobTest(type).test();
        }

        // check that deoptimization works well w/ dummy blobs
        t = new Thread(
                new InfiniteLoop(WHITE_BOX::deoptimizeAll, 1L),
                "Deoptimize Thread");
        t.setDaemon(true);
        System.out.println("Starting " + t.getName());
        t.start();

        for (int i = 0; i < 10_000; ++i) {
            for (BlobType type : blobTypes) {
                long addr = WHITE_BOX.allocateCodeBlob(SIZE, type.id);
            }
        }

    }

    private final BlobType type;
    private final MemoryPoolMXBean bean;
    private AllocationCodeBlobTest(BlobType type) {
        this.type = type;
        bean = type.getMemoryPool();
    }

    private void test() {
        System.out.printf("type %s%n", type);

        // Measure the code cache usage after allocate/free.
        long start = getUsage();
        long addr1 = WHITE_BOX.allocateCodeBlob(SIZE, type.id);
        long firstAllocation = getUsage();
        WHITE_BOX.freeCodeBlob(addr1);
        long firstFree = getUsage();
        long addr2 = WHITE_BOX.allocateCodeBlob(SIZE, type.id);
        long secondAllocation = getUsage();
        WHITE_BOX.freeCodeBlob(addr2);

        // The following code may trigger resolving of invokedynamic
        // instructions and therefore method handle intrinsic creation
        // in the code cache. Make sure this is executed after measuring
        // the code cache usage.
        Asserts.assertNE(0, addr1, "first allocation failed");
        Asserts.assertNE(0, addr2, "second allocation failed");
        Asserts.assertLTE(start + SIZE, firstAllocation,
                "allocation should increase memory usage: "
                + start + " + " + SIZE + " <= " + firstAllocation);
        Asserts.assertLTE(firstFree, firstAllocation,
                "free shouldn't increase memory usage: "
                + firstFree + " <= " + firstAllocation);
        Asserts.assertEQ(firstAllocation, secondAllocation);

        System.out.println("allocating till possible...");
        ArrayList<Long> blobs = new ArrayList<>();
        int size = (int) (CODE_CACHE_SIZE >> 7);
        while ((addr1 = WHITE_BOX.allocateCodeBlob(size, type.id)) != 0) {
            blobs.add(addr1);
        }
        for (Long blob : blobs) {
            WHITE_BOX.freeCodeBlob(blob);
        }
    }

    private long getUsage() {
        return bean.getUsage().getUsed();
    }
}
