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
public class crstepreq010a {

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

    //---------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    static void methodForCommunication() {
        int i = instruction; // crstepreq010.lineForBreak
        int curInstruction = i;
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static final int maxCase = 5;
    static Object waitnotifyObj = new Object();
    static Thread thread1;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = argHandler.createDebugeeLog();

        display("debuggee started!");

        label0:
        for (int testCase = 0; testCase < maxCase && instruction != quit; testCase++) {

            thread1 = new Thread0crstepreq010a(testCase);
            threadStart(thread1);
            threadJoin (thread1, testCase);

        }

        display("debuggee exits");
        System.exit(PASSED + PASS_BASE);
    }

    //--------------------------------------------------------- test specific methodss

    static void threadJoin (Thread t, int number) {
        try {
            t.join();
        } catch ( InterruptedException e ) {
            exitCode = FAILED;
            complain("Case #" + number + ": caught unexpected InterruptedException while waiting for thread finish" );
        }
    }

    static int threadStart (Thread t) {
        synchronized (waitnotifyObj) {
            t.start();
            try {
                waitnotifyObj.wait();
            } catch (InterruptedException e) {
                exitCode = FAILED;
                complain("Caught unexpected InterruptedException while waiting for thread start" );
                return FAILED;
            }
        }
        return PASSED;
    }

}

//--------------------------------------------------------- test specific classes

/**
 * This thread will be suspended on breakpoint. No locks are used.
 */
class Thread0crstepreq010a extends Thread {
    int testCase;

    public Thread0crstepreq010a (int testCase) {
        super("thread" + testCase);
        this.testCase = testCase;
    }

    public void run() {
        crstepreq010a.display("enter thread " + getName());
        synchronized(crstepreq010a.waitnotifyObj) {
            crstepreq010a.waitnotifyObj.notifyAll();
        }

        crstepreq010a.methodForCommunication();
        caseRun();
        crstepreq010a.display("exit thread " + getName());
    }

    void caseRun() {
        int i;
        try {
             switch (testCase) {
                 case 0:
                     i = m00(1); // crstepreq010.checkedLines[0][0-2]
                     i = m00(2);
                     break;

                 case 1:
                     i = m00(1); i = m01(2);
                     break;

                 case 2:
                     i = m02(-1);
                     break;

                 case 3:
                     i = m03(1);
                     break;

                 case 4:
                     m04(); // crstepreq010.checkedLines[4][0-2]
                     break;
             }
        } catch (DummyException e) {
            crstepreq010a.display("DummyException was caught for testCase # " + testCase);
        }
    }

    int m00 (int arg) {
        return arg++;
    }

    int m01 (int arg) {
        return m00(arg); // crstepreq010.checkedLines[1][0-2]
    }

    int m02 (int arg) throws DummyException {
        if (arg < 0) {
            throw new DummyException(); // crstepreq010.checkedLines[2][0-2]
        }
        return arg++;
    }

    int m03 (int arg) throws DummyException {
        return m02(arg); // crstepreq010.checkedLines[3][0-2]
    }

    void m04 () {} // crstepreq010.checkedLines[4][2]

    class DummyException extends Exception {}
}
