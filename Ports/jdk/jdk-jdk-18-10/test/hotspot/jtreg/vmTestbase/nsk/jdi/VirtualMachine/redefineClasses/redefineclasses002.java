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

package nsk.jdi.VirtualMachine.redefineClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * The test against the method <code>com.sun.jdi.VirtualMachine.redefineClasses()</code>
 * and checks up the following assertion:                   <br>
 *     The redefined methods will be used on new invokes.   <br>
 *     If resetting these frames is desired, use
 *     <code>ThreadReference.popFrames(StackFrame)</code> with
 *     <code>Method.isObsolete()</code>.                    <br>
 *
 * The test consits of the following files:                 <br>
 *     <code>redefineclasses002.java</code>           - debugger           <br>
 *     <code>redefineclasses002a.java</code>          - initial debuggee   <br>
 *     <code>newclass/redefineclasses002a.java</code> - redefined debuggee <br>
 *
 *
 * This test performs the following steps:                                  <br>
 *     1. Setting breakpoint at line of method, which will be redefined.
 *        This method has a local variable <code>testedVar</code> and the breakpoint
 *        is placed after it's initializing.                                <br>
 *     2. When breakpoint event is arrived, Debugger requests initial
 *        value of <code>testedVar</code>                                   <br>
 *     3. While VM suspended Debugger redefines the tested method.
 *        It is added new line, after execution point. That line changes
 *        value of <code>testedVar</code>, different from initial-value.    <br>
 *     4. While debugee remains suspending, debuger tries to get value of
 *        <code>testedVar</code>. In this case <code>AbsentInformationException</code>
 *        is expected.                                                      <br>
 *     5. Popping the active frame and trying to get value again.
 *        It is expected, method will be not found in active stack frames.  <br>
 *     6. Setting breakpoint at line after the new one and resuming debugee.<br>
 *     7. Trying to get new value of <code>testedVar</code>. It is expected the last
 *        value will be different from initial-value.                       <br>
 *
 * The test should be compiled with <code>-g</code> option, so cfg-file redefines
 * <code>JAVA_OPTS</code> variable.
 */

