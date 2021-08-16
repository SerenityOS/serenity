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

package nsk.jvmti.GetThreadState;

import java.io.PrintStream;

public class thrstat001 {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_MONITOR = 1;
    public static final int STATUS_WAIT    = 2;

    native static void checkStatus(int statInd);
    native static int getRes();

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String argv[], PrintStream ref) {
        thrstat001 t = new thrstat001();
        t.meth();
        return getRes();
    }

    // barriers for testing thread status values
    public static Lock startingMonitor = new Lock();
    public static Object blockingMonitor = new Object();
    public static Lock endingMonitor = new Lock();

    void meth() {
        thrstat001a thr = new thrstat001a("thr1");

        synchronized (blockingMonitor) {
            synchronized (startingMonitor) {
                startingMonitor.val = 0;
                thr.start();
                while (startingMonitor.val == 0) {
                    try {
                        startingMonitor.wait();
                    } catch (InterruptedException e) {
                        throw new Error("Unexpected: " + e);
                    }
                }
            }
            Thread.yield();
            checkStatus(STATUS_MONITOR);
        }

        synchronized (endingMonitor) {
            checkStatus(STATUS_WAIT);
            endingMonitor.val++;
            endingMonitor.notify();
        }

        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected: " + e);
        }
   }

    static class Lock {
        public int val = 0;
    }
}

class thrstat001a extends Thread {

    public thrstat001a(String name) {
        super(name);
    }

    public void run() {
        synchronized (thrstat001.endingMonitor) {
            thrstat001.checkStatus(thrstat001.STATUS_RUNNING);
            synchronized (thrstat001.startingMonitor) {
                thrstat001.startingMonitor.val++;
                thrstat001.startingMonitor.notifyAll();
            }

            synchronized (thrstat001.blockingMonitor) {
            }

            thrstat001.endingMonitor.val = 0;
            while (thrstat001.endingMonitor.val == 0) {
                try {
                    thrstat001.endingMonitor.wait();
                } catch (InterruptedException e) {
                    throw new Error("Unexpected: " + e);
                }
            }
        }
    }
}
