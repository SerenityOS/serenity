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
 * @bug 8252374
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters
 *       ReferenceTracker AggregateRequestBodyTest
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.internal.httpclient.debug=true
 *                     -Djdk.httpclient.HttpClient.log=requests,responses,errors
 *                     ISO_8859_1_Test
 * @summary Tests that a client is able to receive ISO-8859-1 encoded header values.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class ISO_8859_1_Test implements HttpServerAdapters {

    SSLContext sslContext;
    DummyServer http1DummyServer;
    HttpServerAdapters.HttpTestServer http1TestServer;   // HTTP/1.1 ( http )
    HttpServerAdapters.HttpTestServer https1TestServer;  // HTTPS/1.1 ( https  )
    HttpServerAdapters.HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpServerAdapters.HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
    String http1Dummy;
    String http1URI;
    String https1URI;
    String http2URI;
    String https2URI;

    static final int RESPONSE_CODE = 200;
    static final int ITERATION_COUNT = 4;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    static final Class<CompletionException> CE = CompletionException.class;
    // a shared executor helps reduce the amount of threads created by the test
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

    final ReferenceTracker TRACKER = ReferenceTracker.INSTANCE;
    private volatile HttpClient sharedClient;

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
                    System.out.printf(now() + "Task %s failed: %s%n", id, t);
                    System.err.printf(now() + "Task %s failed: %s%n", id, t);
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
    static final void printFailedTests(ITestContext context) {
        out.println("\n=========================");
        try {
            var failed = context.getFailedTests().getAllResults().stream()
                    .collect(Collectors.toMap(r -> name(r), ITestResult::getThrowable));
            FAILURES.putAll(failed);

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
                System.out.println("WARNING: Some tasks failed");
            }
        } finally {
            out.println("\n=========================\n");
        }
    }

    private String[] uris() {
        return new String[] {
                http1Dummy,
                http1URI,
                https1URI,
                http2URI,
                https2URI,
        };
    }

    static AtomicLong URICOUNT = new AtomicLong();

    @DataProvider(name = "variants")
    public Object[][] variants(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        String[] uris = uris();
        Object[][] result = new Object[uris.length * 2][];
        int i = 0;
        for (boolean sameClient : List.of(false, true)) {
            for (String uri : uris()) {
                result[i++] = new Object[]{uri, sameClient};
            }
        }
        assert i == uris.length * 2;
        return result;
    }

    private HttpClient makeNewClient() {
        clientCount.incrementAndGet();
        HttpClient client =  HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .executor(executor)
                .sslContext(sslContext)
                .build();
        return TRACKER.track(client);
    }

    HttpClient newHttpClient(boolean share) {
        if (!share) return makeNewClient();
        HttpClient shared = sharedClient;
        if (shared != null) return shared;
        synchronized (this) {
            shared = sharedClient;
            if (shared == null) {
                shared = sharedClient = makeNewClient();
            }
            return shared;
        }
    }

    private static final Exception completionCause(CompletionException x) {
        Throwable c = x;
        while (c  instanceof CompletionException
                || c instanceof ExecutionException) {
            if (c.getCause() == null) break;
            c = c.getCause();
        }
        if (c instanceof Error) throw (Error)c;
        return (Exception)c;
    }

    @Test(dataProvider = "variants")
    public void test(String uri, boolean sameClient) throws Exception {
        checkSkip();
        System.out.println("Request to " + uri);

        HttpClient client = newHttpClient(sameClient);

        List<CompletableFuture<HttpResponse<String>>> cfs = new ArrayList<>();
        for (int i = 0; i < ITERATION_COUNT; i++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri + "/" + i))
                    .build();
            cfs.add(client.sendAsync(request, BodyHandlers.ofString()));
        }
        try {
            CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException x) {
            throw completionCause(x);
        }
        for (CompletableFuture<HttpResponse<String>> cf : cfs) {
            var response = cf.get();
            System.out.println("Got: " + response);
            var value = response.headers().firstValue("Header8859").orElse(null);
            assertEquals(value, "U\u00ffU");
        }
        System.out.println("HttpClient: PASSED");
        if (uri.contains("http1")) {
            System.out.println("Testing with URLConnection");
            var url = URI.create(uri).toURL();
            var conn = url.openConnection();
            conn.connect();
            conn.getInputStream().readAllBytes();
            var value = conn.getHeaderField("Header8859");
            assertEquals(value, "U\u00ffU", "legacy stack failed");
            System.out.println("URLConnection: PASSED");
        }
        System.out.println("test: DONE");
    }

    static final class DummyServer extends Thread implements AutoCloseable {
        String RESP = """
                HTTP/1.1 200 OK\r
                Content-length: 0\r
                Header8859: U\u00ffU\r
                Connection: close\r
                \r
                """;

        static final InetSocketAddress LOOPBACK =
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        final ServerSocket socket;
        final CopyOnWriteArrayList<Socket> accepted = new CopyOnWriteArrayList<Socket>();
        final CompletableFuture<Void> done = new CompletableFuture();
        volatile boolean closed;
        DummyServer() throws IOException  {
            socket = new ServerSocket();
            socket.bind(LOOPBACK);
        }

        public String serverAuthority() {
            String address = socket.getInetAddress().getHostAddress();
            if (address.indexOf(':') >= 0) {
                address = "[" + address + "]";
            }
            return address + ":" + socket.getLocalPort();
        }

        public void run() {
            try {
                while (!socket.isClosed()) {
                    try (Socket client = socket.accept()) {
                        accepted.add(client);
                        try {
                            System.out.println("Accepted: " + client);
                            String req = "";
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(client.getInputStream(),
                                            StandardCharsets.ISO_8859_1));
                            String line = null;
                            while (!(line = reader.readLine()).isEmpty()) {
                                System.out.println("Got line: " + line);
                                req = req + line + "\r\n";
                            }
                            System.out.println(req);
                            System.out.println("Sending back " + RESP);
                            client.getOutputStream().write(RESP.getBytes(StandardCharsets.ISO_8859_1));
                            client.getOutputStream().flush();
                        } finally {
                            accepted.remove(client);
                        }
                    }
                }
            } catch (Throwable t) {
                if (closed) {
                    done.complete(null);
                } else {
                    done.completeExceptionally(t);
                }
            } finally {
                done.complete(null);
            }
        }

        final void close(AutoCloseable toclose) {
            try { toclose.close(); } catch (Exception x) {};
        }

        final public void close() {
            closed = true;
            close(socket);
            accepted.forEach(this::close);
        }
    }

    final static class ISO88591Handler implements HttpServerAdapters.HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try (HttpTestExchange e = t) {
                t.getRequestBody().readAllBytes();
                t.getResponseHeaders().addHeader("Header8859", "U\u00ffU");
                t.sendResponseHeaders(200, 0);
            }

        }
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        HttpServerAdapters.HttpTestHandler handler = new ISO88591Handler();
        InetSocketAddress loopback = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        http1DummyServer = new DummyServer();
        http1Dummy = "http://" + http1DummyServer.serverAuthority() +"/http1/dummy/x";

        HttpServer http1 = HttpServer.create(loopback, 0);
        http1TestServer = HttpServerAdapters.HttpTestServer.of(http1);
        http1TestServer.addHandler(handler, "/http1/server/");
        http1URI = "http://" + http1TestServer.serverAuthority() + "/http1/server/x";

        HttpsServer https1 = HttpsServer.create(loopback, 0);
        https1.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        https1TestServer = HttpServerAdapters.HttpTestServer.of(https1);
        https1TestServer.addHandler(handler, "/https1/server/");
        https1URI = "https://" + https1TestServer.serverAuthority() + "/https1/server/x";

        // HTTP/2
        http2TestServer = HttpServerAdapters.HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(handler, "/http2/server/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/server/x";

        https2TestServer = HttpServerAdapters.HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(handler, "/https2/server/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/server/x";

        serverCount.addAndGet(5);
        http1TestServer.start();
        https1TestServer.start();
        http2TestServer.start();
        https2TestServer.start();
        http1DummyServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        String sharedClientName =
                sharedClient == null ? null : sharedClient.toString();
        sharedClient = null;
        Thread.sleep(100);
        AssertionError fail = TRACKER.check(500);
        try {
            http1TestServer.stop();
            https1TestServer.stop();
            http2TestServer.stop();
            https2TestServer.stop();
            http1DummyServer.close();
        } finally {
            if (fail != null) {
                if (sharedClientName != null) {
                    System.err.println("Shared client name is: " + sharedClientName);
                }
                throw fail;
            }
        }
    }
}
