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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

/**
 * @test
 * @bug 4488458
 * @summary IPv4 and IPv6 multicasting broken on Linux
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main Test
 * @run main/othervm -Djava.net.preferIPv4Stack=true Test
 */
public class Test {

    static int count = 0;
    static int failures = 0;

    void doTest(String address) throws IOException {
        boolean failed = false;

        InetAddress ia = InetAddress.getByName(address);

        count++;
        System.out.println("**********************");
        System.out.println("Test " + count + ": " + ia);

        MulticastSocket mc = new MulticastSocket();
        int port = mc.getLocalPort();
        DatagramSocket s1 = new DatagramSocket();

        byte msg[] = "Hello".getBytes();
        DatagramPacket p = new DatagramPacket(msg, msg.length);

        mc.setSoTimeout(2000);

        try {
            for (int i=0; i<2; i++) {

                System.out.println("Join: " + ia);
                mc.joinGroup(ia);

                /* packets should be received */

                for (int j = 0; j < 2; j++) {
                    p.setAddress(ia);
                    p.setPort(port);

                    System.out.println("Send packet to: " + ia);
                    s1.send(p);

                    try {
                        mc.receive(p);
                        System.out.println("Got packet! - Good.");
                    } catch (SocketTimeoutException e) {
                        failed = true;
                        System.out.println("Failed: No packet received within timeout!!!");
                    }
                }

                System.out.println("Leave: " + ia);
                mc.leaveGroup(ia);

                /*
                 * If there are multiple interface we might be a couple of
                 * copies still in our queue
                 */
                try {
                    while (true) {
                        mc.receive(p);
                    }
                } catch (SocketTimeoutException e) { }

                /* packets should not be received */

                p.setAddress(ia);
                p.setPort(port);

                s1.send(p);

                try {
                    mc.receive(p);
                    System.out.println("Failed: Got packet after leaving group!!!");
                    failed = true;
                } catch (SocketTimeoutException e) {
                    System.out.println("No packet received within timeout! - Good.");
                }
            }

         } catch (IOException ioe) {
            System.out.println("Failed: Unexpected exception thrown: ");
            ioe.printStackTrace();
            failed = true;
        }

        mc.close();
        s1.close();

        if (failed) {
            failures++;
            System.out.println("Test failed!!");
        } else {
            System.out.println("Test passed.");
        }
    }

    void allTests() throws IOException {
        NetworkConfiguration nc = NetworkConfiguration.probe();

        // unconditionally test IPv4 address
        doTest("224.80.80.80");

        // If IPv6 is enabled perform multicast tests with various scopes
        if (nc.hasTestableIPv6Address()) {
            doTest("ff01::a");
        }

        if (nc.hasLinkLocalAddress()) {
            doTest("ff02::a");
        }

        if (nc.hasSiteLocalAddress()) {
            doTest("ff05::a");
        }

        if (nc.has_globaladdress()) {
            doTest("ff0e::a");
        }
    }

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        Test t = new Test();

        if (args.length == 0) {
            t.allTests();
        } else {
            for (int i = 0; i < args.length; i++) {
                t.doTest(args[i]);
            }
        }

        System.out.println("**********************");
        System.out.println(count + " test(s) executed. " + failures +
                           " test(s) failed.");

        if (failures > 0) {
            throw new Exception("Test failed - see log file for details");
        }
    }
}
