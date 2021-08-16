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

package nsk.jdwp.Event.FIELD_MODIFICATION;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP event: FIELD_MODIFICATION.
 *
 * See fldmodification001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP event is tested in the method waitForTestedEvent().
 *
 * @see #runIt()
 * @see #waitForTestedEvent()
 */
public class fldmodification001 {

    // exit status constants
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    // VM capability constatnts
    static final int VM_CAPABILITY_NUMBER = JDWP.Capability.CAN_WATCH_FIELD_MODIFICATION;
    static final String VM_CAPABILITY_NAME = "canWatchFieldModification";

    // package and classes names constants
    static final String PACKAGE_NAME = "nsk.jdwp.Event.FIELD_MODIFICATION";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "fldmodification001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    // tested JDWP event constants
    static final byte TESTED_EVENT_KIND = JDWP.EventKind.FIELD_MODIFICATION;
    static final byte TESTED_EVENT_SUSPEND_POLICY = JDWP.SuspendPolicy.ALL;

    // name and signature of the tested class
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedObjectClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";
    static final String TESTED_THREAD_NAME = "TestedThread";

    // name of field and method of tested class
    static final String OBJECT_FIELD_NAME = "object";
    static final String VALUE_FIELD_NAME = "value";
    static final String TESTED_METHOD_NAME = "methodForAccess";
    static final String BREAKPOINT_METHOD_NAME = "run";
    static final int BREAKPOINT_LINE = fldmodification001a.BREAKPOINT_LINE;
    static final int FIELD_MODIFICATION_LINE = fldmodification001a.FIELD_MODIFICATION_LINE;
    static final int FIELD_MODIFICATION_VALUE = 6574;

    // usual scaffold objects
    ArgumentHandler argumentHandler = null;
    Log log = null;
    Binder binder = null;
    Debugee debugee = null;
    Transport transport = null;
    int waitTime = 0;  // minutes
    long timeout = 0;  // milliseconds
    boolean dead = false;
    boolean success = true;

    // obtained data
    long testedClassID = 0;
    long testedThreadID = 0;
    long testedMethodID = 0;
    long testedFieldID = 0;
    long testedObjectID = 0;
    JDWP.Location testedLocation = null;
    int eventRequestID = 0;

    // -------------------------------------------------------------------

