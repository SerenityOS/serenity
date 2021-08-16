/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.net.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import jdk.internal.misc.Unsafe;
import sun.net.util.SocketExceptions;

/**
 * Windows implementation of AsynchronousSocketChannel using overlapped I/O.
 */

class WindowsAsynchronousSocketChannelImpl
    extends AsynchronousSocketChannelImpl implements Iocp.OverlappedChannel
{
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static int addressSize = unsafe.addressSize();

    private static int dependsArch(int value32, int value64) {
        return (addressSize == 4) ? value32 : value64;
    }

    /*
     * typedef struct _WSABUF {
     *     u_long      len;
     *     char FAR *  buf;
     * } WSABUF;
     */
    private static final int SIZEOF_WSABUF  = dependsArch(8, 16);
    private static final int OFFSETOF_LEN   = 0;
    private static final int OFFSETOF_BUF   = dependsArch(4, 8);

    // maximum vector size for scatter/gather I/O
    private static final int MAX_WSABUF     = 16;

    private static final int SIZEOF_WSABUFARRAY = MAX_WSABUF * SIZEOF_WSABUF;


    // socket handle. Use begin()/end() around each usage of this handle.
    final long handle;

    // I/O completion port that the socket is associated with
    private final Iocp iocp;

    // completion key to identify channel when I/O completes
    private final int completionKey;

    // Pending I/O operations are tied to an OVERLAPPED structure that can only
    // be released when the I/O completion event is posted to the completion
    // port. Where I/O operations complete immediately then it is possible
    // there may be more than two OVERLAPPED structures in use.
    private final PendingIoCache ioCache;

    // per-channel arrays of WSABUF structures
    private final long readBufferArray;
    private final long writeBufferArray;


    WindowsAsynchronousSocketChannelImpl(Iocp iocp, boolean failIfGroupShutdown)
        throws IOException
    {
        super(iocp);

        // associate socket with default completion port
        long h = IOUtil.fdVal(fd);
        int key = 0;
        try {
            key = iocp.associate(this, h);
        } catch (ShutdownChannelGroupException x) {
            if (failIfGroupShutdown) {
                closesocket0(h);
                throw x;
            }
        } catch (IOException x) {
            closesocket0(h);
            throw x;
        }

        this.handle = h;
        this.iocp = iocp;
        this.completionKey = key;
        this.ioCache = new PendingIoCache();

        // allocate WSABUF arrays
        this.readBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
        this.writeBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
    }

    WindowsAsynchronousSocketChannelImpl(Iocp iocp) throws IOException {
        this(iocp, true);
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }

    /**
     * Invoked by Iocp when an I/O operation competes.
     */
    @Override
    public <V,A> PendingFuture<V,A> getByOverlapped(long overlapped) {
        return ioCache.remove(overlapped);
    }

    // invoked by WindowsAsynchronousServerSocketChannelImpl
    long handle() {
        return handle;
    }

    // invoked by WindowsAsynchronousServerSocketChannelImpl when new connection
    // accept
    void setConnected(InetSocketAddress localAddress,
                      InetSocketAddress remoteAddress)
    {
        synchronized (stateLock) {
            state = ST_CONNECTED;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }
    }

    @Override
    void implClose() throws IOException {
        // close socket (may cause outstanding async I/O operations to fail).
        closesocket0(handle);

        // waits until all I/O operations have completed
        ioCache.close();

        // release arrays of WSABUF structures
        unsafe.freeMemory(readBufferArray);
        unsafe.freeMemory(writeBufferArray);

        // finally disassociate from the completion port (key can be 0 if
        // channel created when group is shutdown)
        if (completionKey != 0)
            iocp.disassociate(completionKey);
    }

    @Override
    public void onCancel(PendingFuture<?,?> task) {
        if (task.getContext() instanceof ConnectTask)
            killConnect();
        if (task.getContext() instanceof ReadTask)
            killReading();
        if (task.getContext() instanceof WriteTask)
            killWriting();
    }

    /**
     * Implements the task to initiate a connection and the handler to
     * consume the result when the connection is established (or fails).
     */
    private class ConnectTask<A> implements Runnable, Iocp.ResultHandler {
        private final InetSocketAddress remote;
        private final PendingFuture<Void,A> result;

        ConnectTask(InetSocketAddress remote, PendingFuture<Void,A> result) {
            this.remote = remote;
            this.result = result;
        }

        private void closeChannel() {
            try {
                close();
            } catch (IOException ignore) { }
        }

        private IOException toIOException(Throwable x) {
            if (x instanceof IOException) {
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                return (IOException)x;
            }
            return new IOException(x);
        }

        /**
         * Invoke after a connection is successfully established.
         */
        private void afterConnect() throws IOException {
            updateConnectContext(handle);
            synchronized (stateLock) {
                state = ST_CONNECTED;
                remoteAddress = remote;
            }
        }

        /**
         * Task to initiate a connection.
         */
        @Override
        public void run() {
            long overlapped = 0L;
            Throwable exc = null;
            try {
                begin();

                // synchronize on result to allow this thread handle the case
                // where the connection is established immediately.
                synchronized (result) {
                    overlapped = ioCache.add(result);
                    // initiate the connection
                    int n = connect0(handle, Net.isIPv6Available(), remote.getAddress(),
                                     remote.getPort(), overlapped);
                    if (n == IOStatus.UNAVAILABLE) {
                        // connection is pending
                        return;
                    }

                    // connection established immediately
                    afterConnect();
                    result.setResult(null);
                }
            } catch (Throwable x) {
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
                exc = x;
            } finally {
                end();
            }

            if (exc != null) {
                closeChannel();
                exc = SocketExceptions.of(toIOException(exc), remote);
                result.setFailure(exc);
            }
            Invoker.invoke(result);
        }

        /**
         * Invoked by handler thread when connection established.
         */
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            Throwable exc = null;
            try {
                begin();
                afterConnect();
                result.setResult(null);
            } catch (Throwable x) {
                // channel is closed or unable to finish connect
                exc = x;
            } finally {
                end();
            }

            // can't close channel while in begin/end block
            if (exc != null) {
                closeChannel();
                IOException ee = toIOException(exc);
                ee = SocketExceptions.of(ee, remote);
                result.setFailure(ee);
            }

            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        /**
         * Invoked by handler thread when failed to establish connection.
         */
        @Override
        public void failed(int error, IOException x) {
            x = SocketExceptions.of(x, remote);
            if (isOpen()) {
                closeChannel();
                result.setFailure(x);
            } else {
                x = SocketExceptions.of(new AsynchronousCloseException(), remote);
                result.setFailure(x);
            }
            Invoker.invoke(result);
        }
    }

    @SuppressWarnings("removal")
    private void doPrivilegedBind(final SocketAddress sa) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    bind(sa);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    @Override
    <A> Future<Void> implConnect(SocketAddress remote,
                                 A attachment,
                                 CompletionHandler<Void,? super A> handler)
    {
        if (!isOpen()) {
            Throwable exc = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(exc);
            Invoker.invoke(this, handler, attachment, null, exc);
            return null;
        }

        InetSocketAddress isa = Net.checkAddress(remote);

        // permission check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());

        // check and update state
        // ConnectEx requires the socket to be bound to a local address
        IOException bindException = null;
        synchronized (stateLock) {
            if (state == ST_CONNECTED)
                throw new AlreadyConnectedException();
            if (state == ST_PENDING)
                throw new ConnectionPendingException();
            if (localAddress == null) {
                try {
                    SocketAddress any = new InetSocketAddress(0);
                    if (sm == null) {
                        bind(any);
                    } else {
                        doPrivilegedBind(any);
                    }
                } catch (IOException x) {
                    bindException = x;
                }
            }
            if (bindException == null)
                state = ST_PENDING;
        }

        // handle bind failure
        if (bindException != null) {
            try {
                close();
            } catch (IOException ignore) { }
            if (handler == null)
                return CompletedFuture.withFailure(bindException);
            Invoker.invoke(this, handler, attachment, null, bindException);
            return null;
        }

        // setup task
        PendingFuture<Void,A> result =
            new PendingFuture<Void,A>(this, handler, attachment);
        ConnectTask<A> task = new ConnectTask<A>(isa, result);
        result.setContext(task);

        // initiate I/O
        task.run();
        return result;
    }

    /**
     * Implements the task to initiate a read and the handler to consume the
     * result when the read completes.
     */
    private class ReadTask<V,A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer[] bufs;
        private final int numBufs;
        private final boolean scatteringRead;
        private final PendingFuture<V,A> result;

        // set by run method
        private ByteBuffer[] shadow;
        private Runnable scopeHandleReleasers;

        ReadTask(ByteBuffer[] bufs,
                 boolean scatteringRead,
                 PendingFuture<V,A> result)
        {
            this.bufs = bufs;
            this.numBufs = (bufs.length > MAX_WSABUF) ? MAX_WSABUF : bufs.length;
            this.scatteringRead = scatteringRead;
            this.result = result;
        }

        /**
         * Invoked prior to read to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        void prepareBuffers() {
            scopeHandleReleasers = IOUtil.acquireScopes(bufs);
            shadow = new ByteBuffer[numBufs];
            long address = readBufferArray;
            for (int i=0; i<numBufs; i++) {
                ByteBuffer dst = bufs[i];
                int pos = dst.position();
                int lim = dst.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                long a;
                if (!(dst instanceof DirectBuffer)) {
                    // substitute with direct buffer
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    shadow[i] = bb;
                    a = IOUtil.bufferAddress(bb);
                } else {
                    shadow[i] = dst;
                    a = IOUtil.bufferAddress(dst) + pos;
                }
                unsafe.putAddress(address + OFFSETOF_BUF, a);
                unsafe.putInt(address + OFFSETOF_LEN, rem);
                address += SIZEOF_WSABUF;
            }
        }

        /**
         * Invoked after a read has completed to update the buffer positions
         * and release any substituted buffers.
         */
        void updateBuffers(int bytesRead) {
            for (int i=0; i<numBufs; i++) {
                ByteBuffer nextBuffer = shadow[i];
                int pos = nextBuffer.position();
                int len = nextBuffer.remaining();
                if (bytesRead >= len) {
                    bytesRead -= len;
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch (IllegalArgumentException x) {
                        // position changed by another
                    }
                } else { // Buffers not completely filled
                    if (bytesRead > 0) {
                        assert(pos + bytesRead < (long)Integer.MAX_VALUE);
                        int newPosition = pos + bytesRead;
                        try {
                            nextBuffer.position(newPosition);
                        } catch (IllegalArgumentException x) {
                            // position changed by another
                        }
                    }
                    break;
                }
            }

            // Put results from shadow into the slow buffers
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    shadow[i].flip();
                    try {
                        bufs[i].put(shadow[i]);
                    } catch (BufferOverflowException x) {
                        // position changed by another
                    }
                }
            }
        }

        void releaseBuffers() {
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
            IOUtil.releaseScopes(scopeHandleReleasers);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            long overlapped = 0L;
            boolean prepared = false;
            boolean pending = false;

            try {
                begin();

                // substitute non-direct buffers
                prepareBuffers();
                prepared = true;

                // get an OVERLAPPED structure (from the cache or allocate)
                overlapped = ioCache.add(result);

                // initiate read
                int n = read0(handle, numBufs, readBufferArray, overlapped);
                if (n == IOStatus.UNAVAILABLE) {
                    // I/O is pending
                    pending = true;
                    return;
                }
                if (n == IOStatus.EOF) {
                    // input shutdown
                    enableReading();
                    if (scatteringRead) {
                        result.setResult((V)Long.valueOf(-1L));
                    } else {
                        result.setResult((V)Integer.valueOf(-1));
                    }
                } else {
                    throw new InternalError("Read completed immediately");
                }
            } catch (Throwable x) {
                // failed to initiate read
                // reset read flag before releasing waiters
                enableReading();
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException))
                    x = new IOException(x);
                result.setFailure(x);
            } finally {
                // release resources if I/O not pending
                if (!pending) {
                    if (overlapped != 0L)
                        ioCache.remove(overlapped);
                    if (prepared)
                        releaseBuffers();
                }
                end();
            }

            // invoke completion handler
            Invoker.invoke(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            if (bytesTransferred == 0) {
                bytesTransferred = -1;  // EOF
            } else {
                updateBuffers(bytesTransferred);
            }

            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            synchronized (result) {
                if (result.isDone())
                    return;
                enableReading();
                if (scatteringRead) {
                    result.setResult((V)Long.valueOf(bytesTransferred));
                } else {
                    result.setResult((V)Integer.valueOf(bytesTransferred));
                }
            }
            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        @Override
        public void failed(int error, IOException x) {
            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            if (!isOpen())
                x = new AsynchronousCloseException();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableReading();
                result.setFailure(x);
            }
            Invoker.invoke(result);
        }

        /**
         * Invoked if timeout expires before it is cancelled
         */
        void timeout() {
            // synchronize on result as the I/O could complete/fail
            synchronized (result) {
                if (result.isDone())
                    return;

                // kill further reading before releasing waiters
                enableReading(true);
                result.setFailure(new InterruptedByTimeoutException());
            }

            // invoke handler without any locks
            Invoker.invoke(result);
        }
    }

    @Override
    <V extends Number,A> Future<V> implRead(boolean isScatteringRead,
                                            ByteBuffer dst,
                                            ByteBuffer[] dsts,
                                            long timeout,
                                            TimeUnit unit,
                                            A attachment,
                                            CompletionHandler<V,? super A> handler)
    {
        // setup task
        PendingFuture<V,A> result =
            new PendingFuture<V,A>(this, handler, attachment);
        ByteBuffer[] bufs;
        if (isScatteringRead) {
            bufs = dsts;
        } else {
            bufs = new ByteBuffer[1];
            bufs[0] = dst;
        }
        final ReadTask<V,A> readTask =
                new ReadTask<V,A>(bufs, isScatteringRead, result);
        result.setContext(readTask);

        // schedule timeout
        if (timeout > 0L) {
            Future<?> timeoutTask = iocp.schedule(new Runnable() {
                public void run() {
                    readTask.timeout();
                }
            }, timeout, unit);
            result.setTimeoutTask(timeoutTask);
        }

        // initiate I/O
        readTask.run();
        return result;
    }

    /**
     * Implements the task to initiate a write and the handler to consume the
     * result when the write completes.
     */
    private class WriteTask<V,A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer[] bufs;
        private final int numBufs;
        private final boolean gatheringWrite;
        private final PendingFuture<V,A> result;

        // set by run method
        private ByteBuffer[] shadow;
        private Runnable scopeHandleReleasers;

        WriteTask(ByteBuffer[] bufs,
                  boolean gatheringWrite,
                  PendingFuture<V,A> result)
        {
            this.bufs = bufs;
            this.numBufs = (bufs.length > MAX_WSABUF) ? MAX_WSABUF : bufs.length;
            this.gatheringWrite = gatheringWrite;
            this.result = result;
        }

        /**
         * Invoked prior to write to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        void prepareBuffers() {
            scopeHandleReleasers = IOUtil.acquireScopes(bufs);
            shadow = new ByteBuffer[numBufs];
            long address = writeBufferArray;
            for (int i=0; i<numBufs; i++) {
                ByteBuffer src = bufs[i];
                int pos = src.position();
                int lim = src.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                long a;
                if (!(src instanceof DirectBuffer)) {
                    // substitute with direct buffer
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    bb.put(src);
                    bb.flip();
                    src.position(pos);  // leave heap buffer untouched for now
                    shadow[i] = bb;
                    a = IOUtil.bufferAddress(bb);
                } else {
                    shadow[i] = src;
                    a = IOUtil.bufferAddress(src) + pos;
                }
                unsafe.putAddress(address + OFFSETOF_BUF, a);
                unsafe.putInt(address + OFFSETOF_LEN, rem);
                address += SIZEOF_WSABUF;
            }
        }

        /**
         * Invoked after a write has completed to update the buffer positions
         * and release any substituted buffers.
         */
        void updateBuffers(int bytesWritten) {
            // Notify the buffers how many bytes were taken
            for (int i=0; i<numBufs; i++) {
                ByteBuffer nextBuffer = bufs[i];
                int pos = nextBuffer.position();
                int lim = nextBuffer.limit();
                int len = (pos <= lim ? lim - pos : lim);
                if (bytesWritten >= len) {
                    bytesWritten -= len;
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch (IllegalArgumentException x) {
                        // position changed by someone else
                    }
                } else { // Buffers not completely filled
                    if (bytesWritten > 0) {
                        assert(pos + bytesWritten < (long)Integer.MAX_VALUE);
                        int newPosition = pos + bytesWritten;
                        try {
                            nextBuffer.position(newPosition);
                        } catch (IllegalArgumentException x) {
                            // position changed by someone else
                        }
                    }
                    break;
                }
            }
        }

        void releaseBuffers() {
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
            IOUtil.releaseScopes(scopeHandleReleasers);
        }

        @Override
        //@SuppressWarnings("unchecked")
        public void run() {
            long overlapped = 0L;
            boolean prepared = false;
            boolean pending = false;
            boolean shutdown = false;

            try {
                begin();

                // substitute non-direct buffers
                prepareBuffers();
                prepared = true;

                // get an OVERLAPPED structure (from the cache or allocate)
                overlapped = ioCache.add(result);
                int n = write0(handle, numBufs, writeBufferArray, overlapped);
                if (n == IOStatus.UNAVAILABLE) {
                    // I/O is pending
                    pending = true;
                    return;
                }
                if (n == IOStatus.EOF) {
                    // special case for shutdown output
                    shutdown = true;
                    throw new ClosedChannelException();
                }
                // write completed immediately
                throw new InternalError("Write completed immediately");
            } catch (Throwable x) {
                // write failed. Enable writing before releasing waiters.
                enableWriting();
                if (!shutdown && (x instanceof ClosedChannelException))
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException))
                    x = new IOException(x);
                result.setFailure(x);
            } finally {
                // release resources if I/O not pending
                if (!pending) {
                    if (overlapped != 0L)
                        ioCache.remove(overlapped);
                    if (prepared)
                        releaseBuffers();
                }
                end();
            }

            // invoke completion handler
            Invoker.invoke(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            updateBuffers(bytesTransferred);

            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            synchronized (result) {
                if (result.isDone())
                    return;
                enableWriting();
                if (gatheringWrite) {
                    result.setResult((V)Long.valueOf(bytesTransferred));
                } else {
                    result.setResult((V)Integer.valueOf(bytesTransferred));
                }
            }
            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        @Override
        public void failed(int error, IOException x) {
            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            if (!isOpen())
                x = new AsynchronousCloseException();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableWriting();
                result.setFailure(x);
            }
            Invoker.invoke(result);
        }

        /**
         * Invoked if timeout expires before it is cancelled
         */
        void timeout() {
            // synchronize on result as the I/O could complete/fail
            synchronized (result) {
                if (result.isDone())
                    return;

                // kill further writing before releasing waiters
                enableWriting(true);
                result.setFailure(new InterruptedByTimeoutException());
            }

            // invoke handler without any locks
            Invoker.invoke(result);
        }
    }

    @Override
    <V extends Number,A> Future<V> implWrite(boolean gatheringWrite,
                                             ByteBuffer src,
                                             ByteBuffer[] srcs,
                                             long timeout,
                                             TimeUnit unit,
                                             A attachment,
                                             CompletionHandler<V,? super A> handler)
    {
        // setup task
        PendingFuture<V,A> result =
            new PendingFuture<V,A>(this, handler, attachment);
        ByteBuffer[] bufs;
        if (gatheringWrite) {
            bufs = srcs;
        } else {
            bufs = new ByteBuffer[1];
            bufs[0] = src;
        }
        final WriteTask<V,A> writeTask =
                new WriteTask<V,A>(bufs, gatheringWrite, result);
        result.setContext(writeTask);

        // schedule timeout
        if (timeout > 0L) {
            Future<?> timeoutTask = iocp.schedule(new Runnable() {
                public void run() {
                    writeTask.timeout();
                }
            }, timeout, unit);
            result.setTimeoutTask(timeoutTask);
        }

        // initiate I/O
        writeTask.run();
        return result;
    }

    // -- Native methods --

    private static native void initIDs();

    private static native int connect0(long socket, boolean preferIPv6,
        InetAddress remote, int remotePort, long overlapped) throws IOException;

    private static native void updateConnectContext(long socket) throws IOException;

    private static native int read0(long socket, int count, long addres, long overlapped)
        throws IOException;

    private static native int write0(long socket, int count, long address,
        long overlapped) throws IOException;

    private static native void shutdown0(long socket, int how) throws IOException;

    private static native void closesocket0(long socket) throws IOException;

    static {
        IOUtil.load();
        initIDs();
    }
}
