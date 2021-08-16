/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4505829
 * @summary Test ready for connect followed by ready for write
 * @library .. /test/lib
 * @build jdk.test.lib.Utils TestServers
 * @run main ConnectWrite
 */

import java.net.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class ConnectWrite {

    public static void main(String[] args) throws Exception {
        try (TestServers.DayTimeServer daytimeServer
                = TestServers.DayTimeServer.startNewServer(25)) {
            test1(daytimeServer);
        }
    }

    static void test1(TestServers.DayTimeServer daytimeServer) throws Exception {
        Selector selector = SelectorProvider.provider().openSelector();
        InetAddress myAddress = daytimeServer.getAddress();
        InetSocketAddress isa
            = new InetSocketAddress(myAddress, daytimeServer.getPort());
        SocketChannel sc = SocketChannel.open();
        try {
            sc.configureBlocking(false);
            SelectionKey key = sc.register(selector, SelectionKey.OP_CONNECT);
            boolean result = sc.connect(isa);

            while (!result) {
                int keysAdded = selector.select(1000);
                if (keysAdded > 0) {
                    Set readyKeys = selector.selectedKeys();
                    Iterator i = readyKeys.iterator();
                    while (i.hasNext()) {
                        SelectionKey sk = (SelectionKey)i.next();
                        readyKeys.remove(sk);
                        SocketChannel nextReady = (SocketChannel)sk.channel();
                        result = nextReady.finishConnect();
                    }
                }
            }
            if (key != null) {
                key.interestOps(SelectionKey.OP_WRITE);
                int keysAdded = selector.select(1000);
                if (keysAdded <= 0)
                    throw new Exception("connect->write failed");
                if (keysAdded > 0) {
                    Set readyKeys = selector.selectedKeys();
                    Iterator i = readyKeys.iterator();
                    while (i.hasNext()) {
                        SelectionKey sk = (SelectionKey)i.next();
                        if (!sk.isWritable())
                            throw new Exception("connect->write failed");
                    }
                }
            }
        } finally {
            sc.close();
            selector.close();
        }
    }
}
