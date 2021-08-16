/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8156002 8201474
 * @summary Unit test for socket-channel adaptors
 * @library .. /test/lib
 * @build jdk.test.lib.Utils TestServers
 * @run main AdaptSocket
 */

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.Arrays;


public class AdaptSocket {

    static final java.io.PrintStream out = System.out;

    static void test(TestServers.AbstractServer server,
                     int timeout,
                     boolean shouldTimeout)
        throws Exception
    {
        out.println();

        InetSocketAddress isa = new InetSocketAddress(server.getAddress(), server.getPort());
        SocketChannel sc = SocketChannel.open();
        Socket so = sc.socket();
        out.println("opened: " + so);
        out.println("        " + sc);

        //out.println("opts:   " + sc.options());
        so.setTcpNoDelay(true);
        //so.setTrafficClass(SocketOpts.IP.TOS_THROUGHPUT);
        so.setKeepAlive(true);
        so.setSoLinger(true, 42);
        so.setOOBInline(true);
        so.setReceiveBufferSize(512);
        so.setSendBufferSize(512);
        //out.println("        " + sc.options());

        if (timeout == 0)
            so.connect(isa);
        else {
            try {
                so.connect(isa, timeout);
            } catch (SocketTimeoutException x) {
                if (shouldTimeout) {
                    out.println("Connection timed out, as expected");
                    return;
                } else {
                    throw x;
                }
            }
        }
        out.println("connected: " + so);
        out.println("           " + sc);
        byte[] bb = new byte[100];
        int n = so.getInputStream().read(bb);
        String s = new String(bb, 0, n - 2, "US-ASCII");
        out.println(isa + " says: \"" + s + "\"");
        so.shutdownInput();
        out.println("ishut: " + sc);
        so.shutdownOutput();
        out.println("oshut: " + sc);
        so.close();
        out.println("closed: " + so);
        out.println("        " + sc);
    }

    static String dataString = "foo\r\n";

    static void testRead(Socket so, boolean shouldTimeout)
        throws Exception
    {
        String data = "foo\r\n";
        so.getOutputStream().write(dataString.getBytes("US-ASCII"));
        InputStream is = so.getInputStream();
        try {
            byte[] b = new byte[100];
            int n = is.read(b);
            if (shouldTimeout) {
                throw new Exception("Should time out, but not, data: " + Arrays.toString(b));
            }
            if (n != 5) {
                throw new Exception("Incorrect number of bytes read: " + n);
            }
            if (!dataString.equals(new String(b, 0, n, "US-ASCII"))) {
                throw new Exception("Incorrect data read: " + n);
            }
        } catch (SocketTimeoutException x) {
            if (shouldTimeout) {
                out.println("Read timed out, as expected");
                return;
            }
            throw x;
        }
    }

    static void testRead(TestServers.EchoServer echoServer,
                         int timeout,
                         boolean shouldTimeout)
        throws Exception
    {
        out.println();

        InetSocketAddress isa
            = new InetSocketAddress(echoServer.getAddress(),
                                    echoServer.getPort());
        SocketChannel sc = SocketChannel.open();
        sc.connect(isa);
        Socket so = sc.socket();
        out.println("connected: " + so);
        out.println("           " + sc);

        if (timeout > 0)
            so.setSoTimeout(timeout);
        out.println("timeout: " + so.getSoTimeout());

        testRead(so, shouldTimeout);
        for (int i = 0; i < 4; i++) {
            out.println("loop: " + i);
            testRead(so, shouldTimeout);
        }

        sc.close();
    }

    static void testConnect(TestServers.AbstractServer server,
                            int timeout,
                            boolean shouldFail)
        throws Exception
    {
        SocketAddress sa = new InetSocketAddress(server.getAddress(), server.getPort());
        try (SocketChannel sc = SocketChannel.open()) {
            Socket s = sc.socket();
            try {
                if (timeout > 0) {
                    s.connect(sa, timeout);
                } else {
                    s.connect(sa);
                }
                if (shouldFail)
                    throw new Exception("Connection should not be established");
            } catch (SocketException se) {
                if (!shouldFail)
                    throw se;
                out.println("connect failed as expected: " + se);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        try (TestServers.DayTimeServer dayTimeServer
                = TestServers.DayTimeServer.startNewServer()) {
            test(dayTimeServer, 0, false);
            test(dayTimeServer, 1000, false);
        }

        try (TestServers.DayTimeServer lingerDayTimeServer
                = TestServers.DayTimeServer.startNewServer(100)) {
            // this test no longer really test the connection timeout
            // since there is no way to prevent the server from eagerly
            // accepting connection...
            test(lingerDayTimeServer, 10, true);
        }

        try (TestServers.EchoServer echoServer
                = TestServers.EchoServer.startNewServer()) {
            testRead(echoServer, 0, false);
            testRead(echoServer, 8000, false);
        }

        try (TestServers.NoResponseServer noResponseServer
                = TestServers.NoResponseServer.startNewServer()) {
            testRead(noResponseServer, 10, true);
        }

        TestServers.RefusingServer refuser = TestServers.RefusingServer.newRefusingServer();
        testConnect(refuser, 0, true);
        testConnect(refuser, 10000, true);
    }
}
