/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code DataOutput} interface provides
 * for converting data from any of the Java
 * primitive types to a series of bytes and
 * writing these bytes to a binary stream.
 * There is  also a facility for converting
 * a {@code String} into
 * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
 * format and writing the resulting series
 * of bytes.
 * <p>
 * For all the methods in this interface that
 * write bytes, it is generally true that if
 * a byte cannot be written for any reason,
 * an {@code IOException} is thrown.
 *
 * @author  Frank Yellin
 * @see     java.io.DataInput
 * @see     java.io.DataOutputStream
 * @since   1.0
 */
public interface DataOutput {
    /**
     * Writes to the output stream the eight
     * low-order bits of the argument {@code b}.
     * The 24 high-order  bits of {@code b}
     * are ignored.
     *
     * @param      b   the byte to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void write(int b) throws IOException;

    /**
     * Writes to the output stream all the bytes in array {@code b}.
     * If {@code b} is {@code null},
     * a {@code NullPointerException} is thrown.
     * If {@code b.length} is zero, then
     * no bytes are written. Otherwise, the byte
     * {@code b[0]} is written first, then
     * {@code b[1]}, and so on; the last byte
     * written is {@code b[b.length-1]}.
     *
     * @param      b   the data.
     * @throws     IOException  if an I/O error occurs.
     */
    void write(byte b[]) throws IOException;

    /**
     * Writes {@code len} bytes from array
     * {@code b}, in order,  to
     * the output stream.  If {@code b}
     * is {@code null}, a {@code NullPointerException}
     * is thrown.  If {@code off} is negative,
     * or {@code len} is negative, or {@code off+len}
     * is greater than the length of the array
     * {@code b}, then an {@code IndexOutOfBoundsException}
     * is thrown.  If {@code len} is zero,
     * then no bytes are written. Otherwise, the
     * byte {@code b[off]} is written first,
     * then {@code b[off+1]}, and so on; the
     * last byte written is {@code b[off+len-1]}.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @throws     IOException  if an I/O error occurs.
     */
    void write(byte b[], int off, int len) throws IOException;

