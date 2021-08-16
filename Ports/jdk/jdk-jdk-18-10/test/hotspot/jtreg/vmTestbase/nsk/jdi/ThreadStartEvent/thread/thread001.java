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

package nsk.jdi.ThreadStartEvent.thread;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.Iterator;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger in the test

public class thread001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    static final String DEBUGGEE_NAME = "nsk.jdi.ThreadStartEvent.thread.thread001a";

    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private ThreadStartRequest checkedRequest;
    static private String [][] checkedThreads = {
        {"main","0"},
        {"Thread1","0"},
        {"Thread2","0"}
    };

    static private boolean testFailed;
    static private boolean eventsReceived;
    static private long eventTimeout;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

        testFailed = false;
        eventsReceived = false;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // launch and connect to debugee
        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "debuggee >");

        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();

        // create event request and waits for events
        try {

            log.display("Creating request for ThreadStartEvent");
            EventRequestManager erManager = debuggee.VM().eventRequestManager();
            if ((checkedRequest = erManager.createThreadStartRequest()) == null) {
                log.complain("TEST BUG: unable to create createThreadStartRequest");
                pipe.println(COMMAND_QUIT);
                debuggee.waitFor();
                return thread001.FAILED;
            }
            checkedRequest.enable();

            // start event handler
            EventHandler eventHandler = new EventHandler();
            log.display("Starting eventHandler");
            eventHandler.start();

            // resume debuggee
            log.display("Resuming debuggee");
            debuggee.resume();

            // wait for all expected events handled or timeout exceeds
            try {
                eventHandler.join(eventTimeout);
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: interrupted exception while waiting for events: " + e);
                testFailed = false;
            }

            // interrupt events handler thread if not completed yet
            if (eventHandler.isAlive()) {
                log.display("Interrupting EventHandler");
                eventHandler.interrupt();
            }

            // check that all expected events received
            if (eventsReceived) {
                log.display("\nAll expected events received!");
            } else {
                for (int i = 0; i < checkedThreads.length; i++) {
                    if (checkedThreads[i][1].equals("0")) {
                        log.complain("FAILURE 6: ThreadStartEvent was not received for thread: " + checkedThreads[i][0]);
                    }
                }
                testFailed = true;
            }

        } catch (Failure e) {
            log.complain("TEST FAILURE: " + e.getMessage());
            testFailed = true;
        } catch (Exception e) {
            log.complain("Unexpected exception caught: " + e);
            e.printStackTrace(out);
            testFailed = true;
        } finally {
            // disable event request to prevent appearance of further events
            if (checkedRequest != null) {
                log.display("Disabling event request");
                checkedRequest.disable();
            }

            // force debuggee to quit
            log.display("Sending command: " + COMMAND_QUIT);
            pipe.println(COMMAND_QUIT);

            // waiting for debuggee exits and analize its exit code
            log.display("Waiting for debuggee terminated");
            int exitCode = debuggee.endDebugee();
            if (exitCode != PASSED + JCK_STATUS_BASE) {
                log.complain("Debuggee FAILED with exit code: " + exitCode);
                testFailed = true;
            } else {
                log.display("Debuggee PASSED with exit code: " + exitCode);
            }
        }

        // exit
        if (testFailed) {
            log.complain("TEST FAILED");
            return FAILED;
        }

        log.display("TEST PASSED");
        return PASSED;
    }

    // handle events in a separate thread
    static class EventHandler extends Thread {
        public void run() {
            log.display("EventHandler started");
            eventSet = null;
            while (!eventsReceived) {
                try {
                    eventSet = vm.eventQueue().remove();
                } catch (InterruptedException e) {
                    log.complain("Unexpected interrupted exception while receiving event: " + e);
                    testFailed = false;
                    break;
                }

                EventIterator eventIterator = eventSet.eventIterator();
                while (eventIterator.hasNext()) {

                    Event event = eventIterator.nextEvent();
                    log.display("\nEvent received:\n  " + event);

                    if (event instanceof ThreadStartEvent) {
                        ThreadStartEvent castedEvent = (ThreadStartEvent)event;
                        log.display("Received event is ThreadStartEvent:\n  " + castedEvent);

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

                        ThreadReference eventThread = castedEvent.thread();
                        if (eventThread == null) {
                            log.complain("FAILURE 2: ThreadStartEvent.thread() returns null");
                            testFailed = true;
                        }

                        String threadName = eventThread.name();
                        if ((threadName == null) || (threadName.equals(""))) {
                            log.complain("FAILURE 3: thread reference has invalid empty name");
                            testFailed = true;
                        } else {
                            log.display ("Expected ThreadStartEvent was received for thread: " + threadName);
                        }

                        // Check that thread is in VirtualMachine.allThreads() list
                        boolean found = false;
                        Iterator threadsList = debuggee.VM().allThreads().iterator();
                        while (!found && threadsList.hasNext()) {
                             found = eventThread.equals((ThreadReference)threadsList.next());
                        }
                        if (!found) {
                            log.complain("FAILURE 4: " + threadName + " is not in debuggee's allThreads() list");
                            testFailed = true;
                        }

                        // Check that all expected debuggee's thread create ThreadStartEvent only once
                        for (int i = 0; i < checkedThreads.length; i++) {
                             if (threadName.equals(checkedThreads[i][0])) {
                                  if (checkedThreads[i][1].equals("0")) {
                                       checkedThreads[i][1] = "1";
                                  } else {
                                       log.complain("FAILURE 5: ThreadStartEvent for " + threadName + " is received more that once");
                                       testFailed = true;
                                  }
                             }
                        }

                        // Check whether all expected events received
                        if (!eventsReceived) {
                            for (int i = 0; i < checkedThreads.length; i++) {
                                if (checkedThreads[i][1] == "0") {
                                    eventsReceived = false;
                                    break;
                                }
                                eventsReceived = true;
                            }
                        }
                    }
                    // ignore all other events
                }
                eventSet.resume();
            }
            log.display("EventHandler completed");
        } /* run() */
    } /* EventHandler */

}
