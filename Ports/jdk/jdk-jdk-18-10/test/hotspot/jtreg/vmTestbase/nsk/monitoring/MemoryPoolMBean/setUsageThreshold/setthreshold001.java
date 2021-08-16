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

package nsk.monitoring.MemoryPoolMBean.setUsageThreshold;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class setthreshold001 {
    private static MemoryMonitor monitor;
    private static boolean testFailed = false;

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

            boolean isSupported = monitor.isUsageThresholdSupported(pool);
            long max = monitor.getUsage(pool).getMax();
            boolean supportsMax = true;
            long validThreshold = 0;
            long invalidThreshold = 0;
            if (max == -1) {
                // max is undefined for this pool.
                supportsMax = false;
                validThreshold = 1;
                invalidThreshold = Long.MAX_VALUE;
            } else {
                supportsMax = true;
                validThreshold = max;
                invalidThreshold = max + 1;
            }

            if (isSupported) {

                // Usage thresholds are supported
                log.display("  supports usage thresholds");

                // 1. Try to set two different values of thresholds
                monitor.setUsageThreshold(pool, validThreshold);
                log.display("  threshold " + validThreshold + " is set");
                long used = monitor.getUsage(pool).getUsed();
                monitor.setUsageThreshold(pool, used);
                log.display("  threshold " + used + " is set");

                long threshold = monitor.getUsageThreshold(pool);
                if (threshold != used) {
                    log.complain("Threshold value is " + threshold + ", but "
                               + used + " was set in pool "
                               + monitor.getName(pool));
                    testFailed = true;
                } else
                    log.display("  threshold " + threshold + " is read");

                // 2. Try to set negative threshold
                try {
                    monitor.setUsageThreshold(pool, -1);
                    log.complain("IllegalArgumentException is not thrown "
                               + "in pool " + monitor.getName(pool) + " for "
                               + "negative threshold");
                    testFailed = true;
                } catch (Exception e) {

                    // The IllegalArgumentException  is expected, since the
                    // threshold is negative
                    handleIAE(e, log, "Testcase 2.");
                }

                // 3. Try to set threshold that is invalid
                if (supportsMax) {
                    try {
                        // setUsageThreshold() should throw an exception
                        // given that the threshold is invalid
                        monitor.setUsageThreshold(pool, invalidThreshold);
                        log.complain("IllegalArgumentException is not thrown "
                                     + "in pool " + monitor.getName(pool) + " for "
                                     + " threshold " + invalidThreshold + " ("
                                     + monitor.getUsage(pool) + ")");
                        testFailed = true;
                    } catch (Exception e) {

                        // The exception is expected, since the threshold is invalid
                        handleIAE(e, log, "Testcase 3.");
                    }
                } else {
                    // setUsageThreshold() should not throw an
                    // exception given that the memory pool does not
                    // support a max, even though the threshold is invalid

                    monitor.setUsageThreshold(pool, invalidThreshold);
                    log.display("  threshold " + invalidThreshold + " is set");
                    long used2 = monitor.getUsage(pool).getUsed();
                    monitor.setUsageThreshold(pool, used2);
                    log.display("  threshold " + used2 + " is set");

                    long threshold2 = monitor.getUsageThreshold(pool);
                    if (threshold2 != used2) {
                        log.complain("Threshold value is " + threshold2 + ", but "
                                     + used2 + " was set in pool "
                                     + monitor.getName(pool));
                        testFailed = true;
                    } else
                        log.display("  threshold " + threshold2 + " is read");
                }
            } else {

                // 4. If the usage thresholds are not supported, the method
                // must throw UnsupportedOperationException
                log.display("  does not support usage thresholds");

                try {
                    monitor.setUsageThreshold(pool, max);
                    log.complain("UnsupportedOperationException is not thrown "
                               + "in pool " + monitor.getName(pool));
                    testFailed = true;
                } catch (Exception e) {

                    // The exception is expected, since the usage thresholds are
                    // not supported
                    handleUOE(e, log, "Testcase 4.");
                }
            }
        } // for i

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    // Handle UnsupportedOperationException
    private static void handleUOE(Throwable e, Log log, String testcase) {
        Throwable tmp = unwrap(e);

        if (tmp instanceof UnsupportedOperationException) {
            log.display("  " + testcase + " UnsupportedOperationException is "
                      + "thrown");
        } else {
            log.complain("  " + testcase + " Incorrect execption " + tmp + " is "
                       + "thrown, UnsupportedOperationException is expected");
            tmp.printStackTrace(log.getOutStream());
            testFailed = true;
        }
    }

    // Handle IllegalArgumentException
    private static void handleIAE(Throwable e, Log log, String testcase) {
        Throwable tmp = unwrap(e);

        if (tmp instanceof IllegalArgumentException) {
            log.display("  " + testcase + " IllegalArgumentException is "
                      + "thrown");
        } else {
            log.complain("  " + testcase + " Incorrect execption " + tmp + " is "
                       + "thrown, IllegalArgumentException is expected");
            tmp.printStackTrace(log.getOutStream());
            testFailed = true;
        }
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