    /**
     * Writes a {@code boolean} value to this output stream.
     * If the argument {@code v}
     * is {@code true}, the value {@code (byte)1}
     * is written; if {@code v} is {@code false},
     * the  value {@code (byte)0} is written.
     * The byte written by this method may
     * be read by the {@code readBoolean}
     * method of interface {@code DataInput},
     * which will then return a {@code boolean}
     * equal to {@code v}.
     *
     * @param      v   the boolean to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeBoolean(boolean v) throws IOException;

    /**
     * Writes to the output stream the eight low-order
     * bits of the argument {@code v}.
     * The 24 high-order bits of {@code v}
     * are ignored. (This means  that {@code writeByte}
     * does exactly the same thing as {@code write}
     * for an integer argument.) The byte written
     * by this method may be read by the {@code readByte}
     * method of interface {@code DataInput},
     * which will then return a {@code byte}
     * equal to {@code (byte)v}.
     *
     * @param      v   the byte value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeByte(int v) throws IOException;

    /**
     * Writes two bytes to the output
     * stream to represent the value of the argument.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 8))
     * (byte)(0xff & v)
     * }</pre> <p>
     * The bytes written by this method may be
     * read by the {@code readShort} method
     * of interface {@code DataInput}, which
     * will then return a {@code short} equal
     * to {@code (short)v}.
     *
     * @param      v   the {@code short} value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeShort(int v) throws IOException;

    /**
     * Writes a {@code char} value, which
     * is comprised of two bytes, to the
     * output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 8))
     * (byte)(0xff & v)
     * }</pre><p>
     * The bytes written by this method may be
     * read by the {@code readChar} method
     * of interface {@code DataInput}, which
     * will then return a {@code char} equal
     * to {@code (char)v}.
     *
     * @param      v   the {@code char} value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeChar(int v) throws IOException;

    /**
     * Writes an {@code int} value, which is
     * comprised of four bytes, to the output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 24))
     * (byte)(0xff & (v >> 16))
     * (byte)(0xff & (v >>  8))
     * (byte)(0xff & v)
     * }</pre><p>
     * The bytes written by this method may be read
     * by the {@code readInt} method of interface
     * {@code DataInput}, which will then
     * return an {@code int} equal to {@code v}.
     *
     * @param      v   the {@code int} value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeInt(int v) throws IOException;

    /**
     * Writes a {@code long} value, which is
     * comprised of eight bytes, to the output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 56))
     * (byte)(0xff & (v >> 48))
     * (byte)(0xff & (v >> 40))
     * (byte)(0xff & (v >> 32))
     * (byte)(0xff & (v >> 24))
     * (byte)(0xff & (v >> 16))
     * (byte)(0xff & (v >>  8))
     * (byte)(0xff & v)
     * }</pre><p>
     * The bytes written by this method may be
     * read by the {@code readLong} method
     * of interface {@code DataInput}, which
     * will then return a {@code long} equal
     * to {@code v}.
     *
     * @param      v   the {@code long} value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeLong(long v) throws IOException;

    /**
     * Writes a {@code float} value,
     * which is comprised of four bytes, to the output stream.
     * It does this as if it first converts this
     * {@code float} value to an {@code int}
     * in exactly the manner of the {@code Float.floatToIntBits}
     * method  and then writes the {@code int}
     * value in exactly the manner of the  {@code writeInt}
     * method.  The bytes written by this method
     * may be read by the {@code readFloat}
     * method of interface {@code DataInput},
     * which will then return a {@code float}
     * equal to {@code v}.
     *
     * @param      v   the {@code float} value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeFloat(float v) throws IOException;

    /**
     * Writes a {@code double} value,
     * which is comprised of eight bytes, to the output stream.
     * It does this as if it first converts this
     * {@code double} value to a {@code long}
     * in exactly the manner of the {@code Double.doubleToLongBits}
     * method  and then writes the {@code long}
     * value in exactly the manner of the  {@code writeLong}
     * method. The bytes written by this method
     * may be read by the {@code readDouble}
     * method of interface {@code DataInput},
     * which will then return a {@code double}
     * equal to {@code v}.
     *
     * @param      v   the {@code double} value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeDouble(double v) throws IOException;

    /**
     * Writes a string to the output stream.
     * For every character in the string
     * {@code s},  taken in order, one byte
     * is written to the output stream.  If
     * {@code s} is {@code null}, a {@code NullPointerException}
     * is thrown.<p>  If {@code s.length}
     * is zero, then no bytes are written. Otherwise,
     * the character {@code s[0]} is written
     * first, then {@code s[1]}, and so on;
     * the last character written is {@code s[s.length-1]}.
     * For each character, one byte is written,
     * the low-order byte, in exactly the manner
     * of the {@code writeByte} method . The
     * high-order eight bits of each character
     * in the string are ignored.
     *
     * @param      s   the string of bytes to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeBytes(String s) throws IOException;

    /**
     * Writes every character in the string {@code s},
     * to the output stream, in order,
     * two bytes per character. If {@code s}
     * is {@code null}, a {@code NullPointerException}
     * is thrown.  If {@code s.length}
     * is zero, then no characters are written.
     * Otherwise, the character {@code s[0]}
     * is written first, then {@code s[1]},
     * and so on; the last character written is
     * {@code s[s.length-1]}. For each character,
     * two bytes are actually written, high-order
     * byte first, in exactly the manner of the
     * {@code writeChar} method.
     *
     * @param      s   the string value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeChars(String s) throws IOException;

    /**
     * Writes two bytes of length information
     * to the output stream, followed
     * by the
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * representation
     * of  every character in the string {@code s}.
     * If {@code s} is {@code null},
     * a {@code NullPointerException} is thrown.
     * Each character in the string {@code s}
     * is converted to a group of one, two, or
     * three bytes, depending on the value of the
     * character.<p>
     * If a character {@code c}
     * is in the range <code>&#92;u0001</code> through
     * <code>&#92;u007f</code>, it is represented
     * by one byte:
     * <pre>(byte)c </pre>  <p>
     * If a character {@code c} is <code>&#92;u0000</code>
     * or is in the range <code>&#92;u0080</code>
     * through <code>&#92;u07ff</code>, then it is
     * represented by two bytes, to be written
     * in the order shown: <pre>{@code
     * (byte)(0xc0 | (0x1f & (c >> 6)))
     * (byte)(0x80 | (0x3f & c))
     * }</pre> <p> If a character
     * {@code c} is in the range <code>&#92;u0800</code>
     * through {@code uffff}, then it is
     * represented by three bytes, to be written
     * in the order shown: <pre>{@code
     * (byte)(0xe0 | (0x0f & (c >> 12)))
     * (byte)(0x80 | (0x3f & (c >>  6)))
     * (byte)(0x80 | (0x3f & c))
     * }</pre>  <p> First,
     * the total number of bytes needed to represent
     * all the characters of {@code s} is
     * calculated. If this number is larger than
     * {@code 65535}, then a {@code UTFDataFormatException}
     * is thrown. Otherwise, this length is written
     * to the output stream in exactly the manner
     * of the {@code writeShort} method;
     * after this, the one-, two-, or three-byte
     * representation of each character in the
     * string {@code s} is written.<p>  The
     * bytes written by this method may be read
     * by the {@code readUTF} method of interface
     * {@code DataInput}, which will then
     * return a {@code String} equal to {@code s}.
     *
     * @param      s   the string value to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    void writeUTF(String s) throws IOException;
}
