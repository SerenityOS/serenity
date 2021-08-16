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
 * @summary Verify that non-US-ASCII chars are replaced with a sequence of
 *          escaped octets that represent that char in the UTF-8 character set.
 * @bug 8201238
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @compile -encoding utf-8 NonAsciiCharsInURI.java
 * @run testng/othervm
 *       -Djdk.httpclient.HttpClient.log=reqeusts,headers
 *       NonAsciiCharsInURI
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
import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.err;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static org.testng.Assert.assertEquals;

public class NonAsciiCharsInURI implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;        // HTTPS/1.1
    HttpTestServer http2TestServer;        // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;       // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    // € = '\u20AC' => 0xE20x820xAC
    static final String[][] pathsAndQueryStrings = new String[][] {
               // partial-path
            {  "/001/plain"                                                            },
            {  "/002/plain?plainQuery"                                                 },
            {  "/003/withEuroSymbol/€"                                                 },
            {  "/004/withEuroSymbol/€?euroSymbol=€"                                    },
            {  "/005/wiki/エリザベス1世_(イングランド女王)"                                },
            {  "/006/x?url=https://ja.wikipedia.org/wiki/エリザベス1世_(イングランド女王)" },
    };

    @DataProvider(name = "variants")
    public Object[][] variants() {
        List<Object[]> list = new ArrayList<>();

        for (boolean sameClient : new boolean[] { false, true }) {
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {httpURI + e[0], sameClient})
                    .forEach(list::add);
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {httpsURI + e[0], sameClient})
                    .forEach(list::add);
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {http2URI + e[0], sameClient})
                    .forEach(list::add);
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {https2URI + e[0], sameClient})
                    .forEach(list::add);
        }
        return list.stream().toArray(Object[][]::new);
    }

    static final int ITERATION_COUNT = 3; // checks upgrade and re-use

    @Test(dataProvider = "variants")
    void test(String uriString, boolean sameClient) throws Exception {
        out.println("\n--- Starting ");
        // The single-argument factory requires any illegal characters in its
        // argument to be quoted and preserves any escaped octets and other
        // characters that are present.
        URI uri = URI.create(uriString);

        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                        .proxy(NO_PROXY)
                        .sslContext(sslContext)
                        .build();

            HttpRequest request = HttpRequest.newBuilder(uri).build();
            HttpResponse<String> resp = client.send(request, BodyHandlers.ofString());

            out.println("Got response: " + resp);
            out.println("Got body: " + resp.body());
            assertEquals(resp.statusCode(), 200,
                    "Expected 200, got:" + resp.statusCode());

            // the response body should contain the toASCIIString
            // representation of the URI
            String expectedURIString = uri.toASCIIString();
            if (!expectedURIString.contains(resp.body())) {
                err.println("Test failed: " + resp);
                throw new AssertionError(expectedURIString +
                                         " does not contain '" + resp.body() + "'");
            } else {
                out.println("Found expected " + resp.body() + " in " + expectedURIString);
            }
        }
    }

    @Test(dataProvider = "variants")
    void testAsync(String uriString, boolean sameClient) {
        out.println("\n--- Starting ");
        URI uri = URI.create(uriString);

        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                        .proxy(NO_PROXY)
                        .sslContext(sslContext)
                        .build();

            HttpRequest request = HttpRequest.newBuilder(uri).build();

            client.sendAsync(request, BodyHandlers.ofString())
                    .thenApply(response -> {
                        out.println("Got response: " + response);
                        out.println("Got body: " + response.body());
                        assertEquals(response.statusCode(), 200);
                        return response.body(); })
                    .thenAccept(body -> {
                        // the response body should contain the toASCIIString
                        // representation of the URI
                        String expectedURIString = uri.toASCIIString();
                        if (!expectedURIString.contains(body)) {
                            err.println("Test failed: " + body);
                            throw new AssertionError(expectedURIString +
                                    " does not contain '" + body + "'");
                        } else {
                            out.println("Found expected " + body + " in "
                                        + expectedURIString);
                        } })
                    .join();
        }
    }

    static String serverAuthority(HttpTestServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getAddress().getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        HttpTestHandler handler = new HttpUriStringHandler();
        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(handler, "/http1");
        httpURI = "http://" + serverAuthority(httpTestServer) + "/http1";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(handler, "/https1");
        httpsURI = "https://" + serverAuthority(httpsTestServer) + "/https1";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(handler, "/http2");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(handler, "/https2");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2";

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

    /** A handler that returns, as its body, the exact received request URI. */
    static class HttpUriStringHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            String uri = t.getRequestURI().toString();
            out.println("Http1UriStringHandler received, uri: " + uri);
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = uri.getBytes(US_ASCII);
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }
}
