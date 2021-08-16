/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

import javax.net.ServerSocketFactory;

/*
 * A simple socks proxy for traveling socket.
 */
class SocksProxy implements Runnable, AutoCloseable {

    private ServerSocket server;
    private Consumer<Socket> socketConsumer;

    private SocksProxy(ServerSocket server, Consumer<Socket> socketConsumer) {
        this.server = server;
        this.socketConsumer = socketConsumer;
    }

    static SocksProxy startProxy(Consumer<Socket> socketConsumer)
            throws IOException {
        Objects.requireNonNull(socketConsumer, "socketConsumer cannot be null");

        ServerSocket server
                = ServerSocketFactory.getDefault().createServerSocket(0);

        System.setProperty("socksProxyHost",
                InetAddress.getLoopbackAddress().getHostAddress());
        System.setProperty("socksProxyPort",
                String.valueOf(server.getLocalPort()));
        System.setProperty("socksProxyVersion", "5");

        SocksProxy proxy = new SocksProxy(server, socketConsumer);
        Thread proxyThread = new Thread(proxy, "Proxy");
        proxyThread.setDaemon(true);
        proxyThread.start();

        return proxy;
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try(Socket socket = server.accept()) {
                System.out.println("Server: accepted connection");
                if (socketConsumer != null) {
                    socketConsumer.accept(socket);
                }
            } catch (IOException e) {
                if (!server.isClosed()) {
                    throw new RuntimeException(
                            "Server: accept connection failed", e);
                } else {
                    System.out.println("Server is closed.");
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (!server.isClosed()) {
            server.close();
        }
    }

    int getPort() {
        return server.getLocalPort();
    }
}
