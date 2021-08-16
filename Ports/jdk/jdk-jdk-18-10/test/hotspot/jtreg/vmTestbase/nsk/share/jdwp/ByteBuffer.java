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

import java.io.*;
import nsk.share.*;

/**
 * This class represents a byte buffer of variable size.
 */
public class ByteBuffer {

    /**
     * Empty byte value (zero).
     */
    private static final byte EMPTY_BYTE = (byte)0;

    /**
     * Current number of bytes in the buffer.
     */
    private int CurrentSize;

    /**
     * Delta to increase buffer size.
     */
    private int Delta;

    /**
     * Current offset from the buffer begin during parsing packet.
     */
    int parseOffset;

    /**
     * Array of bytes in the buffer.
     */
    protected byte[] bytes;

    /**
     * Make an empty <code>ByteBuffer</code> object.
     */
    public ByteBuffer() {
        this(128, 128);
    }

    /**
     * Make an empty <code>ByteBuffer</code> object with given initial capacity.
     * When there is no space for a new byte in a buffer it's capacity
     * grows by Delta.
     */
    public ByteBuffer(int InitialSize, int Delta) {
        if (Delta <= 0)
            Delta = 16;
        this.Delta = Delta;
        CurrentSize = 0;
        bytes = new byte[InitialSize];
        parseOffset = 0;
    }

    /**
     * Make a copy of specified byte buffer.
     */
    public ByteBuffer(ByteBuffer buffer) {
        int InitialSize = buffer.bytes.length;
        Delta = buffer.Delta;
        CurrentSize = buffer.CurrentSize;
        bytes = new byte[InitialSize];
        for (int i = 0; i < CurrentSize; i++ ) {
            bytes[i] = buffer.bytes[i];
        }
        parseOffset = 0;
    }

    /**
     * Return number of bytes in this buffer.
     */
    public int length() {
        return CurrentSize;
    }

    /**
     * Return array of bytes in this buffer.
     */
    public byte[] getBytes() {
        return bytes;
    }

    //////////////////////////////////////////////////////////////////////////

    /**
     * Replace the byte at the specified offset in this buffer with the
     * less significant byte from the int value.
     *
     * @throws BoundException if specified offset is out of buffer bounds
     */
    public void putByte(int off, byte value) throws BoundException {

        if ((off < 0) || (off >= CurrentSize))
            throw new BoundException("Unable to put one byte at " + offsetString(off));

        bytes[off] = value;
    }

