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
 * @bug 8167108 8265153
 * @summary The test checks that ThreadInfo.getLockOwnerName() returns a
 *   non-null string for a blocked thread and then makes repeated calls
 *   to getThreadInfo() and ThreadInfo.getLockOwnerName() until the thread
 *   has exited.
 * @requires vm.jvmti
 * @run main/othervm/native -agentlib:GetLockOwnerName GetLockOwnerName
 */

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

//
// main               blocker           contender            releaser
// =================  ================  ===================  ================
// launch blocker
// <launch returns>   blocker running
// launch contender   enter threadLock
// <launch returns>   wait for notify   contender running
// launch releaser    :                 block on threadLock
// <launch returns>   :                 :                    releaser running
// wait for           :                 :                    wait for notify
//  contended enter   :                 :                    :
// <ready to test>    :                 :                    :
// getThreadInfo      :                 :                    :
// verify contender   :                 :                    :
//  lock owner name   :                 :                    :
//  is "blocker"      :                 :                    :
// notify releaser    :                 :                    :
// loop until thread  :                 :                    wait finishes
//  is NULL           :                 :                    notify blocker
//   get thread info  wait finishes     :                    releaser exits
//   get lock owner   exit threadLock   :                    :
//    name            blocker exits     enter threadLock     :
// join releaser      :                 exit threadLock      <exit finishes>
// <join returns>     :                 contender exits
// join blocker       <exit finishes>   :
// <join returns>                       :
// join contender                       <exit finishes>
// <join returns>
//

public class GetLockOwnerName {
    private static final String AGENT_LIB = "GetLockOwnerName";

    private static final int DEF_TIME_MAX = 60;    // default max # secs to test
    private static final int JOIN_MAX     = 30;    // max # secs to wait for join

    public static final int TS_INIT              = 1;  // initial testState
    public static final int TS_BLOCKER_RUNNING   = 2;  // blocker is running
    public static final int TS_CONTENDER_RUNNING = 3;  // contender is running
    public static final int TS_RELEASER_RUNNING  = 4;  // releaser is running
    public static final int TS_CONTENDER_BLOCKED = 5;  // contender is blocked
    public static final int TS_READY_TO_RELEASE  = 6;  // ready to release the blocker
    public static final int TS_DONE_BLOCKING     = 7;  // done blocking threadLock
    public static final int TS_CONTENDER_DONE    = 8;  // contender has run; done

    public static Object barrierLaunch = new Object();   // controls thread launch
    public static Object barrierBlocker = new Object();  // controls blocker
    public static Object barrierReleaser = new Object();  // controls releaser
    public static Object threadLock = new Object();      // testing object

    public static long count = 0;
    public static boolean printDebug = false;
    public volatile static int testState;

    private static void log(String msg) { System.out.println(msg); }

    native static int wait4ContendedEnter(GetLockOwnerNameWorker thr);

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

