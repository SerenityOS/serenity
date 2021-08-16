/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4681556
 * @library /test/lib
 * @summary Wrong text if a read is performed on a socket after it
 *      has been closed
 * @run main SocketClosedException
 * @run main/othervm -Djava.net.preferIPv4Stack=true SocketClosedException
 */

import java.io.*;
import java.net.*;
import jdk.test.lib.net.IPSupport;

public class SocketClosedException {
    static void doServerSide() throws Exception {
        try {
            Socket socket = serverSocket.accept();

            OutputStream os = socket.getOutputStream();

            os.write(85);
            os.flush();
            socket.close();
        } finally {
            serverSocket.close();
        }
    }

    static void doClientSide(int port) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        Socket socket = new Socket(loopback, port);
        InputStream is = socket.getInputStream();

        is.read();
        socket.close();
        is.read();
    }

    static ServerSocket serverSocket;
    static Exception serverException = null;

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(loopback, 0));
        startServer();
        try {
            doClientSide(serverSocket.getLocalPort());
        } catch (SocketException e) {
            if (!e.getMessage().equalsIgnoreCase("Socket closed")) {
                throw new Exception("Received a wrong exception message: " +
                                        e.getMessage());
            }
            System.out.println("PASSED: received the right exception message: "
                                        + e.getMessage());
        }
        if (serverException != null) {
            throw serverException;
        }
    }

    static void startServer() {
        (new Thread() {
            public void run() {
                try {
                    doServerSide();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
