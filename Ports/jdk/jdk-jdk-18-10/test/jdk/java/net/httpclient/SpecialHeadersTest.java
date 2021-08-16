/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary  Verify that some special headers - such as User-Agent
 *           can be specified by the caller.
 * @bug 8203771 8218546
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer HttpServerAdapters SpecialHeadersTest
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm
 *       -Djdk.httpclient.HttpClient.log=requests,headers,errors
 *       SpecialHeadersTest
 * @run testng/othervm -Djdk.httpclient.allowRestrictedHeaders=Host
 *       -Djdk.httpclient.HttpClient.log=requests,headers,errors
 *       SpecialHeadersTest
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.nio.charset.StandardCharsets.US_ASCII;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SpecialHeadersTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;        // HTTPS/1.1
    HttpTestServer http2TestServer;        // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;       // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    static final String[][] headerNamesAndValues = new String[][]{
            {"User-Agent: <DEFAULT>"},
            {"User-Agent: camel-cased"},
            {"user-agent: all-lower-case"},
            {"user-Agent: mixed"},
            // headers which were restricted before and are now allowable
            {"referer: lower"},
            {"Referer: normal"},
            {"REFERER: upper"},
            {"origin: lower"},
            {"Origin: normal"},
            {"ORIGIN: upper"},
    };

    // Needs net.property enabled for this part of test
    static final String[][] headerNamesAndValues1 = new String[][]{
            {"Host: <DEFAULT>"},
            {"Host: camel-cased"},
            {"host: all-lower-case"},
            {"hoSt: mixed"}
    };

    @DataProvider(name = "variants")
    public Object[][] variants() {
        String prop = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
        boolean hostTest = prop != null && prop.equalsIgnoreCase("host");
        final String[][] testInput = hostTest ? headerNamesAndValues1 : headerNamesAndValues;

        List<Object[]> list = new ArrayList<>();

        for (boolean sameClient : new boolean[] { false, true }) {
            Arrays.asList(testInput).stream()
                    .map(e -> new Object[] {httpURI, e[0], sameClient})
                    .forEach(list::add);
            Arrays.asList(testInput).stream()
                    .map(e -> new Object[] {httpsURI, e[0], sameClient})
                    .forEach(list::add);
            Arrays.asList(testInput).stream()
                    .map(e -> new Object[] {http2URI, e[0], sameClient})
                    .forEach(list::add);
            Arrays.asList(testInput).stream()
                    .map(e -> new Object[] {https2URI, e[0], sameClient})
                    .forEach(list::add);
        }
        return list.stream().toArray(Object[][]::new);
    }

    static final int ITERATION_COUNT = 3; // checks upgrade and re-use

    static String userAgent() {
        return "Java-http-client/" + System.getProperty("java.version");
    }

    static final Map<String, Function<URI,String>> DEFAULTS = Map.of(
        "USER-AGENT", u -> userAgent(), "HOST", u -> u.getRawAuthority());

    @Test(dataProvider = "variants")
    void test(String uriString,
              String headerNameAndValue,
              boolean sameClient)
        throws Exception
    {
        out.println("\n--- Starting ");

        int index = headerNameAndValue.indexOf(":");
        String name = headerNameAndValue.substring(0, index);
        String v = headerNameAndValue.substring(index+1).trim();
        String key = name.toUpperCase(Locale.ROOT);
        boolean useDefault = "<DEFAULT>".equals(v);

        URI uri = URI.create(uriString+"?name="+key);
        String value =  useDefault ? DEFAULTS.get(key).apply(uri) : v;

        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                        .proxy(NO_PROXY)
                        .sslContext(sslContext)
                        .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
            if (!useDefault) {
                requestBuilder.header(name, value);
            }
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> resp = client.send(request, BodyHandlers.ofString());

            out.println("Got response: " + resp);
            out.println("Got body: " + resp.body());
            assertEquals(resp.statusCode(), 200,
                    "Expected 200, got:" + resp.statusCode());

            boolean isInitialRequest = i == 0;
            boolean isSecure = uri.getScheme().equalsIgnoreCase("https");
            boolean isHTTP2 = resp.version() == HTTP_2;
            boolean isNotH2CUpgrade = isSecure || (sameClient == true && !isInitialRequest);
            boolean isDefaultHostHeader = name.equalsIgnoreCase("host") && useDefault;

            // By default, HTTP/2 sets the `:authority:` pseudo-header, instead
            // of the `Host` header. Therefore, there should be no "X-Host"
            // header in the response, except the response to the h2c Upgrade
            // request which will have been sent through HTTP/1.1.

            if (isDefaultHostHeader && isHTTP2 && isNotH2CUpgrade) {
                assertTrue(resp.headers().firstValue("X-" + key).isEmpty());
                assertTrue(resp.headers().allValues("X-" + key).isEmpty());
                out.println("No X-" + key + " header received, as expected");
            } else {
                String receivedHeaderString = value == null ? null
                        : resp.headers().firstValue("X-"+key).get();
                out.println("Got X-" + key + ": " + resp.headers().allValues("X-"+key));
                if (value != null) {
                    assertEquals(receivedHeaderString, value);
                    assertEquals(resp.headers().allValues("X-"+key), List.of(value));
                } else {
                    assertEquals(resp.headers().allValues("X-"+key).size(), 0);
                }
            }
        }
    }

    @Test(dataProvider = "variants")
    void testHomeMadeIllegalHeader(String uriString,
                                   String headerNameAndValue,
                                   boolean sameClient)
        throws Exception
    {
        out.println("\n--- Starting ");
        final URI uri = URI.create(uriString);

        HttpClient client = HttpClient.newBuilder()
                .proxy(NO_PROXY)
                .sslContext(sslContext)
                .build();

        // Test a request which contains an illegal header created
        HttpRequest req = new HttpRequest() {
            @Override public Optional<BodyPublisher> bodyPublisher() {
                return Optional.of(BodyPublishers.noBody());
            }
            @Override public String method() {
                return "GET";
            }
            @Override public Optional<Duration> timeout() {
                return Optional.empty();
            }
            @Override public boolean expectContinue() {
                return false;
            }
            @Override public URI uri() {
                return uri;
            }
            @Override public Optional<HttpClient.Version> version() {
                return Optional.empty();
            }
            @Override public HttpHeaders headers() {
                Map<String, List<String>> map = Map.of("upgrade", List.of("http://foo.com"));
                return HttpHeaders.of(map, (x, y) -> true);
            }
        };

        try {
            HttpResponse<String> response = client.send(req, BodyHandlers.ofString());
            Assert.fail("Unexpected reply: " + response);
        } catch (IllegalArgumentException ee) {
            out.println("Got IAE as expected");
        }
    }

    @Test(dataProvider = "variants")
    void testAsync(String uriString, String headerNameAndValue, boolean sameClient) {
        out.println("\n--- Starting ");
        int index = headerNameAndValue.indexOf(":");
        String name = headerNameAndValue.substring(0, index);
        String v = headerNameAndValue.substring(index+1).trim();
        String key = name.toUpperCase(Locale.ROOT);
        boolean useDefault = "<DEFAULT>".equals(v);

        URI uri = URI.create(uriString+"?name="+key);
        String value =  useDefault ? DEFAULTS.get(key).apply(uri) : v;

        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                        .proxy(NO_PROXY)
                        .sslContext(sslContext)
                        .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
            if (!useDefault) {
                requestBuilder.header(name, value);
            }
            HttpRequest request = requestBuilder.build();

            boolean isInitialRequest = i == 0;
            boolean isSecure = uri.getScheme().equalsIgnoreCase("https");
            boolean isNotH2CUpgrade = isSecure || (sameClient == true && !isInitialRequest);
            boolean isDefaultHostHeader = name.equalsIgnoreCase("host") && useDefault;

            client.sendAsync(request, BodyHandlers.ofString())
                    .thenApply(response -> {
                        out.println("Got response: " + response);
                        out.println("Got body: " + response.body());
                        assertEquals(response.statusCode(), 200);
                        return response;})
                    .thenAccept(resp -> {
                        // By default, HTTP/2 sets the `:authority:` pseudo-header, instead
                        // of the `Host` header. Therefore, there should be no "X-Host"
                        // header in the response, except the response to the h2c Upgrade
                        // request which will have been sent through HTTP/1.1.

                        if (isDefaultHostHeader && resp.version() == HTTP_2 && isNotH2CUpgrade) {
                            assertTrue(resp.headers().firstValue("X-" + key).isEmpty());
                            assertTrue(resp.headers().allValues("X-" + key).isEmpty());
                            out.println("No X-" + key + " header received, as expected");
                        } else {
                            String receivedHeaderString = value == null ? null
                                    : resp.headers().firstValue("X-"+key).get();
                            out.println("Got X-" + key + ": " + resp.headers().allValues("X-"+key));
                            if (value != null) {
                                assertEquals(receivedHeaderString, value);
                                assertEquals(resp.headers().allValues("X-" + key), List.of(value));
                            } else {
                                assertEquals(resp.headers().allValues("X-" + key).size(), 1);
                            }
                        }
                    })
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

    /** A handler that returns, as its body, the exact received request URI.
     *  The header whose name is in the URI query and is set in the request is
     *  returned in the response with its name prefixed by X-
     */
    static class HttpUriStringHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            URI uri = t.getRequestURI();
            String uriString = uri.toString();
            out.println("Http1UriStringHandler received, uri: " + uriString);
            String query = uri.getQuery();
            String headerName = query.substring(query.indexOf("=")+1).trim();
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = uriString.getBytes(US_ASCII);
                t.getRequestHeaders().keySet().stream()
                        .filter(headerName::equalsIgnoreCase)
                        .forEach(h -> {
                            for (String v : t.getRequestHeaders().get(headerName)) {
                                t.getResponseHeaders().addHeader("X-"+h, v);
                            }
                        });
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }
}
