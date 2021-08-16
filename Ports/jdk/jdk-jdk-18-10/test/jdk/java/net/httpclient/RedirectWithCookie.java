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
 * @summary Test for cookie handling when redirecting
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm
 *       -Djdk.httpclient.HttpClient.log=trace,headers,requests
 *       RedirectWithCookie
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
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
import java.util.List;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RedirectWithCookie implements HttpServerAdapters {

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

    @Test(dataProvider = "positive")
    void test(String uriString) throws Exception {
        out.printf("%n---- starting (%s) ----%n", uriString);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(Redirect.ALWAYS)
                .cookieHandler(new CookieManager())
                .sslContext(sslContext)
                .build();
        assert client.cookieHandler().isPresent();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        out.println("Initial request: " + request.uri());

        for (int i=0; i< ITERATIONS; i++) {
            out.println("iteration: " + i);
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            out.println("  Got response: " + response);
            out.println("  Got body Path: " + response.body());

            assertEquals(response.statusCode(), 200);
            assertEquals(response.body(), MESSAGE);
            // asserts redirected URI in response.request().uri()
            assertTrue(response.uri().getPath().endsWith("message"));
            assertPreviousRedirectResponses(request, response);
        }
    }

    static void assertPreviousRedirectResponses(HttpRequest initialRequest,
                                                HttpResponse<?> finalResponse) {
        // there must be at least one previous response
        finalResponse.previousResponse()
                .orElseThrow(() -> new RuntimeException("no previous response"));

        HttpResponse<?> response = finalResponse;
        do {
            URI uri = response.uri();
            response = response.previousResponse().get();
            assertTrue(300 <= response.statusCode() && response.statusCode() <= 309,
                    "Expected 300 <= code <= 309, got:" + response.statusCode());
            assertEquals(response.body(), null, "Unexpected body: " + response.body());
            String locationHeader = response.headers().firstValue("Location")
                    .orElseThrow(() -> new RuntimeException("no previous Location"));
            assertTrue(uri.toString().endsWith(locationHeader),
                    "URI: " + uri + ", Location: " + locationHeader);

        } while (response.previousResponse().isPresent());

        // initial
        assertEquals(initialRequest, response.request(),
                String.format("Expected initial request [%s] to equal last prev req [%s]",
                        initialRequest, response.request()));
    }

    // -- Infrastructure

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(new CookieRedirectHandler(), "/http1/cookie/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/cookie/redirect";
        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new CookieRedirectHandler(),"/https1/cookie/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/cookie/redirect";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new CookieRedirectHandler(), "/http2/cookie/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/cookie/redirect";
        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new CookieRedirectHandler(), "/https2/cookie/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/cookie/redirect";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static class CookieRedirectHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            System.out.println("CookieRedirectHandler for: " + t.getRequestURI());
            readAllRequestData(t);

            // redirecting
            if (t.getRequestURI().getPath().endsWith("redirect")) {
                String url = t.getRequestURI().resolve("message").toString();
                t.getResponseHeaders().addHeader("Location", url);
                t.getResponseHeaders().addHeader("Set-Cookie",
                                                 "CUSTOMER=WILE_E_COYOTE");
                t.sendResponseHeaders(302, 0);
                return;
            }

            // not redirecting
            try (OutputStream os = t.getResponseBody()) {
                List<String> cookie = t.getRequestHeaders().get("Cookie");

                if (cookie == null || cookie.size() == 0) {
                    String msg = "No cookie header present";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else if (!cookie.get(0).equals("CUSTOMER=WILE_E_COYOTE")) {
                    String msg = "Incorrect cookie header value:[" + cookie.get(0) + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else {
                    assert cookie.get(0).equals("CUSTOMER=WILE_E_COYOTE");
                    byte[] bytes = MESSAGE.getBytes(UTF_8);
                    t.sendResponseHeaders(200, bytes.length);
                    os.write(bytes);
                }
            }
        }
    }

    static void readAllRequestData(HttpTestExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            is.readAllBytes();
        }
    }
}
