/*
 * Copyright (c) 1995, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code DataInput} interface provides
 * for reading bytes from a binary stream and
 * reconstructing from them data in any of
 * the Java primitive types. There is also
 * a
 * facility for reconstructing a {@code String}
 * from data in
 * <a href="#modified-utf-8">modified UTF-8</a>
 * format.
 * <p>
 * It is generally true of all the reading
 * routines in this interface that if end of
 * file is reached before the desired number
 * of bytes has been read, an {@code EOFException}
 * (which is a kind of {@code IOException})
 * is thrown. If any byte cannot be read for
 * any reason other than end of file, an {@code IOException}
 * other than {@code EOFException} is
 * thrown. In particular, an {@code IOException}
 * may be thrown if the input stream has been
 * closed.
 *
 * <h2><a id="modified-utf-8">Modified UTF-8</a></h2>
 * <p>
 * Implementations of the DataInput and DataOutput interfaces represent
 * Unicode strings in a format that is a slight modification of UTF-8.
 * (For information regarding the standard UTF-8 format, see section
 * <i>3.9 Unicode Encoding Forms</i> of <i>The Unicode Standard, Version
 * 4.0</i>)
 *
 * <ul>
 * <li>Characters in the range {@code '\u005Cu0001'} to
 *         {@code '\u005Cu007F'} are represented by a single byte.
 * <li>The null character {@code '\u005Cu0000'} and characters
 *         in the range {@code '\u005Cu0080'} to {@code '\u005Cu07FF'} are
 *         represented by a pair of bytes.
 * <li>Characters in the range {@code '\u005Cu0800'}
 *         to {@code '\u005CuFFFF'} are represented by three bytes.
 * </ul>
 *
 *   <table class="plain" style="margin-left:2em;">
 *     <caption>Encoding of UTF-8 values</caption>
 *     <thead>
 *     <tr>
 *       <th scope="col" rowspan="2">Value</th>
 *       <th scope="col" rowspan="2">Byte</th>
 *       <th scope="col" colspan="8" id="bit_a">Bit Values</th>
 *     </tr>
 *     <tr>
 *       <!-- Value -->
 *       <!-- Byte -->
 *       <th scope="col" style="width:3em"> 7 </th>
 *       <th scope="col" style="width:3em"> 6 </th>
 *       <th scope="col" style="width:3em"> 5 </th>
 *       <th scope="col" style="width:3em"> 4 </th>
 *       <th scope="col" style="width:3em"> 3 </th>
 *       <th scope="col" style="width:3em"> 2 </th>
 *       <th scope="col" style="width:3em"> 1 </th>
 *       <th scope="col" style="width:3em"> 0 </th>
 *     </thead>
 *     <tbody>
 *     <tr>
 *       <th scope="row" style="text-align:left; font-weight:normal">
 *         {@code \u005Cu0001} to {@code \u005Cu007F} </th>
 *       <th scope="row" style="font-weight:normal; text-align:center"> 1 </th>
 *       <td style="text-align:center">0
 *       <td colspan="7" style="text-align:right; padding-right:6em">bits 6-0
 *     </tr>
 *     <tr>
 *       <th scope="row" rowspan="2" style="text-align:left; font-weight:normal">
 *           {@code \u005Cu0000},<br>
 *           {@code \u005Cu0080} to {@code \u005Cu07FF} </th>
 *       <th scope="row" style="font-weight:normal; text-align:center"> 1 </th>
 *       <td style="text-align:center">1
 *       <td style="text-align:center">1
 *       <td style="text-align:center">0
 *       <td colspan="5" style="text-align:right; padding-right:6em">bits 10-6
 *     </tr>
 *     <tr>
 *       <!-- (value) -->
 *       <th scope="row" style="font-weight:normal; text-align:center"> 2 </th>
 *       <td style="text-align:center">1
 *       <td style="text-align:center">0
 *       <td colspan="6" style="text-align:right; padding-right:6em">bits 5-0
 *     </tr>
 *     <tr>
 *       <th scope="row" rowspan="3" style="text-align:left; font-weight:normal">
 *         {@code \u005Cu0800} to {@code \u005CuFFFF} </th>
 *       <th scope="row" style="font-weight:normal; text-align:center"> 1 </th>
 *       <td style="text-align:center">1
 *       <td style="text-align:center">1
 *       <td style="text-align:center">1
 *       <td style="text-align:center">0
 *       <td colspan="4" style="text-align:right; padding-right:6em">bits 15-12
 *     </tr>
 *     <tr>
 *       <!-- (value) -->
 *       <th scope="row" style="font-weight:normal; text-align:center"> 2 </th>
 *       <td style="text-align:center">1
 *       <td style="text-align:center">0
 *       <td colspan="6" style="text-align:right; padding-right:6em">bits 11-6
 *     </tr>
 *     <tr>
 *       <!-- (value) -->
 *       <th scope="row" style="font-weight:normal; text-align:center"> 3 </th>
 *       <td style="text-align:center">1
 *       <td style="text-align:center">0
 *       <td colspan="6" style="text-align:right; padding-right:6em">bits 5-0
 *     </tr>
 *     </tbody>
 *   </table>
 *
 * <p>
 * The differences between this format and the
 * standard UTF-8 format are the following:
 * <ul>
 * <li>The null byte {@code '\u005Cu0000'} is encoded in 2-byte format
 *     rather than 1-byte, so that the encoded strings never have
 *     embedded nulls.
 * <li>Only the 1-byte, 2-byte, and 3-byte formats are used.
 * <li><a href="../lang/Character.html#unicode">Supplementary characters</a>
 *     are represented in the form of surrogate pairs.
 * </ul>
 * @author  Frank Yellin
 * @see     java.io.DataInputStream
 * @see     java.io.DataOutput
 * @since   1.0
 */
