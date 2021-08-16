/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.mixedgc;

import static sun.hotspot.WhiteBox.getWhiteBox;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;

import gc.testlibrary.g1.MixedGCProvoker;

/*
 * @test TestOldGenCollectionUsage.java
 * @bug 8195115
 * @summary G1 Old Gen's CollectionUsage.used is zero after mixed GC which is incorrect
 * @requires vm.gc.G1
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -verbose:gc -XX:NewSize=2m -XX:MaxNewSize=2m -Xmx14m -Xms14m -XX:+AlwaysTenure -XX:InitiatingHeapOccupancyPercent=100 -XX:-G1UseAdaptiveIHOP -XX:G1MixedGCCountTarget=4 -XX:MaxGCPauseMillis=30000 -XX:G1HeapRegionSize=1m -XX:G1HeapWastePercent=0 -XX:G1MixedGCLiveThresholdPercent=100 gc.g1.mixedgc.TestOldGenCollectionUsage
 */

// 8195115 says that for the "G1 Old Gen" MemoryPool, CollectionUsage.used
// is zero for G1 after a mixed collection, which is incorrect. We can not guarantee
// that we start with an old gen occupancy of zero though due to allocation during
// initialization. We check for "increases during mixed gc" instead.

public class TestOldGenCollectionUsage {

    private static final int HeapRegionSize = 1024 * 1024;
    private static final int ObjectSize = 20_000;

    private String poolName = "G1 Old Gen";
    private String collectorName = "G1 Young Generation";

    public static void main(String [] args) throws Exception {
        TestOldGenCollectionUsage t = new TestOldGenCollectionUsage();
        t.run();
    }

    public TestOldGenCollectionUsage() {
        System.out.println("Monitor G1 Old Gen pool with G1 Young Generation collector.");
    }

    public void run() {
        // Find memory pool and collector
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        MemoryPoolMXBean pool = null;
        boolean foundPool = false;
        for (int i = 0; i < pools.size(); i++) {
            pool = pools.get(i);
            String name = pool.getName();
            if (name.contains(poolName)) {
                System.out.println("Found pool: " + name);
                foundPool = true;
                break;
            }
        }
        if (!foundPool) {
            throw new RuntimeException(poolName + " not found, test with -XX:+UseG1GC");
        }

        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        GarbageCollectorMXBean collector = null;
        boolean foundCollector = false;
        for (int i = 0; i < collectors.size(); i++) {
            collector = collectors.get(i);
            String name = collector.getName();
            if (name.contains(collectorName)) {
                System.out.println("Found collector: " + name);
                foundCollector = true;
                break;
            }
        }
        if (!foundCollector) {
            throw new RuntimeException(collectorName + " not found, test with -XX:+UseG1GC");
        }

        getWhiteBox().fullGC(); // Make sure the heap is in a known state.

        var liveOldObjects = new ArrayList<byte[]>();
        MixedGCProvoker.allocateOldObjects(liveOldObjects, HeapRegionSize, ObjectSize);

        long baseUsage = pool.getCollectionUsage().getUsed();
        System.out.println(poolName + ": usage after GC = " + baseUsage);

        // Verify that collections were done
        long collectionCount = collector.getCollectionCount();
        System.out.println(collectorName + ": collection count = "
                           + collectionCount);
        long collectionTime = collector.getCollectionTime();
        System.out.println(collectorName + ": collection time  = "
                           + collectionTime);
        if (collectionCount <= 0) {
            throw new RuntimeException("Collection count <= 0");
        }
        if (collectionTime <= 0) {
            throw new RuntimeException("Collector has not run");
        }

        MixedGCProvoker.provokeMixedGC(liveOldObjects);

        long usage = pool.getCollectionUsage().getUsed();
        System.out.println(poolName + ": usage after GC = " + usage);
        if (usage <= baseUsage) {
            throw new RuntimeException(poolName + " found not updating usage");
        }

        long newCollectionCount = collector.getCollectionCount();
        System.out.println(collectorName + ": collection count = "
                           + newCollectionCount);
        long newCollectionTime = collector.getCollectionTime();
        System.out.println(collectorName + ": collection time  = "
                           + newCollectionTime);
        if (newCollectionCount <= collectionCount) {
            throw new RuntimeException("No new collection");
        }
        if (newCollectionTime < collectionTime) {
            throw new RuntimeException("Collection time ran backwards");
        }

        System.out.println("Test passed.");
    }
}

