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
 * @summary Test SuspendThread with ObjectMonitor enter.
 * @requires vm.jvmti
 * @library /test/lib
 * @compile SuspendWithObjectMonitorEnter.java
 * @run main/othervm/native -agentlib:SuspendWithObjectMonitorEnter SuspendWithObjectMonitorEnter
 */

import java.io.PrintStream;

//
// main               blocker           contender            resumer
// =================  ================  ===================  ================
// launch blocker
// <launch returns>   blocker running
// launch contender   enter threadLock
// <launch returns>   wait for notify   contender running
// launch resumer     :                 block on threadLock
// <launch returns>   :                 :                    resumer running
// suspend contender  :                 <suspended>          wait for notify
// <ready to test>    :                 :                    :
// :                  :                 :                    :
// notify blocker     wait finishes     :                    :
// notify resumer     exit threadLock   :                    wait finishes
// join blocker       :                 :                    enter threadLock
// <join returns>     blocker exits     <resumed>            resume contender
// join resumer                         :                    exit threadLock
// <join returns>                       enter threadLock     resumer exits
// join contender                       exit threadLock
// <join returns>                       contender exits
//

public class SuspendWithObjectMonitorEnter {
    private static final String AGENT_LIB = "SuspendWithObjectMonitorEnter";
    private static final int exit_delta   = 95;

    private static final int DEF_TIME_MAX = 60;    // default max # secs to test
    private static final int JOIN_MAX     = 30;    // max # secs to wait for join

    public static final int TS_INIT              = 1;  // initial testState
    public static final int TS_BLOCKER_RUNNING   = 2;  // blocker is running
    public static final int TS_CONTENDER_RUNNING = 3;  // contender is running
    public static final int TS_RESUMER_RUNNING   = 4;  // resumer is running
    public static final int TS_CALL_SUSPEND      = 5;  // call suspend on contender
    public static final int TS_DONE_BLOCKING     = 6;  // done blocking threadLock
    public static final int TS_READY_TO_RESUME   = 7;  // ready to resume contender
    public static final int TS_CALL_RESUME       = 8;  // call resume on contender
    public static final int TS_CONTENDER_DONE    = 9;  // contender has run; done

    public static Object barrierLaunch = new Object();   // controls thread launch
    public static Object barrierBlocker = new Object();  // controls blocker
    public static Object barrierResumer = new Object();  // controls resumer
    public static Object threadLock = new Object();      // testing object

    public static long count = 0;
    public static boolean printDebug = false;
    public volatile static int testState;

    private static void log(String msg) { System.out.println(msg); }

