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
 * The test checks up <code>com.sun.jdi.VirtualMachine.redefineClasses()</code>.
 * In for loop the test sets breakpoint and tries to redefines debugee's
 * method by changing value of debugee's field. After resuming debugee,
 * the test checks if a tested field contains correct value.
 * When <code>canRedefineClasses()</code> is <code>false</code>, the test is
 * considered as passed and completes it's execution.
 */

public class redefineclasses031 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";
    public final static String EXPECTED_STRING = "!!!Expected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses031";
    private final static String debugeeName = debuggerName + "a";
    private final static String Name = debuggerName + "b";

    public final static String PREPARE_SGL = "prepare";
    public final static String PERFORM_SGL = "perform";
    public final static String QUIT_SGL    = "quit";
    public final static String READY_SGL    = "ready";

    private final static String[] subDirs = {
            "newclass01",
            "newclass02",
            "newclass03",
            "newclass04",
            "newclass05"
    };

    private final static String newClassFile = File.separator
                    + prefix.replace('.',File.separatorChar)
                    + "redefineclasses031a.class";
    private static final String dashes = "--------------------------";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String testDir;
    private static String statDebugee;

    private ClassType debugeeClass;

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

        redefineclasses031 thisTest = new redefineclasses031();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        testDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (Failure e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.resume();
            debugee.quit();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {

        if (!debugee.VM().canRedefineClasses()) {
            display("\n>>>canRedefineClasses() is false<<< test canceled.\n");
            return;
        }

        debugeeClass = (ClassType)debugee.classByName(debugeeName);

        display("\nTEST BEGINS");
        display("===========");

        debugee.VM().suspend();
        debugee.sendSignal(PERFORM_SGL);

        display("Tested class\t:" + debugeeClass.name());

        ThreadReference thrd = debugee.threadByName("main");

        StepRequest sreq;
        BreakpointRequest brkp;
        Field testedField;
        PrimitiveValue val;

        for (int i = 0; i < subDirs.length; i++) {
            brkp = debugee.setBreakpoint(debugeeClass,
                                                redefineclasses031a.brkpMethodName,
                                                redefineclasses031a.brkpLineNumber);
            debugee.resume();

            waitBreakpointEvent(brkp, waitTime);

            redefineDebugee(subDirs[i]);

            sreq = debugee.getEventRequestManager().createStepRequest(thrd,
                                            StepRequest.STEP_LINE,
                                            StepRequest.STEP_OVER);
            sreq.enable();
            debugee.resume();

            waitStepEvent(sreq, waitTime);

            testedField = debugeeClass.fieldByName("testedField");
            val = (PrimitiveValue )debugeeClass.getValue(testedField);
            if (val.intValue() != redefineclasses031a.NEW_VALUES[i]) {
                exitStatus = Consts.TEST_FAILED;
                complain("actual value = " + val.intValue()
                            + ", expected " + redefineclasses031a.NEW_VALUES[i]);
            } else {
                display("actual value = " + val.intValue()
                            + ", expected " + redefineclasses031a.NEW_VALUES[i]);
            }

            debugee.getEventRequestManager().deleteEventRequest(sreq);
        }
        debugee.resume();

        display("\n=============");
        display("TEST FINISHES\n");
    }

    private void redefineDebugee(String newClassDir) {
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes;
        boolean alreadyComplained = false;
        display("\n" + newClassDir + ">>>");
        mapBytes = mapClassToBytes(testDir + File.separator + newClassDir + newClassFile);
        try {
            debugee.VM().redefineClasses(mapBytes);
        } catch (Exception e) {
            throw new Failure(UNEXPECTED_STRING + e);
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
            throw new Failure(UNEXPECTED_STRING + e);
        }

        try {
            inputFile.read(arrayToRedefine);
            inputFile.close();
        } catch (IOException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
        HashMap<com.sun.jdi.ReferenceType,byte[]> mapForClass = new HashMap<com.sun.jdi.ReferenceType,byte[]>();
        mapForClass.put(debugeeClass, arrayToRedefine);
        return mapForClass;
    }

    private BreakpointEvent waitBreakpointEvent(BreakpointRequest brkp, long waitTime) {
        Event event;
        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
        if (!(event instanceof BreakpointEvent )) {
            throw new Failure("BreakpointEvent didn't arrive");
        }

        return (BreakpointEvent )event;
    }

    private StepEvent waitStepEvent(StepRequest brkp, long waitTime) {
        Event event;
        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
        if (!(event instanceof StepEvent )) {
            throw new Failure("StepEvent didn't arrive");
        }

        return (StepEvent )event;
    }
}
