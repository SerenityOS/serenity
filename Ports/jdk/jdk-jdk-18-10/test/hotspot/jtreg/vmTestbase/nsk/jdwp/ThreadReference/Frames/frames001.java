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

package nsk.jdwp.ThreadReference.Frames;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ThreadReference.Frames.
 *
 * See frames001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class frames001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // communication signals constants
    static final String READY = "ready";
    static final String ERROR = "error";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.ThreadReference.Frames";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "frames001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "ThreadReference.Frames";
    static final int JDWP_COMMAND_ID = JDWP.Command.ThreadReference.Frames;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // name of the tested thread and statioc field with thread value
    static final String TESTED_CLASS_FIELD_NAME = frames001a.FIELD_NAME;
    static final String TESTED_THREAD_NAME = frames001a.THREAD_NAME;

    // names of the methods with frames
    static final String TESTED_METHOD_NAME = frames001a.METHOD_NAME;
    static final String RUN_METHOD_NAME = "run";

    // expected number of frames count
    static final int START_FRAME_INDEX = 2;
    static final int FRAMES_COUNT = frames001a.FRAMES_COUNT - START_FRAME_INDEX;

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
        return new frames001().runIt(argv, out);
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
                log.display("Getting classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                // query debuggee for methodID of the recursive method
                log.display("Getting methodID by name: " + TESTED_METHOD_NAME);
                long methodID = debugee.getMethodID(classID, TESTED_METHOD_NAME, true);
                log.display("  got methodID: " + methodID);

                // query debuggee for methodID of the run() method
                log.display("Getting methodID by name: " + RUN_METHOD_NAME);
                long runMethodID = debugee.getMethodID(classID, RUN_METHOD_NAME, true);
                log.display("  got methodID: " + runMethodID);

                // query debuggee for threadID value from a static field
                log.display("Getting threadID value from static field: "
                            + TESTED_CLASS_FIELD_NAME);
                threadID = queryThreadID(classID, TESTED_CLASS_FIELD_NAME);
                log.display("  got threadID: " + threadID);

                // suspend tested thread into debyggee
                log.display("Suspending thread into debuggee for threadID: " + threadID);
                debugee.suspendThread(threadID);
                log.display("  thread suspended");

                // perform testing JDWP command
                log.display("\n>>> Testing JDWP command \n");
                testCommand(threadID, methodID, runMethodID, classID);

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
     * Query debuggee for threadID value of statuic field of the class.
     */
    long queryThreadID(long classID, String fieldName) {
        // get fieledID for static field (declared in the class)
        long fieldID = debugee.getClassFieldID(classID, fieldName, true);
        // get value of the field
        JDWP.Value value = debugee.getStaticFieldValue(classID, fieldID);

        // check that value has THREAD tag
        if (value.getTag() != JDWP.Tag.THREAD) {
            throw new Failure("Not threadID value returned from debuggee: " + value);
        }

        // extract threadID from the value
        long threadID = ((Long)value.getValue()).longValue();
        return threadID;
    }

    /**
     * Perform testing JDWP command for specified threadID.
     */
    void testCommand(long threadID, long methodID, long runMethodID, long classID) {
        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  threadID: " + threadID);
        command.addObjectID(threadID);
        log.display("  startFrame: " + START_FRAME_INDEX);
        command.addInt(START_FRAME_INDEX);
        log.display("  length: " + FRAMES_COUNT);
        command.addInt(FRAMES_COUNT);
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

        // extract number of frames
        int frames = 0;
        try {
            frames = reply.getInt();
            log.display("  frames: " + frames);
        } catch (BoundException e) {
            log.complain("Unable to extract number of frames from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check that frames count is not negative
        if (frames < 0) {
            log.complain("Negative value of frames count in reply packet: "
                        + frames);
            success = false;
        }

        // check that thread has an expected state
        if (frames != FRAMES_COUNT) {
            log.complain("Unexpected number of frames returned: "
                        + frames + " (expected: " + FRAMES_COUNT + ")");
            success = false;
        }

        // use methodID of the recursive method to check all frames except the last one
        long checkedMethodID = methodID;

        // extarct frame IDs and locations
        for (int i = 0; i < frames; i++) {

            log.display("  frame #" + i + ":");

            // extract frame ID
            long frameID = 0;
            try {
                frameID = reply.getFrameID();
                log.display("    frameID: " + frameID);
            } catch (BoundException e) {
                log.complain("Unable to extract " + i + " frameID from reply packet:\n\t"
                        + e.getMessage());
                success = false;
                return;
            }

            // extract frame location
            JDWP.Location location = null;
            try {
                location = reply.getLocation();
                log.display("    location: " + location);
            } catch (BoundException e) {
                e.printStackTrace(log.getOutStream());
                log.complain("Unable to extract " + i + " frame location from reply packet:\n\t"
                        + e.getMessage());
                success = false;
                return;
            }

            // check that frameID is not negative integer
            if (frameID < 0) {
                log.complain("Negative value of " + i + " frameID: "
                            + frameID);
                success = false;
            }

            // check that type tag of location is CLASS tag
            if (location.getTag() != JDWP.TypeTag.CLASS) {
                log.complain("Unexpected type tag of " + i + " frame location: "
                            + location.getTag() + "(expected: " + JDWP.TypeTag.CLASS + ")");
                success = false;
            }

            // check that classID of location is equal to original classID
            if (location.getClassID() != classID) {
                log.complain("Unexpected classID of " + i + " frame location: "
                            + location.getClassID() + "(expected: " + classID + ")");
                success = false;
            }

            // use methodID of run() method for checking last frame
            if (i == frames - 1) {
                checkedMethodID = runMethodID;
            }

            // check that methodID of location is equal to one of original methodIDs
            if (location.getMethodID() != checkedMethodID) {
                log.complain("Unexpected methodID of " + i + " frame location: "
                            + location.getMethodID() + "(expected: " + checkedMethodID + ")");
                success = false;
            }

            // check that code index of location is not negative integer
            if (location.getIndex() < 0) {
                log.complain("Negative value of index of " + i + " frame location: "
                            + location.getIndex());
                success = false;
            }

        }

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

    }

}
