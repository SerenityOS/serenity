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

package nsk.share.jdwp;

import nsk.share.*;

import java.util.Vector;

/**
 * This class represents a JDWP event packet.
 */
public class EventPacket extends CommandPacket {

    /** Offset of the "suspendPolicy" field in a JDWP event packet. */
    public final static int SuspendPolicyOffset = DataOffset;

    /** Offset of the "eventsCount" field in a JDWP event packet. */
    public final static int EventsCountOffset = SuspendPolicyOffset + JDWP.TypeSize.BYTE;

    /** Offset of the first "eventKind" field in a JDWP event packet. */
    public final static int FirstEventKindOffset = EventsCountOffset + JDWP.TypeSize.INT;

    /**
     * Make an empty event packet.
     */
    public EventPacket() {
        super(JDWP.Command.Event.Composite, 0);
    }

    /**
     * Make event packet with data from the specified byte buffer.
     */
//    public EventPacket(ByteBuffer packet) {
    public EventPacket(Packet packet) {
        super(packet);
    }

    /**
     * Return suspend policy of the events in the packet.
     *
     * throws BoundException if event packet structure is not valid
     */
    public byte getSuspendPolicy() {
        try {
            return getByte(SuspendPolicyOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting event kind from header:\n\t"
                            + e);
        }
    }

    /**
     * Return number of events in the packet.
     *
     * throws BoundException if event packet structure is not valid
     */
    public int getEventsCount() {
        try {
            return getInt(EventsCountOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting event kind from header:\n\t"
                            + e);
        }
    }

    /**
     * Return constant indicates kind of the first event in the packet.
     *
     * throws BoundException if event packet structure is not valid
     */
    public int getEventKind() {
        try {
            return getByte(FirstEventKindOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting event kind from header:\n\t"
                            + e);
        }
    }

    /**
     * Check event packet header.
     * This method check if event packet has valid values in the header fields.
     *
     * @throws PacketFormatException if packet header fields have error or invalid values
     */
    public void checkHeader() throws PacketFormatException {
        super.checkHeader();
        if (getFullCommand() != JDWP.Command.Event.Composite) {
            throw new PacketFormatException("Not Event.Composite command in the event packet header: "
                                            + "0x" + toHexDecString(getFullCommand(), 4) );
        }
        if (getFlags() != JDWP.Flag.EVENT_PACKET) {
            throw new PacketFormatException("Unexpected flags in the event packet header: "
                                            + "0x" + toHexDecString(getFlags(), 2));
        }
/*
        if (getPacketID() != 0) {
            throw new PacketFormatException("Non-zero packet ID in the event packet header: "
                                            + getPacketID());
        }
 */
    }

    /**
     * Check event packet header for only one event of specified kind.
     * This method check if event packet has valid values in the header fields.
     *
     * @throws PacketFormatException if packet header fields have error or invalid values
     */
    public void checkHeader(int eventKind) throws PacketFormatException {
        checkHeader();
        if (getEventsCount() != 1) {
            throw new PacketFormatException("Not a single event in the event packet: "
                                            + getEventsCount() + " events");
        }
        if (getEventKind() != eventKind) {
            throw new PacketFormatException("Unexpected event kind in the event packet: "
                                            + "0x" + toHexDecString(getEventKind(), 2));
        }
    }

    /**
     * Return string representation of the event packet header.
     */
    public String headerToString() {
        return super.headerToString()
             + "    " + toHexString(SuspendPolicyOffset,  4)  + " (policy):  0x" + toHexDecString(getSuspendPolicy(), 2) + "\n"
             + "    " + toHexString(EventsCountOffset,  4)    + " (events):  0x" + toHexDecString(getEventsCount(), 8) + "\n"
             + "    " + toHexString(FirstEventKindOffset,  4) + " (kind):    0x" + toHexDecString(getEventKind(), 2) + "\n";
    }

}
