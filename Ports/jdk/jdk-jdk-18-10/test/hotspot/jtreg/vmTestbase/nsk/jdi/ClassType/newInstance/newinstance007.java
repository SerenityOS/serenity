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

package nsk.jdi.ClassType.newInstance;

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
 * The test checks up the method com.sun.jdi.ClassType.newInstance()
 *
 * Test checks up the following assertion:
 *    ...all threads in the target VM are resumed while the method is
 *    being invoked if they were previously suspended by an event or by
 *    VirtualMachine.suspend() or ThreadReference.suspend().
 *
 * This checking is implemented for three cases:
 *    - when the tested thread is suspended by ThreadReference.suspend();
 *    - when the tested thread is suspended by VirtualMachine.suspend();
 *    - when the tested thread is suspended by an event.
 */

public class newinstance007 {

    private final static String prefix = "nsk.jdi.ClassType.newInstance.";
    private final static String debuggerName = prefix + "newinstance007";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";
    public final static String SGNL_STRTHRD = "start_thread";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;

    private ClassType testedClass;
    private ThreadReference thread;

    class TestRuntimeException extends RuntimeException {
        TestRuntimeException(String msg) {
            super("TestRuntimeException: " + msg);
        }
    }

    private static void display(String msg) {
        log.display(msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        newinstance007 thisTest = new newinstance007();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        debugee.VM().suspend();
        testedClass = (ClassType )debugee.classByName(debugeeName);

        BreakpointRequest brkp = debugee.makeBreakpoint(testedClass,
                                            newinstance007a.brkpMethodName,
                                            newinstance007a.brkpLineNumber);
        brkp.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        brkp.enable();

        display("\nTEST BEGINS");
        display("===========");

        ObjectReference objRef = null;
        Field field = testedClass.fieldByName("invokingTime");
        List<? extends com.sun.jdi.Value> params = new Vector<com.sun.jdi.Value>();
        ThreadReference suspendedThread;
        BreakpointEvent brkpEvent;

        Method method = getConstructor(testedClass);
        display("Method      : " + method);

        for (int i = 0; i < 3; i++) {
            display("");
            switch (i) {
            case 0:
                display("\nCASE#0 : Thread is suspended by ThreadReference.suspend()");
                break;
            case 1:
                display("\nCASE#1 : Thread is suspended by VirtualMachine.suspend()");
                break;
            case 2:
                display("\nCASE#2 : Thread is suspended by BreakPointRequest");
                brkp.disable();
                brkp.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                brkp.enable();
            }

            // starting thread
            display("sending signal " + SGNL_STRTHRD);
            debugee.sendSignal(SGNL_STRTHRD);
            debugee.resume();
            display("waiting for signal " + SGNL_READY);
            debugee.receiveExpectedSignal(SGNL_READY);

            brkpEvent = waitForBreakpoint(brkp);

            thread = brkpEvent.thread();

            suspendedThread = debugee.threadByName(newinstance007a.testedThread);
            switch (i) {
            case 0:
                suspendedThread.suspend();
                break;
            case 1:
                debugee.VM().suspend();
                break;
            }

            if ( !suspendedThread.isSuspended() ) {
                complain(suspendedThread.name() + " is not suspended");
                exitStatus = Consts.TEST_FAILED;
            } else {
                display(suspendedThread.name() + " is suspended");
            }
            try {
                if (i == 1) {
                    // for VirtualMachine.suspended() only!
                    // resume thread for invokedMethod
                    thread.resume();
                }
                display("invoking the constructor");
                objRef = testedClass.newInstance(thread, method, params, 0);
                Value retValue = objRef.getValue(field);
                suspendedThread = debugee.threadByName("im007aThread01");
                if (((PrimitiveValue)retValue).longValue() >= waitTime) {
                    complain("CASE #" + i + " FAILED." +
                        "\n\tTimeout occured while invocation of debugee's constructor");
                    exitStatus = Consts.TEST_FAILED;
                } else {
                    display("CASE #" + i + " PASSED");
                }
            } catch(Exception e) {
                complain("Unexpected exception while invocation of debugee's constructor: " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        }

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }

    private BreakpointEvent waitForBreakpoint(BreakpointRequest brkp) {
        Event event = null;
        BreakpointEvent brkpEvent;

        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new TestRuntimeException("unexpected InterruptedException");
        }

        if (!(event instanceof BreakpointEvent )) {
            debugee.resume();
            throw new TestRuntimeException("BreakpointEvent didn't arrive");
        }

        brkpEvent = (BreakpointEvent )event;
        return brkpEvent;
    }

    private Method getConstructor(ReferenceType classType) {
        List methodList = classType.methods();
        Method method;
        for (int i = 0; i < methodList.size(); i++) {
            method = (Method )methodList.get(i);
            if (method.isConstructor()) {
                return method;
            }
        }
        return null;
    }
}
