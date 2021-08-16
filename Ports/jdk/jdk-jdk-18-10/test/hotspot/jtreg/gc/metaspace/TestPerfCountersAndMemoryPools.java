/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.metaspace;

import java.util.List;
import java.lang.management.*;

import jdk.test.lib.Platform;
import static jdk.test.lib.Asserts.*;
import gc.testlibrary.PerfCounters;

/* @test TestPerfCountersAndMemoryPools
 * @bug 8023476
 * @library /test/lib /
 * @requires vm.gc.Serial
 * @summary Tests that a MemoryPoolMXBeans and PerfCounters for metaspace
 *          report the same data.
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-UseCompressedOops -XX:-UseCompressedClassPointers -XX:+UseSerialGC -XX:+UsePerfData -Xint gc.metaspace.TestPerfCountersAndMemoryPools
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+UseSerialGC -XX:+UsePerfData -Xint gc.metaspace.TestPerfCountersAndMemoryPools
 */
public class TestPerfCountersAndMemoryPools {
    public static void main(String[] args) throws Exception {
        checkMemoryUsage("Metaspace", "sun.gc.metaspace");

        if (InputArguments.contains("-XX:+UseCompressedClassPointers") && Platform.is64bit()) {
            checkMemoryUsage("Compressed Class Space", "sun.gc.compressedclassspace");
        }
    }

    private static MemoryPoolMXBean getMemoryPool(String memoryPoolName) {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().equals(memoryPoolName)) {
                return pool;
            }
        }

        throw new RuntimeException("Excpted to find a memory pool with name " +
                                   memoryPoolName);
    }

    private static void checkMemoryUsage(String memoryPoolName, String perfNS)
        throws Exception {
        MemoryPoolMXBean pool = getMemoryPool(memoryPoolName);

        // First, call all the methods to let them allocate their own slab of metadata
        getMinCapacity(perfNS);
        getCapacity(perfNS);
        getUsed(perfNS);
        pool.getUsage().getInit();
        pool.getUsage().getUsed();
        pool.getUsage().getCommitted();
        assertEQ(1L, 1L, "Make assert load");

        // Must do a GC to update performance counters
        System.gc();
        assertEQ(getMinCapacity(perfNS), pool.getUsage().getInit(), "MinCapacity out of sync");

        // Adding a second GC due to metadata allocations caused by getting the
        // initial size from the pool. This is needed when running with -Xcomp.
        System.gc();
        assertEQ(getUsed(perfNS), pool.getUsage().getUsed(), "Used out of sync");
        assertEQ(getCapacity(perfNS), pool.getUsage().getCommitted(), "Committed out of sync");
    }

    private static long getMinCapacity(String ns) throws Exception {
        return PerfCounters.findByName(ns + ".minCapacity").longValue();
    }

    private static long getCapacity(String ns) throws Exception {
        return PerfCounters.findByName(ns + ".capacity").longValue();
    }

    private static long getUsed(String ns) throws Exception {
        return PerfCounters.findByName(ns + ".used").longValue();
    }
}
