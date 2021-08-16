/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233185
 * @summary HttpServer.stop() blocks indefinitely when called on dispatch thread
 * @modules java.base/sun.net.www
 * @library /test/lib
 * @run main/othervm HttpServerTest
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;

public class HttpServerTest implements HttpHandler {
    private static final int HTTP_STATUS_CODE_OK = 200;
    private static CountDownLatch serverStopped = new CountDownLatch(1);
    private final HttpServer server;


    public HttpServerTest(HttpServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        sendHttpStatusCode(HTTP_STATUS_CODE_OK, ex);

        System.out.println("Stopping server ...");
        server.stop(1);
        serverStopped.countDown();
    }

    private void sendHttpStatusCode(int httpCode, HttpExchange ex) {
        try {
            ex.sendResponseHeaders(httpCode, 0);
            ex.close();
        } catch (IOException e) {
            throw new RuntimeException("Server couldn't send response: " + e, e);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        try {
            server.createContext("/context", new HttpServerTest(server));
            server.start();

            URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(server.getAddress().getPort())
                    .path("/context")
                    .toURLUnchecked();

            HttpURLConnection urlc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            System.out.println("Client: Response code received: " + urlc.getResponseCode());
            InputStream is = urlc.getInputStream();
            is.readAllBytes();
            is.close();
        } finally {
            serverStopped.await();
            System.out.println("Server stopped as expected");
        }
    }
}
