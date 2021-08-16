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
 * implements the following test case:                                   <br>
 *     Suite 3 - Hot Swap                                                <br>
 *     Test case:      TC5                                               <br>
 *     Description:    After point of execution, same method - stepping  <br>
 *     Steps:          1.Set breakpoint at line 24 (call to b()          <br>
 *                        from a())                                      <br>
 *                     2.Debug Main                                      <br>
 *                     3.Insert as next line after point of              <br>
 *                        execution: System.err.println("foo");          <br>
 *                     4.Smart Swap                                      <br>
 *                     5.F7 to step into                                 <br>
 *                        X. Steps into method b()                       <br>
 *                                                                       <br>
 * The description was drown up according to steps under JBuilder.
 *
 * Of course, the test has own line numbers and method/class names and
 * works as follow:
 * When the test is starting debugee, debugger sets breakpoint at
 * the 38th line (method <code>method_A</code>).
 * After the breakpoint is reached, debugger redefines debugee adding
 * a new line into <code>method_A</code>, creates <code>StepRequest</code> and
 * resumes debugee. When the location of the current <code>StepEvent</code> is
 * in <code>method_B</code>, created <code>StepRequest</code> is disabled.
 * Working of debugger and debugee is synchronized via IOPipe channel.
 */

public class tc05x001 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.BScenarios.hotswap.";
    private final static String debuggerName = prefix + "tc05x001";
    private final static String debugeeName = debuggerName + "a";

    public final static String PERFORM_SGL = "perform";

    private final static String newClassFile = "newclass" + File.separator
                    + debugeeName.replace('.',File.separatorChar)
                    + ".class";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String testDir;

    private static final String firstMethodName = "method_B";

    private int eventCount;
    private int expectedEventCount = 2;
    private ClassType debugeeClass;
    StepRequest stepRequest = null;

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

        tc05x001 thisTest = new tc05x001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        testDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (Throwable e) {
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

        display("Tested class\t:" + debugeeClass.name());

        BreakpointRequest brkp = debugee.setBreakpoint(debugeeClass,
                                                    tc05x001a.brkpMethodName,
                                                    tc05x001a.brkpLineNumber);
        debugee.resume();
        debugee.sendSignal(PERFORM_SGL);

        BreakpointEvent brkpEvent = waitBreakpointEvent(brkp, waitTime);

        display("\nredefining...");
        redefineDebugee();

        ThreadReference thread = brkpEvent.thread();

        EventRequestManager evm = debugee.getEventRequestManager();

        display("creating step request INTO...");
        stepRequest = evm.createStepRequest(thread, StepRequest.STEP_LINE,
                                                    StepRequest.STEP_INTO);
        stepRequest.addClassFilter(debugeeClass);
        stepRequest.enable();
        debugee.resume();

        boolean cmp = false;
        StepEvent stepEvent;
        Event event;
        for (int i = 0; i < 3; i++) {
            // waiting the breakpoint event
            display("waiting step event...");
            try {
                event = debugee.waitingEvent(stepRequest, waitTime);
            } catch (Exception e) {
                stepRequest.disable();
                throw new Failure(UNEXPECTED_STRING + e);
            }
            if (!(event instanceof StepEvent )) {
                stepRequest.disable();
                throw new Failure("StepEvent didn't arrive");
            }
            if (hitStep((StepEvent )event)) {
                break;
            }
            debugee.resume();
        }
        evm.deleteEventRequest(stepRequest);
        debugee.resume();

        display("=============");
        display("TEST FINISHES\n");
    }

    private void redefineDebugee() {
        Map<com.sun.jdi.ReferenceType,byte[]> mapBytes;
        boolean alreadyComplained = false;
        mapBytes = mapClassToBytes(newClassFile);
        try {
            debugee.VM().redefineClasses(mapBytes);
        } catch (Exception e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
    }

    private Map<com.sun.jdi.ReferenceType,byte[]> mapClassToBytes(String fileName) {
        display("class-file\t:" + fileName);
        File fileToBeRedefined = new File(testDir + File.separator + fileName);
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

    private boolean hitStep(StepEvent event) {
        locationInfo(event);
        String methodName = event.location().method().name();
        boolean ret = methodName.compareTo(firstMethodName) != 0;
        if (ret) {
            if (!event.location().method().isObsolete()) {
                complain("Unexpected event" + event);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("!!!Expected event location at \"" + methodName + "\"");
            }
        } else {
            display("!!!Expected event location at \"" + methodName + "\"");
            stepRequest.disable();
        }
        display("");
        return !ret;
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
