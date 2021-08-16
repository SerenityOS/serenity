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

package nsk.jvmti.StopThread;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class stopthrd007 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new stopthrd007().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // for threads synchronization
    static Wicket startingBarrier = null;
    static Wicket waitingBarrier = null;

    // tested threads
    stopthrd007ThreadRunning runningThread = null;
    stopthrd007ThreadWaiting waitingThread = null;
    stopthrd007ThreadSleeping sleepingThread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        log.display("Debugee started");

        startingBarrier = new Wicket(3);
        waitingBarrier = new Wicket();
        runningThread = new stopthrd007ThreadRunning("DebuggeeRunningThread");
        waitingThread =
            new stopthrd007ThreadWaiting("DebuggeeWaitingThread", timeout);
        sleepingThread =
            new stopthrd007ThreadSleeping("DebuggeeSleepingThread", timeout);

        log.display("Starting tested threads");
        runningThread.start();
        waitingThread.start();
        sleepingThread.start();

        startingBarrier.waitFor();
        status = checkStatus(status);

        waitingBarrier.waitFor(timeout);

        log.display("Finishing tested threads");
        try {
            runningThread.join(timeout);
            waitingThread.join(timeout);
            sleepingThread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        switch (runningThread.getStatus()) {
        case 0:
            log.complain("DebuggeeRunningThread: ThreadDeath lost");
            status = Consts.TEST_FAILED;
            break;
        case 1:
            log.complain("DebuggeeRunningThread: stopped by flag");
            status = Consts.TEST_FAILED;
            break;
        case 2:
            log.display("DebuggeeRunningThread: got ThreadDeath");
            break;
        case 3:
            log.display("DebuggeeSleepingThread: unexpected " +
                sleepingThread.getThrowable());
            status = Consts.TEST_FAILED;
            break;
        }

        switch (waitingThread.getStatus()) {
        case 0:
            log.complain("DebuggeeWaitingThread: ThreadDeath lost");
            status = Consts.TEST_FAILED;
            break;
        case 1:
            log.complain("DebuggeeWaitingThread: wait timed out");
            status = Consts.TEST_FAILED;
            break;
        case 2:
            log.display("DebuggeeWaitingThread: got ThreadDeath");
            break;
        case 3:
            log.display("DebuggeeSleepingThread: unexpected " +
                sleepingThread.getThrowable());
            status = Consts.TEST_FAILED;
            break;
        }

        switch (sleepingThread.getStatus()) {
        case 0:
            log.complain("DebuggeeSleepingThread: ThreadDeath lost");
            status = Consts.TEST_FAILED;
            break;
        case 1:
            log.complain("DebuggeeSleepingThread: sleep timed out");
            status = Consts.TEST_FAILED;
            break;
        case 2:
            log.display("DebuggeeSleepingThread: got ThreadDeath");
            break;
        case 3:
            log.display("DebuggeeSleepingThread: unexpected " +
                sleepingThread.getThrowable());
            status = Consts.TEST_FAILED;
            break;
        }

        log.display("Debugee finished");

        return status;
    }
}

/* =================================================================== */

class stopthrd007ThreadRunning extends Thread {
    private volatile boolean flag = true;
    private volatile int status = 0;
    private volatile Throwable exc = null;

    public stopthrd007ThreadRunning(String name) {
        super(name);
    }

    public void run() {
        stopthrd007.startingBarrier.unlock();
        try {
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
            status = 1;
        } catch (ThreadDeath t) {
            status = 2;
            throw t;
        } catch (Throwable t) {
            exc = t;
            status = 3;
        } finally {
            stopthrd007.waitingBarrier.unlock();
        }
    }

    public int getStatus() {
        return status;
    }

    public Throwable getThrowable() {
        return exc;
    }
}

class stopthrd007ThreadWaiting extends Thread {
    private long timeout = 0;
    private volatile int status = 0;
    private volatile Throwable exc = null;

    public stopthrd007ThreadWaiting(String name, long tout) {
        super(name);
        timeout = tout;
    }

    public synchronized void run() {
        stopthrd007.startingBarrier.unlock();
        try {
            wait(timeout);
            status = 1;
        } catch (ThreadDeath t) {
            status = 2;
            throw t;
        } catch (Throwable t) {
            exc = t;
            status = 3;
        }
    }

    public int getStatus() {
        return status;
    }

    public Throwable getThrowable() {
        return exc;
    }
}

class stopthrd007ThreadSleeping extends Thread {
    private long timeout = 0;
    private volatile int status = 0;
    private volatile Throwable exc = null;

    public stopthrd007ThreadSleeping(String name, long tout) {
        super(name);
        timeout = tout;
    }

    public void run() {
        stopthrd007.startingBarrier.unlock();
        try {
            sleep(timeout);
            status = 1;
        } catch (ThreadDeath t) {
            status = 2;
            throw t;
        } catch (Throwable t) {
            exc = t;
            status = 3;
        }
    }

    public int getStatus() {
        return status;
    }

    public Throwable getThrowable() {
        return exc;
    }
}
