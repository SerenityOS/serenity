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
 * @bug 6350055
 * @run testng AtomicUpdates
 * @summary Unit test for SelectionKey interestOpsOr and interestOpsAnd
 */

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.testng.annotations.Test;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static org.testng.Assert.*;

@Test
public class AtomicUpdates {

    private SelectionKey keyFor(SocketChannel sc) {
        return new SelectionKey() {
            private int ops;
            private boolean invalid;
            private void ensureValid() {
                if (!isValid())
                    throw new CancelledKeyException();
            }
            @Override
            public SelectableChannel channel() {
                return sc;
            }
            @Override
            public Selector selector() {
                throw new RuntimeException();
            }
            @Override
            public boolean isValid() {
                return !invalid;
            }
            @Override
            public void cancel() {
                invalid = true;
            }
            @Override
            public int interestOps() {
                ensureValid();
                return ops;
            }
            @Override
            public SelectionKey interestOps(int ops) {
                ensureValid();
                if ((ops & ~channel().validOps()) != 0)
                    throw new IllegalArgumentException();
                this.ops = ops;
                return this;
            }
            @Override
            public int readyOps() {
                ensureValid();
                return 0;
            }
        };
    }

    private void test(SelectionKey key) {
        assertTrue(key.channel() instanceof SocketChannel);
        key.interestOps(0);

        // 0 -> 0
        int previous = key.interestOpsOr(0);
        assertTrue(previous == 0);
        assertTrue(key.interestOps() == 0);

        // 0 -> OP_CONNECT
        previous = key.interestOpsOr(OP_CONNECT);
        assertTrue(previous == 0);
        assertTrue(key.interestOps() == OP_CONNECT);

        // OP_CONNECT -> OP_CONNECT
        previous = key.interestOpsOr(0);
        assertTrue(previous == OP_CONNECT);
        assertTrue(key.interestOps() == OP_CONNECT);

        // OP_CONNECT -> OP_CONNECT | OP_READ | OP_WRITE
        previous = key.interestOpsOr(OP_READ | OP_WRITE);
        assertTrue(previous == OP_CONNECT);
        assertTrue(key.interestOps() == (OP_CONNECT | OP_READ | OP_WRITE));

        // OP_CONNECT | OP_READ | OP_WRITE -> OP_CONNECT
        previous = key.interestOpsAnd(~(OP_READ | OP_WRITE));
        assertTrue(previous == (OP_CONNECT | OP_READ | OP_WRITE));
        assertTrue(key.interestOps() == OP_CONNECT);

        // OP_CONNECT -> 0
        previous = key.interestOpsAnd(~OP_CONNECT);
        assertTrue(previous == OP_CONNECT);
        assertTrue(key.interestOps() == 0);

        // OP_READ | OP_WRITE -> OP_READ | OP_WRITE
        key.interestOps(OP_READ | OP_WRITE);
        previous = key.interestOpsAnd(~OP_ACCEPT);
        assertTrue(previous == (OP_READ | OP_WRITE));
        assertTrue(key.interestOps() == (OP_READ | OP_WRITE));

        // OP_READ | OP_WRITE -> 0
        previous = key.interestOpsAnd(0);
        assertTrue(previous == (OP_READ | OP_WRITE));
        assertTrue(key.interestOps() == 0);

        // 0 -> 0
        previous = key.interestOpsAnd(0);
        assertTrue(previous == 0);
        assertTrue(key.interestOps() == 0);

        try {
            key.interestOpsOr(OP_ACCEPT);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) { }

        key.cancel();
        try {
            key.interestOpsOr(OP_READ);
            fail("CancelledKeyException expected");
        } catch (CancelledKeyException expected) { }
        try {
            key.interestOpsAnd(~OP_READ);
            fail("CancelledKeyException expected");
        } catch (CancelledKeyException expected) { }
    }

    /**
     * Test default implementation of interestOpsOr/interestOpsAnd
     */
    public void testDefaultImplementation() throws Exception {
        try (SocketChannel sc = SocketChannel.open()) {
            SelectionKey key = keyFor(sc);
            test(key);
        }
    }

    /**
     * Test the default provider implementation of SelectionKey.
     */
    public void testNioImplementation() throws Exception {
        try (SocketChannel sc = SocketChannel.open();
             Selector sel = Selector.open()) {
            sc.configureBlocking(false);
            SelectionKey key = sc.register(sel, 0);
            test(key);
        }
    }
}

