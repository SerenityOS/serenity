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

package nsk.jvmti.GetStackTrace;

import java.io.PrintStream;

public class getstacktr003 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("getstacktr003");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getstacktr003 library");
            System.err.println("java.library.path:"
                    + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void chain();
    native static int check(Thread thread);

    public static Object lockIn = new Object();
    public static Object lockOut = new Object();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        int res;
        TestThread thr = new TestThread();

        synchronized (lockIn) {
            thr.start();
            try {
                lockIn.wait();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }


        synchronized (lockOut) {
            res = check(thr);
            lockOut.notify();
        }

        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return res;
    }

    static void dummy() {
        synchronized (lockOut) {
            synchronized (lockIn) {
                lockIn.notify();
            }
            try {
                lockOut.wait();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }
    }

    static class TestThread extends Thread {
        public void run() {
            chain();
        }
    }
}
