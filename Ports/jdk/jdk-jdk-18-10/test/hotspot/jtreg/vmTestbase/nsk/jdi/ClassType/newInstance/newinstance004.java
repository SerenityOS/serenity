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
 * Test checks up the following assertion:                           <br>
 *   If the invoked method throws an exception, this method will throw
 *   an InvocationException which contains a mirror to the exception object thrown.
 * It is checked for two cases:
 *     1. when the invoked method throws uncaught NullPointerExection.<br>
 *        For this case, InvocationException is expected, the one     <br>
 *        contains a mirror to the NullPointerException object.       <br>
 *     2. when invoked method throws caught exection                  <br>
 *        For this case, no exceptions are expected                   <br>
 */

public class newinstance004 {

    private final static String prefix = "nsk.jdi.ClassType.newInstance.";
    private final static String debuggerName = prefix + "newinstance004";
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

        newinstance004 thisTest = new newinstance004();

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
                                                    newinstance004a.brkpMethodName,
                                                    newinstance004a.brkpLineNumber);
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
                        debugee.classByName(newinstance004a.testException);

        display("\nTEST BEGINS");
        display("===========");

        Value retValue;
        ObjectReference obj = null;
        List<com.sun.jdi.Value> params = new Vector<com.sun.jdi.Value>();

        display("\nuncaught NullPointerException");
        display("-----------------------------");
        Method method = getConstructor(testedClass);
        display("method>" + method);
        params.add(debugee.VM().mirrorOf(false));
        try {
            obj = testedClass.newInstance(thread, method, params, 0);
            complain("***InvocationException is not thrown***");
            exitStatus = Consts.TEST_FAILED;
        } catch(InvocationException e) {
            display("!!!expected " + e);
            if (e.exception().referenceType().equals(exceptionRefType)) {
                display("!!!expected the exception mirror " + e.exception());
            } else {
                complain("Unexpected the exception mirror "
                                            + e.exception().referenceType());
                complain("We are expecting  " + exceptionRefType);
                exitStatus = Consts.TEST_FAILED;
            }
        } catch(Exception e) {
            complain(" unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        // caught NullPointerException
        display("\ncaught NullPointerException");
        display("-----------------------------");
        method = getConstructor(testedClass);
        params.clear();
        params.add(debugee.VM().mirrorOf(true));
        display("method>" + method);
        try {
            obj = testedClass.newInstance(thread, method, params, 0);
            if (!obj.referenceType().equals(testedClass)) {
                complain("wrong object type:" + obj.referenceType());
                exitStatus = Consts.TEST_FAILED;
            }
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
