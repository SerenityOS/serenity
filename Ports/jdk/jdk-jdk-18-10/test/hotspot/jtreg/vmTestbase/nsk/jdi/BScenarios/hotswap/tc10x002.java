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
 * implements the following test case:                                      <br>
 *     Suite 3 - Hot Swap                                                   <br>
 *     Test case:      TC10                                                 <br>
 *     Description:    Changing data type                                   <br>
 *     Steps:          1.Set breakpoint on line 20                          <br>
 *                     2.Debug Main                                         <br>
 *                        X. Stops on line 20                               <br>
 *                     3.Change line 16 to boolean i = true;                <br>
 *                     4.Smart Swap                                         <br>
 *                     5.Resume                                             <br>
 *                        X. Prints "true" as last line of output           <br>
 *                                                                          <br>
 * The description was drown up according to steps under JBuilder.
 *
 * Of course, the test has own line numbers and method/class names and
 * works as follow:
 * When the test is starting debugee, debugger sets breakpoint at
 * the 40th line (method <code>runIt</code>).
 * Note: the <code>runIt</code> method was added into the test, to allow invoking
 * <code>ThreadReference.popFrame()</code>. Current frame can not be reset because of
 * there is no previous frame.
 * After the breakpoint is reached, debugger redefines debugee changing type of
 * local variable, pops current frame, sets a new breakpoint at 37th line and
 * resumes debugee.
 * When the second breakpoint hits, debugger requests value of the local
 * variable <code>i</code> and compares got value with expected value.
 * In this case expected value is the new value after redefinition.
 */

public class tc10x002 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.BScenarios.hotswap.";
    private final static String debuggerName = prefix + "tc10x002";
    private final static String debugeeName = debuggerName + "a";

    private final static String newClassFile = "newclass" + File.separator
                    + debugeeName.replace('.',File.separatorChar)
                    + ".class";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String classDir;

    private int eventCount;
    private int expectedEventCount = 2;
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

        tc10x002 thisTest = new tc10x002();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        classDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);
        debugee.createIOPipe();

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
        BreakpointRequest brkpRequest;
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
            while (eventIterator.hasNext()) {
                event = eventIterator.nextEvent();
//                display("\n event ===>>> " + event);

                if (event instanceof ClassPrepareEvent) {
                    display("\n event ===>>> " + event);
                    debugeeClass = ((ClassPrepareEvent )event).referenceType();
                    display("Tested class\t:" + debugeeClass.name());

                    brkpRequest = debugee.setBreakpoint(debugeeClass,
                                                tc10x002a.brkpMethodName,
                                                tc10x002a.brkpLineNumber1);
                    debugee.resume();

                } else if (event instanceof BreakpointEvent) {
                    display("\n event ===>>> " + event);
                    BreakpointEvent brkpEvent = (BreakpointEvent )event;

                    switch (eventCount) {
                    case 0:
                        hitBreakpoint(brkpEvent, tc10x002a.checkLastLine1);
                        display("redefining...");
                        redefineDebugee();

                        popFrames(brkpEvent.thread());

                        brkpRequest = debugee.setBreakpoint(debugeeClass,
                                                    tc10x002a.brkpMethodName,
                                                    tc10x002a.brkpLineNumber2);
                        break;
                    case 1:
                        Value val = null;
                        hitBreakpoint(brkpEvent, tc10x002a.checkLastLine2);
                        try {
                            StackFrame frame = brkpEvent.thread().frame(0);
                            LocalVariable var = frame.visibleVariableByName("i");
                            display("local variable:\t" + var);
                            val = frame.getValue(var);
                        } catch (Exception e) {
                            throw new Failure(UNEXPECTED_STRING + e);
                        }
                        display("\nValue of local variable:\t" + val);
                        if (((PrimitiveValue)val).booleanValue() ==
                                    tc10x002a.INITIAL_BOOL_VALUE) {
                            display("!!!Expected value of local variable!!!\n");
                        } else {
                            complain("Unexpected value of local variable.\n");
                            exitStatus = Consts.TEST_FAILED;
                        }
                        break;
                    }
                    debugee.resume();

                } else if (event instanceof VMDeathEvent) {
                    exit = true;
                    break;
                } else if (event instanceof VMDisconnectEvent) {
                    exit = true;
                    break;
                } // if
            } // while
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

    private void hitBreakpoint(BreakpointEvent event, int expLine) {
        eventInfo(event);
        if (event.location().lineNumber() == expLine) {
            display("!!!BreakpointEvent steps to the expected line "
                        + event.location().lineNumber() + "!!!");
        } else {
            complain("BreakpointEvent steps to line " + event.location().lineNumber()
                        + ", expected line number is "
                        + expLine);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");
    }

    private void eventInfo(LocatableEvent event) {
        eventCount++;
        display("event info: #" + eventCount);
        display("\tthread\t- " + event.thread().name());
        locationInfo(event);
    }

    private void breakpointInfo(BreakpointRequest request) {
        display("breakpoint info: ");
        display("\tis enabled - " + request.isEnabled());
        locationInfo(request);
    }

    private void locationInfo(Locatable loc) {
        try {
            display("\tsource\t- " + loc.location().sourceName());
            display("\tmethod\t- " + loc.location().method().name());
            display("\tline\t- " + loc.location().lineNumber());
        } catch (AbsentInformationException e) {
            display("***information is not available***");
        }
    }

    private MethodExitRequest createMethodExitRequest(ReferenceType refType) {
        EventRequestManager evm = debugee.getEventRequestManager();
        MethodExitRequest request = evm.createMethodExitRequest();
        request.addClassFilter(refType);
        request.enable();
        return request;
    }
}
