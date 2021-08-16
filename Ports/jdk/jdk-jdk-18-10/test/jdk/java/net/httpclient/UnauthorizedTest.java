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
 * @bug 8203882
 * @summary (httpclient) Check that HttpClient throws IOException when
 *      receiving 401/407 with no WWW-Authenticate/Proxy-Authenticate
 *      header only in the case where an authenticator is configured
 *      for the client. If no authenticator is configured the client
 *      should simply let the caller deal with the unauthorized response.
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
 *       -Djdk.httpclient.HttpClient.log=headers
 *       UnauthorizedTest
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
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class UnauthorizedTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;        // HTTP/1.1
    HttpTestServer httpsTestServer;       // HTTPS/1.1
    HttpTestServer http2TestServer;       // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;      // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;
    HttpClient authClient;
    HttpClient noAuthClient;

    static final int ITERATIONS = 3;

    /*
     * NOT_MODIFIED status code results from a conditional GET where
     * the server does not (must not) return a response body because
     * the condition specified in the request disallows it
     */
    static final int UNAUTHORIZED = 401;
    static final int PROXY_UNAUTHORIZED = 407;
    static final int HTTP_OK = 200;
    static final String MESSAGE = "Unauthorized";

    @DataProvider(name = "all")
    public Object[][] positive() {
        return new Object[][] {
                { httpURI   + "/server", UNAUTHORIZED, true, authClient},
                { httpsURI  + "/server", UNAUTHORIZED, true, authClient},
                { http2URI  + "/server", UNAUTHORIZED, true, authClient},
                { https2URI + "/server", UNAUTHORIZED, true, authClient},
                { httpURI   + "/proxy",  PROXY_UNAUTHORIZED, true, authClient},
                { httpsURI  + "/proxy",  PROXY_UNAUTHORIZED, true, authClient},
                { http2URI  + "/proxy",  PROXY_UNAUTHORIZED, true, authClient},
                { https2URI + "/proxy",  PROXY_UNAUTHORIZED, true, authClient},
                { httpURI   + "/server", UNAUTHORIZED, false, authClient},
                { httpsURI  + "/server", UNAUTHORIZED, false, authClient},
                { http2URI  + "/server", UNAUTHORIZED, false, authClient},
                { https2URI + "/server", UNAUTHORIZED, false, authClient},
                { httpURI   + "/proxy",  PROXY_UNAUTHORIZED, false, authClient},
                { httpsURI  + "/proxy",  PROXY_UNAUTHORIZED, false, authClient},
                { http2URI  + "/proxy",  PROXY_UNAUTHORIZED, false, authClient},
                { https2URI + "/proxy",  PROXY_UNAUTHORIZED, false, authClient},
                { httpURI   + "/server", UNAUTHORIZED, true, noAuthClient},
                { httpsURI  + "/server", UNAUTHORIZED, true, noAuthClient},
                { http2URI  + "/server", UNAUTHORIZED, true, noAuthClient},
                { https2URI + "/server", UNAUTHORIZED, true, noAuthClient},
                { httpURI   + "/proxy",  PROXY_UNAUTHORIZED, true, noAuthClient},
                { httpsURI  + "/proxy",  PROXY_UNAUTHORIZED, true, noAuthClient},
                { http2URI  + "/proxy",  PROXY_UNAUTHORIZED, true, noAuthClient},
                { https2URI + "/proxy",  PROXY_UNAUTHORIZED, true, noAuthClient},
                { httpURI   + "/server", UNAUTHORIZED, false, noAuthClient},
                { httpsURI  + "/server", UNAUTHORIZED, false, noAuthClient},
                { http2URI  + "/server", UNAUTHORIZED, false, noAuthClient},
                { https2URI + "/server", UNAUTHORIZED, false, noAuthClient},
                { httpURI   + "/proxy",  PROXY_UNAUTHORIZED, false, noAuthClient},
                { httpsURI  + "/proxy",  PROXY_UNAUTHORIZED, false, noAuthClient},
                { http2URI  + "/proxy",  PROXY_UNAUTHORIZED, false, noAuthClient},
                { https2URI + "/proxy",  PROXY_UNAUTHORIZED, false, noAuthClient},
        };
    }

    static final AtomicLong requestCounter = new AtomicLong();

    static final Authenticator authenticator = new Authenticator() {
    };

    @Test(dataProvider = "all")
    void test(String uriString, int code, boolean async, HttpClient client) throws Throwable {
        out.printf("%n---- starting (%s, %d, %s, %s) ----%n",
                uriString, code, async ? "async" : "sync",
                client.authenticator().isPresent() ? "authClient" : "noAuthClient");
        URI uri = URI.create(uriString);

        HttpRequest.Builder requestBuilder = HttpRequest
                .newBuilder(uri)
                .GET();

        HttpRequest request = requestBuilder.build();
        out.println("Initial request: " + request.uri());

        boolean shouldThrow = client.authenticator().isPresent();
        String header = (code==UNAUTHORIZED)?"WWW-Authenticate":"Proxy-Authenticate";

        HttpResponse<String> response = null;
        try {
           if (async) {
                response = client.send(request, BodyHandlers.ofString());
            } else {
               try {
                   response = client.sendAsync(request, BodyHandlers.ofString()).get();
               } catch (ExecutionException ex) {
                   throw ex.getCause();
               }
           }
        } catch (IOException ex) {
            if (shouldThrow && ex.getMessage().contains(header)) {
                System.out.println("Got expected exception: " + ex);
                return;
            } else throw ex;
        }

        out.println("  Got response: " + response);
        assertEquals(response.statusCode(), code);
        assertEquals(response.body(),
                (code == UNAUTHORIZED ? "WWW-" : "Proxy-") + MESSAGE);
        if (shouldThrow) {
            throw new RuntimeException("Expected IOException not thrown.");
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
        httpTestServer.addHandler(new UnauthorizedHandler(), "/http1/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1";
        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new UnauthorizedHandler(),"/https1/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new UnauthorizedHandler(), "/http2/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2";
        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new UnauthorizedHandler(), "/https2/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2";

        authClient = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .sslContext(sslContext)
                .authenticator(authenticator)
                .build();

        noAuthClient = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .sslContext(sslContext)
                .build();

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

    static class UnauthorizedHandler implements HttpTestHandler {

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            readAllRequestData(t); // shouldn't be any
            String method = t.getRequestMethod();
            String path = t.getRequestURI().getPath();
            HttpTestResponseHeaders rsph = t.getResponseHeaders();

            int code;
            if (path.contains("server")) {
                code = UNAUTHORIZED;
            } else {
                code = PROXY_UNAUTHORIZED;
            }
            String message = (code == UNAUTHORIZED ? "WWW-" : "Proxy-") + MESSAGE;
            byte[] bytes = message.getBytes(UTF_8);
            t.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = t.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static void readAllRequestData(HttpTestExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            is.readAllBytes();
        }
    }
}
