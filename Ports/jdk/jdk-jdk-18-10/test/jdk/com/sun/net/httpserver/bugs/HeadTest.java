/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6886723
 * @library /test/lib
 * @run main HeadTest
 * @run main/othervm -Djava.net.preferIPv6Addresses=true HeadTest
 * @summary light weight http server doesn't return correct status code for HEAD requests
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;

public class HeadTest {

    public static void main(String[] args) throws Exception {
        server();
    }

    static void server() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress inetAddress = new InetSocketAddress(loopback, 0);
        HttpServer server = HttpServer.create(inetAddress, 5);
        try {
            server.setExecutor(Executors.newFixedThreadPool(5));
            HttpContext chunkedContext = server.createContext("/chunked");
            chunkedContext.setHandler(new HttpHandler() {
                @Override
                public void handle(HttpExchange msg) {
                    try {
                        try {
                            if (msg.getRequestMethod().equals("HEAD")) {
                                msg.getRequestBody().close();
                                msg.getResponseHeaders().add("Transfer-encoding", "chunked");
                                msg.sendResponseHeaders(200, -1);
                            }
                        } catch(IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } finally {
                        msg.close();
                    }
                }
            });
            HttpContext clContext = server.createContext("/content");
            clContext.setHandler(new HttpHandler() {
                @Override
                public void handle(HttpExchange msg) {
                    try {
                        try {
                            if (msg.getRequestMethod().equals("HEAD")) {
                                msg.getRequestBody().close();
                                msg.getResponseHeaders().add("Content-length", "1024");
                                msg.sendResponseHeaders(200, -1);
                            }
                        } catch(IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } finally {
                        msg.close();
                    }
                }
            });
            server.start();
            String urlStr = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(server.getAddress().getPort())
                .path("/")
                .build()
                .toString();
            System.out.println("Server is at " + urlStr);

            // Run the chunked client
            for(int i=0; i < 10; i++) {
                runClient(urlStr + "chunked/");
            }
            // Run the content length client
            for(int i=0; i < 10; i++) {
                runClient(urlStr + "content/");
            }
        } finally {
            // Stop the server
            ((ExecutorService)server.getExecutor()).shutdown();
            server.stop(0);
        }
    }

    static void runClient(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection(Proxy.NO_PROXY);
        conn.setRequestMethod("HEAD");
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("HEAD request doesn't return 200, but returns " + status);
        }
    }
}
