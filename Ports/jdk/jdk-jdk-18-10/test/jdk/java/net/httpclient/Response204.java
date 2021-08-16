/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211437 8216974 8218662
 * @run main/othervm -Djdk.httpclient.HttpClient.log=headers,requests Response204
 * @summary
 */

import com.sun.net.httpserver.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;
import java.io.*;
import java.net.*;
import static java.net.http.HttpClient.Builder.NO_PROXY;

/**
 * Verify that a 204 response code with no content-length is handled correctly
 */
public class Response204 {

    // check for 8216974
    static final AtomicReference<Exception> serverError = new AtomicReference<>();

    public static void main (String[] args) throws Exception {
        Logger logger = Logger.getLogger ("com.sun.net.httpserver");
        ConsoleHandler c = new ConsoleHandler();
        c.setLevel (Level.WARNING);
        logger.addHandler (c);
        logger.setLevel (Level.WARNING);
        Handler handler = new Handler();
        InetSocketAddress addr = new InetSocketAddress (InetAddress.getLoopbackAddress(), 0);
        HttpServer server = HttpServer.create (addr, 0);
        HttpContext ctx = server.createContext ("/test", handler);
        server.createContext ("/zero", new ZeroContentLengthHandler());
        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor (executor);
        server.start ();

        URI uri = new URI("http", null,
                server.getAddress().getHostString(),
                server.getAddress().getPort(),
                "/test/foo.html", null, null);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204)
                throw new RuntimeException("wrong response code");
            if (response.body() != null && !response.body().equals(""))
                throw new RuntimeException("should have received empty response");
            System.out.println(response.headers().firstValue("content-length").orElse("nichts"));
            System.out.println ("OK 1");
            // Send a second time. This time we should get exception because the server
            // is going to send an invalid 204 with a Content-length
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                throw new RuntimeException("send should have thrown exception");
            } catch (IOException ioe) {
                System.out.println("OK 2");
            }

            // check for 8216974
            Exception error = serverError.get();
            if (error != null) throw error;

            // Test 3
            testZeroContentLength(uri.resolve("/zero/xxyy"));
            System.out.println ("OK 3");
        } finally {
            server.stop(2);
            executor.shutdown();
        }
    }

    static void testZeroContentLength(URI uri) throws Exception {
        System.out.println("--- testZeroContentLength ---");
        HttpClient client = HttpClient.newBuilder().proxy(NO_PROXY).build();
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        System.out.println("Received response:" + response);
        System.out.println("Received headers:" + response.headers());
        if (response.statusCode() != 204)
            throw new RuntimeException("Expected 204, got:" + response.statusCode());
        if (response.body() != null && !response.body().equals(""))
            throw new RuntimeException("Expected empty response, got: " + response.body());
        if (response.headers().firstValueAsLong("Content-Length").orElse(-1L) != 0L)
            throw new RuntimeException("Expected Content-Length:0, in: " + response.headers());
    }

    public static boolean error = false;

    static class Handler implements HttpHandler {
        volatile int counter = 0;
        volatile InetSocketAddress remote;

        public void handle(HttpExchange t)
                throws IOException {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            if (counter % 2 == 0) {
                // store the client's address
                remote = t.getRemoteAddress();
                System.out.println("Server received request from: " + remote);
            }
            while (is.read() != -1) ;
            is.close();
            if ((++counter) % 2 == 0) {
                // pretend there is a body
                rmap.set("Content-length", "10");
                // 8216974: the client should have returned the connection
                // to the pool and should therefore have the same
                // remote address.
                if (!t.getRemoteAddress().equals(remote)) {
                    String msg = "Unexpected remote address: "
                            + t.getRemoteAddress()
                            + " - should have been " + remote;
                    System.out.println(msg);
                    serverError.set(new Exception(msg));
                }
            }
            t.sendResponseHeaders(204, -1);
            t.close();
        }
    }

    // A handler that returns a 204 with a `Content-Length: 0` header/value
    static class ZeroContentLengthHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            t.getResponseHeaders().set("Content-length", "0");
            t.sendResponseHeaders(204, -1);
            t.close();
        }
    }
}
