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

package nsk.jdi.ClassUnloadEvent.className;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.List;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger application in the test

public class classname001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY    = "ready";
    static final String COMMAND_QUIT     = "quit";
    static final String COMMAND_LOAD     = "load";
    static final String COMMAND_LOADED   = "loaded";
    static final String COMMAND_UNLOAD   = "unload";
    static final String COMMAND_UNLOADED = "unloaded";

    static final String PREFIX = "nsk.jdi.ClassUnloadEvent.className";
    static final String DEBUGGEE_NAME = PREFIX + ".classname001a";
    static final String CHECKED_CLASS = PREFIX + ".classname001b";
    static final String KLASSLOADER   = ClassUnloader.INTERNAL_CLASS_LOADER_NAME;

    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private ClassUnloadRequest checkedRequest;

    static private long eventTimeout;
    static private boolean testFailed;
    static private boolean eventReceived;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

        testFailed = false;
        eventReceived = false;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // launch debugee

        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "classname001a >");

        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();

        // create request and wait for ClassUnloadEvent

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
            ReferenceType rType;
            if ((rType = debuggee.classByName(DEBUGGEE_NAME)) == null) {
                throw new Failure("TEST BUG: cannot find debuggee's class " + DEBUGGEE_NAME);
            }

            // send command to load checked class and waits for a confirmation
            pipe.println(COMMAND_LOAD);
            log.display("Waiting for checked class is loaded");
            command = pipe.readln();

            if (command == null || !command.equals(COMMAND_LOADED)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // checked class has been loaded!
            log.display("Checked class has been loaded in debuggee!");

            // check that checked class is loaded in debugee
            log.display("Finding checked class in debuggee");
            if ((rType = debuggee.classByName(CHECKED_CLASS)) == null) {
                throw new Failure("TEST BUG: cannot find checked class loaded: " + CHECKED_CLASS);
            }
            rType = null;

            // check that user-defined classloader is also loaded in debuggee
            log.display("Finding user-defined class loader in debuggee");
            if ((rType = debuggee.classByName(KLASSLOADER)) == null) {
                throw new Failure("TEST BUG: cannot find user-defined classloader loaded: " + KLASSLOADER);
            }
            rType = null;

            // create request for class unload event
            log.display("Creating request for ClassUnloadEvent");
            EventRequestManager erManager = vm.eventRequestManager();
            if ((checkedRequest = erManager.createClassUnloadRequest()) == null) {
                throw new Failure("TEST BUG: unable to create ClassUnloadRequest");
            } else {
                log.display("ClassUnloadRequest created");
            }
//            checkedRequest.addClassFilter("nsk.jdi.ClassUnloadEvent.className.*");

            // enable event request
            log.display("Enabling event request");
            checkedRequest.enable();

            // turn off pipe pinging
            pipe.setPingTimeout(0);

            // force checked class to be unloaded from debuggee
            log.display("Waiting for checked class is unloaded");
            pipe.println(COMMAND_UNLOAD);
            command = pipe.readln();

            // end the test if checked class has not been actually unloaded or error occurs
            if (command != null && command.equals(COMMAND_LOADED)) {
                throw new Warning("TEST INCOMPLETE: unable to unload class");
            }

            if (command == null || !command.equals(COMMAND_UNLOADED)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // checked class has been unloaded
            log.display("Checked class forced to be unloaded from debuggee!");

            // waiting for event until timeout exceeds
            log.display("Waiting for ClassUnloadEvent for checked class");
            long timeToFinish = System.currentTimeMillis() + eventTimeout;
            while (!eventReceived && System.currentTimeMillis() < timeToFinish) {

                // get next event set
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

                    // handle ClassUnloadEvent
                    if (event instanceof ClassUnloadEvent) {

                        ClassUnloadEvent castedEvent = (ClassUnloadEvent)event;
                        log.display("Received event is ClassUnloadEvent:\n  " + castedEvent);

                        // check that received event is for checked request
                        EventRequest eventRequest = castedEvent.request();
                        if (!(checkedRequest.equals(eventRequest))) {
                            log.complain("FAILURE 1: eventRequest is not equal to checked request");
                            testFailed = true;
                        }

                        // check that received event is for checked VM
                        VirtualMachine eventMachine = castedEvent.virtualMachine();
                        if (!(vm.equals(eventMachine))) {
                            log.complain("FAILURE 2: eventVirtualMachine is not equal to checked vm");
                            testFailed = true;
                        }

                        // test the method ClassUnloadEvent.className()
                        String refName = castedEvent.className();

                        // check that received event is for checked class
                        log.display("ClassUnloadEvent is received for " + refName);
                        if ((refName == null) || (refName.equals(""))) {
                            log.complain("FAILURE 3: ClassUnloadEvent.className() returns null or empty string");
                            testFailed = true;
                        } else if (refName.equals(CHECKED_CLASS)) {

                            // mark that ClassUnloadEvent for checked class received
                            eventReceived = true;
                            log.display("Expected ClassUnloadEvent for checked class received!");

/*
                            // check that checked class is not included in debuggee's list of loaded classes
                            List loadedClasses = vm.classesByName(CHECKED_CLASS);
                            if (loadedClasses != null) {
                                log.complain("FAILURE 4: ClassUnloadEvent is received for class to be unloaded\n"
                                           + "           but class still presents in the list of all debuggee classes");
                                testFailed = true;
                            }
*/

                        } else {

                            // ClassUnloadEvent for another class received; just display it
                            List loadedClasses = vm.classesByName(refName);
                            if (loadedClasses != null) {
                                log.display("ClassUnloadEvent was received for loaded class " + refName);
                            }
                        }
                    }

                    // ignore all other events
                 }

                 log.display("Resuming event set");
                 eventSet.resume();
            }

            log.display("");

            // check that expected event has been received
            log.display("Searching checked class in debuggee");
            rType = debuggee.classByName(CHECKED_CLASS);
            if (rType != null) {
                if (eventReceived) {
                    log.complain("FAILURE 4: ClassUnloadEvent is received for class to be unloaded\n"
                               + "           but class still presents in the list of all debuggee classes");
                    testFailed = true;
                } else {
                    log.display("WARNING: Unable to test ClassUnloadEvent because checked class\n"
                              + "         was not actually unloaded");
                }
            } else {
                if (!eventReceived) {
                    log.complain("FAILURE 6: ClassUnloadEvent was not received for class to be unloaded\n"
                               + "           but class no longe presents in the list of all debuggee classes ");
                    testFailed = true;
                }
            }

        } catch (Warning e) {
            log.display("WARNING: " + e.getMessage());
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
        return PASSED;
    }

    static class Warning extends Failure {
        Warning(String msg) {
            super(msg);
        }
    }

}
