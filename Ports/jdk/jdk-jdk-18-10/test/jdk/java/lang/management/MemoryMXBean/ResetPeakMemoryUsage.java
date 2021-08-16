/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * The -XX:MarkSweepAlwaysCompactCount=1 argument below makes sure serial gc
 * compacts the heap at every full gc so that the usage is correctly updated.
 */

/*
 * @test
 * @bug     4892507
 * @summary Basic Test for MemoryPool.resetPeakUsage()
 * @author  Mandy Chung
 *
 * @requires vm.opt.ExplicitGCInvokesConcurrent != "true"
 * @requires vm.opt.DisableExplicitGC != "true"
 * @library /test/lib
 * @modules jdk.management
 *
 * @build ResetPeakMemoryUsage MemoryUtil RunUtil
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. ResetPeakMemoryUsage
 */

import java.lang.management.*;
import java.lang.ref.WeakReference;
import java.util.*;

import sun.hotspot.code.Compiler;

public class ResetPeakMemoryUsage {
    private static MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
    // make public so that it can't be optimized away easily
    public static Object[] obj;

    /**
     * Run the test multiple times with different GC versions.
     * First with default command line specified by the framework.
     * Then with all GC versions specified by the test.
     */
    public static void main(String a[]) throws Throwable {
        final String main = "ResetPeakMemoryUsage$TestMain";
        final String ms = "-Xms256m";
        final String mn = "-Xmn8m";

        RunUtil.runTestClearGcOpts(main, ms, mn, "-XX:+UseParallelGC");
        RunUtil.runTestClearGcOpts(main, ms, mn, "-XX:+UseG1GC", "-XX:G1HeapRegionSize=1m");
        RunUtil.runTestClearGcOpts(main, ms, mn, "-XX:+UseSerialGC",
                "-XX:MarkSweepAlwaysCompactCount=1");
    }

    private static class TestMain {
        public static void main(String[] argv) {
            List pools = ManagementFactory.getMemoryPoolMXBeans();
            ListIterator iter = pools.listIterator();
            boolean found = false;
            while (iter.hasNext()) {
                MemoryPoolMXBean p = (MemoryPoolMXBean) iter.next();
                // only check heap pools that support usage threshold
                // this is typically only the old generation space
                // since the other spaces are expected to get filled up
                if (p.getType() == MemoryType.HEAP &&
                    p.isUsageThresholdSupported())
                {
                    found = true;
                    testPool(p);
                }
            }
            if (!found) {
                throw new RuntimeException("No heap pool found");
            }
        }
    }

    private static void testPool(MemoryPoolMXBean mpool) {
        System.out.println("Selected memory pool: ");
        MemoryUtil.printMemoryPool(mpool);

        MemoryUsage usage0 = mpool.getUsage();
        MemoryUsage peak0 = mpool.getPeakUsage();

        // use a size that is larger than the young generation and G1 regions
        // to force the array into the old gen
        int largeArraySize = 9 * 1000 * 1000;

        System.out.println("Before big object array (of size "+largeArraySize+") is allocated: ");
        printMemoryUsage(usage0, peak0);

        obj = new Object[largeArraySize];
        WeakReference<Object> weakRef = new WeakReference<>(obj);

        MemoryUsage usage1 = mpool.getUsage();
        MemoryUsage peak1 = mpool.getPeakUsage();
        System.out.println("After the object is allocated: ");
        printMemoryUsage(usage1, peak1);

        if (usage1.getUsed() <= usage0.getUsed()) {
            throw new RuntimeException(
                formatSize("Before allocation: used", usage0.getUsed()) +
                " expected to be < " +
                formatSize("After allocation: used", usage1.getUsed()));
        }

        if (peak1.getUsed() <= peak0.getUsed()) {
            throw new RuntimeException(
                formatSize("Before allocation: peak", peak0.getUsed()) +
                " expected to be < " +
                formatSize("After allocation: peak", peak1.getUsed()));
        }


        // The object is now garbage and do a GC
        // memory usage should drop
        obj = null;

        //This will cause sure shot GC unlike Runtime.gc() invoked by mbean.gc()
        while(weakRef.get() != null) {
            mbean.gc();
        }

        MemoryUsage usage2 = mpool.getUsage();
        MemoryUsage peak2 = mpool.getPeakUsage();
        System.out.println("After GC: ");
        printMemoryUsage(usage2, peak2);

        if (usage2.getUsed() >= usage1.getUsed()) {
            throw new RuntimeException(
                formatSize("Before GC: used", usage1.getUsed()) + " " +
                " expected to be > " +
                formatSize("After GC: used", usage2.getUsed()));
        }

        mpool.resetPeakUsage();

        MemoryUsage usage3 = mpool.getUsage();
        MemoryUsage peak3 = mpool.getPeakUsage();
        System.out.println("After resetPeakUsage: ");
        printMemoryUsage(usage3, peak3);

        if (peak3.getUsed() != usage3.getUsed()) {
            throw new RuntimeException(
                formatSize("After resetting peak: peak", peak3.getUsed()) + " " +
                " expected to be equal to " +
                formatSize("current used", usage3.getUsed()));
        }

        if (peak3.getUsed() >= peak2.getUsed()) {
            throw new RuntimeException(
                formatSize("After resetting peak: peak", peak3.getUsed()) + " " +
                " expected to be < " +
                formatSize("previous peak", peak2.getUsed()));
        }

        System.out.println(RunUtil.successMessage);
    }

    private static String INDENT = "    ";
    private static void printMemoryUsage(MemoryUsage current, MemoryUsage peak) {
        System.out.println("Current Usage: ");
        MemoryUtil.printMemoryUsage(current);
        System.out.println("Peak Usage: ");
        MemoryUtil.printMemoryUsage(peak);

    }
    private static String formatSize(String name, long value) {
        StringBuffer buf = new StringBuffer(name + " = " + value);
        if (value > 0) {
            buf.append(" (" + (value >> 10) + "K)");
        }
        return buf.toString();
    }
}
