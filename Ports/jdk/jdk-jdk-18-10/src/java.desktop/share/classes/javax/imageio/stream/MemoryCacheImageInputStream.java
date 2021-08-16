/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.IOException;
import com.sun.imageio.stream.StreamFinalizer;
import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;

/**
 * An implementation of {@code ImageInputStream} that gets its
 * input from a regular {@code InputStream}.  A memory buffer is
 * used to cache at least the data between the discard position and
 * the current read position.
 *
 * <p> In general, it is preferable to use a
 * {@code FileCacheImageInputStream} when reading from a regular
 * {@code InputStream}.  This class is provided for cases where
 * it is not possible to create a writable temporary file.
 *
 */
public class MemoryCacheImageInputStream extends ImageInputStreamImpl {

    private InputStream stream;

    private MemoryCache cache = new MemoryCache();

    /** The referent to be registered with the Disposer. */
    private final Object disposerReferent;

    /** The DisposerRecord that resets the underlying MemoryCache. */
    private final DisposerRecord disposerRecord;

    /**
     * Constructs a {@code MemoryCacheImageInputStream} that will read
     * from a given {@code InputStream}.
     *
     * @param stream an {@code InputStream} to read from.
     *
     * @exception IllegalArgumentException if {@code stream} is
     * {@code null}.
     */
    public MemoryCacheImageInputStream(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        this.stream = stream;

        disposerRecord = new StreamDisposerRecord(cache);
        if (getClass() == MemoryCacheImageInputStream.class) {
            disposerReferent = new Object();
            Disposer.addRecord(disposerReferent, disposerRecord);
        } else {
            disposerReferent = new StreamFinalizer(this);
        }
    }

    public int read() throws IOException {
        checkClosed();
        bitOffset = 0;
        long pos = cache.loadFromStream(stream, streamPos+1);
        if (pos >= streamPos+1) {
            return cache.read(streamPos++);
        } else {
            return -1;
        }
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

        long pos = cache.loadFromStream(stream, streamPos+len);

        len = (int)(pos - streamPos);  // In case stream ended early

        if (len > 0) {
            cache.read(b, off, len, streamPos);
            streamPos += len;
            return len;
        } else {
            return -1;
        }
    }

    public void flushBefore(long pos) throws IOException {
        super.flushBefore(pos); // this will call checkClosed() for us
        cache.disposeBefore(pos);
    }

    /**
     * Returns {@code true} since this
     * {@code ImageInputStream} caches data in order to allow
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
     * {@code ImageInputStream} does not maintain a file cache.
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
     * {@code ImageInputStream} maintains a main memory cache.
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
     * Closes this {@code MemoryCacheImageInputStream}, freeing
     * the cache.  The source {@code InputStream} is not closed.
     */
    public void close() throws IOException {
        super.close();
        disposerRecord.dispose(); // this resets the MemoryCache
        stream = null;
        cache = null;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated The {@code finalize} method has been deprecated.
     *     Subclasses that override {@code finalize} in order to perform cleanup
     *     should be modified to use alternative cleanup mechanisms and
     *     to remove the overriding {@code finalize} method.
     *     When overriding the {@code finalize} method, its implementation must explicitly
     *     ensure that {@code super.finalize()} is invoked as described in {@link Object#finalize}.
     *     See the specification for {@link Object#finalize()} for further
     *     information about migration options.
     */
    @Deprecated(since="9")
    protected void finalize() throws Throwable {
        // Empty finalizer: for performance reasons we instead use the
        // Disposer mechanism for ensuring that the underlying
        // MemoryCache is reset prior to garbage collection
    }

    private static class StreamDisposerRecord implements DisposerRecord {
        private MemoryCache cache;

        public StreamDisposerRecord(MemoryCache cache) {
            this.cache = cache;
        }

        public synchronized void dispose() {
            if (cache != null) {
                cache.reset();
                cache = null;
            }
        }
    }
}
