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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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
 * @summary This test verifies that the HttpClient works correctly when connected to a
 *          slow server.
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters DigestEchoServer HttpSlowServerTest
 * @modules java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @run main/othervm -Dtest.requiresHost=true
 *                   -Djdk.httpclient.HttpClient.log=headers
 *                   -Djdk.internal.httpclient.debug=false
 *                   HttpSlowServerTest
 *
 */
public class HttpSlowServerTest implements HttpServerAdapters {
    static final List<String> data = List.of(
            "Lorem ipsum",
            "dolor sit amet",
            "consectetur adipiscing elit, sed do eiusmod tempor",
            "quis nostrud exercitation ullamco",
            "laboris nisi",
            "ut",
            "aliquip ex ea commodo consequat.",
            "Duis aute irure dolor in reprehenderit in voluptate velit esse",
            "cillum dolore eu fugiat nulla pariatur.",
            "Excepteur sint occaecat cupidatat non proident."
    );

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
                .proxy(ps);
        return builder.build();
    }

    public void setUp() throws Exception {
        try {
            InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

            // HTTP/1.1
            HttpServer server1 = HttpServer.create(sa, 0);
            server1.setExecutor(executor);
            http1Server = HttpTestServer.of(server1);
            http1Server.addHandler(new HttpTestSlowHandler(), "/HttpSlowServerTest/http1/");
            http1Server.start();
            http1URI = new URI("http://" + http1Server.serverAuthority() + "/HttpSlowServerTest/http1/");


            // HTTPS/1.1
            HttpsServer sserver1 = HttpsServer.create(sa, 100);
            sserver1.setExecutor(executor);
            sserver1.setHttpsConfigurator(new HttpsConfigurator(context));
            https1Server = HttpTestServer.of(sserver1);
            https1Server.addHandler(new HttpTestSlowHandler(), "/HttpSlowServerTest/https1/");
            https1Server.start();
            https1URI = new URI("https://" + https1Server.serverAuthority() + "/HttpSlowServerTest/https1/");

            // HTTP/2.0
            http2Server = HttpTestServer.of(
                    new Http2TestServer("localhost", false, 0));
            http2Server.addHandler(new HttpTestSlowHandler(), "/HttpSlowServerTest/http2/");
            http2Server.start();
            http2URI = new URI("http://" + http2Server.serverAuthority() + "/HttpSlowServerTest/http2/");

            // HTTPS/2.0
            https2Server = HttpTestServer.of(
                    new Http2TestServer("localhost", true, 0));
            https2Server.addHandler(new HttpTestSlowHandler(), "/HttpSlowServerTest/https2/");
            https2Server.start();
            https2URI = new URI("https://" + https2Server.serverAuthority() + "/HttpSlowServerTest/https2/");

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

    public static void main(String[] args) throws Exception {
        HttpSlowServerTest test = new HttpSlowServerTest();
        test.setUp();
        long start = System.nanoTime();
        try {
            test.run(args);
        } finally {
            try {
                long elapsed = System.nanoTime() - start;
                System.out.println("*** Elapsed: " + Duration.ofNanos(elapsed));
            } finally {
                test.tearDown();
            }
        }
    }

    public void run(String... args) throws Exception {
        List<URI> serverURIs = List.of(http1URI, http2URI, https1URI, https2URI);
        for (int i=0; i<20; i++) {
            for (URI base : serverURIs) {
                if (base.getScheme().equalsIgnoreCase("https")) {
                    URI proxy = i % 1 == 0 ? base.resolve(URI.create("proxy/foo?n="+requestCounter.incrementAndGet()))
                    : base.resolve(URI.create("direct/foo?n="+requestCounter.incrementAndGet()));
                    test(proxy);
                }
            }
            for (URI base : serverURIs) {
                URI direct = base.resolve(URI.create("direct/foo?n="+requestCounter.incrementAndGet()));
                test(direct);
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void test(URI uri) throws Exception {
        System.out.println("Testing with " + uri);
        pending.add(uri);
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        CompletableFuture<HttpResponse<String>> resp =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((r, t) -> this.requestCompleted(request, r, t));
        futures.add(resp);
    }

    private void requestCompleted(HttpRequest request, HttpResponse<?> r, Throwable t) {
        responseCounter.incrementAndGet();
        pending.remove(request.uri());
        System.out.println(request + " -> " + (t == null ? r : t)
                + " [still pending: " + (requestCounter.get() - responseCounter.get()) +"]");
        if (pending.size() < 5 && requestCounter.get() > 100) {
            pending.forEach(u -> System.out.println("\tpending: " + u));
        }
    }

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

    public static class HttpTestSlowHandler implements HttpTestHandler {
        static final AtomicLong respCounter = new AtomicLong();
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                assert bytes.length == 0;
                URI u = t.getRequestURI();
                long responseID = Long.parseLong(u.getQuery().substring(2));
                System.out.println("Server " + t.getRequestURI() + " sending response " + responseID);
                t.sendResponseHeaders(200, -1);
                for (String part : data) {
                    bytes = part.getBytes(StandardCharsets.UTF_8);
                    os.write(bytes);
                    os.flush();
                    System.out.println("\tresp:" + responseID + ": wrote " + bytes.length + " bytes");
                    // wait...
                    try { Thread.sleep(300); } catch (InterruptedException x) {};
                }
                System.out.println("\tresp:" + responseID + ": done");
            }
        }
    }

}
