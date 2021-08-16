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

package nsk.jdwp.ObjectReference.MonitorInfo;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ObjectReference.MonitorInfo.
 *
 * See monitorinfo001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class monitorinfo001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // communication signals constants
    static final String READY = "ready";
    static final String ERROR = "error";
    static final String QUIT = "quit";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.ObjectReference.MonitorInfo";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "monitorinfo001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // VM capability constatnts
    static final int VM_CAPABILITY_NUMBER = JDWP.Capability.CAN_GET_MONITOR_INFO;
    static final String VM_CAPABILITY_NAME = "canGetMonitorInfo";

    // tested JDWP command constants
    static final String JDWP_COMMAND_NAME = "ObjectReference.MonitorInfo";
    static final int JDWP_COMMAND_ID = JDWP.Command.ObjectReference.MonitorInfo;

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME =
                            DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE =
                            "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    // name of the tested thread and statioc field with thread value
    static final String OBJECT_FIELD_NAME =
                            monitorinfo001a.OBJECT_FIELD_NAME;
    static final String MONITOR_OWNER_FIELD_NAME =
                            monitorinfo001a.MONITOR_OWNER_FIELD_NAME;
    static final String MONITOR_WAITER_FIELD_NAMES[] =
                            monitorinfo001a.MONITOR_WAITER_FIELD_NAMES;

    // threadIDs of threads owning or waiting for monitor of the tested object
    long ownerThreadID = 0;
    long waiterThreadIDs[] = null;

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
        return new monitorinfo001().runIt(argv, out);
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
                log.display("\n>>> Checking VM capability \n");

                // check for VM capability
                log.display("Checking VM capability: " + VM_CAPABILITY_NAME);
                if (!debugee.getCapability(VM_CAPABILITY_NUMBER, VM_CAPABILITY_NAME)) {
                    out.println("TEST PASSED: unsupported VM capability: "
                                + VM_CAPABILITY_NAME);
                    return PASSED;
                }

                log.display("\n>>> Obtaining requred data from debugee \n");

                // query debuggee for classID of tested class
                log.display("Getting classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                // query debuggee for objectID value static field
                log.display("Getting objectID value from static field: "
                            + OBJECT_FIELD_NAME);
                long objectID = queryObjectID(classID,
                            OBJECT_FIELD_NAME, JDWP.Tag.OBJECT);
                log.display("  got objectID: " + objectID);

                // query debuggee for threadIDs from static fields
                queryThreadIDs(classID);

                log.display("\n>>> Testing JDWP command \n");

                // suspend all threads into debuggee
                log.display("Suspending all threads into debuggee");
                debugee.suspend();
                log.display("  debuggee suspended");

                // perform testing JDWP command
                testCommand(objectID);

            } finally {
                log.display("\n>>> Finishing test \n");

                // resume all threads into debuggee
                log.display("resuming all threads into debuggee");
                debugee.resume();
                log.display("  debuggee resumed");

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
            throw new TestBug("Debugee was not able to start tested threads"
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
     * Query debuggee for threadID values of static class fields.
     */
    void queryThreadIDs(long classID) {

        // get threadID for thread which ownes monitor of the tested object
        ownerThreadID = queryObjectID(classID, MONITOR_OWNER_FIELD_NAME, JDWP.Tag.THREAD);

        // get threadIDs for threads which wait for monitor of the tested object
        int count = MONITOR_WAITER_FIELD_NAMES.length;
        waiterThreadIDs = new long[count];
        for (int i = 0; i < count; i++) {
            waiterThreadIDs[i] = queryObjectID(classID,
                                    MONITOR_WAITER_FIELD_NAMES[i], JDWP.Tag.THREAD);
        }
    }

    /**
     * Perform testing JDWP command for specified objectID.
     */
    void testCommand(long objectID) {
        // make an array for results of found expected monitor waiters
        int expectedWaiters = waiterThreadIDs.length;
        int foundWaiters[] = new int[expectedWaiters];
        for (int i = 0; i < expectedWaiters; i++) {
            foundWaiters[i] = 0;
        }

        // create command packet and fill requred out data
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  objectID: " + objectID);
        command.addObjectID(objectID);
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

        // extract monitor owner threadID
        long owner = 0;
        try {
            owner = reply.getObjectID();
            log.display("    owner: " + owner);
        } catch (BoundException e) {
            log.complain("Unable to extract monitor owner threadID from reply packet:\n\t"
                    + e.getMessage());
            success = false;
            return;
        }

        // check that monitor owner is as expected
        if (owner < 0) {
            log.complain("Negative value of monitor owner threadID received: "
                        + owner);
            success = false;
        } else if (owner == 0) {
            log.complain("No monitor owner threadID received: "
                        + owner + " (expected: " + ownerThreadID + ")");
            success = false;
        } else if (owner != ownerThreadID) {
            log.complain("Unexpected monitor owner threadID received: "
                        + owner + " (expected: " + ownerThreadID + ")");
            success = false;
        }

        // extract number of monitor entries
        int entryCount = 0;
        try {
            entryCount = reply.getInt();
            log.display("  entryCount: " + entryCount);
        } catch (BoundException e) {
            log.complain("Unable to extract monitor entryCount from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check that number of waiters is not negative and not zero
        if (entryCount < 0) {
            log.complain("Negative number of monitor entryCount received: "
                        + entryCount);
            success = false;
        } else if (entryCount == 0) {
            log.complain("Zero number of monitor entryCount received: "
                        + entryCount);
            success = false;
        }

        // extract number of monitor waiter threads
        int waiters = 0;
        try {
            waiters = reply.getInt();
            log.display("  waiters: " + waiters);
        } catch (BoundException e) {
            log.complain("Unable to extract number of monitor waiters from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check that number of waiters is not negative
        if (waiters < 0) {
            log.complain("Negative number of monitor waiters received: "
                        + waiters);
            success = false;
        }

        // check that number of waiters is as expected
        if (waiters != expectedWaiters) {
            log.complain("Unexpected number of monitors waiters received: "
                        + waiters + " (expected: " + expectedWaiters + ")");
            success = false;
        }

        // extract monitor waiter threadIDs
        for (int i = 0; i < waiters; i++) {

            log.display("  waiter #" + i + ":");

            // extract threadID
            long threadID = 0;
            try {
                threadID = reply.getObjectID();
                log.display("    threadID: " + threadID);
            } catch (BoundException e) {
                log.complain("Unable to extract " + i + " monitor waiter threadID from reply packet:\n\t"
                        + e.getMessage());
                success = false;
                return;
            }

            // find threadID among expected monitor owner threadIDs
            boolean found = false;
            for (int j = 0; j < expectedWaiters; j++) {
                if (threadID == waiterThreadIDs[j]) {
                    foundWaiters[j]++;
                    found = true;
                }
            }
            if (!found) {
                log.complain("Unexpected monitor waiter threadID received: " + threadID);
                success = false;
            }

        }

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        // check if all expected monitor waiter threadIDs found
        for (int j = 0; j < expectedWaiters; j++) {
            if (foundWaiters[j] <= 0) {
                log.complain("Expected monitor waiter threadID NOT received: "
                            + waiterThreadIDs[j]);
                success = false;
            } else if (foundWaiters[j] > 1) {
                log.complain("Expected monitor waiter threadID (" +
                            + waiterThreadIDs[j] + ") received multiply times: "
                            + foundWaiters[j]);
                success = false;
            }
        }

    }

}
