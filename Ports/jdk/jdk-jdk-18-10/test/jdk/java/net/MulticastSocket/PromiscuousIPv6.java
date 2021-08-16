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
 * @run main/othervm PromiscuousIPv6
 * @key randomness
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.util.List;
import java.util.Random;
import jdk.test.lib.NetworkConfiguration;
import jtreg.SkippedException;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/*
 * This test was created as a clone of the Promiscuous test and adapted for
 * IPv6 node-local and link-local multicast addresses on Linux.
 */
public class PromiscuousIPv6 {

    static final Random rand = new Random();

    static final int TIMEOUT =  5 * 1000; // 5 secs

    static int sendDatagram(NetworkInterface nif, InetAddress group, int port)
        throws IOException
    {
        try (MulticastSocket mc = new MulticastSocket()) {
            mc.setOption(StandardSocketOptions.IP_MULTICAST_IF, nif);

            int id = rand.nextInt();
            byte[] msg = Integer.toString(id).getBytes(UTF_8);
            DatagramPacket p = new DatagramPacket(msg, msg.length);
            p.setAddress(group);
            p.setPort(port);

            out.printf("Sending datagram to: %s/%d\n", group, port);
            mc.send(p);
            return id;
        }
    }

    static void receiveDatagram(DatagramSocket mc, boolean datagramExpected, int id)
        throws IOException
    {
        byte[] ba = new byte[100];
        DatagramPacket p = new DatagramPacket(ba, ba.length);
        try {
            mc.receive(p);
            int recvId = Integer.parseInt(
                    new String(p.getData(), 0, p.getLength(), UTF_8));
            if (datagramExpected) {
                if (recvId != id)
                    throw new RuntimeException("Unexpected id, got " + recvId
                                                       + ", expected: " + id);
                out.printf("Received message as expected, %s\n", p.getAddress());
            } else {
                throw new RuntimeException("Unexpected message received, "
                                                   + p.getAddress());
            }
        } catch (SocketTimeoutException e) {
            if (datagramExpected)
                throw new RuntimeException("Expected message not received, "
                                                   + e.getMessage());
            else
                out.printf("Message not received, as expected\n");
        }
    }

    static void test(NetworkInterface nif, InetAddress group1, InetAddress group2)
        throws IOException
    {
        // Bind addresses should include the same network interface / scope, so
        // as to not reply on the default route when there are multiple interfaces
        InetAddress bindAddr1 = Inet6Address.getByAddress(null, group1.getAddress(), nif);
        InetAddress bindAddr2 = Inet6Address.getByAddress(null, group2.getAddress(), nif);

        try (MulticastSocket mc1 = new MulticastSocket(new InetSocketAddress(bindAddr1, 0));
             MulticastSocket mc2 = new MulticastSocket(new InetSocketAddress(bindAddr2, mc1.getLocalPort()))) {

            final int port = mc1.getLocalPort();
            out.printf("Using port: %d\n", port);

            mc1.setSoTimeout(TIMEOUT);
            mc2.setSoTimeout(TIMEOUT);

            mc1.joinGroup(new InetSocketAddress(group1, 0), nif);
            out.printf("mc1 joined the MC group: %s\n", group1);
            mc2.joinGroup(new InetSocketAddress(group2, 0), nif);
            out.printf("mc2 joined the MC group: %s\n", group2);

            out.printf("Sending datagram to: %s/%d\n", group1, port);
            int id = sendDatagram(nif, group1, port);

            // the packet should be received by mc1 only
            receiveDatagram(mc1, true, id);
            receiveDatagram(mc2, false, 0);


            out.printf("Sending datagram to: %s/%d\n", group2, port);
            id = sendDatagram(nif, group2, port);

            // the packet should be received by mc2 only
            receiveDatagram(mc2, true, id);
            receiveDatagram(mc1, false, 0);

            mc1.leaveGroup(new InetSocketAddress(group1, 0), nif);
            mc2.leaveGroup(new InetSocketAddress(group2, 0), nif);
        }
    }

    public static void main(String args[]) throws IOException {
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

        InetAddress interfaceLocal1 = InetAddress.getByName("ff11::3.4.5.6");
        InetAddress interfaceLocal2 = InetAddress.getByName("ff11::5.6.7.8");

        InetAddress linkLocal1 = InetAddress.getByName("ff12::4.5.6.7");
        InetAddress linkLocal2 = InetAddress.getByName("ff12::7.8.9.10");

        for (NetworkInterface nif : nifs) {
            test(nif, interfaceLocal1, interfaceLocal2);
            test(nif, linkLocal1, linkLocal2);
        }
    }
}