    /**
     * Start test from command line.
     */
    public static void main(String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    /**
     * Start test from JCK-compilant environment.
     */
    public static int run(String argv[], PrintStream out) {
        return new fldmodification001().runIt(argv, out);
    }

    // -------------------------------------------------------------------

    /**
     * Perform test execution.
     */
    public int runIt(String argv[], PrintStream out) {

        // make log for debugger messages
        argumentHandler = new ArgumentHandler(argv);
        log = new Log(out, argumentHandler);
        waitTime = argumentHandler.getWaitTime();
        timeout = waitTime * 60 * 1000;

        // execute test and display results
        try {
            log.display("\n>>> Starting debugee \n");

            // launch debuggee
            binder = new Binder(argumentHandler, log);
            log.display("Launching debugee");
            debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
            transport = debugee.getTransport();
            log.display("  ... debugee launched");
            log.display("");

            // set timeout for debuggee responces
            log.display("Setting timeout for debuggee responces: " + waitTime + " minute(s)");
            transport.setReadTimeout(timeout);
            log.display("  ... timeout set");

            // wait for debuggee started
            log.display("Waiting for VM_INIT event");
            debugee.waitForVMInit();
            log.display("  ... VM_INIT event received");

            // query debugee for VM-dependent ID sizes
            log.display("Querying for IDSizes");
            debugee.queryForIDSizes();
            log.display("  ... size of VM-dependent types adjusted");

            // check for VM capability
            log.display("\n>>> Checking VM capability \n");
            log.display("Getting VM capability: " + VM_CAPABILITY_NAME);
            boolean capable = debugee.getCapability(VM_CAPABILITY_NUMBER, VM_CAPABILITY_NAME);
            log.display("  ... got VM capability: " + capable);

            // exit as PASSED if this capability is not supported
            if (!capable) {
                out.println("TEST PASSED: unsupported VM capability: "
                            + VM_CAPABILITY_NAME);
                return PASSED;
            }

            // prepare debuggee
            log.display("\n>>> Getting prepared for testing \n");
            prepareForTest();

            // test JDWP event
            log.display("\n>>> Testing JDWP event \n");
            log.display("Making request for FIELD_MODIFICATION event for field: "
                    + VALUE_FIELD_NAME);
            requestTestedEvent();
            log.display("  ... got requestID: " + eventRequestID);
            log.display("");

            // resume debuggee
            log.display("Resumindg debuggee");
            debugee.resume();
            log.display("  ... debuggee resumed");
            log.display("");

            // wait for tested FIELD_MODIFICATION event
            log.display("Waiting for FIELD_MODIFICATION event received");
            waitForTestedEvent();
            log.display("  ... event received");
            log.display("");

            // clear tested request for FIELD_MODIFICATION event
            log.display("Clearing request for tested event");
            clearTestedRequest();
            log.display("  ... request removed");

            // finish debuggee after testing
            log.display("\n>>> Finishing debuggee \n");

            // resume debuggee
            log.display("Resuming debuggee");
            debugee.resume();
            log.display("  ... debuggee resumed");

            // wait for debuggee exited
            log.display("Waiting for VM_DEATH event");
            debugee.waitForVMDeath();
            dead = true;
            log.display("  ... VM_DEATH event received");

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            success = false;
        } catch (Exception e) {
            e.printStackTrace(out);
            log.complain("Caught unexpected exception while running the test:\n\t" + e);
            success = false;
        } finally {
            // quit debugee
            log.display("\n>>> Finishing test \n");
            quitDebugee();
        }

        // check test results
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
        // wait for tested class loaded
        log.display("Waiting for tested class loaded:\n\t" + TESTED_CLASS_NAME);
        testedClassID = debugee.waitForClassLoaded(TESTED_CLASS_NAME, JDWP.SuspendPolicy.ALL);
        log.display("  ... class loaded with classID: " + testedClassID);
        log.display("");

        // get fieldID for the tested field
        log.display("Getting tested fieldID for field name: " + VALUE_FIELD_NAME);
        testedFieldID = debugee.getClassFieldID(testedClassID, VALUE_FIELD_NAME, true);
        log.display("  ... got fieldID: " + testedFieldID);

        // get methodID for the tested method
        log.display("Getting tested methodID for method name: " + TESTED_METHOD_NAME);
        testedMethodID = debugee.getMethodID(testedClassID, TESTED_METHOD_NAME, true);
        log.display("  ... got methodID: " + testedMethodID);

        // get codeIndex for field access line
        log.display("Getting codeIndex for field modification line: " + FIELD_MODIFICATION_LINE);
        long codeIndex = debugee.getCodeIndex(testedClassID, testedMethodID, FIELD_MODIFICATION_LINE);
        log.display("  ... got index: " + codeIndex);

        // create location for method entry line
        log.display("Creating location for field modofication line");
        testedLocation = new JDWP.Location(JDWP.TypeTag.CLASS, testedClassID,
                                                        testedMethodID, codeIndex);
        log.display("  ... got location: " + testedLocation);
        log.display("");

        // wait for breakpoint reached
        log.display("Waiting for breakpoint reached at: "
                        + BREAKPOINT_METHOD_NAME + ":" + BREAKPOINT_LINE);
        testedThreadID = debugee.waitForBreakpointReached(testedClassID,
                                                        BREAKPOINT_METHOD_NAME,
                                                        BREAKPOINT_LINE,
                                                        JDWP.SuspendPolicy.ALL);
        log.display("  ... breakpoint reached with threadID: " + testedThreadID);
        log.display("");

        // get objectID of the tested object from static field
        log.display("Getting tested objectID from static field: " + OBJECT_FIELD_NAME);
        JDWP.Value value = debugee.getStaticFieldValue(testedClassID, OBJECT_FIELD_NAME, JDWP.Tag.OBJECT);
        testedObjectID = ((Long)value.getValue()).longValue();
        log.display("  ... got objectID: " + testedObjectID);
    }

    /**
     * Make request for tested FIELD_MODIFICATION event.
     */
    void requestTestedEvent() {
        Failure failure = new Failure("Error occured while makind request for tested event");

        // create command packet and fill requred out data
        log.display("Create command packet: " + "EventRequest.Set");
        CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.Set);
        log.display("    eventKind: " + TESTED_EVENT_KIND);
        command.addByte(TESTED_EVENT_KIND);
        log.display("    eventPolicy: " + TESTED_EVENT_SUSPEND_POLICY);
        command.addByte(TESTED_EVENT_SUSPEND_POLICY);
        log.display("    modifiers: " + 1);
        command.addInt(1);
        log.display("      modKind: " + JDWP.EventModifierKind.FIELD_ONLY + " (FIELD_ONLY)");
        command.addByte(JDWP.EventModifierKind.FIELD_ONLY);
        log.display("      classID: " + testedClassID);
        command.addReferenceTypeID(testedClassID);
        log.display("      fieldID: " + testedFieldID);
        command.addFieldID(testedFieldID);
        command.setLength();
        log.display("  ... command packet composed");
        log.display("");

        // send command packet to debugee
        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
            log.display("  ... command packet sent");
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n\t" + e);
            success = false;
            throw failure;
        }
        log.display("");

