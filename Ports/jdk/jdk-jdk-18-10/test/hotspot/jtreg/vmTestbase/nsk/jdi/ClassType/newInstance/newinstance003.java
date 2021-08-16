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
 * Test checks up the following assertions:                           <br>
 *     1. he specified constructor must be defined in this class      <br>                                            <br>
 *     2. <code>IllegalArgumentException</code> is thrown if          <br>
 *       - the method is not a member of this class;                  <br>
 *       - the size of the argument list does not match the number    <br>
 *         of declared arguemnts for the constructor, or if the       <br>
 *         method is not a constructor.                               <br>
 */

public class newinstance003 {

    private final static String prefix = "nsk.jdi.ClassType.newInstance.";
    private final static String debuggerName = prefix + "newinstance003";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;

    private ClassType testedClass;
    private ClassType class2Check;
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

        newinstance003 thisTest = new newinstance003();

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
        class2Check = (ClassType )debugee.classByName(prefix
                                                + newinstance003a.class2Check);
        testedClass = (ClassType )debugee.classByName(debugeeName);

        BreakpointRequest brkp = debugee.makeBreakpoint(testedClass,
                                                    newinstance003a.brkpMethodName,
                                                    newinstance003a.brkpLineNumber);
        brkp.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        brkp.enable();
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

        display("\nTEST BEGINS");
        display("===========");

        ObjectReference obj = null;
        Value retValue, value = null,
              expectedValue = debugee.VM().mirrorOf(6L);
        List<com.sun.jdi.Value> params = new Vector<com.sun.jdi.Value>();

        // 1 case---------------------------------------------------------------
        // checking up IllegalArgumentException, the size of the argument list
        // does not match the number of declared arguments for the constructor
        display("\nthe size of the argument list does not match the number "
                    + "of declared arguments");
        display("----------------------------------------------------------"
                    + "-------------------");
        Method method = getConstructor(class2Check);
        display("method>" + method);
        try {
            obj = class2Check.newInstance(thread, method, params, 0);
            complain("***IllegalArgumentException is not thrown***");
            exitStatus = Consts.TEST_FAILED;
        } catch(IllegalArgumentException e) {
            display(">expected " + e);
        } catch(Exception e) {
            complain(" unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        // 2 case---------------------------------------------------------------
        // checking up IllegalArgumentException, the method is not a constructor
        display("\nthe method is not a constructor");
        display("-------------------------------");
        method = debugee.methodByName(class2Check, newinstance003a.method);
        display("method>" + method);
        try {
            obj = class2Check.newInstance(thread, method, params, 0);
            complain("***IllegalArgumentException is not thrown***");
            exitStatus = Consts.TEST_FAILED;
        } catch(IllegalArgumentException e) {
            display(">expected " + e);
        } catch(Exception e) {
            complain(" unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        // 3 case---------------------------------------------------------------
        // checking up IllegalArgumentException, when constructor is a member
        // of superclass
        params.add(debugee.VM().mirrorOf(1));
        display("\nconstructor is a member of superclass");
        display("-------------------------------------");
        method = getConstructor(testedClass);
        display("method>" + method);
        ReferenceType refType;
        Field field;
        try {
            obj = class2Check.newInstance(thread, method, params, 0);
            complain("***IllegalArgumentException is not thrown***");
            exitStatus = Consts.TEST_FAILED;
        } catch(IllegalArgumentException e) {
            display(">expected " + e);
        } catch(Exception e) {
            complain(" unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        // 4 case---------------------------------------------------------------
        // when constructor is a member of this class
        display("\nconstructor is a member of this class");
        display("-------------------------------------");
        method = getConstructor(class2Check);
        display("method>" + method);
        try {
            obj = class2Check.newInstance(thread, method, params, 0);
            refType = obj.referenceType();
            field = refType.fieldByName("sum");
            retValue = obj.getValue(field);
            if (((PrimitiveValue )retValue).intValue() != 1) {
                complain("unexpected value of field: " + retValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display(">created instance :" + obj);
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
