/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * Utility to obtain memory pools statistics
 *
 */
public class Pools {

    private static final String EDEN_SPACE_POOL = "Eden Space";
    private static final String OLD_GEN_POOL = "Old Gen";
    private static final String METASPACE_POOL = "Metaspace";
    private static final String SURVIVOR_SPACE = "Survivor Space";

    public static long getNGMaxSize() {
        // NewGen is consists of Eden and two Survivor spaces
        return getPoolMaxSize(EDEN_SPACE_POOL) + 2 * getPoolMaxSize(SURVIVOR_SPACE);
    }

    public static long getHeapCommittedSize() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted() / 1024;
    }

    public static long getEdenCommittedSize() {
        return getPoolCommittedSize(EDEN_SPACE_POOL);
    }

    public static long getOldGenCommittedSize() {
        return getPoolCommittedSize(OLD_GEN_POOL);
    }

    public static long getMetaspaceCommittedSize() {
        return getPoolCommittedSize(METASPACE_POOL);
    }

    private static long getPoolMaxSize(String poolName) {
        long result;
        MemoryPoolMXBean pool = findPool(poolName);
        if (pool != null) {
            if (pool.getUsage().getMax() == -1) {
                result = -1;
            } else {
                result = pool.getUsage().getCommitted() / 1024;
            }
        } else {
            throw new RuntimeException("Pool '" + poolName + "' wasn't found");
        }
        log("Max size of the pool '" + poolName + "' is " + result);
        return result;
    }

    private static long getPoolCommittedSize(String poolName) {
        long result;
        MemoryPoolMXBean pool = findPool(poolName);
        if (pool != null) {
            if (pool.getUsage().getCommitted() == -1) {
                result = -1;
            } else {
                result = pool.getUsage().getCommitted() / 1024;
            }
        } else {
            throw new RuntimeException("Pool '" + poolName + "' wasn't found");
        }
        log("Committed size of the pool '" + poolName + "' is " + result);
        return result;
    }

    private static MemoryPoolMXBean findPool(String poolName) {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().contains(poolName)) {
                return pool;
            }
        }
        return null;
    }

    private static void log(String s) {
        System.out.println(s);
    }

}
