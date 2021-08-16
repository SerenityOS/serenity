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

package nsk.jdi.BScenarios.hotswap;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * This test is from the group of so-called Borland's scenarios and
 * implements the following test case:                                       <br>
 *     Suite 3 - Hot Swap                                                    <br>
 *     Test case:      TC8                                                   <br>
 *     Description:    Before point of execution, different method - stepping<br>
 *     Steps:          1.Set breakpoint at line 30 (call to c()              <br>
 *                        from b())                                          <br>
 *                     2.Debug Main                                          <br>
 *                     3.Insert as first line in a():                        <br>
 *                        System.err.println("foo");                         <br>
 *                     4.Smart Swap                                          <br>
 *                     5.Set Smart PopFrame to beginging of                  <br>
 *                        method a()                                         <br>
 *                     6.F7 to step into                                     <br>
 *                        X. Steps into a()                                  <br>
 *                     7.F7 to step into                                     <br>
 *                        X. Prints "foo"                                    <br>
 *                                                                           <br>
 * The description was drown up according to steps under JBuilder.           <br>
 *                                                                           <br>
 * Of course, the test has own line numbers and method/class names and       <br>
 * works as follow:
 * When the test is starting debugee, debugger sets breakpoint at
 * the 38th line (method <code>method_A</code>).
 * After the breakpoint is reached, debugger redefines debugee adding
 * a new line into <code>method_A</code>, pops frame, creates <code>StepRequest</code>
 * and resumes debugee. When the second <code>StepEvent</code> is reached, created
 * <code>StepRequest</code> is disabled.
 */

