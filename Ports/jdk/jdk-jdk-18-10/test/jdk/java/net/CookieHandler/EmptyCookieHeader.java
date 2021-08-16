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

/*
 * @test
 * @bug 8015799
 * @modules jdk.httpserver
 * @summary HttpURLConnection.getHeaderFields() throws IllegalArgumentException
 * @library /test/lib
 * @run main EmptyCookieHeader
 * @run main/othervm -Djava.net.preferIPv6Addresses=true EmptyCookieHeader
 */

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import jdk.test.lib.net.URIBuilder;

public class EmptyCookieHeader {

    public static void main(String[] args) throws Exception {
        new EmptyCookieHeader().runTest();
    }

    public void runTest() throws Exception {
        final CookieHandler oldHandler = CookieHandler.getDefault();
        CookieHandler.setDefault(new TestCookieHandler());
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer s = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        try {
            startServer(s);
            URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(s.getAddress().getPort())
                    .path("/")
                    .toURL();
            HttpURLConnection c = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            c.getHeaderFields();
        } finally {
            CookieHandler.setDefault(oldHandler);
            s.stop(0);
        }
    }

    static void startServer(HttpServer server) throws IOException {
        server.createContext("/", new EmptyCookieHandler());
        server.start();
    }

    static class EmptyCookieHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("GET")) {
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/plain");
                responseHeaders.set("Date", "June 13th 2012");

                // No domain value set
                responseHeaders.set("Set-Cookie2", "name=value");
                responseHeaders.set("Set-Cookie2", "");
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream os = exchange.getResponseBody()) {
                    String str = "This is what the server sent!";
                    os.write(str.getBytes());
                }
            }
        }
    }

    class TestCookieHandler extends CookieHandler {
        @Override
        public Map<String,List<String>> get(URI uri,
                                            Map<String,List<String>> respH) {
            return new HashMap<>();
        }

        @Override
        public void put(URI uri, Map<String,List<String >> respH) { }
    }
}
