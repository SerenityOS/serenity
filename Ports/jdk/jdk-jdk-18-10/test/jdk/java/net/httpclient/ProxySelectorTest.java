/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244205
 * @summary checks that a different proxy returned for
 *          the same host:port is taken into account
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @library /test/lib http2/server
 * @build HttpServerAdapters DigestEchoServer Http2TestServer ProxySelectorTest
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm
 *       -Djdk.http.auth.tunneling.disabledSchemes
 *       -Djdk.httpclient.HttpClient.log=headers,requests
 *       -Djdk.internal.httpclient.debug=true
 *       ProxySelectorTest
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.lang.System.err;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class ProxySelectorTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;            // HTTP/1.1
    HttpTestServer proxyHttpTestServer;       // HTTP/1.1
    HttpTestServer authProxyHttpTestServer;   // HTTP/1.1
    HttpTestServer http2TestServer;           // HTTP/2 ( h2c )
    HttpTestServer httpsTestServer;           // HTTPS/1.1
    HttpTestServer https2TestServer;          // HTTP/2 ( h2  )
    DigestEchoServer.TunnelingProxy proxy;
    DigestEchoServer.TunnelingProxy authproxy;
    String httpURI;
    String httpsURI;
    String proxyHttpURI;
    String authProxyHttpURI;
    String http2URI;
    String https2URI;
    HttpClient client;

    final ReferenceTracker TRACKER = ReferenceTracker.INSTANCE;
    static final long SLEEP_AFTER_TEST = 0; // milliseconds
    static final int ITERATIONS = 3;
    static final Executor executor = new TestExecutor(Executors.newCachedThreadPool());
    static final ConcurrentMap<String, Throwable> FAILURES = new ConcurrentHashMap<>();
    static volatile boolean tasksFailed;
    static final AtomicLong serverCount = new AtomicLong();
    static final AtomicLong clientCount = new AtomicLong();
    static final long start = System.nanoTime();
    public static String now() {
        long now = System.nanoTime() - start;
        long secs = now / 1000_000_000;
        long mill = (now % 1000_000_000) / 1000_000;
        long nan = now % 1000_000;
        return String.format("[%d s, %d ms, %d ns] ", secs, mill, nan);
    }

    static class TestExecutor implements Executor {
        final AtomicLong tasks = new AtomicLong();
        Executor executor;
        TestExecutor(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(Runnable command) {
            long id = tasks.incrementAndGet();
            executor.execute(() -> {
                try {
                    command.run();
                } catch (Throwable t) {
                    tasksFailed = true;
                    out.printf(now() + "Task %s failed: %s%n", id, t);
                    err.printf(now() + "Task %s failed: %s%n", id, t);
                    FAILURES.putIfAbsent("Task " + id, t);
                    throw t;
                }
            });
        }
    }

    protected boolean stopAfterFirstFailure() {
        return Boolean.getBoolean("jdk.internal.httpclient.debug");
    }

    final AtomicReference<SkipException> skiptests = new AtomicReference<>();
    void checkSkip() {
        var skip = skiptests.get();
        if (skip != null) throw skip;
    }
    static String name(ITestResult result) {
        var params = result.getParameters();
        return result.getName()
                + (params == null ? "()" : Arrays.toString(result.getParameters()));
    }

    @BeforeMethod
    void beforeMethod(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            if (skiptests.get() == null) {
                SkipException skip = new SkipException("some tests failed");
                skip.setStackTrace(new StackTraceElement[0]);
                skiptests.compareAndSet(null, skip);
            }
        }
    }

    @AfterClass
    static final void printFailedTests() {
        out.println("\n=========================");
        try {
            // Exceptions should already have been added to FAILURES
            // var failed = context.getFailedTests().getAllResults().stream()
            //        .collect(Collectors.toMap(r -> name(r), ITestResult::getThrowable));
            // FAILURES.putAll(failed);

            out.printf("%n%sCreated %d servers and %d clients%n",
                    now(), serverCount.get(), clientCount.get());
            if (FAILURES.isEmpty()) return;
            out.println("Failed tests: ");
            FAILURES.entrySet().forEach((e) -> {
                out.printf("\t%s: %s%n", e.getKey(), e.getValue());
                e.getValue().printStackTrace(out);
                e.getValue().printStackTrace();
            });
            if (tasksFailed) {
                out.println("WARNING: Some tasks failed");
            }
        } finally {
            out.println("\n=========================\n");
        }
    }

    /*
     * NOT_MODIFIED status code results from a conditional GET where
     * the server does not (must not) return a response body because
     * the condition specified in the request disallows it
     */
    static final int UNAUTHORIZED = 401;
    static final int PROXY_UNAUTHORIZED = 407;
    static final int HTTP_OK = 200;
    static final String MESSAGE = "Unauthorized";
    enum Schemes {
        HTTP, HTTPS
    }
    @DataProvider(name = "all")
    public Object[][] positive() {
        return new Object[][] {
                { Schemes.HTTP,  HttpClient.Version.HTTP_1_1, httpURI,   true},
                { Schemes.HTTP,  HttpClient.Version.HTTP_2,   http2URI,  true},
                { Schemes.HTTPS, HttpClient.Version.HTTP_1_1, httpsURI,  true},
                { Schemes.HTTPS, HttpClient.Version.HTTP_2,   https2URI, true},
                { Schemes.HTTP,  HttpClient.Version.HTTP_1_1, httpURI,   false},
                { Schemes.HTTP,  HttpClient.Version.HTTP_2,   http2URI,  false},
                { Schemes.HTTPS, HttpClient.Version.HTTP_1_1, httpsURI,  false},
                { Schemes.HTTPS, HttpClient.Version.HTTP_2,   https2URI, false},
        };
    }

    static final AtomicLong requestCounter = new AtomicLong();

    static final AtomicLong sleepCount = new AtomicLong();

    @Test(dataProvider = "all")
    void test(Schemes scheme, HttpClient.Version version, String uri, boolean async)
            throws Throwable
    {
        checkSkip();
        var name = String.format("test(%s, %s, %s)", scheme, version, async);
        out.printf("%n---- starting %s ----%n", name);

        for (int i=0; i<ITERATIONS; i++) {
            if (ITERATIONS > 1) out.printf("---- ITERATION %d%n",i);
            try {
                doTest(scheme, version, uri, async);
                long count = sleepCount.incrementAndGet();
                System.err.println(now() + " Sleeping: " + count);
                Thread.sleep(SLEEP_AFTER_TEST);
                System.err.println(now() + " Waking up: " + count);
            } catch (Throwable x) {
                FAILURES.putIfAbsent(name, x);
                throw x;
            }
        }
    }

    private <T> HttpResponse<T> send(HttpClient client,
                                     URI uri,
                                     HttpResponse.BodyHandler<T> handler,
                                     boolean async) throws Throwable {
        HttpRequest.Builder requestBuilder = HttpRequest
                .newBuilder(uri)
                .GET();

        HttpRequest request = requestBuilder.build();
        out.println("Sending request: " + request.uri());

        HttpResponse<T> response = null;
        if (async) {
            response = client.send(request, handler);
        } else {
            try {
                response = client.sendAsync(request, handler).get();
            } catch (ExecutionException ex) {
                throw ex.getCause();
            }
        }
        return response;
    }

    private void doTest(Schemes scheme,
                        HttpClient.Version version,
                        String uriString,
                        boolean async) throws Throwable {

        URI uri1 = URI.create(uriString + "/server/ProxySelectorTest");
        URI uri2 = URI.create(uriString + "/proxy/noauth/ProxySelectorTest");
        URI uri3 = URI.create(uriString + "/proxy/auth/ProxySelectorTest");

        HttpResponse<String> response;

        // First request should go with a direct connection.
        // A plain server or https server should serve it, and we should get 200 OK
        response = send(client, uri1, BodyHandlers.ofString(), async);
        out.println("Got response from plain server: " + response);
        assertEquals(response.statusCode(), HTTP_OK);
        assertEquals(response.headers().firstValue("X-value"),
                scheme == Schemes.HTTPS ? Optional.of("https-server") : Optional.of("plain-server"));

        // Second request should go through a non authenticating proxy.
        // For a clear connection - a proxy-server should serve it, and we should get 200 OK
        // For an https connection - a tunnel should be established through the non
        // authenticating proxy - and we should receive 200 OK from an https-server
        response = send(client, uri2, BodyHandlers.ofString(), async);
        out.println("Got response through noauth proxy: " + response);
        assertEquals(response.statusCode(), HTTP_OK);
        assertEquals(response.headers().firstValue("X-value"),
                scheme == Schemes.HTTPS ? Optional.of("https-server") : Optional.of("proxy-server"));

        // Third request should go through an authenticating proxy.
        // For a clear connection - an auth-proxy-server should serve it, and we
        // should get 407
        // For an https connection - a tunnel should be established through an
        // authenticating proxy - and we should receive 407 directly from the
        // proxy - so the X-value header will be absent
        response = send(client, uri3, BodyHandlers.ofString(), async);
        out.println("Got response through auth proxy: " + response);
        assertEquals(response.statusCode(), PROXY_UNAUTHORIZED);
        assertEquals(response.headers().firstValue("X-value"),
                scheme == Schemes.HTTPS ? Optional.empty() : Optional.of("auth-proxy-server"));

    }

    // -- Infrastructure

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(new PlainServerHandler("plain-server"), "/http1/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1";
        proxyHttpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        proxyHttpTestServer.addHandler(new PlainServerHandler("proxy-server"), "/http1/proxy/");
        proxyHttpTestServer.addHandler(new PlainServerHandler("proxy-server"), "/http2/proxy/");
        proxyHttpURI = "http://" + httpTestServer.serverAuthority() + "/http1";
        authProxyHttpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        authProxyHttpTestServer.addHandler(new UnauthorizedHandler("auth-proxy-server"), "/http1/proxy/");
        authProxyHttpTestServer.addHandler(new UnauthorizedHandler("auth-proxy-server"), "/http2/proxy/");
        proxyHttpURI = "http://" + httpTestServer.serverAuthority() + "/http1";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new PlainServerHandler("https-server"),"/https1/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new PlainServerHandler("plain-server"), "/http2/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2";
        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new PlainServerHandler("https-server"), "/https2/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2";

        proxy = DigestEchoServer.createHttpsProxyTunnel(DigestEchoServer.HttpAuthSchemeType.NONE);
        authproxy = DigestEchoServer.createHttpsProxyTunnel(DigestEchoServer.HttpAuthSchemeType.BASIC);

        client = TRACKER.track(HttpClient.newBuilder()
                .proxy(new TestProxySelector())
                .sslContext(sslContext)
                .executor(executor)
                .build());
        clientCount.incrementAndGet();


        httpTestServer.start();
        serverCount.incrementAndGet();
        proxyHttpTestServer.start();
        serverCount.incrementAndGet();
        authProxyHttpTestServer.start();
        serverCount.incrementAndGet();
        httpsTestServer.start();
        serverCount.incrementAndGet();
        http2TestServer.start();
        serverCount.incrementAndGet();
        https2TestServer.start();
        serverCount.incrementAndGet();
    }

    @AfterTest
    public void teardown() throws Exception {
        client = null;
        Thread.sleep(100);
        AssertionError fail = TRACKER.check(500);

        proxy.stop();
        authproxy.stop();
        httpTestServer.stop();
        proxyHttpTestServer.stop();
        authProxyHttpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
    }

    class TestProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            String path = uri.getPath();
            out.println("Selecting proxy for: " + uri);
            if (path.contains("/proxy/")) {
                if (path.contains("/http1/") || path.contains("/http2/")) {
                    // Simple proxying
                    var p = path.contains("/auth/") ? authProxyHttpTestServer : proxyHttpTestServer;
                    return List.of(new Proxy(Proxy.Type.HTTP, p.getAddress()));
                } else {
                    // Both HTTPS or HTTPS/2 require tunnelling
                    var p = path.contains("/auth/") ? authproxy : proxy;
                    return List.of(new Proxy(Proxy.Type.HTTP, p.getProxyAddress()));
                }
            }
            System.out.print("NO_PROXY for " + uri);
            return List.of(Proxy.NO_PROXY);
        }
        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            System.err.printf("Connect failed for: uri=\"%s\", sa=\"%s\", ioe=%s%n", uri, sa, ioe);
        }
    }

    static class PlainServerHandler implements HttpTestHandler {

        final String serverType;
        PlainServerHandler(String serverType) {
            this.serverType = serverType;
        }

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            readAllRequestData(t); // shouldn't be any
            String method = t.getRequestMethod();
            String path = t.getRequestURI().getPath();
            HttpTestRequestHeaders  reqh = t.getRequestHeaders();
            HttpTestResponseHeaders rsph = t.getResponseHeaders();

            String xValue = serverType;
            rsph.addHeader("X-value", serverType);

            t.getResponseHeaders().addHeader("X-value", xValue);
            byte[] body = "RESPONSE".getBytes(UTF_8);
            t.sendResponseHeaders(HTTP_OK, body.length);
            try (var out = t.getResponseBody()) {
                out.write(body);
            }
        }
    }

    static class UnauthorizedHandler implements HttpTestHandler {

        final String serverType;
        UnauthorizedHandler(String serverType) {
            this.serverType = serverType;
        }

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            readAllRequestData(t); // shouldn't be any
            String method = t.getRequestMethod();
            String path = t.getRequestURI().getPath();
            HttpTestRequestHeaders  reqh = t.getRequestHeaders();
            HttpTestResponseHeaders rsph = t.getResponseHeaders();

            String xValue = serverType;
            String srv = path.contains("/proxy/") ? "proxy" : "server";
            String prefix = path.contains("/proxy/") ? "Proxy-" : "WWW-";
            int code = path.contains("/proxy/") ? PROXY_UNAUTHORIZED : UNAUTHORIZED;
            String resp = prefix + "Unauthorized";
            rsph.addHeader(prefix + "Authenticate", "Basic realm=\"earth\", charset=\"UTF-8\"");

            byte[] body = resp.getBytes(UTF_8);
            t.getResponseHeaders().addHeader("X-value", xValue);
            t.sendResponseHeaders(code, body.length);
            try (var out = t.getResponseBody()) {
                out.write(body);
            }
        }
    }

    static void readAllRequestData(HttpTestExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            is.readAllBytes();
        }
    }
}
