/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013;

import nsk.share.Consts;
import nsk.share.Log;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE
class TestThread implements Runnable {
    // locks acquired through synchronized block
    public static Object javaLock1 = new Object();

    public static Object javaLock2 = new Object();

    public static Object javaLock3 = new Object();

    public static Object javaLock4 = new Object();

    public static Object javaLock5 = new Object();

    private forceEarlyReturn013a testObject;

    public TestThread(forceEarlyReturn013a testObject) {
        this.testObject = testObject;
    }

    public synchronized void run() {
        synchronized (javaLock1) {
            synchronized (javaLock2) {
                synchronized (javaLock3) { // BREAKPOINT_LINE
                    synchronized (javaLock4) {
                        synchronized (javaLock5) {
                            testObject.log().display(Thread.currentThread().getName() + " in run");
                        }
                    }

                }

            }
        }
    }

    static public final int BREAKPOINT_LINE = 51;

    static public final String BREAKPOINT_METHOD_NAME = "run";

}

public class forceEarlyReturn013a extends AbstractJDIDebuggee {
    static {
        try {
            // load thread class to let debugger get ReferenceType for TestThread class
            Class.forName(TestThread.class.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException when load test thread class: " + e);
            e.printStackTrace(System.out);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    public static void main(String args[]) {
        new forceEarlyReturn013a().doTest(args);
    }

    static public final String testThreadName = "forceEarlyReturn0013aTestThread";

    static public final String COMMAND_RUN_TEST_THREAD = "runTestThread";

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_RUN_TEST_THREAD)) {
            TestThread testThread = new TestThread(this);

            Thread thread1 = new Thread(testThread);
            thread1.setName(testThreadName);
            thread1.start();

            try {
                thread1.join(argHandler.getWaitTime() * 60000);

                if (thread1.getState() != Thread.State.TERMINATED) {
                    setSuccess(false);
                    log.complain("Test thread didn't complete execution for " + argHandler.getWaitTime() * 60000
                            + ", possible, forceEarlyReturn works incorrect");
                }
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
            }

            // start new thread, if thread1 released locks this thread can acquire the same locks and finish
            Thread thread2 = new Thread(testThread);
            thread2.setName("LocksCheckingThread");
            thread2.start();

            try {
                thread2.join(argHandler.getWaitTime() * 60000);

                if (thread2.getState() != Thread.State.TERMINATED) {
                    setSuccess(false);
                    log.complain("Locks checking thread didn't complete execution for " + argHandler.getWaitTime() * 60000
                            + ", possible, test thread didn't release locks");
                }
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
            }

            return true;
        }

        return false;
    }

    // access to log for TestThread
    Log log() {
        return log;
    }
}
