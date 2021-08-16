/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.stress.thread;

import java.lang.management.*;
import java.io.*;

import nsk.share.*;
import nsk.monitoring.share.*;

public class cmon001 {
    final static long CONST_BARRIER_TIME = 200;
    final static long ITERATIONS = 50;

    // Precision of value returned by ThreadInfo.getWaitedTime().
    // System.nanoTime() and ThreadInfo.getWaitedTime() may use
    // different methods to sample time, so PRECISION is essential to
    // compare those two times.
    final static long PRECISION = 3; // Milliseconds

    // Ratio between nano and milli
    final static long NANO_MILLI = 1000000;

    private static volatile boolean testFailed = false;
    private static Integer calculated;
    private static String calculatedSync = "abc";
    private static Object common = new Object();
    private static Object[] finishBarriers;
    private static long[] startTime;
    private static long[] endTime;
    private static long[] waitedTime;

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);
        ThreadMonitor monitor = Monitor.getThreadMonitor(log, argHandler);

        // The test passes, if thread contention monitoring is not supported
        if (!monitor.isThreadContentionMonitoringSupported()) {
            log.display("Thread contention monitoring is not supported.");
            log.display("TEST PASSED.");
            return Consts.TEST_PASSED;
        }

        // Enable thread contention monitoring, if it is supported
        monitor.setThreadContentionMonitoringEnabled(true);

        int threadCount = argHandler.getThreadCount();
        MyThread threads[] = new MyThread[threadCount];
        finishBarriers = new Object[threadCount];
        startTime = new long[threadCount];
        endTime = new long[threadCount];
        waitedTime = new long[threadCount];

        for (int i = 0; i < threadCount; i++)
            finishBarriers[i] = new Object();

        // Begin a loop which will start a number of threads
        for (int time = 0; time < ITERATIONS; time++) {
            log.display("Iteration: " + time);

            calculated = Integer.valueOf(0);

            // Start all threads. Half of them are user threads,
            // others - daemon.
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new MyThread(i, time, log, monitor);
                threads[i].setDaemon(i % 2 == 0);
                threads[i].start();
            }

            // Wait for all threads to access "calculated" variable
            while (calculated.intValue() < threadCount)
                Thread.currentThread().yield();
            log.display("All threads have finished calculation: " + calculated);

            // Notify all threads to finish
            for (int i = 0; i < threadCount; i++)
                synchronized (finishBarriers[i]) {
                    finishBarriers[i].notify();
                }

            // Wait for all threads to die
            for (int i = 0; i < threadCount; i++)
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            log.display("All threads have died.");

            // Perform checks

            // All threads must increase "calculated" value by one, so
            // "calculated" must be equal to number of started threads.
            if (calculated.intValue() != threadCount) {
                log.complain("Number of threads that accessed the variable: "
                           + calculated.intValue() + ", expected: "
                           + threadCount);
                testFailed = true;
            }

            // Waited time of each thread must not be greater than overall
            // time of execution of the thread.
            // Precision must be taken into account in this case.
            for (int i = 0; i < threadCount; i++) {
                long liveNano = endTime[i] - startTime[i];
                long liveMilli = liveNano / NANO_MILLI;
                long leastWaitedTime = 2 * CONST_BARRIER_TIME + time;

                if (leastWaitedTime - 2 * PRECISION > waitedTime[i]) {
                    // that is not a bug. see 5070997 for details
                    log.display("Thread " + i + " was waiting for a monitor "
                               + "for at least " + leastWaitedTime
                               + " milliseconds, but "
                               + "ThreadInfo.getWaitedTime() returned value "
                               + waitedTime[i]);
                }

                if (liveMilli + PRECISION < waitedTime[i]) {
                    log.complain("Life time of thread " + i + " is " + liveMilli
                               + " milliseconds, but "
                               + "ThreadInfo.getWaitedTime() returned value "
                               + waitedTime[i]);
                    testFailed = true;
                }
            }
        } // for time

        if (testFailed)
            log.complain("TEST FAILED.");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    } // run()

    private static class MyThread extends Thread {
        int num;
        int time;
        Log log;
        ThreadMonitor monitor;
        Object constBarrier = new Object();
        Object varBarrier = new Object();

        MyThread(int num, int time, Log log, ThreadMonitor monitor) {
            this.num = num;
            this.time = time;
            this.log = log;
            this.monitor = monitor;
        }

        public void run() {
            startTime[num] = System.nanoTime();

            // constBarrier does not receive notification, so the thread will
            // be waiting for CONST_BARRIER_TIME milliseconds
            synchronized (constBarrier) {
                try {
                    constBarrier.wait(CONST_BARRIER_TIME);
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            }

            // varBarrier does not receive notification, so the thread will
            // be waiting for (CONST_BARRIER_TIME + time) milliseconds. This
            // time is different for each iteration.
            synchronized (varBarrier) {
                try {
                    varBarrier.wait(CONST_BARRIER_TIME + time);
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            }

            // Increase "calculated" value by one
            synchronized (common) {
                synchronized (calculatedSync) {
                    calculated = Integer.valueOf(calculated.intValue() + 1);
                }
            }

            synchronized (finishBarriers[num]) {
                try {
                    finishBarriers[num].wait(10 * CONST_BARRIER_TIME);
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            }

            // Save all time stats for the thread
            ThreadInfo info = monitor.getThreadInfo(this.getId(), 0);
            waitedTime[num] = info.getWaitedTime();
            endTime[num] = System.nanoTime();
        }
    } // class MyThread
}
