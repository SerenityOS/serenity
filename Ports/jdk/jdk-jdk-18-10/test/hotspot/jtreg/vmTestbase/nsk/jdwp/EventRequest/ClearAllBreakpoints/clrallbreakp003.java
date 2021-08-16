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

package nsk.jdwp.EventRequest.ClearAllBreakpoints;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: EventRequest.ClearAllBreakpoints.
 *
 * See clrallbreakp003.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #waitForTestedEvent()
 */
public class clrallbreakp003 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.EventRequest.ClearAllBreakpoints";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "clrallbreakp003";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "EventRequest.ClearAllBreakpoints";
    static final int JDWP_COMMAND_ID = JDWP.Command.EventRequest.ClearAllBreakpoints;
    static final byte TESTED_EVENT_KIND = JDWP.EventKind.VM_START;
    static final byte TESTED_EVENT_SUSPEND_POLICY = JDWP.SuspendPolicy.ALL;

    // name and signature of the tested class
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // name of field and method of tested class
    static final String TESTED_METHOD_NAME = "run";
    static final int BREAKPOINT_LINE = clrallbreakp003a.BREAKPOINT_LINE;

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
        return new clrallbreakp003().runIt(argv, out);
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
            testCommand();

            log.display("\n>>> Finishing debuggee \n");

            // resume debuggee
            log.display("Resuming debuggee");
            debugee.resume();
            log.display("  ... debuggee resumed");

            // wait for debuggee exited
            log.display("Waiting for fianl VM_DEATH event");
            debugee.waitForVMDeath();
            dead = true;
            log.display("  ... VM_DEATH event received");

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

    void prepareForTest() {
        // wait for tested class loaded
        log.display("Waiting for tested class loaded");
        testedClassID = debugee.waitForClassLoaded(TESTED_CLASS_NAME, JDWP.SuspendPolicy.ALL);
        log.display("  ... got classID: " + testedClassID);
        log.display("");

        // get methodID for tested method
        log.display("Getting tested methodID by name: " + TESTED_METHOD_NAME);
        testedMethodID = debugee.getMethodID(testedClassID, TESTED_METHOD_NAME, true);
        log.display("  ... got methodID: " + testedMethodID);

        // make request for BREAKPOINT event
        log.display("Making request for BREAKPOINT event at: "
                    + TESTED_METHOD_NAME + ":" + BREAKPOINT_LINE);
        eventRequestID = debugee.requestBreakpointEvent(JDWP.TypeTag.CLASS, testedClassID,
                                                        testedMethodID, BREAKPOINT_LINE,
                                                        JDWP.SuspendPolicy.ALL);
        log.display("  ... got requestID: " + eventRequestID);

        // clear request for BREAKPOINT event
        log.display("Clearing BREAKPOINT event requestID: " + eventRequestID);
        debugee.clearEventRequest(JDWP.EventKind.BREAKPOINT, eventRequestID);
        log.display("  ... request removed");
    }


    /**
     * Test JDWP command.
     */
    void testCommand() {
        // create command packet and fill requred out data
        log.display("Create command packet: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("    no out data");
        command.setLength();
        log.display("  ... command packet created");

        // send command packet to debugee
        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
            log.display("  ... command packet sent");
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n\t" + e);
            success = false;
            return;
        }
        log.display("");

        // receive reply packet from debugee
        ReplyPacket reply = new ReplyPacket();
        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("  ... reply packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n\t" + e);
            success = false;
            return;
        }
        log.display("");

        // check reply packet header
        try{
            log.display("Checking header of reply packet");
            reply.checkHeader(command.getPacketID());
            log.display("  ... packet header is correct");
        } catch (BoundException e) {
            log.complain("Wrong header of reply packet for tested command:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // start parsing reply packet data
        log.display("Parsing reply packet data:");
        reply.resetPosition();

        // no out data
        log.display("    no out data");

        log.display("  ... packet data is parsed");

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }
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
