/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestHumongousConcurrentStartUndo
 * @summary Tests an alternating sequence of Concurrent Mark and Concurrent Undo
 * cycles.
 * reclaim heap occupancy falls below the IHOP value.
 * @requires vm.gc.G1
 * @library /test/lib /testlibrary /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *             sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   gc.g1.TestHumongousConcurrentStartUndo
 */

import gc.testlibrary.Helpers;

import sun.hotspot.WhiteBox;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.lang.ref.Reference;

public class TestHumongousConcurrentStartUndo {
    // Heap sizes < 224 MB are increased to 224 MB if vm_page_size == 64K to
    // fulfill alignment constraints.
    private static final int HeapSize                       = 224; // MB
    private static final int HeapRegionSize                 = 1;   // MB
    private static final int InitiatingHeapOccupancyPercent = 50;  // %
    private static final int YoungSize                      = HeapSize / 8;

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Xbootclasspath/a:.",
            "-XX:+UseG1GC",
            "-Xms" + HeapSize + "m",
            "-Xmx" + HeapSize + "m",
            "-Xmn" + YoungSize + "m",
            "-XX:G1HeapRegionSize=" + HeapRegionSize + "m",
            "-XX:InitiatingHeapOccupancyPercent=" + InitiatingHeapOccupancyPercent,
            "-XX:-G1UseAdaptiveIHOP",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xlog:gc*",
            EdenObjectAllocatorWithHumongousAllocation.class.getName());

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Pause Young (Concurrent Start) (G1 Humongous Allocation)");
        output.shouldContain("Concurrent Undo Cycle");
        output.shouldContain("Concurrent Mark Cycle");
        output.shouldHaveExitValue(0);
        System.out.println(output.getStdout());
    }

    static class EdenObjectAllocatorWithHumongousAllocation {
        private static final WhiteBox WB = WhiteBox.getWhiteBox();

        private static final int M = 1024 * 1024;
        // Make humongous object size 75% of region size
        private static final int HumongousObjectSize =
                (int)(HeapRegionSize * M * 0.75);
        // Number of objects to allocate to go above IHOP
        private static final int NumHumongousObjectAllocations =
                (int)(((HeapSize - YoungSize) * 80 / 100.0) / HeapRegionSize);


        private static void allocateHumongous(int num, Object[] holder) {
            for (int i = 0; i < num; i++) {
                if (i % 10 == 0) {
                    System.out.println("Allocating humongous object " + i + "/" + num +
                                       " of size " + HumongousObjectSize + " bytes");
                }
                holder[i % holder.length] = new byte[HumongousObjectSize];
            }
        }

        private static void runConcurrentUndoCycle() {
            // Start from an "empty" heap.
            WB.fullGC();
            // The queue only holds one element, so only one humongous object
            // will be reachable and the concurrent operation should be undone.
            allocateHumongous(NumHumongousObjectAllocations, new Object[1]);
            Helpers.waitTillCMCFinished(WB, 1);
        }

        private static void runConcurrentMarkCycle() {
            Object[] a = new Object[NumHumongousObjectAllocations];
            // Start from an "empty" heap.
            WB.fullGC();
            // Try to trigger a concurrent mark cycle. Block concurrent operation
            // while we are allocating more humongous objects than the IHOP threshold.
            // After releasing control, trigger the full cycle.
            try {
                System.out.println("Acquire CM control");
                WB.concurrentGCAcquireControl();
                allocateHumongous(NumHumongousObjectAllocations, a);
            } finally {
                System.out.println("Release CM control");
                WB.concurrentGCReleaseControl();
            }
            // At this point we kept NumHumongousObjectAllocations humongous objects live
            // in "a" which is larger than the IHOP threshold. Another dummy humongous
            // allocation must trigger a concurrent cycle that is not an Undo Cycle.
            allocateHumongous(1, new Object[1]);
            Helpers.waitTillCMCFinished(WB, 1);

            Reference.reachabilityFence(a);
        }

        public static void main(String [] args) throws Exception {
            for (int iterate = 0; iterate < 3; iterate++) {
                runConcurrentUndoCycle();
                runConcurrentMarkCycle();
            }
        }
    }
}

