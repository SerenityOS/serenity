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
 * @test
 * @bug     4530538
 * @summary Basic unit test of MemoryMXBean.getMemoryPools() and
 *          MemoryMXBean.getMemoryManager().
 * @requires vm.gc != "Z"
 * @author  Mandy Chung
 *
 * @modules jdk.management
 * @run main MemoryTest 2 3
 */

/*
 * @test
 * @bug     4530538
 * @summary Basic unit test of MemoryMXBean.getMemoryPools() and
 *          MemoryMXBean.getMemoryManager().
 * @requires vm.gc == "Z"
 * @author  Mandy Chung
 *
 * @modules jdk.management
 * @run main MemoryTest 2 1
 */

/*
 * NOTE: This expected result is hardcoded in this test and this test
 *       will be affected if the heap memory layout is changed in
 *       the future implementation.
 */

import java.lang.management.*;
import java.util.*;

public class MemoryTest {
    private static boolean testFailed = false;
    private static MemoryMXBean mm = ManagementFactory.getMemoryMXBean();
    private static final int HEAP = 0;
    private static final int NONHEAP = 1;
    private static final int NUM_TYPES = 2;

    // WARNING: if the number of pools changes in the future,
    // this test needs to be modified to handle different version of VMs.

    // Hotspot VM 1.5 expected to have
    //   heap memory pools     = 3 (Eden, Survivor spaces, Old gen)
    //   non-heap memory pools = 2 (Perm gen, Code cache)
    //                           or 4 if Class Sharing is enabled.
    // Number of memory managers = 3
    // They are: Copy/Scavenger + MSC + CodeCache manager
    // (or equivalent for other collectors)
    // Number of GC memory managers = 2

    // Hotspot VM 1.8+ after perm gen removal is expected to have between two
    // or five non-heap memory pools:
    // - Code cache (between one and three depending on the -XX:SegmentedCodeCache option)
    // - Metaspace
    // - Compressed Class Space (if compressed class pointers are used)

    private static int[] expectedMinNumPools = new int[2];
    private static int[] expectedMaxNumPools = new int[2];
    private static int expectedNumGCMgrs;
    private static int expectedNumMgrs;
    private static String[] types = { "heap", "non-heap" };

    public static void main(String args[]) throws Exception {
        expectedNumGCMgrs = Integer.valueOf(args[0]);
        expectedNumMgrs = expectedNumGCMgrs + 2;

        int expectedNumPools = Integer.valueOf(args[1]);
        expectedMinNumPools[HEAP] = expectedNumPools;
        expectedMaxNumPools[HEAP] = expectedNumPools;

        expectedMinNumPools[NONHEAP] = 2;
        expectedMaxNumPools[NONHEAP] = 5;

        checkMemoryPools();
        checkMemoryManagers();
        if (testFailed)
            throw new RuntimeException("TEST FAILED.");

        System.out.println("Test passed.");

    }

    private static void checkMemoryPools() throws Exception {
        List pools = ManagementFactory.getMemoryPoolMXBeans();
        boolean hasPerm = false;

        int[] numPools = new int[NUM_TYPES];
        for (ListIterator iter = pools.listIterator(); iter.hasNext();) {
            MemoryPoolMXBean pool = (MemoryPoolMXBean) iter.next();
            if (pool.getType() == MemoryType.HEAP) {
                numPools[HEAP]++;
            }
            if (pool.getType() == MemoryType.NON_HEAP) {
                numPools[NONHEAP]++;
            }
            if (pool.getName().toLowerCase().contains("perm")) {
                hasPerm = true;
            }
        }

        if (hasPerm) {
            // If the VM has perm gen there will be between 2 and 4 non heap
            // pools (4 if class data sharing is used)
            expectedMinNumPools[NONHEAP] = 2;
            expectedMaxNumPools[NONHEAP] = 4;
        }

        // Check the number of Memory pools
        for (int i = 0; i < NUM_TYPES; i++) {
            if (numPools[i] < expectedMinNumPools[i] ||
                    numPools[i] > expectedMaxNumPools[i]) {
                throw new RuntimeException("TEST FAILED: " +
                    "Number of " + types[i] + " pools = " + numPools[i] +
                    " but expected <= " + expectedMaxNumPools[i] +
                    " and >= " + expectedMinNumPools[i]);
            }
        }
    }

    private static void checkMemoryManagers() throws Exception {
        List mgrs = ManagementFactory.getMemoryManagerMXBeans();

        int numGCMgr = 0;

        // Check the number of Memory Managers
        for (ListIterator iter = mgrs.listIterator(); iter.hasNext();) {
            MemoryManagerMXBean mgr = (MemoryManagerMXBean) iter.next();
            String[] poolNames = mgr.getMemoryPoolNames();
            if (poolNames == null || poolNames.length == 0) {
                throw new RuntimeException("TEST FAILED: " +
                    "Expected to have one or more pools for " +
                    mgr.getName() + "manager.");
            }

            if (mgr instanceof GarbageCollectorMXBean) {
                numGCMgr++;
            } else {
                for (int i = 0; i < poolNames.length; i++) {
                    checkPoolType(poolNames[i], MemoryType.NON_HEAP);
                }
            }
        }

        if (mgrs.size() != expectedNumMgrs) {
            throw new RuntimeException("TEST FAILED: " +
                "Number of memory managers = " + mgrs.size() +
                " but expected = " + expectedNumMgrs);
        }
        if (numGCMgr != expectedNumGCMgrs) {
            throw new RuntimeException("TEST FAILED: " +
                "Number of GC managers = " + numGCMgr + " but expected = " +
                expectedNumGCMgrs);
        }
    }
    private static List pools = ManagementFactory.getMemoryPoolMXBeans();
    private static void checkPoolType(String name, MemoryType type)
        throws Exception {
        for (ListIterator iter = pools.listIterator(); iter.hasNext(); ) {
            MemoryPoolMXBean pool = (MemoryPoolMXBean) iter.next();
            if (pool.getName().equals(name)) {
                if (pool.getType() != type) {
                    throw new RuntimeException("TEST FAILED: " +
                        "Pool " + pool.getName() + " is of type " +
                        pool.getType() + " but expected to be " + type);
                } else {
                    return;
                }
            }
        }
        throw new RuntimeException("TEST FAILED: " +
            "Pool " + name + " is of type " + type +
            " not found");
    }
}
