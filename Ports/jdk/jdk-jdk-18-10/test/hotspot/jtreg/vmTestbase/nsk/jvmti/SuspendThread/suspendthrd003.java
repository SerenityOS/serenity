/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.SuspendThread;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class suspendthrd003 extends DebugeeClass {
    private final static String AGENT_LIB = "suspendthrd003";
    private final static int DEF_TIME_MAX = 30;  // default max # secs to test

    public static Wicket mainEntrance;

    // load native library if required
    static {
        System.loadLibrary(AGENT_LIB);
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new suspendthrd003().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // tested thread
    suspendthrd003Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        String[] args = argHandler.getArguments();
        int timeMax = 0;
        if (args.length == 0) {
            timeMax = DEF_TIME_MAX;
        } else {
            try {
                timeMax = Integer.parseUnsignedInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.err.println("'" + args[0] + "': invalid timeMax value.");
                usage();
            }
        }

        System.out.println("About to execute for " + timeMax + " seconds.");

        long count = 0;
        int res = -1;
        long start_time = System.currentTimeMillis();
        while (System.currentTimeMillis() < start_time + (timeMax * 1000)) {
            count++;

            // Original suspendthrd001 test block starts here:
            //
            // create tested thread
            // Note: Cannot use TestedThread-N for thread name since
            // the agent has to know the thread's name.
            thread = new suspendthrd003Thread("TestedThread");
            mainEntrance = new Wicket();

            // run tested thread
            log.display("Starting tested thread");
            try {
                thread.start();
                // SP1-w - wait for TestedThread-N to be ready
                mainEntrance.waitFor();

                // testing sync
                log.display("Sync: thread started");
                // SP2.1-w - wait for agent thread
                // SP3.1-n - notify to start test
                // SP5.1-w - wait while testing
                status = checkStatus(status);
            } finally {
                // let thread to finish
                thread.letFinish();
            }

            // wait for thread to finish
            log.display("Finishing tested thread");
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new Failure(e);
            }

            // testing sync
            log.display("Sync: thread finished");
            // SP4.1-w - second wait for agent thread
            // SP6.1-n - notify to end test
            // SP7.1 - wait for agent end
            status = checkStatus(status);

            //
            // Original suspendthrd001 test block ends here.

            if (status != Consts.TEST_PASSED) {
                break;
            }

            resetAgentData();  // reset for another iteration
        }

        System.out.println("Executed " + count + " loops in " + timeMax +
                           " seconds.");

        return status;
    }

    public static void usage() {
        System.err.println("Usage: " + AGENT_LIB + " [time_max]");
        System.err.println("where:");
        System.err.println("    time_max  max looping time in seconds");
        System.err.println("              (default is " + DEF_TIME_MAX +
                           " seconds)");
        System.exit(1);
    }
}

/* =================================================================== */

// basic class for tested threads
class suspendthrd003Thread extends Thread {
    private volatile boolean shouldFinish = false;

    // make thread with specific name
    public suspendthrd003Thread(String name) {
        super(name);
    }

    // run thread continuously
    public void run() {
        // run in a loop
        // SP1-n - tell main we are ready
        suspendthrd003.mainEntrance.unlock();
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

    // let thread to finish
    public void letFinish() {
        shouldFinish = true;
    }
}
