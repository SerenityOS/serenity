/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.Executors;
import jdk.test.lib.net.URIBuilder;

/*
 * @test
 * @bug 7025238
 * @modules jdk.httpserver
 * @library /test/lib
 * @summary HttpURLConnection does not handle URLs with an empty path component
 */
public class B7025238 {

    public static void main(String[] args) throws Exception {
        new B7025238().runTest();
    }

    public void runTest() throws Exception {
        Server s = null;
        try {
            s = new Server();
            s.startServer();
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .loopback()
                      .port(s.getPort())
                      .query("q=test")
                      .toURL();
            System.out.println("Connecting to: " + url);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            int code = urlConnection.getResponseCode();

            if (code != 200) {
                throw new RuntimeException("Test failed!");
            }
        } finally {
            s.stopServer();
        }
    }

    class Server {
        HttpServer server;

        public void startServer() {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);
            try {
                server = HttpServer.create(addr, 0);
            } catch (IOException ioe) {
                throw new RuntimeException("Server could not be created");
            }

            server.createContext("/", new EmptyPathHandler());
            server.start();
        }

        public int getPort() {
            return server.getAddress().getPort();
        }

        public void stopServer() {
            server.stop(0);
        }
    }

    class EmptyPathHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();

            if (requestMethod.equalsIgnoreCase("GET")) {
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, 0);
                OutputStream os = exchange.getResponseBody();
                String str = "Hello from server!";
                os.write(str.getBytes());
                os.flush();
                os.close();
            }
        }
    }
}
