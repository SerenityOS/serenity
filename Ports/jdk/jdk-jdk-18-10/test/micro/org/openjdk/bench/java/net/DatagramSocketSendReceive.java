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

import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Benchmark DatagramSocket send/receive.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class DatagramSocketSendReceive {
    private int counter = 0;

    private DatagramSocket socket1, socket2, connectedSendSocket,
            connectedReceiveSocket, multipleRecieveSocket, multipleSendSocket;
    private DatagramPacket sendPkt1, sendPkt2, connectedSendPkt, multipleSendPkt,
            receivePkt;

    private DatagramSocket[] dsa;
    private DatagramPacket[] pkts;

    @Param({"128", "512", "2048", "8192", "32768"})
    public int size;

    @Param({"4"})
    public int socketCount;

    @Setup
    public void setUp() throws IOException {
        byte[] buf = new byte[size];
        InetAddress addr = InetAddress.getLocalHost();

        receivePkt = new DatagramPacket(buf, buf.length);

        // single send - same socket; different sockets
        socket1 = new DatagramSocket(0, addr);
        socket2 = new DatagramSocket(0, addr);
        sendPkt1 = new DatagramPacket(buf, buf.length, addr,
                socket1.getLocalPort());
        sendPkt2 = new DatagramPacket(buf, buf.length, addr,
                socket2.getLocalPort());

        // connected send/receive
        connectedSendSocket = new DatagramSocket(0, addr);
        connectedReceiveSocket = new DatagramSocket(0, addr);
        connectedSendSocket.connect(addr, connectedReceiveSocket.getLocalPort());
        connectedReceiveSocket.connect(addr, connectedSendSocket.getLocalPort());
        connectedSendPkt = new DatagramPacket(buf, buf.length);

        // multiple senders / multiple receivers
        dsa = new DatagramSocket[socketCount];
        pkts = new DatagramPacket[socketCount];
        for (int i = 0; i < dsa.length; i++) {
            dsa[i] = new DatagramSocket(0, addr);
            pkts[i] = new DatagramPacket(buf, buf.length,
                    addr, dsa[i].getLocalPort());
        }
        multipleRecieveSocket = new DatagramSocket(0, addr);
        multipleSendSocket = new DatagramSocket(0, addr);
        multipleSendPkt = new DatagramPacket(buf, buf.length, addr,
                multipleRecieveSocket.getLocalPort());
    }

    // same sender receiver
    @Benchmark
    public void sendReceiveSingleSocket() throws IOException {
        socket1.send(sendPkt1);
        socket1.receive(receivePkt);
    }

    // single sender, single receiver
    @Benchmark
    public void sendReceive() throws IOException {
        socket1.send(sendPkt2);
        socket2.receive(receivePkt);
    }

    // connected sender receiver
    @Benchmark
    public void sendReceiveConnected() throws IOException {
        connectedSendSocket.send(connectedSendPkt);
        connectedReceiveSocket.receive(receivePkt);
    }

    // multiple senders, single receiver
    @Benchmark
    public void sendMultiple() throws IOException {
        int i = counter;
        dsa[i].send(multipleSendPkt);
        multipleRecieveSocket.receive(receivePkt);
        counter = ++i % dsa.length;
    }

    // single sender, multiple receivers
    @Benchmark
    public void receiveMultiple() throws IOException {
        int i = counter;
        multipleSendSocket.send(pkts[i]);
        dsa[i].receive(receivePkt);
        counter = ++i % dsa.length;
    }

    @TearDown
    public void tearDown() {
        socket1.close();
        socket2.close();
        connectedSendSocket.close();
        connectedReceiveSocket.close();

        multipleRecieveSocket.close();
        multipleSendSocket.close();
        for (DatagramSocket ds : dsa) {
            ds.close();
        }
    }
}
