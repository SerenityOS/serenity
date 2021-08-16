/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.ThreadMXBean.GetThreadAllocatedBytes;

import nsk.share.*;
import nsk.monitoring.share.*;
import nsk.monitoring.ThreadMXBean.ThreadMXBeanTestBase;


/**
 * Tests getThreadAllocatedBytes(long id), getThreadAllocatedBytes(long[] ids)
 * and setThreadAllocatedMemoryEnabled(boolean enabled) functions of
 * com.sun.management.ThreadMXBean
 * <p>
 *  - any method called with null argument should throw NullPointerException
 * for direct and proxy MBean delivery methods and RuntimeException for
 * server MBean delivery method (MBeanServer)
 *  <br>
 *  - any method called with zero (0 for long, { 0 } for long[]) argument
 * should throw IllegalArgumentException for direct and proxy MBean delivery
 * methods and RuntimeException for server MBean delivery method (MBeanServer)
 */
public class IllegalArgumentsTest extends ThreadMXBeanTestBase {

    /**
     * Actually runs the test
     */
    public void run() {
        if (threadMXBean == null)
            return;
        int exceptions = 0;
        // getThreadAllocatedBytes with null argument
        try {
            threadMXBean.getThreadAllocatedBytes(null);
        } catch (NullPointerException e) {
            log.info("Caught expected NPE : " + e.getMessage());
            exceptions++;
        } catch (RuntimeException e1) {
            log.info("Caught expected RuntimeException : "
                    + e1.getMessage());
            exceptions++;
        }
        // getThreadAllocatedBytes(long) with 0
        try {
            threadMXBean.getThreadAllocatedBytes(0);
        } catch (IllegalArgumentException e) {
            log.info("Caught expected IllegalArgumentException : " + e.getMessage());
            exceptions++;
        } catch (RuntimeException e1) {
            log.info("Caught expected RuntimeException : " + e1.getMessage());
            exceptions++;
        }
        // getThreadAllocatedBytes(long[]) with { 0 }
        try {
            threadMXBean.getThreadAllocatedBytes(new long[] { 0 });
        } catch (IllegalArgumentException e) {
            log.info("Caught expected IllegalArgumentException : " + e.getMessage());
            exceptions++;
        } catch (RuntimeException e1) {
            log.info("Caught expected RuntimeException : " + e1.getMessage());
            exceptions++;
        }
        // setThreadAllocatedMemoryEnabled(boolean) with null
        Boolean b = null;
        try {
            threadMXBean.setThreadAllocatedMemoryEnabled(b);
        } catch (NullPointerException e) {
            log.info("Caught expected NPE : " + e.getMessage());
            exceptions++;
        }
        // 4 exceptions should have been caught
        if (exceptions != 4)
            throw new TestFailure("Failure! Expected to catch 4 exceptions, "
                    + "actually caught : " + exceptions);
        log.info("IllegalArgumentsTest passed.");
    }

    /**
     * Entry point for java program
     * @param args sets the test configuration
     */
    public static void main(String[] args) {
        Monitoring.runTest(new IllegalArgumentsTest(), args);
    }
}
