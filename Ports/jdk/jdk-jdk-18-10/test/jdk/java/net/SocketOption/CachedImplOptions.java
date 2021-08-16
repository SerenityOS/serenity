/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8241988
 * @summary Checks that the caching of options does not affect other impls
 * @run testng/othervm CachedImplOptions
 * @run testng/othervm -Djava.net.preferIPv4Stack=true CachedImplOptions
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.util.Set;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CachedImplOptions {

    @Test
    public void testDatagramSocket() throws IOException {
        try (var impl = new DatagramSocket()) {
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
        }
        try (var impl = new DatagramSocket(new FooDatagramSocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(FooDatagramSocketImpl.FOO_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(FooDatagramSocketImpl.FOO_OPTION));
        }
        try (var impl = new DatagramSocket(new BarDatagramSocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(BarDatagramSocketImpl.BAR_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(BarDatagramSocketImpl.BAR_OPTION));
        }
        try (var impl = new DatagramSocket(new BazDatagramSocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(BazDatagramSocketImpl.BAZ_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(BazDatagramSocketImpl.BAZ_OPTION));
        }
        try (var impl = new DatagramSocket()) {
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
        }
    }

    @Test
    public void testMulticastSocket() throws IOException {
        try (var impl = new MulticastSocket()) {
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
        }

        // Use the factory to inject an alternative impl
        DatagramSocket.setDatagramSocketImplFactory(() -> new FooDatagramSocketImpl());

        try (var impl = new MulticastSocket()) {
            assertEquals(impl.supportedOptions(), Set.of(FooDatagramSocketImpl.FOO_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(FooDatagramSocketImpl.FOO_OPTION));
        }
    }

    static class FooDatagramSocketImpl extends AbstractDatagramSocketImpl {
        public static final SocketOption<Boolean> FOO_OPTION = new SocketOption<>() {
            @Override public String name() { return "FOO_OPTION"; }
            @Override public Class<Boolean> type() { return Boolean.class; }
        };
        @Override public Set<SocketOption<?>> supportedOptions() { return Set.of(FOO_OPTION); }
    }

    static class BarDatagramSocketImpl extends AbstractDatagramSocketImpl {
        public static final SocketOption<Integer> BAR_OPTION = new SocketOption<>() {
            @Override public String name() { return "BAR_OPTION"; }
            @Override public Class<Integer> type() { return Integer.class; }
        };
        @Override public Set<SocketOption<?>> supportedOptions() { return Set.of(BAR_OPTION); }
    }

    static class BazDatagramSocketImpl extends AbstractDatagramSocketImpl {
        public static final SocketOption<Long> BAZ_OPTION = new SocketOption<>() {
            @Override public String name() { return "BAZ_OPTION"; }
            @Override public Class<Long> type() { return Long.class; }
        };
        @Override public Set<SocketOption<?>> supportedOptions() { return Set.of(BAZ_OPTION); }
    }

    static abstract class AbstractDatagramSocketImpl extends DatagramSocketImpl {

        @Override public Set<SocketOption<?>> supportedOptions() { return null; }
        @Override protected void create() throws SocketException { }
        @Override protected void bind(int lport, InetAddress laddr){ }
        @Override protected void send(DatagramPacket p) { }
        @Override protected int peek(InetAddress i) { return 0; }
        @Override protected int peekData(DatagramPacket p) { return 0; }
        @Override protected void receive(DatagramPacket p) { }
        @Override protected void setTTL(byte ttl) { }
        @Override protected byte getTTL() { return 0; }
        @Override protected void setTimeToLive(int ttl) { }
        @Override protected int getTimeToLive() { return 0; }
        @Override protected void join(InetAddress inetaddr)  { }
        @Override protected void leave(InetAddress inetaddr) { }
        @Override protected void joinGroup(SocketAddress x, NetworkInterface y) { }
        @Override protected void leaveGroup(SocketAddress x, NetworkInterface y) { }
        @Override protected void close() { }
        @Override public void setOption(int optID, Object value) { }
        @Override public Object getOption(int optID) { return null; }
    }

    // -- socket

    @Test
    public void testSocket() throws IOException {
        try (var impl = new Socket()) {
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
        }
        try (var impl = new Socket(new LarrySocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(LarrySocketImpl.LARRY_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(LarrySocketImpl.LARRY_OPTION));
        }
        try (var impl = new Socket(new CurlySocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(CurlySocketImpl.CURLY_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(CurlySocketImpl.CURLY_OPTION));
        }
        try (var impl = new Socket(new MoeSocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(MoeSocketImpl.MOE_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(MoeSocketImpl.MOE_OPTION));
        }
        try (var impl = new Socket()) {
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF));
        }
    }

    @Test
    public void testServerSocket() throws IOException {
        try (var impl = new ServerSocket()) {
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_RCVBUF));
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_RCVBUF));
        }
        try (var impl = new ServerSocket(new LarrySocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(LarrySocketImpl.LARRY_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(LarrySocketImpl.LARRY_OPTION));
        }
        try (var impl = new ServerSocket(new CurlySocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(CurlySocketImpl.CURLY_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(CurlySocketImpl.CURLY_OPTION));
        }
        try (var impl = new ServerSocket(new MoeSocketImpl()) {}) {
            assertEquals(impl.supportedOptions(), Set.of(MoeSocketImpl.MOE_OPTION));
            assertEquals(impl.supportedOptions(), Set.of(MoeSocketImpl.MOE_OPTION));
        }
        try (var impl = new ServerSocket()) {
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_RCVBUF));
            assertTrue(impl.supportedOptions().contains(StandardSocketOptions.SO_RCVBUF));
        }
    }

    static class LarrySocketImpl extends AbstractSocketImpl {
        public static final SocketOption<Boolean> LARRY_OPTION = new SocketOption<>() {
            @Override public String name() { return "LARRY_OPTION"; }
            @Override public Class<Boolean> type() { return Boolean.class; }
        };
        @Override public Set<SocketOption<?>> supportedOptions() { return Set.of(LARRY_OPTION); }
    }

    static class CurlySocketImpl extends AbstractSocketImpl {
        public static final SocketOption<Integer> CURLY_OPTION = new SocketOption<>() {
            @Override public String name() { return "CURLY_OPTION"; }
            @Override public Class<Integer> type() { return Integer.class; }
        };
        @Override public Set<SocketOption<?>> supportedOptions() { return Set.of(CURLY_OPTION); }
    }

    static class MoeSocketImpl extends AbstractSocketImpl {
        public static final SocketOption<Long> MOE_OPTION = new SocketOption<>() {
            @Override public String name() { return "MOE_OPTION"; }
            @Override public Class<Long> type() { return Long.class; }
        };
        @Override public Set<SocketOption<?>> supportedOptions() { return Set.of(MOE_OPTION); }
    }

    static abstract class AbstractSocketImpl extends SocketImpl {

        @Override protected void create(boolean stream) { }
        @Override protected void connect(String host, int port) { }
        @Override protected void connect(InetAddress address, int port) { }
        @Override protected void connect(SocketAddress address, int timeout) { }
        @Override protected void bind(InetAddress host, int port) { }
        @Override protected void listen(int backlog) { }
        @Override protected void accept(SocketImpl s) { }
        @Override protected InputStream getInputStream() { return null; }
        @Override protected OutputStream getOutputStream() { return null;  }
        @Override protected int available() { return 0; }
        @Override protected void close() { }
        @Override protected void sendUrgentData(int data) { }
        @Override public void setOption(int optID, Object value) { }
        @Override public Object getOption(int optID) { return null; }
    }
}
