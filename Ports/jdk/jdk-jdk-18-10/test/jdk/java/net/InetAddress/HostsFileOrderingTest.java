/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;


/* @test
 * @bug 8244958
 * @summary Test that "jdk.net.hosts.file" NameService implementation returns addresses
 *          with respect to "java.net.preferIPv4Stack" and "java.net.preferIPv6Addresses" system
 *          property values
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=true HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=system HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=notVALID HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=false -Djava.net.preferIPv6Addresses=true HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=false -Djava.net.preferIPv6Addresses=false HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=false -Djava.net.preferIPv6Addresses=system HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt
 *    -Djava.net.preferIPv4Stack=false -Djava.net.preferIPv6Addresses=notVALID HostsFileOrderingTest
 * @run testng/othervm -Djdk.net.hosts.file=TestHostsFile.txt HostsFileOrderingTest
 */

public class HostsFileOrderingTest {

    /*
     * Generate hosts file with the predefined list of IP addresses
     */
    @BeforeClass
    public void generateHostsFile() throws Exception {
        String content = ADDRESSES_LIST.stream()
                .map(addr -> addr + " " + TEST_HOST_NAME)
                .collect(
                        Collectors.joining(System.lineSeparator(),
                                "# Generated hosts file"+System.lineSeparator(),
                                System.lineSeparator())
                );
        Files.write(HOSTS_FILE_PATH, content.getBytes(StandardCharsets.UTF_8));
    }

    /*
     * Test that HostsFile name service returns addresses in order that complies with the
     *  'sun.net.preferIPv4Stack' and 'preferIPv6Addresses'
     */
    @Test
    public void testOrdering() throws Exception {
        String [] resolvedAddresses = Arrays.stream(InetAddress.getAllByName("hostname.test.com"))
                .map(InetAddress::getHostAddress).toArray(String[]::new);
        String [] expectedAddresses = getExpectedAddressesArray();

        if (Arrays.deepEquals(resolvedAddresses, expectedAddresses)) {
            System.err.println("Test passed: The expected list of IP addresses is returned");
        } else {
            System.err.printf("Expected addresses:%n%s%n", Arrays.deepToString(expectedAddresses));
            System.err.printf("Resolved addresses:%n%s%n", Arrays.deepToString(resolvedAddresses));
            Assert.fail("Wrong host resolution result is returned");
        }
    }

    /*
     * Calculate expected order of IP addresses based on the "preferIPv6Addresses" and "preferIPv4Stack"
     * system property values
     */
    static ExpectedOrder getExpectedOrderFromSystemProperties() {
        if (PREFER_IPV4_STACK_VALUE != null &&
            PREFER_IPV4_STACK_VALUE.equalsIgnoreCase("true")) {
            return ExpectedOrder.IPV4_ONLY;
        }

        if (PREFER_IPV6_ADDRESSES_VALUE != null) {
            return switch(PREFER_IPV6_ADDRESSES_VALUE.toLowerCase()) {
                case "true" -> ExpectedOrder.IPV6_IPV4;
                case "system" -> ExpectedOrder.NO_MODIFICATION;
                default -> ExpectedOrder.IPV4_IPV6;
            };
        }
        return ExpectedOrder.IPV4_IPV6;
    }


    /*
     * Return array expected to be returned by InetAddress::getAllByName call
     */
    static String[] getExpectedAddressesArray() {
        List<String> resList = switch (getExpectedOrderFromSystemProperties()) {
            case IPV4_ONLY -> IPV4_LIST;
            case IPV6_IPV4 -> IPV6_THEN_IPV4_LIST;
            case IPV4_IPV6 -> IPV4_THEN_IPV6_LIST;
            case NO_MODIFICATION -> ADDRESSES_LIST;
        };
        return resList.toArray(String[]::new);
    }


    // Possible types of addresses order
    enum ExpectedOrder {
        IPV4_ONLY,
        IPV6_IPV4,
        IPV4_IPV6,
        NO_MODIFICATION;
    }

    // Addresses list
    private static final List<String> ADDRESSES_LIST = List.of(
            "192.168.239.11",
            "2001:db8:85a3:0:0:8a2e:370:7334",
            "192.168.14.10",
            "2001:85a3:db8:0:0:8a2e:7334:370",
            "192.168.129.16",
            "2001:dead:beef:0:0:8a2e:1239:212"
    );

    // List of IPv4 addresses. The order is as in hosts file
    private static final List<String> IPV4_LIST = ADDRESSES_LIST.stream()
            .filter(ips -> ips.contains("."))
            .collect(Collectors.toUnmodifiableList());

    // List of IPv6 addresses. The order is as in hosts file
    private static final List<String> IPV6_LIST = ADDRESSES_LIST.stream()
            .filter(ip -> ip.contains(":"))
            .collect(Collectors.toUnmodifiableList());

    // List of IPv4 then IPv6 addresses. Orders inside each address type block is the same
    // as in hosts file
    private static final List<String> IPV4_THEN_IPV6_LIST = Stream.of(IPV4_LIST, IPV6_LIST)
            .flatMap(Collection::stream).collect(Collectors.toList());

    // List of IPv6 then IPv4 addresses. Orders inside each address type block is the same
    // as in hosts file
    private static final List<String> IPV6_THEN_IPV4_LIST = Stream.of(IPV6_LIST, IPV4_LIST)
            .flatMap(Collection::stream).collect(Collectors.toList());

    private static final String HOSTS_FILE_NAME = "TestHostsFile.txt";
    private static final Path HOSTS_FILE_PATH = Paths.get(
            System.getProperty("user.dir", ".")).resolve(HOSTS_FILE_NAME);
    private static final String TEST_HOST_NAME = "hostname.test.com";
    private static final String PREFER_IPV6_ADDRESSES_VALUE =
            System.getProperty("java.net.preferIPv6Addresses");
    private static final String PREFER_IPV4_STACK_VALUE =
            System.getProperty("java.net.preferIPv4Stack");
}
