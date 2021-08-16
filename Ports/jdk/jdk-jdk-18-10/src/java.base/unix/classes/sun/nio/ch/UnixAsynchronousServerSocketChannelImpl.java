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
import java.util.concurrent.*;
import java.io.IOException;
import java.io.FileDescriptor;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Unix implementation of AsynchronousServerSocketChannel
 */

class UnixAsynchronousServerSocketChannelImpl
    extends AsynchronousServerSocketChannelImpl
    implements Port.PollableChannel
{
    private static final NativeDispatcher nd = new SocketDispatcher();

    private final Port port;
    private final int fdVal;

    // flag to indicate an accept is outstanding
    private final AtomicBoolean accepting = new AtomicBoolean();
    private void enableAccept() {
        accepting.set(false);
    }

    // used to ensure that the context for an asynchronous accept is visible
    // the pooled thread that handles the I/O event
    private final Object updateLock = new Object();

    // pending accept
    private boolean acceptPending;
    private CompletionHandler<AsynchronousSocketChannel,Object> acceptHandler;
    private Object acceptAttachment;
    private PendingFuture<AsynchronousSocketChannel,Object> acceptFuture;

    // context for permission check when security manager set
    @SuppressWarnings("removal")
    private AccessControlContext acceptAcc;


    UnixAsynchronousServerSocketChannelImpl(Port port)
        throws IOException
    {
        super(port);

        try {
            IOUtil.configureBlocking(fd, false);
        } catch (IOException x) {
            nd.close(fd);  // prevent leak
            throw x;
        }
        this.port = port;
        this.fdVal = IOUtil.fdVal(fd);

        // add mapping from file descriptor to this channel
        port.register(fdVal, this);
    }

    @Override
    void implClose() throws IOException {
        // remove the mapping
        port.unregister(fdVal);

        // close file descriptor
        nd.close(fd);

        // if there is a pending accept then complete it
        CompletionHandler<AsynchronousSocketChannel,Object> handler;
        Object att;
        PendingFuture<AsynchronousSocketChannel,Object> future;
        synchronized (updateLock) {
            if (!acceptPending)
                return;  // no pending accept
            acceptPending = false;
            handler = acceptHandler;
            att = acceptAttachment;
            future = acceptFuture;
        }

        // discard the stack trace as otherwise it may appear that implClose
        // has thrown the exception.
        AsynchronousCloseException x = new AsynchronousCloseException();
        x.setStackTrace(new StackTraceElement[0]);
        if (handler == null) {
            future.setFailure(x);
        } else {
            // invoke by submitting task rather than directly
            Invoker.invokeIndirectly(this, handler, att, null, x);
        }
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return port;
    }

    /**
     * Invoked by event handling thread when listener socket is polled
     */
    @Override
    public void onEvent(int events, boolean mayInvokeDirect) {
        synchronized (updateLock) {
            if (!acceptPending)
                return;  // may have been grabbed by asynchronous close
            acceptPending = false;
        }

        // attempt to accept connection
        FileDescriptor newfd = new FileDescriptor();
        InetSocketAddress[] isaa = new InetSocketAddress[1];
        Throwable exc = null;
        try {
            begin();
            int n = Net.accept(this.fd, newfd, isaa);

            // spurious wakeup, is this possible?
            if (n == IOStatus.UNAVAILABLE) {
                synchronized (updateLock) {
                    acceptPending = true;
                }
                port.startPoll(fdVal, Net.POLLIN);
                return;
            }

        } catch (Throwable x) {
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            exc = x;
        } finally {
            end();
        }

        // Connection accepted so finish it when not holding locks.
        AsynchronousSocketChannel child = null;
        if (exc == null) {
            try {
                child = finishAccept(newfd, isaa[0], acceptAcc);
            } catch (Throwable x) {
                if (!(x instanceof IOException) && !(x instanceof SecurityException))
                    x = new IOException(x);
                exc = x;
            }
        }

        // copy field befores accept is re-renabled
        CompletionHandler<AsynchronousSocketChannel,Object> handler = acceptHandler;
        Object att = acceptAttachment;
        PendingFuture<AsynchronousSocketChannel,Object> future = acceptFuture;

        // re-enable accepting and invoke handler
        enableAccept();

        if (handler == null) {
            future.setResult(child, exc);
            // if an async cancel has already cancelled the operation then
            // close the new channel so as to free resources
            if (child != null && future.isCancelled()) {
                try {
                    child.close();
                } catch (IOException ignore) { }
            }
        } else {
            Invoker.invoke(this, handler, att, child, exc);
        }
    }

    /**
     * Completes the accept by creating the AsynchronousSocketChannel for
     * the given file descriptor and remote address. If this method completes
     * with an IOException or SecurityException then the channel/file descriptor
     * will be closed.
     */
    @SuppressWarnings("removal")
    private AsynchronousSocketChannel finishAccept(FileDescriptor newfd,
                                                   final InetSocketAddress remote,
                                                   AccessControlContext acc)
        throws IOException, SecurityException
    {
        AsynchronousSocketChannel ch = null;
        try {
            ch = new UnixAsynchronousSocketChannelImpl(port, newfd, remote);
        } catch (IOException x) {
            nd.close(newfd);
            throw x;
        }

        // permission check must always be in initiator's context
        try {
            if (acc != null) {
                AccessController.doPrivileged(new PrivilegedAction<>() {
                    public Void run() {
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            sm.checkAccept(remote.getAddress().getHostAddress(),
                                    remote.getPort());
                        }
                        return null;
                    }
                }, acc);
            } else {
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkAccept(remote.getAddress().getHostAddress(),
                            remote.getPort());
                }
            }
        } catch (SecurityException x) {
            try {
                ch.close();
            } catch (Throwable suppressed) {
                x.addSuppressed(suppressed);
            }
            throw x;
        }
        return ch;
    }

    @SuppressWarnings("removal")
    @Override
    Future<AsynchronousSocketChannel> implAccept(Object att,
        CompletionHandler<AsynchronousSocketChannel,Object> handler)
    {
        // complete immediately if channel is closed
        if (!isOpen()) {
            Throwable e = new ClosedChannelException();
            if (handler == null) {
                return CompletedFuture.withFailure(e);
            } else {
                Invoker.invoke(this, handler, att, null, e);
                return null;
            }
        }
        if (localAddress == null)
            throw new NotYetBoundException();

        // cancel was invoked with pending accept so connection may have been
        // dropped.
        if (isAcceptKilled())
            throw new RuntimeException("Accept not allowed due cancellation");

        // check and set flag to prevent concurrent accepting
        if (!accepting.compareAndSet(false, true))
            throw new AcceptPendingException();

        // attempt accept
        FileDescriptor newfd = new FileDescriptor();
        InetSocketAddress[] isaa = new InetSocketAddress[1];
        Throwable exc = null;
        try {
            begin();

            int n = Net.accept(this.fd, newfd, isaa);
            if (n == IOStatus.UNAVAILABLE) {

                // need calling context when there is security manager as
                // permission check may be done in a different thread without
                // any application call frames on the stack
                PendingFuture<AsynchronousSocketChannel,Object> result = null;
                synchronized (updateLock) {
                    if (handler == null) {
                        this.acceptHandler = null;
                        result = new PendingFuture<>(this);
                        this.acceptFuture = result;
                    } else {
                        this.acceptHandler = handler;
                        this.acceptAttachment = att;
                    }
                    this.acceptAcc = (System.getSecurityManager() == null) ?
                            null : AccessController.getContext();
                    this.acceptPending = true;
                }

                // register for connections
                port.startPoll(fdVal, Net.POLLIN);
                return result;
            }
        } catch (Throwable x) {
            // accept failed
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            exc = x;
        } finally {
            end();
        }

        AsynchronousSocketChannel child = null;
        if (exc == null) {
            // connection accepted immediately
            try {
                child = finishAccept(newfd, isaa[0], null);
            } catch (Throwable x) {
                exc = x;
            }
        }

        // re-enable accepting before invoking handler
        enableAccept();

        if (handler == null) {
            return CompletedFuture.withResult(child, exc);
        } else {
            Invoker.invokeIndirectly(this, handler, att, child, exc);
            return null;
        }
    }
}
