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

package nsk.jdwp.ThreadReference.Resume;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ThreadReference.Resume.
 *
 * See resume001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class resume001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // communication signals constants
    static final String READY = "ready";
    static final String ERROR = "error";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.ThreadReference.Resume";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "resume001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "ThreadReference.Resume";
    static final int JDWP_COMMAND_ID = JDWP.Command.ThreadReference.Resume;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // name of the tested thread and statioc field with thread value
    static final String TESTED_CLASS_FIELD_NAME = resume001a.FIELD_NAME;
    static final String TESTED_THREAD_NAME = resume001a.THREAD_NAME;

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
        return new resume001().runIt(argv, out);
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

            long threadID = 0;

            // work with prepared debuggee
            try {
                log.display("\n>>> Obtaining requred data from debugee \n");

                // query debuggee for classID of tested class
                log.display("Getting classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                // query debuggee for threadID value from a static field
                log.display("Getting threadID value from static field: "
                            + TESTED_CLASS_FIELD_NAME);
                threadID = queryThreadID(classID, TESTED_CLASS_FIELD_NAME);
                log.display("  got threadID: " + threadID);

                // request debuggee to suspend tested thread
                log.display("Suspendig thread into debuggee for threadID: " + threadID);
                debugee.suspendThread(threadID);

                // perform testing JDWP command
                log.display("\n>>> Testing JDWP command \n");
                testCommand(threadID);

            } finally {

                log.display("\n>>> Finishing test \n");

                // resume suspended thread
                if (threadID != 0) {
                    log.display("Resuming potentially suspended thread");
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
     * Query debuggee for suspend status of the thread.
     */
    int querySuspendStatus(long threadID) {
        log.display("Getting suspend status for threadID: " + threadID);
        CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Status);
        command.addObjectID(threadID);
        ReplyPacket reply = debugee.receiveReplyFor(command);

        try {
            reply.resetPosition();

            int threadStatus = reply.getInt();
            int suspendStatus = reply.getInt();
            log.display("  got suspendStatus: " + suspendStatusString(suspendStatus));
            return suspendStatus;
        } catch (BoundException e) {
            throw new Failure("Caught BoundException while parsing reply for ThreadReference.Status:\n\t"
                        + e);
        }
    }

    /**
     * Perform testing JDWP command for specified threadID.
     */
    void testCommand(long threadID) {
        // check that tested thread is not suspended before command sent
        int suspendStatus = querySuspendStatus(threadID);
        if (suspendStatus != JDWP.SuspendStatus.SUSPEND_STATUS_SUSPENDED) {
            throw new Failure("SuspendStatus reports thread is not suspended before sending Resume command: "
                            + suspendStatusString(suspendStatus));
        } else {
            log.display("Thread is suspended");
        }

        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  threadID: " + threadID);
        command.addObjectID(threadID);
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

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        // check that tested thread is suspended after Resume command sent
        suspendStatus = querySuspendStatus(threadID);
        if ((suspendStatus & JDWP.SuspendStatus.SUSPEND_STATUS_SUSPENDED) != 0) {
            log.complain("SuspendStatus reports thread is suspended after Resume command sent: "
                        + suspendStatusString(suspendStatus));
        } else {
            log.display("Thread is not suspended");
        }

    }

    /**
     * Return string representation of thread suspend status.
     */
    private static String suspendStatusString(int status) {
        String s = null;
        if ((status & JDWP.SuspendStatus.SUSPEND_STATUS_SUSPENDED) != 0) {
            s = "SUSPEND_STATUS_SUSPENDED";
        } else  if (status == 0) {
            s = "NONE";
        } else {
            s = "unknown";
        }
        return status + "=" + s;
    }
}