    native static int suspendThread(SuspendWithObjectMonitorEnterWorker thr);
    native static int wait4ContendedEnter(SuspendWithObjectMonitorEnterWorker thr);

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
        return (new SuspendWithObjectMonitorEnter()).doWork(timeMax, out);
    }

    public static void checkTestState(int exp) {
        if (testState != exp) {
            System.err.println("Failure at " + count + " loops.");
            throw new InternalError("Unexpected test state value: "
                + "expected=" + exp + " actual=" + testState);
        }
    }

    public int doWork(int timeMax, PrintStream out) {
        SuspendWithObjectMonitorEnterWorker blocker;    // blocker thread
        SuspendWithObjectMonitorEnterWorker contender;  // contender thread
        SuspendWithObjectMonitorEnterWorker resumer;    // resumer thread

        System.out.println("About to execute for " + timeMax + " seconds.");

        long start_time = System.currentTimeMillis();
        while (System.currentTimeMillis() < start_time + (timeMax * 1000)) {
            count++;
            testState = TS_INIT;  // starting the test loop

            // launch the blocker thread
            synchronized (barrierLaunch) {
                blocker = new SuspendWithObjectMonitorEnterWorker("blocker");
                blocker.start();

                while (testState != TS_BLOCKER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // launch the contender thread
            synchronized (barrierLaunch) {
                contender = new SuspendWithObjectMonitorEnterWorker("contender");
                contender.start();

                while (testState != TS_CONTENDER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // launch the resumer thread
            synchronized (barrierLaunch) {
                resumer = new SuspendWithObjectMonitorEnterWorker("resumer", contender);
                resumer.start();

                while (testState != TS_RESUMER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // wait for the contender thread to block
            logDebug("before contended enter wait");
            int retCode = wait4ContendedEnter(contender);
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI GetThreadState: " +
                                           "retCode=" + retCode);
            }
            logDebug("done contended enter wait");

            checkTestState(TS_RESUMER_RUNNING);
            testState = TS_CALL_SUSPEND;
            logDebug("before suspend thread");
            retCode = suspendThread(contender);
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI SuspendThread: " +
                                           "retCode=" + retCode);
            }
            logDebug("suspended thread");

            //
            // At this point, all of the child threads are running
            // and we can get to meat of the test:
            //
            // - suspended threadLock contender
            // - a threadLock exit in the blocker thread
            // - a threadLock enter in the resumer thread
            // - resumption of the contender thread
            // - a threadLock enter in the freshly resumed contender thread
            //
            synchronized (barrierBlocker) {
                checkTestState(TS_CALL_SUSPEND);

                // tell blocker thread to exit threadLock
                testState = TS_DONE_BLOCKING;
                barrierBlocker.notify();
            }

            synchronized (barrierResumer) {
                // tell resumer thread to resume contender thread
                testState = TS_READY_TO_RESUME;
                barrierResumer.notify();

                // Can't call checkTestState() here because the
                // resumer thread may have already resumed the
                // contender thread.
            }

            try {
                blocker.join();
                resumer.join(JOIN_MAX * 1000);
                if (resumer.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("resumer thread is stuck");
                }
                contender.join(JOIN_MAX * 1000);
                if (contender.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("contender thread is stuck");
                }
            } catch (InterruptedException ex) {
            }

            checkTestState(TS_CONTENDER_DONE);
        }

        System.out.println("Executed " + count + " loops in " + timeMax +
                           " seconds.");

        return 0;
    }
}

class SuspendWithObjectMonitorEnterWorker extends Thread {
    private SuspendWithObjectMonitorEnterWorker target;  // target for resume operation

    public SuspendWithObjectMonitorEnterWorker(String name) {
        super(name);
    }

    public SuspendWithObjectMonitorEnterWorker(String name, SuspendWithObjectMonitorEnterWorker target) {
        super(name);
        this.target = target;
    }

    native static int resumeThread(SuspendWithObjectMonitorEnterWorker thr);

    public void run() {
        SuspendWithObjectMonitorEnter.logDebug("thread running");

        //
        // Launch the blocker thread:
        // - grabs threadLock
        // - holds threadLock until we tell it let go
        // - releases threadLock
        //
        if (getName().equals("blocker")) {
            // grab threadLock before we tell main we are running
            SuspendWithObjectMonitorEnter.logDebug("before enter threadLock");
            synchronized(SuspendWithObjectMonitorEnter.threadLock) {
                SuspendWithObjectMonitorEnter.logDebug("enter threadLock");

                SuspendWithObjectMonitorEnter.checkTestState(SuspendWithObjectMonitorEnter.TS_INIT);

                synchronized(SuspendWithObjectMonitorEnter.barrierBlocker) {
                    synchronized(SuspendWithObjectMonitorEnter.barrierLaunch) {
                        // tell main we are running
                        SuspendWithObjectMonitorEnter.testState = SuspendWithObjectMonitorEnter.TS_BLOCKER_RUNNING;
                        SuspendWithObjectMonitorEnter.barrierLaunch.notify();
                    }
                    SuspendWithObjectMonitorEnter.logDebug("thread waiting");
                    // TS_READY_TO_RESUME is set right after TS_DONE_BLOCKING
                    // is set so either can get the blocker thread out of
                    // this wait() wrapper:
                    while (SuspendWithObjectMonitorEnter.testState != SuspendWithObjectMonitorEnter.TS_DONE_BLOCKING &&
                           SuspendWithObjectMonitorEnter.testState != SuspendWithObjectMonitorEnter.TS_READY_TO_RESUME) {
                        try {
                            // wait for main to tell us when to exit threadLock
                            SuspendWithObjectMonitorEnter.barrierBlocker.wait(0);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                SuspendWithObjectMonitorEnter.logDebug("exit threadLock");
            }
        }
        //
        // Launch the contender thread:
        // - tries to grab the threadLock
        // - grabs threadLock
        // - releases threadLock
        //
        else if (getName().equals("contender")) {
            synchronized(SuspendWithObjectMonitorEnter.barrierLaunch) {
                // tell main we are running
                SuspendWithObjectMonitorEnter.testState = SuspendWithObjectMonitorEnter.TS_CONTENDER_RUNNING;
                SuspendWithObjectMonitorEnter.barrierLaunch.notify();
            }

            SuspendWithObjectMonitorEnter.logDebug("before enter threadLock");
            synchronized(SuspendWithObjectMonitorEnter.threadLock) {
                SuspendWithObjectMonitorEnter.logDebug("enter threadLock");

                SuspendWithObjectMonitorEnter.checkTestState(SuspendWithObjectMonitorEnter.TS_CALL_RESUME);
                SuspendWithObjectMonitorEnter.testState = SuspendWithObjectMonitorEnter.TS_CONTENDER_DONE;

                SuspendWithObjectMonitorEnter.logDebug("exit threadLock");
            }
        }
        //
        // Launch the resumer thread:
        // - tries to grab the threadLock (should not block!)
        // - grabs threadLock
        // - resumes the contended thread
        // - releases threadLock
        //
        else if (getName().equals("resumer")) {
            synchronized(SuspendWithObjectMonitorEnter.barrierResumer) {
                synchronized(SuspendWithObjectMonitorEnter.barrierLaunch) {
                    // tell main we are running
                    SuspendWithObjectMonitorEnter.testState = SuspendWithObjectMonitorEnter.TS_RESUMER_RUNNING;
                    SuspendWithObjectMonitorEnter.barrierLaunch.notify();
                }
                SuspendWithObjectMonitorEnter.logDebug("thread waiting");
                while (SuspendWithObjectMonitorEnter.testState != SuspendWithObjectMonitorEnter.TS_READY_TO_RESUME) {
                    try {
                        // wait for main to tell us when to continue
                        SuspendWithObjectMonitorEnter.barrierResumer.wait(0);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            SuspendWithObjectMonitorEnter.logDebug("before enter threadLock");
            synchronized(SuspendWithObjectMonitorEnter.threadLock) {
                SuspendWithObjectMonitorEnter.logDebug("enter threadLock");

                SuspendWithObjectMonitorEnter.checkTestState(SuspendWithObjectMonitorEnter.TS_READY_TO_RESUME);
                SuspendWithObjectMonitorEnter.testState = SuspendWithObjectMonitorEnter.TS_CALL_RESUME;

                // resume the contender thread so contender.join() can work
                SuspendWithObjectMonitorEnter.logDebug("before resume thread");
                int retCode = resumeThread(target);
                if (retCode != 0) {
                    throw new RuntimeException("error in JVMTI ResumeThread: " +
                                               "retCode=" + retCode);
                }
                SuspendWithObjectMonitorEnter.logDebug("resumed thread");

                SuspendWithObjectMonitorEnter.logDebug("exit threadLock");
            }
        }
    }
}
