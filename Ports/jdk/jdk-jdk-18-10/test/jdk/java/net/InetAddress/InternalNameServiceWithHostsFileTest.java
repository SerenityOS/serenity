/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8134577
 * @summary Test the internal NameService implementation which is enabled via
 *          the system property jdk.net.hosts.file. This property specifies
 *          a file name that contains address host mappings, similar to those in
 *          /etc/hosts file. TestHosts-III file  exist, with a set of ipv4 and ipv6
 *          mappings
 * @run main/othervm -Djdk.net.hosts.file=${test.src}/TestHosts-III -Dsun.net.inetaddr.ttl=0
 *      InternalNameServiceWithHostsFileTest
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class InternalNameServiceWithHostsFileTest {
    public static void main(String args[]) throws Exception {
        // fe80::1
        byte[] expectedIpv6Address = { (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 1 };
        // fe00::0
        byte[] expectedIpv6LocalAddress = { (byte) 0xfe, (byte) 0x00, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        // 10.2.3.4
        byte[] expectedIpv4Address = { 10, 2, 3, 4 };
        //
        byte[] expectedIpv6LocalhostAddress = { 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 1 };

        // 10.2.3.4  testHost.testDomain
        testHostsMapping(expectedIpv4Address, "testHost.testDomain");
        // ::1     ip6-localhost ip6-loopback
        testHostsMapping(expectedIpv6LocalhostAddress, "ip6-localhost");
        // fe00::0 ip6-localnet
        testHostsMapping(expectedIpv6LocalAddress, "ip6-localnet");
        // fe80::1 link-local-host
        testHostsMapping(expectedIpv6Address, "link-local-host");

        testReverseLookup("10.2.3.4", "testHost.testDomain");

        testReverseLookup("::1", "ip6-localhost");
        testReverseLookup("0:0:0:0:0:0:0:1", "ip6-localhost");
        testReverseLookup("0000:0000:0000:0000:0000:0000:0000:0001", "ip6-localhost");

        testReverseLookup("fe00::0", "ip6-localnet");
        testReverseLookup("fe00:0:0:0:0:0:0:0", "ip6-localnet");
        testReverseLookup("fe00:0000:0000:0000:0000:0000:0000:0000", "ip6-localnet");

        testReverseLookup("fe80::1", "link-local-host");
        testReverseLookup("fe80:000:0:00:0:000:00:1", "link-local-host");
        testReverseLookup("fe80:0000:0000:0000:0000:0000:0000:0001", "link-local-host");
    }

    private static void testHostsMapping(byte[] expectedIpAddress, String hostName)
            throws UnknownHostException {
        InetAddress testAddress;
        byte[] rawIpAddress;
        testAddress = InetAddress.getByName(hostName);
        System.out
                .println("############################  InetAddress == "
                        + testAddress);

        rawIpAddress = testAddress.getAddress();
        if (!Arrays.equals(rawIpAddress, expectedIpAddress)) {
            System.out.println("retrieved address == "
                    + Arrays.toString(rawIpAddress)
                    + " not equal to expected address == "
                    + Arrays.toString(expectedIpAddress));
            throw new RuntimeException(
                    "retrieved address not equal to expected address");
        }
        System.out.println("retrieved address == "
                + Arrays.toString(rawIpAddress)
                + " equal to expected address == "
                + Arrays.toString(expectedIpAddress));
    }

    private static void testReverseLookup(String numericHost, String expectedName)
            throws UnknownHostException {
        String lookupResult = InetAddress.getByName(numericHost).getHostName();
        if (!expectedName.equals(lookupResult)) {
            throw new RuntimeException(
                String.format(
                    "reverse lookup of \"%s\" is \"%s\", should be \"%s\"\n",
                    numericHost, lookupResult, expectedName));
        }
    }
}
