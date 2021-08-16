/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassType.invokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 *
 * The test checks up the method com.sun.jdi.ClassType.invokeMethod()
 *
 * Test checks up the following assertion:
 *   If the target VM is disconnected during the invoke (for example,
 *   through VirtualMachine.dispose()) the method invocation continues.
 */

public class invokemethod008 {

    private final static String prefix = "nsk.jdi.ClassType.invokeMethod.";
    private final static String debuggerName = prefix + "invokemethod008";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";
    public final static String SGNL_FINISH = "finish";
    public final static String SGNL_ABORT = "abort";

    public static int exitStatus;
    public static Log log;
    public static Debugee debugee;
    public static long waitTime;
    private static boolean isAborted = false;

    private ClassType testedClass;
    public final static String method2Invoke = "justMethod";

    public static void display(String msg) {
        log.display(msg);
    }

    public static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        invokemethod008 thisTest = new invokemethod008();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (Failure e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            if (!isAborted) {
                debugee.resume();
                debugee.quit();
            }
        }

        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure{

        debugee.VM().suspend();
        testedClass = (ClassType )debugee.classByName(debugeeName);

        display("\nTEST BEGINS");
        display("===========");

        InvokingThread invokeThrd = new InvokingThread();
        invokeThrd.start();

        // waiting READY signal from invoked method
        display("\nwaiting for signal \""
                        + SGNL_READY
                        + "\" from the invoked method");
        debugee.receiveExpectedSignal(SGNL_READY);

        display("\n" + method2Invoke + " has been invoked. Trying to dispose debugee's VM");

        AbortingThread abortingThrd = new AbortingThread();
        abortingThrd.start();

        debugee.VM().dispose();
        invokeThrd.waitVMDisconnect();

        try {
            invokeThrd.join();
        } catch(InterruptedException e) {
            throw new Failure("unexpected " + e);
        }

        if (isAborted) {
            complain("Debugee's VM disposing hung. Invoking was aborted!!!");
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("\nVirtual machine has been disposed");
            display("sending signal \"" + SGNL_FINISH + "\" to the invoked method");
            debugee.sendSignal(SGNL_FINISH);
        }

        display("\n=============");
        display("TEST FINISHES\n");
    }

    private BreakpointEvent waitForBreakpoint(BreakpointRequest brkp) {
        Event event = null;
        BreakpointEvent brkpEvent;

        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure("unexpected " + e);
        }

        if (!(event instanceof BreakpointEvent )) {
            throw new Failure("BreakpointEvent didn't arrive");
        }

        brkpEvent = (BreakpointEvent )event;
        return brkpEvent;
    }


    class InvokingThread extends Thread {
        volatile boolean isNotified = false;

        public void run() {
            display("");
            BreakpointRequest brkp = debugee.setBreakpoint(testedClass,
                                                    invokemethod008a.brkpMethodName,
                                                    invokemethod008a.brkpLineNumber);
            debugee.resume();

            debugee.sendSignal("");

            BreakpointEvent brkpEvent = waitForBreakpoint(brkp);
            ThreadReference thread = brkpEvent.thread();

            Value retValue;
            List<Value> params = new Vector<Value>();

            Method method = debugee.methodByName(testedClass, method2Invoke);
            display("\nInvoking debugee's method : " + method);

            try {
                retValue = testedClass.invokeMethod(thread, method, params, 0);
                if ( ((PrimitiveValue )retValue).intValue() == Consts.TEST_FAILED ) {
                    complain("VMDisconnectedException is not thrown");
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch(VMDisconnectedException e) {
                display("!!!expected VMDisconnectedException");
                notifyVMDisconnect();
            } catch(Exception e) {
                complain("Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
            debugee.resume();
        }

        synchronized void notifyVMDisconnect() {
            isNotified = true;
            notify();
        }

        synchronized void waitVMDisconnect() {
            try {
                if (!isNotified) {
                    display("\nwaiting VMDisconnectedException");
                    wait(waitTime);
                }
            } catch (InterruptedException e) {
                throw new Failure("unexpected " + e);
            }
        }
    }

    class AbortingThread extends Thread {

        public void run() {
            try {
                sleep(waitTime);
            } catch (InterruptedException e) {
                throw new Failure("unexpected " + e);
            }

            display("\n!!!out of wait time!!!");
            display("sending signal \"" + SGNL_ABORT + "\" to the invoked method");
            isAborted = true;
            debugee.sendSignal(SGNL_ABORT);
            debugee.resume();
            debugee.quit();
        }
    }

}
