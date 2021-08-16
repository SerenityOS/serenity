/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4527345
 * @summary Unit test for DatagramChannel's multicast support
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 *        BasicMulticastTests
 * @run main BasicMulticastTests
 */

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

import jdk.test.lib.NetworkConfiguration;

public class BasicMulticastTests {

    /**
     * Tests that existing membership key is returned by join methods and that
     * membership key methods return the expected results
     */
    static void membershipKeyTests(StandardProtocolFamily family,
                                   InetAddress group,
                                   NetworkInterface nif,
                                   InetAddress source)
        throws IOException
    {
        System.out.format("MembershipKey test using %s @ %s\n",
            group.getHostAddress(), nif.getName());

        DatagramChannel dc = DatagramChannel.open(family)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(source, 0));

        // check existing key is returned
        MembershipKey key = dc.join(group, nif);
        MembershipKey other = dc.join(group, nif);
        if (other != key) {
            throw new RuntimeException("existing key not returned");
        }

        // check key
        if (!key.isValid())
            throw new RuntimeException("key is not valid");
        if (!key.group().equals(group))
            throw new RuntimeException("group is incorrect");
        if (!key.networkInterface().equals(nif))
            throw new RuntimeException("network interface is incorrect");
        if (key.sourceAddress() != null)
            throw new RuntimeException("key is source specific");

        // drop membership
        key.drop();
        if (key.isValid()) {
            throw new RuntimeException("key is still valid");
        }

        // source-specific
        try {
            key = dc.join(group, nif, source);
            other = dc.join(group, nif, source);
            if (other != key) {
                throw new RuntimeException("existing key not returned");
            }
            if (!key.isValid())
                throw new RuntimeException("key is not valid");
            if (!key.group().equals(group))
                throw new RuntimeException("group is incorrect");
            if (!key.networkInterface().equals(nif))
                throw new RuntimeException("network interface is incorrect");
            if (!key.sourceAddress().equals(source))
                throw new RuntimeException("key's source address incorrect");

            // drop membership
            key.drop();
            if (key.isValid()) {
                throw new RuntimeException("key is still valid");
            }
        } catch (UnsupportedOperationException x) {
        }

        // done
        dc.close();
    }

    /**
     * Tests exceptions for invalid arguments or scenarios
     */
    static void exceptionTests(StandardProtocolFamily family,
                               InetAddress group,
                               InetAddress notGroup,
                               NetworkInterface nif,
                               InetAddress loopback)
        throws IOException
    {
        System.out.format("Exception Tests using %s, %s @ %s\n",
            group.getHostAddress(),
            notGroup.getHostAddress(),
            nif.getName());

        DatagramChannel dc = DatagramChannel.open(family)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(0));

        // IllegalStateException
        MembershipKey key;
        key = dc.join(group, nif);
        try {
            dc.join(group, nif, loopback);
            throw new RuntimeException("IllegalStateException not thrown");
        } catch (IllegalStateException x) {
        } catch (UnsupportedOperationException x) {
        }
        key.drop();
        try {
            key = dc.join(group, nif, loopback);
            try {
                dc.join(group, nif);
                throw new RuntimeException("IllegalStateException not thrown");
            } catch (IllegalStateException x) {
            }
            key.drop();
        } catch (UnsupportedOperationException x) {
        }

        // IllegalArgumentException
        try {
            dc.join(notGroup, nif);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException x) {
        }
        try {
            dc.join(notGroup, nif, loopback);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException x) {
        } catch (UnsupportedOperationException x) {
        }

        // NullPointerException
        try {
            dc.join(null, nif);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            dc.join(group, null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            dc.join(group, nif, null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        } catch (UnsupportedOperationException x) {
        }

        dc.close();

        // ClosedChannelException
        try {
            dc.join(group, nif);
            throw new RuntimeException("ClosedChannelException not thrown");
        } catch (ClosedChannelException x) {
        }
        try {
            dc.join(group, nif, loopback);
            throw new RuntimeException("ClosedChannelException not thrown");
        } catch (ClosedChannelException x) {
        } catch (UnsupportedOperationException x) {
        }
    }


    /**
     * Probe interfaces to get interfaces that support IPv4 or IPv6 multicasting
     * and invoke tests.
     */
    public static void main(String[] args) throws IOException {
        NetworkConfiguration.printSystemConfiguration(System.out);

        NetworkConfiguration config = NetworkConfiguration.probe();

        // test IPv4 if available
        {
            var multicastInterfaces = config.ip4MulticastInterfaces().iterator();
            InetAddress group = InetAddress.getByName("225.4.5.6");
            InetAddress notGroup = InetAddress.getByName("1.2.3.4");
            InetAddress loopback = InetAddress.getByName("127.0.0.1");
            while (multicastInterfaces.hasNext()) {
                NetworkInterface nif = multicastInterfaces.next();
                InetAddress anySource = config.ip4Addresses(nif).iterator().next();
                membershipKeyTests(StandardProtocolFamily.INET, group, nif, anySource);
                exceptionTests(StandardProtocolFamily.INET, group, notGroup, nif, loopback);
            }
        }

        // test IPv6 if available
        {
            var multicastInterfaces = config.ip6MulticastInterfaces().iterator();
            InetAddress group = InetAddress.getByName("ff02::a");
            InetAddress notGroup = InetAddress.getByName("fe80::1234");
            InetAddress loopback = InetAddress.getByName("::1");
            while (multicastInterfaces.hasNext()) {
                NetworkInterface nif = multicastInterfaces.next();
                InetAddress anySource = config.ip6Addresses(nif).iterator().next();
                membershipKeyTests(StandardProtocolFamily.INET6, group, nif, anySource);
                exceptionTests(StandardProtocolFamily.INET6, group, notGroup, nif, loopback);
            }
        }
    }
}
