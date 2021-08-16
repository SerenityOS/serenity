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
import java.net.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.io.FileDescriptor;

import sun.net.ConnectionResetException;
import sun.net.NetHooks;
import sun.net.util.SocketExceptions;
import sun.security.action.GetPropertyAction;

/**
 * Unix implementation of AsynchronousSocketChannel
 */

class UnixAsynchronousSocketChannelImpl
    extends AsynchronousSocketChannelImpl implements Port.PollableChannel
{
    private static final NativeDispatcher nd = new SocketDispatcher();
    private static enum OpType { CONNECT, READ, WRITE };

    private static final boolean disableSynchronousRead;
    static {
        String propValue = GetPropertyAction.privilegedGetProperty(
            "sun.nio.ch.disableSynchronousRead", "false");
        disableSynchronousRead = propValue.isEmpty() ?
            true : Boolean.parseBoolean(propValue);
    }

    private final Port port;
    private final int fdVal;

    // used to ensure that the context for I/O operations that complete
    // ascynrhonously is visible to the pooled threads handling I/O events.
    private final Object updateLock = new Object();

    // pending connect (updateLock)
    private boolean connectPending;
    private CompletionHandler<Void,Object> connectHandler;
    private Object connectAttachment;
    private PendingFuture<Void,Object> connectFuture;

    // pending remote address (stateLock)
    private SocketAddress pendingRemote;

    // pending read (updateLock)
    private boolean readPending;
    private boolean isScatteringRead;
    private ByteBuffer readBuffer;
    private ByteBuffer[] readBuffers;
    private Runnable readScopeHandleReleasers;
    private CompletionHandler<Number,Object> readHandler;
    private Object readAttachment;
    private PendingFuture<Number,Object> readFuture;
    private Future<?> readTimer;

    // pending write (updateLock)
    private boolean writePending;
    private boolean isGatheringWrite;
    private ByteBuffer writeBuffer;
    private ByteBuffer[] writeBuffers;
    private Runnable writeScopeHandleReleasers;
    private CompletionHandler<Number,Object> writeHandler;
    private Object writeAttachment;
    private PendingFuture<Number,Object> writeFuture;
    private Future<?> writeTimer;


    UnixAsynchronousSocketChannelImpl(Port port)
        throws IOException
    {
        super(port);

        // set non-blocking
        try {
            IOUtil.configureBlocking(fd, false);
        } catch (IOException x) {
            nd.close(fd);
            throw x;
        }

        this.port = port;
        this.fdVal = IOUtil.fdVal(fd);

        // add mapping from file descriptor to this channel
        port.register(fdVal, this);
    }

    // Constructor for sockets created by UnixAsynchronousServerSocketChannelImpl
    UnixAsynchronousSocketChannelImpl(Port port,
                                      FileDescriptor fd,
                                      InetSocketAddress remote)
        throws IOException
    {
        super(port, fd, remote);

        this.fdVal = IOUtil.fdVal(fd);
        IOUtil.configureBlocking(fd, false);

        try {
            port.register(fdVal, this);
        } catch (ShutdownChannelGroupException x) {
            // ShutdownChannelGroupException thrown if we attempt to register a
            // new channel after the group is shutdown
            throw new IOException(x);
        }

        this.port = port;
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return port;
    }

    // register events for outstanding I/O operations, caller already owns updateLock
    private void updateEvents() {
        assert Thread.holdsLock(updateLock);
        int events = 0;
        if (readPending)
            events |= Net.POLLIN;
        if (connectPending || writePending)
            events |= Net.POLLOUT;
        if (events != 0)
            port.startPoll(fdVal, events);
    }

    // register events for outstanding I/O operations
    private void lockAndUpdateEvents() {
        synchronized (updateLock) {
            updateEvents();
        }
    }

    // invoke to finish read and/or write operations
    private void finish(boolean mayInvokeDirect,
                        boolean readable,
                        boolean writable)
    {
        boolean finishRead = false;
        boolean finishWrite = false;
        boolean finishConnect = false;

        // map event to pending result
        synchronized (updateLock) {
            if (readable && this.readPending) {
                this.readPending = false;
                finishRead = true;
            }
            if (writable) {
                if (this.writePending) {
                    this.writePending = false;
                    finishWrite = true;
                } else if (this.connectPending) {
                    this.connectPending = false;
                    finishConnect = true;
                }
            }
        }

        // complete the I/O operation. Special case for when channel is
        // ready for both reading and writing. In that case, submit task to
        // complete write if write operation has a completion handler.
        if (finishRead) {
            if (finishWrite)
                finishWrite(false);
            finishRead(mayInvokeDirect);
            return;
        }
        if (finishWrite) {
            finishWrite(mayInvokeDirect);
        }
        if (finishConnect) {
            finishConnect(mayInvokeDirect);
        }
    }

    /**
     * Invoked by event handler thread when file descriptor is polled
     */
    @Override
    public void onEvent(int events, boolean mayInvokeDirect) {
        boolean readable = (events & Net.POLLIN) > 0;
        boolean writable = (events & Net.POLLOUT) > 0;
        if ((events & (Net.POLLERR | Net.POLLHUP)) > 0) {
            readable = true;
            writable = true;
        }
        finish(mayInvokeDirect, readable, writable);
    }

    @Override
    void implClose() throws IOException {
        // remove the mapping
        port.unregister(fdVal);

        // close file descriptor
        nd.close(fd);

        // All outstanding I/O operations are required to fail
        finish(false, true, true);
    }

    @Override
    public void onCancel(PendingFuture<?,?> task) {
        if (task.getContext() == OpType.CONNECT)
            killConnect();
        if (task.getContext() == OpType.READ)
            killReading();
        if (task.getContext() == OpType.WRITE)
            killWriting();
    }

    // -- connect --

    private void setConnected() throws IOException {
        synchronized (stateLock) {
            state = ST_CONNECTED;
            localAddress = Net.localAddress(fd);
            remoteAddress = (InetSocketAddress)pendingRemote;
        }
    }

    private void finishConnect(boolean mayInvokeDirect) {
        Throwable e = null;
        try {
            begin();
            checkConnect(fdVal);
            setConnected();
        } catch (Throwable x) {
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            e = x;
        } finally {
            end();
        }
        if (e != null) {
            if (e instanceof IOException) {
                var isa = (InetSocketAddress)pendingRemote;
                e = SocketExceptions.of((IOException)e, isa);
            }
            // close channel if connection cannot be established
            try {
                close();
            } catch (Throwable suppressed) {
                e.addSuppressed(suppressed);
            }
        }

        // invoke handler and set result
        CompletionHandler<Void,Object> handler = connectHandler;
        connectHandler = null;
        Object att = connectAttachment;
        PendingFuture<Void,Object> future = connectFuture;
        if (handler == null) {
            future.setResult(null, e);
        } else {
            if (mayInvokeDirect) {
                Invoker.invokeUnchecked(handler, att, null, e);
            } else {
                Invoker.invokeIndirectly(this, handler, att, null, e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    <A> Future<Void> implConnect(SocketAddress remote,
                                 A attachment,
                                 CompletionHandler<Void,? super A> handler)
    {
        if (!isOpen()) {
            Throwable e = new ClosedChannelException();
            if (handler == null) {
                return CompletedFuture.withFailure(e);
            } else {
                Invoker.invoke(this, handler, attachment, null, e);
                return null;
            }
        }

        InetSocketAddress isa = Net.checkAddress(remote);

        // permission check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());

        // check and set state
        boolean notifyBeforeTcpConnect;
        synchronized (stateLock) {
            if (state == ST_CONNECTED)
                throw new AlreadyConnectedException();
            if (state == ST_PENDING)
                throw new ConnectionPendingException();
            state = ST_PENDING;
            pendingRemote = remote;
            notifyBeforeTcpConnect = (localAddress == null);
        }

        Throwable e = null;
        try {
            begin();
            // notify hook if unbound
            if (notifyBeforeTcpConnect)
                NetHooks.beforeTcpConnect(fd, isa.getAddress(), isa.getPort());
            int n = Net.connect(fd, isa.getAddress(), isa.getPort());
            if (n == IOStatus.UNAVAILABLE) {
                // connection could not be established immediately
                PendingFuture<Void,A> result = null;
                synchronized (updateLock) {
                    if (handler == null) {
                        result = new PendingFuture<Void,A>(this, OpType.CONNECT);
                        this.connectFuture = (PendingFuture<Void,Object>)result;
                    } else {
                        this.connectHandler = (CompletionHandler<Void,Object>)handler;
                        this.connectAttachment = attachment;
                    }
                    this.connectPending = true;
                    updateEvents();
                }
                return result;
            }
            setConnected();
        } catch (Throwable x) {
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            e = x;
        } finally {
            end();
        }

        // close channel if connect fails
        if (e != null) {
            if (e instanceof IOException) {
                e = SocketExceptions.of((IOException)e, isa);
            }
            try {
                close();
            } catch (Throwable suppressed) {
                e.addSuppressed(suppressed);
            }
        }
        if (handler == null) {
            return CompletedFuture.withResult(null, e);
        } else {
            Invoker.invoke(this, handler, attachment, null, e);
            return null;
        }
    }

    // -- read --

    private void finishRead(boolean mayInvokeDirect) {
        int n = -1;
        Throwable exc = null;

        // copy fields as we can't access them after reading is re-enabled.
        boolean scattering = isScatteringRead;
        CompletionHandler<Number,Object> handler = readHandler;
        Object att = readAttachment;
        PendingFuture<Number,Object> future = readFuture;
        Future<?> timeout = readTimer;

        try {
            begin();

            if (scattering) {
                n = (int)IOUtil.read(fd, readBuffers, true, nd);
            } else {
                n = IOUtil.read(fd, readBuffer, -1, true, nd);
            }
            if (n == IOStatus.UNAVAILABLE) {
                // spurious wakeup, is this possible?
                synchronized (updateLock) {
                    readPending = true;
                }
                return;
            }

            // allow objects to be GC'ed.
            this.readBuffer = null;
            this.readBuffers = null;
            this.readAttachment = null;
            this.readHandler = null;
            IOUtil.releaseScopes(readScopeHandleReleasers);

            // allow another read to be initiated
            enableReading();

        } catch (Throwable x) {
            enableReading();
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            if (x instanceof ConnectionResetException)
                x = new IOException(x.getMessage());
            exc = x;
        } finally {
            // restart poll in case of concurrent write
            if (!(exc instanceof AsynchronousCloseException))
                lockAndUpdateEvents();
            end();
        }

        // cancel the associated timer
        if (timeout != null)
            timeout.cancel(false);

        // create result
        Number result = (exc != null) ? null : (scattering) ?
            (Number)Long.valueOf(n) : (Number)Integer.valueOf(n);

        // invoke handler or set result
        if (handler == null) {
            future.setResult(result, exc);
        } else {
            if (mayInvokeDirect) {
                Invoker.invokeUnchecked(handler, att, result, exc);
            } else {
                Invoker.invokeIndirectly(this, handler, att, result, exc);
            }
        }
    }

    private Runnable readTimeoutTask = new Runnable() {
        public void run() {
            CompletionHandler<Number,Object> handler = null;
            Object att = null;
            PendingFuture<Number,Object> future = null;

            synchronized (updateLock) {
                if (!readPending)
                    return;
                readPending = false;
                handler = readHandler;
                att = readAttachment;
                future = readFuture;
            }

            // kill further reading before releasing waiters
            enableReading(true);

            // invoke handler or set result
            Exception exc = new InterruptedByTimeoutException();
            if (handler == null) {
                future.setFailure(exc);
            } else {
                AsynchronousChannel ch = UnixAsynchronousSocketChannelImpl.this;
                Invoker.invokeIndirectly(ch, handler, att, null, exc);
            }
        }
    };

    /**
     * Initiates a read or scattering read operation
     */
    @Override
    @SuppressWarnings("unchecked")
    <V extends Number,A> Future<V> implRead(boolean isScatteringRead,
                                            ByteBuffer dst,
                                            ByteBuffer[] dsts,
                                            long timeout,
                                            TimeUnit unit,
                                            A attachment,
                                            CompletionHandler<V,? super A> handler)
    {
        // A synchronous read is not attempted if disallowed by system property
        // or, we are using a fixed thread pool and the completion handler may
        // not be invoked directly (because the thread is not a pooled thread or
        // there are too many handlers on the stack).
        Invoker.GroupAndInvokeCount myGroupAndInvokeCount = null;
        boolean invokeDirect = false;
        boolean attemptRead = false;
        if (!disableSynchronousRead) {
            if (handler == null) {
                attemptRead = true;
            } else {
                myGroupAndInvokeCount = Invoker.getGroupAndInvokeCount();
                invokeDirect = Invoker.mayInvokeDirect(myGroupAndInvokeCount, port);
                // okay to attempt read with user thread pool
                attemptRead = invokeDirect || !port.isFixedThreadPool();
            }
        }

        int n = IOStatus.UNAVAILABLE;
        Throwable exc = null;
        boolean pending = false;

        try {
            begin();

            if (attemptRead) {
                if (isScatteringRead) {
                    n = (int)IOUtil.read(fd, dsts, true, nd);
                } else {
                    n = IOUtil.read(fd, dst, -1, true, nd);
                }
            }

            if (n == IOStatus.UNAVAILABLE) {
                PendingFuture<V,A> result = null;
                synchronized (updateLock) {
                    this.isScatteringRead = isScatteringRead;
                    this.readScopeHandleReleasers = IOUtil.acquireScopes(dst, dsts);
                    this.readBuffer = dst;
                    this.readBuffers = dsts;
                    if (handler == null) {
                        this.readHandler = null;
                        result = new PendingFuture<V,A>(this, OpType.READ);
                        this.readFuture = (PendingFuture<Number,Object>)result;
                        this.readAttachment = null;
                    } else {
                        this.readHandler = (CompletionHandler<Number,Object>)handler;
                        this.readAttachment = attachment;
                        this.readFuture = null;
                    }
                    if (timeout > 0L) {
                        this.readTimer = port.schedule(readTimeoutTask, timeout, unit);
                    }
                    this.readPending = true;
                    updateEvents();
                }
                pending = true;
                return result;
            }
        } catch (Throwable x) {
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            if (x instanceof ConnectionResetException)
                x = new IOException(x.getMessage());
            exc = x;
        } finally {
            if (!pending)
                enableReading();
            end();
        }

        Number result = (exc != null) ? null : (isScatteringRead) ?
            (Number)Long.valueOf(n) : (Number)Integer.valueOf(n);

        // read completed immediately
        if (handler != null) {
            if (invokeDirect) {
                Invoker.invokeDirect(myGroupAndInvokeCount, handler, attachment, (V)result, exc);
            } else {
                Invoker.invokeIndirectly(this, handler, attachment, (V)result, exc);
            }
            return null;
        } else {
            return CompletedFuture.withResult((V)result, exc);
        }
    }

    // -- write --

    private void finishWrite(boolean mayInvokeDirect) {
        int n = -1;
        Throwable exc = null;

        // copy fields as we can't access them after reading is re-enabled.
        boolean gathering = this.isGatheringWrite;
        CompletionHandler<Number,Object> handler = this.writeHandler;
        Object att = this.writeAttachment;
        PendingFuture<Number,Object> future = this.writeFuture;
        Future<?> timer = this.writeTimer;

        try {
            begin();

            if (gathering) {
                n = (int)IOUtil.write(fd, writeBuffers, true, nd);
            } else {
                n = IOUtil.write(fd, writeBuffer, -1, true, nd);
            }
            if (n == IOStatus.UNAVAILABLE) {
                // spurious wakeup, is this possible?
                synchronized (updateLock) {
                    writePending = true;
                }
                return;
            }

            // allow objects to be GC'ed.
            this.writeBuffer = null;
            this.writeBuffers = null;
            this.writeAttachment = null;
            this.writeHandler = null;
            IOUtil.releaseScopes(writeScopeHandleReleasers);

            // allow another write to be initiated
            enableWriting();

        } catch (Throwable x) {
            enableWriting();
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            exc = x;
        } finally {
            // restart poll in case of concurrent write
            if (!(exc instanceof AsynchronousCloseException))
                lockAndUpdateEvents();
            end();
        }

        // cancel the associated timer
        if (timer != null)
            timer.cancel(false);

        // create result
        Number result = (exc != null) ? null : (gathering) ?
            (Number)Long.valueOf(n) : (Number)Integer.valueOf(n);

        // invoke handler or set result
        if (handler == null) {
            future.setResult(result, exc);
        } else {
            if (mayInvokeDirect) {
                Invoker.invokeUnchecked(handler, att, result, exc);
            } else {
                Invoker.invokeIndirectly(this, handler, att, result, exc);
            }
        }
    }

    private Runnable writeTimeoutTask = new Runnable() {
        public void run() {
            CompletionHandler<Number,Object> handler = null;
            Object att = null;
            PendingFuture<Number,Object> future = null;

            synchronized (updateLock) {
                if (!writePending)
                    return;
                writePending = false;
                handler = writeHandler;
                att = writeAttachment;
                future = writeFuture;
            }

            // kill further writing before releasing waiters
            enableWriting(true);

            // invoke handler or set result
            Exception exc = new InterruptedByTimeoutException();
            if (handler != null) {
                Invoker.invokeIndirectly(UnixAsynchronousSocketChannelImpl.this,
                    handler, att, null, exc);
            } else {
                future.setFailure(exc);
            }
        }
    };

    /**
     * Initiates a read or scattering read operation
     */
    @Override
    @SuppressWarnings("unchecked")
    <V extends Number,A> Future<V> implWrite(boolean isGatheringWrite,
                                             ByteBuffer src,
                                             ByteBuffer[] srcs,
                                             long timeout,
                                             TimeUnit unit,
                                             A attachment,
                                             CompletionHandler<V,? super A> handler)
    {
        Invoker.GroupAndInvokeCount myGroupAndInvokeCount =
            Invoker.getGroupAndInvokeCount();
        boolean invokeDirect = Invoker.mayInvokeDirect(myGroupAndInvokeCount, port);
        boolean attemptWrite = (handler == null) || invokeDirect ||
            !port.isFixedThreadPool();  // okay to attempt write with user thread pool

        int n = IOStatus.UNAVAILABLE;
        Throwable exc = null;
        boolean pending = false;

        try {
            begin();

            if (attemptWrite) {
                if (isGatheringWrite) {
                    n = (int)IOUtil.write(fd, srcs, true, nd);
                } else {
                    n = IOUtil.write(fd, src, -1, true, nd);
                }
            }

            if (n == IOStatus.UNAVAILABLE) {
                PendingFuture<V,A> result = null;
                synchronized (updateLock) {
                    this.isGatheringWrite = isGatheringWrite;
                    this.writeScopeHandleReleasers = IOUtil.acquireScopes(src, srcs);
                    this.writeBuffer = src;
                    this.writeBuffers = srcs;
                    if (handler == null) {
                        this.writeHandler = null;
                        result = new PendingFuture<V,A>(this, OpType.WRITE);
                        this.writeFuture = (PendingFuture<Number,Object>)result;
                        this.writeAttachment = null;
                    } else {
                        this.writeHandler = (CompletionHandler<Number,Object>)handler;
                        this.writeAttachment = attachment;
                        this.writeFuture = null;
                    }
                    if (timeout > 0L) {
                        this.writeTimer = port.schedule(writeTimeoutTask, timeout, unit);
                    }
                    this.writePending = true;
                    updateEvents();
                }
                pending = true;
                return result;
            }
        } catch (Throwable x) {
            if (x instanceof ClosedChannelException)
                x = new AsynchronousCloseException();
            exc = x;
        } finally {
            if (!pending)
                enableWriting();
            end();
        }

        Number result = (exc != null) ? null : (isGatheringWrite) ?
            (Number)Long.valueOf(n) : (Number)Integer.valueOf(n);

        // write completed immediately
        if (handler != null) {
            if (invokeDirect) {
                Invoker.invokeDirect(myGroupAndInvokeCount, handler, attachment, (V)result, exc);
            } else {
                Invoker.invokeIndirectly(this, handler, attachment, (V)result, exc);
            }
            return null;
        } else {
            return CompletedFuture.withResult((V)result, exc);
        }
    }

    // -- Native methods --

    private static native void checkConnect(int fdVal) throws IOException;

    static {
        IOUtil.load();
    }
}
