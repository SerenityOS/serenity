/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;

/**
 * An implementation of {@code ImageOutputStream} that writes its
 * output to a regular {@code OutputStream}.  A memory buffer is
 * used to cache at least the data between the discard position and
 * the current write position.  The only constructor takes an
 * {@code OutputStream}, so this class may not be used for
 * read/modify/write operations.  Reading can occur only on parts of
 * the stream that have already been written to the cache and not
 * yet flushed.
 *
 */
public class MemoryCacheImageOutputStream extends ImageOutputStreamImpl {

    private OutputStream stream;

    private MemoryCache cache = new MemoryCache();

    /**
     * Constructs a {@code MemoryCacheImageOutputStream} that will write
     * to a given {@code OutputStream}.
     *
     * @param stream an {@code OutputStream} to write to.
     *
     * @exception IllegalArgumentException if {@code stream} is
     * {@code null}.
     */
    public MemoryCacheImageOutputStream(OutputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        this.stream = stream;
    }

    public int read() throws IOException {
        checkClosed();

        bitOffset = 0;

        int val = cache.read(streamPos);
        if (val != -1) {
            ++streamPos;
        }
        return val;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();

        if (b == null) {
            throw new NullPointerException("b == null!");
        }
        // Fix 4467608: read([B,I,I) works incorrectly if len<=0
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off+len > b.length || off+len < 0!");
        }

        bitOffset = 0;

        if (len == 0) {
            return 0;
        }

        // check if we're already at/past EOF i.e.
        // no more bytes left to read from cache
        long bytesLeftInCache = cache.getLength() - streamPos;
        if (bytesLeftInCache <= 0) {
            return -1; // EOF
        }

        // guaranteed by now that bytesLeftInCache > 0 && len > 0
        // and so the rest of the error checking is done by cache.read()
        // NOTE that alot of error checking is duplicated
        len = (int)Math.min(bytesLeftInCache, (long)len);
        cache.read(b, off, len, streamPos);
        streamPos += len;
        return len;
    }

    public void write(int b) throws IOException {
        flushBits(); // this will call checkClosed() for us
        cache.write(b, streamPos);
        ++streamPos;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        flushBits(); // this will call checkClosed() for us
        cache.write(b, off, len, streamPos);
        streamPos += len;
    }

    public long length() {
        try {
            checkClosed();
            return cache.getLength();
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Returns {@code true} since this
     * {@code ImageOutputStream} caches data in order to allow
     * seeking backwards.
     *
     * @return {@code true}.
     *
     * @see #isCachedMemory
     * @see #isCachedFile
     */
    public boolean isCached() {
        return true;
    }

    /**
     * Returns {@code false} since this
     * {@code ImageOutputStream} does not maintain a file cache.
     *
     * @return {@code false}.
     *
     * @see #isCached
     * @see #isCachedMemory
     */
    public boolean isCachedFile() {
        return false;
    }

    /**
     * Returns {@code true} since this
     * {@code ImageOutputStream} maintains a main memory cache.
     *
     * @return {@code true}.
     *
     * @see #isCached
     * @see #isCachedFile
     */
    public boolean isCachedMemory() {
        return true;
    }

    /**
     * Closes this {@code MemoryCacheImageOutputStream}.  All
     * pending data is flushed to the output, and the cache
     * is released.  The destination {@code OutputStream}
     * is not closed.
     */
    public void close() throws IOException {
        long length = cache.getLength();
        seek(length);
        flushBefore(length);
        super.close();
        cache.reset();
        cache = null;
        stream = null;
    }

    public void flushBefore(long pos) throws IOException {
        long oFlushedPos = flushedPos;
        super.flushBefore(pos); // this will call checkClosed() for us

        long flushBytes = flushedPos - oFlushedPos;
        cache.writeToStream(stream, oFlushedPos, flushBytes);
        cache.disposeBefore(flushedPos);
        stream.flush();
    }
}
