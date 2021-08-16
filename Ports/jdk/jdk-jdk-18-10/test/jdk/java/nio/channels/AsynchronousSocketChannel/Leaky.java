/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4607272 6999915 7185340
 * @summary Unit test for AsynchronousSocketChannel
 * @modules java.management
 * @run main/othervm -XX:+DisableExplicitGC -XX:MaxDirectMemorySize=75m Leaky
 */

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;

/**
 * Heap buffers must be substituted with direct buffers when doing I/O. This
 * test creates a scenario on Windows that challenges the per-thread buffer
 * cache and quickly leads to an OutOfMemoryError if temporary buffers are
 * not returned to the native heap.
 */

public class Leaky {

    static final int K = 1024;

    static class Connection {
        private final AsynchronousSocketChannel client;
        private final SocketChannel peer;
        private final ByteBuffer dst;
        private Future<Integer> readResult;

        Connection(AsynchronousChannelGroup group) throws Exception {
            ServerSocketChannel ssc =
                ServerSocketChannel.open().bind(new InetSocketAddress(0));
            InetAddress lh = InetAddress.getLocalHost();
            int port = ((InetSocketAddress)(ssc.getLocalAddress())).getPort();
            SocketAddress remote = new InetSocketAddress(lh, port);
            client = AsynchronousSocketChannel.open(group);
            client.connect(remote).get();
            peer = ssc.accept();
            ssc.close();
            dst = ByteBuffer.allocate(K*K);
        }

        void startRead() {
            dst.clear();
            readResult = client.read(dst);
        }

        void write() throws Exception {
            peer.write(ByteBuffer.wrap("X".getBytes()));
        }

        void finishRead() throws Exception {
            readResult.get();
        }
    }

    public static void main(String[] args) throws Exception {
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        };
        AsynchronousChannelGroup group =
            AsynchronousChannelGroup.withFixedThreadPool(4, threadFactory);

        final int CONNECTION_COUNT = 10;
        Connection[] connections = new Connection[CONNECTION_COUNT];
        for (int i=0; i<CONNECTION_COUNT; i++) {
            connections[i] = new Connection(group);
        }

        for (int i=0; i<1024; i++) {
            // initiate reads
            for (Connection conn: connections) {
                conn.startRead();
            }

            // write data so that the read can complete
            for (Connection conn: connections) {
                conn.write();
            }

            // complete read
            for (Connection conn: connections) {
                conn.finishRead();
            }
        }

        // print summary of buffer pool usage
        List<BufferPoolMXBean> pools =
            ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for (BufferPoolMXBean pool: pools)
            System.out.format("         %8s             ", pool.getName());
        System.out.println();
        for (int i=0; i<pools.size(); i++)
            System.out.format("%6s %10s %10s  ",  "Count", "Capacity", "Memory");
        System.out.println();
        for (BufferPoolMXBean pool: pools) {
            System.out.format("%6d %10d %10d  ",
                pool.getCount(), pool.getTotalCapacity(), pool.getMemoryUsed());
        }
        System.out.println();
    }
}
