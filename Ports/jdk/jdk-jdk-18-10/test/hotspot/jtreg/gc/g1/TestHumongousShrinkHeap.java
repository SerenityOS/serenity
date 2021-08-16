/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test TestHumongousShrinkHeap
 * @bug 8036025 8056043
 * @requires vm.gc.G1
 * @summary Verify that heap shrinks after GC in the presence of fragmentation
 * due to humongous objects
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management/sun.management
 * @run main/othervm -XX:-ExplicitGCInvokesConcurrent -XX:MinHeapFreeRatio=10
 * -XX:MaxHeapFreeRatio=12 -XX:+UseG1GC -XX:G1HeapRegionSize=1M -verbose:gc
 * gc.g1.TestHumongousShrinkHeap
 */

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import gc.testlibrary.Helpers;
import static jdk.test.lib.Asserts.*;
import jtreg.SkippedException;

public class TestHumongousShrinkHeap {

    public static final String MIN_FREE_RATIO_FLAG_NAME = "MinHeapFreeRatio";
    public static final String MAX_FREE_RATIO_FLAG_NAME = "MaxHeapFreeRatio";

    private static final List<List<byte[]>> garbage = new ArrayList<>();
    private static final int REGION_SIZE = 1024 * 1024; // 1M
    private static final int LISTS_COUNT = 10;
    private static final int HUMON_SIZE = Math.round(.9f * REGION_SIZE);

    private static final long TOTAL_MEMORY = Runtime.getRuntime().totalMemory();
    private static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();

    private static final int HUMON_COUNT = (int) ((TOTAL_MEMORY / HUMON_SIZE) / LISTS_COUNT);

    public static void main(String[] args) {
        if (HUMON_COUNT == 0) {
            throw new SkippedException("Heap is too small");
        }

        if (TOTAL_MEMORY + REGION_SIZE * HUMON_COUNT > MAX_MEMORY) {
            throw new SkippedException("Initial heap size is to close to max heap size.");
        }

        System.out.format("Running with %s initial heap size of %s maximum heap size. "
                          + "Will allocate humongous object of %s size %d times.%n",
                          MemoryUsagePrinter.NF.format(TOTAL_MEMORY),
                          MemoryUsagePrinter.NF.format(MAX_MEMORY),
                          MemoryUsagePrinter.NF.format(HUMON_SIZE),
                          HUMON_COUNT
        );
        new TestHumongousShrinkHeap().test();
    }

    private final void test() {
        System.gc();
        MemoryUsagePrinter.printMemoryUsage("init");

        allocate();
        MemoryUsagePrinter.printMemoryUsage("allocated");
        MemoryUsage muFull = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        free();
        MemoryUsagePrinter.printMemoryUsage("free");
        MemoryUsage muFree = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        assertLessThan(muFree.getCommitted(), muFull.getCommitted(), String.format(
                "committed free heap size is not less than committed full heap size, heap hasn't been shrunk?%n"
                + "%s = %s%n%s = %s",
                MIN_FREE_RATIO_FLAG_NAME,
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                    .getVMOption(MIN_FREE_RATIO_FLAG_NAME).getValue(),
                MAX_FREE_RATIO_FLAG_NAME,
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                    .getVMOption(MAX_FREE_RATIO_FLAG_NAME).getValue()
        ));
    }

    private void allocate() {

        for (int i = 0; i < LISTS_COUNT; i++) {
            List<byte[]> stuff = new ArrayList<>();
            allocateList(stuff, HUMON_COUNT, HUMON_SIZE);
            MemoryUsagePrinter.printMemoryUsage("allocate #" + (i+1));
            garbage.add(stuff);
        }
    }

    private void free() {
        // do not free last one list
        garbage.subList(0, garbage.size() - 1).clear();

        // do not free last one element from last list
        List<byte[]> stuff = garbage.get(garbage.size() - 1);
        stuff.subList(0, stuff.size() - 1).clear();
        System.gc();
    }

    private static void allocateList(List<byte[]> garbage, int count, int size) {
        for (int i = 0; i < count; i++) {
            garbage.add(new byte[size]);
        }
    }
}

/**
 * Prints memory usage to standard output
 */
class MemoryUsagePrinter {

    public static final NumberFormat NF = Helpers.numberFormatter();

    public static void printMemoryUsage(String label) {
        MemoryUsage memusage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        float freeratio = 1f - (float) memusage.getUsed() / memusage.getCommitted();
        System.out.format("[%-24s] init: %-7s, used: %-7s, comm: %-7s, freeRatio ~= %.1f%%%n",
                label,
                NF.format(memusage.getInit()),
                NF.format(memusage.getUsed()),
                NF.format(memusage.getCommitted()),
                freeratio * 100
        );
    }
}
