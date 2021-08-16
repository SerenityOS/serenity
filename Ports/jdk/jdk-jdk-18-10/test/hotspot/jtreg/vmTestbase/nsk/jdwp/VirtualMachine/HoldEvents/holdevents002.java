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

package nsk.jdwp.VirtualMachine.HoldEvents;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: VirtualMachine.HoldEvents.
 *
 * See holdevents002.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 *
 * @see #runIt()
 */
public class holdevents002 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.VirtualMachine.HoldEvents";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "holdevents002";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "VirtualMachine.HoldEvents";
    static final int JDWP_COMMAND_ID = JDWP.Command.VirtualMachine.HoldEvents;
    static final byte TESTED_EVENT_KIND = JDWP.EventKind.BREAKPOINT;
    static final byte TESTED_EVENT_SUSPEND_POLICY = JDWP.SuspendPolicy.ALL;
    static final byte TESTED_EVENT_MODIFIER = JDWP.EventModifierKind.LOCATION_ONLY;

    // name and signature of the tested class
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // name of field and method of tested class
    static final String TESTED_METHOD_NAME = "run";
    static final int BREAKPOINT_LINE = holdevents002a.BREAKPOINT_LINE;

    // usual scaffold objects
    ArgumentHandler argumentHandler = null;
    Log log = null;
    Binder binder = null;
    Debugee debugee = null;
    Transport transport = null;
    int waitTime = 0;  // minutes
    long timeout = 0;  // milliseconds
    boolean dead = false;
    boolean success = true;

    // obtained data
    long testedClassID = 0;
    long testedMethodID = 0;
    int eventRequestID = 0;

    // -------------------------------------------------------------------

    /**
     * Start test from command line.
     */
    public static void main(String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    /**
     * Start test from JCK-compilant environment.
     */
    public static int run(String argv[], PrintStream out) {
        return new holdevents002().runIt(argv, out);
    }

    // -------------------------------------------------------------------

    /**
     * Perform test execution.
     */
    public int runIt(String argv[], PrintStream out) {

        // make log for debugger messages
        argumentHandler = new ArgumentHandler(argv);
        log = new Log(out, argumentHandler);
        waitTime = argumentHandler.getWaitTime();
        timeout = waitTime * 60 * 1000;

        // execute test and display results
        try {
            log.display("\n>>> Starting debugee \n");

            // launch debuggee
            binder = new Binder(argumentHandler, log);
            log.display("Launching debugee");
            debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
            transport = debugee.getTransport();
            log.display("  ... debugee launched");
            log.display("");

            // set timeout for debuggee responces
            log.display("Setting timeout for debuggee responces: " + waitTime + " minute(s)");
            transport.setReadTimeout(timeout);
            log.display("  ... timeout set");

            // wait for debuggee started
            log.display("Waiting for VM_INIT event");
            debugee.waitForVMInit();
            log.display("  ... VM_INIT event received");

            // query debugee for VM-dependent ID sizes
            log.display("Querying for IDSizes");
            debugee.queryForIDSizes();
            log.display("  ... size of VM-dependent types adjusted");

            // get debuggee prepared for testing
            log.display("\n>>> Get debuggee prepared for testing \n");
            prepareForTest();

            // test JDWP command
            log.display("\n>>> Testing JDWP command \n");

            log.display("Holding events into debuggee");
            holdEvents();
            log.display("  ... events held");

            // resume debuggee
            log.display("Resuming debuggee");
            debugee.resume();
            log.display("  ... debuggee resumed");

            // wait for BREAKPOINT event NOT occured
            log.display("Waiting for BREAKPOINT event NOT received");
            waitForBreakpointEvent();

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            success = false;
        } catch (Exception e) {
            e.printStackTrace(out);
            log.complain("Caught unexpected exception while running the test:\n\t" + e);
            success = false;
        } finally {
            // quit debugee
            log.display("\n>>> Finishing test \n");
            quitDebugee();
        }

        // check test results
        if (!success) {
            log.complain("TEST FAILED");
            return FAILED;
        }

        out.println("TEST PASSED");
        return PASSED;

    }

    /**
     * Prepare debuggee for testing.
     */
    void prepareForTest() {
        // wait for tested class loaded
        log.display("Waiting for tested class loaded");
        testedClassID = debugee.waitForClassLoaded(TESTED_CLASS_NAME, JDWP.SuspendPolicy.ALL);
        log.display("  ... got classID: " + testedClassID);
        log.display("");

        // get methodID for breakpoint method
        log.display("Getting breakpoint methodID by name: " + TESTED_METHOD_NAME);
        testedMethodID = debugee.getMethodID(testedClassID, TESTED_METHOD_NAME, true);
        log.display("  ... got methodID: " + testedMethodID);

        // make request for BREAKPOINT event
        log.display("Making request for BREAKPOINT event at: "
                    + TESTED_METHOD_NAME + ":" + BREAKPOINT_LINE);
        eventRequestID = debugee.requestBreakpointEvent(JDWP.TypeTag.CLASS, testedClassID,
                                                        testedMethodID, BREAKPOINT_LINE,
                                                        JDWP.SuspendPolicy.ALL);
        log.display("  ... got requestID: " + eventRequestID);
    }

    /**
     * Hold events into debuggee.
     */
    void holdEvents() {
        CommandPacket command = new CommandPacket(JDWP.Command.VirtualMachine.HoldEvents);
        ReplyPacket reply = debugee.receiveReplyFor(command);
    }

    /**
     * Wait for requested BREAKPOINT event NOT occured.
     */
    void waitForBreakpointEvent() {

        EventPacket eventPacket = new EventPacket();

        // receive reply packet from debugee
        try {
            log.display("Waiting for event packet");
            transport.setReadTimeout(timeout);
            transport.read(eventPacket);
            log.display("  ... event packet received:\n" + eventPacket);
        } catch (IOException e) {
            log.display("No event packet received for default timeout:\n\t" + e);
            log.display("Requested BREAKPOINT event is not received after holding events");
            return;
        }

        log.complain("An event received after holding events into debuggee");
        log.display("");

        // check reply packet header
        try{
            log.display("Checking header of event packet");
            eventPacket.checkHeader();
            log.display("  ... packet header is correct");
        } catch (BoundException e) {
            log.complain("Bad header of received event packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // start parsing reply packet data
        log.display("Parsing event packet:");
        eventPacket.resetPosition();

        // get suspendPolicy value
        byte suspendPolicy = 0;
        try {
            suspendPolicy = eventPacket.getByte();
            log.display("    suspendPolicy: " + suspendPolicy);
        } catch (BoundException e) {
            log.complain("Unable to get suspendPolicy value from received event packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // get events count
        int events = 0;
        try {
            events = eventPacket.getInt();
            log.display("    events: " + events);
        } catch (BoundException e) {
            log.complain("Unable to get events count from received event packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check events count
        if (events < 0) {
            log.complain("Negative value of events number in received event packet: " +
                        events + " (expected: " + 1 + ")");
            success = false;
        } else if (events != 1) {
            log.complain("Invalid number of events in received event packet: " +
                        events + " (expected: " + 1 + ")");
            success = false;
        }

        // extract each event
        long eventThreadID = 0;
        for (int i = 0; i < events; i++) {
            log.display("    event #" + i + ":");

            // get eventKind
            byte eventKind = 0;
            try {
                eventKind = eventPacket.getByte();
                log.display("      eventKind: " + eventKind);
            } catch (BoundException e) {
                log.complain("Unable to get eventKind of event #" + i + " from received event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check eventKind
            if (eventKind == JDWP.EventKind.VM_DEATH) {
                log.display("Unexpected VM_DEATH event received intead of BREAKPOINT event");
                success = false;
                dead = true;
                return;
            }  else if (eventKind == JDWP.EventKind.BREAKPOINT) {
                log.complain("Hold BREAKPOINT event received in event packet: " +
                            eventKind + " (expected: " + JDWP.EventKind.BREAKPOINT + ")");
                success = false;
            }  else {
                log.complain("Unexpected eventKind of event " + i + " in event packet: " +
                            eventKind + " (expected: " + JDWP.EventKind.BREAKPOINT + ")");
                success = false;
                return;
            }

            // get requestID
            int requestID = 0;
            try {
                requestID = eventPacket.getInt();
                log.display("      requestID: " + requestID);
            } catch (BoundException e) {
                log.complain("Unable to get requestID of event #" + i + " from BREAKPOINT event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check requestID
            if (requestID != eventRequestID) {
                log.complain("Unexpected requestID of event " + i + " in BREAKPOINT event packet: " +
                            requestID + " (expected: " + eventRequestID + ")");
                success = false;
            }

            // get threadID
            long threadID = 0;
            try {
                threadID = eventPacket.getObjectID();
                log.display("      threadID: " + threadID);
            } catch (BoundException e) {
                log.complain("Unable to get threadID of event #" + i + " from BREAKPOINT event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // get location
            JDWP.Location location = null;
            try {
                location = eventPacket.getLocation();
                log.display("      location: " + location);
            } catch (BoundException e) {
                log.complain("Unable to get location of event #" + i + " from BREAKPOINT event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }
        }

        // check for extra data in event packet
        if (!eventPacket.isParsed()) {
            log.complain("Extra trailing bytes found in event packet at: "
                        + eventPacket.offsetString());
            success = false;
        }

        log.display("  ... event packet parsed");
    }

    /**
     * Disconnect debuggee and wait for it exited.
     */
    void quitDebugee() {
        if (debugee == null)
            return;

        // disconnect debugee if not dead
        if (!dead) {
            try {
                log.display("Disconnecting debuggee");
                debugee.dispose();
                log.display("  ... debuggee disconnected");
            } catch (Failure e) {
                log.display("Failed to finally disconnect debuggee:\n\t"
                            + e.getMessage());
            }
        }

        // wait for debugee exited
        log.display("Waiting for debuggee exit");
        int code = debugee.waitFor();
        log.display("  ... debuggee exited with exit code: " + code);

        // analize debugee exit status code
        if (code != JCK_STATUS_BASE + PASSED) {
            log.complain("Debuggee FAILED with exit code: " + code);
            success = false;
        }
    }

}
