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
import nsk.monitoring.ThreadMXBean.*;
import nsk.share.test.Stresser;

/**
 * Tests getThreadAllocatedBytes(long[] id) function of com.sun.management.ThreadMXBean
 * <p>
 * This test starts several threads (according test machine processor count). Each
 * thread allocates different size objects (from 8 bytes to 2 megabytes) during
 * defined Stresser time (default is 1 minute). After that for each thread allocated
 * memory is counted and compared to the result of getThreadAllocatedBytes() call.
 */
public class StressTest extends ThreadMXBeanTestBase {

    /**
     * Stress allocation TestThread thread.
     * Run behavior : Allocates memory for specified amount of time
     * (default - 1 minute) and waits for notify() call
     */
    private class StressTestThread extends MXBeanTestThread {
        private StressTestThread(Stresser stresser) {
            super(stresser);
        }

        @Override
        public void doWork() {
            allocateStress();
            handler.ready();
        }
    }

    /**
     * Actually runs the test
     */
    public void run() {
        if (threadMXBean == null)
            return;
        int threadCount = Runtime.getRuntime().availableProcessors();
        long memMax = Runtime.getRuntime().maxMemory();
        long threadMemMax = Math.round((((double) memMax) * 0.6) / (double) threadCount);
        MXBeanTestThread[] threadArr = new MXBeanTestThread[threadCount];
        long[] idArr = new long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threadArr[i] = new StressTestThread(stresser);
            threadArr[i].setMaxThreadMemory(threadMemMax);
            idArr[i] = threadArr[i].getId();
        }
        stresser.start(0);
        BarrierHandler handler = startThreads(threadArr);
        try {
            long[] actual = threadMXBean.getThreadAllocatedBytes(idArr);
            long[] expected = this.getStressAllocatedBytes(threadArr);
            for (int i = 0; i < threadCount; i++) {
                if (expected[i] > MIN_STRESS_ALLOCATION_AMOUNT
                        && Math.abs(actual[i] - expected[i]) >  expected[i]*DELTA_PERCENT/100)
                {
                    throw new TestFailure("Failure! Expected that Thread-"
                        + threadArr[i].getName() + " allocate " + expected[i]
                        + " bytes. getThreadAllocatedBytes() call returns "
                        + actual[i]);
                }
            }
            log.info("StressTest passed.");
        } finally {
            handler.finish();
        }
    }

    /**
     * Entry point for java program
     * @param args sets the test configuration
     */
    public static void main(String[] args) {
        ThreadMXBeanTestBase test = new StressTest();
        test.setStresser(args);
        Monitoring.runTest(test, test.setGarbageProducer(args));
    }
}
