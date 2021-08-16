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
 * @bug 7021373
 * @summary check that the DatagramPacket setter methods
 *          throw the correct exceptions
 * @run testng Setters
 */


import java.net.DatagramPacket;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class Setters {

    private static final byte[] buf = new byte[128];

    private static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    private static final Class<NullPointerException> NPE = NullPointerException.class;

    @Test
    public void testSetAddress() throws Exception {
        DatagramPacket pkt = new DatagramPacket(buf, 8);

        // No Exception expected for null addresses
        pkt.setAddress(null);
        assertTrue(pkt.getAddress() == null);
    }

    @DataProvider
    Object[][] data() { // add checks for setAddress with null - add getAddress to verify
        return new Object[][]{
                { buf,          0,      -1,     IAE  },
                { buf,          -1,     1,      IAE  },
                { buf,          128,    128,    IAE  },
                { null,         0,      0,      NPE  },
                { new byte[8],  2,      2,      null },
                { buf,          2,      2,      null },
                { buf,          20,     20,     null },
        };
    }

    @Test(dataProvider = "data")
    public void testSetData(byte[] buf,
                            int offset,
                            int length,
                            Class<? extends Exception> exception) {
        DatagramPacket pkt = new DatagramPacket(new byte[8], 8);

        if (exception != null) {
            expectThrows(exception, () -> pkt.setData(buf, offset, length));
        } else if (buf == null) {
            expectThrows(exception, () -> pkt.setData(buf));
        } else {
            pkt.setData(buf, offset, length);
        }
    }

    @DataProvider
    Object[][] lengths() {
        return new Object[][]{
                { 0,     -1,     IAE  },
                { 8,     1,      IAE  },
                { 4,     -2,     IAE  },
                { 0,     9,      IAE  },
                { 2,     2,      null },
                { 4,     4,      null }
        };
    }

    @Test(dataProvider = "lengths")
    public void testSetLength(int offset, int length,
                              Class<? extends Exception> exception) {
        DatagramPacket pkt = new DatagramPacket(new byte[8], offset, 0);

        if (exception != null) {
            expectThrows(exception, () -> pkt.setLength(length));
        } else {
            pkt.setLength(length);
        }
    }

    @DataProvider
    Object[][] ports() {
        return new Object[][]{
                { -1,                IAE  },
                { -666,              IAE  },
                { Integer.MAX_VALUE, IAE  },
                { 0xFFFF + 1,        IAE  },
                { 0xFFFFFF,          IAE  },
                { 0,                 null },
                { 1,                 null },
                { 666,               null },
                { 0xFFFE,            null },
                { 0xFFFF,            null },
        };
    }

    @Test(dataProvider = "ports")
    public void testSetPort(int port, Class<? extends Exception> exception) {
        DatagramPacket pkt = new DatagramPacket(new byte[8], 0);

        if (exception != null) {
            expectThrows(exception, () -> pkt.setPort(port));
        } else {
            pkt.setPort(port);
        }
    }
}
