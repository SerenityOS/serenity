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
 * @run main/othervm BasicAuthTest
 * @summary Basic Authentication Test
 */

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class BasicAuthTest {

    static volatile boolean ok;
    static final String RESPONSE = "Hello world";
    static final String POST_BODY = "This is the POST body 123909090909090";

    public static void main(String[] args) throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(),0);
        HttpServer server = HttpServer.create(addr, 10);
        ExecutorService e = Executors.newCachedThreadPool();
        Handler h = new Handler();
        HttpContext serverContext = server.createContext("/test", h);
        int port = server.getAddress().getPort();
        System.out.println("Server port = " + port);

        ClientAuth ca = new ClientAuth();
        ServerAuth sa = new ServerAuth("foo realm");
        serverContext.setAuthenticator(sa);
        server.setExecutor(e);
        server.start();
        HttpClient client = HttpClient.newBuilder()
                                      .authenticator(ca)
                                      .build();

        try {
            URI uri = new URI("http://localhost:" + Integer.toString(port) + "/test/foo");
            HttpRequest req = HttpRequest.newBuilder(uri).GET().build();

            HttpResponse resp = client.send(req, BodyHandlers.ofString());
            ok = resp.statusCode() == 200 && resp.body().equals(RESPONSE);

            if (!ok || ca.count != 1)
                throw new RuntimeException("Test failed");

            // repeat same request, should succeed but no additional authenticator calls

            resp = client.send(req, BodyHandlers.ofString());
            ok = resp.statusCode() == 200 && resp.body().equals(RESPONSE);

            if (!ok || ca.count != 1)
                throw new RuntimeException("Test failed");

            // try a POST

            req = HttpRequest.newBuilder(uri)
                             .POST(BodyPublishers.ofString(POST_BODY))
                             .build();
            resp = client.send(req, BodyHandlers.ofString());
            ok = resp.statusCode() == 200;

            if (!ok || ca.count != 1)
                throw new RuntimeException("Test failed");
        } finally {
            server.stop(0);
            e.shutdownNow();
        }
        System.out.println("OK");
    }

    static class ServerAuth extends BasicAuthenticator {

        ServerAuth(String realm) {
            super(realm);
        }

        @Override
        public boolean checkCredentials(String username, String password) {
            if (!"user".equals(username) || !"passwd".equals(password)) {
                return false;
            }
            return true;
        }

    }

    static class ClientAuth extends java.net.Authenticator {
        volatile int count = 0;

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            count++;
            return new PasswordAuthentication("user", "passwd".toCharArray());
        }
    }

   static class Handler implements HttpHandler {
        static volatile boolean ok;

        @Override
        public void handle(HttpExchange he) throws IOException {
            String method = he.getRequestMethod();
            InputStream is = he.getRequestBody();
            if (method.equalsIgnoreCase("POST")) {
                String requestBody = new String(is.readAllBytes(), US_ASCII);
                if (!requestBody.equals(POST_BODY)) {
                    he.sendResponseHeaders(500, -1);
                    ok = false;
                } else {
                    he.sendResponseHeaders(200, -1);
                    ok = true;
                }
            } else { // GET
                he.sendResponseHeaders(200, RESPONSE.length());
                OutputStream os = he.getResponseBody();
                os.write(RESPONSE.getBytes(US_ASCII));
                os.close();
                ok = true;
            }
        }
   }
}
