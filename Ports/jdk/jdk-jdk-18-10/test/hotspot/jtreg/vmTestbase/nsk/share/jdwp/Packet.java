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
import java.io.*;

/**
 * This class represents a JDWP packet.
 */
public class Packet extends ByteBuffer {

    /** JDWP packet flags constant. */
//    public final static byte flNoFlags          = (byte)0x0;
    /** JDWP packet flags constant. */
//    public final static byte flReply            = (byte)0x80;

    /** Offset of "length" field of JDWP packet. */
    public final static int LengthOffset        = 0;
    /** Offset of "id" field of JDWP packet. */
    public final static int IdOffset            = LengthOffset + 4;
    /** Offset of "flags" field of JDWP packet. */
    public final static int FlagsOffset         = IdOffset + 4;
    /** Offset of full "command" field of JDWP packet. */
    public final static int FullCommandOffset   = FlagsOffset + 1;
    /** Offset of "command" field of JDWP command packet. */
    public final static int CommandSetOffset    = FullCommandOffset;
    /** Offset of "command" field of JDWP command packet. */
    public final static int CommandOffset       = CommandSetOffset + 1;
    /** Offset of "error" field of JDWP reply packet. */
    public final static int ErrorCodeOffset     = FlagsOffset + 1;
    /** Offset of "data" section of JDWP packet. */
    public final static int DataOffset          = FullCommandOffset + 2;

    /** Size of JDWP packet header. */
    public final static int PacketHeaderSize    = DataOffset;

    /**
     * Makes empty JDWP packet.
     */
    public Packet() {
        super();
        resetBuffer();
    }

    /**
     * Makes JDWP packet with data from the specified byte buffer.
     */
//    public Packet(ByteBuffer packet) {
    public Packet(Packet packet) {
        super(packet);
        resetPosition();
    }

    /**
     * Clear buffer of the packet.
     */
    public void resetBuffer() {
        super.resetBuffer();
        while (length() < PacketHeaderSize)
            addByte((byte) 0);
        setLength();
        resetPosition();
    }

    /**
     * Return current position from begin of packet data area.
     */
    public int currentDataPosition() {
        return currentPosition() - PacketHeaderSize;
    }

    /**
     * Return value to the "length" field of JDWP packet.
     */
    public int getLength() {
        try {
            return getInt(LengthOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting packet length value from header:\n\t"
                            + e);
        }
    }

