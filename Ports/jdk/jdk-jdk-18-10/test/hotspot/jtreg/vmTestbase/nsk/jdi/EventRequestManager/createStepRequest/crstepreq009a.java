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
public class crstepreq009a {

    //------------------------------------------------------ immutable common fields

    static final int PASSED    = 0;
    static final int FAILED    = 2;
    static final int PASS_BASE = 95;
    static final int quit      = -1;

    static int instruction = 1;
    static int lineForComm = 2;
    static int exitCode    = PASSED;

    private static ArgumentHandler argHandler;
    private static Log log;

    //----------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    static void methodForCommunication() {
        int i = instruction; // crstepreq009.lineForBreak
        int curInstruction = i;
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static final int maxCase = 3;
    static Object waitnotifyObj = new Object();
    static Thread thread1;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = argHandler.createDebugeeLog();

        display("debuggee started!");

        label0:
        for (int testCase = 0; testCase < maxCase && instruction != quit; testCase++) {

            thread1 = new Thread0crstepreq009a(testCase);
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
class Thread0crstepreq009a extends Thread {
    int testCase;

    public Thread0crstepreq009a (int testCase) {
        super("thread" + testCase);
        this.testCase = testCase;
    }

    public void run() {
        crstepreq009a.display("enter thread " + getName());
        synchronized(crstepreq009a.waitnotifyObj) {
            crstepreq009a.waitnotifyObj.notifyAll();
        }

        crstepreq009a.methodForCommunication();
        caseRun();
        crstepreq009a.display("exit thread " + getName());
    }

    void caseRun() {
        int i;
        switch (testCase) {
            case 0:
                i = m01(1); // crstepreq009.checkedLines[0][2]
                i = m01(2);
                break;

            case 1:
                i = m02(1);
                break;

            case 2:
                i = m04(-2);
                break;

        }
    }

    int m00 (int arg) {
        return arg++; // crstepreq009.checkedLines[0-2][0]
    }

    int m01 (int arg) {
        return m00(arg); // crstepreq009.checkedLines[0][1]
    }

    int m02 (int arg) {
        int j = m00(arg); return m00(arg); } // crstepreq009.checkedLines[1][1-2]

    int m03 (int arg) throws DummyException {
        arg = m00(arg); if (arg < 0) { throw new DummyException(); };  // crstepreq009.checkedLines[2][1-2]
        return arg++;
    }

    int m04 (int arg) {
        int j = 0;
        try {
            j = m03(arg) + 1;
        } catch (DummyException e) {
            crstepreq009a.display("DummyException was caught for testCase # " + testCase);
        }
        return j;
    }

    class DummyException extends Exception {}
}
