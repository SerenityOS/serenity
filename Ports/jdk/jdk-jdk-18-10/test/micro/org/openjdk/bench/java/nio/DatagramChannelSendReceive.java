/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.net;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark DatagramChannel send/receive.
 */

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class DatagramChannelSendReceive {

    private int counter = 0;

    private ByteBuffer buf;
    private DatagramChannel channel1, channel2, connectedWriteChannel,
            connectedReadChannel, multipleReceiveChannel, multipleSendChannel;
    private DatagramChannel[] dca;

    @Param({"128", "512", "2048", "8192", "32768"})
    public int size;
    @Param({"4"})
    public int channelCount;
    @Param({"true"})
    public boolean useDirectBuffer;

    @Setup
    public void setUp() throws IOException {
        buf = (useDirectBuffer) ? ByteBuffer.allocateDirect(size) :
                ByteBuffer.allocate(size);
        buf.clear();

        InetSocketAddress addr =
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        // single send - same socket; different sockets
        channel1 = DatagramChannel.open().bind(addr);
        channel2 = DatagramChannel.open().bind(addr);

        // connected read / write
        connectedWriteChannel = DatagramChannel.open().bind(addr);
        connectedReadChannel = DatagramChannel.open().bind(addr);
        connectedWriteChannel.connect(connectedReadChannel.getLocalAddress());
        connectedReadChannel.connect(connectedWriteChannel.getLocalAddress());

        // multiple senders / multiple receivers
        dca = new DatagramChannel[channelCount];
        for (int i = 0; i < dca.length; i++) {
            dca[i] = DatagramChannel.open().bind(addr);
        }
        multipleReceiveChannel = DatagramChannel.open().bind(addr);
        multipleSendChannel = DatagramChannel.open().bind(addr);
    }

    // same sender receiver
    @Benchmark
    public void sendReceiveSingleSocket() throws IOException {
        buf.clear();
        channel1.send(buf, channel1.getLocalAddress());
        buf.clear();
        channel1.receive(buf);
    }

    // single sender, single receiver
    @Benchmark
    public void sendReceive() throws IOException {
        buf.clear();
        channel1.send(buf, channel2.getLocalAddress());
        buf.clear();
        channel2.receive(buf);
    }

    // connected sender receiver
    @Benchmark
    public void sendReceiveConnected() throws IOException {
        buf.clear();
        connectedWriteChannel.write(buf);
        buf.clear();
        connectedReadChannel.read(buf);
    }

    // multiple senders, single receiver
    @Benchmark
    public void sendMultiple() throws IOException {
        int i = counter;
        buf.clear();
        dca[i].send(buf, multipleReceiveChannel.getLocalAddress());
        buf.clear();
        multipleReceiveChannel.receive(buf);
        counter = ++i % dca.length;
    }

    // single sender, multiple receivers
    @Benchmark
    public void receiveMultiple() throws IOException {
        int i = counter;
        buf.clear();
        multipleSendChannel.send(buf, dca[i].getLocalAddress());
        buf.clear();
        dca[i].receive(buf);
        counter = ++i % dca.length;
    }

    @TearDown
    public void tearDown() throws IOException {
        channel1.close();
        channel2.close();
        connectedWriteChannel.close();
        connectedReadChannel.close();
        multipleReceiveChannel.close();
        multipleSendChannel.close();
        for (DatagramChannel dc : dca) {
            dc.close();
        }
    }
}
