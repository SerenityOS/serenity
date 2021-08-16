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
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import jdk.internal.misc.Unsafe;

/**
 * Windows implementation of AsynchronousServerSocketChannel using overlapped I/O.
 */

class WindowsAsynchronousServerSocketChannelImpl
    extends AsynchronousServerSocketChannelImpl implements Iocp.OverlappedChannel
{
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    // 2 * (sizeof(SOCKET_ADDRESS) + 16)
    private static final int DATA_BUFFER_SIZE = 88;

    private final long handle;
    private final int completionKey;
    private final Iocp iocp;

    // typically there will be zero, or one I/O operations pending. In rare
    // cases there may be more. These rare cases arise when a sequence of accept
    // operations complete immediately and handled by the initiating thread.
    // The corresponding OVERLAPPED cannot be reused/released until the completion
    // event has been posted.
    private final PendingIoCache ioCache;

    // the data buffer to receive the local/remote socket address
    private final long dataBuffer;

    // flag to indicate that an accept operation is outstanding
    private AtomicBoolean accepting = new AtomicBoolean();


    WindowsAsynchronousServerSocketChannelImpl(Iocp iocp) throws IOException {
        super(iocp);

        // associate socket with given completion port
        long h = IOUtil.fdVal(fd);
        int key;
        try {
            key = iocp.associate(this, h);
        } catch (IOException x) {
            closesocket0(h);   // prevent leak
            throw x;
        }

        this.handle = h;
        this.completionKey = key;
        this.iocp = iocp;
        this.ioCache = new PendingIoCache();
        this.dataBuffer = unsafe.allocateMemory(DATA_BUFFER_SIZE);
    }

    @Override
    public <V,A> PendingFuture<V,A> getByOverlapped(long overlapped) {
        return ioCache.remove(overlapped);
    }

    @Override
    void implClose() throws IOException {
        // close socket (which may cause outstanding accept to be aborted).
        closesocket0(handle);

        // waits until the accept operations have completed
        ioCache.close();

        // finally disassociate from the completion port
        iocp.disassociate(completionKey);

        // release other resources
        unsafe.freeMemory(dataBuffer);
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }

    /**
     * Task to initiate accept operation and to handle result.
     */
    private class AcceptTask implements Runnable, Iocp.ResultHandler {
        private final WindowsAsynchronousSocketChannelImpl channel;
        @SuppressWarnings("removal")
        private final AccessControlContext acc;
        private final PendingFuture<AsynchronousSocketChannel,Object> result;

        AcceptTask(WindowsAsynchronousSocketChannelImpl channel,
                   @SuppressWarnings("removal") AccessControlContext acc,
                   PendingFuture<AsynchronousSocketChannel,Object> result)
        {
            this.channel = channel;
            this.acc = acc;
            this.result = result;
        }

        void enableAccept() {
            accepting.set(false);
        }

        void closeChildChannel() {
            try {
                channel.close();
            } catch (IOException ignore) { }
        }

        // caller must have acquired read lock for the listener and child channel.
        @SuppressWarnings("removal")
        void finishAccept() throws IOException {
            /**
             * Set local/remote addresses. This is currently very inefficient
             * in that it requires 2 calls to getsockname and 2 calls to getpeername.
             * (should change this to use GetAcceptExSockaddrs)
             */
            updateAcceptContext(handle, channel.handle());

            InetSocketAddress local = Net.localAddress(channel.fd);
            final InetSocketAddress remote = Net.remoteAddress(channel.fd);
            channel.setConnected(local, remote);

            // permission check (in context of initiating thread)
            if (acc != null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        SecurityManager sm = System.getSecurityManager();
                        sm.checkAccept(remote.getAddress().getHostAddress(),
                                       remote.getPort());
                        return null;
                    }
                }, acc);
            }
        }

        /**
         * Initiates the accept operation.
         */
        @Override
        public void run() {
            long overlapped = 0L;

            try {
                // begin usage of listener socket
                begin();
                try {
                    // begin usage of child socket (as it is registered with
                    // completion port and so may be closed in the event that
                    // the group is forcefully closed).
                    channel.begin();

                    synchronized (result) {
                        overlapped = ioCache.add(result);

                        int n = accept0(handle, channel.handle(), overlapped, dataBuffer);
                        if (n == IOStatus.UNAVAILABLE) {
                            return;
                        }

                        // connection accepted immediately
                        finishAccept();

                        // allow another accept before the result is set
                        enableAccept();
                        result.setResult(channel);
                    }
                } finally {
                    // end usage on child socket
                    channel.end();
                }
            } catch (Throwable x) {
                // failed to initiate accept so release resources
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
                closeChildChannel();
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException) && !(x instanceof SecurityException))
                    x = new IOException(x);
                enableAccept();
                result.setFailure(x);
            } finally {
                // end of usage of listener socket
                end();
            }

            // accept completed immediately but may not have executed on
            // initiating thread in which case the operation may have been
            // cancelled.
            if (result.isCancelled()) {
                closeChildChannel();
            }

            // invoke completion handler
            Invoker.invokeIndirectly(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            try {
                // connection accept after group has shutdown
                if (iocp.isShutdown()) {
                    throw new IOException(new ShutdownChannelGroupException());
                }

                // finish the accept
                try {
                    begin();
                    try {
                        channel.begin();
                        finishAccept();
                    } finally {
                        channel.end();
                    }
                } finally {
                    end();
                }

                // allow another accept before the result is set
                enableAccept();
                result.setResult(channel);
            } catch (Throwable x) {
                enableAccept();
                closeChildChannel();
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException) && !(x instanceof SecurityException))
                    x = new IOException(x);
                result.setFailure(x);
            }

            // if an async cancel has already cancelled the operation then
            // close the new channel so as to free resources
            if (result.isCancelled()) {
                closeChildChannel();
            }

            // invoke handler (but not directly)
            Invoker.invokeIndirectly(result);
        }

        @Override
        public void failed(int error, IOException x) {
            enableAccept();
            closeChildChannel();

            // release waiters
            if (isOpen()) {
                result.setFailure(x);
            } else {
                result.setFailure(new AsynchronousCloseException());
            }
            Invoker.invokeIndirectly(result);
        }
    }

    @Override
    Future<AsynchronousSocketChannel> implAccept(Object attachment,
        final CompletionHandler<AsynchronousSocketChannel,Object> handler)
    {
        if (!isOpen()) {
            Throwable exc = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(exc);
            Invoker.invokeIndirectly(this, handler, attachment, null, exc);
            return null;
        }
        if (isAcceptKilled())
            throw new RuntimeException("Accept not allowed due to cancellation");

        // ensure channel is bound to local address
        if (localAddress == null)
            throw new NotYetBoundException();

        // create the socket that will be accepted. The creation of the socket
        // is enclosed by a begin/end for the listener socket to ensure that
        // we check that the listener is open and also to prevent the I/O
        // port from being closed as the new socket is registered.
        WindowsAsynchronousSocketChannelImpl ch = null;
        IOException ioe = null;
        try {
            begin();
            ch = new WindowsAsynchronousSocketChannelImpl(iocp, false);
        } catch (IOException x) {
            ioe = x;
        } finally {
            end();
        }
        if (ioe != null) {
            if (handler == null)
                return CompletedFuture.withFailure(ioe);
            Invoker.invokeIndirectly(this, handler, attachment, null, ioe);
            return null;
        }

        // need calling context when there is security manager as
        // permission check may be done in a different thread without
        // any application call frames on the stack
        @SuppressWarnings("removal")
        AccessControlContext acc = (System.getSecurityManager() == null) ?
            null : AccessController.getContext();

        PendingFuture<AsynchronousSocketChannel,Object> result =
            new PendingFuture<AsynchronousSocketChannel,Object>(this, handler, attachment);
        AcceptTask task = new AcceptTask(ch, acc, result);
        result.setContext(task);

        // check and set flag to prevent concurrent accepting
        if (!accepting.compareAndSet(false, true))
            throw new AcceptPendingException();

        // initiate I/O
        task.run();
        return result;
    }

    // -- Native methods --

    private static native void initIDs();

    private static native int accept0(long listenSocket, long acceptSocket,
        long overlapped, long dataBuffer) throws IOException;

    private static native void updateAcceptContext(long listenSocket,
        long acceptSocket) throws IOException;

    private static native void closesocket0(long socket) throws IOException;

    static {
        IOUtil.load();
        initIDs();
    }
}
