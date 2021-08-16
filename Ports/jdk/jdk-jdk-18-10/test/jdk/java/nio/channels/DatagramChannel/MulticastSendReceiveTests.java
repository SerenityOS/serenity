/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4527345 7026376 6633549 8233435
 * @summary Unit test for DatagramChannel's multicast support
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 *        jdk.test.lib.net.IPSupport
 *        MulticastSendReceiveTests
 * @run main MulticastSendReceiveTests
 * @run main/othervm -Djava.net.preferIPv4Stack=true MulticastSendReceiveTests
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import static java.net.StandardProtocolFamily.*;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;

import jdk.test.lib.Platform;
import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

public class MulticastSendReceiveTests {

    static final Random rand = new Random();

    static final ProtocolFamily UNSPEC = new ProtocolFamily() {
        public String name() {
            return "UNSPEC";
        }
    };

    /**
     * Send datagram from given local address to given multicast
     * group.
     */
    static int sendDatagram(InetAddress local,
                            NetworkInterface nif,
                            InetAddress group,
                            int port)
        throws IOException
    {
        ProtocolFamily family = (group instanceof Inet6Address) ?
            StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
        int id = rand.nextInt();
        try (DatagramChannel dc = DatagramChannel.open(family)) {
            dc.bind(new InetSocketAddress(local, 0));
            dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, nif);
            byte[] msg = Integer.toString(id).getBytes("UTF-8");
            ByteBuffer buf = ByteBuffer.wrap(msg);
            System.out.format("Send message from %s -> group %s (id=0x%x)\n",
                    local.getHostAddress(), group.getHostAddress(), id);
            dc.send(buf, new InetSocketAddress(group, port));
        }
        return id;
    }

    /**
     * Wait (with timeout) for datagram.
     *
     * @param   expectedSender - expected sender address, or
     *                           null if no datagram expected
     * @param   id - expected id of datagram
     */
    static void receiveDatagram(DatagramChannel dc,
                                InetAddress expectedSender,
                                int id)
        throws IOException
    {
        Selector sel = Selector.open();
        dc.configureBlocking(false);
        dc.register(sel, SelectionKey.OP_READ);
        ByteBuffer buf = ByteBuffer.allocateDirect(100);

        try {
            for (;;) {
                System.out.println("Waiting to receive message");
                sel.select(5*1000);
                SocketAddress sa = dc.receive(buf);

                // no datagram received
                if (sa == null) {
                    if (expectedSender != null) {
                        throw new RuntimeException("Expected message not received");
                    }
                    System.out.println("No message received (correct)");
                    return;
                }

                // datagram received

                InetAddress sender = ((InetSocketAddress)sa).getAddress();
                buf.flip();
                byte[] bytes = new byte[buf.remaining()];
                buf.get(bytes);
                String s = new String(bytes, "UTF-8");
                int receivedId = -1;
                try {
                    receivedId = Integer.parseInt(s);
                    System.out.format("Received message from %s (id=0x%x)\n",
                            sender, receivedId);
                } catch (NumberFormatException x) {
                    System.out.format("Received message from %s (msg=%s)\n", sender, s);
                }

                if (expectedSender == null) {
                    if (receivedId == id)
                        throw new RuntimeException("Message not expected");
                    System.out.println("Message ignored (has wrong id)");
                } else {
                    if (sender.equals(expectedSender)) {
                        System.out.println("Message expected");
                        return;
                    }
                    System.out.println("Message ignored (wrong sender)");
                }

                sel.selectedKeys().clear();
                buf.rewind();
            }
        } finally {
            sel.close();
        }
    }


    /**
     * Exercise multicast send/receive on given group/interface
     */
    static void test(ProtocolFamily family,
                     NetworkInterface nif,
                     InetAddress group,
                     InetAddress source)
        throws IOException
    {
        System.out.format("\nTest DatagramChannel to %s socket\n", family.name());
        try (DatagramChannel dc = (family == UNSPEC) ?
                DatagramChannel.open() : DatagramChannel.open(family)) {
            dc.setOption(StandardSocketOptions.SO_REUSEADDR, true)
              .bind(new InetSocketAddress(0));

            // join group
            System.out.format("join %s @ %s\n", group.getHostAddress(),
                nif.getName());
            MembershipKey key;
            try {
                key = dc.join(group, nif);
            } catch (IllegalArgumentException iae) {
                if (family == UNSPEC) {
                    System.out.println("Not supported");
                    return;
                }
                throw iae;
            }

            // send message to group
            int port = ((InetSocketAddress)dc.getLocalAddress()).getPort();
            int id = sendDatagram(source, nif, group, port);

            // receive message and check id matches
            receiveDatagram(dc, source, id);

            // exclude-mode filtering

            try {
                System.out.format("block %s\n", source.getHostAddress());

                // may throw UOE
                key.block(source);
                id = sendDatagram(source, nif, group, port);
                receiveDatagram(dc, null, id);

                // unblock source, send message, message should be received
                System.out.format("unblock %s\n", source.getHostAddress());
                key.unblock(source);
                id = sendDatagram(source, nif, group, port);
                receiveDatagram(dc, source, id);
            } catch (UnsupportedOperationException x) {
                String os = System.getProperty("os.name");
                // Exclude-mode filtering supported on these platforms so UOE should never be thrown
                if (os.equals("Linux"))
                    throw x;
                System.out.println("Exclude-mode filtering not supported!");
            }

            key.drop();

            // include-mode filtering

            InetAddress bogus = (group instanceof Inet6Address) ?
                InetAddress.getByName("fe80::1234") :
                InetAddress.getByName("1.2.3.4");
            System.out.format("join %s @ %s only-source %s\n", group.getHostAddress(),
                nif.getName(), bogus.getHostAddress());
            try {
                // may throw UOE
                key = dc.join(group, nif, bogus);

                id = sendDatagram(source, nif, group, port);
                receiveDatagram(dc, null, id);

                System.out.format("join %s @ %s only-source %s\n", group.getHostAddress(),
                    nif.getName(), source.getHostAddress());
                key = dc.join(group, nif, source);

                id = sendDatagram(source, nif, group, port);
                receiveDatagram(dc, source, id);
            } catch (UnsupportedOperationException x) {
                String os = System.getProperty("os.name");
                // Include-mode filtering supported on these platforms so UOE should never be thrown
                if (os.equals("Linux"))
                    throw x;
                System.out.println("Include-mode filtering not supported!");
            }
        }
    }

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

        // Platforms that allow dual sockets join IPv4 multicast groups
        boolean canIPv6JoinIPv4Group =
                Platform.isLinux() ||
                Platform.isOSX() ||
                Platform.isWindows();

        for (NetworkInterface nif : ip4MulticastInterfaces) {
            InetAddress source = config.ip4Addresses(nif).iterator().next();
            test(UNSPEC, nif, ip4Group, source);
            test(INET,   nif, ip4Group, source);
            if (IPSupport.hasIPv6() && canIPv6JoinIPv4Group) {
                test(INET6,  nif, ip4Group, source);
            }
        }

        for (NetworkInterface nif : ip6MulticastInterfaces) {
            InetAddress source = config.ip6Addresses(nif).iterator().next();
            test(UNSPEC, nif, ip6Group, source);
            test(INET6,  nif, ip6Group, source);
        }
    }
}
