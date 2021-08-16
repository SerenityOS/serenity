/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadReference.interrupt;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ThreadReference.                                             <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ThreadReference.interrupt()</code>         <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The case for testing includes two tested threads in a debuggee       <BR>
 * named thread2 and thread3, and a mirror of thread2 in a debugger.    <BR>
 * The method of testing is the comparison of "interrupted status" of  <BR>
 * both threads after the method Thread.interrupt() is applied to       <BR>
 * thread3 and its "mirroring" method ThreadReference.interrupt() is    <BR>
 * applied to the ThreadReference object mirroring thread2.             <BR>
 * The test is passed if both 'interrupted status" are equal,           <BR>
 * otherwise the test failed.                                           <BR>
 *                                                                      <BR>
 * The test consists of two cases as follows:                           <BR>
 * - both debuggee's threads are locked up at synchronized block and    <BR>
 *   are not suspended;                                                 <BR>
 * - both debuggee's threads are locked up at synchronized block and    <BR>
 *   are suspended by java.lang.Thread.suspend() method;                <BR>
 */

public class interrupt001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;

    static final int FAILED = 2;

    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String sHeader1 = "\n==> nsk/jdi/ThreadReference/interrupt/interrupt001  ", sHeader2 = "--> debugger: ",
            sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main(String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new interrupt001().runThis(argv, out);
    }

    //--------------------------------------------------   log procedures

    private static Log logHandler;

    private static void log1(String message) {
        logHandler.display(sHeader1 + message);
    }

    public static void log2(String message) {
        logHandler.display(sHeader2 + message);
    }

    private static void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }

    //  ************************************************    test parameters

    private String debuggeeName = "nsk.jdi.ThreadReference.interrupt.interrupt001a";

    //String mName = "nsk.jdi.ThreadReference.interrupt";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler argsHandler;

    static VirtualMachine vm;

    static Debugee debuggee;

    static IOPipe pipe;

    static int waitTime;

    static int testExitCode = PASSED;

    static final int returnCode0 = 0;

    static final int returnCode1 = 1;

    static final int returnCode2 = 2;

    static final int returnCode3 = 3;

    static final int returnCode4 = 4;

    static final String EQUALS_INTERRUPTED = "Statuses of threads are equal: interrupted";

    static final String ONLY_CHECKED_INTERRUPTED = "Only checked Thread2 is interrupted";

    static final String CHECKED_NOT_INTERRUPTED = "Checked Thread2 is not interrupted";

    static final String EQUALS_NOT_INTERRUPTED = "Statuses of threads are equal: not interrupted";

    static final int MAX_CASE = 2;

    //------------------------------------------------------ methods

    private int runThis(String argv[], PrintStream out) {

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        Binder binder = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);
        }

        waitTime = argsHandler.getWaitTime();
        pipe = debuggee.createIOPipe();

        debuggee.redirectStderr(out);
        log2(debuggeeName + " debuggee launched");
        debuggee.resume();

        try {
            debuggee.receiveExpectedSignal("ready");
        } catch (Failure e) {
            debuggee.quit();
            throw e;
        }

        vm = debuggee.VM();

        //------------------------------------------------------  testing section

        log1("      TESTING BEGINS");

        for (int i = 0; i < MAX_CASE; i++) {

            debuggee.sendSignal("newcheck");
            try {
                debuggee.receiveExpectedSignal("checkready");
            } catch (Failure e) {
                debuggee.quit();
                throw e;
            }

            log1("BEGIN OF CASE #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part
            switch (i) {
            case 0:
                executeCase(i, "Thread02");
                break;

            case 1:
                executeCase(i, "Thread12");
                break;
            }
        }
        log1("      TESTING ENDS");

        //--------------------------------------------------   test summary section
        //-------------------------------------------------    standard end section

        debuggee.sendSignal("quit");
        log2("waiting for the debuggee to finish ...");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("debuggee returned UNEXPECTED exit status: " + status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log2("debuggee returned expected exit status: " + status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;

    }

    private void executeCase(int testCase, String threadName2) {
        ThreadReference thread2 = debuggee.threadByName(threadName2);
        if (thread2 == null) {
            debuggee.quit();
            throw new TestBug("ERROR: Not found ThreadReference for name :" + threadName2);
        }

        log2("......interrupting the thread2");
        thread2.interrupt();

        log2("......instructing main thread to check up threads' interrupted statuses");
        debuggee.sendSignal("check_interruption");
        try {
            debuggee.receiveExpectedSignal(EQUALS_INTERRUPTED);
            log2("CASE #" + testCase + " PASSED: " + EQUALS_INTERRUPTED);
        } catch (Failure e) {
            log3("CASE #" + testCase + " FAILED: " + e.getMessage());
            testExitCode = returnCode1;
        }

        log2("      forcing the main thread to leave synchronized block");
        debuggee.sendSignal("continue");
        try {
            debuggee.receiveExpectedSignal("docontinue");
        } catch (Failure e) {
            debuggee.quit();
            throw e;
        }
    }
}
