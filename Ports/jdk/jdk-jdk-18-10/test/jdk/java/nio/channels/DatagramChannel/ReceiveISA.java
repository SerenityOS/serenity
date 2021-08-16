/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4503641 8130394 8249773
 * @summary Check that DatagramChannel.receive returns a new SocketAddress
 *          when it receives a packet from the same source address but
 *          different endpoint.
 */
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import static java.lang.System.out;

public class ReceiveISA {

    public static void main(String args[]) throws Exception {

        String regex = "Dia duit![0-2]";

        try (DatagramChannel dc1 = DatagramChannel.open();        // client
             DatagramChannel dc2 = DatagramChannel.open();        // client
             DatagramChannel dc3 = DatagramChannel.open();
             DatagramChannel dc4 = DatagramChannel.open()) {      // client

            dc3.socket().bind((SocketAddress) null); // bind server to any port

            // get server address
            InetAddress lh = InetAddress.getLocalHost();
            InetSocketAddress isa = new InetSocketAddress(lh, dc3.socket().getLocalPort());

            ByteBuffer bb = ByteBuffer.allocateDirect(100);
            bb.put("Dia duit!0".getBytes());
            bb.flip();

            ByteBuffer bb1 = ByteBuffer.allocateDirect(100);
            bb1.put("Dia duit!1".getBytes());
            bb1.flip();

            ByteBuffer bb2 = ByteBuffer.allocateDirect(100);
            bb2.put("Dia duit!2".getBytes());
            bb2.flip();

            ByteBuffer bb3 = ByteBuffer.allocateDirect(100);
            bb3.put("garbage".getBytes());
            bb3.flip();

            dc1.send(bb, isa);      // packet 1 from dc1
            dc4.send(bb3, isa);     // interference, packet 4 from dc4
            dc1.send(bb1, isa);     // packet 2 from dc1
            dc2.send(bb2, isa);     // packet 3 from dc2


            // receive 4 packets
            dc3.socket().setSoTimeout(1000);
            ByteBuffer rb = ByteBuffer.allocateDirect(100);
            SocketAddress sa[] = new SocketAddress[3];

            for (int i = 0; i < 3;) {
                SocketAddress receiver = dc3.receive(rb);
                rb.flip();
                byte[] bytes = new byte[rb.limit()];
                rb.get(bytes, 0, rb.limit());
                String msg = new String(bytes);

                if (msg.matches("Dia duit![0-2]")) {
                    if (msg.equals("Dia duit!0")) {
                        sa[0] = receiver;
                        i++;
                    }
                    if (msg.equals("Dia duit!1")) {
                        sa[1] = receiver;
                        i++;
                    }
                    if (msg.equals("Dia duit!2")) {
                        sa[2] = receiver;
                        i++;
                    }
                } else {
                    out.println("Interfered packet sender address is : " + receiver);
                    out.println("random interfered packet is : " + msg);
                }
                rb.clear();
            }

            /*
             * Check that sa[0] equals sa[1] (both from dc1)
             * Check that sa[1] not equal to sa[2] (one from dc1, one from dc2)
             */

            if (!sa[0].equals(sa[1])) {
                throw new Exception("Source address for packets 1 & 2 should be equal");
            }

            if (sa[1].equals(sa[2])) {
                throw new Exception("Source address for packets 2 & 3 should be different");
            }
        }
    }
}
