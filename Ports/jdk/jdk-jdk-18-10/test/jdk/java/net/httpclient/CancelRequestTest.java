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
 * @bug 8245462 8229822
 * @summary Tests cancelling the request.
 * @library /test/lib http2/server
 * @key randomness
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters
 *        ReferenceTracker CancelRequestTest
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.internal.httpclient.debug=true
 *                     -Djdk.httpclient.enableAllMethodRetry=true
 *                     CancelRequestTest
 */
// *                     -Dseed=3582896013206826205L
// *                     -Dseed=5784221742235559231L
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.RandomFactory;
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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.arraycopy;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CancelRequestTest implements HttpServerAdapters {

    private static final Random random = RandomFactory.getRandom();

    SSLContext sslContext;
    HttpTestServer httpTestServer;    // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;   // HTTPS/1.1
    HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    static final long SERVER_LATENCY = 75;
    static final int MAX_CLIENT_DELAY = 75;
    static final int ITERATION_COUNT = 3;
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
        var failed = context.getFailedTests().getAllResults().stream()
                .collect(Collectors.toMap(r -> name(r), ITestResult::getThrowable));
        FAILURES.putAll(failed);
        try {
            out.printf("%n%sCreated %d servers and %d clients%n",
                    now(), serverCount.get(), clientCount.get());
            if (FAILURES.isEmpty()) return;
            out.println("Failed tests: ");
            FAILURES.entrySet().forEach((e) -> {
                out.printf("\t%s: %s%n", e.getKey(), e.getValue());
                e.getValue().printStackTrace(out);
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
                httpURI,
                httpsURI,
                http2URI,
                https2URI,
        };
    }

    @DataProvider(name = "asyncurls")
    public Object[][] asyncurls() {
        String[] uris = uris();
        Object[][] result = new Object[uris.length * 2 * 3][];
        //Object[][] result = new Object[uris.length][];
        int i = 0;
        for (boolean mayInterrupt : List.of(true, false, true)) {
            for (boolean sameClient : List.of(false, true)) {
                //if (!sameClient) continue;
                for (String uri : uris()) {
                    String path = sameClient ? "same" : "new";
                    path = path + (mayInterrupt ? "/interrupt" : "/nointerrupt");
                    result[i++] = new Object[]{uri + path, sameClient, mayInterrupt};
                }
            }
        }
        assert i == uris.length * 2 * 3;
        // assert i == uris.length ;
        return result;
    }

    @DataProvider(name = "urls")
    public Object[][] alltests() {
        String[] uris = uris();
        Object[][] result = new Object[uris.length * 2][];
        //Object[][] result = new Object[uris.length][];
        int i = 0;
        for (boolean sameClient : List.of(false, true)) {
            //if (!sameClient) continue;
            for (String uri : uris()) {
                String path = sameClient ? "same" : "new";
                path = path + "/interruptThread";
                result[i++] = new Object[]{uri + path, sameClient};
            }
        }
        assert i == uris.length * 2;
        // assert i == uris.length ;
        return result;
    }

    private HttpClient makeNewClient() {
        clientCount.incrementAndGet();
        return TRACKER.track(HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .executor(executor)
                .sslContext(sslContext)
                .build());
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

    final static String BODY = "Some string | that ? can | be split ? several | ways.";

    // should accept SSLHandshakeException because of the connectionAborter
    // with http/2 and should accept Stream 5 cancelled.
    //  => also examine in what measure we should always
    //     rewrap in "Request Cancelled" when the multi exchange was aborted...
    private static boolean isCancelled(Throwable t) {
        while (t instanceof ExecutionException) t = t.getCause();
        if (t instanceof CancellationException) return true;
        if (t instanceof IOException) return String.valueOf(t).contains("Request cancelled");
        out.println("Not a cancellation exception: " + t);
        t.printStackTrace(out);
        return false;
    }

    private static void delay() {
        int delay = random.nextInt(MAX_CLIENT_DELAY);
        try {
            System.out.println("client delay: " + delay);
            Thread.sleep(delay);
        } catch (InterruptedException x) {
            out.println("Unexpected exception: " + x);
        }
    }

    @Test(dataProvider = "asyncurls")
    public void testGetSendAsync(String uri, boolean sameClient, boolean mayInterruptIfRunning)
            throws Exception {
        checkSkip();
        HttpClient client = null;
        uri = uri + "/get";
        out.printf("%n%s testGetSendAsync(%s, %b, %b)%n", now(), uri, sameClient, mayInterruptIfRunning);
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                    .GET()
                    .build();
            BodyHandler<String> handler = BodyHandlers.ofString();
            CountDownLatch latch = new CountDownLatch(1);
            CompletableFuture<HttpResponse<String>> response = client.sendAsync(req, handler);
            var cf1 = response.whenComplete((r,t) -> System.out.println(t));
            CompletableFuture<HttpResponse<String>> cf2 = cf1.whenComplete((r,t) -> latch.countDown());
            out.println("response: " + response);
            out.println("cf1: " + cf1);
            out.println("cf2: " + cf2);
            delay();
            cf1.cancel(mayInterruptIfRunning);
            out.println("response after cancel: " + response);
            out.println("cf1 after cancel: " + cf1);
            out.println("cf2 after cancel: " + cf2);
            try {
                String body = cf2.get().body();
                assertEquals(body, Stream.of(BODY.split("\\|")).collect(Collectors.joining()));
                throw new AssertionError("Expected CancellationException not received");
            } catch (ExecutionException x) {
                out.println("Got expected exception: " + x);
                assertTrue(isCancelled(x));
            }

            // Cancelling the request may cause an IOException instead...
            boolean hasCancellationException = false;
            try {
                cf1.get();
            } catch (CancellationException | ExecutionException x) {
                out.println("Got expected exception: " + x);
                assertTrue(isCancelled(x));
                hasCancellationException = x instanceof CancellationException;
            }

            // because it's cf1 that was cancelled then response might not have
            // completed yet - so wait for it here...
            try {
                String body = response.get().body();
                assertEquals(body, Stream.of(BODY.split("\\|")).collect(Collectors.joining()));
                if (mayInterruptIfRunning) {
                    // well actually - this could happen... In which case we'll need to
                    // increase the latency in the server handler...
                    throw new AssertionError("Expected Exception not received");
                }
            } catch (ExecutionException x) {
                assertEquals(response.isDone(), true);
                Throwable wrapped = x.getCause();
                assertTrue(CancellationException.class.isAssignableFrom(wrapped.getClass()));
                Throwable cause = wrapped.getCause();
                out.println("CancellationException cause: " + x);
                assertTrue(IOException.class.isAssignableFrom(cause.getClass()));
                if (cause instanceof HttpConnectTimeoutException) {
                    cause.printStackTrace(out);
                    throw new RuntimeException("Unexpected timeout exception", cause);
                }
                if (mayInterruptIfRunning) {
                    out.println("Got expected exception: " + wrapped);
                    out.println("\tcause: " + cause);
                } else {
                    out.println("Unexpected exception: " + wrapped);
                    wrapped.printStackTrace(out);
                    throw x;
                }
            }

            assertEquals(response.isDone(), true);
            assertEquals(response.isCancelled(), false);
            assertEquals(cf1.isCancelled(), hasCancellationException);
            assertEquals(cf2.isDone(), true);
            assertEquals(cf2.isCancelled(), false);
            assertEquals(latch.getCount(), 0);
        }
    }

    @Test(dataProvider = "asyncurls")
    public void testPostSendAsync(String uri, boolean sameClient, boolean mayInterruptIfRunning)
            throws Exception {
        checkSkip();
        uri = uri + "/post";
        HttpClient client = null;
        out.printf("%n%s testPostSendAsync(%s, %b, %b)%n", now(), uri, sameClient, mayInterruptIfRunning);
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            CompletableFuture<CompletableFuture<?>> cancelFuture = new CompletableFuture<>();

            Iterable<byte[]> iterable = new Iterable<byte[]>() {
                @Override
                public Iterator<byte[]> iterator() {
                    // this is dangerous
                    out.println("waiting for completion on: " + cancelFuture);
                    boolean async = random.nextBoolean();
                    Runnable cancel = () -> {
                        out.println("Cancelling from " + Thread.currentThread());
                        var cf1 = cancelFuture.join();
                        cf1.cancel(mayInterruptIfRunning);
                        out.println("cancelled " + cf1);
                    };
                    if (async) executor.execute(cancel);
                    else cancel.run();
                    return List.of(BODY.getBytes(UTF_8)).iterator();
                }
            };

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(iterable))
                    .build();
            BodyHandler<String> handler = BodyHandlers.ofString();
            CountDownLatch latch = new CountDownLatch(1);
            CompletableFuture<HttpResponse<String>> response = client.sendAsync(req, handler);
            var cf1 = response.whenComplete((r,t) -> System.out.println(t));
            CompletableFuture<HttpResponse<String>> cf2 = cf1.whenComplete((r,t) -> latch.countDown());
            out.println("response: " + response);
            out.println("cf1: " + cf1);
            out.println("cf2: " + cf2);
            cancelFuture.complete(cf1);
            out.println("response after cancel: " + response);
            out.println("cf1 after cancel: " + cf1);
            out.println("cf2 after cancel: " + cf2);
            try {
                String body = cf2.get().body();
                assertEquals(body, Stream.of(BODY.split("\\|")).collect(Collectors.joining()));
                throw new AssertionError("Expected CancellationException not received");
            } catch (ExecutionException x) {
                out.println("Got expected exception: " + x);
                assertTrue(isCancelled(x));
            }

            // Cancelling the request may cause an IOException instead...
            boolean hasCancellationException = false;
            try {
                cf1.get();
            } catch (CancellationException | ExecutionException x) {
                out.println("Got expected exception: " + x);
                assertTrue(isCancelled(x));
                hasCancellationException = x instanceof CancellationException;
            }

            // because it's cf1 that was cancelled then response might not have
            // completed yet - so wait for it here...
            try {
                String body = response.get().body();
                assertEquals(body, Stream.of(BODY.split("\\|")).collect(Collectors.joining()));
                if (mayInterruptIfRunning) {
                    // well actually - this could happen... In which case we'll need to
                    // increase the latency in the server handler...
                    throw new AssertionError("Expected Exception not received");
                }
            } catch (ExecutionException x) {
                assertEquals(response.isDone(), true);
                Throwable wrapped = x.getCause();
                assertTrue(CancellationException.class.isAssignableFrom(wrapped.getClass()));
                Throwable cause = wrapped.getCause();
                assertTrue(IOException.class.isAssignableFrom(cause.getClass()));
                if (cause instanceof HttpConnectTimeoutException) {
                    cause.printStackTrace(out);
                    throw new RuntimeException("Unexpected timeout exception", cause);
                }
                if (mayInterruptIfRunning) {
                    out.println("Got expected exception: " + wrapped);
                    out.println("\tcause: " + cause);
                } else {
                    out.println("Unexpected exception: " + wrapped);
                    wrapped.printStackTrace(out);
                    throw x;
                }
            }

            assertEquals(response.isDone(), true);
            assertEquals(response.isCancelled(), false);
            assertEquals(cf1.isCancelled(), hasCancellationException);
            assertEquals(cf2.isDone(), true);
            assertEquals(cf2.isCancelled(), false);
            assertEquals(latch.getCount(), 0);
        }
    }

    @Test(dataProvider = "urls")
    public void testPostInterrupt(String uri, boolean sameClient)
            throws Exception {
        checkSkip();
        HttpClient client = null;
        out.printf("%n%s testPostInterrupt(%s, %b)%n", now(), uri, sameClient);
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);
            Thread main = Thread.currentThread();
            CompletableFuture<Thread> interruptingThread = new CompletableFuture<>();
            Runnable interrupt = () -> {
                Thread current = Thread.currentThread();
                out.printf("%s Interrupting main from: %s (%s)", now(), current, uri);
                interruptingThread.complete(current);
                main.interrupt();
            };
            Iterable<byte[]> iterable = () -> {
                var async = random.nextBoolean();
                if (async) executor.execute(interrupt);
                else interrupt.run();
                return List.of(BODY.getBytes(UTF_8)).iterator();
            };

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(iterable))
                    .build();
            String body = null;
            Exception failed = null;
            try {
                body = client.send(req, BodyHandlers.ofString()).body();
            } catch (Exception x) {
                failed = x;
            }

            if (failed instanceof InterruptedException) {
                out.println("Got expected exception: " + failed);
            } else if (failed instanceof IOException) {
                // that could be OK if the main thread was interrupted
                // from the main thread: the interrupt status could have
                // been caught by writing to the socket from the main
                // thread.
                if (interruptingThread.get() == main) {
                    out.println("Accepting IOException: " + failed);
                    failed.printStackTrace(out);
                } else {
                    throw failed;
                }
            } else if (failed != null) {
                assertEquals(body, Stream.of(BODY.split("\\|")).collect(Collectors.joining()));
                throw failed;
            }
        }
    }



    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        // HTTP/1.1
        HttpTestHandler h1_chunkHandler = new HTTPSlowHandler();
        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(h1_chunkHandler, "/http1/x/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/x/";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(h1_chunkHandler, "/https1/x/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/x/";

        // HTTP/2
        HttpTestHandler h2_chunkedHandler = new HTTPSlowHandler();

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(h2_chunkedHandler, "/http2/x/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/x/";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(h2_chunkedHandler, "/https2/x/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/x/";

        serverCount.addAndGet(4);
        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        String sharedClientName =
                sharedClient == null ? null : sharedClient.toString();
        sharedClient = null;
        Thread.sleep(100);
        AssertionError fail = TRACKER.check(500);
        try {
            httpTestServer.stop();
            httpsTestServer.stop();
            http2TestServer.stop();
            https2TestServer.stop();
        } finally {
            if (fail != null) {
                if (sharedClientName != null) {
                    System.err.println("Shared client name is: " + sharedClientName);
                }
                throw fail;
            }
        }
    }

    private static boolean isThreadInterrupt(HttpTestExchange t) {
        return t.getRequestURI().getPath().contains("/interruptThread");
    }

    /**
     * A handler that slowly sends back a body to give time for the
     * the request to get cancelled before the body is fully received.
     */
    static class HTTPSlowHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try {
                out.println("HTTPSlowHandler received request to " + t.getRequestURI());
                System.err.println("HTTPSlowHandler received request to " + t.getRequestURI());

                boolean isThreadInterrupt = isThreadInterrupt(t);
                byte[] req;
                try (InputStream is = t.getRequestBody()) {
                    req = is.readAllBytes();
                }
                t.sendResponseHeaders(200, -1); // chunked/variable
                try (OutputStream os = t.getResponseBody()) {
                    // lets split the response in several chunks...
                    String msg = (req != null && req.length != 0)
                            ? new String(req, UTF_8)
                            : BODY;
                    String[] str = msg.split("\\|");
                    for (var s : str) {
                        req = s.getBytes(UTF_8);
                        os.write(req);
                        os.flush();
                        try {
                            Thread.sleep(SERVER_LATENCY);
                        } catch (InterruptedException x) {
                            // OK
                        }
                        out.printf("Server wrote %d bytes%n", req.length);
                    }
                }
            } catch (Throwable e) {
                out.println("HTTPSlowHandler: unexpected exception: " + e);
                e.printStackTrace();
                throw e;
            } finally {
                out.printf("HTTPSlowHandler reply sent: %s%n", t.getRequestURI());
                System.err.printf("HTTPSlowHandler reply sent: %s%n", t.getRequestURI());
            }
        }
    }

}
