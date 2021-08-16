/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import jdk.internal.access.foreign.MemorySegmentProxy;
import jdk.internal.misc.TerminatingThreadLocal;
import jdk.internal.misc.Unsafe;
import sun.security.action.GetPropertyAction;

public class Util {

    // -- Caches --

    // The number of temp buffers in our pool
    private static final int TEMP_BUF_POOL_SIZE = IOUtil.IOV_MAX;

    // The max size allowed for a cached temp buffer, in bytes
    private static final long MAX_CACHED_BUFFER_SIZE = getMaxCachedBufferSize();

    // Per-thread cache of temporary direct buffers
    private static ThreadLocal<BufferCache> bufferCache = new TerminatingThreadLocal<>() {
        @Override
        protected BufferCache initialValue() {
            return new BufferCache();
        }
        @Override
        protected void threadTerminated(BufferCache cache) { // will never be null
            while (!cache.isEmpty()) {
                ByteBuffer bb = cache.removeFirst();
                free(bb);
            }
        }
    };

    /**
     * Returns the max size allowed for a cached temp buffers, in
     * bytes. It defaults to Long.MAX_VALUE. It can be set with the
     * jdk.nio.maxCachedBufferSize property. Even though
     * ByteBuffer.capacity() returns an int, we're using a long here
     * for potential future-proofing.
     */
    private static long getMaxCachedBufferSize() {
        String s = GetPropertyAction
                .privilegedGetProperty("jdk.nio.maxCachedBufferSize");
        if (s != null) {
            try {
                long m = Long.parseLong(s);
                if (m >= 0) {
                    return m;
                } else {
                    // if it's negative, ignore the system property
                }
            } catch (NumberFormatException e) {
                // if the string is not well formed, ignore the system property
            }
        }
        return Long.MAX_VALUE;
    }

    /**
     * Returns true if a buffer of this size is too large to be
     * added to the buffer cache, false otherwise.
     */
    private static boolean isBufferTooLarge(int size) {
        return size > MAX_CACHED_BUFFER_SIZE;
    }

    /**
     * Returns true if the buffer is too large to be added to the
     * buffer cache, false otherwise.
     */
    private static boolean isBufferTooLarge(ByteBuffer buf) {
        return isBufferTooLarge(buf.capacity());
    }

    /**
     * A simple cache of direct buffers.
     */
    private static class BufferCache {
        // the array of buffers
        private ByteBuffer[] buffers;

        // the number of buffers in the cache
        private int count;

        // the index of the first valid buffer (undefined if count == 0)
        private int start;

        private int next(int i) {
            return (i + 1) % TEMP_BUF_POOL_SIZE;
        }

        BufferCache() {
            buffers = new ByteBuffer[TEMP_BUF_POOL_SIZE];
        }

        /**
         * Removes and returns a buffer from the cache of at least the given
         * size (or null if no suitable buffer is found).
         */
        ByteBuffer get(int size) {
            // Don't call this if the buffer would be too large.
            assert !isBufferTooLarge(size);

            if (count == 0)
                return null;  // cache is empty

            ByteBuffer[] buffers = this.buffers;

            // search for suitable buffer (often the first buffer will do)
            ByteBuffer buf = buffers[start];
            if (buf.capacity() < size) {
                buf = null;
                int i = start;
                while ((i = next(i)) != start) {
                    ByteBuffer bb = buffers[i];
                    if (bb == null)
                        break;
                    if (bb.capacity() >= size) {
                        buf = bb;
                        break;
                    }
                }
                if (buf == null)
                    return null;
                // move first element to here to avoid re-packing
                buffers[i] = buffers[start];
            }

            // remove first element
            buffers[start] = null;
            start = next(start);
            count--;

            // prepare the buffer and return it
            buf.rewind();
            buf.limit(size);
            return buf;
        }

        boolean offerFirst(ByteBuffer buf) {
            // Don't call this if the buffer is too large.
            assert !isBufferTooLarge(buf);

            if (count >= TEMP_BUF_POOL_SIZE) {
                return false;
            } else {
                start = (start + TEMP_BUF_POOL_SIZE - 1) % TEMP_BUF_POOL_SIZE;
                buffers[start] = buf;
                count++;
                return true;
            }
        }

        boolean offerLast(ByteBuffer buf) {
            // Don't call this if the buffer is too large.
            assert !isBufferTooLarge(buf);

            if (count >= TEMP_BUF_POOL_SIZE) {
                return false;
            } else {
                int next = (start + count) % TEMP_BUF_POOL_SIZE;
                buffers[next] = buf;
                count++;
                return true;
            }
        }

        boolean isEmpty() {
            return count == 0;
        }

