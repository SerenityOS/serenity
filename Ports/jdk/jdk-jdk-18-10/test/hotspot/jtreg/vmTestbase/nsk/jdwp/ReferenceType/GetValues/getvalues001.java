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

package nsk.jdwp.ReferenceType.GetValues;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ReferenceType.GetValues.
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
    static final String PACKAGE_NAME = "nsk.jdwp.ReferenceType.GetValues";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "getvalues001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "ReferenceType.GetValues";
    static final int JDWP_COMMAND_ID = JDWP.Command.ReferenceType.GetValues;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // nested classes names and signatures array
    static final Object fields [][] = {
                    { "booleanValue", "boolean",  Boolean.valueOf(true),           "own"},
                    { "byteValue",    "byte",     Byte.valueOf((byte)0x0F),        "own"},
                    { "charValue",    "char",     Character.valueOf('Z'),          "own"},
                    { "intValue",     "int",      Integer.valueOf(100),            "own"},
                    { "shortValue",   "short",    Short.valueOf((short)10),        "own"},
                    { "longValue",    "long",     Long.valueOf((long)1000000),     "own"},
                    { "floatValue",   "float",    Float.valueOf((float)3.14),      "own"},
                    { "doubleValue",  "double",   Double.valueOf((double)2.8e-12), "own"},
                    { "objectValue",  "objectID", Long.valueOf((long)0),           "own"},

                };
    static final int FIELDS_COUNT = fields.length;

    // field ID's for tested class static fields
    static final long[] fieldIDs = new long[FIELDS_COUNT];

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
                long typeID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got TypeID: " + typeID);

                // query debugee for fieldIDs of tested class static fields
                log.display("Getting fieldIDs for static fields of the tested class");
                queryClassFieldIDs(typeID);

                // perform testing JDWP command
                log.display("\n>>> Testing JDWP command \n");
                testCommand(typeID);

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
     * Query debugee for TypeIDs for nested classes from nested_classes array
     * and put them into nested_classesIDs array.
     */
    void queryClassFieldIDs(long typeID) {
        for (int i = 0; i < FIELDS_COUNT; i++) {
            fieldIDs[i] = 0;
        }

        int count = 0;
        try {
            CommandPacket command = new CommandPacket(JDWP.Command.ReferenceType.Fields);
            command.addReferenceTypeID(typeID);
            command.setLength();

            ReplyPacket reply = debugee.receiveReplyFor(command);
            reply.resetPosition();

            long declared = reply.getInt();
            log.display("  declared: " + declared);

            for (int i = 0; i < declared; i++ ) {
                long fieldID = reply.getFieldID();
                String name = reply.getString();
                String signature = reply.getString();
                int modBits = reply.getInt();

                boolean found = false;
                for (int j = 0; j < FIELDS_COUNT; j++) {
                    if (fields[j][0].equals(name)) {
                        if (fieldIDs[j] != 0) {
                            throw new Failure("Duplicated field name of the tested class returned: " + name);
                        }
                        fieldIDs[j] = fieldID;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Failure("Unexpected field of the tested class returned: " + name);
                }
            }

            if (declared < FIELDS_COUNT) {
                throw new Failure("Too few fields of the tested class returned: " + declared
                                    + " (expected: " + FIELDS_COUNT + ")");
            }

            if (declared > FIELDS_COUNT) {
                throw new Failure("Too many fields of the tested class returned: " + declared
                                    + " (expected: " + FIELDS_COUNT + ")");
            }

            if (!reply.isParsed()) {
                throw new Failure("Extra trailing bytes found in the reply packet at: "
                                    + reply.currentPosition());
            }

        } catch (BoundException e) {
            throw new Failure("Unable to extract field IDs from the reply packet:\n"
                            + e.getMessage());
        }
    }

    /**
     * Extract and check i-th value from the reply packet.
     */
    void checkValue(int i, JDWP.Value value) {
        if (!fields[i][2].equals(value.getValue())) {
            log.complain("Unexpected value for " + i + " field in reply packet: " + value
                            + " (expected value: " + fields[i][2] + ")");
            success = false;
        }
    }

    /**
     * Perform testing JDWP command for specified TypeID.
     */
    void testCommand(long typeID) {
        // create command packet
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);

        // add out data to the command packet
        log.display("  refType: " + typeID);
        command.addReferenceTypeID(typeID);
        log.display("  fields: " + FIELDS_COUNT);
        command.addInt(FIELDS_COUNT);
        for (int i = 0; i < FIELDS_COUNT; i++) {
            log.display("  #" + i +": fieldID: " + fieldIDs[i]);
            command.addFieldID(fieldIDs[i]);
        }
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

        // extract and check number of values
        int values = 0;
        try {
            values = reply.getInt();
            log.display("  values: " + values);

            if (values != FIELDS_COUNT) {
                log.complain("Unexpected number of values in the reply packet:" + values
                            + " (expected: " + FIELDS_COUNT + ")");
                success = false;
            }
        } catch (BoundException e) {
            log.complain("Unable to extract number of values form reply packet:\n" + e.getMessage());
            success = false;
        }

        // extract and check TypeID for each nested class
        for (int i = 0; i < values; i++ ) {
            log.display("  value #" + i + " (field: " + fields[i][0] + ")");

            // extract TypeTag byte
            JDWP.Value value = null;
            try {
                value = reply.getValue();
                log.display("    value: " + value);
            } catch (BoundException e) {
                log.complain("Unable to extract type tag of " + i + " value:\n" + e.getMessage());
                success = false;
                break;
            }

            // extract and check value by known type tag
            checkValue(i, value);
        }

        // check for extra data in reply packet
        if (! reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + "0x" + reply.toHexString(reply.currentDataPosition(), 4));
            success = false;
        }
    }

}
