/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313882 4981129 8143610 8232673
 * @summary Unit test for datagram-socket-channel adaptors
 * @modules java.base/java.net:+open
 * @library .. /test/lib
 * @build jdk.test.lib.Utils TestServers
 * @run main AdaptorBasic
 * @key randomness
 */

import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.lang.reflect.Field;


public class AdaptorBasic {

    static java.io.PrintStream out = System.out;
    static Random rand = new Random();

    static String toString(DatagramPacket dp) {
        return ("DatagramPacket[off=" + dp.getOffset()
                + ", len=" + dp.getLength()
                + "]");
    }

    static int getBufLength(DatagramPacket p) throws Exception {
        Field f = DatagramPacket.class.getDeclaredField("bufLength");
        f.setAccessible(true);
        return (int) f.get(p);
    }

    static void test(DatagramSocket ds, InetSocketAddress dst, boolean shouldTimeout)
        throws Exception
    {
        DatagramPacket op = new DatagramPacket(new byte[100], 13, 42, dst);
        rand.nextBytes(op.getData());
        int bufLength = 100 - 19;
        DatagramPacket ip = new DatagramPacket(new byte[100], 19, bufLength);
        out.println("pre  op: " + toString(op) + "  ip: " + toString(ip));

        long start = System.currentTimeMillis();
        ds.send(op);

        for (;;) {
            try {
                ds.receive(ip);
            } catch (SocketTimeoutException x) {
                if (shouldTimeout) {
                    out.println("Receive timed out, as expected");
                    return;
                }
                throw x;
            }
            break;
        }

        out.println("rtt: " + (System.currentTimeMillis() - start));
        out.println("post op: " + toString(op) + "  ip: " + toString(ip));

        for (int i = 0; i < ip.getLength(); i++) {
            if (ip.getData()[ip.getOffset() + i]
                != op.getData()[op.getOffset() + i])
                throw new Exception("Incorrect data received");
        }

        if (!(ip.getSocketAddress().equals(dst))) {
            throw new Exception("Incorrect sender address, expected: " + dst
                + " actual: " + ip.getSocketAddress());
        }

        if (getBufLength(ip) != bufLength) {
            throw new Exception("DatagramPacket bufLength changed by receive!!!");
        }
    }

    static void test(InetSocketAddress dst,
                     int timeout, boolean shouldTimeout,
                     boolean connect)
        throws Exception
    {
        out.println();
        out.println("dst: " + dst);

        DatagramSocket ds;
        if (false) {
            // Original
            ds = new DatagramSocket();
        } else {
            DatagramChannel dc = DatagramChannel.open();
            ds = dc.socket();
            ds.bind(new InetSocketAddress(0));
        }

        out.println("socket: " + ds);
        if (connect) {
            ds.connect(dst);
            out.println("connect: " + ds);
        }
        InetSocketAddress src = new InetSocketAddress(ds.getLocalAddress(),
                                                      ds.getLocalPort());
        out.println("src: " + src);

        if (timeout > 0)
            ds.setSoTimeout(timeout);
        out.println("timeout: " + ds.getSoTimeout());

        for (int i = 0; i < 5; i++) {
            test(ds, dst, shouldTimeout);
        }

        // Leave the socket open so that we don't reuse the old src address
        //ds.close();

    }

    public static void main(String[] args) throws Exception {
        // need an UDP echo server
        try (TestServers.UdpEchoServer echoServer
                = TestServers.UdpEchoServer.startNewServer(100)) {
            final InetSocketAddress address
                = new InetSocketAddress(echoServer.getAddress(),
                                        echoServer.getPort());
            test(address, 0, false, false);
            test(address, 0, false, true);
            test(address, Integer.MAX_VALUE, false, false);
        }
        try (TestServers.UdpDiscardServer discardServer
                = TestServers.UdpDiscardServer.startNewServer()) {
            final InetSocketAddress address
                = new InetSocketAddress(discardServer.getAddress(),
                                        discardServer.getPort());
            test(address, 10, true, false);
        }
    }

}