        int retCode = run(timeMax, System.out);
        if (retCode != 0) {
            throw new RuntimeException("Test failed with retCode=" + retCode);
        }
    }

    public static void logDebug(String mesg) {
        if (printDebug) {
            System.err.println(Thread.currentThread().getName() + ": " + mesg);
        }
    }

    public static void usage() {
        System.err.println("Usage: " + AGENT_LIB + " [-p][time_max]");
        System.err.println("where:");
        System.err.println("    -p        print debug info");
        System.err.println("    time_max  max looping time in seconds");
        System.err.println("              (default is " + DEF_TIME_MAX +
                           " seconds)");
        System.exit(1);
    }

    public static int run(int timeMax, PrintStream out) {
        return (new GetLockOwnerName()).doWork(timeMax, out);
    }

    public static void checkTestState(int exp) {
        if (testState != exp) {
            System.err.println("Failure at " + count + " loops.");
            throw new InternalError("Unexpected test state value: "
                + "expected=" + exp + " actual=" + testState);
        }
    }

    public int doWork(int timeMax, PrintStream out) {
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

        GetLockOwnerNameWorker blocker;    // blocker thread
        GetLockOwnerNameWorker contender;  // contender thread
        GetLockOwnerNameWorker releaser;   // releaser thread

        System.out.println("About to execute for " + timeMax + " seconds.");

        long start_time = System.currentTimeMillis();
        while (System.currentTimeMillis() < start_time + (timeMax * 1000)) {
            count++;
            testState = TS_INIT;  // starting the test loop

            // launch the blocker thread
            synchronized (barrierLaunch) {
                blocker = new GetLockOwnerNameWorker("blocker");
                blocker.start();

                while (testState != TS_BLOCKER_RUNNING) {
                    try {
                        barrierLaunch.wait();  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // launch the contender thread
            synchronized (barrierLaunch) {
                contender = new GetLockOwnerNameWorker("contender");
                contender.start();

                while (testState != TS_CONTENDER_RUNNING) {
                    try {
                        barrierLaunch.wait();  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // launch the releaser thread
            synchronized (barrierLaunch) {
                releaser = new GetLockOwnerNameWorker("releaser");
                releaser.start();

                while (testState != TS_RELEASER_RUNNING) {
                    try {
                        barrierLaunch.wait();  // wait until it is running
                    } catch (InterruptedException ex) {
                    }
                }
            }

            // wait for the contender thread to block
            logDebug("before contended enter wait");
            int retCode = wait4ContendedEnter(contender);
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI GetThreadState " +
                                           "or GetCurrentContendedMonitor " +
                                           "retCode=" + retCode);
            }
            testState = TS_CONTENDER_BLOCKED;
            logDebug("done contended enter wait");

            //
            // At this point, all of the child threads are running
            // and we can get to meat of the test:
            //
            // - query the contender thread and verify that it is blocked
            //   by the blocker thread
            // - tell the releaser thread to release the blocker thread
            // - continue to query the contender thread until it exits
            //
            long id = 0;
            ThreadInfo info = null;
            int lateCount = 0;

            checkTestState(TS_CONTENDER_BLOCKED);

            id = contender.getId();
            info = mbean.getThreadInfo(id, 0);
            String name = info.getLockOwnerName();

            if (name == null) {
                out.println("Failure at " + count + " loops.");
                throw new RuntimeException("ThreadInfo.GetLockOwnerName() "
                                           + "returned null name for "
                                           + "contender.");
            } else if (!name.equals("blocker")) {
                out.println("Failure at " + count + " loops.");
                throw new RuntimeException("name='" + name + "': name "
                                           + "should be blocker.");
            } else {
                logDebug("ThreadInfo.GetLockOwnerName() returned blocker.");
            }

            synchronized (barrierReleaser) {
                // tell releaser thread to release the blocker thread
                testState = TS_READY_TO_RELEASE;
                barrierReleaser.notify();
            }

            while (true) {
                // maxDepth == 0 requires no safepoint so alternate.
                int maxDepth = ((count % 1) == 1) ? Integer.MAX_VALUE : 0;
                info = mbean.getThreadInfo(id, maxDepth);
                if (info == null) {
                    // the contender has exited
                    break;
                }
                name = info.getLockOwnerName();
                // We can't verify that name == null here because contender
                // might be slow leaving the threadLock monitor.
                lateCount++;
            }
            logDebug("made " + lateCount + " late calls to getThreadInfo() " +
                     "and info.getLockOwnerName().");

            try {
                releaser.join(JOIN_MAX * 1000);
                if (releaser.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("releaser thread is stuck");
                }
                blocker.join(JOIN_MAX * 1000);
                if (blocker.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("blocker thread is stuck");
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

class GetLockOwnerNameWorker extends Thread {
    public GetLockOwnerNameWorker(String name) {
        super(name);
    }

    public void run() {
        GetLockOwnerName.logDebug("thread running");

        //
        // The blocker thread:
        // - grabs threadLock
        // - holds threadLock until we tell it let go
        // - releases threadLock
        //
        if (getName().equals("blocker")) {
            // grab threadLock before we tell main we are running
            GetLockOwnerName.logDebug("before enter threadLock");
            synchronized(GetLockOwnerName.threadLock) {
                GetLockOwnerName.logDebug("enter threadLock");

                GetLockOwnerName.checkTestState(GetLockOwnerName.TS_INIT);

                synchronized(GetLockOwnerName.barrierBlocker) {
                    synchronized(GetLockOwnerName.barrierLaunch) {
                        // tell main we are running
                        GetLockOwnerName.testState = GetLockOwnerName.TS_BLOCKER_RUNNING;
                        GetLockOwnerName.barrierLaunch.notify();
                    }
                    GetLockOwnerName.logDebug("thread waiting");
                    while (GetLockOwnerName.testState != GetLockOwnerName.TS_DONE_BLOCKING) {
                        try {
                            // wait for main to tell us when to exit threadLock
                            GetLockOwnerName.barrierBlocker.wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                GetLockOwnerName.logDebug("exit threadLock");
            }
        }
        //
        // The contender thread:
        // - tries to grab the threadLock
        // - grabs threadLock
        // - releases threadLock
        //
        else if (getName().equals("contender")) {
            synchronized(GetLockOwnerName.barrierLaunch) {
                // tell main we are running
                GetLockOwnerName.testState = GetLockOwnerName.TS_CONTENDER_RUNNING;
                GetLockOwnerName.barrierLaunch.notify();
            }

            GetLockOwnerName.logDebug("before enter threadLock");
            synchronized(GetLockOwnerName.threadLock) {
                GetLockOwnerName.logDebug("enter threadLock");

                GetLockOwnerName.checkTestState(GetLockOwnerName.TS_DONE_BLOCKING);
                GetLockOwnerName.testState = GetLockOwnerName.TS_CONTENDER_DONE;

                GetLockOwnerName.logDebug("exit threadLock");
            }
        }
        //
        // The releaser thread:
        // - tries to grab the barrierBlocker (should not block!)
        // - grabs barrierBlocker
        // - releases the blocker thread
        // - releases barrierBlocker
        //
        else if (getName().equals("releaser")) {
            synchronized(GetLockOwnerName.barrierReleaser) {
                synchronized(GetLockOwnerName.barrierLaunch) {
                    // tell main we are running
                    GetLockOwnerName.testState = GetLockOwnerName.TS_RELEASER_RUNNING;
                    GetLockOwnerName.barrierLaunch.notify();
                }
                GetLockOwnerName.logDebug("thread waiting");
                while (GetLockOwnerName.testState != GetLockOwnerName.TS_READY_TO_RELEASE) {
                    try {
                        // wait for main to tell us when to continue
                        GetLockOwnerName.barrierReleaser.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }

            GetLockOwnerName.logDebug("before enter barrierBlocker");
            synchronized (GetLockOwnerName.barrierBlocker) {
                GetLockOwnerName.logDebug("enter barrierBlocker");

                // tell blocker thread to exit threadLock
                GetLockOwnerName.testState = GetLockOwnerName.TS_DONE_BLOCKING;
                GetLockOwnerName.barrierBlocker.notify();

                GetLockOwnerName.logDebug("released blocker thread");
                GetLockOwnerName.logDebug("exit barrierBlocker");
            }
        }
    }
}
