/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;

import jdk.test.lib.net.IPSupport;

/**
 * @test
 * @bug 8263442
 * @summary Potential bug in jdk.internal.net.http.common.Utils.CONTEXT_RESTRICTED
 * @library /test/lib
 * @run main/othervm AuthFilter
 */

public class AuthFilter {
    static class Auth extends Authenticator {
    }

    static HttpServer createServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 5);
        HttpHandler handler = (HttpExchange e) -> {
            InputStream is = e.getRequestBody();
            is.readAllBytes();
            is.close();
            Headers reqh = e.getRequestHeaders();
            if (reqh.containsKey("authorization")) {
                e.sendResponseHeaders(500, -1);
            } else {
                e.sendResponseHeaders(200, -1);
            }
        };
        server.createContext("/", handler);
        return server;
    }

    public static void main(String[] args) throws Exception {
        test(false);
        test(true);
    }

    /**
     *  Fake proxy. Just looks for Proxy-Authorization header
     *  and returns error if seen. Returns 200 OK if not.
     *  Does not actually forward the request
     */
    static class ProxyServer extends Thread {

        final ServerSocketChannel server;
        final int port;
        volatile SocketChannel c;

        ProxyServer() throws IOException {
            server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(0));
            if (server.getLocalAddress() instanceof InetSocketAddress isa) {
                port = isa.getPort();
            } else {
                port = -1;
            }
        }

        int getPort() {
            return port;
        }

        static String ok = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n";
        static String notok1 = "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n";
        static String notok2 = "HTTP/1.1 501 Not Implemented\r\nContent-Length: 0\r\n\r\n";

        static void reply(String msg, Writer writer) throws IOException {
            writer.write(msg);
            writer.flush();
        }

        public void run() {
            try {
                c = server.accept();
                var cs = StandardCharsets.US_ASCII;
                LineNumberReader reader = new LineNumberReader(Channels.newReader(c, cs));
                Writer writer = Channels.newWriter(c, cs);

                String line;
                while ((line=reader.readLine()) != null) {
                    if (line.indexOf("Proxy-Authorization") != -1) {
                        reply(notok1, writer);
                        return;
                    }
                    if (line.equals("")) {
                        // end of headers
                        reply(ok, writer);
                        return;
                    }
                }
                reply(notok2, writer);
            } catch (IOException e) {
            }
            try {
                server.close();
                c.close();
            } catch (IOException ee) {}
        }
    }

    private static InetSocketAddress getLoopback(int port) throws IOException {
        if (IPSupport.hasIPv4()) {
            return new InetSocketAddress("127.0.0.1", port);
        } else {
            return new InetSocketAddress("::1", port);
        }
    }

    public static void test(boolean useProxy) throws Exception {
        HttpServer server = createServer();
        int port = server.getAddress().getPort();
        ProxyServer proxy;

        InetSocketAddress proxyAddr;
        String authHdr;
        if (useProxy) {
            proxy = new ProxyServer();
            proxyAddr = getLoopback(proxy.getPort());
            proxy.start();
            authHdr = "Proxy-Authorization";
        } else {
            authHdr = "Authorization";
            proxyAddr = null;
        }

        server.start();

        // proxyAddr == null => proxying disabled
        HttpClient client = HttpClient
                .newBuilder()
                .authenticator(new Auth())
                .proxy(ProxySelector.of(proxyAddr))
                .build();


        URI uri = new URI("http://127.0.0.1:" + Integer.toString(port));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header(authHdr, "nonsense")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int r = response.statusCode();
        System.out.println(r);
        server.stop(0);
        if (r != 200)
            throw new RuntimeException("Test failed : " + r);
    }
}
