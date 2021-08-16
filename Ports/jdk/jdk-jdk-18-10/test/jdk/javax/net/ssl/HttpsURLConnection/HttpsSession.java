/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8212261
 * @summary Add SSLSession accessors to HttpsURLConnection and
 *          SecureCacheResponse
 * @library /test/lib
 * @modules jdk.httpserver
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run main/othervm HttpsSession
 */
import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.util.concurrent.*;
import java.util.Objects;
import jdk.test.lib.net.SimpleSSLContext;

public class HttpsSession {

    static SSLContext sslContext;

    public static void main(String[] args) throws Exception {
        HttpsServer httpsServer = null;
        ExecutorService executor = null;
        try {
            httpsServer = HttpsServer.create(new InetSocketAddress(0), 0);
            HttpContext c2 =
                    httpsServer.createContext("/test", new HttpsHandler());

            executor = Executors.newCachedThreadPool();
            httpsServer.setExecutor(executor);

            sslContext = new SimpleSSLContext().get();
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            httpsServer.start();

            int httpsPort = httpsServer.getAddress().getPort();
            System.out.println(
                    "Server address: " + httpsServer.getAddress());

            runTest(httpsPort);
        } finally {
            if (httpsServer != null) {
                httpsServer.stop(2);
            }
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    private static class HttpsHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            InputStream is = httpExchange.getRequestBody();

            while (is.read() != -1) {
                // read to EOF
            }
            is.close();

            httpExchange.sendResponseHeaders(200, 0);
            httpExchange.close();
        }
    }

    static void runTest(int port) throws Exception {
        URL url = new URL(
                String.format("https://localhost:%s/test/", port));
        HttpsURLConnection urlc =
                (HttpsURLConnection)url.openConnection();

        urlc.setSSLSocketFactory(sslContext.getSocketFactory());
        urlc.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String s, SSLSession s1) {
                return true;
            }
        });

        try {
            urlc.getSSLSession();
            throw new Exception(
                "HttpsURLConnection.getSSLSession() should throw " +
                "IllegalStateException before the connection established");
        } catch (IllegalStateException ise) {
            // That's the expected behavior, continue.
        }

        try (InputStream is = urlc.getInputStream()) {
            while (is.read() != -1) {
                // read to EOF
            }

            SSLSession session = urlc.getSSLSession().orElseThrow();
            if (!Objects.equals(urlc.getCipherSuite(),
                    session.getCipherSuite())) {
                throw new Exception(
                    "Incorrect SSLSession for HTTPsURLConnection: " +
                    urlc.getCipherSuite() + "/" + session.getCipherSuite());
            }
        }
    }
}
