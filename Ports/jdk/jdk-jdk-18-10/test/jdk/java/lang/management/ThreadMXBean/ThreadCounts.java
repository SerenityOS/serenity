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
 * @bug     4530538
 * @summary Basic unit test of mbean.getThreadCount()
 *                             mbean.getTotalStartedThreadCount()
 *                             mbean.getPeakThreadCount()
 *                             mbean.getDaemonThreadCount()
 * @author  Alexei Guibadoulline
 *
 * @run main ThreadCounts
 */

import java.lang.management.*;

public class ThreadCounts {
    static final int DAEMON_THREADS = 21;
    static final int USER_THREADS_1 = 11;
    static final int USER_THREADS_2 = 9;
    static final int USER_THREADS = USER_THREADS_1 + USER_THREADS_2;
    static final int ALL_THREADS = DAEMON_THREADS + USER_THREADS;
    private static volatile boolean live[] = new boolean[ALL_THREADS];
    private ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
    private static boolean testFailed = false;

    // barrier for threads communication
    private static Barrier barrier = new Barrier(DAEMON_THREADS);

    public static void main(String argv[]) {
        ThreadCounts test = new ThreadCounts();
        Thread allThreads[] = new Thread[ALL_THREADS];

        // Start DAEMON_THREADS threads and wait to be sure they all are alive
        barrier.set(DAEMON_THREADS);
        for (int i = 0; i < DAEMON_THREADS; i++) {
            live[i] = true;
            allThreads[i] = new MyThread(i);
            allThreads[i].setDaemon(true);
            allThreads[i].start();
        }
        // wait until all threads have started.
        barrier.await();


        System.out.println("Number of daemon threads added = " +
                           DAEMON_THREADS);
        // Check mbean now
        if ( (!test.checkCount  (DAEMON_THREADS)) ||
             (!test.checkCreated(DAEMON_THREADS)) ||
             (!test.checkPeak   (DAEMON_THREADS)) ||
             (!test.checkDaemon (DAEMON_THREADS))
           )
            testFailed = true;

        // Start USER_THREADS_1 threads and wait to be sure they all are alive
        barrier.set(USER_THREADS_1);
        for (int i = DAEMON_THREADS; i < DAEMON_THREADS + USER_THREADS_1; i++) {
            live[i] = true;
            allThreads[i] = new MyThread(i);
            allThreads[i].setDaemon(false);
            allThreads[i].start();
        }
        // wait until user1 threads have started.
        barrier.await();

        System.out.println("Number of threads added = " +
                           USER_THREADS_1);
        // Check mbean now
        if ( (!test.checkCount  (DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkCreated(DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkPeak   (DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkDaemon (DAEMON_THREADS))
           )
            testFailed = true;

        // Stop daemon threads and wait to be sure they all are dead
        barrier.set(DAEMON_THREADS);
        for (int i = 0; i < DAEMON_THREADS; i++) {
            live[i] = false;
        }
        // wait until daemon threads terminated.
        barrier.await();

        System.out.println("Daemon threads terminated.");
        // Check mbean now
        if ( (!test.checkCount  (USER_THREADS_1))                  ||
             (!test.checkCreated(DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkPeak   (DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkDaemon (0))
           )
            testFailed = true;

        // Start USER_THREADS_2 threads and wait to be sure they all are alive
        barrier.set(USER_THREADS_2);
        for (int i = DAEMON_THREADS + USER_THREADS_1; i < ALL_THREADS; i++) {
            live[i] = true;
            allThreads[i] = new MyThread(i);
            allThreads[i].setDaemon(false);
            allThreads[i].start();
        }
        // wait until user2 threads have started.
        barrier.await();

        System.out.println("Number of threads added = " +
                           USER_THREADS_2);
        // Check mbean now
        if ( (!test.checkCount  (USER_THREADS_1 + USER_THREADS_2)) ||
             (!test.checkCreated(ALL_THREADS))                     ||
             (!test.checkPeak   (DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkDaemon (0))
           )
            testFailed = true;

        // Stop user1 threads and wait to be sure they all are dead
        barrier.set(USER_THREADS_1);
        for (int i = DAEMON_THREADS; i < DAEMON_THREADS + USER_THREADS_1; i++) {
            live[i] = false;
        }
        // wait until user1 threads terminated.
        barrier.await();

        System.out.println("Number of threads terminated = " +
                           USER_THREADS_1);
        // Check mbean now
        if ( (!test.checkCount  (USER_THREADS_2))                  ||
             (!test.checkCreated(ALL_THREADS))                     ||
             (!test.checkPeak   (DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkDaemon (0))
           )
            testFailed = true;

        // Stop user2 threads and wait to be sure they all are dead
        barrier.set(USER_THREADS_2);
        for (int i = DAEMON_THREADS + USER_THREADS_1; i < ALL_THREADS; i++) {
            live[i] = false;
        }
        // wait until user2 threads terminated.
        barrier.await();

        System.out.println("Number of threads terminated = " +
                           USER_THREADS_2);
        // Check mbean now
        if ( (!test.checkCount  (0))                               ||
             (!test.checkCreated(ALL_THREADS))                     ||
             (!test.checkPeak   (DAEMON_THREADS + USER_THREADS_1)) ||
             (!test.checkDaemon (0))
           )
            testFailed = true;

        if (testFailed)
            throw new RuntimeException("TEST FAILED.");

        System.out.println("Test passed.");
    }

    // Nobody knows how many threads are in the JVM in the exact moment. The
    // only thing to check for sure is minimal number of threads alive (or
    // created) in the application
    private boolean checkCount(long min) {
        long result = mbean.getThreadCount();

        if (result < min) {
            System.err.println("TEST FAILED: " +
                               "Minimal number of live threads is " +
                                min +
                                ". ThreadMXBean.getThreadCount() returned " +
                                result);
            return false;
        }
        return true;
    }

    private boolean checkCreated(long min) {
        long result = mbean.getTotalStartedThreadCount();

        if (result < min) {
            System.err.println("TEST FAILED: " +
                               "Minimal number of created threads is " +
                                min +
                                ". ThreadMXBean.getTotalStartedThreadCount() "+
                                "returned " + result);
            return false;
        }
        return true;
    }

    private boolean checkPeak(long min) {
        long result = mbean.getPeakThreadCount();

        if (result < min) {
            System.err.println("TEST FAILED: " +
                               "Minimal peak thread count is " +
                                min +
                                ". ThreadMXBean.getPeakThreadCount() "+
                                "returned " + result);
            return false;
        }
        return true;
    }

    private boolean checkDaemon(long min) {
        long result = mbean.getDaemonThreadCount();

        if (result < min) {
            System.err.println("TEST FAILED: " +
                               "Minimal number of daemon thread count is " +
                                min +
                               "ThreadMXBean.getDaemonThreadCount() returned "
                                + result);
            return false;
        }
        return true;
    }

    // The MyThread thread lives as long as correspondent live[i] value is true
    private static class MyThread extends Thread {
        int id;

        MyThread(int id) {
            this.id = id;
        }

        public void run() {
            // signal started
            barrier.signal();
            while (live[id]) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("Unexpected exception is thrown.");
                    e.printStackTrace(System.out);
                    testFailed = true;
                }
            }
            // signal about to exit
            barrier.signal();
        }
    }
}
