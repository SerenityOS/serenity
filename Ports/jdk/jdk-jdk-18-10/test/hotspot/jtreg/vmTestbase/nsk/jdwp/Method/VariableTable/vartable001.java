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

package nsk.jdwp.Method.VariableTable;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: Method.VariableTable.
 *
 * See vartable001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class vartable001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // communication signals constants
    static final String READY = "ready";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.Method.VariableTable";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "vartable001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "Method.VariableTable";
    static final int JDWP_COMMAND_ID = JDWP.Command.Method.VariableTable;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // tested types signature conatants
    static final String OBJECT_CLASS_SIGNATURE = "Ljava/lang/Object;";
    static final String STRING_CLASS_SIGNATURE = "Ljava/lang/String;";


    // tested method name constant
    static final String TESTED_METHOD_NAME = "testedMethod";

    // list of tested variables names and signatures
    static final String variablesList[][] = {
                // synthetic method arguments
                {"this",            TESTED_CLASS_SIGNATURE},
                // method arguments
                {"booleanArgument", "Z"},
                {"byteArgument",    "B"},
                {"charArgument",    "C"},
                {"shortArgument",   "S"},
                {"intArgument",     "I"},
                {"longArgument",    "J"},
                {"floatArgument",   "F"},
                {"doubleArgument",  "D"},
                {"objectArgument",  OBJECT_CLASS_SIGNATURE},
                {"stringArgument",  STRING_CLASS_SIGNATURE},
                // local variables
                {"booleanLocal",    "Z"},
                {"byteLocal",       "B"},
                {"charLocal",       "C"},
                {"shortLocal",      "S"},
                {"intLocal",        "I"},
                {"longLocal",       "J"},
                {"floatLocal",      "F"},
                {"doubleLocal",     "D"},
                {"objectLocal",     OBJECT_CLASS_SIGNATURE},
                {"stringLocal",     STRING_CLASS_SIGNATURE}
    };
    static final int variablesCount = variablesList.length;

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
        return new vartable001().runIt(argv, out);
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
            try {
                log.display("\n>>> Obtaining requred data from debugee \n");

                // query debuggee for classID of tested class
                log.display("Getting classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                // query debuggee for methodID of tested method (declared in the class)
                log.display("Getting methodID by name: " + TESTED_METHOD_NAME);
                long methodID = debugee.getMethodID(classID, TESTED_METHOD_NAME, true);
                log.display("  got methodID: " + methodID);

                // perform testing JDWP command
                log.display("\n>>> Testing JDWP command \n");
                testCommand(classID, methodID);

            } finally {
                // quit debugee
                log.display("\n>>> Finishing test \n");
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
        if (! signal.equals(READY)) {
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
     * Perform testing JDWP command for specified TypeID.
     */
    void testCommand(long classID, long methodID) {
        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  referenceTypeID: " + classID);
        command.addReferenceTypeID(classID);
        log.display("  methodID: " + methodID);
        command.addMethodID(methodID);
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
        }

        // start parsing reply packet data
        log.display("Parsing reply packet:");
        reply.resetPosition();

        // clear list of found variables
        int[] foundVariablesList = new int[variablesCount];
        for (int i = 0; i < variablesCount; i++) {
            foundVariablesList[i] = 0;
        }

        // extract and check reply data

        // extract number of argumnets
        int argCount = 0;
        try {
            argCount = reply.getInt();
            log.display("  argCount: " + argCount);
        } catch (BoundException e) {
            log.complain("Unable to extract number of arguments from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check that number of arguments is not negative
        if (argCount < 0) {
            log.complain("Negative of arguments in reply packet: " + argCount);
            success = false;
        }

        // extract number of slots
        int slots = 0;
        try {
            slots = reply.getInt();
            log.display("  slots: " + slots);
        } catch (BoundException e) {
            log.complain("Unable to extract number of slots from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check that number of slots is not negative
        if (slots < 0) {
            log.complain("Negative value of end code index in reply packet: " + slots);
            success = false;
        }

        // check that start code is not less than expected
        if (slots < variablesCount) {
            log.complain("Number of slots (" + slots
                        + ") is less than expected (" + variablesCount + ")");
            success = false;
        }

        // extract and check each slot attributes
        for (int i = 0; i < slots; i++) {
            log.display("  slot #" + i + ":");

            // extract code index of a slot
            long codeIndex = 0;
            try {
                codeIndex = reply.getLong();
                log.display("    codeIndex: " + codeIndex);
            } catch (BoundException e) {
                log.complain("Unable to extract code index of slot #" + i
                            + " from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check that code index is not negative
            if (codeIndex < 0) {
                log.complain("Negative code index of slot #" + i + ":" + codeIndex);
                success = false;
            }

            // extract name of a slot
            String name = null;
            try {
                name = reply.getString();
                log.display("    name: " + name);
            } catch (BoundException e) {
                log.complain("Unable to extract name of slot #" + i
                            + " from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // extract signature of a slot
            String signature = null;
            try {
                signature = reply.getString();
                log.display("    signature: " + signature);
            } catch (BoundException e) {
                log.complain("Unable to extract signature of slot #" + i
                            + " from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // extract code length
            int length = 0;
            try {
                length = reply.getInt();
                log.display("    length: " + length);
            } catch (BoundException e) {
                log.complain("Unable to extract code length for slot #" + i
                            + " from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // extract code length
            int slot = 0;
            try {
                slot = reply.getInt();
                log.display("    slot: " + length);
            } catch (BoundException e) {
                log.complain("Unable to extract slot index of slot #" + i
                            + " from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // find slot name into list of expected variables
            int found = -1;
            for (int j = 0; j < variablesCount; j++) {
                if (variablesList[j][0].equals(name)) {
                    found = j;
                    break;
                }
            }

            // check if slot is found and not duplicated
            if (found >= 0) {
                if (foundVariablesList[found] > 0) {
                    log.complain("Slot #" + i + " is duplicated "
                                + foundVariablesList[found] + " times: "
                                + name);
                    success = false;
/*
                } else {
                    log.display("Found expected variable #" + found + ": "
                                + variablesList[found][0]);
*/
                }
                foundVariablesList[found]++;

                // check slot signature
                if (!variablesList[found][1].equals(signature)) {
                    log.complain("Unexpected signature for slot #" + i + ": " + signature
                                + " (expected: " + variablesList[found][1] + ")");
                    success = false;
                }
            } else {
                log.display("Unexpected slot #" + i + " (may be synthetic): " + name);
            }

            // check that code length is not negative
            if (length < 0) {
                log.complain("Code length for slot #" + i + " is negative: " + length);
                success = false;
            }

            // check that slot index is not negative
            if (slot < 0) {
                log.complain("Index of slot #" + i + " is negative: " + slot);
                success = false;
            }
        }

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        // check that all expected variables found
        for (int i = 0; i < variablesCount; i++) {
            if (foundVariablesList[i] <= 0) {
                log.complain("No slot found in reply packet for variable: "
                            + variablesList[i][0]);
                success = false;
            }
        }

    }

}