    /**
     * Replace len bytes starting at offset off with the bytes from the
     * given byte array.
     *
     * @throws BoundException if offset and length are out of buffer bounds
     */
    public void putBytes(int off, byte[] value, int start, int len) throws BoundException {
        if (len > (CurrentSize - off)) {
            throw new BoundException("Unable to put " + len + " bytes at " + offsetString(off) +
                                     " (available bytes: " + (CurrentSize - off) + ")" );
        }
        try {
            for (int i = 0; i < len; i++)
                putByte(off++, value[start++]);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + len +
                              "bytes at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Replace count (1 - 8) bytes starting at offset off with the less
     * significant bytes from the specified ID value.
     *
     * @throws BoundException if offset and count are out of buffer bounds
     */
    public void putID(int off, long value, int count) throws BoundException {

        if ((count <= 0) || (count > 8))
            throw new TestBug("Illegal number of bytes of ID value to put: " + count);

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of ID value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            putValueBytes(off, value, count);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + count +
                              "bytes of ID value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Replace four bytes starting at offset off with the bytes from the
     * specified int value.
     *
     * @throws BoundException if offset is out of buffer bounds
     */
    public void putInt(int off, int value) throws BoundException {
        final int count = JDWP.TypeSize.INT;

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of int value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            putValueBytes(off, value, count);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + count +
                              "bytes of int value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Replace two bytes starting at offset off with the bytes
     * from the specified short value.
     *
     * @throws BoundException if offset is out of buffer bounds
     */
    public void putShort(int off, short value) throws BoundException {
        final int count = JDWP.TypeSize.SHORT;

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of short value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            putValueBytes(off, value, count);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + count +
                              "bytes of short value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Replace eight bytes starting at offset off with the bytes
     * from the specified long value.
     *
     * @throws BoundException if offset is out of buffer bounds
     */
    public void putLong(int off, long value) throws BoundException {
        final int count = JDWP.TypeSize.LONG;

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of long value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            putValueBytes(off, value, count);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + count +
                              "bytes of long value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Replace four bytes starting at offset off with the bytes
     * from the specified float value.
     *
     * @throws BoundException if offset is out of buffer bounds
     */
    public void putFloat(int off, float value) throws BoundException {
        final int count = JDWP.TypeSize.FLOAT;

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of float value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            long l = Float.floatToIntBits(value);
            putValueBytes(off, l, count);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + count +
                              "bytes of float value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Replace eight bytes starting at offset off with the bytes
     * from the specified double value.
     *
     * @throws BoundException if offset is out of buffer bounds
     */
    public void putDouble(int off, double value) throws BoundException {
        final int count = JDWP.TypeSize.DOUBLE;

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of double value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            long l = Double.doubleToLongBits(value);
            putValueBytes(off, l, count);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + count +
                              "bytes of double value at " + offsetString(off) + ": \n\t" + e);
        }
    }

    /**
     * Replace two bytes starting at offset off with the bytes
     * from the specified char value.
     *
     * @throws BoundException if offset is out of buffer bounds
     */
    public void putChar(int off, char value) throws BoundException {
        final int count = JDWP.TypeSize.CHAR;

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of char value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            long l = (long)value;
            putValueBytes(off, l, count);
        } catch (BoundException e) {
            throw new Failure("Caught unexpected bound exception while putting " + count +
                              "bytes of char value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    //////////////////////////////////////////////////////////////////////////

    /**
     * Append the specified byte to the end of this buffer.
     */
    public void addByte(byte value) {
        checkSpace(1);

        int where = CurrentSize;
        CurrentSize++;

        try {
            putByte(where, value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding one byte:\n\t"
                            + e);
        };
    }

    /**
     * Append specified byte value repeated count to the end of this buffer.
     */
    public void addBytes(byte value, int count) {
        checkSpace(count);
        for (int i = 0; i < count; i++) {
            addByte(value);
        }
    }

    /**
     * Append the bytes from the specified byte array to the end of this buffer.
     */
    public void addBytes(byte[] value, int start, int len) {
        checkSpace(len);

        int where = CurrentSize;
        CurrentSize = CurrentSize + len;

        try {
            putBytes(where, value, start, len);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               len + " bytes:\n\t" + e);
        };
    }

    /**
     * Appends the count (1 - 8) less significant bytes from the
     * specified ID value to the end of this buffer.
     */
    public void addID(long value, int count) {
        if ((count <= 0) || (count > 8))
            throw new TestBug("Illegal number bytes of ID value to add: " + count);

        final int where = CurrentSize;
        addBytes(EMPTY_BYTE, count);

        try {
            putID(where, value, count);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               count + " bytes of ID value:\n\t" + e);
        };
    }

    /**
     * Append four bytes from the specified int value to the
     * end of this buffer.
     */
    public void addInt(int value) {
        final int count = JDWP.TypeSize.INT;
        final int where = CurrentSize;
        addBytes(EMPTY_BYTE, count);

        try {
            putInt(where, value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               count + " bytes of int value:\n\t" + e);
        };
    }

    /**
     * Append two bytes from the specified int value to the
     * end of this buffer.
     */
    public void addShort(short value) {
        final int count = JDWP.TypeSize.SHORT;
        final int where = CurrentSize;
        addBytes(EMPTY_BYTE, count);
        try {
            putShort(where, value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               count + " bytes of short value:\n\t" + e);
        };
    }

    /**
     * Appends eight bytes from the specified long
     * value to the end of this buffer.
     */
    public void addLong(long value) {
        final int count = JDWP.TypeSize.LONG;
        final int where = CurrentSize;
        addBytes(EMPTY_BYTE, count);
        try {
            putLong(where, value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               count + " bytes of long value:\n\t" + e);
        };
    }

    /**
     * Appends four bytes from the specified float
     * value to the end of this buffer.
     */
    public void addFloat(float value) {
        final int count = JDWP.TypeSize.FLOAT;
        final int where = CurrentSize;
        addBytes(EMPTY_BYTE, count);
        try {
            putFloat(where, value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               count + " bytes of float value:\n\t" + e);
        };
    }

    /**
     * Appends eight bytes from the specified double
     * value to the end of this buffer.
     */
    public void addDouble(double value) {
        final int count = JDWP.TypeSize.DOUBLE;
        final int where = CurrentSize;
        addBytes(EMPTY_BYTE, count);
        try {
            putDouble(where, value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               count + " bytes of double value:\n\t" + e);
        };
    }

    /**
     * Appends four bytes from the specified char
     * value to the end of this buffer.
     */
    public void addChar(char value) {
        final int count = JDWP.TypeSize.CHAR;
        final int where = CurrentSize;
        addBytes(EMPTY_BYTE, count);
        try {
            putChar(where, value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while adding " +
                               count + " bytes of float value:\n\t" + e);
        };
    }

    //////////////////////////////////////////////////////////////////////////

    /**
     * Read a byte value from this buffer at the specified position.
     *
     * @throws BoundException if there are no bytes at this position
     */
    public byte getByte(int off) throws BoundException {
        if (off < 0 || off >= CurrentSize) {
            throw new BoundException("Unable to get one byte at " + offsetString(off) +
                                    ": no bytes available");
        }
        return bytes[off];
    }

    /**
     * Read count bytes (1-8) from this buffer at the specified
     * position and returns a long value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes at this position
     */
    public long getID(int off, int count) throws BoundException {
        if ((count <= 0) || (count > 8))
            throw new TestBug("Illegal number of bytes of ID value to get: " + count);

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to get " + count + " bytes of ID value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            return getValueBytes(off, count);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of ID value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Read four bytes from this buffer at the specified
     * position and returns an integer value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes at this position
     */
    public int getInt(int off) throws BoundException {
        final int count = JDWP.TypeSize.INT;
        if (count > CurrentSize - off) {
            throw new BoundException("Unable to get " + count + " bytes of int value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            return (int)getValueBytes(off, count);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of int value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Read two bytes from this buffer at the specified
     * position and returns a short value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes at this position
     */
    public short getShort(int off) throws BoundException {
        final int count = JDWP.TypeSize.SHORT;
        if (count > CurrentSize - off) {
            throw new BoundException("Unable to get " + count + " bytes of short value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            return (short)getValueBytes(off, count);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of short value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Read eight bytes from this buffer at the specified
     * position and returns a long value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes at this position
     */
    public long getLong(int off) throws BoundException {
        final int count = JDWP.TypeSize.LONG;
        if (count > CurrentSize - off) {
            throw new BoundException("Unable to get " + count + " bytes of long value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            return getValueBytes(off, count);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of long value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Read eight bytes from this buffer at the specified
     * position and returns a double value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes at this position
     */
    public double getDouble(int off) throws BoundException {
        final int count = JDWP.TypeSize.DOUBLE;
        if (count > CurrentSize - off) {
            throw new BoundException("Unable to get " + count + " bytes of double value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            long value = getValueBytes(off, count);
            return Double.longBitsToDouble(value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of long value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Read four bytes from this buffer at the specified
     * position and returns a float value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes at this position
     */
    public float getFloat(int off) throws BoundException {
        final int count = JDWP.TypeSize.FLOAT;
        if (count > CurrentSize - off) {
            throw new BoundException("Unable to get " + count + " bytes of float value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            int value = (int)getValueBytes(off, count);
            return Float.intBitsToFloat(value);
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of float value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    /**
     * Read two bytes from this buffer at the specified
     * position and returns a char value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes at this position
     */
    public char getChar(int off) throws BoundException {
        final int count = JDWP.TypeSize.CHAR;
        if (count > CurrentSize - off) {
            throw new BoundException("Unable to get " + count + " bytes of char value at " +
                                     offsetString(off) + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        try {
            int value = (int)getValueBytes(off, count);
            return (char)value;
        }
        catch (BoundException e) {
            throw new TestBug("Caught unexpected bound exception while getting " +
                               count + " bytes of char value at " + offsetString(off) + ":\n\t" + e);
        }
    }

    //////////////////////////////////////////////////////////////////////////

    /**
     * Set the current parser position to 0.
     */
    public void resetPosition() {
        resetPosition(0);
    }

    /**
     * Set the current parser position to the specified value.
     */
    public void resetPosition(int i) {
        parseOffset = i;
    }

    /**
     * Return current parser position.
     */
    public int currentPosition() {
        return parseOffset;
    }

    /**
     * Return true if the parser pointer is set to the end of buffer.
     */
    public boolean isParsed() {
        return (parseOffset == CurrentSize);
    }

    /**
     * Read a byte value from this buffer at the current parser position.
     *
     * @throws BoundException if there are no more bytes in the buffer
     */
    public byte getByte() throws BoundException {
        return getByte(parseOffset++);
    }

    /**
     * Read count bytes (1-8) from this buffer at the current parser
     * position and returns a long value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public long getID(int count) throws BoundException {
        long value = getID(parseOffset, count);
        parseOffset += count;
        return value;
    }

    /**
     * Read four bytes from this buffer at the current parser
     * position and returns an integer value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public int getInt() throws BoundException {
        final int count = JDWP.TypeSize.INT;
        int value = getInt(parseOffset);
        parseOffset += count;
        return value;
    }

    /**
     * Read two bytes from this buffer at the current parser
     * position and returns a short value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public short getShort() throws BoundException {
        final int count = JDWP.TypeSize.SHORT;
        short value = getShort(parseOffset);
        parseOffset += count;
        return value;
    }

    /**
     * Read eight bytes from this buffer at the current parser
     * position and returns a long value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public long getLong() throws BoundException {
        final int count = JDWP.TypeSize.LONG;
        long value = getLong(parseOffset);
        parseOffset += count;
        return value;
    }

    /**
     * Read eight bytes from this buffer at the current parser
     * position and returns a double value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public double getDouble() throws BoundException {
        final int count = JDWP.TypeSize.DOUBLE;
        double value = getDouble(parseOffset);
        parseOffset += count;
        return value;
    }

    /**
     * Read four bytes from this buffer at the current parser
     * position and returns a float value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public float getFloat() throws BoundException {
        final int count = JDWP.TypeSize.FLOAT;
        float value = getFloat(parseOffset);
        parseOffset += count;
        return value;
    }

    /**
     * Read two bytes from this buffer at the current parser
     * position and returns a char value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public char getChar() throws BoundException {
        final int count = JDWP.TypeSize.CHAR;
        char value = getChar(parseOffset);
        parseOffset += count;
        return value;
    }

    /**
     * Remove at least first count bytes from the buffer.
     */

    public void deleteBytes(int count) {
        int j = 0;
        while (count < CurrentSize)
            bytes[j++] = bytes[count++];

        CurrentSize = j;
    }

    /**
     * Clear the buffer.
     */
    public void resetBuffer() {
        CurrentSize = 0;
    }

    /**
     * Return string representation of the buffer starting at given offset.
     */
    public String toString(int start) {

        String Result = "", HexLine = "", DisplayLine = "";

        int j = 0;

        for (int i = start; i < length(); i++) {

            HexLine = HexLine + toHexString(bytes[i], 2) + " ";

            String ch = ".";
            if (bytes[i] >= 0x20 && bytes[i] < 0x80) {
                try {
                    ch = new String(bytes, i, 1, "US-ASCII");
                } catch (UnsupportedEncodingException ignore) {
                }
            }
            DisplayLine = DisplayLine + ch;

            if ((i == length() - 1) || (((i - start) & 0x0F) == 0x0F)) {
                Result = Result +
                         "    " +
                         toHexString(j, 4) + ": " +
                         PadR(HexLine, 48) + "  " +
                         DisplayLine + "\n";
                HexLine = "";
                DisplayLine = "";
                j = j + 16;
            }
        }
        return Result;
    }

    /**
     * Return string representation of the buffer.
     */
    public String toString() {
        return toString(0);
    }

    /**
     * Return string with hexadecimal representation of bytes.
     */
    public static String toHexString(long b, int length) {
        return Right(Long.toHexString(b), length).replace(' ', '0');
    }

    /**
     * Return string with hexadecimal representation of bytes.
     */
    public static String toHexDecString(long b, int length) {
        return toHexString(b, length) + " (" + b + ")";
    }

    // -----

    /**
     * Return string with hexadecimal representation of offset.
     */
    public static String offsetString(int off) {
        return "0x" + toHexString(off, 4);
    }

    /**
     * Return string with hexadecimal representation of the current offset.
     */
    public String offsetString() {
        return offsetString(currentPosition());
    }

    // -----

    /**
     * Check if there space for new bytes in the buffer.
     */
    protected void checkSpace(int space) {

        int newSize = CurrentSize + space;

        if (bytes.length >= newSize)
            return;

        byte[] newBytes = new byte[newSize];

        for (int i = 0; i < CurrentSize; i++)
            newBytes[i] = bytes[i];

        bytes = newBytes;
    }

    /**
     * Replace count (1 - 8) bytes starting at offset off with the less
     * significant bytes from the specified long value.
     *
     * @throws BoundException if offset and count are out of buffer bounds
     */
    protected void putValueBytes(int off, long value, int count) throws BoundException {
        if ((count <= 0) || (count > 8))
            throw new TestBug("Illegal number of bytes of value to put: " + count);

        if (count > CurrentSize - off) {
            throw new BoundException("Unable to put " + count + " bytes of value at " +
                                     off + " (available bytes: " + (CurrentSize - off) + ")" );
        }

        int shift = (count - 1) * 8;
        for (int i = 0; i < count; i++) {
            putByte(off++, (byte) ((value >>> shift) & 0xFF));
            shift = shift - 8;
        }
    }
    /**
     * Appends the count (1 - 8) less significant bytes from the
     * specified long value to the end of this buffer.
     */
    protected void addValueBytes(long value, int count) throws BoundException {
        if ((count <= 0) || (count > 8))
            throw new TestBug("Illegal number of bytes of value to add: " + count);

        checkSpace(count);

        int where = CurrentSize;
        CurrentSize += count;

        putValueBytes(where, value, count);
    }

    /**
     * Read count bytes (1-8) from this buffer at the specified
     * position and returns a long value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
    public long getValueBytes(int off, int count) throws BoundException {
        if ((count <= 0) || (count > 8))
            throw new TestBug("Illegal number of bytes of value to get: " + count);

        long l = 0;

        for (int i = 0; i < count; i++) {
            l = (l * 0x100) + ((long) getByte(off + i) & 0xFF);
        }

        return l;
    }

    /**
     * Read count bytes (1-8) from this buffer at the current parser
     * position and returns a long value composed of these bytes.
     *
     * @throws BoundException if there are no so many bytes in the buffer
     */
/*
    protected long getValueBytes(int count) throws BoundException {
        long value = getValueBytes(parseOffset);
        parseOffset += count;
        return value;
    }
 */

    // ---

    private static String PadL(String source, int length, String what) {

        if (length <= 0)
            return "";

        if (source.length() > length)
            return PadL("", length, "*");

        while (source.length() < length)
            source = what + source;

        return source;
    }


    private static String PadL(String source, int length) {
        return PadL(source, length, " ");
    }

    private static String PadR(String source, int length, String what) {

        if (length <= 0)
            return "";

        if (source.length() > length)
            return PadR("", length, "*");

        while (source.length() < length)
            source = source + what;

        return source;
    }

    private static String PadR(String source, int length) {
        return PadR(source, length, " ");
    }

    private static String Left(String source, int length) {

        if (length <= 0)
            return "";

        if (length <= source.length())
            return source.substring(0, length);
        else
            return PadR(source, length);
    }

    private static String Right(String source, int length) {

        if (length <= 0)
            return "";

        if (length <= source.length())
            return source.substring(source.length() - length, source.length());
        else
            return PadL(source, length);
    }

}
