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

package nsk.jvmti.scenarios.capability.CM03;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;
import nsk.share.jvmti.ThreadState;


public class cm03t001 extends DebugeeClass {

    // Thread states used in this test.
    public static final String stateInit = "init";
    public static final String stateDebuggeeStarted = "debuggeeStarted";
    public static final String stateJvmtiInited = "jvmtiInited";
    public static final String stateDebuggeeWaiting = "debuggeeWaiting";
    public static final String stateDebuggeeThreadDeath = "debuggeeThreadDeath";

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new cm03t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    int status = Consts.TEST_PASSED;
    static long timeout = 0;

    // tested thread
    cm03t001Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        timeout = argHandler.getWaitTime() * 60000; // milliseconds
        log.display("Timeout = " + timeout + " ms.");

        ThreadState threadState = new ThreadState(stateInit, 15000);
        thread = new cm03t001Thread("Debuggee Thread", threadState);
        thread.start();

        // Wait for debuggee thread to start. Then init jvmti.
        threadState.waitForState(stateDebuggeeStarted);
        status = checkStatus(status);

        // Notify debugge that we have inited jvmti and then wait for debugge to be ready.
        threadState.setAndWait(stateJvmtiInited, stateDebuggeeWaiting);

        // Execute jvmti commands and wait for debugge to get a ThreadDeath exception.
        status = checkStatus(status);
        threadState.waitForState(stateDebuggeeThreadDeath);

        // Stop debuggee thread
        thread.letItGo();
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        // Verify all jvmti events have been received.
        status = checkStatus(status);
        log.display("Testing sync: debuggee exit");
        return status;
    }
}

class cm03t001Thread extends Thread {
    ThreadState threadState;
    private volatile boolean waitingFlag = false;

    public cm03t001Thread(String name, ThreadState threadState) {
        super(name);
        this.threadState = threadState;
    }

    public void run() {
        Object o = new Object();
        int i = 1;
        long l = 2;
        float f = 3.0F;
        double d = 4.0;

        // Signal that debuggee thread is started and then wait for jvmti to be initied.
        threadState.setAndWait(cm03t001.stateDebuggeeStarted, cm03t001.stateJvmtiInited);

        try {
            delay();
        } catch (ThreadDeath td) {
            threadState.setState(cm03t001.stateDebuggeeThreadDeath);
            // run waitUntilQuit() again to make sure we have touched waitingFlag.
            // In function dealay() we may have got a ThreadDeath exception
            // before we could call waitUntilQuit.
            waitUntilQuit();
        }
    }

    // jvmti commands will be run when we are in this function.
    private void delay() {
        // Notify main thread that we are ready for jvmti commands.
        threadState.setState(cm03t001.stateDebuggeeWaiting);
        waitUntilQuit();
    }

    private void waitUntilQuit() {
        long dummy = 0;
        while (!waitingFlag) {
            dummy++;
        }
    }

    public void letItGo() {
        waitingFlag = true;
    }
}
