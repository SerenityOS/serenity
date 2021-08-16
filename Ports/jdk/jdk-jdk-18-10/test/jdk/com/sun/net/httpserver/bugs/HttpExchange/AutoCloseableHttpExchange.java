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

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.test.lib.net.URIBuilder;
import sun.net.httpserver.HttpExchangeAccess;


/**
 * @test
 * @bug 8203036
 * @library /test/lib
 * @modules jdk.httpserver/sun.net.httpserver
 * @build jdk.httpserver/sun.net.httpserver.HttpExchangeAccess AutoCloseableHttpExchange
 * @run main/othervm AutoCloseableHttpExchange
 * @summary Ensure that HttpExchange closes correctly when utilising the
 * AutoCloseable interface e.g. both request InputStream and response OutputStream
 * are closed, if not already.
 */

public class AutoCloseableHttpExchange {

    static HttpServer testHttpServer;
    static AtomicBoolean exchangeCloseFail = new AtomicBoolean(false);

    static class Handler implements HttpHandler {
        private CountDownLatch latch;

        Handler(CountDownLatch latch) {
            this.latch = latch;
        }

        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            try (HttpExchange e = t) {
                while (is.read() != -1) ;
                t.sendResponseHeaders(200, -1);
            }
            if (!HttpExchangeAccess.isClosed(t)) {
                exchangeCloseFail.set(true);
            }
            latch.countDown();
        }
    }

    static void connectAndCheck(String realm) throws Exception {
        URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(testHttpServer.getAddress().getPort())
                .path(realm)
                .toURL();
        HttpURLConnection testConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        InputStream is = testConnection.getInputStream();
        while (is.read() != -1) ;
        is.close();
    }

    public static void main(String[] args) throws Exception {
        int CONNECTION_COUNT = 5;
        CountDownLatch latch = new CountDownLatch(CONNECTION_COUNT);

        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        testHttpServer = HttpServer.create(addr, 0);
        testHttpServer.createContext("/test", new Handler(latch));

        ExecutorService executor = Executors.newFixedThreadPool(CONNECTION_COUNT);
        testHttpServer.setExecutor(executor);
        testHttpServer.start();

        while (CONNECTION_COUNT-- != 0) {
            connectAndCheck("/test");
        }
        latch.await();
        testHttpServer.stop(2);
        executor.shutdown();

        if (exchangeCloseFail.get())
            throw new RuntimeException("The exchange was not closed properly");
    }
}
