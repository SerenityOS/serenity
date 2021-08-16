/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.MemoryPoolMBean.getUsage;

import java.io.*;
import java.lang.management.*;
import java.util.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class getusage001 {
    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);
        MemoryMonitor monitor = Monitor.getMemoryMonitor(log, argHandler);
        List pools = monitor.getMemoryPoolMBeans();
        MemoryUsage usage = null;

        for (int i = 0; i < pools.size(); i++) {
            byte[] b = new byte[10 * 1024]; // Eat 10K
            Object pool = pools.get(i);

            // No exceptions should be thrown
            try {
                usage = monitor.getUsage(pool);
                log.display(i + " " + monitor.getName(pool) + ": " + usage);
            } catch (Throwable t) {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                log.complain("Unexpected exception in pool "
                           + monitor.getName(pool));
                t.printStackTrace(log.getOutStream());
                testFailed = true;
                continue;
            }

            boolean isValid = monitor.isValid(pool);
            if (isValid) {
                if (usage == null) {
                    log.complain("getPeakUsage() returned null for the valid "
                           + "pool " + monitor.getName(pool));
                    testFailed = true;
                }
            } else {
                if (usage != null) {
                    log.complain("getPeakUsage() returned not-null: " + usage
                               + " for invalid pool " + monitor.getName(pool));
                    testFailed = true;
                }
            }
        } // for i

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }
}
