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
 * @bug     4892507
 * @summary Basic Test for the following reset methods:
 *          - ThreadMXBean.findMonitorDeadlockedThreads()
 * @author  Mandy Chung
 *
 * @build MonitorDeadlock
 * @build ThreadDump
 * @run main/othervm FindMonitorDeadlock
 */

import java.lang.management.*;
import java.util.*;

public class FindMonitorDeadlock {
    public static void main(String[] argv) {
        // create deadlocked threads
        MonitorDeadlock md = new MonitorDeadlock();

        // No deadlock
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        long[] threads = mbean.findMonitorDeadlockedThreads();
        if (threads != null) {
            throw new RuntimeException("TEST FAILED: Should return null.");
        }

        // Let the threads to proceed
        md.goDeadlock();

        // wait until the deadlock is ready
        md.waitUntilDeadlock();

        // Find Deadlock
        threads = mbean.findMonitorDeadlockedThreads();
        if (threads == null) {
            ThreadDump.dumpStacks();
            throw new RuntimeException("TEST FAILED: Deadlock not detected.");
        }

        // Print Deadlock stack trace
        System.out.println("Found threads that are in deadlock:-");
        ThreadInfo[] infos = mbean.getThreadInfo(threads, Integer.MAX_VALUE);
        for (int i = 0; i < infos.length; i++) {
            ThreadDump.printThreadInfo(infos[i]);
        }

        md.checkResult(threads);
        System.out.println("Test passed");
    }
}
