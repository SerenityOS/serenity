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
 * @summary Verify that dependent synchronous actions added before the promise CF
 *          completes are executed either asynchronously in an executor when the
 *          CF later completes, or in the user thread that joins.
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters DependentPromiseActionsTest
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.internal.httpclient.debug=true DependentPromiseActionsTest
 * @run testng/othervm/java.security.policy=dependent.policy
  *           -Djdk.internal.httpclient.debug=true DependentPromiseActionsTest
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.StackWalker.StackFrame;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.err;
import static java.lang.System.out;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DependentPromiseActionsTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
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
                http2URI_fixed,
                http2URI_chunk,
                https2URI_fixed,
                https2URI_chunk,
        };
    }

    enum SubscriberType {EAGER, LAZZY}

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
            BodyHandler<Stream<String>> handler =
                    new StallingBodyHandler((w) -> {},
                            BodyHandlers.ofLines());
            Map<HttpRequest, CompletableFuture<HttpResponse<Stream<String>>>> pushPromises =
                    new ConcurrentHashMap<>();
            PushPromiseHandler<Stream<String>> pushHandler = new PushPromiseHandler<>() {
                @Override
                public void applyPushPromise(HttpRequest initiatingRequest,
                                             HttpRequest pushPromiseRequest,
                                             Function<BodyHandler<Stream<String>>,
                                                     CompletableFuture<HttpResponse<Stream<String>>>>
                                                     acceptor) {
                    pushPromises.putIfAbsent(pushPromiseRequest, acceptor.apply(handler));
                }
            };
            HttpResponse<Stream<String>> response =
                    client.sendAsync(req, BodyHandlers.ofLines(), pushHandler).get();
            String body = response.body().collect(Collectors.joining("|"));
            assertEquals(URI.create(body).getPath(), URI.create(uri).getPath());
            for (HttpRequest promised : pushPromises.keySet()) {
                out.printf("%s Received promise: %s%n\tresponse: %s%n",
                        now(), promised, pushPromises.get(promised).get());
                String promisedBody = pushPromises.get(promised).get().body()
                        .collect(Collectors.joining("|"));
                assertEquals(promisedBody, promised.uri().toASCIIString());
            }
            assertEquals(3, pushPromises.size());
        }
    }

    @Test(dataProvider = "variants")
    public void testAsStringAsync(String uri,
                                  boolean sameClient,
                                  Supplier<Staller> stallers)
            throws Exception
    {
        String test = format("testAsStringAsync(%s, %b, %s)",
                uri, sameClient, stallers);
        testDependent(test, uri, sameClient, BodyHandlers::ofString,
                this::finish, this::extractString, stallers,
                SubscriberType.EAGER);
    }

    @Test(dataProvider = "variants")
    public void testAsLinesAsync(String uri,
                                 boolean sameClient,
                                 Supplier<Staller> stallers)
            throws Exception
    {
        String test = format("testAsLinesAsync(%s, %b, %s)",
                uri, sameClient, stallers);
        testDependent(test, uri, sameClient, BodyHandlers::ofLines,
                this::finish, this::extractStream, stallers,
                SubscriberType.LAZZY);
    }

    @Test(dataProvider = "variants")
    public void testAsInputStreamAsync(String uri,
                                       boolean sameClient,
                                       Supplier<Staller> stallers)
            throws Exception
    {
        String test = format("testAsInputStreamAsync(%s, %b, %s)",
                uri, sameClient, stallers);
        testDependent(test, uri, sameClient, BodyHandlers::ofInputStream,
                this::finish, this::extractInputStream, stallers,
                SubscriberType.LAZZY);
    }

    private <T,U> void testDependent(String name, String uri, boolean sameClient,
                                     Supplier<BodyHandler<T>> handlers,
                                     Finisher finisher,
                                     Extractor<T> extractor,
                                     Supplier<Staller> stallers,
                                     SubscriberType subscriberType)
            throws Exception
    {
        out.printf("%n%s%s%n", now(), name);
        try {
            testDependent(uri, sameClient, handlers, finisher,
                          extractor, stallers, subscriberType);
        } catch (Error | Exception x) {
            FAILURES.putIfAbsent(name, x);
            throw x;
        }
    }

    private <T,U> void testDependent(String uri, boolean sameClient,
                                     Supplier<BodyHandler<T>> handlers,
                                     Finisher finisher,
                                     Extractor<T> extractor,
                                     Supplier<Staller> stallers,
                                     SubscriberType subscriberType)
            throws Exception
    {
        HttpClient client = null;
        for (Where where : EnumSet.of(Where.BODY_HANDLER)) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            HttpRequest req = HttpRequest.
                    newBuilder(URI.create(uri))
                    .build();
            StallingPushPromiseHandler<T> promiseHandler =
                    new StallingPushPromiseHandler<>(where, handlers, stallers);
            BodyHandler<T> handler = handlers.get();
            System.out.println("try stalling in " + where);
            CompletableFuture<HttpResponse<T>> responseCF =
                    client.sendAsync(req, handler, promiseHandler);
            assert subscriberType == SubscriberType.LAZZY || !responseCF.isDone();
            finisher.finish(where, responseCF, promiseHandler, extractor);
        }
    }

    enum Where {
        ON_PUSH_PROMISE, BODY_HANDLER, ON_SUBSCRIBE, ON_NEXT, ON_COMPLETE, ON_ERROR, GET_BODY, BODY_CF;
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

    static final class StallingPushPromiseHandler<T> implements PushPromiseHandler<T> {

        static final class Tuple<U> {
            public final CompletableFuture<HttpResponse<U>> response;
            public final Staller staller;
            public final AtomicReference<RuntimeException> failed;
            Tuple(AtomicReference<RuntimeException> failed,
                  CompletableFuture<HttpResponse<U>> response,
                  Staller staller) {
                this.response = response;
                this.staller = staller;
                this.failed = failed;
            }
        }

        public final ConcurrentMap<HttpRequest, Tuple<T>> promiseMap =
                new ConcurrentHashMap<>();
        private final Supplier<Staller> stallers;
        private final Supplier<BodyHandler<T>> handlers;
        private final Where where;
        private final Thread thread = Thread.currentThread(); // main thread

        StallingPushPromiseHandler(Where where,
                                   Supplier<BodyHandler<T>> handlers,
                                   Supplier<Staller> stallers) {
            this.where = where;
            this.handlers = handlers;
            this.stallers = stallers;
        }

        @Override
        public void applyPushPromise(HttpRequest initiatingRequest,
                                     HttpRequest pushPromiseRequest,
                                     Function<BodyHandler<T>,
                                             CompletableFuture<HttpResponse<T>>> acceptor) {
            AtomicReference<RuntimeException> failed = new AtomicReference<>();
            Staller staller = stallers.get();
            staller.acquire();
            assert staller.willStall();
            try {
                BodyHandler handler = new StallingBodyHandler<>(
                        where.select(staller), handlers.get());
                CompletableFuture<HttpResponse<T>> cf = acceptor.apply(handler);
                Tuple<T> tuple = new Tuple(failed, cf, staller);
                promiseMap.putIfAbsent(pushPromiseRequest, tuple);
                CompletableFuture<?> done = cf.whenComplete(
                        (r, t) -> checkThreadAndStack(thread, failed, r, t));
                assert !cf.isDone();
            } finally {
                staller.release();
            }
        }
    }

    interface Extractor<T> {
        public List<String> extract(HttpResponse<T> resp);
    }

    final List<String> extractString(HttpResponse<String> resp) {
        return List.of(resp.body());
    }

    final List<String> extractStream(HttpResponse<Stream<String>> resp) {
        return resp.body().collect(Collectors.toList());
    }

    final List<String> extractInputStream(HttpResponse<InputStream> resp) {
        try (InputStream is = resp.body()) {
            return new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.toList());
        } catch (IOException x) {
            throw new CompletionException(x);
        }
    }

    interface Finisher<T> {
        public void finish(Where w,
                           CompletableFuture<HttpResponse<T>> cf,
                           StallingPushPromiseHandler<T> ph,
                           Extractor<T> extractor);
    }

    static Optional<StackFrame> findFrame(Stream<StackFrame> s, String name) {
        return s.filter((f) -> f.getClassName().contains(name))
                .filter((f) -> f.getDeclaringClass().getModule().equals(HttpClient.class.getModule()))
                .findFirst();
    }

    static <T> void checkThreadAndStack(Thread thread,
                                        AtomicReference<RuntimeException> failed,
                                        T result,
                                        Throwable error) {
        if (Thread.currentThread() == thread) {
            //failed.set(new RuntimeException("Dependant action was executed in " + thread));
            List<StackFrame> httpStack = WALKER.walk(s -> s.filter(f -> f.getDeclaringClass()
                    .getModule().equals(HttpClient.class.getModule()))
                    .collect(Collectors.toList()));
            if (!httpStack.isEmpty()) {
                System.out.println("Found unexpected trace: ");
                httpStack.forEach(f -> System.out.printf("\t%s%n", f));
                failed.set(new RuntimeException("Dependant action has unexpected frame in " +
                        Thread.currentThread() + ": " + httpStack.get(0)));

            }            return;
        } else if (System.getSecurityManager() != null) {
            Optional<StackFrame> sf = WALKER.walk(s -> findFrame(s, "PrivilegedRunnable"));
            if (!sf.isPresent()) {
                failed.set(new RuntimeException("Dependant action does not have expected frame in "
                        + Thread.currentThread()));
                return;
            } else {
                System.out.println("Found expected frame: " + sf.get());
            }
        } else {
            List<StackFrame> httpStack = WALKER.walk(s -> s.filter(f -> f.getDeclaringClass()
                    .getModule().equals(HttpClient.class.getModule()))
                    .collect(Collectors.toList()));
            if (!httpStack.isEmpty()) {
                System.out.println("Found unexpected trace: ");
                httpStack.forEach(f -> System.out.printf("\t%s%n", f));
                failed.set(new RuntimeException("Dependant action has unexpected frame in " +
                        Thread.currentThread() + ": " + httpStack.get(0)));

            }
        }
    }

    <T> void finish(Where w,
                    StallingPushPromiseHandler.Tuple<T> tuple,
                    Extractor<T> extractor) {
        AtomicReference<RuntimeException> failed = tuple.failed;
        CompletableFuture<HttpResponse<T>> done = tuple.response;
        Staller staller = tuple.staller;
        try {
            HttpResponse<T> response = done.join();
            List<String> result = extractor.extract(response);
            URI uri = response.uri();
            RuntimeException error = failed.get();
            if (error != null) {
                throw new RuntimeException("Test failed in "
                        + w + ": " + uri, error);
            }
            assertEquals(result, List.of(response.request().uri().toASCIIString()));
        } finally {
            staller.reset();
        }
    }

    <T> void finish(Where w,
                    CompletableFuture<HttpResponse<T>> cf,
                    StallingPushPromiseHandler<T> ph,
                    Extractor<T> extractor) {
        HttpResponse<T> response = cf.join();
        List<String> result = extractor.extract(response);
        for (HttpRequest req : ph.promiseMap.keySet()) {
            finish(w, ph.promiseMap.get(req), extractor);
        }
        assertEquals(ph.promiseMap.size(), 3,
                "Expected 3 push promises for " + w + " in "
                        + response.request().uri());
        assertEquals(result, List.of(response.request().uri().toASCIIString()));

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
            sem.acquireUninterruptibly();
        }

        @Override
        public void release() {
            sem.release();
        }

        @Override
        public void acquire() {
            sem.acquireUninterruptibly();
        }

        @Override
        public void reset() {
            sem.drainPermits();
            sem.release();
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

        // HTTP/2
        HttpTestHandler h2_fixedLengthHandler = new HTTP_FixedLengthHandler();
        HttpTestHandler h2_chunkedHandler = new HTTP_ChunkedHandler();

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(h2_fixedLengthHandler, "/http2/fixed");
        http2TestServer.addHandler(h2_chunkedHandler, "/http2/chunk");
        http2URI_fixed = "http://" + http2TestServer.serverAuthority() + "/http2/fixed/y";
        http2URI_chunk = "http://" + http2TestServer.serverAuthority() + "/http2/chunk/y";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(h2_fixedLengthHandler, "/https2/fixed");
        https2TestServer.addHandler(h2_chunkedHandler, "/https2/chunk");
        https2URI_fixed = "https://" + https2TestServer.serverAuthority() + "/https2/fixed/y";
        https2URI_chunk = "https://" + https2TestServer.serverAuthority() + "/https2/chunk/y";

        serverCount.addAndGet(4);
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        sharedClient = null;
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static final BiPredicate<String,String> ACCEPT_ALL = (x, y) -> true;

    private static void pushPromiseFor(HttpTestExchange t,
                                       URI requestURI,
                                       String pushPath,
                                       boolean fixed)
        throws IOException
    {
        try {
            URI promise = new URI(requestURI.getScheme(),
                    requestURI.getAuthority(),
                    pushPath, null, null);
            byte[] promiseBytes = promise.toASCIIString().getBytes(UTF_8);
            out.printf("TestServer: %s Pushing promise: %s%n", now(), promise);
            err.printf("TestServer: %s Pushing promise: %s%n", now(), promise);
            HttpHeaders headers;
            if (fixed) {
                String length = String.valueOf(promiseBytes.length);
                headers = HttpHeaders.of(Map.of("Content-Length", List.of(length)),
                                         ACCEPT_ALL);
            } else {
                headers = HttpHeaders.of(Map.of(), ACCEPT_ALL); // empty
            }
            t.serverPush(promise, headers, promiseBytes);
        } catch (URISyntaxException x) {
            throw new IOException(x.getMessage(), x);
        }
    }

    static class HTTP_FixedLengthHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            out.println("HTTP_FixedLengthHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            URI requestURI = t.getRequestURI();
            for (int i = 1; i<2; i++) {
                String path = requestURI.getPath() + "/before/promise-" + i;
                pushPromiseFor(t, requestURI, path, true);
            }
            byte[] resp = t.getRequestURI().toString().getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(200, resp.length);  //fixed content length
            try (OutputStream os = t.getResponseBody()) {
                int bytes = resp.length/3;
                for (int i = 0; i<2; i++) {
                    String path = requestURI.getPath() + "/after/promise-" + (i + 2);
                    os.write(resp, i * bytes, bytes);
                    os.flush();
                    pushPromiseFor(t, requestURI, path, true);
                }
                os.write(resp, 2*bytes, resp.length - 2*bytes);
            }
        }

    }

    static class HTTP_ChunkedHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            out.println("HTTP_ChunkedHandler received request to " + t.getRequestURI());
            byte[] resp = t.getRequestURI().toString().getBytes(StandardCharsets.UTF_8);
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            URI requestURI = t.getRequestURI();
            for (int i = 1; i<2; i++) {
                String path = requestURI.getPath() + "/before/promise-" + i;
                pushPromiseFor(t, requestURI, path, false);
            }
            t.sendResponseHeaders(200, -1); // chunked/variable
            try (OutputStream os = t.getResponseBody()) {
                int bytes = resp.length/3;
                for (int i = 0; i<2; i++) {
                    String path = requestURI.getPath() + "/after/promise-" + (i + 2);
                    os.write(resp, i * bytes, bytes);
                    os.flush();
                    pushPromiseFor(t, requestURI, path, false);
                }
                os.write(resp, 2*bytes, resp.length - 2*bytes);
            }
        }
    }


}
