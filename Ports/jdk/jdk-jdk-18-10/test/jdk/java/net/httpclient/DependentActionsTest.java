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
 * @summary Verify that dependent synchronous actions added before the CF
 *          completes are executed either asynchronously in an executor when the
 *          CF later completes, or in the user thread that joins.
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters DependentActionsTest
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.internal.httpclient.debug=true DependentActionsTest
 * @run testng/othervm/java.security.policy=dependent.policy
  *        -Djdk.internal.httpclient.debug=true DependentActionsTest
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.StackWalker.StackFrame;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.AfterClass;
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
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.out;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DependentActionsTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;    // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;   // HTTPS/1.1
    HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI_fixed;
    String httpURI_chunk;
    String httpsURI_fixed;
    String httpsURI_chunk;
    String http2URI_fixed;
    String http2URI_chunk;
    String https2URI_fixed;
    String https2URI_chunk;

    static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    static final int ITERATION_COUNT = 1;
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

    @AfterClass
    static final void printFailedTests() {
        out.println("\n=========================");
        try {
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
                httpURI_fixed,
                httpURI_chunk,
                httpsURI_fixed,
                httpsURI_chunk,
                http2URI_fixed,
                http2URI_chunk,
                https2URI_fixed,
                https2URI_chunk,
        };
    }

    static final class SemaphoreStallerSupplier
            implements Supplier<SemaphoreStaller> {
        @Override
        public SemaphoreStaller get() {
            return new SemaphoreStaller();
        }
        @Override
        public String toString() {
            return "SemaphoreStaller";
        }
    }

    @DataProvider(name = "noStalls")
    public Object[][] noThrows() {
        String[] uris = uris();
        Object[][] result = new Object[uris.length * 2][];
        int i = 0;
        for (boolean sameClient : List.of(false, true)) {
            for (String uri: uris()) {
                result[i++] = new Object[] {uri, sameClient};
            }
        }
        assert i == uris.length * 2;
        return result;
    }

    @DataProvider(name = "variants")
    public Object[][] variants() {
        String[] uris = uris();
        Object[][] result = new Object[uris.length * 2][];
        int i = 0;
        Supplier<? extends Staller> s = new SemaphoreStallerSupplier();
        for (Supplier<? extends Staller> staller : List.of(s)) {
            for (boolean sameClient : List.of(false, true)) {
                for (String uri : uris()) {
                    result[i++] = new Object[]{uri, sameClient, staller};
                }
            }
        }
        assert i == uris.length * 2;
        return result;
    }

    private HttpClient makeNewClient() {
        clientCount.incrementAndGet();
        return HttpClient.newBuilder()
                .executor(executor)
                .sslContext(sslContext)
                .build();
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

    @Test(dataProvider = "noStalls")
    public void testNoStalls(String uri, boolean sameClient)
            throws Exception {
        HttpClient client = null;
        out.printf("%ntestNoStalls(%s, %b)%n", uri, sameClient);
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                    .build();
            BodyHandler<String> handler =
                    new StallingBodyHandler((w) -> {},
                            BodyHandlers.ofString());
            HttpResponse<String> response = client.send(req, handler);
            String body = response.body();
            assertEquals(URI.create(body).getPath(), URI.create(uri).getPath());
        }
    }

    @Test(dataProvider = "variants")
    public void testAsStringAsync(String uri,
                                  boolean sameClient,
                                  Supplier<Staller> s)
            throws Exception
    {
        Staller staller = s.get();
        String test = format("testAsStringAsync(%s, %b, %s)",
                uri, sameClient, staller);
        testDependent(test, uri, sameClient, BodyHandlers::ofString,
                this::finish, this::extractString, staller);
    }

    @Test(dataProvider = "variants")
    public void testAsLinesAsync(String uri,
                                 boolean sameClient,
                                 Supplier<Staller> s)
            throws Exception
    {
        Staller staller = s.get();
        String test = format("testAsLinesAsync(%s, %b, %s)",
                uri, sameClient, staller);
        testDependent(test, uri, sameClient, BodyHandlers::ofLines,
                this::finish, this::extractStream, staller);
    }

    @Test(dataProvider = "variants")
    public void testAsInputStreamAsync(String uri,
                                       boolean sameClient,
                                       Supplier<Staller> s)
            throws Exception
    {
        Staller staller = s.get();
        String test = format("testAsInputStreamAsync(%s, %b, %s)",
                uri, sameClient, staller);
        testDependent(test, uri, sameClient, BodyHandlers::ofInputStream,
                this::finish, this::extractInputStream, staller);
    }

    private <T,U> void testDependent(String name, String uri, boolean sameClient,
                                     Supplier<BodyHandler<T>> handlers,
                                     Finisher finisher,
                                     Extractor extractor,
                                     Staller staller)
            throws Exception
    {
        out.printf("%n%s%s%n", now(), name);
        try {
            testDependent(uri, sameClient, handlers, finisher, extractor, staller);
        } catch (Error | Exception x) {
            FAILURES.putIfAbsent(name, x);
            throw x;
        }
    }

    private <T,U> void testDependent(String uri, boolean sameClient,
                                     Supplier<BodyHandler<T>> handlers,
                                     Finisher finisher,
                                     Extractor extractor,
                                     Staller staller)
            throws Exception
    {
        HttpClient client = null;
        for (Where where : EnumSet.of(Where.BODY_HANDLER)) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            HttpRequest req = HttpRequest.
                    newBuilder(URI.create(uri))
                    .build();
            BodyHandler<T> handler =
                    new StallingBodyHandler(where.select(staller), handlers.get());
            System.out.println("try stalling in " + where);
            staller.acquire();
            assert staller.willStall();
            CompletableFuture<HttpResponse<T>> responseCF = client.sendAsync(req, handler);
            assert !responseCF.isDone();
            finisher.finish(where, responseCF, staller, extractor);
        }
    }

    enum Where {
        BODY_HANDLER, ON_SUBSCRIBE, ON_NEXT, ON_COMPLETE, ON_ERROR, GET_BODY, BODY_CF;
        public Consumer<Where> select(Consumer<Where> consumer) {
            return new Consumer<Where>() {
                @Override
                public void accept(Where where) {
                    if (Where.this == where) {
                        consumer.accept(where);
                    }
                }
            };
        }
    }

    interface Extractor<T> {
        public List<String> extract(HttpResponse<T> resp);
    }

    final List<String> extractString(HttpResponse<String> resp) {
        return List.of(resp.body());
    }

    final List<String> extractStream(HttpResponse<Stream<String>> resp) {
        return resp.body().collect(toList());
    }

    final List<String> extractInputStream(HttpResponse<InputStream> resp) {
        try (InputStream is = resp.body()) {
            return new BufferedReader(new InputStreamReader(is))
                    .lines().collect(toList());
        } catch (IOException x) {
            throw new CompletionException(x);
        }
    }

    interface Finisher<T> {
        public void finish(Where w,
                           CompletableFuture<HttpResponse<T>> cf,
                           Staller staller,
                           Extractor extractor);
    }

    Optional<StackFrame> findFrame(Stream<StackFrame> s, String name) {
        return s.filter((f) -> f.getClassName().contains(name))
                .filter((f) -> f.getDeclaringClass().getModule().equals(HttpClient.class.getModule()))
                .findFirst();
    }

    static final Predicate<StackFrame> DAT = sfe ->
            sfe.getClassName().startsWith("DependentActionsTest");
    static final Predicate<StackFrame> JUC = sfe ->
            sfe.getClassName().startsWith("java.util.concurrent");
    static final Predicate<StackFrame> JLT = sfe ->
            sfe.getClassName().startsWith("java.lang.Thread");
    static final Predicate<StackFrame> NotDATorJUCorJLT = Predicate.not(DAT.or(JUC).or(JLT));


    <T> void checkThreadAndStack(Thread thread,
                                 AtomicReference<RuntimeException> failed,
                                 T result,
                                 Throwable error) {
        //failed.set(new RuntimeException("Dependant action was executed in " + thread));
        List<StackFrame> otherFrames = WALKER.walk(s -> s.filter(NotDATorJUCorJLT).collect(toList()));
        if (!otherFrames.isEmpty()) {
            System.out.println("Found unexpected trace: ");
            otherFrames.forEach(f -> System.out.printf("\t%s%n", f));
            failed.set(new RuntimeException("Dependant action has unexpected frame in " +
                       Thread.currentThread() + ": " + otherFrames.get(0)));

        }
    }

    <T> void finish(Where w, CompletableFuture<HttpResponse<T>> cf,
                    Staller staller,
                    Extractor<T> extractor) {
        Thread thread = Thread.currentThread();
        AtomicReference<RuntimeException> failed = new AtomicReference<>();
        CompletableFuture<HttpResponse<T>> done = cf.whenComplete(
                (r,t) -> checkThreadAndStack(thread, failed, r, t));
        assert !cf.isDone();
        try {
            Thread.sleep(100);
        } catch (Throwable t) {/* don't care */}
        assert !cf.isDone();
        staller.release();
        try {
            HttpResponse<T> response = done.join();
            List<String> result = extractor.extract(response);
            RuntimeException error = failed.get();
            if (error != null) {
                throw new RuntimeException("Test failed in "
                        + w + ": " + response, error);
            }
            assertEquals(result, List.of(response.request().uri().getPath()));
        } finally {
            staller.reset();
        }
    }

    interface Staller extends Consumer<Where> {
        void release();
        void acquire();
        void reset();
        boolean willStall();
    }

    static final class SemaphoreStaller implements Staller {
        final Semaphore sem = new Semaphore(1);
        @Override
        public void accept(Where where) {
            System.out.println("Acquiring semaphore in "
                    + where + " permits=" + sem.availablePermits());
            sem.acquireUninterruptibly();
            System.out.println("Semaphored acquired in " + where);
        }

        @Override
        public void release() {
            System.out.println("Releasing semaphore: permits="
                    + sem.availablePermits());
            sem.release();
        }

        @Override
        public void acquire() {
            sem.acquireUninterruptibly();
            System.out.println("Semaphored acquired");
        }

        @Override
        public void reset() {
            System.out.println("Reseting semaphore: permits="
                    + sem.availablePermits());
            sem.drainPermits();
            sem.release();
            System.out.println("Semaphore reset: permits="
                    + sem.availablePermits());
        }

        @Override
        public boolean willStall() {
            return sem.availablePermits() <= 0;
        }

        @Override
        public String toString() {
            return "SemaphoreStaller";
        }
    }

    static final class StallingBodyHandler<T> implements BodyHandler<T> {
        final Consumer<Where> stalling;
        final BodyHandler<T> bodyHandler;
        StallingBodyHandler(Consumer<Where> stalling, BodyHandler<T> bodyHandler) {
            this.stalling = stalling;
            this.bodyHandler = bodyHandler;
        }
        @Override
        public BodySubscriber<T> apply(HttpResponse.ResponseInfo rinfo) {
            stalling.accept(Where.BODY_HANDLER);
            BodySubscriber<T> subscriber = bodyHandler.apply(rinfo);
            return new StallingBodySubscriber(stalling, subscriber);
        }
    }

    static final class StallingBodySubscriber<T> implements BodySubscriber<T> {
        private final BodySubscriber<T> subscriber;
        volatile boolean onSubscribeCalled;
        final Consumer<Where> stalling;
        StallingBodySubscriber(Consumer<Where> stalling, BodySubscriber<T> subscriber) {
            this.stalling = stalling;
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            //out.println("onSubscribe ");
            onSubscribeCalled = true;
            stalling.accept(Where.ON_SUBSCRIBE);
            subscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            // out.println("onNext " + item);
            assertTrue(onSubscribeCalled);
            stalling.accept(Where.ON_NEXT);
            subscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            //out.println("onError");
            assertTrue(onSubscribeCalled);
            stalling.accept(Where.ON_ERROR);
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            //out.println("onComplete");
            assertTrue(onSubscribeCalled, "onComplete called before onSubscribe");
            stalling.accept(Where.ON_COMPLETE);
            subscriber.onComplete();
        }

        @Override
        public CompletionStage<T> getBody() {
            stalling.accept(Where.GET_BODY);
            try {
                stalling.accept(Where.BODY_CF);
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
            return subscriber.getBody();
        }
    }


    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        // HTTP/1.1
        HttpTestHandler h1_fixedLengthHandler = new HTTP_FixedLengthHandler();
        HttpTestHandler h1_chunkHandler = new HTTP_ChunkedHandler();
        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(h1_fixedLengthHandler, "/http1/fixed");
        httpTestServer.addHandler(h1_chunkHandler, "/http1/chunk");
        httpURI_fixed = "http://" + httpTestServer.serverAuthority() + "/http1/fixed/x";
        httpURI_chunk = "http://" + httpTestServer.serverAuthority() + "/http1/chunk/x";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(h1_fixedLengthHandler, "/https1/fixed");
        httpsTestServer.addHandler(h1_chunkHandler, "/https1/chunk");
        httpsURI_fixed = "https://" + httpsTestServer.serverAuthority() + "/https1/fixed/x";
        httpsURI_chunk = "https://" + httpsTestServer.serverAuthority() + "/https1/chunk/x";

        // HTTP/2
        HttpTestHandler h2_fixedLengthHandler = new HTTP_FixedLengthHandler();
        HttpTestHandler h2_chunkedHandler = new HTTP_ChunkedHandler();

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(h2_fixedLengthHandler, "/http2/fixed");
        http2TestServer.addHandler(h2_chunkedHandler, "/http2/chunk");
        http2URI_fixed = "http://" + http2TestServer.serverAuthority() + "/http2/fixed/x";
        http2URI_chunk = "http://" + http2TestServer.serverAuthority() + "/http2/chunk/x";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(h2_fixedLengthHandler, "/https2/fixed");
        https2TestServer.addHandler(h2_chunkedHandler, "/https2/chunk");
        https2URI_fixed = "https://" + https2TestServer.serverAuthority() + "/https2/fixed/x";
        https2URI_chunk = "https://" + https2TestServer.serverAuthority() + "/https2/chunk/x";

        serverCount.addAndGet(4);
        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        sharedClient = null;
        httpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static class HTTP_FixedLengthHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            out.println("HTTP_FixedLengthHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            byte[] resp = t.getRequestURI().getPath().getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(200, resp.length);  //fixed content length
            try (OutputStream os = t.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    static class HTTP_ChunkedHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            out.println("HTTP_ChunkedHandler received request to " + t.getRequestURI());
            byte[] resp = t.getRequestURI().getPath().toString().getBytes(StandardCharsets.UTF_8);
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            t.sendResponseHeaders(200, -1); // chunked/variable
            try (OutputStream os = t.getResponseBody()) {
                os.write(resp);
            }
        }
    }
}
