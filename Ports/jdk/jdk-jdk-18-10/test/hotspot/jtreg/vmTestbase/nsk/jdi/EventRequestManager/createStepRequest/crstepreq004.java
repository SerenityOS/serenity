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
public class crstepreq004 {

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
    private final static String className = ".crstepreq004";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------ immutable common methods

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + PASS_BASE);
    }

    //------------------------------------------------------ test specific fields

    static final int lineForBreakInThread = 146;
    static final int[] checkedLines = { 160, 160, 193 };
    static final int[] checkedLinesAlt = { 161, 161, 193 };

    //------------------------------------------------------ mutable common methods

    public static int run (String argv[], PrintStream out) {

        int exitStatus = new crstepreq004().runThis(argv, out);
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
            debuggee.redirectStdout(log, "debuggee stdout> ");
            debuggee.redirectStderr(log, "debuggee stderr> ");
            debuggee.createIOPipe();
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
        ThreadReference mainThread = debuggee.threadByNameOrThrow("main");

        BreakpointRequest bpRequest = setBreakpoint( mainThread,
                                                     debuggeeClass,
                                                     "methodForCommunication",
                                                     lineForBreakInThread,
                                                     "breakpointForCommunication");
        bpRequest.enable();

        display("TESTING BEGINS");

        label0:
        for (int testCase = 0; instruction != quit; testCase++) {

//            waitForEvent(bpRequest);
            instruction = getInstruction();
            if (instruction == quit) {
                vm.resume();
                break;
            }

            display(":: case: # " + testCase);

            switch (testCase) {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ test case section
                 case 0:
                     display("Step request will be created with size == StepRequest.STEP_LINE, depth == StepRequest.STEP_INTO");
                     setAndCheckStepEvent ( bpRequest,
                                            "StepRequest0",
                                            "thread2",
                                            testCase,
                                            StepRequest.STEP_INTO);
                     break;

                 case 1:
                     display("Step request will be created with size == StepRequest.STEP_LINE, depth == StepRequest.STEP_OVER");
                     setAndCheckStepEvent ( bpRequest,
                                            "StepRequest1",
                                            "thread2",
                                            testCase,
                                            StepRequest.STEP_OVER);
                     break;
                 case 2:
                     display("Step request will be created with size == StepRequest.STEP_LINE, depth == StepRequest.STEP_OUT");
                     setAndCheckStepEvent ( bpRequest,
                                            "StepRequest2",
                                            "thread2",
                                            testCase,
                                            StepRequest.STEP_OUT);
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ end of section
                 default:
                     instruction = quit;
                     setInstruction("quit");
            }
        }
        display("TESTING ENDS");
    }

    //--------------------------------------------------------- test specific methodss

    private StepRequest setStepRequest ( ThreadReference thread, int size, int depth, String property ) {
        display("Setting a step request in  thread: " + thread);
        StepRequest stepRequest = null;

        try {
            stepRequest = eventRManager.createStepRequest(thread, size, depth);
            stepRequest.putProperty("number", property);
        } catch ( Exception e1 ) {
            complain("setStepRequest(): unexpected Exception while creating StepRequest: " + e1);
            throw new Failure("setStep(): A StepRequest has not been set up.");
        }

        display("setStepRequest(): A StepRequest has been set up.");
        return stepRequest;
    }

    private void setAndCheckStepEvent ( BreakpointRequest bpRequest,
                                        String caseProperty,
                                        String threadName,
                                        int testCaseIndex,
                                        int stepDepth) {
        display("Wait for brakepoint event in " + threadName);
        BreakpointEvent bpEvent = (BreakpointEvent)waitForEvent(bpRequest);

        // check location of breakpoint event
        int lineOfEvent = ((LocatableEvent)bpEvent).location().lineNumber();
        if (lineOfEvent != lineForBreakInThread) {
            complain("Wrong line number of BreakpointEvent for " + threadName);
            complain("\texpected value : " + lineForBreakInThread + "; got one : " + lineOfEvent);
            exitCode = FAILED;
        }

        ThreadReference thread = debuggee.threadByNameOrThrow(threadName);
        StepRequest stepRequest = setStepRequest( thread,
                                                  StepRequest.STEP_LINE,
                                                  stepDepth,
                                                  caseProperty);
        stepRequest.enable();

        display("waiting for StepEvent in " + threadName);
        Event newEvent = waitForEvent(stepRequest);
        if (newEvent instanceof StepEvent) {
            String property = (String) newEvent.request().getProperty("number");
            display("got new StepEvent with property 'number' == " + property);

            if ( !property.equals(caseProperty) ) {
                complain("property is not : " + caseProperty);
                exitCode = FAILED;
            }
            // check location of step event
            lineOfEvent = ((LocatableEvent)newEvent).location().lineNumber();
            boolean isCorrectLine = lineOfEvent == checkedLines[testCaseIndex] || lineOfEvent == checkedLinesAlt[testCaseIndex];
            if (!isCorrectLine) {
                switch (stepDepth) {
                     case StepRequest.STEP_INTO:
                         complain("Wrong line number of StepEvent for request with depth == StepRequest.STEP_INTO:" );
                         break;
                     case StepRequest.STEP_OVER:
                         complain("Wrong line number of StepEvent for request with depth == StepRequest.STEP_OVER:" );
                         break;
                     case StepRequest.STEP_OUT:
                         complain("Wrong line number of StepEvent for request with depth == StepRequest.STEP_OUT:" );
                         break;
                }
                String msg = "\texpected line %d or %d; got %d";
                complain(String.format(msg, checkedLines[testCaseIndex], checkedLinesAlt[testCaseIndex], lineOfEvent));
                exitCode = FAILED;
            }

        } else if (newEvent instanceof BreakpointEvent) {
            vm.resume();
            exitCode = FAILED;
            complain("got unexpected BreakpointEvent, but StepEvent is not received");
        } else if (newEvent instanceof VMDeathEvent) {
            exitCode = FAILED;
            throw new Failure("got unexpected VMDeathtEvent, but StepEvent is not received");
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
