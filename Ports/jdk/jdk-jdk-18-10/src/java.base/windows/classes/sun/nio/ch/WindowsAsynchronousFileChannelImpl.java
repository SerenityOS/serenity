/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.channels.*;
import java.util.concurrent.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.io.IOException;
import java.io.FileDescriptor;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaIOFileDescriptorAccess;

/**
 * Windows implementation of AsynchronousFileChannel using overlapped I/O.
 */

public class WindowsAsynchronousFileChannelImpl
    extends AsynchronousFileChannelImpl
    implements Iocp.OverlappedChannel, Groupable
{
    private static final JavaIOFileDescriptorAccess fdAccess =
        SharedSecrets.getJavaIOFileDescriptorAccess();

    // error when EOF is detected asynchronously.
    private static final int ERROR_HANDLE_EOF = 38;

    // Lazy initialization of default I/O completion port
    private static class DefaultIocpHolder {
        static final Iocp defaultIocp = defaultIocp();
        private static Iocp defaultIocp() {
            try {
                return new Iocp(null, ThreadPool.createDefault()).start();
            } catch (IOException ioe) {
                throw new InternalError(ioe);
            }
        }
    }

    // Used for force/truncate/size methods
    private static final FileDispatcher nd = new FileDispatcherImpl();

    // The handle is extracted for use in native methods invoked from this class.
    private final long handle;

    // The key that identifies the channel's association with the I/O port
    private final int completionKey;

    // I/O completion port (group)
    private final Iocp iocp;

    private final boolean isDefaultIocp;

    // Caches OVERLAPPED structure for each outstanding I/O operation
    private final PendingIoCache ioCache;


    private WindowsAsynchronousFileChannelImpl(FileDescriptor fdObj,
                                               boolean reading,
                                               boolean writing,
                                               Iocp iocp,
                                               boolean isDefaultIocp)
        throws IOException
    {
        super(fdObj, reading, writing, iocp.executor());
        this.handle = fdAccess.getHandle(fdObj);
        this.iocp = iocp;
        this.isDefaultIocp = isDefaultIocp;
        this.ioCache = new PendingIoCache();
        this.completionKey = iocp.associate(this, handle);
    }

    public static AsynchronousFileChannel open(FileDescriptor fdo,
                                               boolean reading,
                                               boolean writing,
                                               ThreadPool pool)
        throws IOException
    {
        Iocp iocp;
        boolean isDefaultIocp;
        if (pool == null) {
            iocp = DefaultIocpHolder.defaultIocp;
            isDefaultIocp = true;
        } else {
            iocp = new Iocp(null, pool).start();
            isDefaultIocp = false;
        }
        try {
            return new
                WindowsAsynchronousFileChannelImpl(fdo, reading, writing, iocp, isDefaultIocp);
        } catch (IOException x) {
            // error binding to port so need to close it (if created for this channel)
            if (!isDefaultIocp)
                iocp.implClose();
            throw x;
        }
    }

    @Override
    public <V,A> PendingFuture<V,A> getByOverlapped(long overlapped) {
        return ioCache.remove(overlapped);
    }

    @Override
    public void close() throws IOException {
        closeLock.writeLock().lock();
        try {
            if (closed)
                return;     // already closed
            closed = true;
        } finally {
            closeLock.writeLock().unlock();
        }

        // invalidate all locks held for this channel
        invalidateAllLocks();

        // close the file
        nd.close(fdObj);

        // waits until all I/O operations have completed
        ioCache.close();

        // disassociate from port
        iocp.disassociate(completionKey);

        // for the non-default group close the port
        if (!isDefaultIocp)
            iocp.detachFromThreadPool();
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }

    /**
     * Translates Throwable to IOException
     */
    private static IOException toIOException(Throwable x) {
        if (x instanceof IOException) {
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            return (IOException)x;
        }
        return new IOException(x);
    }

    @Override
    public long size() throws IOException {
        try {
            begin();
            return nd.size(fdObj);
        } finally {
            end();
        }
    }

    @Override
    public AsynchronousFileChannel truncate(long size) throws IOException {
        if (size < 0)
            throw new IllegalArgumentException("Negative size");
        if (!writing)
            throw new NonWritableChannelException();
        try {
            begin();
            if (size > nd.size(fdObj))
                return this;
            nd.truncate(fdObj, size);
        } finally {
            end();
        }
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
        try {
            begin();
            nd.force(fdObj, metaData);
        } finally {
            end();
        }
    }

    // -- file locking --

    /**
     * Task that initiates locking operation and handles completion result.
     */
    private class LockTask<A> implements Runnable, Iocp.ResultHandler {
        private final long position;
        private final FileLockImpl fli;
        private final PendingFuture<FileLock,A> result;

        LockTask(long position,
                 FileLockImpl fli,
                 PendingFuture<FileLock,A> result)
        {
            this.position = position;
            this.fli = fli;
            this.result = result;
        }

        @Override
        public void run() {
            long overlapped = 0L;
            try {
                begin();

                // allocate OVERLAPPED structure
                overlapped = ioCache.add(result);

                // synchronize on result to avoid race with handler thread
                // when lock is acquired immediately.
                synchronized (result) {
                    int n = lockFile(handle, position, fli.size(), fli.isShared(),
                                     overlapped);
                    if (n == IOStatus.UNAVAILABLE) {
                        // I/O is pending
                        return;
                    }
                    // acquired lock immediately
                    result.setResult(fli);
                }

            } catch (Throwable x) {
                // lock failed or channel closed
                removeFromFileLockTable(fli);
                result.setFailure(toIOException(x));
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
            } finally {
                end();
            }

            // invoke completion handler
            Invoker.invoke(result);
        }

        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            // release waiters and invoke completion handler
            result.setResult(fli);
            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        @Override
        public void failed(int error, IOException x) {
            // lock not acquired so remove from lock table
            removeFromFileLockTable(fli);

            // release waiters
            if (isOpen()) {
                result.setFailure(x);
            } else {
                result.setFailure(new AsynchronousCloseException());
            }
            Invoker.invoke(result);
        }
    }

    @Override
    <A> Future<FileLock> implLock(final long position,
                                  final long size,
                                  final boolean shared,
                                  A attachment,
                                  final CompletionHandler<FileLock,? super A> handler)
    {
        if (shared && !reading)
            throw new NonReadableChannelException();
        if (!shared && !writing)
            throw new NonWritableChannelException();

        // add to lock table
        FileLockImpl fli = addToFileLockTable(position, size, shared);
        if (fli == null) {
            Throwable exc = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(exc);
            Invoker.invoke(this, handler, attachment, null, exc);
            return null;
        }

        // create Future and task that will be invoked to acquire lock
        PendingFuture<FileLock,A> result =
            new PendingFuture<FileLock,A>(this, handler, attachment);
        LockTask<A> lockTask = new LockTask<A>(position, fli, result);
        result.setContext(lockTask);

        // initiate I/O
        lockTask.run();
        return result;
    }

    static final int NO_LOCK = -1;       // Failed to lock
    static final int LOCKED = 0;         // Obtained requested lock

    @Override
    public FileLock tryLock(long position, long size, boolean shared)
        throws IOException
    {
        if (shared && !reading)
            throw new NonReadableChannelException();
        if (!shared && !writing)
            throw new NonWritableChannelException();

        // add to lock table
        final FileLockImpl fli = addToFileLockTable(position, size, shared);
        if (fli == null)
            throw new ClosedChannelException();

        boolean gotLock = false;
        try {
            begin();
            // try to acquire the lock
            int res = nd.lock(fdObj, false, position, size, shared);
            if (res == NO_LOCK)
                return null;
            gotLock = true;
            return fli;
        } finally {
            if (!gotLock)
                removeFromFileLockTable(fli);
            end();
        }
    }

    @Override
    protected void implRelease(FileLockImpl fli) throws IOException {
        nd.release(fdObj, fli.position(), fli.size());
    }

    /**
     * Task that initiates read operation and handles completion result.
     */
    private class ReadTask<A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer dst;
        private final int pos, rem;     // buffer position/remaining
        private final long position;    // file position
        private final PendingFuture<Integer,A> result;

        // set to dst if direct; otherwise set to substituted direct buffer
        private volatile ByteBuffer buf;

        ReadTask(ByteBuffer dst,
                 int pos,
                 int rem,
                 long position,
                 PendingFuture<Integer,A> result)
        {
            this.dst = dst;
            this.pos = pos;
            this.rem = rem;
            this.position = position;
            this.result = result;
        }

        void releaseBufferIfSubstituted() {
            if (buf != dst)
                Util.releaseTemporaryDirectBuffer(buf);
        }

        void updatePosition(int bytesTransferred) {
            // if the I/O succeeded then adjust buffer position
            if (bytesTransferred > 0) {
                if (buf == dst) {
                    try {
                        dst.position(pos + bytesTransferred);
                    } catch (IllegalArgumentException x) {
                        // someone has changed the position; ignore
                    }
                } else {
                    // had to substitute direct buffer
                    buf.position(bytesTransferred).flip();
                    try {
                        dst.put(buf);
                    } catch (BufferOverflowException x) {
                        // someone has changed the position; ignore
                    }
                }
            }
        }

        @Override
        public void run() {
            int n = -1;
            long overlapped = 0L;
            long address;

            // Substitute a native buffer if not direct
            if (dst instanceof DirectBuffer) {
                buf = dst;
                address = ((DirectBuffer)dst).address() + pos;
            } else {
                buf = Util.getTemporaryDirectBuffer(rem);
                address = ((DirectBuffer)buf).address();
            }

            boolean pending = false;
            try {
                begin();

                // allocate OVERLAPPED
                overlapped = ioCache.add(result);

                // initiate read
                n = readFile(handle, address, rem, position, overlapped);
                if (n == IOStatus.UNAVAILABLE) {
                    // I/O is pending
                    pending = true;
                    return;
                } else if (n == IOStatus.EOF) {
                    result.setResult(n);
                } else {
                    throw new InternalError("Unexpected result: " + n);
                }

            } catch (Throwable x) {
                // failed to initiate read
                result.setFailure(toIOException(x));
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
            } finally {
                if (!pending)
                    // release resources
                    releaseBufferIfSubstituted();
                end();
            }

            // invoke completion handler
            Invoker.invoke(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            updatePosition(bytesTransferred);

            // return direct buffer to cache if substituted
            releaseBufferIfSubstituted();

            // release waiters and invoke completion handler
            result.setResult(bytesTransferred);
            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        @Override
        public void failed(int error, IOException x) {
            // if EOF detected asynchronously then it is reported as error
            if (error == ERROR_HANDLE_EOF) {
                completed(-1, false);
            } else {
                // return direct buffer to cache if substituted
                releaseBufferIfSubstituted();

                // release waiters
                if (isOpen()) {
                    result.setFailure(x);
                } else {
                    result.setFailure(new AsynchronousCloseException());
                }
                Invoker.invoke(result);
            }
        }
    }

    @Override
    <A> Future<Integer> implRead(ByteBuffer dst,
                                 long position,
                                 A attachment,
                                 CompletionHandler<Integer,? super A> handler)
    {
        if (!reading)
            throw new NonReadableChannelException();
        if (position < 0)
            throw new IllegalArgumentException("Negative position");
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");

        // check if channel is closed
        if (!isOpen()) {
            Throwable exc = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(exc);
            Invoker.invoke(this, handler, attachment, null, exc);
            return null;
        }

        int pos = dst.position();
        int lim = dst.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        // no space remaining
        if (rem == 0) {
            if (handler == null)
                return CompletedFuture.withResult(0);
            Invoker.invoke(this, handler, attachment, 0, null);
            return null;
        }

        // create Future and task that initiates read
        PendingFuture<Integer,A> result =
            new PendingFuture<Integer,A>(this, handler, attachment);
        ReadTask<A> readTask = new ReadTask<A>(dst, pos, rem, position, result);
        result.setContext(readTask);

        // initiate I/O
        readTask.run();
        return result;
    }

    /**
     * Task that initiates write operation and handles completion result.
     */
    private class WriteTask<A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer src;
        private final int pos, rem;     // buffer position/remaining
        private final long position;    // file position
        private final PendingFuture<Integer,A> result;

        // set to src if direct; otherwise set to substituted direct buffer
        private volatile ByteBuffer buf;

        WriteTask(ByteBuffer src,
                  int pos,
                  int rem,
                  long position,
                  PendingFuture<Integer,A> result)
        {
            this.src = src;
            this.pos = pos;
            this.rem = rem;
            this.position = position;
            this.result = result;
        }

        void releaseBufferIfSubstituted() {
            if (buf != src)
                Util.releaseTemporaryDirectBuffer(buf);
        }

        void updatePosition(int bytesTransferred) {
            // if the I/O succeeded then adjust buffer position
            if (bytesTransferred > 0) {
                try {
                    src.position(pos + bytesTransferred);
                } catch (IllegalArgumentException x) {
                    // someone has changed the position
                }
            }
        }

        @Override
        public void run() {
            int n = -1;
            long overlapped = 0L;
            long address;

            // Substitute a native buffer if not direct
            if (src instanceof DirectBuffer) {
                buf = src;
                address = ((DirectBuffer)src).address() + pos;
            } else {
                buf = Util.getTemporaryDirectBuffer(rem);
                buf.put(src);
                buf.flip();
                // temporarily restore position as we don't know how many bytes
                // will be written
                src.position(pos);
                address = ((DirectBuffer)buf).address();
            }

            try {
                begin();

                // allocate an OVERLAPPED structure
                overlapped = ioCache.add(result);

                // initiate the write
                n = writeFile(handle, address, rem, position, overlapped);
                if (n == IOStatus.UNAVAILABLE) {
                    // I/O is pending
                    return;
                } else {
                    throw new InternalError("Unexpected result: " + n);
                }

            } catch (Throwable x) {
                // failed to initiate read:
                result.setFailure(toIOException(x));

                // release resources
                releaseBufferIfSubstituted();
                if (overlapped != 0L)
                    ioCache.remove(overlapped);

            } finally {
                end();
            }

            // invoke completion handler
            Invoker.invoke(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            updatePosition(bytesTransferred);

            // return direct buffer to cache if substituted
            releaseBufferIfSubstituted();

            // release waiters and invoke completion handler
            result.setResult(bytesTransferred);
            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        @Override
        public void failed(int error, IOException x) {
            // return direct buffer to cache if substituted
            releaseBufferIfSubstituted();

            // release waiters and invoker completion handler
            if (isOpen()) {
                result.setFailure(x);
            } else {
                result.setFailure(new AsynchronousCloseException());
            }
            Invoker.invoke(result);
        }
    }

    <A> Future<Integer> implWrite(ByteBuffer src,
                                  long position,
                                  A attachment,
                                  CompletionHandler<Integer,? super A> handler)
    {
        if (!writing)
            throw new NonWritableChannelException();
        if (position < 0)
            throw new IllegalArgumentException("Negative position");

        // check if channel is closed
        if (!isOpen()) {
           Throwable exc = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(exc);
            Invoker.invoke(this, handler, attachment, null, exc);
            return null;
        }

        int pos = src.position();
        int lim = src.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        // nothing to write
        if (rem == 0) {
            if (handler == null)
                return CompletedFuture.withResult(0);
            Invoker.invoke(this, handler, attachment, 0, null);
            return null;
        }

        // create Future and task to initiate write
        PendingFuture<Integer,A> result =
            new PendingFuture<Integer,A>(this, handler, attachment);
        WriteTask<A> writeTask = new WriteTask<A>(src, pos, rem, position, result);
        result.setContext(writeTask);

        // initiate I/O
        writeTask.run();
        return result;
    }

    // -- Native methods --

    private static native int readFile(long handle, long address, int len,
        long offset, long overlapped) throws IOException;

    private static native int writeFile(long handle, long address, int len,
        long offset, long overlapped) throws IOException;

    private static native int lockFile(long handle, long position, long size,
        boolean shared, long overlapped) throws IOException;

    static {
        IOUtil.load();
    }
}
