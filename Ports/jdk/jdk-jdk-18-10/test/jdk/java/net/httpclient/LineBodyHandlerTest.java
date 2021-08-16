/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @summary Basic tests for line adapter subscribers as created by
 *          the BodyHandlers returned by BodyHandler::fromLineSubscriber
 *          and BodyHandler::asLines
 * @bug 8256459
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer LineBodyHandlerTest HttpServerAdapters
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:DiagnoseSyncOnValueBasedClasses=1 LineBodyHandlerTest
 */

public class LineBodyHandlerTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;    // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;   // HTTPS/1.1
    HttpTestServer http2TestServer;   // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    @DataProvider(name = "uris")
    public Object[][] variants() {
        return new Object[][]{
                { httpURI   },
                { httpsURI  },
                { http2URI  },
                { https2URI },
        };
    }

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    @Test
    public void testNull() {
        assertThrows(NPE, () -> BodyHandlers.fromLineSubscriber(null));
        assertNotNull(BodyHandlers.fromLineSubscriber(new StringSubscriber()));
        assertThrows(NPE, () -> BodyHandlers.fromLineSubscriber(null, Function.identity(), "\n"));
        assertThrows(NPE, () -> BodyHandlers.fromLineSubscriber(new StringSubscriber(), null, "\n"));
        assertNotNull(BodyHandlers.fromLineSubscriber(new StringSubscriber(), Function.identity(), null));
        assertThrows(NPE, () -> BodyHandlers.fromLineSubscriber(null, null, "\n"));
        assertThrows(NPE, () -> BodyHandlers.fromLineSubscriber(null, Function.identity(), null));
        assertThrows(NPE, () -> BodyHandlers.fromLineSubscriber(new StringSubscriber(), null, null));

        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, Function.identity(),
                Charset.defaultCharset(), System.lineSeparator()));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(new StringSubscriber(), null,
                Charset.defaultCharset(), System.lineSeparator()));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(new StringSubscriber(), Function.identity(),
                null, System.lineSeparator()));
        assertNotNull(BodySubscribers.fromLineSubscriber(new StringSubscriber(), Function.identity(),
                Charset.defaultCharset(), null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, null,
                Charset.defaultCharset(), System.lineSeparator()));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, Function.identity(),
                null, System.lineSeparator()));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, Function.identity(),
                Charset.defaultCharset(), null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(new StringSubscriber(), null,
                null, System.lineSeparator()));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(new StringSubscriber(), null,
                Charset.defaultCharset(), null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(new StringSubscriber(), Function.identity(),
                null, null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(new StringSubscriber(), null, null, null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, Function.identity(),
                null, null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, null,
                Charset.defaultCharset(), null));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, null,
                null, System.lineSeparator()));
        assertThrows(NPE, () -> BodySubscribers.fromLineSubscriber(null, null, null, null));
    }

    @Test
    public void testIAE() {
        assertThrows(IAE, () -> BodyHandlers.fromLineSubscriber(new StringSubscriber(), Function.identity(),""));
        assertThrows(IAE, () -> BodyHandlers.fromLineSubscriber(new CharSequenceSubscriber(), Function.identity(),""));
        assertThrows(IAE, () -> BodyHandlers.fromLineSubscriber(new ObjectSubscriber(), Function.identity(), ""));
        assertThrows(IAE, () -> BodySubscribers.fromLineSubscriber(new StringSubscriber(), Function.identity(),
                    StandardCharsets.UTF_8, ""));
        assertThrows(IAE, () -> BodySubscribers.fromLineSubscriber(new CharSequenceSubscriber(), Function.identity(),
                    StandardCharsets.UTF_16, ""));
        assertThrows(IAE, () -> BodySubscribers.fromLineSubscriber(new ObjectSubscriber(), Function.identity(),
                    StandardCharsets.US_ASCII, ""));
    }

    private static final List<String> lines(String text, String eol) {
        if (eol == null) {
            return new BufferedReader(new StringReader(text)).lines().collect(Collectors.toList());
        } else {
            String replaced = text.replace(eol, "|");
            int i=0;
            while(replaced.endsWith("||")) {
                replaced = replaced.substring(0,replaced.length()-1);
                i++;
            }
            List<String> res = List.of(replaced.split("\\|"));
            if (i > 0) {
                res = new ArrayList<>(res);
                for (int j=0; j<i; j++) res.add("");
            }
            return res;
        }
    }

    HttpClient newClient() {
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .proxy(Builder.NO_PROXY)
                .build();
    }

    @Test(dataProvider = "uris")
    void testStringWithFinisher(String url) {
        String body = "May the luck of the Irish be with you!";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        StringSubscriber subscriber = new StringSubscriber();
        CompletableFuture<HttpResponse<String>> cf
                = client.sendAsync(request, BodyHandlers.fromLineSubscriber(
                        subscriber, Supplier::get, "\n"));
        assertNoObtrusion(cf);
        HttpResponse<String> response = cf.join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, body);
        assertEquals(subscriber.list, lines(body, "\n"));
    }

    @Test(dataProvider = "uris")
    void testAsStream(String url) {
        String body = "May the luck of the Irish be with you!";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        CompletableFuture<HttpResponse<Stream<String>>> cf
                = client.sendAsync(request, BodyHandlers.ofLines());
        assertNoObtrusion(cf);
        HttpResponse<Stream<String>> response = cf.join();
        Stream<String> stream = response.body();
        List<String> list = stream.collect(Collectors.toList());
        String text = list.stream().collect(Collectors.joining("|"));
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, body);
        assertEquals(list, List.of(body));
        assertEquals(list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testStringWithFinisher2(String url) {
        String body = "May the luck\r\n\r\n of the Irish be with you!";
        HttpClient client = newClient();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        StringSubscriber subscriber = new StringSubscriber();
        CompletableFuture<HttpResponse<Void>> cf
                = client.sendAsync(request,
                                   BodyHandlers.fromLineSubscriber(subscriber));
        assertNoObtrusion(cf);
        HttpResponse<Void> response = cf.join();
        String text = subscriber.get();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, body.replace("\r\n", "\n"));
        assertEquals(subscriber.list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testAsStreamWithCRLF(String url) {
        String body = "May the luck\r\n\r\n of the Irish be with you!";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        CompletableFuture<HttpResponse<Stream<String>>> cf
                = client.sendAsync(request, BodyHandlers.ofLines());
        assertNoObtrusion(cf);
        HttpResponse<Stream<String>> response = cf.join();
        Stream<String> stream = response.body();
        List<String> list = stream.collect(Collectors.toList());
        String text = list.stream().collect(Collectors.joining("|"));
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, "May the luck|| of the Irish be with you!");
        assertEquals(list, List.of("May the luck",
                                   "",
                                   " of the Irish be with you!"));
        assertEquals(list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testStringWithFinisherBlocking(String url) throws Exception {
        String body = "May the luck of the Irish be with you!";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body)).build();

        StringSubscriber subscriber = new StringSubscriber();
        HttpResponse<String> response = client.send(request,
                BodyHandlers.fromLineSubscriber(subscriber, Supplier::get, "\n"));
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, "May the luck of the Irish be with you!");
        assertEquals(subscriber.list, lines(body, "\n"));
    }

    @Test(dataProvider = "uris")
    void testStringWithoutFinisherBlocking(String url) throws Exception {
        String body = "May the luck of the Irish be with you!";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body)).build();

        StringSubscriber subscriber = new StringSubscriber();
        HttpResponse<Void> response = client.send(request,
                BodyHandlers.fromLineSubscriber(subscriber));
        String text = subscriber.get();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, "May the luck of the Irish be with you!");
        assertEquals(subscriber.list, lines(body, null));
    }

    // Subscriber<Object>

    @Test(dataProvider = "uris")
    void testAsStreamWithMixedCRLF(String url) {
        String body = "May\r\n the wind\r\n always be\rat your back.\r\r";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        CompletableFuture<HttpResponse<Stream<String>>> cf
                = client.sendAsync(request, BodyHandlers.ofLines());
        assertNoObtrusion(cf);
        HttpResponse<Stream<String>> response = cf.join();
        Stream<String> stream = response.body();
        List<String> list = stream.collect(Collectors.toList());
        String text = list.stream().collect(Collectors.joining("|"));
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May| the wind| always be|at your back.|");
        assertEquals(list, List.of("May",
                                   " the wind",
                                   " always be",
                                   "at your back.",
                                   ""));
        assertEquals(list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testAsStreamWithMixedCRLF_UTF8(String url) {
        String body = "May\r\n the wind\r\n always be\rat your back.\r\r";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-type", "text/text; charset=UTF-8")
                .POST(BodyPublishers.ofString(body, UTF_8)).build();

        CompletableFuture<HttpResponse<Stream<String>>> cf
                = client.sendAsync(request, BodyHandlers.ofLines());
        assertNoObtrusion(cf);
        HttpResponse<Stream<String>> response = cf.join();
        Stream<String> stream = response.body();
        List<String> list = stream.collect(Collectors.toList());
        String text = list.stream().collect(Collectors.joining("|"));
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May| the wind| always be|at your back.|");
        assertEquals(list, List.of("May",
                                   " the wind",
                                   " always be",
                                   "at your back.", ""));
        assertEquals(list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testAsStreamWithMixedCRLF_UTF16(String url) {
        String body = "May\r\n the wind\r\n always be\rat your back.\r\r";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-type", "text/text; charset=UTF-16")
                .POST(BodyPublishers.ofString(body, UTF_16)).build();

        CompletableFuture<HttpResponse<Stream<String>>> cf
                = client.sendAsync(request, BodyHandlers.ofLines());
        assertNoObtrusion(cf);
        HttpResponse<Stream<String>> response = cf.join();
        Stream<String> stream = response.body();
        List<String> list = stream.collect(Collectors.toList());
        String text = list.stream().collect(Collectors.joining("|"));
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May| the wind| always be|at your back.|");
        assertEquals(list, List.of("May",
                                   " the wind",
                                   " always be",
                                   "at your back.",
                                   ""));
        assertEquals(list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testObjectWithFinisher(String url) {
        String body = "May\r\n the wind\r\n always be\rat your back.";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        ObjectSubscriber subscriber = new ObjectSubscriber();
        CompletableFuture<HttpResponse<String>> cf
                = client.sendAsync(request, BodyHandlers.fromLineSubscriber(
                        subscriber, ObjectSubscriber::get, "\r\n"));
        assertNoObtrusion(cf);
        HttpResponse<String> response = cf.join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May\n the wind\n always be\rat your back.");
        assertEquals(subscriber.list, List.of("May",
                                              " the wind",
                                              " always be\rat your back."));
        assertEquals(subscriber.list, lines(body, "\r\n"));
    }

    @Test(dataProvider = "uris")
    void testObjectWithFinisher_UTF16(String url) {
        String body = "May\r\n the wind\r\n always be\rat your back.\r\r";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-type", "text/text; charset=UTF-16")
                .POST(BodyPublishers.ofString(body, UTF_16)).build();
        ObjectSubscriber subscriber = new ObjectSubscriber();
        CompletableFuture<HttpResponse<String>> cf
                = client.sendAsync(request, BodyHandlers.fromLineSubscriber(
                        subscriber, ObjectSubscriber::get, null));
        assertNoObtrusion(cf);
        HttpResponse<String> response = cf.join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May\n the wind\n always be\nat your back.\n");
        assertEquals(subscriber.list, List.of("May",
                                              " the wind",
                                              " always be",
                                              "at your back.",
                                              ""));
        assertEquals(subscriber.list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testObjectWithoutFinisher(String url) {
        String body = "May\r\n the wind\r\n always be\rat your back.";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        ObjectSubscriber subscriber = new ObjectSubscriber();
        CompletableFuture<HttpResponse<Void>> cf
                = client.sendAsync(request,
                                   BodyHandlers.fromLineSubscriber(subscriber));
        assertNoObtrusion(cf);
        HttpResponse<Void> response = cf.join();
        String text = subscriber.get();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May\n the wind\n always be\nat your back.");
        assertEquals(subscriber.list, List.of("May",
                                              " the wind",
                                              " always be",
                                              "at your back."));
        assertEquals(subscriber.list, lines(body, null));
    }

    @Test(dataProvider = "uris")
    void testObjectWithFinisherBlocking(String url) throws Exception {
        String body = "May\r\n the wind\r\n always be\nat your back.";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        ObjectSubscriber subscriber = new ObjectSubscriber();
        HttpResponse<String> response = client.send(request,
                BodyHandlers.fromLineSubscriber(subscriber,
                                               ObjectSubscriber::get,
                                   "\r\n"));
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May\n the wind\n always be\nat your back.");
        assertEquals(subscriber.list, List.of("May",
                                              " the wind",
                                              " always be\nat your back."));
        assertEquals(subscriber.list, lines(body, "\r\n"));
    }

    @Test(dataProvider = "uris")
    void testObjectWithoutFinisherBlocking(String url) throws Exception {
        String body = "May\r\n the wind\r\n always be\nat your back.";
        HttpClient client = newClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(body))
                .build();

        ObjectSubscriber subscriber = new ObjectSubscriber();
        HttpResponse<Void> response = client.send(request,
                BodyHandlers.fromLineSubscriber(subscriber));
        String text = subscriber.get();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertTrue(text.length() != 0);  // what else can be asserted!
        assertEquals(text, "May\n the wind\n always be\nat your back.");
        assertEquals(subscriber.list, List.of("May",
                                              " the wind",
                                              " always be",
                                              "at your back."));
        assertEquals(subscriber.list, lines(body, null));
    }

    static private final String LINE = "Bient\u00f4t nous plongerons dans les" +
            " fr\u00f4\ud801\udc00des t\u00e9n\u00e8bres, ";

    static private final String bigtext() {
        StringBuilder res = new StringBuilder((LINE.length() + 1) * 50);
        for (int i = 0; i<50; i++) {
            res.append(LINE);
            if (i%2 == 0) res.append("\r\n");
        }
        return res.toString();
    }

    @Test(dataProvider = "uris")
    void testBigTextFromLineSubscriber(String url) {
        HttpClient client = newClient();
        String bigtext = bigtext();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(bigtext))
                .build();

        StringSubscriber subscriber = new StringSubscriber();
        CompletableFuture<HttpResponse<String>> cf
                = client.sendAsync(request, BodyHandlers.fromLineSubscriber(
                        subscriber, Supplier::get, "\r\n"));
        assertNoObtrusion(cf);
        HttpResponse<String> response = cf.join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, bigtext.replace("\r\n", "\n"));
        assertEquals(subscriber.list, lines(bigtext, "\r\n"));
    }

    @Test(dataProvider = "uris")
    void testBigTextAsStream(String url) {
        HttpClient client = newClient();
        String bigtext = bigtext();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(BodyPublishers.ofString(bigtext))
                .build();

        CompletableFuture<HttpResponse<Stream<String>>> cf
                = client.sendAsync(request, BodyHandlers.ofLines());
        assertNoObtrusion(cf);
        HttpResponse<Stream<String>> response = cf.join();
        Stream<String> stream = response.body();
        List<String> list = stream.collect(Collectors.toList());
        String text = list.stream().collect(Collectors.joining("|"));
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, bigtext.replace("\r\n", "|"));
        assertEquals(list, List.of(bigtext.split("\r\n")));
        assertEquals(list, lines(bigtext, null));
    }

    /** An abstract Subscriber that converts all received data into a String. */
    static abstract class AbstractSubscriber implements Supplier<String> {
        protected volatile Flow.Subscription subscription;
        protected final StringBuilder baos = new StringBuilder();
        protected volatile String text;
        protected volatile RuntimeException error;
        protected final List<Object> list = new CopyOnWriteArrayList<>();

        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }
        public void onError(Throwable throwable) {
            System.out.println(this + " onError: " + throwable);
            error = new RuntimeException(throwable);
        }
        public void onComplete() {
            System.out.println(this + " onComplete");
            text = baos.toString();
        }
        @Override public String get() {
            if (error != null) throw error;
            return text;
        }
    }

    static class StringSubscriber extends AbstractSubscriber
            implements Flow.Subscriber<String>, Supplier<String>
    {
        @Override public void onNext(String item) {
            System.out.print(this + " onNext: \"" + item + "\"");
            if (baos.length() != 0) baos.append('\n');
            baos.append(item);
            list.add(item);
        }
    }

    static class CharSequenceSubscriber extends AbstractSubscriber
            implements Flow.Subscriber<CharSequence>, Supplier<String>
    {
        @Override public void onNext(CharSequence item) {
            System.out.print(this + " onNext: " + item);
            if (baos.length() != 0) baos.append('\n');
            baos.append(item);
            list.add(item);
        }
    }

    static class ObjectSubscriber extends AbstractSubscriber
            implements Flow.Subscriber<Object>, Supplier<String>
    {
        @Override public void onNext(Object item) {
            System.out.print(this + " onNext: " + item);
            if (baos.length() != 0) baos.append('\n');
            baos.append(item);
            list.add(item);
        }
    }


    static void uncheckedWrite(ByteArrayOutputStream baos, byte[] ba) {
        try {
            baos.write(ba);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ExecutorService executorFor(String serverThreadName) {
        ThreadFactory factory = new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(serverThreadName + "#" + counter.incrementAndGet());
                return thread;
            }
        };
        return Executors.newCachedThreadPool(factory);
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0),
                executorFor("HTTP/1.1 Server Thread"));
        httpTestServer.addHandler(new HttpTestEchoHandler(), "/http1/echo");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/echo";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer,
                executorFor("HTTPS/1.1 Server Thread"));
        httpsTestServer.addHandler(new HttpTestEchoHandler(),"/https1/echo");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/echo";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new HttpTestEchoHandler(), "/http2/echo");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/echo";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new HttpTestEchoHandler(), "/https2/echo");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/echo";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static void printBytes(PrintStream out, String prefix, byte[] bytes) {
        int padding = 4 + 4 - (bytes.length % 4);
        padding = padding > 4 ? padding - 4 : 4;
        byte[] bigbytes = new byte[bytes.length + padding];
        System.arraycopy(bytes, 0, bigbytes, padding, bytes.length);
        out.println(prefix + bytes.length + " "
                    + new BigInteger(bigbytes).toString(16));
    }

    private static void assertNoObtrusion(CompletableFuture<?> cf) {
        assertThrows(UnsupportedOperationException.class,
                     () -> cf.obtrudeException(new RuntimeException()));
        assertThrows(UnsupportedOperationException.class,
                     () -> cf.obtrudeValue(null));
    }
}
