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
import java.util.List;
import java.util.Map;
import static java.net.Proxy.NO_PROXY;

/*
 * @test
 * @bug 7169142
 * @key intermittent
 * @modules jdk.httpserver
 * @summary CookieHandler does not work with localhost. This requires
 *    binding to the wildcard address and might fail intermittently
 *    due to port reuse issues.
 * @run main/othervm LocalHostCookie
 */
public class LocalHostCookie {

    public static void main(String[] args) throws Exception {
        new LocalHostCookie().runTest();
    }

    public void runTest() throws Exception {
        Server s = null;
        try {
            s = new Server();
            s.startServer();
            URL url = new URL("http","localhost", s.getPort(), "/");
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection(NO_PROXY);
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();
            urlConnection.getInputStream();

            CookieHandler cookieHandler = CookieHandler.getDefault();
            if (cookieHandler == null) {
                cookieHandler = new java.net.CookieManager();
                CookieHandler.setDefault(cookieHandler);
            }
            cookieHandler.put(urlConnection.getURL().toURI(),
                    urlConnection.getHeaderFields());
            Map<String, List<String>> map =
                    cookieHandler.get(urlConnection.getURL().toURI(),
                    urlConnection.getHeaderFields());
            if (map.containsKey("Cookie")) {
                List<String> list = map.get("Cookie");
                // name-value list will be empty if ".local" is not appended
                if (list == null || list.size() ==  0) {
                    throw new RuntimeException("Test failed!");
                }
            }
        } finally {
            if (s != null) {
                s.stopServer();
            }
        }
    }

    class Server {
        HttpServer server;

        public void startServer() {
            InetSocketAddress addr = new InetSocketAddress(0);
            try {
                server = HttpServer.create(addr, 0);
            } catch (IOException ioe) {
                throw new RuntimeException("Server could not be created");
            }

            server.createContext("/", new MyCookieHandler());
            server.start();
        }

        public int getPort() {
            return server.getAddress().getPort();
        }

        public void stopServer() {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    class MyCookieHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("GET")){
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/plain");
                responseHeaders.set("Date", "June 13th 2012");
                // No domain value set
                responseHeaders.set("Set-Cookie2", "name=value");
                exchange.sendResponseHeaders(200, 0);
                OutputStream os = exchange.getResponseBody();
                String str = "This is what the server sent!";
                os.write(str.getBytes());
                os.flush();
                os.close();
            }
        }
    }
}
