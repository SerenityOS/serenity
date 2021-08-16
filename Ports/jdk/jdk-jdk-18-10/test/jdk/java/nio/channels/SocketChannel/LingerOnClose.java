/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8203059
 * @summary Test SocketChannel.close with SO_LINGER enabled
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class LingerOnClose {

    private enum TestMode {
        BLOCKING,
        NON_BLOCKING,
        NON_BLOCKING_AND_REGISTERED;
    }

    public static void main(String[] args) throws IOException {
        // blocking mode
        test(TestMode.BLOCKING, -1);
        test(TestMode.BLOCKING, 0);
        test(TestMode.BLOCKING, 1);

        // non-blocking mode
        test(TestMode.NON_BLOCKING, -1);
        test(TestMode.NON_BLOCKING, 0);
        test(TestMode.NON_BLOCKING, 1);

        // non-blocking mode, close while registered with Selector
        test(TestMode.NON_BLOCKING_AND_REGISTERED, -1);
        test(TestMode.NON_BLOCKING_AND_REGISTERED, 0);
        test(TestMode.NON_BLOCKING_AND_REGISTERED, 1);
    }

    /**
     * Test closing a SocketChannel with SO_LINGER set to the given linger
     * interval. If the linger interval is 0, it checks that the peer observes
     * a connection reset (TCP RST).
     */
    static void test(TestMode mode, int interval) throws IOException {
        SocketChannel sc = null;
        SocketChannel peer = null;
        Selector sel = null;

        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));

            // establish loopback connection
            sc = SocketChannel.open(ssc.getLocalAddress());
            peer = ssc.accept();

            // configured blocking mode and register with Selector if needed
            if (mode != TestMode.BLOCKING)
                sc.configureBlocking(false);
            if (mode == TestMode.NON_BLOCKING_AND_REGISTERED) {
                sel = Selector.open();
                sc.register(sel, SelectionKey.OP_READ);
                sel.selectNow();
            }

            // enable or disable SO_LINGER
            sc.setOption(StandardSocketOptions.SO_LINGER, interval);

            // close channel and flush Selector if needed
            sc.close();
            if (mode == TestMode.NON_BLOCKING_AND_REGISTERED)
                sel.selectNow();

            // read other end of connection, expect EOF or RST
            ByteBuffer bb = ByteBuffer.allocate(100);
            try {
                int n = peer.read(bb);
                if (interval == 0) {
                    throw new RuntimeException("RST expected");
                } else if (n != -1) {
                    throw new RuntimeException("EOF expected");
                }
            } catch (IOException ioe) {
                if (interval != 0) {
                    // exception not expected
                    throw ioe;
                }
            }
        } finally {
            if (sc != null) sc.close();
            if (peer != null) peer.close();
            if (sel != null) sel.close();
        }
    }
}
