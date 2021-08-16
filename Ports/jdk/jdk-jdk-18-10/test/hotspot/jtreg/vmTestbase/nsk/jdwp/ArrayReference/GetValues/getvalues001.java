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

package nsk.jdwp.ArrayReference.GetValues;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ArrayReference.GetValues.
 *
 * See getvalues001.README for description of test execution.
 *
 * Test is executed by invoking method runIt().
 * JDWP command is tested in method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class getvalues001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // communication signals constants
    static final String READY = "ready";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.ArrayReference.GetValues";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "getvalues001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "ArrayReference.GetValues";
    static final int JDWP_COMMAND_ID = JDWP.Command.ArrayReference.GetValues;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // name of the static field in the tested class with the tested object value
    static final String ARRAY_FIELD_NAME = getvalues001a.ARRAY_FIELD_NAME;
    static final int ARRAY_LENGTH = getvalues001a.ARRAY_LENGTH;

    // first index and number of array components to get
    static final int ARRAY_FIRST_INDEX = 4;
    static final int ARRAY_ITEMS_COUNT = 10;

    // usual scaffold objects
    ArgumentHandler argumentHandler = null;
    Log log = null;
    Binder binder = null;
    Debugee debugee = null;
    Transport transport = null;
    IOPipe pipe = null;

    // test passed or not
    boolean success = true;

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
        return new getvalues001().runIt(argv, out);
    }

    // -------------------------------------------------------------------

    /**
     * Perform test execution.
     */
    public int runIt(String argv[], PrintStream out) {

        // make log for debugger messages
        argumentHandler = new ArgumentHandler(argv);
        log = new Log(out, argumentHandler);

        // execute test and display results
        try {
            log.display("\n>>> Preparing debugee for testing \n");

            // launch debugee
            binder = new Binder(argumentHandler, log);
            log.display("Launching debugee");
            debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
            transport = debugee.getTransport();
            pipe = debugee.createIOPipe();

            // make debuggee ready for testing
            prepareDebugee();

            // work with prepared debugee
            try {
                log.display("\n>>> Obtaining requred data from debugee \n");

                // query debugee for TypeID of tested class
                log.display("Getting ReferenceTypeID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                // query debuggee for arrayID value from static field
                log.display("Getting arrayID value from static field: "
                            + ARRAY_FIELD_NAME);
                long arrayID = queryObjectID(classID,
                            ARRAY_FIELD_NAME, JDWP.Tag.ARRAY);
                log.display("  got arrayID: " + arrayID);

                // perform testing JDWP command
                log.display("\n>>> Testing JDWP command \n");
                testCommand(arrayID);

            } finally {
                // quit debugee
                log.display("\n>>> Finishing test \n");
                quitDebugee();
            }

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            e.printStackTrace(out);
            success = false;
        } catch (Exception e) {
            log.complain("Caught unexpected exception:\n" + e);
            e.printStackTrace(out);
            success = false;
        }

        if (!success) {
            log.complain("TEST FAILED");
            return FAILED;
        }

        out.println("TEST PASSED");
        return PASSED;

    }

    /**
     * Prepare debugee for testing and waiting for ready signal.
     */
    void prepareDebugee() {
        // wait for VM_INIT event from debugee
        log.display("Waiting for VM_INIT event");
        debugee.waitForVMInit();

        // query debugee for VM-dependent ID sizes
        log.display("Querying for IDSizes");
        debugee.queryForIDSizes();

        // resume initially suspended debugee
        log.display("Resuming debugee VM");
        debugee.resume();

        // wait for READY signal from debugee
        log.display("Waiting for signal from debugee: " + READY);
        String signal = pipe.readln();
        log.display("Received signal from debugee: " + signal);
        if (! signal.equals(READY)) {
            throw new TestBug("Unexpected signal received form debugee: " + signal
                            + " (expected: " + READY + ")");
        }
    }

    /**
     * Sending debugee signal to quit and waiting for it exits.
     */
    void quitDebugee() {
        // send debugee signal to quit
        log.display("Sending signal to debugee: " + QUIT);
        pipe.println(QUIT);

        // wait for debugee exits
        log.display("Waiting for debugee exits");
        int code = debugee.waitFor();

        // analize debugee exit status code
        if (code == JCK_STATUS_BASE + PASSED) {
            log.display("Debugee PASSED with exit code: " + code);
        } else {
            log.complain("Debugee FAILED with exit code: " + code);
            success = false;
        }
    }

    /**
     * Query debuggee for objectID value of static class field.
     */
    long queryObjectID(long classID, String fieldName, byte tag) {
        // get fieledID for static field (declared in the class)
        long fieldID = debugee.getClassFieldID(classID, fieldName, true);
        // get value of the field
        JDWP.Value value = debugee.getStaticFieldValue(classID, fieldID);

        // check that value has THREAD tag
        if (value.getTag() != tag) {
            throw new Failure("Wrong objectID tag received from field \"" + fieldName
                            + "\": " + value.getTag() + " (expected: " + tag + ")");
        }

        // extract threadID from the value
        long objectID = ((Long)value.getValue()).longValue();
        return objectID;
    }

    /**
     * Perform testing JDWP command for specified objectID.
     */
    void testCommand(long arrayID) {
        // create command packet
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);

        // add out data to the command packet
        log.display("  arrayID: " + arrayID);
        command.addObjectID(arrayID);
        log.display("  firstIndex: " + ARRAY_FIRST_INDEX);
        command.addInt(ARRAY_FIRST_INDEX);
        log.display("  length: " + ARRAY_ITEMS_COUNT);
        command.addInt(ARRAY_ITEMS_COUNT);
        command.setLength();

        // send command packet to debugee
        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n" + e);
            success = false;
            return;
        }

        ReplyPacket reply = new ReplyPacket();

        // receive reply packet from debugee
        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("Reply packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n" + e);
            success = false;
            return;
        }

        // check reply packet header
        try{
            log.display("Checking reply packet header");
            reply.checkHeader(command.getPacketID());
        } catch (BoundException e) {
            log.complain("Bad header of reply packet: " + e.getMessage());
            success = false;
        }

        // start parsing reply packet data
        log.display("Parsing reply packet:");
        reply.resetPosition();

        // extract values tag
        byte tag = (byte)0;
        try {
            tag = reply.getByte();
            log.display("  tag: " + tag);

        } catch (BoundException e) {
            log.complain("Unable to extract values tag from reply packet:\n\t"
                        + e.getMessage());
            success = false;
        }

        // check if number of values are as expected
        if (tag != JDWP.Tag.INT) {
            log.complain("Unexpected values tag received:" + tag
                        + " (expected: " + JDWP.Tag.INT + ")");
            success = false;
        }

        // extract number of values
        int values = 0;
        try {
            values = reply.getInt();
            log.display("  values: " + values);

        } catch (BoundException e) {
            log.complain("Unable to extract number of values from reply packet:\n\t"
                        + e.getMessage());
            success = false;
        }

        // check if number of values are as expected
        if (values < 0) {
            log.complain("Negative number of values received:" + values
                        + " (expected: " + ARRAY_ITEMS_COUNT + ")");
            success = false;
        } else if (values != ARRAY_ITEMS_COUNT) {
            log.complain("Unexpected number of values received:" + values
                        + " (expected: " + ARRAY_ITEMS_COUNT + ")");
            success = false;
        }

        // extract and check each value
        for (int i = 0; i < values; i++ ) {
            int index = i + ARRAY_FIRST_INDEX;
            log.display("  value #" + i + " (index: " + index  + ")");

            // extract value
            JDWP.UntaggedValue value = null;
            try {
                value = reply.getUntaggedValue(JDWP.Tag.INT);
                log.display("    untagged_value: " + value);
            } catch (BoundException e) {
                log.complain("Unable to extract " + i + " value from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                break;
            }

            // check that extracted value is as expected
            int intValue = ((Integer)value.getValue()).intValue();
            if (intValue != index * 10) {
                log.complain("Unexpected value for " + index + " component received: "
                            + intValue + " (expected: " + (index * 10) + ")");
                success = false;
            }
        }

        // check for extra data in reply packet
        if (! reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + "0x" + reply.toHexString(reply.currentDataPosition(), 4));
            success = false;
        }
    }

}
