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

package nsk.jdwp.StackFrame.PopFrames;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: StackFrame.PopFrames.
 *
 * See popframes001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class popframes001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // VM capability constatnts
    static final int VM_CAPABILITY_NUMBER = JDWP.Capability.CAN_POP_FRAMES;
    static final String VM_CAPABILITY_NAME = "canPopFrames";

    // package and classes names
    static final String PACKAGE_NAME = "nsk.jdwp.StackFrame.PopFrames";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "popframes001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command
    static final String JDWP_COMMAND_NAME = "StackFrame.PopFrames";
    static final int JDWP_COMMAND_ID = JDWP.Command.StackFrame.PopFrames;

    // tested class name and signature
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedThreadClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // field and method names
    static final String TESTED_METHOD_NAME = "testedMethod";
    static final int BREAKPOINT_LINE_NUMBER = popframes001a.BREAKPOINT_LINE_NUMBER;

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

    // data obtained from debuggee
    long testedClassID = 0;
    long testedThreadID = 0;
    long testedMethodID = 0;
    long testedFrameID = 0;
    int breakpointRequestID = 0;

    // -------------------------------------------------------------------

    /**
     * Start test from command line.
     */
    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    /**
     * Start JCK-compilant test.
     */
    public static int run(String argv[], PrintStream out) {
        return new popframes001().runIt(argv, out);
    }

    // -------------------------------------------------------------------

    /**
     * Perform test execution.
     */
    public int runIt(String argv[], PrintStream out) {

        // make log for debugger messages
        argumentHandler = new ArgumentHandler(argv);
        log = new Log(out, argumentHandler);
        waitTime = argumentHandler.getWaitTime();  // minutes
        timeout = waitTime * 60 * 1000;           // milliseconds

        // execute test and display results
        try {
            log.display("\n>>> Starting debugee \n");

            // launch debuggee
            binder = new Binder(argumentHandler, log);
            log.display("Launching debugee VM");
            debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
            transport = debugee.getTransport();
            log.display("  ... debuggee launched");

            // set timeout for debuggee responces
            log.display("Setting timeout for debuggee responces: " + waitTime + " minute(s)");
            transport.setReadTimeout(timeout);
            log.display("  ... timeout set");

            // wait for VM_INIT event
            log.display("Waiting for VM_INIT event");
            debugee.waitForVMInit();
            log.display("  ... VM_INIT event received");

            // query debugee for VM-dependent ID sizes
            log.display("Querying for IDSizes");
            debugee.queryForIDSizes();
            log.display("  ... size of VM-dependent types adjusted");

            // check for VM capability
            log.display("\n>>> Checking VM capability \n");
            log.display("Getting new VM capability: " + VM_CAPABILITY_NAME);
            boolean capable = debugee.getNewCapability(VM_CAPABILITY_NUMBER, VM_CAPABILITY_NAME);
            log.display("  ... got VM capability: " + capable);

            // exit as PASSED if this capability is not supported
            if (!capable) {
                out.println("TEST PASSED: unsupported VM capability: "
                            + VM_CAPABILITY_NAME);
                return PASSED;
            }

            // prepare debuggee for testing and obtain required data
            log.display("\n>>> Getting prepared for testing \n");
            prepareForTest();

            // test JDWP command
            log.display("\n>> Testing JDWP command \n");
            testCommand();

            // check command results
            if (success) {
                log.display("\n>>> Checking result of tested command \n");
                checkResult();
            }

            // finish debuggee
            log.display("\n>> Finishing debuggee \n");

            // clear BREAKPOIN event request
            log.display("Clearing BREAKPOINT event requestID: " + breakpointRequestID);
            debugee.clearEventRequest(JDWP.EventKind.BREAKPOINT, breakpointRequestID);
            log.display("  ... request cleared");

            // resume debuggee after testing command
            log.display("Resuming debuggee");
            debugee.resume();
            log.display("  ... debuggee resumed");

            // wait for VM_DEATH event
            log.display("Waiting for VM_DEATH event");
            debugee.waitForVMDeath();
            log.display("  ... VM_DEATH event received");
            dead = true;

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            success = false;
        } catch (Exception e) {
            e.printStackTrace(out);
            log.complain("Caught unexpected exception while running the test:\n\t" + e);
            success = false;
        } finally {
            log.display("\n>>> Finishing test \n");

            // disconnect debugee and wait for its exit
            if (debugee != null) {
                quitDebugee();
            }
        }

        // check result
        if (!success) {
            log.complain("TEST FAILED");
            return FAILED;
        }
        out.println("TEST PASSED");
        return PASSED;
    }

    /**
     * Get debuggee prepared for testing and obtain required data.
     */
    void prepareForTest() {
        // wait for tested class loaded on debuggee startup and obtain its classID
        log.display("Waiting for class loaded:\n\t" + TESTED_CLASS_NAME);
        testedClassID = debugee.waitForClassLoaded(TESTED_CLASS_NAME,
                                                    JDWP.SuspendPolicy.ALL);
        log.display("  ... class loaded with classID: " + testedClassID);

        // query debuggee for tested methodID
        log.display("Getting tested methodID by name: " + TESTED_METHOD_NAME);
        testedMethodID = debugee.getMethodID(testedClassID, TESTED_METHOD_NAME, true);
        log.display("  ... got methodID: " + testedMethodID);
        log.display("");

        // make BREAKPOINT event request
        log.display("Creating breakpoint requests at: "
                    + TESTED_METHOD_NAME + ":" +  BREAKPOINT_LINE_NUMBER);
        breakpointRequestID =
                debugee.requestBreakpointEvent(JDWP.TypeTag.CLASS,
                                                testedClassID, testedMethodID,
                                                BREAKPOINT_LINE_NUMBER,
                                                JDWP.SuspendPolicy.ALL);
        log.display("  ... got breakpoint requestID: " + breakpointRequestID);

        // resume debuggee after testing command
        log.display("Resuming debuggee");
        debugee.resume();
        log.display("  ... debuggee resumed");
        log.display("");

        // wait for BREAKPOINT event
        log.display("Waiting for BREAKPOINT event for requestID: " + breakpointRequestID);
        testedThreadID = waitForBreakpointEvent(breakpointRequestID, "first");
        log.display("  ... breakpoint reached with threadID: " + testedThreadID);

        // query debuggee for top frameID
        log.display("Getting top frameID for threadID: " + testedThreadID);
        testedFrameID = queryTopFrameID(testedThreadID);
        log.display("  ... got frameID: " + testedFrameID);
        log.display("");

        // tested thread is suspended by an event
        log.display("Tested thread is suspended by BREAKPOINT event before pop frameID: "
                    + testedFrameID);
    }

    /**
     * Perform testing JDWP command.
     */
    void testCommand() {
        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  threadID: " + testedThreadID);
        command.addObjectID(testedThreadID);
        log.display("  frameID: " + testedFrameID);
        command.addFrameID(testedFrameID);
        command.setLength();

        // send command packet to debugee
        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n\t" + e);
            success = false;
            return;
        }

        // receive reply packet from debugee
        ReplyPacket reply = new ReplyPacket();
        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("  ... reply packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet for tested command:\n\t" + e);
            success = false;
            return;
        }

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

        // no reply data to parse
        log.display("  no reply data");

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        log.display("  ... packed data parsed");
    }

    /**
     * Check result of the tested JDWP command.
     */
    void checkResult() {
        // resume debuggee after sending command
        log.display("Resuming debuggee after command");
        debugee.resume();
        log.display("  ... debuggee resumed");

        // wait for BREAKPOINT event
        log.display("Waiting for second BREAKPOINT event for requestID: " + breakpointRequestID);
        long threadID = waitForBreakpointEvent(breakpointRequestID, "second");
        log.display("  ... breakpoint secondly reached with threadID: " + threadID);

        log.display("Tested method was invoked two times as expected");
    }

    /**
     * Query debuggee for top frameID value for given threadID.
     */
    long queryTopFrameID(long threadID) {
        String error = "Error occured while getting top frameID for threadID: " + threadID;

        CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Frames);
        command.addObjectID(threadID);
        command.addInt(0);        // top frame index
        command.addInt(1);        // number of frames

        ReplyPacket reply = debugee.receiveReplyFor(command, "ThreadReference.Frames");
        reply.resetPosition();

        try {
            int frames = reply.getInt();
            if (frames != 1) {
                log.complain("Wrong number of frames returned in reply packet: " + frames
                            + " (expected: " + 1 + ")");
                throw new Failure(error);
            }
            long frameID = reply.getFrameID();
            return frameID;
        } catch (BoundException e) {
            log.complain("Unable to extract data from reply packet:\n\t"
                        + e.getMessage());
            throw new Failure(error);
        }
    }

    /**
     * Wait for BREAKPOINT event made by the given request and return threadID.
     * Debuggee will be left suspended by the BREAKPOINT event.
     */
    public long waitForBreakpointEvent(int requestID, String kind) {
        String error = "Error occured while waiting for " + kind + " BREAKPOINT event";

        for(;;) {
            EventPacket event = debugee.receiveEvent();
            byte eventSuspendPolicy = 0;
            long eventThreadID = 0;
            try {
                eventSuspendPolicy = event.getByte();
                int events = event.getInt();
                for (int i = 0; i < events; i++) {
                    // check event kind
                    byte eventKind = event.getByte();
                    if (eventKind == JDWP.EventKind.VM_DEATH) {
                        log.complain("Unexpected VM_DEATH event received: " + eventKind
                                    + " (expected: " + JDWP.EventKind.BREAKPOINT +")");
                        dead = true;
                        throw new Failure(error);
                    } else if (eventKind != JDWP.EventKind.BREAKPOINT) {
                        log.complain("Unexpected event kind received: " + eventKind
                                + " (expected: " + JDWP.EventKind.BREAKPOINT +")");
                        throw new Failure(error);
                    }

                    // extract specific BREAKPOINT event data
                    int eventRequestID = event.getInt();
                    eventThreadID = event.getObjectID();
                    JDWP.Location eventLocation = event.getLocation();

                    if (eventRequestID == requestID) {
                        return eventThreadID;
                    } else {
                        log.complain("Unexpected BREAKPOINT event received with requestID: "
                                    + eventRequestID + " (expected: " + requestID + ")");
                    }
                }
            } catch (BoundException e) {
                log.complain("Unable to extract data from event packet while waiting for BREAKPOINT event:\n\t"
                            + e.getMessage() + "\n" + event);
                throw new Failure(error);
            }

            // resume debuggee after unhandled event set
            debugee.resumeEvent(eventSuspendPolicy, eventThreadID);
        }
    }

    /**
     * Disconnect debuggee and wait for it exited.
     */
    void quitDebugee() {
        if (debugee == null)
            return;

        // disconnect debugee
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
