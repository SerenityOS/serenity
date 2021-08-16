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

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;

/**
 * @test
 * @bug 8232853
 * @summary AuthenticationFilter.Cache::remove may throw ConcurrentModificationException
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters DigestEchoServer HttpRedirectTest
 * @modules java.net.http/jdk.internal.net.http.common
 * java.net.http/jdk.internal.net.http.frame
 * java.net.http/jdk.internal.net.http.hpack
 * java.logging
 * java.base/sun.net.www.http
 * java.base/sun.net.www
 * java.base/sun.net
 * @run testng/othervm -Dtest.requiresHost=true
 * -Djdk.httpclient.HttpClient.log=headers
 * -Djdk.internal.httpclient.debug=false
 * AuthFilterCacheTest
 */

public class AuthFilterCacheTest implements HttpServerAdapters {

    static final String RESPONSE_BODY = "Hello World!";
    static final int REQUEST_COUNT = 5;
    static final int URI_COUNT = 6;
    static final CyclicBarrier barrier = new CyclicBarrier(REQUEST_COUNT * URI_COUNT);
    static final SSLContext context;

    static {
        try {
            context = new jdk.test.lib.net.SimpleSSLContext().get();
            SSLContext.setDefault(context);
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }

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
    MyAuthenticator auth;
    HttpClient client;
    Executor executor = Executors.newCachedThreadPool();

    @DataProvider(name = "uris")
    Object[][] testURIs() {
        Object[][] uris = new Object[][]{
                {List.of(http1URI.resolve("direct/orig/"),
                        https1URI.resolve("direct/orig/"),
                        https1URI.resolve("proxy/orig/"),
                        http2URI.resolve("direct/orig/"),
                        https2URI.resolve("direct/orig/"),
                        https2URI.resolve("proxy/orig/"))}
        };
        return uris;
    }

    public HttpClient newHttpClient(ProxySelector ps, Authenticator auth) {
        HttpClient.Builder builder = HttpClient
                .newBuilder()
                .sslContext(context)
                .authenticator(auth)
                .proxy(ps);
        return builder.build();
    }

    @BeforeClass
    public void setUp() throws Exception {
        try {
            InetSocketAddress sa =
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
            auth = new MyAuthenticator();

            // HTTP/1.1
            HttpServer server1 = HttpServer.create(sa, 0);
            server1.setExecutor(executor);
            http1Server = HttpTestServer.of(server1);
            http1Server.addHandler(new TestHandler(), "/AuthFilterCacheTest/http1/");
            http1Server.start();
            http1URI = new URI("http://" + http1Server.serverAuthority()
                    + "/AuthFilterCacheTest/http1/");

            // HTTPS/1.1
            HttpsServer sserver1 = HttpsServer.create(sa, 100);
            sserver1.setExecutor(executor);
            sserver1.setHttpsConfigurator(new HttpsConfigurator(context));
            https1Server = HttpTestServer.of(sserver1);
            https1Server.addHandler(new TestHandler(), "/AuthFilterCacheTest/https1/");
            https1Server.start();
            https1URI = new URI("https://" + https1Server.serverAuthority()
                    + "/AuthFilterCacheTest/https1/");

            // HTTP/2.0
            http2Server = HttpTestServer.of(
                    new Http2TestServer("localhost", false, 0));
            http2Server.addHandler(new TestHandler(), "/AuthFilterCacheTest/http2/");
            http2Server.start();
            http2URI = new URI("http://" + http2Server.serverAuthority()
                    + "/AuthFilterCacheTest/http2/");

            // HTTPS/2.0
            https2Server = HttpTestServer.of(
                    new Http2TestServer("localhost", true, 0));
            https2Server.addHandler(new TestHandler(), "/AuthFilterCacheTest/https2/");
            https2Server.start();
            https2URI = new URI("https://" + https2Server.serverAuthority()
                    + "/AuthFilterCacheTest/https2/");

            proxy = DigestEchoServer.createHttpsProxyTunnel(
                    DigestEchoServer.HttpAuthSchemeType.NONE);
            proxyAddress = proxy.getProxyAddress();
            proxySelector = new HttpProxySelector(proxyAddress);
            client = newHttpClient(proxySelector, auth);

            System.out.println("Setup: done");
        } catch (Exception x) {
            tearDown();
            throw x;
        } catch (Error e) {
            tearDown();
            throw e;
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

        System.out.println("Teardown: done");
    }

    private interface Stoppable<T> {
        void stop(T service) throws Exception;
    }

    static <T> T stop(T service, Stoppable<T> stop) {
        try {
            if (service != null) stop.stop(service);
        } catch (Throwable x) {
        }
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
            // Our proxy only supports tunneling
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

    public static class TestHandler implements HttpTestHandler {
        static final AtomicLong respCounter = new AtomicLong();

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            var count = respCounter.incrementAndGet();
            System.out.println("Responses handled: " + count);
            t.getRequestBody().readAllBytes();

            if (t.getRequestMethod().equalsIgnoreCase("GET")) {
                if (!t.getRequestHeaders().containsKey("Authorization")) {
                    t.getResponseHeaders()
                            .addHeader("WWW-Authenticate", "Basic realm=\"Earth\"");
                    t.sendResponseHeaders(401, 0);
                } else {
                    byte[] resp = RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
                    t.sendResponseHeaders(200, resp.length);
                    try {
                        barrier.await();
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                    t.getResponseBody().write(resp);
                }
            }
            t.close();
        }
    }

    void doClient(List<URI> uris) {
        assert uris.size() == URI_COUNT;
        barrier.reset();
        System.out.println("Client opening connection to: " + uris.toString());

        List<CompletableFuture<HttpResponse<String>>> cfs = new ArrayList<>();

        for (int i = 0; i < REQUEST_COUNT; i++) {
            for (URI uri : uris) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(uri)
                        .build();
                cfs.add(client.sendAsync(req, HttpResponse.BodyHandlers.ofString()));
            }
        }
        CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0])).join();
    }

    static class MyAuthenticator extends Authenticator {
        private int count = 0;

        MyAuthenticator() {
            super();
        }

        public PasswordAuthentication getPasswordAuthentication() {
            System.out.println("Authenticator called: " + ++count);
            return (new PasswordAuthentication("user" + count,
                    ("passwordNotCheckedAnyway" + count).toCharArray()));
        }

        public int getCount() {
            return count;
        }
    }

    @Test(dataProvider = "uris")
    public void test(List<URI> uris) throws Exception {
        System.out.println("Server listening at " + uris.toString());
        doClient(uris);
    }
}
