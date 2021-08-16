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
 * Tests isThreadAllocatedMemorySupported(), isThreadAllocatedMemoryEnabled()
 * and setThreadAllocatedMemoryEnabled(boolean enabled) functions
 * of com.sun.management.ThreadMXBean
 * <p>
 *  - isThreadAllocatedMemorySupported() should return true
 * <br>
 *  - isThreadAllocatedMemoryEnabled() should return true by default
 * <br>
 *  - isThreadAllocatedMemoryEnabled() should return false after
 * setThreadAllocatedMemoryEnabled(false) call
 * <br>
 *  - isThreadAllocatedMemoryEnabled() should return true again after
 * setThreadAllocatedMemoryEnabled(true) call
 */
public class AllocatedMemorySupportedTest extends ThreadMXBeanTestBase {

    /**
     * Actually runs the test
     */
    public void run() {
        if (threadMXBean == null)
            return;
        // Check that isThreadAllocatedMemorySupported() returns true by default
        if (! threadMXBean.isThreadAllocatedMemorySupported()) {
            throw new TestFailure("Failure! isThreadAllocatedMemorySupported() "
                   + "does not return true by default...");
        }
        // Check that isThreadAllocatedMemoryEnabled() returns true by default
        if (! threadMXBean.isThreadAllocatedMemoryEnabled()) {
            throw new TestFailure("Failure! isThreadAllocatedMemoryEnabled() "
                    + "does not return true by default...");
        }
        // Call setThreadAllocatedMemoryEnabled(false)
        threadMXBean.setThreadAllocatedMemoryEnabled(false);
        // Check that isThreadAllocatedMemoryEnabled() now returns false
        if (threadMXBean.isThreadAllocatedMemoryEnabled()) {
            throw new TestFailure("Failure! setThreadAllocatedMemoryEnabled(false) "
                    + "does not operate as expected...");
        }
        // Call setThreadAllocatedMemoryEnabled(true)
        threadMXBean.setThreadAllocatedMemoryEnabled(true);
        // Check that isThreadAllocatedMemoryEnabled() returns true again
        if (! threadMXBean.isThreadAllocatedMemoryEnabled()) {
            throw new TestFailure("Failure! setThreadAllocatedMemoryEnabled(true) "
                    + "does not operate as expected...");
        }
        log.info("AllocatedMemorySupportedTest passed.");
    }

    /**
     * Entry point for java program
     * @param args sets the test configuration
     */
    public static void main(String[] args) {
        Monitoring.runTest(new AllocatedMemorySupportedTest(), args);
    }
}
