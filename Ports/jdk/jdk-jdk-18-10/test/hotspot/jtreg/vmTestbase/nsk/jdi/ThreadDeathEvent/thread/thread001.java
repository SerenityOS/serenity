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

package nsk.jdi.ThreadDeathEvent.thread;

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

    //synchronization commands
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";
    static final String COMMAND_GO = "go";
    static final String COMMAND_DONE = "done";
    static final String DEBUGGEE_NAME = "nsk.jdi.ThreadDeathEvent.thread.thread001a";

    // scaffold objects
    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private ThreadDeathRequest checkedRequest;
//    static private ThreadReference    eventThread;
    static private String checkedThreads [][] = {
                                            {"innerThread","0"},
                                            {"innerDaemon","0"},
                                            {"outerThread","0"},
                                            {"outerDaemon","0"}
                                        };

    static private long eventTimeout;
    static private int eventsExpected;
    static private int eventsReceived;

    static volatile private boolean testFailed = false;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

        testFailed = false;
        eventsExpected = checkedThreads.length;
        eventsReceived = 0;

        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // launch and connect to debuggee
        Binder binder = new Binder(argHandler, log);
        log.display("Connecting to debuggee");
        debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
        debuggee.redirectStderr(log, "debuggee >");

        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();

        // resume debuggee suspended at start up
        log.display("Resuming debuggee");
        debuggee.resume();

        // perform the test, catch exceptions and finally quit debuggee
        try {

            // wait for debuggee started
            log.display("Waiting for command: " + COMMAND_READY);
            String command = pipe.readln();
            if (!command.equals(COMMAND_READY)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
            }

            // create and enable event request
            log.display("Creating request for ClassPrepareEvent");
            EventRequestManager erManager = debuggee.VM().eventRequestManager();
            if ((checkedRequest = erManager.createThreadDeathRequest()) == null) {
                log.complain("TEST BUG: unable to create createThreadDeathRequest");
                pipe.println(COMMAND_QUIT);
                debuggee.waitFor();
                return thread001.FAILED;
            }
            checkedRequest.enable();

            // define separate thread for handling events
            class EventHandler extends Thread {
                public void run() {
                    eventSet = null;
                    try {
                        eventHandlingLoop:
                        while (eventsReceived < eventsExpected) {
                            eventSet = vm.eventQueue().remove();

                            EventIterator eventIterator = eventSet.eventIterator();
                            while (eventIterator.hasNext()) {

                                Event event = eventIterator.nextEvent();
                                log.display("\nEvent received:\n  " + event);

                                // handle ThreadDeathEvent
                                if (event instanceof ThreadDeathEvent) {
                                    log.display("\nThreadDeathEvent received");
                                    ThreadDeathEvent castedEvent = (ThreadDeathEvent)event;

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

                                    ThreadReference eventThread = ((ThreadDeathEvent)event).thread();
                                    if (eventThread == null) {
                                        log.complain("FAILURE 2: ThreadDeathEvent.thread() returns null");
                                        testFailed = true;
                                    }

                                    String threadName = eventThread.name();
                                    if ((threadName == null) || (threadName.equals(""))) {
                                        log.complain("FAILURE 3: thread reference has invalid empty name");
                                        testFailed = true;
                                    } else {
                                        log.display ("Expected ThreadDeathEvent was received for " + threadName);
                                    }

                                    // Check that all expected debuggee's thread create ThreadDeathEvent only once
                                    eventsReceived = 0;
                                    for (int i = 0; i < checkedThreads.length; i++) {
                                        if (threadName.equals(checkedThreads[i][0])) {
                                            if (checkedThreads[i][1].equals("0")) {
                                                checkedThreads[i][1] = "1";
                                            } else {
                                                log.complain("FAILURE 5: ThreadDeathEvent for " + threadName + " is received more that once");
                                                testFailed = true;
                                            }
                                        }
                                        if (checkedThreads[i][1].equals("1")) {
                                            eventsReceived++;
                                        }
                                    }
                                }

                                // ignore each other events

                            } // end of event handling loop

//                            log.display("Resuming event set");
                            eventSet.resume();

                        } // end of event set handling loop

                    } catch (InterruptedException e) {
                        log.complain("TEST INCOMPLETE: caught InterruptionException while waiting for event");
                    } catch (VMDisconnectedException e) {
                        log.complain("TEST INCOMPLETE: caught VMDisconnectedException while waiting for event");
                        testFailed = true;
                    }

                    log.display("Disabling event request");
                    checkedRequest.disable();

                    log.display("eventHandler completed");

                } // end of run()

            } // end of EventHandler

            // start EventHandler thread
            EventHandler eventHandler = new EventHandler();
            log.display("Starting eventHandler");
            eventHandler.start();

            // force debuggee to quit
            log.display("Sending command: " + COMMAND_GO);
            pipe.println(COMMAND_GO);

            // wait for all tested threads completed
            command = pipe.readln();
            if (!command.equals(COMMAND_DONE)) {
                throw new Failure("TEST BUG: unexpected debuggee's command: " + command);
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

            // Check that all expected debuggee's thread created ThreadDeathEvent
            for (int i = 0; i < checkedThreads.length; i++) {
                if (checkedThreads[i][1].equals("0")) {
                    log.complain("FAILURE 1: ThreadDeathEvent for thread " + checkedThreads[i][0] + " is not received");
                    testFailed = true;
                }
            }

        } catch (Failure e) {
            log.complain("TEST FAILURE: " + e.getMessage());
            testFailed = true;
        } catch (Exception e) {
            log.complain("TEST FAILURE: Unexpected exception: " + e);
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
