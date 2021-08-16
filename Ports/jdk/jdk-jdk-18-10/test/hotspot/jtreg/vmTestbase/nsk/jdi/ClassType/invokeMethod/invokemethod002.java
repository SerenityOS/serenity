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
import java.lang.reflect.Array;

/**
 * For every primitive value there is the simplest method on the debugee side,
 * which has alone argument of this primitive value and returns the same value
 * as argument. Such methods are invoked for boundary values of the primitive
 * type. <br>
 * The debugger compares the argument value and the return value and
 * it is expected, these values must be equal.
 */

public class invokemethod002 {

    private final static String prefix = "nsk.jdi.ClassType.invokeMethod.";
    private final static String debuggerName = prefix + "invokemethod002";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;

    private ClassType testedClass;
    ThreadReference thread;

    class TestRuntimeException extends RuntimeException {
        TestRuntimeException(String msg) {
            super("TestRuntimeException: " + msg);
        }
    }

    public final static String [] methods2Invoke = {
                "methodBOOLParam",
                "methodBYTEParam",
                "methodCHARParam",
                "methodDOUBLEParam",
                "methodFLOATParam",
                "methodINTParam",
                "methodLONGParam",
                "methodSHORTParam",
                "methodOBJParam"
    };

    private static boolean  [] boolParamValues =
                                    {true, false};
    private static byte     [] byteParamValues =
                                    {Byte.MIN_VALUE,-1,0,1,Byte.MAX_VALUE};
    private static char     [] charParamValues =
                                    {Character.MIN_VALUE,Character.MAX_VALUE};
    private static double   [] doubleParamValues =
                                    {Double.NEGATIVE_INFINITY, Double.MIN_VALUE,-1.D,
                                     0.D,1.D,Double.MAX_VALUE,Double.POSITIVE_INFINITY};
    private static float    [] floatParamValues =
                                    {Float.NEGATIVE_INFINITY,Float.MIN_VALUE,-1.F,
                                     0.F,1.F,Float.MAX_VALUE,Float.POSITIVE_INFINITY};
    private static int      [] intParamValues =
                                    {Integer.MIN_VALUE,-1,0,1,Integer.MAX_VALUE};
    private static long     [] longParamValues =
                                    {Long.MIN_VALUE,-1L,0L,1L,Long.MAX_VALUE};
    private static short    [] shortParamValues =
                                    {Short.MIN_VALUE,-1,0,1,Short.MAX_VALUE};
    private static String   [] objParamValues =
                                    {"Hello world"};

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

        invokemethod002 thisTest = new invokemethod002();

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


        Value retValue, value = null;
        Vector<Value> params = new Vector<Value>();

        Method method;
        Object arr = null;
        for (int j = 0; j < methods2Invoke.length; j++) {
            method = debugee.methodByName(testedClass, methods2Invoke[j]);
            display("");
            switch (j) {
            case 0:
                    arr = boolParamValues;
                    display("boolean values");
                    break;
            case 1:
                    arr = byteParamValues;
                    display("byte values");
                    break;
            case 2:
                    arr = charParamValues;
                    display("char values");
                    break;
            case 3:
                    arr = doubleParamValues;
                    display("double values");
                    break;
            case 4:
                    arr = floatParamValues;
                    display("float values");
                    break;
            case 5:
                    arr = intParamValues;
                    display("integer values");
                    break;
            case 6:
                    arr = longParamValues;
                    display("long values");
                    break;
            case 7:
                    arr = shortParamValues;
                    display("short values");
                    break;
            case 8:
                    arr = objParamValues;
                    display("String values");
                    break;
            default:
                    complain("***TEST CASE ERROR***");
                    exitStatus = Consts.TEST_FAILED;
                    continue;
            }
            display("--------------");
            for (int i = 0; i < Array.getLength(arr); i++) {
                params.clear();
                if (arr instanceof boolean[]) {
                    value = debugee.VM().mirrorOf(Array.getBoolean(arr,i));
                } else if (arr instanceof byte[]) {
                    value = debugee.VM().mirrorOf(Array.getByte(arr,i));
                } else if (arr instanceof char[]) {
                    value = debugee.VM().mirrorOf(Array.getChar(arr,i));
                } else if (arr instanceof double[]) {
                    value = debugee.VM().mirrorOf(Array.getDouble(arr,i));
                } else if (arr instanceof float[]) {
                    value = debugee.VM().mirrorOf(Array.getFloat(arr,i));
                } else if (arr instanceof int[]) {
                    value = debugee.VM().mirrorOf(Array.getInt(arr,i));
                } else if (arr instanceof long[]) {
                    value = debugee.VM().mirrorOf(Array.getLong(arr,i));
                } else if (arr instanceof short[]) {
                    value = debugee.VM().mirrorOf(Array.getShort(arr,i));
                } else if (arr instanceof String[]) {
                    value = debugee.VM().mirrorOf((String )Array.get(arr,i));
                } else {
                    complain("***TEST CASE ERROR***");
                    exitStatus = Consts.TEST_FAILED;
                }
                params.add(value);
                retValue = invokeMethod(thread, method, params, value);
            }
        }

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }

    private void prepareTestCase() throws InterruptedException {
        Event event = null;

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
                                                    invokemethod002a.brkpMethodName,
                                                    invokemethod002a.brkpLineNumber);
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

    private Value invokeMethod(ThreadReference thread, Method method, List<? extends com.sun.jdi.Value> params,
                                    Value expectedValue) {
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
}
