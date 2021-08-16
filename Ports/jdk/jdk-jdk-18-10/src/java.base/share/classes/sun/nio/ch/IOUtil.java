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
import java.nio.ByteBuffer;
import java.util.Objects;
import jdk.internal.access.JavaNioAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.ScopedMemoryAccess.Scope;

/**
 * File-descriptor based I/O utilities that are shared by NIO classes.
 */

public class IOUtil {

    /**
     * Max number of iovec structures that readv/writev supports
     */
    static final int IOV_MAX;

    private IOUtil() { }                // No instantiation

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     NativeDispatcher nd)
        throws IOException
    {
        return write(fd, src, position, false, false, -1, nd);
    }

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     boolean async, NativeDispatcher nd)
        throws IOException
    {
        return write(fd, src, position, false, async, -1, nd);
    }

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     boolean directIO, int alignment, NativeDispatcher nd)
        throws IOException
    {
        return write(fd, src, position, directIO, false, alignment, nd);
    }

    static int write(FileDescriptor fd, ByteBuffer src, long position,
                     boolean directIO, boolean async, int alignment,
                     NativeDispatcher nd)
        throws IOException
    {
        if (src instanceof DirectBuffer) {
            return writeFromNativeBuffer(fd, src, position, directIO, async, alignment, nd);
        }

        // Substitute a native buffer
        int pos = src.position();
        int lim = src.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        ByteBuffer bb;
        if (directIO) {
            Util.checkRemainingBufferSizeAligned(rem, alignment);
            bb = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
        } else {
            bb = Util.getTemporaryDirectBuffer(rem);
        }
        try {
            bb.put(src);
            bb.flip();
            // Do not update src until we see how many bytes were written
            src.position(pos);

            int n = writeFromNativeBuffer(fd, bb, position, directIO, async, alignment, nd);
            if (n > 0) {
                // now update src
                src.position(pos + n);
            }
            return n;
        } finally {
            Util.offerFirstTemporaryDirectBuffer(bb);
        }
    }

    private static int writeFromNativeBuffer(FileDescriptor fd, ByteBuffer bb,
                                             long position, boolean directIO,
                                             boolean async, int alignment,
                                             NativeDispatcher nd)
        throws IOException
    {
        int pos = bb.position();
        int lim = bb.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (directIO) {
            Util.checkBufferPositionAligned(bb, pos, alignment);
            Util.checkRemainingBufferSizeAligned(rem, alignment);
        }

        int written = 0;
        if (rem == 0)
            return 0;
        var handle = acquireScope(bb, async);
        try {
            if (position != -1) {
                written = nd.pwrite(fd, bufferAddress(bb) + pos, rem, position);
            } else {
                written = nd.write(fd, bufferAddress(bb) + pos, rem);
            }
        } finally {
            releaseScope(handle);
        }
        if (written > 0)
            bb.position(pos + written);
        return written;
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, boolean async,
                      NativeDispatcher nd)
        throws IOException
    {
        return write(fd, bufs, 0, bufs.length, false, async, -1, nd);
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                      NativeDispatcher nd)
        throws IOException
    {
        return write(fd, bufs, offset, length, false, false, -1, nd);
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                      boolean direct, int alignment, NativeDispatcher nd)
        throws IOException
    {
        return write(fd, bufs, offset, length, direct, false, alignment, nd);
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                      boolean directIO, boolean async,
                      int alignment, NativeDispatcher nd)
        throws IOException
    {
        IOVecWrapper vec = IOVecWrapper.get(length);

        boolean completed = false;
        int iov_len = 0;
        Runnable handleReleasers = null;
        try {
            // Iterate over buffers to populate native iovec array.
            int count = offset + length;
            int i = offset;
            while (i < count && iov_len < IOV_MAX) {
                ByteBuffer buf = bufs[i];
                var h = acquireScope(buf, async);
                if (h != null) {
                    handleReleasers = LinkedRunnable.of(Releaser.of(h), handleReleasers);
                }
                int pos = buf.position();
                int lim = buf.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                if (directIO)
                    Util.checkRemainingBufferSizeAligned(rem, alignment);

                if (rem > 0) {
                    vec.setBuffer(iov_len, buf, pos, rem);

                    // allocate shadow buffer to ensure I/O is done with direct buffer
                    if (!(buf instanceof DirectBuffer)) {
                        ByteBuffer shadow;
                        if (directIO)
                            shadow = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
                        else
                            shadow = Util.getTemporaryDirectBuffer(rem);
                        shadow.put(buf);
                        shadow.flip();
                        vec.setShadow(iov_len, shadow);
                        buf.position(pos);  // temporarily restore position in user buffer
                        buf = shadow;
                        pos = shadow.position();
                    }

                    vec.putBase(iov_len, bufferAddress(buf) + pos);
                    vec.putLen(iov_len, rem);
                    iov_len++;
                }
                i++;
            }
            if (iov_len == 0)
                return 0L;

            long bytesWritten = nd.writev(fd, vec.address, iov_len);

            // Notify the buffers how many bytes were taken
            long left = bytesWritten;
            for (int j=0; j<iov_len; j++) {
                if (left > 0) {
                    ByteBuffer buf = vec.getBuffer(j);
                    int pos = vec.getPosition(j);
                    int rem = vec.getRemaining(j);
                    int n = (left > rem) ? rem : (int)left;
                    buf.position(pos + n);
                    left -= n;
                }
                // return shadow buffers to buffer pool
                ByteBuffer shadow = vec.getShadow(j);
                if (shadow != null)
                    Util.offerLastTemporaryDirectBuffer(shadow);
                vec.clearRefs(j);
            }

            completed = true;
            return bytesWritten;

        } finally {
            releaseScopes(handleReleasers);
            // if an error occurred then clear refs to buffers and return any shadow
            // buffers to cache
            if (!completed) {
                for (int j=0; j<iov_len; j++) {
                    ByteBuffer shadow = vec.getShadow(j);
                    if (shadow != null)
                        Util.offerLastTemporaryDirectBuffer(shadow);
                    vec.clearRefs(j);
                }
            }
        }
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    NativeDispatcher nd)
        throws IOException
    {
        return read(fd, dst, position, false, false, -1, nd);
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    boolean async, NativeDispatcher nd)
        throws IOException
    {
        return read(fd, dst, position, false, async, -1, nd);
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    boolean directIO, int alignment, NativeDispatcher nd)
        throws IOException
    {
        return read(fd, dst, position, directIO, false, alignment, nd);
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position,
                    boolean directIO, boolean async,
                    int alignment, NativeDispatcher nd)
        throws IOException
    {
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        if (dst instanceof DirectBuffer)
            return readIntoNativeBuffer(fd, dst, position, directIO, async, alignment, nd);

        // Substitute a native buffer
        ByteBuffer bb;
        int rem = dst.remaining();
        if (directIO) {
            Util.checkRemainingBufferSizeAligned(rem, alignment);
            bb = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
        } else {
            bb = Util.getTemporaryDirectBuffer(rem);
        }
        try {
            int n = readIntoNativeBuffer(fd, bb, position, directIO, async, alignment, nd);
            bb.flip();
            if (n > 0)
                dst.put(bb);
            return n;
        } finally {
            Util.offerFirstTemporaryDirectBuffer(bb);
        }
    }

    private static int readIntoNativeBuffer(FileDescriptor fd, ByteBuffer bb,
                                            long position, boolean directIO,
                                            boolean async, int alignment,
                                            NativeDispatcher nd)
        throws IOException
    {
        int pos = bb.position();
        int lim = bb.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (directIO) {
            Util.checkBufferPositionAligned(bb, pos, alignment);
            Util.checkRemainingBufferSizeAligned(rem, alignment);
        }

        if (rem == 0)
            return 0;
        int n = 0;
        var handle = acquireScope(bb, async);
        try {
            if (position != -1) {
                n = nd.pread(fd, bufferAddress(bb) + pos, rem, position);
            } else {
                n = nd.read(fd, bufferAddress(bb) + pos, rem);
            }
        } finally {
            releaseScope(handle);
        }
        if (n > 0)
            bb.position(pos + n);
        return n;
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, NativeDispatcher nd)
        throws IOException
    {
        return read(fd, bufs, 0, bufs.length, false, false, -1, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, boolean async,
                     NativeDispatcher nd)
        throws IOException
    {
        return read(fd, bufs, 0, bufs.length, false, async, -1, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                     NativeDispatcher nd)
        throws IOException
    {
        return read(fd, bufs, offset, length, false, false, -1, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                     boolean directIO, int alignment, NativeDispatcher nd)

        throws IOException
    {
        return read(fd, bufs, offset, length, directIO, false, alignment, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length,
                     boolean directIO, boolean async,
                     int alignment, NativeDispatcher nd)

        throws IOException
    {
        IOVecWrapper vec = IOVecWrapper.get(length);

        boolean completed = false;
        int iov_len = 0;
        Runnable handleReleasers = null;
        try {
            // Iterate over buffers to populate native iovec array.
            int count = offset + length;
            int i = offset;
            while (i < count && iov_len < IOV_MAX) {
                ByteBuffer buf = bufs[i];
                if (buf.isReadOnly())
                    throw new IllegalArgumentException("Read-only buffer");
                var h = acquireScope(buf, async);
                if (h != null) {
                    handleReleasers = LinkedRunnable.of(Releaser.of(h), handleReleasers);
                }
                int pos = buf.position();
                int lim = buf.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);

                if (directIO)
                    Util.checkRemainingBufferSizeAligned(rem, alignment);

                if (rem > 0) {
                    vec.setBuffer(iov_len, buf, pos, rem);

                    // allocate shadow buffer to ensure I/O is done with direct buffer
                    if (!(buf instanceof DirectBuffer)) {
                        ByteBuffer shadow;
                        if (directIO) {
                            shadow = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
                        } else {
                            shadow = Util.getTemporaryDirectBuffer(rem);
                        }
                        vec.setShadow(iov_len, shadow);
                        buf = shadow;
                        pos = shadow.position();
                    }

                    vec.putBase(iov_len, bufferAddress(buf) + pos);
                    vec.putLen(iov_len, rem);
                    iov_len++;
                }
                i++;
            }
            if (iov_len == 0)
                return 0L;

            long bytesRead = nd.readv(fd, vec.address, iov_len);

            // Notify the buffers how many bytes were read
            long left = bytesRead;
            for (int j=0; j<iov_len; j++) {
                ByteBuffer shadow = vec.getShadow(j);
                if (left > 0) {
                    ByteBuffer buf = vec.getBuffer(j);
                    int rem = vec.getRemaining(j);
                    int n = (left > rem) ? rem : (int)left;
                    if (shadow == null) {
                        int pos = vec.getPosition(j);
                        buf.position(pos + n);
                    } else {
                        shadow.limit(shadow.position() + n);
                        buf.put(shadow);
                    }
                    left -= n;
                }
                if (shadow != null)
                    Util.offerLastTemporaryDirectBuffer(shadow);
                vec.clearRefs(j);
            }

            completed = true;
            return bytesRead;

        } finally {
            releaseScopes(handleReleasers);
            // if an error occurred then clear refs to buffers and return any shadow
            // buffers to cache
            if (!completed) {
                for (int j=0; j<iov_len; j++) {
                    ByteBuffer shadow = vec.getShadow(j);
                    if (shadow != null)
                        Util.offerLastTemporaryDirectBuffer(shadow);
                    vec.clearRefs(j);
                }
            }
        }
    }

    private static final JavaNioAccess NIO_ACCESS = SharedSecrets.getJavaNioAccess();

    static Scope.Handle acquireScope(ByteBuffer bb, boolean async) {
        return NIO_ACCESS.acquireScope(bb, async);
    }

    private static void releaseScope(Scope.Handle handle) {
        if (handle == null)
            return;
        try {
            handle.scope().release(handle);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Runnable acquireScopes(ByteBuffer[] buffers) {
        return acquireScopes(null, buffers);
    }

    static Runnable acquireScopes(ByteBuffer buf, ByteBuffer[] buffers) {
        if (buffers == null) {
            assert buf != null;
            return IOUtil.Releaser.ofNullable(IOUtil.acquireScope(buf, true));
        } else {
            assert buf == null;
            Runnable handleReleasers = null;
            for (var b : buffers) {
                var h = IOUtil.acquireScope(b, true);
                if (h != null) {
                    handleReleasers = IOUtil.LinkedRunnable.of(IOUtil.Releaser.of(h), handleReleasers);
                }
            }
            return handleReleasers;
        }
    }

    static void releaseScopes(Runnable releasers) {
        if (releasers != null)
            releasers.run();
    }

    static record LinkedRunnable(Runnable node, Runnable next)
        implements Runnable
    {
        LinkedRunnable {
            Objects.requireNonNull(node);
        }
        @Override
        public void run() {
            try {
                node.run();
            } finally {
                if (next != null)
                    next.run();
            }
        }
        static LinkedRunnable of(Runnable first, Runnable second) {
            return new LinkedRunnable(first, second);
        }
    }

    static record Releaser(Scope.Handle handle) implements Runnable {
        Releaser { Objects.requireNonNull(handle) ; }
        @Override public void run() { releaseScope(handle); }
        static Runnable of(Scope.Handle handle) { return new Releaser(handle); }
        static Runnable ofNullable(Scope.Handle handle) {
            if (handle == null)
                return () -> { };
            return new Releaser(handle);
        }
    }

    static long bufferAddress(ByteBuffer buf) {
        return NIO_ACCESS.getBufferAddress(buf);
    }

    public static FileDescriptor newFD(int i) {
        FileDescriptor fd = new FileDescriptor();
        setfdVal(fd, i);
        return fd;
    }

    static native boolean randomBytes(byte[] someBytes);

    /**
     * Returns two file descriptors for a pipe encoded in a long.
     * The read end of the pipe is returned in the high 32 bits,
     * while the write end is returned in the low 32 bits.
     */
    static native long makePipe(boolean blocking) throws IOException;

    static native int write1(int fd, byte b) throws IOException;

    /**
     * Read and discard all bytes.
     */
    static native boolean drain(int fd) throws IOException;

    /**
     * Read and discard at most one byte
     * @return the number of bytes read or IOS_INTERRUPTED
     */
    static native int drain1(int fd) throws IOException;

    public static native void configureBlocking(FileDescriptor fd,
                                                boolean blocking)
        throws IOException;

    public static native int fdVal(FileDescriptor fd);

    static native void setfdVal(FileDescriptor fd, int value);

    static native int fdLimit();

    static native int iovMax();

    static native void initIDs();

    /**
     * Used to trigger loading of native libraries
     */
    public static void load() { }

    static {
        jdk.internal.loader.BootLoader.loadLibrary("net");
        jdk.internal.loader.BootLoader.loadLibrary("nio");
        initIDs();

        IOV_MAX = iovMax();
    }

}
