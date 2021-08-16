/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.String.format;
import static java.lang.System.out;
import static java.net.StandardSocketOptions.IP_MULTICAST_IF;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @bug 8236441
 * @summary Bound MulticastSocket fails when setting outbound interface on Windows
 * @library /test/lib
 * @run testng IPMulticastIF
 * @run testng/othervm -Djava.net.preferIPv4Stack=true IPMulticastIF
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true IPMulticastIF
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=true IPMulticastIF
 */
public class IPMulticastIF {

    @BeforeTest
    public void sanity() {
        IPSupport.throwSkippedExceptionIfNonOperational();
        NetworkConfiguration.printSystemConfiguration(out);
    }

    @DataProvider(name = "scenarios")
    public Object[][] positive() throws Exception {
        List<InetAddress> addrs = List.of(InetAddress.getLocalHost(),
                                          InetAddress.getLoopbackAddress());
        List<Object[]> list = new ArrayList<>();
        NetworkConfiguration nc = NetworkConfiguration.probe();
        addrs.stream().forEach(a -> nc.multicastInterfaces(true)
                                      .map(nif -> new Object[] { new InetSocketAddress(a, 0), nif })
                                      .forEach(list::add) );

        return list.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "scenarios")
    public void testSetGetInterfaceBound(InetSocketAddress bindAddr, NetworkInterface nif)
        throws Exception
    {
        out.println(format("\n\n--- testSetGetInterfaceBound bindAddr=[%s], nif=[%s]", bindAddr, nif));
        try (MulticastSocket ms = new MulticastSocket(bindAddr)) {
            ms.setNetworkInterface(nif);
            NetworkInterface msNetIf = ms.getNetworkInterface();
            assertEquals(msNetIf, nif);
        }
    }

    @Test(dataProvider = "scenarios")
    public void testSetGetInterfaceUnbound(InetSocketAddress ignore, NetworkInterface nif)
        throws Exception
    {
        out.println(format("\n\n--- testSetGetInterfaceUnbound nif=[%s]", nif));
        try (MulticastSocket ms = new MulticastSocket()) {
            ms.setNetworkInterface(nif);
            NetworkInterface msNetIf = ms.getNetworkInterface();
            assertEquals(msNetIf, nif);
        }
    }

    @Test(dataProvider = "scenarios")
    public void testSetGetOptionBound(InetSocketAddress bindAddr, NetworkInterface nif)
        throws Exception
    {
        out.println(format("\n\n--- testSetGetOptionBound bindAddr=[%s], nif=[%s]", bindAddr, nif));
        try (MulticastSocket ms = new MulticastSocket(bindAddr)) {
            ms.setOption(IP_MULTICAST_IF, nif);
            NetworkInterface msNetIf = ms.getOption(IP_MULTICAST_IF);
            assertEquals(msNetIf, nif);
        }
    }

    @Test(dataProvider = "scenarios")
    public void testSetGetOptionUnbound(InetSocketAddress ignore, NetworkInterface nif)
        throws Exception
    {
        out.println(format("\n\n--- testSetGetOptionUnbound nif=[%s]", nif));
        try (MulticastSocket ms = new MulticastSocket()) {
            ms.setOption(IP_MULTICAST_IF, nif);
            NetworkInterface msNetIf = ms.getOption(IP_MULTICAST_IF);
            assertEquals(msNetIf, nif);
        }
    }

    // -- get without set

    @DataProvider(name = "bindAddresses")
    public Object[][] bindAddresses() throws Exception {
        return new Object[][] {
            { new InetSocketAddress(InetAddress.getLocalHost(), 0)       },
            { new InetSocketAddress(InetAddress.getLoopbackAddress(), 0) },
        };
    }

    @Test(dataProvider = "bindAddresses")
    public void testGetInterfaceBound(InetSocketAddress bindAddr)
        throws Exception
    {
        out.println(format("\n\n--- testGetInterfaceBound bindAddr=[%s]", bindAddr));
        try (MulticastSocket ms = new MulticastSocket(bindAddr)) {
            assertPlaceHolder(ms.getNetworkInterface());
        }
    }

    @Test
    public void testGettInterfaceUnbound() throws Exception {
        out.println("\n\n--- testGettInterfaceUnbound ");
        try (MulticastSocket ms = new MulticastSocket()) {
            assertPlaceHolder(ms.getNetworkInterface());
        }
    }

    @Test(dataProvider = "bindAddresses")
    public void testGetOptionBound(InetSocketAddress bindAddr)
        throws Exception
    {
        out.println(format("\n\n--- testGetOptionBound bindAddr=[%s]", bindAddr));
        try (MulticastSocket ms = new MulticastSocket(bindAddr)) {
            assertEquals(ms.getOption(IP_MULTICAST_IF), null);
        }
    }

    @Test
    public void testGetOptionUnbound() throws Exception {
        out.println("\n\n--- testGetOptionUnbound ");
        try (MulticastSocket ms = new MulticastSocket()) {
            assertEquals(ms.getOption(IP_MULTICAST_IF), null);
        }
    }

    // Asserts that the placeholder NetworkInterface has a single InetAddress
    // that represent any local address.
    static void assertPlaceHolder(NetworkInterface nif) {
        List<InetAddress> addrs = nif.inetAddresses().collect(toList());
        assertEquals(addrs.size(), 1);
        assertTrue(addrs.get(0).isAnyLocalAddress());
    }
}
