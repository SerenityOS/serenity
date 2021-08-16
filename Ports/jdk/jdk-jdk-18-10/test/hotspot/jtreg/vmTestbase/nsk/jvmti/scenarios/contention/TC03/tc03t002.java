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
import java.util.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class tc03t002 extends DebugeeClass {

    final static int WAIT_TIME = 100;

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new tc03t002().runIt(argv, out);
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

        log.display("Debugee started");

        // create deadlocked threads
        String A = "A";
        String B = "B";
        String C = "C";
        tc03t002Thread threadAB = new tc03t002Thread(A, B);
        tc03t002Thread threadBC = new tc03t002Thread(B, C);
        tc03t002Thread threadCA = new tc03t002Thread(C, A);

        // start the threads
        threadAB.start();
        threadBC.start();
        threadCA.start();

        // wait until all the threads have started
        tc03t002Thread.startingBarrier.waitFor();

        // let the threads to proceed
        threadAB.waitingBarrier.unlock();
        threadBC.waitingBarrier.unlock();
        threadCA.waitingBarrier.unlock();

        // wait until the deadlock is ready
        tc03t002Thread.lockingBarrier.waitFor();

        // sleep a little while to wait until threads are blocked
        try {
            Thread.sleep(WAIT_TIME);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        // try to find the deadlock
        status = checkStatus(status);

        log.display("Debugee finished");
        return status;
    }
}

/* =================================================================== */

class tc03t002Thread extends Thread {

    static Wicket startingBarrier = new Wicket(3);
    static Wicket lockingBarrier = new Wicket(3);
    Wicket waitingBarrier = new Wicket();
    Object lock1;
    Object lock2;

    public tc03t002Thread(Object o1, Object o2) {
        super("Debuggee Thread " + o1 + o2);
        lock1 = o1;
        lock2 = o2;
    }

    public void run() {
        tc03t002.log.display("Started " + this);
        while (true) {
            synchronized (lock1) {
                startingBarrier.unlock();
                waitingBarrier.waitFor();
                lockingBarrier.unlock();
                synchronized (lock2) {
                    throw new Failure(this + ": should not reach here");
                }
            }
        }
    }
}
