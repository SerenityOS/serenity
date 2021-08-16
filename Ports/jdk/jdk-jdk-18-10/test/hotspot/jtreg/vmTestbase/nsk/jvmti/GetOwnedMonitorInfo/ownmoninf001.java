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

package nsk.jvmti.GetOwnedMonitorInfo;

import nsk.share.Wicket;
import java.io.PrintStream;

public class ownmoninf001 {

    final static int JCK_STATUS_BASE = 95;

    native static void checkMon0(int point, Thread thr);
    native static void checkMon1(int point, Thread thr, Object o1);
    native static void checkMon2(int point, Thread thr, Object o1, Object o2);
    native static int getRes();

    public static Wicket waitingBarrier;
    public static Wicket startingBarrier;
    static Object lockFld = new Object();

    static boolean DEBUG_MODE = false;
    static PrintStream out;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        out = ref;
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("-v")) // verbose mode
                DEBUG_MODE = true;
        }

        Object lock = new Object();
        Thread currThr = Thread.currentThread();

        if (DEBUG_MODE)
            out.println("\nCheck #1: verifying owned monitors of current thread \""
                + currThr.getName() + "\" ...");
        checkMon0(1, currThr);
        if (DEBUG_MODE)
            out.println("Check #1 done");

        synchronized (lock) {
            synchronized (lock) {
                if (DEBUG_MODE)
                    out.println("\nCheck #2: verifying owned monitors of current thread \""
                        + currThr.getName() + "\" ...");
                checkMon1(2, currThr, lock);
                if (DEBUG_MODE)
                    out.println("Check #2 done");
            }
        }

        ownmoninf001a thr = new ownmoninf001a();
        startingBarrier = new Wicket();
        waitingBarrier = new Wicket();
        thr.setDaemon(true);
        thr.start();
        if (DEBUG_MODE)
            out.println("Waiting for notification from auxiliary thread ...");
        startingBarrier.waitFor();
        if (DEBUG_MODE) {
            out.println("Got the notification from auxiliary thread\n");
            out.println("Check #3: verifying owned monitors of auxiliary thread ...");
        }
        synchronized (thr.lock) {
            while (!thr.isWaiting) {
                try {
                    thr.lock.wait(100);
                } catch (InterruptedException ex) {
                    // skip it
                }
            }
            // Now we know it is waiting
            checkMon0(3, thr);
            if (DEBUG_MODE)
                out.println("Check #3 done\nNotifying auxiliary thread ...");
            thr.lock.notify();
            thr.wasNotified = true;
        }

        if (DEBUG_MODE)
            out.println("Waiting for notification from auxiliary thread ...");
        waitingBarrier.waitFor();

        // Check #4 is commented out because the check result is non-deterministic.
        // checkMon2(4, thr, thr, lockFld);

        thr.letItGo();

        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return getRes();
    }
}

class ownmoninf001a extends Thread {
    public boolean isWaiting = false;
    public boolean wasNotified = false;

    public Object lock = new Object();
    private volatile boolean flag = true;

    public void letItGo() {
        flag = false;
    }

    private synchronized void meth() {
        synchronized (ownmoninf001.lockFld) {
            ownmoninf001.waitingBarrier.unlock();

            if (ownmoninf001.DEBUG_MODE)
                ownmoninf001.out.println("ownmoninf001a.meth: going to loop until main thread changes <flag> ...");
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
            if (ownmoninf001.DEBUG_MODE)
                ownmoninf001.out.println("ownmoninf001a.meth: check #5: verifying own monitors of current auxiliary thread ...");
            ownmoninf001.checkMon2(5, currentThread(), this, ownmoninf001.lockFld);
            if (ownmoninf001.DEBUG_MODE)
                ownmoninf001.out.println("ownmoninf001a.meth: check #5 done");
        }
    }

    public void run() {
        synchronized (lock) {
            if (ownmoninf001.DEBUG_MODE)
                ownmoninf001.out.println("ownmoninf001a: notifying main thread ...");
            ownmoninf001.startingBarrier.unlock();
            if (ownmoninf001.DEBUG_MODE)
                ownmoninf001.out.println("ownmoninf001a: check #6: verifying own monitors of current auxiliary thread ...");
            ownmoninf001.checkMon1(6, currentThread(), lock);
            if (ownmoninf001.DEBUG_MODE)
                ownmoninf001.out.println("ownmoninf001a: check #6 done");
            try {
                if (ownmoninf001.DEBUG_MODE)
                    ownmoninf001.out.println("ownmoninf001a: releasing <lock> and waiting for notification ...");
                isWaiting = true;
                while (!wasNotified) { // handling spurious wakeups
                    lock.wait();
                }
                wasNotified = false;
                isWaiting = false;
                if (ownmoninf001.DEBUG_MODE)
                    ownmoninf001.out.println("ownmoninf001a: got the notification");
            } catch (InterruptedException e) {
                throw new Error("ownmoninf001a: unexpected " + e);
            }
        }

        meth();

        if (ownmoninf001.DEBUG_MODE)
            ownmoninf001.out.println("ownmoninf001a: thread exiting");
    }
}
