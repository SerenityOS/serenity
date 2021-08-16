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
 * implements the following test case:
 *     Suite 3 - Hot Swap
 *     Test case:      TC2
 *     Description:    Before point of execution, same method
 *     Steps:          1.Set breakpoint at line 24 (call to b()
 *                        from a())
 *                     2.Debug Main
 *                     3.Insert as line before point of
 *                        execution: System.err.println("foo");
 *                     4.Smart Swap
 *                     5.Set Smart PopFrame to beginging of
 *                        method a()
 *                     6.Resume
 *                        X. Prints out "foo" and hits breakpoint
 *                        on line 24
 *                     7.Resume
 *                        X. Prints numbers
 * The description was drown up according to steps under JBuilder.
 *
 * Of course, the test has own line numbers and method/class names and
 * works as follow:
 * When the test is starting debugee, debugger sets breakpoint at
 * the 38th line (method <code>method_A</code>).
 * After the breakpoint is reached, debugger:                   <br>
 *     - redefines debugee adding a new line into "method_A",   <br>
 *     - pops current frame,                                    <br>
 *     - sets breakpoint at the 38th line again                 <br>
 * and resumes debugee.
 * It is expected the new code will be actual after the redefining and
 * it doesn't hit breakpoint according to JVMDI Redefine Classes spec :
 * "...All breakpoints in the class are cleared."
 */

public class tc02x002 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.BScenarios.hotswap.";
    private final static String debuggerName = prefix + "tc02x002";
    private final static String debugeeName = debuggerName + "a";

    private final static String newClassFile = "newclass" + File.separator
                    + debugeeName.replace('.',File.separatorChar)
                    + ".class";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String classDir;

    private int expectedEventCount = 6;
    private int eventCount = 0;

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

        tc02x002 thisTest = new tc02x002();

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
                                                    tc02x002a.brkpMethodName,
                                                    tc02x002a.brkpLineNumber);

                        debugee.resume();
                        eventCount++;

                    } else if (event instanceof BreakpointEvent) {
                        display("\n event ===>>> " + event);
                        switch (eventCount) {
                        case 1:
                            display("redefining...");
                            redefineDebugee();
                            popFrames(((BreakpointEvent )event).thread());

                            debugee.setBreakpoint(debugeeClass,
                                                        tc02x002a.brkpMethodName,
                                                        tc02x002a.brkpLineNumber);
                            break;

                        case 2:
                            display("!!!Expected second breakpoint event!!!");
                            createMethodExitRequest(debugeeClass);
                            break;

                        default:
                            complain("Unexpected breakpoint event");
                            exitStatus = Consts.TEST_FAILED;
                        }
                        eventCount++;
                        hitBreakpoint((BreakpointEvent )event);
                        debugee.resume();

                    } else if (event instanceof MethodExitEvent) {
                        display("\n event ===>>> " + event);
                        hitMethodExit((MethodExitEvent)event);
                        eventCount++;
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

    private void popFrames(ThreadReference thread) {
        try {
            StackFrame frame = thread.frame(0);
            thread.popFrames(frame);
        } catch (IncompatibleThreadStateException e) {
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

    private MethodExitRequest createMethodExitRequest(ReferenceType refType) {
        EventRequestManager evm = debugee.getEventRequestManager();
        MethodExitRequest request = evm.createMethodExitRequest();
        request.addClassFilter(refType);
        request.enable();
        return request;
    }

    private void hitBreakpoint(BreakpointEvent event) {
        locationInfo(event);
        if (event.location().lineNumber() != tc02x002a.checkLastLine) {
            complain("BreakpointEvent steps to line " + event.location().lineNumber()
                        + ", expected line number is "
                        + tc02x002a.checkLastLine);
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("!!!BreakpointEvent steps to the expected line "
                        + event.location().lineNumber() + "!!!");
        }
        display("");
    }

    private void hitMethodExit(MethodExitEvent event) {

        locationInfo(event);

        Method mthd = ((MethodExitEvent )event).method();
        if (mthd.name().compareTo(tc02x002a.brkpMethodName) == 0) {
            Field fld = debugeeClass.fieldByName(tc02x002a.fieldToCheckName);
            Value val = debugeeClass.getValue(fld);
            if (((IntegerValue )val).value() != tc02x002a.CHANGED_VALUE) {
                complain("Unexpected: new code is not actual "
                            + "after resetting frames");
                complain("Unexpected value of checked field: "
                            + val);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("!!!Expected: new code is actual "
                            + "after resetting frames!!!");
            }
        }
        display("");
    }

    private void locationInfo(LocatableEvent event) {
        display("event info:");
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("\tmethod\t- " + event.location().method().name());
        display("\tline\t- " + event.location().lineNumber());
    }
}