        ByteBuffer removeFirst() {
            assert count > 0;
            ByteBuffer buf = buffers[start];
            buffers[start] = null;
            start = next(start);
            count--;
            return buf;
        }
    }

    /**
     * Returns a temporary buffer of at least the given size
     */
    public static ByteBuffer getTemporaryDirectBuffer(int size) {
        // If a buffer of this size is too large for the cache, there
        // should not be a buffer in the cache that is at least as
        // large. So we'll just create a new one. Also, we don't have
        // to remove the buffer from the cache (as this method does
        // below) given that we won't put the new buffer in the cache.
        if (isBufferTooLarge(size)) {
            return ByteBuffer.allocateDirect(size);
        }

        BufferCache cache = bufferCache.get();
        ByteBuffer buf = cache.get(size);
        if (buf != null) {
            return buf;
        } else {
            // No suitable buffer in the cache so we need to allocate a new
            // one. To avoid the cache growing then we remove the first
            // buffer from the cache and free it.
            if (!cache.isEmpty()) {
                buf = cache.removeFirst();
                free(buf);
            }
            return ByteBuffer.allocateDirect(size);
        }
    }

    /**
     * Returns a temporary buffer of at least the given size and
     * aligned to the alignment
     */
    public static ByteBuffer getTemporaryAlignedDirectBuffer(int size,
                                                             int alignment) {
        if (isBufferTooLarge(size)) {
            return ByteBuffer.allocateDirect(size + alignment - 1)
                    .alignedSlice(alignment);
        }

        BufferCache cache = bufferCache.get();
        ByteBuffer buf = cache.get(size);
        if (buf != null) {
            if (buf.alignmentOffset(0, alignment) == 0) {
                return buf;
            }
        } else {
            if (!cache.isEmpty()) {
                buf = cache.removeFirst();
                free(buf);
            }
        }
        return ByteBuffer.allocateDirect(size + alignment - 1)
                .alignedSlice(alignment);
    }

    /**
     * Releases a temporary buffer by returning to the cache or freeing it.
     */
    public static void releaseTemporaryDirectBuffer(ByteBuffer buf) {
        offerFirstTemporaryDirectBuffer(buf);
    }

    /**
     * Releases a temporary buffer by returning to the cache or freeing it. If
     * returning to the cache then insert it at the start so that it is
     * likely to be returned by a subsequent call to getTemporaryDirectBuffer.
     */
    static void offerFirstTemporaryDirectBuffer(ByteBuffer buf) {
        // If the buffer is too large for the cache we don't have to
        // check the cache. We'll just free it.
        if (isBufferTooLarge(buf)) {
            free(buf);
            return;
        }

        assert buf != null;
        BufferCache cache = bufferCache.get();
        if (!cache.offerFirst(buf)) {
            // cache is full
            free(buf);
        }
    }

    /**
     * Releases a temporary buffer by returning to the cache or freeing it. If
     * returning to the cache then insert it at the end. This makes it
     * suitable for scatter/gather operations where the buffers are returned to
     * cache in same order that they were obtained.
     */
    static void offerLastTemporaryDirectBuffer(ByteBuffer buf) {
        // If the buffer is too large for the cache we don't have to
        // check the cache. We'll just free it.
        if (isBufferTooLarge(buf)) {
            free(buf);
            return;
        }

        assert buf != null;
        BufferCache cache = bufferCache.get();
        if (!cache.offerLast(buf)) {
            // cache is full
            free(buf);
        }
    }

    /**
     * Frees the memory for the given direct buffer
     */
    private static void free(ByteBuffer buf) {
        ((DirectBuffer)buf).cleaner().clean();
    }


    // -- Random stuff --

    static ByteBuffer[] subsequence(ByteBuffer[] bs, int offset, int length) {
        if ((offset == 0) && (length == bs.length))
            return bs;
        int n = length;
        ByteBuffer[] bs2 = new ByteBuffer[n];
        for (int i = 0; i < n; i++)
            bs2[i] = bs[offset + i];
        return bs2;
    }

    static <E> Set<E> ungrowableSet(final Set<E> s) {
        return new Set<E>() {

                public int size()                 { return s.size(); }
                public boolean isEmpty()          { return s.isEmpty(); }
                public boolean contains(Object o) { return s.contains(o); }
                public Object[] toArray()         { return s.toArray(); }
                public <T> T[] toArray(T[] a)     { return s.toArray(a); }
                public String toString()          { return s.toString(); }
                public Iterator<E> iterator()     { return s.iterator(); }
                public boolean equals(Object o)   { return s.equals(o); }
                public int hashCode()             { return s.hashCode(); }
                public void clear()               { s.clear(); }
                public boolean remove(Object o)   { return s.remove(o); }

                public boolean containsAll(Collection<?> coll) {
                    return s.containsAll(coll);
                }
                public boolean removeAll(Collection<?> coll) {
                    return s.removeAll(coll);
                }
                public boolean retainAll(Collection<?> coll) {
                    return s.retainAll(coll);
                }

                public boolean add(E o){
                    throw new UnsupportedOperationException();
                }
                public boolean addAll(Collection<? extends E> coll) {
                    throw new UnsupportedOperationException();
                }

        };
    }


