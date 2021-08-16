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

package nsk.jdwp.Method.IsObsolete;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: Method.IsObsolete.
 *
 * See isobsolete002.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class isobsolete002 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // VM capability constatnts
    static final int VM_CAPABILITY_NUMBER = JDWP.Capability.CAN_REDEFINE_CLASSES;
    static final String VM_CAPABILITY_NAME = "canRedefineClasses";

    // package and classes names
    static final String PACKAGE_NAME = "nsk.jdwp.Method.IsObsolete";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "isobsolete002";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command
    static final String JDWP_COMMAND_NAME = "Method.IsObsolete";
    static final int JDWP_COMMAND_ID = JDWP.Command.Method.IsObsolete;

    // tested class name and signature
    static final String TESTED_CLASS_NAME = TEST_CLASS_NAME + "b";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // tested method name
    static final String TESTED_METHOD_NAME = "testedMethod";
    static final int BREAKPOINT_LINE = isobsolete002a.BREAKPOINT_LINE;

    // filename for redefined class
    static final String REDEFINED_CLASS_FILE_NAME = "bin" + File.separator + "newclass"
                    + File.separator + PACKAGE_NAME.replace('.', File.separatorChar)
                    + File.separator + "isobsolete002b.class";

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
    long testedClassID = 0;
    long testedMethodID = 0;

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
        return new isobsolete002().runIt(argv, out);
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
            log.display("\n>>> Testing JDWP command \n");
            testCommand(testedMethodID, TESTED_METHOD_NAME);

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
                        + "\n\t" + TESTED_CLASS_NAME);
        testedClassID = debugee.waitForClassLoaded(TESTED_CLASS_NAME,
                                                    JDWP.SuspendPolicy.ALL);
        log.display("  ... class loaded with classID: " + testedClassID);
        log.display("");

/*
        // get tested methodID by names
        log.display("Getting methodID for method name :" + TESTED_METHOD_NAME);
        testedMethodID = debugee.getMethodID(testedClassID, TESTED_METHOD_NAME, true);
        log.display("  ... got methodID: " + testedMethodID);
        log.display("");
*/

        // wait for breakpoint reached
        log.display("Waiting for breakpoint reached at: "
                        + TESTED_METHOD_NAME + ":" + BREAKPOINT_LINE);
        long threadID = debugee.waitForBreakpointReached(testedClassID,
                                                        TESTED_METHOD_NAME,
                                                        BREAKPOINT_LINE,
                                                        JDWP.SuspendPolicy.ALL);
        log.display("  ... breakpoint reached with threadID: " + threadID);
        log.display("");

        // load class file for redefined class
        log.display("Loading bytecode of redefined class from file: " +
                    REDEFINED_CLASS_FILE_NAME);
        byte[] classBytes = loadClassBytes(REDEFINED_CLASS_FILE_NAME, testDir);
        log.display("   ... loaded bytes: " + classBytes.length);

        // redefine class
        log.display("Redefine class by classID: " + testedClassID);
        redefineClass(testedClassID, classBytes);
        log.display("  ... class redefined");
        log.display("");

        // get top frameID of the thread
        log.display("Getting top frameID of the threadID: " + threadID);
        JDWP.Location location = queryTopFrameLocation(threadID);
        log.display("  ... got location: " + location);

        // get methodID of the top frameID
        log.display("Getting methodID for the location :" + location);
        testedMethodID = location.getMethodID();
        log.display("  ... got methodID: " + testedMethodID);
        log.display("");

    }

    /**
     * Perform testing JDWP command for given methodID.
     */
    void testCommand(long testedMethodID, String methodName) {
        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("    refTypeID: " + testedClassID);
        command.addReferenceTypeID(testedClassID);
        log.display("    methodID: " + testedMethodID);
        command.addMethodID(testedMethodID);
        command.setLength();

        // send command packet to debugee
        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
        } catch (IOException e) {
            log.complain("Unable to send command packet for method " + methodName + ":\n\t" + e);
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
            log.complain("Unable to read reply packet for method " + methodName + ":\n\t" + e);
            success = false;
            return;
        }

        // check reply packet header
        try{
            log.display("Checking header of reply packet");
            reply.checkHeader(command.getPacketID());
            log.display("  ... packet header is correct");
        } catch (BoundException e) {
            log.complain("Wrong header of reply packet for method " + methodName + ":\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // start parsing reply packet data
        log.display("Parsing reply packet data:");
        reply.resetPosition();

        // extract boolean isObsolete
        byte isObsolete = 0;
        try {
            isObsolete = reply.getByte();
            log.display("    isObsolete: " + isObsolete);
        } catch (BoundException e) {
            log.complain("Unable to extract isObsolete value from reply packet for method "
                        + methodName + ":\n\t" + e.getMessage());
            success = false;
        }

        // check isObsolete
        if (isObsolete == 0) {
            log.complain("Unexpected isObsolete value for method "
                        + methodName + ": " + isObsolete + " (expected: not " + 0 + ")");
            success = false;
        }

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes in reply packet for "
                        + methodName + " method at: " + reply.offsetString());
            success = false;
        }

        log.display("  ... packed data parsed");
    }

    /**
     * Redefine class bytes for given classID.
     */
    void redefineClass(long classID, byte[] classBytes) {
        int length = classBytes.length;

        CommandPacket command = new CommandPacket(JDWP.Command.VirtualMachine.RedefineClasses);
        command.addInt(1);
        command.addReferenceTypeID(classID);
        command.addInt(length);
        command.addBytes(classBytes, 0, length);

        // receive reply packet from debugee
        ReplyPacket reply = debugee.receiveReplyFor(command, "VirtualMachine.RedefineClasses");
    }

    /**
     * Query debuggee VM for top frameID of the thread.
     */
    JDWP.Location queryTopFrameLocation(long threadID) {
        String error = "Error occured while getting top frameID for threadID: " + threadID;

        CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Frames);
        command.addObjectID(threadID);
        command.addInt(0);
        command.addInt(1);
        command.setLength();

        ReplyPacket reply = debugee.receiveReplyFor(command, "ThreadReference.Frames");
        reply.resetPosition();

        // extract number of frames
        int frames = 0;
        try {
            frames = reply.getInt();
        } catch (BoundException e) {
            log.complain("Unable to extract number of frames from reply packet:\n\t"
                        + e.getMessage());
            throw new Failure(error);
        }

        // check frames count
        if (frames != 1) {
            log.complain("Unexpected number of frames returned: "
                        + frames + " (expected: " + 1 + ")");
            throw new Failure(error);
        }

        // extract frame ID
        long frameID = 0;
        try {
            frameID = reply.getFrameID();
        } catch (BoundException e) {
            log.complain("Unable to extract top frameID from reply packet:\n\t"
                    + e.getMessage());
            throw new Failure(error);
        }

        // extract frame location
        JDWP.Location location = null;
        try {
            location = reply.getLocation();
        } catch (BoundException e) {
            log.complain("Unable to extract location for top frame from reply packet:\n\t"
                    + e.getMessage());
            throw new Failure(error);
        }

        return location;
    }


    /**
     * Load class bytes form the given file.
     */
    byte[] loadClassBytes(String fileName, String dirName) {
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
        return bytes;
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
