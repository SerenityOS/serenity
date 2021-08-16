/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4413752 8262881
 * @summary Test SuspendThread with ObjectMonitor wait.
 * @requires vm.jvmti
 * @library /test/lib
 * @compile SuspendWithObjectMonitorWait.java
 * @run main/othervm/native -agentlib:SuspendWithObjectMonitorWait SuspendWithObjectMonitorWait
 */

import java.io.PrintStream;

//
// main               waiter              resumer
// =================  ==================  ===================
// launch waiter
// <launch returns>   waiter running
// launch resumer     enter threadLock
// <launch returns>   threadLock.wait()   resumer running
// enter threadLock   :                   wait for notify
// threadLock.notify  wait finishes       :
// :                  reenter blocks      :
// suspend waiter     <suspended>         :
// exit threadLock    :                   :
// <ready to test>    :                   :
// :                  :                   :
// notify resumer     :                   wait finishes
// join resumer       :                   enter threadLock
// :                  <resumed>           resume waiter
// :                  :                   exit threadLock
// :                  reenter threadLock  :
// <join returns>     :                   resumer exits
// join waiter        :
// <join returns>     waiter exits
//

public class SuspendWithObjectMonitorWait {
    private static final String AGENT_LIB = "SuspendWithObjectMonitorWait";
    private static final int exit_delta   = 95;

    private static final int DEF_TIME_MAX = 60;    // default max # secs to test
    private static final int JOIN_MAX     = 30;    // max # secs to wait for join

    public static final int TS_INIT            = 1;  // initial testState
    public static final int TS_WAITER_RUNNING  = 2;  // waiter is running
    public static final int TS_RESUMER_RUNNING = 3;  // resumer is running
    public static final int TS_READY_TO_NOTIFY = 4;  // ready to notify threadLock
    public static final int TS_CALL_SUSPEND    = 5;  // call suspend on contender
    public static final int TS_READY_TO_RESUME = 6;  // ready to resume waiter
    public static final int TS_CALL_RESUME     = 7;  // call resume on waiter
    public static final int TS_WAITER_DONE     = 8;  // waiter has run; done

    public static Object barrierLaunch = new Object();   // controls thread launch
    public static Object barrierResumer = new Object();  // controls resumer
    public static Object threadLock = new Object();      // testing object

    public static long count = 0;
    public static boolean printDebug = false;
    public volatile static int testState;

    private static void log(String msg) { System.out.println(msg); }

    native static int suspendThread(SuspendWithObjectMonitorWaitWorker thr);
    native static int wait4ContendedEnter(SuspendWithObjectMonitorWaitWorker thr);

    public static void main(String[] args) throws Exception {
        try {
            System.loadLibrary(AGENT_LIB);
            log("Loaded library: " + AGENT_LIB);
        } catch (UnsatisfiedLinkError ule) {
            log("Failed to load library: " + AGENT_LIB);
            log("java.library.path: " + System.getProperty("java.library.path"));
            throw ule;
        }

        int timeMax = 0;
        if (args.length == 0) {
            timeMax = DEF_TIME_MAX;
        } else {
            int argIndex = 0;
            int argsLeft = args.length;
            if (args[0].equals("-p")) {
                printDebug = true;
                argIndex = 1;
                argsLeft--;
            }
            if (argsLeft == 0) {
                timeMax = DEF_TIME_MAX;
            } else if (argsLeft == 1) {
                try {
                    timeMax = Integer.parseUnsignedInt(args[argIndex]);
                } catch (NumberFormatException nfe) {
                    System.err.println("'" + args[argIndex] +
                                       "': invalid timeMax value.");
                    usage();
                }
            } else {
                usage();
            }
        }

        System.exit(run(timeMax, System.out) + exit_delta);
    }

    public static void logDebug(String mesg) {
        if (printDebug) {
            System.err.println(Thread.currentThread().getName() + ": " + mesg);
        }
    }