    // -- Unsafe access --

    private static Unsafe unsafe = Unsafe.getUnsafe();

    private static byte _get(long a) {
        return unsafe.getByte(a);
    }

    private static void _put(long a, byte b) {
        unsafe.putByte(a, b);
    }

    static void erase(ByteBuffer bb) {
        unsafe.setMemory(((DirectBuffer)bb).address(), bb.capacity(), (byte)0);
    }

    static Unsafe unsafe() {
        return unsafe;
    }

    private static int pageSize = -1;

    static int pageSize() {
        if (pageSize == -1)
            pageSize = unsafe().pageSize();
        return pageSize;
    }

    private static volatile Constructor<?> directByteBufferConstructor;

    @SuppressWarnings("removal")
    private static void initDBBConstructor() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    try {
                        Class<?> cl = Class.forName("java.nio.DirectByteBuffer");
                        Constructor<?> ctor = cl.getDeclaredConstructor(
                            new Class<?>[] { int.class,
                                             long.class,
                                             FileDescriptor.class,
                                             Runnable.class,
                                             boolean.class, MemorySegmentProxy.class});
                        ctor.setAccessible(true);
                        directByteBufferConstructor = ctor;
                    } catch (ClassNotFoundException   |
                             NoSuchMethodException    |
                             IllegalArgumentException |
                             ClassCastException x) {
                        throw new InternalError(x);
                    }
                    return null;
                }});
    }

    static MappedByteBuffer newMappedByteBuffer(int size, long addr,
                                                FileDescriptor fd,
                                                Runnable unmapper,
                                                boolean isSync)
    {
        MappedByteBuffer dbb;
        if (directByteBufferConstructor == null)
            initDBBConstructor();
        try {
            dbb = (MappedByteBuffer)directByteBufferConstructor.newInstance(
              new Object[] { size,
                             addr,
                             fd,
                             unmapper,
                             isSync, null});
        } catch (InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException e) {
            throw new InternalError(e);
        }
        return dbb;
    }

    private static volatile Constructor<?> directByteBufferRConstructor;

    @SuppressWarnings("removal")
    private static void initDBBRConstructor() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    try {
                        Class<?> cl = Class.forName("java.nio.DirectByteBufferR");
                        Constructor<?> ctor = cl.getDeclaredConstructor(
                            new Class<?>[] { int.class,
                                             long.class,
                                             FileDescriptor.class,
                                             Runnable.class,
                                             boolean.class, MemorySegmentProxy.class });
                        ctor.setAccessible(true);
                        directByteBufferRConstructor = ctor;
                    } catch (ClassNotFoundException |
                             NoSuchMethodException |
                             IllegalArgumentException |
                             ClassCastException x) {
                        throw new InternalError(x);
                    }
                    return null;
                }});
    }

    static MappedByteBuffer newMappedByteBufferR(int size, long addr,
                                                 FileDescriptor fd,
                                                 Runnable unmapper,
                                                 boolean isSync)
    {
        MappedByteBuffer dbb;
        if (directByteBufferRConstructor == null)
            initDBBRConstructor();
        try {
            dbb = (MappedByteBuffer)directByteBufferRConstructor.newInstance(
              new Object[] { size,
                             addr,
                             fd,
                             unmapper,
                             isSync, null});
        } catch (InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException e) {
            throw new InternalError(e);
        }
        return dbb;
    }

    static void checkBufferPositionAligned(ByteBuffer bb,
                                                     int pos, int alignment)
        throws IOException
    {
        if (bb.alignmentOffset(pos, alignment) != 0) {
            throw new IOException("Current location of the bytebuffer ("
                + pos + ") is not a multiple of the block size ("
                + alignment + ")");
        }
    }

    static void checkRemainingBufferSizeAligned(int rem,
                                                          int alignment)
        throws IOException
    {
        if (rem % alignment != 0) {
            throw new IOException("Number of remaining bytes ("
                + rem + ") is not a multiple of the block size ("
                + alignment + ")");
        }
    }

    static void checkChannelPositionAligned(long position,
                                                      int alignment)
        throws IOException
    {
        if (position % alignment != 0) {
           throw new IOException("Channel position (" + position
               + ") is not a multiple of the block size ("
               + alignment + ")");
        }
    }
}
