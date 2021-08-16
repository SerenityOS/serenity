/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.MethodEntryEvent.method;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.lang.Thread;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger in the test

public class method001 {
    // exit status constants
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    // timeout interval for waiting events in a loop
    static final int TIMEOUT_DELTA = 1000; // milliseconds

    // synchronization commands
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    // class names
    static final String TEST_NAME     = "nsk.jdi.MethodEntryEvent.method.method001";
    static final String DEBUGGEE_NAME = TEST_NAME + "a";

    // JDI scaffold objects
    static private Debugee debuggee;
    static private IOPipe pipe;
    static private VirtualMachine vm;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    // mirrors for tested debuggee entities
    static private MethodEntryRequest checkedRequest;
    static private ThreadReference    checkedThread;
    static private Method             checkedMethod;
    static private Field              checkedField;
    static private ReferenceType      checkedClass;
    static private Field              depthField;

    // auxilary breakpoints
    static private BreakpointRequest  startingBreakpointRequest;
    static private BreakpointRequest  endingBreakpointRequest;
    static private Method             runMethod;

    // flags and counters
    static private long eventTimeout;
    static private int depthVal, eventsCounter;
    static private volatile boolean testFailed, eventReceived;

    // start test from command line
    public static void main (String args[]) {
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    // start test from JCK-compatible environment
    public static int run(final String args[], final PrintStream out) {

        testFailed = false;
        eventReceived = false;
        eventsCounter = 0;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // launch debuggee
        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "debuggee >");

        pipe = debuggee.createIOPipe();

        // resume debuggee
        log.display("Resuming debuggee");
        debuggee.resume();

        try {

            // wait for debugee started
            log.display("Waiting for command: " + COMMAND_READY);
            String command = pipe.readln();
            if (!command.equals(COMMAND_READY)) {
                throw new Failure("TEST BUG: unknown debuggee's command: " + command);
            }

            // get mirrors for checked class, thread, and method

            vm = debuggee.VM();

            log.display("Getting loaded class in debuggee");
            checkedClass = debuggee.classByName(DEBUGGEE_NAME);

            Iterator threadIterator = vm.allThreads().iterator();
            while (threadIterator.hasNext()) {
                ThreadReference curThread = (ThreadReference) threadIterator.next();
                if (curThread.name().equals("main")) {
                    checkedThread = curThread;
                }
            }
            if (checkedThread == null) {
                throw new Failure("TEST BUG: unable to find reference to main thread");
            }

            log.display("Getting reference to method <foo>");
            checkedMethod = debuggee.methodByName(checkedClass, "foo");
            if (checkedMethod == null) {
                throw new Failure("TEST BUG: returned null reference to method <foo>");
            }

            log.display("Getting reference to field <flag>");
            checkedField = checkedClass.fieldByName("flag");
            if (checkedField == null) {
                throw new Failure("TEST BUG: unable to find reference to field <flag>");
            }

            log.display("Getting reference to field <depth>");
            depthField = checkedClass.fieldByName("depth");
            if (depthField == null) {
                throw new Failure("TEST BUG: unable to find reference to field <depth>");
            }

            log.display("Getting value of field <depth>");
            try {
                 depthVal = ((IntegerValue)checkedClass.getValue(depthField)).value();
                 log.display("depth == " + depthVal);
            } catch (ClassCastException e) {
                 throw new Failure("TEST BUG: cannot get value of <depth> field");
            }

            // create event request (initially disabled)

            EventRequestManager eventRManager = vm.eventRequestManager();
            log.display("Creating MethodEntryRequest");
            if ((checkedRequest = eventRManager.createMethodEntryRequest()) == null) {
                throw new Failure("TEST BUG: unable to create MethodEntryRequest");
            }

            checkedRequest.addThreadFilter(checkedThread);
            checkedRequest.addClassFilter(checkedClass);
            checkedRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);

            switch (checkedRequest.suspendPolicy()) {
                case EventRequest.SUSPEND_ALL:
                     log.display("  suspend policy is SUSPEND_ALL");
                     break;
                case EventRequest.SUSPEND_EVENT_THREAD:
                     log.display("  suspend policy is SUSPEND_EVENT_THREAD");
                     break;
                case EventRequest.SUSPEND_NONE:
                     log.display("  suspend policy is SUSPEND_NONE");
                     break;
                default:
                     log.complain("TEST BUG: Unknown suspend policy!");
            }

            log.display("MethodEntryRequest is created");

            // create two auxilary breakpoints

            log.display("Getting reference to method <run>");
            runMethod = debuggee.methodByName(checkedClass, "run");
            if (runMethod == null) {
                throw new Failure("TEST BUG: returned null reference to method <run>");
            }

            log.display("Creating two auxilary breakpoints into method <run>");
            startingBreakpointRequest = debuggee.setBreakpoint(runMethod, method001a.STARTING_BREAKPOINT_LINE);
            endingBreakpointRequest = debuggee.setBreakpoint(runMethod, method001a.ENDING_BREAKPOINT_LINE);

            // define separate thread for handling events
            class EventHandler extends Thread {
                public void run() {
                    eventSet = null;
                    try {
                        while (!eventReceived) {
                            eventSet = vm.eventQueue().remove();

                            EventIterator eventIterator = eventSet.eventIterator();
                            while (eventIterator.hasNext()) {

                                Event event = eventIterator.nextEvent();

                                // enable or disable checked event request at BreakpointEvent
                                if (event instanceof BreakpointEvent) {
                                    Location eventLocation  = ((BreakpointEvent)event).location();
                                    int lineNumber = eventLocation.lineNumber();
                                    log.display("BreakpointEvent received for location " + lineNumber);
                                    if (lineNumber == method001a.STARTING_BREAKPOINT_LINE) {
                                        log.display("Enabling MethodEntryRequest at breakpoint before invoking method");
                                        checkedRequest.enable();
                                    } else if (lineNumber == method001a.ENDING_BREAKPOINT_LINE) {
                                        log.display("Disabling MethodEntryRequest at breakpoint after invoking method");
                                        checkedRequest.disable();
                                        eventReceived = true;
                                    } else {
                                        testFailed = true;
                                        throw new Failure("TEST BUG: Unknown location of breakpoint event: " + lineNumber);
                                    }
                                }

                                // handle checked MethodEntryEvent
                                if (event instanceof MethodEntryEvent) {
                                    MethodEntryEvent castedEvent = (MethodEntryEvent) event;
                                    EventRequest eventRequest = castedEvent.request();
                                    if (!(checkedRequest.equals(eventRequest))) {
                                        log.complain("FAILURE 2: eventRequest is not equal to checked request");
                                        testFailed = true;
                                    }
                                    ThreadReference eventThread = castedEvent.thread();
                                    if (!(checkedThread.equals(eventThread))) {
                                        log.complain("FAILURE 3: eventThread is not equal to checked thread");
                                        testFailed = true;
                                    }
                                    VirtualMachine eventMachine = castedEvent.virtualMachine();
                                    if (!(vm.equals(eventMachine))) {
                                        log.complain("FAILURE 4: eventVirtualMachine is not equal to checked vm");
                                        testFailed = true;
                                    }

                                    Method method1 = castedEvent.method();
                                    Method method2 = castedEvent.method();
                                    if (!method1.equals(method2)) {
                                        log.complain("FAILURE 7: return of MethodEntryEvent.method() is inconsistent");
                                        testFailed = true;
                                    }

                                    if (castedEvent.method().equals(checkedMethod)) {
                                        eventsCounter++;
                                        log.display("MethodEntryEvent is received for method " + checkedMethod.name() + ", counter = " + eventsCounter);
                                        try {
                                            int flagVal = ((IntegerValue)checkedClass.getValue(checkedField)).value();
                                            log.display("flag == " + flagVal);
                                            if ( flagVal > 0) {
                                                if (!eventThread.isSuspended()) {
                                                    log.complain("FAILURE 5: eventThread is not suspended");
                                                } else {
                                                    log.complain("FAILURE 6: MethodEntryEvent is generated after code execution");
                                                }
                                                testFailed = true;
                                            }
                                        } catch (ClassCastException e) {
                                            log.complain("TEST BUG: cannot get value of <flag> field");
                                            testFailed = true;
                                        }
                                    }
                                }
                            }
                            eventSet.resume();
                        }
                    } catch (InterruptedException e) {
                        log.complain("TEST INCOMPLETE: caught InterruptedException while waiting for event");
                        testFailed = true;
                    } catch (VMDisconnectedException e) {
                        log.complain("TEST INCOMPLETE: caught VMDisconnectedException while waiting for event");
                        testFailed = true;
                    }
                    log.display("eventHandler completed");
                }
            }

            // start event handling thread
            EventHandler eventHandler = new EventHandler();
            log.display("Starting eventHandler");
            eventHandler.start();

            // force debuggee to invoke method
            log.display("Sending command: " + COMMAND_GO);
            pipe.println(COMMAND_GO);

            log.display("");

            // waiting for debuggee comfirms method invoked
            log.display("Waiting for command: " + COMMAND_DONE);
            command = pipe.readln();
            if (!command.equals(COMMAND_DONE)) {
                throw new Failure("TEST BUG: unknown debuggee's command: " + command);
            }

            log.display("");

            // wait for all expected events received or timeout exceeds
            log.display("Waiting for all expected events received");
            try {
                eventHandler.join(eventTimeout);
                if (eventHandler.isAlive()) {
                    log.complain("FAILURE 20: Timeout for waiting event was exceeded");
                    eventHandler.interrupt();
                    testFailed = true;
                }
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: InterruptedException caught while waiting for eventHandler's death");
                testFailed = true;
            }

            // check whether all expected events received or not
            if (eventsCounter == 0) {
                log.complain("FAILURE 1: No any MethodEntryEvent received");
                testFailed = true;
            } else if (eventsCounter < depthVal) {
                log.complain("FAILURE 1: Some " + (depthVal - eventsCounter) + " MethodEntryEvent NOT received (too few)");
                testFailed = true;
            } else if (eventsCounter > depthVal) {
                log.complain("FAILURE 1: Extra " + (eventsCounter - depthVal) + " MethodEntryEvent received (too many)");
                testFailed = true;
            }

        } catch (Failure e) {
            log.complain("TEST FAILURE: " + e.getMessage());
            testFailed = true;
        } catch (Exception e) {
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(out);
            testFailed = true;
        } finally {

            log.display("");

            // force debuggee to exit
            log.display("Sending command: " + COMMAND_QUIT);
            pipe.println(COMMAND_QUIT);

            // wait for debuggee exits and analyze its exit code
            log.display("Waiting for debuggee terminating");
            int debuggeeStatus = debuggee.endDebugee();
            if (debuggeeStatus == PASSED + JCK_STATUS_BASE) {
                log.display("Debuggee PASSED with exit code: " + debuggeeStatus);
            } else {
                log.complain("Debuggee FAILED with exit code: " + debuggeeStatus);
                testFailed = true;
            }
        }

        // check test results
        if (testFailed) {
            log.complain("TEST FAILED");
            return FAILED;
        }

        log.display("TEST PASSED");
        return PASSED;

    }
}
