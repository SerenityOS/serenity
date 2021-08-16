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

package nsk.jdi.WatchpointEvent.valueCurrent;

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

public class valuecur001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    static final String DEBUGGEE_NAME = "nsk.jdi.WatchpointEvent.valueCurrent.valuecur001a";
    static final String CHECKED_CLASS_NAME = "nsk.jdi.WatchpointEvent.valueCurrent.CheckedClass";

    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private ReferenceType rType;
    static private WatchpointRequest checkedRequest;
    static private List fieldsList;

    static private volatile boolean testFailed;
    static private volatile boolean done;
    static private volatile int requestsCount, awpEventsCount, mwpEventsCount;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

        testFailed = false;
        requestsCount = 0;
        awpEventsCount = 0;
        mwpEventsCount = 0;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);

        // launch debuggee
        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "valuecur001a >");
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();

        // resume debuggee
        log.display("Resuming debuggee");
        debuggee.resume();

        // check debuggee's capabilities
        if (!debuggee.VM().canWatchFieldAccess()) {
            log.display("TEST WARNING: test passed because JDI implementation does not support " +
             "watchpoints for field access");
            pipe.println(COMMAND_QUIT);
            debuggee.waitFor();
            return PASSED;
        }

        if (!debuggee.VM().canWatchFieldModification()) {
            log.display("TEST WARNING: test passed because JDI implementation does not support " +
             "watchpoints for field modification");
            pipe.println(COMMAND_QUIT);
            debuggee.waitFor();
            return PASSED;
        }

        try {

            // wait for debuggee started
            log.display("Waiting for command: " + COMMAND_READY);
            String command = pipe.readln();
            if (!command.equals(COMMAND_READY)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // get mirrors
            if ((rType = debuggee.classByName(CHECKED_CLASS_NAME)) == null) {
                throw new Failure("TEST BUG: cannot find " + CHECKED_CLASS_NAME);
            }

            // create events requests
            log.display("Creating requests for WatchpointEvent");
            EventRequestManager erManager = debuggee.VM().eventRequestManager();
            Iterator fieldsIter;
            try {
                fieldsList = rType.fields();
                fieldsIter = fieldsList.iterator();
            } catch (ClassNotPreparedException e) {
                throw new Failure( "TEST_BUG: " + rType.name() + " is not prepared");
            }
            while (fieldsIter.hasNext()) {
                Field refField = (Field)fieldsIter.next();
                if ((checkedRequest = erManager.createAccessWatchpointRequest(refField)) == null) {
                    throw new Failure("TEST BUG: unable to create AccessWatchpointRequest");
                } else {
                    log.display("AccessWatchpointRequest for field " +  refField.name() + " created");
                    checkedRequest.enable();
                }
                if ((checkedRequest = erManager.createModificationWatchpointRequest(refField)) == null) {
                    throw new Failure("TEST BUG: unable to create ModificationWatchpointRequest");
                } else {
                    log.display("ModificationWatchpointRequest for field " +  refField.name() + " created");
                    checkedRequest.enable();
                }
                requestsCount++;
            }
            log.display("Created total " + requestsCount + " WatchpointRequests");

            // define separate thread for handling events
            class EventHandler extends Thread {
                  public void run() {
                       eventSet = null;
                       try {
                            // handle events until all events generated and received
                            while ( awpEventsCount < requestsCount
                                    || mwpEventsCount < requestsCount ) {

                                 eventSet = null;
                                 eventSet = vm.eventQueue().remove(TIMEOUT_DELTA);

                                 if (eventSet == null)
                                     continue;

                                 EventIterator eventIterator = eventSet.eventIterator();
                                 while (eventIterator.hasNext()) {

                                     Event event = eventIterator.nextEvent();

                                     // handle WatchpointEvent
                                     if (event instanceof WatchpointEvent) {
                                        WatchpointEvent castedEvent = (WatchpointEvent)event;
                                        Value evValue = castedEvent.valueCurrent();
                                        Field evField = castedEvent.field();

                                        if (event instanceof AccessWatchpointEvent) {
                                            awpEventsCount++;
                                            log.display("AccessWatchpointEvent received for " + evField.name());
                                        } else if (event instanceof ModificationWatchpointEvent) {
                                            mwpEventsCount++;
                                            log.display("ModificationWatchpointEvent received for " + evField.name());
                                        }

                                        if (evValue == null) {
                                             log.complain("FAILURE 1: WatchpointEvent.valueCurrent() returns null for " + evField.name());
                                             testFailed = true;
                                        } else {
                                             int ind = fieldsList.indexOf(evField);
                                             if (ind == -1) {
                                                 log.complain("FAILURE 2: Watchpoint.field() returns unknown field");
                                                 testFailed = true;
                                             } else {
                                                 Value valueCur = castedEvent.valueCurrent();
                                                 Value valueFld;
                                                 try {
                                                     valueFld = castedEvent.object().getValue(evField);
                                                 } catch (NullPointerException e) {
                                                     // field is static
                                                     valueFld = rType.getValue(evField);
                                                 }
                                                 if (!valueFld.equals(valueCur)) {
                                                     log.complain("FAILURE 3: Watchpoint.valueCurrent() returns incorrect value for " +
                                                       evField.name() + "\nvalueCurrent(): " + valueCur + " ; getValue(): " + valueFld);
                                                     testFailed = true;
                                                 } else {
                                                     valuecur001.log.display(evField.name() + " equals to " + castedEvent.valueCurrent().toString());
                                                 }
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

            // force debuggee to generate events
            log.display("Sending command: " + COMMAND_GO);
            pipe.println(COMMAND_GO);

            // wait for confirmation from debugee
            log.display("Waiting for command: " + COMMAND_DONE);
            command = pipe.readln();
            if (command == null || !command.equals(COMMAND_DONE)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // notify EventHandler that all events generated
            done = true;

            // wait for all expected events received
            log.display("Waiting for all expected events received");
            try {
                eventHandler.join(argHandler.getWaitTime()*60000);
                if (eventHandler.isAlive()) {
                    log.complain("FAILURE 20: Timeout for waiting of event was exceeded");
                    eventHandler.interrupt();
                    testFailed = true;
                }
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: InterruptedException caught while waiting for eventHandler's death");
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
