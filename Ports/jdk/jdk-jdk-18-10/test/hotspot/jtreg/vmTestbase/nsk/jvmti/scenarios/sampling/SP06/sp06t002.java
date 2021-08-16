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

package nsk.jvmti.scenarios.sampling.SP06;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class sp06t002 extends DebugeeClass {

    // load native library if required
    static {
        System.loadLibrary("sp06t002");
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new sp06t002().runIt(argv, out);
    }

    /* =================================================================== */

    // constants
    public static final int COMPILE_ITERATIONS = 100;

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // monitors for threads synchronization
    static Object endingMonitor = new Object();

    // tested threads list
    sp06t002Thread threads[] = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // create threads list
        threads = new sp06t002Thread[] {
            new sp06t002ThreadRunning("threadRunning", log),
            new sp06t002ThreadEntering("threadEntering", log),
            new sp06t002ThreadWaiting("threadWaiting", log),
            new sp06t002ThreadSleeping("threadSleeping", log),
            new sp06t002ThreadRunningInterrupted("threadRunningInterrupted", log),
            new sp06t002ThreadRunningNative("threadRunningNative", log)
        };

        // run threads
        log.display("Starting tested threads");
        try {
            synchronized (endingMonitor) {
                // start threads (except first one)
                for (int i = 0; i < threads.length; i++) {
                    threads[i].start();
                    if (!threads[i].checkReady()) {
                        throw new Failure("Unable to prepare thread #" + i + ": " + threads[i]);
                    }
                    log.display("  thread ready: " + threads[i].getName());
                }

                // testing sync
                log.display("Testing sync: threads ready");
                status = checkStatus(status);
            }
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
abstract class sp06t002Thread extends Thread {
    Log log = null;
    volatile boolean threadReady = false;
    volatile boolean shouldFinish = false;

    // make thread with specific name
    public sp06t002Thread(String name, Log log) {
        super(name);
        this.log = log;
    }

    // run thread
    public void run() {
        // run tested method in a loop to provoke compilation
        for (int i = 0; i < sp06t002.COMPILE_ITERATIONS; i++) {
            testedMethod(true, i);
        }
        // run tested method for check
        testedMethod(false, 0);
    }

    // tested method
    public abstract void testedMethod(boolean simulate, int i);

    // check if thread is ready for testing
    public boolean checkReady() {
        try {
            while (!threadReady) {
                sleep(1000);
            }
        } catch (InterruptedException e) {
            log.complain("Interrupted " + getName() + ": " + e);
            return false;
        }
        return true;
    }

    // let thread to finish
    public void letFinish() {
        shouldFinish = true;
    }
}

/* =================================================================== */

class sp06t002ThreadRunning extends sp06t002Thread {
    public sp06t002ThreadRunning(String name, Log log) {
        super(name, log);
    }

    public void testedMethod(boolean simulate, int i) {
        if (!simulate) {
            threadReady = true;
            // run in a loop
            int k = 0;
            int n = 1000;
            while (!shouldFinish) {
                if (n <= 0) {
                    n = 1000;
                }
                if (k > n) {
                    k = 0;
                    n = n - 1;
                }
                k = k + 1;
            }
        }
    }
}

class sp06t002ThreadEntering extends sp06t002Thread {
    public sp06t002ThreadEntering(String name, Log log) {
        super(name, log);
    }

    public void testedMethod(boolean simulated, int i) {
        if (!simulated) {
            threadReady = true;
            // wait for endingMonitor to become released
            synchronized (sp06t002.endingMonitor) {
                // do nothing
            }
        }
    }
}

class sp06t002ThreadWaiting extends sp06t002Thread {
    private Object waitingMonitor = new Object();

    public sp06t002ThreadWaiting(String name, Log log) {
        super(name, log);
    }

    public void testedMethod(boolean simulate, int i) {
        synchronized (waitingMonitor) {
            if (!simulate) {
                // wait on monitor
                try {
                    waitingMonitor.wait();
                } catch (InterruptedException ignore) {
                    // just finish
                }
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

class sp06t002ThreadSleeping extends sp06t002Thread {
    public sp06t002ThreadSleeping(String name, Log log) {
        super(name, log);
    }

    public void testedMethod(boolean simulate, int i) {
        long longTimeout = 7 * 24 * 60 * 60 * 1000; // one week in milliseconds

        if (!simulate) {
            // sleep for a loooong time
            try {
                threadReady = true;
                sleep(longTimeout);
            } catch (InterruptedException ignore) {
                // just finish
            }
        }
    }

    public void letFinish() {
        interrupt();
    }
}

class sp06t002ThreadRunningInterrupted extends sp06t002Thread {
    private Object waitingMonitor = new Object();

    public sp06t002ThreadRunningInterrupted(String name, Log log) {
        super(name, log);
    }

    public void testedMethod(boolean simulate, int i) {
        if (!simulate) {
            synchronized (waitingMonitor) {
                // wait on watingMonitor until interrupted
                try {
                    waitingMonitor.wait();
                } catch (InterruptedException ignore) {
                    // just continue
                }
            }

            threadReady = true;
            // run in a loop after interrupted
            int k = 0;
            int n = 1000;
            while (!shouldFinish) {
                if (n <= 0) {
                    n = 1000;
                }
                if (k > n) {
                    k = 0;
                    n = n - 1;
                }
                k = k + 1;
            }
        }
    }

    public boolean checkReady() {
        // interrupt thread on wait()
        synchronized (waitingMonitor) {
            interrupt();
        }
        return true;
    }
}

class sp06t002ThreadRunningNative extends sp06t002Thread {
    public sp06t002ThreadRunningNative(String name, Log log) {
        super(name, log);
    }

    public native void testedMethod(boolean simulate, int i);

    public native boolean checkReady();
    public native void letFinish();
}