public class redefineclasses002 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";
    public final static String EXPECTED_STRING = "!!!Expected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses002";
    private final static String debugeeName = debuggerName + "a";
    private final static String newClass = "newclass"
                    + File.separator + prefix.replace('.',File.separatorChar)
                    + "redefineclasses002a.class";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String testDir;

    private ClassType testedClass;

    class MethodNotFoundException extends RuntimeException {
        MethodNotFoundException (String msg) {
            super(msg);
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

        redefineclasses002 thisTest = new redefineclasses002();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        testDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (TestBug e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.resume();
            debugee.quit();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws TestBug {

        if (!debugee.VM().canRedefineClasses()) {
            debugee.sendSignal("continue");
            display("\n>>>canRedefineClasses() is false<<< test canceled.\n");
            return;
        }

        debugee.VM().suspend();
        testedClass = (ClassType)debugee.classByName(debugeeName);
        display("Tested class\t:" + testedClass.name());
        BreakpointRequest brkp = debugee.setBreakpoint(testedClass,
                                                    redefineclasses002a.brkpMethodName,
                                                    redefineclasses002a.brkpLineNumber);
        debugee.resume();
        debugee.sendSignal("continue");

        BreakpointEvent brkpEvent = waitBreakpointEvent(brkp, waitTime);

        ThreadReference thread = brkpEvent.thread();

        display("\nTEST BEGINS");
        display("===========");

        IntegerValue val = readVariableValue(thread, brkpEvent.location().method(),
                                                redefineclasses002a.testedVarName);

        if ( val.intValue() != redefineclasses002a.INITIAL_VALUE) {
            complain("***UNEXPECTED value*** " + val);
            exitStatus = Consts.TEST_FAILED;
        }

        display(">redefining the Debugee class...");
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes = mapClassToBytes(testDir + File.separator + newClass);
        try {
            debugee.VM().redefineClasses(mapBytes);
        } catch (UnsupportedOperationException e) {
            if (!debugee.VM().canRedefineClasses()) {
                complain("*** Warning: " + e);
                complain("*** The test is considered as passed");
                return;
            }
            throw new TestBug(UNEXPECTED_STRING + e);
        } catch (Exception e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }

        Method method = null;
        try {
            display("thread location>" + thread.frame(0).location());
            method = thread.frame(0).location().method();
        } catch(IncompatibleThreadStateException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }

        val = readVariableValue(thread, method, redefineclasses002a.testedVarName);

        display(">frames is being popped...");
        try {
            thread.popFrames(thread.frame(0));
        } catch (IncompatibleThreadStateException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }

        try {
            val = readVariableValue(thread, method, redefineclasses002a.testedVarName);
        } catch (MethodNotFoundException e) {
            display(EXPECTED_STRING + e + "\n");
        }

        brkp = debugee.setBreakpoint(testedClass,
                                redefineclasses002a.brkpMethodName,
                                redefineclasses002a.brkpLineNumber + 2);
        debugee.resume();

        brkpEvent = waitBreakpointEvent(brkp, waitTime);

        ThreadReference thrd = brkpEvent.thread();
        val = readVariableValue(thrd, brkpEvent.location().method(),
                                    redefineclasses002a.testedVarName);
        if ( val.intValue() != redefineclasses002a.REDEFINED_VALUE) {
            complain("***UNEXPECTED value*** " + val);
            exitStatus = Consts.TEST_FAILED;
        }

        debugee.resume();

        display("=============");
        display("TEST FINISHES\n");
    }

    private Map<? extends com.sun.jdi.ReferenceType,byte[]> mapClassToBytes(String fileName) {
        display("class-file\t:" + fileName);
        File fileToBeRedefined = new File(fileName);
        int fileToRedefineLength = (int )fileToBeRedefined.length();
        byte[] arrayToRedefine = new byte[fileToRedefineLength];

        FileInputStream inputFile;
        try {
            inputFile = new FileInputStream(fileToBeRedefined);
        } catch (FileNotFoundException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }

        try {
            inputFile.read(arrayToRedefine);
            inputFile.close();
        } catch (IOException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }
        HashMap<com.sun.jdi.ReferenceType,byte[]> mapForClass = new HashMap<com.sun.jdi.ReferenceType,byte[]>();
        mapForClass.put(testedClass, arrayToRedefine);
        return mapForClass;
    }

    private IntegerValue readVariableValue(ThreadReference thrd, Method method,
                                        String varName) {
        String methodName = method.name();
        display(">getting value of local variable...");
        display("method name\t: " + methodName);
        display("variable name\t: " + varName);
        StackFrame stckFrm = null;
        List frames = null;
        try {
            frames = thrd.frames();
        } catch (IncompatibleThreadStateException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }

        Iterator iterator = frames.iterator();
        boolean isMethodFound = false;
        boolean isIntegerExpected = true;
        IntegerValue res = null;

        while (iterator.hasNext()) {
            stckFrm = (StackFrame )iterator.next();

            if (stckFrm == null)
                throw new TestBug("Not defined frame");
            if (method.equals(stckFrm.location().method())) {
                isMethodFound = true;
                try {
                    LocalVariable var = stckFrm.visibleVariableByName(varName);
                    res = (IntegerValue )stckFrm.getValue(var);
                } catch (AbsentInformationException e) {
                    if (method.isObsolete()) {
                        display(EXPECTED_STRING + e + "\n");
                        isIntegerExpected = false;
                    }
                    else
                        throw new TestBug(UNEXPECTED_STRING + e);
                }
                break;
            }
        }
        if (!isMethodFound)
            throw new MethodNotFoundException("Method :" + methodName + " not found");

        if ( !(res instanceof IntegerValue) ) {
            if (isIntegerExpected)
                display("unexpected value\t: <" + res + ">\n");
        } else {
            display(varName + " = " + res + "\n");
        }

        return res;
    }

    private BreakpointEvent waitBreakpointEvent(BreakpointRequest brkp, long waitTime) {
        Event event;
        display(">waiting breakpoint event...");
        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }
        if (!(event instanceof BreakpointEvent )) {
            throw new TestBug("BreakpointEvent didn't arrive");
        }

        return (BreakpointEvent )event;
    }

}