    public static void usage() {
        System.err.println("Usage: " + AGENT_LIB + " [-p][time_max]");
        System.err.println("where:");
        System.err.println("    -p       ::= print debug info");
        System.err.println("    time_max ::= max looping time in seconds");
        System.err.println("                 (default is " + DEF_TIME_MAX +
                           " seconds)");
        System.exit(1);
    }

    public static int run(int timeMax, PrintStream out) {
        return (new SuspendWithObjectMonitorWait()).doWork(timeMax, out);
    }

    public static void checkTestState(int exp) {
        if (testState != exp) {
            System.err.println("Failure at " + count + " loops.");
            throw new InternalError("Unexpected test state value: "
                + "expected=" + exp + " actual=" + testState);
        }
    }

    public int doWork(int timeMax, PrintStream out) {
        SuspendWithObjectMonitorWaitWorker waiter;    // waiter thread
        SuspendWithObjectMonitorWaitWorker resumer;    // resumer thread

        System.out.println("About to execute for " + timeMax + " seconds.");

        long start_time = System.currentTimeMillis();
        while (System.currentTimeMillis() < start_time + (timeMax * 1000)) {
            count++;
            testState = TS_INIT;  // starting the test loop

            // launch the waiter thread
            synchronized (barrierLaunch) {
                waiter = new SuspendWithObjectMonitorWaitWorker("waiter");
                waiter.start();

                while (testState != TS_WAITER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // launch the resumer thread
            synchronized (barrierLaunch) {
                resumer = new SuspendWithObjectMonitorWaitWorker("resumer", waiter);
                resumer.start();

                while (testState != TS_RESUMER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            checkTestState(TS_RESUMER_RUNNING);

            // The waiter thread was synchronized on threadLock before it
            // set TS_WAITER_RUNNING and notified barrierLaunch above so
            // we cannot enter threadLock until the waiter thread calls
            // threadLock.wait().
            synchronized (threadLock) {
                // notify waiter thread so it can try to reenter threadLock
                testState = TS_READY_TO_NOTIFY;
                threadLock.notify();

                // wait for the waiter thread to block
                logDebug("before contended enter wait");
                int retCode = wait4ContendedEnter(waiter);
                if (retCode != 0) {
                    throw new RuntimeException("error in JVMTI GetThreadState: "
                                               + "retCode=" + retCode);
                }
                logDebug("done contended enter wait");

                checkTestState(TS_READY_TO_NOTIFY);
                testState = TS_CALL_SUSPEND;
                logDebug("before suspend thread");
                retCode = suspendThread(waiter);
                if (retCode != 0) {
                    throw new RuntimeException("error in JVMTI SuspendThread: "
                                               + "retCode=" + retCode);
                }
                logDebug("suspended thread");
            }

            //
            // At this point, all of the child threads are running
            // and we can get to meat of the test:
            //
            // - suspended threadLock waiter (trying to reenter)
            // - a threadLock enter in the resumer thread
            // - resumption of the waiter thread
            // - a threadLock enter in the freshly resumed waiter thread
            //

            synchronized (barrierResumer) {
                checkTestState(TS_CALL_SUSPEND);

                // tell resumer thread to resume waiter thread
                testState = TS_READY_TO_RESUME;
                barrierResumer.notify();

                // Can't call checkTestState() here because the
                // resumer thread may have already resumed the
                // waiter thread.
            }

            try {
                resumer.join(JOIN_MAX * 1000);
                if (resumer.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("resumer thread is stuck");
                }
                waiter.join(JOIN_MAX * 1000);
                if (waiter.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("waiter thread is stuck");
                }
            } catch (InterruptedException ex) {
            }

            checkTestState(TS_WAITER_DONE);
        }

        System.out.println("Executed " + count + " loops in " + timeMax +
                           " seconds.");

        return 0;
    }
}

class SuspendWithObjectMonitorWaitWorker extends Thread {
    private SuspendWithObjectMonitorWaitWorker target;  // target for resume operation

    public SuspendWithObjectMonitorWaitWorker(String name) {
        super(name);
    }

    public SuspendWithObjectMonitorWaitWorker(String name, SuspendWithObjectMonitorWaitWorker target) {
        super(name);
        this.target = target;
    }

    native static int resumeThread(SuspendWithObjectMonitorWaitWorker thr);

    public void run() {
        SuspendWithObjectMonitorWait.logDebug("thread running");

        //
        // Launch the waiter thread:
        // - grab the threadLock
        // - threadLock.wait()
        // - releases threadLock
        //
        if (getName().equals("waiter")) {
            // grab threadLock before we tell main we are running
            SuspendWithObjectMonitorWait.logDebug("before enter threadLock");
            synchronized(SuspendWithObjectMonitorWait.threadLock) {
                SuspendWithObjectMonitorWait.logDebug("enter threadLock");

                SuspendWithObjectMonitorWait.checkTestState(SuspendWithObjectMonitorWait.TS_INIT);

                synchronized(SuspendWithObjectMonitorWait.barrierLaunch) {
                    // tell main we are running
                    SuspendWithObjectMonitorWait.testState = SuspendWithObjectMonitorWait.TS_WAITER_RUNNING;
                    SuspendWithObjectMonitorWait.barrierLaunch.notify();
                }

                SuspendWithObjectMonitorWait.logDebug("before wait");

                // TS_READY_TO_NOTIFY is set after the main thread has
                // entered threadLock so a spurious wakeup can't get the
                // waiter thread out of this threadLock.wait(0) call:
                while (SuspendWithObjectMonitorWait.testState <= SuspendWithObjectMonitorWait.TS_READY_TO_NOTIFY) {
                    try {
                        SuspendWithObjectMonitorWait.threadLock.wait(0);
                    } catch (InterruptedException ex) {
                    }
                }

                SuspendWithObjectMonitorWait.logDebug("after wait");

                SuspendWithObjectMonitorWait.checkTestState(SuspendWithObjectMonitorWait.TS_CALL_RESUME);
                SuspendWithObjectMonitorWait.testState = SuspendWithObjectMonitorWait.TS_WAITER_DONE;

                SuspendWithObjectMonitorWait.logDebug("exit threadLock");
            }
        }
        //
        // Launch the resumer thread:
        // - tries to grab the threadLock (should not block!)
        // - grabs threadLock
        // - resumes the waiter thread
        // - releases threadLock
        //
        else if (getName().equals("resumer")) {
            synchronized(SuspendWithObjectMonitorWait.barrierResumer) {
                synchronized(SuspendWithObjectMonitorWait.barrierLaunch) {
                    // tell main we are running
                    SuspendWithObjectMonitorWait.testState = SuspendWithObjectMonitorWait.TS_RESUMER_RUNNING;
                    SuspendWithObjectMonitorWait.barrierLaunch.notify();
                }
                SuspendWithObjectMonitorWait.logDebug("thread waiting");
                while (SuspendWithObjectMonitorWait.testState != SuspendWithObjectMonitorWait.TS_READY_TO_RESUME) {
                    try {
                        // wait for main to tell us when to continue
                        SuspendWithObjectMonitorWait.barrierResumer.wait(0);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            SuspendWithObjectMonitorWait.logDebug("before enter threadLock");
            synchronized(SuspendWithObjectMonitorWait.threadLock) {
                SuspendWithObjectMonitorWait.logDebug("enter threadLock");

                SuspendWithObjectMonitorWait.checkTestState(SuspendWithObjectMonitorWait.TS_READY_TO_RESUME);
                SuspendWithObjectMonitorWait.testState = SuspendWithObjectMonitorWait.TS_CALL_RESUME;

                // resume the waiter thread so waiter.join() can work
                SuspendWithObjectMonitorWait.logDebug("before resume thread");
                int retCode = resumeThread(target);
                if (retCode != 0) {
                    throw new RuntimeException("error in JVMTI ResumeThread: " +
                                               "retCode=" + retCode);
                }
                SuspendWithObjectMonitorWait.logDebug("resumed thread");

                SuspendWithObjectMonitorWait.logDebug("exit threadLock");
            }
        }
    }
}
