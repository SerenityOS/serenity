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
 * @bug 4455376
 * @summary Ensure that socket objects obtained from channels
 *          carry the correct address information
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;


public class Shadow {

    static PrintStream log = System.err;

    private static void dump(ServerSocket s) {
        log.println("getInetAddress(): " + s.getInetAddress());
        log.println("getLocalPort(): " + s.getLocalPort());
    }

    private static void dump(Socket s) {
        log.println("getInetAddress(): " + s.getInetAddress());
        log.println("getPort(): " + s.getPort());
        log.println("getLocalAddress(): " + s.getLocalAddress());
        log.println("getLocalPort(): " + s.getLocalPort());
    }

    private static int problems = 0;

    private static void problem(String s) {
        log.println("FAILURE: " + s);
        problems++;
    }

    private static void check(Socket s) {
        if (s.getPort() == 0)
            problem("Socket has no port");
        if (s.getLocalPort() == 0)
            problem("Socket has no local port");
        if (!s.getLocalAddress().equals(s.getInetAddress()))
            problem("Socket has wrong local address");
    }

    public static void main(String[] args) throws Exception {
        boolean useChannels
            = ((args.length == 0) || Boolean.valueOf(args[0]).booleanValue());
        int port = (args.length > 1 ? Integer.parseInt(args[1]) : -1);

        // open server socket
        ServerSocket serverSocket;
        if (useChannels) {
            ServerSocketChannel serverSocketChannel =
                ServerSocketChannel.open();
            log.println("opened ServerSocketChannel: " +
                      serverSocketChannel);
            serverSocket = serverSocketChannel.socket();
            log.println("associated ServerSocket: " + serverSocket);
        } else {
            serverSocket = new ServerSocket();
            log.println("opened ServerSocket: " + serverSocket);
        }

        // bind server socket to port
        SocketAddress bindAddr =
            new InetSocketAddress((port == -1) ? 0 : port);
        serverSocket.bind(bindAddr);
        log.println("bound ServerSocket: " + serverSocket);

        log.println();

        // open client socket
        Socket socket;
        if (useChannels) {
            SocketChannel socketChannel = SocketChannel.open();
            log.println("opened SocketChannel: " + socketChannel);

            socket = socketChannel.socket();
            log.println("associated Socket: " + socket);
        } else {
            socket = new Socket();
            log.println("opened Socket: " + socket);
        }

        // connect client socket to port
        SocketAddress connectAddr =
            new InetSocketAddress(InetAddress.getLoopbackAddress(),
                                  serverSocket.getLocalPort());
        socket.connect(connectAddr);
        log.println("connected Socket: " + socket);

        log.println();

        // accept connection
        Socket acceptedSocket = serverSocket.accept();
        log.println("accepted Socket: " + acceptedSocket);

        log.println();
        log.println("========================================");

        log.println("*** ServerSocket info: ");
        dump(serverSocket);
        log.println();

        log.println("*** client Socket info: ");
        dump(socket);
        check(socket);
        log.println();

        log.println("*** accepted Socket info: ");
        dump(acceptedSocket);
        check(acceptedSocket);
        log.println();

        if (problems > 0)
            throw new Exception(problems + " tests failed");
    }

}
