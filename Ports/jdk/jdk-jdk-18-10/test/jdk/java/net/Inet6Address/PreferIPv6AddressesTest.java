/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8016521
 * @library /test/lib
 * @summary InetAddress should not always re-order addresses returned from name
 *          service
 * @run main/othervm -Djava.net.preferIPv6Addresses=false PreferIPv6AddressesTest
 * @run main/othervm -Djava.net.preferIPv6Addresses=true PreferIPv6AddressesTest
 * @run main/othervm -Djava.net.preferIPv6Addresses=system PreferIPv6AddressesTest
 * @run main/othervm PreferIPv6AddressesTest
 */

import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.stream.IntStream;
import static java.lang.System.out;
import jdk.test.lib.net.IPSupport;

public class PreferIPv6AddressesTest {

    // A name, that if resolves, returns both IPv4 and IPv6 addresses.
    static final String HOST_NAME = "www.google.com";

    static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    static final String preferIPV6Address =
            System.getProperty("java.net.preferIPv6Addresses", "false");

    public static void main(String args[]) throws IOException {
        InetAddress addrs[];
        try {
            addrs = InetAddress.getAllByName(HOST_NAME);
        } catch (UnknownHostException e) {
            out.println("Unknown host " + HOST_NAME + ", cannot run test.");
            return;
        }

        int firstIPv4Address = IntStream.range(0, addrs.length)
                .filter(x -> addrs[x] instanceof Inet4Address)
                .findFirst().orElse(-1);
        int firstIPv6Address = IntStream.range(0, addrs.length)
                .filter(x -> addrs[x] instanceof Inet6Address)
                .findFirst().orElse(-1);

        out.println("IPv6 supported: " + IPSupport.hasIPv6());
        out.println("Addresses: " + Arrays.asList(addrs));

        if (preferIPV6Address.equalsIgnoreCase("true") && firstIPv6Address != -1) {
            int off = firstIPv4Address != -1 ? firstIPv4Address : addrs.length;
            assertAllv6Addresses(addrs, 0, off);
            assertAllv4Addresses(addrs, off, addrs.length);
            assertLoopbackAddress(Inet6Address.class);
            assertAnyLocalAddress(Inet6Address.class);
        } else if (preferIPV6Address.equalsIgnoreCase("false") && firstIPv4Address != -1) {
            int off = firstIPv6Address != -1 ? firstIPv6Address : addrs.length;
            assertAllv4Addresses(addrs, 0, off);
            assertAllv6Addresses(addrs, off, addrs.length);
            assertLoopbackAddress(Inet4Address.class);
            assertAnyLocalAddress(Inet4Address.class);
        } else if (preferIPV6Address.equalsIgnoreCase("system") && IPSupport.hasIPv6()) {
            assertLoopbackAddress(Inet6Address.class);
            assertAnyLocalAddress(Inet6Address.class);
        } else if (preferIPV6Address.equalsIgnoreCase("system") && !IPSupport.hasIPv6()) {
            assertLoopbackAddress(Inet4Address.class);
            assertAnyLocalAddress(Inet4Address.class);
        }
    }

    static void assertAllv4Addresses(InetAddress[] addrs, int off, int len) {
        IntStream.range(off, len)
                 .mapToObj(x -> addrs[x])
                 .forEach(x -> {
                     if (!(x instanceof Inet4Address))
                         throw new RuntimeException("Expected IPv4, got " + x);
                 });
    }

    static void assertAllv6Addresses(InetAddress[] addrs, int off, int len) {
        IntStream.range(off, len)
                .mapToObj(x -> addrs[x])
                .forEach(x -> {
                    if (!(x instanceof Inet6Address))
                        throw new RuntimeException("Expected IPv6, got " + x);
                });
    }

    static void assertLoopbackAddress(Class<?> expectedType) {
        if (!LOOPBACK.getClass().isAssignableFrom(expectedType))
            throw new RuntimeException("Expected " + expectedType
                    + ", got " + LOOPBACK.getClass());
    }

    static void assertAnyLocalAddress(Class<?> expectedType) {
        InetAddress anyAddr = (new InetSocketAddress(0)).getAddress();
        if (!anyAddr.getClass().isAssignableFrom(expectedType))
            throw new RuntimeException("Expected " + expectedType
                    + ", got " + anyAddr.getClass());
    }
}
