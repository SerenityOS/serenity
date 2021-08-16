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
 * @bug 8053479
 * @run main Truncate
 * @summary Test DatagramChannel receive/read where there are fewer bytes remaining
 *     in the buffer than are required to hold the datagram. The remainder of the
 *     datagram should be silently discarded.
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.stream.IntStream;

public class Truncate {
    static final int LARGE_SIZE = 1000;
    static final int SMALL_SIZE = 100;

    public static void main(String[] args) throws Exception {
        try (DatagramChannel dc = DatagramChannel.open()) {
            dc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

            // not connected
            testReceiveDiscards(dc);

            // connected
            dc.connect(dc.getLocalAddress());
            testReceiveDiscards(dc);
            testReadDiscards(dc);
            testScatteringReadDiscards(dc);
        }
    }

    /**
     * Receive a datagram with a buffer that has fewer bytes remaining than are
     * required to hold the datagram.
     */
    static void testReceiveDiscards(DatagramChannel dc) throws IOException {
        ByteBuffer largeBuffer = send(dc, LARGE_SIZE, dc.getLocalAddress());

        ByteBuffer smallBuffer = ByteBuffer.allocate(SMALL_SIZE);
        SocketAddress sender = dc.receive(smallBuffer);
        assertTrue(sender.equals(dc.getLocalAddress()));

        // check buffer/contents
        smallBuffer.flip();
        assertTrue(smallBuffer.remaining() == SMALL_SIZE);
        assertTrue(Arrays.equals(smallBuffer.array(), 0, SMALL_SIZE,
                largeBuffer.array(), 0, SMALL_SIZE));
    }

    /**
     * Read a datagram with a buffer that has fewer bytes remaining than are
     * required to hold the datagram.
     */
    static void testReadDiscards(DatagramChannel dc) throws IOException {
        ByteBuffer largeBuffer = send(dc, LARGE_SIZE, dc.getRemoteAddress());

        ByteBuffer smallBuffer = ByteBuffer.allocate(SMALL_SIZE);
        int n = dc.read(smallBuffer);
        assertTrue(n == SMALL_SIZE);

        // check buffer/contents
        smallBuffer.flip();
        assertTrue(smallBuffer.remaining() == SMALL_SIZE);
        assertTrue(Arrays.equals(smallBuffer.array(), 0, SMALL_SIZE,
                largeBuffer.array(), 0, SMALL_SIZE));
    }

    /**
     * Read a datagram with an array of buffers that have fewer bytes remaining
     * than are required to hold the datagram.
     */
    static void testScatteringReadDiscards(DatagramChannel dc) throws IOException {
        ByteBuffer largeBuffer = send(dc, LARGE_SIZE, dc.getRemoteAddress());

        ByteBuffer smallBuffer1 = ByteBuffer.allocate(SMALL_SIZE);
        ByteBuffer smallBuffer2 = ByteBuffer.allocate(SMALL_SIZE);
        ByteBuffer[] bufs = new ByteBuffer[] { smallBuffer1, smallBuffer2 };
        long n = dc.read(bufs);
        assertTrue(n == (SMALL_SIZE * bufs.length));

        // check buffer/contents
        smallBuffer1.flip();
        assertTrue(smallBuffer1.remaining() == SMALL_SIZE);
        assertTrue(Arrays.equals(smallBuffer1.array(), 0, SMALL_SIZE,
                largeBuffer.array(), 0, SMALL_SIZE));
        smallBuffer2.flip();
        assertTrue(smallBuffer2.remaining() == SMALL_SIZE);
        assertTrue(Arrays.equals(smallBuffer2.array(), 0, SMALL_SIZE,
                largeBuffer.array(), SMALL_SIZE, SMALL_SIZE << 1));
    }

    /**
     * Send a datagram of the given size to the given target address.
     * @return the buffer with the datagram sent to the target address
     */
    static ByteBuffer send(DatagramChannel dc, int size, SocketAddress target)
        throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        IntStream.range(0, size).forEach(i -> buffer.put((byte)i));
        buffer.flip();

        int n = dc.send(buffer, target);
        assertTrue(n == size);
        buffer.flip();
        return buffer;
    }

    static void assertTrue(boolean e) {
        if (!e) throw new RuntimeException();
    }
}
