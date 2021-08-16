/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7200742
 * @key intermittent
 * @summary Test that Selector doesn't spin when changing interest ops
 */

/* @test
 * @requires (os.family == "windows")
 * @run main/othervm -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider ChangingInterests
 */

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import static java.nio.channels.SelectionKey.*;
import java.io.IOException;

public class ChangingInterests {

    static int OPS[] = { 0, OP_WRITE, OP_READ, (OP_WRITE|OP_READ) };

    static String toOpsString(int ops) {
        String s = "";
        if ((ops & OP_READ) > 0)
            s += "POLLIN";
        if ((ops & OP_WRITE) > 0) {
            if (s.length() > 0)
                s += "|";
            s += "POLLOUT";
        }
        if (s.length() == 0)
            s = "0";
        return "(" + s + ")";
    }

    /**
     * Writes two bytes to 'out' and reads one byte from 'in' so
     * as to make 'in' readable.
     */
    static void makeReadable(SocketChannel out, SocketChannel in) throws IOException {
        out.write(ByteBuffer.wrap(new byte[2]));
        ByteBuffer oneByte = ByteBuffer.wrap(new byte[1]);
        do {
            int n = in.read(oneByte);
            if (n == 1) {
                break;
            } else if (n == 0) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) { }
            } else {
                throw new RuntimeException
                    ("Expected to read 0 or 1 byte; actual number was " + n);
            }
        } while (true);
    }

    static void drain(SocketChannel sc) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(100);
        int n;
        while ((n = sc.read(buf)) > 0) {
            buf.rewind();
        }
    }

    /**
     * Changes the given key's interest set from one set to another and then
     * checks the selected key set and the key's channel.
     */
    static void testChange(SelectionKey key, int from, int to) throws IOException {
        Selector sel = key.selector();
        assertTrue(sel.keys().size() == 1, "Only one channel should be registered");

        // ensure that channel is registered with the "from" interest set
        key.interestOps(from);
        sel.selectNow();
        sel.selectedKeys().clear();

        // change to the "to" interest set
        key.interestOps(to);
        System.out.println("select...");
        int selected = sel.selectNow();
        System.out.println("" + selected + " channel(s) selected");

        int expected = (to == 0) ? 0 : 1;
        assertTrue(selected == expected, "Expected " + expected);

        // check selected keys
        for (SelectionKey k: sel.selectedKeys()) {
            assertTrue(k == key, "Unexpected key selected");

            boolean readable = k.isReadable();
            boolean writable = k.isWritable();

            System.out.println("key readable: " + readable);
            System.out.println("key writable: " + writable);

            if ((to & OP_READ) == 0) {
                assertTrue(!readable, "Not expected to be readable");
            } else {
                assertTrue(readable, "Expected to be readable");
            }

            if ((to & OP_WRITE) == 0) {
                assertTrue(!writable, "Not expected to be writable");
            } else {
                assertTrue(writable, "Expected to be writable");
            }

            sel.selectedKeys().clear();
        }
    }

    /**
     * Tests that given Selector's select method blocks.
     */
    static void testForSpin(Selector sel) throws IOException {
        System.out.println("Test for spin...");
        long start = System.currentTimeMillis();
        int count = 3;
        while (count-- > 0) {
            int selected = sel.select(1000);
            System.out.println("" + selected + " channel(s) selected");
            assertTrue(selected == 0, "Channel should not be selected");
        }
        long dur = System.currentTimeMillis() - start;
        assertTrue(dur > 1000, "select was too short");
    }

    public static void main(String[] args) throws IOException {
        InetAddress lh = InetAddress.getLocalHost();

        // create loopback connection
        ServerSocketChannel ssc =
            ServerSocketChannel.open().bind(new InetSocketAddress(0));

        final SocketChannel sc = SocketChannel.open();
        sc.setOption(StandardSocketOptions.TCP_NODELAY, true);
        sc.connect(new InetSocketAddress(lh, ssc.socket().getLocalPort()));
        SocketChannel peer = ssc.accept();
        peer.setOption(StandardSocketOptions.TCP_NODELAY, true);

        sc.configureBlocking(false);

        // ensure that channel "sc" is readable
        makeReadable(peer, sc);

        try (Selector sel = Selector.open()) {
            SelectionKey key = sc.register(sel, 0);
            sel.selectNow();

            // test all transitions
            for (int from: OPS) {
                for (int to: OPS) {

                    System.out.println(toOpsString(from) + " -> " + toOpsString(to));

                    testChange(key, from, to);

                    // if the interst ops is now 0 then Selector should not spin
                    if (to == 0)
                        testForSpin(sel);

                    // if interest ops is now OP_READ then make non-readable
                    // and test that Selector does not spin.
                    if (to == OP_READ) {
                        System.out.println("Drain channel...");
                        drain(sc);
                        testForSpin(sel);
                        System.out.println("Make channel readable again");
                        makeReadable(peer, sc);
                    }

                    System.out.println();
                }
            }

        } finally {
            sc.close();
            peer.close();
            ssc.close();
        }
    }

    static void assertTrue(boolean v, String msg) {
        if (!v) throw new RuntimeException(msg);
    }

}
