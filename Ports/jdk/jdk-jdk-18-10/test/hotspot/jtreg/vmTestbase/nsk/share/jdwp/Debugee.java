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

package nsk.share.jdwp;

import nsk.share.*;
import nsk.share.jpda.*;

import java.util.*;
import java.io.*;

/**
 * This class is used to interact with debugee VM using JDWP features.
 * <p>
 * This class is an mirror of debugee VM that is constructed by
 * <code>Binder</code> and uses <code>Transport</code> object
 * to interact with debugee VM.
 * <p>
 * In addition to the general abities to control of debugee VM process,
 * provided by the base class <code>DebugeeProcess</code>, this class
 * adds some service methods that uses JDWP protocol to simplify interaction
 * with debugee VM (such as finding classes, setting breakpoints,
 * handling events, and so on.).
 *
 * @see Binder
 * @see Transport
 * @see DebugeeProcess
 */
abstract public class Debugee extends DebugeeProcess {

    /** Binder that creates this debugee. */
    protected Binder binder = null;

    protected LinkedList<EventPacket> eventQueue = new LinkedList<EventPacket>();

    protected Transport transport = null;

    /** Make new <code>Debugee</code> object for the given binder. */
    protected Debugee (Binder binder) {
        super(binder);
        this.argumentHandler = binder.getArgumentHandler();
        this.binder = binder;
        prefix = "Debugee> ";
    }

    /** Return <code>Binder</code> of the debugee object. */
    public Binder getBinder() {
        return binder;
    }

    /** Return <code>Transport</code> of the debugee object. */
    public Transport getTransport() {
        return transport;
    }

    /**
     * Prepare transport object for establishing connection.
     * This may change connection options in <code>argumentHandler</code>.
     *
     * @return specific address string if listening has started or null otherwise
     */
    public String prepareTransport(ArgumentHandler argumentHandler) {
        String address = null;
        try {
            if (argumentHandler.isSocketTransport()) {
                SocketTransport socket_transport = new SocketTransport(log);
                if (argumentHandler.isListeningConnector()) {
                    int port = 0;
                    if (argumentHandler.isTransportAddressDynamic()) {
                        port = socket_transport.bind(0);
//                        argumentHandler.setTransportPortNumber(port);
                    } else {
                        port = argumentHandler.getTransportPortNumber();
                        socket_transport.bind(port);
                    }
                    address = argumentHandler.getTestHost() + ":" + port;
                }
                transport = socket_transport;
/*
            } else if (argumentHandler.isShmemTransport()) {
                ShmemTransport shmem_transport = new ShmemTransport(log);
                if (argumentHandler.isListeningConnector()) {
                    String sharedName = agrHandler.getTransportSharedName();
                    shmem_transport.bind(sharedName);
                    address = sharedName;
                }
                transport = shmem_transport;
 */
            } else {
               throw new TestBug("Unexpected transport type: "
                               + argumentHandler.getTransportType());
            }

        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught IOException while preparing for JDWP transport connection:\n\t"
                    + e);
        }

