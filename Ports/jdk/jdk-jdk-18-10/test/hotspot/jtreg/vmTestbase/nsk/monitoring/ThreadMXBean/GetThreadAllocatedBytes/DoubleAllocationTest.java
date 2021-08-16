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
 *  - Test starts thread and allocates some amount of memory, then calculates this
 * allocation using getThreadAllocatedBytes() call (allocation1). Then this thread
 * allocates just the same amount of memory and calculates getThreadAllocatedBytes()
 * again (allocation2). It is assumed that allocation2/allocation1 ~ 2.
 */
public class DoubleAllocationTest extends ThreadMXBeanTestBase {

    /**
     * Actually runs the test
     */
    public void run() {
        if (threadMXBean == null)
            return;
        //TestThread tr = new DoubleAllocationTestThread();

        // Double allocation TestThread thread.
        // Run behavior : Allocates memory, waits for notify() call,
        // allocates memory again and waits for notify() call again
        MXBeanTestThread.warmUp(garbageProducerId);
        MXBeanTestThread tr = new MXBeanTestThread(garbageProducerId) {
            @Override
            public void doWork() {
                //threadStartBytes = threadMXBean.getThreadAllocatedBytes(Thread.currentThread().getId());
                //allocate();
                handler.ready();
                allocate();
                handler.ready();
                allocate();
                handler.ready();
            }
        };
        BarrierHandler handler = startThreads(tr);
        try {
            long startBytes = threadMXBean.getThreadAllocatedBytes(tr.getId());
            handler.proceed();
            long value1 = threadMXBean.getThreadAllocatedBytes(tr.getId())
                    - startBytes;
            handler.proceed();
            long value2 = threadMXBean.getThreadAllocatedBytes(tr.getId())
                    - startBytes;
            // Expect value1 and value2 differs not more then for 15%
            if (Math.abs(((double) value2 / (double) value1) - 2) > (double)2*DELTA_PERCENT/100)
            //if ( Math.abs(2*value1 - value2) > value1*DELTA_PERCENT/100)
                throw new TestFailure("Failure! Expected getThreadAllocatedBytes() "
                        + "measurement for some thread at one moment could not be "
                        + "greater then similar measurement for the same thread "
                        + "at later moment. Thread allocates same amount of memory "
                        + "before each measurement. Excpected ~2 times difference. "
                        + "Recieved: " + value1 + " and " + value2);
            log.info("DoubleAllocationTest passed.");
        } finally {
            handler.finish();
        }
    }

    /**
     * Entry point for java program
     * @param args sets the test configuration
     */
    public static void main(String[] args) {
        ThreadMXBeanTestBase test = new DoubleAllocationTest();
        Monitoring.runTest(test, test.setGarbageProducer(args));
    }
}
