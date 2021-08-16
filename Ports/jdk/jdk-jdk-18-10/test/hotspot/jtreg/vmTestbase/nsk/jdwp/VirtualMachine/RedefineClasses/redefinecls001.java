/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.VirtualMachine.RedefineClasses;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: VirtualMachine.RedefineClasses.
 *
 * See redefinecls001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class redefinecls001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // VM capability constatnts
    static final int VM_CAPABILITY_NUMBER = JDWP.Capability.CAN_REDEFINE_CLASSES;
    static final String VM_CAPABILITY_NAME = "canRedefineClasses";

    // package and classes names
    static final String PACKAGE_NAME = "nsk.jdwp.VirtualMachine.RedefineClasses";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "redefinecls001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command
    static final String JDWP_COMMAND_NAME = "VirtualMachine.RedefineClasses";
    static final int JDWP_COMMAND_ID = JDWP.Command.VirtualMachine.RedefineClasses;

    // tested class name and signature
    static final String TESTED_CLASS_NAME = TEST_CLASS_NAME + "b";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // field and method names
    static final String CONSTRUCTOR_FIELD_NAME = "constructorInvoked";
    static final String STATIC_METHOD_FIELD_NAME = "staticMethodInvoked";
    static final String OBJECT_METHOD_FIELD_NAME = "objectMethodInvoked";
    static final String BREAKPOINT_METHOD_NAME = "runIt";
    static final int BREAKPOINT_LINE_BEFORE = redefinecls001a.BREAKPOINT_LINE_BEFORE;
    static final int BREAKPOINT_LINE_AFTER = redefinecls001a.BREAKPOINT_LINE_AFTER;

    // filename for redefined class
    static final String REDEFINED_CLASS_FILE_NAME = "bin" + File.separator + "newclass"
                    + File.separator + PACKAGE_NAME.replace('.',File.separatorChar)
                    + File.separator + "redefinecls001b.class";

    // usual scaffold objects
    ArgumentHandler argumentHandler = null;
    Log log = null;
    Binder binder = null;
    Debugee debugee = null;
    Transport transport = null;
    int waitTime = 0;  // minutes
    long timeout = 0;  // milliseconds
    String testDir = null;
    boolean dead = false;
    boolean success = true;

    // data obtained from debuggee
    long debugeeClassID = 0;
    long testedClassID = 0;
    long breakpointMethodID = 0;
    ByteBuffer redefinedClassBytes = null;

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
        return new redefinecls001().runIt(argv, out);
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

        // get testDir as first positional parameter
        String args[] = argumentHandler.getArguments();
        if (args.length < 1) {
            log.complain("Test dir required as the first positional argument");
            return FAILED;
        }
        testDir = args[0];

        // execute test and display results
        try {
            log.display("\n>>> Loading redefined class \n");

            // load class file for redefined class
            log.display("Loading bytecode of redefined class from file: " +
                        REDEFINED_CLASS_FILE_NAME);
            redefinedClassBytes = loadClassBytes(REDEFINED_CLASS_FILE_NAME, testDir);
            log.display("   ... loaded bytes: " + redefinedClassBytes.length());

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
        // wait for debuggee and tested classes loaded on debuggee startup
        log.display("Waiting for classes loaded:"
                        + "\n\t" + DEBUGEE_CLASS_NAME
                        + "\n\t" + TESTED_CLASS_NAME);
        String classNames[] = {DEBUGEE_CLASS_NAME, TESTED_CLASS_NAME};
        long classIDs[] = debugee.waitForClassesLoaded(classNames,
                                                    JDWP.SuspendPolicy.ALL);
        debugeeClassID = classIDs[0];
        log.display("  ... debuggee class loaded with classID: " + debugeeClassID);
        testedClassID = classIDs[1];
        log.display("  ... tested class loaded with classID: " + testedClassID);
        log.display("");

        // set breakpoint before redefinition and wait for debugee reached it
        log.display("Waiting for breakpoint before redefiniton reached at: "
                    + BREAKPOINT_METHOD_NAME + ":" +  BREAKPOINT_LINE_BEFORE);
        long threadID = debugee.waitForBreakpointReached(debugeeClassID,
                                                BREAKPOINT_METHOD_NAME,
                                                BREAKPOINT_LINE_BEFORE,
                                                JDWP.SuspendPolicy.ALL);
        log.display("  ... breakpoint before redefinition reached with threadID: " + threadID);
        log.display("");
    }

    /**
     * Perform testing JDWP command.
     */
    void testCommand() {
        int length = redefinedClassBytes.length();
        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  classes: " + 1);
        command.addInt(1);
        log.display("  refTypeID: " + testedClassID);
        command.addReferenceTypeID(testedClassID);
        log.display("  classfile: " + length + " bytes");
        command.addInt(length);
        log.display("  classbytes:\n" + redefinedClassBytes);
        command.addBytes(redefinedClassBytes.getBytes(), 0, length);
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
        // set breakpoint after redefinition and wait for debugee reached it
        log.display("Waiting for breakpoint after redefiniton reached at: "
                    + BREAKPOINT_METHOD_NAME + ":" +  BREAKPOINT_LINE_AFTER);
        long threadID = debugee.waitForBreakpointReached(debugeeClassID,
                                                BREAKPOINT_METHOD_NAME,
                                                BREAKPOINT_LINE_AFTER,
                                                JDWP.SuspendPolicy.ALL);
        log.display("  ... breakpoint after redefinition reached with threadID: " + threadID);
        log.display("");

        // check invoked methods
        log.display("Getting value of static field: " + CONSTRUCTOR_FIELD_NAME);
        JDWP.Value value = debugee.getStaticFieldValue(testedClassID,
                                                CONSTRUCTOR_FIELD_NAME, JDWP.Tag.INT);
        int constructorInvoked = ((Integer)value.getValue()).intValue();
        log.display("  ... got constructorInvoked:  " + methodKind(constructorInvoked));

        if (constructorInvoked != redefinecls001b.NOT_REDEFINED_METHOD_INVOKED) {
            log.complain("Constructor has been invoked after class redefinition");
            success = false;
        }

        log.display("Getting value of static field: " + STATIC_METHOD_FIELD_NAME);
        value = debugee.getStaticFieldValue(testedClassID,
                                                STATIC_METHOD_FIELD_NAME, JDWP.Tag.INT);
        int staticMethodInvoked = ((Integer)value.getValue()).intValue();
        log.display("  ... got staticMethodInvoked: " + methodKind(staticMethodInvoked));

        if (staticMethodInvoked != redefinecls001b.REDEFINED_METHOD_INVOKED) {
            log.complain("Not redefined static method is invoked after class redefinition");
            success = false;
        }

        log.display("Getting value of static field: " + OBJECT_METHOD_FIELD_NAME);
        value = debugee.getStaticFieldValue(testedClassID,
                                                OBJECT_METHOD_FIELD_NAME, JDWP.Tag.INT);
        int objectMethodInvoked = ((Integer)value.getValue()).intValue();
        log.display("  ... got objectMethodInvoked: " + methodKind(objectMethodInvoked));

        if (objectMethodInvoked != redefinecls001b.REDEFINED_METHOD_INVOKED) {
            log.complain("Not redefined object method is invoked after class redefinition");
            success = false;
        }
    }

    /**
     * Load class bytes form the given file.
     */
    ByteBuffer loadClassBytes(String fileName, String dirName) {
        String fileSep = System.getProperty("file.separator");
        String filePath = dirName + fileSep + fileName;

        String error = "Unable to read bytes from class file:\n\t" + filePath;

        int length = 0;
        byte bytes[] = null;
        try {
            File file = new File(filePath);
            length = (int)file.length();
            FileInputStream is = new FileInputStream(file);
            bytes = new byte[length];
            int number = is.read(bytes);
            if (number < 0) {
                log.complain("EOF reached while reading bytes from file");
                throw new Failure(error);
            } else if (number != length) {
                log.complain("Unexpected number of bytes red from file: " + number
                            + " (expected: " + length + ")");
                throw new Failure(error);
            }
            is.close();
        } catch ( IOException e ) {
            log.complain("Caught IOException while reading bytes from file:\n\t" + e);
            throw new Failure(error);
        }
        ByteBuffer byteBuffer = new ByteBuffer(length, 0);
        byteBuffer.addBytes(bytes, 0, length);
        return byteBuffer;
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

    // return string representation of kind of invoked method
    private static String methodKind(int kind) {
        switch (kind) {
            case redefinecls001b.METHOD_NOT_INVOKED:
                return "METHOD_NOT_INVOKED";
            case redefinecls001b.REDEFINED_METHOD_INVOKED:
                return "REDEFINED_METHOD_INVOKED";
            case redefinecls001b.NOT_REDEFINED_METHOD_INVOKED:
                return "NOT_REDEFINED_METHOD_INVOKED";
            default:
                return "UNKNOWN_METHOD_KIND";
        }
    }
}
