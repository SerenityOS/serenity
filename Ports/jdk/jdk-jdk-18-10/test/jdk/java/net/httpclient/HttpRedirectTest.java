/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @test
 * @bug 8232625
 * @summary This test verifies that the HttpClient works correctly when redirecting a post request.
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters DigestEchoServer HttpRedirectTest
 * @modules java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @run testng/othervm -Dtest.requiresHost=true
 *                   -Djdk.httpclient.HttpClient.log=headers
 *                   -Djdk.internal.httpclient.debug=false
 *                   HttpRedirectTest
 *
 */
public class HttpRedirectTest implements HttpServerAdapters {
    static final String GET_RESPONSE_BODY = "Lorem ipsum dolor sit amet";
    static final String REQUEST_BODY = "Here it goes";
    static final SSLContext context;
    static {
        try {
            context = new SimpleSSLContext().get();
            SSLContext.setDefault(context);
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    final AtomicLong requestCounter = new AtomicLong();
    final AtomicLong responseCounter = new AtomicLong();
    HttpTestServer http1Server;
    HttpTestServer http2Server;
    HttpTestServer https1Server;
    HttpTestServer https2Server;
    DigestEchoServer.TunnelingProxy proxy;

    URI http1URI;
    URI https1URI;
    URI http2URI;
    URI https2URI;
    InetSocketAddress proxyAddress;
    ProxySelector proxySelector;
    HttpClient client;
    List<CompletableFuture<?>>  futures = new CopyOnWriteArrayList<>();
    Set<URI> pending = new CopyOnWriteArraySet<>();

    final ExecutorService executor = new ThreadPoolExecutor(12, 60, 10,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>()); // Shared by HTTP/1.1 servers
    final ExecutorService clientexec = new ThreadPoolExecutor(6, 12, 1,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>()); // Used by the client

    public HttpClient newHttpClient(ProxySelector ps) {
        HttpClient.Builder builder = HttpClient
                .newBuilder()
                .sslContext(context)
                .executor(clientexec)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .proxy(ps);
        return builder.build();
    }

    @DataProvider(name="uris")
    Object[][] testURIs() throws URISyntaxException {
        List<URI> uris = List.of(
                http1URI.resolve("direct/orig/"),
                https1URI.resolve("direct/orig/"),
                https1URI.resolve("proxy/orig/"),
                http2URI.resolve("direct/orig/"),
                https2URI.resolve("direct/orig/"),
                https2URI.resolve("proxy/orig/"));
        List<Map.Entry<Integer, String>> redirects = List.of(
                Map.entry(301, "GET"),
                Map.entry(308, "POST"),
                Map.entry(302, "GET"),
                Map.entry(303, "GET"),
                Map.entry(307, "POST"),
                Map.entry(300, "DO_NOT_FOLLOW"),
                Map.entry(304, "DO_NOT_FOLLOW"),
                Map.entry(305, "DO_NOT_FOLLOW"),
                Map.entry(306, "DO_NOT_FOLLOW"),
                Map.entry(309, "DO_NOT_FOLLOW"),
                Map.entry(new Random().nextInt(90) + 310, "DO_NOT_FOLLOW")
        );
        Object[][] tests = new Object[redirects.size() * uris.size()][3];
        int count = 0;
        for (int i=0; i < uris.size(); i++) {
            URI u = uris.get(i);
            for (int j=0; j < redirects.size() ; j++) {
                int code = redirects.get(j).getKey();
                String m = redirects.get(j).getValue();
                tests[count][0] = u.resolve(code +"/");
                tests[count][1] = code;
                tests[count][2] = m;
                count++;
            }
        }
        return tests;
    }

    @BeforeClass
    public void setUp() throws Exception {
        try {
            InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

            // HTTP/1.1
            HttpServer server1 = HttpServer.create(sa, 0);
            server1.setExecutor(executor);
            http1Server = HttpTestServer.of(server1);
            http1Server.addHandler(new HttpTestRedirectHandler("http", http1Server),
                    "/HttpRedirectTest/http1/");
            http1Server.start();
            http1URI = new URI("http://" + http1Server.serverAuthority() + "/HttpRedirectTest/http1/");


            // HTTPS/1.1
            HttpsServer sserver1 = HttpsServer.create(sa, 100);
            sserver1.setExecutor(executor);
            sserver1.setHttpsConfigurator(new HttpsConfigurator(context));
            https1Server = HttpTestServer.of(sserver1);
            https1Server.addHandler(new HttpTestRedirectHandler("https", https1Server),
                    "/HttpRedirectTest/https1/");
            https1Server.start();
            https1URI = new URI("https://" + https1Server.serverAuthority() + "/HttpRedirectTest/https1/");

            // HTTP/2.0
            http2Server = HttpTestServer.of(
                    new Http2TestServer("localhost", false, 0));
            http2Server.addHandler(new HttpTestRedirectHandler("http", http2Server),
                    "/HttpRedirectTest/http2/");
            http2Server.start();
            http2URI = new URI("http://" + http2Server.serverAuthority() + "/HttpRedirectTest/http2/");

            // HTTPS/2.0
            https2Server = HttpTestServer.of(
                    new Http2TestServer("localhost", true, 0));
            https2Server.addHandler(new HttpTestRedirectHandler("https", https2Server),
                    "/HttpRedirectTest/https2/");
            https2Server.start();
            https2URI = new URI("https://" + https2Server.serverAuthority() + "/HttpRedirectTest/https2/");

            proxy = DigestEchoServer.createHttpsProxyTunnel(
                    DigestEchoServer.HttpAuthSchemeType.NONE);
            proxyAddress = proxy.getProxyAddress();
            proxySelector = new HttpProxySelector(proxyAddress);
            client = newHttpClient(proxySelector);
            System.out.println("Setup: done");
        } catch (Exception x) {
            tearDown(); throw x;
        } catch (Error e) {
            tearDown(); throw e;
        }
    }

    private void testNonIdempotent(URI u, HttpRequest request,
                                   int code, String method) throws Exception {
        System.out.println("Testing with " + u);
        CompletableFuture<HttpResponse<String>> respCf =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> resp = respCf.join();
        if (method.equals("DO_NOT_FOLLOW")) {
            assertEquals(resp.statusCode(), code, u + ": status code");
        } else {
            assertEquals(resp.statusCode(), 200, u + ": status code");
        }
        if (method.equals("POST")) {
            assertEquals(resp.body(), REQUEST_BODY, u + ": body");
        } else if (code == 304) {
            assertEquals(resp.body(), "", u + ": body");
        } else if (method.equals("DO_NOT_FOLLOW")) {
            assertNotEquals(resp.body(), GET_RESPONSE_BODY, u + ": body");
            assertNotEquals(resp.body(), REQUEST_BODY, u + ": body");
        } else {
            assertEquals(resp.body(), GET_RESPONSE_BODY, u + ": body");
        }
    }

    public void testIdempotent(URI u, HttpRequest request,
                               int code, String method) throws Exception {
        CompletableFuture<HttpResponse<String>> respCf =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> resp = respCf.join();
        if (method.equals("DO_NOT_FOLLOW")) {
            assertEquals(resp.statusCode(), code, u + ": status code");
        } else {
            assertEquals(resp.statusCode(), 200, u + ": status code");
        }
        if (method.equals("POST")) {
            assertEquals(resp.body(), REQUEST_BODY, u + ": body");
        } else if (code == 304) {
            assertEquals(resp.body(), "", u + ": body");
        } else if (method.equals("DO_NOT_FOLLOW")) {
            assertNotEquals(resp.body(), GET_RESPONSE_BODY, u + ": body");
            assertNotEquals(resp.body(), REQUEST_BODY, u + ": body");
        } else if (code == 303) {
            assertEquals(resp.body(), GET_RESPONSE_BODY, u + ": body");
        } else {
            assertEquals(resp.body(), REQUEST_BODY, u + ": body");
        }
    }

    @Test(dataProvider = "uris")
    public void testPOST(URI uri, int code, String method) throws Exception {
        URI u = uri.resolve("foo?n=" + requestCounter.incrementAndGet());
        HttpRequest request = HttpRequest.newBuilder(u)
                .POST(HttpRequest.BodyPublishers.ofString(REQUEST_BODY)).build();
        // POST is not considered idempotent.
        testNonIdempotent(u, request, code, method);
    }

    @Test(dataProvider = "uris")
    public void testPUT(URI uri, int code, String method) throws Exception {
        URI u = uri.resolve("foo?n=" + requestCounter.incrementAndGet());
        System.out.println("Testing with " + u);
        HttpRequest request = HttpRequest.newBuilder(u)
                .PUT(HttpRequest.BodyPublishers.ofString(REQUEST_BODY)).build();
        // PUT is considered idempotent.
        testIdempotent(u, request, code, method);
    }

    @Test(dataProvider = "uris")
    public void testFoo(URI uri, int code, String method) throws Exception {
        URI u = uri.resolve("foo?n=" + requestCounter.incrementAndGet());
        System.out.println("Testing with " + u);
        HttpRequest request = HttpRequest.newBuilder(u)
                .method("FOO",
                        HttpRequest.BodyPublishers.ofString(REQUEST_BODY)).build();
        // FOO is considered idempotent.
        testIdempotent(u, request, code, method);
    }

    @Test(dataProvider = "uris")
    public void testGet(URI uri, int code, String method) throws Exception {
        URI u = uri.resolve("foo?n=" + requestCounter.incrementAndGet());
        System.out.println("Testing with " + u);
        HttpRequest request = HttpRequest.newBuilder(u)
                .method("GET",
                        HttpRequest.BodyPublishers.ofString(REQUEST_BODY)).build();
        CompletableFuture<HttpResponse<String>> respCf =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> resp = respCf.join();
        // body will be preserved except for 304 and 303: this is a GET.
        if (method.equals("DO_NOT_FOLLOW")) {
            assertEquals(resp.statusCode(), code, u + ": status code");
        } else {
            assertEquals(resp.statusCode(), 200, u + ": status code");
        }
        if (code == 304) {
            assertEquals(resp.body(), "", u + ": body");
        } else if (method.equals("DO_NOT_FOLLOW")) {
            assertNotEquals(resp.body(), GET_RESPONSE_BODY, u + ": body");
            assertNotEquals(resp.body(), REQUEST_BODY, u + ": body");
        } else if (code == 303) {
            assertEquals(resp.body(), GET_RESPONSE_BODY, u + ": body");
        } else {
            assertEquals(resp.body(), REQUEST_BODY, u + ": body");
        }
    }

    @AfterClass
    public void tearDown() {
        proxy = stop(proxy, DigestEchoServer.TunnelingProxy::stop);
        http1Server = stop(http1Server, HttpTestServer::stop);
        https1Server = stop(https1Server, HttpTestServer::stop);
        http2Server = stop(http2Server, HttpTestServer::stop);
        https2Server = stop(https2Server, HttpTestServer::stop);
        client = null;
        try {
            executor.awaitTermination(2000, TimeUnit.MILLISECONDS);
        } catch (Throwable x) {
        } finally {
            executor.shutdownNow();
        }
        try {
            clientexec.awaitTermination(2000, TimeUnit.MILLISECONDS);
        } catch (Throwable x) {
        } finally {
            clientexec.shutdownNow();
        }
        System.out.println("Teardown: done");
    }

    private interface Stoppable<T> { public void stop(T service) throws Exception; }

    static <T>  T stop(T service, Stoppable<T> stop) {
        try { if (service != null) stop.stop(service); } catch (Throwable x) { };
        return null;
    }

    static class HttpProxySelector extends ProxySelector {
        private static final List<Proxy> NO_PROXY = List.of(Proxy.NO_PROXY);
        private final List<Proxy> proxyList;
        HttpProxySelector(InetSocketAddress proxyAddress) {
            proxyList = List.of(new Proxy(Proxy.Type.HTTP, proxyAddress));
        }

        @Override
        public List<Proxy> select(URI uri) {
            // our proxy only supports tunneling
            if (uri.getScheme().equalsIgnoreCase("https")) {
                if (uri.getPath().contains("/proxy/")) {
                    return proxyList;
                }
            }
            return NO_PROXY;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            System.err.println("Connection to proxy failed: " + ioe);
            System.err.println("Proxy: " + sa);
            System.err.println("\tURI: " + uri);
            ioe.printStackTrace();
        }
    }

    public static class HttpTestRedirectHandler implements HttpTestHandler {
        static final AtomicLong respCounter = new AtomicLong();
        final String scheme;
        final HttpTestServer server;
        HttpTestRedirectHandler(String scheme, HttpTestServer server) {
            this.scheme = scheme;
            this.server = server;
        }

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody()) {
                byte[] bytes = is.readAllBytes();
                URI u = t.getRequestURI();
                long responseID = Long.parseLong(u.getQuery().substring(2));
                String path = u.getPath();
                int i = path.lastIndexOf('/');
                String file = path.substring(i+1);
                String parent =  path.substring(0, i);
                int code = 200;
                if (file.equals("foo")) {
                    i = parent.lastIndexOf("/");
                    code = Integer.parseInt(parent.substring(i+1));
                }
                String response;
                if (code == 200) {
                    if (t.getRequestMethod().equals("GET")) {
                        if (bytes.length == 0) {
                            response = GET_RESPONSE_BODY;
                        } else {
                            response = new String(bytes, StandardCharsets.UTF_8);
                        }
                    } else if (t.getRequestMethod().equals("POST")) {
                        response = new String(bytes, StandardCharsets.UTF_8);
                    } else {
                        response = new String(bytes, StandardCharsets.UTF_8);
                    }
                } else if (code < 300 || code > 399) {
                    response = "Unexpected code: " + code;
                    code = 400;
                } else {
                    try {
                        URI reloc = new URI(scheme, server.serverAuthority(), parent + "/bar", u.getQuery(), null);
                        t.getResponseHeaders().addHeader("Location", reloc.toASCIIString());
                        if (code != 304) {
                            response = "Code: " + code;
                        } else response = null;
                    } catch (URISyntaxException x) {
                        x.printStackTrace();
                        x.printStackTrace(System.out);
                        code = 400;
                        response = x.toString();
                    }
                }

                System.out.println("Server " + t.getRequestURI() + " sending response " + responseID);
                System.out.println("code: " + code + " body: " + response);
                t.sendResponseHeaders(code, code == 304 ? 0: -1);
                if (code != 304) {
                    try (OutputStream os = t.getResponseBody()) {
                        bytes = response.getBytes(StandardCharsets.UTF_8);
                        os.write(bytes);
                        os.flush();
                    }
                } else {
                    bytes = new byte[0];
                }

                System.out.println("\tresp:" + responseID + ": wrote " + bytes.length + " bytes");
            } catch (Throwable e) {
                e.printStackTrace();
                e.printStackTrace(System.out);
                throw e;
            }
        }
    }

}
