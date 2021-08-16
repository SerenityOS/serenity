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

/**
 * @test
 * @bug 8198372
 * @modules jdk.net java.base/sun.nio.ch:+open
 * @run testng Basic
 * @summary Basic tests for jdk.nio.Channels
 */

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import jdk.nio.Channels;
import jdk.nio.Channels.SelectableChannelCloser;

import sun.nio.ch.IOUtil;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class Basic {

    /**
     * A loopback connection
     */
    static class Connection implements Closeable {
        private final SocketChannel sc1;
        private final SocketChannel sc2;

        private Connection(SocketChannel sc1, SocketChannel sc2) {
            this.sc1 = sc1;
            this.sc2 = sc2;
        }

        static Connection open() throws IOException {
            try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
                InetAddress lb = InetAddress.getLoopbackAddress();
                ssc.bind(new InetSocketAddress(lb, 0));
                SocketChannel sc1 = SocketChannel.open(ssc.getLocalAddress());
                SocketChannel sc2 = ssc.accept();
                return new Connection(sc1, sc2);
            }
        }

        SocketChannel channel1() {
            return sc1;
        }

        SocketChannel channel2() {
            return sc2;
        }

        public void close() throws IOException {
            try {
                sc1.close();
            } finally {
                sc2.close();
            }
        }
    }

    /**
     * A SelectableChannelCloser that tracks if the implCloseChannel and
     * implReleaseChannel methods are invoked
     */
    static class Closer implements SelectableChannelCloser {
        int closeCount;
        SelectableChannel invokedToClose;
        int releaseCount;
        SelectableChannel invokedToRelease;

        @Override
        public void implCloseChannel(SelectableChannel sc) {
            closeCount++;
            invokedToClose = sc;
        }

        @Override
        public void implReleaseChannel(SelectableChannel sc) {
            releaseCount++;
            invokedToRelease = sc;
        }
    }

    /**
     * Basic test of channel registered with Selector
     */
    public void testSelect() throws IOException {
        Selector sel = Selector.open();
        try (Connection connection = Connection.open()) {

            // create channel with the file descriptor from one end of the connection
            FileDescriptor fd = getFD(connection.channel1());
            SelectableChannel ch = Channels.readWriteSelectableChannel(fd, new Closer());

            // register for read events, channel should not be selected
            ch.configureBlocking(false);
            SelectionKey key = ch.register(sel, SelectionKey.OP_READ);
            int n = sel.selectNow();
            assertTrue(n == 0);

            // write bytes to other end of connection
            SocketChannel peer = connection.channel2();
            ByteBuffer msg = ByteBuffer.wrap("hello".getBytes("UTF-8"));
            int nwrote = peer.write(msg);
            assertTrue(nwrote >= 0);

            // channel should be selected
            n = sel.select();
            assertTrue(n == 1);
            assertTrue(sel.selectedKeys().contains(key));
            assertTrue(key.isReadable());
            assertFalse(key.isWritable());
            sel.selectedKeys().clear();

            // change interest set for writing, channel should be selected
            key.interestOps(SelectionKey.OP_WRITE);
            n = sel.select();
            assertTrue(n == 1);
            assertTrue(sel.selectedKeys().contains(key));
            assertTrue(key.isWritable());
            assertFalse(key.isReadable());
            sel.selectedKeys().clear();

            // change interest set for reading + writing, channel should be selected
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            n = sel.select();
            assertTrue(n == 1);
            assertTrue(sel.selectedKeys().contains(key));
            assertTrue(key.isWritable());
            assertTrue(key.isReadable());
            sel.selectedKeys().clear();

            // change interest set to 0 to deregister, channel should not be selected
            key.interestOps(0);
            n = sel.selectNow();
            assertTrue(n == 0);

        } finally {
            sel.close();
        }
    }

    /**
     * Test that the SelectableChannelCloser implCloseChannel method is invoked.
     */
    public void testImplCloseChannel() throws IOException {
        try (Connection connection = Connection.open()) {
            FileDescriptor fd = getFD(connection.channel1());
            Closer closer = new Closer();
            SelectableChannel ch = Channels.readWriteSelectableChannel(fd, closer);

            // close channel twice, checking that the closer is invoked only once
            for (int i=0; i<2; i++) {
                ch.close();

                // implCloseChannel should been invoked once
                assertTrue(closer.closeCount == 1);
                assertTrue(closer.invokedToClose == ch);

                // implReleaseChannel should not have been invoked
                assertTrue(closer.releaseCount == 0);
            }
        }
    }

    /**
     * Test that the SelectableChannelCloser implReleaseChannel method is invoked.
     */
    public void testImplReleaseChannel() throws IOException {
        Selector sel = Selector.open();
        try (Connection connection = Connection.open()) {
            FileDescriptor fd = getFD(connection.channel1());
            Closer closer = new Closer();
            SelectableChannel ch = Channels.readWriteSelectableChannel(fd, closer);

            // register with Selector, invoking selectNow to ensure registered
            ch.configureBlocking(false);
            ch.register(sel, SelectionKey.OP_WRITE);
            sel.selectNow();

            // close channel
            ch.close();

            // implCloseChannel should have been invoked
            assertTrue(closer.closeCount == 1);
            assertTrue(closer.invokedToClose == ch);

            // implReleaseChannel should not have been invoked
            assertTrue(closer.releaseCount == 0);

            // flush the selector
            sel.selectNow();

            // implReleaseChannel should have been invoked
            assertTrue(closer.releaseCount == 1);
            assertTrue(closer.invokedToRelease == ch);

        } finally {
            sel.close();
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidFileDescriptor() throws IOException {
        FileDescriptor fd = IOUtil.newFD(-1);
        Channels.readWriteSelectableChannel(fd, new SelectableChannelCloser() {
            @Override
            public void implCloseChannel(SelectableChannel sc) { }
            @Override
            public void implReleaseChannel(SelectableChannel sc) { }
        });
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullFileDescriptor() throws IOException {
        Channels.readWriteSelectableChannel(null, new SelectableChannelCloser() {
            @Override
            public void implCloseChannel(SelectableChannel sc) { }
            @Override
            public void implReleaseChannel(SelectableChannel sc) { }
        });
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullCloser() throws IOException {
        try (Connection connection = Connection.open()) {
            FileDescriptor fd = getFD(connection.channel1());
            Channels.readWriteSelectableChannel(fd, null);
        }
    }

    private static FileDescriptor getFD(SocketChannel sc) {
        try {
            Class<?> clazz = sc.getClass();
            Field f = clazz.getDeclaredField("fd");
            f.setAccessible(true);
            return (FileDescriptor) f.get(sc);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
