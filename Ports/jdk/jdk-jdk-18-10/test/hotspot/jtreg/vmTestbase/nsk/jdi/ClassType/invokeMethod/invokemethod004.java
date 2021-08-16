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
 * Test checks up the following assertion:                           <br>
 *   If the invoked method throws an exception, this method will throw
 *   an InvocationException which contains a mirror to the exception object thrown.
 * It is checked for two cases:
 *     1. when the invoked method throws uncought NullPointerExection.<br>
 *        For this case, InvocationException is expected, the one     <br>
 *        contains a mirror to the NullPointerException object.       <br>
 *     2. when invoked method throws cought exection                  <br>
 *        For this case, no exceptions are expected                   <br>
 */

public class invokemethod004 {

    private final static String prefix = "nsk.jdi.ClassType.invokeMethod.";
    private final static String debuggerName = prefix + "invokemethod004";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

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

    public final static String [] methods2Invoke = {
                "throwNPE",
                "throwCaughtNPE"
    };

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

        invokemethod004 thisTest = new invokemethod004();

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

        BreakpointRequest brkp = debugee.setBreakpoint(testedClass,
                                                    invokemethod004a.brkpMethodName,
                                                    invokemethod004a.brkpLineNumber);
        debugee.resume();

        debugee.sendSignal("");
        Event event = null;

        // waiting the breakpoint event
        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new TestRuntimeException("unexpected InterruptedException");
        }
        if (!(event instanceof BreakpointEvent )) {
            debugee.resume();
            throw new TestRuntimeException("BreakpointEvent didn't arrive");
        }

        BreakpointEvent brkpEvent = (BreakpointEvent )event;

        thread = brkpEvent.thread();

        ReferenceType exceptionRefType =
                        debugee.classByName(invokemethod004a.testException);

        display("\nTEST BEGINS");
        display("===========");

        Value retValue;
        List<Value> params = createParams(0);

        Method method;

        display("\nuncought NullPointerException");
        display("-----------------------------");

        // uncought NullPointerException
        method = debugee.methodByName(testedClass, methods2Invoke[0]);
        try {
            retValue = invokeMethod(thread, method, params);
            complain("InvocationException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(InvocationException e) {
            display("!!!expected InvocationException");

            // cheking of the excpetion type
            if (e.exception().referenceType().equals(exceptionRefType)) {
                display("!!!expected the exception mirror " + e.exception());
            } else {
                complain("Unexpected the exception mirror "
                                            + e.exception().referenceType());
                complain("We are expecting  " + exceptionRefType);
                exitStatus = Consts.TEST_FAILED;
            }
        }
        display("");

        display("\ncought NullPointerException");
        display("-----------------------------");
        // cought NullPointerException
        method = debugee.methodByName(testedClass, methods2Invoke[1]);
        try {
            retValue = invokeMethod(thread, method, params);
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }

    private void prepareTestCase() throws InterruptedException {
        Event event = null;

        // waiting VMStartEvent
        event = debugee.waitingEvent(null, waitTime);
        if (!(event instanceof VMStartEvent)) {
            debugee.resume();
            throw new TestRuntimeException("VMStartEvent didn't arrive");
        }

        ClassPrepareRequest cprep
                    = debugee.getEventRequestManager().createClassPrepareRequest();
        cprep.addClassFilter(debugeeName);
        cprep.enable();

        debugee.resume();

        // waiting ClassPrepareEvent for debugeeName
        event = debugee.waitingEvent(cprep, waitTime);
        if (!(event instanceof ClassPrepareEvent)) {
            debugee.resume();
            throw new TestRuntimeException("ClassPrepareEvent didn't arrive");
        }

        testedClass = (ClassType )debugee.classByName(debugeeName);

        BreakpointRequest brkp = debugee.setBreakpoint(testedClass,
                                                    invokemethod004a.brkpMethodName,
                                                    invokemethod004a.brkpLineNumber);
        debugee.resume();

        debugee.createIOPipe();
        debugee.redirectStdout(log,"");
        debugee.redirectStderr(log,"");
        debugee.receiveExpectedSignal(SGNL_READY);

        // waiting the breakpoint event
        event = debugee.waitingEvent(brkp, waitTime);
        if (!(event instanceof BreakpointEvent )) {
            debugee.resume();
            throw new TestRuntimeException("BreakpointEvent didn't arrive");
        }

        BreakpointEvent brkpEvent = (BreakpointEvent )event;
        if (brkpEvent == null) {
            debugee.resume();
            throw new TestRuntimeException("No breakpoint events");
        }

        thread = brkpEvent.thread();
    }

    private Value invokeMethod(ThreadReference thread, Method method, List<? extends com.sun.jdi.Value> params)
                        throws InvocationException {
        Value returnedValue = null,
              param;
        try {
            display("Method      : " + method);
            for (int i = 0; i < params.size(); i++) {
                param = (Value )params.get(i);
                display("Parameters  : " + param + "(" + param.type() + ")");
            }
            returnedValue = testedClass.invokeMethod(thread, method, params,
                                                    ClassType.INVOKE_SINGLE_THREADED);
        } catch(InvalidTypeException e) {
            complain("exception: " + e);
            exitStatus = Consts.TEST_FAILED;
        } catch(ClassNotLoadedException e) {
            complain("exception: " + e);
            exitStatus = Consts.TEST_FAILED;
        } catch(IncompatibleThreadStateException e) {
            complain("exception: " + e);
            exitStatus = Consts.TEST_FAILED;
        }

        String retType = returnedValue != null ? returnedValue.type().toString()
                                                  : "";
        display("Return value: " + returnedValue + "(" + retType + ")");

        display("");
        return returnedValue;
    }

    private List<Value> createParams(int size) {
        Vector<Value> params = new Vector<Value>();
        for (int i = 0; i < size; i++) {
            params.add(debugee.VM().mirrorOf(i + 1));
        }
        return params;
    }
}
