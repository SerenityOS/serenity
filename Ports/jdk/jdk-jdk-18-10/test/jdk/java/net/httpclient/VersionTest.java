/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8175814
 * @modules java.net.http java.logging jdk.httpserver
 * @library /lib/testlibrary/ /
 * @build ProxyServer
 * @run main/othervm -Djdk.httpclient.HttpClient.log=errors,requests,headers,trace VersionTest
 */

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;

public class VersionTest {
    static HttpServer s1 ;
    static ProxyServer proxy;
    static ExecutorService executor;
    static int port;
    static InetSocketAddress proxyAddr;
    static HttpClient client, clientWithProxy;
    static URI uri;
    static volatile boolean error = false;

    public static void main(String[] args) throws Exception {
        initServer();

        client = HttpClient.newBuilder()
                           .executor(executor)
                           .build();

        clientWithProxy = HttpClient.newBuilder()
                           .executor(executor)
                           .proxy(ProxySelector.of(proxyAddr))
                           .build();

        // first check that the version is HTTP/2
        if (client.version() != HttpClient.Version.HTTP_2) {
            throw new RuntimeException("Default version not HTTP_2");
        }
        try {
            test(HTTP_1_1, false);
            test(HTTP_2, false);
            test(HTTP_2, true);
        } finally {
            s1.stop(0);
            executor.shutdownNow();
        }
        if (error)
            throw new RuntimeException();
    }

    public static void test(HttpClient.Version version, boolean proxy) throws Exception {
        HttpRequest r = HttpRequest.newBuilder(uri)
                .version(version)
                .GET()
                .build();
        HttpClient c = proxy ? clientWithProxy : client;
        HttpResponse<Void> resp = c.send(r, BodyHandlers.discarding());
        System.out.printf("Client: response is %d\n", resp.statusCode());
        if (resp.version() != HTTP_1_1) {
            throw new RuntimeException();
        }
        //System.out.printf("Client: response body is %s\n", resp.body());
    }

    static void initServer() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        s1 = HttpServer.create (addr, 0);
        HttpHandler h = new Handler();

        HttpContext c1 = s1.createContext("/", h);

        executor = Executors.newCachedThreadPool();
        s1.setExecutor(executor);
        s1.start();

        port = s1.getAddress().getPort();
        uri = new URI("http://localhost:" + Integer.toString(port) + "/foo");
        System.out.println("HTTP server port = " + port);
        proxy = new ProxyServer(0, false);
        int proxyPort = proxy.getPort();
        proxyAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(), proxyPort);
    }

    static class Handler implements HttpHandler {
        int counter;

        void checkHeader(Headers h) {
            counter++;
            if (counter == 1 && h.containsKey("Upgrade")) {
                VersionTest.error = true;
            }
            if (counter == 2 && !h.containsKey("Upgrade")) {
                VersionTest.error = true;
            }
            if (counter == 3 && h.containsKey("Upgrade")) {
                VersionTest.error = true;
            }
        }

        @Override
        public synchronized void handle(HttpExchange t) throws IOException {
            String reply = "Hello world";
            int len = reply.length();
            Headers h = t.getRequestHeaders();
            checkHeader(h);
            System.out.printf("Sending response 200\n");
            t.sendResponseHeaders(200, len);
            OutputStream o = t.getResponseBody();
            o.write(reply.getBytes());
            t.close();
        }
    }
}
