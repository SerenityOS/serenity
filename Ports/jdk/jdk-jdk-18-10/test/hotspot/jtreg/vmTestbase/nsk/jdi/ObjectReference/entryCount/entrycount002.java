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

package nsk.jdi.ObjectReference.entryCount;

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
public class entrycount002 {

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

    private final static String prefix = "nsk.jdi.ObjectReference.entryCount.";
    private final static String className = "entrycount002";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";
    static final int lineForBreak = 62;

    //------------------------------------------------------ immutable common methods

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + PASS_BASE);
    }

    //------------------------------------------------------ test specific fields

    //------------------------------------------------------ mutable common methods

    public static int run (String argv[], PrintStream out) {

        int exitStatus = new entrycount002().runThis(argv, out);
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
        ThreadReference mainThread = debuggee.threadByName("main");

        BreakpointRequest bpRequest = debuggee.makeBreakpoint(debuggeeClass,
                                                            "methodForCommunication",
                                                            lineForBreak);
        bpRequest.addThreadFilter(mainThread);
        bpRequest.putProperty("number", "breakpointForCommunication");
        bpRequest.enable();

        display("TESTING BEGINS");

        label0:
        for (int testCase = 0; instruction != quit; testCase++) {

            waitForEvent(bpRequest);
            instruction = getInstruction();
            if (instruction == quit) {
                vm.resume();
                break;
            }

            display(":: case: # " + testCase);

            switch (testCase) {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ test case section
                 case 0:

                     if (vm.canGetMonitorInfo()) {
                         // create method entry request to track the moments of locking of entrycount002.lockObj
                         MethodEntryRequest meRequest = eventRManager.createMethodEntryRequest();
                         meRequest.addClassFilter(debuggeeName + "Lock");
                         meRequest.enable();
                         display("methodEntryRequest is enabled ");

                         String fieldName = "lockObj";
    //                     display("CHECK1 : checking waitingThreads method for ObjectReference of waitingthreads003a." + fieldName);
                         ObjectReference lockRef = (ObjectReference) debuggeeClass.getValue(debuggeeClass.fieldByName(fieldName));

                         for (int i = 1; i <= entrycount002a.levelMax; i++) {

                             Event meEvent = waitForEvent(meRequest);

                             display("Checking entryCount for iteration : " + i);
                             try {
                                 int entryCount = lockRef.entryCount();
                                 if (entryCount != i) {
                                     exitCode = Consts.TEST_FAILED;
                                     complain("entry count method returned unexpected value : " + entryCount +
                                         "\n\t expected one : " + i);
                                 }
                             } catch (Exception e) {
                                 throw new Failure("Unexpected exception while invoking entryCount method: " + e);
                             }
                         }
                         meRequest.disable();
                     }
                     break;

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ end of section
                 default:
                     instruction = quit;
                     setInstruction("quit");
            }
        }
        display("TESTING ENDS");
    }

    //--------------------------------------------------------- test specific methodss

    //--------------------------------------------------------- immutable common methods

    void display(String msg) {
        log.display("debugger > " + msg);
    }

    void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
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
