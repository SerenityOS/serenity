/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * This is an class used to allocate specified amount of metaspace and heap.
 */
public class GarbageProducer {

    // Uses fixed small objects to avoid Humongous objects allocation with G1 GC.
    private static final int MEMORY_CHUNK = 2048;

    public static List<Object> allocatedMetaspace;
    public static List<Object> allocatedMemory;

    private final MemoryMXBean memoryMXBean;
    private final float targetMemoryUsagePercent;
    private final long targetMemoryUsage;

    /**
     * @param targetMemoryUsagePercent how many percent of metaspace and heap to
     * allocate
     */
    public GarbageProducer(float targetMemoryUsagePercent) {
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.targetMemoryUsagePercent = targetMemoryUsagePercent;
        targetMemoryUsage = (long) (memoryMXBean.getHeapMemoryUsage().getMax() * targetMemoryUsagePercent);
    }

    /**
     * Allocates heap and metaspace upon exit targetMemoryUsagePercent percents
     * of heap and metaspace have been consumed.
     */
    public void allocateMetaspaceAndHeap() {
        // Metaspace should be filled before Java Heap to prevent unexpected OOME
        // in the Java Heap while filling Metaspace
        allocatedMetaspace = eatMetaspace(targetMemoryUsagePercent);
        allocatedMemory = allocateGarbage(targetMemoryUsage);
    }

    private List<Object> eatMetaspace(float targetUsage) {
        List<Object> list = new ArrayList<>();
        MemoryPoolMXBean metaspacePool = getMatchedMemoryPool(".*Metaspace.*");
        float currentUsage;
        GeneratedClassProducer gp = new GeneratedClassProducer();
        do {
            try {
                list.add(gp.create(0));
            } catch (OutOfMemoryError oome) {
                list = null;
                throw new RuntimeException("Unexpected OOME '" + oome.getMessage() + "' while eating " + targetUsage + " of Metaspace.");
            }
            MemoryUsage memoryUsage = metaspacePool.getUsage();
            currentUsage = (((float) memoryUsage.getUsed()) / memoryUsage.getMax());
        } while (currentUsage < targetUsage);
        return list;
    }

    private MemoryPoolMXBean getMatchedMemoryPool(String patternPoolName) {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(bean -> bean.getName().matches(patternPoolName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find '" + patternPoolName + "' memory pool."));
    }

    private List<Object> allocateGarbage(long targetMemoryUsage) {
        List<Object> list = new ArrayList<>();
        do {
            try {
                list.add(new byte[MEMORY_CHUNK]);
            } catch (OutOfMemoryError e) {
                list = null;
                throw new RuntimeException("Unexpected OOME '" + e.getMessage() + "'");
            }
        } while (memoryMXBean.getHeapMemoryUsage().getUsed() < targetMemoryUsage);
        return list;
    }

    /**
     * Returns allocation rate for old gen based on appropriate MemoryPoolMXBean
     * memory usage.
     *
     * @return allocation rate
     */
    public float getOldGenAllocationRatio() {
        MemoryPoolMXBean oldGenBean = getMatchedMemoryPool(".*Old.*|.*Tenured.*");
        MemoryUsage usage = oldGenBean.getUsage();
        System.out.format("Memory usage for %1s.\n", oldGenBean.getName());
        System.out.format("Used: %1d\n", usage.getUsed());
        System.out.format("Commited: %1d\n", usage.getCommitted());
        System.out.format("Max: %1d\n", usage.getMax());
        return ((float) usage.getUsed()) / usage.getCommitted();
    }
}
