/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
package sun.net.www.http;

import java.io.*;
import java.util.concurrent.locks.ReentrantLock;
import sun.net.www.*;
import sun.nio.cs.US_ASCII;

/**
 * A <code>ChunkedInputStream</code> provides a stream for reading a body of
 * a http message that can be sent as a series of chunks, each with its own
 * size indicator. Optionally the last chunk can be followed by trailers
 * containing entity-header fields.
 * <p>
 * A <code>ChunkedInputStream</code> is also <code>Hurryable</code> so it
 * can be hurried to the end of the stream if the bytes are available on
 * the underlying stream.
 */
public class ChunkedInputStream extends InputStream implements Hurryable {

    /**
     * The underlying stream
     */
    private InputStream in;

    /**
     * The <code>HttpClient</code> that should be notified when the chunked stream has
     * completed.
     */
    private HttpClient hc;

    /**
     * The <code>MessageHeader</code> that is populated with any optional trailer
     * that appear after the last chunk.
     */
    private MessageHeader responses;

    /**
     * The size, in bytes, of the chunk that is currently being read.
     * This size is only valid if the current position in the underlying
     * input stream is inside a chunk (ie: state == STATE_READING_CHUNK).
     */
    private int chunkSize;

    /**
     * The number of bytes read from the underlying stream for the current
     * chunk. This value is always in the range <code>0</code> through to
     * <code>chunkSize</code>
     */
    private int chunkRead;

    /**
     * The internal buffer array where chunk data is available for the
     * application to read.
     */
    private byte chunkData[] = new byte[4096];

    /**
     * The current position in the buffer. It contains the index
     * of the next byte to read from <code>chunkData</code>
     */
    private int chunkPos;

    /**
     * The index one greater than the index of the last valid byte in the
     * buffer. This value is always in the range <code>0</code> through
     * <code>chunkData.length</code>.
     */
    private int chunkCount;

    /**
     * The internal buffer where bytes from the underlying stream can be
     * read. It may contain bytes representing chunk-size, chunk-data, or
     * trailer fields.
     */
    private byte rawData[] = new byte[32];

    /**
     * The current position in the buffer. It contains the index
     * of the next byte to read from <code>rawData</code>
     */
    private int rawPos;

    /**
     * The index one greater than the index of the last valid byte in the
     * buffer. This value is always in the range <code>0</code> through
     * <code>rawData.length</code>.
     */
    private int rawCount;

    /**
     * Indicates if an error was encountered when processing the chunked
     * stream.
     */
    private boolean error;

    /**
     * Indicates if the chunked stream has been closed using the
     * <code>close</code> method.
     */
    private boolean closed;

    private final ReentrantLock readLock = new ReentrantLock();

    /*
     * Maximum chunk header size of 2KB + 2 bytes for CRLF
     */
    private static final int MAX_CHUNK_HEADER_SIZE = 2050;

    /**
     * State to indicate that next field should be :-
     *  chunk-size [ chunk-extension ] CRLF
     */
    static final int STATE_AWAITING_CHUNK_HEADER    = 1;

    /**
     * State to indicate that we are currently reading the chunk-data.
     */
    static final int STATE_READING_CHUNK            = 2;

    /**
     * Indicates that a chunk has been completely read and the next
     * fields to be examine should be CRLF
     */
    static final int STATE_AWAITING_CHUNK_EOL       = 3;

    /**
     * Indicates that all chunks have been read and the next field
     * should be optional trailers or an indication that the chunked
     * stream is complete.
     */
    static final int STATE_AWAITING_TRAILERS        = 4;

    /**
     * State to indicate that the chunked stream is complete and
     * no further bytes should be read from the underlying stream.
     */
    static final int STATE_DONE                     = 5;

    /**
     * Indicates the current state.
     */
    private int state;


