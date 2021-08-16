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
 * @summary Test the DatagramChannel socket adaptor getter methods
 * @run testng AdaptorGetters
 */

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class AdaptorGetters {

    /**
     * Test getters on unbound socket, before and after it is closed.
     */
    public void testUnboundSocket() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        DatagramSocket s = dc.socket();
        try {

            // state
            assertFalse(s.isBound());
            assertFalse(s.isConnected());
            assertFalse(s.isClosed());

            // local address
            assertTrue(s.getLocalAddress().isAnyLocalAddress());
            assertTrue(s.getLocalPort() == 0);
            assertTrue(s.getLocalSocketAddress() == null);

            // remote address
            assertTrue(s.getInetAddress() == null);
            assertTrue(s.getPort() == -1);

        } finally {
            dc.close();
        }

        // state
        assertFalse(s.isBound());
        assertFalse(s.isConnected());
        assertTrue(s.isClosed());

        // local address
        assertTrue(s.getLocalAddress() == null);
        assertTrue(s.getLocalPort() == -1);
        assertTrue(s.getLocalSocketAddress() == null);

        // remote address
        assertTrue(s.getInetAddress() == null);
        assertTrue(s.getPort() == -1);
        assertTrue((s.getRemoteSocketAddress() == null));
    }

    /**
     * Test getters on bound socket, before and after it is closed.
     */
    public void testBoundSocket() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        DatagramSocket s = dc.socket();
        try {
            dc.bind(new InetSocketAddress(0));
            var localAddress = (InetSocketAddress) dc.getLocalAddress();

            // state
            assertTrue(s.isBound());
            assertFalse(s.isConnected());
            assertFalse(s.isClosed());

            // local address
            assertEquals(s.getLocalAddress(), localAddress.getAddress());
            assertTrue(s.getLocalPort() == localAddress.getPort());
            assertEquals(s.getLocalSocketAddress(), localAddress);

            // remote address
            assertTrue(s.getInetAddress() == null);
            assertTrue(s.getPort() == -1);
            assertTrue((s.getRemoteSocketAddress() == null));

        } finally {
            dc.close();
        }

        // state
        assertTrue(s.isBound());
        assertFalse(s.isConnected());
        assertTrue(s.isClosed());

        // local address
        assertTrue(s.getLocalAddress() == null);
        assertTrue(s.getLocalPort() == -1);
        assertTrue(s.getLocalSocketAddress() == null);

        // remote address
        assertTrue(s.getInetAddress() == null);
        assertTrue(s.getPort() == -1);
        assertTrue((s.getRemoteSocketAddress() == null));
    }

    /**
     * Test getters on connected socket, before and after it is closed.
     */
    public void testConnectedSocket() throws Exception {
        var loopback = InetAddress.getLoopbackAddress();
        var remoteAddress = new InetSocketAddress(loopback, 7777);
        DatagramChannel dc = DatagramChannel.open();
        DatagramSocket s = dc.socket();
        try {
            dc.connect(remoteAddress);
            var localAddress = (InetSocketAddress) dc.getLocalAddress();

            // state
            assertTrue(s.isBound());
            assertTrue(s.isConnected());
            assertFalse(s.isClosed());

            // local address
            assertEquals(s.getLocalAddress(), localAddress.getAddress());
            assertTrue(s.getLocalPort() == localAddress.getPort());
            assertEquals(s.getLocalSocketAddress(), localAddress);

            // remote address
            assertEquals(s.getInetAddress(), remoteAddress.getAddress());
            assertTrue(s.getPort() == remoteAddress.getPort());
            assertEquals(s.getRemoteSocketAddress(), remoteAddress);

        } finally {
            dc.close();
        }

        // state
        assertTrue(s.isBound());
        assertTrue(s.isConnected());
        assertTrue(s.isClosed());

        // local address
        assertTrue(s.getLocalAddress() == null);
        assertTrue(s.getLocalPort() == -1);
        assertTrue(s.getLocalSocketAddress() == null);

        // remote address
        assertEquals(s.getInetAddress(), remoteAddress.getAddress());
        assertTrue(s.getPort() == remoteAddress.getPort());
        assertEquals(s.getRemoteSocketAddress(), remoteAddress);
    }

    /**
     * Test getters on disconnected socket, before and after it is closed.
     */
    public void testDisconnectedSocket() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        DatagramSocket s = dc.socket();
        try {
            var loopback = InetAddress.getLoopbackAddress();
            dc.connect(new InetSocketAddress(loopback, 7777));
            dc.disconnect();

            var localAddress = (InetSocketAddress) dc.getLocalAddress();

            // state
            assertTrue(s.isBound());
            assertFalse(s.isConnected());
            assertFalse(s.isClosed());

            // local address
            assertEquals(s.getLocalAddress(), localAddress.getAddress());
            assertTrue(s.getLocalPort() == localAddress.getPort());
            assertEquals(s.getLocalSocketAddress(), localAddress);

            // remote address
            assertTrue(s.getInetAddress() == null);
            assertTrue(s.getPort() == -1);
            assertTrue((s.getRemoteSocketAddress() == null));


        } finally {
            dc.close();
        }

        // state
        assertTrue(s.isBound());
        assertFalse(s.isConnected());
        assertTrue(s.isClosed());

        // local address
        assertTrue(s.getLocalAddress() == null);
        assertTrue(s.getLocalPort() == -1);
        assertTrue(s.getLocalSocketAddress() == null);

        // remote address
        assertTrue(s.getInetAddress() == null);
        assertTrue(s.getPort() == -1);
        assertTrue((s.getRemoteSocketAddress() == null));
    }
}
