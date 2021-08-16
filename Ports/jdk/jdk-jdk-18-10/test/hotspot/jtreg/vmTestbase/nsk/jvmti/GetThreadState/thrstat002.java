/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetThreadState;

import nsk.share.Wicket;
import java.io.PrintStream;

public class thrstat002 {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_MONITOR = 1;
    public static final int STATUS_WAIT    = 2;

    native static void init(int waitTime);
    native static void checkStatus(int statInd, boolean susp);
    native static int getRes();

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String args[], PrintStream out) {
        int waitTime = 2;
        if (args.length > 0) {
            try {
                int i  = Integer.parseInt(args[0]);
                waitTime = i;
            } catch (NumberFormatException ex) {
                out.println("# Wrong argument \"" + args[0]
                    + "\", the default value is used");
            }
        }
        out.println("# Waiting time = " + waitTime + " mins");
        init(waitTime);
        new thrstat002().meth();
        return getRes();
    }

    public static Wicket startingBarrier;
    public static Wicket runningBarrier;
    public static Object blockingMonitor = new Object();
    public static Lock endingMonitor = new Lock();

    static volatile boolean targetAboutToLock = false;

    void meth() {
        thrstat002a thr = new thrstat002a("thr1");
        startingBarrier = new Wicket();
        runningBarrier = new Wicket();

        synchronized (blockingMonitor) {
            thr.start();
            System.out.println("thrstat002.meth after thr.start()");

            startingBarrier.waitFor();
            System.out.println("thrstat002.meth after thr.startingBarrier.waitFor()");

            waitForThreadBlocked(thr);

            checkStatus(STATUS_MONITOR, false);
            System.out.println("thrstat002.meth after checkStatus(STATUS_MONITOR,false)");

            thr.suspend();
            System.out.println("thrstat002.meth after thr.suspend()");
            checkStatus(STATUS_MONITOR, true);
            System.out.println("thrstat002.meth after checkStatus(STATUS_MONITOR,true)");

            thr.resume();
            System.out.println("thrstat002.meth after thr.resume()");
            checkStatus(STATUS_MONITOR, false);
            System.out.println("thrstat002.meth after checkStatus(STATUS_MONITOR,false)");
        }

        runningBarrier.waitFor();
        checkStatus(STATUS_RUNNING, false);
        thr.suspend();
        checkStatus(STATUS_RUNNING, true);
        thr.resume();
        checkStatus(STATUS_RUNNING, false);
        thr.letItGo();

        synchronized (endingMonitor) {
            checkStatus(STATUS_WAIT, false);
            thr.suspend();
            checkStatus(STATUS_WAIT, true);
            thr.resume();
            checkStatus(STATUS_WAIT, false);
            endingMonitor.val++;
            endingMonitor.notifyAll();
        }

        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected: " + e);
        }
    }

    private static void waitForThreadBlocked(Thread t) {
        // Ensure that the thread is blocked on the right monitor
        while (!targetAboutToLock || t.getState() != Thread.State.BLOCKED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                System.out.println("thrstat002.waitForThreadBlocked was interrupted: " + ex.getMessage());
            }
        }
    }

    static class Lock {
        public int val = 0;
    }
}

class thrstat002a extends Thread {
    private volatile boolean flag = true;

    public thrstat002a(String name) {
        super(name);
    }

    public void run() {
        synchronized (thrstat002.endingMonitor) {
            System.out.println("thrstat002a.run before startingBarrier.unlock");
            thrstat002.startingBarrier.unlock();

            System.out.println("thrstat002a.run after  startingBarrier.unlock");

            System.out.println("thrstat002a.run before blockingMonitor lock");

            thrstat002.targetAboutToLock = true;

            synchronized (thrstat002.blockingMonitor) {
               System.out.println("thrstat002a.run blockingMonitor locked");
            }
            System.out.println("thrstat002a.run after blockingMonitor lock");

            System.out.println("thrstat002a.run before runningBarrier unlock");
            thrstat002.runningBarrier.unlock();

            // Don't do println's from this point until we have exited the loop,
            // else we can suspend in the println in an unexpected state.
            int i = 0;
            int n = 1000;
            while (flag) {
                if (n <= 0) {
                    n = 1000;
                }
                if (i > n) {
                    i = 0;
                    n--;
                }
                i++;
            }

            thrstat002.endingMonitor.val = 0;
            while (thrstat002.endingMonitor.val == 0) {
                try {
                    thrstat002.endingMonitor.wait();
                } catch (InterruptedException e) {
                    throw new Error("Unexpected: " + e);
                }
            }
            System.out.println("thrstat002a.run before endingMonitor unlock");
       }
    }

    public void letItGo() {
        flag = false;
    }
}
