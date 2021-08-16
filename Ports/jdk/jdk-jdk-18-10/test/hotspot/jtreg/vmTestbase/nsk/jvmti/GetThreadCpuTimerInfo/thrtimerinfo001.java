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

package nsk.jvmti.GetThreadCpuTimerInfo;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

/** Debuggee class for this test. */
public class thrtimerinfo001 extends DebugeeClass {

    /** Load native library if required. */
    static {
        loadLibrary("thrtimerinfo001");
    }

    /** Run test from command line. */
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    /** Run test from JCK-compatible environment. */
    public static int run(String argv[], PrintStream out) {
        return new thrtimerinfo001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    /** Run debuggee. */
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        thrtimerinfo001Thread thread = new thrtimerinfo001Thread("TestedThread");

        // sync before thread started
        log.display("Sync: tested thread created");
        status = checkStatus(status);

        // start and finish tested thread
        try {
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            throw new Failure("Main thread interrupted while waiting for tested thread:\n\t"
                                + e);
        }

        // sync after thread finished
        log.display("Sync: tested thread started and finished");
        status = checkStatus(status);

        return status;
    }
}

/* =================================================================== */

/** Class for tested thread. */
class thrtimerinfo001Thread extends Thread {
    /** Make thread with specific name. */
    public thrtimerinfo001Thread(String name) {
        super(name);
    }

    /** Run some code. */
    public void run() {
        // do something
        int n = 1000;
        int s = 0;
        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                s += i * 10;
            } else {
                s -= i * 10;
            }
        }
    }
}