    /**
     * Assign specified value to the "length" field of JDWP packet.
     */
    public void setLength(int length) {
        try {
            putInt(LengthOffset, length);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while setting packet length value into header:\n\t"
                            + e);
        }
    }

    /**
     * Assign packet length value to the "length" field of JDWP packet.
     */
    public void setLength() {
        setLength(length());
    }

    /**
     * Return value of the "id" field of JDWP packet.
     */
    public int getPacketID() {
        try {
            return getInt(IdOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting packet ID value from header:\n\t"
                            + e);
        }
    }

    /**
     * Assign value to the "id" field of JDWP packet.
     */
    public void setPacketID(int Id) {
        try {
            putInt(IdOffset, Id);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while setting packet ID value into header:\n\t"
                            + e);
        }
    }

    /**
     * Return value of the "flags" field of JDWP packet.
     */
    public byte getFlags() {
        try {
            return getByte(FlagsOffset);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while getting packet flags value from header:\n\t"
                            + e);
        }
    }

    /**
     * Assign value to the "flags" field of JDWP packet.
     */
    public void setFlags(byte flags) {
        try {
            putByte(FlagsOffset, flags);
        }
        catch (BoundException e) {
            throw new Failure("Caught unexpected exception while setting packet flags value into header:\n\t"
                            + e);
        }
    }

    /**
     * Sets the current parser position to "data" field of JDWP packet.
     */
    public void resetPosition() {
        resetPosition(PacketHeaderSize);
    }

    /**
     * Return size of the "data" part of JDWP packet.
     */
    public int getDataSize() {
        return length() - PacketHeaderSize;
    }

    //////////////////////////////////////////////////////////////////////////

    /**
     * Append fieldID to the end of this buffer.
     */
    public void addFieldID(long b) {
        addID(b, JDWP.TypeSize.FIELD_ID);
    }

    /**
     * Append methodID to the end of this buffer.
     */
    public void addMethodID(long b) {
        addID(b, JDWP.TypeSize.METHOD_ID);
    }

    /**
     * Append objectID to the end of this buffer.
     */
    public void addObjectID(long b) {
        addID(b, JDWP.TypeSize.OBJECT_ID);
    }

    /**
     * Append referenceID to the end of this buffer.
     */
    public void addReferenceTypeID(long b) {
        addID(b, JDWP.TypeSize.REFERENCE_TYPE_ID);
    }

    /**
     * Append frameID to the end of this buffer.
     */
    public void addFrameID(long b) {
        addID(b, JDWP.TypeSize.FRAME_ID);
    }

    /**
     * Append string value (an UTF-8 encoded string, not zero terminated,
     * preceded by a four-byte integer length) to the end of this buffer.
     */
    public void addString(String value) {
        final int count = JDWP.TypeSize.INT + value.length();
        addInt(value.length());
        try {
            addBytes(value.getBytes("UTF-8"), 0, value.length());
        } catch (UnsupportedEncodingException e) {
            throw new Failure("Unsupported UTF-8 ecnoding while adding string value to JDWP packet:\n\t"
                                + e);
        }
    }

    /**
     * Append location value to the end of this buffer.
     */
    public void addLocation(JDWP.Location location) {
        addBytes(location.getBytes(), 0, location.length());
    }

    /**
     * Append untagged value to the end of this buffer.
     */
    public void addUntaggedValue(JDWP.UntaggedValue value, byte tag) {
        value.addValueTo(this, tag);
    }

    /**
     * Append tagged value to the end of this buffer.
     */
    public void addValue(JDWP.Value value) {
        value.addValueTo(this);
    }

    //////////////////////////////////////////////////////////////////////////
    // get packet data
    //////////////////////////////////////////////////////////////////////////

    /**
     * Read a fieldID value from byte buffer at the current parser position.
     *
     * @throws BoundException if there is no valid value bytes at the given position
     */
    public long getFieldID() throws BoundException {
        return getID(JDWP.TypeSize.FIELD_ID);
    }

    /**
     * Read a methodID value from byte buffer at the current parser position.
     *
     * @throws BoundException if there is no valid value bytes at the given position
     */
    public long getMethodID() throws BoundException {
        return getID(JDWP.TypeSize.METHOD_ID);
    }

    /**
     * Read an objectID value from byte buffer at the current parser position.
     *
     * @throws BoundException if there is no valid value bytes at the given position
     */
    public long getObjectID() throws BoundException {
        return getID(JDWP.TypeSize.OBJECT_ID);
    }

    /**
     * Read a referenceTypeID value from byte buffer at the current parser position.
     *
     * @throws BoundException if there is no valid value bytes at the given position
     */
    public long getReferenceTypeID() throws BoundException {
        return getID(JDWP.TypeSize.REFERENCE_TYPE_ID);
    }

    /**
     * Read a frameID value from byte buffer at the current parser position.
     *
     * @throws BoundException if there is no valid value bytes at the given position
     */
    public long getFrameID() throws BoundException {
        return getID(JDWP.TypeSize.FRAME_ID);
    }

    /**
     * Read from this buffer a string value at the current parser
     * position and returns this value.
     *
     * @throws BoundException if there are no valid string bytes in the buffer
     */
    public String getString() throws BoundException {
        final int count = JDWP.TypeSize.INT;
        int available = length() - currentPosition();
        if (count > available) {
            throw new BoundException("Unable to get " + count + " bytes of string length value at " +
                                      offsetString() + " (available bytes: " + available + ")" );
        }

        int len = getInt();

        if (len < 0)
            throw new BoundException("Negative length of string to get: " + len);

        if (len == 0)
            return "";

        available = length() - currentPosition();
        if (len > available) {
            throw new BoundException("Unable to get " + len + " bytes of string value at " +
                                     offsetString() + " (available bytes: " + available + ")" );
        }

        byte[] s = new byte[len];
        for (int i = 0; i < len; i++)
            s[i] = getByte();

        try {
            return new String(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Failure("Unsupported UTF-8 ecnoding while extracting string value from JDWP packet:\n\t"
                                + e);
        }
    }

    /**
     * Reads from this buffer an location value at the current parser
     * position and returns this value.
     *
     * @throws BoundException if there are no enough bytes in the buffer
     */
    public JDWP.Location getLocation() throws BoundException {
        final int count = JDWP.TypeSize.LOCATION;
        final int available = length() - currentPosition();
        if (count > available) {
            throw new BoundException("Unable to get " + count + " bytes of location value at " +
                                     offsetString() + " (available bytes: " + available + ")" );
        }

        JDWP.Location location = new JDWP.Location();
        try {
            for (int i = 0; i < JDWP.TypeSize.LOCATION; i++) {
                location.putByte(i, getByte());
            }
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of location value:\n\t" + e);
        };

        return location;
    }

    /**
     * Reads from this buffer an untagged value at the current parser
     * position and returns this value.
     *
     * @throws BoundException if there are no enough bytes in the buffer
     */
    public JDWP.UntaggedValue getUntaggedValue(byte tag) throws BoundException {
        JDWP.UntaggedValue value = new JDWP.UntaggedValue();
        try {
            value.getValueFrom(this, tag);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               " bytes of a value:\n\t" + e);
        };

        return value;
    }

    /**
     * Reads from this buffer a tagged value at the current parser
     * position and returns this value.
     *
     * @throws BoundException if there are no enough bytes in the buffer
     */
    public JDWP.Value getValue() throws BoundException {
        JDWP.Value value = new JDWP.Value();
        try {
            value.getValueFrom(this);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               " bytes of a value:\n\t" + e);
        };

        return value;
    }


    ////////////////////////////////////////////////////////////////

    /**
     * Read packet bytes from the stream.
     *
     * @throws IOException if error occured when reading bytes
     */
    public void readFrom(Transport transport) throws IOException {
        resetBuffer();

//        System.err.println("Reading packet header");
        try {
            for (int i = 0; i < PacketHeaderSize; i++) {
                byte b = transport.read();
                putByte(i, b);
            }
        }
        catch (BoundException e) {
            throw new TestBug(e);
        }

        int length = getLength();

        checkSpace(length - PacketHeaderSize);

//        System.err.println("Packet size: " + length);
        for (int i = PacketHeaderSize; i < length; i++) {
            byte b = transport.read();
            addByte(b);
        }
//        System.err.println("Packet read successfully");
    }

    /**
     * Write packet bytes to the stream.
     *
     * @throws IOException if error occured when reading bytes
     */
    public void writeTo(Transport transport) throws IOException {
        setLength();
//        System.err.println("Writing packet bytes: " + length());
        transport.write(bytes, 0, length());
    }

    /**
     * Check packet header.
     * This method check if packet has valid values in header fields.
     *
     * @throws PacketFormatException if packet header fields has error or invalid values
     */
    public void checkHeader() throws PacketFormatException {
        if (getLength() != length()) {
            throw new PacketFormatException("Unexpected packet length value in the header:"
                                            + getLength());
        }
    }

    /**
     * Check if packet is parsed totally and no trailing bytes left unparsed.
     * This method check if packet has valid values in header fields.
     *
     * @throws PacketFormatException if there are trailing bytes left unparsed
     */
    public void checkParsed() throws PacketFormatException {
        if (! isParsed()) {
            throw new PacketFormatException("Extra trailing bytes found in the packet at: "
                                            + offsetString());
        }
    }

    /**
     * Return string representation of the packet header.
     */
    public String headerToString() {
        return "Packet header (" + PacketHeaderSize + " bytes):" + "\n"
             + "    " + toHexString(LengthOffset, 4) + " (length) : 0x" + toHexDecString(getLength(), 8) + "\n"
             + "    " + toHexString(IdOffset,     4) + " (id)     : 0x" + toHexDecString(getPacketID(), 8) + "\n"
             + "    " + toHexString(FlagsOffset,  4) + " (flags)  : 0x" + toHexDecString(getFlags(), 2) + "\n";
    }

    /**
     * Return string representation of the packet.
     */
    public String toString() {
        return headerToString()
             + "Entire packet (" + length() + " bytes): " + "\n"
             + super.toString(0)
             + "Packet end";
    }

    /**
     * Exception indicated that packet has an ivnalid structure.
     */
    class PacketFormatException extends BoundException {
        PacketFormatException(String message) {
            super(message);
        }
    }
}
