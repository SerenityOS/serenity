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
 *  Test checks up that NullPointerException will be thrown in
 *  the following cases:                                                        <br>
 *      - newInstance(null, method, params,ClassType.INVOKE_SINGLE_THREADED)   <br>
 *      - newInstance(thread, null, params,ClassType.INVOKE_SINGLE_THREADED)   <br>
 *      - newInstance(thread, method, null,ClassType.INVOKE_SINGLE_THREADED)   <br>
 *  In case                                                     <br>
 *      newInstance(thread, method, params,Integer.MAX_VALUE)  <br>
 *  no exception is expected.
 */

public class newinstance006 {

    private final static String prefix = "nsk.jdi.ClassType.newInstance.";
    private final static String debuggerName = prefix + "newinstance006";
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

    public final static String method2Invoke = "justMethod";

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

        newinstance006 thisTest = new newinstance006();

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
                                                    newinstance006a.brkpMethodName,
                                                    newinstance006a.brkpLineNumber);
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

        ObjectReference objRef;
        List<? extends com.sun.jdi.Value> params = createParams(1);

        Method method = getConstructor(testedClass);
        display("Method      : " + method);

        display("newInstance(null, method, params,"
                    + "ClassType.INVOKE_SINGLE_THREADED)");
        try {
            objRef = testedClass.newInstance(null, method, params,
                                                ClassType.INVOKE_SINGLE_THREADED);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("newInstance(thread, null, params,"
                    + "ClassType.INVOKE_SINGLE_THREADED)");
        try {
            objRef = testedClass.newInstance(thread, null, params,
                                                ClassType.INVOKE_SINGLE_THREADED);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("newInstance(thread, method, null,"
                    + "ClassType.INVOKE_SINGLE_THREADED)");
        try {
            objRef = testedClass.newInstance(thread, method, null,
                                                ClassType.INVOKE_SINGLE_THREADED);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("newInstance(thread, method, params,"
                    + "Integer.MAX_VALUE)");
        try {
            objRef = testedClass.newInstance(thread, method, params,
                                                Integer.MAX_VALUE);
            if (!objRef.referenceType().equals(testedClass)) {
                complain("Wrong returned value " + objRef.toString());
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("returned value is " + objRef.toString());
            }
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("newInstance(thread, method, params,"
                    + "Integer.MIN_VALUE)");
        try {
            objRef = testedClass.newInstance(thread, method, params,
                                                Integer.MIN_VALUE);
            if (!objRef.referenceType().equals(testedClass)) {
                complain("Wrong returned value " + objRef.toString());
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("returned value is " + objRef.toString());
            }
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

    private List<? extends com.sun.jdi.Value> createParams(int size) {
        Vector<com.sun.jdi.Value> params = new Vector<com.sun.jdi.Value>();
        for (int i = 0; i < size; i++) {
            params.add(debugee.VM().mirrorOf(i + 1));
        }
        return params;
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
