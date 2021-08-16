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

package nsk.jvmti.GetObjectMonitorUsage;

import java.io.PrintStream;

public class objmonusage004 {

    final static int JCK_STATUS_BASE = 95;
    final static int NUMBER_OF_THREADS = 16;
    final static int WAIT_TIME = 100;

    static {
        try {
            System.loadLibrary("objmonusage004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load objmonusage004 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    static Object lockStart = new Object();
    static Object lockCheck = new Object();

    native static int getRes();
    native static void check(Object obj, Thread owner,
                             int entryCount, int waiterCount);

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        Thread currThread = Thread.currentThread();
        ContendThread thr[] = new ContendThread[NUMBER_OF_THREADS];
        synchronized (lockCheck) {
            synchronized (lockCheck) {
                check(lockCheck, currThread, 2, 0);
            }
            for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                thr[i] = new ContendThread();
                synchronized (lockStart) {
                    thr[i].start();
                    try {
                        lockStart.wait();
                        lockStart.wait(WAIT_TIME);
                    } catch (InterruptedException e) {
                        throw new Error("Unexpected " + e);
                    }
                }
            check(lockCheck, currThread, 1, i + 1);
            }
        }

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            try {
                thr[i].join();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }

        check(lockCheck, null, 0, 0);
        return getRes();
    }

    static class ContendThread extends Thread {
        public synchronized void run() {
            synchronized (lockStart) {
                lockStart.notify();
            }
            synchronized (lockCheck) {
                lockCheck.notify();
            }
        }
    }
}
