/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8236184
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.net.IPSupport
 * @requires (os.family == "linux") | (os.family == "windows")
 * @run main Loopback
 * @run main/othervm -Djava.net.preferIPv4Stack=true Loopback
 * @summary Test the IP_MULTICAST_LOOP option
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.stream.Collectors;
import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardProtocolFamily.INET6;
import static java.net.StandardSocketOptions.IP_MULTICAST_IF;
import static java.net.StandardSocketOptions.IP_MULTICAST_LOOP;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

public class Loopback {

    static final ProtocolFamily UNSPEC = () -> "UNSPEC";

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        // IPv4 and IPv6 interfaces that support multicasting
        NetworkConfiguration config = NetworkConfiguration.probe();
        List<NetworkInterface> ip4MulticastInterfaces = config.ip4MulticastInterfaces()
                .collect(Collectors.toList());
        List<NetworkInterface> ip6MulticastInterfaces = config.ip6MulticastInterfaces()
                .collect(Collectors.toList());

        // IPv4 multicast group
        InetAddress ip4Group = InetAddress.getByName("225.4.5.6");
        for (NetworkInterface ni : ip4MulticastInterfaces) {
            test(UNSPEC, ip4Group, ni);
            test(INET, ip4Group, ni);
            if (IPSupport.hasIPv6()) {
                test(INET6, ip4Group, ni);
            }
        }

        // IPv6 multicast group
        InetAddress ip6Group = InetAddress.getByName("ff02::a");
        for (NetworkInterface ni : ip6MulticastInterfaces) {
            test(UNSPEC, ip6Group, ni);
            test(INET6, ip6Group, ni);
        }
    }

    /**
     * Joins a multicast group and send datagrams to that group with both
     * IP_MULTICAST_LOOP enabled and disabled.
     */
    static void test(ProtocolFamily family, InetAddress group, NetworkInterface ni)
        throws IOException
    {
        System.out.format("\n%s socket\n", family.name());
        DatagramChannel dc;
        if (family == UNSPEC) {
            dc = DatagramChannel.open();
        } else {
            dc = DatagramChannel.open(family);
        }
        try (dc) {
            dc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            dc.bind(new InetSocketAddress(0));
            int localPort = dc.socket().getLocalPort();
            SocketAddress target = new InetSocketAddress(group, localPort);

            System.out.format("join %s @ %s%n", group.getHostAddress(), ni.getName());
            dc.join(group, ni);

            System.out.format("set outgoing multicast interface to %s%n", ni.getName());
            dc.setOption(IP_MULTICAST_IF, ni);

            // -- IP_MULTICAST_LOOP enabled --

            assertTrue(dc.getOption(IP_MULTICAST_LOOP), "IP_MULTICAST_LOOP not enabled");
            System.out.println("IP_MULTICAST_LOOP enabled");

            // send datagram to multicast group
            System.out.format("send %s -> %s%n", dc.getLocalAddress(), target);
            ByteBuffer src = ByteBuffer.wrap("hello".getBytes("UTF-8"));
            dc.send(src, target);

            // receive datagram sent to multicast group
            ByteBuffer dst = ByteBuffer.allocate(100);
            int senderPort;
            do {
                dst.clear();
                SocketAddress sender = dc.receive(dst);
                System.out.format("received %s from %s%n", dst, sender);
                senderPort = ((InetSocketAddress) sender).getPort();
            } while (senderPort != localPort);
            dst.flip();
            assertTrue(dst.remaining() == src.capacity(), "Unexpected message size");

            // -- IP_MULTICAST_LOOP disabled --

            dc.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
            System.out.println("IP_MULTICAST_LOOP disabled");

            // send datagram to multicast group
            System.out.format("send %s -> %s%n", dc.getLocalAddress(), target);
            src.clear();
            dc.send(src, target);

            // test that we don't receive the datagram sent to multicast group
            dc.configureBlocking(false);
            try (Selector sel = Selector.open()) {
                dc.register(sel, SelectionKey.OP_READ);
                boolean done = false;
                while (!done) {
                    int n = sel.select(3000);
                    System.out.format("selected %d%n", n);
                    if (n == 0) {
                        // timeout, no datagram received
                        done = true;
                    } else {
                        sel.selectedKeys().clear();
                        SocketAddress sender = dc.receive(dst);
                        if (sender != null) {
                            System.out.format("received %s from %s%n", dst, sender);
                            senderPort = ((InetSocketAddress) sender).getPort();
                            assertTrue(senderPort != localPort, "Unexpected message");
                        }
                    }
                }
            }
        }
    }

    static void assertTrue(boolean e, String msg) {
        if (!e) throw new RuntimeException(msg);
    }
}
