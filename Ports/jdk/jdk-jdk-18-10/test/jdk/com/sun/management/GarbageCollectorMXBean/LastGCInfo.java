/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4982301
 * @summary Sanity Test for GarbageCollectorMXBean.getLastGcInfo().
 * @author  Mandy Chung
 *
 * @run main/othervm -XX:-ExplicitGCInvokesConcurrent LastGCInfo
 */
// Passing "-XX:-ExplicitGCInvokesConcurrent" to force System.gc()
// run on foreground when CMS is used and prevent situations when "GcInfo"
// is missing even though System.gc() was successfuly processed.

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.MemoryPoolMXBean;
import java.util.*;
import com.sun.management.GcInfo;
import com.sun.management.GarbageCollectorMXBean;

public class LastGCInfo {
    public static void main(String[] argv) throws Exception {
        boolean hasGcInfo = false;

        System.gc();
        List mgrs = ManagementFactory.getGarbageCollectorMXBeans();
        for (ListIterator iter = mgrs.listIterator(); iter.hasNext(); ) {
            Object mgr = iter.next();
            if (mgr instanceof GarbageCollectorMXBean) {
                GarbageCollectorMXBean gc = (GarbageCollectorMXBean) mgr;
                GcInfo info = gc.getLastGcInfo();
                if (info != null) {
                    checkGcInfo(gc.getName(), info);
                    hasGcInfo = true;
                }
            }
        }

        if (! hasGcInfo) {
            throw new RuntimeException("No GcInfo returned");
        }
        System.out.println("Test passed.");
    }

    private static void checkGcInfo(String name, GcInfo info) throws Exception {
        System.out.println("GC statistic for : " + name);
        System.out.print("GC #" + info.getId());
        System.out.print(" start:" + info.getStartTime());
        System.out.print(" end:" + info.getEndTime());
        System.out.println(" (" + info.getDuration() + "ms)");
        Map usage = info.getMemoryUsageBeforeGc();

        List pnames = new ArrayList();
        for (Iterator iter = usage.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            String poolname = (String) entry.getKey();
            pnames.add(poolname);
            MemoryUsage busage = (MemoryUsage) entry.getValue();
            MemoryUsage ausage = (MemoryUsage) info.getMemoryUsageAfterGc().get(poolname);
            if (ausage == null) {
                throw new RuntimeException("After Gc Memory does not exist" +
                    " for " + poolname);
            }
            System.out.println("Usage for pool " + poolname);
            System.out.println("   Before GC: " + busage);
            System.out.println("   After GC: " + ausage);
        }

        // check if memory usage for all memory pools are returned
        List pools = ManagementFactory.getMemoryPoolMXBeans();
        for (Iterator iter = pools.iterator(); iter.hasNext(); ) {
            MemoryPoolMXBean p = (MemoryPoolMXBean) iter.next();
            if (!pnames.contains(p.getName())) {
                throw new RuntimeException("GcInfo does not contain " +
                    "memory usage for pool " + p.getName());
            }
        }
    }
}
