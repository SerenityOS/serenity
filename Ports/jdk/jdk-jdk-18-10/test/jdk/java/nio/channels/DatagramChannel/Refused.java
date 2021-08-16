/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test DatagramChannel's receive when port unreachable
 * @author Mike McCloskey
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class Refused {

    static ByteBuffer outBuf = ByteBuffer.allocateDirect(100);
    static ByteBuffer inBuf  = ByteBuffer.allocateDirect(100);
    static DatagramChannel client;
    static DatagramChannel server;
    static InetSocketAddress isa;

    public static void main(String[] args) throws Exception {
        outBuf.put("Blah Blah".getBytes());
        outBuf.flip();
        test1();

        // This test has been disabled because there are many circumstances
        // under which no ICMP port unreachable packets are received
        // See http://java.sun.com/j2se/1.4/networking-relnotes.html
        if ((args.length > 0) && (args[0].equals("test2"))) {
            outBuf.rewind();
            test2();
        }
    }

    public static void setup() throws Exception {
        client = DatagramChannel.open();
        server = DatagramChannel.open();

        client.socket().bind((SocketAddress)null);
        server.socket().bind((SocketAddress)null);

        client.configureBlocking(false);
        server.configureBlocking(false);

        InetAddress address = InetAddress.getLocalHost();
        int port = client.socket().getLocalPort();
        isa = new InetSocketAddress(address, port);
    }

    // Since this is not connected no PortUnreachableException should be thrown
    public static void test1() throws Exception {
        setup();

        server.send(outBuf, isa);
        server.receive(inBuf);

        client.close();

        outBuf.rewind();
        server.send(outBuf, isa);
        server.receive(inBuf);

        server.close();
    }

    // Test the connected case to see if PUE is thrown
    public static void test2() throws Exception {

        setup();
        server.configureBlocking(true);
        server.connect(isa);
        server.configureBlocking(false);
        outBuf.rewind();
        server.write(outBuf);
        server.receive(inBuf);

        client.close();
        Thread.sleep(2000);
        outBuf.rewind();

        try {
            server.write(outBuf);
            Thread.sleep(2000);
            inBuf.clear();
            server.read(inBuf);
        } catch (PortUnreachableException pue) {
            System.err.println("received PUE");
        }
        server.close();
    }
}
