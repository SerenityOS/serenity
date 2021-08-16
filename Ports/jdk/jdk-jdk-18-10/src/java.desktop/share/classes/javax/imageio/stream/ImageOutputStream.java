/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.stream;

import java.io.DataOutput;
import java.io.IOException;

/**
 * A seekable output stream interface for use by
 * {@code ImageWriter}s.  Various output destinations, such as
 * {@code OutputStream}s and {@code File}s, as well as
 * future fast I/O destinations may be "wrapped" by a suitable
 * implementation of this interface for use by the Image I/O API.
 *
 * <p> Unlike a standard {@code OutputStream}, ImageOutputStream
 * extends its counterpart, {@code ImageInputStream}.  Thus it is
 * possible to read from the stream as it is being written.  The same
 * seek and flush positions apply to both reading and writing, although
 * the semantics for dealing with a non-zero bit offset before a byte-aligned
 * write are necessarily different from the semantics for dealing with
 * a non-zero bit offset before a byte-aligned read.  When reading bytes,
 * any bit offset is set to 0 before the read; when writing bytes, a
 * non-zero bit offset causes the remaining bits in the byte to be written
 * as 0s. The byte-aligned write then starts at the next byte position.
 *
 * @see ImageInputStream
 *
 */
public interface ImageOutputStream extends ImageInputStream, DataOutput {

    /**
     * Writes a single byte to the stream at the current position.
     * The 24 high-order bits of {@code b} are ignored.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.  Implementers can use the
     * {@link ImageOutputStreamImpl#flushBits flushBits}
     * method of {@link ImageOutputStreamImpl ImageOutputStreamImpl}
     * to guarantee this.
     *
     * @param b an {@code int} whose lower 8 bits are to be
     * written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void write(int b) throws IOException;

    /**
     * Writes a sequence of bytes to the stream at the current
     * position.  If {@code b.length} is 0, nothing is written.
     * The byte {@code b[0]} is written first, then the byte
     * {@code b[1]}, and so on.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param b an array of {@code byte}s to be written.
     *
     * @exception NullPointerException if {@code b} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void write(byte[] b) throws IOException;

    /**
     * Writes a sequence of bytes to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The byte {@code b[off]} is written first, then the byte
     * {@code b[off + 1]}, and so on.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.  Implementers can use the
     * {@link ImageOutputStreamImpl#flushBits flushBits}
     * method of {@link ImageOutputStreamImpl ImageOutputStreamImpl}
     * to guarantee this.
     *
     * @param b an array of {@code byte}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code byte}s to write.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code b.length}.
     * @exception NullPointerException if {@code b} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void write(byte[] b, int off, int len) throws IOException;

    /**
     * Writes a {@code boolean} value to the stream.  If
     * {@code v} is true, the value {@code (byte)1} is
     * written; if {@code v} is false, the value
     * {@code (byte)0} is written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v the {@code boolean} to be written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeBoolean(boolean v) throws IOException;

    /**
     * Writes the 8 low-order bits of {@code v} to the
     * stream. The 24 high-order bits of {@code v} are ignored.
     * (This means that {@code writeByte} does exactly the same
     * thing as {@code write} for an integer argument.)
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v an {@code int} containing the byte value to be
     * written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeByte(int v) throws IOException;

    /**
     * Writes the 16 low-order bits of {@code v} to the
     * stream. The 16 high-order bits of {@code v} are ignored.
     * If the stream uses network byte order, the bytes written, in
     * order, will be:
     *
     * <pre>
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)(v &amp; 0xff)
     * </pre>
     *
     * Otherwise, the bytes written will be:
     *
     * <pre>
     * (byte)(v &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * </pre>
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v an {@code int} containing the short value to be
     * written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeShort(int v) throws IOException;

    /**
     * This method is a synonym for {@link #writeShort writeShort}.
     *
     * @param v an {@code int} containing the char (unsigned
     * short) value to be written.
     *
     * @exception IOException if an I/O error occurs.
     *
     * @see #writeShort(int)
     */
    void writeChar(int v) throws IOException;

