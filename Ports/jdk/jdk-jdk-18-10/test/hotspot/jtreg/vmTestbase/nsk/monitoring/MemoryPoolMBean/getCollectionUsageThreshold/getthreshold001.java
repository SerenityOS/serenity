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

package nsk.monitoring.MemoryPoolMBean.getCollectionUsageThreshold;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class getthreshold001 {
    private static boolean testFailed = false;
    private static MemoryMonitor monitor;

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

            boolean isSupported = monitor.isCollectionThresholdSupported(pool);
            if (isSupported) {
                log.display("  supports collection usage thresholds");

                // Test three values for the threshold
                long max = monitor.getUsage(pool).getMax();
                long used = monitor.getUsage(pool).getUsed();

                // max value can be -1, so take an absolute value
                test(monitor, pool, Math.abs(max), log);
                test(monitor, pool, 0, log);
                test(monitor, pool, used, log);
            } else {
                log.display("  does not support collection usage thresholds");

                // UnsupportedOperationException is expected
                try {
                    long threshold = monitor.getCollectionThreshold(pool);
                    log.complain("Threshold " + threshold + " is returned "
                               + "instead of UnsupportedOperationException "
                               + "in pool " + monitor.getName(pool));
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
                                   + "expected in pool "
                                   + monitor.getName(pool));
                        unwrapped.printStackTrace(log.getOutStream());
                        testFailed = true;
                    }
                } // try
            }
        } // for i

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    private static void test(MemoryMonitor monitor, Object pool,
                                                      long threshold, Log log) {
        log.display("  setting threshold " + threshold);
        try {
            monitor.setCollectionThreshold(pool, threshold);
        } catch (Exception e) {
            log.complain("Unexpected exception " + e + " in pool "
                       + monitor.getName(pool));
            e.printStackTrace(log.getOutStream());
            testFailed = true;
            return;
        }
        log.display("  threshold " + threshold + " is set");

        long result = monitor.getCollectionThreshold(pool);
        if (threshold != result) {
            log.complain("Threshold value is " + result + " in pool "
                       + monitor.getName(pool) + ", " + threshold
                       + " expected");
            testFailed = true;
        }
        log.display("  threshold " + threshold + " is read");
    } // test()


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
