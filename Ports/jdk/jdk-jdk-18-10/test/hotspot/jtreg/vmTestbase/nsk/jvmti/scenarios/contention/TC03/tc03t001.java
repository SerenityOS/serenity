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

package nsk.jvmti.scenarios.contention.TC03;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class tc03t001 extends DebugeeClass {

    final static int WAIT_TIME = 100;

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new tc03t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    int status = Consts.TEST_PASSED;
    long timeout = 0;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;
        log.display("Timeout = " + timeout + " msc.");
        long MAX_LOOP = timeout / WAIT_TIME;

        String A = "A";
        String B = "B";
        tc03t001Thread threadAB = new tc03t001Thread(A, B);
        tc03t001Thread threadBA = new tc03t001Thread(B, A);

        threadAB.start();
        threadBA.start();

        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int v4 = 0;
        int old_v1 = 0;
        int old_v2 = 0;
        int old_v3 = 0;
        int old_v4 = 0;

        for (long i = 0; i < MAX_LOOP; ++i) {

            v1 = threadAB.getLock1Counter();
            v2 = threadAB.getLock2Counter();
            v3 = threadBA.getLock1Counter();
            v4 = threadBA.getLock2Counter();

            log.display("Iteration " + i);
            log.display("    threadAB: lock1Counter = " + v1 +
                        ", lock2Counter = " + v2);
            log.display("    threadBA: lock1Counter = " + v3 +
                        ", lock2Counter = " + v4);

            // break loop if threads are making no more progress
            if ((v1 + v2 + v3 + v4 > 0)
                    && (v1 == old_v1)
                    && (v2 == old_v2)
                    && (v3 == old_v3)
                    && (v4 == old_v4)) {
                return checkStatus(status);
            } else {
                old_v1 = v1;
                old_v2 = v2;
                old_v3 = v3;
                old_v4 = v4;
            }

            // wait a bit before checking again
            try {
                Thread.sleep(WAIT_TIME);
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        log.display("Debugee timed out waiting deadlock");
        return status;
    }
}

/* =================================================================== */

class tc03t001Thread extends Thread {

    Object lock1;
    Object lock2;
    int lock1Counter = 0;
    int lock2Counter = 0;

    public tc03t001Thread(Object o1, Object o2) {
        super("Debuggee Thread " + o1 + o2);
        lock1 = o1;
        lock2 = o2;
    }

    public void run() {
        tc03t001.log.display("Started " + this);
        while (true) {
            synchronized (lock1) {
                ++lock1Counter;
                synchronized (lock2) {
                    ++lock2Counter;
                }
            }
        }
    }

    public int getLock1Counter() {
        return lock1Counter;
    }

    public int getLock2Counter() {
        return lock2Counter;
    }
}