        // receive reply packet from debugee
        ReplyPacket reply = new ReplyPacket();
        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("  ... packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n\t" + e);
            success = false;
            throw failure;
        }
        log.display("");

        // check reply packet header
        try{
            log.display("Checking header of reply packet");
            reply.checkHeader(command.getPacketID());
            log.display("  ... packet header is correct");
        } catch (BoundException e) {
            log.complain("Bad header of reply packet:\n\t" + e.getMessage());
            success = false;
            throw failure;
        }

        // start parsing reply packet data
        log.display("Parsing reply packet:");
        reply.resetPosition();

        // extract requestID
        int requestID = 0;
        try {
            requestID = reply.getInt();
            log.display("    requestID: " + requestID);
        } catch (BoundException e) {
            log.complain("Unable to extract requestID from request reply packet:\n\t"
                        + e.getMessage());
            success = false;
            throw failure;
        }

        // check requestID
        if (requestID == 0) {
            log.complain("Unexpected null requestID returned: " + requestID);
            success = false;
            throw failure;
        }

        eventRequestID = requestID;

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in request reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        log.display("  ... reply packet parsed");
    }

    /**
     * Clear request for tested FIELD_MODIFICATION event.
     */
    void clearTestedRequest() {
        Failure failure = new Failure("Error occured while clearing request for tested event");

        // create command packet and fill requred out data
        log.display("Create command packet: " + "EventRequest.Clear");
        CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.Clear);
        log.display("    event: " + TESTED_EVENT_KIND);
        command.addByte(TESTED_EVENT_KIND);
        log.display("    requestID: " + eventRequestID);
        command.addInt(eventRequestID);
        log.display("  ... command packet composed");
        log.display("");

        // send command packet to debugee
        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
            log.display("  ... command packet sent");
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n\t" + e);
            success = false;
            throw failure;
        }
        log.display("");

        ReplyPacket reply = new ReplyPacket();

        // receive reply packet from debugee
        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("  ... packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n\t" + e);
            success = false;
            throw failure;
        }

        // check reply packet header
        try{
            log.display("Checking header of reply packet");
            reply.checkHeader(command.getPacketID());
            log.display("  ... packet header is correct");
        } catch (BoundException e) {
            log.complain("Bad header of reply packet:\n\t" + e.getMessage());
            success = false;
            throw failure;
        }

        // start parsing reply packet data
        log.display("Parsing reply packet:");
        reply.resetPosition();

        log.display("    no data");

        // check for extra data in reply packet
        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in request reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        log.display("  ... reply packet parsed");
    }

    /**
     * Wait for tested FIELD_MODIFICATION event.
     */
    void waitForTestedEvent() {

        EventPacket eventPacket = null;

        // receive reply packet from debugee
        try {
            log.display("Waiting for event packet");
            eventPacket = debugee.getEventPacket(timeout);
            log.display("  ... event packet received:\n" + eventPacket);
        } catch (IOException e) {
            log.complain("Unable to read tested event packet:\n\t" + e);
            success = false;
            return;
        }
        log.display("");

        // check reply packet header
        try{
            log.display("Checking header of event packet");
            eventPacket.checkHeader();
            log.display("  ... packet header is correct");
        } catch (BoundException e) {
            log.complain("Bad header of tested event packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // start parsing reply packet data
        log.display("Parsing event packet:");
        eventPacket.resetPosition();

        // get suspendPolicy value
        byte suspendPolicy = 0;
        try {
            suspendPolicy = eventPacket.getByte();
            log.display("    suspendPolicy: " + suspendPolicy);
        } catch (BoundException e) {
            log.complain("Unable to get suspendPolicy value from tested event packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check suspendPolicy value
        if (suspendPolicy != TESTED_EVENT_SUSPEND_POLICY) {
            log.complain("Unexpected SuspendPolicy in tested event packet: " +
                        suspendPolicy + " (expected: " + TESTED_EVENT_SUSPEND_POLICY + ")");
            success = false;
        }

        // get events count
        int events = 0;
        try {
            events = eventPacket.getInt();
            log.display("    events: " + events);
        } catch (BoundException e) {
            log.complain("Unable to get events count from tested event packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        // check events count
        if (events < 0) {
            log.complain("Negative value of events number in tested event packet: " +
                        events + " (expected: " + 1 + ")");
            success = false;
        } else if (events != 1) {
            log.complain("Invalid number of events in tested event packet: " +
                        events + " (expected: " + 1 + ")");
            success = false;
        }

        // extract each event
        long eventThreadID = 0;
        for (int i = 0; i < events; i++) {
            log.display("    event #" + i + ":");

            // get eventKind
            byte eventKind = 0;
            try {
                eventKind = eventPacket.getByte();
                log.display("      eventKind: " + eventKind);
            } catch (BoundException e) {
                log.complain("Unable to get eventKind of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check eventKind
            if (eventKind == JDWP.EventKind.VM_DEATH) {
                log.complain("Unexpected VM_DEATH event received: " +
                            eventKind + " (expected: " + JDWP.EventKind.FIELD_MODIFICATION + ")");
                dead = true;
                success = false;
                return;
            }  else if (eventKind != JDWP.EventKind.FIELD_MODIFICATION) {
                log.complain("Unexpected eventKind of event " + i + " in tested event packet: " +
                            eventKind + " (expected: " + JDWP.EventKind.FIELD_MODIFICATION + ")");
                success = false;
                return;
            }

            // get requestID
            int requestID = 0;
            try {
                requestID = eventPacket.getInt();
                log.display("      requestID: " + requestID);
            } catch (BoundException e) {
                log.complain("Unable to get requestID of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check requestID
            if (requestID != eventRequestID) {
                log.complain("Unexpected requestID of event " + i + " in tested event packet: " +
                            requestID + " (expected: " + eventRequestID + ")");
                success = false;
            }

            // get threadID
            long threadID = 0;
            try {
                threadID = eventPacket.getObjectID();
                log.display("      threadID: " + threadID);
            } catch (BoundException e) {
                log.complain("Unable to get threadID of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check threadID
            if (threadID != testedThreadID) {
                log.complain("Unexpected threadID of event " + i + " in tested event packet: " +
                            threadID + " (expected: " + testedThreadID + ")");
                success = false;
            }

            // get location
            JDWP.Location location = null;
            try {
                location = eventPacket.getLocation();
                log.display("      location: " + location);
            } catch (BoundException e) {
                log.complain("Unable to get location of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check location
            checkLocation(location, testedLocation, i, FIELD_MODIFICATION_LINE);

            // get type tag
            byte refTypeTag = 0;
            try {
                refTypeTag = eventPacket.getByte();
                log.display("      refTypeTag: " + refTypeTag);
            } catch (BoundException e) {
                log.complain("Unable to get reftypetag of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check type tag
            if (refTypeTag != JDWP.TypeTag.CLASS) {
                log.complain("Unexpected refTypeTag of event " + i + " in tested event packet: " +
                            refTypeTag + " (expected: " + JDWP.TypeTag.CLASS + ")");
                success = false;
            }

            // get typeID
            long typeID = 0;
            try {
                typeID = eventPacket.getReferenceTypeID();
                log.display("      typeID: " + typeID);
            } catch (BoundException e) {
                log.complain("Unable to get typeID of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check typeID
            if (typeID != testedClassID) {
                log.complain("Unexpected typeID of event " + i + " in tested event packet: " +
                            typeID + " (expected: " + testedClassID + ")");
                success = false;
            }

            // get fieldID
            long fieldID = 0;
            try {
                fieldID = eventPacket.getFieldID();
                log.display("      fieldID: " + fieldID);
            } catch (BoundException e) {
                log.complain("Unable to get fieldID of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check fieldID
            if (fieldID != testedFieldID) {
                log.complain("Unexpected fieldID of event " + i + " in tested event packet: " +
                            fieldID + " (expected: " + testedFieldID + ")");
                success = false;
            }

            // get object tag
            byte objectTag = 0;
            try {
                objectTag = eventPacket.getByte();
                log.display("      objectTag: " + objectTag);
            } catch (BoundException e) {
                log.complain("Unable to get object tag of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check object tag
            if (objectTag != JDWP.Tag.OBJECT) {
                log.complain("Unexpected object tag of event " + i + " in tested event packet: " +
                            objectTag + " (expected: " + JDWP.Tag.OBJECT + ")");
                success = false;
            }

            // get objectID
            long objectID = 0;
            try {
                objectID = eventPacket.getObjectID();
                log.display("      objectID: " + objectID);
            } catch (BoundException e) {
                log.complain("Unable to get objectID of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check objectID
            if (objectID != 0) {
                log.complain("Unexpected objectID of event " + i + " in tested event packet: " +
                            objectID + " (expected: " + 0 + ")");
                success = false;
            }

            // get valueToBe
            JDWP.Value valueToBe = null;
            try {
                valueToBe = eventPacket.getValue();
                log.display("      valueToBe: " + valueToBe);
            } catch (BoundException e) {
                log.complain("Unable to get valueToBe of event #" + i + " from tested event packet:\n\t"
                            + e.getMessage());
                success = false;
                return;
            }

            // check value tag
            byte valueTag = valueToBe.getTag();
            if (valueTag != JDWP.Tag.INT) {
                log.complain("Unexpected valueToBe tag of event " + i + " in tested event packet: " +
                            valueTag + " (expected: " + JDWP.Tag.INT + ")");
                success = false;
            }

            // check integer value
            int intValue = ((Integer)valueToBe.getValue()).intValue();
            if (intValue != FIELD_MODIFICATION_VALUE) {
                log.complain("Unexpected valueToBe of event " + i + " in tested event packet: " +
                            intValue + " (expected: " + FIELD_MODIFICATION_VALUE + ")");
                success = false;
            }
        }

        // check for extra data in event packet
        if (!eventPacket.isParsed()) {
            log.complain("Extra trailing bytes found in event packet at: "
                        + eventPacket.offsetString());
            success = false;
        }

        log.display("  ... event packet parsed");
    }

    /**
     * Check if given location is equal to the expected one.
     */
    void checkLocation(JDWP.Location location, JDWP.Location expectedLocation,
                                                int eventNumber, int expectedLine) {
        if (location.getTag() != expectedLocation.getTag()) {
            log.complain("Unexpected class tag of location of event "
                        + eventNumber + " in tested event packet: " + location.getTag()
                        + " (expected: " + expectedLocation.getTag() + ")");
            success = false;
        }
        if (location.getClassID() != expectedLocation.getClassID()) {
            log.complain("Unexpected classID of location of event "
                        + eventNumber + " in tested event packet: " + location.getClassID()
                        + " (expected: " + expectedLocation.getClassID() + ")");
            success = false;
        }
        if (location.getMethodID() != expectedLocation.getMethodID()) {
            log.complain("Unexpected methodID of location of event "
                        + eventNumber + " in tested event packet: " + location.getMethodID()
                        + " (expected: " + expectedLocation.getMethodID() + ")");
            success = false;
        }
        if (location.getIndex() != expectedLocation.getIndex()) {
/*
            log.complain("Unexpected codeIndex of location of event " + i
                        + " in tested event packet: " + location.getIndex()
                        + " (expected: " + expectedLocation.getIndex() + ")");
            success = false;
*/
            try {
                // find approximate line number for location
                int lineNumber = debugee.getLineNumber(location, true);
                if (lineNumber != expectedLine) {
                    log.complain("Unexpected line number of location of event "
                                + eventNumber + " in tested event packet: " + lineNumber
                                + " (expected: " + expectedLine + ")");
                    success = false;
                } else {
                    log.display("Unexpected codeIndex of location: " + location.getIndex()
                                + " (expected: " + expectedLocation.getIndex() + ")");
                    log.display("Though line number of catch location is as expected: "
                                + expectedLine);
                }
            } catch (Failure e) {
                log.complain("Unable to get line number for location of event "
                            + eventNumber + " in tested event packet:\n\t" + e.getMessage());
                success = false;
            }
        }
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