public interface DataInput {
    /**
     * Reads some bytes from an input
     * stream and stores them into the buffer
     * array {@code b}. The number of bytes
     * read is equal
     * to the length of {@code b}.
     * <p>
     * This method blocks until one of the
     * following conditions occurs:
     * <ul>
     * <li>{@code b.length}
     * bytes of input data are available, in which
     * case a normal return is made.
     *
     * <li>End of
     * file is detected, in which case an {@code EOFException}
     * is thrown.
     *
     * <li>An I/O error occurs, in
     * which case an {@code IOException} other
     * than {@code EOFException} is thrown.
     * </ul>
     * <p>
     * If {@code b} is {@code null},
     * a {@code NullPointerException} is thrown.
     * If {@code b.length} is zero, then
     * no bytes are read. Otherwise, the first
     * byte read is stored into element {@code b[0]},
     * the next one into {@code b[1]}, and
     * so on.
     * If an exception is thrown from
     * this method, then it may be that some but
     * not all bytes of {@code b} have been
     * updated with data from the input stream.
     *
     * @param   b   the buffer into which the data is read.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @throws  EOFException  if this stream reaches the end before reading
     *          all the bytes.
     * @throws  IOException   if an I/O error occurs.
     */
    void readFully(byte b[]) throws IOException;

    /**
     *
     * Reads {@code len}
     * bytes from
     * an input stream.
     * <p>
     * This method
     * blocks until one of the following conditions
     * occurs:
     * <ul>
     * <li>{@code len} bytes
     * of input data are available, in which case
     * a normal return is made.
     *
     * <li>End of file
     * is detected, in which case an {@code EOFException}
     * is thrown.
     *
     * <li>An I/O error occurs, in
     * which case an {@code IOException} other
     * than {@code EOFException} is thrown.
     * </ul>
     * <p>
     * If {@code b} is {@code null},
     * a {@code NullPointerException} is thrown.
     * If {@code off} is negative, or {@code len}
     * is negative, or {@code off+len} is
     * greater than the length of the array {@code b},
     * then an {@code IndexOutOfBoundsException}
     * is thrown.
     * If {@code len} is zero,
     * then no bytes are read. Otherwise, the first
     * byte read is stored into element {@code b[off]},
     * the next one into {@code b[off+1]},
     * and so on. The number of bytes read is,
     * at most, equal to {@code len}.
     *
     * @param   b    the buffer into which the data is read.
     * @param   off  an int specifying the offset in the data array {@code b}.
     * @param   len  an int specifying the number of bytes to read.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @throws  IndexOutOfBoundsException if {@code off} is negative,
     *          {@code len} is negative, or {@code len} is greater than
     *          {@code b.length - off}.
     * @throws  EOFException  if this stream reaches the end before reading
     *          all the bytes.
     * @throws  IOException   if an I/O error occurs.
     */
    void readFully(byte b[], int off, int len) throws IOException;

    /**
     * Makes an attempt to skip over
     * {@code n} bytes
     * of data from the input
     * stream, discarding the skipped bytes. However,
     * it may skip
     * over some smaller number of
     * bytes, possibly zero. This may result from
     * any of a
     * number of conditions; reaching
     * end of file before {@code n} bytes
     * have been skipped is
     * only one possibility.
     * This method never throws an {@code EOFException}.
     * The actual
     * number of bytes skipped is returned.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the number of bytes actually skipped.
     * @throws     IOException   if an I/O error occurs.
     */
    int skipBytes(int n) throws IOException;

