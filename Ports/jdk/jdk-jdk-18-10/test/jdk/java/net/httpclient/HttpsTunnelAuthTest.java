/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;

import static java.lang.System.out;

/**
 * @test
 * @bug 8262027
 * @summary Verify that it's possible to handle proxy authentication manually
 *          even when using an HTTPS tunnel. This test uses an authenticating
 *          proxy (basic auth) serving an authenticated server (basic auth).
 *          The test also helps verifying the fix for 8262027.
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters ProxyServer HttpsTunnelAuthTest
 * @modules java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @run main/othervm -Djdk.httpclient.HttpClient.log=requests,headers,errors
 *                   -Djdk.http.auth.tunneling.disabledSchemes
 *                   -Djdk.httpclient.allowRestrictedHeaders=connection
 *                   -Djdk.internal.httpclient.debug=true
 *                   HttpsTunnelAuthTest
 *
 */
//-Djdk.internal.httpclient.debug=true -Dtest.debug=true
public class HttpsTunnelAuthTest implements HttpServerAdapters, AutoCloseable {

    static final String data[] = {
        "Lorem ipsum",
        "dolor sit amet",
        "consectetur adipiscing elit, sed do eiusmod tempor",
        "quis nostrud exercitation ullamco",
        "laboris nisi",
        "ut",
        "aliquip ex ea commodo consequat." +
        "Duis aute irure dolor in reprehenderit in voluptate velit esse" +
        "cillum dolore eu fugiat nulla pariatur.",
        "Excepteur sint occaecat cupidatat non proident."
    };

    static final SSLContext context;
    static {
        try {
            context = new SimpleSSLContext().get();
            SSLContext.setDefault(context);
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    final String realm = "earth";
    final String sUserName = "arthur";
    final String pUserName = "porpoise";
    final String sPassword = "dent";
    final String pPassword = "fish";
    final String proxyAuth = "Basic " + Base64.getEncoder().withoutPadding()
            .encodeToString((pUserName+":"+pPassword).getBytes(StandardCharsets.US_ASCII));
    final String serverAuth = "Basic " + Base64.getEncoder().withoutPadding()
            .encodeToString((sUserName+":"+sPassword).getBytes(StandardCharsets.UTF_8));
    final DigestEchoServer.HttpTestAuthenticator testAuth =
            new DigestEchoServer.HttpTestAuthenticator(realm, sUserName);

    DigestEchoServer http1Server;
    DigestEchoServer https1Server;
    DigestEchoServer https2Server;
    ProxyServer proxy;
    ProxySelector proxySelector;
    HttpClient client;

    HttpsTunnelAuthTest() {
    }

    void setUp() throws IOException {
        // Creates an HTTP/1.1 Server that will authenticate for
        // arthur with password dent
        http1Server = DigestEchoServer.createServer(Version.HTTP_1_1,
                "http",
                DigestEchoServer.HttpAuthType.SERVER,
                testAuth,
                DigestEchoServer.HttpAuthSchemeType.BASICSERVER,
                new HttpTestEchoHandler(),
                "/");

        // Creates a TLS HTTP/1.1 Server that will authenticate for
        // arthur with password dent
        https1Server = DigestEchoServer.createServer(Version.HTTP_1_1,
                        "https",
                        DigestEchoServer.HttpAuthType.SERVER,
                        testAuth,
                        DigestEchoServer.HttpAuthSchemeType.BASICSERVER,
                        new HttpTestEchoHandler(),
                        "/");

        // Creates a TLS HTTP/2 Server that will authenticate for
        // arthur with password dent
        https2Server = DigestEchoServer.createServer(Version.HTTP_2,
                        "https",
                        DigestEchoServer.HttpAuthType.SERVER,
                        testAuth,
                        DigestEchoServer.HttpAuthSchemeType.BASICSERVER,
                        new HttpTestEchoHandler(), "/");

        // Creates a proxy server that will authenticate for
        // porpoise with password fish.
        proxy = new ProxyServer(0, true, pUserName, pPassword);

        // Creates a proxy selector that unconditionally select the
        // above proxy.
        var ps = proxySelector = ProxySelector.of(proxy.getProxyAddress());

        // Creates a client that uses the above proxy selector
        client = newHttpClient(ps);
    }

    @Override
    public void close() throws Exception {
        if (proxy != null) close(proxy::stop);
        if (http1Server != null) close(http1Server::stop);
        if (https1Server != null) close(https1Server::stop);
        if (https2Server != null) close(https2Server::stop);
    }

    private void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception x) {
            // OK.
        }
    }

    public HttpClient newHttpClient(ProxySelector ps) {
        HttpClient.Builder builder = HttpClient
                .newBuilder()
                .sslContext(context)
                .proxy(ps);
        return builder.build();
    }

    public static void main(String[] args) throws Exception {
        try (HttpsTunnelAuthTest test = new HttpsTunnelAuthTest()) {
            test.setUp();

            // tests proxy and server authentication through:
            // - plain proxy connection to plain HTTP/1.1 server,
            test.test(Version.HTTP_1_1, "http", "/foo/http1");

            // can't really test plain proxy connection to plain HTTP/2 server:
            // this is not supported: we downgrade to HTTP/1.1 in that case
            // so that is actually somewhat equivalent to the first case:
            // therefore we will use a new client to force re-authentication
            // of the proxy connection.
            test.client = test.newHttpClient(test.proxySelector);
            test.test(Version.HTTP_2, "http", "/foo/http2");

            // - proxy tunnel SSL connection to HTTP/1.1 server
            test.test(Version.HTTP_1_1, "https", "/foo/https1");

            // - proxy tunnel SSl connection to HTTP/2 server
            test.test(Version.HTTP_2, "https", "/foo/https2");
        }
    }

