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

/*
 * @test
 * @bug 8087112
 * @library /test/lib server
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.httpclient.HttpClient.log=ssl,requests,responses,errors NoBodyTest
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.*;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.Test;
import static java.net.http.HttpClient.Version.HTTP_2;

@Test
public class NoBodyTest {
    static int httpPort, httpsPort;
    static Http2TestServer httpServer, httpsServer;
    static HttpClient client = null;
    static ExecutorService clientExec;
    static ExecutorService serverExec;
    static SSLContext sslContext;
    static String TEST_STRING = "The quick brown fox jumps over the lazy dog ";

    static String httpURIString, httpsURIString;

    static void initialize() throws Exception {
        try {
            SimpleSSLContext sslct = new SimpleSSLContext();
            sslContext = sslct.get();
            client = getClient();
            httpServer = new Http2TestServer(false, 0, serverExec, sslContext);
            httpServer.addHandler(new Handler(), "/");
            httpPort = httpServer.getAddress().getPort();

            httpsServer = new Http2TestServer(true, 0, serverExec, sslContext);
            httpsServer.addHandler(new Handler(), "/");

            httpsPort = httpsServer.getAddress().getPort();
            httpURIString = "http://localhost:" + httpPort + "/foo/";
            httpsURIString = "https://localhost:" + httpsPort + "/bar/";

            httpServer.start();
            httpsServer.start();
        } catch (Throwable e) {
            System.err.println("Throwing now");
            e.printStackTrace(System.err);
            throw e;
        }
    }

    @Test
    public static void runtest() throws Exception {
        try {
            initialize();
            warmup(false);
            warmup(true);
            test(false);
            test(true);
        } catch (Throwable tt) {
            System.err.println("tt caught");
            tt.printStackTrace(System.err);
            throw tt;
        } finally {
            httpServer.stop();
            httpsServer.stop();
        }
    }

    static HttpClient getClient() {
        if (client == null) {
            serverExec = Executors.newCachedThreadPool();
            clientExec = Executors.newCachedThreadPool();
            client = HttpClient.newBuilder()
                               .executor(clientExec)
                               .sslContext(sslContext)
                               .version(HTTP_2)
                               .build();
        }
        return client;
    }

    static URI getURI(boolean secure) {
        if (secure)
            return URI.create(httpsURIString);
        else
            return URI.create(httpURIString);
    }

    static void checkStatus(int expected, int found) throws Exception {
        if (expected != found) {
            System.err.printf ("Test failed: wrong status code %d/%d\n",
                expected, found);
            throw new RuntimeException("Test failed");
        }
    }

    static void checkStrings(String expected, String found) throws Exception {
        if (!expected.equals(found)) {
            System.err.printf ("Test failed: wrong string %s/%s\n",
                expected, found);
            throw new RuntimeException("Test failed");
        }
    }

    static final int LOOPS = 13;

    static void warmup(boolean secure) throws Exception {
        URI uri = getURI(secure);
        String type = secure ? "https" : "http";
        System.err.println("Request to " + uri);

        // Do a simple warmup request

        HttpClient client = getClient();
        HttpRequest req = HttpRequest.newBuilder(uri)
                                     .POST(BodyPublishers.ofString("Random text"))
                                     .build();
        HttpResponse<String> response = client.send(req, BodyHandlers.ofString());
        checkStatus(200, response.statusCode());
        String responseBody = response.body();
        HttpHeaders h = response.headers();
        checkStrings(TEST_STRING + type, responseBody);
    }

    static void test(boolean secure) throws Exception {
        URI uri = getURI(secure);
        String type = secure ? "https" : "http";
        System.err.println("Request to " + uri);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(BodyPublishers.ofString(TEST_STRING))
                .build();
        for (int i = 0; i < LOOPS; i++) {
            System.out.println("Loop " + i);
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int expectedResponse = (i % 2) == 0 ? 204 : 200;
            if (response.statusCode() != expectedResponse)
                throw new RuntimeException("wrong response code " + Integer.toString(response.statusCode()));
            if (expectedResponse == 200 && !response.body().equals(TEST_STRING + type)) {
                System.err.printf("response received/expected %s/%s\n", response.body(), TEST_STRING + type);
                throw new RuntimeException("wrong response body");
            }
        }
        System.err.println("test: DONE");
    }

    static class Handler implements Http2Handler {

        public Handler() {}

        volatile int invocation = 0;

        @Override
        public void handle(Http2TestExchange t)
                throws IOException {
            try {
                URI uri = t.getRequestURI();
                System.err.printf("Handler received request to %s from %s\n",
                        uri, t.getRemoteAddress());
                String type = uri.getScheme().toLowerCase();
                InputStream is = t.getRequestBody();
                while (is.read() != -1);
                is.close();

                // every second response is 204.

                if ((invocation++ % 2) == 1) {
                    System.err.println("Server sending 204");
                    t.sendResponseHeaders(204, -1);
                } else {
                    String body = TEST_STRING + type;
                    t.sendResponseHeaders(200, body.length());
                    OutputStream os = t.getResponseBody();
                    os.write(body.getBytes());
                    os.close();
                }
            } catch (Throwable e) {
                e.printStackTrace(System.err);
                throw new IOException(e);
            }
        }
    }
}
