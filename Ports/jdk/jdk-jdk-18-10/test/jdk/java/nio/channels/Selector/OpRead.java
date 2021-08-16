/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4755720
 * @summary Test if OP_READ is detected with OP_WRITE in interestOps
 */

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class OpRead {

    static void test() throws Exception {
        ServerSocketChannel ssc = null;
        SocketChannel sc = null;
        SocketChannel peer = null;
        try {
            ssc = ServerSocketChannel.open().bind(new InetSocketAddress(0));

            // loopback connection
            InetAddress lh = InetAddress.getLocalHost();
            sc = SocketChannel.open(new InetSocketAddress(lh, ssc.socket().getLocalPort()));
            peer = ssc.accept();

            // peer sends message so that "sc" will be readable
            int n = peer.write(ByteBuffer.wrap("Hello".getBytes()));
            assert n > 0;

            sc.configureBlocking(false);

            Selector selector = Selector.open();
            SelectionKey key = sc.register(selector, SelectionKey.OP_READ |
                SelectionKey.OP_WRITE);

            boolean done = false;
            int failCount = 0;
            while (!done) {
                int nSelected = selector.select();
                if (nSelected > 0) {
                    if (nSelected > 1)
                        throw new RuntimeException("More than one channel selected");
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        iterator.remove();
                        if (key.isWritable()) {
                            failCount++;
                            if (failCount > 10)
                                throw new RuntimeException("Test failed");
                            Thread.sleep(250);
                        }
                        if (key.isReadable()) {
                            done = true;
                        }
                    }
                }
            }
        } finally {
            if (peer != null) peer.close();
            if (sc != null) sc.close();
            if (ssc != null) ssc.close();
        }
    }

    public static void main(String[] args) throws Exception {
        test();
    }

}
