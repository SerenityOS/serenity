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
package nsk.monitoring.ThreadMXBean.GetThreadCpuTime;

import nsk.share.*;
import nsk.monitoring.share.*;
import nsk.monitoring.ThreadMXBean.*;

/**
 * Tests getThreadCpuTime(long[] ids) and getThreadUserTime(long[] ids)
 * functions of com.sun.management.ThreadMXBean
 * <p>
 * All methods should
 * <br>
 * -  return -1 if ThreadCpuTime allocation is
 * not enabled
 * <br>
 * - return -1 for not started thread
 * <br>
 * - return > 0 value for any running thread
 * <br>
 * - return -1 for finished thread
 */
public class BaseBehaviorTest extends ThreadMXBeanTestBase {

    /**
     * Actually runs the test
     */
    public void run() {
        if (threadMXBean == null)
            return;
        MXBeanTestThread thread = new MXBeanTestThread();
        long id = thread.getId();
        long[] idArr = new long[] { id };
        long[] resultArr;
        // Expect -1 for not started threads
        resultArr = threadMXBean.getThreadCpuTime(idArr);
        if (resultArr[0] != -1)
            throw new TestFailure("Failure! getThreadCpuTime(long[] ids) should "
                    + "return -1 for not started threads. Recieved : " + resultArr[0]);
        resultArr = threadMXBean.getThreadUserTime(idArr);
        if (resultArr[0] != -1)
            throw new TestFailure("Failure! getThreadUserTime(long[] ids) should "
                    + "return -1 for not started threads. Recieved : " + resultArr[0]);
        BarrierHandler handler = startThreads(thread);
        try {
            handler.proceed();
            // Expect -1 for running thread if ThreadAllocatedMemory (CpuTime) is disabled
            threadMXBean.setThreadCpuTimeEnabled(false);
            resultArr = threadMXBean.getThreadCpuTime(idArr);
            if (resultArr[0] != -1)
                throw new TestFailure("Failure! getThreadCpuTime(long[] ids) should "
                    + "return -1 if threadCpuTimeEnabled is set to false. "
                    + "Recieved : " + resultArr[0]);
            resultArr = threadMXBean.getThreadUserTime(idArr);
            if (resultArr[0] != -1)
                throw new TestFailure("Failure! getThreadUserTime(long[] ids) should "
                    + "return -1 if threadCpuTimeEnabled is set to false. "
                    + "Recieved : " + resultArr[0]);
            threadMXBean.setThreadCpuTimeEnabled(true);
            // Expect > 0 value for running threads
            resultArr = threadMXBean.getThreadCpuTime(idArr);
            if (resultArr[0] < 0)
                throw new TestFailure("Failure! getThreadCpuTime(long[] ids) should "
                    + "return > 0 value for RUNNING thread. Recieved : " + resultArr[0]);
            resultArr = threadMXBean.getThreadUserTime(idArr);
            if (resultArr[0] < 0)
                throw new TestFailure("Failure! getThreadUserTime(long[] ids) should "
                    + "return > 0 value for RUNNING thread. Recieved : " + resultArr[0]);
        } finally {
            // Let thread finish
            handler.finish();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {}
        // Expect -1 for finished thread
        resultArr = threadMXBean.getThreadCpuTime(idArr);
        if (resultArr[0] != -1)
            throw new TestFailure("Failure! getThreadCpuTime(long[] ids) should "
                    + "return -1 for finished threads. Recieved : " + resultArr[0]);
        resultArr = threadMXBean.getThreadUserTime(idArr);
        if (resultArr[0] != -1)
            throw new TestFailure("Failure! getThreadUserTime(long[] ids) should "
                    + "return -1 for finished threads. Recieved : " + resultArr[0]);
        log.info("BaseBehaviorTest passed.");
    }

    /**
     * Entry point for java program
     * @param args sets the test configuration
     */
    public static void main(String[] args) {
        ThreadMXBeanTestBase test = new BaseBehaviorTest();
        Monitoring.runTest(test, test.setGarbageProducer(args));
    }
}
