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
 *          /etc/hosts file.
 * @run main/othervm -Djdk.net.hosts.file=TestHosts -Dsun.net.inetaddr.ttl=0
 *      InternalNameServiceTest
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class InternalNameServiceTest {

    static final String HOSTS_FILE_NAME = System.getProperty("jdk.net.hosts.file");

    public static void main(String args[]) throws Exception {
        testHostToIPAddressMappings(HOSTS_FILE_NAME);
        testIpAddressToHostNameMappings(HOSTS_FILE_NAME);
    }

    private static void testHostToIPAddressMappings(String hostsFileName)
            throws Exception, UnknownHostException {
        System.out.println(" TEST HOST TO  IP ADDRESS MAPPINGS ");
        InetAddress testAddress;
        byte[] retrievedIpAddr;
        byte[] expectedIpAddr1 = { 1, 2, 3, 4 };
        byte[] expectedIpAddr2 = { 5, 6, 7, 8 };
        byte[] expectedIpAddrIpv6_1 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

        // hosts file with
        // # test hosts file for internal NameService
        // 1.2.3.4 host.sample-domain
        // 5.6.7.8 host1.sample-domain
        // 1.2.3.4 host2.sample-domain  # this is a comment
        // host3.sample-domain # no ip address
        //  host4.sample-domain # space as ip address
        //   host5.sample-domain # double space as ip address

        // add comment to hosts file
        addMappingToHostsFile("test hosts file for internal NameService ", "#", hostsFileName,
                false);
        addMappingToHostsFile("host.sample-domain", "1.2.3.4", hostsFileName,
                true);

        testAddress = InetAddress.getByName("host.sample-domain");
        retrievedIpAddr = testAddress.getAddress();
        if (!Arrays.equals(retrievedIpAddr, expectedIpAddr1)) {
            throw new RuntimeException(
                    "retrievedIpAddr not equal to expectedipAddr");
        }

        addMappingToHostsFile("host1.sample-domain", "5.6.7.8", hostsFileName,
                true);
        addMappingToHostsFile("host2.sample-domain", "1.2.3.4", hostsFileName,
                true);

        testAddress = InetAddress.getByName("host1.sample-domain");
        retrievedIpAddr = testAddress.getAddress();
        if (!Arrays.equals(retrievedIpAddr, expectedIpAddr2)) {
            throw new RuntimeException(
                    "retrievedIpAddr not equal to expectedIpAddr");
        }

        testAddress = InetAddress.getByName("host2.sample-domain");
        retrievedIpAddr = testAddress.getAddress();
        if (!Arrays.equals(retrievedIpAddr, expectedIpAddr1)) {
            throw new RuntimeException(
                    "retrievedIpAddr not equal to expectedIpAddr");
        }

        try {
            addMappingToHostsFile("host3.sample-domain", "", hostsFileName,
                    true);
            testAddress = InetAddress.getByName("host3.sample-domain");
            throw new RuntimeException(
                    "Expected UnknownHostException not thrown");
        } catch (UnknownHostException uhEx) {
            System.out.println("UnknownHostException as expected for host host3.sample-domain");
        }

        try {
            addMappingToHostsFile("host4.sample-domain", " ", hostsFileName,
                    true);
            testAddress = InetAddress.getByName("host4.sample-domain");
            throw new RuntimeException(
                    "Expected UnknownHostException not thrown");
        } catch (UnknownHostException uhEx) {
            System.out.println("UnknownHostException as expected for host host4.sample-domain");
        }

        try {
            addMappingToHostsFile("host5.sample-domain", "  ", hostsFileName,
                    true);
            testAddress = InetAddress.getByName("host4.sample-domain");
            throw new RuntimeException(
                    "Expected UnknownHostException not thrown");
        } catch (UnknownHostException uhEx) {
            System.out.println("UnknownHostException as expected for host host5.sample-domain");
        }

        // IPV6 tests

        // IPV6 tests
        addMappingToHostsFile("host-ipv6.sample-domain", "::1", hostsFileName,
                true);
        testAddress = InetAddress.getByName("host-ipv6.sample-domain");
        retrievedIpAddr = testAddress.getAddress();
        if (!Arrays.equals(retrievedIpAddr, expectedIpAddrIpv6_1)) {
            System.out.println("retrieved ipv6 addr == " + Arrays.toString(retrievedIpAddr));
            System.out.println("expected ipv6 addr == " + Arrays.toString(expectedIpAddrIpv6_1));
            throw new RuntimeException(
                    "retrieved IPV6 Addr not equal to expected IPV6 Addr");
        }
    }

    private static void testIpAddressToHostNameMappings(String hostsFileName)
            throws Exception {
        System.out.println(" TEST IP ADDRESS TO HOST MAPPINGS ");
        InetAddress testAddress;
        String retrievedHost;
        String expectedHost = "testHost.testDomain";

        byte[] testHostIpAddr = { 10, 2, 3, 4 };
        byte[] testHostIpAddr2 = { 10, 5, 6, 7 };
        byte[] testHostIpAddr3 = { 10, 8, 9, 10 };
        byte[] testHostIpAddr4 = { 10, 8, 9, 11 };

        // add comment to hosts file
        addMappingToHostsFile("test hosts file for internal NameService ", "#", hostsFileName,
                false);
        addMappingToHostsFile("testHost.testDomain", "10.2.3.4", hostsFileName,
                true);

        testAddress = InetAddress.getByAddress(testHostIpAddr);
        System.out.println("*******   testAddress == " + testAddress);
        retrievedHost = testAddress.getHostName();
        if (!expectedHost.equals(retrievedHost)) {
            throw new RuntimeException(
                    "retrieved host name not equal to expected host name");
        }

        addMappingToHostsFile("testHost.testDomain", "10.5.6.7", hostsFileName,
                true);

        testAddress = InetAddress.getByAddress(testHostIpAddr2);
        System.out.println("*******   testAddress == " + testAddress);
        retrievedHost = testAddress.getHostName();
        System.out.println("*******   retrievedHost == " + retrievedHost);
        if (!expectedHost.equals(retrievedHost)) {
            throw new RuntimeException("retrieved host name " + retrievedHost
                    + " not equal to expected host name" + expectedHost);
        }

        testAddress = InetAddress.getByAddress(testHostIpAddr4);
        System.out.println("*******   testAddress == " + testAddress);
        if ("10.8.9.11".equalsIgnoreCase(testAddress.getCanonicalHostName())) {
            System.out.println("addr = " + addrToString(testHostIpAddr4)
                    + "  resolve to a host address as expected");
        } else {
            System.out.println("addr = " + addrToString(testHostIpAddr4)
                    + " does not resolve as expected, testAddress == " + testAddress.getCanonicalHostName());
            throw new RuntimeException("problem with resolving "
                    + addrToString(testHostIpAddr4));
        }

        try {
            addMappingToHostsFile("", "10.8.9.10", hostsFileName, true);
            testAddress = InetAddress.getByAddress(testHostIpAddr3);
            System.out.println("*******   testAddress == " + testAddress);
            retrievedHost = testAddress.getCanonicalHostName();
        } catch (Throwable t) {
            throw new RuntimeException("problem with resolving "
                    + addrToString(testHostIpAddr3));
        }

    }

    private static String addrToString(byte addr[]) {
        return Byte.toString(addr[0]) + "." + Byte.toString(addr[1]) + "."
                + Byte.toString(addr[2]) + "." + Byte.toString(addr[3]);
    }

    private static void addMappingToHostsFile( String host,
                                               String addr,
                                               String hostsFileName,
                                               boolean append)
                                               throws Exception {
        String mapping = addr + " " + host;
        try (PrintWriter hfPWriter = new PrintWriter(new BufferedWriter(
                new FileWriter(hostsFileName, append)))) {
            hfPWriter.println(mapping);
        }
    }

}
