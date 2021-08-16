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
 *  - Check that result of getThreadAllocatedBytes(long id) call for similar
 * threads are approximately equals.
 */
public class EqualThreadsTest extends ThreadMXBeanTestBase {

    /**
     * Actually runs the test
     */
    public void run() {
        if (threadMXBean == null)
            return;
        MXBeanTestThread.warmUp(garbageProducerId);
        MXBeanTestThread tr1 = new MXBeanTestThread(garbageProducerId);
        MXBeanTestThread tr2 = new MXBeanTestThread(garbageProducerId);
        BarrierHandler handler = startThreads(tr1, tr2);
        try {
            long startBytesTr1 = threadMXBean.getThreadAllocatedBytes(tr1.getId());
            long startBytesTr2 = threadMXBean.getThreadAllocatedBytes(tr2.getId());
            handler.proceed();
            long value1 = threadMXBean.getThreadAllocatedBytes(tr1.getId())
                - startBytesTr1;
            long value2 = threadMXBean.getThreadAllocatedBytes(tr2.getId())
                - startBytesTr2;
            // Expect value1 and value2 differs not more then for 10%
            if ( Math.abs(value1 - value2) > value1*DELTA_PERCENT/100)
                throw new TestFailure("Failure! Let f(thread) = getThreadAllocatedBytes()."
                    + " Expected if thread tr1 is similar to thread tr2"
                    + " then f(tr1) and f(tr2) differs not more then for "
                    + DELTA_PERCENT + " %. Recieved: f(tr1)=" + value1
                    + " f(tr2)=" + value2);
            log.info("EqualThreadsTest passed.");
        } finally {
            handler.finish();
        }
    }

    /**
     * Entry point for java program
     * @param args sets the test configuration
     */
    public static void main(String[] args) {
        ThreadMXBeanTestBase test = new EqualThreadsTest();
        Monitoring.runTest(test, test.setGarbageProducer(args));
    }
}
