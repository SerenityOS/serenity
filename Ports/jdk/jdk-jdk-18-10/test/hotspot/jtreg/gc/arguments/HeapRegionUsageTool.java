/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;

/**
 * Utility class used by tests to get heap region usage.
 */
public final class HeapRegionUsageTool {

    /**
     * Get MemoryUsage from MemoryPoolMXBean which name matches passed string.
     *
     * @param name
     * @return MemoryUsage
     */
    private static MemoryUsage getUsage(String name){
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().matches(name)) {
                return pool.getUsage();
            }
        }
        return null;
    }

    /**
     * Get MemoryUsage of Eden space.
     *
     * @return MemoryUsage
     */
    public static MemoryUsage getEdenUsage() {
        return getUsage(".*Eden.*");
    }

    /**
     * Get MemoryUsage of Survivor space.
     *
     * @return MemoryUsage
     */
    public static MemoryUsage getSurvivorUsage() {
        return getUsage(".*Survivor.*");
    }

    /**
     * Get memory usage of Tenured space
     *
     * @return MemoryUsage
     */
    public static MemoryUsage getOldUsage() {
        return getUsage(".*(Old|Tenured).*");
    }

    /**
     * Get heap usage.
     *
     * @return MemoryUsage
     */
    public static MemoryUsage getHeapUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    /**
     * Helper function to align up.
     *
     * @param value
     * @param alignment
     * @return aligned value
     */
    public static long alignUp(long value, long alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }

    /**
     * Helper function to align down.
     *
     * @param value
     * @param alignment
     * @return aligned value
     */
    public static long alignDown(long value, long alignment) {
        return value & ~(alignment - 1);
    }
}
