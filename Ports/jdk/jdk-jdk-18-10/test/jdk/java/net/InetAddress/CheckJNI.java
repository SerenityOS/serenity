/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4889870 4890033
 * @summary java -Xcheck:jni failing in net code on Solaris / [Datagram]Socket.getLocalAddress() failure
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main/othervm -Xcheck:jni CheckJNI
 */

import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

public class CheckJNI {
    public static void main (String[] args) throws Exception {
        /* try to invoke as much java.net native code as possible */

        InetAddress loopback = InetAddress.getLoopbackAddress();
        System.out.println("Testing loopback Socket/ServerSocket");
        testSocket(loopback);

        System.out.println("Testing loopback DatagramSocket");
        testDatagram(loopback);

        if (IPSupport.hasIPv4()) {
            InetAddress loopback4 = InetAddress.getByName("127.0.0.1");
            System.out.println("Testing IPv4 Socket/ServerSocket");
            testSocket(loopback4);

            System.out.println("Testing IPv4 DatagramSocket");
            testDatagram(loopback4);
        }

        /* Find link local IPv6 addrs to test */
        List<Inet6Address> addrs = NetworkConfiguration.probe()
                .ip6Addresses()
                .filter(Inet6Address::isLinkLocalAddress)
                .collect(Collectors.toList());

        for (Inet6Address ia6 : addrs) {
            System.out.println("Address:" + ia6);
            System.out.println("Testing IPv6 Socket");
            testSocket(ia6);

            System.out.println("Testing IPv6 DatagramSocket");
            testDatagram(ia6);
        }
        System.out.println("OK");
    }

    static void testSocket(InetAddress ia) throws Exception {
        ServerSocket server = new ServerSocket(0, 0, ia);
        Socket s = new Socket(ia, server.getLocalPort());
        s.close();
        server.close();
    }

    static void testDatagram(InetAddress ia) throws Exception {
        DatagramSocket s1 = new DatagramSocket(0, ia);
        DatagramSocket s2 = new DatagramSocket(0, ia);
        System.out.println("s1: local address=" + s1.getLocalAddress()
                            + ", local port=" + s1.getLocalPort());
        System.out.println("s2: local address=" + s2.getLocalAddress()
                            + ", local port=" + s2.getLocalPort());

        DatagramPacket p1 = new DatagramPacket (
                "hello world".getBytes(),
                0, "hello world".length(), s2.getLocalAddress(),
                s2.getLocalPort()
        );

        DatagramPacket p2 = new DatagramPacket(new byte[128], 128);
        s1.send(p1);
        s2.receive(p2);
        s1.close();
        s2.close();
    }
}
