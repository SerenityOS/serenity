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

package nsk.jvmti.scenarios.capability.CM01;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;
import nsk.share.jvmti.ThreadState;

public class cm01t007 extends DebugeeClass {

    // Thread states used in this test.
    public static final String stateInit = "init";
    public static final String stateDebuggeeWaiting = "debuggeWaiting";

    // load native library if required
    static {
        loadLibrary("cm01t007");
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new cm01t007().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    int status = Consts.TEST_PASSED;
    static long timeout = 0;

    // tested thread
    cm01t007Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60000; // milliseconds
        log.display("Timeout = " + timeout + " msc.");

        ThreadState threadState = new ThreadState(stateInit, 15000);
        thread = new cm01t007Thread("Debuggee Thread", threadState);
        thread.start();

        // Wait for debuggee thread to be ready.
        threadState.waitForState(stateDebuggeeWaiting);

        log.display("Testing sync: thread ready");
        if ((status = checkStatus(status)) != Consts.TEST_PASSED) {
            return status;
        }

        log.display("Testing sync: thread finish");
        thread.letFinish();

        // wait for thread finish
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Testing sync: debuggee exit");
        return status;
    }
}

/* =================================================================== */

class cm01t007Thread extends Thread {
    ThreadState threadState;
    private volatile boolean waitingFlag = false;

    public cm01t007Thread(String name, ThreadState threadState) {
        super(name);
        this.threadState = threadState;
    }

    public void run() {
        Object o = new Object();
        int i = 1;
        long l = 2;
        float f = 3.0F;
        double d = 4.0;

        delay();
    }

    private void delay() {
        // Signal that debugge thread is ready for jvmti commands.
        threadState.setState(cm01t007.stateDebuggeeWaiting);

        long dummy = 0;
        while (!waitingFlag) {
            dummy++;
        }
    }

    public void letFinish() {
        waitingFlag = true;
    }
}
