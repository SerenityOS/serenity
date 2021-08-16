/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test nonblocking connect and finishConnect
 * @bug 4457776
 * @library .. /test/lib
 * @build jdk.test.lib.Utils TestServers
 * @run main BasicConnect
 */

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;


/**
 * Typically there would be more than one channel registered to select
 * on, this test is just a very simple version with only one channel
 * registered for the connectSelector.
 */

public class BasicConnect {

    public static void main(String[] args) throws Exception {
        Selector connectSelector =
            SelectorProvider.provider().openSelector();
        try (TestServers.EchoServer echoServer
                = TestServers.EchoServer.startNewServer(100)) {
            InetSocketAddress isa
                = new InetSocketAddress(echoServer.getAddress(),
                                        echoServer.getPort());
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            boolean result = sc.connect(isa);
            if (result) {
                System.out.println("Socket immediately connected on "
                        + System.getProperty("os.name")
                        + ": " + sc);
            }
            while (!result) {
                SelectionKey connectKey = sc.register(connectSelector,
                                                      SelectionKey.OP_CONNECT);
                int keysAdded = connectSelector.select();
                if (keysAdded > 0) {
                    Set readyKeys = connectSelector.selectedKeys();
                    Iterator i = readyKeys.iterator();
                    while (i.hasNext()) {
                        SelectionKey sk = (SelectionKey)i.next();
                        i.remove();
                        SocketChannel nextReady = (SocketChannel)sk.channel();
                        result = nextReady.finishConnect();
                        if (result)
                            sk.cancel();
                    }
                }
            }

            byte[] bs = new byte[] { (byte)0xca, (byte)0xfe,
                                     (byte)0xba, (byte)0xbe };
            ByteBuffer bb = ByteBuffer.wrap(bs);
            sc.configureBlocking(true);
            sc.write(bb);
            bb.rewind();

            ByteBuffer bb2 = ByteBuffer.allocateDirect(100);
            int n = sc.read(bb2);
            bb2.flip();

            sc.close();
            connectSelector.close();

            if (!bb.equals(bb2))
                throw new Exception("Echoed bytes incorrect: Sent "
                                    + bb + ", got " + bb2);
        }
    }
}
