/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.jgss;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;
import sun.security.util.*;

/**
 * Utilities for processing GSS Tokens.
 *
 */

public abstract class GSSToken {

    /**
     * Copies an integer value to a byte array in little endian form.
     * @param value the integer value to write
     * @param array the byte array into which the integer must be copied. It
     * is assumed that the array will be large enough to hold the 4 bytes of
     * the integer.
     */
    public static final void writeLittleEndian(int value, byte[] array) {
        writeLittleEndian(value, array, 0);
    }

    /**
     * Copies an integer value to a byte array in little endian form.
     * @param value the integer value to write
     * @param array the byte array into which the integer must be copied. It
     * is assumed that the array will be large enough to hold the 4 bytes of
     * the integer.
     * @param pos the position at which to start writing
     */
    public static final void writeLittleEndian(int value, byte[] array,
                                               int pos) {
        array[pos++] = (byte)(value);
        array[pos++] = (byte)((value>>>8));
        array[pos++] = (byte)((value>>>16));
        array[pos++] = (byte)((value>>>24));
    }

    public static final void writeBigEndian(int value, byte[] array) {
        writeBigEndian(value, array, 0);
    }

    public static final void writeBigEndian(int value, byte[] array,
                                               int pos) {
        array[pos++] = (byte)((value>>>24));
        array[pos++] = (byte)((value>>>16));
        array[pos++] = (byte)((value>>>8));
        array[pos++] = (byte)(value);
    }

    /**
     * Reads an integer value from a byte array in little endian form. This
     * method allows the reading of two byte values as well as four bytes
     * values both of which are needed in the Kerberos v5 GSS-API mechanism.
     *
     * @param data the array containing the bytes of the integer value
     * @param pos the offset in the array
     * @param size the number of bytes to read from the array.
     * @return the integer value
     */
    public static final int readLittleEndian(byte[] data, int pos, int size) {
        int retVal = 0;
        int shifter = 0;
        while (size > 0) {
            retVal += (data[pos] & 0xff) << shifter;
            shifter += 8;
            pos++;
            size--;
        }
        return retVal;
    }

    public static final int readBigEndian(byte[] data, int pos, int size) {
        int retVal = 0;
        int shifter = (size-1)*8;
        while (size > 0) {
            retVal += (data[pos] & 0xff) << shifter;
            shifter -= 8;
            pos++;
            size--;
        }
        return retVal;
    }

    /**
     * Writes a two byte integer value to a OutputStream.
     *
     * @param val the integer value. It will lose the high-order two bytes.
     * @param os the OutputStream to write to
     * @throws IOException if an error occurs while writing to the OutputStream
     */
    public static final void writeInt(int val, OutputStream os)
        throws IOException {
        os.write(val>>>8);
        os.write(val);
    }

    /**
     * Writes a two byte integer value to a byte array.
     *
     * @param val the integer value. It will lose the high-order two bytes.
     * @param dest the byte array to write to
     * @param pos the offset to start writing to
     */
    public static final int writeInt(int val, byte[] dest, int pos) {
        dest[pos++] = (byte)(val>>>8);
        dest[pos++] = (byte)val;
        return pos;
    }

    /**
     * Reads a two byte integer value from an InputStream.
     *
     * @param is the InputStream to read from
     * @return the integer value
     * @throws IOException if some errors occurs while reading the integer
     * bytes.
     */
    public static final int readInt(InputStream is) throws IOException {
        return (((0xFF & is.read()) << 8)
                 | (0xFF & is.read()));
    }

    /**
     * Reads a two byte integer value from a byte array.
     *
     * @param src the byte arra to read from
     * @param pos the offset to start reading from
     * @return the integer value
     */
    public static final int readInt(byte[] src, int pos) {
        return ((0xFF & src[pos])<<8 | (0xFF & src[pos+1]));
    }

    /**
     * Blocks till the required number of bytes have been read from the
     * input stream.
     *
     * @param is the InputStream to read from
     * @param buffer the buffer to store the bytes into
     * @throws EOFException if EOF is reached before all bytes are
     *         read.
     * @throws IOException is an error occurs while reading
     */
    public static final void readFully(InputStream is, byte[] buffer)
        throws IOException {
        readFully(is, buffer, 0, buffer.length);
    }

    /**
     * Blocks till the required number of bytes have been read from the
     * input stream.
     *
     * @param is the InputStream to read from
     * @param buffer the buffer to store the bytes into
     * @param offset the offset to start storing at
     * @param len the number of bytes to read
     * @throws EOFException if EOF is reached before all bytes are
     *         read.
     * @throws IOException is an error occurs while reading
     */
    public static final void readFully(InputStream is,
                                       byte[] buffer, int offset, int len)
        throws IOException {
        int temp;
        while (len > 0) {
            temp = is.read(buffer, offset, len);
            if (temp == -1)
                throw new EOFException("Cannot read all "
                                       + len
                                       + " bytes needed to form this token!");
            offset += temp;
            len -= temp;
        }
    }

    public static final void debug(String str) {
        System.err.print(str);
    }

    public static final  String getHexBytes(byte[] bytes) {
        return getHexBytes(bytes, 0, bytes.length);
    }

    public static final  String getHexBytes(byte[] bytes, int len) {
        return getHexBytes(bytes, 0, len);
    }

    public static final String getHexBytes(byte[] bytes, int pos, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = pos; i < (pos+len); i++) {
            int b1 = (bytes[i]>>4) & 0x0f;
            int b2 = bytes[i] & 0x0f;

            sb.append(Integer.toHexString(b1));
            sb.append(Integer.toHexString(b2));
            sb.append(' ');
        }
        return sb.toString();
    }

}
