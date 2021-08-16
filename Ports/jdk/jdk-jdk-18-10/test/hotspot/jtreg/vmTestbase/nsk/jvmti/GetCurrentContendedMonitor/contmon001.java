/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetCurrentContendedMonitor;

import java.io.PrintStream;

public class contmon001 {

    native static void checkMon(int point, Thread thr, Object mon);
    native static int getRes();

    static {
        try {
            System.loadLibrary("contmon001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load contmon001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    public static volatile boolean startingBarrier = true;
    public static volatile boolean waitingBarrier = true;
    static Object lockFld = new Object();

    static boolean DEBUG_MODE = false;
    static PrintStream out;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static void doSleep() {
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            throw new Error("Unexpected " + e);
        }
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
            out.println("\nCheck #1: verifying a contended monitor of current thread \""
                + currThr.getName() + "\" ...");
        synchronized (lock) {
            checkMon(1, currThr, null);
        }
        if (DEBUG_MODE)
            out.println("Check #1 done");

        contmon001a thr = new contmon001a();

        thr.start();
        if (DEBUG_MODE)
            out.println("\nWaiting for auxiliary thread ...");
        while (startingBarrier) {
            doSleep();
        }
        if (DEBUG_MODE)
            out.println("Auxiliary thread is ready");

        if (DEBUG_MODE)
            out.println("\nCheck #3: verifying a contended monitor of auxiliary thread ...");
        checkMon(3, thr, null);
        if (DEBUG_MODE)
            out.println("Check #3 done");

        thr.letItGo();

        while (waitingBarrier) {
            doSleep();
        }
        synchronized (lockFld) {
            if (DEBUG_MODE)
                out.println("\nMain thread entered lockFld's monitor"
                    + "\n\tand calling lockFld.notifyAll() to awake auxiliary thread");
            lockFld.notifyAll();
            if (DEBUG_MODE)
                out.println("\nCheck #4: verifying a contended monitor of auxiliary thread ...");
            checkMon(4, thr, lockFld);
            if (DEBUG_MODE)
                out.println("Check #4 done");
        }

        if (DEBUG_MODE)
            out.println("\nMain thread released lockFld's monitor"
                + "\n\tand waiting for auxiliary thread death ...");

        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }
        if (DEBUG_MODE)
            out.println("\nCheck #5: verifying a contended monitor of dead auxiliary thread ...");
        checkMon(5, thr, null);
        if (DEBUG_MODE)
            out.println("Check #5 done");

        return getRes();
    }
}


class contmon001a extends Thread {
    private volatile boolean flag = true;

    public void run() {
        if (contmon001.DEBUG_MODE)
            contmon001.out.println("check #2: verifying a contended monitor of current auxiliary thread ...");
        contmon001.checkMon(2, currentThread(), null);
        if (contmon001.DEBUG_MODE)
            contmon001.out.println("check #2 done");

        if (contmon001.DEBUG_MODE)
            contmon001.out.println("notifying main thread");
        contmon001.startingBarrier = false;

        if (contmon001.DEBUG_MODE)
            contmon001.out.println("thread is going to loop while <flag> is true ...");
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
        if (contmon001.DEBUG_MODE)
            contmon001.out.println("looping is done: <flag> is false");

        synchronized (contmon001.lockFld) {
            contmon001.waitingBarrier = false;
            if (contmon001.DEBUG_MODE)
                contmon001.out.println("\nthread entered lockFld's monitor"
                    + "\n\tand releasing it through the lockFld.wait() call");
            try {
                contmon001.lockFld.wait();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }

        if (contmon001.DEBUG_MODE)
            contmon001.out.println("thread exiting");
    }

    public void letItGo() {
        flag = false;
    }
}
