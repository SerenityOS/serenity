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

/* @test
 * @bug 8236925 8241786
 * @summary Test DatagramChannel socket adaptor as a MulticastSocket
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.net.IPSupport
 * @run main/othervm AdaptorMulticasting
 * @run main/othervm -Djava.net.preferIPv4Stack=true AdaptorMulticasting
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.SocketOption;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static java.net.StandardSocketOptions.*;
import static java.net.StandardProtocolFamily.*;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

public class AdaptorMulticasting {
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

    static void test(ProtocolFamily family, InetAddress group, NetworkInterface ni)
        throws IOException
    {
        System.out.format("Test family=%s, multicast group=%s, interface=%s%n",
            family.name(), group, ni.getName());

        // test 1-arg joinGroup/leaveGroup
        try (MulticastSocket s = create(family)) {
            testJoinGroup1(family, s, group, ni);
        }

        // test 2-arg joinGroup/leaveGroup
        try (MulticastSocket s = create(family)) {
            testJoinGroup2(family, s, group, ni);
        }

        // test socket options
        try (MulticastSocket s = create(family)) {
            testNetworkInterface(s, ni);
            testTimeToLive(s);
            testLoopbackMode(s);
        }
    }

    /**
     * Creates a MulticastSocket. The SO_REUSEADDR socket option is set and it
     * is bound to the wildcard address.
     */
    static MulticastSocket create(ProtocolFamily family) throws IOException {
        DatagramChannel dc = (family == UNSPEC)
                ? DatagramChannel.open()
                : DatagramChannel.open(family);
        try {
            dc.setOption(SO_REUSEADDR, true).bind(new InetSocketAddress(0));
        } catch (IOException ioe) {
            dc.close();
            throw ioe;
        }
        return (MulticastSocket) dc.socket();
    }

    /**
     * Test 1-arg joinGroup/leaveGroup
     */
    static void testJoinGroup1(ProtocolFamily family,
                               MulticastSocket s,
                               InetAddress group,
                               NetworkInterface ni) throws IOException {

        System.out.format("testJoinGroup1: local socket address: %s%n", s.getLocalSocketAddress());

        // check network interface not set
        assertTrue(s.getOption(IP_MULTICAST_IF) == null);

        // join
        s.joinGroup(group);

        // join should not set the outgoing multicast interface
        assertTrue(s.getOption(IP_MULTICAST_IF) == null);

        // already a member (exception not specified)
        assertThrows(SocketException.class, () -> s.joinGroup(group));

        // leave
        s.leaveGroup(group);

        // not a member (exception not specified)
        assertThrows(SocketException.class, () -> s.leaveGroup(group));

        // join/leave with outgoing multicast interface set and check that
        // multicast datagrams can be sent and received
        s.setOption(IP_MULTICAST_IF, ni);
        s.joinGroup(group);
        testSendReceive(s, group);
        s.leaveGroup(group);
        testSendNoReceive(s, group);

        // not a multicast address
        var localHost = InetAddress.getLocalHost();
        assertThrows(SocketException.class, () -> s.joinGroup(localHost));
        assertThrows(SocketException.class, () -> s.leaveGroup(localHost));

        // IPv4 socket cannot join IPv6 group (exception not specified)
        if (family == INET) {
            InetAddress ip6Group = InetAddress.getByName("ff02::a");
            assertThrows(SocketException.class, () -> s.joinGroup(ip6Group));
            assertThrows(SocketException.class, () -> s.leaveGroup(ip6Group));
        }

        // null (exception not specified)
        assertThrows(NullPointerException.class, () -> s.joinGroup(null));
        assertThrows(NullPointerException.class, () -> s.leaveGroup(null));
    }

    /**
     * Test 2-arg joinGroup/leaveGroup
     */
    static void testJoinGroup2(ProtocolFamily family,
                               MulticastSocket s,
                               InetAddress group,
                               NetworkInterface ni) throws IOException {

        System.out.format("testJoinGroup2: local socket address: %s%n", s.getLocalSocketAddress());

        // check network interface not set
        assertTrue(s.getOption(IP_MULTICAST_IF) == null);

        // join on default interface
        s.joinGroup(new InetSocketAddress(group, 0), null);

        // join should not change the outgoing multicast interface
        assertTrue(s.getOption(IP_MULTICAST_IF) == null);

        // already a member (exception not specified)
        assertThrows(SocketException.class,
                     () -> s.joinGroup(new InetSocketAddress(group, 0), null));

        // leave
        s.leaveGroup(new InetSocketAddress(group, 0), null);

        // not a member (exception not specified)
        assertThrows(SocketException.class,
                     () -> s.leaveGroup(new InetSocketAddress(group, 0), null));

        // join on specified interface
        s.joinGroup(new InetSocketAddress(group, 0), ni);

        // join should not change the outgoing multicast interface
        assertTrue(s.getOption(IP_MULTICAST_IF) == null);

        // already a member (exception not specified)
        assertThrows(SocketException.class,
                     () -> s.joinGroup(new InetSocketAddress(group, 0), ni));

        // leave
        s.leaveGroup(new InetSocketAddress(group, 0), ni);

        // not a member (exception not specified)
        assertThrows(SocketException.class,
                     () -> s.leaveGroup(new InetSocketAddress(group, 0), ni));

        // join/leave with outgoing multicast interface set and check that
        // multicast datagrams can be sent and received
        s.setOption(IP_MULTICAST_IF, ni);
        s.joinGroup(new InetSocketAddress(group, 0), null);
        testSendReceive(s, group);
        s.leaveGroup(new InetSocketAddress(group, 0), null);
        testSendNoReceive(s, group);
        s.joinGroup(new InetSocketAddress(group, 0), ni);
        testSendReceive(s, group);
        s.leaveGroup(new InetSocketAddress(group, 0), ni);
        testSendNoReceive(s, group);

        // not a multicast address
        var localHost = InetAddress.getLocalHost();
        assertThrows(SocketException.class,
                     () -> s.joinGroup(new InetSocketAddress(localHost, 0), null));
        assertThrows(SocketException.class,
                     () -> s.leaveGroup(new InetSocketAddress(localHost, 0), null));
        assertThrows(SocketException.class,
                     () -> s.joinGroup(new InetSocketAddress(localHost, 0), ni));
        assertThrows(SocketException.class,
                     () -> s.leaveGroup(new InetSocketAddress(localHost, 0), ni));

        // not an InetSocketAddress
        var customSocketAddress = new SocketAddress() { };
        assertThrows(IllegalArgumentException.class,
                     () -> s.joinGroup(customSocketAddress, null));
        assertThrows(IllegalArgumentException.class,
                     () -> s.leaveGroup(customSocketAddress, null));
        assertThrows(IllegalArgumentException.class,
                     () -> s.joinGroup(customSocketAddress, ni));
        assertThrows(IllegalArgumentException.class,
                     () -> s.leaveGroup(customSocketAddress, ni));

        // IPv4 socket cannot join IPv6 group
        if (family == INET) {
            InetAddress ip6Group = InetAddress.getByName("ff02::a");
            assertThrows(IllegalArgumentException.class,
                         () -> s.joinGroup(new InetSocketAddress(ip6Group, 0), null));
            assertThrows(IllegalArgumentException.class,
                         () -> s.joinGroup(new InetSocketAddress(ip6Group, 0), ni));

            // not a member of IPv6 group (exception not specified)
            assertThrows(SocketException.class,
                         () -> s.leaveGroup(new InetSocketAddress(ip6Group, 0), null));
            assertThrows(SocketException.class,
                         () -> s.leaveGroup(new InetSocketAddress(ip6Group, 0), ni));
        }

        // null
        assertThrows(IllegalArgumentException.class, () -> s.joinGroup(null, null));
        assertThrows(IllegalArgumentException.class, () -> s.leaveGroup(null, null));
        assertThrows(IllegalArgumentException.class, () -> s.joinGroup(null, ni));
        assertThrows(IllegalArgumentException.class, () -> s.leaveGroup(null, ni));
    }

    /**
     * Test getNetworkInterface/setNetworkInterface/getInterface/setInterface
     * and IP_MULTICAST_IF socket option.
     */
    static void testNetworkInterface(MulticastSocket s,
                                     NetworkInterface ni) throws IOException {
        // default value
        NetworkInterface nif = s.getNetworkInterface();
        assertTrue(nif.getIndex() == 0);
        assertTrue(nif.inetAddresses().count() == 1);
        assertTrue(nif.inetAddresses().findAny().orElseThrow().isAnyLocalAddress());
        assertTrue(s.getOption(IP_MULTICAST_IF) == null);
        assertTrue(s.getInterface().isAnyLocalAddress());

        // setNetworkInterface
        s.setNetworkInterface(ni);
        assertTrue(s.getNetworkInterface().equals(ni));
        assertTrue(s.getOption(IP_MULTICAST_IF).equals(ni));
        InetAddress address = s.getInterface();
        assertTrue(ni.inetAddresses().filter(address::equals).findAny().isPresent());

        // setInterface
        s.setInterface(address);
        assertTrue(s.getInterface().equals(address));
        assertTrue(s.getNetworkInterface()
                .inetAddresses()
                .filter(address::equals)
                .findAny()
                .isPresent());

        // null (exception not specified)
        assertThrows(IllegalArgumentException.class, () -> s.setNetworkInterface(null));
        assertThrows(SocketException.class, () -> s.setInterface(null));

        // setOption(IP_MULTICAST_IF)
        s.setOption(IP_MULTICAST_IF, ni);
        assertTrue(s.getOption(IP_MULTICAST_IF).equals(ni));
        assertTrue(s.getNetworkInterface().equals(ni));

        // bad values for IP_MULTICAST_IF
        assertThrows(IllegalArgumentException.class,
                     () -> s.setOption(IP_MULTICAST_IF, null));
        assertThrows(IllegalArgumentException.class,
                     () -> s.setOption((SocketOption) IP_MULTICAST_IF, "badValue"));
    }

    /**
     * Test getTimeToLive/setTimeToLive/getTTL/getTTL and IP_MULTICAST_TTL socket
     * option.
     */
    static void testTimeToLive(MulticastSocket s) throws IOException {
        // should be 1 by default
        assertTrue(s.getTimeToLive() == 1);
        assertTrue(s.getTTL() == 1);
        assertTrue(s.getOption(IP_MULTICAST_TTL) == 1);

        // setTimeToLive
        for (int ttl = 0; ttl <= 2; ttl++) {
            s.setTimeToLive(ttl);
            assertTrue(s.getTimeToLive() == ttl);
            assertTrue(s.getTTL() == ttl);
            assertTrue(s.getOption(IP_MULTICAST_TTL) == ttl);
        }
        assertThrows(IllegalArgumentException.class, () -> s.setTimeToLive(-1));

        // setTTL
        for (byte ttl = (byte) -2; ttl <= 2; ttl++) {
            s.setTTL(ttl);
            assertTrue(s.getTTL() == ttl);
            int intValue = Byte.toUnsignedInt(ttl);
            assertTrue(s.getTimeToLive() == intValue);
            assertTrue(s.getOption(IP_MULTICAST_TTL) == intValue);
        }

        // setOption(IP_MULTICAST_TTL)
        for (int ttl = 0; ttl <= 2; ttl++) {
            s.setOption(IP_MULTICAST_TTL, ttl);
            assertTrue(s.getOption(IP_MULTICAST_TTL) == ttl);
            assertTrue(s.getTimeToLive() == ttl);
            assertTrue(s.getTTL() == ttl);
        }

        // bad values for IP_MULTICAST_TTL
        assertThrows(IllegalArgumentException.class,
                    () -> s.setOption(IP_MULTICAST_TTL, -1));
        assertThrows(IllegalArgumentException.class,
                    () -> s.setOption(IP_MULTICAST_TTL, null));
        assertThrows(IllegalArgumentException.class,
                    () -> s.setOption((SocketOption) IP_MULTICAST_TTL, "badValue"));
    }

    /**
     * Test getLoopbackMode/setLoopbackMode and IP_MULTICAST_LOOP socket option.
     */
    static void testLoopbackMode(MulticastSocket s) throws IOException {
        // should be enabled by default
        assertTrue(s.getLoopbackMode() == false);
        assertTrue(s.getOption(IP_MULTICAST_LOOP) == true);

        // setLoopbackMode
        s.setLoopbackMode(true);    // disable
        assertTrue(s.getLoopbackMode());
        assertTrue(s.getOption(IP_MULTICAST_LOOP) == false);
        s.setLoopbackMode(false);   // enable
        assertTrue(s.getLoopbackMode() == false);
        assertTrue(s.getOption(IP_MULTICAST_LOOP) == true);

        // setOption(IP_MULTICAST_LOOP)
        s.setOption(IP_MULTICAST_LOOP, false);   // disable
        assertTrue(s.getOption(IP_MULTICAST_LOOP) == false);
        assertTrue(s.getLoopbackMode() == true);
        s.setOption(IP_MULTICAST_LOOP, true);  // enable
        assertTrue(s.getOption(IP_MULTICAST_LOOP) == true);
        assertTrue(s.getLoopbackMode() == false);

        // bad values for IP_MULTICAST_LOOP
        assertThrows(IllegalArgumentException.class,
                     () -> s.setOption(IP_MULTICAST_LOOP, null));
        assertThrows(IllegalArgumentException.class,
                     () -> s.setOption((SocketOption) IP_MULTICAST_LOOP, "badValue"));
    }

    /**
     * Send a datagram to the given multicast group and check that it is received.
     */
    static void testSendReceive(MulticastSocket s, InetAddress group) throws IOException {

        System.out.println("testSendReceive");

        // outgoing multicast interface needs to be set
        assertTrue(s.getOption(IP_MULTICAST_IF) != null);

        SocketAddress target = new InetSocketAddress(group, s.getLocalPort());
        byte[] message = "hello".getBytes("UTF-8");

        // send message to multicast group
        DatagramPacket p = new DatagramPacket(message, message.length);
        p.setSocketAddress(target);
        s.send(p, (byte) 1);

        // receive message
        s.setSoTimeout(0);
        p = new DatagramPacket(new byte[1024], 100);
        s.receive(p);

        assertTrue(p.getLength() == message.length);
        assertTrue(p.getPort() == s.getLocalPort());
    }

    /**
     * Send a datagram to the given multicast group and check that it is not
     * received.
     */
    static void testSendNoReceive(MulticastSocket s, InetAddress group) throws IOException {

        System.out.println("testSendNoReceive");

        // outgoing multicast interface needs to be set
        assertTrue(s.getOption(IP_MULTICAST_IF) != null);

        SocketAddress target = new InetSocketAddress(group, s.getLocalPort());
        long nano = System.nanoTime();
        String text = nano + ": hello";
        byte[] message = text.getBytes("UTF-8");

        // send datagram to multicast group
        DatagramPacket p = new DatagramPacket(message, message.length);
        p.setSocketAddress(target);
        s.send(p, (byte) 1);

        // datagram should not be received
        s.setSoTimeout(500);
        p = new DatagramPacket(new byte[1024], 100);
        while (true) {
            try {
                s.receive(p);
                if (Arrays.equals(p.getData(), p.getOffset(), p.getLength(), message, 0, message.length)) {
                    throw new RuntimeException("message shouldn't have been received");
                } else {
                    System.out.format("Received unexpected message from %s%n", p.getSocketAddress());
                }
            } catch (SocketTimeoutException expected) {
                break;
            }
        }
    }


    static void assertTrue(boolean e) {
        if (!e) throw new RuntimeException();
    }

    interface ThrowableRunnable {
        void run() throws Exception;
    }

    static void assertThrows(Class<?> exceptionClass, ThrowableRunnable task) {
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
