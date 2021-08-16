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

package nsk.jvmti.GetAllThreads;

import java.io.PrintStream;

public class allthr001 {

    static {
        try {
            System.loadLibrary("allthr001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load allthr001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void setSysCnt();
    native static void checkInfo(int thr_ind);
    native static int getRes();

    public static Object lock1 = new Object();
    public static Object lock2 = new Object();
    public static int waitTime = 2;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String args[], PrintStream out) {
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

        setSysCnt();
        checkInfo(0);

        ThreadGroup tg = new ThreadGroup("tg1");
        allthr001a t_a = new allthr001a(tg, "thread1");
        t_a.setDaemon(true);
        checkInfo(1);

        synchronized (lock1) {
            try {
                t_a.start();
                lock1.wait();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }
        checkInfo(2);

        synchronized (lock2) {
            lock2.notify();
        }

        try {
            t_a.join(waitTime * 60000);
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }
        checkInfo(3);

        checkInfo(4);
        return getRes();
    }
}

class allthr001a extends Thread {
    allthr001a(ThreadGroup tg, String name) {
        super(tg, name);
    }

    public void run() {
        synchronized (allthr001.lock2) {
            synchronized (allthr001.lock1) {
                allthr001.lock1.notify();
            }
            try {
                allthr001.lock2.wait();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }
    }
}
