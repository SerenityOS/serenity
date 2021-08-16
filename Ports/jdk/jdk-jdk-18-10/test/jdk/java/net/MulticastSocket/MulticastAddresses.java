/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4488458
 * @library /test/lib
 * @summary Test that MutlicastSocket.joinGroup is working for
 *          various multicast and non-multicast addresses.
 * @run main MulticastAddresses
 */

import jdk.test.lib.NetworkConfiguration;

import java.net.*;
import java.io.IOException;
import java.util.stream.Collectors;

public class MulticastAddresses {
    public static void runTest(NetworkInterface ni,
                               String[] multicasts,
                               String[] nonMulticasts) throws Exception {
        int failures = 0;

        MulticastSocket s = new MulticastSocket();

        /* test valid multicast addresses */
        for (int i = 0; i < multicasts.length; i++) {
            InetAddress ia = InetAddress.getByName(multicasts[i]);

            System.out.println("Test: " + ia + " " + " ni: " + ni);
            try {

                System.out.print("    joinGroup(InetAddress) ");
                s.joinGroup(ia);
                s.leaveGroup(ia);
                System.out.println("    Passed.");

                System.out.print("    joinGroup(InetAddress,NetworkInterface) ");
                s.joinGroup(new InetSocketAddress(ia, 0), ni);
                s.leaveGroup(new InetSocketAddress(ia, 0), ni);
                System.out.println("    Passed.");
            } catch (IOException e) {
                failures++;
                System.out.println("Failed: " + e.getMessage());
            }

        }

        /* test non-multicast addresses */
        for (int i = 0; i < nonMulticasts.length; i++) {
            InetAddress ia = InetAddress.getByName(nonMulticasts[i]);
            boolean failed = false;

            System.out.println("Test: " + ia + " ");
            try {
                System.out.println("    joinGroup(InetAddress) ");
                s.joinGroup(ia);

                System.out.println("Failed!! -- incorrectly joined group");
                failed = true;
            } catch (IOException e) {
                System.out.println("    Passed: " + e.getMessage());
            }
            if (failed) {
                s.leaveGroup(ia);
                failures++;
            }
        }
        s.close();
        if (failures > 0) {
            throw new Exception(failures + " test(s) failed - see log file.");
        }
    }


    public static void main(String args[]) throws Exception {

        String[] multicastIPv4 = {
                "224.80.80.80",
        };
        String[] multicastIPv6 = {
                "ff01::1",
                "ff02::1234",
                "ff05::a",
                "ff0e::1234:a"};

        String[] nonMulticastIPv4 = {
                "129.1.1.1"
        };

        String[] nonMulticastIPv6 = {
                "::1",
                "::129.1.1.1",
                "fe80::a00:20ff:fee5:bc02"};

        /*
         * Examine the network interfaces and determine :-
         *
         * 1. If host has IPv6 support
         * 2. Get reference to a non-loopback interface
         */
        NetworkConfiguration nc = NetworkConfiguration.probe();
        var ipv6List = nc.ip6MulticastInterfaces(false)
                .collect(Collectors.toList());

        var ipv4List = nc.ip4MulticastInterfaces(false)
                .collect(Collectors.toList());

        if (ipv6List.retainAll(ipv4List)) {
            runTest(ipv6List.get(0), multicastIPv4, nonMulticastIPv4);
            runTest(ipv6List.get(0), multicastIPv6, nonMulticastIPv6);
        } else {
            if (!ipv4List.isEmpty())
                runTest(ipv4List.get(0), multicastIPv4, nonMulticastIPv4);
            if (!ipv6List.isEmpty())
                runTest(ipv6List.get(0), multicastIPv6, nonMulticastIPv6);
        }
    }
}
