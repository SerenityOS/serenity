/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic Test for HotspotThreadMBean.getInternalThreadCount()
 *          and getInternalThreadCpuTime()
 * @author  Mandy Chung
 *
 * @run main/othervm -XX:+UsePerfData GetInternalThreads
 */

import sun.management.*;
import java.util.*;
import java.lang.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

public class GetInternalThreads {
    private final static HotspotThreadMBean mbean =
        ManagementFactoryHelper.getHotspotThreadMBean();

    // Minimum number of VM internal threads
    //   VM thread, watcher thread, Low memory detector, compiler thread
    private static final long MIN_VALUE_FOR_PASS = 4;
    private static final long MAX_VALUE_FOR_PASS = Long.MAX_VALUE;

    public static void main(String[] args) throws Exception {
        long value = mbean.getInternalThreadCount();

        if (value < MIN_VALUE_FOR_PASS || value > MAX_VALUE_FOR_PASS) {
            throw new RuntimeException("Internal thread count " +
                                       "illegal value: " + value + " " +
                                       "(MIN = " + MIN_VALUE_FOR_PASS + "; " +
                                       "MAX = " + MAX_VALUE_FOR_PASS + ")");
        }

        System.out.println("Internal Thread Count = " + value);

        ThreadMXBean thread =
            ManagementFactory.getThreadMXBean();
        if (!thread.isThreadCpuTimeSupported()) {
            System.out.println("Thread Cpu Time is not supported.");
            return;
        }

        while(!testCPUTime()) {
            Thread.sleep(100);
        }
    }

    private static boolean testCPUTime() {
        Map<String, Long> times = mbean.getInternalThreadCpuTimes();
        for(Map.Entry<String, Long> entry : times.entrySet())  {
            String threadName = entry.getKey();
            long cpuTime = entry.getValue();
            System.out.println("CPU time = " + cpuTime + " for " + threadName);
            if (cpuTime == -1) {
                // Can happen when there is a race between a thread being created
                // and the request to get its CPU time. The "/proc/..." structure might
                // not be ready at that time and the routine will return -1.
                System.out.println("Retry, proc structure might not be ready (-1)");
                return false;
            }
            if (cpuTime < 0) {
                throw new RuntimeException("Illegal CPU time: " + cpuTime);
            }
        }
        return true;
    }
}
