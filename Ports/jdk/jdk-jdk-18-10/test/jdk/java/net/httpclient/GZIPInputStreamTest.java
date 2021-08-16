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

/*
 * @test
 * @bug 8217264
 * @summary Tests that you can map an InputStream to a GZIPInputStream
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm GZIPInputStreamTest
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
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class GZIPInputStreamTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;    // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;   // HTTPS/1.1
    HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    static final int ITERATION_COUNT = 3;
    // a shared executor helps reduce the amount of threads created by the test
    // this test will block if the executor doesn't have at least two threads.
    static final Executor executor = Executors.newFixedThreadPool(2);
    static final Executor singleThreadExecutor = Executors.newSingleThreadExecutor();

    public static final String LOREM_IPSUM =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Proin et lorem a sem faucibus finibus. "
                    + "Nam nisl nibh, elementum laoreet rutrum quis, lobortis at sem. "
                    + "Aenean purus libero, vehicula sed auctor ac, finibus commodo massa. "
                    + "Etiam dapibus nisl ex, viverra iaculis sapien suscipit sit amet. "
                    + "Phasellus fringilla id orci sit amet convallis. "
                    + "Nam suscipit tempor felis sed feugiat. "
                    + "Mauris quis viverra justo, vitae vulputate turpis. "
                    + "Ut eu orci eget ante faucibus volutpat quis quis urna. "
                    + "Ut porttitor mattis diam, ac sollicitudin ligula volutpat vel. "
                    + "Quisque pretium leo sed augue lacinia, eu mollis dui tempor.\n\n"
                    + "Nullam at mi porttitor, condimentum enim et, tristique felis. "
                    + "Nulla ante elit, interdum id ante ac, dignissim suscipit urna. "
                    + "Sed rhoncus felis eget placerat tincidunt. "
                    + "Duis pellentesque, eros et laoreet lacinia, urna arcu elementum metus, "
                    + "et tempor nibh ante vel odio. "
                    + "Donec et dolor posuere, sagittis libero sit amet, imperdiet ligula. "
                    + "Sed aliquam nulla congue bibendum hendrerit. "
                    + "Morbi ut tincidunt turpis. "
                    + "Nullam semper ipsum et sem imperdiet, sit amet commodo turpis euismod. "
                    + "Nullam aliquet metus id libero elementum, ut pulvinar urna gravida. "
                    + "Nullam non rhoncus diam. "
                    + "Mauris sagittis bibendum odio, sed accumsan sem ullamcorper ut.\n\n"
                    + "Proin malesuada nisl a quam dignissim rhoncus. "
                    + "Pellentesque vitae dui velit. "
                    + "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Vivamus sagittis magna id magna vestibulum, nec lacinia odio maximus. "
                    + "Nunc commodo, nisl non sagittis posuere, tortor ligula accumsan diam, "
                    + "a rhoncus augue velit quis enim. "
                    + "Nulla et dictum mauris. "
                    + "Vivamus et accumsan mauris, et tincidunt nunc.\n\n"
                    + "Nullam non pharetra lectus. "
                    + "Fusce lobortis sapien ante, quis egestas tellus tincidunt efficitur. "
                    + "Proin tempus mollis urna, sit amet congue diam eleifend in. "
                    + "Ut auctor metus ipsum, at porta turpis consectetur sed. "
                    + "Ut malesuada euismod massa, ut elementum nisi mattis eget. "
                    + "Donec ultrices vel dolor at convallis. "
                    + "Nunc eget felis nec nunc faucibus finibus. "
                    + "Curabitur nec auctor metus, sit amet tristique lorem. "
                    + "Donec tempus fringilla suscipit. Cras sit amet ante elit. "
                    + "Ut sodales sagittis eros quis cursus. "
                    + "Maecenas finibus ante quis euismod rutrum. "
                    + "Aenean scelerisque placerat nisi. "
                    + "Fusce porta, nibh vel efficitur sodales, urna eros consequat tellus, "
                    + "at fringilla ex justo in mi. "
                    + "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Morbi accumsan, justo nec tincidunt pretium, justo ex consectetur ante, "
                    + "nec euismod diam velit vitae quam.\n\n"
                    + "Vestibulum ante ipsum primis in faucibus orci luctus et "
                    + "ultrices posuere cubilia Curae; "
                    + "Praesent eget consequat nunc, vel dapibus nulla. "
                    + "Maecenas egestas luctus consectetur. "
                    + "Duis lacus risus, sollicitudin sit amet justo sed, "
                    + "ultrices facilisis sapien. "
                    + "Mauris eget fermentum risus. "
                    + "Suspendisse potenti. Nam at tempor risus. "
                    + "Quisque lacus augue, dictum vel interdum quis, interdum et mi. "
                    + "In purus mauris, pellentesque et lectus eget, condimentum pretium odio."
                    + " Donec imperdiet congue laoreet. "
                    + "Cras pharetra hendrerit purus ac efficitur. \n";



    @DataProvider(name = "variants")
    public Object[][] variants() {
        return new Object[][]{
                { httpURI,   false },
                { httpURI,   true },
                { httpsURI,  false },
                { httpsURI,  true },
                { http2URI,  false },
                { http2URI,  true },
                { https2URI, false },
                { https2URI, true },
        };
    }

    final ReferenceTracker TRACKER = ReferenceTracker.INSTANCE;
    HttpClient newHttpClient() {
        return TRACKER.track(HttpClient.newBuilder()
                         .executor(executor)
                         .sslContext(sslContext)
                         .build());
    }

    HttpClient newSingleThreadClient() {
        return TRACKER.track(HttpClient.newBuilder()
                .executor(singleThreadExecutor)
                .sslContext(sslContext)
                .build());
    }

    HttpClient newInLineClient() {
        return TRACKER.track(HttpClient.newBuilder()
                .executor((r) -> r.run() )
                .sslContext(sslContext)
                .build());
    }

    @Test(dataProvider = "variants")
    public void testPlainSyncAsString(String uri, boolean sameClient) throws Exception {
        out.println("\nSmoke test: verify that the result we get from the server is correct.");
        out.println("Uses plain send() and `asString` to get the plain string.");
        out.println("Uses single threaded executor");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newSingleThreadClient(); // should work with 1 single thread

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri +"/txt/LoremIpsum.txt"))
                    .build();
            BodyHandler<String> handler = BodyHandlers.ofString(UTF_8);
            HttpResponse<String> response = client.send(req, handler);
            String lorem = response.body();
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testPlainSyncAsInputStream(String uri, boolean sameClient) throws Exception {
        out.println("Uses plain send() and `asInputStream` - calls readAllBytes() from main thread");
        out.println("Uses single threaded executor");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newSingleThreadClient(); // should work with 1 single thread

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/txt/LoremIpsum.txt"))
                    .build();
            BodyHandler<InputStream> handler = BodyHandlers.ofInputStream();
            HttpResponse<InputStream> response = client.send(req, handler);
            String lorem = new String(response.body().readAllBytes(), UTF_8);
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testGZIPSyncAsInputStream(String uri, boolean sameClient) throws Exception {
        out.println("Uses plain send() and `asInputStream` - " +
                "creates GZIPInputStream and calls readAllBytes() from main thread");
        out.println("Uses single threaded executor");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newSingleThreadClient(); // should work with 1 single thread

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/gz/LoremIpsum.txt.gz"))
                    .build();
            BodyHandler<InputStream> handler = BodyHandlers.ofInputStream();
            HttpResponse<InputStream> response = client.send(req, handler);
            GZIPInputStream gz = new GZIPInputStream(response.body());
            String lorem = new String(gz.readAllBytes(), UTF_8);
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testGZIPSyncAsGZIPInputStream(String uri, boolean sameClient) throws Exception {
        out.println("Uses plain send() and a mapping subscriber to "+
                "create the GZIPInputStream. Calls readAllBytes() from main thread");
        out.println("Uses a fixed thread pool executor with 2 thread");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(); // needs at least 2 threads

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/gz/LoremIpsum.txt.gz"))
                    .build();
            // This is dangerous, because the finisher will block.
            // We support this, but the executor must have enough threads.
            BodyHandler<InputStream> handler = new GZIPBodyHandler();
            HttpResponse<InputStream> response = client.send(req, handler);
            String lorem = new String(response.body().readAllBytes(), UTF_8);
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testGZIPSyncAsGZIPInputStreamSupplier(String uri, boolean sameClient) throws Exception {
        out.println("Uses plain send() and a mapping subscriber to "+
                "create a Supplier<GZIPInputStream>. Calls Supplier.get() " +
                "and readAllBytes() from main thread");
        out.println("Uses a single threaded executor");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newSingleThreadClient(); // should work with 1 single thread

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/gz/LoremIpsum.txt.gz"))
                    .build();
            // This is dangerous, because the finisher will block.
            // We support this, but the executor must have enough threads.
            BodyHandler<Supplier<InputStream>> handler = new BodyHandler<Supplier<InputStream>>() {
                 public HttpResponse.BodySubscriber<Supplier<InputStream>> apply(
                         HttpResponse.ResponseInfo responseInfo)
                 {
                    String contentType = responseInfo.headers().firstValue("Content-Encoding")
                            .orElse("identity");
                    out.println("Content-Encoding: " + contentType);
                    if (contentType.equalsIgnoreCase("gzip")) {
                        // This is dangerous. Blocking in the mapping function can wedge the
                        // response. We do support it provided that there enough thread in
                        // the executor.
                        return BodySubscribers.mapping(BodySubscribers.ofInputStream(),
                                (is) -> (() -> {
                                    try {
                                        return new GZIPInputStream(is);
                                    } catch (IOException io) {
                                        throw new UncheckedIOException(io);
                                    }
                                }));
                    } else return BodySubscribers.mapping(BodySubscribers.ofInputStream(),
                            (is) -> (() -> is));
                }
            };
            HttpResponse<Supplier<InputStream>> response = client.send(req, handler);
            String lorem = new String(response.body().get().readAllBytes(), UTF_8);
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testPlainAsyncAsInputStreamBlocks(String uri, boolean sameClient) throws Exception {
        out.println("Uses sendAsync() and `asInputStream`. Registers a dependent action "+
                "that calls readAllBytes()");
        out.println("Uses a fixed thread pool executor with two threads");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(); // needs at least 2 threads

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/txt/LoremIpsum.txt"))
                    .build();
            BodyHandler<InputStream> handler = BodyHandlers.ofInputStream();
            CompletableFuture<HttpResponse<InputStream>> responseCF = client.sendAsync(req, handler);
            // This is dangerous. Blocking in the mapping function can wedge the
            // response. We do support it provided that there enough threads in
            // the executor.
            String lorem = responseCF.thenApply((r) -> {
                try {
                    return new String(r.body().readAllBytes(), UTF_8);
                } catch (IOException io) {
                    throw new UncheckedIOException(io);
                }
            }).join();
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testGZIPAsyncAsGZIPInputStreamBlocks(String uri, boolean sameClient) throws Exception {
        out.println("Uses sendAsync() and a mapping subscriber to create a GZIPInputStream. " +
                 "Registers a dependent action that calls readAllBytes()");
        out.println("Uses a fixed thread pool executor with two threads");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(); // needs at least 2 threads

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/gz/LoremIpsum.txt.gz"))
                    .build();
            BodyHandler<InputStream> handler = new GZIPBodyHandler();
            CompletableFuture<HttpResponse<InputStream>> responseCF = client.sendAsync(req, handler);
            // This is dangerous - we support this, but it can block
            // if there are not enough threads available.
            // Correct custom code should use thenApplyAsync instead.
            String lorem = responseCF.thenApply((r) -> {
                try {
                    return new String(r.body().readAllBytes(), UTF_8);
                } catch (IOException io) {
                    throw new UncheckedIOException(io);
                }
            }).join();
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testGZIPSyncAsGZIPInputStreamBlocks(String uri, boolean sameClient) throws Exception {
        out.println("Uses sendAsync() and a mapping subscriber to create a GZIPInputStream," +
                "which is mapped again using a mapping subscriber " +
                "to call readAllBytes() and map to String");
        out.println("Uses a fixed thread pool executor with two threads");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(); // needs at least 2 threads

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/gz/LoremIpsum.txt.gz"))
                    .build();
            // This is dangerous. Blocking in the mapping function can wedge the
            // response. We do support it provided that there enough thread in
            // the executor.
            BodyHandler<String> handler = new MappingBodyHandler<>(new GZIPBodyHandler(),
                    (InputStream is) ->  {
                        try {
                            return new String(is.readAllBytes(), UTF_8);
                        } catch(IOException io) {
                            throw new UncheckedIOException(io);
                        }
                    });
            HttpResponse<String> response = client.send(req, handler);
            String lorem = response.body();
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    @Test(dataProvider = "variants")
    public void testGZIPSyncAsGZIPInputStreamSupplierInline(String uri, boolean sameClient) throws Exception {
        out.println("Uses plain send() and a mapping subscriber to "+
                "create a Supplier<GZIPInputStream>. Calls Supplier.get() " +
                "and readAllBytes() from main thread");
        out.println("Uses an inline executor (no threads)");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newInLineClient(); // should even work with no threads

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri + "/gz/LoremIpsum.txt.gz"))
                    .build();
            // This is dangerous, because the finisher will block.
            // We support this, but the executor must have enough threads.
            BodyHandler<Supplier<InputStream>> handler = new BodyHandler<Supplier<InputStream>>() {
                public HttpResponse.BodySubscriber<Supplier<InputStream>> apply(
                        HttpResponse.ResponseInfo responseInfo)
                {
                    String contentType = responseInfo.headers().firstValue("Content-Encoding")
                            .orElse("identity");
                    out.println("Content-Encoding: " + contentType);
                    if (contentType.equalsIgnoreCase("gzip")) {
                        // This is dangerous. Blocking in the mapping function can wedge the
                        // response. We do support it provided that there enough thread in
                        // the executor.
                        return BodySubscribers.mapping(BodySubscribers.ofInputStream(),
                                (is) -> (() -> {
                                    try {
                                        return new GZIPInputStream(is);
                                    } catch (IOException io) {
                                        throw new UncheckedIOException(io);
                                    }
                                }));
                    } else return BodySubscribers.mapping(BodySubscribers.ofInputStream(),
                            (is) -> (() -> is));
                }
            };
            HttpResponse<Supplier<InputStream>> response = client.send(req, handler);
            String lorem = new String(response.body().get().readAllBytes(), UTF_8);
            if (!LOREM_IPSUM.equals(lorem)) {
                out.println("Response doesn't match");
                out.println("[" + LOREM_IPSUM + "] != [" + lorem + "]");
                assertEquals(LOREM_IPSUM, lorem);
            } else {
                out.println("Received expected response.");
            }
        }
    }

    static final class GZIPBodyHandler implements BodyHandler<InputStream> {
        @Override
        public HttpResponse.BodySubscriber<InputStream> apply(HttpResponse.ResponseInfo responseInfo) {
            String contentType = responseInfo.headers().firstValue("Content-Encoding")
                    .orElse("identity");
            out.println("Content-Encoding: " + contentType);
            if (contentType.equalsIgnoreCase("gzip")) {
                // This is dangerous. Blocking in the mapping function can wedge the
                // response. We do support it provided that there enough thread in
                // the executor.
                return BodySubscribers.mapping(BodySubscribers.ofInputStream(),
                        (is) -> {
                    try {
                        return new GZIPInputStream(is);
                    } catch (IOException io) {
                        throw new UncheckedIOException(io);
                    }
                });
            } else return BodySubscribers.ofInputStream();
        }
    }

    static final class MappingBodyHandler<T,U> implements BodyHandler<U> {
        final BodyHandler<T> upstream;
        final Function<? super T,? extends U> finisher;
        MappingBodyHandler(BodyHandler<T> upstream, Function<T,U> finisher) {
            this.upstream = upstream;
            this.finisher = finisher;
        }
        @Override
        public HttpResponse.BodySubscriber<U> apply(HttpResponse.ResponseInfo responseInfo) {
            return BodySubscribers.mapping(upstream.apply(responseInfo), finisher);
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

        HttpTestHandler plainHandler = new LoremIpsumPlainHandler();
        HttpTestHandler gzipHandler  = new LoremIpsumGZIPHandler();

        // HTTP/1.1
        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(plainHandler, "/http1/chunk/txt");
        httpTestServer.addHandler(gzipHandler,  "/http1/chunk/gz");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/chunk";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(plainHandler, "/https1/chunk/txt");
        httpsTestServer.addHandler(gzipHandler, "/https1/chunk/gz");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/chunk";

        // HTTP/2
        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(plainHandler, "/http2/chunk/txt");
        http2TestServer.addHandler(gzipHandler, "/http2/chunk/gz");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/chunk";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(plainHandler, "/https2/chunk/txt");
        https2TestServer.addHandler(gzipHandler, "/https2/chunk/gz");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/chunk";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        Thread.sleep(100);
        AssertionError fail = TRACKER.check(500);
        try {
            httpTestServer.stop();
            httpsTestServer.stop();
            http2TestServer.stop();
            https2TestServer.stop();
        } finally {
            if (fail != null) {
                throw fail;
            }
        }
    }


    static class LoremIpsumPlainHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try {
                out.println("LoremIpsumPlainHandler received request to " + t.getRequestURI());
                t.getResponseHeaders().addHeader("Content-Encoding", "identity");
                t.sendResponseHeaders(200, -1);
                long size = 0;
                try (OutputStream os = t.getResponseBody()) {
                    for (String s : LOREM_IPSUM.split("\n")) {
                        byte[] buf = s.getBytes(StandardCharsets.UTF_8);
                        System.out.println("Writing " + (buf.length + 1) + " bytes...");
                        os.write(buf);
                        os.write('\n');
                        os.flush();
                        size += buf.length + 1;
                    }
                } finally {
                    System.out.println("Sent " + size + " bytes");
                }
            } catch (IOException | RuntimeException | Error e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    static class LoremIpsumGZIPHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try {
                out.println("LoremIpsumGZIPHandler received request to " + t.getRequestURI());
                t.getResponseHeaders().addHeader("Content-Encoding", "gzip");
                t.sendResponseHeaders(200, -1);
                long size = 0;
                try (GZIPOutputStream os =
                             new GZIPOutputStream(t.getResponseBody())) {
                    for (String s : LOREM_IPSUM.split("\n")) {
                        byte[] buf = s.getBytes(StandardCharsets.UTF_8);
                        System.out.println("Writing and compressing "
                                + (buf.length + 1) + " uncompressed bytes...");
                        os.write(buf);
                        os.write('\n');
                        os.flush();
                        size += buf.length + 1;
                    }
                } finally {
                    System.out.println("Sent and compressed " + size + " uncompressed bytes");
                }
            } catch (IOException | RuntimeException | Error e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

}
