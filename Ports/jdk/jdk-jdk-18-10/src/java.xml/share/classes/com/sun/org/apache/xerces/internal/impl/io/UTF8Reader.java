/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;

import java.util.Locale;
import com.sun.org.apache.xerces.internal.util.MessageFormatter;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;

import com.sun.xml.internal.stream.util.BufferAllocator;
import com.sun.xml.internal.stream.util.ThreadLocalBufferAllocator;

/**
 * <p>A UTF-8 reader.</p>
 *
 * @xerces.internal
 *
 * @author Andy Clark, IBM
 *
 */
public class UTF8Reader
    extends Reader {

    //
    // Constants
    //

    /** Default byte buffer size (2048). */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    // debugging

    /** Debug read. */
    private static final boolean DEBUG_READ = false;

    //
    // Data
    //

    /** Input stream. */
    protected InputStream fInputStream;

    /** Byte buffer. */
    protected byte[] fBuffer;

    /** Offset into buffer. */
    protected int fOffset;

    /** Surrogate character. */
    private int fSurrogate = -1;

    // message formatter; used to produce localized
    // exception messages
    private MessageFormatter fFormatter = null;

    //Locale to use for messages
    private Locale fLocale = null;

    //
    // Constructors
    //

    /**
     * Constructs a UTF-8 reader from the specified input stream
     * using the default buffer size.  Primarily for testing.
     *
     * @param inputStream The input stream.
     */
    public UTF8Reader(InputStream inputStream) {
        this(inputStream, DEFAULT_BUFFER_SIZE, new XMLMessageFormatter(), Locale.getDefault());
    } // <init>(InputStream, MessageFormatter)

    /**
     * Constructs a UTF-8 reader from the specified input stream
     * using the default buffer size and the given MessageFormatter.
     *
     * @param inputStream The input stream.
     * @param messageFormatter  given MessageFormatter
     * @param locale    Locale to use for messages
     */
    public UTF8Reader(InputStream inputStream, MessageFormatter messageFormatter,
            Locale locale) {
        this(inputStream, DEFAULT_BUFFER_SIZE, messageFormatter, locale);
    } // <init>(InputStream, MessageFormatter, Locale)

    /**
     * Constructs a UTF-8 reader from the specified input stream,
     * buffer size and MessageFormatter.
     *
     * @param inputStream The input stream.
     * @param size        The initial buffer size.
     * @param messageFormatter  the formatter for localizing/formatting errors.
     * @param locale    the Locale to use for messages
     */
    public UTF8Reader(InputStream inputStream, int size,
            MessageFormatter messageFormatter, Locale locale) {
        fInputStream = inputStream;
        BufferAllocator ba = ThreadLocalBufferAllocator.getBufferAllocator();
        fBuffer = ba.getByteBuffer(size);
        if (fBuffer == null) {
            fBuffer = new byte[size];
        }
        fFormatter = messageFormatter;
        fLocale = locale;
    } // <init>(InputStream, int, MessageFormatter, Locale)

    //
    // Reader methods
    //

    /**
     * Read a single character.  This method will block until a character is
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * <p> Subclasses that intend to support efficient single-character input
     * should override this method.
     *
     * @return     The character read, as an integer in the range 0 to 16383
     *             (<tt>0x00-0xffff</tt>), or -1 if the end of the stream has
     *             been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {

        // decode character
        int c = fSurrogate;
        if (fSurrogate == -1) {
            // NOTE: We use the index into the buffer if there are remaining
            //       bytes from the last block read. -Ac
            int index = 0;

            // get first byte
            int b0 = index == fOffset
                   ? fInputStream.read() : fBuffer[index++] & 0x00FF;
            if (b0 == -1) {
                return -1;
            }

            // UTF-8:   [0xxx xxxx]
            // Unicode: [0000 0000] [0xxx xxxx]
            if (b0 < 0x80) {
                c = (char)b0;
            }

            // UTF-8:   [110y yyyy] [10xx xxxx]
            // Unicode: [0000 0yyy] [yyxx xxxx]
            else if ((b0 & 0xE0) == 0xC0 && (b0 & 0x1E) != 0) {
                int b1 = index == fOffset
                       ? fInputStream.read() : fBuffer[index++] & 0x00FF;
                if (b1 == -1) {
                    expectedByte(2, 2);
                }
                if ((b1 & 0xC0) != 0x80) {
                    invalidByte(2, 2, b1);
                }
                c = ((b0 << 6) & 0x07C0) | (b1 & 0x003F);
            }

            // UTF-8:   [1110 zzzz] [10yy yyyy] [10xx xxxx]
            // Unicode: [zzzz yyyy] [yyxx xxxx]
            else if ((b0 & 0xF0) == 0xE0) {
                int b1 = index == fOffset
                       ? fInputStream.read() : fBuffer[index++] & 0x00FF;
                if (b1 == -1) {
                    expectedByte(2, 3);
                }
                if ((b1 & 0xC0) != 0x80
                    || (b0 == 0xED && b1 >= 0xA0)
                    || ((b0 & 0x0F) == 0 && (b1 & 0x20) == 0)) {
                    invalidByte(2, 3, b1);
                }
                int b2 = index == fOffset
                       ? fInputStream.read() : fBuffer[index++] & 0x00FF;
                if (b2 == -1) {
                    expectedByte(3, 3);
                }
                if ((b2 & 0xC0) != 0x80) {
                    invalidByte(3, 3, b2);
                }
                c = ((b0 << 12) & 0xF000) | ((b1 << 6) & 0x0FC0) |
                    (b2 & 0x003F);
            }

            // UTF-8:   [1111 0uuu] [10uu zzzz] [10yy yyyy] [10xx xxxx]*
            // Unicode: [1101 10ww] [wwzz zzyy] (high surrogate)
            //          [1101 11yy] [yyxx xxxx] (low surrogate)
            //          * uuuuu = wwww + 1
            else if ((b0 & 0xF8) == 0xF0) {
                int b1 = index == fOffset
                       ? fInputStream.read() : fBuffer[index++] & 0x00FF;
                if (b1 == -1) {
                    expectedByte(2, 4);
                }
                if ((b1 & 0xC0) != 0x80
                    || ((b1 & 0x30) == 0 && (b0 & 0x07) == 0)) {
                    invalidByte(2, 3, b1);
                }
                int b2 = index == fOffset
                       ? fInputStream.read() : fBuffer[index++] & 0x00FF;
                if (b2 == -1) {
                    expectedByte(3, 4);
                }
                if ((b2 & 0xC0) != 0x80) {
                    invalidByte(3, 3, b2);
                }
                int b3 = index == fOffset
                       ? fInputStream.read() : fBuffer[index++] & 0x00FF;
                if (b3 == -1) {
                    expectedByte(4, 4);
                }
                if ((b3 & 0xC0) != 0x80) {
                    invalidByte(4, 4, b3);
                }
                int uuuuu = ((b0 << 2) & 0x001C) | ((b1 >> 4) & 0x0003);
                if (uuuuu > 0x10) {
                    invalidSurrogate(uuuuu);
                }
                int wwww = uuuuu - 1;
                int hs = 0xD800 |
                         ((wwww << 6) & 0x03C0) | ((b1 << 2) & 0x003C) |
                         ((b2 >> 4) & 0x0003);
                int ls = 0xDC00 | ((b2 << 6) & 0x03C0) | (b3 & 0x003F);
                c = hs;
                fSurrogate = ls;
            }

            // error
            else {
                invalidByte(1, 1, b0);
            }
        }

        // use surrogate
        else {
            fSurrogate = -1;
        }

        // return character
        if (DEBUG_READ) {
            System.out.println("read(): 0x"+Integer.toHexString(c));
        }
        return c;

    } // read():int

    /**
     * Read characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     * @param      ch     Destination buffer
     * @param      offset Offset at which to start storing characters
     * @param      length Maximum number of characters to read
     *
     * @return     The number of characters read, or -1 if the end of the
     *             stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read(char ch[], int offset, int length) throws IOException {

        // handle surrogate
        int out = offset;
        if (fSurrogate != -1) {
            ch[offset + 1] = (char)fSurrogate;
            fSurrogate = -1;
            length--;
            out++;
        }

        // read bytes
        int count = 0;
        if (fOffset == 0) {
            // adjust length to read
            if (length > fBuffer.length) {
                length = fBuffer.length;
            }

            // perform read operation
            count = fInputStream.read(fBuffer, 0, length);
            if (count == -1) {
                return -1;
            }
            count += out - offset;
        }

        // skip read; last character was in error
        // NOTE: Having an offset value other than zero means that there was
        //       an error in the last character read. In this case, we have
        //       skipped the read so we don't consume any bytes past the
        //       error. By signalling the error on the next block read we
        //       allow the method to return the most valid characters that
        //       it can on the previous block read. -Ac
        else {
            count = fOffset;
            fOffset = 0;
        }

        // convert bytes to characters
        final int total = count;
        int in;
        byte byte1;
        final byte byte0 = 0;
        for (in = 0; in < total; in++) {
            byte1 = fBuffer[in];
            if (byte1 >= byte0) {
                ch[out++] = (char)byte1;
            }
            else   {
                break;
            }
        }
        for ( ; in < total; in++) {
            byte1 = fBuffer[in];

            // UTF-8:   [0xxx xxxx]
            // Unicode: [0000 0000] [0xxx xxxx]
            if (byte1 >= byte0) {
                ch[out++] = (char)byte1;
                continue;
            }

            // UTF-8:   [110y yyyy] [10xx xxxx]
            // Unicode: [0000 0yyy] [yyxx xxxx]
            int b0 = byte1 & 0x0FF;
            if ((b0 & 0xE0) == 0xC0 && (b0 & 0x1E) != 0) {
                int b1 = -1;
                if (++in < total) {
                    b1 = fBuffer[in] & 0x00FF;
                }
                else {
                    b1 = fInputStream.read();
                    if (b1 == -1) {
                        if (out > offset) {
                            fBuffer[0] = (byte)b0;
                            fOffset = 1;
                            return out - offset;
                        }
                        expectedByte(2, 2);
                    }
                    count++;
                }
                if ((b1 & 0xC0) != 0x80) {
                    if (out > offset) {
                        fBuffer[0] = (byte)b0;
                        fBuffer[1] = (byte)b1;
                        fOffset = 2;
                        return out - offset;
                    }
                    invalidByte(2, 2, b1);
                }
                int c = ((b0 << 6) & 0x07C0) | (b1 & 0x003F);
                ch[out++] = (char)c;
                count -= 1;
                continue;
            }

            // UTF-8:   [1110 zzzz] [10yy yyyy] [10xx xxxx]
            // Unicode: [zzzz yyyy] [yyxx xxxx]
            if ((b0 & 0xF0) == 0xE0) {
                int b1 = -1;
                if (++in < total) {
                    b1 = fBuffer[in] & 0x00FF;
                }
                else {
                    b1 = fInputStream.read();
                    if (b1 == -1) {
                        if (out > offset) {
                            fBuffer[0] = (byte)b0;
                            fOffset = 1;
                            return out - offset;
                        }
                        expectedByte(2, 3);
                    }
                    count++;
                }
                if ((b1 & 0xC0) != 0x80
                    || (b0 == 0xED && b1 >= 0xA0)
                    || ((b0 & 0x0F) == 0 && (b1 & 0x20) == 0)) {
                    if (out > offset) {
                        fBuffer[0] = (byte)b0;
                        fBuffer[1] = (byte)b1;
                        fOffset = 2;
                        return out - offset;
                    }
                    invalidByte(2, 3, b1);
                }
                int b2 = -1;
                if (++in < total) {
                    b2 = fBuffer[in] & 0x00FF;
                }
                else {
                    b2 = fInputStream.read();
                    if (b2 == -1) {
                        if (out > offset) {
                            fBuffer[0] = (byte)b0;
                            fBuffer[1] = (byte)b1;
                            fOffset = 2;
                            return out - offset;
                        }
                        expectedByte(3, 3);
                    }
                    count++;
                }
                if ((b2 & 0xC0) != 0x80) {
                    if (out > offset) {
                        fBuffer[0] = (byte)b0;
                        fBuffer[1] = (byte)b1;
                        fBuffer[2] = (byte)b2;
                        fOffset = 3;
                        return out - offset;
                    }
                    invalidByte(3, 3, b2);
                }
                int c = ((b0 << 12) & 0xF000) | ((b1 << 6) & 0x0FC0) |
                        (b2 & 0x003F);
                ch[out++] = (char)c;
                count -= 2;
                continue;
            }

            // UTF-8:   [1111 0uuu] [10uu zzzz] [10yy yyyy] [10xx xxxx]*
            // Unicode: [1101 10ww] [wwzz zzyy] (high surrogate)
            //          [1101 11yy] [yyxx xxxx] (low surrogate)
            //          * uuuuu = wwww + 1
            if ((b0 & 0xF8) == 0xF0) {
                int b1 = -1;
                if (++in < total) {
                    b1 = fBuffer[in] & 0x00FF;
                }
                else {
                    b1 = fInputStream.read();
                    if (b1 == -1) {
                        if (out > offset) {
                            fBuffer[0] = (byte)b0;
                            fOffset = 1;
                            return out - offset;
                        }
                        expectedByte(2, 4);
                    }
                    count++;
                }
                if ((b1 & 0xC0) != 0x80
                    || ((b1 & 0x30) == 0 && (b0 & 0x07) == 0)) {
                    if (out > offset) {
                        fBuffer[0] = (byte)b0;
                        fBuffer[1] = (byte)b1;
                        fOffset = 2;
                        return out - offset;
                    }
                    invalidByte(2, 4, b1);
                }
                int b2 = -1;
                if (++in < total) {
                    b2 = fBuffer[in] & 0x00FF;
                }
                else {
                    b2 = fInputStream.read();
                    if (b2 == -1) {
                        if (out > offset) {
                            fBuffer[0] = (byte)b0;
                            fBuffer[1] = (byte)b1;
                            fOffset = 2;
                            return out - offset;
                        }
                        expectedByte(3, 4);
                    }
                    count++;
                }
                if ((b2 & 0xC0) != 0x80) {
                    if (out > offset) {
                        fBuffer[0] = (byte)b0;
                        fBuffer[1] = (byte)b1;
                        fBuffer[2] = (byte)b2;
                        fOffset = 3;
                        return out - offset;
                    }
                    invalidByte(3, 4, b2);
                }
                int b3 = -1;
                if (++in < total) {
                    b3 = fBuffer[in] & 0x00FF;
                }
                else {
                    b3 = fInputStream.read();
                    if (b3 == -1) {
                        if (out > offset) {
                            fBuffer[0] = (byte)b0;
                            fBuffer[1] = (byte)b1;
                            fBuffer[2] = (byte)b2;
                            fOffset = 3;
                            return out - offset;
                        }
                        expectedByte(4, 4);
                    }
                    count++;
                }
                if ((b3 & 0xC0) != 0x80) {
                    if (out > offset) {
                        fBuffer[0] = (byte)b0;
                        fBuffer[1] = (byte)b1;
                        fBuffer[2] = (byte)b2;
                        fBuffer[3] = (byte)b3;
                        fOffset = 4;
                        return out - offset;
                    }
                    invalidByte(4, 4, b2);
                }

                // check if output buffer is large enough to hold 2 surrogate chars
                if (out + 1 >= ch.length) {
                    fBuffer[0] = (byte)b0;
                    fBuffer[1] = (byte)b1;
                    fBuffer[2] = (byte)b2;
                    fBuffer[3] = (byte)b3;
                    fOffset = 4;
                    return out - offset;
                }

                // decode bytes into surrogate characters
                int uuuuu = ((b0 << 2) & 0x001C) | ((b1 >> 4) & 0x0003);
                if (uuuuu > 0x10) {
                    invalidSurrogate(uuuuu);
                }
                int wwww = uuuuu - 1;
                int zzzz = b1 & 0x000F;
                int yyyyyy = b2 & 0x003F;
                int xxxxxx = b3 & 0x003F;
                int hs = 0xD800 | ((wwww << 6) & 0x03C0) | (zzzz << 2) | (yyyyyy >> 4);
                int ls = 0xDC00 | ((yyyyyy << 6) & 0x03C0) | xxxxxx;

                // set characters
                ch[out++] = (char)hs;
                ch[out++] = (char)ls;
                count -= 2;
                continue;
            }

            // error
            if (out > offset) {
                fBuffer[0] = (byte)b0;
                fOffset = 1;
                return out - offset;
            }
            invalidByte(1, 1, b0);
        }

        // return number of characters converted
        if (DEBUG_READ) {
            System.out.println("read(char[],"+offset+','+length+"): count="+count);
        }
        return count;

    } // read(char[],int,int)

    /**
     * Skip characters.  This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param  n  The number of characters to skip
     *
     * @return    The number of characters actually skipped
     *
     * @exception  IOException  If an I/O error occurs
     */
    public long skip(long n) throws IOException {

        long remaining = n;
        final char[] ch = new char[fBuffer.length];
        do {
            int length = ch.length < remaining ? ch.length : (int)remaining;
            int count = read(ch, 0, length);
            if (count > 0) {
                remaining -= count;
            }
            else {
                break;
            }
        } while (remaining > 0);

        long skipped = n - remaining;
        return skipped;

    } // skip(long):long

    /**
     * Tell whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input,
     * false otherwise.  Note that returning false does not guarantee that the
     * next read will block.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        return false;
    } // ready()

    /**
     * Tell whether this stream supports the mark() operation.
     */
    public boolean markSupported() {
        return false;
    } // markSupported()

    /**
     * Mark the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.  Not all
     * character-input streams support the mark() operation.
     *
     * @param  readAheadLimit  Limit on the number of characters that may be
     *                         read while still preserving the mark.  After
     *                         reading this many characters, attempting to
     *                         reset the stream may fail.
     *
     * @exception  IOException  If the stream does not support mark(),
     *                          or if some other I/O error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException(fFormatter.formatMessage(fLocale, "OperationNotSupported", new Object[]{"mark()", "UTF-8"}));
    } // mark(int)

    /**
     * Reset the stream.  If the stream has been marked, then attempt to
     * reposition it at the mark.  If the stream has not been marked, then
     * attempt to reset it in some way appropriate to the particular stream,
     * for example by repositioning it to its starting point.  Not all
     * character-input streams support the reset() operation, and some support
     * reset() without supporting mark().
     *
     * @exception  IOException  If the stream has not been marked,
     *                          or if the mark has been invalidated,
     *                          or if the stream does not support reset(),
     *                          or if some other I/O error occurs
     */
    public void reset() throws IOException {
        fOffset = 0;
        fSurrogate = -1;
    } // reset()

    /**
     * Close the stream.  Once a stream has been closed, further read(),
     * ready(), mark(), or reset() invocations will throw an IOException.
     * Closing a previously-closed stream, however, has no effect.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        BufferAllocator ba = ThreadLocalBufferAllocator.getBufferAllocator();
        ba.returnByteBuffer(fBuffer);
        fBuffer = null;
        fInputStream.close();
    } // close()

    //
    // Private methods
    //

    /** Throws an exception for expected byte. */
    private void expectedByte(int position, int count)
        throws MalformedByteSequenceException {

        throw new MalformedByteSequenceException(fFormatter,
            fLocale,
            XMLMessageFormatter.XML_DOMAIN,
            "ExpectedByte",
            new Object[] {Integer.toString(position), Integer.toString(count)});

    } // expectedByte(int,int)

    /** Throws an exception for invalid byte. */
    private void invalidByte(int position, int count, int c)
        throws MalformedByteSequenceException {

        throw new MalformedByteSequenceException(fFormatter,
            fLocale,
            XMLMessageFormatter.XML_DOMAIN,
            "InvalidByte",
            new Object [] {Integer.toString(position), Integer.toString(count)});

    } // invalidByte(int,int,int)

    /** Throws an exception for invalid surrogate bits. */
    private void invalidSurrogate(int uuuuu) throws MalformedByteSequenceException {

        throw new MalformedByteSequenceException(fFormatter,
            fLocale,
            XMLMessageFormatter.XML_DOMAIN,
            "InvalidHighSurrogate",
            new Object[] {Integer.toHexString(uuuuu)});

    } // invalidSurrogate(int)

} // class UTF8Reader
