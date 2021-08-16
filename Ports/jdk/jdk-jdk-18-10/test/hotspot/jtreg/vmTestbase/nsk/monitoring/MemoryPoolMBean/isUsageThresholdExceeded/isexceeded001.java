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

package nsk.monitoring.MemoryPoolMBean.isUsageThresholdExceeded;

import java.lang.management.*;
import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.monitoring.share.*;

import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public class isexceeded001 {
    private static boolean testFailed = false;
    private static final int INCREMENT = 100 * 1024; // 100kb
    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    static byte[] b;

    public static int run(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);
        MemoryMonitor monitor = Monitor.getMemoryMonitor(log, argHandler);
        List pools = monitor.getMemoryPoolMBeans();

        for (int i = 0; i < pools.size(); i++) {
            Object pool = pools.get(i);
            log.display(i + " pool " + monitor.getName(pool));

            if (!monitor.isUsageThresholdSupported(pool)) {
                log.display("  does not support usage thresholds");
                continue;
            } else
                log.display("  supports usage thresholds");

            // Set a threshold that is greater than used value
            MemoryUsage usage = monitor.getUsage(pool);
            long used = usage.getUsed();
            long max = usage.getMax();
            long threshold = used + 1;

            if ( (max > -1) && (threshold > max) ) {
                // we can't test threshold - not enough memory
                log.display("not enough memory for testing threshold:" +
                 " used=" + used +
                 ", max = " + max );
            }

            monitor.setUsageThreshold(pool, threshold);
            log.display("  threshold " + threshold + " is set, used = " + used );

            monitor.resetPeakUsage(pool);
            log.display("  resetting peak usage");
            log.display("  peak usage = " + monitor.getPeakUsage(pool).getUsed());

            // Eat some memory - provoke usage of the pool to cross the
            // threshold value
            b = new byte[INCREMENT]; // Eat 100K

            boolean isExceeded = monitor.isUsageThresholdExceeded(pool);
            usage = monitor.getPeakUsage(pool);
            used = usage.getUsed();

            log.display("  used value is " + used);

            if (used < threshold && isExceeded) {
                // There're problems with isUsageThresholdExceeded()
                log.complain("isUsageThresholdExceeded() returned "
                    + "true, while threshold = " + threshold
                    + " and used peak = " + used);
                    testFailed = true;
            } else
            if (used >= threshold && !isExceeded) {
                // we can introduce some imprecision during pooling memory usage
                // value at the Code Cache memory pool. Amount of used memory
                // was changed after we'd calculated isExceeded value

                if (monitor.isUsageThresholdExceeded(pool)) {
                    // that's mean such imprecision
                    log.display("isUsageThresholdExceeded() returned false,"
                        + " while threshold = " + threshold + " and "
                        + "used peak = " + used);
                } else {
                    // some other problems with isUsageThresholdExceeded()
                    log.complain("isUsageThresholdExceeded() returned false,"
                        + " while threshold = " + threshold + " and "
                        + "used peak = " + used);
                        testFailed = true;
                }
            }

        } // for i

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }
}
