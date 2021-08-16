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
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardProtocolFamily.INET6;
import static java.net.StandardProtocolFamily.UNIX;

import sun.net.ConnectionResetException;
import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;
import sun.net.util.SocketExceptions;

/**
 * An implementation of SocketChannels
 */

class SocketChannelImpl
    extends SocketChannel
    implements SelChImpl
{
    // Used to make native read and write calls
    private static final NativeDispatcher nd = new SocketDispatcher();

    // The protocol family of the socket
    private final ProtocolFamily family;

    // Our file descriptor object
    private final FileDescriptor fd;
    private final int fdVal;

    // Lock held by current reading or connecting thread
    private final ReentrantLock readLock = new ReentrantLock();

    // Lock held by current writing or connecting thread
    private final ReentrantLock writeLock = new ReentrantLock();

    // Lock held by any thread that modifies the state fields declared below
    // DO NOT invoke a blocking I/O operation while holding this lock!
    private final Object stateLock = new Object();

    // Input/Output closed
    private volatile boolean isInputClosed;
    private volatile boolean isOutputClosed;

    // Connection reset protected by readLock
    private boolean connectionReset;

    // -- The following fields are protected by stateLock

    // set true when exclusive binding is on and SO_REUSEADDR is emulated
    private boolean isReuseAddress;

    // State, increases monotonically
    private static final int ST_UNCONNECTED = 0;
    private static final int ST_CONNECTIONPENDING = 1;
    private static final int ST_CONNECTED = 2;
    private static final int ST_CLOSING = 3;
    private static final int ST_CLOSED = 4;
    private volatile int state;  // need stateLock to change

    // IDs of native threads doing reads and writes, for signalling
    private long readerThread;
    private long writerThread;

    // Binding
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;

    // Socket adaptor, created on demand
    private Socket socket;

    // -- End of fields protected by stateLock

    SocketChannelImpl(SelectorProvider sp) throws IOException {
        this(sp, Net.isIPv6Available() ? INET6 : INET);
    }

    SocketChannelImpl(SelectorProvider sp, ProtocolFamily family) throws IOException {
        super(sp);
        Objects.requireNonNull(family, "'family' is null");
        if ((family != INET) && (family != INET6) && (family != UNIX)) {
            throw new UnsupportedOperationException("Protocol family not supported");
        }
        if (family == INET6 && !Net.isIPv6Available()) {
            throw new UnsupportedOperationException("IPv6 not available");
        }

        this.family = family;
        if (family == UNIX) {
            this.fd = UnixDomainSockets.socket();
        } else {
            this.fd = Net.socket(family, true);
        }
        this.fdVal = IOUtil.fdVal(fd);
    }

    // Constructor for sockets obtained from server sockets
    //
    SocketChannelImpl(SelectorProvider sp,
                      ProtocolFamily family,
                      FileDescriptor fd,
                      SocketAddress remoteAddress)
        throws IOException
    {
        super(sp);
        this.family = family;
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
        synchronized (stateLock) {
            if (family == UNIX) {
                this.localAddress = UnixDomainSockets.localAddress(fd);
            } else {
                this.localAddress = Net.localAddress(fd);
            }
            this.remoteAddress = remoteAddress;
            this.state = ST_CONNECTED;
        }
    }

    /**
     * Returns true if this channel is to a INET or INET6 socket.
     */
    boolean isNetSocket() {
        return (family == INET) || (family == INET6);
    }

    /**
     * Returns true if this channel is to a UNIX socket.
     */
    boolean isUnixSocket() {
        return (family == UNIX);
    }

    /**
     * Checks that the channel is open.
     *
     * @throws ClosedChannelException if channel is closed (or closing)
     */
    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen())
            throw new ClosedChannelException();
    }

    /**
     * Checks that the channel is open and connected.
     *
     * @apiNote This method uses the "state" field to check if the channel is
     * open. It should never be used in conjuncion with isOpen or ensureOpen
     * as these methods check AbstractInterruptibleChannel's closed field - that
     * field is set before implCloseSelectableChannel is called and so before
     * the state is changed.
     *
     * @throws ClosedChannelException if channel is closed (or closing)
     * @throws NotYetConnectedException if open and not connected
     */
    private void ensureOpenAndConnected() throws ClosedChannelException {
        int state = this.state;
        if (state < ST_CONNECTED) {
            throw new NotYetConnectedException();
        } else if (state > ST_CONNECTED) {
            throw new ClosedChannelException();
        }
    }

    @Override
    public Socket socket() {
        synchronized (stateLock) {
            if (socket == null) {
                if (isNetSocket()) {
                    socket = SocketAdaptor.create(this);
                } else {
                    throw new UnsupportedOperationException("Not supported");
                }
            }
            return socket;
        }
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (isUnixSocket()) {
                return UnixDomainSockets.getRevealedLocalAddress(localAddress);
            } else {
                return Net.getRevealedLocalAddress(localAddress);
            }
        }
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            return remoteAddress;
        }
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value)
        throws IOException
    {
        Objects.requireNonNull(name);
        if (!supportedOptions().contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");
        if (!name.type().isInstance(value))
            throw new IllegalArgumentException("Invalid value '" + value + "'");

        synchronized (stateLock) {
            ensureOpen();

            if (isNetSocket()) {
                if (name == StandardSocketOptions.IP_TOS) {
                    // special handling for IP_TOS
                    Net.setSocketOption(fd, family, name, value);
                    return this;
                }
                if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                    // SO_REUSEADDR emulated when using exclusive bind
                    isReuseAddress = (Boolean) value;
                    return this;
                }
            }

            // no options that require special handling
            Net.setSocketOption(fd, name, value);
            return this;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOption(SocketOption<T> name)
        throws IOException
    {
        Objects.requireNonNull(name);
        if (!supportedOptions().contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");

        synchronized (stateLock) {
            ensureOpen();

            if (isNetSocket()) {
                if (name == StandardSocketOptions.IP_TOS) {
                    // special handling for IP_TOS
                    return (T) Net.getSocketOption(fd, family, name);
                }
                if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                    // SO_REUSEADDR emulated when using exclusive bind
                    return (T) Boolean.valueOf(isReuseAddress);
                }
            }

            // no options that require special handling
            return (T) Net.getSocketOption(fd, name);
        }
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultInetOptions = defaultInetOptions();
        static final Set<SocketOption<?>> defaultUnixOptions = defaultUnixOptions();

        private static Set<SocketOption<?>> defaultInetOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_KEEPALIVE);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if (Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.add(StandardSocketOptions.SO_LINGER);
            set.add(StandardSocketOptions.TCP_NODELAY);
            // additional options required by socket adaptor
            set.add(StandardSocketOptions.IP_TOS);
            set.add(ExtendedSocketOption.SO_OOBINLINE);
            set.addAll(ExtendedSocketOptions.clientSocketOptions());
            return Collections.unmodifiableSet(set);
        }

        private static Set<SocketOption<?>> defaultUnixOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_LINGER);
            set.addAll(ExtendedSocketOptions.unixDomainSocketOptions());
            return Collections.unmodifiableSet(set);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        if (isUnixSocket()) {
            return DefaultOptionsHolder.defaultUnixOptions;
        } else {
            return DefaultOptionsHolder.defaultInetOptions;
        }
    }

    /**
     * Marks the beginning of a read operation that might block.
     *
     * @throws ClosedChannelException if blocking and the channel is closed
     */
    private void beginRead(boolean blocking) throws ClosedChannelException {
        if (blocking) {
            // set hook for Thread.interrupt
            begin();

            synchronized (stateLock) {
                ensureOpen();
                // record thread so it can be signalled if needed
                readerThread = NativeThread.current();
            }
        }
    }

    /**
     * Marks the end of a read operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     * thread being interrupted on a blocking read operation.
     */
    private void endRead(boolean blocking, boolean completed)
        throws AsynchronousCloseException
    {
        if (blocking) {
            synchronized (stateLock) {
                readerThread = 0;
                if (state == ST_CLOSING) {
                    tryFinishClose();
                }
            }
            // remove hook for Thread.interrupt
            end(completed);
        }
    }

    private void throwConnectionReset() throws SocketException {
        throw new SocketException("Connection reset");
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        Objects.requireNonNull(buf);

        readLock.lock();
        try {
            ensureOpenAndConnected();
            boolean blocking = isBlocking();
            int n = 0;
            try {
                beginRead(blocking);

                // check if connection has been reset
                if (connectionReset)
                    throwConnectionReset();

                // check if input is shutdown
                if (isInputClosed)
                    return IOStatus.EOF;

                n = IOUtil.read(fd, buf, -1, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = IOUtil.read(fd, buf, -1, nd);
                    }
                }
            } catch (ConnectionResetException e) {
                connectionReset = true;
                throwConnectionReset();
            } finally {
                endRead(blocking, n > 0);
                if (n <= 0 && isInputClosed)
                    return IOStatus.EOF;
            }
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length)
        throws IOException
    {
        Objects.checkFromIndexSize(offset, length, dsts.length);

        readLock.lock();
        try {
            ensureOpenAndConnected();
            boolean blocking = isBlocking();
            long n = 0;
            try {
                beginRead(blocking);

                // check if connection has been reset
                if (connectionReset)
                    throwConnectionReset();

                // check if input is shutdown
                if (isInputClosed)
                    return IOStatus.EOF;

                n = IOUtil.read(fd, dsts, offset, length, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = IOUtil.read(fd, dsts, offset, length, nd);
                    }
                }
            } catch (ConnectionResetException e) {
                connectionReset = true;
                throwConnectionReset();
            } finally {
                endRead(blocking, n > 0);
                if (n <= 0 && isInputClosed)
                    return IOStatus.EOF;
            }
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Marks the beginning of a write operation that might block.
     *
     * @throws ClosedChannelException if blocking and the channel is closed
     */
    private void beginWrite(boolean blocking) throws ClosedChannelException {
        if (blocking) {
            // set hook for Thread.interrupt
            begin();

            synchronized (stateLock) {
                ensureOpen();
                if (isOutputClosed)
                    throw new ClosedChannelException();
                // record thread so it can be signalled if needed
                writerThread = NativeThread.current();
            }
        }
    }

    /**
     * Marks the end of a write operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     * thread being interrupted on a blocking write operation.
     */
    private void endWrite(boolean blocking, boolean completed)
        throws AsynchronousCloseException
    {
        if (blocking) {
            synchronized (stateLock) {
                writerThread = 0;
                if (state == ST_CLOSING) {
                    tryFinishClose();
                }
            }
            // remove hook for Thread.interrupt
            end(completed);
        }
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        Objects.requireNonNull(buf);
        writeLock.lock();
        try {
            ensureOpenAndConnected();
            boolean blocking = isBlocking();
            int n = 0;
            try {
                beginWrite(blocking);
                n = IOUtil.write(fd, buf, -1, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLOUT);
                        n = IOUtil.write(fd, buf, -1, nd);
                    }
                }
            } finally {
                endWrite(blocking, n > 0);
                if (n <= 0 && isOutputClosed)
                    throw new AsynchronousCloseException();
            }
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length)
        throws IOException
    {
        Objects.checkFromIndexSize(offset, length, srcs.length);

        writeLock.lock();
        try {
            ensureOpenAndConnected();
            boolean blocking = isBlocking();
            long n = 0;
            try {
                beginWrite(blocking);
                n = IOUtil.write(fd, srcs, offset, length, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLOUT);
                        n = IOUtil.write(fd, srcs, offset, length, nd);
                    }
                }
            } finally {
                endWrite(blocking, n > 0);
                if (n <= 0 && isOutputClosed)
                    throw new AsynchronousCloseException();
            }
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Writes a byte of out of band data.
     */
    int sendOutOfBandData(byte b) throws IOException {
        writeLock.lock();
        try {
            ensureOpenAndConnected();
            boolean blocking = isBlocking();
            int n = 0;
            try {
                beginWrite(blocking);
                if (blocking) {
                    do {
                        n = Net.sendOOB(fd, b);
                    } while (n == IOStatus.INTERRUPTED && isOpen());
                } else {
                    n = Net.sendOOB(fd, b);
                }
            } finally {
                endWrite(blocking, n > 0);
                if (n <= 0 && isOutputClosed)
                    throw new AsynchronousCloseException();
            }
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        readLock.lock();
        try {
            writeLock.lock();
            try {
                lockedConfigureBlocking(block);
            } finally {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Adjusts the blocking mode. readLock or writeLock must already be held.
     */
    private void lockedConfigureBlocking(boolean block) throws IOException {
        assert readLock.isHeldByCurrentThread() || writeLock.isHeldByCurrentThread();
        synchronized (stateLock) {
            ensureOpen();
            IOUtil.configureBlocking(fd, block);
        }
    }

    /**
     * Adjusts the blocking mode if the channel is open. readLock or writeLock
     * must already be held.
     *
     * @return {@code true} if the blocking mode was adjusted, {@code false} if
     *         the blocking mode was not adjusted because the channel is closed
     */
    private boolean tryLockedConfigureBlocking(boolean block) throws IOException {
        assert readLock.isHeldByCurrentThread() || writeLock.isHeldByCurrentThread();
        synchronized (stateLock) {
            if (isOpen()) {
                IOUtil.configureBlocking(fd, block);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Returns the local address, or null if not bound
     */
    SocketAddress localAddress() {
        synchronized (stateLock) {
            return localAddress;
        }
    }

    /**
     * Returns the remote address, or null if not connected
     */
    SocketAddress remoteAddress() {
        synchronized (stateLock) {
            return remoteAddress;
        }
    }

    @Override
    public SocketChannel bind(SocketAddress local) throws IOException {
        readLock.lock();
        try {
            writeLock.lock();
            try {
                synchronized (stateLock) {
                    ensureOpen();
                    if (state == ST_CONNECTIONPENDING)
                        throw new ConnectionPendingException();
                    if (localAddress != null)
                        throw new AlreadyBoundException();
                    if (isUnixSocket()) {
                        localAddress = unixBind(local);
                    } else {
                        localAddress = netBind(local);
                    }
                }
            } finally {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
        return this;
    }

    private SocketAddress unixBind(SocketAddress local) throws IOException {
        UnixDomainSockets.checkPermission();
        if (local == null) {
            return UnixDomainSockets.UNNAMED;
        } else {
            Path path = UnixDomainSockets.checkAddress(local).getPath();
            if (path.toString().isEmpty()) {
                return UnixDomainSockets.UNNAMED;
            } else {
                // bind to non-empty path
                UnixDomainSockets.bind(fd, path);
                return UnixDomainSockets.localAddress(fd);
            }
        }
    }

    private SocketAddress netBind(SocketAddress local) throws IOException {
        InetSocketAddress isa;
        if (local == null) {
            isa = new InetSocketAddress(Net.anyLocalAddress(family), 0);
        } else {
            isa = Net.checkAddress(local, family);
        }
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkListen(isa.getPort());
        }
        NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
        Net.bind(family, fd, isa.getAddress(), isa.getPort());
        return Net.localAddress(fd);
    }

    @Override
    public boolean isConnected() {
        return (state == ST_CONNECTED);
    }

    @Override
    public boolean isConnectionPending() {
        return (state == ST_CONNECTIONPENDING);
    }

    /**
     * Marks the beginning of a connect operation that might block.
     * @param blocking true if configured blocking
     * @param isa the remote address
     * @throws ClosedChannelException if the channel is closed
     * @throws AlreadyConnectedException if already connected
     * @throws ConnectionPendingException is a connection is pending
     * @throws IOException if the pre-connect hook fails
     */
    private void beginConnect(boolean blocking, SocketAddress sa)
        throws IOException
    {
        if (blocking) {
            // set hook for Thread.interrupt
            begin();
        }
        synchronized (stateLock) {
            ensureOpen();
            int state = this.state;
            if (state == ST_CONNECTED)
                throw new AlreadyConnectedException();
            if (state == ST_CONNECTIONPENDING)
                throw new ConnectionPendingException();
            assert state == ST_UNCONNECTED;
            this.state = ST_CONNECTIONPENDING;

            if (isNetSocket() && (localAddress == null)) {
                InetSocketAddress isa = (InetSocketAddress) sa;
                NetHooks.beforeTcpConnect(fd, isa.getAddress(), isa.getPort());
            }
            remoteAddress = sa;

            if (blocking) {
                // record thread so it can be signalled if needed
                readerThread = NativeThread.current();
            }
        }
    }

    /**
     * Marks the end of a connect operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     * thread being interrupted on a blocking connect operation.
     * @throws IOException if completed and unable to obtain the local address
     */
    private void endConnect(boolean blocking, boolean completed)
        throws IOException
    {
        endRead(blocking, completed);

        if (completed) {
            synchronized (stateLock) {
                if (state == ST_CONNECTIONPENDING) {
                    if (isUnixSocket()) {
                        localAddress = UnixDomainSockets.localAddress(fd);
                    } else {
                        localAddress = Net.localAddress(fd);
                    }
                    state = ST_CONNECTED;
                }
            }
        }
    }

    /**
     * Checks the remote address to which this channel is to be connected.
     */
    private SocketAddress checkRemote(SocketAddress sa) {
        if (isUnixSocket()) {
            UnixDomainSockets.checkPermission();
            return UnixDomainSockets.checkAddress(sa);
        } else {
            InetSocketAddress isa = Net.checkAddress(sa, family);
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
            }
            InetAddress address = isa.getAddress();
            if (address.isAnyLocalAddress()) {
                int port = isa.getPort();
                if (address instanceof Inet4Address) {
                    return new InetSocketAddress(Net.inet4LoopbackAddress(), port);
                } else {
                    assert family == INET6;
                    return new InetSocketAddress(Net.inet6LoopbackAddress(), port);
                }
            } else {
                return isa;
            }
        }
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        SocketAddress sa = checkRemote(remote);
        try {
            readLock.lock();
            try {
                writeLock.lock();
                try {
                    boolean blocking = isBlocking();
                    boolean connected = false;
                    try {
                        beginConnect(blocking, sa);
                        int n;
                        if (isUnixSocket()) {
                            n = UnixDomainSockets.connect(fd, sa);
                        } else {
                            n = Net.connect(family, fd, sa);
                        }
                        if (n > 0) {
                            connected = true;
                        } else if (blocking) {
                            assert IOStatus.okayToRetry(n);
                            boolean polled = false;
                            while (!polled && isOpen()) {
                                park(Net.POLLOUT);
                                polled = Net.pollConnectNow(fd);
                            }
                            connected = polled && isOpen();
                        }
                    } finally {
                        endConnect(blocking, connected);
                    }
                    return connected;
                } finally {
                    writeLock.unlock();
                }
            } finally {
                readLock.unlock();
            }
        } catch (IOException ioe) {
            // connect failed, close the channel
            close();
            throw SocketExceptions.of(ioe, sa);
        }
    }

    /**
     * Marks the beginning of a finishConnect operation that might block.
     *
     * @throws ClosedChannelException if the channel is closed
     * @throws NoConnectionPendingException if no connection is pending
     */
    private void beginFinishConnect(boolean blocking) throws ClosedChannelException {
        if (blocking) {
            // set hook for Thread.interrupt
            begin();
        }
        synchronized (stateLock) {
            ensureOpen();
            if (state != ST_CONNECTIONPENDING)
                throw new NoConnectionPendingException();
            if (blocking) {
                // record thread so it can be signalled if needed
                readerThread = NativeThread.current();
            }
        }
    }

    /**
     * Marks the end of a finishConnect operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     * thread being interrupted on a blocking connect operation.
     * @throws IOException if completed and unable to obtain the local address
     */
    private void endFinishConnect(boolean blocking, boolean completed)
        throws IOException
    {
        endRead(blocking, completed);

        if (completed) {
            synchronized (stateLock) {
                if (state == ST_CONNECTIONPENDING) {
                    if (isUnixSocket()) {
                        localAddress = UnixDomainSockets.localAddress(fd);
                    } else {
                        localAddress = Net.localAddress(fd);
                    }
                    state = ST_CONNECTED;
                }
            }
        }
    }

    @Override
    public boolean finishConnect() throws IOException {
        try {
            readLock.lock();
            try {
                writeLock.lock();
                try {
                    // no-op if already connected
                    if (isConnected())
                        return true;

                    boolean blocking = isBlocking();
                    boolean connected = false;
                    try {
                        beginFinishConnect(blocking);
                        boolean polled = Net.pollConnectNow(fd);
                        if (blocking) {
                            while (!polled && isOpen()) {
                                park(Net.POLLOUT);
                                polled = Net.pollConnectNow(fd);
                            }
                        }
                        connected = polled && isOpen();
                    } finally {
                        endFinishConnect(blocking, connected);
                    }
                    assert (blocking && connected) ^ !blocking;
                    return connected;
                } finally {
                    writeLock.unlock();
                }
            } finally {
                readLock.unlock();
            }
        } catch (IOException ioe) {
            // connect failed, close the channel
            close();
            throw SocketExceptions.of(ioe, remoteAddress);
        }
    }

    /**
     * Closes the socket if there are no I/O operations in progress and the
     * channel is not registered with a Selector.
     */
    private boolean tryClose() throws IOException {
        assert Thread.holdsLock(stateLock) && state == ST_CLOSING;
        if ((readerThread == 0) && (writerThread == 0) && !isRegistered()) {
            state = ST_CLOSED;
            nd.close(fd);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Invokes tryClose to attempt to close the socket.
     *
     * This method is used for deferred closing by I/O and Selector operations.
     */
    private void tryFinishClose() {
        try {
            tryClose();
        } catch (IOException ignore) { }
    }

    /**
     * Closes this channel when configured in blocking mode.
     *
     * If there is an I/O operation in progress then the socket is pre-closed
     * and the I/O threads signalled, in which case the final close is deferred
     * until all I/O operations complete.
     *
     * Note that a channel configured blocking may be registered with a Selector
     * This arises when a key is canceled and the channel configured to blocking
     * mode before the key is flushed from the Selector.
     */
    private void implCloseBlockingMode() throws IOException {
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            state = ST_CLOSING;
            if (!tryClose()) {
                long reader = readerThread;
                long writer = writerThread;
                if (reader != 0 || writer != 0) {
                    nd.preClose(fd);
                    if (reader != 0)
                        NativeThread.signal(reader);
                    if (writer != 0)
                        NativeThread.signal(writer);
                }
            }
        }
    }

    /**
     * Closes this channel when configured in non-blocking mode.
     *
     * If the channel is registered with a Selector then the close is deferred
     * until the channel is flushed from all Selectors.
     *
     * If the socket is connected and the channel is registered with a Selector
     * then the socket is shutdown for writing so that the peer reads EOF. In
     * addition, if SO_LINGER is set to a non-zero value then it is disabled so
     * that the deferred close does not wait.
     */
    private void implCloseNonBlockingMode() throws IOException {
        boolean connected;
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            connected = (state == ST_CONNECTED);
            state = ST_CLOSING;
        }

        // wait for any read/write operations to complete
        readLock.lock();
        readLock.unlock();
        writeLock.lock();
        writeLock.unlock();

        // if the socket cannot be closed because it's registered with a Selector
        // then shutdown the socket for writing.
        synchronized (stateLock) {
            if (state == ST_CLOSING && !tryClose() && connected && isRegistered()) {
                try {
                    SocketOption<Integer> opt = StandardSocketOptions.SO_LINGER;
                    int interval = (int) Net.getSocketOption(fd, Net.UNSPEC, opt);
                    if (interval != 0) {
                        if (interval > 0) {
                            // disable SO_LINGER
                            Net.setSocketOption(fd, Net.UNSPEC, opt, -1);
                        }
                        Net.shutdown(fd, Net.SHUT_WR);
                    }
                } catch (IOException ignore) { }
            }
        }
    }

    /**
     * Invoked by implCloseChannel to close the channel.
     */
    @Override
    protected void implCloseSelectableChannel() throws IOException {
        assert !isOpen();
        if (isBlocking()) {
            implCloseBlockingMode();
        } else {
            implCloseNonBlockingMode();
        }
    }

    @Override
    public void kill() {
        synchronized (stateLock) {
            if (state == ST_CLOSING) {
                tryFinishClose();
            }
        }
    }

    @Override
    public SocketChannel shutdownInput() throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (!isConnected())
                throw new NotYetConnectedException();
            if (!isInputClosed) {
                Net.shutdown(fd, Net.SHUT_RD);
                long thread = readerThread;
                if (thread != 0)
                    NativeThread.signal(thread);
                isInputClosed = true;
            }
            return this;
        }
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (!isConnected())
                throw new NotYetConnectedException();
            if (!isOutputClosed) {
                Net.shutdown(fd, Net.SHUT_WR);
                long thread = writerThread;
                if (thread != 0)
                    NativeThread.signal(thread);
                isOutputClosed = true;
            }
            return this;
        }
    }

    boolean isInputOpen() {
        return !isInputClosed;
    }

    boolean isOutputOpen() {
        return !isOutputClosed;
    }

    /**
     * Waits for a connection attempt to finish with a timeout
     * @throws SocketTimeoutException if the connect timeout elapses
     */
    private boolean finishTimedConnect(long nanos) throws IOException {
        long startNanos = System.nanoTime();
        boolean polled = Net.pollConnectNow(fd);
        while (!polled && isOpen()) {
            long remainingNanos = nanos - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                throw new SocketTimeoutException("Connect timed out");
            }
            park(Net.POLLOUT, remainingNanos);
            polled = Net.pollConnectNow(fd);
        }
        return polled && isOpen();
    }

    /**
     * Attempts to establish a connection to the given socket address with a
     * timeout. Closes the socket if connection cannot be established.
     *
     * @apiNote This method is for use by the socket adaptor.
     *
     * @throws IllegalBlockingModeException if the channel is non-blocking
     * @throws SocketTimeoutException if the read timeout elapses
     */
    void blockingConnect(SocketAddress remote, long nanos) throws IOException {
        SocketAddress sa = checkRemote(remote);
        try {
            readLock.lock();
            try {
                writeLock.lock();
                try {
                    if (!isBlocking())
                        throw new IllegalBlockingModeException();
                    boolean connected = false;
                    try {
                        beginConnect(true, sa);
                        // change socket to non-blocking
                        lockedConfigureBlocking(false);
                        try {
                            int n;
                            if (isUnixSocket()) {
                                n = UnixDomainSockets.connect(fd, sa);
                            } else {
                                n = Net.connect(family, fd, sa);
                            }
                            connected = (n > 0) ? true : finishTimedConnect(nanos);
                        } finally {
                            // restore socket to blocking mode (if channel is open)
                            tryLockedConfigureBlocking(true);
                        }
                    } finally {
                        endConnect(true, connected);
                    }
                } finally {
                    writeLock.unlock();
                }
            } finally {
                readLock.unlock();
            }
        } catch (IOException ioe) {
            // connect failed, close the channel
            close();
            throw SocketExceptions.of(ioe, sa);
        }
    }

    /**
     * Attempts to read bytes from the socket into the given byte array.
     */
    private int tryRead(byte[] b, int off, int len) throws IOException {
        ByteBuffer dst = Util.getTemporaryDirectBuffer(len);
        assert dst.position() == 0;
        try {
            int n = nd.read(fd, ((DirectBuffer)dst).address(), len);
            if (n > 0) {
                dst.get(b, off, n);
            }
            return n;
        } finally{
            Util.offerFirstTemporaryDirectBuffer(dst);
        }
    }

    /**
     * Reads bytes from the socket into the given byte array with a timeout.
     * @throws SocketTimeoutException if the read timeout elapses
     */
    private int timedRead(byte[] b, int off, int len, long nanos) throws IOException {
        long startNanos = System.nanoTime();
        int n = tryRead(b, off, len);
        while (n == IOStatus.UNAVAILABLE && isOpen()) {
            long remainingNanos = nanos - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                throw new SocketTimeoutException("Read timed out");
            }
            park(Net.POLLIN, remainingNanos);
            n = tryRead(b, off, len);
        }
        return n;
    }

    /**
     * Reads bytes from the socket into the given byte array.
     *
     * @apiNote This method is for use by the socket adaptor.
     *
     * @throws IllegalBlockingModeException if the channel is non-blocking
     * @throws SocketTimeoutException if the read timeout elapses
     */
    int blockingRead(byte[] b, int off, int len, long nanos) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            // nothing to do
            return 0;
        }

        readLock.lock();
        try {
            ensureOpenAndConnected();

            // check that channel is configured blocking
            if (!isBlocking())
                throw new IllegalBlockingModeException();

            int n = 0;
            try {
                beginRead(true);

                // check if connection has been reset
                if (connectionReset)
                    throwConnectionReset();

                // check if input is shutdown
                if (isInputClosed)
                    return IOStatus.EOF;

                if (nanos > 0) {
                    // change socket to non-blocking
                    lockedConfigureBlocking(false);
                    try {
                        n = timedRead(b, off, len, nanos);
                    } finally {
                        // restore socket to blocking mode (if channel is open)
                        tryLockedConfigureBlocking(true);
                    }
                } else {
                    // read, no timeout
                    n = tryRead(b, off, len);
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = tryRead(b, off, len);
                    }
                }
            } catch (ConnectionResetException e) {
                connectionReset = true;
                throwConnectionReset();
            } finally {
                endRead(true, n > 0);
                if (n <= 0 && isInputClosed)
                    return IOStatus.EOF;
            }
            assert n > 0 || n == -1;
            return n;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Attempts to write a sequence of bytes to the socket from the given
     * byte array.
     */
    private int tryWrite(byte[] b, int off, int len) throws IOException {
        ByteBuffer src = Util.getTemporaryDirectBuffer(len);
        assert src.position() == 0;
        try {
            src.put(b, off, len);
            return nd.write(fd, ((DirectBuffer)src).address(), len);
        } finally {
            Util.offerFirstTemporaryDirectBuffer(src);
        }
    }

    /**
     * Writes a sequence of bytes to the socket from the given byte array.
     *
     * @apiNote This method is for use by the socket adaptor.
     */
    void blockingWriteFully(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            // nothing to do
            return;
        }

        writeLock.lock();
        try {
            ensureOpenAndConnected();

            // check that channel is configured blocking
            if (!isBlocking())
                throw new IllegalBlockingModeException();

            // loop until all bytes have been written
            int pos = off;
            int end = off + len;
            try {
                beginWrite(true);
                while (pos < end && isOpen()) {
                    int size = end - pos;
                    int n = tryWrite(b, pos, size);
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLOUT);
                        n = tryWrite(b, pos, size);
                    }
                    if (n > 0) {
                        pos += n;
                    }
                }
            } finally {
                endWrite(true, pos >= end);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Return the number of bytes in the socket input buffer.
     */
    int available() throws IOException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            if (isInputClosed) {
                return 0;
            } else {
                return Net.available(fd);
            }
        }
    }

    /**
     * Translates native poll revent ops into a ready operation ops
     */
    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl ski) {
        int intOps = ski.nioInterestOps();
        int oldOps = ski.nioReadyOps();
        int newOps = initialOps;

        if ((ops & Net.POLLNVAL) != 0) {
            // This should only happen if this channel is pre-closed while a
            // selection operation is in progress
            // ## Throw an error if this channel has not been pre-closed
            return false;
        }

        if ((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
            newOps = intOps;
            ski.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }

        boolean connected = isConnected();
        if (((ops & Net.POLLIN) != 0) &&
            ((intOps & SelectionKey.OP_READ) != 0) && connected)
            newOps |= SelectionKey.OP_READ;

        if (((ops & Net.POLLCONN) != 0) &&
            ((intOps & SelectionKey.OP_CONNECT) != 0) && isConnectionPending())
            newOps |= SelectionKey.OP_CONNECT;

        if (((ops & Net.POLLOUT) != 0) &&
            ((intOps & SelectionKey.OP_WRITE) != 0) && connected)
            newOps |= SelectionKey.OP_WRITE;

        ski.nioReadyOps(newOps);
        return (newOps & ~oldOps) != 0;
    }

    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, ski.nioReadyOps(), ski);
    }

    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, 0, ski);
    }

    /**
     * Translates an interest operation set into a native poll event set
     */
    public int translateInterestOps(int ops) {
        int newOps = 0;
        if ((ops & SelectionKey.OP_READ) != 0)
            newOps |= Net.POLLIN;
        if ((ops & SelectionKey.OP_WRITE) != 0)
            newOps |= Net.POLLOUT;
        if ((ops & SelectionKey.OP_CONNECT) != 0)
            newOps |= Net.POLLCONN;
        return newOps;
    }

    public FileDescriptor getFD() {
        return fd;
    }

    public int getFDVal() {
        return fdVal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSuperclass().getName());
        sb.append('[');
        if (!isOpen())
            sb.append("closed");
        else {
            synchronized (stateLock) {
                switch (state) {
                case ST_UNCONNECTED:
                    sb.append("unconnected");
                    break;
                case ST_CONNECTIONPENDING:
                    sb.append("connection-pending");
                    break;
                case ST_CONNECTED:
                    sb.append("connected");
                    if (isInputClosed)
                        sb.append(" ishut");
                    if (isOutputClosed)
                        sb.append(" oshut");
                    break;
                }
                SocketAddress addr = localAddress();
                if (addr != null) {
                    sb.append(" local=");
                    if (isUnixSocket()) {
                        sb.append(UnixDomainSockets.getRevealedLocalAddressAsString(addr));
                    } else {
                        sb.append(Net.getRevealedLocalAddressAsString(addr));
                    }
                }
                if (remoteAddress() != null) {
                    sb.append(" remote=");
                    sb.append(remoteAddress().toString());
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
