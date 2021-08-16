/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.ThreadMXBean.findMonitorDeadlockedThreads;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class find006 {
    private final static int DEF_TIME_MAX = 30;  // default max # secs to test
    private final static String PROG_NAME = "find006";

    private static Wicket mainEntrance;
    private static boolean testFailed = false;
    private static Object waiter = new Object();

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);

        String[] args = argHandler.getArguments();
        int timeMax = 0;
        if (args.length == 0) {
            timeMax = DEF_TIME_MAX;
        } else {
            try {
                timeMax = Integer.parseUnsignedInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.err.println("'" + args[0] + "': invalid timeMax value.");
                usage();
            }
        }

        Log log = new Log(out, argHandler);
        ThreadMonitor monitor = Monitor.getThreadMonitor(log, argHandler);
        long id = Thread.currentThread().getId();
        long[] ids = monitor.findMonitorDeadlockedThreads();

        if (ids == null) {
            log.display("findCircularBlockedThread() returned null");
        } else if (ids.length == 0) {
            log.display("findCircularBlockedThread() returned array of length "
                      + "0");
        } else {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == id) {
                    log.complain("TEST FAILED");
                    log.complain("findCircularBlockedThread() returned current "
                               + "thread (id = " + id + ")");
                    testFailed = true;
                    break;
                }
            }
        }

        System.out.println("About to execute for " + timeMax + " seconds.");

        long count = 0;
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        long start_time = System.currentTimeMillis();
        while (System.currentTimeMillis() < start_time + (timeMax * 1000)) {
            count++;

            MyThread thread = new MyThread(out);
            mainEntrance = new Wicket();
            thread.start();

            // Wait for MyThread to start
            mainEntrance.waitFor();
            id = thread.getId();

            synchronized (waiter) {
                thread.die = true;
                waiter.notifyAll();
            }

            while (true) {
                ids = monitor.findMonitorDeadlockedThreads();
                ThreadInfo info = mbean.getThreadInfo(id, Integer.MAX_VALUE);
                if (info == null) {
                    // the thread has exited
                    break;
                }
            }

            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new Error("Unexpected: " + e);
            }
        }

        System.out.println("Executed " + count + " loops in " + timeMax +
                           " seconds.");

        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    public static void usage() {
        System.err.println("Usage: " + PROG_NAME + " [time_max]");
        System.err.println("where:");
        System.err.println("    time_max  max looping time in seconds");
        System.err.println("              (default is " + DEF_TIME_MAX +
                           " seconds)");
        System.exit(1);
    }

    private static class MyThread extends Thread {
        final static long WAIT_TIME = 10; // Milliseconds
        volatile boolean die = false;
        PrintStream out;

        MyThread(PrintStream out) {
            this.out = out;
        }

        public void run() {

            // Notify "main" thread that MyThread has started
            mainEntrance.unlock();

            while (!die) {
                synchronized(waiter) {
                    try {
                        waiter.wait(WAIT_TIME);
                    } catch (InterruptedException e) {
                        out.println("Unexpected exception.");
                        e.printStackTrace(out);
                        testFailed = true;
                    }
                } // synchronized
            }
        } // run()
    } // MyThread
}
