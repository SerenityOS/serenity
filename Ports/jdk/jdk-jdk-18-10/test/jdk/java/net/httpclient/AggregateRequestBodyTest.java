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
 *                     AggregateRequestBodyTest
 * @summary Tests HttpRequest.BodyPublishers::concat
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
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

public class AggregateRequestBodyTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer http1TestServer;   // HTTP/1.1 ( http )
    HttpTestServer https1TestServer;  // HTTPS/1.1 ( https  )
    HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
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

    static final List<String> BODIES = List.of(
            "Lorem ipsum",
            "dolor sit amet",
            "consectetur adipiscing elit, sed do eiusmod tempor",
            "quis nostrud exercitation ullamco",
            "laboris nisi",
            "ut",
            "aliquip ex ea commodo consequat." +
                    "Duis aute irure dolor in reprehenderit in voluptate velit esse" +
                    "cillum dolore eu fugiat nulla pariatur.",
            "Excepteur sint occaecat cupidatat non proident."
    );

    static BodyPublisher[] publishers(String... content) {
        if (content == null) return null;
        BodyPublisher[] result = new BodyPublisher[content.length];
        for (int i=0; i < content.length ; i++) {
            result[i] = content[i] == null ? null : BodyPublishers.ofString(content[i]);
        }
        return result;
    }

    static String[] strings(String... s) {
        return s;
    }

    @DataProvider(name = "sparseContent")
    Object[][] nulls() {
        return new Object[][] {
                {"null array", null},
                {"null element", strings((String)null)},
                {"null first element", strings(null, "one")},
                {"null second element", strings( "one", null)},
                {"null third element", strings( "one", "two", null)},
                {"null fourth element", strings( "one", "two", "three", null)},
                {"null random element", strings( "one", "two", "three", null, "five")},
        };
    }

    static List<Long> lengths(long... lengths) {
        return LongStream.of(lengths)
                .mapToObj(Long::valueOf)
                .collect(Collectors.toList());
    }

    @DataProvider(name = "contentLengths")
    Object[][] contentLengths() {
        return new Object[][] {
                {-1, lengths(-1)},
                {-42, lengths(-42)},
                {42, lengths(42)},
                {42, lengths(10, 0, 20, 0, 12)},
                {-1, lengths(10, 0, 20, -1, 12)},
                {-1, lengths(-1, 0, 20, 10, 12)},
                {-1, lengths(10, 0, 20, 12, -1)},
                {-1, lengths(10, 0, 20, -10, 12)},
                {-1, lengths(-10, 0, 20, 10, 12)},
                {-1, lengths(10, 0, 20, 12, -10)},
                {-1, lengths(10, 0, Long.MIN_VALUE, -1, 12)},
                {-1, lengths(-1, 0, Long.MIN_VALUE, 10, 12)},
                {-1, lengths(10, Long.MIN_VALUE, 20, 12, -1)},
                {Long.MAX_VALUE, lengths(10, Long.MAX_VALUE - 42L, 20, 0, 12)},
                {-1, lengths(10, Long.MAX_VALUE - 40L, 20, 0, 12)},
                {-1, lengths(10, Long.MAX_VALUE - 12L, 20, 0, 12)},
                {-1, lengths(10, Long.MAX_VALUE/2L, Long.MAX_VALUE/2L + 1L, 0, 12)},
                {-1, lengths(10, Long.MAX_VALUE/2L, -1, Long.MAX_VALUE/2L + 1L, 12)},
                {-1, lengths(10, Long.MAX_VALUE, 12, Long.MAX_VALUE, 20)},
                {-1, lengths(10, Long.MAX_VALUE, Long.MAX_VALUE, 12, 20)},
                {-1, lengths(0, Long.MAX_VALUE, Long.MAX_VALUE, 12, 20)},
                {-1, lengths(Long.MAX_VALUE, Long.MAX_VALUE, 12, 0, 20)}
        };
    }

    @DataProvider(name="negativeRequests")
    Object[][] negativeRequests() {
        return new Object[][] {
                {0L}, {-1L}, {-2L}, {Long.MIN_VALUE + 1L}, {Long.MIN_VALUE}
        };
    }


    static class ContentLengthPublisher implements BodyPublisher {
        final long length;
        ContentLengthPublisher(long length) {
            this.length = length;
        }
        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        }

        static ContentLengthPublisher[] of(List<Long> lengths) {
            return lengths.stream()
                    .map(ContentLengthPublisher::new)
                    .toArray(ContentLengthPublisher[]::new);
        }
    }

    /**
     * A dummy publisher that allows to call onError on its subscriber (or not...).
     */
    static class PublishWithError implements BodyPublisher {
        final ConcurrentHashMap<Subscriber<?>, ErrorSubscription> subscribers = new ConcurrentHashMap<>();
        final long length;
        final List<String> content;
        final int errorAt;
        final Supplier<? extends Throwable> errorSupplier;
        PublishWithError(List<String> content, int errorAt, Supplier<? extends Throwable> supplier) {
            this.content = content;
            this.errorAt = errorAt;
            this.errorSupplier = supplier;
            length = content.stream().mapToInt(String::length).sum();
        }

        boolean hasErrors() {
            return errorAt < content.size();
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            ErrorSubscription subscription = new ErrorSubscription(subscriber);
            subscribers.put(subscriber, subscription);
            subscriber.onSubscribe(subscription);
        }

        class ErrorSubscription implements Flow.Subscription {
            volatile boolean cancelled;
            volatile int at;
            final Subscriber<? super ByteBuffer> subscriber;
            ErrorSubscription(Subscriber<? super ByteBuffer> subscriber) {
                this.subscriber = subscriber;
            }
            @Override
            public void request(long n) {
                while (!cancelled && --n >= 0 && at < Math.min(errorAt+1, content.size())) {
                    if (at++ == errorAt) {
                        subscriber.onError(errorSupplier.get());
                        return;
                    } else if (at <= content.size()){
                        subscriber.onNext(ByteBuffer.wrap(
                                content.get(at-1).getBytes()));
                        if (at == content.size()) {
                            subscriber.onComplete();
                            return;
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        }
    }

    static class RequestSubscriber implements Flow.Subscriber<ByteBuffer> {
        CompletableFuture<Subscription> subscriptionCF = new CompletableFuture<>();
        ConcurrentLinkedDeque<ByteBuffer> items = new ConcurrentLinkedDeque<>();
        CompletableFuture<List<ByteBuffer>> resultCF = new CompletableFuture<>();

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscriptionCF.complete(subscription);
        }

        @Override
        public void onNext(ByteBuffer item) {
            items.addLast(item);
        }

        @Override
        public void onError(Throwable throwable) {
            resultCF.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            resultCF.complete(items.stream().collect(Collectors.toUnmodifiableList()));
        }

        CompletableFuture<List<ByteBuffer>> resultCF() { return resultCF; }
    }

    static String stringFromBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes);
    }

    String stringFromBytes(Stream<ByteBuffer> buffers) {
        return buffers.map(AggregateRequestBodyTest::stringFromBuffer)
                .collect(Collectors.joining());
    }

    static PublishWithError withNoError(String content) {
        return new PublishWithError(List.of(content), 1,
                () -> new AssertionError("Should not happen!"));
    }

    static PublishWithError withNoError(List<String> content) {
        return new PublishWithError(content, content.size(),
                () -> new AssertionError("Should not happen!"));
    }

    @Test(dataProvider = "sparseContent") // checks that NPE is thrown
    public void testNullPointerException(String description, String[] content) {
        checkSkip();
        BodyPublisher[] publishers = publishers(content);
        Assert.assertThrows(NullPointerException.class, () -> BodyPublishers.concat(publishers));
    }

    // Verifies that an empty array creates a "noBody" publisher
    @Test
    public void testEmpty() {
        checkSkip();
        BodyPublisher publisher = BodyPublishers.concat();
        RequestSubscriber subscriber = new RequestSubscriber();
        assertEquals(publisher.contentLength(), 0);
        publisher.subscribe(subscriber);
        subscriber.subscriptionCF.thenAccept(s -> s.request(1));
        List<ByteBuffer> result = subscriber.resultCF.join();
        assertEquals(result, List.of());
        assertTrue(subscriber.items.isEmpty());;
    }

    // verifies that error emitted by upstream publishers are propagated downstream.
    @Test(dataProvider = "sparseContent") // nulls are replaced with error publisher
    public void testOnError(String description, String[] content) {
        checkSkip();
        final RequestSubscriber subscriber = new RequestSubscriber();
        final PublishWithError errorPublisher;
        final BodyPublisher[] publishers;
        String result = BODIES.stream().collect(Collectors.joining());
        if (content == null) {
            content = List.of(result).toArray(String[]::new);
            errorPublisher = new PublishWithError(BODIES, BODIES.size(),
                    () -> new AssertionError("Unexpected!!"));
            publishers = List.of(errorPublisher).toArray(new BodyPublisher[0]);
            description = "No error";
        } else {
            publishers = publishers(content);
            description = description.replace("null", "error at");
            errorPublisher = new PublishWithError(BODIES, 2, () -> new Exception("expected"));
        }
        result = "";
        boolean hasErrors = false;
        for (int i=0; i < content.length; i++) {
            if (content[i] == null) {
                publishers[i] = errorPublisher;
                if (hasErrors) continue;
                if (!errorPublisher.hasErrors()) {
                    result = result + errorPublisher
                            .content.stream().collect(Collectors.joining());
                } else {
                    result = result + errorPublisher.content
                            .stream().limit(errorPublisher.errorAt)
                            .collect(Collectors.joining());
                    result = result + "<error>";
                    hasErrors = true;
                }
            } else if (!hasErrors) {
                result = result + content[i];
            }
        }
        BodyPublisher publisher = BodyPublishers.concat(publishers);
        publisher.subscribe(subscriber);
        subscriber.subscriptionCF.thenAccept(s -> s.request(Long.MAX_VALUE));
        if (errorPublisher.hasErrors()) {
            CompletionException ce = expectThrows(CompletionException.class,
                    () -> subscriber.resultCF.join());
            out.println(description + ": got expected " + ce);
            assertEquals(ce.getCause().getClass(), Exception.class);
            assertEquals(stringFromBytes(subscriber.items.stream()) + "<error>", result);
        } else {
            assertEquals(stringFromBytes(subscriber.resultCF.join().stream()), result);
            out.println(description + ": got expected result: " + result);
        }
    }

    // Verifies that if an upstream publisher has an unknown length, the
    // aggregate publisher will have an unknown length as well. Otherwise
    // the length should be known.
    @Test(dataProvider = "sparseContent") // nulls are replaced with unknown length
    public void testUnknownContentLength(String description, String[] content) {
        checkSkip();
        if (content == null) {
            content = BODIES.toArray(String[]::new);
            description = "BODIES (known length)";
        } else {
            description = description.replace("null", "length(-1)");
        }
        BodyPublisher[] publishers = publishers(content);
        BodyPublisher nolength = new BodyPublisher() {
            final BodyPublisher missing = BodyPublishers.ofString("missing");
            @Override
            public long contentLength() { return -1; }
            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                missing.subscribe(subscriber);
            }
        };
        long length = 0;
        for (int i=0; i < content.length; i++) {
            if (content[i] == null) {
                publishers[i] = nolength;
                length = -1;
            } else if (length >= 0) {
                length += content[i].length();
            }
        }
        out.printf("testUnknownContentLength(%s): %d%n", description, length);
        BodyPublisher publisher = BodyPublishers.concat(publishers);
        assertEquals(publisher.contentLength(), length,
                description.replace("null", "length(-1)"));
    }

    private static final Throwable completionCause(CompletionException x) {
        while (x.getCause() instanceof CompletionException) {
            x = (CompletionException)x.getCause();
        }
        return x.getCause();
    }

    @Test(dataProvider = "negativeRequests")
    public void testNegativeRequest(long n) {
        checkSkip();
        assert n <= 0 : "test for negative request called with n > 0 : " + n;
        BodyPublisher[] publishers = ContentLengthPublisher.of(List.of(1L, 2L, 3L));
        BodyPublisher publisher = BodyPublishers.concat(publishers);
        RequestSubscriber subscriber = new RequestSubscriber();
        publisher.subscribe(subscriber);
        Subscription subscription = subscriber.subscriptionCF.join();
        subscription.request(n);
        CompletionException expected = expectThrows(CE, () -> subscriber.resultCF.join());
        Throwable cause = completionCause(expected);
        if (cause instanceof IllegalArgumentException) {
            System.out.printf("Got expected IAE for %d: %s%n", n, cause);
        } else {
            throw new AssertionError("Unexpected exception: " + cause,
                    (cause == null) ? expected : cause);
        }
    }

    static BodyPublisher[] ofStrings(String... strings) {
        return Stream.of(strings).map(BodyPublishers::ofString).toArray(BodyPublisher[]::new);
    }

    @Test
    public void testPositiveRequests()  {
        checkSkip();
        // A composite array of publishers
        BodyPublisher[] publishers = Stream.of(
                Stream.of(ofStrings("Lorem", " ", "ipsum", " ")),
                Stream.of(BodyPublishers.concat(ofStrings("dolor", " ", "sit", " ", "amet", ", "))),
                Stream.<BodyPublisher>of(withNoError(List.of("consectetur", " ", "adipiscing"))),
                Stream.of(ofStrings(" ")),
                Stream.of(BodyPublishers.concat(ofStrings("elit", ".")))
        ).flatMap((s) -> s).toArray(BodyPublisher[]::new);
        BodyPublisher publisher = BodyPublishers.concat(publishers);

        // Test that we can request all 13 items in a single request call.
        RequestSubscriber requestSubscriber1 = new RequestSubscriber();
        publisher.subscribe(requestSubscriber1);
        Subscription subscription1 = requestSubscriber1.subscriptionCF.join();
        subscription1.request(16);
        assertTrue(requestSubscriber1.resultCF().isDone());
        List<ByteBuffer> list1 = requestSubscriber1.resultCF().join();
        String result1 = stringFromBytes(list1.stream());
        assertEquals(result1, "Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
        System.out.println("Got expected sentence with one request: \"%s\"".formatted(result1));

        // Test that we can split our requests call any which way we want
        // (whether in the 'middle of a publisher' or at the boundaries.
        RequestSubscriber requestSubscriber2 = new RequestSubscriber();
        publisher.subscribe(requestSubscriber2);
        Subscription subscription2 = requestSubscriber2.subscriptionCF.join();
        subscription2.request(1);
        assertFalse(requestSubscriber2.resultCF().isDone());
        subscription2.request(10);
        assertFalse(requestSubscriber2.resultCF().isDone());
        subscription2.request(4);
        assertFalse(requestSubscriber2.resultCF().isDone());
        subscription2.request(1);
        assertTrue(requestSubscriber2.resultCF().isDone());
        List<ByteBuffer> list2 = requestSubscriber2.resultCF().join();
        String result2 = stringFromBytes(list2.stream());
        assertEquals(result2, "Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
        System.out.println("Got expected sentence with 4 requests: \"%s\"".formatted(result1));
    }

    @Test(dataProvider = "contentLengths")
    public void testContentLength(long expected, List<Long> lengths) {
        checkSkip();
        BodyPublisher[] publishers = ContentLengthPublisher.of(lengths);
        BodyPublisher aggregate = BodyPublishers.concat(publishers);
        assertEquals(aggregate.contentLength(), expected,
                "Unexpected result for %s".formatted(lengths));
    }

    // Verifies that cancelling the subscription ensure that downstream
    // publishers are no longer subscribed etc...
    @Test
    public void testCancel() {
        checkSkip();
        BodyPublisher[] publishers = BODIES.stream()
                .map(BodyPublishers::ofString)
                .toArray(BodyPublisher[]::new);
        BodyPublisher publisher = BodyPublishers.concat(publishers);

        assertEquals(publisher.contentLength(),
                BODIES.stream().mapToInt(String::length).sum());
        Map<RequestSubscriber, String> subscribers = new LinkedHashMap<>();

        for (int n=0; n < BODIES.size(); n++) {

            String description = String.format(
                    "cancel after %d/%d onNext() invocations",
                    n, BODIES.size());
            RequestSubscriber subscriber = new RequestSubscriber();
            publisher.subscribe(subscriber);
            Subscription subscription = subscriber.subscriptionCF.join();
            subscribers.put(subscriber, description);

            // receive half the data
            for (int i = 0; i < n; i++) {
                subscription.request(1);
                ByteBuffer buffer = subscriber.items.pop();
            }

            // cancel subscription
            subscription.cancel();
            // request the rest...
            subscription.request(Long.MAX_VALUE);
        }

        CompletableFuture[] results = subscribers.keySet()
                .stream().map(RequestSubscriber::resultCF)
                .toArray(CompletableFuture[]::new);
        CompletableFuture<?> any = CompletableFuture.anyOf(results);

        // subscription was cancelled, so nothing should be received...
        try {
            TimeoutException x = Assert.expectThrows(TimeoutException.class,
                    () -> any.get(5, TimeUnit.SECONDS));
            out.println("Got expected " + x);
        } finally {
            subscribers.keySet().stream()
                    .filter(rs -> rs.resultCF.isDone())
                    .forEach(rs -> System.err.printf(
                            "Failed: %s completed with %s",
                            subscribers.get(rs), rs.resultCF));
        }
        Consumer<RequestSubscriber> check = (rs) -> {
            Assert.assertTrue(rs.items.isEmpty(), subscribers.get(rs) + " has items");
            Assert.assertFalse(rs.resultCF.isDone(), subscribers.get(rs) + " was not cancelled");
            out.println(subscribers.get(rs) + ": PASSED");
        };
        subscribers.keySet().stream().forEach(check);
    }

    // Verifies that cancelling the subscription is propagated downstream
    @Test
    public void testCancelSubscription() {
        checkSkip();
        PublishWithError upstream = new PublishWithError(BODIES, BODIES.size(),
                () -> new AssertionError("should not come here"));
        BodyPublisher publisher = BodyPublishers.concat(upstream);

        assertEquals(publisher.contentLength(),
                BODIES.stream().mapToInt(String::length).sum());
        Map<RequestSubscriber, String> subscribers = new LinkedHashMap<>();

        for (int n=0; n < BODIES.size(); n++) {

            String description = String.format(
                    "cancel after %d/%d onNext() invocations",
                    n, BODIES.size());
            RequestSubscriber subscriber = new RequestSubscriber();
            publisher.subscribe(subscriber);
            Subscription subscription = subscriber.subscriptionCF.join();
            subscribers.put(subscriber, description);

            // receive half the data
            for (int i = 0; i < n; i++) {
                subscription.request(1);
                ByteBuffer buffer = subscriber.items.pop();
            }

            // cancel subscription
            subscription.cancel();
            // request the rest...
            subscription.request(Long.MAX_VALUE);
            assertTrue(upstream.subscribers.get(subscriber).cancelled,
                    description + " upstream subscription not cancelled");
            out.println(description + " upstream subscription was properly cancelled");
        }

        CompletableFuture[] results = subscribers.keySet()
                .stream().map(RequestSubscriber::resultCF)
                .toArray(CompletableFuture[]::new);
        CompletableFuture<?> any = CompletableFuture.anyOf(results);

        // subscription was cancelled, so nothing should be received...
        try {
            TimeoutException x = Assert.expectThrows(TimeoutException.class,
                    () -> any.get(5, TimeUnit.SECONDS));
            out.println("Got expected " + x);
        } finally {
            subscribers.keySet().stream()
                    .filter(rs -> rs.resultCF.isDone())
                    .forEach(rs -> System.err.printf(
                            "Failed: %s completed with %s",
                            subscribers.get(rs), rs.resultCF));
        }
        Consumer<RequestSubscriber> check = (rs) -> {
            Assert.assertTrue(rs.items.isEmpty(), subscribers.get(rs) + " has items");
            Assert.assertFalse(rs.resultCF.isDone(), subscribers.get(rs) + " was not cancelled");
            out.println(subscribers.get(rs) + ": PASSED");
        };
        subscribers.keySet().stream().forEach(check);

    }

    @Test(dataProvider = "variants")
    public void test(String uri, boolean sameClient) throws Exception {
        checkSkip();
        System.out.println("Request to " + uri);

        HttpClient client = newHttpClient(sameClient);

        BodyPublisher publisher = BodyPublishers.concat(
                BODIES.stream()
                        .map(BodyPublishers::ofString)
                        .toArray(HttpRequest.BodyPublisher[]::new)
                );
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .POST(publisher)
                .build();
        for (int i = 0; i < ITERATION_COUNT; i++) {
            System.out.println("Iteration: " + i);
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int expectedResponse =  RESPONSE_CODE;
            if (response.statusCode() != expectedResponse)
                throw new RuntimeException("wrong response code " + Integer.toString(response.statusCode()));
            assertEquals(response.body(), BODIES.stream().collect(Collectors.joining()));
        }
        System.out.println("test: DONE");
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        HttpTestHandler handler = new HttpTestEchoHandler();
        InetSocketAddress loopback = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        HttpServer http1 = HttpServer.create(loopback, 0);
        http1TestServer = HttpTestServer.of(http1);
        http1TestServer.addHandler(handler, "/http1/echo/");
        http1URI = "http://" + http1TestServer.serverAuthority() + "/http1/echo/x";

        HttpsServer https1 = HttpsServer.create(loopback, 0);
        https1.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        https1TestServer = HttpTestServer.of(https1);
        https1TestServer.addHandler(handler, "/https1/echo/");
        https1URI = "https://" + https1TestServer.serverAuthority() + "/https1/echo/x";

        // HTTP/2
        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(handler, "/http2/echo/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/echo/x";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(handler, "/https2/echo/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/echo/x";

        serverCount.addAndGet(4);
        http1TestServer.start();
        https1TestServer.start();
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
            http1TestServer.stop();
            https1TestServer.stop();
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
}
