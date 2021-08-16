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
 * @summary Preserve URI component escaped octets when converting to HTTP headers
 * @bug 8198716
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
 *       -Djdk.httpclient.HttpClient.log=reqeusts,headers
 *       EscapedOctetsInURI
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static org.testng.Assert.assertEquals;

public class EscapedOctetsInURI {

    SSLContext sslContext;
    HttpServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpsServer httpsTestServer;       // HTTPS/1.1
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    static final String[][] pathsAndQueryStrings = new String[][] {
        // partial-path       URI query
        {  "/001/noSpace", "?noQuotedOctets" },
        {  "/002/noSpace", "?name=chegar,address=Dublin%20Ireland", },
        {  "/003/noSpace", "?target=http%3A%2F%2Fwww.w3.org%2Fns%2Foa%23hasBody" },

        {  "/010/with%20space", "?noQuotedOctets" },
        {  "/011/with%20space", "?name=chegar,address=Dublin%20Ireland" },
        {  "/012/with%20space", "?target=http%3A%2F%2Fwww.w3.org%2Fns%2Foa%23hasBody" },
    };

    @DataProvider(name = "variants")
    public Object[][] variants() {
        List<Object[]> list = new ArrayList<>();

        for (boolean sameClient : new boolean[] { false, true }) {
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {httpURI + e[0] + e[1], sameClient})
                    .forEach(list::add);
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {httpsURI + e[0] + e[1], sameClient})
                    .forEach(list::add);
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {http2URI + e[0] + e[1], sameClient})
                    .forEach(list::add);
            Arrays.asList(pathsAndQueryStrings).stream()
                    .map(e -> new Object[] {https2URI + e[0] + e[1], sameClient})
                    .forEach(list::add);
        }
        return list.stream().toArray(Object[][]::new);
    }

    static final int ITERATION_COUNT = 3; // checks upgrade and re-use

    @Test(dataProvider = "variants")
    void test(String uriString, boolean sameClient) throws Exception {
        System.out.println("\n--- Starting ");

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

            // the response body should contain the exact escaped request URI
            URI retrievedURI = URI.create(resp.body());
            assertEquals(retrievedURI.getRawPath(),  uri.getRawPath());
            assertEquals(retrievedURI.getRawQuery(), uri.getRawQuery());
        }
    }

    @Test(dataProvider = "variants")
    void testAsync(String uriString, boolean sameClient) {
        System.out.println("\n--- Starting ");
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
                  .thenApply(body -> URI.create(body))
                  .thenAccept(retrievedURI -> {
                      // the body should contain the exact escaped request URI
                      assertEquals(retrievedURI.getRawPath(), uri.getRawPath());
                      assertEquals(retrievedURI.getRawQuery(), uri.getRawQuery()); })
                  .join();
        }
    }

    static String serverAuthority(HttpServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getAddress().getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpServer.create(sa, 0);
        httpTestServer.createContext("/http1", new Http1ASCIIUriStringHandler());
        httpURI = "http://" + serverAuthority(httpTestServer) + "/http1";

        httpsTestServer = HttpsServer.create(sa, 0);
        httpsTestServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer.createContext("/https1", new Http1ASCIIUriStringHandler());
        httpsURI = "https://" + serverAuthority(httpsTestServer) + "/https1";

        http2TestServer = new Http2TestServer("localhost", false, 0);
        http2TestServer.addHandler(new HttpASCIIUriStringHandler(), "/http2");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2";

        https2TestServer = new Http2TestServer("localhost", true, sslContext);
        https2TestServer.addHandler(new HttpASCIIUriStringHandler(), "/https2");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop(0);
        httpsTestServer.stop(0);
        http2TestServer.stop();
        https2TestServer.stop();
    }

    /** A handler that returns as its body the exact escaped request URI. */
    static class Http1ASCIIUriStringHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String asciiUriString = t.getRequestURI().toASCIIString();
            out.println("Http1ASCIIUriString received, asciiUriString: " + asciiUriString);
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = asciiUriString.getBytes(US_ASCII);
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    /** A handler that returns as its body the exact escaped request URI. */
    static class HttpASCIIUriStringHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            String asciiUriString = t.getRequestURI().toASCIIString();
            out.println("Http2ASCIIUriString received, asciiUriString: " + asciiUriString);
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = asciiUriString.getBytes(US_ASCII);
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }
}
