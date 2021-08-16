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

package nsk.jvmti.GetThreadGroupChildren;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class getthrdgrpchld001 extends DebugeeClass {

    // load native library if required
    static {
        System.loadLibrary("getthrdgrpchld001");
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new getthrdgrpchld001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // constants
    public static final int DEFAULT_THREADS_COUNT = 4;

    // monitors
    public static Object endingMonitor = new Object();

    // tested thread groups
    ThreadGroup rootGroup = new ThreadGroup("rootThreadGroup");
    ThreadGroup runningGroup = new ThreadGroup(rootGroup, "runningThreadGroup");
    ThreadGroup notStartedGroup = new ThreadGroup(rootGroup, "notStartedThreadGroup");
    ThreadGroup finishedGroup = new ThreadGroup(rootGroup, "finishedThreadGroup");

    // tested threads
    getthrdgrpchld001ThreadRunning runningThreads[] = null;
    getthrdgrpchld001Thread notStartedThreads[] = null;
    getthrdgrpchld001Thread finishedThreads[] = null;

    int threadsCount = 0;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        threadsCount = argHandler.findOptionIntValue("threads", DEFAULT_THREADS_COUNT);

        // create tested threads
        runningThreads = new getthrdgrpchld001ThreadRunning[threadsCount];
        notStartedThreads = new getthrdgrpchld001Thread[threadsCount];
        finishedThreads = new getthrdgrpchld001Thread[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            runningThreads[i] =
                new getthrdgrpchld001ThreadRunning(runningGroup, "runningThread #" + i);
            notStartedThreads[i] =
                new getthrdgrpchld001Thread(notStartedGroup, "notStartedThread #" + i);
            finishedThreads[i] =
                new getthrdgrpchld001Thread(finishedGroup, "finishedThread #" + i);
        }

        // run tested threads
        log.display("Staring tested threads");
        try {
            synchronized (endingMonitor) {
                for (int i = 0; i < threadsCount; i++) {
                    synchronized (runningThreads[i].startingMonitor) {
                        runningThreads[i].start();
                        runningThreads[i].startingMonitor.wait();
                    }
                    finishedThreads[i].start();
                    finishedThreads[i].join();
                }
                // testing sync
                log.display("Sync: thread ready");
                status = checkStatus(status);

                // let running threads to finish
            }

            // wait for running threads to finish
            log.display("Finishing running threads");
            for (int i = 0; i < threadsCount; i++) {
                runningThreads[i].join();
            }
        } catch (InterruptedException e) {
            throw new Failure("Interruption while running tested threads: \n\t" + e);
        }

        return status;
    }
}

/* =================================================================== */

// basic class for tested thread
class getthrdgrpchld001Thread extends Thread {

    public getthrdgrpchld001Thread(ThreadGroup group, String name) {
        super(group, name);
    }

    public void run() {
        // do something
        int s = 0;
        for (int i = 0; i < 1000; i++) {
            s += i;
            if (s > 1000)
                s = 0;
        }
    }
}

// tested thread running
class getthrdgrpchld001ThreadRunning extends getthrdgrpchld001Thread {
    public Object startingMonitor = new Object();

    public getthrdgrpchld001ThreadRunning(ThreadGroup group, String name) {
        super(group, name);
    }

    public void run() {
        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        // block before finishing
        synchronized ( getthrdgrpchld001.endingMonitor) {
            // just finish
        }
    }
}
