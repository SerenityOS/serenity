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

package nsk.jdi.BreakpointEvent._itself_;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.List;
import java.util.Iterator;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger in the test

public class breakpoint001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    static final String TEST_NAME = "nsk.jdi.BreakpointEvent._itself_.breakpoint001";
    static final String DEBUGGEE_NAME = TEST_NAME + "a";

    static final int EXPECTED_EVENTS_COUNT = 10;

    static private Debugee debuggee;
    static private IOPipe pipe;
    static private VirtualMachine vm;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private BreakpointRequest checkedRequest;
    static private Field             checkedField;
    static private Method            checkedMethod;
    static private Location          checkedLocation;
    static private ThreadReference   checkedThread;
    static private ReferenceType     checkedClass;

    static private boolean testFailed;
    static private int eventsReceived, oldValue;

    static private volatile boolean methodCompleted;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

        testFailed = false;
        eventsReceived = 0;
        oldValue = 0;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);

        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "debuggee >");

        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();

        debuggee.resume();

        try {

            // waiting for debuggee started
            log.display("Waiting for command: " + COMMAND_READY);
            String command = pipe.readln();
            if (!command.equals(COMMAND_READY)) {
                throw new Failure("TEST BUG: unknown debuggee's command: " + command);
            }

            // find checked location and create StepEventrequest
            EventRequestManager eventRManager = vm.eventRequestManager();

            log.display("Getting loaded class in debuggee");
            List classes = vm.classesByName(DEBUGGEE_NAME);
            checkedClass = (ReferenceType) classes.get(0);

            log.display("Getting reference to main thread");
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
            List allMethods  = checkedClass.methodsByName("foo");
            try {
                checkedMethod = (Method) allMethods.get(0);
            } catch (IndexOutOfBoundsException e) {
                throw new Failure("No method foo() found for the checked class: " + e);
            }

            if (checkedMethod == null) {
                throw new Failure("Null reference returned for method foo() of the checked class");
            }

            log.display("Getting reference to field <counter>");
            checkedField = checkedClass.fieldByName("counter");
            if (checkedField == null) {
                throw new Failure("TEST BUG: unable to find reference to field <counter>");
            }

            log.display("Getting all locations");
            List allLineLocations;
            try {
                allLineLocations = checkedMethod.allLineLocations();
            } catch ( AbsentInformationException e) {
                throw new Failure("TEST BUG: caught AbsentInformationException " + e);
            }

            log.display("Getting checked location");
            Iterator locIterator = allLineLocations.iterator();
            while (locIterator.hasNext()) {
                 Location curLocation = (Location)locIterator.next();
                 int curNumber = curLocation.lineNumber();
                 if (curLocation.lineNumber() == breakpoint001a.breakpointLineNumber) {
                      if (checkedLocation != null) {
                          throw new Failure("TEST BUG: multiple locations on breakpoint line");
                      } else {
                          checkedLocation = curLocation;
                      }
                 }
            }
            if (checkedLocation == null) {
                throw new Failure("TEST BUG: incorrect line number of the location in <foo> method");
            }

            log.display("Creating BreakpointRequest for the location");
            if ((checkedRequest = eventRManager.createBreakpointRequest(checkedLocation)) == null) {
                throw new Failure("TEST BUG: unable to create BreakpointRequest");
            }

            checkedRequest.addThreadFilter(checkedThread);
            checkedRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);

            switch (checkedRequest.suspendPolicy()) {
                case EventRequest.SUSPEND_ALL:
                     log.display("suspend policy is SUSPEND_ALL");
                     break;
                case EventRequest.SUSPEND_EVENT_THREAD:
                     log.display("suspend policy is SUSPEND_EVENT_THREAD");
                     break;
                case EventRequest.SUSPEND_NONE:
                     log.display("suspend policy is SUSPEND_NONE");
                     break;
                default:
                     log.complain("TEST BUG: Unknown suspend policy!");
            }

            log.display("BreakpointRequest is created for location " + checkedLocation.lineNumber());

            // define separate thread for handling received events
            class EventHandler extends Thread {
                public void run() {
                    // handle events until method invoking completes and all expected events received
                    while (!methodCompleted || eventsReceived < EXPECTED_EVENTS_COUNT) {
                        eventSet = null;
                        try {
                            eventSet = vm.eventQueue().remove(TIMEOUT_DELTA);
                        } catch (InterruptedException e) {
                            log.complain("Unexpected InterruptedException while receiving event: " + e);
                            break;
                        }

                        if (eventSet == null) {
                            continue;
                        }

                        // handle each event of the event set
                        EventIterator eventIterator = eventSet.eventIterator();
                        while (eventIterator.hasNext()) {

                            Event event = eventIterator.nextEvent();
                            log.display("\nEvent received:\n  " + event);

                            // handle BreakPointEvent
                            if (event instanceof BreakpointEvent) {
                                BreakpointEvent castedEvent = (BreakpointEvent) event;
                                log.display("Received event is BreakpointEvent:\n  " + event);

                                EventRequest eventRequest = castedEvent.request();
                                if (!(checkedRequest.equals(eventRequest))) {
                                    log.complain("FAILURE 2: eventRequest is not equal to checked request");
                                    testFailed = true;
                                } else {
                                    eventsReceived++;
                                    log.display("Expected BreakpointEvent received: " + eventsReceived + " times");
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

                                Location eventLocation  = castedEvent.location();
                                log.display("BreakpointEvent received for location: " + eventLocation.lineNumber());

                                if (!(checkedLocation.equals(eventLocation))) {
                                    log.complain("FAILURE 5: eventLocation is not equal to checked location");
                                    testFailed = true;
                                }

                                try {
                                     int counterValue = ((IntegerValue)checkedClass.getValue(checkedField)).value();
                                     log.display("Counter == " + counterValue);
                                     if ( counterValue > oldValue) {
                                          if (!eventThread.isSuspended()) {
                                               log.complain("FAILURE 6: eventThread is not suspended");
                                          } else {
                                               log.complain("FAILURE 7: BreakpointEvent is generated after code execution");

                                          }
                                          testFailed = true;
                                     }
                                     oldValue++;  // modify for next event

                                } catch (ClassCastException e) {
                                     log.complain("TEST BUG: cannot get value of <counter> field");
                                     testFailed = true;
                                }

                            }

                            // ignore each other event
                        }

                        log.display("Resuming event set");
                        eventSet.resume();
                    }
                    log.display("eventHandler completed");
                }
            }

            EventHandler eventHandler = new EventHandler();
            log.display("Starting eventHandler");
            eventHandler.start();

            log.display("Enabling breakpoint request");
            checkedRequest.enable();

            // force debuggee to invoke method and generate BreakpouintEvents
            log.display("Sending command: " + COMMAND_GO);
            pipe.println(COMMAND_GO);

            log.display("");

            // waiting for debuggee finished invoking method
            log.display("Waiting for command: " + COMMAND_DONE);
            command = pipe.readln();
            if (!command.equals(COMMAND_DONE)) {
                throw new Failure("TEST BUG: unknown debuggee's command: " + command);
            }
            methodCompleted = true;

            log.display("");

            // wait for all expected events received or timeout exceeds
            log.display("Waiting for all expected events received");
            try {
                eventHandler.join(argHandler.getWaitTime()*60000);
                if (eventHandler.isAlive()) {
                     log.complain("FAILURE 20: Timeout for waiting event was exceeded");
                     eventHandler.interrupt();
                     testFailed = true;
                }
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: InterruptedException caught while waiting for eventHandler's death");
                testFailed = true;
            }

            // Check if all expected BreakpointEvents are received
            if (eventsReceived < EXPECTED_EVENTS_COUNT) {
                log.display("Too few BreakpointEvents are received: " + eventsReceived);
                testFailed = true;
            } else if (eventsReceived > EXPECTED_EVENTS_COUNT) {
                log.display("Too many BreakpointEvents are received: " + eventsReceived);
                testFailed = true;
            } else {
                log.display("All expected BreakpointEvents are received");
            }

        } catch (Failure e) {
            log.complain("TEST FAILURE: " + e.getMessage());
            testFailed = true;
        } catch (Exception e) {
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(out);
            testFailed = true;
        } finally {

            // disable event request to prevent appearance of further events
            if (checkedRequest != null && checkedRequest.isEnabled()) {
                log.display("Disabling BreakpointEvent request");
                checkedRequest.disable();
            }

            // force debugee to exit
            log.display("Sending command: " + COMMAND_QUIT);
            pipe.println(COMMAND_QUIT);

            // wait for debuggee exits and analize its exit code
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
