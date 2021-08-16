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
 * @summary Basic test for redirect and redirect policies
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
 *       BasicRedirectTest
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BasicRedirectTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;        // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;       // HTTPS/1.1
    HttpTestServer http2TestServer;       // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;      // HTTP/2 ( h2  )
    String httpURI;
    String httpURIToMoreSecure; // redirects HTTP to HTTPS
    String httpsURI;
    String httpsURIToLessSecure; // redirects HTTPS to HTTP
    String http2URI;
    String http2URIToMoreSecure; // redirects HTTP to HTTPS
    String https2URI;
    String https2URIToLessSecure; // redirects HTTPS to HTTP

    static final String MESSAGE = "Is fearr Gaeilge briste, na Bearla cliste";
    static final int ITERATIONS = 3;

    @DataProvider(name = "positive")
    public Object[][] positive() {
        return new Object[][] {
                { httpURI,               Redirect.ALWAYS        },
                { httpsURI,              Redirect.ALWAYS        },
                { http2URI,              Redirect.ALWAYS        },
                { https2URI,             Redirect.ALWAYS        },
                { httpURIToMoreSecure,   Redirect.ALWAYS        },
                { http2URIToMoreSecure,  Redirect.ALWAYS        },
                { httpsURIToLessSecure,  Redirect.ALWAYS        },
                { https2URIToLessSecure, Redirect.ALWAYS        },

                { httpURI,               Redirect.NORMAL        },
                { httpsURI,              Redirect.NORMAL        },
                { http2URI,              Redirect.NORMAL        },
                { https2URI,             Redirect.NORMAL        },
                { httpURIToMoreSecure,   Redirect.NORMAL        },
                { http2URIToMoreSecure,  Redirect.NORMAL        },
        };
    }

    @Test(dataProvider = "positive")
    void test(String uriString, Redirect redirectPolicy) throws Exception {
        out.printf("%n---- starting positive (%s, %s) ----%n", uriString, redirectPolicy);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(redirectPolicy)
                .sslContext(sslContext)
                .build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        out.println("Initial request: " + request.uri());

        for (int i=0; i< ITERATIONS; i++) {
            out.println("iteration: " + i);
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            out.println("  Got response: " + response);
            out.println("  Got body Path: " + response.body());
            out.println("  Got response.request: " + response.request());

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

    // --  negatives

    @DataProvider(name = "negative")
    public Object[][] negative() {
        return new Object[][] {
                { httpURI,               Redirect.NEVER         },
                { httpsURI,              Redirect.NEVER         },
                { http2URI,              Redirect.NEVER         },
                { https2URI,             Redirect.NEVER         },
                { httpURIToMoreSecure,   Redirect.NEVER         },
                { http2URIToMoreSecure,  Redirect.NEVER         },
                { httpsURIToLessSecure,  Redirect.NEVER         },
                { https2URIToLessSecure, Redirect.NEVER         },

                { httpsURIToLessSecure,  Redirect.NORMAL        },
                { https2URIToLessSecure, Redirect.NORMAL        },
        };
    }

    @Test(dataProvider = "negative")
    void testNegatives(String uriString,Redirect redirectPolicy) throws Exception {
        out.printf("%n---- starting negative (%s, %s) ----%n", uriString, redirectPolicy);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(redirectPolicy)
                .sslContext(sslContext)
                .build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        out.println("Initial request: " + request.uri());

        for (int i=0; i< ITERATIONS; i++) {
            out.println("iteration: " + i);
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            out.println("  Got response: " + response);
            out.println("  Got body Path: " + response.body());
            out.println("  Got response.request: " + response.request());

            assertEquals(response.statusCode(), 302);
            assertEquals(response.body(), "XY");
            // asserts original URI in response.request().uri()
            assertTrue(response.uri().equals(uri));
            assertFalse(response.previousResponse().isPresent());
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
        httpTestServer.addHandler(new BasicHttpRedirectHandler(), "/http1/same/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/same/redirect";
        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new BasicHttpRedirectHandler(),"/https1/same/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/same/redirect";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new BasicHttpRedirectHandler(), "/http2/same/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/same/redirect";
        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new BasicHttpRedirectHandler(), "/https2/same/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/same/redirect";


        // HTTP to HTTPS redirect handler
        httpTestServer.addHandler(new ToSecureHttpRedirectHandler(httpsURI), "/http1/toSecure/");
        httpURIToMoreSecure = "http://" + httpTestServer.serverAuthority()+ "/http1/toSecure/redirect";
        // HTTP2 to HTTP2S redirect handler
        http2TestServer.addHandler(new ToSecureHttpRedirectHandler(https2URI), "/http2/toSecure/");
        http2URIToMoreSecure = "http://" + http2TestServer.serverAuthority() + "/http2/toSecure/redirect";

        // HTTPS to HTTP redirect handler
        httpsTestServer.addHandler(new ToLessSecureRedirectHandler(httpURI), "/https1/toLessSecure/");
        httpsURIToLessSecure = "https://" + httpsTestServer.serverAuthority() + "/https1/toLessSecure/redirect";
        // HTTPS2 to HTTP2 redirect handler
        https2TestServer.addHandler(new ToLessSecureRedirectHandler(http2URI), "/https2/toLessSecure/");
        https2URIToLessSecure = "https://" + https2TestServer.serverAuthority() + "/https2/toLessSecure/redirect";

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

    // Redirects to same protocol
    static class BasicHttpRedirectHandler implements HttpTestHandler {
        // flip-flop between chunked/variable and fixed length redirect responses
        volatile int count;

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            System.out.println("BasicHttpRedirectHandler for: " + t.getRequestURI());
            readAllRequestData(t);

            if (t.getRequestURI().getPath().endsWith("redirect")) {
                String url = t.getRequestURI().resolve("message").toString();
                t.getResponseHeaders().addHeader("Location", url);
                int len = count % 2 == 0 ? 2 : -1;
                t.sendResponseHeaders(302, len);
                try (OutputStream os = t.getResponseBody()) {
                    os.write(new byte[]{'X', 'Y'});  // stuffing some response body
                }
            } else {
                try (OutputStream os = t.getResponseBody()) {
                    byte[] bytes = MESSAGE.getBytes(UTF_8);
                    t.sendResponseHeaders(200, bytes.length);
                    os.write(bytes);
                }
            }
        }
    }

    // Redirects to a, possibly, more secure protocol, (HTTP to HTTPS)
    static class ToSecureHttpRedirectHandler implements HttpTestHandler {
        final String targetURL;
        ToSecureHttpRedirectHandler(String targetURL) {
            this.targetURL = targetURL;
        }
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            System.out.println("ToSecureHttpRedirectHandler for: " + t.getRequestURI());
            readAllRequestData(t);

            if (t.getRequestURI().getPath().endsWith("redirect")) {
                t.getResponseHeaders().addHeader("Location", targetURL);
                System.out.println("ToSecureHttpRedirectHandler redirecting to: " + targetURL);
                t.sendResponseHeaders(302, 2); // fixed-length
                try (OutputStream os = t.getResponseBody()) {
                    os.write(new byte[]{'X', 'Y'});
                }
            } else {
                Throwable ex = new RuntimeException("Unexpected request");
                ex.printStackTrace();
                t.sendResponseHeaders(500, 0);
            }
        }
    }

    // Redirects to a, possibly, less secure protocol (HTTPS to HTTP)
    static class ToLessSecureRedirectHandler implements HttpTestHandler {
        final String targetURL;
        ToLessSecureRedirectHandler(String targetURL) {
            this.targetURL = targetURL;
        }
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            System.out.println("ToLessSecureRedirectHandler for: " + t.getRequestURI());
            readAllRequestData(t);

            if (t.getRequestURI().getPath().endsWith("redirect")) {
                t.getResponseHeaders().addHeader("Location", targetURL);
                System.out.println("ToLessSecureRedirectHandler redirecting to: " + targetURL);
                t.sendResponseHeaders(302, -1);  // chunked/variable
                try (OutputStream os = t.getResponseBody()) {
                    os.write(new byte[]{'X', 'Y'});
                }
            } else {
                Throwable ex = new RuntimeException("Unexpected request");
                ex.printStackTrace();
                t.sendResponseHeaders(500, 0);
            }
        }
    }

    static void readAllRequestData(HttpTestExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            is.readAllBytes();
        }
    }
}
