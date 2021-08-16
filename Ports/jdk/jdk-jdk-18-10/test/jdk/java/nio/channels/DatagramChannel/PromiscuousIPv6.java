/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8215294
 * @requires os.family == "linux"
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        PromiscuousIPv6
 * @run main PromiscuousIPv6
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;
import jdk.test.lib.NetworkConfiguration;
import jtreg.SkippedException;
import static java.net.StandardProtocolFamily.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/*
 * This test was created as a copy of the Promiscuous test and adapted for
 * IPv6 node-local and link-local multicast addresses on Linux.
 */
public class PromiscuousIPv6 {

    static final Random rand = new Random();

    static final ProtocolFamily UNSPEC = () -> "UNSPEC";

    /**
     * Sends a datagram to the given multicast group
     */
    static int sendDatagram(NetworkInterface nif,
                            InetAddress group,
                            int port)
            throws IOException
    {
        ProtocolFamily family = (group instanceof Inet6Address) ? INET6 : INET;
        DatagramChannel dc = DatagramChannel.open(family)
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, nif);
        int id = rand.nextInt();
        byte[] msg = Integer.toString(id).getBytes(UTF_8);
        ByteBuffer buf = ByteBuffer.wrap(msg);
        System.out.format("Send message -> group %s (id=0x%x)\n",
                          group.getHostAddress(), id);
        dc.send(buf, new InetSocketAddress(group, port));
        dc.close();
        return id;
    }

    /**
     * Waits (with timeout) for datagram. The {@code datagramExpected}
     * parameter indicates whether a datagram is expected, and if
     * {@code true} then {@code id} is the identifier in the payload.
     */
    static void receiveDatagram(DatagramChannel dc,
                                String name,
                                boolean datagramExpected,
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
                    if (datagramExpected) {
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

                if (!datagramExpected) {
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

        // Bind addresses should include the same network interface / scope, so
        // as to not reply on the default route when there are multiple interfaces
        InetAddress bindAddr1 = Inet6Address.getByAddress(null, group1.getAddress(), nif);
        InetAddress bindAddr2 = Inet6Address.getByAddress(null, group2.getAddress(), nif);

        DatagramChannel dc1 = (family == UNSPEC) ?
                DatagramChannel.open() : DatagramChannel.open(family);
        DatagramChannel dc2 = (family == UNSPEC) ?
                DatagramChannel.open() : DatagramChannel.open(family);

        try {
            dc1.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            dc2.setOption(StandardSocketOptions.SO_REUSEADDR, true);

            dc1.bind(new InetSocketAddress(bindAddr1, 0));
            int port = dc1.socket().getLocalPort();
            dc2.bind(new InetSocketAddress(bindAddr2, port));

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

        String os = System.getProperty("os.name");

        if (!os.equals("Linux")) {
            throw new SkippedException("This test should be run only on Linux");
        } else {
            String osVersion = System.getProperty("os.version");
            String prefix = "3.10.0";
            if (osVersion.startsWith(prefix)) {
                throw new SkippedException(
                        String.format("The behavior under test is known NOT to work on '%s' kernels", prefix));
            }
        }

        NetworkConfiguration.printSystemConfiguration(System.out);
        List<NetworkInterface> nifs = NetworkConfiguration.probe()
                .ip6MulticastInterfaces()
                .collect(toList());

        if (nifs.size() == 0) {
            throw new SkippedException(
                    "No IPv6 interfaces that support multicast found");
        }

        InetAddress interfaceLocal1 = InetAddress.getByName("ff11::2.3.4.5");
        InetAddress interfaceLocal2 = InetAddress.getByName("ff11::6.7.8.9");

        InetAddress linkLocal1 = InetAddress.getByName("ff12::2.3.4.5");
        InetAddress linkLocal2 = InetAddress.getByName("ff12::6.7.8.9");

        for (NetworkInterface nif : nifs) {
            test(INET6, nif, interfaceLocal1, interfaceLocal2);
            test(INET6, nif, linkLocal1, linkLocal2);
        }
    }
}
