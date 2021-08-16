/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc;

/**
 * @test TestFullGCCount.java
 * @bug 7072527
 * @summary JMM GC counters overcount in some cases
 * @comment Shenandoah has "ExplicitGCInvokesConcurrent" on by default
 * @requires !(vm.gc == "Shenandoah" & vm.opt.ExplicitGCInvokesConcurrent != false)
 * @requires vm.gc != "Z"
 * @modules java.management
 * @run main/othervm -Xlog:gc gc.TestFullGCCount
 */

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * Originally for a specific failure in CMS[[keep]], this test now monitors all
 * collectors for double-counting of collections.
 */
public class TestFullGCCount {

    static List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();

    public static void main(String[] args) {
        int iterations = 20;
        boolean failed = false;
        String errorMessage = "";
        HashMap<String, List<Long>> counts = new HashMap<>();

        // Prime the collection of count lists for all collectors.
        for (int i = 0; i < collectors.size(); i++) {
            GarbageCollectorMXBean collector = collectors.get(i);
            counts.put(collector.getName(), new ArrayList<>(iterations));
        }

        // Perform some gc, record collector counts.
        for (int i = 0; i < iterations; i++) {
            System.gc();
            addCollectionCount(counts, i);
        }

        // Check the increments:
        //   Old gen collectors should increase by one,
        //   New collectors may or may not increase.
        //   Any increase >=2 is unexpected.
        for (String collector : counts.keySet()) {
            System.out.println("Checking: " + collector);

            for (int i = 0; i < iterations - 1; i++) {
                List<Long> theseCounts = counts.get(collector);
                long a = theseCounts.get(i);
                long b = theseCounts.get(i + 1);
                if (b - a >= 2) {
                    failed = true;
                    errorMessage += "Collector '" + collector + "' has increment " + (b - a) +
                                    " at iteration " + i + "\n";
                }
            }
        }
        if (failed) {
            System.err.println(errorMessage);
            throw new RuntimeException("FAILED: System.gc collections miscounted.");
        }
        System.out.println("Passed.");
    }

    private static void addCollectionCount(HashMap<String, List<Long>> counts, int iteration) {
        for (int i = 0; i < collectors.size(); i++) {
            GarbageCollectorMXBean collector = collectors.get(i);
            List<Long> thisList = counts.get(collector.getName());
            thisList.add(collector.getCollectionCount());
        }
    }
}
