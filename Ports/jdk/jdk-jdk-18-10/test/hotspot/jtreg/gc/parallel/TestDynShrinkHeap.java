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

package gc.parallel;

/**
 * @test TestDynShrinkHeap
 * @bug 8016479
 * @requires vm.gc.Parallel & os.maxMemory > 1G
 * @summary Verify that the heap shrinks after full GC according to the current values of the Min/MaxHeapFreeRatio flags
 * @modules java.base/jdk.internal.misc
 * @modules jdk.management
 * @library /test/lib /
 * @run main/othervm -XX:+UseAdaptiveSizePolicyWithSystemGC -XX:+UseParallelGC -XX:MinHeapFreeRatio=0 -XX:MaxHeapFreeRatio=100 -Xmx1g -verbose:gc gc.parallel.TestDynShrinkHeap
 */
import jdk.test.lib.management.DynamicVMOption;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.text.NumberFormat;
import static jdk.test.lib.Asserts.assertLessThan;
import com.sun.management.HotSpotDiagnosticMXBean;
import gc.testlibrary.Helpers;

public class TestDynShrinkHeap {

    public static final String MIN_FREE_RATIO_FLAG_NAME = "MinHeapFreeRatio";
    public static final String MAX_FREE_RATIO_FLAG_NAME = "MaxHeapFreeRatio";

    private static ArrayList<byte[]> list = new ArrayList<>(0);
    private static final int LEN = 512 * 1024 + 1;

    public TestDynShrinkHeap() {
    }

    private final void test() {
        System.gc();
        MemoryUsagePrinter.printMemoryUsage("init");

        eat();
        MemoryUsagePrinter.printMemoryUsage("eaten");
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

    private void eat() {
        for (int i = 0; i < LEN; i++) {
            list.add(new byte[1024]);
        }
        MemoryUsagePrinter.printMemoryUsage("allocated " + LEN + " arrays");

        list.subList(0, LEN / 2).clear();
        System.gc();
        MemoryUsagePrinter.printMemoryUsage("array halved");
    }

    private void free() {
        int min = DynamicVMOption.getInt(MIN_FREE_RATIO_FLAG_NAME);
        DynamicVMOption.setInt(MAX_FREE_RATIO_FLAG_NAME, min);
        System.gc();
        MemoryUsagePrinter.printMemoryUsage("under pressure");
    }

    public static void main(String[] args) {
        new TestDynShrinkHeap().test();
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
