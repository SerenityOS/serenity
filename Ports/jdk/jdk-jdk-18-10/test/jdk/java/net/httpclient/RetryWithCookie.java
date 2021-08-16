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

/*
 * @test
 * @bug 8199943
 * @summary Test for cookie handling when retrying after close
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext ReferenceTracker
 * @run testng/othervm
 *       -Djdk.httpclient.HttpClient.log=trace,headers,requests
 *       RetryWithCookie
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RetryWithCookie implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;        // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;       // HTTPS/1.1
    HttpTestServer http2TestServer;       // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;      // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    static final String MESSAGE = "BasicRedirectTest message body";
    static final int ITERATIONS = 3;

    @DataProvider(name = "positive")
    public Object[][] positive() {
        return new Object[][] {
                { httpURI,    },
                { httpsURI,   },
                { http2URI,   },
                { https2URI,  },
        };
    }

    static final AtomicLong requestCounter = new AtomicLong();
    final ReferenceTracker TRACKER = ReferenceTracker.INSTANCE;

    @Test(dataProvider = "positive")
    void test(String uriString) throws Exception {
        out.printf("%n---- starting (%s) ----%n", uriString);
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
                .proxy(NO_PROXY)
                .followRedirects(Redirect.ALWAYS)
                .cookieHandler(cookieManager)
                .sslContext(sslContext)
                .build();
        TRACKER.track(client);
        assert client.cookieHandler().isPresent();

        URI uri = URI.create(uriString);
        List<String> cookies = new ArrayList<>();
        cookies.add("CUSTOMER=ARTHUR_DENT");
        Map<String, List<String>> cookieHeaders = new HashMap<>();
        cookieHeaders.put("Set-Cookie", cookies);
        cookieManager.put(uri, cookieHeaders);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("X-uuid", "uuid-" + requestCounter.incrementAndGet())
                .build();
        out.println("Initial request: " + request.uri());

        for (int i=0; i< ITERATIONS; i++) {
            out.println("iteration: " + i);
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            out.println("  Got response: " + response);
            out.println("  Got body Path: " + response.body());

            assertEquals(response.statusCode(), 200);
            assertEquals(response.body(), MESSAGE);
            assertEquals(response.headers().allValues("X-Request-Cookie"), cookies);
            request = HttpRequest.newBuilder(uri)
                    .header("X-uuid", "uuid-" + requestCounter.incrementAndGet())
                    .build();
        }
    }

    // -- Infrastructure

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(new CookieRetryHandler(), "/http1/cookie/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/cookie/retry";
        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new CookieRetryHandler(),"/https1/cookie/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/cookie/retry";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new CookieRetryHandler(), "/http2/cookie/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/cookie/retry";
        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new CookieRetryHandler(), "/https2/cookie/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/cookie/retry";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        Thread.sleep(100);
        AssertionError fail = TRACKER.check(500);
        try {
            httpTestServer.stop();
            httpsTestServer.stop();
            http2TestServer.stop();
            https2TestServer.stop();
        } finally {
            if (fail != null) throw fail;
        }
    }

    static class CookieRetryHandler implements HttpTestHandler {
        ConcurrentHashMap<String,String> closedRequests = new ConcurrentHashMap<>();

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            System.out.println("CookieRetryHandler for: " + t.getRequestURI());

            List<String> uuids = t.getRequestHeaders().get("X-uuid");
            if (uuids == null || uuids.size() != 1) {
                readAllRequestData(t);
                try (OutputStream os = t.getResponseBody()) {
                    String msg = "Incorrect uuid header values:[" + uuids + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                }
                return;
            }

            String uuid = uuids.get(0);
            // retrying
            if (closedRequests.putIfAbsent(uuid, t.getRequestURI().toString()) == null) {
                if (t.getExchangeVersion() == HttpClient.Version.HTTP_1_1) {
                    // Throwing an exception here only causes a retry
                    // with HTTP_1_1 - where it forces the server to close
                    // the connection.
                    // For HTTP/2 then throwing an IOE would cause the server
                    // to close the stream, and throwing anything else would
                    // cause it to close the connection, but neither would
                    // cause the client to retry.
                    // So we simply do not try to retry with HTTP/2 and just verify
                    // we have received the expected cookie
                    throw new IOException("Closing on first request");
                }
            }

            // not retrying
            readAllRequestData(t);
            try (OutputStream os = t.getResponseBody()) {
                List<String> cookie = t.getRequestHeaders().get("Cookie");

                if (cookie == null || cookie.size() == 0) {
                    String msg = "No cookie header present";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else if (!cookie.get(0).equals("CUSTOMER=ARTHUR_DENT")) {
                    String msg = "Incorrect cookie header value:[" + cookie.get(0) + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else if (cookie.size() > 1) {
                    String msg = "Incorrect cookie header values:[" + cookie + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else {
                    assert cookie.get(0).equals("CUSTOMER=ARTHUR_DENT");
                    byte[] bytes = MESSAGE.getBytes(UTF_8);
                    for (String value : cookie) {
                        t.getResponseHeaders().addHeader("X-Request-Cookie", value);
                    }
                    t.sendResponseHeaders(200, bytes.length);
                    os.write(bytes);
                }
            }

            closedRequests.remove(uuid);
        }
    }

    static void readAllRequestData(HttpTestExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            is.readAllBytes();
        }
    }
}
