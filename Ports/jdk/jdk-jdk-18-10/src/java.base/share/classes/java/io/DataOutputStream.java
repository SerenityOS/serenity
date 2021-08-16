/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

/**
 * A data output stream lets an application write primitive Java data
 * types to an output stream in a portable way. An application can
 * then use a data input stream to read the data back in.
 * <p>
 * A DataOutputStream is not safe for use by multiple concurrent
 * threads. If a DataOutputStream is to be used by more than one
 * thread then access to the data output stream should be controlled
 * by appropriate synchronization.
 *
 * @see     java.io.DataInputStream
 * @since   1.0
 */
public class DataOutputStream extends FilterOutputStream implements DataOutput {
    /**
     * The number of bytes written to the data output stream so far.
     * If this counter overflows, it will be wrapped to Integer.MAX_VALUE.
     */
    protected int written;

    /**
     * bytearr is initialized on demand by writeUTF
     */
    private byte[] bytearr = null;

    private final byte[] writeBuffer = new byte[8];

    /**
     * Creates a new data output stream to write data to the specified
     * underlying output stream. The counter {@code written} is
     * set to zero.
     *
     * @param   out   the underlying output stream, to be saved for later
     *                use.
     * @see     java.io.FilterOutputStream#out
     */
    public DataOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Increases the written counter by the specified value
     * until it reaches Integer.MAX_VALUE.
     */
    private void incCount(int value) {
        int temp = written + value;
        if (temp < 0) {
            temp = Integer.MAX_VALUE;
        }
        written = temp;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument
     * {@code b}) to the underlying output stream. If no exception
     * is thrown, the counter {@code written} is incremented by
     * {@code 1}.
     * <p>
     * Implements the {@code write} method of {@code OutputStream}.
     *
     * @param      b   the {@code byte} to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public synchronized void write(int b) throws IOException {
        out.write(b);
        incCount(1);
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to the underlying output stream.
     * If no exception is thrown, the counter {@code written} is
     * incremented by {@code len}.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public synchronized void write(byte b[], int off, int len)
        throws IOException
    {
        out.write(b, off, len);
        incCount(len);
    }

    /**
     * Flushes this data output stream. This forces any buffered output
     * bytes to be written out to the stream.
     * <p>
     * The {@code flush} method of {@code DataOutputStream}
     * calls the {@code flush} method of its underlying output stream.
     *
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     * @see        java.io.OutputStream#flush()
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Writes a {@code boolean} to the underlying output stream as
     * a 1-byte value. The value {@code true} is written out as the
     * value {@code (byte)1}; the value {@code false} is
     * written out as the value {@code (byte)0}. If no exception is
     * thrown, the counter {@code written} is incremented by
     * {@code 1}.
     *
     * @param      v   a {@code boolean} value to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
        incCount(1);
    }

    /**
     * Writes out a {@code byte} to the underlying output stream as
     * a 1-byte value. If no exception is thrown, the counter
     * {@code written} is incremented by {@code 1}.
     *
     * @param      v   a {@code byte} value to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeByte(int v) throws IOException {
        out.write(v);
        incCount(1);
    }

    /**
     * Writes a {@code short} to the underlying output stream as two
     * bytes, high byte first. If no exception is thrown, the counter
     * {@code written} is incremented by {@code 2}.
     *
     * @param      v   a {@code short} to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeShort(int v) throws IOException {
        writeBuffer[0] = (byte)(v >>> 8);
        writeBuffer[1] = (byte)(v >>> 0);
        out.write(writeBuffer, 0, 2);
        incCount(2);
    }

    /**
     * Writes a {@code char} to the underlying output stream as a
     * 2-byte value, high byte first. If no exception is thrown, the
     * counter {@code written} is incremented by {@code 2}.
     *
     * @param      v   a {@code char} value to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeChar(int v) throws IOException {
        writeBuffer[0] = (byte)(v >>> 8);
        writeBuffer[1] = (byte)(v >>> 0);
        out.write(writeBuffer, 0, 2);
        incCount(2);
    }

    /**
     * Writes an {@code int} to the underlying output stream as four
     * bytes, high byte first. If no exception is thrown, the counter
     * {@code written} is incremented by {@code 4}.
     *
     * @param      v   an {@code int} to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeInt(int v) throws IOException {
        writeBuffer[0] = (byte)(v >>> 24);
        writeBuffer[1] = (byte)(v >>> 16);
        writeBuffer[2] = (byte)(v >>>  8);
        writeBuffer[3] = (byte)(v >>>  0);
        out.write(writeBuffer, 0, 4);
        incCount(4);
    }

    /**
     * Writes a {@code long} to the underlying output stream as eight
     * bytes, high byte first. In no exception is thrown, the counter
     * {@code written} is incremented by {@code 8}.
     *
     * @param      v   a {@code long} to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeLong(long v) throws IOException {
        writeBuffer[0] = (byte)(v >>> 56);
        writeBuffer[1] = (byte)(v >>> 48);
        writeBuffer[2] = (byte)(v >>> 40);
        writeBuffer[3] = (byte)(v >>> 32);
        writeBuffer[4] = (byte)(v >>> 24);
        writeBuffer[5] = (byte)(v >>> 16);
        writeBuffer[6] = (byte)(v >>>  8);
        writeBuffer[7] = (byte)(v >>>  0);
        out.write(writeBuffer, 0, 8);
        incCount(8);
    }

    /**
     * Converts the float argument to an {@code int} using the
     * {@code floatToIntBits} method in class {@code Float},
     * and then writes that {@code int} value to the underlying
     * output stream as a 4-byte quantity, high byte first. If no
     * exception is thrown, the counter {@code written} is
     * incremented by {@code 4}.
     *
     * @param      v   a {@code float} value to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     * @see        java.lang.Float#floatToIntBits(float)
     */
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /**
     * Converts the double argument to a {@code long} using the
     * {@code doubleToLongBits} method in class {@code Double},
     * and then writes that {@code long} value to the underlying
     * output stream as an 8-byte quantity, high byte first. If no
     * exception is thrown, the counter {@code written} is
     * incremented by {@code 8}.
     *
     * @param      v   a {@code double} value to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     * @see        java.lang.Double#doubleToLongBits(double)
     */
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /**
     * Writes out the string to the underlying output stream as a
     * sequence of bytes. Each character in the string is written out, in
     * sequence, by discarding its high eight bits. If no exception is
     * thrown, the counter {@code written} is incremented by the
     * length of {@code s}.
     *
     * @param      s   a string of bytes to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            out.write((byte)s.charAt(i));
        }
        incCount(len);
    }

    /**
     * Writes a string to the underlying output stream as a sequence of
     * characters. Each character is written to the data output stream as
     * if by the {@code writeChar} method. If no exception is
     * thrown, the counter {@code written} is incremented by twice
     * the length of {@code s}.
     *
     * @param      s   a {@code String} value to be written.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.DataOutputStream#writeChar(int)
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            int v = s.charAt(i);
            writeBuffer[0] = (byte)(v >>> 8);
            writeBuffer[1] = (byte)(v >>> 0);
            out.write(writeBuffer, 0, 2);
        }
        incCount(len * 2);
    }

    /**
     * Writes a string to the underlying output stream using
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * encoding in a machine-independent manner.
     * <p>
     * First, two bytes are written to the output stream as if by the
     * {@code writeShort} method giving the number of bytes to
     * follow. This value is the number of bytes actually written out,
     * not the length of the string. Following the length, each character
     * of the string is output, in sequence, using the modified UTF-8 encoding
     * for the character. If no exception is thrown, the counter
     * {@code written} is incremented by the total number of
     * bytes written to the output stream. This will be at least two
     * plus the length of {@code str}, and at most two plus
     * thrice the length of {@code str}.
     *
     * @param      str   a string to be written.
     * @throws     UTFDataFormatException  if the modified UTF-8 encoding of
     *             {@code str} would exceed 65535 bytes in length
     * @throws     IOException  if some other I/O error occurs.
     * @see        #writeChars(String)
     */
    public final void writeUTF(String str) throws IOException {
        writeUTF(str, this);
    }

    /**
     * Writes a string to the specified DataOutput using
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * encoding in a machine-independent manner.
     * <p>
     * First, two bytes are written to out as if by the {@code writeShort}
     * method giving the number of bytes to follow. This value is the number of
     * bytes actually written out, not the length of the string. Following the
     * length, each character of the string is output, in sequence, using the
     * modified UTF-8 encoding for the character. If no exception is thrown, the
     * counter {@code written} is incremented by the total number of
     * bytes written to the output stream. This will be at least two
     * plus the length of {@code str}, and at most two plus
     * thrice the length of {@code str}.
     *
     * @param      str   a string to be written.
     * @param      out   destination to write to
     * @return     The number of bytes written out.
     * @throws     UTFDataFormatException  if the modified UTF-8 encoding of
     *             {@code str} would exceed 65535 bytes in length
     * @throws     IOException  if some other I/O error occurs.
     */
    static int writeUTF(String str, DataOutput out) throws IOException {
        final int strlen = str.length();
        int utflen = strlen; // optimized for ASCII

        for (int i = 0; i < strlen; i++) {
            int c = str.charAt(i);
            if (c >= 0x80 || c == 0)
                utflen += (c >= 0x800) ? 2 : 1;
        }

        if (utflen > 65535 || /* overflow */ utflen < strlen)
            throw new UTFDataFormatException(tooLongMsg(str, utflen));

        final byte[] bytearr;
        if (out instanceof DataOutputStream dos) {
            if (dos.bytearr == null || (dos.bytearr.length < (utflen + 2)))
                dos.bytearr = new byte[(utflen*2) + 2];
            bytearr = dos.bytearr;
        } else {
            bytearr = new byte[utflen + 2];
        }

        int count = 0;
        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

        int i = 0;
        for (i = 0; i < strlen; i++) { // optimized for initial run of ASCII
            int c = str.charAt(i);
            if (c >= 0x80 || c == 0) break;
            bytearr[count++] = (byte) c;
        }

        for (; i < strlen; i++) {
            int c = str.charAt(i);
            if (c < 0x80 && c != 0) {
                bytearr[count++] = (byte) c;
            } else if (c >= 0x800) {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            } else {
                bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            }
        }
        out.write(bytearr, 0, utflen + 2);
        return utflen + 2;
    }

    private static String tooLongMsg(String s, int bits32) {
        int slen = s.length();
        String head = s.substring(0, 8);
        String tail = s.substring(slen - 8, slen);
        // handle int overflow with max 3x expansion
        long actualLength = (long)slen + Integer.toUnsignedLong(bits32 - slen);
        return "encoded string (" + head + "..." + tail + ") too long: "
            + actualLength + " bytes";
    }

    /**
     * Returns the current value of the counter {@code written},
     * the number of bytes written to this data output stream so far.
     * If the counter overflows, it will be wrapped to Integer.MAX_VALUE.
     *
     * @return  the value of the {@code written} field.
     * @see     java.io.DataOutputStream#written
     */
    public final int size() {
        return written;
    }
}
