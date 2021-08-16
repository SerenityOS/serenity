/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ClassType.invokeMethod;

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
public class invokemethod015 {

    //----------------------------------------------------- immutable common fields

    private int waitTime;
    private static int exitStatus = Consts.TEST_PASSED;

    private ArgumentHandler     argHandler;
    private Log                 log;
    private Debugee             debuggee;
    private VirtualMachine      vm;
    private ReferenceType       debuggeeClass;

    private EventRequestManager eventRManager;
    private EventSet            eventSet;
    private EventIterator       eventIterator;

    //------------------------------------------------------ mutable common fields

    private final static String prefix = "nsk.jdi.ClassType.invokeMethod";
    private final static String className = ".invokemethod015";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";
    static final int lineForBreak = 54;

    //------------------------------------------------------ immutable common methods

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    //------------------------------------------------------ test specific fields

    private static final String fieldName = "f1";
    private static final String methodName = "values";
//    private static final String methodSignature = "()[Lnsk/jdi/ClassType/invokeMethod/invokemethod015aEnum";
    private static final String[] expectedEnumFieldsNames = { "e1", "e2" };

    //------------------------------------------------------ mutable common methods

    public static int run (String argv[], PrintStream out) {

        int exitStatus = new invokemethod015().runThis(argv, out);
        return exitStatus;
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
                if (status != (Consts.TEST_PASSED + Consts.JCK_STATUS_BASE)) {
                    complain("Debuggee returned UNEXPECTED exit status: " + status);
                    exitStatus = Consts.TEST_FAILED;
                }
            } else {
                throw new TestBug("Last event is not the VMDeathEvent");
            }

        } catch (VMDisconnectedException e) {
            exitStatus = Consts.TEST_FAILED;
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
            exitStatus = Consts.TEST_FAILED;
            complain("Unexpected Exception: " + e.getMessage());
            e.printStackTrace(out);
            complain("The test has not finished normally. Forcing: vm.exit().");
            if (vm != null) {
                vm.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
            }
            debuggee.resume();
            getEventSet();
        }

        return exitStatus;
    }

    //--------------------------------------------------------- mutable common methods

    private void execTest() {
        BreakpointRequest bpRequest = setBreakpoint( null,
                                                     debuggeeClass,
                                                     "methodForCommunication",
                                                     lineForBreak,
                                                     "breakForCommunication");
        bpRequest.enable();

        display("Wait for initial brakepoint event...");
        BreakpointEvent bpEvent = (BreakpointEvent)waitForEvent(bpRequest);
        ThreadReference thread = bpEvent.thread();

        display("TESTING BEGINS");
        try {
            ClassType checkedClass = (ClassType)debuggeeClass.fieldByName(fieldName).type();
            String className = checkedClass.name();

            List<Method> l = checkedClass.methods();
            Method checkedMethod = null;
            if (l.isEmpty()) {
                complain("\t ReferenceType.methods() returned empty list for type: " + className);
                exitStatus = Consts.TEST_FAILED;
            } else {
                Iterator<Method> it = l.iterator();
                while (it.hasNext()) {
                    Method m = it.next();
                    if (methodName.equals(m.name()))
                        checkedMethod = m;
                }
                if (checkedMethod != null) {
                    ArrayReference values = (ArrayReference)checkedClass.invokeMethod(thread, checkedMethod , Collections.<Value>emptyList(), 0);
                    if (values.length() == 2) {
                        List constants = values.getValues();
                        Iterator it1 = constants.iterator();
                        while (it1.hasNext()) {
                            ObjectReference checkedField = (ObjectReference)it1.next();
                            if (checkedField.type().equals(checkedClass)) {
                                display("Invoked method " + methodName + " returned expected object " + checkedField.toString());
                            } else {
                                display("Invoked method " + methodName + " returned unexpected object " + checkedField.toString());
                                exitStatus = Consts.TEST_FAILED;
                            }
                        }
                    } else {
                        complain("Invoked method " + methodName + " returned list of unexpected size: " + values.length());
                        exitStatus = Consts.TEST_FAILED;
                    }
                } else {
                    complain("Cannot find in " + className + " the checked method " + methodName);
                    exitStatus = Consts.TEST_FAILED;
                }
            }

        } catch (Exception e) {
            complain("Unexpected exception while checking of " + className + ": " + e);
            e.printStackTrace(System.out);
            exitStatus = Consts.TEST_FAILED;
        }
        display("TESTING ENDS");
    }

    //--------------------------------------------------------- test specific methods

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
}
