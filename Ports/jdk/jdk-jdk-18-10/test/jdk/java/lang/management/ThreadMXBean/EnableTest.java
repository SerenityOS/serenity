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
 * @summary Basic unit test of
 *          ThreadMXBean.setThreadContentionMonitoringEnabled()
 *          and ThreadMXBean.setThreadCpuTimeEnabled().
 * @author  Mandy Chung
 *
 * @run main EnableTest
 */

import java.lang.management.*;

public class EnableTest {
    private static ThreadMXBean tm = ManagementFactory.getThreadMXBean();

    private static void checkThreadContentionMonitoring(boolean expectedValue)
        throws Exception {
        boolean value = tm.isThreadContentionMonitoringEnabled();
        if (value != expectedValue) {
             throw new RuntimeException("TEST FAILED: " +
                 "isThreadContentionMonitoringEnabled() returns " + value +
                 " but expected to be " + expectedValue);
        }

    }

    private static void testThreadContentionMonitoring()
        throws Exception {
        if (!tm.isThreadContentionMonitoringSupported()) return;

        // Test setThreadContentionMonitoringEnabled()
        checkThreadContentionMonitoring(false);

        // Check enabled once
        tm.setThreadContentionMonitoringEnabled(true);
        checkThreadContentionMonitoring(true);

        // Enable it two more times
        tm.setThreadContentionMonitoringEnabled(true);
        checkThreadContentionMonitoring(true);

        tm.setThreadContentionMonitoringEnabled(true);

        // Disable once will globally disable the thread contention monitoring
        tm.setThreadContentionMonitoringEnabled(false);
        checkThreadContentionMonitoring(false);

        tm.setThreadContentionMonitoringEnabled(false);
        checkThreadContentionMonitoring(false);

        tm.setThreadContentionMonitoringEnabled(true);
        checkThreadContentionMonitoring(true);
    }

    private static void checkThreadCpuTime(boolean expectedValue)
        throws Exception {
        boolean value = tm.isThreadCpuTimeEnabled();
        if (value != expectedValue) {
             throw new RuntimeException("TEST FAILED: " +
                 "isThreadCpuTimeEnabled() returns " + value +
                 " but expected to be " + expectedValue);
        }
    }

    private static void testThreadCpuTime()
        throws Exception {
        if (!tm.isThreadCpuTimeSupported()) return;

        // Test setThreadCpuTimeEnabled()
        if (!tm.isThreadCpuTimeEnabled()) {
            tm.setThreadCpuTimeEnabled(true);
            checkThreadCpuTime(true);
        }

        tm.setThreadCpuTimeEnabled(false);
        checkThreadCpuTime(false);

        tm.setThreadCpuTimeEnabled(true);
        checkThreadCpuTime(true);

        tm.setThreadCpuTimeEnabled(true);
        checkThreadCpuTime(true);

        tm.setThreadCpuTimeEnabled(true);

        // disable globally
        tm.setThreadCpuTimeEnabled(false);
        checkThreadCpuTime(false);

        tm.setThreadCpuTimeEnabled(false);
        checkThreadCpuTime(false);

        tm.setThreadCpuTimeEnabled(true);
        checkThreadCpuTime(true);
    }

    public static void main(String args[]) throws Exception {
        try {
            testThreadContentionMonitoring();
            testThreadCpuTime();
        } finally {
            // restore the default
            if (tm.isThreadContentionMonitoringSupported()) {
                tm.setThreadContentionMonitoringEnabled(false);
            }
            if (tm.isThreadCpuTimeSupported()) {
                tm.setThreadCpuTimeEnabled(false);
            }
        }


        System.out.println("Test passed.");
    }
}
