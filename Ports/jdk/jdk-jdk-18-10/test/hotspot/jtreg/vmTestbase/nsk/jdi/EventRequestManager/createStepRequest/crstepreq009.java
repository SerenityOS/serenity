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

package nsk.jdi.EventRequestManager.createStepRequest;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import java.io.*;
import java.util.*;

/**
 */
public class crstepreq009 {

    //----------------------------------------------------- immutable common fields

    static final int PASSED    = 0;
    static final int FAILED    = 2;
    static final int PASS_BASE = 95;
    static final int quit      = -1;

    private int instruction = 1;
    private int waitTime;
    private static int exitCode = PASSED;

    private ArgumentHandler     argHandler;
    private Log                 log;
    private Debugee             debuggee;
    private VirtualMachine      vm;
    private ReferenceType       debuggeeClass;

    private EventRequestManager eventRManager;
    private EventSet            eventSet;
    private EventIterator       eventIterator;

    //------------------------------------------------------ mutable common fields

    private final static String prefix = "nsk.jdi.EventRequestManager.createStepRequest";
    private final static String className = ".crstepreq009";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";
    static final int lineForBreak = 62;

    //------------------------------------------------------ immutable common methods

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + PASS_BASE);
    }

    //------------------------------------------------------ test specific fields

    static final int maxCase = 3;
    static final String[] brakeMethods = {
        "m00",
        "m00",
        "m00"
                                        };
    static final int[][] checkedLines = {
        { 167, 171, 151},
        { 167, 175, 175},
        { 167, 178, 178}
                                        };

    static final String debuggeeThreadName = prefix + ".Thread0crstepreq009a";

    //------------------------------------------------------ mutable common methods

    public static int run (String argv[], PrintStream out) {

        int exitStatus = new crstepreq009().runThis(argv, out);
        System.out.println (exitStatus == PASSED ? "TEST PASSED" : "TEST FAILED");
        return exitCode;
    }

    private int runThis(String argv[], PrintStream out) {

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        try {

            Binder binder = new Binder(argHandler, log);
            debuggee = binder.bindToDebugee(debuggeeName);
            debuggee.redirectStderr(log, "");
            eventRManager = debuggee.getEventRequestManager();

            vm = debuggee.VM();
            eventRManager = vm.eventRequestManager();

            debuggeeClass = waitForDebuggeeClassPrepared();

            execTest();

            debuggee.resume();
            getEventSet();
            if (eventIterator.nextEvent() instanceof VMDeathEvent) {
                display("Waiting for the debuggee's finish...");
                debuggee.waitFor();

                display("Getting the debuggee's exit status.");
                int status = debuggee.getStatus();
                if (status != (PASSED + PASS_BASE)) {
                    complain("Debuggee returned UNEXPECTED exit status: " + status);
                    exitCode = Consts.TEST_FAILED;
                }
            } else {
                throw new TestBug("Last event is not the VMDeathEvent");
            }

        } catch (VMDisconnectedException e) {
            exitCode = Consts.TEST_FAILED;
            complain("The test cancelled due to VMDisconnectedException.");
            e.printStackTrace(out);
            display("Trying: vm.process().destroy();");
            if (vm != null) {
                Process vmProcess = vm.process();
                if (vmProcess != null) {
                    vmProcess.destroy();
                }
            }

        } catch (Exception e) {
            exitCode = Consts.TEST_FAILED;
            complain("Unexpected Exception: " + e.getMessage());
            e.printStackTrace(out);
            complain("The test has not finished normally. Forcing: vm.exit().");
            if (vm != null) {
                vm.exit(PASSED + PASS_BASE);
            }
            debuggee.resume();
            getEventSet();
        }

        return exitCode;
    }

    //--------------------------------------------------------- mutable common methods

    private void execTest() {
        BreakpointRequest bpRequest = setBreakpoint( null,
                                                     debuggeeClass,
                                                     "methodForCommunication",
                                                     lineForBreak,
                                                     "breakForCommunication");
        bpRequest.enable();

        StepRequest stepRequest = null;

        display("TESTING BEGINS");
        for (int testCase = 0; testCase < maxCase && instruction != quit; testCase++) {

            instruction = getInstruction();
            if (instruction == quit) {
                vm.resume();
                break;
            }

            display(":: CASE # " + testCase);
            stepRequest = setStepRequest( bpRequest,
                                          "thread" + testCase,
                                          testCase,
                                          "stepRequest" + testCase );

            checkStepEvent( stepRequest,
                            "thread" + testCase,
                            testCase );
        }
        display("TESTING ENDS");
    }

    //--------------------------------------------------------- test specific methods

    private StepRequest setStepRequest ( BreakpointRequest bpRequest,
                                         String threadName,
                                         int testCase,
                                         String property ) {
        StepRequest stepRequest = null;
        for (;;) {
            display("Wait for initial brakepoint event in " + threadName);
            BreakpointEvent bpEvent = (BreakpointEvent)waitForEvent(bpRequest);

            // check location of breakpoint event
            int lineOfEvent = ((LocatableEvent)bpEvent).location().lineNumber();
            if (lineOfEvent != lineForBreak) {
                complain("Wrong line number of initial brakepoint event for " + threadName);
                complain("\texpected value : " + lineForBreak + "; got one : " + lineOfEvent);
                break;
            }

            display("Getting mirror of thread: " + threadName);
            ThreadReference thread = debuggee.threadByNameOrThrow(threadName);

            display("Getting ReferenceType of thread: " + threadName);
            ReferenceType debuggeeThread = debuggee.classByName(debuggeeThreadName);

            // set second breakpoint to suspend checked thread at the right location before
            // setting step request
            BreakpointRequest bpRequest1 = setBreakpoint( thread,
                                                         debuggeeThread,
                                                         brakeMethods[testCase],
                                                         checkedLines[testCase][0],
                                                         "");
            bpRequest1.addCountFilter(1);
            bpRequest1.enable();

            display("Wait for additional brakepoint event in " + threadName);
            bpEvent = (BreakpointEvent)waitForEvent(bpRequest1);

            // check location of breakpoint event
            lineOfEvent = ((LocatableEvent)bpEvent).location().lineNumber();
            if (lineOfEvent != checkedLines[testCase][0]) {
                complain("Wrong line number of additional brakepoint event for " + threadName);
                complain("\texpected value : " + checkedLines[testCase][0] + "; got one : " + lineOfEvent);
                break;
            }

            display("Setting a step request in  thread: " + thread);
            try {
                stepRequest = eventRManager.createStepRequest ( thread,
                                                                StepRequest.STEP_MIN,
                                                                StepRequest.STEP_OUT );
                stepRequest.putProperty("number", property);
            } catch ( Exception e1 ) {
                complain("setStepRequest(): unexpected Exception while creating StepRequest: " + e1);
                break;
            }
            break;
        }
        if (stepRequest == null) {
            throw new Failure("setStepRequest(): StepRequest has not been set up.");
        }
        display("setStepRequest(): StepRequest has been set up.");
        return stepRequest;
    }

    private void checkStepEvent ( StepRequest stepRequest,
                                  String threadName,
                                  int testCase ) {
        stepRequest.enable();

        display("waiting for first StepEvent in " + threadName);
        Event newEvent = waitForEvent(stepRequest);
        display("got first StepEvent");

        display("CHECK1 for line location of first StepEvent.");
        int lineOfEvent = ((LocatableEvent)newEvent).location().lineNumber();
        if (lineOfEvent != checkedLines[testCase][1]) {
            complain("CHECK1 for line location of first StepEvent FAILED for CASE # " + testCase);
            complain("\texpected value : " + checkedLines[testCase][1] + "; got one : " + lineOfEvent);
            exitCode = FAILED;
        } else {
            display("CHECK1 PASSED");
        }

        display("waiting for second StepEvent in " + threadName);
        newEvent = waitForEvent(stepRequest);
        display("got second StepEvent");

        display("CHECK2 for line location of second StepEvent.");
        lineOfEvent = ((LocatableEvent)newEvent).location().lineNumber();
        if (lineOfEvent != checkedLines[testCase][2]) {
            complain("CHECK2 for line location of second StepEvent FAILED for CASE # " + testCase);
            complain("\texpected value : " + checkedLines[testCase][2] + "; got one : " + lineOfEvent);
            exitCode = FAILED;
        } else {
            display("CHECK2 PASSED");
        }

        stepRequest.disable();
        eventRManager.deleteEventRequest(stepRequest);
        stepRequest = null;
        display("request for StepEvent in " + threadName + " is deleted");
    }

    //--------------------------------------------------------- immutable common methods

    void display(String msg) {
        log.display("debugger > " + msg);
    }

    void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

   /**
    * Sets up a breakpoint at given line number within a given method in a given class
    * for a given thread.
    *
    * Returns a BreakpointRequest object in case of success, otherwise throws Failure.
    */
    private BreakpointRequest setBreakpoint ( ThreadReference thread,
                                              ReferenceType testedClass,
                                              String methodName,
                                              int bpLine,
                                              String property) {

        display("Setting a breakpoint in :");
        display("  thread: " + thread + "; class: " + testedClass +
                "; method: " + methodName + "; line: " + bpLine + "; property: " + property);

        List allLineLocations = null;
        Location lineLocation = null;
        BreakpointRequest breakpRequest = null;

        try {
            Method  method  = (Method) testedClass.methodsByName(methodName).get(0);

            allLineLocations = method.allLineLocations();

            display("Getting location for breakpoint...");
            Iterator locIterator = allLineLocations.iterator();
            while (locIterator.hasNext()) {
                Location curLocation = (Location)locIterator.next();
                int curNumber = curLocation.lineNumber();
                if (curLocation.lineNumber() == bpLine) {
                    lineLocation = curLocation;
                    break;
                }
            }
            if (lineLocation == null) {
                throw new TestBug("Incorrect line number of methods' location");
            }

            try {
                breakpRequest = eventRManager.createBreakpointRequest(lineLocation);
                if (thread != null) {
                    breakpRequest.addThreadFilter(thread);
                }
                breakpRequest.putProperty("number", property);
            } catch ( Exception e1 ) {
                complain("setBreakpoint(): unexpected Exception while creating BreakpointRequest: " + e1);
                breakpRequest = null;
            }
        } catch ( Exception e2 ) {
            complain("setBreakpoint(): unexpected Exception while getting locations: " + e2);
            breakpRequest = null;
        }

        if (breakpRequest == null) {
            throw new Failure("setBreakpoint(): A breakpoint has not been set up.");
        }

        display("setBreakpoint(): A breakpoint has been set up.");
        return breakpRequest;
    }

    private Event waitForEvent (EventRequest eventRequest) {
        vm.resume();
        Event resultEvent = null;
        try {
            eventSet = null;
            eventIterator = null;
            eventSet = vm.eventQueue().remove(waitTime);
            if (eventSet == null) {
                throw new Failure("TIMEOUT while waiting for an event");
            }
            eventIterator = eventSet.eventIterator();
            while (eventIterator.hasNext()) {
                Event curEvent = eventIterator.nextEvent();
                if (curEvent instanceof VMDisconnectEvent) {
                    throw new Failure("Unexpected VMDisconnectEvent received.");
                } else {
                    EventRequest evRequest = curEvent.request();
                    if (evRequest != null && evRequest.equals(eventRequest)) {
                        display("Requested event received: " + curEvent.toString() +
                            "; request property: " + (String) curEvent.request().getProperty("number"));
                        resultEvent = curEvent;
                        break;
                    } else {
                        throw new Failure("Unexpected event received: " + curEvent.toString());
                    }
                }
            }
        } catch (Exception e) {
            throw new Failure("Unexpected exception while waiting for an event: " + e);
        }
        return resultEvent;
    }

    private Event waitForEvent () {
        vm.resume();
        Event resultEvent = null;
        try {
            eventSet = null;
            eventIterator = null;
            eventSet = vm.eventQueue().remove(waitTime);
            if (eventSet == null) {
                throw new Failure("TIMEOUT while waiting for an event");
            }
            eventIterator = eventSet.eventIterator();
            while (eventIterator.hasNext()) {
                resultEvent = eventIterator.nextEvent();
                if (resultEvent instanceof VMDisconnectEvent) {
                    throw new Failure("Unexpected VMDisconnectEvent received.");
                }
            }
        } catch (Exception e) {
            throw new Failure("Unexpected exception while waiting for an event: " + e);
        }
        return resultEvent;
    }

    private void getEventSet() {
        try {
            eventSet = vm.eventQueue().remove(waitTime);
            if (eventSet == null) {
                throw new Failure("TIMEOUT while waiting for an event");
            }
            eventIterator = eventSet.eventIterator();
        } catch (Exception e) {
            throw new Failure("getEventSet(): Unexpected exception while waiting for an event: " + e);
        }
    }

    private ReferenceType waitForDebuggeeClassPrepared () {
        display("Creating request for ClassPrepareEvent for debuggee.");
        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.addClassFilter(debuggeeName);
        cpRequest.addCountFilter(1);
        cpRequest.enable();

        ClassPrepareEvent event = (ClassPrepareEvent) waitForEvent(cpRequest);
        cpRequest.disable();

        if (!event.referenceType().name().equals(debuggeeName)) {
           throw new Failure("Unexpected class name for ClassPrepareEvent : " + debuggeeClass.name());
        }
        return event.referenceType();
    }

    private int getInstruction () {
        if (debuggeeClass == null) {
            throw new Failure("getInstruction() :: debuggeeClass reference is null");
        }
        return ((IntegerValue) (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();
    }

    private void setInstruction (String instructionField) {
        if (debuggeeClass == null) {
            throw new Failure("getInstruction() :: debuggeeClass reference is null");
        }
        Field instrField = debuggeeClass.fieldByName("instruction");
        IntegerValue instrValue = (IntegerValue) (debuggeeClass.getValue(debuggeeClass.fieldByName(instructionField)));
        try {
            ((ClassType)debuggeeClass).setValue(instrField, instrValue );
        } catch (InvalidTypeException e1) {
            throw new Failure("Caught unexpected InvalidTypeException while setting value '" + instructionField + "' for instruction field");
        } catch (ClassNotLoadedException e2) {
            throw new Failure("Caught unexpected ClassNotLoadedException while setting value '" + instructionField + "' for instruction field");
        }
    }
}