    DigestEchoServer server(String scheme, Version version) {
        return switch (scheme) {
            case "https" -> secure(version);
            case "http" -> unsecure(version);
            default -> throw new IllegalArgumentException(scheme);
        };
    }

    DigestEchoServer unsecure(Version version) {
        return switch (version) {
            // when accessing HTTP/2 through a proxy we downgrade to HTTP/1.1
            case HTTP_1_1, HTTP_2 -> http1Server;
            default -> throw new IllegalArgumentException(String.valueOf(version));
        };
    }

    DigestEchoServer secure(Version version) {
        return switch (version) {
            case HTTP_1_1 -> https1Server;
            case HTTP_2 -> https2Server;
            default -> throw new IllegalArgumentException(String.valueOf(version));
        };
    }

    Version expectedVersion(String scheme, Version version) {
        // when trying to send a plain HTTP/2 request through a proxy
        // it should be downgraded to HTTP/1
        return "http".equals(scheme) ? Version.HTTP_1_1 : version;
    }

    public void test(Version version, String scheme, String path) throws Exception {
        System.out.printf("%nTesting %s, %s, %s%n", version, scheme, path);
        DigestEchoServer server = server(scheme, version);
        try {

            URI uri = jdk.test.lib.net.URIBuilder.newBuilder()
                    .scheme(scheme)
                    .host("localhost")
                    .port(server.getServerAddress().getPort())
                    .path(path).build();

            out.println("Proxy is: " + proxySelector.select(uri));

            List<String> lines = List.of(Arrays.copyOfRange(data, 0, data.length));
            assert lines.size() == data.length;
            String body = lines.stream().collect(Collectors.joining("\r\n"));
            HttpRequest.BodyPublisher reqBody = HttpRequest.BodyPublishers.ofString(body);

            // Build first request, with no authorization header
            HttpRequest.Builder req1Builder = HttpRequest
                    .newBuilder(uri)
                    .version(Version.HTTP_2)
                    .POST(reqBody);
            HttpRequest req1 = req1Builder.build();
            out.printf("%nPosting to %s server at: %s%n", expectedVersion(scheme, version), req1);

            // send first request, with no authorization: we expect 407
            HttpResponse<Stream<String>> response = client.send(req1, BodyHandlers.ofLines());
            out.println("Checking response: " + response);
            if (response.body() != null) response.body().sequential().forEach(out::println);

            // check that we got 407, and check that we got the expected
            // Proxy-Authenticate header
            if (response.statusCode() != 407) {
                throw new RuntimeException("Unexpected status code: " + response);
            }
            var pAuthenticate = response.headers().firstValue("proxy-authenticate").get();
            if (!pAuthenticate.equals("Basic realm=\"proxy realm\"")) {
                throw new RuntimeException("Unexpected proxy-authenticate: " + pAuthenticate);
            }

            // Second request will have Proxy-Authorization, no Authorization.
            // We should get 401 from the server this time.
            out.printf("%nPosting with Proxy-Authorization to %s server at: %s%n", expectedVersion(scheme, version), req1);
            HttpRequest authReq1 = HttpRequest.newBuilder(req1, (k, v)-> true)
                    .header("proxy-authorization", proxyAuth).build();
            response = client.send(authReq1, BodyHandlers.ofLines());
            out.println("Checking response: " + response);
            if (response.body() != null) response.body().sequential().forEach(out::println);

            // Check that we have 401, and that we got the expected
            // WWW-Authenticate header
            if (response.statusCode() != 401) {
                throw new RuntimeException("Unexpected status code: " + response);
            }
            var sAuthenticate = response.headers().firstValue("www-authenticate").get();
            if (!sAuthenticate.startsWith("Basic realm=\"earth\"")) {
                throw new RuntimeException("Unexpected authenticate: " + sAuthenticate);
            }

            // Third request has both Proxy-Authorization and Authorization,
            // so we now expect 200
            out.printf("%nPosting with Authorization to %s server at: %s%n", expectedVersion(scheme, version), req1);
            HttpRequest authReq2 = HttpRequest.newBuilder(authReq1, (k, v)-> true)
                    .header("authorization", serverAuth).build();
            response = client.send(authReq2, BodyHandlers.ofLines());
            out.println("Checking response: " + response);

            // Check that we have 200 and the expected body echoed back.
            // Check that the response version is as expected too.
            if (response.statusCode() != 200) {
                throw new RuntimeException("Unexpected status code: " + response);
            }

            if (response.version() != expectedVersion(scheme, version)) {
                throw new RuntimeException("Unexpected protocol version: "
                        + response.version());
            }
            List<String> respLines = response.body().collect(Collectors.toList());
            if (!lines.equals(respLines)) {
                throw new RuntimeException("Unexpected response 1: " + respLines);
            }
        } catch(Throwable t) {
            out.println("Unexpected exception: exiting: " + t);
            t.printStackTrace(out);
            throw t;
        }
    }

}
