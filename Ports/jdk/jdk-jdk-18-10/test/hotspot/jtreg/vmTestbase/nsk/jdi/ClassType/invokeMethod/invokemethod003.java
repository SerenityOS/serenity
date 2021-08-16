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
 * Test checks up the following assertions:                           <br>
 *     1. The specified method can be defined in this class, or in
 *     a superclass.                                                  <br>
 *     2. <code>IllegalArgumentException</code> is thrown if          <br>
 *       - the method is not a member of this class or a superclass;  <br>
 *       - the size of the argument list does not match the number of declared
 *         arguemnts for the method.                                  <br>
 * The first case considers the invokations for the <code>private</code>,
 * <code>protected</code> and <code>public</code> methods. In this case
 * no exceptions are expected.<br>
 */

public class invokemethod003 {

    private final static String prefix = "nsk.jdi.ClassType.invokeMethod.";
    private final static String debuggerName = prefix + "invokemethod003";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;

    private ClassType testedClass;
    private ReferenceType anotherClass;
    private ThreadReference thread;

    class TestRuntimeException extends RuntimeException {
        TestRuntimeException(String msg) {
            super("TestRuntimeException: " + msg);
        }
    }

    public final static String [] methods2Invoke = {
                "publicFromParent",
                "protectFromParent",
                "privateFromParent",
                "fromChild"
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

        invokemethod003 thisTest = new invokemethod003();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        try {
            prepareTestCase();
        } catch(InterruptedException e) {
            complain("InterruptedException occurs");
            exitStatus = Consts.TEST_FAILED;
            return;
        } catch(TestRuntimeException e) {
            complain(" " + e);
            for (int i = 0; i < e.getStackTrace().length; i++) {
                display("\t" + e.getStackTrace()[i]);
            }
            if ( debugee.getIOPipe() == null ) {
                debugee.createIOPipe();
            }
            debugee.receiveExpectedSignal(SGNL_READY);
            debugee.quit();
            exitStatus = Consts.TEST_FAILED;
            return;
        }

        display("\nTEST BEGINS");
        display("===========");

        Value retValue, value = null,
              expectedValue = debugee.VM().mirrorOf(6L);
        List<com.sun.jdi.Value> params = createParams(3),
             params4 = createParams(4);

        Method method;

        // checking up IllegalArgumentException, when the method is not a member
        // of this class or a superclass
        display("\nthe method is not a member of this class or a superclass");
        display("--------------------------------------------------------");
        method = debugee.methodByName(anotherClass, "run");
        try {
            retValue = invokeMethod(thread, method, params, expectedValue);
            complain("***IllegalArgumentException is not thrown***");
            exitStatus = Consts.TEST_FAILED;
        } catch(IllegalArgumentException e) {
            display(">expected " + e);
        } catch(Exception e) {
            complain(" unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        // checking up for public, protected, private methods of the superclass
        display("\npublic, protected, private methods of the superclass");
        display("----------------------------------------------------");
        for (int j = 0; j < methods2Invoke.length; j++) {
            method = debugee.methodByName(testedClass, methods2Invoke[j]);
            try {
                retValue = invokeMethod(thread, method, params, expectedValue);
            } catch(Exception e) {
                complain("***unexpected " + e + "***");
                exitStatus = Consts.TEST_FAILED;
            }
        }
        display("");

        // checking up IllegalArgumentException, wrong size of the argument list
        display("wrong size of the argument list: " + params4.size()
                        + "(it should be 3)");
        display("--------------------------------------------------");
        method = debugee.methodByName(testedClass, methods2Invoke[0]);
        try {
            retValue = invokeMethod(thread, method, params4, expectedValue);
            complain("***IllegalArgumentException is not thrown***");
            exitStatus = Consts.TEST_FAILED;
        } catch(IllegalArgumentException e) {
            display(">expected " + e);
        } catch(Exception e) {
            complain(" unexpected " + e);
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

        ClassPrepareRequest cprep
                    = debugee.getEventRequestManager().createClassPrepareRequest();
        cprep.addClassFilter(prefix + invokemethod003a.class2Check);
        cprep.enable();

        debugee.resume();

        // waiting ClassPrepareEvent for debugeeName
        event = debugee.waitingEvent(cprep, waitTime);
        if (!(event instanceof ClassPrepareEvent)) {
            debugee.resume();
            throw new TestRuntimeException("ClassPrepareEvent didn't arrive");
        }


        testedClass = (ClassType )debugee.classByName(prefix
                                                    + invokemethod003a.class2Check);

        ReferenceType debugeeClass = debugee.classByName(debugeeName);
        BreakpointRequest brkp = debugee.setBreakpoint(debugeeClass,
                                                    invokemethod003a.brkpMethodName,
                                                    invokemethod003a.brkpLineNumber);

        debugee.resume();

        debugee.createIOPipe();
        debugee.redirectStdout(log,"");
        debugee.redirectStderr(log,"");
        debugee.receiveExpectedSignal(SGNL_READY);

        anotherClass = debugee.classByName(prefix
                                                + invokemethod003a.anotherClassName);
        if (anotherClass == null) {
            debugee.resume();
            throw new TestRuntimeException(prefix
                            + invokemethod003a.anotherClassName
                            + " has not been loaded yet");
        }


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

    private Value invokeMethod(ThreadReference thread, Method method, List<Value> params,
                                    Value expectedValue) {
        Value returnedValue = null,
              param;
        try {
            display("Method      : " + method);
            for (int i = 0; i < params.size(); i++) {
                param = params.get(i);
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
        } catch(InvocationException e) {
            complain("exception: " + e);
            exitStatus = Consts.TEST_FAILED;
        }

        String retType = returnedValue != null ? returnedValue.type().toString()
                                                  : "";
        display("Return value: " + returnedValue + "(" + retType + ")");

        if (!returnedValue.equals(expectedValue)) {
            String expType = expectedValue.type().toString() ;
            complain("***wrong the return value***");
            complain("expected value        : " + expectedValue + "("
                                            + expType + ")");
            exitStatus = Consts.TEST_FAILED;
        }
        display("");
        return returnedValue;
    }

    private List<com.sun.jdi.Value> createParams(int size) {
        Vector<com.sun.jdi.Value> params = new Vector<com.sun.jdi.Value>();
        for (int i = 0; i < size; i++) {
            params.add(debugee.VM().mirrorOf(i + 1));
        }
        return params;
    }
}
