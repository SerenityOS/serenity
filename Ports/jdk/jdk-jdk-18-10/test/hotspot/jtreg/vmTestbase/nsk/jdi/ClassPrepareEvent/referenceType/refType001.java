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

package nsk.jdi.ClassPrepareEvent.referenceType;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.Iterator;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger in the test

public class refType001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_RUN   = "run";
    static final String COMMAND_DONE  = "done";
    static final String COMMAND_ERROR = "error";

    static final String PACKAGE_NAME = "nsk.jdi.ClassPrepareEvent.referenceType";
    static final String DEBUGEE_NAME = PACKAGE_NAME + ".refType001a";

    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;

    static private ClassPrepareRequest checkedRequest;
    static private ThreadReference debuggeeThread;
    static private volatile String checkedTypes[][] = {
                {"main",            "refType001a",              "0"},
                {"main",            "AnotherThread",            "0"},
                {"AnotherThread",   "ClassForAnotherThread",    "0"},
                {"AnotherThread",   "Inter",                    "0"}
             };

    static private volatile boolean testFailed, eventsReceived, threadsStarted;
    static private int eventTimeout;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {
        String command;

        testFailed = false;
        eventsReceived = false;
        threadsStarted = false;


        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // launch debugee
        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGEE_NAME);
        debuggee.redirectStderr(log, "refType001a >");
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();

        // create request and wait for expected events
        try {

            // create request for ClassPrepareEvent
            log.display("Creating request for ClassPrepareEvent");
            EventRequestManager erManager = debuggee.VM().eventRequestManager();
            if ((checkedRequest = erManager.createClassPrepareRequest()) == null) {
                throw new Failure("TEST FAILED: unable to create ClassPrepareRequest");
            }
            log.display("ClassPrepareRequest is created");

            // define separate thread to handle received events
            class EventHandler extends Thread {

                public void run() {

                    // handle events until all threads started and all expected events received
                    while (!(threadsStarted && eventsReceived)) {
                        EventSet eventSet = null;
                        try {
                            eventSet = vm.eventQueue().remove(TIMEOUT_DELTA);
                        } catch (InterruptedException e) {
                            throw new Failure("Unexpected InterruptedException while receiving events: " + e);
                        }

                        if (eventSet == null) {
                            continue;
                        }

                        // handle each event
                        EventIterator eventIterator = eventSet.eventIterator();
                        while (eventIterator.hasNext()) {
                            Event event = eventIterator.nextEvent();
//                            log.display("\nEvent received:\n  " + event);

                            // handle ClassPrepareEvent
                            if (event instanceof ClassPrepareEvent) {
                                ClassPrepareEvent castedEvent = (ClassPrepareEvent)event;
                                log.display("\nClassPrepareEvent received:\n  " + event);

                                EventRequest eventRequest = castedEvent.request();
                                if (!(checkedRequest.equals(eventRequest))) {
                                    log.complain("FAILURE 1: eventRequest is not equal to checked request");
                                    testFailed = true;
                                }
                                VirtualMachine eventMachine = castedEvent.virtualMachine();
                                if (!(vm.equals(eventMachine))) {
                                    log.complain("FAILURE 2: eventVirtualMachine is not equal to checked vm");
                                    testFailed = true;
                                }

                                debuggeeThread = castedEvent.thread();
                                String threadName = debuggeeThread.name();

                                // test ClassPrepareEvent.referenceType() method
                                ReferenceType refType = castedEvent.referenceType();
                                if (refType == null) {
                                    log.complain("FAILURE 3: ClassPrepareEvent.referenceType() returns null");
                                    testFailed = true;
                                } else {

                                    String refName = refType.name();
                                    if ((refName == null) || (refName.equals(""))) {
                                        log.complain("FAILURE 4: Reference has invalid empty name");
                                        testFailed = true;
                                    } else {
                                         if ( refName.startsWith(PACKAGE_NAME)) {
                                             log.display("Reference " + refName + " prepared in thread " + threadName);

                                              // Check that all expected debuggee's thread create ClassPrepareEvent only once
                                              for (int i = 0; i < checkedTypes.length; i++) {
                                                   if (threadName.equals(checkedTypes[i][0]) && refName.endsWith(checkedTypes[i][1])) {
                                                        if (checkedTypes[i][2] == "0") {
                                                              checkedTypes[i][2] = "1";
                                                        } else {
                                                              log.complain("FAILURE 5: ClassPrepareEvent for " + threadName + " is received more that once");
                                                              testFailed = true;
                                                        }
                                                   }
                                              }

                                              // Check that all static fields in reference type are initialized
                                              Iterator fieldsList;
                                              try {
                                                  fieldsList = refType.fields().iterator();
                                              } catch (ClassNotPreparedException e) {
                                                  log.complain( "FAILURE 6: " + refName + " is not prepared");
                                                  testFailed = true;
                                                  throw new Failure("ClassNotPrepared exception caught: " + e);
                                              }
                                              while (fieldsList.hasNext()) {
                                                   Field refField = (Field)fieldsList.next();
                                                   if (refField.isStatic()) {
                                                        if (refField.equals(null)) {
                                                            log.complain( "FAILURE 7: " + refField.name() + " of " + refName + " is not initialized");
                                                            testFailed = true;
                                                        }
                                                   }
                                              }

                                              // Check that all expected ClassPrepareEvent are received
                                              eventsReceived = true;
                                              for (int i = 0; i < checkedTypes.length; i++) {
                                                   if (checkedTypes[i][2] == "0")
                                                       eventsReceived = false;
                                              }
                                         }
                                    }

                                    if (!refType.isPrepared()) {
                                        log.complain("FAILURE 8: Reference " + refName + " is not prepared" );
                                        testFailed = true;
                                    }
                                }
                            }

                            // ignore each oter event

                        } // event handled

//                        log.display("Resuming event set");
                        eventSet.resume();

                    } // event set handled

                    log.display("eventHandler completed");

                } // for run()

            } // for EventHandler

            // start EventHandler thread
            EventHandler eventHandler = new EventHandler();
            log.display("Starting eventHandler");
            eventHandler.start();

            // enable event request and resume debugee
            log.display("Enabling ClassPrepareEvent request");
            checkedRequest.enable();

            log.display("Resuming debuggee");
            debuggee.resume();

            // wait for debugee started
            log.display("Waiting for command: " + COMMAND_READY);
            command = pipe.readln();
            if (!command.equals(COMMAND_READY)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // force debugee to start another thread
            log.display("Sending a command: " + COMMAND_RUN);
            pipe.println(COMMAND_RUN);

            // wait for debugee started requested thread
            log.display("Waiting for command: " + COMMAND_DONE);
            command = pipe.readln();
            if (!command.equals(COMMAND_DONE)) {
                log.complain("TEST BUG: unexpected debuggee's command: " + command);
                testFailed = true;
            }

            // notify EventHandler that all threads started
            threadsStarted = true;

            // wait for all expected events received or timeout exceeds
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

            // check that all expected debuggee's threads created ClassPrepareEvent
            for (int i = 0; i < checkedTypes.length; i++) {
                if (checkedTypes[i][2].equals("0")) {
                    log.complain("FAILURE 9: ClassPrepareEvent for " + checkedTypes[i][1] +
                         " in thread " + checkedTypes[i][0] + " is not received");
                    testFailed = true;
                }
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
