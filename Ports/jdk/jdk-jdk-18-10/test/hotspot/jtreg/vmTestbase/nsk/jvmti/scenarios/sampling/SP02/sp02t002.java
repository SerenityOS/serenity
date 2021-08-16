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

package nsk.jvmti.scenarios.sampling.SP02;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class sp02t002 extends DebugeeClass {

    // load native library if required
    static {
        System.loadLibrary("sp02t002");
    }

    // run test from command line
    public static void main(String argv[]) {
    argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

    // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new sp02t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    static public Log log = null;

    ArgumentHandler argHandler = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // monitors for threads synchronization
    static Object endingMonitor = new Object();

    // tested threads list
    sp02t002Thread threads[] = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // create threads list
        threads = new sp02t002Thread[] {
            new sp02t002ThreadRunning("threadRunning"),
            new sp02t002ThreadEntering("threadEntering"),
            new sp02t002ThreadWaiting("threadWaiting"),
            new sp02t002ThreadSleeping("threadSleeping"),
            new sp02t002ThreadRunningInterrupted("threadRunningInterrupted"),
            new sp02t002ThreadRunningNative("threadRunningNative")
        };

        // run threads
        log.display("Starting tested threads");
        try {
            synchronized (endingMonitor) {
                // start threads (except first one)
                for (int i = 0; i < threads.length; i++) {
                    synchronized (threads[i].startingMonitor) {
                        threads[i].start();
                        threads[i].startingMonitor.wait();
                    }
                    if (!threads[i].checkReady()) {
                        throw new Failure("Unable to prepare thread #" + i + ": " + threads[i]);
                    }
                }

                // testing sync
            log.display("Testing sync: threads ready");
                status = checkStatus(status);
            }
        } catch (InterruptedException e) {
            throw new Failure(e);
        } finally {
            // let all threads to finish
            for (int i = 0; i < threads.length; i++) {
                threads[i].letFinish();
            }
        }

        // wait for all threads to finish
        log.display("Finishing tested threads");
        try {
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        return status;
    }
}

/* =================================================================== */

// basic class for tested threads
abstract class sp02t002Thread extends Thread {
    public Object startingMonitor = new Object();

    // make thread with specific name
    public sp02t002Thread(String name) {
        super(name);
    }

    // tested method
    public abstract void testedMethod();

    // run thread and call tested method
    public void run() {
        // call tested mathod
        sp02t002.log.display(getName() + ": run(): before call to testedMethod");
        testedMethod();
        sp02t002.log.display(getName() + ": run():  after call to testedMethod");
    }

    // check if thread is ready for testing
    public boolean checkReady() {
        // return true by default
        return true;
    }

    // let thread to finish
    public void letFinish() {
        // do nothing by default
    }
}

/* =================================================================== */

class sp02t002ThreadRunning extends sp02t002Thread {
    private volatile boolean shouldFinish = false;

    public sp02t002ThreadRunning(String name) {
        super(name);
    }

    public void testedMethod() {
        sp02t002.log.display(getName() + ": testedMethod()");

        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        // run in a loop
        int i = 0;
        int n = 1000;
        while (!shouldFinish) {
            if (n <= 0) {
                n = 1000;
            }
            if (i > n) {
                i = 0;
                n = n - 1;
            }
            i = i + 1;
        }
    }

    public void letFinish() {
        shouldFinish = true;
    }
}

class sp02t002ThreadEntering extends sp02t002Thread {
    public sp02t002ThreadEntering(String name) {
        super(name);
    }

    public void testedMethod() {
        sp02t002.log.display(getName() + ": testedMethod()");

        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        // wait for endingMonitor to become released
        synchronized (sp02t002.endingMonitor) {
            // do nothing
        }
    }
}

class sp02t002ThreadWaiting extends sp02t002Thread {
    private Object waitingMonitor = new Object();

    public sp02t002ThreadWaiting(String name) {
        super(name);
    }

    public void testedMethod() {
        sp02t002.log.display(getName() + ": testedMethod()");

        synchronized (waitingMonitor) {

            // notify about starting
            synchronized (startingMonitor) {
                startingMonitor.notifyAll();
            }

            // wait on monitor
            try {
                waitingMonitor.wait();
            } catch (InterruptedException ignore) {
                // just finish
            }
        }
    }

    public boolean checkReady() {
        // wait until waitingMonitor released on wait()
        synchronized (waitingMonitor) {
        }
        return true;
    }

    public void letFinish() {
        synchronized (waitingMonitor) {
            waitingMonitor.notifyAll();
        }
    }
}

class sp02t002ThreadSleeping extends sp02t002Thread {
    public sp02t002ThreadSleeping(String name) {
        super(name);
    }

    public void testedMethod() {
        sp02t002.log.display(getName() + ": testedMethod()");

        // sleep for a loooong time
        long timeout = 7 * 24 * 60 * 60 * 1000; // one week in milliseconds

        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        try {
            sleep(timeout);
        } catch (InterruptedException ignore) {
            // just finish
        }
    }

    public void letFinish() {
        interrupt();
    }
}

class sp02t002ThreadRunningInterrupted extends sp02t002Thread {
    private Object waitingMonitor = new Object();
    private volatile boolean shouldFinish = false;

    public sp02t002ThreadRunningInterrupted(String name) {
        super(name);
    }

    public void testedMethod() {
        sp02t002.log.display(getName() + ": testedMethod()");

        synchronized (waitingMonitor) {

            // notify about starting
            synchronized (startingMonitor) {
                startingMonitor.notifyAll();
            }

            // wait on watingMonitor until interrupted
            try {
                waitingMonitor.wait();
            } catch (InterruptedException ignore) {
                // just continue
            }
        }

        // run in a loop
        int i = 0;
        int n = 1000;
        while (!shouldFinish) {
            if (n <= 0) {
                n = 1000;
            }
            if (i > n) {
                i = 0;
                n = n - 1;
            }
            i = i + 1;
        }
    }

    public boolean checkReady() {
        // interrupt thread on wait()
        synchronized (waitingMonitor) {
            interrupt();
        }

        return true;
    }

    public void letFinish() {
        shouldFinish = true;
    }
}

class sp02t002ThreadRunningNative extends sp02t002Thread {
    public sp02t002ThreadRunningNative(String name) {
        super(name);
    }

    public void run() {
        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        // call tested mathod
        sp02t002.log.display(getName() + ": run(): before call to testedMethod");
        testedMethod();
        sp02t002.log.display(getName() + ": run():  after call to testedMethod");
    }

    public native void testedMethod();

    public native boolean checkReady();
    public native void letFinish();
}
