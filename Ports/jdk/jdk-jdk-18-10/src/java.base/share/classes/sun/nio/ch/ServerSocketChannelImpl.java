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
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
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

import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;

/**
 * An implementation of ServerSocketChannels
 */

class ServerSocketChannelImpl
    extends ServerSocketChannel
    implements SelChImpl
{
    // Used to make native close and configure calls
    private static final NativeDispatcher nd = new SocketDispatcher();

    // The protocol family of the socket
    private final ProtocolFamily family;

    // Our file descriptor
    private final FileDescriptor fd;
    private final int fdVal;

    // Lock held by thread currently blocked on this channel
    private final ReentrantLock acceptLock = new ReentrantLock();

    // Lock held by any thread that modifies the state fields declared below
    // DO NOT invoke a blocking I/O operation while holding this lock!
    private final Object stateLock = new Object();

    // -- The following fields are protected by stateLock

    // Channel state, increases monotonically
    private static final int ST_INUSE = 0;
    private static final int ST_CLOSING = 1;
    private static final int ST_CLOSED = 2;
    private int state;

    // ID of native thread currently blocked in this channel, for signalling
    private long thread;

    // Binding
    private SocketAddress localAddress; // null => unbound

    // set true when exclusive binding is on and SO_REUSEADDR is emulated
    private boolean isReuseAddress;

    // Our socket adaptor, if any
    private ServerSocket socket;

    // -- End of fields protected by stateLock

    ServerSocketChannelImpl(SelectorProvider sp) throws IOException {
        this(sp, Net.isIPv6Available() ? INET6 : INET);
    }

    ServerSocketChannelImpl(SelectorProvider sp, ProtocolFamily family)
        throws IOException
    {
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
            this.fd = Net.serverSocket(family, true);
        }
        this.fdVal = IOUtil.fdVal(fd);
    }

    ServerSocketChannelImpl(SelectorProvider sp,
                            ProtocolFamily family,
                            FileDescriptor fd,
                            boolean bound)
        throws IOException
    {
        super(sp);

        if (family == UNIX) {
            this.family = UNIX;
        } else {
            this.family = Net.isIPv6Available() ? INET6 : INET;
        }
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);

        if (bound) {
            synchronized (stateLock) {
                if (family == UNIX) {
                    localAddress = UnixDomainSockets.localAddress(fd);
                } else {
                    localAddress = Net.localAddress(fd);
                }
            }
        }
    }

    /**
     * Returns true if this channel is to a INET or INET6 socket.
     */
    private boolean isNetSocket() {
        return (family == INET) || (family == INET6);
    }

    /**
     * Returns true if this channel is to a UNIX socket.
     */
    boolean isUnixSocket() {
        return (family == UNIX);
    }

    // @throws ClosedChannelException if channel is closed
    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen())
            throw new ClosedChannelException();
    }

    @Override
    public ServerSocket socket() {
        synchronized (stateLock) {
            if (socket == null) {
                if (isNetSocket()) {
                    socket = ServerSocketAdaptor.create(this);
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
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value)
        throws IOException
    {
        Objects.requireNonNull(name);
        if (!supportedOptions().contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");
        if (!name.type().isInstance(value))
            throw new IllegalArgumentException("Invalid value '" + value + "'");

        synchronized (stateLock) {
            ensureOpen();
            if (isNetSocket()
                    && name == StandardSocketOptions.SO_REUSEADDR
                    && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                isReuseAddress = (Boolean) value;
            } else {
                // no options that require special handling
                Net.setSocketOption(fd, Net.UNSPEC, name, value);
            }
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
            if (isNetSocket()
                    && name == StandardSocketOptions.SO_REUSEADDR
                    && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                return (T) Boolean.valueOf(isReuseAddress);
            } else {
                // no options that require special handling
                return (T) Net.getSocketOption(fd, Net.UNSPEC, name);
            }
        }
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultInetOptions = defaultInetOptions();
        static final Set<SocketOption<?>> defaultUnixDomainOptions = defaultUnixDomainOptions();

        private static Set<SocketOption<?>> defaultInetOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if (Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.addAll(ExtendedSocketOptions.serverSocketOptions());
            return Collections.unmodifiableSet(set);
        }

        private static Set<SocketOption<?>> defaultUnixDomainOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_RCVBUF);
            return Collections.unmodifiableSet(set);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        if (isUnixSocket()) {
            return DefaultOptionsHolder.defaultUnixDomainOptions;
        } else {
            return DefaultOptionsHolder.defaultInetOptions;
        }
    }

    @Override
    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (localAddress != null)
                throw new AlreadyBoundException();
            if (isUnixSocket()) {
                localAddress = unixBind(local, backlog);
            } else {
                localAddress = netBind(local, backlog);
            }
        }
        return this;
    }

    private SocketAddress unixBind(SocketAddress local, int backlog) throws IOException {
        UnixDomainSockets.checkPermission();
        if (local == null) {
            // Attempt up to 10 times to find an unused name in temp directory.
            // If local address supplied then bind called only once
            boolean bound = false;
            int attempts = 0;
            while (attempts < 10 && !bound) {
                try {
                    Path path = UnixDomainSockets.generateTempName().getPath();
                    UnixDomainSockets.bind(fd, path);
                    bound = true;
                } catch (BindException e) { }
                attempts++;
            }
            if (!bound)
                throw new BindException("Could not bind to temporary name");
        } else {
            Path path = UnixDomainSockets.checkAddress(local).getPath();
            UnixDomainSockets.bind(fd, path);
        }
        Net.listen(fd, backlog < 1 ? 50 : backlog);
        return UnixDomainSockets.localAddress(fd);
    }

    private SocketAddress netBind(SocketAddress local, int backlog) throws IOException {
        InetSocketAddress isa;
        if (local == null) {
            isa = new InetSocketAddress(Net.anyLocalAddress(family), 0);
        } else {
            isa = Net.checkAddress(local, family);
        }
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkListen(isa.getPort());
        NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
        Net.bind(family, fd, isa.getAddress(), isa.getPort());
        Net.listen(fd, backlog < 1 ? 50 : backlog);
        return Net.localAddress(fd);
    }

    /**
     * Marks the beginning of an I/O operation that might block.
     *
     * @throws ClosedChannelException if the channel is closed
     * @throws NotYetBoundException if the channel's socket has not been bound yet
     */
    private void begin(boolean blocking) throws ClosedChannelException {
        if (blocking)
            begin();  // set blocker to close channel if interrupted
        synchronized (stateLock) {
            ensureOpen();
            if (localAddress == null)
                throw new NotYetBoundException();
            if (blocking)
                thread = NativeThread.current();
        }
    }

    /**
     * Marks the end of an I/O operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     * thread being interrupted on a blocking I/O operation.
     */
    private void end(boolean blocking, boolean completed)
        throws AsynchronousCloseException
    {
        if (blocking) {
            synchronized (stateLock) {
                thread = 0;
                if (state == ST_CLOSING) {
                    tryFinishClose();
                }
            }
            end(completed);
        }
    }

    @Override
    public SocketChannel accept() throws IOException {
        int n = 0;
        FileDescriptor newfd = new FileDescriptor();
        SocketAddress[] saa = new SocketAddress[1];

        acceptLock.lock();
        try {
            boolean blocking = isBlocking();
            try {
                begin(blocking);
                n = implAccept(this.fd, newfd, saa);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = implAccept(this.fd, newfd, saa);
                    }
                }
            } finally {
                end(blocking, n > 0);
                assert IOStatus.check(n);
            }
        } finally {
            acceptLock.unlock();
        }

        if (n > 0) {
            return finishAccept(newfd, saa[0]);
        } else {
            return null;
        }
    }

    private int implAccept(FileDescriptor fd, FileDescriptor newfd, SocketAddress[] saa)
        throws IOException
    {
        if (isUnixSocket()) {
            UnixDomainSockets.checkPermission();
            String[] pa = new String[1];
            int n = UnixDomainSockets.accept(fd, newfd, pa);
            if (n > 0)
                saa[0] = UnixDomainSocketAddress.of(pa[0]);
            return n;
        } else {
            InetSocketAddress[] issa = new InetSocketAddress[1];
            int n = Net.accept(fd, newfd, issa);
            if (n > 0)
                saa[0] = issa[0];
            return n;
        }
    }

    /**
     * Accepts a new connection with a given timeout. This method requires the
     * channel to be configured in blocking mode.
     *
     * @apiNote This method is for use by the socket adaptor.
     *
     * @param nanos the timeout, in nanoseconds
     * @throws IllegalBlockingModeException if the channel is configured non-blocking
     * @throws SocketTimeoutException if the timeout expires
     */
    SocketChannel blockingAccept(long nanos) throws IOException {
        int n = 0;
        FileDescriptor newfd = new FileDescriptor();
        SocketAddress[] saa = new SocketAddress[1];

        acceptLock.lock();
        try {
            // check that channel is configured blocking
            if (!isBlocking())
                throw new IllegalBlockingModeException();

            try {
                begin(true);
                // change socket to non-blocking
                lockedConfigureBlocking(false);
                try {
                    long startNanos = System.nanoTime();
                    n = implAccept(fd, newfd, saa);
                    while (n == IOStatus.UNAVAILABLE && isOpen()) {
                        long remainingNanos = nanos - (System.nanoTime() - startNanos);
                        if (remainingNanos <= 0) {
                            throw new SocketTimeoutException("Accept timed out");
                        }
                        park(Net.POLLIN, remainingNanos);
                        n = implAccept(fd, newfd, saa);
                    }
                } finally {
                    // restore socket to blocking mode (if channel is open)
                    tryLockedConfigureBlocking(true);
                }
            } finally {
                end(true, n > 0);
            }
        } finally {
            acceptLock.unlock();
        }

        assert n > 0;
        return finishAccept(newfd, saa[0]);
    }

    private SocketChannel finishAccept(FileDescriptor newfd, SocketAddress sa)
        throws IOException
    {
        try {
            // newly accepted socket is initially in blocking mode
            IOUtil.configureBlocking(newfd, true);

            // check permitted to accept connections from the remote address
            if (isNetSocket()) {
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    InetSocketAddress isa = (InetSocketAddress) sa;
                    sm.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
                }
            }
            return new SocketChannelImpl(provider(), family, newfd, sa);
        } catch (Exception e) {
            nd.close(newfd);
            throw e;
        }
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        acceptLock.lock();
        try {
            lockedConfigureBlocking(block);
        } finally {
            acceptLock.unlock();
        }
    }

    /**
     * Adjust the blocking. acceptLock must already be held.
     */
    private void lockedConfigureBlocking(boolean block) throws IOException {
        assert acceptLock.isHeldByCurrentThread();
        synchronized (stateLock) {
            ensureOpen();
            IOUtil.configureBlocking(fd, block);
        }
    }

    /**
     * Adjusts the blocking mode if the channel is open. acceptLock must already
     * be held.
     *
     * @return {@code true} if the blocking mode was adjusted, {@code false} if
     *         the blocking mode was not adjusted because the channel is closed
     */
    private boolean tryLockedConfigureBlocking(boolean block) throws IOException {
        assert acceptLock.isHeldByCurrentThread();
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
     * Closes the socket if there are no accept in progress and the channel is
     * not registered with a Selector.
     */
    private boolean tryClose() throws IOException {
        assert Thread.holdsLock(stateLock) && state == ST_CLOSING;
        if ((thread == 0) && !isRegistered()) {
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
     * If there is an accept in progress then the socket is pre-closed and the
     * accept thread is signalled, in which case the final close is deferred
     * until the accept aborts.
     */
    private void implCloseBlockingMode() throws IOException {
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            state = ST_CLOSING;
            if (!tryClose()) {
                long th = thread;
                if (th != 0) {
                    nd.preClose(fd);
                    NativeThread.signal(th);
                }
            }
        }
    }

    /**
     * Closes this channel when configured in non-blocking mode.
     *
     * If the channel is registered with a Selector then the close is deferred
     * until the channel is flushed from all Selectors.
     */
    private void implCloseNonBlockingMode() throws IOException {
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            state = ST_CLOSING;
        }
        // wait for any accept to complete before trying to close
        acceptLock.lock();
        acceptLock.unlock();
        synchronized (stateLock) {
            if (state == ST_CLOSING) {
                tryClose();
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

    /**
     * Returns true if channel's socket is bound
     */
    boolean isBound() {
        synchronized (stateLock) {
            return localAddress != null;
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
     * Translates native poll revent set into a ready operation set
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

        if (((ops & Net.POLLIN) != 0) &&
            ((intOps & SelectionKey.OP_ACCEPT) != 0))
                newOps |= SelectionKey.OP_ACCEPT;

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
        if ((ops & SelectionKey.OP_ACCEPT) != 0)
            newOps |= Net.POLLIN;
        return newOps;
    }

    public FileDescriptor getFD() {
        return fd;
    }

    public int getFDVal() {
        return fdVal;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append('[');
        if (!isOpen()) {
            sb.append("closed");
        } else {
            synchronized (stateLock) {
                SocketAddress addr = localAddress;
                if (addr == null) {
                    sb.append("unbound");
                } else if (isUnixSocket()) {
                    sb.append(UnixDomainSockets.getRevealedLocalAddressAsString(addr));
                } else {
                    sb.append(Net.getRevealedLocalAddressAsString(addr));
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
