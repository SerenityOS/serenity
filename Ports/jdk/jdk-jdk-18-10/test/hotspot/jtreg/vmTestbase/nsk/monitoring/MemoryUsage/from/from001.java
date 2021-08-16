/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.MemoryUsage.from;

import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.*;
import java.io.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class from001 {

    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);

        // 1. Check null CompositeData - null must be returned
        MemoryUsage result = MemoryUsage.from(null);

        if (result != null) {
            log.complain("FAILURE 1.");
            log.complain("MemoryUsage.from(null) returned " + result
                      + ", expected: null.");
            testFailed = true;
        }

        // 2. Check CompositeData that doest not represnt
        // MemoryUsage - IllegalArgumentException must be thrown
        Monitor.getThreadMonitor(log, argHandler);

        Thread thread = Thread.currentThread();
        long id = thread.getId();

        Object[] params = {Long.valueOf(id), Integer.valueOf(Integer.MAX_VALUE)};
        String[] signature = {"long", "int"};

        ObjectName mbeanObjectName = null;
        CompositeData cdata = null;
        try {
            mbeanObjectName = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
            cdata = (CompositeData) Monitor.getMBeanServer().invoke(
                                                mbeanObjectName,
                                                "getThreadInfo", params, signature);
        } catch (Exception e) {
            log.complain("Unexpected exception " + e);
            e.printStackTrace(log.getOutStream());
            testFailed = true;
        }

        try {
            result = MemoryUsage.from(cdata);
            log.complain("FAILURE 2.");
            log.complain("MemoryUsage.from(CompositeData) returned " + result
                      + ", expected: IllegalArgumentException.");
            testFailed = true;
        } catch (IllegalArgumentException e) {

            // Expected: CompositeData doest not represnt MemoryUsage
        }

        // 3. Check correct CompositeData
        CompositeData correctCD = null;
        try {
            mbeanObjectName = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
            correctCD = (CompositeData) Monitor.getMBeanServer().getAttribute(
                                                mbeanObjectName,
                                                "HeapMemoryUsage");
        } catch (Exception e) {
            log.complain("Unexpected exception " + e);
            e.printStackTrace(log.getOutStream());
            testFailed = true;
        }

        result = MemoryUsage.from(correctCD);
        long init = result.getInit();
        long used = result.getUsed();
        long comm = result.getCommitted();
        long max = result.getMax();

        if (testFailed)
            log.complain("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }
}
