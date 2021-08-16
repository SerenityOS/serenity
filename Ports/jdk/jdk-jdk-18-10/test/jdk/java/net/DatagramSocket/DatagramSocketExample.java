/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237352
 * @summary Verifies that the examples using DatagramSocket for
 * sending and receiving multicast datagrams are functional.
 * See "Multicasting with DatagramSocket" API note in
 * DatagramSocket.java
 *
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.net.IPSupport
 * @run main/othervm DatagramSocketExample
 * @run main/othervm -Djava.net.preferIPv4Stack=true DatagramSocketExample
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardProtocolFamily.INET6;
import static java.net.StandardSocketOptions.IP_MULTICAST_IF;
import static java.net.StandardSocketOptions.IP_MULTICAST_LOOP;
import static java.net.StandardSocketOptions.IP_MULTICAST_TTL;
import static java.net.StandardSocketOptions.SO_REUSEADDR;

public class DatagramSocketExample {
    static final ProtocolFamily UNSPEC = () -> "UNSPEC";

    public static void main(String[] args) throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();

        // IPv4 and IPv6 interfaces that support multicasting
        NetworkConfiguration config = NetworkConfiguration.probe();
        List<NetworkInterface> ip4MulticastInterfaces = config.ip4MulticastInterfaces()
                .collect(Collectors.toList());
        List<NetworkInterface> ip6MulticastInterfaces = config.ip6MulticastInterfaces()
                .collect(Collectors.toList());

        // multicast groups used for the test
        InetAddress ip4Group = InetAddress.getByName("225.4.5.6");
        InetAddress ip6Group = InetAddress.getByName("ff02::a");

        for (NetworkInterface ni : ip4MulticastInterfaces) {
            test(INET, ip4Group, ni);
            if (IPSupport.hasIPv6()) {
                test(UNSPEC, ip4Group, ni);
                test(INET6, ip4Group, ni);
            }
        }
        for (NetworkInterface ni : ip6MulticastInterfaces) {
            test(UNSPEC, ip6Group, ni);
            test(INET6, ip6Group, ni);
        }
    }

    static void test(ProtocolFamily family, InetAddress mcastaddr, NetworkInterface ni)
        throws IOException
    {
        System.out.format("Test family=%s, multicast group=%s, interface=%s%n",
            family.name(), mcastaddr, ni.getName());

        // An instance of DatagramSocket can also be used to receive
        // multicast datagram packets. A DatagramSocket that is created
        // with the intent of receiving multicast datagrams should be
        // created unbound. Before binding the socket, setReuseAddress(true)
        // should be configured:
        try (DatagramSocket socket = new DatagramSocket(null); // unbound
             DatagramSocket sender = new DatagramSocket(new InetSocketAddress(0))) {

            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(0));

            // joinGroup
            // InetAddress mcastaddr = InetAddress.getByName("228.5.6.7");
            InetSocketAddress group = new InetSocketAddress(mcastaddr, 0);
            // NetworkInterface netIf = NetworkInterface.getByName("en0");
            NetworkInterface netIf = ni;

            socket.joinGroup(group, netIf);
            try {
                byte[] rcvBytes = new byte[1024]; // up to 1024 bytes
                DatagramPacket packet = new DatagramPacket(rcvBytes, rcvBytes.length);

                // An instance of DatagramSocket can be used to send or receive
                // multicast datagram packets. Before sending out datagram packets,
                // the default outgoing interface for sending datagram packets
                // should be configured first using setOption and
                // StandardSocketOptions.IP_MULTICAST_IF:

                // DatagramSocket sender = new DatagramSocket(new InetSocketAddress(0));
                // NetworkInterface outgoingIf = NetworkInterface.getByName("en0");
                NetworkInterface outgoingIf = ni;
                sender.setOption(StandardSocketOptions.IP_MULTICAST_IF, outgoingIf);

                // optionally configure multicast TTL
                int ttl = 1; // a number betwen 0 and 255
                sender.setOption(StandardSocketOptions.IP_MULTICAST_TTL, ttl);

                // send a packet to a multicast group
                byte[] msgBytes = "Hello".getBytes(StandardCharsets.UTF_8);
                int port = socket.getLocalPort();
                InetSocketAddress dest = new InetSocketAddress(mcastaddr, port);
                DatagramPacket hi = new DatagramPacket(msgBytes, msgBytes.length, dest);
                sender.send(hi);

                socket.receive(packet);
                byte[] bytes = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                assertTrue("Hello".equals(new String(bytes, StandardCharsets.UTF_8)));
            } finally {
                // eventually leave group
                socket.leaveGroup(group, netIf);
            }
        }

    }

    static void assertTrue(boolean e) {
        if (!e) throw new RuntimeException();
    }

    interface ThrowableRunnable {
        void run() throws Exception;
    }

    static void assertThrows(java.lang.Class<?> exceptionClass, ThrowableRunnable task) {
        try {
            task.run();
            throw new RuntimeException("Exception not thrown");
        } catch (Exception e) {
            if (!exceptionClass.isInstance(e)) {
                throw new RuntimeException("expected: " + exceptionClass + ", actual: " + e);
            }
        }
    }
}
