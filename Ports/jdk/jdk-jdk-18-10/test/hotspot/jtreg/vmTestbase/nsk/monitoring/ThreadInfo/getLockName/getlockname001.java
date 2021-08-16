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

package nsk.monitoring.ThreadInfo.getLockName;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;

public class getlockname001 {
    private static Wicket mainEntrance = new Wicket();
    private static Object backDoor = new Object();
    private static String lock
        = backDoor.getClass().getName() + "@"
        + Integer.toHexString(System.identityHashCode(backDoor));
    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        ThreadInfo info;
        String name;

        // Test MyThread that is going to be locked on "backDoor" object
        MyThread myThread = new MyThread(out);
        myThread.start();
        long id = myThread.getId();

        // Wait for MyThread to start
        mainEntrance.waitFor();

        synchronized(backDoor) {

            // MyThread is about to be locked on "backDoor" right now
            info = mbean.getThreadInfo(id, Integer.MAX_VALUE);
            name = info.getLockName();
            backDoor.notify();

            if (!lock.equals(name)) {
                out.println("Failure 1.");
                out.println("ThreadInfo.getLockName() returned string \"" + name
                          + "\" for a locked thread, but \"" + lock
                          + "\" expected.");
                testFailed = true;
            }
        }

        // Test "main" that is not locked on any object
        id = Thread.currentThread().getId();
        info = mbean.getThreadInfo(id, Integer.MAX_VALUE);
        name = info.getLockName();

        if (name != null) {
            out.println("Failure 2.");
            out.println("ThreadInfo.getLockName() returned not-null \"" + name
                      + "\" for a running thread.");
            testFailed = true;
        }

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    private static class MyThread extends Thread {
        PrintStream out;

        MyThread(PrintStream out) {
            this.out = out;
        }

        public void run() {
            synchronized(backDoor) {

                // Notify "main" thread that "backDoor" is waiting for a signal
                mainEntrance.unlock();

                // The thread is locked on "backDoor" object
                try {
                    backDoor.wait();
                } catch (InterruptedException e) {
                    out.println("Unexpected exception.");
                    e.printStackTrace(out);
                    testFailed = true;
                }
            }
        } // run()
    } // MyThread
}
