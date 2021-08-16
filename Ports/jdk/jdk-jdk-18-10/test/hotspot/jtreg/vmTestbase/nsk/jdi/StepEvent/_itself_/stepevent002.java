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

public class stepevent002 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    static final String TEST_NAME = "nsk.jdi.StepEvent._itself_.stepevent002";
    static final String DEBUGGEE_NAME = TEST_NAME + "a";

    static final int EXPECTED_EVENTS_COUNT = 10;

    static private Debugee debuggee;
    static private IOPipe pipe;
    static private VirtualMachine vm;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private StepRequest       checkedRequest;
    static private Location          checkedLocation;
    static private ThreadReference   checkedThread;
    static private ReferenceType     checkedClass;

    static private long eventTimeout;
    static private EventRequestManager eventRManager;
    static private boolean testFailed;
    static private int eventsCount, frameCount, oldFrameCount;

    static private volatile boolean threadFinished;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

        testFailed = false;
        threadFinished = false;
        eventsCount = 0;
        frameCount = -1;
        oldFrameCount = -1;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "debuggee >");
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();

        // prepare debugee, create request, and wait for StepEvent
        try {

            // resume debugee and wait for it becomes ready
            log.display("Resuming debuggee");
            debuggee.resume();

            log.display("Waiting for command: " + COMMAND_READY);
            String command = pipe.readln();
            if (command == null || !command.equals(COMMAND_READY)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // get mirror of debugee class
            if ((checkedClass = debuggee.classByName(DEBUGGEE_NAME)) == null) {
                throw new Failure("TEST BUG: cannot find debuggee's class " + DEBUGGEE_NAME);
            }

            // create request for step event
            log.display("Getting reference to thread <threadForEvent>");
            checkedThread = (ThreadReference) checkedClass.getValue(checkedClass.fieldByName("threadForEvent") ) ;
            if (checkedThread == null) {
                throw new Failure("TEST BUG: unable to find reference to <threadForEvent>");
            }

            log.display("Creating StepRequest with size = STEP_LINE, depth = STEP_INTO");
            eventRManager = vm.eventRequestManager();
            if ((checkedRequest = eventRManager.createStepRequest( checkedThread,
                    StepRequest.STEP_LINE, StepRequest.STEP_INTO)) == null) {
                throw new Failure("TEST BUG: unable to create StepRequest");
            }
//              checkedRequest.addClassFilter(checkedThread.referenceType());
            checkedRequest.addClassFilter(checkedClass);

            // separate thread for handling events
            class EventHandler extends Thread {
                public void run() {
                    log.display("Event handler started");

                    // handle events until thread finishes and all expected events received
                    while (!(threadFinished && eventsCount >= EXPECTED_EVENTS_COUNT)) {

                        // get next available event set
                        eventSet = null;
                        try {
                            eventSet = vm.eventQueue().remove(TIMEOUT_DELTA);
                        } catch (Exception e) {
                            throw new Failure("TEST INCOMPLETE: Unexpected exception while getting event: " + e);
                        }
                        if (eventSet == null)
                            continue;

                        // handle each event from the event set
                        EventIterator eventIterator = eventSet.eventIterator();
                        while (eventIterator.hasNext()) {

                            Event event = eventIterator.nextEvent();
                            log.display("\nEvent received:\n  " + event);

                            // handle StepEvent
                            if (event instanceof StepEvent) {

                                StepEvent castedEvent = (StepEvent)event;
                                log.display("Received event is StepEvent:\n  " + castedEvent);

                                // check that received event is for checked request
                                EventRequest eventRequest = castedEvent.request();
                                if (checkedRequest.equals(eventRequest)) {
                                    eventsCount++;
                                    log.display("Expected StepEvent for checked request received: " + eventsCount);
                                } else {
                                    log.complain("FAILURE 1: eventRequest is not equal to checked request");
                                    testFailed = true;
                                }

                                // check that received event is for checked VM
                                VirtualMachine eventMachine = castedEvent.virtualMachine();
                                if (!(vm.equals(eventMachine))) {
                                    log.complain("FAILURE 2: eventVirtualMachine is not equal to checked vm");
                                    testFailed = true;
                                }

                                // check that received event is for checked thread
                                ThreadReference eventThread = castedEvent.thread();
                                if (!(checkedThread.equals(eventThread))) {
                                    log.complain("FAILURE 3: eventThread is not equal to checked thread");
                                    testFailed = true;
                                }

                                Location eventLocation = castedEvent.location();
                                log.display("StepEvent received for location: " + eventLocation.lineNumber());

                                try {
                                    frameCount = checkedThread.frameCount();
                                    log.display("frame count " + frameCount);
                                    if (oldFrameCount > frameCount) {
                                       log.complain("FAILURE 4: step event is not generated for STEP_INTO");
                                       testFailed = true;
                                    }
                                    oldFrameCount = frameCount;
                                } catch (Exception e) {
                                    log.complain("ERROR: Unexpected exception is thrown while trying frame count " + e.getMessage());
                                    testFailed = true;
                                }
                            }

                            // ignore all other events
                         }

                         // resume each received event set
                         log.display("Resuming event set");
                         eventSet.resume();
                    }

                    log.display("Event handler finished successfully");
                }
            }

            // start event handler in a separate thread
            log.display("Starting event handler thread");
            EventHandler eventHandler = new EventHandler();
            eventHandler.start();

            // enable event request
            log.display("Enabling event request");
            checkedRequest.enable();
            log.display("StepRequest for threadForEvent is created and enabled");

            // force checked class to be unloaded from debuggee
            log.display("Force debuggee to invoke checked method and generate step events");
            pipe.println(COMMAND_GO);

            log.display("");

            // wait for thread finished in debugee
            log.display("Waiting for command: " + COMMAND_DONE);
            command = pipe.readln();
            if (command == null || !command.equals(COMMAND_DONE)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }
            threadFinished = true;

            log.display("");

            // waiting for all expected events received or timeout exceeds
            log.display("Waiting for all expected events received");
            try {
                eventHandler.join(eventTimeout);
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: InterruptedException caught while waiting for eventHandler's death");
                testFailed = true;
            }

            if (eventHandler.isAlive()) {
                log.display("Interrupting event handler thread");
                eventHandler.interrupt();
            }

            // check number of received events
            if (eventsCount == 0) {
                log.complain("FAILURE 4: No any StepEvent received");
                testFailed = true;
            }
            if (eventsCount < EXPECTED_EVENTS_COUNT) {
                log.complain("FAILURE 4: Too few StepEvents received: " + eventsCount);
                testFailed = true;
            }
            if (eventsCount > EXPECTED_EVENTS_COUNT) {
                log.complain("FAILURE 4: Too many StepEvents received: " + eventsCount);
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

            // disable event request to prevent appearance of further events
            if (checkedRequest != null) {
                log.display("Disabling event request");
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
