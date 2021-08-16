/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8087112
 * @modules java.net.http
 *          jdk.httpserver
 * @run main/othervm ImmutableHeaders
 * @summary ImmutableHeaders
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class ImmutableHeaders {

    final static String RESPONSE = "Hello world";

    public static void main(String[] args) throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        HttpServer server = HttpServer.create(addr, 10);
        ExecutorService serverExecutor = Executors.newCachedThreadPool();
        ExecutorService clientExecutor = Executors.newCachedThreadPool();
        server.createContext("/test", new ImmutableHeadersHandler());
        int port = server.getAddress().getPort();
        System.out.println("Server port = " + port);

        server.setExecutor(serverExecutor);
        server.start();
        HttpClient client = HttpClient.newBuilder()
                                      .executor(clientExecutor)
                                      .build();

        try {
            URI uri = new URI("http://localhost:" + port + "/test/foo");
            HttpRequest req = HttpRequest.newBuilder(uri)
                                         .headers("X-Foo", "bar")
                                         .headers("X-Bar", "foo")
                                         .GET()
                                         .build();

            try {
                HttpHeaders hd = req.headers();
                List<String> v = hd.allValues("X-Foo");
                if (!v.get(0).equals("bar"))
                    throw new RuntimeException("Test failed");
                v.add("XX");
                throw new RuntimeException("Test failed");
            } catch (UnsupportedOperationException ex) {
            }
            HttpResponse resp = client.send(req, BodyHandlers.discarding());
            try {
                HttpHeaders hd = resp.headers();
                List<String> v = hd.allValues("X-Foo-Response");
                if (!v.get(0).equals("resp"))
                    throw new RuntimeException("Test failed");
                v.add("XX");
                throw new RuntimeException("Test failed");
            } catch (UnsupportedOperationException ex) {
            }

        } finally {
            clientExecutor.shutdownNow();
            serverExecutor.shutdownNow();
            server.stop(0);
        }
        System.out.println("OK");
    }

   static class ImmutableHeadersHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {
            String method = he.getRequestMethod();
            InputStream is = he.getRequestBody();
            Headers h = he.getResponseHeaders();
            h.add("X-Foo-Response", "resp");
            he.sendResponseHeaders(200, RESPONSE.length());
            OutputStream os = he.getResponseBody();
            os.write(RESPONSE.getBytes(US_ASCII));
            os.close();
        }

   }
}
