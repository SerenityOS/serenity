/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8213418
 * @summary Ensure correct impl supported socket options
 * @run testng ImplSupportedOptions
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.util.Set;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ImplSupportedOptions {

    @Test
    public void socketSupportedOptions() throws Exception {
        Socket s = new Socket();
        Set<?> standardOptions = s.supportedOptions();
        assertTrue(standardOptions.contains(StandardSocketOptions.SO_LINGER),
                   "Expected SO_LINGER, in:" + standardOptions);
        assertEquals(standardOptions, s.supportedOptions());
        assertEquals(standardOptions, s.supportedOptions());

        s = new DummySocket();
        Set<?> dummyOptions = s.supportedOptions();
        assertEquals(dummyOptions.size(), 1);
        assertTrue(dummyOptions.contains(DummySocketImpl.SOCKET_OPT));
        assertEquals(dummyOptions, s.supportedOptions());
        assertEquals(dummyOptions, s.supportedOptions());

        s = new Socket();
        standardOptions = s.supportedOptions();
        assertTrue(standardOptions.contains(StandardSocketOptions.SO_LINGER),
                   "Expected SO_LINGER, in:" + standardOptions);
        assertEquals(standardOptions, s.supportedOptions());
        assertEquals(standardOptions, s.supportedOptions());

        s = new DummySocket();
        dummyOptions = s.supportedOptions();
        assertEquals(dummyOptions.size(), 1);
        assertTrue(dummyOptions.contains(DummySocketImpl.SOCKET_OPT));
        assertEquals(dummyOptions, s.supportedOptions());
        assertEquals(dummyOptions, s.supportedOptions());
    }

    @Test
    public void serverSocketSupportedOptions() throws Exception {
        ServerSocket s = new ServerSocket();
        Set<?> standardOptions = s.supportedOptions();
        assertTrue(standardOptions.contains(StandardSocketOptions.SO_REUSEADDR),
                   "Expected SO_REUSEADDR, in:" + standardOptions);
        assertEquals(standardOptions, s.supportedOptions());
        assertEquals(standardOptions, s.supportedOptions());

        s = new DummyServerSocket();
        Set<?> dummyOptions = s.supportedOptions();
        assertEquals(dummyOptions.size(), 1);
        assertTrue(dummyOptions.contains(DummySocketImpl.SOCKET_OPT));
        assertEquals(dummyOptions, s.supportedOptions());
        assertEquals(dummyOptions, s.supportedOptions());

        s = new ServerSocket();
        standardOptions = s.supportedOptions();
        assertTrue(standardOptions.contains(StandardSocketOptions.SO_REUSEADDR),
                   "Expected SO_REUSEADDR, in:" + standardOptions);
        assertEquals(standardOptions, s.supportedOptions());
        assertEquals(standardOptions, s.supportedOptions());

        s = new DummyServerSocket();
        dummyOptions = s.supportedOptions();
        assertEquals(dummyOptions.size(), 1);
        assertTrue(dummyOptions.contains(DummySocketImpl.SOCKET_OPT));
        assertEquals(dummyOptions, s.supportedOptions());
        assertEquals(dummyOptions, s.supportedOptions());
    }

    static class DummySocket extends Socket {
        DummySocket() throws IOException  {
            super(new DummySocketImpl());
        }
    }

    static class DummyServerSocket extends ServerSocket {
        DummyServerSocket() throws IOException  {
            super(new DummySocketImpl());
        }
    }

    static class DummySocketImpl extends SocketImpl {

        public static final SocketOption<String> SOCKET_OPT =
                new ImplSocketOption<>("SOCKET_OPT", String.class);

        private static class ImplSocketOption<T> implements SocketOption<T> {
            private final String name;
            private final Class<T> type;
            ImplSocketOption(String name, Class<T> type) {
                this.name = name;
                this.type = type;
            }
            @Override public String name() { return name; }
            @Override public Class<T> type() { return type; }
            @Override public String toString() { return name; }
        }

        private final Set<SocketOption<?>> SO = Set.of(SOCKET_OPT);

        @Override
        public Set<SocketOption<?>> supportedOptions() { return SO; }

        // ---

        @Override
        protected void create(boolean stream) throws IOException { }

        @Override
        protected void connect(String host, int port) throws IOException { }

        @Override
        protected void connect(InetAddress address, int port) throws IOException { }

        @Override
        protected void connect(SocketAddress address, int timeout) throws IOException { }

        @Override
        protected void bind(InetAddress host, int port) throws IOException { }

        @Override
        protected void listen(int backlog) throws IOException { }

        @Override
        protected void accept(SocketImpl s) throws IOException { }

        @Override
        protected InputStream getInputStream() throws IOException { return null; }

        @Override
        protected OutputStream getOutputStream() throws IOException { return null; }

        @Override
        protected int available() throws IOException { return 0; }

        @Override
        protected void close() throws IOException { }

        @Override
        protected void sendUrgentData(int data) throws IOException { }

        @Override
        public void setOption(int optID, Object value) throws SocketException { }

        @Override
        public Object getOption(int optID) throws SocketException { return null; }
    }
}
