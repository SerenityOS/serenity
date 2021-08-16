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

package nsk.jdwp.ClassLoaderReference.VisibleClasses;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ClassLoaderReference.VisibleClasses.
 *
 * See visibclasses001.README for description of test execution.
 *
 * Test is executed by invoking method runIt().
 * JDWP command is tested in method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class visibclasses001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // communication signals constants
    static final String READY = "ready";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.ClassLoaderReference.VisibleClasses";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "visibclasses001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "ClassLoaderReference.VisibleClasses";
    static final int JDWP_COMMAND_ID = JDWP.Command.ClassLoaderReference.VisibleClasses;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

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
        return new visibclasses001().runIt(argv, out);
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

                // query debugee for TypeIDs of classes been nested
                log.display("Getting classLoaderID for tested classes");
                long classLoaderID = queryClassLoaderID(classID);
                log.display("  got classLoaderID: " + classLoaderID);

                // perform testing JDWP command
                log.display("\n>>> Testing JDWP command \n");
                testCommand(classLoaderID, classID);

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
     * Query debugee for classLoaderID for specified classID.
     */
    long queryClassLoaderID(long classID) {
        CommandPacket command =
                        new CommandPacket(JDWP.Command.ReferenceType.ClassLoader);
        command.addReferenceTypeID(classID);
        ReplyPacket reply = debugee.receiveReplyFor(command);

        try {
            reply.resetPosition();

            long classLoaderID = reply.getObjectID();
            return classLoaderID;
        } catch (BoundException e) {
            throw new Failure("Unable to parse reply packet for ReferenceType.ClassLoader:\n\t"
                            + e);
        }
    }

    /**
     * Perform testing JDWP command for specified classLoaderID.
     */
    void testCommand(long classLoaderID, long expectedClassID) {
        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        log.display("  classLoaderID: " + classLoaderID);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        command.addObjectID(classLoaderID);
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

        // extract and check number of nested classes
        int classes = 0;
        try {
            classes = reply.getInt();
            log.display("  classes: " + classes);

        } catch (BoundException e) {
            log.complain("Unable to extract number of nested classes from reply packet:\n\t"
                        + e.getMessage());
            success = false;
        }

        if (classes < 0) {
            log.complain("Negative number of classes in the reply packet:" + classes);
            success = false;
        } else if (classes == 0) {
            log.complain("Zero number of classes in the reply packet:" + classes);
            success = false;
        }

        boolean found = false;
        // extract and check TypeID for each received class
        for (int i = 0; i < classes; i++ ) {
                log.display("  class #" + i);

            // extract TypeTag byte
            byte refTypeTag = (byte)0;
            String refTypeTagName = null;
            try {
                refTypeTag = reply.getByte();
                String tag;
                switch (refTypeTag) {
                    case JDWP.TypeTag.CLASS:
                        refTypeTagName = "CLASS";
                        break;
                    case JDWP.TypeTag.INTERFACE:
                        refTypeTagName = "INTERFACE";
                        break;
                    case JDWP.TypeTag.ARRAY:
                        refTypeTagName = "ARRAY";
                        break;
                    default:
                        refTypeTagName = "UNKNOWN";
                        break;
                }
                log.display("    refTypeTag: " + refTypeTag + "=" + refTypeTagName);
            } catch (BoundException e) {
                log.complain("Unable to extract refTypetag of " + i
                            + " class from reply packet:\n\t"
                            + e.getMessage());
                success = false;
                break;
            }

            // extract and check TypeID
            long typeID = 0;
            try {
                typeID = reply.getReferenceTypeID();
                log.display("    typeID: " + typeID);

            } catch (BoundException e) {
                log.complain("Unable to extract TypeID of " + i
                                + " nested class from reply packet:\n\t"
                                + e.getMessage());
                success = false;
                break;
            }

            if (typeID == expectedClassID) {
                log.display("Found expected classID: " + expectedClassID);
                found = true;
                if (refTypeTag != JDWP.TypeTag.CLASS) {
                    log.complain("unexpected refTypeTag returned for checked class: "
                                + refTypeTag + "=" + refTypeTagName
                                + " (expected: " + JDWP.TypeTag.CLASS + "=CLASS)");
                    success = false;
                }
            }
        }

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: " + reply.offsetString());
            success = false;
        }

        if (!found) {
            log.complain("Expected classID not found in the list of visible classes: " + expectedClassID);
            success = false;
        }
    }

}
