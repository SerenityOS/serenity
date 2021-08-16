/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8242885 8250886 8240901
 * @key randomness
 * @summary This test verifies that on macOS, the send buffer size is configured
 *          by default so that none of our implementations of the UDP protocol
 *          will fail with a "packet too large" exception when trying to send a
 *          packet of the maximum possible size allowed by the protocol.
 *          However, an exception is expected if the packet size exceeds that
 *          limit.
 * @library /test/lib
 * @build jdk.test.lib.net.IPSupport
 * @run testng/othervm SendReceiveMaxSize
 * @run testng/othervm -Djava.net.preferIPv4Stack=true SendReceiveMaxSize
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true SendReceiveMaxSize
 */

import jdk.test.lib.RandomFactory;
import jdk.test.lib.Platform;
import jdk.test.lib.net.IPSupport;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.channels.DatagramChannel;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class SendReceiveMaxSize {
    private int BUF_LIMIT;
    private InetAddress HOST_ADDR;
    private final static int IPV4_SNDBUF = 65507;
    private final static int IPV6_SNDBUF = 65527;
    private final static Class<IOException> IOE = IOException.class;
    private final static Random random = RandomFactory.getRandom();

    public interface DatagramSocketSupplier {
        DatagramSocket open() throws IOException;
    }
    static DatagramSocketSupplier supplier(DatagramSocketSupplier supplier) { return supplier; }

    @BeforeTest
    public void setUp() throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();
        HOST_ADDR = InetAddress.getLocalHost();
        BUF_LIMIT = (HOST_ADDR instanceof Inet6Address) ? IPV6_SNDBUF : IPV4_SNDBUF;
        System.out.printf("Host address: %s, Buffer limit: %d%n", HOST_ADDR, BUF_LIMIT);
    }

    @DataProvider
    public Object[][] invariants() {
        var ds = supplier(() -> new DatagramSocket());
        var ms = supplier(() -> new MulticastSocket());
        var dsa = supplier(() -> DatagramChannel.open().socket());
        return new Object[][]{
                { "DatagramSocket",        BUF_LIMIT - 1, ds,   null },
                { "DatagramSocket",        BUF_LIMIT,     ds,   null },
                { "DatagramSocket",        BUF_LIMIT + 1, ds,   IOE  },
                { "MulticastSocket",       BUF_LIMIT - 1, ms,   null },
                { "MulticastSocket",       BUF_LIMIT,     ms,   null },
                { "MulticastSocket",       BUF_LIMIT + 1, ms,   IOE  },
                { "DatagramSocketAdaptor", BUF_LIMIT - 1, dsa,  null },
                { "DatagramSocketAdaptor", BUF_LIMIT,     dsa,  null },
                { "DatagramSocketAdaptor", BUF_LIMIT + 1, dsa,  IOE  },
        };
    }

    @Test(dataProvider = "invariants")
    public void testSendReceiveMaxSize(String name, int capacity,
                                       DatagramSocketSupplier supplier,
                                       Class<? extends Exception> exception) throws IOException {
        try (var receiver = new DatagramSocket(new InetSocketAddress(HOST_ADDR, 0))) {
            var port = receiver.getLocalPort();
            var addr = new InetSocketAddress(HOST_ADDR, port);
            try (var sender = supplier.open()) {
                if (!Platform.isOSX()) {
                    if (sender.getSendBufferSize() < capacity)
                        sender.setSendBufferSize(capacity);
                }
                byte[] testData = new byte[capacity];
                random.nextBytes(testData);
                var sendPkt = new DatagramPacket(testData, capacity, addr);

                if (exception != null) {
                    Exception ex = expectThrows(IOE, () -> sender.send(sendPkt));
                    System.out.println(name + " got expected exception: " + ex);
                } else {
                    sender.send(sendPkt);
                    var receivePkt = new DatagramPacket(new byte[capacity], capacity);
                    receiver.receive(receivePkt);

                    // check packet data has been fragmented and re-assembled correctly at receiver
                    assertEquals(receivePkt.getLength(), capacity);
                    assertEquals(receivePkt.getData(), testData);
                }
            }
        }
    }
}
