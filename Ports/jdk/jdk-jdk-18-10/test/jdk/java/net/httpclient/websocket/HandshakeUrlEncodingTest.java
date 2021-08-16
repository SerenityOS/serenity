/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8245245
 * @summary Test for Websocket URI encoding during HandShake
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.net.http
 *          jdk.httpserver
 * @run testng/othervm -Djdk.internal.httpclient.debug=true HandshakeUrlEncodingTest
 */

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpExchange;
import jdk.test.lib.net.SimpleSSLContext;
import jdk.test.lib.net.URIBuilder;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.http.HttpClient.Builder.NO_PROXY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static java.lang.System.out;

public class HandshakeUrlEncodingTest {

    SSLContext sslContext;
    HttpServer httpTestServer;
    HttpsServer httpsTestServer;
    String httpURI;
    String httpsURI;

    static String queryPart;

    static final int ITERATION_COUNT = 10;
    // a shared executor helps reduce the amount of threads created by the test
    static final ExecutorService executor = Executors.newCachedThreadPool();

    @DataProvider(name = "variants")
    public Object[][] variants() {
        return new Object[][]{
            { httpURI,   false },
            { httpsURI,  false },
            { httpURI,   true  },
            { httpsURI,  true  }
        };
    }

    HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                         .proxy(NO_PROXY)
                         .executor(executor)
                         .sslContext(sslContext)
                         .build();
    }

    @Test(dataProvider = "variants")
    public void test(String uri, boolean sameClient) {
        HttpClient client = null;
        out.println("The url is " + uri);
        for (int i = 0; i < ITERATION_COUNT; i++) {
            System.out.printf("iteration %s%n", i);
            if (!sameClient || client == null)
                client = newHttpClient();

            try {
                client.newWebSocketBuilder()
                    .buildAsync(URI.create(uri), new WebSocket.Listener() { })
                    .join();
                fail("Expected to throw");
            } catch (CompletionException ce) {
                Throwable t = getCompletionCause(ce);
                if (!(t instanceof WebSocketHandshakeException)) {
                    throw new AssertionError("Unexpected exception", t);
                }
                WebSocketHandshakeException wse = (WebSocketHandshakeException) t;
                assertNotNull(wse.getResponse());
                assertNotNull(wse.getResponse().body());
                assertEquals(wse.getResponse().body().getClass(), String.class);
                String body = (String)wse.getResponse().body();
                String expectedBody = "/?&raw=abc+def/ghi=xyz&encoded=abc%2Bdef%2Fghi%3Dxyz";
                assertEquals(body, expectedBody);
                out.println("Status code is " + wse.getResponse().statusCode());
                out.println("Response is " + body);
                assertNotNull(wse.getResponse().statusCode());
                out.println("Status code is " + wse.getResponse().statusCode());
                assertEquals(wse.getResponse().statusCode(), 400);
            }
        }
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");


        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        queryPart = "?&raw=abc+def/ghi=xyz&encoded=abc%2Bdef%2Fghi%3Dxyz";
        httpTestServer = HttpServer.create(sa, 10);
        httpURI = URIBuilder.newBuilder()
                            .scheme("ws")
                            .host("localhost")
                            .port(httpTestServer.getAddress().getPort())
                            .path("/")
                            .build()
                            .toString() + queryPart;

        httpTestServer.createContext("/", new UrlHandler());

        httpsTestServer = HttpsServer.create(sa, 10);
        httpsTestServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsURI = URIBuilder.newBuilder()
                             .scheme("wss")
                             .host("localhost")
                             .port(httpsTestServer.getAddress().getPort())
                             .path("/")
                             .build()
                             .toString() + queryPart;

        httpsTestServer.createContext("/", new UrlHandler());

        httpTestServer.start();
        httpsTestServer.start();
    }

    @AfterTest
    public void teardown() {
        httpTestServer.stop(0);
        httpsTestServer.stop(0);
        executor.shutdownNow();
    }

    private static Throwable getCompletionCause(Throwable x) {
        if (!(x instanceof CompletionException)
            && !(x instanceof ExecutionException)) return x;
        final Throwable cause = x.getCause();
        if (cause == null) {
            throw new InternalError("Unexpected null cause", x);
        }
        return cause;
    }

    static class UrlHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange e) throws IOException {
            try(InputStream is = e.getRequestBody();
                OutputStream os = e.getResponseBody()) {
                String testUri = "/?&raw=abc+def/ghi=xyz&encoded=abc%2Bdef%2Fghi%3Dxyz";
                URI uri = e.getRequestURI();
                byte[] bytes = is.readAllBytes();
                if (uri.toString().equals(testUri)) {
                    bytes = testUri.getBytes();
                }
                e.sendResponseHeaders(400, bytes.length);
                os.write(bytes);

            }
        }
    }
}

