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

import jdk.test.lib.Utils;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * The test consits of the following files:
 *     <code>redefineclasses003.java</code>               - debugger
 *     <code>redefineclasses003a.java</code>              - initial debuggee
 *     <code>newclass/redefineclasses003a.java</code>     - redefined debuggee
 *
 * When the test is starting debugee, debugger sets breakpoint at
 * a line of <code>method_A</code>, where <code>method_B</code> is called.
 * After the breakpoint is reached, debugger:
 *  - redefines debugee-class, that is inserting line after point of
 *    execution: <code>log.display("new line");</code>
 *  - creates step request adding class filter
 *  - waits for a step event three times.
 *
 * In case, a step event didn't arrive from <code>method_B</code>, test fails.
 * When <code>canRedefineClasses()</code> is <code>false</code>, the test is
 * considered as passed and completes it's execution.
 *
 * The test should be compiled with <code>-g</code> option, so cfg-file redefines
 * <code>JAVA_OPTS</code> variable.
 */

public class redefineclasses003 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";
    public final static String EXPECTED_STRING = "!!!Expected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses003";
    private final static String debugeeName = debuggerName + "a";
    private final static String newClassFile = "newclass"
                    + File.separator + prefix.replace('.',File.separatorChar)
                    + "redefineclasses003a.class";
    private final static String oldClassFile
            = ClassFileFinder.findClassFile(debugeeName, Utils.TEST_CLASS_PATH)
                             .toString();
    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String clsDir;

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

        redefineclasses003 thisTest = new redefineclasses003();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        clsDir = argv[0];

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
                                                    redefineclasses003a.brkpMethodName,
                                                    redefineclasses003a.brkpLineNumber);
        debugee.resume();
        debugee.sendSignal("continue");

        waitBreakpointEvent(brkp, waitTime);

        Field fld = null;

        display("\nTEST BEGINS");
        display("===========");

        display(">>> Static fields initialized by <clinit>");
        checkFields(redefineclasses003a.INITIAL_VALUE);
        redefineClass(clsDir + File.separator + newClassFile);
        checkFields(redefineclasses003a.INITIAL_VALUE);

        display("\n>>> Static fields initialized by debugger");
        IntegerValue newValue = debugee.VM().mirrorOf(redefineclasses003a.ASSIGNED_VALUE);
        for (int i = 0; i < redefineclasses003a.testedFields.length; i++) {
            fld = testedClass.fieldByName(redefineclasses003a.testedFields[i]);
            try {
                ((ClassType )testedClass).setValue(fld, newValue);
            } catch (Exception e) {
                throw new TestBug(UNEXPECTED_STRING + e);
            }
        }
        checkFields(redefineclasses003a.ASSIGNED_VALUE);
        redefineClass(oldClassFile);
        checkFields(redefineclasses003a.ASSIGNED_VALUE);

        display("\n>>> Static fields initialized by debuggee at runtime");
        brkp = debugee.setBreakpoint(testedClass,
                                redefineclasses003a.brkpMethodName,
                                redefineclasses003a.brkpLineNumber + 9);
        debugee.resume();

        waitBreakpointEvent(brkp, waitTime);

        checkFields(redefineclasses003a.REASSIGNED_VALUE);
        redefineClass(clsDir + File.separator + newClassFile);
        checkFields(redefineclasses003a.REASSIGNED_VALUE);

        debugee.resume();
        display("=============");
        display("TEST FINISHES\n");
    }

    private void checkFields(int expectedValue) {
        int value;
        Field fld;
        Value val;
        for (int i = 0; i < redefineclasses003a.testedFields.length; i++) {
            fld = testedClass.fieldByName(redefineclasses003a.testedFields[i]);
            val = testedClass.getValue(fld);
            value = ((IntegerValue )val).value();
            if (value != expectedValue) {
                complain("Unexpected " + fld.name() + " = "
                            + redefineclasses003a.value2String(value));
                exitStatus = Consts.TEST_FAILED;
                return;
            }
            display(fld.name() + " = " + redefineclasses003a.value2String(value));
        }
    }

    private void redefineClass(String classFile) {
        display(">redefining the Debugee class...");
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes = mapClassToBytes(classFile);
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
