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
 * This class represents a JDWP reply packet.
 */
public class ReplyPacket extends Packet {

    /** Error code constant. */
//    public final static int errOk               = JDWP.Error.NONE;
    /** Error code constant. */
//    public final static int errWrongPacketSize  = 0x400;
    /** Error code constant. */
//    public final static int errNotAvailable     = 0x401;
    /** Error code constant. */
//    public final static int errEvent            = 0x4064;

    /**
     * Make empty reply packet.
     */
    public ReplyPacket() {
        super();
    }

    /**
     * Make reply packet with data from the specified byte buffer.
     */
//    public ReplyPacket(ByteBuffer packet) {
    public ReplyPacket(Packet packet) {
        super(packet);
    }

    /**
     * Return value of the "error code" field of JDWP reply packet.
     */
    public int getErrorCode() {

        int err = 0;
        try {
            err = (int) getID(ErrorCodeOffset, 2);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting error code from header:\n\t"
                            + e);
        }

        return err;
    }

    /**
     * Set value of the "error code" field of JDWP reply packet.
     */
    public void setErrorCode(long err) {
        try {
            putID(ErrorCodeOffset, err, 2);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while setting error code into header:\n\t"
                            + e);
        }
    }

    /**
     * Check reply packet header.
     * This method check if reply packet has valid values in the header fields.
     *
     * @throws PacketFormatException if packet header fields have error or invalid values
     */
/*
    protected void checkHeaderForReplyOrEvent() throws PacketFormatException {
        super.checkHeader();
    }
 */

    /**
     * Check reply packet header.
     * This method check if reply packet has valid values in the header fields.
     *
     * @throws PacketFormatException if packet header fields have error or invalid values
     */
    public void checkHeader() throws PacketFormatException {
//        checkHeaderForReplyOrEvent();
        super.checkHeader();
        if (getFlags() != JDWP.Flag.REPLY_PACKET) {
            throw new PacketFormatException("Unexpected flags in reply packet header: "
                                            + "0x" + toHexDecString(getFlags(), 2));
        }
        if (getErrorCode() != JDWP.Error.NONE) {
            throw new PacketFormatException("Unexpected error code in reply packet header: "
                                            + "0x" + toHexDecString(getErrorCode(), 4));
        }
    }

    /**
     * Check reply packet header for specified reply ID.
     * This method check if reply packet has valid values in the header fields.
     *
     * @throws PacketFormatException if packet header fields have error or invalid values
     */
    public void checkHeader(int id) throws PacketFormatException {
        checkHeader();
        if (getPacketID() != id) {
            throw new PacketFormatException("Unexpected ID in reply packet header: "
                                            + getPacketID());
        }
    }

    /**
     * Return string representation of the reply packet header.
     */
    public String headerToString() {
        String s;

        if (getFlags() == 0)
            s = " (command)";
        else
            s = " (error)  ";

        return super.headerToString()
             + "    " + toHexString(CommandOffset,  4) + s + ": 0x" + toHexDecString(getErrorCode(), 4) + "\n";
    }

}
