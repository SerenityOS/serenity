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
import nsk.monitoring.ThreadMXBean.MXBeanTestThread;
import nsk.monitoring.ThreadMXBean.BarrierHandler;

/**
 * Tests getThreadAllocatedBytes(long id) function of com.sun.management.ThreadMXBean
 * <p>
 *  - Test starts thread that does not allocate any additional memory and stores
 * it's getThreadAllocatedBytes() result (value1)
 * Then it starts several other threads that does allocate memory and, after these
 * threads are finished, checks that getThreadAllocatedBytes() result (value2)
 * does not differ from value1
 */
public class NoAllocationTest extends ThreadMXBeanTestBase {
    private volatile boolean start = false;
    private volatile boolean stop = false;

    /**
     * Actually runs the test
     */
    public void run() {
        if (threadMXBean == null)
            return;
        // No allocation TestThread thread.
        // Run behavior : does nothing,  waits for notify() call
        MXBeanTestThread tr = new MXBeanTestThread() {
                @Override
                public void doWork() {
                    start = true;
                    while (!stop) { /* empty */ }
                }
            };
        tr.start();
        MXBeanTestThread tr1 = new MXBeanTestThread();
        MXBeanTestThread tr2 = new MXBeanTestThread();
        BarrierHandler handler = startThreads(tr1, tr2);
        while (!start) { /* empty */ }
        long value1 = threadMXBean.getThreadAllocatedBytes(tr.getId());
        handler.proceed();
        long value2 = threadMXBean.getThreadAllocatedBytes(tr.getId());
        if (value1 != value2) {
            throw new TestFailure("Failure! It is expected that idle thread "
                                  + "does not allocate any memory. getThreadAllocatedBytes() call "
                                  + "for idle TestThread-" + tr.getName() + " returns different "
                                  + "values. Recieved : " + value1 + " and " + value2);
        }
        log.info("NoAllocationTest passed.");
        stop = true;
        handler.finish();
    }

    /**
     * Entry point for java program
     * @param args sets the test configuration
     */
    public static void main(String[] args) {
        ThreadMXBeanTestBase test = new NoAllocationTest();
        Monitoring.runTest(test, test.setGarbageProducer(args));
    }
}
