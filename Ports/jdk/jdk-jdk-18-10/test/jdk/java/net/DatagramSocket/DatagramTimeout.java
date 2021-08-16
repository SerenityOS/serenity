/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4163126 8222829
 * @summary Test to see if timeout hangs. Also checks that
 * negative timeout value fails as expected.
 * @run testng DatagramTimeout
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class DatagramTimeout {
    private static final Class<IllegalArgumentException> IAE =
            IllegalArgumentException.class;
    private static final Class<SocketTimeoutException> STE =
            SocketTimeoutException.class;
    private static final Class<SocketException> SE = SocketException.class;

    private DatagramSocket datagramSocket, multicastSocket,
            datagramSocketAdaptor;

    @BeforeTest
    public void setUp() throws Exception {
        datagramSocket = new DatagramSocket();
        multicastSocket = new MulticastSocket();
        datagramSocketAdaptor = DatagramChannel.open().socket();
    }

    @DataProvider(name = "data")
    public Object[][] variants() {
        return new Object[][]{
                { datagramSocket        },
                { datagramSocketAdaptor },
                { multicastSocket       },
        };
    }

    @Test(dataProvider = "data")
    public void testSetNegTimeout(DatagramSocket ds)  {
        assertThrows(IAE, () -> ds.setSoTimeout(-1));
    }

    @Test(dataProvider = "data")
    public void testSetTimeout(DatagramSocket ds) throws Exception {
        byte[] buffer = new byte[50];
        DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
        ds.setSoTimeout(2);
        assertThrows(STE, () -> ds.receive(pkt));
    }

    @Test(dataProvider = "data")
    public void testGetTimeout(DatagramSocket ds) throws Exception {
        ds.setSoTimeout(10);
        assertEquals(10, ds.getSoTimeout());
    }

    @AfterTest
    public void tearDown() {
        datagramSocket.close();
        multicastSocket.close();
        datagramSocketAdaptor.close();
    }

    @Test
    public void testSetGetAfterClose() throws Exception {
        var ds = new DatagramSocket();
        var ms = new MulticastSocket();
        var dsa = DatagramChannel.open().socket();

        ds.close();
        ms.close();
        dsa.close();
        assertThrows(SE, () -> ds.setSoTimeout(10));
        assertThrows(SE, () -> ds.getSoTimeout());
        assertThrows(SE, () -> ms.setSoTimeout(10));
        assertThrows(SE, () -> ms.getSoTimeout());
        assertThrows(SE, () -> dsa.setSoTimeout(10));
        assertThrows(SE, () -> dsa.getSoTimeout());
    }
}
