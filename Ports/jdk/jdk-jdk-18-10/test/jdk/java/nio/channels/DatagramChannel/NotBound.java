/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4512723 6621689
 * @summary Test that connect/send/receive with unbound DatagramChannel causes
 *     the channel's socket to be bound to a local address
 */

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.io.IOException;

public class NotBound {

    static void checkBound(DatagramChannel dc) throws IOException {
        if (dc.getLocalAddress() == null)
            throw new RuntimeException("Not bound??");
    }

    // starts a thread to send a datagram to the given channel once the channel
    // is bound to a local address
    static void wakeupWhenBound(final DatagramChannel dc) {
        Runnable wakeupTask = new Runnable() {
            public void run() {
                try {
                    // poll for local address
                    InetSocketAddress local;
                    do {
                        Thread.sleep(50);
                        local = (InetSocketAddress)dc.getLocalAddress();
                    } while (local == null);

                    // send message to channel to wakeup receiver
                    DatagramChannel sender = DatagramChannel.open();
                    try {
                        ByteBuffer bb = ByteBuffer.wrap("hello".getBytes());
                        InetAddress lh = InetAddress.getLocalHost();
                        SocketAddress target =
                            new InetSocketAddress(lh, local.getPort());
                        sender.send(bb, target);
                    } finally {
                        sender.close();
                    }

                } catch (Exception x) {
                    x.printStackTrace();
                }
            }};
        new Thread(wakeupTask).start();
    }

    public static void main(String[] args) throws IOException {
        DatagramChannel dc;

        // connect
        dc = DatagramChannel.open();
        try {
            DatagramChannel peer = DatagramChannel.open()
                .bind(new InetSocketAddress(0));
            int peerPort = ((InetSocketAddress)(peer.getLocalAddress())).getPort();
            try {
                dc.connect(new InetSocketAddress(InetAddress.getLocalHost(), peerPort));
                checkBound(dc);
            } finally {
                peer.close();
            }
        } finally {
            dc.close();
        }

        // send
        dc = DatagramChannel.open();
        try {
            ByteBuffer bb = ByteBuffer.wrap("ignore this".getBytes());
            SocketAddress target =
                new InetSocketAddress(InetAddress.getLocalHost(), 5000);
            dc.send(bb, target);
            checkBound(dc);
        } finally {
            dc.close();
        }

        // receive (blocking)
        dc = DatagramChannel.open();
        try {
            ByteBuffer bb = ByteBuffer.allocateDirect(128);
            wakeupWhenBound(dc);
            SocketAddress sender = dc.receive(bb);
            if (sender == null)
                throw new RuntimeException("Sender should not be null");
            checkBound(dc);
        } finally {
            dc.close();
        }

        // receive (non-blocking)
        dc = DatagramChannel.open();
        try {
            dc.configureBlocking(false);
            ByteBuffer bb = ByteBuffer.allocateDirect(128);
            SocketAddress sender = dc.receive(bb);
            if (sender != null)
                throw new RuntimeException("Sender should be null");
            checkBound(dc);
        } finally {
            dc.close();
        }
    }
}
