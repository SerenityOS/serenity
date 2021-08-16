/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.ThreadInfo.isSuspended;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;

public class issuspended001 {
    private static Wicket mainEntrance = new Wicket();
    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        MyThread thread = new MyThread(out);
        thread.start();

        // Wait for MyThread to start
        mainEntrance.waitFor();

        long id = thread.getId();
        ThreadInfo info = mbean.getThreadInfo(id, Integer.MAX_VALUE);
        boolean isSuspended = info.isSuspended();
        if (isSuspended) {
            out.println("Failure 1.");
            out.println("ThreadInfo.isSuspended() returned true, before "
                      + "Thread.suspend() was invoked.");
            testFailed = true;
        }

        thread.suspend();
        info = mbean.getThreadInfo(id, Integer.MAX_VALUE);
        isSuspended = info.isSuspended();
        if (!isSuspended) {
            out.println("Failure 2.");
            out.println("ThreadInfo.isSuspended() returned false, after "
                      + "Thread.suspend() was invoked.");
            testFailed = true;
        }

        thread.resume();
        info = mbean.getThreadInfo(id, Integer.MAX_VALUE);
        isSuspended = info.isSuspended();
        if (isSuspended) {
            out.println("Failure 3.");
            out.println("ThreadInfo.isSuspended() returned true, after "
                      + "Thread.resume() was invoked.");
            testFailed = true;
        }

        thread.die = true;

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    private static class MyThread extends Thread {
        final static long WAIT_TIME = 500; // Milliseconds
        Object object = new Object();
        boolean die = false;
        PrintStream out;

        MyThread(PrintStream out) {
            this.out = out;
        }

        public void run() {

            // Notify "main" thread that MyThread has started
            mainEntrance.unlock();

            while (!die) {
                synchronized(object) {
                    try {
                        object.wait(WAIT_TIME);
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
