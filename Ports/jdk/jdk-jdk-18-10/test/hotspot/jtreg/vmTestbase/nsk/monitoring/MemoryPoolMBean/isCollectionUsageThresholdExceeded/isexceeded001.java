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

package nsk.monitoring.MemoryPoolMBean.isCollectionUsageThresholdExceeded;

import java.lang.management.*;
import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class isexceeded001 {
    private static boolean testFailed = false;
    static MemoryMonitor monitor;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);
        monitor = Monitor.getMemoryMonitor(log, argHandler);
        List pools = monitor.getMemoryPoolMBeans();

        for (int i = 0; i < pools.size(); i++) {
            Object pool = pools.get(i);
            log.display(i + " pool " + monitor.getName(pool));

            if (!monitor.isCollectionThresholdSupported(pool)) {
                log.display("  does not support collection usage thresholds");

                // UnsupportedOperationException is expected
                try {
                    boolean isExceeded
                        = monitor.isCollectionThresholdExceeded(pool);
                    log.complain("isCollectionUsageThresholdExceeded() returned"
                               + " " + isExceeded + "instead of "
                               + "UnsupportedOperationException in pool "
                               + monitor.getName(pool));
                    testFailed = true;
                } catch (Exception e) {

                    Throwable unwrapped = unwrap(e);

                    if (unwrapped instanceof UnsupportedOperationException) {
                        log.display("  UnsupportedOperationException is "
                                  + "thrown");
                    } else {
                        log.complain("Incorrect execption " + unwrapped
                                   + " is thrown, "
                                   + "UnsupportedOperationException is "
                                   + "expected");
                        unwrapped.printStackTrace(log.getOutStream());
                        testFailed = true;
                    }
                } // try
                continue;
            } // if

            log.display("  supports collection usage thresholds");

            // Set a threshold that is greater than used value
            MemoryUsage usage = monitor.getCollectionUsage(pool);
            long used = usage.getUsed();
            long max = usage.getMax();
            long threshold = used + 1;

            if ( (max > -1) && (threshold > max) )
                threshold = max;
            log.display("  setting threshold " + threshold + " "
                      + monitor.getCollectionUsage(pool));
            try {
                monitor.setCollectionThreshold(pool, threshold);
            } catch (Exception e) {
                log.complain("Unexpected " + e + " in pool "
                           + monitor.getName(pool));
                e.printStackTrace(log.getOutStream());
                testFailed = true;
                continue;
            }
            boolean isExceeded = monitor.isCollectionThresholdExceeded(pool);
            log.display("  threshold " + threshold + " is set, (used = " + used
                      + ", isExceeded = " + isExceeded + ")");

            // Eat some memory - provoke usage of the pool to cross the
            // threshold value
            byte[] b = new byte[100 * 1024]; // Eat 100K

            threshold = monitor.getCollectionThreshold(pool);
            usage = monitor.getCollectionUsage(pool);
            used = usage.getUsed();

            if (used >= threshold) {
                log.display("  used value (" + used + ") crossed the threshold "
                          + "value (" + threshold + ")");

                isExceeded = monitor.isCollectionThresholdExceeded(pool);
                if (!isExceeded) {

                    // Refresh the values
                    threshold = monitor.getCollectionThreshold(pool);
                    usage = monitor.getCollectionUsage(pool);
                    if (used >= threshold) {
                        log.complain("isCollectionUsageThresholdExceeded() "
                                   + "returned false, while threshold = "
                                   + threshold + " and " + "used = " + used);
                        testFailed = true;
                    }
                }
            } else {
                log.display("  used value (" + used + ") did not cross the "
                          + "threshold value (" + threshold + ")");

                isExceeded = monitor.isCollectionThresholdExceeded(pool);
                if (isExceeded) {

                    // Refresh the values
                    threshold = monitor.getCollectionThreshold(pool);
                    usage = monitor.getCollectionUsage(pool);
                    if (used < threshold) {
                        log.complain("isCollectionUsageThresholdExceeded() "
                                   + "returned true, while threshold = "
                                   + threshold + " and " + "used = " + used);
                        testFailed = true;
                    }
                }
            }
        } // for i

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    static Throwable unwrap(Throwable throwable) {

        Throwable unwrapped, t = throwable;

        do {
            unwrapped = t;

            if (unwrapped instanceof UnsupportedOperationException) {
                break;
            }

            t = unwrapped.getCause();

        } while (t != null);

        return unwrapped;
    }
}
