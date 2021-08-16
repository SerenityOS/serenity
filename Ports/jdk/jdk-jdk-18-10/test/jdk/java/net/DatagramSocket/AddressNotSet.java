/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233141
 * @summary DatagramSocket.send should throw IllegalArgumentException
 *          when the packet address is not correctly set.
 * @run main AddressNotSet
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import static java.lang.System.out;

public class AddressNotSet {

    final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
    final DatagramSocket serversock;
    int i;
    AddressNotSet() throws Exception {
        serversock = new DatagramSocket(0, loopbackAddress);
    }

    public static void main (String args[]) throws Exception {
        new AddressNotSet().run();
    }

    public void run() throws Exception {
        try (var ss = serversock) {
            try (DatagramSocket sock = new DatagramSocket()) {
                test(sock);
            }
            try (DatagramSocket sock = new MulticastSocket()) {
                test(sock);
            }
            try (DatagramSocket sock = DatagramChannel.open().socket()) {
                test(sock);
            }
        }
    }

    private void test(DatagramSocket sock) throws Exception {
        out.println("Testing with " + sock.getClass());
        InetAddress addr = loopbackAddress;
        byte[] buf;
        DatagramPacket p;
        int port = serversock.getLocalPort();
        SocketAddress connectedAddress = serversock.getLocalSocketAddress();

        out.println("Checking send to non-connected address ...");
        try {
            out.println("Checking send with no packet address");
            buf = ("Hello, server"+(++i)).getBytes();
            p = new DatagramPacket(buf, buf.length);
            sock.send(p);
            throw new AssertionError("Expected IllegalArgumentException not received");
        } catch (IllegalArgumentException x) {
            out.println("Got expected exception: " + x);
        }

        out.println("Checking send to valid address");
        buf = ("Hello, server"+(++i)).getBytes();
        p = new DatagramPacket(buf, buf.length, addr, port);
        sock.send(p);
        serversock.receive(p);

        out.println("Connecting to server address: " + connectedAddress);
        sock.connect(connectedAddress);

        try {
            out.println("Checking send with different address than connected");
            buf = ("Hello, server"+(++i)).getBytes();
            p = new DatagramPacket(buf, buf.length, addr, port+1);
            sock.send(p);
            throw new AssertionError("Expected IllegalArgumentException not received");
        } catch (IllegalArgumentException x) {
            out.println("Got expected exception: " + x);
        }

        out.println("Checking send to valid address");
        buf = ("Hello, server"+(++i)).getBytes();
        p = new DatagramPacket(buf, buf.length, addr, port);
        sock.send(p);
        serversock.receive(p);

        if (sock instanceof MulticastSocket) {
            sock.disconnect();
            testTTL((MulticastSocket)sock);
        }
    }

    private void testTTL(MulticastSocket sock) throws Exception {
        out.println("Testing deprecated send TTL with " + sock.getClass());
        final byte ttl = 100;
        InetAddress addr = loopbackAddress;
        byte[] buf;
        DatagramPacket p;
        int port = serversock.getLocalPort();

        out.println("Checking send to non-connected address ...");
        try {
            out.println("Checking send with no packet address");
            buf = ("Hello, server"+(++i)).getBytes();
            p = new DatagramPacket(buf, buf.length);
            sock.send(p,ttl);
            throw new AssertionError("Expected IllegalArgumentException not received");
        } catch (IllegalArgumentException x) {
            out.println("Got expected exception: " + x);
        }

        out.println("Connecting to connected address: " + sock);
        sock.connect(addr, port);

        try {
            out.println("Checking send with different address than connected");
            buf = ("Hello, server"+(++i)).getBytes();
            p = new DatagramPacket(buf, buf.length, addr, port+1);
            sock.send(p, ttl);
            throw new AssertionError("Expected IllegalArgumentException not received");
        } catch (IllegalArgumentException x) {
            out.println("Got expected exception: " + x);
        }
    }
}