public class tc08x001 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.BScenarios.hotswap.";
    private final static String debuggerName = prefix + "tc08x001";
    private final static String debugeeName = debuggerName + "a";

    private final static String newClassFile = "newclass" + File.separator
                    + debugeeName.replace('.',File.separatorChar)
                    + ".class";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String classDir;

    private static final String firstMethodName = "method_A";

    private int eventCount;
    private int expectedEventCount = 3;
    private ReferenceType debugeeClass;

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

        tc08x001 thisTest = new tc08x001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        classDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);

        try {
            thisTest.execTest();
        } catch (Throwable e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.endDebugee();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {

        if (!debugee.VM().canRedefineClasses()) {
            display("\n>>>canRedefineClasses() is false<<< test canceled.\n");
            return;
        }

        display("\nTEST BEGINS");
        display("===========");

        EventSet eventSet = null;
        EventIterator eventIterator = null;
        Event event;
        long totalTime = waitTime;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;

        eventCount = 0;
        EventRequestManager evm = debugee.getEventRequestManager();
        ClassPrepareRequest req = evm.createClassPrepareRequest();
        req.addClassFilter(debugeeName);
        req.enable();
        debugee.resume();

        while (totalTime > 0 && !exit) {
            if (eventIterator == null || !eventIterator.hasNext()) {
                try {
                    eventSet = debugee.VM().eventQueue().remove(totalTime);
                } catch (InterruptedException e) {
                    new Failure(e);
                }
                if (eventSet != null) {
                    eventIterator = eventSet.eventIterator();
                } else {
                    eventIterator = null;
                }
            }
            if (eventIterator != null) {
                while (eventIterator.hasNext()) {
                    event = eventIterator.nextEvent();
//                    display("\n event ===>>> " + event);

                    if (event instanceof ClassPrepareEvent) {
                        display("\n event ===>>> " + event);
                        debugeeClass = ((ClassPrepareEvent )event).referenceType();
                        display("Tested class\t:" + debugeeClass.name());
                        debugee.setBreakpoint(debugeeClass,
                                                    tc08x001a.brkpMethodName,
                                                    tc08x001a.brkpLineNumber);

                        debugee.resume();

                    } else if (event instanceof BreakpointEvent) {
                        display("\n event ===>>> " + event);
                        hitBreakpoint((BreakpointEvent )event);
                        display("redefining...");
                        redefineDebugee();
                        popFrames(((BreakpointEvent )event).thread());
                        createStepRequest(((LocatableEvent )event).thread());
                        debugee.resume();

                    } else if (event instanceof StepEvent) {
                        display("\n event ===>>> " + event);
                        hitStep((StepEvent )event);
                        debugee.resume();

                    } else if (event instanceof VMDeathEvent) {
                        exit = true;
                        break;
                    } else if (event instanceof VMDisconnectEvent) {
                        exit = true;
                        break;
                    } // if
                } // while
            } // if
            tmp = System.currentTimeMillis();
            delta = tmp - begin;
            totalTime -= delta;
                begin = tmp;
        }

        if (eventCount != expectedEventCount) {
            if (totalTime <= 0) {
                complain("out of wait time...");
            }
            complain("expecting " + expectedEventCount
                        + " events, but "
                        + eventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");
    }

    private void redefineDebugee() {
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes;
        boolean alreadyComplained = false;
        mapBytes = mapClassToBytes(newClassFile);
        try {
            debugee.VM().redefineClasses(mapBytes);
        } catch (Exception e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
    }

    private Map<? extends com.sun.jdi.ReferenceType,byte[]> mapClassToBytes(String fileName) {
        display("class-file\t:" + fileName);
        File fileToBeRedefined = new File(classDir + File.separator + fileName);
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

    private void popFrames(ThreadReference thread) {
        display("\npop frames...");
        try {
            StackFrame frame = thread.frame(0);
            Method mthd = frame.location().method();
            do {
                thread.popFrames(frame);
                display(mthd.name() + " is resetted");
                frame = thread.frame(0);
                mthd = frame.location().method();
            } while (mthd.isObsolete());
        } catch (IncompatibleThreadStateException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
        display("");
    }

    private StepRequest createStepRequest(ThreadReference thread) {
        EventRequestManager evm = debugee.getEventRequestManager();
        StepRequest request = evm.createStepRequest(thread,
                                                        StepRequest.STEP_LINE,
                                                        StepRequest.STEP_INTO);
        request.enable();
        return request;
    }

    private void hitBreakpoint(BreakpointEvent event) {
        locationInfo(event);
        if (event.location().lineNumber() != tc08x001a.checkLastLine) {
            complain("BreakpointEvent steps to line " + event.location().lineNumber()
                        + ", expected line number is "
                        + tc08x001a.checkLastLine);
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("!!!BreakpointEvent steps to the expected line "
                        + event.location().lineNumber() + "!!!");
        }
        display("");
    }

    private void hitStep(StepEvent event) {
        locationInfo(event);
        String methodName = event.location().method().name();
        StepRequest request = (StepRequest )event.request();

        switch (eventCount) {
        case 2:
            checkLocMethod(methodName, firstMethodName);
            break;
        case 3:
            checkLocMethod(methodName, firstMethodName);
            Field fld = debugeeClass.fieldByName(tc08x001a.fieldToCheckName);
            Value val = debugeeClass.getValue(fld);
            if (((IntegerValue )val).value() != tc08x001a.CHANGED_VALUE) {
                complain("Unexpected: new code is not actual");
                complain("Unexpected value of checked field: "
                            + val);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("!!!Expected: Inserted line has worked");
            }
            request.disable();
            break;
        default:
            complain("Unexpected event" + event);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");
    }

    private void checkLocMethod(String currentMethodName, String expectedMethodName) {
        if (currentMethodName.compareTo(expectedMethodName) != 0) {
            complain("Unexpected event location at \"" + currentMethodName + "\"");
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("!!!Expected event location at \"" + currentMethodName + "\"");
        }
    }

    private void locationInfo(LocatableEvent event) {
        eventCount++;
        display("event info: #" + eventCount);
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
            display("\tmethod\t- " + event.location().method().name());
            display("\tline\t- " + event.location().lineNumber());
        } catch (AbsentInformationException e) {
        }
    }
}
