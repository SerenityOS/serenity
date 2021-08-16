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
 * @bug     4956978
 * @summary The capability is disabled regardless of number of times
 *          it was enabled.
 * @author  Mandy Chung
 */

import java.lang.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

public class DisableTest {
    private static ThreadMXBean tm = ManagementFactory.getThreadMXBean();

    public static void main(String args[]) throws Exception {
        try {
            testThreadContentionMonitoring();
            testThreadCpuMonitoring();
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

    private static void testThreadContentionMonitoring()
        throws Exception {
        if (!tm.isThreadContentionMonitoringSupported()) {
            System.out.println("JVM does not supports thread contention monitoring");
            return;
        }

        // Default is false.
        tm.setThreadContentionMonitoringEnabled(false);
        tm.setThreadContentionMonitoringEnabled(false);

        // check if disabled
        if (tm.isThreadContentionMonitoringEnabled()) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected thread contention monitoring to be disabled");
        }

        tm.setThreadContentionMonitoringEnabled(true);
        // check if enabled
        if (!tm.isThreadContentionMonitoringEnabled()) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected thread contention monitoring to be enabled");
        }
    }

    private static void testThreadCpuMonitoring()
        throws Exception {
        if (!tm.isThreadCpuTimeSupported()) {
            System.out.println("JVM does not support thread CPU time monitoring");
            return;
        }

        if (tm.isThreadCpuTimeEnabled()) {
            tm.setThreadCpuTimeEnabled(false);
        }

        // check if disabled
        if (tm.isThreadCpuTimeEnabled()) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected thread CPU time monitoring to be disabled");
        }

        tm.setThreadCpuTimeEnabled(false);
        tm.setThreadCpuTimeEnabled(false);

        if (tm.isThreadCpuTimeEnabled()) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected thread CPU time monitoring to be disabled");
        }

        tm.setThreadCpuTimeEnabled(true);
        if (!tm.isThreadCpuTimeEnabled()) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected thread CPU time monitoring to be disabled");
        }
    }

}
