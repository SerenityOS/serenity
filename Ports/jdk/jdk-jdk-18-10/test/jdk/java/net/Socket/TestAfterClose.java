/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6505016
 * @library /test/lib
 * @summary Socket spec should clarify what getInetAddress/getPort/etc return
 *          after the Socket is closed
 * @run main TestAfterClose
 * @run main/othervm -Djava.net.preferIPv4Stack=true TestAfterClose
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.IPSupport;

public class TestAfterClose
{
    static int failCount;

    public static void main(String[] args) {
        IPSupport.throwSkippedExceptionIfNonOperational();

        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            ServerSocket ss = new ServerSocket(0, 0, loopback);
            Socket socket = new Socket(loopback, ss.getLocalPort());
            ss.accept();
            ss.close();
            test(socket);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if (failCount > 0)
            throw new RuntimeException("Failed: failcount = " + failCount);

    }

    static void test(Socket socket) throws IOException {
        //Before Close
        int socketPort = socket.getPort();
        InetAddress socketInetAddress = socket.getInetAddress();
        SocketAddress socketRemoteSocketAddress = socket.getRemoteSocketAddress();
        int socketLocalPort = socket.getLocalPort();

        //After Close
        socket.close();

        if (socketPort != socket.getPort()) {
            System.out.println("Socket.getPort failed");
            failCount++;
        }

        if (!socket.getInetAddress().equals(socketInetAddress)) {
            System.out.println("Socket.getInetAddress failed");
            failCount++;
        }

        if (!socket.getRemoteSocketAddress().equals(socketRemoteSocketAddress)) {
            System.out.println("Socket.getRemoteSocketAddresss failed");
            failCount++;
        }

        if (socketLocalPort != socket.getLocalPort()) {
            System.out.println("Socket.getLocalPort failed");
            failCount++;
        }

        InetAddress anyAddr = null;
        try {
            anyAddr = InetAddress.getByAddress("",new byte[] {0,0,0,0});
        } catch (UnknownHostException uhe) {
        }

        if (anyAddr != null && !socket.getLocalAddress().equals(anyAddr)) {
            System.out.println("Socket.getLocalAddress failed");
            failCount++;
        }

        InetSocketAddress addr = new InetSocketAddress(socket.getLocalPort());
        if (!socket.getLocalSocketAddress().equals(addr)) {
            System.out.println("Socket.getLocalSocketAddress failed");
            failCount++;
        }

        if (!socket.isConnected()) {
            System.out.println("Socket.isConnected failed");
            failCount++;
        }

        if (!socket.isBound()) {
            System.out.println("Socket.isBound failed");
            failCount++;
        }
    }
}
