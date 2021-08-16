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

package nsk.jvmti.GetCurrentThreadCpuTime;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

/** Debuggee class for this test. */
public class curthrcputime001 extends DebugeeClass {

    /** Load native library if required. */
    static {
        loadLibrary("curthrcputime001");
    }

    /** Run test from command line. */
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    /** Run test from JCK-compatible environment. */
    public static int run(String argv[], PrintStream out) {
        return new curthrcputime001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    static final String TESTED_THREAD_NAME = "curthrcputime001Thread";
    static final int SLEEP_TIME = 5 * 1000; // milliseconds

    /** Run debuggee. */
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        int iterations = argHandler.findOptionIntValue("iterations", 1000);

        curthrcputime001Thread thread = new curthrcputime001Thread(TESTED_THREAD_NAME, iterations);

        // sync before thread started
        log.display("Sync: tested thread created");
        status = checkStatus(status);

        // start and finish tested thread
        try {
            synchronized (thread.endingMonitor) {
                // start thread and wait for start notification
                synchronized (thread.startingMonitor) {
                    thread.start();
                    thread.startingMonitor.wait();
                }

                // sync after thread started
                log.display("Sync: tested thread started");
                status = checkStatus(status);

                // let thread to finish
            }

            // wait for thread to finish
            thread.join();

        } catch (InterruptedException e) {
            throw new Failure("Main thread interrupted while running tested thread:\n\t"
                                + e);
        }

        // sync after thread finished
        log.display("Sync: tested thread finished");
        status = checkStatus(status);

        return status;
    }
}

/* =================================================================== */

/** Class for tested thread. */
class curthrcputime001Thread extends Thread {
    public int iterations;

    public Object startingMonitor = new Object();
    public Object endingMonitor = new Object();

    /** Make thread with specific name. */
    public curthrcputime001Thread(String name, int iterations) {
        super(name);
        this.iterations = iterations;
    }

    /** Run some code. */
    public void run() {

        runIterations(iterations);

        // notify about start
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        runIterations(iterations);

        // wait for finishing enabled
        synchronized (endingMonitor) {
        }

        runIterations(iterations);

    }

    /** Run some code with given number of iterations. */
    void runIterations(int n) {
        for (int k = 0; k < n; k++) {
            int s = k;
            for (int i = 0; i < n; i++) {
                if (i % 2 == 0) {
                    s += i * 10;
                } else {
                    s -= i * 10;
                }
            }
        }
    }
}
