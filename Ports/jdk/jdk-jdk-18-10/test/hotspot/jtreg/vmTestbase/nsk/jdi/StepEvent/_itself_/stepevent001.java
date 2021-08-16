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

package nsk.jdi.StepEvent._itself_;

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

public class stepevent001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    static final String TEST_NAME = "nsk.jdi.StepEvent._itself_.stepevent001";
    static final String DEBUGGEE_NAME = TEST_NAME + "a";

    static private final int EXPECTED_EVENTS_COUNT = 10;

    static private Debugee debuggee;
    static private IOPipe pipe;
    static private VirtualMachine vm;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private StepRequest       checkedRequest;
    static private BreakpointRequest breakpointRequest;
    static private Field             checkedField;
    static private Method            checkedMethod;
    static private Location          checkedLocation;
    static private ThreadReference   checkedThread;
    static private ReferenceType     checkedClass;

    static private boolean testFailed;
    static private long eventTimeout;
    static private int eventsReceived, oldValue;

    static private volatile boolean methodCompleted;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

         testFailed = false;
         methodCompleted = false;
         eventsReceived = 0;
         oldValue = 0;

         argHandler = new ArgumentHandler(args);
         log = new Log(out, argHandler);
         eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

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
            checkedMethod = (Method) allMethods.get(0);

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
                 if (curLocation.lineNumber() == stepevent001a.stepLineBegin) {
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

            log.display("Creating auxiliary BreakpointRequest");
            if ((breakpointRequest = eventRManager.createBreakpointRequest(checkedLocation)) == null) {
                throw new Failure("TEST BUG: unable to create BreakpointRequest");
            }

            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);

            switch (breakpointRequest.suspendPolicy()) {
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

            breakpointRequest.enable();
            log.display("Auxiliary BreakpointRequest is created");

            log.display("Creating StepRequest");
            if ((checkedRequest = eventRManager.createStepRequest(checkedThread,
                  StepRequest.STEP_LINE, StepRequest.STEP_OVER)) == null) {
               throw new Failure("TEST BUG: unable to create StepRequest");
            }
            checkedRequest.addClassFilter(checkedClass);
            log.display("StepRequest is created but not yet enabled");

            // sdefine separate thread for handleing received events
            class EventHandler extends Thread {
                  public void run() {
                       // handle events antil method invoking completes and all expected events received
                       while (!methodCompleted || eventsReceived < EXPECTED_EVENTS_COUNT) {
                           eventSet = null;
                           try {
                               eventSet = vm.eventQueue().remove(TIMEOUT_DELTA);
                           } catch (InterruptedException e) {
                               log.complain("Unexpected InterruptedException while receiving event: " + e);
                               break;
                           }

                           if (eventSet == null) {
                               log.display("No event");
                               continue;
                            }

                           EventIterator eventIterator = eventSet.eventIterator();
                           while (eventIterator.hasNext()) {

                               Event event = eventIterator.nextEvent();
                               log.display("\nEvent received:\n  " + event);

                               // handle BreakPointEvent
                               if (event instanceof BreakpointEvent) {
                                   Location eventLocation  = ((BreakpointEvent)event).location();
                                   log.display("BreakpointEvent received for location " + eventLocation.lineNumber());
                                   if (eventLocation.lineNumber() == stepevent001a.stepLineBegin) {
                                       checkedRequest.enable();
                                       log.display("StepRequest is enabled upon receiving breakpoint event on checked line");
                                   }
                               }

                               // handle StepEvent
                               if (event instanceof StepEvent) {
                                   StepEvent castedEvent = (StepEvent) event;
                                   log.display("Received event is StepEvent:\n  " + event);
                                   EventRequest eventRequest = castedEvent.request();
                                   if (!(checkedRequest.equals(eventRequest))) {
                                       log.complain("FAILURE 1: eventRequest is not equal to checked request");
                                       testFailed = true;
                                   } else {
                                       eventsReceived++;
                                       log.display("Expected StepEvent received: " + eventsReceived + " times");
                                   }
                                   ThreadReference eventThread = castedEvent.thread();
                                   if (!(checkedThread.equals(eventThread))) {
                                       log.complain("FAILURE 2: eventThread is not equal to checked thread");
                                       testFailed = true;
                                   }
                                   VirtualMachine eventMachine = castedEvent.virtualMachine();
                                   if (!(vm.equals(eventMachine))) {
                                       log.complain("FAILURE 3: eventVirtualMachine is not equal to checked vm");
                                       testFailed = true;
                                   }
                                   Location eventLocation  = castedEvent.location();
                                   int lineNumber = eventLocation.lineNumber();
                                   log.display("StepEvent received for location: " + lineNumber);

                                   try {
                                        int counterValue = ((IntegerValue)checkedClass.getValue(checkedField)).value();
                                        log.display("Counter == " + counterValue);
                                        if ( counterValue > oldValue) {
                                             if (!eventThread.isSuspended()) {
                                                  log.complain("FAILURE 4: eventThread is not suspended");
                                             } else {
                                                  log.complain("FAILURE 5: StepEvent is generated after code execution");
                                             }
                                             testFailed = true;
                                        }
                                        oldValue++;  // modify for the next event

                                   } catch (ClassCastException e) {
                                        log.complain("TEST BUG: cannot get value of <counter> field");
                                        testFailed = true;
                                   }

                                   // disable StepEvent when last checked line reached
                                   if (lineNumber == stepevent001a.stepLineEnd) {
                                       log.display("Disabling event request at the last checked line: " + lineNumber);
                                       checkedRequest.disable();
                                   }

                               }

                               // ignore all oter events
                           }

                           log.display("Resuming event set");
                           eventSet.resume();
                       }

                       log.display("eventHandler completed");
                  }
            }

            // start EventHandler thread
            EventHandler eventHandler = new EventHandler();
            log.display("Starting eventHandler");
            eventHandler.start();

            // force debuggee to invoke method and generate StepEvents
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

            // Check if all StepEvents are received
            if (eventsReceived < EXPECTED_EVENTS_COUNT) {
                log.display("Too few StepEvents are received: " + eventsReceived);
                testFailed = true;
            } else if (eventsReceived > EXPECTED_EVENTS_COUNT) {
                log.display("Too many StepEvents are received: " + eventsReceived);
                testFailed = true;
            } else {
                log.display("All expected StepEvents are received");
            }

        } catch (Failure e) {
            log.complain("TEST FAILURE: " + e.getMessage());
            testFailed = true;
        } catch (Exception e) {
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(out);
            testFailed = true;
        } finally {

            // disable event requests to prevent appearance of further events
            if (checkedRequest != null && checkedRequest.isEnabled()) {
                log.display("Disabling StepEvent request");
                checkedRequest.disable();
            }
            if (breakpointRequest != null && breakpointRequest.isEnabled()) {
                log.display("Disabling auxilary breakpoint request");
                breakpointRequest.disable();
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
