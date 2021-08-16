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

/* @test
 * @bug 8243507 8243999
 * @summary Checks to ensure that DatagramSocket constructors behave as expected
 * @run testng Constructor
 */

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;

import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class Constructor {
    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();
    private static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    private class TestSocketAddress extends SocketAddress {
        TestSocketAddress() {
        }
    }

    @Test
    public void testBindAddress() {
        var addr = new TestSocketAddress();
        assertThrows(IllegalArgumentException.class,
                () -> new DatagramSocket(addr));
    }

    @Test
    public void testInvalidPortRange() {
        var invalidPortValues = new int[]{-1, 65536, Integer.MAX_VALUE};
        for (int i : invalidPortValues) {
            assertThrows(IAE, () -> new DatagramSocket(i));
            assertThrows(IAE, () -> new DatagramSocket(i, LOOPBACK));
        }
    }

    @Test
    public void testDSNullAddress() throws IOException {
        try (var ds = new DatagramSocket()) {
            assertTrue(ds.getLocalAddress().isAnyLocalAddress());
        }

        try (var ds1 = new DatagramSocket(null)) {
            assertTrue(ds1.getLocalAddress().isAnyLocalAddress());
        }

        try (var ds2 = new DatagramSocket(0, null)) {
            assertTrue(ds2.getLocalAddress().isAnyLocalAddress());
        }
    }
}
