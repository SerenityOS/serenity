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

/**
 * This class represents a JDWP command packet.
 */
public class CommandPacket extends Packet {

    /**
     * Static counter for enumeration of the command packets.
     */
    private static int nextID = 1;

    /**
     * Return next free number for enumeration of the command packets.
     */
    public static int getLastID() {
        return (nextID - 1);
    }

    /**
     * Make JDWP command packet for specified command.
     */
    public CommandPacket(int fullCommand) {
        super();

        setPacketID(nextID++);
        setFlags(JDWP.Flag.NONE);
        setFullCommand(fullCommand);
    }

    /**
     * Make JDWP command packet for specified command.
     */
    public CommandPacket(int fullCommand, int id) {
        super();

        setPacketID(id);
        setFlags(JDWP.Flag.NONE);
        setFullCommand(fullCommand);
    }

    /**
     * Make command packet with data from the specified byte buffer.
     */
//    public CommandPacket(ByteBuffer packet) {
    public CommandPacket(Packet packet) {
        super(packet);
    }

    /**
     * Return full command number for this packet.
     */
    public int getFullCommand() {
        int id = 0;

        try {
            id = (int) getID(FullCommandOffset, 2);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting command number from header:\n\t"
                            + e);
        }

        return id;
    }

    /**
     * Return short command number for this packet.
     */
    public byte getCommand() {
        byte id = 0;

        try {
            id = getByte(CommandOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting command number from header:\n\t"
                            + e);
        }

        return id;
    }

    /**
     * Return command set number for this packet.
     */
    public byte getCommandSet() {
        byte id = 0;

        try {
            id = getByte(CommandSetOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting command number from header:\n\t"
                            + e);
        }

        return id;
    }

    /**
     * Assign command number for this packet.
     */
    public void setFullCommand(int fullCommand) {
        try {
            putID(FullCommandOffset, fullCommand, 2);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while setting command number into header: "
                            + e);
        }
    }

    /**
     * Return string representation of the command packet header.
     */
    public String headerToString() {
        return super.headerToString()
             + "    " + toHexString(CommandSetOffset, 4) + " (cmd set): 0x" + toHexDecString(getCommandSet(), 2) + "\n"
             + "    " + toHexString(CommandOffset,    4) + " (command): 0x" + toHexDecString(getCommand(), 2) + "\n";
    }

}
