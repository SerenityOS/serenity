/*
 * Copyright (c) 2020 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.test.lib.hprof.parser;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class GzipRandomAccess implements AutoCloseable, Closeable {
    // A comparator which compares chunks by their file offset.
    private static FileOffsetComparator fileOffsetComp = new FileOffsetComparator();

    // A comparator which compares chunks by their offset.
    private static OffsetComparator offsetComp = new OffsetComparator();

    // The size to use when reading from the random access file.
    private static final int READ_SIZE = 65536;

    // The last used buffer.
    private Buffer last;

    // The underlying random access file to use.
    private final RandomAccessFile raf;

    // The length of the random access file.
    private final long fileSize;

    // The maximum size of a buffer cache.
    private final int cacheSize;

    // The maximum numbers of buffers to cache.
    private final int maxCachedBuffers;

    // A buffer used to read from the file.
    private final byte[] in;

    // A sorted list of the buffers we know so far.
    private final ArrayList<Buffer> buffers;

    // The inflater to use.
    private final Inflater inf;

    // The head of the list which contains the buffers with cached data.
    private final Buffer cacheHead;

    // The number of cached buffers in the list.
    private int cachedBuffers;

    // This is private to ensure we only handle the specific hprof gzip files
    // written by the VM.
    private GzipRandomAccess(String file, int bufferSize, int maxCachedBuffers)
            throws IOException {
        last = null;
        raf = new RandomAccessFile(file, "r");
        fileSize = raf.length();
        this.cacheSize = bufferSize;
        this.maxCachedBuffers = maxCachedBuffers;
        cachedBuffers = 0;
        in = new byte[READ_SIZE];
        buffers = new ArrayList<>();
        inf = new Inflater(true);
        cacheHead = new Buffer(-1, -1);
        cacheHead.next = cacheHead;
        cacheHead.prev = cacheHead;
        buffers.add(new Buffer(0, 0));
    }

    /**
     * Clears the cache.
     */
    public synchronized void clearCache() {
        while (cacheHead.next != cacheHead) {
            assert cacheHead.next.cache != null;
            Buffer buf = cacheHead.next;
            remove(buf);
            buf.cache = null;
        }

        last = null;
        cachedBuffers = 0;
    }

    /**
     * Returns an approximate file offset for the given offset. The return value should
     * only be used for progress indication and the like. Note that you should only query
     * offsets you've already read.
     *
     * @param offset The offset.
     * @return The approximate file offset.
     */
    public synchronized long getFileOffset(long offset) {
        int pos = Collections.binarySearch(buffers, new Buffer(0, offset), offsetComp);
        int realPos = pos >= 0 ? pos : -pos - 2;

        if (realPos >= buffers.size() - 1) {
            return buffers.get(buffers.size() - 1).fileOffset;
        }

        // Assume uniform compression.
        Buffer buf = buffers.get(realPos);
        long diff = offset - buf.offset;
        long bufFileEnd = buffers.get(realPos + 1).fileOffset;
        long fileDiff = bufFileEnd - buf.fileOffset;
        double filePerDiff = (double) Math.max(1, fileDiff) / Math.max(1, buf.cacheLen);

        return buf.fileOffset + (long) (filePerDiff * diff);
    }

    /**
     * @return Returns the size of the underlying file.
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Returns an @link {@link InputStream} to read from the given offset. Note
     * that closing the input stream does not close the underlying @link
     * {@link GzipRandomAccess} object.
     *
     * @param offset The offset.
     * @return The input stream.
     */
    public InputStream asStream(long offset) {
        return new InputStreamImpl(offset, this);
    }

    /**
     * Returns a @link ReadBuffer which uses this object to do the actual
     * operation. Note that closing the returned object closes the
     * underlying @link {@link GzipRandomAccess} object.
     *
     * @return The @link ReadBuffer.
     */
    public ReadBuffer asFileBuffer() {
        return new ReadBufferImpl(this);
    }

    /**
     * Closes the object and clears the cache. The object is unusable after this
     * call.
     */
    @Override
    public synchronized void close() throws IOException {
        clearCache();
        buffers.clear();
        raf.close();
        inf.end();
    }

    /**
     * Reads bytes from the gzip file.
     *
     * @param offset The offset from which to start the read.
     * @param b The buffer to read into.
     * @param off The offset in the buffer to use.
     * @param len The number of bytes to read at most.
     * @return The number of bytes read or -1 if we are at the end of the file.
     * @throws IOException On error.
     */
    public synchronized int read(long offset, byte[] b, int off, int len) throws IOException {
        Buffer buf = last;

        while (buf == null || (buf.offset > offset) || (buf.offset + buf.cacheLen <= offset)) {
            int pos = Collections.binarySearch(buffers, new Buffer(0, offset), offsetComp);
            buf = buffers.get(pos >= 0 ? pos : -pos - 2);

            if (buf.fileOffset >= fileSize) {
                return -1;
            }

            if (buf.cache != null) {
                // If already loaded, move to front of the cache list.
                last = buf;

                if (cacheHead.next != buf) {
                    remove(buf);
                    addFirst(buf);
                }
            } else {
                try {
                    // Note that the load will also add the following buffer to the list,
                    // so the while loop will eventually terminate.
                    loadBuffer(buf);
                } catch (DataFormatException e) {
                    throw new IOException(e);
                }
            }
        }

        int copyOffset = (int) (offset - buf.offset);
        int toCopyMax = buf.cacheLen - copyOffset;
        int toCopy = Math.min(toCopyMax, len);

        if (toCopy <= 0) {
            return -1;
        }

        System.arraycopy(buf.cache, copyOffset, b, off, toCopy);

        return toCopy;
    }

    /**
     * Returns the access object for the given file or <code>null</code> if not
     * supported for the file.
     *
     * @param file The file name.
     * @param cacheSizeInMB The size of the cache to use in megabytes.
     * @return The access object or <code>null</code>.
     */
    public static GzipRandomAccess getAccess(String file, int cacheSizeInMB)
            throws IOException {
        try  (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int header = raf.readInt();

            if ((header >>> 8) != 0x1f8b08) {
                // No gzip with deflate.
                return null;
            }

            if ((header & 16) == 0) {
                // No comment
                return null;
            }

            raf.readInt(); // timestamp
            raf.readChar(); // Extra flags and os.

            if ((header & 4) != 0) {
                // Skip extra flags.
                raf.skipBytes(raf.read() + (raf.read() << 256));
            }

            // Skip name
            if ((header & 8) != 0) {
                while (raf.read() != 0) {
                    // Wait for the last 0.
                }
            }

            // Read the comment.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int b;

            while ((b = raf.read()) > 0) {
                bos.write(b);
            }

            // Check if the block size is included in the comment.
            String comment = bos.toString("UTF-8");
            String expectedPrefix = "HPROF BLOCKSIZE=";

            if (comment.startsWith(expectedPrefix)) {
                String chunkSizeStr = comment.substring(expectedPrefix.length()).split(" ")[0];

                try {
                    int chunkSize = Integer.parseInt(chunkSizeStr);

                    if (chunkSize > 0) {
                        long nrOfChunks = Math.max(1000, cacheSizeInMB * 1024L * 1024L /
                                                         chunkSize);

                        return new GzipRandomAccess(file, chunkSize, (int) nrOfChunks);
                    }
                } catch (NumberFormatException e) {
                    // Could not parse.
                }
            }
        }

        return null;
    }

    // Loads the content of a buffer. If this is the first time the buffer is
    // loaded, the next buffer is added too (but not loaded).
    private void loadBuffer(Buffer buf) throws IOException, DataFormatException {
        // If we have used all caches, take a cache from the least recently used cached buffer.
        if (cachedBuffers >= maxCachedBuffers) {
            Buffer toRemove = cacheHead.prev;
            remove(toRemove);
            buf.cache = toRemove.cache;
            toRemove.cache = null;
        } else {
            // Otherwise allocate a new cache.
            buf.cache = new byte[cacheSize];
            cachedBuffers += 1;
        }

        // Move to front of LRU list.
        last = buf;
        addFirst(buf);

        // Fill in the cache
        inf.reset();
        raf.seek(buf.fileOffset);

        int read = raf.read(in, 0, READ_SIZE);
        int inCount = read;
        int outCount = 0;

        // Skip header, but check at least a little
        if (read < 4) {
            throw new IOException("Missing data");
        }

        if ((in[0] != 0x1f) || ((in[1] & 0xff) != 0x8b)) {
            throw new IOException("Missing gzip id");
        }

        if (in[2] != 8) {
            throw new IOException("Only supports deflate");
        }

        int off = 10;

        // Extras
        if ((in[3] & 4) != 0) {
            int len = (in[off + 1] & 0xff) * 256 + (in[off] & 0xff);
            off += 2 + len;
        }

        // Name
        if ((in[3] & 8) != 0) {
            int len = 0;

            while (in[off + len] != 0) {
                ++len;
            }

            off += len + 1;
        }

        // Comment
        if ((in[3] & 16) != 0) {
            int len = 0;

            while (in[off + len] != 0) {
                ++len;
            }

            off += len + 1;
        }

        // Header CRC
        if ((in[3] & 2) != 0) {
            off += 2;
        }

        inf.setInput(in, off, read - off);
        outCount = inf.inflate(buf.cache, 0, buf.cache.length);

        while (!inf.finished()) {
            if (inf.needsInput()) {
                read = raf.read(in, 0, READ_SIZE);
                inf.setInput(in, 0, read);
                inCount += read;
            }

            outCount += inf.inflate(buf.cache, outCount, buf.cache.length - outCount);
        }

        // Add the following buffer too if needed.
        if ((inf.getRemaining() != 0) || (inCount + buf.fileOffset + 8 != fileSize)) {
            long nextFileOffset = inCount - inf.getRemaining() + buf.fileOffset + 8 /* CRC */;
            long nextOffset = outCount + buf.offset;

            Buffer nextChunk = new Buffer(nextFileOffset, nextOffset);
            int pos = Collections.binarySearch(buffers, nextChunk, fileOffsetComp);

            if (pos < 0) {
                buffers.add(-pos - 1, nextChunk);
            }
        }

        buf.cacheLen = outCount;
    }

    // Adds the buffer to the front of the LRU list.
    private void addFirst(Buffer buf) {
        assert buf.next == null;
        assert buf.prev == null;
        assert buf.cache != null;

        if (cacheHead.prev == cacheHead) {
            cacheHead.prev = buf;
        }

        cacheHead.next.prev = buf;
        buf.next = cacheHead.next;
        buf.prev = cacheHead;
        cacheHead.next = buf;
    }

    // Removes the buffer from the LRU list.
    private void remove(Buffer buf) {
        assert buf.prev != null;
        assert buf.next != null;
        assert buf.cache != null;
        assert cacheHead.prev != cacheHead;

        buf.prev.next = buf.next;
        buf.next.prev = buf.prev;
        buf.next = null;
        buf.prev = null;
    }

    // Represents a gzipped buffer. The gzipped hprof file consists of a list of these buffers.
    private static class Buffer {
        public byte[] cache;
        public int cacheLen;
        public final long fileOffset;
        public final long offset;
        public Buffer next;
        public Buffer prev;

        public Buffer(long fileOffset, long offset) {
            this.cache = null;
            this.cacheLen = 0;
            this.fileOffset = fileOffset;
            this.offset = offset;
            this.next = null;
            this.prev = null;
        }
    }

    // Compares chunks by file offset.
    private static class FileOffsetComparator implements Comparator<Buffer> {

        @Override
        public int compare(Buffer x, Buffer y) {
            return Long.compare(x.fileOffset, y.fileOffset);
        }
    }

    // Compares chunks by offset.
    private static class OffsetComparator implements Comparator<Buffer> {

        @Override
        public int compare(Buffer x, Buffer y) {
            return Long.compare(x.offset, y.offset);
        }
    }

    // Implements an InputStream for this object.
    private static class InputStreamImpl extends InputStream {

        private long offset;
        private final GzipRandomAccess access;

        public InputStreamImpl(long offset, GzipRandomAccess access) {
            this.offset = offset;
            this.access = access;
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            int read = access.read(offset, b, off, len);

            if (read > 0) {
                this.offset += read;
            }

            return read;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int read = read(b, 0, 1);

            if (read != 1) {
                return -1;
            }

            return b[0] & 0xff;
        }
    }

    // Implements a ReadBuffer for this object.
    public static class ReadBufferImpl implements ReadBuffer {

        private final GzipRandomAccess access;
        private final byte[] tmp = new byte[8];

        public ReadBufferImpl(GzipRandomAccess access) {
            this.access = access;
        }

        private void readFully(long pos, int len) throws IOException {
            int left = len;
            int off = 0;

            while (left > 0) {
                int read = access.read(pos, tmp, off, left);

                if (read <= 0) {
                    throw new EOFException("Could not read at " + pos);
                }

                left -= read;
                off += read;
                pos += read;
            }
        }

        private int readInt(int offset) {
            return (((tmp[offset + 0] & 0xff) << 24) | ((tmp[offset + 1] & 0xff) << 16) |
                     ((tmp[offset + 2] & 0xff) << 8) | (tmp[offset + 3] & 0xff));
        }

        @Override
        public void close() throws Exception {
            access.close();
        }

        @Override
        public char getChar(long pos) throws IOException {
            readFully(pos, 2);
            return (char) (((tmp[0] & 0xff) << 8) | (tmp[1] & 0xff));
        }

        @Override
        public byte getByte(long pos) throws IOException {
            readFully(pos, 1);
            return tmp[0];
        }

        @Override
        public short getShort(long pos) throws IOException {
            return (short) getChar(pos);
        }

        @Override
        public int getInt(long pos) throws IOException {
            readFully(pos, 4);
            return readInt(0);
        }

        @Override
        public long getLong(long pos) throws IOException {
            readFully(pos, 8);
            long i1 = readInt(0);
            int i2 = readInt(4);

            return (i1 << 32) | (i2 & 0xffffffffl);
        }
    }
}
