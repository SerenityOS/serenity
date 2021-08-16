/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.contention.TC04;

import java.io.PrintStream;
import java.util.concurrent.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class tc04t001 extends DebugeeClass {

    final static int THREADS_LIMIT = 2;
    final static CountDownLatch threadsDoneSignal = new CountDownLatch(THREADS_LIMIT);

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new tc04t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    int status = Consts.TEST_PASSED;
    long timeout = 0;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;
        log.display("Timeout = " + timeout + " msc.");

        tc04t001Thread threads[] = new tc04t001Thread[THREADS_LIMIT];
        status = checkStatus(status);
        for (int i = 0; i < THREADS_LIMIT; i++) {
            threads[i] = new tc04t001Thread(i);
            threads[i].start();
        }

        try {
            if (!threadsDoneSignal.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Threads timeout");
            }
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        status = checkStatus(status);

        log.display("Debugee finished, value: " + tc04t001Thread.value);
        if (tc04t001Thread.value !=
                THREADS_LIMIT*tc04t001Thread.INCREMENT_LIMIT) {
            log.complain("Wrong value: " + tc04t001Thread.value +
                ", expected: " + THREADS_LIMIT*tc04t001Thread.INCREMENT_LIMIT);
            status = Consts.TEST_FAILED;
        }
        return status;
    }
}

/* =================================================================== */

class tc04t001Thread extends Thread {

    final static int INCREMENT_LIMIT = 100;
    final static int DELAY = 1000;

    static volatile int value = 0;

    static Flicker flicker = new Flicker();

    private int id;
    private static volatile int lastEnterEventsCount;
    private static native   int enterEventsCount();

    public tc04t001Thread(int i) {
        super("Debuggee Thread " + i);
        id = i;
    }

    public synchronized void run() {
        for (int i = 0; i < INCREMENT_LIMIT; i++) {
            flicker.waitFor(id);
            increment(id);
            try {
                wait(1);
            } catch (InterruptedException e) {}
        }
        tc04t001.threadsDoneSignal.countDown();
    }

    static synchronized void increment(int i) {
        flicker.unlock(i);
        int temp = value;
        boolean done = false;

        // Wait in a loop for a MonitorContendedEnter event.
        // Timeout is: 20ms * DELAY.
        for (int j = 0; j < DELAY; j++) {
            try {
                sleep(20);
            } catch (InterruptedException e) {}

            done = (tc04t001.threadsDoneSignal.getCount() == 1);
            if (done) {
                break; // This thread is the only remaining thread, no more contention
            }
            if (enterEventsCount() > lastEnterEventsCount) {
                System.out.println("Thread-" + i + ": increment event: " + enterEventsCount());
                break; // Got an expected MonitorContendedEnter event
            }
        }

        if (!done && enterEventsCount() == lastEnterEventsCount) {
            String msg = "Timeout in waiting for a MonitorContendedEnter event";
            throw new RuntimeException(msg);
        }
        value = temp + 1;
        lastEnterEventsCount = enterEventsCount();
    }
}

class Flicker {

    private int owner = -1;

    public synchronized void waitFor(int owner) {
        while (this.owner == owner) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
    }

    public synchronized void unlock(int owner) {
        if (this.owner == owner)
            throw new IllegalStateException("the same owner: " + owner);

        this.owner = owner;
        notifyAll();
    }
}
