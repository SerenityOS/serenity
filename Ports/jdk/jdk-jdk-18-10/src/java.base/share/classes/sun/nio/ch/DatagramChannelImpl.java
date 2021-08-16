/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.MembershipKey;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import jdk.internal.ref.CleanerFactory;
import sun.net.ResourceManager;
import sun.net.ext.ExtendedSocketOptions;
import sun.net.util.IPAddressUtil;

/**
 * An implementation of DatagramChannels.
 */

class DatagramChannelImpl
    extends DatagramChannel
    implements SelChImpl
{
    // Used to make native read and write calls
    private static final NativeDispatcher nd = new DatagramDispatcher();

    // true if interruptible (can be false to emulate legacy DatagramSocket)
    private final boolean interruptible;

    // The protocol family of the socket
    private final ProtocolFamily family;

    // Our file descriptor
    private final FileDescriptor fd;
    private final int fdVal;

    // Native sockaddrs and cached InetSocketAddress for receive, protected by readLock
    private NativeSocketAddress sourceSockAddr;
    private NativeSocketAddress cachedSockAddr;
    private InetSocketAddress cachedInetSocketAddress;

    // Native sockaddr and cached objects for send, protected by writeLock
    private final NativeSocketAddress targetSockAddr;
    private InetSocketAddress previousTarget;
    private int previousSockAddrLength;

    // Cleaner to close file descriptor and free native socket address
    private final Cleanable cleaner;

    // Lock held by current reading or connecting thread
    private final ReentrantLock readLock = new ReentrantLock();

    // Lock held by current writing or connecting thread
    private final ReentrantLock writeLock = new ReentrantLock();

    // Lock held by any thread that modifies the state fields declared below
    // DO NOT invoke a blocking I/O operation while holding this lock!
    private final Object stateLock = new Object();

    // -- The following fields are protected by stateLock

    // State (does not necessarily increase monotonically)
    private static final int ST_UNCONNECTED = 0;
    private static final int ST_CONNECTED = 1;
    private static final int ST_CLOSING = 2;
    private static final int ST_CLOSED = 3;
    private int state;

    // IDs of native threads doing reads and writes, for signalling
    private long readerThread;
    private long writerThread;

    // Local and remote (connected) address
    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;

    // Local address prior to connecting
    private InetSocketAddress initialLocalAddress;

    // Socket adaptor, created lazily
    private static final VarHandle SOCKET;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            SOCKET = l.findVarHandle(DatagramChannelImpl.class, "socket", DatagramSocket.class);
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }
    private volatile DatagramSocket socket;

    // Multicast support
    private MembershipRegistry registry;

    // set true when socket is bound and SO_REUSEADDRESS is emulated
    private boolean reuseAddressEmulated;

    // set true/false when socket is already bound and SO_REUSEADDR is emulated
    private boolean isReuseAddress;

    // -- End of fields protected by stateLock


    DatagramChannelImpl(SelectorProvider sp, boolean interruptible) throws IOException {
        this(sp, (Net.isIPv6Available()
                ? StandardProtocolFamily.INET6
                : StandardProtocolFamily.INET),
                interruptible);
    }

    DatagramChannelImpl(SelectorProvider sp, ProtocolFamily family, boolean interruptible)
        throws IOException
    {
        super(sp);

        Objects.requireNonNull(family, "'family' is null");
        if ((family != StandardProtocolFamily.INET) &&
                (family != StandardProtocolFamily.INET6)) {
            throw new UnsupportedOperationException("Protocol family not supported");
        }
        if (family == StandardProtocolFamily.INET6 && !Net.isIPv6Available()) {
            throw new UnsupportedOperationException("IPv6 not available");
        }

        FileDescriptor fd = null;
        NativeSocketAddress[] sockAddrs = null;

        ResourceManager.beforeUdpCreate();
        boolean initialized = false;
        try {
            this.interruptible = interruptible;
            this.family = family;
            this.fd = fd = Net.socket(family, false);
            this.fdVal = IOUtil.fdVal(fd);

            sockAddrs = NativeSocketAddress.allocate(3);
            readLock.lock();
            try {
                this.sourceSockAddr = sockAddrs[0];
                this.cachedSockAddr = sockAddrs[1];
            } finally {
                readLock.unlock();
            }
            this.targetSockAddr = sockAddrs[2];

            initialized = true;
        } finally {
            if (!initialized) {
                if (sockAddrs != null) NativeSocketAddress.freeAll(sockAddrs);
                if (fd != null) nd.close(fd);
                ResourceManager.afterUdpClose();
            }
        }

        Runnable releaser = releaserFor(fd, sockAddrs);
        this.cleaner = CleanerFactory.cleaner().register(this, releaser);
    }

    DatagramChannelImpl(SelectorProvider sp, FileDescriptor fd)
        throws IOException
    {
        super(sp);

        NativeSocketAddress[] sockAddrs = null;

        ResourceManager.beforeUdpCreate();
        boolean initialized = false;
        try {
            this.interruptible = true;
            this.family = Net.isIPv6Available()
                    ? StandardProtocolFamily.INET6
                    : StandardProtocolFamily.INET;
            this.fd = fd;
            this.fdVal = IOUtil.fdVal(fd);

            sockAddrs = NativeSocketAddress.allocate(3);
            readLock.lock();
            try {
                this.sourceSockAddr = sockAddrs[0];
                this.cachedSockAddr = sockAddrs[1];
            } finally {
                readLock.unlock();
            }
            this.targetSockAddr = sockAddrs[2];

            initialized = true;
        } finally {
            if (!initialized) {
                if (sockAddrs != null) NativeSocketAddress.freeAll(sockAddrs);
                nd.close(fd);
                ResourceManager.afterUdpClose();
            }
        }

        Runnable releaser = releaserFor(fd, sockAddrs);
        this.cleaner = CleanerFactory.cleaner().register(this, releaser);

        synchronized (stateLock) {
            this.localAddress = Net.localAddress(fd);
        }
    }

    // @throws ClosedChannelException if channel is closed
    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen())
            throw new ClosedChannelException();
    }

    @Override
    public DatagramSocket socket() {
        DatagramSocket socket = this.socket;
        if (socket == null) {
            socket = DatagramSocketAdaptor.create(this);
            if (!SOCKET.compareAndSet(this, null, socket)) {
                socket = this.socket;
            }
        }
        return socket;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            // Perform security check before returning address
            return Net.getRevealedLocalAddress(localAddress);
        }
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            return remoteAddress;
        }
    }

    /**
     * Returns the protocol family to specify to set/getSocketOption for the
     * given socket option.
     */
    private ProtocolFamily familyFor(SocketOption<?> name) {
        assert Thread.holdsLock(stateLock);

        // unspecified (most options)
        if (SocketOptionRegistry.findOption(name, Net.UNSPEC) != null)
            return Net.UNSPEC;

        // IPv4 socket
        if (family == StandardProtocolFamily.INET)
            return StandardProtocolFamily.INET;

        // IPv6 socket that is unbound
        if (localAddress == null)
            return StandardProtocolFamily.INET6;

        // IPv6 socket bound to wildcard or IPv6 address
        InetAddress address = localAddress.getAddress();
        if (address.isAnyLocalAddress() || (address instanceof Inet6Address))
            return StandardProtocolFamily.INET6;

        // IPv6 socket bound to IPv4 address
        if (Net.canUseIPv6OptionsWithIPv4LocalAddress()) {
            // IPV6_XXX options can be used
            return StandardProtocolFamily.INET6;
        } else {
            // IPV6_XXX options cannot be used
            return StandardProtocolFamily.INET;
        }
    }

    @Override
    public <T> DatagramChannel setOption(SocketOption<T> name, T value)
        throws IOException
    {
        Objects.requireNonNull(name);
        if (!supportedOptions().contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");
        if (!name.type().isInstance(value))
            throw new IllegalArgumentException("Invalid value '" + value + "'");

        synchronized (stateLock) {
            ensureOpen();

            ProtocolFamily family = familyFor(name);

            // Some platforms require both IPV6_XXX and IP_XXX socket options to
            // be set when the channel's socket is IPv6 and it is used to send
            // IPv4 multicast datagrams. The IP_XXX socket options are set on a
            // best effort basis.
            boolean needToSetIPv4Option = (family != Net.UNSPEC)
                    && (this.family == StandardProtocolFamily.INET6)
                    && Net.shouldSetBothIPv4AndIPv6Options();

            // outgoing multicast interface
            if (name == StandardSocketOptions.IP_MULTICAST_IF) {
                assert family != Net.UNSPEC;
                NetworkInterface interf = (NetworkInterface) value;
                if (family == StandardProtocolFamily.INET6) {
                    int index = interf.getIndex();
                    if (index == -1)
                        throw new IOException("Network interface cannot be identified");
                    Net.setInterface6(fd, index);
                }
                if (family == StandardProtocolFamily.INET || needToSetIPv4Option) {
                    // need IPv4 address to identify interface
                    Inet4Address target = Net.anyInet4Address(interf);
                    if (target != null) {
                        try {
                            Net.setInterface4(fd, Net.inet4AsInt(target));
                        } catch (IOException ioe) {
                            if (family == StandardProtocolFamily.INET) throw ioe;
                        }
                    } else if (family == StandardProtocolFamily.INET) {
                        throw new IOException("Network interface not configured for IPv4");
                    }
                }
                return this;
            }

            // SO_REUSEADDR needs special handling as it may be emulated
            if (name == StandardSocketOptions.SO_REUSEADDR
                && Net.useExclusiveBind() && localAddress != null) {
                reuseAddressEmulated = true;
                this.isReuseAddress = (Boolean)value;
            }

            // remaining options don't need any special handling
            Net.setSocketOption(fd, family, name, value);
            if (needToSetIPv4Option && family != StandardProtocolFamily.INET) {
                try {
                    Net.setSocketOption(fd, StandardProtocolFamily.INET, name, value);
                } catch (IOException ignore) { }
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

            ProtocolFamily family = familyFor(name);

            if (name == StandardSocketOptions.IP_MULTICAST_IF) {
                if (family == StandardProtocolFamily.INET) {
                    int address = Net.getInterface4(fd);
                    if (address == 0)
                        return null;    // default interface

                    InetAddress ia = Net.inet4FromInt(address);
                    NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
                    if (ni == null)
                        throw new IOException("Unable to map address to interface");
                    return (T) ni;
                } else {
                    int index = Net.getInterface6(fd);
                    if (index == 0)
                        return null;    // default interface

                    NetworkInterface ni = NetworkInterface.getByIndex(index);
                    if (ni == null)
                        throw new IOException("Unable to map index to interface");
                    return (T) ni;
                }
            }

            if (name == StandardSocketOptions.SO_REUSEADDR && reuseAddressEmulated) {
                return (T) Boolean.valueOf(isReuseAddress);
            }

            // no special handling
            return (T) Net.getSocketOption(fd, family, name);
        }
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if (Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.add(StandardSocketOptions.SO_BROADCAST);
            set.add(StandardSocketOptions.IP_TOS);
            set.add(StandardSocketOptions.IP_MULTICAST_IF);
            set.add(StandardSocketOptions.IP_MULTICAST_TTL);
            set.add(StandardSocketOptions.IP_MULTICAST_LOOP);
            set.addAll(ExtendedSocketOptions.datagramSocketOptions());
            return Collections.unmodifiableSet(set);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    /**
     * Marks the beginning of a read operation that might block.
     *
     * @param blocking true if configured blocking
     * @param mustBeConnected true if the socket must be connected
     * @return remote address if connected
     * @throws ClosedChannelException if the channel is closed
     * @throws NotYetConnectedException if mustBeConnected and not connected
     * @throws IOException if socket not bound and cannot be bound
     */
    private SocketAddress beginRead(boolean blocking, boolean mustBeConnected)
        throws IOException
    {
        if (blocking && interruptible) {
            // set hook for Thread.interrupt
            begin();
        }
        SocketAddress remote;
        synchronized (stateLock) {
            ensureOpen();
            remote = remoteAddress;
            if ((remote == null) && mustBeConnected)
                throw new NotYetConnectedException();
            if (localAddress == null)
                bindInternal(null);
            if (blocking)
                readerThread = NativeThread.current();
        }
        return remote;
    }

    /**
     * Marks the end of a read operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed asynchronously
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
            if (interruptible) {
                // remove hook for Thread.interrupt (may throw AsynchronousCloseException)
                end(completed);
            } else if (!completed && !isOpen()) {
                throw new AsynchronousCloseException();
            }
        }
    }

    @Override
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        readLock.lock();
        try {
            boolean blocking = isBlocking();
            SocketAddress sender = null;
            try {
                SocketAddress remote = beginRead(blocking, false);
                boolean connected = (remote != null);
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (connected || (sm == null)) {
                    // connected or no security manager
                    int n = receive(dst, connected);
                    if (blocking) {
                        while (IOStatus.okayToRetry(n) && isOpen()) {
                            park(Net.POLLIN);
                            n = receive(dst, connected);
                        }
                    }
                    if (n >= 0) {
                        // sender address is in socket address buffer
                        sender = sourceSocketAddress();
                    }
                } else {
                    // security manager and unconnected
                    sender = untrustedReceive(dst);
                }
                return sender;
            } finally {
                endRead(blocking, (sender != null));
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Receives a datagram into an untrusted buffer. When there is a security
     * manager set, and the socket is not connected, datagrams have to be received
     * into a buffer that is not accessible to the user. The datagram is copied
     * into the user's buffer when the sender address is accepted by the security
     * manager.
     */
    private SocketAddress untrustedReceive(ByteBuffer dst) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        assert readLock.isHeldByCurrentThread()
                && sm != null && remoteAddress == null;

        ByteBuffer bb = Util.getTemporaryDirectBuffer(dst.remaining());
        try {
            boolean blocking = isBlocking();
            for (;;) {
                int n = receive(bb, false);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = receive(bb, false);
                    }
                }
                if (n >= 0) {
                    // sender address is in socket address buffer
                    InetSocketAddress isa = sourceSocketAddress();
                    try {
                        sm.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
                        bb.flip();
                        dst.put(bb);
                        return isa;
                    } catch (SecurityException se) {
                        // ignore datagram
                        bb.clear();
                    }
                } else {
                    return null;
                }
            }
        } finally {
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }

    /**
     * Receives a datagram into the given buffer.
     *
     * @apiNote This method is for use by the socket adaptor. The buffer is
     * assumed to be trusted, meaning it is not accessible to user code.
     *
     * @throws IllegalBlockingModeException if the channel is non-blocking
     * @throws SocketTimeoutException if the timeout elapses
     */
    SocketAddress blockingReceive(ByteBuffer dst, long nanos) throws IOException {
        readLock.lock();
        try {
            ensureOpen();
            if (!isBlocking())
                throw new IllegalBlockingModeException();
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            boolean connected = isConnected();
            SocketAddress sender;
            do {
                if (nanos > 0) {
                    sender = trustedBlockingReceive(dst, nanos);
                } else {
                    sender = trustedBlockingReceive(dst);
                }
                // check sender when security manager set and not connected
                if (sm != null && !connected) {
                    InetSocketAddress isa = (InetSocketAddress) sender;
                    try {
                        sm.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
                    } catch (SecurityException e) {
                        sender = null;
                    }
                }
            } while (sender == null);
            return sender;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Receives a datagram into given buffer. This method is used to support
     * the socket adaptor. The buffer is assumed to be trusted.
     * @throws SocketTimeoutException if the timeout elapses
     */
    private SocketAddress trustedBlockingReceive(ByteBuffer dst)
        throws IOException
    {
        assert readLock.isHeldByCurrentThread() && isBlocking();
        SocketAddress sender = null;
        try {
            SocketAddress remote = beginRead(true, false);
            boolean connected = (remote != null);
            int n = receive(dst, connected);
            while (IOStatus.okayToRetry(n) && isOpen()) {
                park(Net.POLLIN);
                n = receive(dst, connected);
            }
            if (n >= 0) {
                // sender address is in socket address buffer
                sender = sourceSocketAddress();
            }
            return sender;
        } finally {
            endRead(true, (sender != null));
        }
    }

    /**
     * Receives a datagram into given buffer with a timeout. This method is
     * used to support the socket adaptor. The buffer is assumed to be trusted.
     * @throws SocketTimeoutException if the timeout elapses
     */
    private SocketAddress trustedBlockingReceive(ByteBuffer dst, long nanos)
        throws IOException
    {
        assert readLock.isHeldByCurrentThread() && isBlocking();
        SocketAddress sender = null;
        try {
            SocketAddress remote = beginRead(true, false);
            boolean connected = (remote != null);

            // change socket to non-blocking
            lockedConfigureBlocking(false);
            try {
                long startNanos = System.nanoTime();
                int n = receive(dst, connected);
                while (n == IOStatus.UNAVAILABLE && isOpen()) {
                    long remainingNanos = nanos - (System.nanoTime() - startNanos);
                    if (remainingNanos <= 0) {
                        throw new SocketTimeoutException("Receive timed out");
                    }
                    park(Net.POLLIN, remainingNanos);
                    n = receive(dst, connected);
                }
                if (n >= 0) {
                    // sender address is in socket address buffer
                    sender = sourceSocketAddress();
                }
                return sender;
            } finally {
                // restore socket to blocking mode (if channel is open)
                tryLockedConfigureBlocking(true);
            }
        } finally {
            endRead(true, (sender != null));
        }
    }

    private int receive(ByteBuffer dst, boolean connected) throws IOException {
        int pos = dst.position();
        int lim = dst.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        if (dst instanceof DirectBuffer && rem > 0)
            return receiveIntoNativeBuffer(dst, rem, pos, connected);

        // Substitute a native buffer. If the supplied buffer is empty
        // we must instead use a nonempty buffer, otherwise the call
        // will not block waiting for a datagram on some platforms.
        int newSize = Math.max(rem, 1);
        ByteBuffer bb = Util.getTemporaryDirectBuffer(newSize);
        try {
            int n = receiveIntoNativeBuffer(bb, newSize, 0, connected);
            bb.flip();
            if (n > 0 && rem > 0)
                dst.put(bb);
            return n;
        } finally {
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }

    private int receiveIntoNativeBuffer(ByteBuffer bb, int rem, int pos,
                                        boolean connected)
        throws IOException
    {
        int n = receive0(fd,
                         ((DirectBuffer)bb).address() + pos, rem,
                         sourceSockAddr.address(),
                         connected);
        if (n > 0)
            bb.position(pos + n);
        return n;
    }

    /**
     * Return an InetSocketAddress to represent the source/sender socket address
     * in sourceSockAddr. Returns the cached InetSocketAddress if the source
     * address is the same as the cached address.
     */
    private InetSocketAddress sourceSocketAddress() throws IOException {
        assert readLock.isHeldByCurrentThread();
        if (cachedInetSocketAddress != null && sourceSockAddr.equals(cachedSockAddr)) {
            return cachedInetSocketAddress;
        }
        InetSocketAddress isa = sourceSockAddr.decode();
        // swap sourceSockAddr and cachedSockAddr
        NativeSocketAddress tmp = cachedSockAddr;
        cachedSockAddr = sourceSockAddr;
        sourceSockAddr = tmp;
        cachedInetSocketAddress = isa;
        return isa;
    }

    @Override
    public int send(ByteBuffer src, SocketAddress target)
        throws IOException
    {
        Objects.requireNonNull(src);
        InetSocketAddress isa = Net.checkAddress(target, family);

        writeLock.lock();
        try {
            boolean blocking = isBlocking();
            int n;
            boolean completed = false;
            try {
                SocketAddress remote = beginWrite(blocking, false);
                if (remote != null) {
                    // connected
                    if (!target.equals(remote)) {
                        throw new AlreadyConnectedException();
                    }
                    n = IOUtil.write(fd, src, -1, nd);
                    if (blocking) {
                        while (IOStatus.okayToRetry(n) && isOpen()) {
                            park(Net.POLLOUT);
                            n = IOUtil.write(fd, src, -1, nd);
                        }
                    }
                    completed = (n > 0);
                } else {
                    // not connected
                    @SuppressWarnings("removal")
                    SecurityManager sm = System.getSecurityManager();
                    InetAddress ia = isa.getAddress();
                    if (sm != null) {
                        if (ia.isMulticastAddress()) {
                            sm.checkMulticast(ia);
                        } else {
                            sm.checkConnect(ia.getHostAddress(), isa.getPort());
                        }
                    }
                    if (ia.isLinkLocalAddress())
                        isa = IPAddressUtil.toScopedAddress(isa);
                    if (isa.getPort() == 0)
                        throw new SocketException("Can't send to port 0");
                    n = send(fd, src, isa);
                    if (blocking) {
                        while (IOStatus.okayToRetry(n) && isOpen()) {
                            park(Net.POLLOUT);
                            n = send(fd, src, isa);
                        }
                    }
                    completed = (n >= 0);
                }
            } finally {
                endWrite(blocking, completed);
            }
            assert n >= 0 || n == IOStatus.UNAVAILABLE;
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Sends a datagram from the bytes in given buffer.
     *
     * @apiNote This method is for use by the socket adaptor.
     *
     * @throws IllegalBlockingModeException if the channel is non-blocking
     */
    void blockingSend(ByteBuffer src, SocketAddress target) throws IOException {
        writeLock.lock();
        try {
            ensureOpen();
            if (!isBlocking())
                throw new IllegalBlockingModeException();
            send(src, target);
        } finally {
            writeLock.unlock();
        }
    }

    private int send(FileDescriptor fd, ByteBuffer src, InetSocketAddress target)
        throws IOException
    {
        if (src instanceof DirectBuffer)
            return sendFromNativeBuffer(fd, src, target);

        // Substitute a native buffer
        int pos = src.position();
        int lim = src.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
        try {
            bb.put(src);
            bb.flip();
            // Do not update src until we see how many bytes were written
            src.position(pos);

            int n = sendFromNativeBuffer(fd, bb, target);
            if (n > 0) {
                // now update src
                src.position(pos + n);
            }
            return n;
        } finally {
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }

    private int sendFromNativeBuffer(FileDescriptor fd, ByteBuffer bb,
                                     InetSocketAddress target)
        throws IOException
    {
        int pos = bb.position();
        int lim = bb.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        int written;
        try {
            int addressLen = targetSocketAddress(target);
            written = send0(fd, ((DirectBuffer)bb).address() + pos, rem,
                            targetSockAddr.address(), addressLen);
        } catch (PortUnreachableException pue) {
            if (isConnected())
                throw pue;
            written = rem;
        }
        if (written > 0)
            bb.position(pos + written);
        return written;
    }

    /**
     * Encodes the given InetSocketAddress into targetSockAddr, returning the
     * length of the sockaddr structure (sizeof struct sockaddr or sockaddr6).
     */
    private int targetSocketAddress(InetSocketAddress isa) {
        assert writeLock.isHeldByCurrentThread();
        // Nothing to do if target address is already in the buffer. Use
        // identity rather than equals as Inet6Address.equals ignores scope_id.
        if (isa == previousTarget)
            return previousSockAddrLength;
        previousTarget = null;
        int len = targetSockAddr.encode(family, isa);
        previousTarget = isa;
        previousSockAddrLength = len;
        return len;
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        Objects.requireNonNull(buf);

        readLock.lock();
        try {
            boolean blocking = isBlocking();
            int n = 0;
            try {
                beginRead(blocking, true);
                n = IOUtil.read(fd, buf, -1, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = IOUtil.read(fd, buf, -1, nd);
                    }
                }
            } finally {
                endRead(blocking, n > 0);
                assert IOStatus.check(n);
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
            boolean blocking = isBlocking();
            long n = 0;
            try {
                beginRead(blocking, true);
                n = IOUtil.read(fd, dsts, offset, length, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n)  && isOpen()) {
                        park(Net.POLLIN);
                        n = IOUtil.read(fd, dsts, offset, length, nd);
                    }
                }
            } finally {
                endRead(blocking, n > 0);
                assert IOStatus.check(n);
            }
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Marks the beginning of a write operation that might block.
     * @param blocking true if configured blocking
     * @param mustBeConnected true if the socket must be connected
     * @return remote address if connected
     * @throws ClosedChannelException if the channel is closed
     * @throws NotYetConnectedException if mustBeConnected and not connected
     * @throws IOException if socket not bound and cannot be bound
     */
    private SocketAddress beginWrite(boolean blocking, boolean mustBeConnected)
        throws IOException
    {
        if (blocking && interruptible) {
            // set hook for Thread.interrupt
            begin();
        }
        SocketAddress remote;
        synchronized (stateLock) {
            ensureOpen();
            remote = remoteAddress;
            if ((remote == null) && mustBeConnected)
                throw new NotYetConnectedException();
            if (localAddress == null)
                bindInternal(null);
            if (blocking)
                writerThread = NativeThread.current();
        }
        return remote;
    }

    /**
     * Marks the end of a write operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed asynchronously
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

            if (interruptible) {
                // remove hook for Thread.interrupt (may throw AsynchronousCloseException)
                end(completed);
            } else if (!completed && !isOpen()) {
                throw new AsynchronousCloseException();
            }
        }
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        Objects.requireNonNull(buf);

        writeLock.lock();
        try {
            boolean blocking = isBlocking();
            int n = 0;
            try {
                beginWrite(blocking, true);
                n = IOUtil.write(fd, buf, -1, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLOUT);
                        n = IOUtil.write(fd, buf, -1, nd);
                    }
                }
            } finally {
                endWrite(blocking, n > 0);
                assert IOStatus.check(n);
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
            boolean blocking = isBlocking();
            long n = 0;
            try {
                beginWrite(blocking, true);
                n = IOUtil.write(fd, srcs, offset, length, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLOUT);
                        n = IOUtil.write(fd, srcs, offset, length, nd);
                    }
                }
            } finally {
                endWrite(blocking, n > 0);
                assert IOStatus.check(n);
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

    InetSocketAddress localAddress() {
        synchronized (stateLock) {
            return localAddress;
        }
    }

    InetSocketAddress remoteAddress() {
        synchronized (stateLock) {
            return remoteAddress;
        }
    }

    @Override
    public DatagramChannel bind(SocketAddress local) throws IOException {
        readLock.lock();
        try {
            writeLock.lock();
            try {
                synchronized (stateLock) {
                    ensureOpen();
                    if (localAddress != null)
                        throw new AlreadyBoundException();
                    bindInternal(local);
                }
            } finally {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
        return this;
    }

    private void bindInternal(SocketAddress local) throws IOException {
        assert Thread.holdsLock(stateLock )&& (localAddress == null);

        InetSocketAddress isa;
        if (local == null) {
            // only Inet4Address allowed with IPv4 socket
            if (family == StandardProtocolFamily.INET) {
                isa = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0);
            } else {
                isa = new InetSocketAddress(0);
            }
        } else {
            isa = Net.checkAddress(local, family);
        }
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkListen(isa.getPort());

        Net.bind(family, fd, isa.getAddress(), isa.getPort());
        localAddress = Net.localAddress(fd);
    }

    @Override
    public boolean isConnected() {
        synchronized (stateLock) {
            return (state == ST_CONNECTED);
        }
    }

    @Override
    public DatagramChannel connect(SocketAddress sa) throws IOException {
        return connect(sa, true);
    }

    /**
     * Connects the channel's socket.
     *
     * @param sa the remote address to which this channel is to be connected
     * @param check true to check if the channel is already connected.
     */
    DatagramChannel connect(SocketAddress sa, boolean check) throws IOException {
        InetSocketAddress isa = Net.checkAddress(sa, family);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            InetAddress ia = isa.getAddress();
            if (ia.isMulticastAddress()) {
                sm.checkMulticast(ia);
            } else {
                sm.checkConnect(ia.getHostAddress(), isa.getPort());
                sm.checkAccept(ia.getHostAddress(), isa.getPort());
            }
        }

        readLock.lock();
        try {
            writeLock.lock();
            try {
                synchronized (stateLock) {
                    ensureOpen();
                    if (check && state == ST_CONNECTED)
                        throw new AlreadyConnectedException();
                    if (isa.getPort() == 0)
                        throw new SocketException("Can't connect to port 0");

                    // ensure that the socket is bound
                    if (localAddress == null) {
                        bindInternal(null);
                    }

                    // capture local address before connect
                    initialLocalAddress = localAddress;

                    int n = Net.connect(family,
                                        fd,
                                        isa.getAddress(),
                                        isa.getPort());
                    if (n <= 0)
                        throw new Error();      // Can't happen

                    // connected
                    remoteAddress = isa;
                    state = ST_CONNECTED;

                    // refresh local address
                    localAddress = Net.localAddress(fd);

                    // flush any packets already received.
                    boolean blocking = isBlocking();
                    if (blocking) {
                        IOUtil.configureBlocking(fd, false);
                    }
                    try {
                        ByteBuffer buf = ByteBuffer.allocate(100);
                        while (receive(buf, false) >= 0) {
                            buf.clear();
                        }
                    } finally {
                        if (blocking) {
                            IOUtil.configureBlocking(fd, true);
                        }
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

    @Override
    public DatagramChannel disconnect() throws IOException {
        readLock.lock();
        try {
            writeLock.lock();
            try {
                synchronized (stateLock) {
                    if (!isOpen() || (state != ST_CONNECTED))
                        return this;

                    // disconnect socket
                    boolean isIPv6 = (family == StandardProtocolFamily.INET6);
                    disconnect0(fd, isIPv6);

                    // no longer connected
                    remoteAddress = null;
                    state = ST_UNCONNECTED;

                    // refresh localAddress, should be same as it was prior to connect
                    localAddress = Net.localAddress(fd);
                    try {
                        if (!localAddress.equals(initialLocalAddress)) {
                            // Workaround connect(2) issues on Linux and macOS
                            repairSocket(initialLocalAddress);
                            assert (localAddress != null)
                                    && localAddress.equals(Net.localAddress(fd))
                                    && localAddress.equals(initialLocalAddress);
                        }
                    } finally {
                        initialLocalAddress = null;
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

    /**
     * "Repair" the channel's socket after a disconnect that didn't restore the
     * local address.
     *
     * On Linux, connect(2) dissolves the association but changes the local port
     * to 0 when it was initially bound to an ephemeral port. The workaround here
     * is to rebind to the original port.
     *
     * On macOS, connect(2) dissolves the association but rebinds the socket to
     * the wildcard address when it was initially bound to a specific address.
     * The workaround here is to re-create the socket.
     */
    private void repairSocket(InetSocketAddress target)
        throws IOException
    {
        assert Thread.holdsLock(stateLock);

        // Linux: try to bind the socket to the original address/port
        if (localAddress.getPort() == 0) {
            assert localAddress.getAddress().equals(target.getAddress());
            Net.bind(family, fd, target.getAddress(), target.getPort());
            localAddress = Net.localAddress(fd);
            return;
        }

        // capture the value of all existing socket options
        Map<SocketOption<?>, Object> map = new HashMap<>();
        for (SocketOption<?> option : supportedOptions()) {
            Object value = getOption(option);
            if (value != null) {
                map.put(option, value);
            }
        }

        // macOS: re-create the socket.
        FileDescriptor newfd = Net.socket(family, false);
        try {
            // copy the socket options that are protocol family agnostic
            for (Map.Entry<SocketOption<?>, Object> e : map.entrySet()) {
                SocketOption<?> option = e.getKey();
                if (SocketOptionRegistry.findOption(option, Net.UNSPEC) != null) {
                    Object value = e.getValue();
                    try {
                        Net.setSocketOption(newfd, Net.UNSPEC, option, value);
                    } catch (IOException ignore) { }
                }
            }

            // copy the blocking mode
            if (!isBlocking()) {
                IOUtil.configureBlocking(newfd, false);
            }

            // dup this channel's socket to the new socket. If this succeeds then
            // fd will reference the new socket. If it fails then it will still
            // reference the old socket.
            nd.dup(newfd, fd);
        } finally {
            // release the file descriptor
            nd.close(newfd);
        }

        // bind to the original local address
        try {
            Net.bind(family, fd, target.getAddress(), target.getPort());
        } catch (IOException ioe) {
            // bind failed, socket is left unbound
            localAddress = null;
            throw ioe;
        }

        // restore local address
        localAddress = Net.localAddress(fd);

        // restore all socket options (including those set in first pass)
        for (Map.Entry<SocketOption<?>, Object> e : map.entrySet()) {
            @SuppressWarnings("unchecked")
            SocketOption<Object> option = (SocketOption<Object>) e.getKey();
            Object value = e.getValue();
            try {
                setOption(option, value);
            } catch (IOException ignore) { }
        }

        // restore multicast group membership
        MembershipRegistry registry = this.registry;
        if (registry != null) {
            registry.forEach(k -> {
                if (k instanceof MembershipKeyImpl.Type6) {
                    MembershipKeyImpl.Type6 key6 = (MembershipKeyImpl.Type6) k;
                    Net.join6(fd, key6.groupAddress(), key6.index(), key6.source());
                } else {
                    MembershipKeyImpl.Type4 key4 = (MembershipKeyImpl.Type4) k;
                    Net.join4(fd, key4.groupAddress(), key4.interfaceAddress(), key4.source());
                }
            });
        }

        // reset registration in all Selectors that this channel is registered with
        AbstractSelectableChannels.forEach(this, SelectionKeyImpl::reset);
    }

    /**
     * Defines static methods to access AbstractSelectableChannel non-public members.
     */
    @SuppressWarnings("removal")
    private static class AbstractSelectableChannels {
        private static final Method FOREACH;
        static {
            try {
                PrivilegedExceptionAction<Method> pae = () -> {
                    Method m = AbstractSelectableChannel.class.getDeclaredMethod("forEach", Consumer.class);
                    m.setAccessible(true);
                    return m;
                };
                FOREACH = AccessController.doPrivileged(pae);
            } catch (Exception e) {
                throw new InternalError(e);
            }
        }
        static void forEach(AbstractSelectableChannel ch, Consumer<SelectionKeyImpl> action) {
            try {
                FOREACH.invoke(ch, action);
            } catch (Exception e) {
                throw new InternalError(e);
            }
        }
    }

    /**
     * Joins channel's socket to the given group/interface and
     * optional source address.
     */
    private MembershipKey innerJoin(InetAddress group,
                                    NetworkInterface interf,
                                    InetAddress source)
        throws IOException
    {
        if (!group.isMulticastAddress())
            throw new IllegalArgumentException("Group not a multicast address");

        // check multicast address is compatible with this socket
        if (group instanceof Inet4Address) {
            if (family == StandardProtocolFamily.INET6 && !Net.canIPv6SocketJoinIPv4Group())
                throw new IllegalArgumentException("IPv6 socket cannot join IPv4 multicast group");
        } else if (group instanceof Inet6Address) {
            if (family != StandardProtocolFamily.INET6)
                throw new IllegalArgumentException("Only IPv6 sockets can join IPv6 multicast group");
        } else {
            throw new IllegalArgumentException("Address type not supported");
        }

        // check source address
        if (source != null) {
            if (source.isAnyLocalAddress())
                throw new IllegalArgumentException("Source address is a wildcard address");
            if (source.isMulticastAddress())
                throw new IllegalArgumentException("Source address is multicast address");
            if (source.getClass() != group.getClass())
                throw new IllegalArgumentException("Source address is different type to group");
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkMulticast(group);

        synchronized (stateLock) {
            ensureOpen();

            // check the registry to see if we are already a member of the group
            if (registry == null) {
                registry = new MembershipRegistry();
            } else {
                // return existing membership key
                MembershipKey key = registry.checkMembership(group, interf, source);
                if (key != null)
                    return key;
            }

            MembershipKeyImpl key;
            if ((family == StandardProtocolFamily.INET6) &&
                ((group instanceof Inet6Address) || Net.canJoin6WithIPv4Group()))
            {
                int index = interf.getIndex();
                if (index == -1)
                    throw new IOException("Network interface cannot be identified");

                // need multicast and source address as byte arrays
                byte[] groupAddress = Net.inet6AsByteArray(group);
                byte[] sourceAddress = (source == null) ? null :
                    Net.inet6AsByteArray(source);

                // join the group
                int n = Net.join6(fd, groupAddress, index, sourceAddress);
                if (n == IOStatus.UNAVAILABLE)
                    throw new UnsupportedOperationException();

                key = new MembershipKeyImpl.Type6(this, group, interf, source,
                                                  groupAddress, index, sourceAddress);

            } else {
                // need IPv4 address to identify interface
                Inet4Address target = Net.anyInet4Address(interf);
                if (target == null)
                    throw new IOException("Network interface not configured for IPv4");

                int groupAddress = Net.inet4AsInt(group);
                int targetAddress = Net.inet4AsInt(target);
                int sourceAddress = (source == null) ? 0 : Net.inet4AsInt(source);

                // join the group
                int n = Net.join4(fd, groupAddress, targetAddress, sourceAddress);
                if (n == IOStatus.UNAVAILABLE)
                    throw new UnsupportedOperationException();

                key = new MembershipKeyImpl.Type4(this, group, interf, source,
                                                  groupAddress, targetAddress, sourceAddress);
            }

            registry.add(key);
            return key;
        }
    }

    @Override
    public MembershipKey join(InetAddress group,
                              NetworkInterface interf)
        throws IOException
    {
        return innerJoin(group, interf, null);
    }

    @Override
    public MembershipKey join(InetAddress group,
                              NetworkInterface interf,
                              InetAddress source)
        throws IOException
    {
        Objects.requireNonNull(source);
        return innerJoin(group, interf, source);
    }

    // package-private
    void drop(MembershipKeyImpl key) {
        assert key.channel() == this;

        synchronized (stateLock) {
            if (!key.isValid())
                return;

            try {
                if (key instanceof MembershipKeyImpl.Type6) {
                    MembershipKeyImpl.Type6 key6 =
                        (MembershipKeyImpl.Type6)key;
                    Net.drop6(fd, key6.groupAddress(), key6.index(), key6.source());
                } else {
                    MembershipKeyImpl.Type4 key4 = (MembershipKeyImpl.Type4)key;
                    Net.drop4(fd, key4.groupAddress(), key4.interfaceAddress(),
                        key4.source());
                }
            } catch (IOException ioe) {
                // should not happen
                throw new AssertionError(ioe);
            }

            key.invalidate();
            registry.remove(key);
        }
    }

    /**
     * Finds an existing membership of a multicast group. Returns null if this
     * channel's socket is not a member of the group.
     *
     * @apiNote This method is for use by the socket adaptor
     */
    MembershipKey findMembership(InetAddress group, NetworkInterface interf) {
        synchronized (stateLock) {
            if (registry != null) {
                return registry.checkMembership(group, interf, null);
            } else {
                return null;
            }
        }
    }

    /**
     * Block datagrams from the given source.
     */
    void block(MembershipKeyImpl key, InetAddress source)
        throws IOException
    {
        assert key.channel() == this;
        assert key.sourceAddress() == null;

        synchronized (stateLock) {
            if (!key.isValid())
                throw new IllegalStateException("key is no longer valid");
            if (source.isAnyLocalAddress())
                throw new IllegalArgumentException("Source address is a wildcard address");
            if (source.isMulticastAddress())
                throw new IllegalArgumentException("Source address is multicast address");
            if (source.getClass() != key.group().getClass())
                throw new IllegalArgumentException("Source address is different type to group");

            int n;
            if (key instanceof MembershipKeyImpl.Type6) {
                 MembershipKeyImpl.Type6 key6 =
                    (MembershipKeyImpl.Type6)key;
                n = Net.block6(fd, key6.groupAddress(), key6.index(),
                               Net.inet6AsByteArray(source));
            } else {
                MembershipKeyImpl.Type4 key4 =
                    (MembershipKeyImpl.Type4)key;
                n = Net.block4(fd, key4.groupAddress(), key4.interfaceAddress(),
                               Net.inet4AsInt(source));
            }
            if (n == IOStatus.UNAVAILABLE) {
                // ancient kernel
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Unblock the given source.
     */
    void unblock(MembershipKeyImpl key, InetAddress source) {
        assert key.channel() == this;
        assert key.sourceAddress() == null;

        synchronized (stateLock) {
            if (!key.isValid())
                throw new IllegalStateException("key is no longer valid");

            try {
                if (key instanceof MembershipKeyImpl.Type6) {
                    MembershipKeyImpl.Type6 key6 =
                        (MembershipKeyImpl.Type6)key;
                    Net.unblock6(fd, key6.groupAddress(), key6.index(),
                                 Net.inet6AsByteArray(source));
                } else {
                    MembershipKeyImpl.Type4 key4 =
                        (MembershipKeyImpl.Type4)key;
                    Net.unblock4(fd, key4.groupAddress(), key4.interfaceAddress(),
                                 Net.inet4AsInt(source));
                }
            } catch (IOException ioe) {
                // should not happen
                throw new AssertionError(ioe);
            }
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
            try {
                // close socket
                cleaner.clean();
            } catch (UncheckedIOException ioe) {
                throw ioe.getCause();
            }
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
     */
    private void implCloseBlockingMode() throws IOException {
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            state = ST_CLOSING;

            // if member of any multicast groups then invalidate the keys
            if (registry != null)
                registry.invalidateAll();

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
     */
    private void implCloseNonBlockingMode() throws IOException {
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            state = ST_CLOSING;

            // if member of any multicast groups then invalidate the keys
            if (registry != null)
                registry.invalidateAll();
        }

        // wait for any read/write operations to complete before trying to close
        readLock.lock();
        readLock.unlock();
        writeLock.lock();
        writeLock.unlock();
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
            ((intOps & SelectionKey.OP_READ) != 0))
            newOps |= SelectionKey.OP_READ;

        if (((ops & Net.POLLOUT) != 0) &&
            ((intOps & SelectionKey.OP_WRITE) != 0))
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
            newOps |= Net.POLLIN;
        return newOps;
    }

    public FileDescriptor getFD() {
        return fd;
    }

    public int getFDVal() {
        return fdVal;
    }

    /**
     * Returns an action to release the given file descriptor and socket addresses.
     */
    private static Runnable releaserFor(FileDescriptor fd, NativeSocketAddress... sockAddrs) {
        return () -> {
            try {
                nd.close(fd);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            } finally {
                // decrement socket count and release memory
                ResourceManager.afterUdpClose();
                NativeSocketAddress.freeAll(sockAddrs);
            }
        };
    }

    // -- Native methods --

    private static native void disconnect0(FileDescriptor fd, boolean isIPv6)
        throws IOException;

    private static native int receive0(FileDescriptor fd, long address, int len,
                                       long senderAddress, boolean connected)
        throws IOException;

    private static native int send0(FileDescriptor fd, long address, int len,
                                    long targetAddress, int targetAddressLen)
        throws IOException;

    static {
        IOUtil.load();
    }
}
