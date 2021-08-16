/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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


/**
 * This is not a test. Actual tests are implemented by concrete subclasses.
 * The abstract class AbstractThrowingPushPromises provides a base framework
 * to test what happens when push promise handlers and their
 * response body handlers and subscribers throw unexpected exceptions.
 * Concrete tests that extend this abstract class will need to include
 * the following jtreg tags:
 *
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters
 *        ReferenceTracker AbstractThrowingPushPromises
 *        <concrete-class-name>
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm -Djdk.internal.httpclient.debug=true <concrete-class-name>
 */

import jdk.test.lib.net.SimpleSSLContext;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.out;
import static java.lang.System.err;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public abstract class AbstractThrowingPushPromises implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
    String http2URI_fixed;
    String http2URI_chunk;
    String https2URI_fixed;
    String https2URI_chunk;

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
    static final void printFailedTests(ITestContext context) {
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

    private String[] uris() {
        return new String[] {
                http2URI_fixed,
                http2URI_chunk,
                https2URI_fixed,
                https2URI_chunk,
        };
    }

    @DataProvider(name = "sanity")
    public Object[][] sanity() {
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

    enum Where {
        BODY_HANDLER, ON_SUBSCRIBE, ON_NEXT, ON_COMPLETE, ON_ERROR, GET_BODY, BODY_CF,
        BEFORE_ACCEPTING, AFTER_ACCEPTING;
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

    private Object[][] variants(List<Thrower> throwers) {
        String[] uris = uris();
        // reduce traces by always using the same client if
        // stopAfterFirstFailure is requested.
        List<Boolean> sameClients = stopAfterFirstFailure()
                ? List.of(true)
                : List.of(false, true);
        Object[][] result = new Object[uris.length * sameClients.size() * throwers.size()][];
        int i = 0;
        for (Thrower thrower : throwers) {
            for (boolean sameClient : sameClients) {
                for (String uri : uris()) {
                    result[i++] = new Object[]{uri, sameClient, thrower};
                }
            }
        }
        assert i == uris.length * sameClients.size() * throwers.size();
        return result;
    }

    @DataProvider(name = "ioVariants")
    public Object[][] ioVariants(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return variants(List.of(
                new UncheckedIOExceptionThrower()));
    }

    @DataProvider(name = "customVariants")
    public Object[][] customVariants(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return variants(List.of(
                new UncheckedCustomExceptionThrower()));
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

    // @Test(dataProvider = "sanity")
    protected void testSanityImpl(String uri, boolean sameClient)
            throws Exception {
        HttpClient client = null;
        out.printf("%ntestNoThrows(%s, %b)%n", uri, sameClient);
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                    .build();
            BodyHandler<Stream<String>> handler =
                    new ThrowingBodyHandler((w) -> {},
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

    // @Test(dataProvider = "variants")
    protected void testThrowingAsStringImpl(String uri,
                                     boolean sameClient,
                                     Thrower thrower)
            throws Exception
    {
        String test = format("testThrowingAsString(%s, %b, %s)",
                             uri, sameClient, thrower);
        testThrowing(test, uri, sameClient, BodyHandlers::ofString,
                this::checkAsString, thrower);
    }

    //@Test(dataProvider = "variants")
    protected void testThrowingAsLinesImpl(String uri,
                                    boolean sameClient,
                                    Thrower thrower)
            throws Exception
    {
        String test =  format("testThrowingAsLines(%s, %b, %s)",
                uri, sameClient, thrower);
        testThrowing(test, uri, sameClient, BodyHandlers::ofLines,
                this::checkAsLines, thrower);
    }

    //@Test(dataProvider = "variants")
    protected void testThrowingAsInputStreamImpl(String uri,
                                          boolean sameClient,
                                          Thrower thrower)
            throws Exception
    {
        String test = format("testThrowingAsInputStream(%s, %b, %s)",
                uri, sameClient, thrower);
        testThrowing(test, uri, sameClient, BodyHandlers::ofInputStream,
                this::checkAsInputStream,  thrower);
    }

    private <T,U> void testThrowing(String name, String uri, boolean sameClient,
                                    Supplier<BodyHandler<T>> handlers,
                                    Finisher finisher, Thrower thrower)
            throws Exception
    {
        checkSkip();
        out.printf("%n%s%s%n", now(), name);
        try {
            testThrowing(uri, sameClient, handlers, finisher, thrower);
        } catch (Error | Exception x) {
            FAILURES.putIfAbsent(name, x);
            throw x;
        }
    }

    private <T,U> void testThrowing(String uri, boolean sameClient,
                                    Supplier<BodyHandler<T>> handlers,
                                    Finisher finisher, Thrower thrower)
            throws Exception
    {
        HttpClient client = null;
        for (Where where : Where.values()) {
            if (where == Where.ON_ERROR) continue;
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            HttpRequest req = HttpRequest.
                    newBuilder(URI.create(uri))
                    .build();
            ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<T>>> promiseMap =
                    new ConcurrentHashMap<>();
            Supplier<BodyHandler<T>> throwing = () ->
                    new ThrowingBodyHandler(where.select(thrower), handlers.get());
            PushPromiseHandler<T> pushHandler = new ThrowingPromiseHandler<>(
                    where.select(thrower),
                    PushPromiseHandler.of((r) -> throwing.get(), promiseMap));
            out.println("try throwing in " + where);
            HttpResponse<T> response = null;
            try {
                response = client.sendAsync(req, handlers.get(), pushHandler).join();
            } catch (Error | Exception x) {
                throw x;
            }
            if (response != null) {
                finisher.finish(where, req.uri(), response, thrower, promiseMap);
            }
        }
    }

    interface Thrower extends Consumer<Where>, Predicate<Throwable> {

    }

    interface Finisher<T,U> {
        U finish(Where w, URI requestURI, HttpResponse<T> resp, Thrower thrower,
                 Map<HttpRequest, CompletableFuture<HttpResponse<T>>> promises);
    }

    final <T,U> U shouldHaveThrown(Where w, HttpResponse<T> resp, Thrower thrower) {
        String msg = "Expected exception not thrown in " + w
                + "\n\tReceived: " + resp
                + "\n\tWith body: " + resp.body();
        System.out.println(msg);
        throw new RuntimeException(msg);
    }

    final List<String> checkAsString(Where w, URI reqURI,
                                    HttpResponse<String> resp,
                                    Thrower thrower,
                                    Map<HttpRequest, CompletableFuture<HttpResponse<String>>> promises) {
        Function<HttpResponse<String>, List<String>> extractor =
                (r) -> List.of(r.body());
        return check(w, reqURI, resp, thrower, promises, extractor);
    }

    final List<String> checkAsLines(Where w, URI reqURI,
                                    HttpResponse<Stream<String>> resp,
                                    Thrower thrower,
                                    Map<HttpRequest, CompletableFuture<HttpResponse<Stream<String>>>> promises) {
        Function<HttpResponse<Stream<String>>, List<String>> extractor =
                (r) -> r.body().collect(Collectors.toList());
        return check(w, reqURI, resp, thrower, promises, extractor);
    }

    final List<String> checkAsInputStream(Where w, URI reqURI,
                                          HttpResponse<InputStream> resp,
                                          Thrower thrower,
                                          Map<HttpRequest, CompletableFuture<HttpResponse<InputStream>>> promises)
    {
        Function<HttpResponse<InputStream>, List<String>> extractor = (r) -> {
            List<String> result;
            try (InputStream is = r.body()) {
                result = new BufferedReader(new InputStreamReader(is))
                        .lines().collect(Collectors.toList());
            } catch (Throwable t) {
                throw new CompletionException(t);
            }
            return result;
        };
        return check(w, reqURI, resp, thrower, promises, extractor);
    }

    private final <T> List<String> check(Where w, URI reqURI,
                                 HttpResponse<T> resp,
                                 Thrower thrower,
                                 Map<HttpRequest, CompletableFuture<HttpResponse<T>>> promises,
                                 Function<HttpResponse<T>, List<String>> extractor)
    {
        List<String> result = extractor.apply(resp);
        for (HttpRequest req : promises.keySet()) {
            switch (w) {
                case BEFORE_ACCEPTING:
                    throw new RuntimeException("No push promise should have been received" +
                            " for " + reqURI + " in " + w + ": got " + promises.keySet());
                default:
                    break;
            }
            HttpResponse<T> presp;
            try {
                presp = promises.get(req).join();
            } catch (Error | Exception x) {
                Throwable cause = findCause(x, thrower);
                if (cause != null) {
                    out.println(now() + "Got expected exception in "
                            + w + ": " + cause);
                    continue;
                }
                throw x;
            }
            switch (w) {
                case BEFORE_ACCEPTING:
                case AFTER_ACCEPTING:
                case BODY_HANDLER:
                case GET_BODY:
                case BODY_CF:
                    return shouldHaveThrown(w, presp, thrower);
                default:
                    break;
            }
            List<String> presult = null;
            try {
                presult = extractor.apply(presp);
            } catch (Error | Exception x) {
                Throwable cause = findCause(x, thrower);
                if (cause != null) {
                    out.println(now() + "Got expected exception for "
                            + req + " in " + w + ": " + cause);
                    continue;
                }
                throw x;
            }
            throw new RuntimeException("Expected exception not thrown for "
                    + req + " in " + w);
        }
        final int expectedCount;
        switch (w) {
            case BEFORE_ACCEPTING:
                expectedCount = 0;
                break;
            default:
                expectedCount = 3;
        }
        assertEquals(promises.size(), expectedCount,
                "bad promise count for " + reqURI + " with " + w);
        assertEquals(result, List.of(reqURI.toASCIIString()));
        return result;
    }

    private static Throwable findCause(Throwable x,
                                       Predicate<Throwable> filter) {
        while (x != null && !filter.test(x)) x = x.getCause();
        return x;
    }

    static final class UncheckedCustomExceptionThrower implements Thrower {
        @Override
        public void accept(Where where) {
            out.println(now() + "Throwing in " + where);
            throw new UncheckedCustomException(where.name());
        }

        @Override
        public boolean test(Throwable throwable) {
            return UncheckedCustomException.class.isInstance(throwable);
        }

        @Override
        public String toString() {
            return "UncheckedCustomExceptionThrower";
        }
    }

    static final class UncheckedIOExceptionThrower implements Thrower {
        @Override
        public void accept(Where where) {
            out.println(now() + "Throwing in " + where);
            throw new UncheckedIOException(new CustomIOException(where.name()));
        }

        @Override
        public boolean test(Throwable throwable) {
            return UncheckedIOException.class.isInstance(throwable)
                    && CustomIOException.class.isInstance(throwable.getCause());
        }

        @Override
        public String toString() {
            return "UncheckedIOExceptionThrower";
        }
    }

    static final class UncheckedCustomException extends RuntimeException {
        UncheckedCustomException(String message) {
            super(message);
        }
        UncheckedCustomException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static final class CustomIOException extends IOException {
        CustomIOException(String message) {
            super(message);
        }
        CustomIOException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static final class ThrowingPromiseHandler<T> implements PushPromiseHandler<T> {
        final Consumer<Where> throwing;
        final PushPromiseHandler<T> pushHandler;
        ThrowingPromiseHandler(Consumer<Where> throwing, PushPromiseHandler<T> pushHandler) {
            this.throwing = throwing;
            this.pushHandler = pushHandler;
        }

        @Override
        public void applyPushPromise(HttpRequest initiatingRequest,
                                     HttpRequest pushPromiseRequest,
                                     Function<BodyHandler<T>,
                                             CompletableFuture<HttpResponse<T>>> acceptor) {
            throwing.accept(Where.BEFORE_ACCEPTING);
            pushHandler.applyPushPromise(initiatingRequest, pushPromiseRequest, acceptor);
            throwing.accept(Where.AFTER_ACCEPTING);
        }
    }

    static final class ThrowingBodyHandler<T> implements BodyHandler<T> {
        final Consumer<Where> throwing;
        final BodyHandler<T> bodyHandler;
        ThrowingBodyHandler(Consumer<Where> throwing, BodyHandler<T> bodyHandler) {
            this.throwing = throwing;
            this.bodyHandler = bodyHandler;
        }
        @Override
        public BodySubscriber<T> apply(HttpResponse.ResponseInfo rinfo) {
            throwing.accept(Where.BODY_HANDLER);
            BodySubscriber<T> subscriber = bodyHandler.apply(rinfo);
            return new ThrowingBodySubscriber(throwing, subscriber);
        }
    }

    static final class ThrowingBodySubscriber<T> implements BodySubscriber<T> {
        private final BodySubscriber<T> subscriber;
        volatile boolean onSubscribeCalled;
        final Consumer<Where> throwing;
        ThrowingBodySubscriber(Consumer<Where> throwing, BodySubscriber<T> subscriber) {
            this.throwing = throwing;
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            //out.println("onSubscribe ");
            onSubscribeCalled = true;
            throwing.accept(Where.ON_SUBSCRIBE);
            subscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
           // out.println("onNext " + item);
            assertTrue(onSubscribeCalled);
            throwing.accept(Where.ON_NEXT);
            subscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            //out.println("onError");
            assertTrue(onSubscribeCalled);
            throwing.accept(Where.ON_ERROR);
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            //out.println("onComplete");
            assertTrue(onSubscribeCalled, "onComplete called before onSubscribe");
            throwing.accept(Where.ON_COMPLETE);
            subscriber.onComplete();
        }

        @Override
        public CompletionStage<T> getBody() {
            throwing.accept(Where.GET_BODY);
            try {
                throwing.accept(Where.BODY_CF);
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
        http2URI_fixed = "http://" + http2TestServer.serverAuthority() + "/http2/fixed/x";
        http2URI_chunk = "http://" + http2TestServer.serverAuthority() + "/http2/chunk/x";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(h2_fixedLengthHandler, "/https2/fixed");
        https2TestServer.addHandler(h2_chunkedHandler, "/https2/chunk");
        https2URI_fixed = "https://" + https2TestServer.serverAuthority() + "/https2/fixed/x";
        https2URI_chunk = "https://" + https2TestServer.serverAuthority() + "/https2/chunk/x";

        serverCount.addAndGet(2);
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
