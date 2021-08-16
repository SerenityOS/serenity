/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8161086 8258914
 * @key intermittent
 * @summary DTLS handshaking fails if some messages were lost
 * @modules java.base/sun.security.util
 * @library /test/lib
 * @build DTLSOverDatagram
 *
 * @run main/othervm RespondToRetransmit client 0 hello_request
 * @run main/othervm RespondToRetransmit client 1 client_hello
 * @run main/othervm RespondToRetransmit client 2 server_hello
 * @run main/othervm RespondToRetransmit client 3 hello_verify_request
 * @run main/othervm RespondToRetransmit client 4 new_session_ticket
 * @run main/othervm RespondToRetransmit client 11 certificate
 * @run main/othervm RespondToRetransmit client 12 server_key_exchange
 * @run main/othervm RespondToRetransmit client 13 certificate_request
 * @run main/othervm RespondToRetransmit client 14 server_hello_done
 * @run main/othervm RespondToRetransmit client 15 certificate_verify
 * @run main/othervm RespondToRetransmit client 16 client_key_exchange
 * @run main/othervm RespondToRetransmit client 20 finished
 * @run main/othervm RespondToRetransmit client 21 certificate_url
 * @run main/othervm RespondToRetransmit client 22 certificate_status
 * @run main/othervm RespondToRetransmit client 23 supplemental_data
 * @run main/othervm RespondToRetransmit client -1 change_cipher_spec
 * @run main/othervm RespondToRetransmit server 0 hello_request
 * @run main/othervm RespondToRetransmit server 1 client_hello
 * @run main/othervm RespondToRetransmit server 2 server_hello
 * @run main/othervm RespondToRetransmit server 3 hello_verify_request
 * @run main/othervm RespondToRetransmit server 4 new_session_ticket
 * @run main/othervm RespondToRetransmit server 11 certificate
 * @run main/othervm RespondToRetransmit server 12 server_key_exchange
 * @run main/othervm RespondToRetransmit server 13 certificate_request
 * @run main/othervm RespondToRetransmit server 14 server_hello_done
 * @run main/othervm RespondToRetransmit server 15 certificate_verify
 * @run main/othervm RespondToRetransmit server 16 client_key_exchange
 * @run main/othervm RespondToRetransmit server 20 finished
 * @run main/othervm RespondToRetransmit server 21 certificate_url
 * @run main/othervm RespondToRetransmit server 22 certificate_status
 * @run main/othervm RespondToRetransmit server 23 supplemental_data
 * @run main/othervm RespondToRetransmit server -1 change_cipher_spec
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
public class RespondToRetransmit extends DTLSOverDatagram {
    private static boolean isClient;
    private static byte handshakeType;

    private boolean needPacketDuplicate = true;

    public static void main(String[] args) throws Exception {
        isClient = args[0].equals("client");
        handshakeType = Byte.parseByte(args[1]);

        RespondToRetransmit testCase = new RespondToRetransmit();
        testCase.runTest(testCase);
    }

    @Override
    boolean produceHandshakePackets(SSLEngine engine, SocketAddress socketAddr,
            String side, List<DatagramPacket> packets) throws Exception {

        boolean finished = super.produceHandshakePackets(
                engine, socketAddr, side, packets);

        if (needPacketDuplicate && (isClient == engine.getUseClientMode())) {
            DatagramPacket packet = getPacket(packets, handshakeType);
            if (packet != null) {
                needPacketDuplicate = false;

                System.out.println("Duplicate the flight.");
                List<DatagramPacket> duplicates = new ArrayList<>();
                finished = super.produceHandshakePackets(
                        engine, socketAddr, side, duplicates);
                packets.addAll(duplicates);
            }
        }

        return finished;
    }
}
