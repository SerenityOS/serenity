/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231632
 * @summary HttpURLConnection::usingProxy could specify that it lazily evaluates the fact
 * @modules java.base/sun.net.www
 * @library /test/lib
 * @run main/othervm HttpURLConnUsingProxy
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;

public class HttpURLConnUsingProxy {
    static HttpServer server;
    static Proxy proxy;
    static InetSocketAddress isa;

    static class Handler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] response = "Hello World!".getBytes(StandardCharsets.UTF_8);
            try (InputStream req = exchange.getRequestBody()) {
                req.readAllBytes();
            }
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream resp = exchange.getResponseBody()) {
                resp.write(response);
            }
        }
    }

    public static void main(String[] args) {
        try {
            InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
            InetSocketAddress addr = new InetSocketAddress(loopbackAddress, 0);
            server = HttpServer.create(addr, 0);
            server.createContext("/HttpURLConnUsingProxy/http1/", new Handler());
            server.start();

            ProxyServer pserver = new ProxyServer(loopbackAddress,
                    server.getAddress().getPort());
            // Start proxy server
            new Thread(pserver).start();

            URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(server.getAddress().getPort())
                    .path("/HttpURLConnUsingProxy/http1/x.html")
                    .toURLUnchecked();

            // NO_PROXY
            try {
                HttpURLConnection urlc =
                        (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                assertEqual(urlc.usingProxy(), false);
                urlc.getResponseCode();
                assertEqual(urlc.usingProxy(), false);
                urlc.disconnect();
            } catch (IOException ioe) {
                throw new RuntimeException("Direct connection should succeed: "
                        + ioe.getMessage());
            }

            // Non-existing proxy
            try {
                isa = InetSocketAddress.createUnresolved("inexistent", 8080);
                proxy = new Proxy(Proxy.Type.HTTP, isa);
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection(proxy);
                assertEqual(urlc.usingProxy(), true);
                InputStream is = urlc.getInputStream();
                is.close();
                throw new RuntimeException("Non-existing proxy should cause IOException");
            } catch (IOException ioe) {
                // expected
            }

            // Normal proxy settings
            try {
                isa = InetSocketAddress.createUnresolved(loopbackAddress.getHostAddress(),
                        pserver.getPort());
                proxy = new Proxy(Proxy.Type.HTTP, isa);
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection(proxy);
                assertEqual(urlc.usingProxy(), true);
                urlc.getResponseCode();
                assertEqual(urlc.usingProxy(), true);
                urlc.disconnect();
            } catch (IOException ioe) {
                throw new RuntimeException("Connection through local proxy should succeed: "
                        + ioe.getMessage());
            }

            // Reuse proxy with new HttpURLConnection
            try {
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection(proxy);
                assertEqual(urlc.usingProxy(), true);
                urlc.getResponseCode();
                assertEqual(urlc.usingProxy(), true);
                read(urlc.getInputStream());
                assertEqual(urlc.usingProxy(), true);
            } catch (IOException ioe) {
                throw new RuntimeException("Connection through local proxy should succeed: "
                        + ioe.getMessage());
            }

            // Reuse proxy with existing HttpURLConnection
            try {
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection(proxy);
                assertEqual(urlc.usingProxy(), true);
                urlc.getResponseCode();
                assertEqual(urlc.usingProxy(), true);
                read(urlc.getInputStream());
                assertEqual(urlc.usingProxy(), true);
                urlc.disconnect();
            } catch (IOException ioe) {
                throw new RuntimeException("Connection through local proxy should succeed: "
                        + ioe.getMessage());
            }

            // ProxySelector with normal proxy settings
            try {
                ProxySelector.setDefault(ProxySelector.of(isa));
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                assertEqual(urlc.usingProxy(), false);
                urlc.getResponseCode();
                assertEqual(urlc.usingProxy(), true);
                read(urlc.getInputStream());
                assertEqual(urlc.usingProxy(), true);
                urlc.disconnect();
                assertEqual(urlc.usingProxy(), true);
            } catch (IOException ioe) {
                throw new RuntimeException("Connection through local proxy should succeed: "
                        + ioe.getMessage());
            }

            // ProxySelector with proxying disabled
            try {
                ProxySelector.setDefault(ProxySelector.of(null));
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                assertEqual(urlc.usingProxy(), false);
                urlc.getResponseCode();
                assertEqual(urlc.usingProxy(), false);
                read(urlc.getInputStream());
                assertEqual(urlc.usingProxy(), false);
            } catch (IOException ioe) {
                throw new RuntimeException("Direct connection should succeed: "
                        + ioe.getMessage());
            }

            // ProxySelector overwritten
            try {
                ProxySelector.setDefault(ProxySelector.of(isa));
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                assertEqual(urlc.usingProxy(), false);
                urlc.disconnect();
            } catch (IOException ioe) {
                throw new RuntimeException("Direct connection should succeed: "
                        + ioe.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    static class ProxyServer extends Thread {
        private static ServerSocket ss = null;

        // Client requesting a tunnel
        private Socket clientSocket = null;

        /*
         * Origin server's address and port that the client
         * wants to establish the tunnel for communication.
         */
        private InetAddress serverInetAddr;
        private int serverPort;

        public ProxyServer(InetAddress server, int port) throws IOException {
            serverInetAddr = server;
            serverPort = port;
            ss = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        }

        public void run() {
            while (true) {
                try {
                    clientSocket = ss.accept();
                    processRequests();
                } catch (Exception e) {
                    System.out.println("Proxy failed: " + e);
                    e.printStackTrace();
                    try {
                        ss.close();
                    } catch (IOException ioe) {
                        System.out.println("ProxyServer close error: " + ioe);
                        ioe.printStackTrace();
                    }
                }
            }
        }

        private void processRequests() throws Exception {
            // Connection set to tunneling mode

            Socket serverSocket = new Socket(serverInetAddr, serverPort);
            ProxyTunnel clientToServer = new ProxyTunnel(
                    clientSocket, serverSocket);
            ProxyTunnel serverToClient = new ProxyTunnel(
                    serverSocket, clientSocket);
            clientToServer.start();
            serverToClient.start();
            System.out.println("Proxy: Started tunneling...");

            clientToServer.join();
            serverToClient.join();
            System.out.println("Proxy: Finished tunneling...");

            clientToServer.close();
            serverToClient.close();
        }

        /**
         * **************************************************************
         * Helper methods follow
         * **************************************************************
         */
        public int getPort() {
            return ss.getLocalPort();
        }

        /*
         * This inner class provides unidirectional data flow through the sockets
         * by continuously copying bytes from input socket to output socket
         * while both sockets are open and EOF has not been received.
         */
        static class ProxyTunnel extends Thread {
            Socket sockIn;
            Socket sockOut;
            InputStream input;
            OutputStream output;

            public ProxyTunnel(Socket sockIn, Socket sockOut) throws Exception {
                this.sockIn = sockIn;
                this.sockOut = sockOut;
                input = sockIn.getInputStream();
                output = sockOut.getOutputStream();
            }

            public void run() {
                int BUFFER_SIZE = 400;
                byte[] buf = new byte[BUFFER_SIZE];
                int bytesRead = 0;
                int count = 0;    // Keep track of amount of data transferred

                try {
                    while ((bytesRead = input.read(buf)) >= 0) {
                        output.write(buf, 0, bytesRead);
                        output.flush();
                        count += bytesRead;
                    }
                } catch (IOException e) {
                    /*
                     * Peer end has closed connection
                     * so we close tunnel
                     */
                    close();
                }
            }

            public void close() {
                try {
                    if (!sockIn.isClosed())
                        sockIn.close();
                    if (!sockOut.isClosed())
                        sockOut.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void assertEqual(boolean usingProxy, boolean expected) {
        if (usingProxy != expected) {
            throw new RuntimeException("Expected: " + expected
                    + " but usingProxy returned: " + usingProxy);
        }
    }

    private static String read(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        int i = bufferedReader.read();
        while (i != -1) {
            sb.append((char) i);
            i = bufferedReader.read();
        }
        bufferedReader.close();
        return sb.toString();
    }
}
