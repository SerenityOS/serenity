/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sun.nio.cs.US_ASCII;

/**
 * OutputStream that sends the output to the underlying stream using chunked
 * encoding as specified in RFC 2068.
 */
public class ChunkedOutputStream extends OutputStream {

    /* Default chunk size (including chunk header) if not specified */
    static final int DEFAULT_CHUNK_SIZE = 4096;
    private static final byte[] CRLF = {'\r', '\n'};
    private static final int CRLF_SIZE = CRLF.length;
    private static final byte[] FOOTER = CRLF;
    private static final int FOOTER_SIZE = CRLF_SIZE;
    private static final byte[] EMPTY_CHUNK_HEADER = getHeader(0);
    private static final int EMPTY_CHUNK_HEADER_SIZE = getHeaderSize(0);

    /* internal buffer */
    private byte buf[];
    /* size of data (excluding footers and headers) already stored in buf */
    private int size;
    /* current index in buf (i.e. buf[count] */
    private int count;
    /* number of bytes to be filled up to complete a data chunk
     * currently being built */
    private int spaceInCurrentChunk;

    /* underlying stream */
    private PrintStream out;

    /* the chunk size we use */
    private int preferredChunkDataSize;
    private int preferedHeaderSize;
    private int preferredChunkGrossSize;
    /* header for a complete Chunk */
    private byte[] completeHeader;

    private final Lock writeLock = new ReentrantLock();

    /* return the size of the header for a particular chunk size */
    private static int getHeaderSize(int size) {
        return (Integer.toHexString(size)).length() + CRLF_SIZE;
    }

    /* return a header for a particular chunk size */
    private static byte[] getHeader(int size) {
        String hexStr = Integer.toHexString(size);
        byte[] hexBytes = hexStr.getBytes(US_ASCII.INSTANCE);
        byte[] header = new byte[getHeaderSize(size)];
        for (int i=0; i<hexBytes.length; i++)
            header[i] = hexBytes[i];
        header[hexBytes.length] = CRLF[0];
        header[hexBytes.length+1] = CRLF[1];
        return header;
    }

    public ChunkedOutputStream(PrintStream o) {
        this(o, DEFAULT_CHUNK_SIZE);
    }

    public ChunkedOutputStream(PrintStream o, int size) {
        out = o;

        if (size <= 0) {
            size = DEFAULT_CHUNK_SIZE;
        }

        /* Adjust the size to cater for the chunk header - eg: if the
         * preferred chunk size is 1k this means the chunk size should
         * be 1017 bytes (differs by 7 from preferred size because of
         * 3 bytes for chunk size in hex and CRLF (header) and CRLF (footer)).
         *
         * If headerSize(adjusted_size) is shorter then headerSize(size)
         * then try to use the extra byte unless headerSize(adjusted_size+1)
         * increases back to headerSize(size)
         */
        if (size > 0) {
            int adjusted_size = size - getHeaderSize(size) - FOOTER_SIZE;
            if (getHeaderSize(adjusted_size+1) < getHeaderSize(size)){
                adjusted_size++;
            }
            size = adjusted_size;
        }

        if (size > 0) {
            preferredChunkDataSize = size;
        } else {
            preferredChunkDataSize = DEFAULT_CHUNK_SIZE -
                    getHeaderSize(DEFAULT_CHUNK_SIZE) - FOOTER_SIZE;
        }

        preferedHeaderSize = getHeaderSize(preferredChunkDataSize);
        preferredChunkGrossSize = preferedHeaderSize + preferredChunkDataSize
                + FOOTER_SIZE;
        completeHeader = getHeader(preferredChunkDataSize);

        /* start with an initial buffer */
        buf = new byte[preferredChunkGrossSize];
        reset();
    }

