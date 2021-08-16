/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.ClassType.InvokeMethod;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ClassType.InvokeMethod.
 *
 * See invokemeth001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class invokemeth001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // package and classes names
    static final String PACKAGE_NAME = "nsk.jdwp.ClassType.InvokeMethod";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "invokemeth001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command
    static final String JDWP_COMMAND_NAME = "ClassType.InvokeMethod";
    static final int JDWP_COMMAND_ID = JDWP.Command.ClassType.InvokeMethod;

    // tested class name and signature
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedObjectClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // field and method names
    static final String RESULT_FIELD_NAME = "result";
    static final String TESTED_METHOD_NAME = "testedMethod";
    static final String BREAKPOINT_METHOD_NAME = "run";
    static final int BREAKPOINT_LINE_NUMBER = invokemeth001a.BREAKPOINT_LINE_NUMBER;

    // data for invoked method
    static final int ARGUMENTS_COUNT = 1;
    static final int INITIAL_VALUE = invokemeth001a.INITIAL_VALUE;
    static final int ARGUMENT_VALUE = invokemeth001a.FINAL_VALUE;
    static final int RETURN_VALUE = INITIAL_VALUE;
    static final int INVOKE_OPTIONS = 0;

    // usual scaffold objects
    ArgumentHandler argumentHandler = null;
    Log log = null;
    Binder binder = null;
    Debugee debugee = null;
    Transport transport = null;
    IOPipe pipe = null;
    int waitTime = 0;  // minutes
    long timeout = 0;  // milliseconds
    boolean dead = false;
    boolean success = true;

    // data obtained from debuggee
    long classID = 0;
    long threadID = 0;
    long methodID = 0;

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
        return new invokemeth001().runIt(argv, out);
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
        classID = debugee.waitForClassLoaded(TESTED_CLASS_NAME, JDWP.SuspendPolicy.ALL);
        log.display("  ... class loaded with classID: " + classID);
        log.display("");

        // query debuggee for tested methodID
        log.display("Getting tested methodID by name: " + TESTED_METHOD_NAME);
        methodID = debugee.getMethodID(classID, TESTED_METHOD_NAME, true);
        log.display("  ... got methodID: " + methodID);
        log.display("");

        // set breakpoint and wait for debugee reached it
        log.display("Waiting for breakpoint reached at: "
                    + BREAKPOINT_METHOD_NAME + ":" +  BREAKPOINT_LINE_NUMBER);
        threadID = debugee.waitForBreakpointReached(classID,
                                                BREAKPOINT_METHOD_NAME,
                                                BREAKPOINT_LINE_NUMBER,
                                                JDWP.SuspendPolicy.ALL);
        log.display("  ... breakpoint reached with threadID: " + threadID);
        log.display("Tested thread is suspended by breakpoint event");
    }

    /**
     * Perform testing JDWP command.
     */
    void testCommand() {
        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  classID: " + classID);
        command.addReferenceTypeID(classID);
        log.display("  threadID: " + threadID);
        command.addObjectID(threadID);
        log.display("  methodID: " + methodID);
        command.addMethodID(methodID);
        log.display("  arguments: " + ARGUMENTS_COUNT);
        command.addInt(ARGUMENTS_COUNT);
        for (int i = 0; i < ARGUMENTS_COUNT; i++) {
            JDWP.Value value = new JDWP.Value(JDWP.Tag.INT, Integer.valueOf(ARGUMENT_VALUE));
            log.display("    arg: " + value);
            command.addValue(value);
        }
        log.display("  options: " + INVOKE_OPTIONS);
        command.addInt(INVOKE_OPTIONS);
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

        ReplyPacket reply = new ReplyPacket();

        // receive reply packet from debugee
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

        // extract return value
        JDWP.Value returnValue = null;
        try {
            returnValue = reply.getValue();
            log.display("    returnValue: " + returnValue);
        } catch (BoundException e) {
            log.complain("Unable to extract returnValues from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // extract exception tag
        JDWP.Value exception = null;
        try {
            exception = reply.getValue();
            log.display("    exception: " + exception);
        } catch (BoundException e) {
            log.complain("Unable to extract exception from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        log.display("  ... packed data parsed");

        // check that return value is an integer
        if (returnValue.getTag() != JDWP.Tag.INT) {
            log.complain("Unexpected tag of returnValue returned: " + returnValue.getTag()
                        + " (expected: " + JDWP.Tag.INT + ")");
            success = false;
        }

        // check that return value is as expected
        int intValue = ((Integer)returnValue.getValue()).intValue();
        if (intValue != RETURN_VALUE) {
            log.complain("Unexpected value of returnValue returned: " + intValue
                        + " (expected: " + RETURN_VALUE + ")");
            success = false;
        }

        // check that exception value is an object
        if (exception.getTag() != JDWP.Tag.OBJECT) {
            log.complain("Unexpected tag of exception returned: " + exception.getTag()
                        + " (expected: " + JDWP.Tag.OBJECT + ")");
            success = false;
        }

        // check that exception object is null
        long exceptionID = ((Long)exception.getValue()).longValue();
        if (exceptionID != 0) {
            log.complain("Non-null exception object returned: " + exceptionID
                        + " (expected: " + 0 + ")");
            success = false;
        }
    }

    /**
     * Check result of the tested JDWP command.
     */
    void checkResult() {
        // query debuggee for result value from a static field
        log.display("Getting result value from static field: " + RESULT_FIELD_NAME);
        JDWP.Value value = debugee.getStaticFieldValue(classID, RESULT_FIELD_NAME, JDWP.Tag.INT);
        int result = ((Integer)value.getValue()).intValue();
        log.display("  ... got result: " + result);

        // check if the result value is changed as expected
        if (result != ARGUMENT_VALUE) {
            log.complain("Method has not been really invoked: \n\t"
                        + "variable not changed by the method: " + result
                        + " (expected: " + ARGUMENT_VALUE + ")");
            success = false;
        } else {
            log.display("Method has been really invoked: \n\t"
                        + " variable changed by the method: " + result
                        + " (expected: " + ARGUMENT_VALUE + ")");
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
