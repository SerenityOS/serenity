/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import jdk.internal.ref.CleanerFactory;
import sun.net.ConnectionResetException;
import sun.net.NetHooks;
import sun.net.PlatformSocketImpl;
import sun.net.ResourceManager;
import sun.net.ext.ExtendedSocketOptions;
import sun.net.util.SocketExceptions;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * NIO based SocketImpl.
 *
 * The underlying socket used by this SocketImpl is initially configured
 * blocking. If the connect method is used to establish a connection with a
 * timeout then the socket is configured non-blocking for the connect attempt,
 * and then restored to blocking mode when the connection is established.
 * If the accept or read methods are used with a timeout then the socket is
 * configured non-blocking and is never restored. When in non-blocking mode,
 * operations that don't complete immediately will poll the socket and preserve
 * the semantics of blocking operations.
 */

public final class NioSocketImpl extends SocketImpl implements PlatformSocketImpl {
    private static final NativeDispatcher nd = new SocketDispatcher();

    // The maximum number of bytes to read/write per syscall to avoid needing
    // a huge buffer from the temporary buffer cache
    private static final int MAX_BUFFER_SIZE = 128 * 1024;

    // true if this is a SocketImpl for a ServerSocket
    private final boolean server;

    // Lock held when reading (also used when accepting or connecting)
    private final ReentrantLock readLock = new ReentrantLock();

    // Lock held when writing
    private final ReentrantLock writeLock = new ReentrantLock();

    // The stateLock for read/changing state
    private final Object stateLock = new Object();
    private static final int ST_NEW = 0;
    private static final int ST_UNCONNECTED = 1;
    private static final int ST_CONNECTING = 2;
    private static final int ST_CONNECTED = 3;
    private static final int ST_CLOSING = 4;
    private static final int ST_CLOSED = 5;
    private volatile int state;  // need stateLock to change

    // set by SocketImpl.create, protected by stateLock
    private boolean stream;
    private Cleanable cleaner;

    // set to true when the socket is in non-blocking mode
    private volatile boolean nonBlocking;

    // used by connect/read/write/accept, protected by stateLock
    private long readerThread;
    private long writerThread;

    // used when SO_REUSEADDR is emulated, protected by stateLock
    private boolean isReuseAddress;

    // read or accept timeout in millis
    private volatile int timeout;

    // flags to indicate if the connection is shutdown for input and output
    private volatile boolean isInputClosed;
    private volatile boolean isOutputClosed;

    // used by read to emulate legacy behavior, protected by readLock
    private boolean readEOF;
    private boolean connectionReset;

    /**
     * Creates an instance of this SocketImpl.
     * @param server true if this is a SocketImpl for a ServerSocket
     */
    public NioSocketImpl(boolean server) {
        this.server = server;
    }

    /**
     * Returns true if the socket is open.
     */
    private boolean isOpen() {
        return state < ST_CLOSING;
    }

    /**
     * Throws SocketException if the socket is not open.
     */
    private void ensureOpen() throws SocketException {
        int state = this.state;
        if (state == ST_NEW)
            throw new SocketException("Socket not created");
        if (state >= ST_CLOSING)
            throw new SocketException("Socket closed");
    }

    /**
     * Throws SocketException if the socket is not open and connected.
     */
    private void ensureOpenAndConnected() throws SocketException {
        int state = this.state;
        if (state < ST_CONNECTED)
            throw new SocketException("Not connected");
        if (state > ST_CONNECTED)
            throw new SocketException("Socket closed");
    }

    /**
     * Disables the current thread for scheduling purposes until the socket is
     * ready for I/O, or is asynchronously closed, for up to the specified
     * waiting time.
     * @throws IOException if an I/O error occurs
     */
    private void park(FileDescriptor fd, int event, long nanos) throws IOException {
        long millis;
        if (nanos == 0) {
            millis = -1;
        } else {
            millis = NANOSECONDS.toMillis(nanos);
        }
        Net.poll(fd, event, millis);
    }

    /**
     * Disables the current thread for scheduling purposes until the socket is
     * ready for I/O or is asynchronously closed.
     * @throws IOException if an I/O error occurs
     */
    private void park(FileDescriptor fd, int event) throws IOException {
        park(fd, event, 0);
    }

