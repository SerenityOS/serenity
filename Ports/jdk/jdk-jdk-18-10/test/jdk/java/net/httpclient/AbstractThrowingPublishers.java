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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public abstract class AbstractThrowingPublishers implements HttpServerAdapters {

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

    @DataProvider(name = "sanity")
    public Object[][] sanity() {
        String[] uris = uris();
        Object[][] result = new Object[uris.length * 2][];
        //Object[][] result = new Object[uris.length][];
        int i = 0;
        for (boolean sameClient : List.of(false, true)) {
            //if (!sameClient) continue;
            for (String uri: uris()) {
                result[i++] = new Object[] {uri + "/sanity", sameClient};
            }
        }
        assert i == uris.length * 2;
        // assert i == uris.length ;
        return result;
    }

    enum Where {
        BEFORE_SUBSCRIBE, BEFORE_REQUEST, BEFORE_NEXT_REQUEST, BEFORE_CANCEL,
        AFTER_SUBSCRIBE, AFTER_REQUEST, AFTER_NEXT_REQUEST, AFTER_CANCEL;
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

    private Object[][] variants(List<Thrower> throwers, Set<Where> whereValues) {
        String[] uris = uris();
        Object[][] result = new Object[uris.length * 2 * throwers.size()][];
        //Object[][] result = new Object[(uris.length/2) * 2 * 2][];
        int i = 0;
        for (Thrower thrower : throwers) {
            for (boolean sameClient : List.of(false, true)) {
                for (String uri : uris()) {
                    // if (uri.contains("http2") || uri.contains("https2")) continue;
                    // if (!sameClient) continue;
                    result[i++] = new Object[]{uri, sameClient, thrower, whereValues};
                }
            }
        }
        assert i == uris.length * 2 * throwers.size();
        //assert Stream.of(result).filter(o -> o != null).count() == result.length;
        return result;
    }

    @DataProvider(name = "subscribeProvider")
    public Object[][] subscribeProvider(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return  variants(List.of(
                new UncheckedCustomExceptionThrower(),
                new UncheckedIOExceptionThrower()),
                EnumSet.of(Where.BEFORE_SUBSCRIBE, Where.AFTER_SUBSCRIBE));
    }

    @DataProvider(name = "requestProvider")
    public Object[][] requestProvider(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return  variants(List.of(
                new UncheckedCustomExceptionThrower(),
                new UncheckedIOExceptionThrower()),
                EnumSet.of(Where.BEFORE_REQUEST, Where.AFTER_REQUEST));
    }

    @DataProvider(name = "nextRequestProvider")
    public Object[][] nextRequestProvider(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return  variants(List.of(
                new UncheckedCustomExceptionThrower(),
                new UncheckedIOExceptionThrower()),
                EnumSet.of(Where.BEFORE_NEXT_REQUEST, Where.AFTER_NEXT_REQUEST));
    }

    @DataProvider(name = "beforeCancelProviderIO")
    public Object[][] beforeCancelProviderIO(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return  variants(List.of(
                new UncheckedIOExceptionThrower()),
                EnumSet.of(Where.BEFORE_CANCEL));
    }

    @DataProvider(name = "afterCancelProviderIO")
    public Object[][] afterCancelProviderIO(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return  variants(List.of(
                new UncheckedIOExceptionThrower()),
                EnumSet.of(Where.AFTER_CANCEL));
    }

    @DataProvider(name = "beforeCancelProviderCustom")
    public Object[][] beforeCancelProviderCustom(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return  variants(List.of(
                new UncheckedCustomExceptionThrower()),
                EnumSet.of(Where.BEFORE_CANCEL));
    }

    @DataProvider(name = "afterCancelProviderCustom")
    public Object[][] afterCancelProvider(ITestContext context) {
        if (stopAfterFirstFailure() && context.getFailedTests().size() > 0) {
            return new Object[0][];
        }
        return  variants(List.of(
                new UncheckedCustomExceptionThrower()),
                EnumSet.of(Where.AFTER_CANCEL));
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

    final String BODY = "Some string | that ? can | be split ? several | ways.";

    //@Test(dataProvider = "sanity")
    protected void testSanityImpl(String uri, boolean sameClient)
            throws Exception {
        HttpClient client = null;
        out.printf("%n%s testSanity(%s, %b)%n", now(), uri, sameClient);
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            SubmissionPublisher<ByteBuffer> publisher
                    = new SubmissionPublisher<>(executor,10);
            ThrowingBodyPublisher bodyPublisher = new ThrowingBodyPublisher((w) -> {},
                    BodyPublishers.fromPublisher(publisher));
            CompletableFuture<Void> subscribedCF = bodyPublisher.subscribedCF();
            subscribedCF.whenComplete((r,t) -> System.out.println(now() + " subscribe completed " + t))
                    .thenAcceptAsync((v) -> {
                                Stream.of(BODY.split("\\|"))
                                        .forEachOrdered(s -> {
                                                System.out.println("submitting \"" + s +"\"");
                                                publisher.submit(ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)));
                                        });
                                System.out.println("publishing done");
                                publisher.close();
                            },
                    executor);

            HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                    .POST(bodyPublisher)
                    .build();
            BodyHandler<String> handler = BodyHandlers.ofString();
            CompletableFuture<HttpResponse<String>> response = client.sendAsync(req, handler);

            String body = response.join().body();
            assertEquals(body, Stream.of(BODY.split("\\|")).collect(Collectors.joining()));
        }
    }

    // @Test(dataProvider = "variants")
    protected void testThrowingAsStringImpl(String uri,
                                     boolean sameClient,
                                     Thrower thrower,
                                     Set<Where> whereValues)
            throws Exception
    {
        String test = format("testThrowingAsString(%s, %b, %s, %s)",
                             uri, sameClient, thrower, whereValues);
        List<byte[]> bytes = Stream.of(BODY.split("|"))
                .map(s -> s.getBytes(UTF_8))
                .collect(Collectors.toList());
        testThrowing(test, uri, sameClient, () -> BodyPublishers.ofByteArrays(bytes),
                this::shouldNotThrowInCancel, thrower,false, whereValues);
    }

    private <T,U> void testThrowing(String name, String uri, boolean sameClient,
                                    Supplier<BodyPublisher> publishers,
                                    Finisher finisher, Thrower thrower,
                                    boolean async, Set<Where> whereValues)
            throws Exception
    {
        checkSkip();
        out.printf("%n%s%s%n", now(), name);
        try {
            testThrowing(uri, sameClient, publishers, finisher, thrower, async, whereValues);
        } catch (Error | Exception x) {
            FAILURES.putIfAbsent(name, x);
            throw x;
        }
    }

    private void testThrowing(String uri, boolean sameClient,
                                    Supplier<BodyPublisher> publishers,
                                    Finisher finisher, Thrower thrower,
                                    boolean async, Set<Where> whereValues)
            throws Exception
    {
        HttpClient client = null;
        for (Where where : whereValues) {
            //if (where == Where.ON_SUBSCRIBE) continue;
            //if (where == Where.ON_ERROR) continue;
            if (!sameClient || client == null)
                client = newHttpClient(sameClient);

            ThrowingBodyPublisher bodyPublisher =
                    new ThrowingBodyPublisher(where.select(thrower), publishers.get());
            HttpRequest req = HttpRequest.
                    newBuilder(URI.create(uri))
                    .header("X-expect-exception", "true")
                    .POST(bodyPublisher)
                    .build();
            BodyHandler<String> handler = BodyHandlers.ofString();
            System.out.println("try throwing in " + where);
            HttpResponse<String> response = null;
            if (async) {
                try {
                    response = client.sendAsync(req, handler).join();
                } catch (Error | Exception x) {
                    Throwable cause = findCause(where, x, thrower);
                    if (cause == null) throw causeNotFound(where, x);
                    System.out.println(now() + "Got expected exception: " + cause);
                }
            } else {
                try {
                    response = client.send(req, handler);
                } catch (Error | Exception t) {
                    // synchronous send will rethrow exceptions
                    Throwable throwable = t.getCause();
                    assert throwable != null;

                    if (thrower.test(where, throwable)) {
                        System.out.println(now() + "Got expected exception: " + throwable);
                    } else throw causeNotFound(where, t);
                }
            }
            if (response != null) {
                finisher.finish(where, response, thrower);
            }
        }
    }

    // can be used to reduce the surface of the test when diagnosing
    // some failure
    Set<Where> whereValues() {
        //return EnumSet.of(Where.BEFORE_CANCEL, Where.AFTER_CANCEL);
        return EnumSet.allOf(Where.class);
    }

    interface Thrower extends Consumer<Where>, BiPredicate<Where,Throwable> {

    }

    interface Finisher<T,U> {
        U finish(Where w, HttpResponse<T> resp, Thrower thrower) throws IOException;
    }

    final <T,U> U shouldNotThrowInCancel(Where w, HttpResponse<T> resp, Thrower thrower) {
        switch (w) {
            case BEFORE_CANCEL: return null;
            case AFTER_CANCEL: return null;
            default: break;
        }
        return shouldHaveThrown(w, resp, thrower);
    }


    final <T,U> U shouldHaveThrown(Where w, HttpResponse<T> resp, Thrower thrower) {
        String msg = "Expected exception not thrown in " + w
                + "\n\tReceived: " + resp
                + "\n\tWith body: " + resp.body();
        System.out.println(msg);
        throw new RuntimeException(msg);
    }


    private static Throwable findCause(Where w,
                                       Throwable x,
                                       BiPredicate<Where, Throwable> filter) {
        while (x != null && !filter.test(w,x)) x = x.getCause();
        return x;
    }

    static AssertionError causeNotFound(Where w, Throwable t) {
        return new AssertionError("Expected exception not found in " + w, t);
    }

    static boolean isConnectionClosedLocally(Throwable t) {
        if (t instanceof CompletionException) t = t.getCause();
        if (t instanceof ExecutionException) t = t.getCause();
        if (t instanceof IOException) {
            String msg = t.getMessage();
            return msg == null ? false
                    : msg.contains("connection closed locally");
        }
        return false;
    }

    static final class UncheckedCustomExceptionThrower implements Thrower {
        @Override
        public void accept(Where where) {
            out.println(now() + "Throwing in " + where);
            throw new UncheckedCustomException(where.name());
        }

        @Override
        public boolean test(Where w, Throwable throwable) {
            switch (w) {
                case AFTER_REQUEST:
                case BEFORE_NEXT_REQUEST:
                case AFTER_NEXT_REQUEST:
                    if (isConnectionClosedLocally(throwable)) return true;
                    break;
                default:
                    break;
            }
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
        public boolean test(Where w, Throwable throwable) {
            switch (w) {
                case AFTER_REQUEST:
                case BEFORE_NEXT_REQUEST:
                case AFTER_NEXT_REQUEST:
                    if (isConnectionClosedLocally(throwable)) return true;
                    break;
                default:
                    break;
            }
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


    static final class ThrowingBodyPublisher implements BodyPublisher {
        private final BodyPublisher publisher;
        private final CompletableFuture<Void> subscribedCF = new CompletableFuture<>();
        final Consumer<Where> throwing;
        ThrowingBodyPublisher(Consumer<Where> throwing, BodyPublisher publisher) {
            this.throwing = throwing;
            this.publisher = publisher;
        }

        @Override
        public long contentLength() {
            return publisher.contentLength();
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            try {
                throwing.accept(Where.BEFORE_SUBSCRIBE);
                publisher.subscribe(new SubscriberWrapper(subscriber));
                subscribedCF.complete(null);
                throwing.accept(Where.AFTER_SUBSCRIBE);
            } catch (Throwable t) {
                subscribedCF.completeExceptionally(t);
                throw t;
            }
        }

        CompletableFuture<Void> subscribedCF() {
            return subscribedCF;
        }

        class SubscriptionWrapper implements Flow.Subscription {
            final Flow.Subscription subscription;
            final AtomicLong requestCount = new AtomicLong();
            SubscriptionWrapper(Flow.Subscription subscription) {
                this.subscription = subscription;
            }
            @Override
            public void request(long n) {
                long count = requestCount.incrementAndGet();
                System.out.printf("%s request-%d(%d)%n", now(), count, n);
                if (count > 1) throwing.accept(Where.BEFORE_NEXT_REQUEST);
                throwing.accept(Where.BEFORE_REQUEST);
                subscription.request(n);
                throwing.accept(Where.AFTER_REQUEST);
                if (count > 1) throwing.accept(Where.AFTER_NEXT_REQUEST);
            }

            @Override
            public void cancel() {
                throwing.accept(Where.BEFORE_CANCEL);
                subscription.cancel();
                throwing.accept(Where.AFTER_CANCEL);
            }
        }

        class SubscriberWrapper implements Flow.Subscriber<ByteBuffer> {
            final Flow.Subscriber<? super ByteBuffer> subscriber;
            SubscriberWrapper(Flow.Subscriber<? super ByteBuffer> subscriber) {
                this.subscriber = subscriber;
            }
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(new SubscriptionWrapper(subscription));
            }
            @Override
            public void onNext(ByteBuffer item) {
                subscriber.onNext(item);
            }
            @Override
            public void onComplete() {
                subscriber.onComplete();
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
            }
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

    static class HTTP_FixedLengthHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            out.println("HTTP_FixedLengthHandler received request to " + t.getRequestURI());
            byte[] resp;
            try (InputStream is = t.getRequestBody()) {
                resp = is.readAllBytes();
            }
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
            byte[] resp;
            try (InputStream is = t.getRequestBody()) {
                resp = is.readAllBytes();
            }
            t.sendResponseHeaders(200, -1); // chunked/variable
            try (OutputStream os = t.getResponseBody()) {
                os.write(resp);
            }
        }
    }

}
