/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4849277 7183800
 * @summary Test DatagramChannel send while connected
 * @library ..
 * @run testng ConnectedSend
 * @author Mike McCloskey
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConnectedSend {
    // Check if DatagramChannel.send while connected can include
    // address without throwing
    @Test
    public static void sendToConnectedAddress() throws Exception {
        DatagramChannel sndChannel = DatagramChannel.open();
        sndChannel.bind(null);
        InetAddress address = InetAddress.getLocalHost();
        if (address.isLoopbackAddress()) {
            address = InetAddress.getLoopbackAddress();
        }
        InetSocketAddress sender = new InetSocketAddress(
            address,
            sndChannel.socket().getLocalPort());

        DatagramChannel rcvChannel = DatagramChannel.open();
        rcvChannel.bind(null);
        InetSocketAddress receiver = new InetSocketAddress(
            address,
            rcvChannel.socket().getLocalPort());

        rcvChannel.connect(sender);
        sndChannel.connect(receiver);

        ByteBuffer bb = ByteBuffer.allocate(256);
        bb.put("hello".getBytes());
        bb.flip();
        int sent = sndChannel.send(bb, receiver);
        bb.clear();
        rcvChannel.receive(bb);
        bb.flip();
        CharBuffer cb = Charset.forName("US-ASCII").newDecoder().decode(bb);
        assertTrue(cb.toString().startsWith("h"), "Unexpected message content");

        rcvChannel.close();
        sndChannel.close();
    }

    // Check if the datagramsocket adaptor can send with a packet
    // that has not been initialized with an address; the legacy
    // datagram socket will send in this case
    @Test
    public static void sendAddressedPacket() throws Exception {
        DatagramChannel sndChannel = DatagramChannel.open();
        sndChannel.bind(null);
        InetAddress address = InetAddress.getLocalHost();
        if (address.isLoopbackAddress()) {
            address = InetAddress.getLoopbackAddress();
        }
        InetSocketAddress sender = new InetSocketAddress(
            address,
            sndChannel.socket().getLocalPort());

        DatagramChannel rcvChannel = DatagramChannel.open();
        rcvChannel.bind(null);
        InetSocketAddress receiver = new InetSocketAddress(
            address,
            rcvChannel.socket().getLocalPort());

        rcvChannel.connect(sender);
        sndChannel.connect(receiver);

        byte b[] = "hello".getBytes("UTF-8");
        DatagramPacket pkt = new DatagramPacket(b, b.length);
        sndChannel.socket().send(pkt);

        ByteBuffer bb = ByteBuffer.allocate(256);
        rcvChannel.receive(bb);
        bb.flip();
        CharBuffer cb = Charset.forName("US-ASCII").newDecoder().decode(bb);
        assertTrue(cb.toString().startsWith("h"), "Unexpected message content");

        // Check that the pkt got set with the target address;
        // This is legacy behavior
        assertEquals(pkt.getSocketAddress(), receiver,
            "Unexpected address set on packet");

        rcvChannel.close();
        sndChannel.close();
    }
}
