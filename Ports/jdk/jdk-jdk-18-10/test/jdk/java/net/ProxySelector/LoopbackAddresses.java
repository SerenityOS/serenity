/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4924226
 * @key intermittent
 * @summary PIT: Can no launch jnlp application via 127.0.0.1 address on the web server.
 *          This test might fail intermittently as it needs a server that
 *          binds to the wildcard address.
 * @modules java.base/sun.net.www
 * @library ../../../sun/net/www/httptest/ /test/lib
 * @build ClosedChannelList TestHttpServer HttpTransaction HttpCallback
 * @compile LoopbackAddresses.java
 * @run main/othervm LoopbackAddresses
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;

/**
 * Our default proxy selector should bypass localhost and loopback
 * addresses when selecting proxies. This is the existing behaviour.
 */

public class LoopbackAddresses implements HttpCallback {
    static TestHttpServer server;

    public void request (HttpTransaction req) {
        req.setResponseEntityBody ("Hello .");
        try {
            req.sendResponse (200, "Ok");
            req.orderlyClose();
        } catch (IOException e) {
        }
    }

    public static void main(String[] args) {
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();

            // This server needs to bind to the wildcard address as we want it
            // to answer both for the loopback and "localhost".
            // Though "localhost" usually point to the loopback there is no
            // hard guarantee.
            server = new TestHttpServer (new LoopbackAddresses(), 1, 10, 0);
            ProxyServer pserver = new ProxyServer(InetAddress.getByName("localhost"), server.getLocalPort());
            // start proxy server
            new Thread(pserver).start();

            System.setProperty("http.proxyHost", loopback.getHostAddress());
            System.setProperty("http.proxyPort", pserver.getPort()+"");

            URL url = new URL("http://localhost:"+server.getLocalPort());

            try {
                HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
                int respCode = urlc.getResponseCode();
                urlc.disconnect();
            } catch (IOException ioex) {
                throw new RuntimeException("direct connection should succeed :"+ioex.getMessage());
            }

            try {
                url = URIBuilder.newBuilder()
                      .scheme("http")
                      .host(loopback.getHostAddress())
                      .port(server.getLocalPort())
                      .toURL();
                HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
                int respCode = urlc.getResponseCode();
                urlc.disconnect();
            } catch (IOException ioex) {
                throw new RuntimeException("direct connection should succeed :"+ioex.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (server != null) {
                server.terminate();
            }
        }

    }

    private static class ProxyServer extends Thread {
        private static ServerSocket ss = null;

        // client requesting for a tunnel
        private Socket clientSocket = null;

        /*
         * Origin server's address and port that the client
         * wants to establish the tunnel for communication.  */
        private InetAddress serverInetAddr;
        private int     serverPort;

        public ProxyServer(InetAddress server, int port) throws IOException {
            serverInetAddr = server;
            serverPort = port;
            ss = new ServerSocket();
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        }

        public void run() {
            try {
                clientSocket = ss.accept();
                throw new RuntimeException("loopback addresses shouldn't go through the proxy "+clientSocket);

            } catch (IOException e) {
                System.out.println("Proxy Failed: " + e);
                e.printStackTrace();
            } finally {
                try {
                    ss.close();
                }
                catch (IOException excep) {
                    System.out.println("ProxyServer close error: " + excep);
                    excep.printStackTrace();
                }
            }
        }

        /**
***************************************************************
*                       helper methods follow
***************************************************************
*/
        public int getPort() {
            return ss.getLocalPort();
        }
    }
}