    /**
     * Check to make sure that this stream has not been closed.
     */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("stream is closed");
        }
    }


    /**
     * Ensures there is <code>size</code> bytes available in
     * <code>rawData</code>. This requires that we either
     * shift the bytes in use to the begining of the buffer
     * or allocate a large buffer with sufficient space available.
     */
    private void ensureRawAvailable(int size) {
        if (rawCount + size > rawData.length) {
            int used = rawCount - rawPos;
            if (used + size > rawData.length) {
                byte tmp[] = new byte[used + size];
                if (used > 0) {
                    System.arraycopy(rawData, rawPos, tmp, 0, used);
                }
                rawData = tmp;
            } else {
                if (used > 0) {
                    System.arraycopy(rawData, rawPos, rawData, 0, used);
                }
            }
            rawCount = used;
            rawPos = 0;
        }
    }


    /**
     * Close the underlying input stream by either returning it to the
     * keep alive cache or closing the stream.
     * <p>
     * As a chunked stream is inheritly persistent (see HTTP 1.1 RFC) the
     * underlying stream can be returned to the keep alive cache if the
     * stream can be completely read without error.
     */
    private void closeUnderlying() throws IOException {
        if (in == null) {
            return;
        }

        if (!error && state == STATE_DONE) {
            hc.finished();
        } else {
            if (!hurry()) {
                hc.closeServer();
            }
        }

        in = null;
    }

    /**
     * Attempt to read the remainder of a chunk directly into the
     * caller's buffer.
     * <p>
     * Return the number of bytes read.
     */
    private int fastRead(byte[] b, int off, int len) throws IOException {

        // assert state == STATE_READING_CHUNKS;

        int remaining = chunkSize - chunkRead;
        int cnt = (remaining < len) ? remaining : len;
        if (cnt > 0) {
            int nread;
            try {
                nread = in.read(b, off, cnt);
            } catch (IOException e) {
                error = true;
                throw e;
            }
            if (nread > 0) {
                chunkRead += nread;
                if (chunkRead >= chunkSize) {
                    state = STATE_AWAITING_CHUNK_EOL;
                }
                return nread;
            }
            error = true;
            throw new IOException("Premature EOF");
        } else {
            return 0;
        }
    }

    /**
     * Process any outstanding bytes that have already been read into
     * <code>rawData</code>.
     * <p>
     * The parsing of the chunked stream is performed as a state machine with
     * <code>state</code> representing the current state of the processing.
     * <p>
     * Returns when either all the outstanding bytes in rawData have been
     * processed or there is insufficient bytes available to continue
     * processing. When the latter occurs <code>rawPos</code> will not have
     * been updated and thus the processing can be restarted once further
     * bytes have been read into <code>rawData</code>.
     */
    private void processRaw() throws IOException {
        int pos;
        int i;

        while (state != STATE_DONE) {

            switch (state) {

                /**
                 * We are awaiting a line with a chunk header
                 */
                case STATE_AWAITING_CHUNK_HEADER:
                    /*
                     * Find \n to indicate end of chunk header. If not found when there is
                     * insufficient bytes in the raw buffer to parse a chunk header.
                     */
                    pos = rawPos;
                    while (pos < rawCount) {
                        if (rawData[pos] == '\n') {
                            break;
                        }
                        pos++;
                        if ((pos - rawPos) >= MAX_CHUNK_HEADER_SIZE) {
                            error = true;
                            throw new IOException("Chunk header too long");
                        }
                    }
                    if (pos >= rawCount) {
                        return;
                    }

                    /*
                     * Extract the chunk size from the header (ignoring extensions).
                     */
                    String header = new String(rawData, rawPos, pos-rawPos+1,
                            US_ASCII.INSTANCE);
                    for (i=0; i < header.length(); i++) {
                        if (Character.digit(header.charAt(i), 16) == -1)
                            break;
                    }
                    try {
                        chunkSize = Integer.parseInt(header, 0, i, 16);
                    } catch (NumberFormatException e) {
                        error = true;
                        throw new IOException("Bogus chunk size");
                    }

                    /*
                     * Chunk has been parsed so move rawPos to first byte of chunk
                     * data.
                     */
                    rawPos = pos + 1;
                    chunkRead = 0;

                    /*
                     * A chunk size of 0 means EOF.
                     */
                    if (chunkSize > 0) {
                        state = STATE_READING_CHUNK;
                    } else {
                        state = STATE_AWAITING_TRAILERS;
                    }
                    break;


                /**
                 * We are awaiting raw entity data (some may have already been
                 * read). chunkSize is the size of the chunk; chunkRead is the
                 * total read from the underlying stream to date.
                 */
                case STATE_READING_CHUNK :
                    /* no data available yet */
                    if (rawPos >= rawCount) {
                        return;
                    }

                    /*
                     * Compute the number of bytes of chunk data available in the
                     * raw buffer.
                     */
                    int copyLen = Math.min( chunkSize-chunkRead, rawCount-rawPos );

                    /*
                     * Expand or compact chunkData if needed.
                     */
                    if (chunkData.length < chunkCount + copyLen) {
                        int cnt = chunkCount - chunkPos;
                        if (chunkData.length < cnt + copyLen) {
                            byte tmp[] = new byte[cnt + copyLen];
                            System.arraycopy(chunkData, chunkPos, tmp, 0, cnt);
                            chunkData = tmp;
                        } else {
                            System.arraycopy(chunkData, chunkPos, chunkData, 0, cnt);
                        }
                        chunkPos = 0;
                        chunkCount = cnt;
                    }

                    /*
                     * Copy the chunk data into chunkData so that it's available
                     * to the read methods.
                     */
                    System.arraycopy(rawData, rawPos, chunkData, chunkCount, copyLen);
                    rawPos += copyLen;
                    chunkCount += copyLen;
                    chunkRead += copyLen;

                    /*
                     * If all the chunk has been copied into chunkData then the next
                     * token should be CRLF.
                     */
                    if (chunkSize - chunkRead <= 0) {
                        state = STATE_AWAITING_CHUNK_EOL;
                    } else {
                        return;
                    }
                    break;


                /**
                 * Awaiting CRLF after the chunk
                 */
                case STATE_AWAITING_CHUNK_EOL:
                    /* not available yet */
                    if (rawPos + 1 >= rawCount) {
                        return;
                    }

                    if (rawData[rawPos] != '\r') {
                        error = true;
                        throw new IOException("missing CR");
                    }
                    if (rawData[rawPos+1] != '\n') {
                        error = true;
                        throw new IOException("missing LF");
                    }
                    rawPos += 2;

                    /*
                     * Move onto the next chunk
                     */
                    state = STATE_AWAITING_CHUNK_HEADER;
                    break;


                /**
                 * Last chunk has been read so not we're waiting for optional
                 * trailers.
                 */
                case STATE_AWAITING_TRAILERS:

                    /*
                     * Do we have an entire line in the raw buffer?
                     */
                    pos = rawPos;
                    while (pos < rawCount) {
                        if (rawData[pos] == '\n') {
                            break;
                        }
                        pos++;
                    }
                    if (pos >= rawCount) {
                        return;
                    }

                    if (pos == rawPos) {
                        error = true;
                        throw new IOException("LF should be proceeded by CR");
                    }
                    if (rawData[pos-1] != '\r') {
                        error = true;
                        throw new IOException("LF should be proceeded by CR");
                    }

                    /*
                     * Stream done so close underlying stream.
                     */
                    if (pos == (rawPos + 1)) {

                        state = STATE_DONE;
                        closeUnderlying();

                        return;
                    }

                    /*
                     * Extract any tailers and append them to the message
                     * headers.
                     */
                    String trailer = new String(rawData, rawPos, pos-rawPos,
                            US_ASCII.INSTANCE);
                    i = trailer.indexOf(':');
                    if (i == -1) {
                        throw new IOException("Malformed tailer - format should be key:value");
                    }
                    String key = (trailer.substring(0, i)).trim();
                    String value = (trailer.substring(i+1, trailer.length())).trim();

                    responses.add(key, value);

                    /*
                     * Move onto the next trailer.
                     */
                    rawPos = pos+1;
                    break;

            } /* switch */
        }
    }


    /**
     * Reads any available bytes from the underlying stream into
     * <code>rawData</code> and returns the number of bytes of
     * chunk data available in <code>chunkData</code> that the
     * application can read.
     */
    private int readAheadNonBlocking() throws IOException {

        /*
         * If there's anything available on the underlying stream then we read
         * it into the raw buffer and process it. Processing ensures that any
         * available chunk data is made available in chunkData.
         */
        int avail = in.available();
        if (avail > 0) {

            /* ensure that there is space in rawData to read the available */
            ensureRawAvailable(avail);

            int nread;
            try {
                nread = in.read(rawData, rawCount, avail);
            } catch (IOException e) {
                error = true;
                throw e;
            }
            if (nread < 0) {
                error = true;   /* premature EOF ? */
                return -1;
            }
            rawCount += nread;

            /*
             * Process the raw bytes that have been read.
             */
            processRaw();
        }

        /*
         * Return the number of chunked bytes available to read
         */
        return chunkCount - chunkPos;
    }

    /**
     * Reads from the underlying stream until there is chunk data
     * available in <code>chunkData</code> for the application to
     * read.
     */
    private int readAheadBlocking() throws IOException {

        do {
            /*
             * All of chunked response has been read to return EOF.
             */
            if (state == STATE_DONE) {
                return -1;
            }

            /*
             * We must read into the raw buffer so make sure there is space
             * available. We use a size of 32 to avoid too much chunk data
             * being read into the raw buffer.
             */
            ensureRawAvailable(32);
            int nread;
            try {
                nread = in.read(rawData, rawCount, rawData.length-rawCount);
            } catch (IOException e) {
                error = true;
                throw e;
            }

            /**
             * If we hit EOF it means there's a problem as we should never
             * attempt to read once the last chunk and trailers have been
             * received.
             */
            if (nread < 0) {
                error = true;
                throw new IOException("Premature EOF");
            }

            /**
             * Process the bytes from the underlying stream
             */
            rawCount += nread;
            processRaw();

        } while (chunkCount <= 0);

        /*
         * Return the number of chunked bytes available to read
         */
        return chunkCount - chunkPos;
    }

    /**
     * Read ahead in either blocking or non-blocking mode. This method
     * is typically used when we run out of available bytes in
     * <code>chunkData</code> or we need to determine how many bytes
     * are available on the input stream.
     */
    private int readAhead(boolean allowBlocking) throws IOException {

        /*
         * Last chunk already received - return EOF
         */
        if (state == STATE_DONE) {
            return -1;
        }

        /*
         * Reset position/count if data in chunkData is exhausted.
         */
        if (chunkPos >= chunkCount) {
            chunkCount = 0;
            chunkPos = 0;
        }

        /*
         * Read ahead blocking or non-blocking
         */
        if (allowBlocking) {
            return readAheadBlocking();
        } else {
            return readAheadNonBlocking();
        }
    }

    /**
     * Creates a <code>ChunkedInputStream</code> and saves its  arguments, for
     * later use.
     *
     * @param   in   the underlying input stream.
     * @param   hc   the HttpClient
     * @param   responses   the MessageHeader that should be populated with optional
     *                      trailers.
     */
    public ChunkedInputStream(InputStream in, HttpClient hc, MessageHeader responses) throws IOException {

        /* save arguments */
        this.in = in;
        this.responses = responses;
        this.hc = hc;

        /*
         * Set our initial state to indicate that we are first starting to
         * look for a chunk header.
         */
        state = STATE_AWAITING_CHUNK_HEADER;
    }

    /**
     * See
     * the general contract of the <code>read</code>
     * method of <code>InputStream</code>.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterInputStream#in
     */
    public int read() throws IOException {
        readLock.lock();
        try {
            ensureOpen();
            if (chunkPos >= chunkCount) {
                if (readAhead(true) <= 0) {
                    return -1;
                }
            }
            return chunkData[chunkPos++] & 0xff;
        } finally {
            readLock.unlock();
        }
    }


    /**
     * Reads bytes from this stream into the specified byte array, starting at
     * the given offset.
     *
     * @param      b     destination buffer.
     * @param      off   offset at which to start storing bytes.
     * @param      len   maximum number of bytes to read.
     * @return     the number of bytes read, or <code>-1</code> if the end of
     *             the stream has been reached.
     * @exception  IOException  if an I/O error occurs.
     */
    public int read(byte b[], int off, int len)
        throws IOException
    {
        readLock.lock();
        try {
            ensureOpen();
            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int avail = chunkCount - chunkPos;
            if (avail <= 0) {
                /*
                 * Optimization: if we're in the middle of the chunk read
                 * directly from the underlying stream into the caller's
                 * buffer
                 */
                if (state == STATE_READING_CHUNK) {
                    return fastRead(b, off, len);
                }

                /*
                 * We're not in the middle of a chunk so we must read ahead
                 * until there is some chunk data available.
                 */
                avail = readAhead(true);
                if (avail < 0) {
                    return -1;      /* EOF */
                }
            }
            int cnt = (avail < len) ? avail : len;
            System.arraycopy(chunkData, chunkPos, b, off, cnt);
            chunkPos += cnt;

            return cnt;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     *
     * @return     the number of bytes that can be read from this input
     *             stream without blocking.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterInputStream#in
     */
    public int available() throws IOException {
        readLock.lock();
        try {
            ensureOpen();

            int avail = chunkCount - chunkPos;
            if (avail > 0) {
                return avail;
            }

            avail = readAhead(false);

            if (avail < 0) {
                return 0;
            } else {
                return avail;
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Close the stream by either returning the connection to the
     * keep alive cache or closing the underlying stream.
     * <p>
     * If the chunked response hasn't been completely read we
     * try to "hurry" to the end of the response. If this is
     * possible (without blocking) then the connection can be
     * returned to the keep alive cache.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        if (closed) return;
        readLock.lock();
        try {
            if (closed) {
                return;
            }
            closeUnderlying();
            closed = true;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Hurry the input stream by reading everything from the underlying
     * stream. If the last chunk (and optional trailers) can be read without
     * blocking then the stream is considered hurried.
     * <p>
     * Note that if an error has occurred or we can't get to last chunk
     * without blocking then this stream can't be hurried and should be
     * closed.
     */
    public boolean hurry() {
        readLock.lock();
        try {
            if (in == null || error) {
                return false;
            }

            try {
                readAhead(false);
            } catch (Exception e) {
                return false;
            }

            if (error) {
                return false;
            }

            return (state == STATE_DONE);
        } finally {
            readLock.unlock();
        }
    }

}
