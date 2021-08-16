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

package nsk.jvmti.unit.functions.nosuspendMonitorInfo;

import java.io.PrintStream;

public class JvmtiTest {
    final static int JCK_STATUS_BASE = 95;
    final static int NUMBER_OF_THREADS = 32;
    static boolean DEBUG_MODE = false;
    static PrintStream out;
    static Thread mainThr;


    static {
        try {
            System.loadLibrary("nosuspendMonitorInfo");
        } catch (UnsatisfiedLinkError err) {
            System.err.println("Could not load nosuspendMonitorInfo library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw err;
        }
    }

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("-v")) // verbose mode
                DEBUG_MODE = true;
        }

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    static volatile boolean lock1held = false;
    static Object lock1 = new Object();
    static Object lock2 = new Object();

    native static int GetResult();
    native static void CheckMonitorInfo(Thread thr, Object obj2, int expected);

    public static int run(String argv[], PrintStream out) {

        JvmtiTesta runn = new JvmtiTesta();

        Thread thr = Thread.currentThread();
        mainThr = thr;


        synchronized(lock1) {
            runn.start();
            try {
                CheckMonitorInfo(thr,lock1, 1);
                CheckMonitorInfo(runn,lock1, 0);
                lock1.wait();
            } catch (Throwable e) {
                out.println(e);
                return 2;
            }
            CheckMonitorInfo(thr,lock1,1);
            CheckMonitorInfo(runn,lock1,0);
        }

        while (!lock1held) {
            Thread.yield();
        }

        synchronized (lock2) {
            lock1held = false;
            CheckMonitorInfo(thr,lock2,1);
            lock2.notifyAll();
            CheckMonitorInfo(thr,lock2,1);
            CheckMonitorInfo(runn,lock2,0);
        }

        while (!lock1held) {
            Thread.yield();
        }

        synchronized (lock2) {
            lock1held = false;
            CheckMonitorInfo(thr,lock2,1);
            CheckMonitorInfo(runn,lock2,1);
            CheckMonitorInfo(runn,lock1,1);
            lock2.notifyAll();
            CheckMonitorInfo(thr,lock2,1);
            CheckMonitorInfo(runn,lock2,0);
        }

        try {
            runn.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return GetResult();
    }
}

class JvmtiTesta extends Thread {
    public void run() {
        synchronized (JvmtiTest.lock1) {

            JvmtiTest.CheckMonitorInfo(currentThread(), JvmtiTest.lock1, 1);

            JvmtiTest.lock1.notify();

            JvmtiTest.CheckMonitorInfo(currentThread(), JvmtiTest.lock1, 1);

        }

        synchronized (JvmtiTest.lock2) {
            JvmtiTest.lock1held = true;
            JvmtiTest.CheckMonitorInfo(currentThread(), JvmtiTest.lock2,1);
            try {
                JvmtiTest.lock2.wait();
                JvmtiTest.CheckMonitorInfo(currentThread(), JvmtiTest.lock2,0);
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }

        synchronized (JvmtiTest.lock1) {
            synchronized (JvmtiTest.lock2) {
                JvmtiTest.lock1held = true;
                try {
                    JvmtiTest.lock2.wait();
                    JvmtiTest.CheckMonitorInfo(currentThread(), JvmtiTest.lock2,0);
                } catch (InterruptedException e) {
                    throw new Error("Unexpected " + e);
                }
            }
        }


    }
}