    /**
     * Reads one input byte and returns
     * {@code true} if that byte is nonzero,
     * {@code false} if that byte is zero.
     * This method is suitable for reading
     * the byte written by the {@code writeBoolean}
     * method of interface {@code DataOutput}.
     *
     * @return     the {@code boolean} value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    boolean readBoolean() throws IOException;

    /**
     * Reads and returns one input byte.
     * The byte is treated as a signed value in
     * the range {@code -128} through {@code 127},
     * inclusive.
     * This method is suitable for
     * reading the byte written by the {@code writeByte}
     * method of interface {@code DataOutput}.
     *
     * @return     the 8-bit value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    byte readByte() throws IOException;

    /**
     * Reads one input byte, zero-extends
     * it to type {@code int}, and returns
     * the result, which is therefore in the range
     * {@code 0}
     * through {@code 255}.
     * This method is suitable for reading
     * the byte written by the {@code writeByte}
     * method of interface {@code DataOutput}
     * if the argument to {@code writeByte}
     * was intended to be a value in the range
     * {@code 0} through {@code 255}.
     *
     * @return     the unsigned 8-bit value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    int readUnsignedByte() throws IOException;

    /**
     * Reads two input bytes and returns
     * a {@code short} value. Let {@code a}
     * be the first byte read and {@code b}
     * be the second byte. The value
     * returned
     * is:
     * <pre>{@code (short)((a << 8) | (b & 0xff))
     * }</pre>
     * This method
     * is suitable for reading the bytes written
     * by the {@code writeShort} method of
     * interface {@code DataOutput}.
     *
     * @return     the 16-bit value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    short readShort() throws IOException;

    /**
     * Reads two input bytes and returns
     * an {@code int} value in the range {@code 0}
     * through {@code 65535}. Let {@code a}
     * be the first byte read and
     * {@code b}
     * be the second byte. The value returned is:
     * <pre>{@code (((a & 0xff) << 8) | (b & 0xff))
     * }</pre>
     * This method is suitable for reading the bytes
     * written by the {@code writeShort} method
     * of interface {@code DataOutput}  if
     * the argument to {@code writeShort}
     * was intended to be a value in the range
     * {@code 0} through {@code 65535}.
     *
     * @return     the unsigned 16-bit value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    int readUnsignedShort() throws IOException;

    /**
     * Reads two input bytes and returns a {@code char} value.
     * Let {@code a}
     * be the first byte read and {@code b}
     * be the second byte. The value
     * returned is:
     * <pre>{@code (char)((a << 8) | (b & 0xff))
     * }</pre>
     * This method
     * is suitable for reading bytes written by
     * the {@code writeChar} method of interface
     * {@code DataOutput}.
     *
     * @return     the {@code char} value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    char readChar() throws IOException;

    /**
     * Reads four input bytes and returns an
     * {@code int} value. Let {@code a-d}
     * be the first through fourth bytes read. The value returned is:
     * <pre>{@code
     * (((a & 0xff) << 24) | ((b & 0xff) << 16) |
     *  ((c & 0xff) <<  8) | (d & 0xff))
     * }</pre>
     * This method is suitable
     * for reading bytes written by the {@code writeInt}
     * method of interface {@code DataOutput}.
     *
     * @return     the {@code int} value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    int readInt() throws IOException;

    /**
     * Reads eight input bytes and returns
     * a {@code long} value. Let {@code a-h}
     * be the first through eighth bytes read.
     * The value returned is:
     * <pre>{@code
     * (((long)(a & 0xff) << 56) |
     *  ((long)(b & 0xff) << 48) |
     *  ((long)(c & 0xff) << 40) |
     *  ((long)(d & 0xff) << 32) |
     *  ((long)(e & 0xff) << 24) |
     *  ((long)(f & 0xff) << 16) |
     *  ((long)(g & 0xff) <<  8) |
     *  ((long)(h & 0xff)))
     * }</pre>
     * <p>
     * This method is suitable
     * for reading bytes written by the {@code writeLong}
     * method of interface {@code DataOutput}.
     *
     * @return     the {@code long} value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    long readLong() throws IOException;

    /**
     * Reads four input bytes and returns
     * a {@code float} value. It does this
     * by first constructing an {@code int}
     * value in exactly the manner
     * of the {@code readInt}
     * method, then converting this {@code int}
     * value to a {@code float} in
     * exactly the manner of the method {@code Float.intBitsToFloat}.
     * This method is suitable for reading
     * bytes written by the {@code writeFloat}
     * method of interface {@code DataOutput}.
     *
     * @return     the {@code float} value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    float readFloat() throws IOException;

    /**
     * Reads eight input bytes and returns
     * a {@code double} value. It does this
     * by first constructing a {@code long}
     * value in exactly the manner
     * of the {@code readLong}
     * method, then converting this {@code long}
     * value to a {@code double} in exactly
     * the manner of the method {@code Double.longBitsToDouble}.
     * This method is suitable for reading
     * bytes written by the {@code writeDouble}
     * method of interface {@code DataOutput}.
     *
     * @return     the {@code double} value read.
     * @throws     EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @throws     IOException   if an I/O error occurs.
     */
    double readDouble() throws IOException;