    /*
     * Flush a buffered, completed chunk to an underlying stream. If the data in
     * the buffer is insufficient to build up a chunk of "preferredChunkSize"
     * then the data do not get flushed unless flushAll is true. If flushAll is
     * true then the remaining data builds up a last chunk which size is smaller
     * than preferredChunkSize, and then the last chunk gets flushed to
     * underlying stream. If flushAll is true and there is no data in a buffer
     * at all then an empty chunk (containing a header only) gets flushed to
     * underlying stream.
     */
     private void flush(boolean flushAll) {
        if (spaceInCurrentChunk == 0) {
            /* flush a completed chunk to underlying stream */
            out.write(buf, 0, preferredChunkGrossSize);
            out.flush();
            reset();
        } else if (flushAll){
            /* complete the last chunk and flush it to underlying stream */
            if (size > 0) {
                /* adjust a header start index in case the header of the last
                 * chunk is shorter then preferedHeaderSize */

                int adjustedHeaderStartIndex = preferedHeaderSize -
                        getHeaderSize(size);

                /* write header */
                System.arraycopy(getHeader(size), 0, buf,
                        adjustedHeaderStartIndex, getHeaderSize(size));

                /* write footer */
                buf[count++] = FOOTER[0];
                buf[count++] = FOOTER[1];

                //send the last chunk to underlying stream
                out.write(buf, adjustedHeaderStartIndex, count - adjustedHeaderStartIndex);
            } else {
                //send an empty chunk (containing just a header) to underlying stream
                out.write(EMPTY_CHUNK_HEADER, 0, EMPTY_CHUNK_HEADER_SIZE);
            }

            out.flush();
            reset();
        }
    }

    public boolean checkError() {
        var out = this.out;
        return out == null || out.checkError();
    }

    /* Check that the output stream is still open */
    private void ensureOpen() throws IOException {
        if (out == null)
            throw new IOException("closed");
    }

   /*
    * Writes data from b[] to an internal buffer and stores the data as data
    * chunks of a following format: {Data length in Hex}{CRLF}{data}{CRLF}
    * The size of the data is preferredChunkSize. As soon as a completed chunk
    * is read from b[] a process of reading from b[] suspends, the chunk gets
    * flushed to the underlying stream and then the reading process from b[]
    * continues. When there is no more sufficient data in b[] to build up a
    * chunk of preferredChunkSize size the data get stored as an incomplete
    * chunk of a following format: {space for data length}{CRLF}{data}
    * The size of the data is of course smaller than preferredChunkSize.
    */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        writeLock.lock();
        try {
            ensureOpen();
            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }

            /* if b[] contains enough data then one loop cycle creates one complete
             * data chunk with a header, body and a footer, and then flushes the
             * chunk to the underlying stream. Otherwise, the last loop cycle
             * creates incomplete data chunk with empty header and with no footer
             * and stores this incomplete chunk in an internal buffer buf[]
             */
            int bytesToWrite = len;
            int inputIndex = off;  /* the index of the byte[] currently being written */

            do {
                /* enough data to complete a chunk */
                if (bytesToWrite >= spaceInCurrentChunk) {

                    /* header */
                    for (int i = 0; i < completeHeader.length; i++)
                        buf[i] = completeHeader[i];

                    /* data */
                    System.arraycopy(b, inputIndex, buf, count, spaceInCurrentChunk);
                    inputIndex += spaceInCurrentChunk;
                    bytesToWrite -= spaceInCurrentChunk;
                    count += spaceInCurrentChunk;

                    /* footer */
                    buf[count++] = FOOTER[0];
                    buf[count++] = FOOTER[1];
                    spaceInCurrentChunk = 0; //chunk is complete

                    flush(false);
                    if (checkError()) {
                        break;
                    }
                }

                /* not enough data to build a chunk */
                else {
                    /* header */
                    /* do not write header if not enough bytes to build a chunk yet */

                    /* data */
                    System.arraycopy(b, inputIndex, buf, count, bytesToWrite);
                    count += bytesToWrite;
                    size += bytesToWrite;
                    spaceInCurrentChunk -= bytesToWrite;
                    bytesToWrite = 0;

                    /* footer */
                    /* do not write header if not enough bytes to build a chunk yet */
                }
            } while (bytesToWrite > 0);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void write(int _b) throws IOException {
        writeLock.lock();
        try {
            byte b[] = {(byte) _b};
            write(b, 0, 1);
        } finally {
            writeLock.unlock();
        }
    }

    public void reset() {
        writeLock.lock();
        try {
            count = preferedHeaderSize;
            size = 0;
            spaceInCurrentChunk = preferredChunkDataSize;
        } finally {
            writeLock.unlock();
        }
    }

    public int size() {
        return size;
    }

    @Override
    public void close() {
        writeLock.lock();
        try {
           if (out == null) return;

            /* if we have buffer a chunked send it */
            if (size > 0) {
                flush(true);
            }

            /* send a zero length chunk */
            flush(true);

            /* don't close the underlying stream */
            out = null;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        writeLock.lock();
        try {
            ensureOpen();
            if (size > 0) {
                flush(true);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
