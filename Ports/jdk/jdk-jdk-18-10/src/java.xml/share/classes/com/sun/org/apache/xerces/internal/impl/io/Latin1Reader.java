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

/**
 * <p>
 * Reader for the ISO-8859-1 encoding.</p>
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 *
 * @version $Id: Latin1Reader.java 718095 2008-11-16 20:00:14Z mrglavas $
 */
public final class Latin1Reader
        extends Reader {

    //
    // Constants
    //
    /**
     * Default byte buffer size (2048).
     */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

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

    //
    // Constructors
    //
    /**
     * Constructs an ISO-8859-1 reader from the specified input stream using the
     * default buffer size.
     *
     * @param inputStream The input stream.
     */
    public Latin1Reader(InputStream inputStream) {
        this(inputStream, DEFAULT_BUFFER_SIZE);
    } // <init>(InputStream)

    /**
     * Constructs an ISO-8859-1 reader from the specified input stream and
     * buffer size.
     *
     * @param inputStream The input stream.
     * @param size The initial buffer size.
     */
    public Latin1Reader(InputStream inputStream, int size) {
        this(inputStream, new byte[size]);
    } // <init>(InputStream, int)

    /**
     * Constructs an ISO-8859-1 reader from the specified input stream and
     * buffer.
     *
     * @param inputStream The input stream.
     * @param buffer The byte buffer.
     */
    public Latin1Reader(InputStream inputStream, byte[] buffer) {
        fInputStream = inputStream;
        fBuffer = buffer;
    } // <init>(InputStream, byte[])

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
     * @return The character read, as an integer in the range 0 to 255
     * (<tt>0x00-0xff</tt>), or -1 if the end of the stream has been reached
     *
     * @exception IOException If an I/O error occurs
     */
    public int read() throws IOException {
        return fInputStream.read();
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
        if (length > fBuffer.length) {
            length = fBuffer.length;
        }
        int count = fInputStream.read(fBuffer, 0, length);
        for (int i = 0; i < count; ++i) {
            ch[offset + i] = (char) (fBuffer[i] & 0xff);
        }
        return count;
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
        return fInputStream.skip(n);
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
        return fInputStream.markSupported();
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
        fInputStream.mark(readAheadLimit);
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
        fInputStream.reset();
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

} // class Latin1Reader
