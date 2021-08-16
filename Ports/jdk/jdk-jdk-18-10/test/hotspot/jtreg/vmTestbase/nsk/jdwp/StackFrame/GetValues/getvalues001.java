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

package nsk.jdwp.StackFrame.GetValues;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: StackFrame.GetValues.
 *
 * See getvalues001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
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
    static final String ERROR = "error";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.StackFrame.GetValues";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "getvalues001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "StackFrame.GetValues";
    static final int JDWP_COMMAND_ID = JDWP.Command.StackFrame.GetValues;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + getvalues001a.OBJECT_CLASS_NAME;
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // names of the static fields with the tested thread and object values
    static final String TESTED_THREAD_FIELD_NAME = getvalues001a.THREAD_FIELD_NAME;
    static final String TESTED_OBJECT_FIELD_NAME = getvalues001a.OBJECT_FIELD_NAME;
    static final String TESTED_OBJECT_METHOD_NAME = getvalues001a.OBJECT_METHOD_NAME;

    // list of tested variables names and values
    static final Object variables[][] = {
                    { "booleanValue", "boolean",  Boolean.valueOf(true),           Byte.valueOf(JDWP.Tag.BOOLEAN)},
                    { "byteValue",    "byte",     Byte.valueOf((byte)0x0F),        Byte.valueOf(JDWP.Tag.BYTE)   },
                    { "charValue",    "char",     Character.valueOf('Z'),          Byte.valueOf(JDWP.Tag.CHAR)   },
                    { "intValue",     "int",      Integer.valueOf(100),            Byte.valueOf(JDWP.Tag.INT)    },
                    { "shortValue",   "short",    Short.valueOf((short)10),        Byte.valueOf(JDWP.Tag.SHORT)  },
                    { "longValue",    "long",     Long.valueOf((long)1000000),     Byte.valueOf(JDWP.Tag.LONG)   },
                    { "floatValue",   "float",    Float.valueOf((float)3.14),      Byte.valueOf(JDWP.Tag.FLOAT)  },
                    { "doubleValue",  "double",   Double.valueOf((double)2.8e-12), Byte.valueOf(JDWP.Tag.DOUBLE) },
                    { "objectValue",  "objectID", Long.valueOf((long)0),           Byte.valueOf(JDWP.Tag.OBJECT) }
                };
    static final int VARIABLES_COUNT = variables.length;

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

            // launch debuggee
            binder = new Binder(argumentHandler, log);
            log.display("Launching debugee");
            debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
            transport = debugee.getTransport();
            pipe = debugee.createIOPipe();

            // make debuggee ready for testing
            prepareDebugee();

            // work with prepared debuggee
            long threadID = 0;
            try {
                log.display("\n>>> Obtaining requred data from debugee \n");

                // query debuggee for classID of tested class
                log.display("Getting tested classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                // query debuggee for tested methodID
                log.display("Getting tested methodID by name: " + TESTED_OBJECT_METHOD_NAME);
                long methodID = debugee.getMethodID(classID, TESTED_OBJECT_METHOD_NAME, true);
                log.display("  got methodID: " + methodID);

                // query debugee for indexes of the method local variables
                log.display("Getting indexes of the tested local variables for methodID: " + methodID);
                int indexes[] = queryVariableIndexes(classID, methodID);
                log.display("  got indexes: " + indexes.length);

                // query debuggee for tested objectID value from static field
                log.display("Getting tested objectID value from static field: "
                            + TESTED_OBJECT_FIELD_NAME);
                long objectID = queryObjectID(classID, TESTED_OBJECT_FIELD_NAME, JDWP.Tag.OBJECT);
                log.display("  got objectID: " + objectID);

                // query debuggee for tested threadID value from static field
                log.display("Getting tested threadID value from static field: "
                            + TESTED_THREAD_FIELD_NAME);
                threadID = queryObjectID(classID, TESTED_THREAD_FIELD_NAME, JDWP.Tag.THREAD);
                log.display("  got threadID: " + threadID);

                // suspend tested thread into debuggee
                log.display("Suspending thread into debuggee for threadID: " + threadID);
                debugee.suspendThread(threadID);
                log.display("  thread suspended");

                // query debuggee for current frameID of the tested thread
                log.display("Getting current frameID for the threadID: "
                            + threadID);
                long frameID = debugee.getCurrentFrameID(threadID);
                log.display("  got frameID: " + frameID);

                // perform testing JDWP command
                log.display("\n>>> Testing JDWP command \n");
                testCommand(frameID, threadID, indexes);

            } finally {
                log.display("\n>>> Finishing test \n");

                // resume suspended thread
                if (threadID != 0) {
                    log.display("Resuming suspended thread");
                    debugee.resumeThread(threadID);
                }

                // quit debugee
                quitDebugee();
            }

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            success = false;
        } catch (Exception e) {
            e.printStackTrace(out);
            log.complain("Caught unexpected exception while running the test:\n\t" + e);
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
        if (signal == null) {
            throw new TestBug("Null signal received from debugee: " + signal
                            + " (expected: " + READY + ")");
        } else if (signal.equals(ERROR)) {
            throw new TestBug("Debugee was not able to start tested thread"
                            + " (received signal: " + signal + ")");
        } else if (!signal.equals(READY)) {
            throw new TestBug("Unexpected signal received from debugee: " + signal
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
     * Query debuggee for objectID value of static field of the class.
     */
    long queryObjectID(long classID, String fieldName, byte tag) {
        // get fieledID for static field (declared in the class)
        long fieldID = debugee.getClassFieldID(classID, fieldName, true);
        // get value of the field
        JDWP.Value value = debugee.getStaticFieldValue(classID, fieldID);

        // check that value has expected tag
        if (value.getTag() != tag) {
            throw new Failure("Unexpected tag for object value returned: "
                + value.getTag() + " (expected: " + tag + ")");
        }

        // extract objectID from the value
        long objectID = ((Long)value.getValue()).longValue();
        return objectID;
    }

    /**
     * Query debugee for indexes of the local method variables.
     */
    int[] queryVariableIndexes(long classID, long methodID) {
        // create array for expected indexes
        int indexes[] = new int[VARIABLES_COUNT];
        for (int i = 0; i < VARIABLES_COUNT; i++) {
            indexes[i] = 0;
        }

        // obtain variable indexes from debuggee
        int count = 0;
        try {
            CommandPacket command = new CommandPacket(JDWP.Command.Method.VariableTable);
            command.addReferenceTypeID(classID);
            command.addMethodID(methodID);
            command.setLength();

            ReplyPacket reply = debugee.receiveReplyFor(command);
            reply.resetPosition();

            int argCount = reply.getInt();
            long slots = reply.getInt();
            if (slots < VARIABLES_COUNT) {
                throw new Failure("Too few method local variables returned: " + slots
                                    + " (expected: at least " + VARIABLES_COUNT + ")");
            }

            for (int i = 0; i < slots; i++ ) {
                long codeindex = reply.getLong();
                String name = reply.getString();
                String signature = reply.getString();
                int length = reply.getInt();
                int slot = reply.getInt();

                for (int j = 0; j < VARIABLES_COUNT; j++) {
                    if (variables[j][0].equals(name)) {
                        indexes[j] = slot;
                        break;
                    }
                }
            }

        } catch (BoundException e) {
            log.complain("Unable to extract local variable indexes from the reply packet:\n\t"
                            + e.getMessage());
            throw new Failure("Error occured while getting local variable indexes for methodID:"
                            + methodID);
        }

        return indexes;
    }

    /**
     * Check i-th value from the reply packet.
     */
    void checkValue(int i, JDWP.Value value) {
        if (!variables[i][2].equals(value.getValue())) {
            log.complain("Unexpected value for " + i + " variable ("
                            + variables[i][0] + ") received: " + value
                            + " (expected: " + variables[i][2] + ")");
            success = false;
        }
    }

    /**
     * Perform testing JDWP command for specified frameID.
     */
    void testCommand(long frameID, long threadID, int indexes[]) {
        int slots = indexes.length;

        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  threadID: " + threadID);
        command.addObjectID(threadID);
        log.display("  frameID: " + frameID);
        command.addFrameID(frameID);
        log.display("  slots: " + slots);
        command.addInt(slots);

        // add code indexes of the requested variables
        for (int i = 0; i < slots; i++) {
            log.display("    slot #" + i + ":");
            log.display("      slot: " + indexes[i]);
            command.addInt(indexes[i]);
            byte tag = ((Byte)variables[i][3]).byteValue();
            log.display("      sigbyte: " + tag);
            command.addByte(tag);
        }
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
            log.display("Reply packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n\t" + e);
            success = false;
            return;
        }

        // check reply packet header
        try{
            log.display("Checking reply packet header");
            reply.checkHeader(command.getPacketID());
        } catch (BoundException e) {
            log.complain("Bad header of reply packet:\n\t" + e.getMessage());
            success = false;
            return;
        }

        // start parsing reply packet data
        log.display("Parsing reply packet:");
        reply.resetPosition();

        // extract and check number of values
        int values = 0;
        try {
            values = reply.getInt();
            log.display("  values: " + values);

        } catch (BoundException e) {
            log.complain("Unable to extract number of values form reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check if number of values are as expected
        if (values < 0) {
            log.complain("Negative number of values received:" + values
                        + " (expected: " + slots + ")");
            success = false;
        } else if (values != slots) {
            log.complain("Unexpected number of values received:" + values
                        + " (expected: " + slots + ")");
            success = false;
        }

        // extract and check each value
        for (int i = 0; i < values; i++ ) {
            log.display("  value #" + i + " (variable: " + variables[i][0] + ")");

            // extract value
            JDWP.Value value = null;
            try {
                value = reply.getValue();
                log.display("    slotValue: " + value);
            } catch (BoundException e) {
                log.complain("Unable to extract " + i + " slot value:\n\t"
                            + e.getMessage());
                success = false;
                break;
            }

            // extract and check value by known type tag
            checkValue(i, value);
        }

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

    }

}
