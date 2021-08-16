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

package nsk.monitoring.MemoryPoolMBean.setCollectionUsageThreshold;

import java.io.*;
import java.util.*;
import java.lang.management.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class setthreshold001 {
    private static class TestParameters {
        private final long max, validThreshold, invalidThreshold;
        private final boolean supportsMax;

        public TestParameters(MemoryUsage usg) {
            max = usg.getMax();
            supportsMax = (max != -1);
            validThreshold = supportsMax ? max : 1;
            invalidThreshold = supportsMax ? max * 2 : Long.MAX_VALUE;
        }
    }
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

            boolean isSupported = monitor.isCollectionThresholdSupported(pool);
            MemoryUsage usage = monitor.getCollectionUsage(pool);
            if (usage == null) {
                log.display("  getCollectionUsage() is not supported: it "
                          + "returned null");
                continue;
            }

            if (isSupported) {
                // Usage thresholds are supported
                log.display("  supports collection usage thresholds");
                testcase1(log, pool);
                testcase2(log, pool);
                testcase3(log, pool);
            } else {
                log.display("  does not support usage thresholds");
                testcase4(log, pool);


            }
        } // for i

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    private static void testcase4(Log log, Object pool) {
        // 4. If the usage thresholds are not supported, the method
        // must throw UnsupportedOperationException
        TestParameters tp = new TestParameters(monitor.getUsage(pool));
        try {
            monitor.setUsageThreshold(pool, tp.validThreshold);
            log.complain("UnsupportedOperationException is not thrown "
                       + "in pool " + monitor.getName(pool));
            testFailed = true;
        } catch (Exception e) {

            // The exception is expected, since the usage thresholds are
            // not supported
            handleUOE(e, log, "Testcase 4.");
        }
    }

    private static void testcase3(Log log, Object pool) {
        // 3. Try to set threshold that is greater than max value
        //    or somehow invalid
        TestParameters tp = new TestParameters(monitor.getUsage(pool));
        if (tp.supportsMax) {
            try {
                // setCollectionThreshold() should throw an exception
                // given that the threshold is invalid

                monitor.setCollectionThreshold(pool, tp.invalidThreshold);
                log.complain("IllegalArgumentException is not thrown "
                             + "in pool " + monitor.getName(pool) + " for "
                             + "threshold " + tp.invalidThreshold + " ("
                             + monitor.getCollectionUsage(pool) + ")");
                testFailed = true;
            } catch (Exception e) {
                // The exception is expected, since the threshold is invalid
                handleIAE(e, log, "Testcase 3.");
            }
        } else {
            try {
                // setCollectionThreshold() should not throw
                // an exception given that the memory pool
                // does not support a max, even though the
                // threshold is invalid

                monitor.setCollectionThreshold(pool, tp.invalidThreshold);
                log.display("  threshold " + tp.invalidThreshold + " is set");
                long used = monitor.getCollectionUsage(pool).getUsed();
                monitor.setCollectionThreshold(pool, used);
                log.display("  threshold " + used + " is set");
                long threshold = monitor.getCollectionThreshold(pool);
                if (threshold != used) {
                    log.complain("Threshold value is " + threshold + ", "
                                 + "but " + used + " was set in pool "
                                 + monitor.getName(pool));
                    testFailed = true;
                } else
                    log.display("  threshold " + threshold + " is read");
            } catch (Exception e) {
                log.complain("Unexpected " + e + " in pool "
                             + monitor.getName(pool));
                e.printStackTrace(log.getOutStream());
                testFailed = true;
            }
        }
    }

    private static void testcase2(Log log, Object pool) {
        // 2. Try to set negative threshold
        try {
            monitor.setCollectionThreshold(pool, -1);
            log.complain("IllegalArgumentException is not thrown "
                       + "in pool " + monitor.getName(pool) + " for "
                       + "negative threshold");
            testFailed = true;
        } catch (Exception e) {

            // The IllegalArgumentException  is expected, since the
            // threshold is negative
            handleIAE(e, log, "Testcase 2.");
        }
    }

    private static void testcase1(Log log, Object pool) {
        // 1. Try to set two different values of thresholds
        try {
            TestParameters tp = new TestParameters(monitor.getUsage(pool));

            monitor.setCollectionThreshold(pool, tp.validThreshold);
            log.display("  threshold " + tp.validThreshold + " is set");
            long used = monitor.getCollectionUsage(pool).getUsed();
            monitor.setCollectionThreshold(pool, used);
            log.display("  threshold " + used + " is set");
            long threshold = monitor.getCollectionThreshold(pool);
            if (threshold != used) {
                log.complain("Threshold value is " + threshold + ", "
                           + "but " + used + " was set in pool "
                           + monitor.getName(pool));
                testFailed = true;
            } else
                log.display("  threshold " + threshold + " is read");
        } catch (Exception e) {
            log.complain("Unexpected " + e + " in pool "
                       + monitor.getName(pool));
            e.printStackTrace(log.getOutStream());
            testFailed = true;
        }
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
