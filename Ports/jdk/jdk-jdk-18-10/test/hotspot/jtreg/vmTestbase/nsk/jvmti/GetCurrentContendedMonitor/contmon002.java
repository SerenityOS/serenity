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

public class contmon002 {

    native static void checkMon(int point, Thread thr);
    native static int getRes();

    static {
        try {
            System.loadLibrary("contmon002");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load contmon002 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    public static boolean startingBarrier = true;

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
        checkMon(1, Thread.currentThread());

        contmon002a thr = new contmon002a();
        thr.start();
        while (startingBarrier) {
            doSleep();
        }
        checkMon(2, thr);
        thr.letItGo();
        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return getRes();
    }
}

class contmon002a extends Thread {
    private volatile boolean flag = true;

    private synchronized void meth() {
        contmon002.startingBarrier = false;
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
    }

    public void run() {
        meth();
    }

    public void letItGo() {
        flag = false;
    }
}