    /**
     * Reads the next line of text from the input stream.
     * It reads successive bytes, converting
     * each byte separately into a character,
     * until it encounters a line terminator or
     * end of
     * file; the characters read are then
     * returned as a {@code String}. Note
     * that because this
     * method processes bytes,
     * it does not support input of the full Unicode
     * character set.
     * <p>
     * If end of file is encountered
     * before even one byte can be read, then {@code null}
     * is returned. Otherwise, each byte that is
     * read is converted to type {@code char}
     * by zero-extension. If the character {@code '\n'}
     * is encountered, it is discarded and reading
     * ceases. If the character {@code '\r'}
     * is encountered, it is discarded and, if
     * the following byte converts &#32;to the
     * character {@code '\n'}, then that is
     * discarded also; reading then ceases. If
     * end of file is encountered before either
     * of the characters {@code '\n'} and
     * {@code '\r'} is encountered, reading
     * ceases. Once reading has ceased, a {@code String}
     * is returned that contains all the characters
     * read and not discarded, taken in order.
     * Note that every character in this string
     * will have a value less than {@code \u005Cu0100},
     * that is, {@code (char)256}.
     *
     * @return the next line of text from the input stream,
     *         or {@code null} if the end of file is
     *         encountered before a byte can be read.
     * @throws IOException  if an I/O error occurs.
     */
    String readLine() throws IOException;

    /**
     * Reads in a string that has been encoded using a
     * <a href="#modified-utf-8">modified UTF-8</a>
     * format.
     * The general contract of {@code readUTF}
     * is that it reads a representation of a Unicode
     * character string encoded in modified
     * UTF-8 format; this string of characters
     * is then returned as a {@code String}.
     * <p>
     * First, two bytes are read and used to
     * construct an unsigned 16-bit integer in
     * exactly the manner of the {@code readUnsignedShort}
     * method . This integer value is called the
     * <i>UTF length</i> and specifies the number
     * of additional bytes to be read. These bytes
     * are then converted to characters by considering
     * them in groups. The length of each group
     * is computed from the value of the first
     * byte of the group. The byte following a
     * group, if any, is the first byte of the
     * next group.
     * <p>
     * If the first byte of a group
     * matches the bit pattern {@code 0xxxxxxx}
     * (where {@code x} means "may be {@code 0}
     * or {@code 1}"), then the group consists
     * of just that byte. The byte is zero-extended
     * to form a character.
     * <p>
     * If the first byte
     * of a group matches the bit pattern {@code 110xxxxx},
     * then the group consists of that byte {@code a}
     * and a second byte {@code b}. If there
     * is no byte {@code b} (because byte
     * {@code a} was the last of the bytes
     * to be read), or if byte {@code b} does
     * not match the bit pattern {@code 10xxxxxx},
     * then a {@code UTFDataFormatException}
     * is thrown. Otherwise, the group is converted
     * to the character:
     * <pre>{@code (char)(((a & 0x1F) << 6) | (b & 0x3F))
     * }</pre>
     * If the first byte of a group
     * matches the bit pattern {@code 1110xxxx},
     * then the group consists of that byte {@code a}
     * and two more bytes {@code b} and {@code c}.
     * If there is no byte {@code c} (because
     * byte {@code a} was one of the last
     * two of the bytes to be read), or either
     * byte {@code b} or byte {@code c}
     * does not match the bit pattern {@code 10xxxxxx},
     * then a {@code UTFDataFormatException}
     * is thrown. Otherwise, the group is converted
     * to the character:
     * <pre>{@code
     * (char)(((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F))
     * }</pre>
     * If the first byte of a group matches the
     * pattern {@code 1111xxxx} or the pattern
     * {@code 10xxxxxx}, then a {@code UTFDataFormatException}
     * is thrown.
     * <p>
     * If end of file is encountered
     * at any time during this entire process,
     * then an {@code EOFException} is thrown.
     * <p>
     * After every group has been converted to
     * a character by this process, the characters
     * are gathered, in the same order in which
     * their corresponding groups were read from
     * the input stream, to form a {@code String},
     * which is returned.
     * <p>
     * The {@code writeUTF}
     * method of interface {@code DataOutput}
     * may be used to write data that is suitable
     * for reading by this method.
     * @return     a Unicode string.
     * @throws     EOFException            if this stream reaches the end
     *               before reading all the bytes.
     * @throws     IOException             if an I/O error occurs.
     * @throws     UTFDataFormatException  if the bytes do not represent a
     *               valid modified UTF-8 encoding of a string.
     */
    String readUTF() throws IOException;
}