    /**
     * Writes the 32 bits of {@code v} to the stream.  If the
     * stream uses network byte order, the bytes written, in order,
     * will be:
     *
     * <pre>
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)(v &amp; 0xff)
     * </pre>
     *
     * Otheriwse, the bytes written will be:
     *
     * <pre>
     * (byte)(v &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * </pre>
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v an {@code int} containing the value to be
     * written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeInt(int v) throws IOException;

    /**
     * Writes the 64 bits of {@code v} to the stream.  If the
     * stream uses network byte order, the bytes written, in order,
     * will be:
     *
     * <pre>
     * (byte)((v &gt;&gt; 56) &amp; 0xff)
     * (byte)((v &gt;&gt; 48) &amp; 0xff)
     * (byte)((v &gt;&gt; 40) &amp; 0xff)
     * (byte)((v &gt;&gt; 32) &amp; 0xff)
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)(v &amp; 0xff)
     * </pre>
     *
     * Otherwise, the bytes written will be:
     *
     * <pre>
     * (byte)(v &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * (byte)((v &gt;&gt; 32) &amp; 0xff)
     * (byte)((v &gt;&gt; 40) &amp; 0xff)
     * (byte)((v &gt;&gt; 48) &amp; 0xff)
     * (byte)((v &gt;&gt; 56) &amp; 0xff)
     * </pre>
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v a {@code long} containing the value to be
     * written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeLong(long v) throws IOException;

    /**
     * Writes a {@code float} value, which is comprised of four
     * bytes, to the output stream. It does this as if it first
     * converts this {@code float} value to an {@code int}
     * in exactly the manner of the {@code Float.floatToIntBits}
     * method and then writes the int value in exactly the manner of
     * the {@code writeInt} method.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v a {@code float} containing the value to be
     * written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeFloat(float v) throws IOException;

    /**
     * Writes a {@code double} value, which is comprised of four
     * bytes, to the output stream. It does this as if it first
     * converts this {@code double} value to a {@code long}
     * in exactly the manner of the
     * {@code Double.doubleToLongBits} method and then writes the
     * long value in exactly the manner of the {@code writeLong}
     * method.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v a {@code double} containing the value to be
     * written.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeDouble(double v) throws IOException;

    /**
     * Writes a string to the output stream. For every character in
     * the string {@code s}, taken in order, one byte is written
     * to the output stream. If {@code s} is {@code null}, a
     * {@code NullPointerException} is thrown.
     *
     * <p> If {@code s.length} is zero, then no bytes are
     * written. Otherwise, the character {@code s[0]} is written
     * first, then {@code s[1]}, and so on; the last character
     * written is {@code s[s.length-1]}. For each character, one
     * byte is written, the low-order byte, in exactly the manner of
     * the {@code writeByte} method. The high-order eight bits of
     * each character in the string are ignored.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param s a {@code String} containing the value to be
     * written.
     *
     * @exception NullPointerException if {@code s} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeBytes(String s) throws IOException;

    /**
     * Writes a string to the output stream. For every character in
     * the string {@code s}, taken in order, two bytes are
     * written to the output stream, ordered according to the current
     * byte order setting.  If network byte order is being used, the
     * high-order byte is written first; the order is reversed
     * otherwise.  If {@code s} is {@code null}, a
     * {@code NullPointerException} is thrown.
     *
     * <p> If {@code s.length} is zero, then no bytes are
     * written. Otherwise, the character {@code s[0]} is written
     * first, then {@code s[1]}, and so on; the last character
     * written is {@code s[s.length-1]}.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param s a {@code String} containing the value to be
     * written.
     *
     * @exception NullPointerException if {@code s} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeChars(String s) throws IOException;

    /**
     * Writes two bytes of length information to the output stream in
     * network byte order, followed by the
     * <a href="../../../../java.base/java/io/DataInput.html#modified-utf-8">
     * modified UTF-8</a>
     * representation of every character in the string {@code s}.
     * If {@code s} is {@code null}, a
     * {@code NullPointerException} is thrown.  Each character in
     * the string {@code s} is converted to a group of one, two,
     * or three bytes, depending on the value of the character.
     *
     * <p> If a character {@code c} is in the range
     * <code>&#92;u0001</code> through <code>&#92;u007f</code>, it is
     * represented by one byte:
     *
     * <pre>
     * (byte)c
     * </pre>
     *
     * <p> If a character {@code c} is <code>&#92;u0000</code> or
     * is in the range <code>&#92;u0080</code> through
     * <code>&#92;u07ff</code>, then it is represented by two bytes,
     * to be written in the order shown:
     *
     * <pre><code>
     * (byte)(0xc0 | (0x1f &amp; (c &gt;&gt; 6)))
     * (byte)(0x80 | (0x3f &amp; c))
     * </code></pre>
     *
     * <p> If a character {@code c} is in the range
     * <code>&#92;u0800</code> through {@code uffff}, then it is
     * represented by three bytes, to be written in the order shown:
     *
     * <pre><code>
     * (byte)(0xe0 | (0x0f &amp; (c &gt;&gt; 12)))
     * (byte)(0x80 | (0x3f &amp; (c &gt;&gt; 6)))
     * (byte)(0x80 | (0x3f &amp; c))
     * </code></pre>
     *
     * <p> First, the total number of bytes needed to represent all
     * the characters of {@code s} is calculated. If this number
     * is larger than {@code 65535}, then a
     * {@code UTFDataFormatException} is thrown. Otherwise, this
     * length is written to the output stream in exactly the manner of
     * the {@code writeShort} method; after this, the one-, two-,
     * or three-byte representation of each character in the string
     * {@code s} is written.
     *
     * <p> The current byte order setting is ignored.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * <p><strong>Note:</strong> This method should not be used in
     * the  implementation of image formats that use standard UTF-8,
     * because  the modified UTF-8 used here is incompatible with
     * standard UTF-8.
     *
     * @param s a {@code String} containing the value to be
     * written.
     *
     * @exception NullPointerException if {@code s} is
     * {@code null}.
     * @exception java.io.UTFDataFormatException if the modified UTF-8
     * representation of {@code s} requires more than 65536 bytes.
     * @exception IOException if an I/O error occurs.
     */
    void writeUTF(String s) throws IOException;

