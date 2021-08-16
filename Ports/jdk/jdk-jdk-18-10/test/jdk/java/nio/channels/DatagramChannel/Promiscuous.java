/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *

/* @test
 * @bug 8014377 8241786
 * @summary Test for interference when two sockets are bound to the same
 *   port but joined to different multicast groups
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 *        Promiscuous
 * @run main Promiscuous
 * @run main/othervm -Djava.net.preferIPv4Stack=true Promiscuous
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import static java.net.StandardProtocolFamily.*;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

public class Promiscuous {

    static final Random rand = new Random();

    static final ProtocolFamily UNSPEC = new ProtocolFamily() {
        public String name() {
            return "UNSPEC";
        }
    };

    /**
     * Sends a datagram to the given multicast group
     */
    static int sendDatagram(NetworkInterface nif,
                            InetAddress group,
                            int port)
        throws IOException
    {
        ProtocolFamily family = (group instanceof Inet6Address) ?
            StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
        int id = rand.nextInt();
        try (DatagramChannel dc = DatagramChannel.open(family)) {
            dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, nif);
            byte[] msg = Integer.toString(id).getBytes("UTF-8");
            ByteBuffer buf = ByteBuffer.wrap(msg);
            System.out.format("Send message -> group %s (id=0x%x)\n",
                    group.getHostAddress(), id);
            dc.send(buf, new InetSocketAddress(group, port));
        }
        return id;
    }

    /**
     * Wait (with timeout) for datagram. The {@code datagramExepcted}
     * parameter indicates whether a datagram is expected, and if
     * {@true} then {@code id} is the identifier in the payload.
     */
    static void receiveDatagram(DatagramChannel dc,
                                String name,
                                boolean datagramExepcted,
                                int id)
        throws IOException
    {
        System.out.println("Checking if received by " + name);

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
                    if (datagramExepcted) {
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

                if (!datagramExepcted) {
                    if (receivedId == id)
                        throw new RuntimeException("Message not expected");
                    System.out.println("Message ignored (has wrong id)");
                } else {
                    if (receivedId == id) {
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

    static void test(ProtocolFamily family,
                     NetworkInterface nif,
                     InetAddress group1,
                     InetAddress group2)
        throws IOException
    {

        System.out.format("%nTest family=%s%n", family.name());

        DatagramChannel dc1 = (family == UNSPEC) ?
            DatagramChannel.open() : DatagramChannel.open(family);
        DatagramChannel dc2 = (family == UNSPEC) ?
            DatagramChannel.open() : DatagramChannel.open(family);

        try {
            dc1.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            dc2.setOption(StandardSocketOptions.SO_REUSEADDR, true);

            dc1.bind(new InetSocketAddress(0));
            int port = dc1.socket().getLocalPort();
            dc2.bind(new InetSocketAddress(port));

            System.out.format("dc1 joining [%s]:%d @ %s\n",
                group1.getHostAddress(), port, nif.getName());
            System.out.format("dc2 joining [%s]:%d @ %s\n",
                group2.getHostAddress(), port, nif.getName());

            dc1.join(group1, nif);
            dc2.join(group2, nif);

            int id = sendDatagram(nif, group1, port);

            receiveDatagram(dc1, "dc1", true, id);
            receiveDatagram(dc2, "dc2", false, id);

            id = sendDatagram(nif, group2, port);

            receiveDatagram(dc1, "dc1", false, id);
            receiveDatagram(dc2, "dc2", true, id);

        } finally {
            dc1.close();
            dc2.close();
        }
    }

    public static void main(String[] args) throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();

        String os = System.getProperty("os.name");

        // Requires IP_MULTICAST_ALL on Linux (new since 2.6.31) so skip
        // on older kernels. Note that we skip on <= version 3 to keep the
        // parsing simple
        if (os.equals("Linux")) {
            String osversion = System.getProperty("os.version");
            String[] vers = osversion.split("\\.", 0);
            int major = Integer.parseInt(vers[0]);
            if (major < 3) {
                System.out.format("Kernel version is %s, test skipped%n", osversion);
                return;
            }
        }

        // get local network configuration to use
        NetworkConfiguration config = NetworkConfiguration.probe();

        // multicast groups used for the test
        InetAddress ip4Group1 = InetAddress.getByName("225.4.5.6");
        InetAddress ip4Group2 = InetAddress.getByName("225.4.6.6");

        for (NetworkInterface nif: config.ip4MulticastInterfaces()
                                         .collect(Collectors.toList())) {
            InetAddress source = config.ip4Addresses(nif).iterator().next();
            test(INET, nif, ip4Group1, ip4Group2);

            // Solaris and Linux allow IPv6 sockets join IPv4 multicast groups
            if (os.equals("Linux"))
                test(UNSPEC, nif, ip4Group1, ip4Group2);
        }
    }
}
