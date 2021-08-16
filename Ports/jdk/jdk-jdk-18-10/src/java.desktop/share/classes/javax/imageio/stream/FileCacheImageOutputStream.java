/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import com.sun.imageio.stream.StreamCloser;

/**
 * An implementation of {@code ImageOutputStream} that writes its
 * output to a regular {@code OutputStream}.  A file is used to
 * cache data until it is flushed to the output stream.
 *
 */
public class FileCacheImageOutputStream extends ImageOutputStreamImpl {

    private OutputStream stream;

    private File cacheFile;

    private RandomAccessFile cache;

    // Pos after last (rightmost) byte written
    private long maxStreamPos = 0L;

    /** The CloseAction that closes the stream in
     *  the StreamCloser's shutdown hook                     */
    private final StreamCloser.CloseAction closeAction;

    /**
     * Constructs a {@code FileCacheImageOutputStream} that will write
     * to a given {@code outputStream}.
     *
     * <p> A temporary file is used as a cache.  If
     * {@code cacheDir} is non-{@code null} and is a
     * directory, the file will be created there.  If it is
     * {@code null}, the system-dependent default temporary-file
     * directory will be used (see the documentation for
     * {@code File.createTempFile} for details).
     *
     * @param stream an {@code OutputStream} to write to.
     * @param cacheDir a {@code File} indicating where the
     * cache file should be created, or {@code null} to use the
     * system directory.
     *
     * @exception IllegalArgumentException if {@code stream}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code cacheDir} is
     * non-{@code null} but is not a directory.
     * @exception IOException if a cache file cannot be created.
     */
    public FileCacheImageOutputStream(OutputStream stream, File cacheDir)
        throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        if ((cacheDir != null) && !(cacheDir.isDirectory())) {
            throw new IllegalArgumentException("Not a directory!");
        }
        this.stream = stream;
        if (cacheDir == null)
            this.cacheFile = Files.createTempFile("imageio", ".tmp").toFile();
        else
            this.cacheFile = Files.createTempFile(cacheDir.toPath(), "imageio", ".tmp")
                                  .toFile();
        this.cache = new RandomAccessFile(cacheFile, "rw");

        this.closeAction = StreamCloser.createCloseAction(this);
        StreamCloser.addToQueue(closeAction);
    }

    public int read() throws IOException {
        checkClosed();
        bitOffset = 0;
        int val =  cache.read();
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
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off+len > b.length || off+len < 0!");
        }

        bitOffset = 0;

        if (len == 0) {
            return 0;
        }

        int nbytes = cache.read(b, off, len);
        if (nbytes != -1) {
            streamPos += nbytes;
        }
        return nbytes;
    }

    public void write(int b) throws IOException {
        flushBits(); // this will call checkClosed() for us
        cache.write(b);
        ++streamPos;
        maxStreamPos = Math.max(maxStreamPos, streamPos);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        flushBits(); // this will call checkClosed() for us
        cache.write(b, off, len);
        streamPos += len;
        maxStreamPos = Math.max(maxStreamPos, streamPos);
    }

    public long length() {
        try {
            checkClosed();
            return cache.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Sets the current stream position and resets the bit offset to
     * 0.  It is legal to seek past the end of the file; an
     * {@code EOFException} will be thrown only if a read is
     * performed.  The file length will not be increased until a write
     * is performed.
     *
     * @exception IndexOutOfBoundsException if {@code pos} is smaller
     * than the flushed position.
     * @exception IOException if any other I/O error occurs.
     */
    public void seek(long pos) throws IOException {
        checkClosed();

        if (pos < flushedPos) {
            throw new IndexOutOfBoundsException();
        }

        cache.seek(pos);
        this.streamPos = cache.getFilePointer();
        maxStreamPos = Math.max(maxStreamPos, streamPos);
        this.bitOffset = 0;
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
     * Returns {@code true} since this
     * {@code ImageOutputStream} maintains a file cache.
     *
     * @return {@code true}.
     *
     * @see #isCached
     * @see #isCachedMemory
     */
    public boolean isCachedFile() {
        return true;
    }

    /**
     * Returns {@code false} since this
     * {@code ImageOutputStream} does not maintain a main memory
     * cache.
     *
     * @return {@code false}.
     *
     * @see #isCached
     * @see #isCachedFile
     */
    public boolean isCachedMemory() {
        return false;
    }

    /**
     * Closes this {@code FileCacheImageOutputStream}.  All
     * pending data is flushed to the output, and the cache file
     * is closed and removed.  The destination {@code OutputStream}
     * is not closed.
     *
     * @exception IOException if an error occurs.
     */
    public void close() throws IOException {
        maxStreamPos = cache.length();

        seek(maxStreamPos);
        flushBefore(maxStreamPos);
        super.close();
        cache.close();
        cache = null;
        cacheFile.delete();
        cacheFile = null;
        stream.flush();
        stream = null;
        StreamCloser.removeFromQueue(closeAction);
    }

    public void flushBefore(long pos) throws IOException {
        long oFlushedPos = flushedPos;
        super.flushBefore(pos); // this will call checkClosed() for us

        long flushBytes = flushedPos - oFlushedPos;
        if (flushBytes > 0) {
            int bufLen = 512;
            byte[] buf = new byte[bufLen];
            cache.seek(oFlushedPos);
            while (flushBytes > 0) {
                int len = (int)Math.min(flushBytes, bufLen);
                cache.readFully(buf, 0, len);
                stream.write(buf, 0, len);
                flushBytes -= len;
            }
            stream.flush();
        }
    }
}