    /**
     * Configures the socket to blocking mode. This method is a no-op if the
     * socket is already in blocking mode.
     * @throws IOException if closed or there is an I/O error changing the mode
     */
    private void configureBlocking(FileDescriptor fd) throws IOException {
        assert readLock.isHeldByCurrentThread();
        if (nonBlocking) {
            synchronized (stateLock) {
                ensureOpen();
                IOUtil.configureBlocking(fd, true);
                nonBlocking = false;
            }
        }
    }

    /**
     * Configures the socket to non-blocking mode. This method is a no-op if the
     * socket is already in non-blocking mode.
     * @throws IOException if closed or there is an I/O error changing the mode
     */
    private void configureNonBlocking(FileDescriptor fd) throws IOException {
        assert readLock.isHeldByCurrentThread();
        if (!nonBlocking) {
            synchronized (stateLock) {
                ensureOpen();
                IOUtil.configureBlocking(fd, false);
                nonBlocking = true;
            }
        }
    }

    /**
     * Marks the beginning of a read operation that might block.
     * @throws SocketException if the socket is closed or not connected
     */
    private FileDescriptor beginRead() throws SocketException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            readerThread = NativeThread.current();
            return fd;
        }
    }

    /**
     * Marks the end of a read operation that may have blocked.
     * @throws SocketException is the socket is closed
     */
    private void endRead(boolean completed) throws SocketException {
        synchronized (stateLock) {
            readerThread = 0;
            int state = this.state;
            if (state == ST_CLOSING)
                tryFinishClose();
            if (!completed && state >= ST_CLOSING)
                throw new SocketException("Socket closed");
        }
    }

    /**
     * Attempts to read bytes from the socket into the given byte array.
     */
    private int tryRead(FileDescriptor fd, byte[] b, int off, int len)
        throws IOException
    {
        ByteBuffer dst = Util.getTemporaryDirectBuffer(len);
        assert dst.position() == 0;
        try {
            int n = nd.read(fd, ((DirectBuffer)dst).address(), len);
            if (n > 0) {
                dst.get(b, off, n);
            }
            return n;
        } finally {
            Util.offerFirstTemporaryDirectBuffer(dst);
        }
    }

    /**
     * Reads bytes from the socket into the given byte array with a timeout.
     * @throws SocketTimeoutException if the read timeout elapses
     */
    private int timedRead(FileDescriptor fd, byte[] b, int off, int len, long nanos)
        throws IOException
    {
        long startNanos = System.nanoTime();
        int n = tryRead(fd, b, off, len);
        while (n == IOStatus.UNAVAILABLE && isOpen()) {
            long remainingNanos = nanos - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                throw new SocketTimeoutException("Read timed out");
            }
            park(fd, Net.POLLIN, remainingNanos);
            n = tryRead(fd, b, off, len);
        }
        return n;
    }

    /**
     * Reads bytes from the socket into the given byte array.
     * @return the number of bytes read or -1 at EOF
     * @throws SocketException if the socket is closed or a socket I/O error occurs
     * @throws SocketTimeoutException if the read timeout elapses
     */
    private int implRead(byte[] b, int off, int len) throws IOException {
        int n = 0;
        FileDescriptor fd = beginRead();
        try {
            if (connectionReset)
                throw new SocketException("Connection reset");
            if (isInputClosed)
                return -1;
            int timeout = this.timeout;
            if (timeout > 0) {
                // read with timeout
                configureNonBlocking(fd);
                n = timedRead(fd, b, off, len, MILLISECONDS.toNanos(timeout));
            } else {
                // read, no timeout
                n = tryRead(fd, b, off, len);
                while (IOStatus.okayToRetry(n) && isOpen()) {
                    park(fd, Net.POLLIN);
                    n = tryRead(fd, b, off, len);
                }
            }
            return n;
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (ConnectionResetException e) {
            connectionReset = true;
            throw new SocketException("Connection reset");
        } catch (IOException ioe) {
            throw new SocketException(ioe.getMessage());
        } finally {
            endRead(n > 0);
        }
    }

    /**
     * Reads bytes from the socket into the given byte array.
     * @return the number of bytes read or -1 at EOF
     * @throws IndexOutOfBoundsException if the bound checks fail
     * @throws SocketException if the socket is closed or a socket I/O error occurs
     * @throws SocketTimeoutException if the read timeout elapses
     */
    private int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        } else {
            readLock.lock();
            try {
                // emulate legacy behavior to return -1, even if socket is closed
                if (readEOF)
                    return -1;
                // read up to MAX_BUFFER_SIZE bytes
                int size = Math.min(len, MAX_BUFFER_SIZE);
                int n = implRead(b, off, size);
                if (n == -1)
                    readEOF = true;
                return n;
            } finally {
                readLock.unlock();
            }
        }
    }

    /**
     * Marks the beginning of a write operation that might block.
     * @throws SocketException if the socket is closed or not connected
     */
    private FileDescriptor beginWrite() throws SocketException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            writerThread = NativeThread.current();
            return fd;
        }
    }

    /**
     * Marks the end of a write operation that may have blocked.
     * @throws SocketException is the socket is closed
     */
    private void endWrite(boolean completed) throws SocketException {
        synchronized (stateLock) {
            writerThread = 0;
            int state = this.state;
            if (state == ST_CLOSING)
                tryFinishClose();
            if (!completed && state >= ST_CLOSING)
                throw new SocketException("Socket closed");
        }
    }

    /**
     * Attempts to write a sequence of bytes to the socket from the given
     * byte array.
     */
    private int tryWrite(FileDescriptor fd, byte[] b, int off, int len)
        throws IOException
    {
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
     * @return the number of bytes written
     * @throws SocketException if the socket is closed or a socket I/O error occurs
     */
    private int implWrite(byte[] b, int off, int len) throws IOException {
        int n = 0;
        FileDescriptor fd = beginWrite();
        try {
            n = tryWrite(fd, b, off, len);
            while (IOStatus.okayToRetry(n) && isOpen()) {
                park(fd, Net.POLLOUT);
                n = tryWrite(fd, b, off, len);
            }
            return n;
        } catch (IOException ioe) {
            throw new SocketException(ioe.getMessage());
        } finally {
            endWrite(n > 0);
        }
    }

    /**
     * Writes a sequence of bytes to the socket from the given byte array.
     * @throws SocketException if the socket is closed or a socket I/O error occurs
     */
    private void write(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len > 0) {
            writeLock.lock();
            try {
                int pos = off;
                int end = off + len;
                while (pos < end) {
                    // write up to MAX_BUFFER_SIZE bytes
                    int size = Math.min((end - pos), MAX_BUFFER_SIZE);
                    int n = implWrite(b, pos, size);
                    pos += n;
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * Creates the socket.
     * @param stream {@code true} for a streams socket
     */
    @Override
    protected void create(boolean stream) throws IOException {
        synchronized (stateLock) {
            if (state != ST_NEW)
                throw new IOException("Already created");
            if (!stream)
                ResourceManager.beforeUdpCreate();
            FileDescriptor fd;
            try {
                if (server) {
                    assert stream;
                    fd = Net.serverSocket(true);
                } else {
                    fd = Net.socket(stream);
                }
            } catch (IOException ioe) {
                if (!stream)
                    ResourceManager.afterUdpClose();
                throw ioe;
            }
            Runnable closer = closerFor(fd, stream);
            this.fd = fd;
            this.stream = stream;
            this.cleaner = CleanerFactory.cleaner().register(this, closer);
            this.state = ST_UNCONNECTED;
        }
    }

    /**
     * Marks the beginning of a connect operation that might block.
     * @throws SocketException if the socket is closed or already connected
     */
    private FileDescriptor beginConnect(InetAddress address, int port)
        throws IOException
    {
        synchronized (stateLock) {
            int state = this.state;
            if (state != ST_UNCONNECTED) {
                if (state == ST_NEW)
                    throw new SocketException("Not created");
                if (state == ST_CONNECTING)
                    throw new SocketException("Connection in progress");
                if (state == ST_CONNECTED)
                    throw new SocketException("Already connected");
                if (state >= ST_CLOSING)
                    throw new SocketException("Socket closed");
                assert false;
            }
            this.state = ST_CONNECTING;

            // invoke beforeTcpConnect hook if not already bound
            if (localport == 0) {
                NetHooks.beforeTcpConnect(fd, address, port);
            }

            // save the remote address/port
            this.address = address;
            this.port = port;

            readerThread = NativeThread.current();
            return fd;
        }
    }

    /**
     * Marks the end of a connect operation that may have blocked.
     * @throws SocketException is the socket is closed
     */
    private void endConnect(FileDescriptor fd, boolean completed) throws IOException {
        synchronized (stateLock) {
            readerThread = 0;
            int state = this.state;
            if (state == ST_CLOSING)
                tryFinishClose();
            if (completed && state == ST_CONNECTING) {
                this.state = ST_CONNECTED;
                localport = Net.localAddress(fd).getPort();
            } else if (!completed && state >= ST_CLOSING) {
                throw new SocketException("Socket closed");
            }
        }
    }

    /**
     * Waits for a connection attempt to finish with a timeout
     * @throws SocketTimeoutException if the connect timeout elapses
     */
    private boolean timedFinishConnect(FileDescriptor fd, long nanos) throws IOException {
        long startNanos = System.nanoTime();
        boolean polled = Net.pollConnectNow(fd);
        while (!polled && isOpen()) {
            long remainingNanos = nanos - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                throw new SocketTimeoutException("Connect timed out");
            }
            park(fd, Net.POLLOUT, remainingNanos);
            polled = Net.pollConnectNow(fd);
        }
        return polled && isOpen();
    }

    /**
     * Attempts to establish a connection to the given socket address with a
     * timeout. Closes the socket if connection cannot be established.
     * @throws IOException if the address is not a resolved InetSocketAddress or
     *         the connection cannot be established
     */
    @Override
    protected void connect(SocketAddress remote, int millis) throws IOException {
        // SocketImpl connect only specifies IOException
        if (!(remote instanceof InetSocketAddress))
            throw new IOException("Unsupported address type");
        InetSocketAddress isa = (InetSocketAddress) remote;
        if (isa.isUnresolved()) {
            throw new UnknownHostException(isa.getHostName());
        }

        InetAddress address = isa.getAddress();
        if (address.isAnyLocalAddress())
            address = InetAddress.getLocalHost();
        int port = isa.getPort();

        ReentrantLock connectLock = readLock;
        try {
            connectLock.lock();
            try {
                boolean connected = false;
                FileDescriptor fd = beginConnect(address, port);
                try {

                    // configure socket to non-blocking mode when there is a timeout
                    if (millis > 0) {
                        configureNonBlocking(fd);
                    }

                    int n = Net.connect(fd, address, port);
                    if (n > 0) {
                        // connection established
                        connected = true;
                    } else {
                        assert IOStatus.okayToRetry(n);
                        if (millis > 0) {
                            // finish connect with timeout
                            long nanos = MILLISECONDS.toNanos(millis);
                            connected = timedFinishConnect(fd, nanos);
                        } else {
                            // finish connect, no timeout
                            boolean polled = false;
                            while (!polled && isOpen()) {
                                park(fd, Net.POLLOUT);
                                polled = Net.pollConnectNow(fd);
                            }
                            connected = polled && isOpen();
                        }
                    }

                    // restore socket to blocking mode
                    if (connected && millis > 0) {
                        configureBlocking(fd);
                    }

                } finally {
                    endConnect(fd, connected);
                }
            } finally {
                connectLock.unlock();
            }
        } catch (IOException ioe) {
            close();
            throw SocketExceptions.of(ioe, isa);
        }
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        connect(new InetSocketAddress(host, port), 0);
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        connect(new InetSocketAddress(address, port), 0);
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (localport != 0)
                throw new SocketException("Already bound");
            NetHooks.beforeTcpBind(fd, host, port);
            Net.bind(fd, host, port);
            // set the address field to the given host address to
            // maintain long standing behavior. When binding to 0.0.0.0
            // then the actual local address will be ::0 when IPv6 is enabled.
            address = host;
            localport = Net.localAddress(fd).getPort();
        }
    }

    @Override
    protected void listen(int backlog) throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (localport == 0)
                throw new SocketException("Not bound");
            Net.listen(fd, backlog < 1 ? 50 : backlog);
        }
    }

    /**
     * Marks the beginning of an accept operation that might block.
     * @throws SocketException if the socket is closed
     */
    private FileDescriptor beginAccept() throws SocketException {
        synchronized (stateLock) {
            ensureOpen();
            if (!stream)
                throw new SocketException("Not a stream socket");
            if (localport == 0)
                throw new SocketException("Not bound");
            readerThread = NativeThread.current();
            return fd;
        }
    }

    /**
     * Marks the end of an accept operation that may have blocked.
     * @throws SocketException is the socket is closed
     */
    private void endAccept(boolean completed) throws SocketException {
        synchronized (stateLock) {
            int state = this.state;
            readerThread = 0;
            if (state == ST_CLOSING)
                tryFinishClose();
            if (!completed && state >= ST_CLOSING)
                throw new SocketException("Socket closed");
        }
    }

    /**
     * Accepts a new connection with a timeout.
     * @throws SocketTimeoutException if the accept timeout elapses
     */
    private int timedAccept(FileDescriptor fd,
                            FileDescriptor newfd,
                            InetSocketAddress[] isaa,
                            long nanos)
        throws IOException
    {
        long startNanos = System.nanoTime();
        int n = Net.accept(fd, newfd, isaa);
        while (n == IOStatus.UNAVAILABLE && isOpen()) {
            long remainingNanos = nanos - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                throw new SocketTimeoutException("Accept timed out");
            }
            park(fd, Net.POLLIN, remainingNanos);
            n = Net.accept(fd, newfd, isaa);
        }
        return n;
    }

    /**
     * Accepts a new connection so that the given SocketImpl is connected to
     * the peer. The SocketImpl must be a newly created NioSocketImpl.
     */
    @Override
    protected void accept(SocketImpl si) throws IOException {
        NioSocketImpl nsi = (NioSocketImpl) si;
        if (nsi.state != ST_NEW)
            throw new SocketException("Not a newly created SocketImpl");

        FileDescriptor newfd = new FileDescriptor();
        InetSocketAddress[] isaa = new InetSocketAddress[1];

        // acquire the lock, adjusting the timeout for cases where several
        // threads are accepting connections and there is a timeout set
        ReentrantLock acceptLock = readLock;
        int timeout = this.timeout;
        long remainingNanos = 0;
        if (timeout > 0) {
            remainingNanos = tryLock(acceptLock, timeout, MILLISECONDS);
            if (remainingNanos <= 0) {
                assert !acceptLock.isHeldByCurrentThread();
                throw new SocketTimeoutException("Accept timed out");
            }
        } else {
            acceptLock.lock();
        }

        // accept a connection
        try {
            int n = 0;
            FileDescriptor fd = beginAccept();
            try {
                if (remainingNanos > 0) {
                    // accept with timeout
                    configureNonBlocking(fd);
                    n = timedAccept(fd, newfd, isaa, remainingNanos);
                } else {
                    // accept, no timeout
                    n = Net.accept(fd, newfd, isaa);
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(fd, Net.POLLIN);
                        n = Net.accept(fd, newfd, isaa);
                    }
                }
            } finally {
                endAccept(n > 0);
                assert IOStatus.check(n);
            }
        } finally {
            acceptLock.unlock();
        }

        // get local address and configure accepted socket to blocking mode
        InetSocketAddress localAddress;
        try {
            localAddress = Net.localAddress(newfd);
            IOUtil.configureBlocking(newfd, true);
        } catch (IOException ioe) {
            nd.close(newfd);
            throw ioe;
        }

        // set the fields
        Runnable closer = closerFor(newfd, true);
        synchronized (nsi.stateLock) {
            nsi.fd = newfd;
            nsi.stream = true;
            nsi.cleaner = CleanerFactory.cleaner().register(nsi, closer);
            nsi.localport = localAddress.getPort();
            nsi.address = isaa[0].getAddress();
            nsi.port = isaa[0].getPort();
            nsi.state = ST_CONNECTED;
        }
    }

    @Override
    protected InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                byte[] a = new byte[1];
                int n = read(a, 0, 1);
                return (n > 0) ? (a[0] & 0xff) : -1;
            }
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return NioSocketImpl.this.read(b, off, len);
            }
            @Override
            public int available() throws IOException {
                return NioSocketImpl.this.available();
            }
            @Override
            public void close() throws IOException {
                NioSocketImpl.this.close();
            }
        };
    }

    @Override
    protected OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                byte[] a = new byte[]{(byte) b};
                write(a, 0, 1);
            }
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                NioSocketImpl.this.write(b, off, len);
            }
            @Override
            public void close() throws IOException {
                NioSocketImpl.this.close();
            }
        };
    }

    @Override
    protected int available() throws IOException {
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
     * Closes the socket if there are no I/O operations in progress.
     */
    private boolean tryClose() throws IOException {
        assert Thread.holdsLock(stateLock) && state == ST_CLOSING;
        if (readerThread == 0 && writerThread == 0) {
            try {
                cleaner.clean();
            } catch (UncheckedIOException ioe) {
                throw ioe.getCause();
            } finally {
                state = ST_CLOSED;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Invokes tryClose to attempt to close the socket.
     *
     * This method is used for deferred closing by I/O operations.
     */
    private void tryFinishClose() {
        try {
            tryClose();
        } catch (IOException ignore) { }
    }

    /**
     * Closes the socket. If there are I/O operations in progress then the
     * socket is pre-closed and the threads are signalled. The socket will be
     * closed when the last I/O operation aborts.
     */
    @Override
    protected void close() throws IOException {
        synchronized (stateLock) {
            int state = this.state;
            if (state >= ST_CLOSING)
                return;
            if (state == ST_NEW) {
                // stillborn
                this.state = ST_CLOSED;
                return;
            }
            this.state = ST_CLOSING;

            // shutdown output when linger interval not set to 0
            try {
                var SO_LINGER = StandardSocketOptions.SO_LINGER;
                if ((int) Net.getSocketOption(fd, SO_LINGER) != 0) {
                    Net.shutdown(fd, Net.SHUT_WR);
                }
            } catch (IOException ignore) { }

            // attempt to close the socket. If there are I/O operations in progress
            // then the socket is pre-closed and the thread(s) signalled. The
            // last thread will close the file descriptor.
            if (!tryClose()) {
                nd.preClose(fd);
                long reader = readerThread;
                if (reader != 0)
                    NativeThread.signal(reader);
                long writer = writerThread;
                if (writer != 0)
                    NativeThread.signal(writer);
            }
        }
    }

    // the socket options supported by client and server sockets
    private static volatile Set<SocketOption<?>> clientSocketOptions;
    private static volatile Set<SocketOption<?>> serverSocketOptions;

    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> options = (server) ? serverSocketOptions : clientSocketOptions;
        if (options == null) {
            options = new HashSet<>();
            options.add(StandardSocketOptions.SO_RCVBUF);
            options.add(StandardSocketOptions.SO_REUSEADDR);
            if (server) {
                // IP_TOS added for server socket to maintain compatibility
                options.add(StandardSocketOptions.IP_TOS);
                options.addAll(ExtendedSocketOptions.serverSocketOptions());
            } else {
                options.add(StandardSocketOptions.IP_TOS);
                options.add(StandardSocketOptions.SO_KEEPALIVE);
                options.add(StandardSocketOptions.SO_SNDBUF);
                options.add(StandardSocketOptions.SO_LINGER);
                options.add(StandardSocketOptions.TCP_NODELAY);
                options.addAll(ExtendedSocketOptions.clientSocketOptions());
            }
            if (Net.isReusePortAvailable())
                options.add(StandardSocketOptions.SO_REUSEPORT);
            options = Collections.unmodifiableSet(options);
            if (server) {
                serverSocketOptions = options;
            } else {
                clientSocketOptions = options;
            }
        }
        return options;
    }

    @Override
    protected <T> void setOption(SocketOption<T> opt, T value) throws IOException {
        if (!supportedOptions().contains(opt))
            throw new UnsupportedOperationException("'" + opt + "' not supported");
        if (!opt.type().isInstance(value))
            throw new IllegalArgumentException("Invalid value '" + value + "'");
        synchronized (stateLock) {
            ensureOpen();
            if (opt == StandardSocketOptions.IP_TOS) {
                // maps to IP_TOS or IPV6_TCLASS
                Net.setSocketOption(fd, family(), opt, value);
            } else if (opt == StandardSocketOptions.SO_REUSEADDR) {
                boolean b = (boolean) value;
                if (Net.useExclusiveBind()) {
                    isReuseAddress = b;
                } else {
                    Net.setSocketOption(fd, opt, b);
                }
            } else {
                // option does not need special handling
                Net.setSocketOption(fd, opt, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getOption(SocketOption<T> opt) throws IOException {
        if (!supportedOptions().contains(opt))
            throw new UnsupportedOperationException("'" + opt + "' not supported");
        synchronized (stateLock) {
            ensureOpen();
            if (opt == StandardSocketOptions.IP_TOS) {
                return (T) Net.getSocketOption(fd, family(), opt);
            } else if (opt == StandardSocketOptions.SO_REUSEADDR) {
                if (Net.useExclusiveBind()) {
                    return (T) Boolean.valueOf(isReuseAddress);
                } else {
                    return (T) Net.getSocketOption(fd, opt);
                }
            } else {
                // option does not need special handling
                return (T) Net.getSocketOption(fd, opt);
            }
        }
    }

    private boolean booleanValue(Object value, String desc) throws SocketException {
        if (!(value instanceof Boolean))
            throw new SocketException("Bad value for " + desc);
        return (boolean) value;
    }

    private int intValue(Object value, String desc) throws SocketException {
        if (!(value instanceof Integer))
            throw new SocketException("Bad value for " + desc);
        return (int) value;
    }

    @Override
    public void setOption(int opt, Object value) throws SocketException {
        synchronized (stateLock) {
            ensureOpen();
            try {
                switch (opt) {
                case SO_LINGER: {
                    // the value is "false" to disable, or linger interval to enable
                    int i;
                    if (value instanceof Boolean && ((boolean) value) == false) {
                        i = -1;
                    } else {
                        i = intValue(value, "SO_LINGER");
                    }
                    Net.setSocketOption(fd, StandardSocketOptions.SO_LINGER, i);
                    break;
                }
                case SO_TIMEOUT: {
                    int i = intValue(value, "SO_TIMEOUT");
                    if (i < 0)
                        throw new IllegalArgumentException("timeout < 0");
                    timeout = i;
                    break;
                }
                case IP_TOS: {
                    int i = intValue(value, "IP_TOS");
                    Net.setSocketOption(fd, family(), StandardSocketOptions.IP_TOS, i);
                    break;
                }
                case TCP_NODELAY: {
                    boolean b = booleanValue(value, "TCP_NODELAY");
                    Net.setSocketOption(fd, StandardSocketOptions.TCP_NODELAY, b);
                    break;
                }
                case SO_SNDBUF: {
                    int i = intValue(value, "SO_SNDBUF");
                    if (i <= 0)
                        throw new SocketException("SO_SNDBUF <= 0");
                    Net.setSocketOption(fd, StandardSocketOptions.SO_SNDBUF, i);
                    break;
                }
                case SO_RCVBUF: {
                    int i = intValue(value, "SO_RCVBUF");
                    if (i <= 0)
                        throw new SocketException("SO_RCVBUF <= 0");
                    Net.setSocketOption(fd, StandardSocketOptions.SO_RCVBUF, i);
                    break;
                }
                case SO_KEEPALIVE: {
                    boolean b = booleanValue(value, "SO_KEEPALIVE");
                    Net.setSocketOption(fd, StandardSocketOptions.SO_KEEPALIVE, b);
                    break;
                }
                case SO_OOBINLINE: {
                    boolean b = booleanValue(value, "SO_OOBINLINE");
                    Net.setSocketOption(fd, ExtendedSocketOption.SO_OOBINLINE, b);
                    break;
                }
                case SO_REUSEADDR: {
                    boolean b = booleanValue(value, "SO_REUSEADDR");
                    if (Net.useExclusiveBind()) {
                        isReuseAddress = b;
                    } else {
                        Net.setSocketOption(fd, StandardSocketOptions.SO_REUSEADDR, b);
                    }
                    break;
                }
                case SO_REUSEPORT: {
                    if (!Net.isReusePortAvailable())
                        throw new SocketException("SO_REUSEPORT not supported");
                    boolean b = booleanValue(value, "SO_REUSEPORT");
                    Net.setSocketOption(fd, StandardSocketOptions.SO_REUSEPORT, b);
                    break;
                }
                default:
                    throw new SocketException("Unknown option " + opt);
                }
            } catch (SocketException e) {
                throw e;
            } catch (IllegalArgumentException | IOException e) {
                throw new SocketException(e.getMessage());
            }
        }
    }

    @Override
    public Object getOption(int opt) throws SocketException {
        synchronized (stateLock) {
            ensureOpen();
            try {
                switch (opt) {
                case SO_TIMEOUT:
                    return timeout;
                case TCP_NODELAY:
                    return Net.getSocketOption(fd, StandardSocketOptions.TCP_NODELAY);
                case SO_OOBINLINE:
                    return Net.getSocketOption(fd, ExtendedSocketOption.SO_OOBINLINE);
                case SO_LINGER: {
                    // return "false" when disabled, linger interval when enabled
                    int i = (int) Net.getSocketOption(fd, StandardSocketOptions.SO_LINGER);
                    if (i == -1) {
                        return Boolean.FALSE;
                    } else {
                        return i;
                    }
                }
                case SO_REUSEADDR:
                    if (Net.useExclusiveBind()) {
                        return isReuseAddress;
                    } else {
                        return Net.getSocketOption(fd, StandardSocketOptions.SO_REUSEADDR);
                    }
                case SO_BINDADDR:
                    return Net.localAddress(fd).getAddress();
                case SO_SNDBUF:
                    return Net.getSocketOption(fd, StandardSocketOptions.SO_SNDBUF);
                case SO_RCVBUF:
                    return Net.getSocketOption(fd, StandardSocketOptions.SO_RCVBUF);
                case IP_TOS:
                    return Net.getSocketOption(fd, family(), StandardSocketOptions.IP_TOS);
                case SO_KEEPALIVE:
                    return Net.getSocketOption(fd, StandardSocketOptions.SO_KEEPALIVE);
                case SO_REUSEPORT:
                    if (!Net.isReusePortAvailable())
                        throw new SocketException("SO_REUSEPORT not supported");
                    return Net.getSocketOption(fd, StandardSocketOptions.SO_REUSEPORT);
                default:
                    throw new SocketException("Unknown option " + opt);
                }
            } catch (SocketException e) {
                throw e;
            } catch (IllegalArgumentException | IOException e) {
                throw new SocketException(e.getMessage());
            }
        }
    }

    @Override
    protected void shutdownInput() throws IOException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            if (!isInputClosed) {
                Net.shutdown(fd, Net.SHUT_RD);
                isInputClosed = true;
            }
        }
    }

    @Override
    protected void shutdownOutput() throws IOException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            if (!isOutputClosed) {
                Net.shutdown(fd, Net.SHUT_WR);
                isOutputClosed = true;
            }
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        return true;
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        writeLock.lock();
        try {
            int n = 0;
            FileDescriptor fd = beginWrite();
            try {
                do {
                    n = Net.sendOOB(fd, (byte) data);
                } while (n == IOStatus.INTERRUPTED && isOpen());
                if (n == IOStatus.UNAVAILABLE) {
                    throw new SocketException("No buffer space available");
                }
            } finally {
                endWrite(n > 0);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns an action to close the given file descriptor.
     */
    private static Runnable closerFor(FileDescriptor fd, boolean stream) {
        if (stream) {
            return () -> {
                try {
                    nd.close(fd);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            };
        } else {
            return () -> {
                try {
                    nd.close(fd);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                } finally {
                    // decrement
                    ResourceManager.afterUdpClose();
                }
            };
        }
    }

    /**
     * Attempts to acquire the given lock within the given waiting time.
     * @return the remaining time in nanoseconds when the lock is acquired, zero
     *         or less if the lock was not acquired before the timeout expired
     */
    private static long tryLock(ReentrantLock lock, long timeout, TimeUnit unit) {
        assert timeout > 0;
        boolean interrupted = false;
        long nanos = NANOSECONDS.convert(timeout, unit);
        long remainingNanos = nanos;
        long startNanos = System.nanoTime();
        boolean acquired = false;
        while (!acquired && (remainingNanos > 0)) {
            try {
                acquired = lock.tryLock(remainingNanos, NANOSECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            }
            remainingNanos = nanos - (System.nanoTime() - startNanos);
        }
        if (acquired && remainingNanos <= 0L)
            lock.unlock();  // release lock if timeout has expired
        if (interrupted)
            Thread.currentThread().interrupt();
        return remainingNanos;
    }

    /**
     * Returns the socket protocol family.
     */
    private static ProtocolFamily family() {
        if (Net.isIPv6Available()) {
            return StandardProtocolFamily.INET6;
        } else {
            return StandardProtocolFamily.INET;
        }
    }
}
