/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 8043758
 * @summary Datagram Transport Layer Security (DTLS)
 * @modules java.base/sun.security.util
 * @library /test/lib
 * @build DTLSOverDatagram
 * @run main/othervm Retransmission
 */

import java.util.List;
import java.util.ArrayList;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import javax.net.ssl.SSLEngine;

/**
 * Test that DTLS implementation is able to do retransmission internally
 * automatically if packet get lost.
 */
public class Retransmission extends DTLSOverDatagram {
    boolean needPacketLoss = true;

    public static void main(String[] args) throws Exception {
        Retransmission testCase = new Retransmission();
        testCase.runTest(testCase);
    }

    @Override
    boolean produceHandshakePackets(SSLEngine engine, SocketAddress socketAddr,
            String side, List<DatagramPacket> packets) throws Exception {

        boolean finished = super.produceHandshakePackets(
                engine, socketAddr, side, packets);

        if (!needPacketLoss || (!engine.getUseClientMode())) {
            return finished;
        }

        List<DatagramPacket> parts = new ArrayList<>();
        int lostSeq = 2;
        for (DatagramPacket packet : packets) {
            lostSeq--;
            if (lostSeq == 0) {
                needPacketLoss = false;
                // loss this packet
                System.out.println("Loss a packet");
                continue;
            }

            parts.add(packet);
        }

        packets.clear();
        packets.addAll(parts);

        return finished;
    }
}
