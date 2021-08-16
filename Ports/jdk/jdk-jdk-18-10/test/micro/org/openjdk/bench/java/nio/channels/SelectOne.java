/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.java.nio.channels;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for Selector.select(Consumer) when there is one channel ready.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class SelectOne {
    private Selector sel;
    private List<SocketChannel> clients;
    private List<SocketChannel> peers;

    private final int nready = 1;  // one channel ready for reading

    @Param({"1", "10", "100", "1000", "10000"})
    private int nchannels;  // number of registered channels

    @Setup
    public void setup() throws IOException {
        sel = Selector.open();
        clients = new ArrayList<SocketChannel>();
        peers = new ArrayList<SocketChannel>();

        try (ServerSocketChannel listener = ServerSocketChannel.open()) {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            listener.bind(new InetSocketAddress(loopback, 0));

            SocketAddress remote = listener.getLocalAddress();
            for (int i = 0; i < nchannels; i++) {
                SocketChannel sc = SocketChannel.open(remote);
                sc.configureBlocking(false);
                sc.register(sel, SelectionKey.OP_READ);
                clients.add(sc);

                SocketChannel peer = listener.accept();
                peers.add(peer);
            }

            for (int i = nready - 1; i >= 0; i--) {
                SocketChannel peer = peers.get(i);
                peer.write(ByteBuffer.allocate(1));
            }
        }
    }

    @TearDown
    public void teardown() throws IOException {
        for (SocketChannel sc: clients) {
            sc.close();
        }
        for (SocketChannel sc: peers) {
            sc.close();
        }
        if (sel != null) {
            sel.close();
        }
    }

    @Benchmark
    public void testSelectOne() throws IOException {
        int nselected = sel.select(k -> { });
        if (nselected != 1) {
            throw new RuntimeException();
        }
    }
}
