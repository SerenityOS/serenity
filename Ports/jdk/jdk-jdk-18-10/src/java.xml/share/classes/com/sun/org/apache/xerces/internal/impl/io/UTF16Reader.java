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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Locale;

import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.MessageFormatter;

/**
 * <p>
 * A UTF-16 reader. Can also be used for UCS-2 (i.e. ISO-10646-UCS-2).</p>
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 *
 * @version $Id: UTF16Reader.java 718095 2008-11-16 20:00:14Z mrglavas $
 */
public final class UTF16Reader
        extends Reader {

    //
    // Constants
    //
    /**
     * Default byte buffer size (4096).
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    //
    // Data
    //
    /**
     * Input stream.
     */
    protected final InputStream fInputStream;

    /**
     * Byte buffer.
     */
    protected final byte[] fBuffer;

    /**
     * Endianness.
     */
    protected final boolean fIsBigEndian;

    // message formatter; used to produce localized exception messages
    private final MessageFormatter fFormatter;

    // Locale to use for messages
    private final Locale fLocale;

    //
    // Constructors
    //
    /**
     * Constructs a UTF-16 reader from the specified input stream using the
     * default buffer size. Primarily for testing.
     *
     * @param inputStream The input stream.
     * @param isBigEndian The byte order.
     */
    public UTF16Reader(InputStream inputStream, boolean isBigEndian) {
        this(inputStream, DEFAULT_BUFFER_SIZE, isBigEndian,
                new XMLMessageFormatter(), Locale.getDefault());
    } // <init>(InputStream, boolean)

    /**
     * Constructs a UTF-16 reader from the specified input stream using the
     * default buffer size and the given MessageFormatter.
     *
     * @param inputStream The input stream.
     * @param isBigEndian The byte order.
     */
    public UTF16Reader(InputStream inputStream, boolean isBigEndian,
            MessageFormatter messageFormatter, Locale locale) {
        this(inputStream, DEFAULT_BUFFER_SIZE, isBigEndian, messageFormatter, locale);
    } // <init>(InputStream, boolean, MessageFormatter, Locale)

    /**
     * Constructs a UTF-16 reader from the specified input stream and buffer
     * size and given MessageFormatter.
     *
     * @param inputStream The input stream.
     * @param size The initial buffer size.
     * @param isBigEndian The byte order.
     * @param messageFormatter Given MessageFormatter
     * @param locale Locale to use for messages
     */
    public UTF16Reader(InputStream inputStream, int size, boolean isBigEndian,
            MessageFormatter messageFormatter, Locale locale) {
        this(inputStream, new byte[size], isBigEndian, messageFormatter, locale);
    } // <init>(InputStream, int, boolean, MessageFormatter, Locale)

    /**
     * Constructs a UTF-16 reader from the specified input stream, buffer and
     * MessageFormatter.
     *
     * @param inputStream The input stream.
     * @param buffer The byte buffer.
     * @param isBigEndian The byte order.
     * @param messageFormatter Given MessageFormatter
     * @param locale Locale to use for messages
     */
    public UTF16Reader(InputStream inputStream, byte[] buffer, boolean isBigEndian,
            MessageFormatter messageFormatter, Locale locale) {
        fInputStream = inputStream;
        fBuffer = buffer;
        fIsBigEndian = isBigEndian;
        fFormatter = messageFormatter;
        fLocale = locale;
    } // <init>(InputStream, byte[], boolean, MessageFormatter, Locale)

    //
    // Reader methods
    //
    /**
     * Read a single character. This method will block until a character is
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * <p>
     * Subclasses that intend to support efficient single-character input should
     * override this method.
     *
     * @return The character read, as an integer in the range 0 to 65535
     * (<tt>0x00-0xffff</tt>), or -1 if the end of the stream has been reached
     *
     * @exception IOException If an I/O error occurs
     */
    public int read() throws IOException {
        final int b0 = fInputStream.read();
        if (b0 == -1) {
            return -1;
        }
        final int b1 = fInputStream.read();
        if (b1 == -1) {
            expectedTwoBytes();
        }
        // UTF-16BE
        if (fIsBigEndian) {
            return (b0 << 8) | b1;
        }
        // UTF-16LE
        return (b1 << 8) | b0;
    } // read():int

    /**
     * Read characters into a portion of an array. This method will block until
     * some input is available, an I/O error occurs, or the end of the stream is
     * reached.
     *
     * @param ch Destination buffer
     * @param offset Offset at which to start storing characters
     * @param length Maximum number of characters to read
     *
     * @return The number of characters read, or -1 if the end of the stream has
     * been reached
     *
     * @exception IOException If an I/O error occurs
     */
    public int read(char ch[], int offset, int length) throws IOException {
        int byteLength = length << 1;
        if (byteLength > fBuffer.length) {
            byteLength = fBuffer.length;
        }
        int byteCount = fInputStream.read(fBuffer, 0, byteLength);
        if (byteCount == -1) {
            return -1;
        }
        // If an odd number of bytes were read, we still need to read one more.
        if ((byteCount & 1) != 0) {
            int b = fInputStream.read();
            if (b == -1) {
                expectedTwoBytes();
            }
            fBuffer[byteCount++] = (byte) b;
        }
        final int charCount = byteCount >> 1;
        if (fIsBigEndian) {
            processBE(ch, offset, charCount);
        } else {
            processLE(ch, offset, charCount);
        }
        return charCount;
    } // read(char[],int,int)

    /**
     * Skip characters. This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param n The number of characters to skip
     *
     * @return The number of characters actually skipped
     *
     * @exception IOException If an I/O error occurs
     */
    public long skip(long n) throws IOException {
        long bytesSkipped = fInputStream.skip(n << 1);
        if ((bytesSkipped & 1) != 0) {
            int b = fInputStream.read();
            if (b == -1) {
                expectedTwoBytes();
            }
            ++bytesSkipped;
        }
        return bytesSkipped >> 1;
    } // skip(long):long

    /**
     * Tell whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input,
     * false otherwise. Note that returning false does not guarantee that the
     * next read will block.
     *
     * @exception IOException If an I/O error occurs
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
     * Mark the present position in the stream. Subsequent calls to reset() will
     * attempt to reposition the stream to this point. Not all character-input
     * streams support the mark() operation.
     *
     * @param readAheadLimit Limit on the number of characters that may be read
     * while still preserving the mark. After reading this many characters,
     * attempting to reset the stream may fail.
     *
     * @exception IOException If the stream does not support mark(), or if some
     * other I/O error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException(fFormatter.formatMessage(fLocale, "OperationNotSupported", new Object[]{"mark()", "UTF-16"}));
    } // mark(int)

    /**
     * Reset the stream. If the stream has been marked, then attempt to
     * reposition it at the mark. If the stream has not been marked, then
     * attempt to reset it in some way appropriate to the particular stream, for
     * example by repositioning it to its starting point. Not all
     * character-input streams support the reset() operation, and some support
     * reset() without supporting mark().
     *
     * @exception IOException If the stream has not been marked, or if the mark
     * has been invalidated, or if the stream does not support reset(), or if
     * some other I/O error occurs
     */
    public void reset() throws IOException {
    } // reset()

    /**
     * Close the stream. Once a stream has been closed, further read(), ready(),
     * mark(), or reset() invocations will throw an IOException. Closing a
     * previously-closed stream, however, has no effect.
     *
     * @exception IOException If an I/O error occurs
     */
    public void close() throws IOException {
        fInputStream.close();
    } // close()

    //
    // Private methods
    //
    /**
     * Decodes UTF-16BE *
     */
    private void processBE(final char ch[], int offset, final int count) {
        int curPos = 0;
        for (int i = 0; i < count; ++i) {
            final int b0 = fBuffer[curPos++] & 0xff;
            final int b1 = fBuffer[curPos++] & 0xff;
            ch[offset++] = (char) ((b0 << 8) | b1);
        }
    } // processBE(char[],int,int)

    /**
     * Decodes UTF-16LE *
     */
    private void processLE(final char ch[], int offset, final int count) {
        int curPos = 0;
        for (int i = 0; i < count; ++i) {
            final int b0 = fBuffer[curPos++] & 0xff;
            final int b1 = fBuffer[curPos++] & 0xff;
            ch[offset++] = (char) ((b1 << 8) | b0);
        }
    } // processLE(char[],int,int)

    /**
     * Throws an exception for expected byte.
     */
    private void expectedTwoBytes()
            throws MalformedByteSequenceException {
        throw new MalformedByteSequenceException(fFormatter,
                fLocale,
                XMLMessageFormatter.XML_DOMAIN,
                "ExpectedByte",
                new Object[]{"2", "2"});
    } // expectedTwoBytes()

} // class UTF16Reader
