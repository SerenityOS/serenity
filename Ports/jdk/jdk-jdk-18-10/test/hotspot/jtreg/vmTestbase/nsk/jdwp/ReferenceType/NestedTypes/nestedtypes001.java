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

package nsk.jdwp.ReferenceType.NestedTypes;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ReferenceType.NestedTypes.
 *
 * See nestedtypes001.README for description of test execution.
 *
 * Test is executed by invoking method runIt().
 * JDWP command is tested in method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class nestedtypes001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // communication signals constants
    static final String READY = "ready";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.ReferenceType.NestedTypes";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "nestedtypes001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "ReferenceType.NestedTypes";
    static final int JDWP_COMMAND_ID = JDWP.Command.ReferenceType.NestedTypes;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // nested classes names and signatures array
    static final String nested_classes [][] = {
                    { TESTED_CLASS_NAME + "$" + "NestedInterface", "" },
                    { TESTED_CLASS_NAME + "$" + "StaticNestedClass", "" },
                    { TESTED_CLASS_NAME + "$" + "InnerNestedClass", "" }
                };
    static final int NESTED_CLASSES = nested_classes.length;

    // nested classes IDs array
    static final long nested_classesIDs[] = new long[NESTED_CLASSES];

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
        return new nestedtypes001().runIt(argv, out);
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

                // query debugee for TypeIDs of classes been nested
                log.display("Getting ReferenceTypeIDs for nested classes");
                queryNestedClassesTypeIDs();

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
     * Query debugee for TypeIDs for nested classes from nested_classes array
     * and put them into nested_classesIDs array.
     */
    void queryNestedClassesTypeIDs() {
        for (int i = 0; i < NESTED_CLASSES; i++) {
            nested_classes[i][1] = "L" + nested_classes[i][0].replace('.', '/') + ";";
            log.display("Getting ReferenceTypeID by signature:\n"
                        + "  " + nested_classes[i][1]);
            nested_classesIDs[i] = debugee.getReferenceTypeID(nested_classes[i][1]);
            log.display("  got TypeID: " + nested_classesIDs[i]);
        }
    }

    /**
     * Perform testing JDWP command for specified TypeID.
     */
    void testCommand(long typeID) {

        // count nested classes found in the reply packet
        int found_classes[] = new int[NESTED_CLASSES];
        for (int i = 0; i < NESTED_CLASSES; i++) {
            found_classes[i] = 0;
        }

        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        log.display("  ReferenceTypeID: " + typeID);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        command.addReferenceTypeID(typeID);
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

        // extract and check number of nested classes
        int classes = 0;
        try {
            classes = reply.getInt();
            log.display("  classes: " + classes);

            if (classes != NESTED_CLASSES) {
                log.complain("Unexpected number of nested classes in the reply packet:"
                            + classes + " (expected: " + NESTED_CLASSES + ")");
                success = false;
            }
        } catch (BoundException e) {
            log.complain("Unable to extract number of nested classes from reply packet:\n\t"
                        + e.getMessage());
            success = false;
        }

        // extract and check TypeID for each nested class
        for (int i = 0; i < classes; i++ ) {
            log.display("  nested class #" + i);

            // extract TypeTag byte
            try {
                byte refTypeTag = reply.getByte();
                log.display("    refTypeTag: " + refTypeTag);
            } catch (BoundException e) {
                log.complain("Unable to extract refTypetag of " + i + " nested class from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                break;
            }

            // extract and check TypeID
            long referenceTypeID;
            try {
                referenceTypeID = reply.getReferenceTypeID();
                log.display("    referenceTypeID: " + referenceTypeID);

            } catch (BoundException e) {
                log.complain("Unable to extract TypeID of " + i
                                + " nested class from reply packet:\n\t"
                                + e.getMessage());
                success = false;
                break;
            }

            if (referenceTypeID == 0) {
                log.complain("Unexpected null ReferenceTypeID for nested class #" + i
                            + " in the reply packet: " + referenceTypeID);
                success = false;
            } else {
                boolean found = false;;
                for (int j = 0; j < NESTED_CLASSES; j++) {
                    if (referenceTypeID == nested_classesIDs[j]) {
                        log.display("! Found expected nested class #" + j + ": "
                                    + nested_classes[j][0]);
                        found = true;
                        found_classes[j]++;
                        break;
                    }
                }
                if (!found) {
                    log.complain("Unexpected TypeID of nested class found in reply packet: "
                                + referenceTypeID);
                    success = false;
                }
            }
        }

        // check for extra data in reply packet
        if (! reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: " + reply.offsetString());
            success = false;
        }

        // check if all expected nested classes are found
        for (int j = 0; j < NESTED_CLASSES; j++) {
            if (found_classes[j] <= 0) {
                log.complain("No TypeID for expected nested class found in reply packet: "
                            + nested_classesIDs[j] + " (" + nested_classes[j][0] + ")");
                success = false;
            } else if (found_classes[j] > 1) {
                log.complain("Duplicated " + found_classes[j] + " TypeIDs for expected nested class found in reply packet: "
                            + nested_classesIDs[j] + " (" + nested_classes[j][0] + ")");
                success = false;
            }
        }
    }

}
