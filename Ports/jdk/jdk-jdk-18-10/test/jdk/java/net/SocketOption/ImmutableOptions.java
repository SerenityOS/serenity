/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148609
 * @library /test/lib
 * @summary Assert that the set of socket options are immutable
 * @run testng/othervm ImmutableOptions
 * @run testng/othervm -Djava.net.preferIPv4Stack=true ImmutableOptions
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Set;

import jdk.test.lib.net.IPSupport;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ImmutableOptions {

    @BeforeTest
    void setupServerSocketFactory() throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();
        ServerSocket.setSocketFactory(new ServerSocketImplFactory());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void socketThrows() throws IOException {
        CustomSocketImpl impl = new CustomSocketImpl();
        Socket socket = new CustomSocket(impl);
        socket.supportedOptions().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void socketImplThrows() throws IOException {
        CustomSocketImpl impl = new CustomSocketImpl();
        impl.supportedOptions().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void serverSocketThrows() throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.supportedOptions().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void serverSocketImplThrows() throws IOException {
        ServerSocket ss = new ServerSocket();
        ServerSocketImplFactory.mostRecentlyCreated.supportedOptions().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void datagramSocketThrows() throws IOException {
        CustomDatagramSocketImpl impl = new CustomDatagramSocketImpl();
        DatagramSocket socket = new CustomDatagramSocket(impl);
        socket.supportedOptions().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void datagramSocketImplThrows() throws IOException {
        CustomDatagramSocketImpl impl = new CustomDatagramSocketImpl();
        impl.supportedOptions().clear();
    }


    // Socket descendants
    static class CustomSocket extends Socket {
        public CustomSocket(SocketImpl impl) throws IOException {
            super(impl);
        }
    }

    static class CustomDatagramSocket extends DatagramSocket {
        public CustomDatagramSocket(DatagramSocketImpl impl) {
            super(impl);
        }
    }

    static class ServerSocketImplFactory implements SocketImplFactory {
        static volatile CustomSocketImpl mostRecentlyCreated;

        @Override public SocketImpl createSocketImpl() {
            return mostRecentlyCreated = new CustomSocketImpl();
        }
    }

    // Custom impl's
    static class CustomSocketImpl extends SocketImpl {
        // The only method interesting to this test.
        @Override public Set<SocketOption<?>> supportedOptions() {
            return super.supportedOptions();
        }

        public void create(boolean stream) throws IOException { }

        public void connect(String host, int port) throws IOException { }

        public void connect(InetAddress addr, int port) throws IOException { }

        public void connect(SocketAddress addr, int timeout) throws IOException { }

        public void bind(InetAddress host, int port) throws IOException { }

        public void listen(int backlog) throws IOException { }

        public void accept(SocketImpl s) throws IOException { }

        public InputStream getInputStream() throws IOException { return null; }

        public OutputStream getOutputStream() throws IOException { return null; }

        public int available() throws IOException { return 0; }

        public void close() throws IOException { }

        public void sendUrgentData(int data) throws IOException { }

        public Object getOption(int i) throws SocketException { return null; }

        public void setOption(int i, Object o) throws SocketException { }
    }

    static class CustomDatagramSocketImpl extends DatagramSocketImpl {
        // The only method interesting to this test.
        @Override public Set<SocketOption<?>> supportedOptions() {
            return super.supportedOptions();
        }

        protected void create() throws SocketException { }

        protected void bind(int lport, InetAddress laddr) throws SocketException { }

        protected void send(DatagramPacket p) throws IOException { }

        protected int peek(InetAddress i) throws IOException { return 0; }

        protected int peekData(DatagramPacket p) throws IOException { return 0; }

        protected void receive(DatagramPacket p) throws IOException { }

        protected void setTTL(byte ttl) throws IOException { }

        protected byte getTTL() throws IOException { return 0; }

        protected void setTimeToLive(int ttl) throws IOException { }

        protected int getTimeToLive() throws IOException { return 0; }

        protected void join(InetAddress inetaddr) throws IOException { }

        protected void leave(InetAddress inetaddr) throws IOException { }

        protected void joinGroup(SocketAddress x, NetworkInterface y)
            throws IOException { }

        protected void leaveGroup(SocketAddress x, NetworkInterface y)
            throws IOException { }

        protected void close() { }

        public void setOption(int optID, Object value) throws SocketException { }

        public Object getOption(int optID) throws SocketException { return null; }
    }
}
