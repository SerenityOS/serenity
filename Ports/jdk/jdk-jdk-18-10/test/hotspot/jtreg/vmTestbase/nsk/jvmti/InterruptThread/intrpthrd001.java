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

package nsk.jvmti.InterruptThread;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class intrpthrd001 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new intrpthrd001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // for threads synchronization
    static Wicket startingBarrier = null;

    // tested threads
    intrpthrd001ThreadRunning runningThread = null;
    intrpthrd001ThreadWaiting waitingThread = null;
    intrpthrd001ThreadSleeping sleepingThread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        log.display("Debugee started");

        startingBarrier = new Wicket(3);
        runningThread = new intrpthrd001ThreadRunning("DebuggeeRunningThread");
        waitingThread =
            new intrpthrd001ThreadWaiting("DebuggeeWaitingThread", timeout);
        sleepingThread =
            new intrpthrd001ThreadSleeping("DebuggeeSleepingThread", timeout);

        log.display("Starting tested threads");
        runningThread.start();
        waitingThread.start();
        sleepingThread.start();

        startingBarrier.waitFor();
        status = checkStatus(status);

        if (runningThread.isInterrupted()) {
            log.display("DebuggeeRunningThread: got interrupt signal");
        } else {
            log.complain("DebuggeeRunningThread: interrupt signal lost");
            status = Consts.TEST_FAILED;
        }

        runningThread.letItGo();

        log.display("Finishing tested threads");
        try {
            runningThread.join(timeout);
            waitingThread.join(timeout);
            sleepingThread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        switch (waitingThread.getStatus()) {
        case 0:
            log.complain("DebuggeeWaitingThread: InterruptedException lost");
            status = Consts.TEST_FAILED;
        case 1:
            log.complain("DebuggeeWaitingThread: wait timed out");
            status = Consts.TEST_FAILED;
        case 2:
            log.display("DebuggeeWaitingThread: got InterruptedException");
        }

        switch (sleepingThread.getStatus()) {
        case 0:
            log.complain("DebuggeeSleepingThread: InterruptedException lost");
            status = Consts.TEST_FAILED;
        case 1:
            log.complain("DebuggeeSleepingThread: sleep timed out");
            status = Consts.TEST_FAILED;
        case 2:
            log.display("DebuggeeSleepingThread: got InterruptedException");
        }

        log.display("Debugee finished");

        return status;
    }
}

/* =================================================================== */

class intrpthrd001ThreadRunning extends Thread {
    private volatile boolean flag = true;

    public intrpthrd001ThreadRunning(String name) {
        super(name);
    }

    public void run() {
        intrpthrd001.startingBarrier.unlock();
        int i = 0;
        int n = 1000;
        while (flag) {
            if (n <= 0) {
                n = 1000;
            }
            if (i > n) {
                i = 0;
                n--;
            }
            i++;
        }
    }

    public void letItGo() {
        flag = false;
    }
}

class intrpthrd001ThreadWaiting extends Thread {
    private volatile int status = 0;
    private long timeout = 0;

    public intrpthrd001ThreadWaiting(String name, long tout) {
        super(name);
        timeout = tout;
    }

    public synchronized void run() {
        intrpthrd001.startingBarrier.unlock();
        try {
            wait(timeout);
            status = 1;
        } catch (InterruptedException e) {
            status = 2;
        }
    }

    public int getStatus() {
        return status;
    }
}

class intrpthrd001ThreadSleeping extends Thread {
    private volatile int status = 0;
    private long timeout = 0;

    public intrpthrd001ThreadSleeping(String name, long tout) {
        super(name);
        timeout = tout;
    }

    public void run() {
        intrpthrd001.startingBarrier.unlock();
        try {
            sleep(timeout);
            status = 1;
        } catch (InterruptedException e) {
            status = 2;
        }
    }

    public int getStatus() {
        return status;
    }
}
