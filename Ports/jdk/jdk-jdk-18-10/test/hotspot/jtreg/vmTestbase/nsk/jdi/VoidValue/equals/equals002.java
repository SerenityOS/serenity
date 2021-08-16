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

package nsk.jdi.VoidValue.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

public class equals002 {

    private final static String prefix = "nsk.jdi.VoidValue.equals.";
    private final static String className = "equals002";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";
    private final static String arrPrimitives = "testedFields";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static String method2Invoke = "voidValue";
    private ClassType testedClass;
    private ThreadReference thread;
    private static long waitTime;


    class TestRuntimeException extends RuntimeException {
        TestRuntimeException(String msg) {
            super("TestRuntimeException: " + msg);
        }
    }

//----------------------------------------------------

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

        equals002 thisTest = new equals002();

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
                                                    equals002a.brkpMethodName,
                                                    equals002a.brkpLineNumber);
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
        if (brkpEvent == null) {
            debugee.resume();
            throw new TestRuntimeException("No breakpoint events");
        }

        thread = brkpEvent.thread();

        display("\nTEST BEGINS");
        display("===========");

        Method method = debugee.methodByName(testedClass, method2Invoke);
        List<Value> params = new Vector<Value>();
        VoidValue voidValue1 = null,
                  voidValue2 = null;
        try {
            voidValue1 = (VoidValue )testedClass.invokeMethod(thread, method, params, 0);
            voidValue2 = (VoidValue )testedClass.invokeMethod(thread, method, params, 0);
        } catch(Exception e) {
            complain("unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }

        // geting of array of primitive types
        Field fldOtherType, field = testedClass.fieldByName(arrPrimitives);
        if ( field == null ) {
            complain("Field '" + arrPrimitives + "' not found.");
            exitStatus = Consts.TEST_FAILED;
        }
        Value valueOtherType, arrValue = testedClass.getValue(field);
        if ( arrValue == null || !(arrValue instanceof ArrayReference) ) {
            complain("Field '" + arrValue + "' is wrong.");
            exitStatus = Consts.TEST_FAILED;
        }
        ArrayReference primitivValues = (ArrayReference )arrValue;

        // comparing with debugee's fields
        if ( !PerformComparing(voidValue1, voidValue2) )
            exitStatus = Consts.TEST_FAILED;
        for (int j = 0; j < primitivValues.length(); j++) {
            arrValue = primitivValues.getValue(j);

            fldOtherType = testedClass.fieldByName(((StringReference )arrValue).value());
            if ( fldOtherType == null ) {
                complain("Field '" + arrValue + "' not found.");
                exitStatus = Consts.TEST_FAILED;
                continue;
            }

            valueOtherType = testedClass.getValue(fldOtherType);

            if ( !PerformComparing(voidValue1, valueOtherType) )
                exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }

    private boolean PerformComparing(VoidValue value, Value object ) {
        boolean res = true;
        String msg = "";
        String type = "(" + (object == null ? "" : object.type().toString()) + ")";
        try {
            if (value.equals(object)) {
                if (object instanceof VoidValue) {
                    msg += "--> " + value + " == "  + object + type;
                }
                else {
                    msg += "##> " + value + " == "  + object + type
                                + " : are different types " + value.type() + " - "
                                + ((Value )object).type();
                    res = false;
                }
                if (object == null) {
                    msg += " ***Wrong result!!!***";
                    res = false;
                }
            } else {
                if (!(object instanceof VoidValue)) {
                    msg += "--> " + value + " != "  + object + type;
                }
            }
        } catch (Exception e) {
            msg += " ***Unexpected " + e + "***";
            res = false;
        }

        if ( !res )
            complain(msg);
        else
            display(msg);

        return res;
    }
}
