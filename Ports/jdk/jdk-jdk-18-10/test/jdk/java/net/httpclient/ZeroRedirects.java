/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164941
 * @modules java.net.http java.logging jdk.httpserver
 * @run main/othervm ZeroRedirects
 */

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ZeroRedirects {
    static HttpServer s1 ;
    static ExecutorService executor;
    static int port;
    static HttpClient client;
    static URI uri;

    public static void main(String[] args) throws Exception {
        initServer();

        client = HttpClient.newBuilder()
                           .executor(executor)
                           .followRedirects(HttpClient.Redirect.ALWAYS)
                           .build();
        try {
            test();
        } finally {
            s1.stop(0);
            executor.shutdownNow();
        }
    }

    public static void test() throws Exception {
        System.setProperty("java.net.http.redirects.retrylimit", "0");
        HttpRequest r = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        HttpResponse<Void> resp = client.send(r, BodyHandlers.discarding());
        System.out.printf("Client: response is %d\n", resp.statusCode());
        if (resp.statusCode() != 200)
            throw new RuntimeException();
    }

    static void initServer() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        s1 = HttpServer.create(addr, 0);
        HttpHandler h = new Handler();

        HttpContext c1 = s1.createContext("/", h);

        executor = Executors.newCachedThreadPool();
        s1.setExecutor(executor);
        s1.start();

        port = s1.getAddress().getPort();
        uri = new URI("http://localhost:" + port + "/foo");
        System.out.println("HTTP server port = " + port);
    }

    static class Handler implements HttpHandler {

        @Override
        public synchronized void handle(HttpExchange t) throws IOException {
            String reply = "Hello world";
            int len = reply.length();
            System.out.printf("Sending response 200\n");
            t.sendResponseHeaders(200, len);
            OutputStream o = t.getResponseBody();
            o.write(reply.getBytes());
            t.close();
        }
    }
}
