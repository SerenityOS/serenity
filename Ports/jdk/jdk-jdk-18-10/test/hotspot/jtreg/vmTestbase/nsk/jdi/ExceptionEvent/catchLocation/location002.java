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

package nsk.jdi.ExceptionEvent.catchLocation;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger in the test

public class location002 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    // time interval to wait for events
    static final int TIMEOUT_DELTA = 1000; // milliseconds

    // synchronization commands
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";
    static final String COMMAND_ERROR = "error";

    // checked classes names
    static final String DEBUGGEE_NAME  = "nsk.jdi.ExceptionEvent.catchLocation.location002a";
    static final String JAVA_EXCEPTION = "java.lang.NumberFormatException";

    // scaffold objects
    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    // timeout for waiting events
    static private long eventTimeout;

    // mirrors for checked classes and threads in debuggee
    static private ExceptionRequest  checkedRequest;
    static private ReferenceType     checkedClass;
    static private ThreadReference   checkedThread;

    // results of test execution
    static private boolean eventsReceived;
    static private boolean testFailed;

    // execute test from command line
    public static void main (String args[]) {
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    // execute test from JCK-compatible harness
    public static int run(final String args[], final PrintStream out) {

        testFailed = false;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // launch debuggee
        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "location001a >");

        // create synchronization channel with debuggee
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        EventRequestManager erManager = vm.eventRequestManager();

        // resume debuggee
        log.display("Resuming debuggee");
        debuggee.resume();

        // catch exceptions while testing and finally quit debuggee
        try {

            // wait for debuggee started
            log.display("Waiting for command: " + COMMAND_READY);
            String command = pipe.readln();
            if (!command.equals(COMMAND_READY)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // get mirrors of checked classes in debuggee
            log.display("Getting loaded classes in debuggee");
            if ((checkedClass = debuggee.classByName(DEBUGGEE_NAME)) == null) {
                throw new Failure("TEST BUG: cannot find " + DEBUGGEE_NAME);
            }

            // get mirror of main thread in debuggee
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

            // create ExceptionRequest for all throwable classes (initially disabled)
            log.display("Creating ExceptionRequest");
            boolean notifyCaught = true;
            boolean notifyUncaught = true;
            if ((checkedRequest = erManager.createExceptionRequest(null, notifyCaught, notifyUncaught)) == null) {
                throw new Failure("TEST BUG: unable to create ExceptionRequest");
            }

            // define separate thread for handling received events
            class EventHandler extends Thread {
                public void run() {
                    // handle events until expected event received
                    eventHandlingLoop:
                    while (!eventsReceived) {
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

                        // handle each event from event set
                        EventIterator eventIterator = eventSet.eventIterator();
                        while (eventIterator.hasNext()) {

                            Event event = eventIterator.nextEvent();
//                            log.display("Event received:\n  " + event);

                            // break event handling loop if VMDisconnectEvent received
                            if (event instanceof VMDisconnectEvent) {
                                log.display("VMDisconnectEvent received");
                                break eventHandlingLoop;
                            }

                            // handle ExceptionEvent
                            if (event instanceof ExceptionEvent) {
                                log.display("ExceptionEvent received");

                                ExceptionEvent castedEvent  = (ExceptionEvent)event;
                                ReferenceType  eventRefType = castedEvent.exception().referenceType();

                                String eventTypeName = eventRefType.name();
                                if (eventTypeName.equals(JAVA_EXCEPTION)) {
                                    log.display("Expected ExceptionEvent for " + JAVA_EXCEPTION + " is received");
                                    eventsReceived = true;

                                    Location catchLocation  = castedEvent.catchLocation();
                                    if (catchLocation != null) {
                                        log.complain("FAILURE 1: catchLocation for uncaught " + JAVA_EXCEPTION +
                                                     " is not null :");
                                        log.complain("declaring type: " + catchLocation.declaringType().name());
                                        log.complain("line number   : " + catchLocation.lineNumber());
                                    }

                                }
                            }

                            // ignore each other events

                        } // end of event handling loop

//                        log.display("Resuming event set");
                        eventSet.resume();

                    } // end of event set handling loop

                    log.display("eventHandler completed");

                } // end of run()

            } // end of EventHandler

            // start EventHandler thread
            EventHandler eventHandler = new EventHandler();
            log.display("Starting eventHandler");
            eventHandler.start();

            // enable event request
            log.display("Enabling ExceptionRequest");
            checkedRequest.enable();

            // force debuggee to throw exceptions and generate events
            log.display("Sending command: " + COMMAND_GO);
            pipe.println(COMMAND_GO);

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

            // complain if not all expected events received
            if (!eventsReceived) {
                log.complain("FAILURE 2: " + JAVA_EXCEPTION + " was not received");
                testFailed= true;
            }

            // end testing

        } catch (Failure e) {
            log.complain("TEST FAILURE: " + e.getMessage());
            testFailed = true;
        } catch (Exception e) {
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(out);
            testFailed = true;
        } finally {

            log.display("");

            // wait for debuggee exits and analize its exit code
            log.display("Waiting for debuggee terminating");
            int debuggeeStatus = debuggee.endDebugee();
            log.display("Debuggee terminated with exit code: " + debuggeeStatus);
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