        return address;
    }

    /**
     * Establish connection to debugee VM.
     */
    public Transport connect() {
        if (transport == null) {
            throw new Failure("Attemt to establish JDWP connection for not prepared transport");
        }

        try {
            if (argumentHandler.isSocketTransport()) {
                display("Establishing JDWP socket connection");
                SocketTransport socket_transport = (SocketTransport)transport;
                int transportPort = argumentHandler.getTransportPortNumber();
                if (argumentHandler.isAttachingConnector()) {
                    String debugeeHost = argumentHandler.getDebugeeHost();
                    display("Attaching to debugee: " + debugeeHost + ":" + transportPort);
                    socket_transport.attach(debugeeHost, transportPort);
                } else if (argumentHandler.isListeningConnector()) {
                    display("Listening from debugee");
                    socket_transport.accept();
                } else {
                    throw new TestBug("Unexpected connector type: "
                                    + argumentHandler.getConnectorType());
                }
/*
            } else if (argumentHandler.isShmemTransport()) {
                display("Establishing JDWP shared-memory connection");
                ShmemTransport shmem_transport = (ShmemTransport)transport;
                String sharedName = argumentHandler.getTransportSharedName();
                if (argumentHandler.isAttachingConnector()) {
                    display("Attaching to debugee: " + sharedName);
                    shmem_transport.attach(sharedName);
                } else if (argumentHandler.isListeningConnector()) {
                    display("Listening from debugee");
                    shmem_transport.accept();
                } else {
                    throw new TestBug("Unexpected connector type: "
                                    + argumentHandler.getConnectorType());
                }
 */
            } else {
               throw new TestBug("Unexpected transport type: "
                               + argumentHandler.getTransportType());
            }

            transport.handshake();

        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught IOException while establishing JDWP transport connection:\n\t"
                    + e);
        }
        return transport;
    }

    // --------------------------------------------------- //

   /**
    * Waits for VM_INIT event from debugee VM.
    */
    public void waitForVMInit() {
        String eventName = "VirtualMachine.VM_START";
        EventPacket packet = receiveEventFor(JDWP.EventKind.VM_START, eventName);
//        String versionInfo = getVersionInfo();
//        display("Target VM started:\n" + versionInfo);
    }

   /**
    * Waits for VM_DEATH event from debugee VM.
    */
    public void waitForVMDeath() {
        String eventName = "VirtualMachine.VM_DEATH";
        EventPacket packet = receiveEventFor(JDWP.EventKind.VM_DEATH, eventName);
    }

    /**
     * Wait for class loaded on debugee start up and return its classID.
     * Debuggee should be initially suspended and it will also left suspended
     * by the CLASS_PREPARE event request.
     */
    public long waitForClassLoaded(String className, byte suspendPolicy) {
        // make request for CLASS_PREPARE_EVENT for this class name
        int requestID = requestClassPrepareEvent(className, suspendPolicy);
        // resume initially suspended debugee
        resume();
        // wait for CLASS_PREPARE_EVENT
        return waitForClassPrepareEvent(requestID, className);
    }

    /**
     * Wait for classes loaded on debugee start up and return their classIDs.
     * Debuggee should be initially suspended and it will also left suspended
     * by the CLASS_PREPARE event request.
     */
    public long[] waitForClassesLoaded(String classNames[], byte suspendPolicy) {
        int count = classNames.length;

        // make requests for CLASS_PREPARE_EVENT for these class names
        int[] requestIDs = new int[count];
        for (int i = 0; i < count; i++) {
            requestIDs[i] = requestClassPrepareEvent(classNames[i], suspendPolicy);
        }

        // resume initially suspended debugee
        resume();

        return waitForClassPrepareEvents(requestIDs, classNames);
    }

    /**
     * Wait for breakpoint reached and return threadIDs.
     * Debuggee should be initially suspended and it will also left suspended
     * by the BREAKPOINT event request.
     */
    public long waitForBreakpointReached(long classID, String methodName,
                                                int line, byte suspendPolicy) {
        // query debuggee for methodID
        long methodID = getMethodID(classID, methodName, true);
        // create BREAKPOINT event request
        int requestID = requestBreakpointEvent(JDWP.TypeTag.CLASS, classID, methodID,
                                                                line, suspendPolicy);
        // resume initially suspended debugee
        resume();
        // wait for BREAKPOINT event
        return waitForBreakpointEvent(requestID);
    }

    // --------------------------------------------------- //

    /**
     * Wait for CLASS_PREPARE event made by given request received
     * and return classID.
     * Debuggee will be left suspended by the CLASS_PREPARE event.
     */
    public long waitForClassPrepareEvent(int requestID, String className) {
        String error = "Error occured while waiting for CLASS_PREPARE event for class:\n\t"
                        + className;

        String signature = "L" + className.replace('.', '/') + ";";
        long classID = 0;

        // wait for CLASS_PREPARE event
        for(;;) {
            EventPacket event = receiveEvent();
            byte eventSuspendPolicy = 0;
            long eventThreadID = 0;
            try {
                eventSuspendPolicy = event.getByte();
                int events = event.getInt();
                for (int i = 0; i < events; i++) {
                    // check event kind
                    byte eventKind = event.getByte();
                    if (eventKind == JDWP.EventKind.VM_DEATH) {
                        complain("Unexpected VM_DEATH event received: " + eventKind
                                + " (expected: " + JDWP.EventKind.CLASS_PREPARE +")");
                        throw new Failure(error);
                    } else if (eventKind != JDWP.EventKind.CLASS_PREPARE) {
                        complain("Unexpected event kind received: " + eventKind
                                + " (expected: " + JDWP.EventKind.CLASS_PREPARE +")");
                        throw new Failure(error);
                    }

                    // extract CLASS_PREPARE event specific data
                    int eventRequestID = event.getInt();
                    eventThreadID = event.getObjectID();
                    byte eventRefTypeTag = event.getByte();
                    long eventClassID = event.getReferenceTypeID();
                    String eventClassSignature = event.getString();
                    int eventClassStatus = event.getInt();

                    // check if event was single
                    if (events > 1) {
                        complain("Not single CLASS_PREPARE event received for class:\n\t"
                                    + eventClassSignature);
                        throw new Failure(error);
                    }

                    // check if event is for expected class
                    if (eventClassSignature.equals(signature)) {

                        // check if event is because of expected request
                        if (eventRequestID != requestID) {
                            complain("CLASS_PREPARE event with unexpected requestID ("
                                    + eventRequestID + ") received for class:\n\t"
                                    + eventClassSignature);
                            throw new Failure(error);
                        }

                        // remove event request
                        clearEventRequest(JDWP.EventKind.CLASS_PREPARE, requestID);

                        return eventClassID;
                    } else {
                        complain("Unexpected CLASS_PREPARE event received with class signature:\n"
                               + "  " + eventClassSignature);
                    }

                }

            } catch (BoundException e) {
                complain("Unable to extract data from event packet while waiting for CLASS_PREPARE event:\n\t"
                        + e.getMessage() + "\n" + event);
                throw new Failure(error);
            }

            // resume debuggee according to event suspend policy
            resumeEvent(eventSuspendPolicy, eventThreadID);
        }
    }

    /**
     * Wait for CLASS_PREPARE events made by given requests received
     * and return classIDs.
     * Debuggee will be left suspended by the CLASS_PREPARE event.
     */
    public long[] waitForClassPrepareEvents(int requestIDs[], String classNames[]) {
        int count = classNames.length;
        String error = "Error occured while waiting for " + count + " CLASS_PREPARE events";

        // prepare expected class signatures
        String[] signatures = new String[count];
        for (int i = 0; i < count; i++) {
            signatures[i] = "L" + classNames[i].replace('.', '/') + ";";
        }

        // clear list of classIDs
        long[] classIDs = new long[count];
        for (int i = 0; i < count; i++) {
            classIDs[i] = 0;
        }

        // wait for all expected CLASS_PREPARE events
        int received = 0;
        for(;;) {
            EventPacket event = receiveEvent();
            byte eventSuspendPolicy = 0;
            long eventThreadID = 0;
            try {
                eventSuspendPolicy = event.getByte();
                int events = event.getInt();
                for (int i = 0; i < events; i++) {
                    // check event kind
                    byte eventKind = event.getByte();
                    if (eventKind == JDWP.EventKind.VM_DEATH) {
                        complain("Unexpected VM_DEATH event received: " + eventKind
                                + " (expected: " + JDWP.EventKind.CLASS_PREPARE +")");
                        throw new Failure(error);
                    } else if (eventKind != JDWP.EventKind.CLASS_PREPARE) {
                        complain("Unexpected event kind received: " + eventKind
                                + " (expected: " + JDWP.EventKind.CLASS_PREPARE +")");
                        throw new Failure(error);
                    }

                    // extracy CLASS_PREPARE event specific data
                    int eventRequestID = event.getInt();
                    eventThreadID = event.getObjectID();
                    byte eventRefTypeTag = event.getByte();
                    long eventClassID = event.getReferenceTypeID();
                    String eventClassSignature = event.getString();
                    int eventClassStatus = event.getInt();

                    // check if event was single
                    if (events > 1) {
                        complain("Not single CLASS_PREPARE event received for class:\n\t"
                                + eventClassSignature);
                    }

                    // find appropriate class by signature
                    boolean found = false;
                    for (int j = 0; j < count; j++) {
                        if (eventClassSignature.equals(signatures[j])) {
                            found = true;

                            // check if event is not duplicated
                            if (classIDs[j] != 0) {
                                complain("Extra CLASS_PREPARE event recieved for class:\n\t"
                                        + eventClassSignature);
                            } else {
                                classIDs[j] = eventClassID;
                                received ++;
                            }

                            // check if event is because of expected request
                            if (eventRequestID != requestIDs[j]) {
                                complain("CLASS_PREPARE event with unexpected requestID ("
                                        + requestIDs[j] + ") received for class:\n\t"
                                        + eventClassSignature);
                            } else {
                                clearEventRequest(JDWP.EventKind.CLASS_PREPARE, requestIDs[j]);
                            }
                        }
                    }
                    if (!found) {
                        log.complain("Unexpected CLASS_PREPARE event received with class signature:\n"
                                   + "  " + eventClassSignature);
                    }
                }
            } catch (BoundException e) {
                complain("Unable to extract data from event packet while waiting for CLASS_PREPARE event:\n\t"
                            + e.getMessage() + "\n" + event);
                throw new Failure(error);
            }

            // if all events received return without resuming
            if (received >= count)
                return classIDs;

            // resume debuggee according to events suspend policy
            resumeEvent(eventSuspendPolicy, eventThreadID);
        }
    }

    /**
     * Wait for BREAKPOINT event made by the given request and return threadID.
     * Debuggee will be left suspended by the BREAKPOINT event.
     */
    public long waitForBreakpointEvent(int requestID) {
        String error = "Error occured while waiting for BREAKPOINT event for ";

        for(;;) {
            EventPacket event = receiveEvent();
            byte eventSuspendPolicy = 0;
            long eventThreadID = 0;
            try {
                eventSuspendPolicy = event.getByte();
                int events = event.getInt();
                for (int i = 0; i < events; i++) {
                    // check event kind
                    byte eventKind = event.getByte();
                    if (eventKind == JDWP.EventKind.VM_DEATH) {
                        complain("Unexpected VM_DEATH event received: " + eventKind
                                + " (expected: " + JDWP.EventKind.BREAKPOINT +")");
                        throw new Failure(error);
                    } else if (eventKind != JDWP.EventKind.BREAKPOINT) {
                        complain("Unexpected event kind received: " + eventKind
                                + " (expected: " + JDWP.EventKind.BREAKPOINT +")");
                        throw new Failure(error);
                    }

                    // extrack specific BREAKPOINT event data
                    int eventRequestID = event.getInt();
                    eventThreadID = event.getObjectID();
                    JDWP.Location eventLocation = event.getLocation();

                    if (eventRequestID == requestID) {
                        clearEventRequest(JDWP.EventKind.BREAKPOINT, requestID);
                        return eventThreadID;
                    } else {
                        complain("Unexpected BREAKPOINT event received with requestID: "
                                + eventRequestID + " (expected: " + requestID + ")");
                    }
                }
            } catch (BoundException e) {
                complain("Unable to extract data from event packet while waiting for BREAKPOINT event:\n\t"
                            + e.getMessage() + "\n" + event);
                throw new Failure(error);
            }

            resumeEvent(eventSuspendPolicy, eventThreadID);
        }
    }

    /**
     * Resume debuggee according given event suspend policy.
     */
    public void resumeEvent(byte suspendPolicy, long threadID) {
        if (suspendPolicy == JDWP.SuspendPolicy.NONE) {
            // do nothing
        } else if (suspendPolicy == JDWP.SuspendPolicy.EVENT_THREAD) {
            resumeThread(threadID);
        } else if (suspendPolicy == JDWP.SuspendPolicy.ALL) {
            resume();
        } else {
            throw new Failure("Unexpected event suspend policy while resuming debuggee: "
                            + suspendPolicy);
        }
    }

    // --------------------------------------------------- //

    /**
     * Query target VM for version info.
     */
    public String getVersionInfo() {
        String commandName = "VirtualMachine.Version";
        CommandPacket command =
            new CommandPacket(JDWP.Command.VirtualMachine.Version);
        ReplyPacket reply = receiveReplyFor(command, commandName);

        try {
            String description = reply.getString();
            int jdwpMajor = reply.getInt();
            int jdwpMinor = reply.getInt();
            String vmVersion = reply.getString();
            String vmName = reply.getString();
            return description;
        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting JDWP and VM version info");
        }
    }

   /**
    * Query target VM about VM dependent ID sizes.
    */
    public void queryForIDSizes() {
        String commandName = "VirtualMachine.IDSizes";
        CommandPacket command = new CommandPacket(JDWP.Command.VirtualMachine.IDSizes);
        ReplyPacket reply = receiveReplyFor(command);
        try {
            reply.resetPosition();
            JDWP.TypeSize.FIELD_ID = reply.getInt();
            JDWP.TypeSize.METHOD_ID = reply.getInt();
            JDWP.TypeSize.OBJECT_ID = reply.getInt();
            JDWP.TypeSize.REFERENCE_TYPE_ID = reply.getInt();
            JDWP.TypeSize.FRAME_ID = reply.getInt();
        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting VM dependent ID sizes");
        }
        JDWP.TypeSize.CalculateSizes();
    }

    // --------------------------------------------------- //

    /**
     * Suspend the debugee VM by sending VirtualMachine.Suspend command.
     */
    public void suspend() {
        String commandName = "VirtualMachine.Suspend";
        CommandPacket command = new CommandPacket(JDWP.Command.VirtualMachine.Suspend);
        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    /**
     * Resume the debugee VM by sending VirtualMachine.Resume command.
     */
    public void resume() {
        String commandName = "VirtualMachine.Resume";
        CommandPacket command = new CommandPacket(JDWP.Command.VirtualMachine.Resume);
        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    /**
     * Dispose the debugee VM by sending VirtualMachine.Dispose command.
     */
    public void dispose() {
        String commandName = "VirtualMachine.Dispose";
        CommandPacket command = new CommandPacket(JDWP.Command.VirtualMachine.Dispose);
        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    // --------------------------------------------------- //

    /**
     * Sends JDWP command packet.
     */
    public void sendCommand(CommandPacket packet, String commandName) {
        try {
            transport.write(packet);
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            complain("Caught IOException while sending command packet for "
                    + commandName + ":\n\t" + e);
            display("Command packet:\n" + packet);
            throw new Failure("Error occured while sending command: " + commandName);
        }
    }

    /**
     * Receive next JDWP packet.
     */
/*
    public Packet receivePacket() {
        try {
            ReplyPacket packet = new ReplyPacket();
            transport.read(packet);
            return packet;
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught IOException while receiving reply packet:\n\t" + e);
        }
    }
 */
    /**
     * Receive next JDWP reply packet.
     */
    public ReplyPacket receiveReply() {
        try {
            for (;;) {
                Packet packet = new Packet();
                transport.read(packet);

                if (packet.getFlags() == JDWP.Flag.REPLY_PACKET) {
                    ReplyPacket reply = new ReplyPacket(packet);
                    return reply;
                }

                EventPacket event = new EventPacket(packet);
                display("Placing received event packet into queue");
                eventQueue.add(event);
            }
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught IOException while receiving reply packet:\n\t" + e);
        }
    }

    /**
     * Get next JDWP event packet by reading from transport or getting stored
     * event in the event queue.
     */
    public EventPacket getEventPacket() throws IOException {
        // check events queue first
        if (!eventQueue.isEmpty()) {
            EventPacket event = (EventPacket)(eventQueue.removeFirst());
            return event;
        }

        // read from transport
        Packet packet = new Packet();
        transport.read(packet);

        EventPacket event = new EventPacket(packet);
        return event;
    }

    /**
     * Get next JDWP event packet by reading from transport for specified timeout
     * or getting stored event in the event queue.
     */
    public EventPacket getEventPacket(long timeout) throws IOException {
        transport.setReadTimeout(timeout);
        return getEventPacket();
    }

    /**
     * Receive next JDWP event packet.
     */
    public EventPacket receiveEvent() {
        EventPacket packet = null;
        try {
            packet = getEventPacket();
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught IOException while receiving event packet:\n\t" + e);
        }

        if (packet.getFlags() == JDWP.Flag.REPLY_PACKET) {
            ReplyPacket reply = new ReplyPacket(packet);
            log.complain("Unexpected reply packet received with id: "
                        + reply.getPacketID());
            log.display("Reply packet:\n" + reply);
            throw new Failure("Unexpected reply packet received instead of event packet");
        }

        return packet;
    }

    /**
     * Send specified command packet, receive and check reply packet.
     *
     * @throws Failure if exception caught in sending and reading packets
     */
    public ReplyPacket receiveReplyFor(CommandPacket command) {
        return receiveReplyFor(command, Packet.toHexString(command.getCommand(), 4));
    }

    /**
     * Send specified command packet, receive and check reply packet.
     *
     * @throws Failure if exception caught in sending and reading packets
     */
    public ReplyPacket receiveReplyFor(CommandPacket command, String commandName) {
        ReplyPacket reply = null;
        sendCommand(command, commandName);
        reply = receiveReply();
        try {
            reply.checkHeader(command.getPacketID());
        } catch (BoundException e) {
            complain("Wrong header of reply packet for command "+ commandName + ":\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Wrong reply packet received for command: " + commandName);
        }
        return reply;
    }

    /**
     * Receive and check event packet for specified event kind.
     *
     * @throws Failure if exception caught in sending and reading packets
     */
    public EventPacket receiveEventFor(int eventKind, String eventName) {
        EventPacket event = null;
        event = receiveEvent();
        try {
            event.checkHeader(eventKind);
        } catch (BoundException e) {
            complain("Wrong header of event packet for expected "+ eventName + " event:\n\t"
                    + e.getMessage());
            display("Event packet:\n" + event);
            throw new Failure("Wrong event packet received for expected event: " + eventName);
        }
        return event;
    }

    // --------------------------------------------------- //

    /**
     * Check common VM capability.
     */
    public boolean getCapability(int capability, String name) {
        String commandName = "VirtualMachine.Capabilities";

        int count = JDWP.Capability.CAN_GET_MONITOR_INFO + 1;
        if (capability < 0 || capability >= count) {
            throw new TestBug("Illegal capability number (" + capability
                            + ") while checking for VM capability: " + name);
        }

        CommandPacket command =
            new CommandPacket(JDWP.Command.VirtualMachine.Capabilities);
        ReplyPacket reply = receiveReplyFor(command, commandName);

        try {
            reply.resetPosition();

            for (int i = 0; i < count; i++) {
                byte value = reply.getByte();
                if (i == capability) {
                    return (value != 0);
                }
            }

        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting VM capability: "
                                + name);
        }

        throw new TestBug("Illegal capability number (" + capability
                        + ") while checking for VM capability: " + name);
    }

    /**
     * Check new VM capability (since JDWP version 1.4).
     */
    public boolean getNewCapability(int capability, String name) {
        String commandName = "VirtualMachine.CapabilitiesNew";
        int count = JDWP.Capability.CAN_SET_DEFAULT_STRATUM + 1;

        if (capability < 0 || capability >= count) {
            throw new TestBug("Illegal capability number (" + capability
            + ") while checking for VM new capability: " + name);
        }

        CommandPacket command =
            new CommandPacket(JDWP.Command.VirtualMachine.CapabilitiesNew);
        ReplyPacket reply = receiveReplyFor(command, commandName);

        try {
            reply.resetPosition();

            for (int i = 0; i < count; i++) {
                byte value = reply.getByte();
                if (i == capability) {
                    return (value != 0);
                }
            }
        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting VM new capability: "
                                + name);
        }

        throw new TestBug("Illegal capability number (" + capability
                        + ") while checking for VM new capability: " + name);
    }

    // --------------------------------------------------- //

    /**
     * Return ReferenceTypeID for requested class by given signature.
     */
    public long getReferenceTypeID(String classSignature) {
        String commandName = "VirtualMachine.ClassesBySignature";
        CommandPacket command =
            new CommandPacket(JDWP.Command.VirtualMachine.ClassesBySignature);
        command.addString(classSignature);
        command.setLength();
        ReplyPacket reply = receiveReplyFor(command, commandName);

        long typeID = 0;

        try {
            reply.resetPosition();

            int classes = reply.getInt();
            for (int i = 0; i < classes; i++) {
                byte refTypeTag = reply.getByte();
                typeID = reply.getReferenceTypeID();
                int status = reply.getInt();
            }

            if (classes < 0) {
                throw new Failure("Negative number (" + classes
                                + ") of referenceTypeIDs received for signature: "
                                + classSignature);
            }

            if (classes == 0) {
                throw new Failure("No any referenceTypeID received for signature: "
                                + classSignature);
            }

            if (classes > 1) {
                throw new Failure("Too many (" + classes
                                + ") referenceTypeIDs received for signature: "
                                + classSignature);
            }

        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting referenceTypeID for signature: "
                                + classSignature);
        }

        return typeID;
    }

    // --------------------------------------------------- //


    /**
     * Get list of IDs of supertypes (interfaces and classes) for given class.
     */
    public long[] getSupertypes(long classID, boolean declared) {
        Vector<Long> vector = new Vector<Long>();
        addSupertypes(classID, vector, null, null, false, declared);
        return makeListOfLongValues(vector);
    }

    /**
     * Get list of IDs of superclasses for given class.
     */
    public long[] getSuperclasses(long classID, boolean declared) {
        Vector<Long> vector = new Vector<Long>();
        addSupertypes(classID, null, null, vector, false, declared);
        return makeListOfLongValues(vector);
    }

    /**
     * Get list of IDs of implemented interfaces for given class.
     */
    public long[] getImplementedInterfaces(long classID, boolean declared) {
        Vector<Long> vector = new Vector<Long>();
        addSupertypes(classID, null, vector, null, false, declared);
        return makeListOfLongValues(vector);
    }

    /**
     * Get list of IDs of superinterfaces for given interface.
     */
    public long[] getSuperinterfaces(long interfaceID, boolean declared) {
        Vector<Long> vector = new Vector<Long>();
        addSupertypes(interfaceID, null, vector, null, true, declared);
        return makeListOfLongValues(vector);
    }

    // --------------------------------------------------- //

    /**
     * Get list of IDs of methods of given class.
     */
    public long[] getMethodIDs(long classID, boolean declared) {
        Vector<Long> list = new Vector<Long>();
        addMethods(classID, list, null, null, null, false, declared);
        return makeListOfLongValues(list);
    }

    /**
     * Get list of names of methods of given class.
     */
    public String[] getMethodNames(long classID, boolean declared) {
        Vector<String> list = new Vector<String>();
        addMethods(classID, null, list, null, null, false, declared);
        return makeListOfStringValues(list);
    }

    /**
     * Get list of signatures of methods of given class.
     */
    public String[] getMethodSignatures(long classID, boolean declared) {
        Vector<String> list = new Vector<String>();
        addMethods(classID, null, null, list, null, false, declared);
        return makeListOfStringValues(list);
    }

    /**
     * Get ID of a method of given class by name.
     */
    public long getMethodID(long classID, String name, boolean declared) {
        Vector<Long> IDs = new Vector<Long>();
        Vector<String> names = new Vector<String>();
        addMethods(classID, IDs, names, null, null, false, declared);
        int count = names.size();
        for (int i = 0; i < count; i++) {
            if (name.equals(names.elementAt(i))) {
                return (IDs.elementAt(i)).longValue();
            }
        }
        throw new Failure("Method \"" + name + "\" not found for classID: " + classID);
    }

    // --------------------------------------------------- //

    /**
     * Get list of IDs of static fields of given class.
     */
    public long[] getClassFieldIDs(long classID, boolean declared) {
        Vector<Long> list = new Vector<Long>();
        addFields(classID, list, null, null, null, false, declared);
        return makeListOfLongValues(list);
    }

    /**
     * Get list of names of static fields of given class.
     */
    public String[] getClassFieldNames(long classID, boolean declared) {
        Vector<String> list = new Vector<String>();
        addFields(classID, null, list, null, null, false, declared);
        return makeListOfStringValues(list);
    }

    /**
     * Get list of signatures of static fields of given class.
     */
    public String[] getClassFieldSignatures(long classID, boolean declared) {
        Vector<String> list = new Vector<String>();
        addFields(classID, null, null, list, null, false, declared);
        return makeListOfStringValues(list);
    }

    /**
     * Get ID of a static field of given class by name.
     */
    public long getClassFieldID(long classID, String name, boolean declared) {
        Vector<Long> IDs = new Vector<Long>();
        Vector<String> names = new Vector<String>();
        addFields(classID, IDs, names, null, null, false, declared);
        int count = names.size();
        for (int i = 0; i < count; i++) {
            if (name.equals((String)names.elementAt(i))) {
                return ((Long)IDs.elementAt(i)).longValue();
            }
        }
        throw new Failure("Static field \"" + name + "\" not found for classID: " + classID);
    }

    // --------------------------------------------------- //

    /**
     * Get value of a static field of given class.
     */
    public JDWP.Value getStaticFieldValue(long typeID, long fieldID) {
        String commandName = "ReferenceType.GetValues";
        CommandPacket command =
            new CommandPacket(JDWP.Command.ReferenceType.GetValues);
        command.addReferenceTypeID(typeID);
        command.addInt(1);
        command.addFieldID(fieldID);

        ReplyPacket reply = receiveReplyFor(command, commandName);
        JDWP.Value value = null;

        try {
            reply.resetPosition();

            int count = reply.getInt();
            if (count < 1) {
                throw new Failure("No values returned for static fieldID: " + fieldID);
            }
            value = reply.getValue();

        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName +" command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting value of static field: " +
                                + fieldID);
        }
        return value;
    }

    /**
     * Get value of particular type from a static field of given class.
     */
    public JDWP.Value getStaticFieldValue(long typeID, String fieldName, byte tag) {
        long fieldID = getClassFieldID(typeID, fieldName, true);
        JDWP.Value value = getStaticFieldValue(typeID, fieldID);

        if (value.getTag() != tag) {
            complain("unexpedted value tag returned from debuggee: " + value.getTag()
                        + " (expected: " + tag + ")");
            throw new Failure("Error occured while getting value from static field: "
                            + fieldName);
        }
        return value;
    }

    /**
     * Set value of a static field of given class.
     */
    public void setStaticFieldValue(long typeID, long fieldID, JDWP.Value value) {
        String commandName = "ClassType.SetValues";
        CommandPacket command =
            new CommandPacket(JDWP.Command.ClassType.SetValues);
        command.addReferenceTypeID(typeID);
        command.addInt(1);
        command.addFieldID(fieldID);
        command.addUntaggedValue(value, value.getTag());

        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    /**
     * Get value of a field of given object.
     */
    public JDWP.Value getObjectFieldValue(long objectID, long fieldID) {
        String commandName = "ObjectReference.GetValues";
        CommandPacket command =
            new CommandPacket(JDWP.Command.ObjectReference.GetValues);
        command.addObjectID(objectID);
        command.addInt(1);
        command.addFieldID(fieldID);

        ReplyPacket reply = receiveReplyFor(command, commandName);
        JDWP.Value value = null;

        try {
            reply.resetPosition();

            int count = reply.getInt();
            if (count < 1) {
                throw new Failure("No values returned for object fieldID: " + fieldID);
            }
            value = reply.getValue();

        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting value of object field: " +
                                + fieldID);
        }
        return value;
    }

    /**
     * Set value of a static field of given class.
     */
    public void setObjectFieldValue(long objectID, long fieldID, JDWP.Value value) {
        String commandName = "ObjectReference.SetValues";
        CommandPacket command =
            new CommandPacket(JDWP.Command.ObjectReference.SetValues);
        command.addObjectID(objectID);
        command.addInt(1);
        command.addFieldID(fieldID);
        command.addUntaggedValue(value, value.getTag());

        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    // --------------------------------------------------- //

    /**
     * Find threadID for given thread name among all active threads.
     */
    public long getThreadID(String name) {
        // request list of all threadIDs
        int threads = 0;
        long threadIDs[] = null;
        {
            String commandName = "VirtualMachine.AllThreads";
            CommandPacket command = new CommandPacket(JDWP.Command.VirtualMachine.AllThreads);
            ReplyPacket reply = receiveReplyFor(command, commandName);
            reply.resetPosition();
            try {
                threads = reply.getInt();
                threadIDs = new long[threads];

                for (int i = 0; i < threads; i++) {
                    threadIDs[i] = reply.getObjectID();
                }
            } catch (BoundException e) {
                complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                        + e.getMessage());
                display("Reply packet:\n" + reply);
                throw new Failure("Error occured while getting threadID for thread name: "
                                    + name);
            }
        }

        // request name for each threadID
        for (int i = 0; i < threads; i++) {
            String commandName = "ThreadReference.Name";
            CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Name);
            command.addObjectID(threadIDs[i]);
            ReplyPacket reply = receiveReplyFor(command, commandName);
            try {
                reply.resetPosition();
                String threadName = reply.getString();
                if (threadName.equals(name)) {
                    return threadIDs[i];
                }
            } catch (BoundException e) {
                complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                        + e.getMessage());
                display("Reply packet:\n" + reply);
                throw new Failure("Error occured while getting name for threadID: "
                                + threadIDs[i]);
            }
        }

        throw new Failure("No threadID found for thread name: " + name);
    }

    /**
     * Return thread name for the given threadID.
     */
    public String getThreadName(long threadID) {
        String commandName = "ThreadReference.Name";
        CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Name);
        command.addObjectID(threadID);
        ReplyPacket reply = receiveReplyFor(command, commandName);
        try {
            reply.resetPosition();
            String threadName = reply.getString();
            return threadName;
        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting name for threadID: "
                            + threadID);
        }
    }

    /**
     * Suspend thread for the given threadID.
     */
    public void suspendThread(long threadID) {
        String commandName = "ThreadReference.Suspend";
        CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Suspend);
        command.addObjectID(threadID);

        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    /**
     * Resume thread for the given threadID.
     */
    public void resumeThread(long threadID) {
        String commandName = "ThreadReference.resume";
        CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Resume);
        command.addObjectID(threadID);

        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    /**
     * Return frameID for the current frame of the thread.
     * Thread must be suspended.
     */
    public long getCurrentFrameID(long threadID) {
        String commandName = "ThreadReference.Frames";
        CommandPacket command = new CommandPacket(JDWP.Command.ThreadReference.Frames);
        command.addObjectID(threadID);     // threadID
        command.addInt(0);                 // startFrame
        command.addInt(1);                 // length

        ReplyPacket reply = receiveReplyFor(command, commandName);
        try {
            reply.resetPosition();
            int frames = reply.getInt();
            if (frames != 1) {
                throw new Failure("Not only one current frame returned for threadID "
                                + threadID + ": " + frames);
            }
            long frameID = reply.getFrameID();
            JDWP.Location location = reply.getLocation();
            return frameID;
        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting current frame for threadID: "
                            + threadID);
        }
    }

    // --------------------------------------------------- //

    /**
     * Find line number for the given location from the method line table.
     * If <code>approximate</code> is <it>true</i> the found line should begin
     * with code index of the given location. Otherwise, the found line will
     * just cover code index of the given location.
     */
    public int getLineNumber(JDWP.Location location, boolean approximate) {
        String commandName = "Method.LineTable";
        CommandPacket command = new CommandPacket(JDWP.Command.Method.LineTable);
        command.addReferenceTypeID(location.getClassID());
        command.addMethodID(location.getMethodID());
        long index = location.getIndex();
        ReplyPacket reply = receiveReplyFor(command, commandName);
        String msg = "Error occured while getting line number for location: " + location;
        try {
            reply.resetPosition();
            long start = reply.getLong();
            if (index < start) {
                complain("Location index (" + index
                        + ") is less than start method index (" + start);
                throw new Failure(msg);
            }
            long end = reply.getLong();
            if (index > end) {
                complain("Location index (" + index
                        + ") is greater than end method index (" + end);
                throw new Failure(msg);
            }
            int lines = reply.getInt();
            if (!approximate) {
                for (int i = 0; i < lines; i++) {
                    long lineCodeIndex = reply.getLong();
                    int lineNumber = reply.getInt();
                    if (lineCodeIndex == index) {
                        return lineNumber;
                    }
                }
                throw new Failure("No exact line number exactly for location: " + location);
            } else {
                int prevLine = -1;
                for (int i = 0; i < lines; i++) {
                    long lineCodeIndex = reply.getLong();
                    int lineNumber = reply.getInt();
                    if (lineCodeIndex == index) {
                        return lineNumber;
                    } else if (lineCodeIndex > index) {
                        break;
                    }
                    prevLine = lineNumber;
                }
                if (prevLine < 0)
                    throw new Failure("No approximate line number found for location: " + location);
                return prevLine;
            }
        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure(msg);
        }
    }

    /**
     * Find line index for the given line number from the method line table.
     */
    public long getCodeIndex(long classID, long methodID, int lineNumber) {
        String commandName = "Method.LineTable";
        CommandPacket command = new CommandPacket(JDWP.Command.Method.LineTable);
        command.addReferenceTypeID(classID);
        command.addMethodID(methodID);
        ReplyPacket reply = receiveReplyFor(command, commandName);
        String msg = "Error occured while getting code index for line number: " + lineNumber;
        try {
            reply.resetPosition();
            long start = reply.getLong();
            long end = reply.getLong();
            int lines = reply.getInt();
            for (int i = 0; i < lines; i++) {
                long lineCodeIndex = reply.getLong();
                int line = reply.getInt();
                if (lineNumber == line) {
                    return lineCodeIndex;
                }
            }
        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure(msg);
        }

        throw new Failure("No code index found for line number: " + lineNumber);
    }

    // --------------------------------------------------- //

    /**
     * Make the specified event request into debuggee.
     */
    public int requestEvent(CommandPacket requestCommand, String name) {
        String commandName = "EventRequest.Set";
        ReplyPacket reply = receiveReplyFor(requestCommand, name);

        try {
            reply.resetPosition();

            int requestID = reply.getInt();
            return requestID;

        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Request command packet:\n" + requestCommand);
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while making event request: " + name);
        }
    }

    /**
     * Remove existing event request from debuggee.
     */
    public void clearEventRequest(byte eventKind, int requestID) {
        String commandName = "EventRequest.Clear";
        CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.Clear);
        command.addByte(eventKind);
        command.addInt(requestID);
        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    /**
     * Make request for CLASS_PREPARE event for specified class into debuggee.
     */
    public int requestClassPrepareEvent(String className, byte suspendPolicy) {
        CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.Set);
        command.addByte(JDWP.EventKind.CLASS_PREPARE);
        command.addByte(suspendPolicy);
        command.addInt(1);
            command.addByte(JDWP.EventModifierKind.CLASS_MATCH);
                command.addString(className);

        return requestEvent(command, "CLASS_PREPARE");
    }

    /**
     * Make request for BREAKPOINT event for the specified location into debuggee.
     */
    public int requestBreakpointEvent(JDWP.Location location, byte suspendPolicy) {
        CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.Set);
        command.addByte(JDWP.EventKind.BREAKPOINT);
        command.addByte(suspendPolicy);
        command.addInt(1);
            command.addByte(JDWP.EventModifierKind.LOCATION_ONLY);
                command.addLocation(location);
        return requestEvent(command, "BREAKPOINT");
    }

    /**
     * Make request for BREAKPOINT event for the specified line of the given method.
     */
    public int requestBreakpointEvent(byte typeTag, long classID, long methodID,
                                            int lineNumber, byte suspendPolicy) {
        long codeIndex = getCodeIndex(classID, methodID, lineNumber);
        JDWP.Location location = new JDWP.Location(typeTag, classID, methodID, codeIndex);
        return requestBreakpointEvent(location, suspendPolicy);
    }

    /**
     * Remove all existing BREAKPOINT event requests from debuggee.
     */
    public void clearAllBreakpoints() {
        String commandName = "EventRequest.ClearAllBreakpoints";
        CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.ClearAllBreakpoints);
        ReplyPacket reply = receiveReplyFor(command, commandName);
    }

    // --------------------------------------------------- //

    /**
     * Add IDs of supertypes (interfaces and classes) for given class to the lists.
     */
    private void addSupertypes(long referenceTypeID, Vector<Long> supertypes,
                                Vector<Long> interfaces, Vector<Long> superclasses,
                                boolean interfaceOnly, boolean declared) {

        if (supertypes != null || interfaces != null) {

            // obtain list of direct implemented interfaces
            String commandName = "ReferenceType.Interfaces";
            CommandPacket command = new CommandPacket(JDWP.Command.ReferenceType.Interfaces);
            command.addReferenceTypeID(referenceTypeID);
            ReplyPacket reply = receiveReplyFor(command, commandName);

            try {
                reply.resetPosition();

                int count = reply.getInt();
                if (count < 0) {
                    throw new Failure("Negative number (" + count
                                    + ") of declared interfaces received for referenceTypeID: "
                                    + referenceTypeID);
                }

                for (int i = 0; i < count; i++) {
                    long typeID = reply.getReferenceTypeID();
                    if (!declared) {
                        addSupertypes(typeID, supertypes, interfaces, superclasses,
                                        true, declared);
                    }
                    Long value = Long.valueOf(typeID);
                    if (supertypes != null) {
                        supertypes.add(value);
                    }
                    if (interfaces != null) {
                        interfaces.add(value);
                    }
                }

            } catch (BoundException e) {
                complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                        + e.getMessage());
                display("Reply packet:\n" + reply);
                throw new Failure("Error occured while getting interfeceIDs for referenceTypeID: " +
                                    + referenceTypeID);
            }

        }

        if (!interfaceOnly) {

            String commandName = "ClassType.Superclasses";
            CommandPacket command = new CommandPacket(JDWP.Command.ClassType.Superclass);
            command.addReferenceTypeID(referenceTypeID);
            ReplyPacket reply = receiveReplyFor(command, commandName);

            try {
                reply.resetPosition();

                long typeID = reply.getReferenceTypeID();
                if (typeID != 0) {
                    if (!declared) {
                        addSupertypes(typeID, supertypes, interfaces, superclasses,
                                    false, declared);
                    }
                    Long value = Long.valueOf(typeID);
                    if (supertypes != null) {
                        supertypes.add(value);
                    }
                    if (superclasses != null) {
                        superclasses.add(value);
                    }
                }
            } catch (BoundException e) {
                complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                        + e.getMessage());
                display("Reply packet:\n" + reply);
                throw new Failure("Error occured while getting superclass ID for classID: " +
                                    + referenceTypeID);
            }

        }
    }

    /**
     * Add attributes of fields of given class to the lists.
     */
    private void addFields(long referenceTypeID, Vector<Long> IDs, Vector<String> names,
                                Vector<String> signatures, Vector<Integer> modifiers,
                                boolean interfaceOnly, boolean declared) {

        if (!declared) {
            Vector<Long> supertypes = new Vector<Long>();
            addSupertypes(referenceTypeID, supertypes, null, null, interfaceOnly, declared);
            int count = supertypes.size();
            for (int i = 0; i < count; i++) {
                long typeID = (supertypes.elementAt(i)).longValue();
                addFields(typeID, IDs, names, signatures, modifiers,
                            interfaceOnly, declared);
            }
        }

        String commandName = "ReferenceType.Fields";
        CommandPacket command = new CommandPacket(JDWP.Command.ReferenceType.Fields);
        command.addReferenceTypeID(referenceTypeID);
        ReplyPacket reply = receiveReplyFor(command, commandName);

        try {
            reply.resetPosition();

            int count = reply.getInt();
            if (count < 0) {
                throw new Failure("Negative number (" + count
                                + ") of declared fields received for referenceTypeID: "
                                + referenceTypeID);
            }

            for (int i = 0; i < count; i++) {
                long id = reply.getFieldID();
                String name = reply.getString();
                String signature = reply.getString();
                int modBits = reply.getInt();

                if (IDs != null)
                    IDs.add(Long.valueOf(id));
                if (names != null)
                    names.add(name);
                if (signatures != null)
                    signatures.add(signature);
                if (modifiers != null)
                    modifiers.add(Integer.valueOf(modBits));
            }

        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting fieldIDs for referenceTypeID: " +
                                + referenceTypeID);
        }
    }

    /**
     * Add attributes of methods of given class to the lists.
     */
    private void addMethods(long referenceTypeID, Vector<Long> IDs, Vector<String> names,
                                Vector<String> signatures, Vector<Integer> modifiers,
                                boolean interfaceOnly, boolean declared) {

        if (!declared) {
            Vector<Long> supertypes = new Vector<Long>();
            addSupertypes(referenceTypeID, supertypes, null, null, interfaceOnly, declared);
            int count = supertypes.size();
            for (int i = 0; i < count; i++) {
                long typeID = (supertypes.elementAt(i)).longValue();
                addMethods(typeID, IDs, names, signatures, modifiers,
                            interfaceOnly, declared);
            }
        }

        String commandName = "ReferenceType.Methods";
        CommandPacket command = new CommandPacket(JDWP.Command.ReferenceType.Methods);
        command.addReferenceTypeID(referenceTypeID);
        ReplyPacket reply = receiveReplyFor(command, commandName);

        try {
            reply.resetPosition();

            int count = reply.getInt();
            if (count < 0) {
                throw new Failure("Negative number (" + count
                                + ") of declared fields received for referenceTypeID: "
                                + referenceTypeID);
            }

            for (int i = 0; i < count; i++) {
                long id = reply.getMethodID();
                String name = reply.getString();
                String signature = reply.getString();
                int modBits = reply.getInt();

                if (IDs != null)
                    IDs.add(Long.valueOf(id));
                if (names != null)
                    names.add(name);
                if (signatures != null)
                    signatures.add(signature);
                if (modifiers != null)
                    modifiers.add(Integer.valueOf(modBits));
            }

        } catch (BoundException e) {
            complain("Unable to parse reply packet for " + commandName + " command:\n\t"
                    + e.getMessage());
            display("Reply packet:\n" + reply);
            throw new Failure("Error occured while getting methodIDs for referenceTypeID: " +
                                + referenceTypeID);
        }
    }

    // --------------------------------------------------- //

    private static long[] makeListOfLongValues(Vector<Long> vector) {
        int count = vector.size();
        long[] list = new long[count];
        for (int i = 0; i < count; i++) {
            list[i] = (vector.elementAt(i)).longValue();
        }
        return list;
    }

    private static String[] makeListOfStringValues(Vector<String> vector) {
        int count = vector.size();
        String[] list = new String[count];
        for (int i = 0; i < count; i++) {
            list[i] = vector.elementAt(i);
        }
        return list;
    }

    // --------------------------------------------------- //

    /**
     * Force debugge VM to exit using JDWP connection if possible.
     */
    protected void killDebugee() {
        // ignore
    }

    /**
     * Close transport channel and kill the debugee VM if it is not terminated yet.
     */
    public void close() {
        if (transport != null) {
            try {
                transport.close();
            } catch (IOException e) {
                log.display("WARNING: Caught IOException while closing JDWP connection:\n\t"
                            + e);
            }
        }
        super.close();
    }

}