    /**
     * Writes a sequence of shorts to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The short {@code s[off]} is written first, then the short
     * {@code s[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param s an array of {@code short}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code short}s to write.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code s.length}.
     * @exception NullPointerException if {@code s} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeShorts(short[] s, int off, int len) throws IOException;

    /**
     * Writes a sequence of chars to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The char {@code c[off]} is written first, then the char
     * {@code c[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param c an array of {@code char}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code char}s to write.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code c.length}.
     * @exception NullPointerException if {@code c} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeChars(char[] c, int off, int len) throws IOException;

    /**
     * Writes a sequence of ints to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The int {@code i[off]} is written first, then the int
     * {@code i[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param i an array of {@code int}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code int}s to write.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code i.length}.
     * @exception NullPointerException if {@code i} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeInts(int[] i, int off, int len) throws IOException;

    /**
     * Writes a sequence of longs to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The long {@code l[off]} is written first, then the long
     * {@code l[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param l an array of {@code long}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code long}s to write.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code l.length}.
     * @exception NullPointerException if {@code l} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeLongs(long[] l, int off, int len) throws IOException;

    /**
     * Writes a sequence of floats to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The float {@code f[off]} is written first, then the float
     * {@code f[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param f an array of {@code float}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code float}s to write.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code f.length}.
     * @exception NullPointerException if {@code f} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeFloats(float[] f, int off, int len) throws IOException;

    /**
     * Writes a sequence of doubles to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The double {@code d[off]} is written first, then the double
     * {@code d[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param d an array of {@code doubles}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code double}s to write.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code d.length}.
     * @exception NullPointerException if {@code d} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    void writeDoubles(double[] d, int off, int len) throws IOException;

    /**
     * Writes a single bit, given by the least significant bit of the
     * argument, to the stream at the current bit offset within the
     * current byte position.  The upper 31 bits of the argument are
     * ignored.  The given bit replaces the previous bit at that
     * position.  The bit offset is advanced by one and reduced modulo
     * 8.
     *
     * <p> If any bits of a particular byte have never been set
     * at the time the byte is flushed to the destination, those
     * bits will be set to 0 automatically.
     *
     * @param bit an {@code int} whose least significant bit
     * is to be written to the stream.
     *
     * @exception IOException if an I/O error occurs.
     */
    void writeBit(int bit) throws IOException;

    /**
     * Writes a sequence of bits, given by the {@code numBits}
     * least significant bits of the {@code bits} argument in
     * left-to-right order, to the stream at the current bit offset
     * within the current byte position.  The upper {@code 64 - numBits}
     * bits of the argument are ignored.  The bit
     * offset is advanced by {@code numBits} and reduced modulo
     * 8.  Note that a bit offset of 0 always indicates the
     * most-significant bit of the byte, and bytes of bits are written
     * out in sequence as they are encountered.  Thus bit writes are
     * always effectively in network byte order.  The actual stream
     * byte order setting is ignored.
     *
     * <p> Bit data may be accumulated in memory indefinitely, until
     * {@code flushBefore} is called.  At that time, all bit data
     * prior to the flushed position will be written.
     *
     * <p> If any bits of a particular byte have never been set
     * at the time the byte is flushed to the destination, those
     * bits will be set to 0 automatically.
     *
     * @param bits a {@code long} containing the bits to be
     * written, starting with the bit in position {@code numBits - 1}
     * down to the least significant bit.
     *
     * @param numBits an {@code int} between 0 and 64, inclusive.
     *
     * @exception IllegalArgumentException if {@code numBits} is
     * not between 0 and 64, inclusive.
     * @exception IOException if an I/O error occurs.
     */
    void writeBits(long bits, int numBits) throws IOException;

    /**
     * Flushes all data prior to the given position to the underlying
     * destination, such as an {@code OutputStream} or
     * {@code File}.  Attempting to seek to the flushed portion
     * of the stream will result in an
     * {@code IndexOutOfBoundsException}.
     *
     * @param pos a {@code long} containing the length of the
     * stream prefix that may be flushed to the destination.
     *
     * @exception IndexOutOfBoundsException if {@code pos} lies
     * in the flushed portion of the stream or past the current stream
     * position.
     * @exception IOException if an I/O error occurs.
     */
    void flushBefore(long pos) throws IOException;
}
