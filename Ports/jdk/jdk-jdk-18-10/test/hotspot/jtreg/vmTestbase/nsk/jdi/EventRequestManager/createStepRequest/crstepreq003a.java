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
public class crstepreq003a {

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

    static Object waitnotifyObj = new Object();

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = new Log(System.out, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        display("debuggee started!");

        label0:
        for (int testCase = 0; instruction != quit; testCase++) {

            switch (testCase) {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ test case section
                case 0:

                    Thread thread0 = new Thread0crstepreq003a("thread0");
                    threadStart(thread0);
                    threadJoin (thread0, "0");
                    break;

                case 1:

                    Thread thread1 = new Thread0crstepreq003a("thread1");
                    threadStart(thread1);
                    threadJoin (thread1, "1");
                    break;

                case 2:

                    Thread thread2 = new Thread0crstepreq003a("thread2");
                    threadStart(thread2);
                    threadJoin (thread2, "2");

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ end of section

                default:
                    instruction = quit;
                    break;
            }

//            display("call methodForCommunication() #0");
//            methodForCommunication();
            if (instruction == quit)
                break;
        }

        display("debuggee exits");
        System.exit(PASSED + PASS_BASE);
    }

    //--------------------------------------------------------- test specific methodss

    static void threadJoin (Thread t, String number) {
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

    static void breakInThread() {
        Object dummy = new Object();
        synchronized(dummy) { // crstepreq003.lineForBreakInThread
            int i = 1;        // This is line of step event's location for STEP_OVER and STEP_INTO -- crstepreq003.checkedLines[0-1]
        }
    }

    //---------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    private static void methodForCommunication() {
        int i = instruction;
        int curInstruction = i;
    }
}

//--------------------------------------------------------- test specific classes

/**
 * This thread will be suspended on breakpoint. No locks are used.
 */
class Thread0crstepreq003a extends Thread {
    public Thread0crstepreq003a (String name) {
        super(name);
    }

    public void run() {
        crstepreq003a.display("enter thread " + getName());

        synchronized(crstepreq003a.waitnotifyObj) {
            crstepreq003a.waitnotifyObj.notifyAll();
        }

        crstepreq003a.display("call breakInThread()");
        crstepreq003a.breakInThread();

        crstepreq003a.display("exit thread " + getName()); // This is line of step event's location for STEP_OUT -- crstepreq003.checkedLines[2]
    }
}
