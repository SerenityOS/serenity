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
 * @run testng UpdateReadyOps
 * @summary Test that the ready set from a selection operation is bitwise-disjoined
 *     into a key's ready set when the key is already in the selected-key set
 */

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class UpdateReadyOps {

    /**
     * Test that OP_WRITE is preserved when updating the ready set of a key in
     * the selected-key set to add OP_READ.
     */
    public void testOpWritePreserved() throws Exception {
        try (ConnectionPair pair = new ConnectionPair();
             Selector sel = Selector.open()) {

            SocketChannel sc1 = pair.channel1();
            SocketChannel sc2 = pair.channel2();

            sc1.configureBlocking(false);
            SelectionKey key = sc1.register(sel, SelectionKey.OP_WRITE);

            int updated = sel.select();
            assertTrue(updated == 1);
            assertTrue(sel.selectedKeys().contains(key));
            assertFalse(key.isReadable());
            assertTrue(key.isWritable());

            // select again, should be no updates
            updated = sel.select();
            assertTrue(updated == 0);
            assertTrue(sel.selectedKeys().contains(key));
            assertFalse(key.isReadable());
            assertTrue(key.isWritable());

            // write some bytes
            sc2.write(helloMessage());

            // change interest ops to OP_READ, do a selection operation, and
            // check that the ready set becomes OP_READ|OP_WRITE.

            key.interestOps(SelectionKey.OP_READ);
            updated = sel.select();
            assertTrue(updated == 1);
            assertTrue(sel.selectedKeys().size() == 1);
            assertTrue(key.isReadable());
            assertTrue(key.isWritable());
            assertTrue(key.readyOps() == (SelectionKey.OP_READ|SelectionKey.OP_WRITE));

            // select again, should be no updates
            updated = sel.select();
            assertTrue(updated == 0);
            assertTrue(sel.selectedKeys().size() == 1);
            assertTrue(key.isReadable());
            assertTrue(key.isWritable());
        }
    }

    /**
     * Test that OP_READ is preserved when updating the ready set of a key in
     * the selected-key set to add OP_WRITE.
     */
    public void testOpReadPreserved() throws Exception {
        try (ConnectionPair pair = new ConnectionPair();
             Selector sel = Selector.open()) {

            SocketChannel sc1 = pair.channel1();
            SocketChannel sc2 = pair.channel2();

            sc1.configureBlocking(false);
            SelectionKey key = sc1.register(sel, SelectionKey.OP_READ);

            // write some bytes
            sc2.write(helloMessage());

            int updated = sel.select();
            assertTrue(updated == 1);
            assertTrue(sel.selectedKeys().size() == 1);
            assertTrue(sel.selectedKeys().contains(key));
            assertTrue(key.isReadable());
            assertFalse(key.isWritable());

            // select again, should be no updates
            updated = sel.select();
            assertTrue(updated == 0);
            assertTrue(sel.selectedKeys().contains(key));
            assertTrue(key.isReadable());
            assertFalse(key.isWritable());

            key.interestOps(SelectionKey.OP_WRITE);
            updated = sel.select();
            assertTrue(updated == 1);
            assertTrue(sel.selectedKeys().size() == 1);
            assertTrue(sel.selectedKeys().contains(key));
            assertTrue(key.isReadable());
            assertTrue(key.isWritable());
            assertTrue(key.readyOps() == (SelectionKey.OP_READ|SelectionKey.OP_WRITE));

            // select again, should be no updates
            updated = sel.select();
            assertTrue(updated == 0);
            assertTrue(sel.selectedKeys().size() == 1);
            assertTrue(key.isReadable());
            assertTrue(key.isWritable());
        }
    }

    static class ConnectionPair implements Closeable {

        private final SocketChannel sc1;
        private final SocketChannel sc2;

        ConnectionPair() throws IOException {
            InetAddress lb = InetAddress.getLoopbackAddress();
            try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
                ssc.bind(new InetSocketAddress(lb, 0));
                this.sc1 = SocketChannel.open(ssc.getLocalAddress());
                this.sc2 = ssc.accept();
            }
        }

        SocketChannel channel1() {
            return sc1;
        }

        SocketChannel channel2() {
            return sc2;
        }

        public void close() throws IOException {
            sc1.close();
            sc2.close();
        }
    }

    static ByteBuffer helloMessage() throws Exception {
        return ByteBuffer.wrap("hello".getBytes("UTF-8"));
    }
}
