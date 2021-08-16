/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4469866
 * @library /test/lib
 * @summary Connecting to a link-local IPv6 address should not
 *          causes a SocketException to be thrown.
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main LinkLocal
 * @run main/othervm -Djava.net.preferIPv4Stack=true LinkLocal
 */

import java.net.*;
import java.util.List;
import java.util.stream.Collectors;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

public class LinkLocal {

    static int testCount = 0;
    static int failed = 0;

    static void TcpTest(InetAddress ia) throws Exception {
        System.out.println("**************************************");
        testCount++;
        System.out.println("Test " + testCount + ": TCP connect to " + ia);

        /*
         * Create ServerSocket on wildcard address and then
         * try to connect Socket to link-local address.
         */
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(ia, 0));

        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(ia, ss.getLocalPort()));

            System.out.println("Test passed - connection established.");

            // connection was established so accept it
            Socket s2 = ss.accept();
            s2.close();
        } catch (SocketException e) {
            failed++;
            System.out.println("Test failed: " + e);
        } finally {
            s.close();
            ss.close();
        }
    }

    static void UdpTest(InetAddress ia, boolean connected) throws Exception {

        System.out.println("**************************************");
        testCount++;

        if (connected) {
            System.out.println("Test " + testCount + ": UDP connect to " + ia);
        } else {
            System.out.println("Test " + testCount + ": UDP send to " + ia);
        }

        DatagramSocket ds1 = new DatagramSocket();
        DatagramSocket ds2 = new DatagramSocket(0, ia);

        try {
            byte b[] = "Hello".getBytes();
            DatagramPacket p = new DatagramPacket(b, b.length);

            if (connected) {
                ds1.connect( new InetSocketAddress(ia, ds2.getLocalPort()) );
                System.out.println("DatagramSocket connected.");
            } else {
                p.setAddress(ia);
                p.setPort(ds2.getLocalPort());
            }
            ds1.send(p);
            System.out.println("Packet has been sent.");

            ds2.setSoTimeout(5000);
            ds2.receive(p);
            System.out.println("Test passed - packet received.");
        } catch (SocketException e) {
            failed++;
            System.out.println("Test failed: " + e);
        } finally {
            ds1.close();
            ds2.close();
        }
    }

    static void TestAddress(InetAddress ia) throws Exception {
        TcpTest(ia);
        UdpTest(ia, true);      /* unconnected */
        UdpTest(ia, false);     /* connected */
    }

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        /*
         * If an argument is provided ensure that it's
         * a link-local IPv6 address.
         */
        if (args.length > 0) {
            InetAddress ia = InetAddress.getByName(args[0]);

            if ( !(ia instanceof Inet6Address) ||
                !ia.isLinkLocalAddress()) {
                throw new Exception(ia +
                        " is not a link-local IPv6 address");
            }

            TestAddress(ia);
        }

        /*
         * If no argument is provided then enumerate the
         * local addresses and run the test on each link-local
         * IPv6 address.
         */
        if (args.length == 0) {
            List<Inet6Address> addrs = NetworkConfiguration.probe()
                    .ip6Addresses()
                    .filter(Inet6Address::isLinkLocalAddress)
                    .collect(Collectors.toList());

            for (Inet6Address addr : addrs) {
                TestAddress(addr);
            }
        }

        /*
         * Print results
         */
        if (testCount == 0) {
            System.out.println("No link-local IPv6 addresses - test skipped!");
        } else {
            System.out.println("**************************************");
            System.out.println(testCount + " test(s) executed, " +
                failed + " failed.");
            if (failed > 0) {
                throw new Exception( failed + " test(s) failed.");
            }
        }
    }
}
