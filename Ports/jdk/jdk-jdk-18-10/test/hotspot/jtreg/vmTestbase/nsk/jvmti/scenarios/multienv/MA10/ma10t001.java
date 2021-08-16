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

package nsk.jvmti.scenarios.multienv.MA10;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ma10t001 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new ma10t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    int status = Consts.TEST_PASSED;
    Log log = null;
    long timeout = 0;

    // tested thread
    ma10t001Thread testedThread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        log.display("Debugee started");

        testedThread = new ma10t001Thread("Debuggee Thread");
        testedThread.start();

        testedThread.startingBarrier.waitFor();
        status = checkStatus(status);
        testedThread.waitingBarrier.unlock();

        try {
            testedThread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Debugee finished");

        return checkStatus(status);
    }
}

/* =================================================================== */

class ma10t001Thread extends Thread {
    Wicket startingBarrier = new Wicket();
    Wicket waitingBarrier = new Wicket();

    public ma10t001Thread(String name) {
        super(name);
    }

    public synchronized void run() {
        startingBarrier.unlock();
        waitingBarrier.waitFor();
        try {
            meth1();
        } catch (Throwable e) {}
        try {
            meth2(0);
        } catch (Throwable e) {}
        try {
            meth3(2);
        } catch (Throwable e) {}
    }

    public static void meth1() throws Throwable {
        throw new ma10t001Throwable();
    }

    public int meth2(int i) throws ArithmeticException {
        return (10 / i);
    }

    public int meth3(int i) throws IndexOutOfBoundsException {
        int arr[] = {1};
        return arr[i];
    }

    static class ma10t001Throwable extends Throwable {
    }
}
