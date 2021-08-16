/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 8220493
 * @modules java.base/java.net:+open java.base/sun.nio.ch:+open
 * @run testng/othervm SocketImplCombinations
 * @summary Test Socket and ServerSocket with combinations of SocketImpls
 */

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.BiConsumer;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class SocketImplCombinations {

    /**
     * Test creating an unconnected Socket, it should be created with a platform SocketImpl.
     */
    public void testNewSocket1() throws IOException {
        try (Socket s = new Socket()) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(isSocksSocketImpl(si));
            SocketImpl delegate = getDelegate(si);
            assertTrue(isPlatformSocketImpl(delegate));
        }
    }

    /**
     * Test creating a connected Socket, it should be created with a platform SocketImpl.
     */
    public void testNewSocket2() throws IOException {
        try (ServerSocket ss = boundServerSocket()) {
            try (Socket s = new Socket(ss.getInetAddress(), ss.getLocalPort())) {
                SocketImpl si = getSocketImpl(s);
                assertTrue(isSocksSocketImpl(si));
                SocketImpl delegate = getDelegate(si);
                assertTrue(isPlatformSocketImpl(delegate));
            }
        }
    }

    /**
     * Test creating a Socket for a DIRECT connection, it should be created with a
     * platform SocketImpl.
     */
    public void testNewSocket3() throws IOException {
        try (Socket s = new Socket(Proxy.NO_PROXY)) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(isPlatformSocketImpl(si));
        }
    }

    /**
     * Test creating a Socket for a SOCKS connection, it should be created with a
     * SOCKS SocketImpl.
     */
    public void testNewSocket4() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 1080);
        var socksProxy = new Proxy(Proxy.Type.SOCKS, address);
        try (Socket s = new Socket(socksProxy)) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(isSocksSocketImpl(si));
            SocketImpl delegate = getDelegate(si);
            assertTrue(isPlatformSocketImpl(delegate));
        }
    }

    /**
     * Test creating a Socket for a HTTP proxy connection, it should be created with
     * a HTTP proxy SocketImpl.
     */
    public void testNewSocket5() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 8080);
        var httpProxy = new Proxy(Proxy.Type.HTTP, address);
        try (Socket s = new Socket(httpProxy)) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(isHttpConnectSocketImpl(si));
            SocketImpl delegate = getDelegate(si);
            assertTrue(isPlatformSocketImpl(delegate));
        }
    }

    /**
     * Test creating a Socket no SocketImpl. A platform SocketImpl should be
     * created lazily.
     */
    public void testNewSocket6() throws IOException {
        Socket s = new Socket((SocketImpl) null) { };
        try (s) {
            assertTrue(getSocketImpl(s) == null);
            s.bind(loopbackSocketAddress());   // force SocketImpl to be created
            SocketImpl si = getSocketImpl(s);
            assertTrue(isSocksSocketImpl(si));
            SocketImpl delegate = getDelegate(si);
            assertTrue(isPlatformSocketImpl(delegate));
        }
    }

    /**
     * Test creating a Socket with a custom SocketImpl.
     */
    public void testNewSocket7() throws IOException {
        Socket s = new Socket(new CustomSocketImpl(false)) { };
        try (s) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(si instanceof CustomSocketImpl);
        }
    }

    /**
     * Test creating a Socket when there is a SocketImplFactory set.
     */
    public void testNewSocket8() throws IOException {
        setSocketSocketImplFactory(() -> new CustomSocketImpl(false));
        try (Socket s = new Socket()) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(si instanceof CustomSocketImpl);
        } finally {
            setSocketSocketImplFactory(null);
        }
    }

    /**
     * Test creating a Socket for a DIRECT connection when there is a
     * SocketImplFactory set.
     */
    public void testNewSocket9() throws IOException {
        setSocketSocketImplFactory(() -> new CustomSocketImpl(false));
        try (Socket s = new Socket(Proxy.NO_PROXY)) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(si instanceof CustomSocketImpl);
        } finally {
            setSocketSocketImplFactory(null);
        }
    }

    /**
     * Test creating a Socket for a SOCKS connection when there is a
     * SocketImplFactory set.
     */
    public void testNewSocket10() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 1080);
        var socksProxy = new Proxy(Proxy.Type.SOCKS, address);
        setSocketSocketImplFactory(() -> new CustomSocketImpl(false));
        try (Socket s = new Socket(socksProxy)) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(isSocksSocketImpl(si));
            SocketImpl delegate = getDelegate(si);
            assertTrue(isPlatformSocketImpl(delegate));
        } finally {
            setSocketSocketImplFactory(null);
        }
    }

    /**
     * Test creating a Socket for a HTTP proxy connection when there is a
     * SocketImplFactory set.
     */
    public void testNewSocket11() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 8080);
        var httpProxy = new Proxy(Proxy.Type.HTTP, address);
        setSocketSocketImplFactory(() -> new CustomSocketImpl(false));
        try (Socket s = new Socket(httpProxy)) {
            SocketImpl si = getSocketImpl(s);
            assertTrue(isHttpConnectSocketImpl(si));
            SocketImpl delegate = getDelegate(si);
            assertTrue(isPlatformSocketImpl(delegate));
        } finally {
            setSocketSocketImplFactory(null);
        }
    }

    /**
     * Test creating a Socket no SocketImpl when there is a SocketImplFactory set.
     */
    public void testNewSocket12() throws IOException {
        setSocketSocketImplFactory(() -> new CustomSocketImpl(false));
        try {
            Socket s = new Socket((SocketImpl) null) { };
            try (s) {
                assertTrue(getSocketImpl(s) == null);
                s.bind(loopbackSocketAddress());   // force SocketImpl to be created
                assertTrue(getSocketImpl(s) instanceof CustomSocketImpl);
            }
        } finally {
            setSocketSocketImplFactory(null);
        }
    }

    /**
     * Test creating an unbound ServerSocket, it should be created with a platform
     * SocketImpl.
     */
    public void testNewServerSocket1() throws IOException {
        try (ServerSocket ss = new ServerSocket()) {
            SocketImpl si = getSocketImpl(ss);
            assertTrue(isPlatformSocketImpl(si));
        }
    }

    /**
     * Test creating a bound ServerSocket, it should be created with a platform
     * SocketImpl.
     */
    public void testNewServerSocket2() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            SocketImpl si = getSocketImpl(ss);
            assertTrue(isPlatformSocketImpl(si));
        }
    }

    /**
     * Test creating a ServerSocket with a custom SocketImpl.
     */
    public void testNewServerSocket3() throws IOException {
        ServerSocket ss = new ServerSocket(new CustomSocketImpl(true)) { };
        try (ss) {
            SocketImpl si = getSocketImpl(ss);
            assertTrue(si instanceof CustomSocketImpl);
        }
    }

    /**
     * Test creating an unbound ServerSocket when there is a SocketImplFactory set.
     */
    public void testNewServerSocket4() throws IOException {
        setServerSocketImplFactory(() -> new CustomSocketImpl(true));
        try (ServerSocket ss = new ServerSocket()) {
            SocketImpl si = getSocketImpl(ss);
            assertTrue(si instanceof CustomSocketImpl);
        } finally {
            setServerSocketImplFactory(null);
        }
    }

    /**
     * Test creating a bound ServerSocket when there is a SocketImplFactory set.
     */
    public void testNewServerSocket5() throws IOException {
        setServerSocketImplFactory(() -> new CustomSocketImpl(true));
        try (ServerSocket ss = new ServerSocket(0)) {
            SocketImpl si = getSocketImpl(ss);
            assertTrue(si instanceof CustomSocketImpl);
        } finally {
            setServerSocketImplFactory(null);
        }
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a platform SocketImpl,
     * the Socket to accept is created with no SocketImpl.
     */
    public void testServerSocketAccept1() throws IOException {
        var socket = new Socket((SocketImpl) null) { };
        assertTrue(getSocketImpl(socket) == null);

        serverSocketAccept(socket, (ss, s) -> {
            assertTrue(isPlatformSocketImpl(getSocketImpl(ss)));
            assertTrue(s == socket);
            SocketImpl si = getSocketImpl(s);
            assertTrue(isPlatformSocketImpl(si));
            checkFields(si);
        });
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a platform SocketImpl,
     * the Socket to accept is created with no SocketImpl, and there is a custom
     * client SocketImplFactory set.
     */
    public void testServerSocketAccept2() throws IOException {
        var socket = new Socket((SocketImpl) null) { };
        assertTrue(getSocketImpl(socket) == null);

        serverSocketAccept(socket, () -> new CustomSocketImpl(false), (ss, s) -> {
            assertTrue(isPlatformSocketImpl(getSocketImpl(ss)));
            assertTrue(s == socket);
            SocketImpl si = getSocketImpl(s);
            assertTrue(isPlatformSocketImpl(si));
            checkFields(si);
        });
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a platform SocketImpl,
     * the Socket to accept is created with a SocketImpl that delegates to a
     * platform SocketImpl.
     */
    public void testServerSocketAccept3() throws IOException {
        var socket = new Socket();
        SocketImpl si = getSocketImpl(socket);
        assertTrue(isSocksSocketImpl(si));
        SocketImpl delegate = getDelegate(si);
        assertTrue(isPlatformSocketImpl(delegate));

        serverSocketAccept(socket, (ss, s) -> {
            assertTrue(isPlatformSocketImpl(getSocketImpl(ss)));
            assertTrue(s == socket);
            SocketImpl psi = getSocketImpl(socket);
            assertTrue(isPlatformSocketImpl(psi));
            checkFields(psi);
        });
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a platform SocketImpl,
     * the Socket to accept is created with a custom SocketImpl.
     */
    public void testServerSocketAccept4a() throws IOException {
        SocketImpl clientImpl = new CustomSocketImpl(false);
        Socket socket = new Socket(clientImpl) { };
        assertTrue(getSocketImpl(socket) == clientImpl);

        try (ServerSocket ss = serverSocketToAccept(socket)) {
            expectThrows(IOException.class, ss::accept);
        } finally {
            socket.close();
        }
    }

    public void testServerSocketAccept4b() throws IOException {
        SocketImpl clientImpl = new CustomSocketImpl(false);
        Socket socket = new Socket(clientImpl) { };
        assertTrue(getSocketImpl(socket) == clientImpl);

        setSocketSocketImplFactory(() -> new CustomSocketImpl(false));
        try (ServerSocket ss = serverSocketToAccept(socket)) {
            expectThrows(IOException.class, ss::accept);
        } finally {
            setSocketSocketImplFactory(null);
            socket.close();
        }
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a custom SocketImpl,
     * the Socket to accept is created no SocketImpl.
     */
    public void testServerSocketAccept5a() throws IOException {
        SocketImpl serverImpl = new CustomSocketImpl(true);
        try (ServerSocket ss = new ServerSocket(serverImpl) { }) {
            ss.bind(loopbackSocketAddress());
            expectThrows(IOException.class, ss::accept);
        }
    }

    public void testServerSocketAccept5b() throws IOException {
        var socket = new Socket((SocketImpl) null) { };
        assertTrue(getSocketImpl(socket) == null);

        SocketImpl serverImpl = new CustomSocketImpl(true);
        try (ServerSocket ss = serverSocketToAccept(serverImpl, socket)) {
            expectThrows(IOException.class, ss::accept);
        } finally {
            socket.close();
        }
    }

    public void testServerSocketAccept5c() throws IOException {
        setServerSocketImplFactory(() -> new CustomSocketImpl(true));
        try (ServerSocket ss = new ServerSocket(0)) {
            expectThrows(IOException.class, ss::accept);
        } finally {
            setServerSocketImplFactory(null);
        }
    }

    public void testServerSocketAccept5d() throws IOException {
        var socket = new Socket((SocketImpl) null) { };
        assertTrue(getSocketImpl(socket) == null);

        setServerSocketImplFactory(() -> new CustomSocketImpl(true));
        try (ServerSocket ss = serverSocketToAccept(socket)) {
            expectThrows(IOException.class, ss::accept);
        } finally {
            setServerSocketImplFactory(null);
            socket.close();
        }
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a custom SocketImpl,
     * the Socket to accept is created with no SocketImpl, and there is a custom
     * client SocketImplFactory set.
     */
    public void testServerSocketAccept6() throws Exception {
        var socket = new Socket((SocketImpl) null) { };
        assertTrue(getSocketImpl(socket) == null);

        SocketImpl serverImpl = new CustomSocketImpl(true);
        SocketImplFactory clientFactory = () -> new CustomSocketImpl(false);
        serverSocketAccept(serverImpl, socket, clientFactory, (ss, s) -> {
            assertTrue(getSocketImpl(ss) == serverImpl);
            SocketImpl si = getSocketImpl(s);
            assertTrue(si instanceof CustomSocketImpl);
            checkFields(si);
        });
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a custom SocketImpl,
     * the Socket to accept is created with a SocketImpl that delegates to a
     * platform SocketImpl.
     */
    public void testServerSocketAccept7a() throws IOException {
        var socket = new Socket();
        SocketImpl si = getSocketImpl(socket);
        assertTrue(isSocksSocketImpl(si));
        SocketImpl delegate = getDelegate(si);
        assertTrue(isPlatformSocketImpl(delegate));

        SocketImpl serverImpl = new CustomSocketImpl(true);
        try (ServerSocket ss = serverSocketToAccept(serverImpl, socket)) {
            expectThrows(IOException.class, ss::accept);
        } finally {
            socket.close();
        }
    }

    public void testServerSocketAccept7b() throws IOException {
        var socket = new Socket();
        SocketImpl si = getSocketImpl(socket);
        assertTrue(isSocksSocketImpl(si));
        SocketImpl delegate = getDelegate(si);
        assertTrue(isPlatformSocketImpl(delegate));

        setServerSocketImplFactory(() -> new CustomSocketImpl(true));
        try (ServerSocket ss = serverSocketToAccept(socket)) {
            expectThrows(IOException.class, ss::accept);
        } finally {
            setServerSocketImplFactory(null);
            socket.close();
        }
    }

    /**
     * Test ServerSocket.accept. The ServerSocket uses a custom SocketImpl,
     * the Socket to accept is created with a custom SocketImpl.
     */
    public void testServerSocketAccept8() throws Exception {
        SocketImpl clientImpl = new CustomSocketImpl(false);
        Socket socket = new Socket(clientImpl) { };
        assertTrue(getSocketImpl(socket) == clientImpl);

        SocketImpl serverImpl = new CustomSocketImpl(true);
        SocketImplFactory clientFactory = () -> new CustomSocketImpl(false);
        serverSocketAccept(serverImpl, socket, clientFactory, (ss, s) -> {
            assertTrue(getSocketImpl(ss) == serverImpl);
            assertTrue(getSocketImpl(s) == clientImpl);
            checkFields(clientImpl);
        });
    }

    /**
     * Creates a ServerSocket that returns the given Socket from accept.
     * The consumer is invoked with the server socket and the accepted socket.
     */
    static void serverSocketAccept(Socket socket,
                                   BiConsumer<ServerSocket, Socket> consumer)
        throws IOException
    {
        Socket s1 = null;
        Socket s2 = null;
        try (ServerSocket ss = serverSocketToAccept(socket)) {
            s1 = new Socket(ss.getInetAddress(), ss.getLocalPort());
            s2 = ss.accept();
            consumer.accept(ss, s2);
        } finally {
            if (s1 != null) s1.close();
            if (s2 != null) s2.close();
        }
    }

    /**
     * Creates a ServerSocket that returns the given Socket from accept. The
     * given SocketImplFactory is set during the accept and the consumer is
     * invoked when the server socket and the accepted socket.
     */
    static void serverSocketAccept(Socket socket,
                                   SocketImplFactory factory,
                                   BiConsumer<ServerSocket, Socket> consumer)
        throws IOException
    {
        Socket s1 = null;
        Socket s2 = null;
        try (ServerSocket ss = serverSocketToAccept(socket)) {
            s1 = new Socket(ss.getInetAddress(), ss.getLocalPort());
            setSocketSocketImplFactory(factory);
            try {
                s2 = ss.accept();
            } finally {
                setSocketSocketImplFactory(null);
            }
            consumer.accept(ss, s2);
        } finally {
            if (s1 != null) s1.close();
            if (s2 != null) s2.close();
        }
    }

    /**
     * Creates a ServerSocket with a SocketImpl returns the given Socket from
     * accept. The given SocketImplFactory is set during the accept and the
     * consumer is invoked when the server socket and the accepted socket.
     */
    static void serverSocketAccept(SocketImpl impl,
                                   Socket socket,
                                   SocketImplFactory factory,
                                   BiConsumer<ServerSocket, Socket> consumer)
        throws IOException
    {
        Socket s1 = null;
        Socket s2 = null;
        try (ServerSocket ss = serverSocketToAccept(impl, socket)) {
            s1 = new Socket(ss.getInetAddress(), ss.getLocalPort());
            setSocketSocketImplFactory(factory);
            try {
                s2 = ss.accept();
            } finally {
                setSocketSocketImplFactory(null);
            }
            consumer.accept(ss, s2);
        } finally {
            if (s1 != null) s1.close();
            if (s2 != null) s2.close();
        }
    }

    /**
     * Returns a new InetSocketAddress with the loopback interface
     * and port 0.
     */
    static InetSocketAddress loopbackSocketAddress() {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        return new InetSocketAddress(loopback, 0);
    }

    /**
     * Returns a ServerSocket bound to a port on the loopback address
     */
    static ServerSocket boundServerSocket() throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.bind(loopbackSocketAddress());
        return ss;
    }

    /**
     * Creates a ServerSocket that returns the given Socket from accept.
     */
    static ServerSocket serverSocketToAccept(Socket s) throws IOException {
        ServerSocket ss = new ServerSocket() {
            @Override
            public Socket accept() throws IOException {
                implAccept(s);
                return s;
            }
        };
        ss.bind(loopbackSocketAddress());
        return ss;
    }

    /**
     * Creates a ServerSocket with a SocketImpl that returns the given Socket
     * from accept.
     */
    static ServerSocket serverSocketToAccept(SocketImpl impl, Socket s) throws IOException {
        ServerSocket ss = new ServerSocket(impl) {
            @Override
            public Socket accept() throws IOException {
                implAccept(s);
                return s;
            }
        };
        ss.bind(loopbackSocketAddress());
        return ss;
    }

    /**
     * Returns the socket's SocketImpl
     */
    static SocketImpl getSocketImpl(Socket s) {
        try {
            Field f = Socket.class.getDeclaredField("impl");
            f.setAccessible(true);
            return (SocketImpl) f.get(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the server socket's SocketImpl
     */
    static SocketImpl getSocketImpl(ServerSocket ss) {
        try {
            Field f = ServerSocket.class.getDeclaredField("impl");
            f.setAccessible(true);
            return (SocketImpl) f.get(ss);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the SocketImpl that the given SocketImpl delegates to
     */
    static SocketImpl getDelegate(SocketImpl si) {
        try {
            Class<?> clazz = Class.forName("java.net.DelegatingSocketImpl");
            Field f = clazz.getDeclaredField("delegate");
            f.setAccessible(true);
            return (SocketImpl) f.get(si);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the value of a SocketImpl field
     */
    static <T> T get(SocketImpl si, String name) {
        try {
            Field f = SocketImpl.class.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(si);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the value of SocketImpl field
     */
    static void set(SocketImpl si, String name, Object value) {
        try {
            Field f = SocketImpl.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(si, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the SocketImpl is a PlatformSocketImpl
     */
    static boolean isPlatformSocketImpl(SocketImpl si) {
        try {
            Class<?> clazz = Class.forName("sun.net.PlatformSocketImpl");
            return clazz.isInstance(si);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the SocketImpl is a SocksSocketImpl
     */
    static boolean isSocksSocketImpl(SocketImpl si) {
        try {
            Class<?> clazz = Class.forName("java.net.SocksSocketImpl");
            return clazz.isInstance(si);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the SocketImpl is a HttpConnectSocketImpl
     */
    static boolean isHttpConnectSocketImpl(SocketImpl si) {
        try {
            Class<?> clazz = Class.forName("java.net.HttpConnectSocketImpl");
            return clazz.isInstance(si);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Socket.setSocketImplFactory(SocketImplFactory)
     */
    static void setSocketSocketImplFactory(SocketImplFactory factory) {
        try {
            Field f = Socket.class.getDeclaredField("factory");
            f.setAccessible(true);
            f.set(null, factory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ServerSocket.setSocketFactory(SocketImplFactory)
     */
    static void setServerSocketImplFactory(SocketImplFactory factory) {
        try {
            Field f = ServerSocket.class.getDeclaredField("factory");
            f.setAccessible(true);
            f.set(null, factory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks the 4 protected fields of a SocketImpl to make sure that they
     * have been initialized.
     */
    static void checkFields(SocketImpl si) {
        FileDescriptor fd = get(si, "fd");
        InetAddress address = get(si, "address");
        int port = get(si, "port");
        int localport = get(si, "localport");
        assertTrue(fd.valid() && address != null && port != 0 && localport != 0);
    }

    /**
     * Custom SocketImpl that is layed on a SocketChannel or ServerSocketChannel
     */
    static class CustomSocketImpl extends SocketImpl {
        private final boolean server;
        private ServerSocketChannel ssc;
        private SocketChannel sc;

        CustomSocketImpl(boolean server) {
            this.server = server;
        }

        @Override
        protected void create(boolean stream) throws IOException {
            if (server) {
                ssc = ServerSocketChannel.open();
            } else {
                sc = SocketChannel.open();
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
        protected void connect(SocketAddress remote, int timeout) throws IOException {
            sc.connect(remote);
            super.address = ((InetSocketAddress) remote).getAddress();
            super.port = ((InetSocketAddress) remote).getPort();
        }

        @Override
        protected void bind(InetAddress address, int port) throws IOException {
            if (server) {
                ssc.bind(new InetSocketAddress(address, port));
                super.localport = ssc.socket().getLocalPort();
            } else {
                sc.bind(new InetSocketAddress(address, port));
                super.localport = sc.socket().getLocalPort();
            }
            super.address = address;
        }

        @Override
        protected void listen(int backlog) {
            // do nothing
        }

        @Override
        protected void accept(SocketImpl si) throws IOException {
            SocketChannel peer = ssc.accept();
            FileDescriptor fd;
            try {
                Class<?> clazz = Class.forName("sun.nio.ch.SocketChannelImpl");
                Field f = clazz.getDeclaredField("fd");
                f.setAccessible(true);
                fd = (FileDescriptor) f.get(peer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            set(si, "fd", fd);
            set(si, "address", peer.socket().getInetAddress());
            set(si, "port", peer.socket().getPort());
            set(si, "localport", peer.socket().getLocalPort());
        }

        @Override
        protected InputStream getInputStream() {
            throw new RuntimeException();
        }

        @Override
        protected OutputStream getOutputStream() {
            throw new RuntimeException();
        }

        @Override
        protected int available() {
            return 0;
        }

        @Override
        protected void close() {
        }

        @Override
        protected void sendUrgentData(int data) {
            throw new RuntimeException();
        }

        @Override
        public void setOption(int option, Object value) {
            throw new RuntimeException();
        }

        @Override
        public Object getOption(int option) {
            throw new RuntimeException();
        }
    }
}
