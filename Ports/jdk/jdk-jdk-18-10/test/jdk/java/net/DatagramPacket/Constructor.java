/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4091803 7021373
 * @summary this tests that the constructor of DatagramPacket rejects
 *          bogus arguments properly.
 * @run testng Constructor
 */

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class Constructor {

    private static final byte[] buf = new byte[128];

    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();
    private static final Class<NullPointerException> NPE = NullPointerException.class;
    private static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    @Test
    public void testNullPacket() {
        expectThrows(NPE,
                () -> new DatagramPacket(null, 100));
    }
    @Test
    public void testNull() throws Exception {
        expectThrows(NPE, () -> new DatagramPacket(null, 100));
        expectThrows(NPE, () -> new DatagramPacket(null, 0, 10));
        expectThrows(NPE, () -> new DatagramPacket(null, 0, 10, LOOPBACK, 80));
        expectThrows(NPE, () -> new DatagramPacket(null, 10, LOOPBACK, 80));
        expectThrows(NPE, () -> new DatagramPacket(null, 0, 10, new InetSocketAddress(80)));
        expectThrows(NPE, () -> new DatagramPacket(null, 10, new InetSocketAddress(80)));

        // no Exception expected for null addresses
        new DatagramPacket(buf, 10, null, 0);
        new DatagramPacket(buf, 10, 10, null, 0);
    }

    @Test
    public void testNegativeBufferLength() {
        /* length lesser than buffer length */
        expectThrows(IAE,
                () -> new DatagramPacket(buf, -128));
    }

    @Test
    public void testPacketLengthTooLarge() {
        /* length greater than buffer length */
        expectThrows(IAE,
                () -> new DatagramPacket(buf, 256));
    }

    @Test
    public void testNegativePortValue() throws Exception {
        /* negative port */
        InetAddress addr = InetAddress.getLocalHost();

        expectThrows(IAE,
                () -> new DatagramPacket(buf, 100, addr, -1));
    }

    @Test
    public void testPortValueTooLarge() {
        /* invalid port value */
        expectThrows(IAE,
                () -> new DatagramPacket(buf, 128, LOOPBACK, Integer.MAX_VALUE));
    }

    @Test
    public void testSimpleConstructor() {
        int offset = 10;
        int length = 50;
        DatagramPacket pkt = new DatagramPacket(buf, offset, length);

        assertFalse((pkt.getData() != buf || pkt.getOffset() != offset ||
                pkt.getLength() != length), "simple constructor failed");
    }

    @Test
    public void testFullConstructor() {
        int offset = 10;
        int length = 50;
        int port = 8080;
        DatagramPacket packet = new DatagramPacket(buf, offset, length, LOOPBACK, port);

        assertFalse((packet.getData() != buf || packet.getOffset() != offset ||
                packet.getLength() != length ||
                packet.getAddress() != LOOPBACK ||
                packet.getPort() != port), "full constructor failed");
    }

    @Test
    public void testDefaultValues() {
        DatagramPacket packet = new DatagramPacket(buf, 0);
        assertTrue(packet.getAddress() == null);
        assertTrue(packet.getPort() == 0);

        DatagramPacket packet1 = new DatagramPacket(buf, 0, 0);
        assertTrue(packet1.getAddress() == null);
        assertTrue(packet1.getPort() == 0);
    }
}
