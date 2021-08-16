/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.EventRequestManager.createStepRequest;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * The debugged application of the test.
 */
public class crstepreq004a {

    //----------------------------------------------------- immutable common fields

    static final int PASSED    = 0;
    static final int FAILED    = 2;
    static final int PASS_BASE = 95;
    static final int quit      = -1;

    static int instruction = 1;
    static int lineForComm = 2;
    static int exitCode    = PASSED;

    private static ArgumentHandler argHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static Object lockObj       = new Object();
    static Object lockObj1      = new Object();
    private static volatile boolean isFirstThreadReady = false;
    private static volatile boolean isSecondThreadReady = false;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = new Log(System.out, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        display("debuggee started!");

        label0:
        for (int testCase = 0; instruction != quit; testCase++) {

            switch (testCase) {
                case 0:
                case 1:
                case 2:
                    runTestCase(testCase);
                    break;
                default:
                    instruction = quit;
                    break;
            }

            if (instruction == quit)
                break;
        }

        display("debuggee exits");
        System.exit(PASSED + PASS_BASE);
    }

    //--------------------------------------------------------- test specific methodss

    private static void runTestCase(int testCaseId) {
        isFirstThreadReady = false;
        isSecondThreadReady = false;
        Thread thread1 = new Thread1crstepreq004a("thread1");
        Thread thread2 = new Thread2crstepreq004a("thread2");
        synchronized (lockObj) {
            thread1.start();
            while (!isFirstThreadReady) {
                shortSleep();
            }
            thread2.start();
            while (!isSecondThreadReady) {
                shortSleep();
            }

            display("call methodForCommunication() #" + testCaseId);
            methodForCommunication();
        }
        threadJoin(thread1, "1");
        threadJoin(thread2, "2");
    }

    static void threadJoin (Thread t, String number) {
        try {
            t.join();
        } catch ( InterruptedException e ) {
            exitCode = FAILED;
            complain("Case #" + number + ": caught unexpected InterruptedException while waiting for thread finish" );
        }
    }

    //---------------------------------------------------------- immutable common methods

    static void shortSleep() {
        try {
            Thread.currentThread().sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    private static void methodForCommunication() {
        int i = instruction;
        int curInstruction = i;  // crstepreq004.lineForBreakInThread
    }

    static void lockAndNotify1() {
        synchronized(lockObj1) {
           isFirstThreadReady = true;
           synchronized(lockObj) {
           }
        }
    }

    static void lockAndNotify2() {
        isSecondThreadReady = true;
        synchronized(lockObj1) {     // thread is waiting here the lock when step request is created.
            int i = 1;               // This is line of step event for STEP_INTO and STEP_OVER -- crstepreq004.checkedLines[0-1]
        } // crstepreq004.checkedLinesAlt[0-1]
    }
}

//--------------------------------------------------------- test specific classes

/**
 * First thread which owns and locks the crstepreq004a.lockObj1 monitor .
 */
class Thread1crstepreq004a extends Thread {
    public Thread1crstepreq004a (String name) {
        super(name);
    }

    public void run() {
        crstepreq004a.display("enter thread " +  getName());
        crstepreq004a.lockAndNotify1();
        crstepreq004a.display("exit thread " +  getName());
    }
}

/**
 * Second thread which who owns the crstepreq004a.lockObj1 monitor .
 */
class Thread2crstepreq004a extends Thread {
    public Thread2crstepreq004a (String name) {
        super(name);
    }

    public void run() {
        crstepreq004a.display("enter thread " +  getName());
        crstepreq004a.lockAndNotify2();
        crstepreq004a.display("exit thread " +  getName()); // This is line of step event for STEP_OUT -- crstepreq004.checkedLines[2] checkedLinesAlt[2].
    }
}
