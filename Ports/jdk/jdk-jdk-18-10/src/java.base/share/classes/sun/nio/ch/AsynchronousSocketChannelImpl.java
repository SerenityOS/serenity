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

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.FileDescriptor;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;

/**
 * Base implementation of AsynchronousSocketChannel
 */

abstract class AsynchronousSocketChannelImpl
    extends AsynchronousSocketChannel
    implements Cancellable, Groupable
{
    protected final FileDescriptor fd;

    // protects state, localAddress, and remoteAddress
    protected final Object stateLock = new Object();

    protected volatile InetSocketAddress localAddress;
    protected volatile InetSocketAddress remoteAddress;

    // State, increases monotonically
    static final int ST_UNINITIALIZED = -1;
    static final int ST_UNCONNECTED = 0;
    static final int ST_PENDING = 1;
    static final int ST_CONNECTED = 2;
    protected volatile int state = ST_UNINITIALIZED;

    // reading state
    private final Object readLock = new Object();
    private boolean reading;
    private boolean readShutdown;
    private boolean readKilled;     // further reading disallowed due to timeout

    // writing state
    private final Object writeLock = new Object();
    private boolean writing;
    private boolean writeShutdown;
    private boolean writeKilled;    // further writing disallowed due to timeout

    // close support
    private final ReadWriteLock closeLock = new ReentrantReadWriteLock();
    private volatile boolean closed;

    // set true when exclusive binding is on and SO_REUSEADDR is emulated
    private boolean isReuseAddress;

    AsynchronousSocketChannelImpl(AsynchronousChannelGroupImpl group)
        throws IOException
    {
        super(group.provider());
        this.fd = Net.socket(true);
        this.state = ST_UNCONNECTED;
    }

    // Constructor for sockets obtained from AsynchronousServerSocketChannelImpl
    AsynchronousSocketChannelImpl(AsynchronousChannelGroupImpl group,
                                  FileDescriptor fd,
                                  InetSocketAddress remote)
        throws IOException
    {
        super(group.provider());
        this.fd = fd;
        this.state = ST_CONNECTED;
        this.localAddress = Net.localAddress(fd);
        this.remoteAddress = remote;
    }

    @Override
    public final boolean isOpen() {
        return !closed;
    }

    /**
     * Marks beginning of access to file descriptor/handle
     */
    final void begin() throws IOException {
        closeLock.readLock().lock();
        if (!isOpen())
            throw new ClosedChannelException();
    }

    /**
     * Marks end of access to file descriptor/handle
     */
    final void end() {
        closeLock.readLock().unlock();
    }

    /**
     * Invoked to close socket and release other resources.
     */
    abstract void implClose() throws IOException;

    @Override
    public final void close() throws IOException {
        // synchronize with any threads initiating asynchronous operations
        closeLock.writeLock().lock();
        try {
            if (closed)
                return;     // already closed
            closed = true;
        } finally {
            closeLock.writeLock().unlock();
        }
        implClose();
    }

    final void enableReading(boolean killed) {
        synchronized (readLock) {
            reading = false;
            if (killed)
                readKilled = true;
        }
    }

    final void enableReading() {
        enableReading(false);
    }

    final void enableWriting(boolean killed) {
        synchronized (writeLock) {
            writing = false;
            if (killed)
                writeKilled = true;
        }
    }

    final void enableWriting() {
        enableWriting(false);
    }

    final void killReading() {
        synchronized (readLock) {
            readKilled = true;
        }
    }

    final void killWriting() {
        synchronized (writeLock) {
            writeKilled = true;
        }
    }

    final void killConnect() {
        // when a connect is cancelled then the connection may have been
        // established so prevent reading or writing.
        killReading();
        killWriting();
    }

    /**
     * Invoked by connect to initiate the connect operation.
     */
    abstract <A> Future<Void> implConnect(SocketAddress remote,
                                          A attachment,
                                          CompletionHandler<Void,? super A> handler);

    @Override
    public final Future<Void> connect(SocketAddress remote) {
        return implConnect(remote, null, null);
    }

    @Override
    public final <A> void connect(SocketAddress remote,
                                  A attachment,
                                  CompletionHandler<Void,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        implConnect(remote, attachment, handler);
    }

    /**
     * Invoked by read to initiate the I/O operation.
     */
    abstract <V extends Number,A> Future<V> implRead(boolean isScatteringRead,
                                                     ByteBuffer dst,
                                                     ByteBuffer[] dsts,
                                                     long timeout,
                                                     TimeUnit unit,
                                                     A attachment,
                                                     CompletionHandler<V,? super A> handler);

    @SuppressWarnings("unchecked")
    private <V extends Number,A> Future<V> read(boolean isScatteringRead,
                                                ByteBuffer dst,
                                                ByteBuffer[] dsts,
                                                long timeout,
                                                TimeUnit unit,
                                                A att,
                                                CompletionHandler<V,? super A> handler)
    {
        if (!isOpen()) {
            Throwable e = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(e);
            Invoker.invoke(this, handler, att, null, e);
            return null;
        }

        if (remoteAddress == null)
            throw new NotYetConnectedException();

        boolean hasSpaceToRead = isScatteringRead || dst.hasRemaining();
        boolean shutdown = false;

        // check and update state
        synchronized (readLock) {
            if (readKilled)
                throw new IllegalStateException("Reading not allowed due to timeout or cancellation");
            if (reading)
                throw new ReadPendingException();
            if (readShutdown) {
                shutdown = true;
            } else {
                if (hasSpaceToRead) {
                    reading = true;
                }
            }
        }

        // immediately complete with -1 if shutdown for read
        // immediately complete with 0 if no space remaining
        if (shutdown || !hasSpaceToRead) {
            Number result;
            if (isScatteringRead) {
                result = (shutdown) ? Long.valueOf(-1L) : Long.valueOf(0L);
            } else {
                result = (shutdown) ? -1 : 0;
            }
            if (handler == null)
                return CompletedFuture.withResult((V)result);
            Invoker.invoke(this, handler, att, (V)result, null);
            return null;
        }

        return implRead(isScatteringRead, dst, dsts, timeout, unit, att, handler);
    }

    @Override
    public final Future<Integer> read(ByteBuffer dst) {
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        return read(false, dst, null, 0L, TimeUnit.MILLISECONDS, null, null);
    }

    @Override
    public final <A> void read(ByteBuffer dst,
                               long timeout,
                               TimeUnit unit,
                               A attachment,
                               CompletionHandler<Integer,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        read(false, dst, null, timeout, unit, attachment, handler);
    }

    @Override
    public final <A> void read(ByteBuffer[] dsts,
                               int offset,
                               int length,
                               long timeout,
                               TimeUnit unit,
                               A attachment,
                               CompletionHandler<Long,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        Objects.checkFromIndexSize(offset, length, dsts.length);
        ByteBuffer[] bufs = Util.subsequence(dsts, offset, length);
        for (int i=0; i<bufs.length; i++) {
            if (bufs[i].isReadOnly())
                throw new IllegalArgumentException("Read-only buffer");
        }
        read(true, null, bufs, timeout, unit, attachment, handler);
    }

    /**
     * Invoked by write to initiate the I/O operation.
     */
    abstract <V extends Number,A> Future<V> implWrite(boolean isGatheringWrite,
                                                      ByteBuffer src,
                                                      ByteBuffer[] srcs,
                                                      long timeout,
                                                      TimeUnit unit,
                                                      A attachment,
                                                      CompletionHandler<V,? super A> handler);

    @SuppressWarnings("unchecked")
    private <V extends Number,A> Future<V> write(boolean isGatheringWrite,
                                                 ByteBuffer src,
                                                 ByteBuffer[] srcs,
                                                 long timeout,
                                                 TimeUnit unit,
                                                 A att,
                                                 CompletionHandler<V,? super A> handler)
    {
        boolean hasDataToWrite = isGatheringWrite || src.hasRemaining();

        boolean closed = false;
        if (isOpen()) {
            if (remoteAddress == null)
                throw new NotYetConnectedException();
            // check and update state
            synchronized (writeLock) {
                if (writeKilled)
                    throw new IllegalStateException("Writing not allowed due to timeout or cancellation");
                if (writing)
                    throw new WritePendingException();
                if (writeShutdown) {
                    closed = true;
                } else {
                    if (hasDataToWrite)
                        writing = true;
                }
            }
        } else {
            closed = true;
        }

        // channel is closed or shutdown for write
        if (closed) {
            Throwable e = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(e);
            Invoker.invoke(this, handler, att, null, e);
            return null;
        }

        // nothing to write so complete immediately
        if (!hasDataToWrite) {
            Number result = (isGatheringWrite) ? (Number)0L : (Number)0;
            if (handler == null)
                return CompletedFuture.withResult((V)result);
            Invoker.invoke(this, handler, att, (V)result, null);
            return null;
        }

        return implWrite(isGatheringWrite, src, srcs, timeout, unit, att, handler);
    }

    @Override
    public final Future<Integer> write(ByteBuffer src) {
        return write(false, src, null, 0L, TimeUnit.MILLISECONDS, null, null);
    }

    @Override
    public final <A> void write(ByteBuffer src,
                                long timeout,
                                TimeUnit unit,
                                A attachment,
                                CompletionHandler<Integer,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        write(false, src, null, timeout, unit, attachment, handler);
    }

    @Override
    public final <A> void  write(ByteBuffer[] srcs,
                                 int offset,
                                 int length,
                                 long timeout,
                                 TimeUnit unit,
                                 A attachment,
                                 CompletionHandler<Long,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        Objects.checkFromIndexSize(offset, length, srcs.length);
        srcs = Util.subsequence(srcs, offset, length);
        write(true, null, srcs, timeout, unit, attachment, handler);
    }

    @Override
    public final AsynchronousSocketChannel bind(SocketAddress local)
        throws IOException
    {
        try {
            begin();
            synchronized (stateLock) {
                if (state == ST_PENDING)
                    throw new ConnectionPendingException();
                if (localAddress != null)
                    throw new AlreadyBoundException();
                InetSocketAddress isa = (local == null) ?
                    new InetSocketAddress(0) : Net.checkAddress(local);
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkListen(isa.getPort());
                }
                NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
                Net.bind(fd, isa.getAddress(), isa.getPort());
                localAddress = Net.localAddress(fd);
            }
        } finally {
            end();
        }
        return this;
    }

    @Override
    public final SocketAddress getLocalAddress() throws IOException {
        if (!isOpen())
            throw new ClosedChannelException();
         return Net.getRevealedLocalAddress(localAddress);
    }

    @Override
    public final <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value)
        throws IOException
    {
        if (name == null)
            throw new NullPointerException();
        if (!supportedOptions().contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");

        try {
            begin();
            if (writeShutdown)
                throw new IOException("Connection has been shutdown for writing");
            if (name == StandardSocketOptions.SO_REUSEADDR &&
                    Net.useExclusiveBind())
            {
                // SO_REUSEADDR emulated when using exclusive bind
                isReuseAddress = (Boolean)value;
            } else {
                Net.setSocketOption(fd, Net.UNSPEC, name, value);
            }
            return this;
        } finally {
            end();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getOption(SocketOption<T> name) throws IOException {
        if (name == null)
            throw new NullPointerException();
        if (!supportedOptions().contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");

        try {
            begin();
            if (name == StandardSocketOptions.SO_REUSEADDR &&
                    Net.useExclusiveBind())
            {
                // SO_REUSEADDR emulated when using exclusive bind
                return (T)Boolean.valueOf(isReuseAddress);
            }
            return (T) Net.getSocketOption(fd, Net.UNSPEC, name);
        } finally {
            end();
        }
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>(5);
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_KEEPALIVE);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if (Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.add(StandardSocketOptions.TCP_NODELAY);
            set.addAll(ExtendedSocketOptions.clientSocketOptions());
            return Collections.unmodifiableSet(set);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    @Override
    public final SocketAddress getRemoteAddress() throws IOException {
        if (!isOpen())
            throw new ClosedChannelException();
        return remoteAddress;
    }

    @Override
    public final AsynchronousSocketChannel shutdownInput() throws IOException {
        try {
            begin();
            if (remoteAddress == null)
                throw new NotYetConnectedException();
            synchronized (readLock) {
                if (!readShutdown) {
                    Net.shutdown(fd, Net.SHUT_RD);
                    readShutdown = true;
                }
            }
        } finally {
            end();
        }
        return this;
    }

    @Override
    public final AsynchronousSocketChannel shutdownOutput() throws IOException {
        try {
            begin();
            if (remoteAddress == null)
                throw new NotYetConnectedException();
            synchronized (writeLock) {
                if (!writeShutdown) {
                    Net.shutdown(fd, Net.SHUT_WR);
                    writeShutdown = true;
                }
            }
        } finally {
            end();
        }
        return this;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append('[');
        synchronized (stateLock) {
            if (!isOpen()) {
                sb.append("closed");
            } else {
                switch (state) {
                case ST_UNCONNECTED:
                    sb.append("unconnected");
                    break;
                case ST_PENDING:
                    sb.append("connection-pending");
                    break;
                case ST_CONNECTED:
                    sb.append("connected");
                    if (readShutdown)
                        sb.append(" ishut");
                    if (writeShutdown)
                        sb.append(" oshut");
                    break;
                }
                if (localAddress != null) {
                    sb.append(" local=");
                    sb.append(
                            Net.getRevealedLocalAddressAsString(localAddress));
                }
                if (remoteAddress != null) {
                    sb.append(" remote=");
                    sb.append(remoteAddress.toString());
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
