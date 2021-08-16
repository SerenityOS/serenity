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
 * @bug 8216417
 * @summary cleanup of IPv6 scope-id handling
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 * @run main/othervm Scoping
 */

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import jdk.test.lib.NetworkConfiguration;

public class Scoping {

    interface ThrowingConsumer<T> {
        public void accept(T t) throws Exception;
    }

    static List<ThrowingConsumer<InetSocketAddress>> targets = List.of(
        /* 0 */  (a) -> {Socket s = new Socket(); s.bind(a);s.close();},
        /* 1 */  (a) -> {ServerSocket s = new ServerSocket(); s.bind(a);s.close();},
        /* 2 */  (a) -> {DatagramSocket s = new DatagramSocket(null); s.bind(a); s.close();},
        /* 3 */  (a) -> {MulticastSocket s = new MulticastSocket(null); s.bind(a); s.close();},
        /* 4 */  (a) -> {SocketChannel s = SocketChannel.open(); s.bind(a);s.close();},
        /* 5 */  (a) -> {ServerSocketChannel s = ServerSocketChannel.open(); s.bind(a);s.close();},
        /* 6 */  (a) -> {DatagramChannel s = DatagramChannel.open(); s.bind(a); s.close();},
        /* 7 */  (a) -> {SocketChannel s = SocketChannel.open(); s.socket().bind(a);s.close();},
        /* 8 */  (a) -> {ServerSocketChannel s = ServerSocketChannel.open(); s.socket().bind(a);s.close();},
        /* 9 */  (a) -> {DatagramChannel s = DatagramChannel.open(); s.socket().bind(a); s.close();},
        /* 10 */ (a) -> {socketTest(a);},
        /* 11 */ (a) -> {dgSocketTest(a, false);},
        /* 12 */ (a) -> {dgSocketTest(a, true);},
        /* 13 */ (a) -> {dgChannelTest(a, false);},
        /* 14 */ (a) -> {dgChannelTest(a, true);}
    );

    static List<Inet6Address> getLinkLocalAddrs() throws IOException {
        return NetworkConfiguration.probe()
                   .ip6Addresses()
                   .filter(Inet6Address::isLinkLocalAddress)
                   .collect(Collectors.toList());
    }

    static void compare(InetSocketAddress a, InetSocketAddress b) {
        Inet6Address a1 = (Inet6Address)a.getAddress();
        Inet6Address b1 = (Inet6Address)b.getAddress();
        compare (a1, b1);
    }

    static void compare(InetAddress a, InetAddress b) {
        Inet6Address a1 = (Inet6Address)a;
        Inet6Address b1 = (Inet6Address)b;
        if (!a1.equals(b1)) {
            System.out.printf("%s != %s\n", a1, b1);
            throw new RuntimeException("Addresses not the same");
        }

        if (!a1.getHostAddress().equals(b1.getHostAddress())) {
            System.out.printf("%s != %s\n", a1, b1);
            if (a1.getScopeId() != b1.getScopeId())
                throw new RuntimeException("Scope ids not the same");
        }
    }

    static void socketTest(InetSocketAddress a) throws Exception {
        System.out.printf("socketTest: address %s\n", a);
        try (ServerSocket server = new ServerSocket(0, 5, a.getAddress())) {
            InetAddress saddr = server.getInetAddress();
            int port = server.getLocalPort();
            Socket client = new Socket(saddr, port);
            compare(client.getInetAddress(), saddr);
            try {
                client.close();
            } catch (IOException e) {}
        }
    }

    static void dgSocketTest(InetSocketAddress a, boolean connect) throws Exception {
        try (DatagramSocket s = new DatagramSocket(null)) {
            System.out.println("dgSocketTest: " + a);
            s.bind(a);
            String t = "Hello world";
            DatagramPacket rx = new DatagramPacket(new byte[128], 128);
            int port = s.getLocalPort();
            InetSocketAddress dest = new InetSocketAddress(a.getAddress(), port);
            DatagramPacket tx = new DatagramPacket(t.getBytes("ISO8859_1"), t.length(), dest);
            if (connect) {
                s.connect(dest);
                System.out.println("dgSocketTest: connect remote addr = " + s.getRemoteSocketAddress());
                compare(a, (InetSocketAddress)s.getRemoteSocketAddress());
            }
            s.send(tx);
            s.receive(rx);
            String t1 = new String(rx.getData(), rx.getOffset(), rx.getLength(), "ISO8859_1");
            if (!t1.equals(t))
                throw new RuntimeException("Packets not equal");
        }
    }

    static void dgChannelTest(InetSocketAddress a, boolean connect) throws Exception {
        try (DatagramChannel s = DatagramChannel.open()) {
            System.out.println("dgChannelTest: " + a);
            s.bind(a);
            String t = "Hello world";
            ByteBuffer rx = ByteBuffer.allocate(128);
            int port = ((InetSocketAddress)s.getLocalAddress()).getPort();
            InetSocketAddress dest = new InetSocketAddress(a.getAddress(), port);
            ByteBuffer tx = ByteBuffer.wrap(t.getBytes("ISO8859_1"));
            if (connect) {
                s.connect(dest);
                System.out.println("dgChannelTest: connect remote addr = " + s.getRemoteAddress());
                compare(a, (InetSocketAddress)s.getRemoteAddress());
            }
            s.send(tx, dest);
            s.receive(rx);
            rx.flip();
            String t1 = new String(rx.array(), 0, rx.limit(), "ISO8859_1");
            System.out.printf("rx : %s, data: %s\n", rx, t1);
            if (!t1.equals(t))
                throw new RuntimeException("Packets not equal");
        }
    }

    static Inet6Address stripScope(Inet6Address address) {
        byte[] bytes = address.getAddress();
        InetAddress result = null;
        try {
            result = InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            assert false;
        }
        return (Inet6Address)result;
    }

    public static void main(String[] args) throws Exception {
        for (Inet6Address address : getLinkLocalAddrs()) {
            Inet6Address stripped = stripScope(address);
            InetSocketAddress s1 = new InetSocketAddress(address, 0);
            InetSocketAddress s2 = new InetSocketAddress(stripped, 0);
            System.out.println("Trying: " + address);
            int count = 0, success = 0;
            for (ThrowingConsumer<InetSocketAddress> target : targets) {
                try {
                    target.accept(s1);
                    System.out.println("target " + count + " OK");
                    // if that succeeds try s2 (the actual test)
                    try {
                        target.accept(s2);
                        success++;
                    } catch (IOException ee) {
                        String msg = "Failed: " + s2.toString() + "count: " + Integer.toString(count);
                        throw new RuntimeException (msg);
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    // OK
                }
                count++;
            }
            System.out.println("Succeeded with " + success + " binds");
        }
    }
}
