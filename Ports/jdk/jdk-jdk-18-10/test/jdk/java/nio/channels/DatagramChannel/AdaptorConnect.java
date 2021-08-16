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

/* @test
 * @bug 8232673
 * @summary Test DatagramChannel socket adaptor connect method with illegal args
 * @run testng AdaptorConnect
 */

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import static java.net.InetAddress.getLoopbackAddress;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class AdaptorConnect {

    /**
     * Invoke the given socket's connect method with illegal arguments.
     */
    private void testConnectWithIllegalArguments(DatagramSocket s) {
        assertThrows(IllegalArgumentException.class, () -> s.connect(null));
        assertThrows(IllegalArgumentException.class, () -> s.connect(null, 7000));
        assertThrows(IllegalArgumentException.class, () -> s.connect(getLoopbackAddress(), -1));
        assertThrows(IllegalArgumentException.class, () -> s.connect(getLoopbackAddress(), 100_000));

        SocketAddress sillyAddress = new SocketAddress() { };
        assertThrows(IllegalArgumentException.class, () -> s.connect(sillyAddress));

        SocketAddress unresolved = InetSocketAddress.createUnresolved("foo", 7777);
        assertThrows(SocketException.class, () -> s.connect(unresolved));
    }

    /**
     * Test connect method with an open socket.
     */
    public void testOpenSocket() throws Exception {
        try (DatagramChannel dc = DatagramChannel.open()) {
            DatagramSocket s = dc.socket();

            testConnectWithIllegalArguments(s);

            // should not be bound or connected
            assertTrue(s.getLocalSocketAddress() == null);
            assertTrue(s.getRemoteSocketAddress() == null);

            // connect(SocketAddress)
            var remote1 = new InetSocketAddress(getLoopbackAddress(), 7001);
            s.connect(remote1);
            assertEquals(s.getRemoteSocketAddress(), remote1);
            testConnectWithIllegalArguments(s);
            assertEquals(s.getRemoteSocketAddress(), remote1);

            // connect(SocketAddress)
            var remote2 = new InetSocketAddress(getLoopbackAddress(), 7002);
            s.connect(remote2);
            assertEquals(s.getRemoteSocketAddress(), remote2);
            testConnectWithIllegalArguments(s);
            assertEquals(s.getRemoteSocketAddress(), remote2);

            // connect(InetAddress, int)
            var remote3 = new InetSocketAddress(getLoopbackAddress(), 7003);
            s.connect(remote3.getAddress(), remote3.getPort());
            assertEquals(s.getRemoteSocketAddress(), remote3);
            testConnectWithIllegalArguments(s);
            assertEquals(s.getRemoteSocketAddress(), remote3);

            // connect(InetAddress, int)
            var remote4 = new InetSocketAddress(getLoopbackAddress(), 7004);
            s.connect(remote4.getAddress(), remote4.getPort());
            assertEquals(s.getRemoteSocketAddress(), remote4);
            testConnectWithIllegalArguments(s);
            assertEquals(s.getRemoteSocketAddress(), remote4);
        }
    }

    /**
     * Test connect method with a closed socket.
     */
    public void testClosedSocket() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        DatagramSocket s = dc.socket();
        dc.close();

        testConnectWithIllegalArguments(s);

        // connect does not throw an exception when closed
        var remote = new InetSocketAddress(getLoopbackAddress(), 7001);
        s.connect(remote);
        s.connect(remote.getAddress(), remote.getPort());
    }
}
