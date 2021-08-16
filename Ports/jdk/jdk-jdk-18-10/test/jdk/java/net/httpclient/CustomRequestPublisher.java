/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Checks correct handling of Publishers that call onComplete without demand
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm CustomRequestPublisher
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class CustomRequestPublisher {

    SSLContext sslContext;
    HttpServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpsServer httpsTestServer;       // HTTPS/1.1
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    @DataProvider(name = "variants")
    public Object[][] variants() {
        Supplier<BodyPublisher> fixedSupplier   = () -> new FixedLengthBodyPublisher();
        Supplier<BodyPublisher> unknownSupplier = () -> new UnknownLengthBodyPublisher();

        return new Object[][]{
                { httpURI,   fixedSupplier,   false },
                { httpURI,   unknownSupplier, false },
                { httpsURI,  fixedSupplier,   false },
                { httpsURI,  unknownSupplier, false },
                { http2URI,  fixedSupplier,   false },
                { http2URI,  unknownSupplier, false },
                { https2URI, fixedSupplier,   false,},
                { https2URI, unknownSupplier, false },

                { httpURI,   fixedSupplier,   true },
                { httpURI,   unknownSupplier, true },
                { httpsURI,  fixedSupplier,   true },
                { httpsURI,  unknownSupplier, true },
                { http2URI,  fixedSupplier,   true },
                { http2URI,  unknownSupplier, true },
                { https2URI, fixedSupplier,   true,},
                { https2URI, unknownSupplier, true },
        };
    }

    static final int ITERATION_COUNT = 10;

    /** Asserts HTTP Version, and SSLSession presence when applicable. */
    static void assertVersionAndSession(HttpResponse response, String uri) {
        if (uri.contains("http2") || uri.contains("https2"))
            assertEquals(response.version(), HTTP_2);
        else if (uri.contains("http1") || uri.contains("https1"))
            assertEquals(response.version(), HTTP_1_1);
        else
            fail("Unknown HTTP version in test for: " + uri);

        Optional<SSLSession> ssl = response.sslSession();
        if (uri.contains("https")) {
            assertTrue(ssl.isPresent(),
                    "Expected optional containing SSLSession but got:" + ssl);
            try {
                ssl.get().invalidate();
                fail("SSLSession is not immutable: " + ssl.get());
            } catch (UnsupportedOperationException expected) { }
        } else {
            assertTrue(!ssl.isPresent(), "UNEXPECTED non-empty optional:" + ssl);
        }
    }

    @Test(dataProvider = "variants")
    void test(String uri, Supplier<BodyPublisher> bpSupplier, boolean sameClient)
            throws Exception
    {
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder().sslContext(sslContext).build();

            BodyPublisher bodyPublisher = bpSupplier.get();
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .POST(bodyPublisher)
                    .build();

            HttpResponse<String> resp = client.send(request, ofString());

            out.println("Got response: " + resp);
            out.println("Got body: " + resp.body());
            assertTrue(resp.statusCode() == 200,
                    "Expected 200, got:" + resp.statusCode());
            assertEquals(resp.body(), bodyPublisher.bodyAsString());

            assertVersionAndSession(resp, uri);
        }
    }

    @Test(dataProvider = "variants")
    void testAsync(String uri, Supplier<BodyPublisher> bpSupplier, boolean sameClient)
            throws Exception
    {
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder().sslContext(sslContext).build();

            BodyPublisher bodyPublisher = bpSupplier.get();
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .POST(bodyPublisher)
                    .build();

            CompletableFuture<HttpResponse<String>> cf = client.sendAsync(request, ofString());
            HttpResponse<String> resp = cf.get();

            out.println("Got response: " + resp);
            out.println("Got body: " + resp.body());
            assertTrue(resp.statusCode() == 200,
                    "Expected 200, got:" + resp.statusCode());
            assertEquals(resp.body(), bodyPublisher.bodyAsString());

            assertVersionAndSession(resp, uri);
        }
    }

    /** A Publisher that returns an UNKNOWN content length. */
    static class UnknownLengthBodyPublisher extends BodyPublisher {
        @Override
        public long contentLength() {
            return -1;  // unknown
        }
    }

    /** A Publisher that returns a FIXED content length. */
    static class FixedLengthBodyPublisher extends BodyPublisher {
        final int LENGTH = Arrays.stream(BODY)
                .mapToInt(s-> s.getBytes(US_ASCII).length)
                .sum();
        @Override
        public long contentLength() {
            return LENGTH;
        }
    }

    /**
     * A Publisher that ( quite correctly ) invokes onComplete, after the last
     * item has been published, even without any outstanding demand.
     */
    static abstract class BodyPublisher implements HttpRequest.BodyPublisher {

        String[] BODY = new String[]
                { "Say ", "Hello ", "To ", "My ", "Little ", "Friend" };

        protected volatile Flow.Subscriber subscriber;

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new InternalSubscription());
        }

        @Override
        public abstract long contentLength();

        String bodyAsString() {
            return Arrays.stream(BODY).collect(Collectors.joining());
        }

        class InternalSubscription implements Flow.Subscription {

            private final AtomicLong demand = new AtomicLong();
            private final AtomicBoolean cancelled = new AtomicBoolean();
            private volatile int position;

            private static final int IDLE    =  1;
            private static final int PUSHING =  2;
            private static final int AGAIN   =  4;
            private final AtomicInteger state = new AtomicInteger(IDLE);

            @Override
            public void request(long n) {
                if (n <= 0L) {
                    subscriber.onError(new IllegalArgumentException(
                            "non-positive subscription request"));
                    return;
                }
                if (cancelled.get()) {
                    return;
                }

                while (true) {
                    long prev = demand.get(), d;
                    if ((d = prev + n) < prev) // saturate
                        d = Long.MAX_VALUE;
                    if (demand.compareAndSet(prev, d))
                        break;
                }

                while (true) {
                    int s = state.get();
                    if (s == IDLE) {
                        if (state.compareAndSet(IDLE, PUSHING)) {
                            while (true) {
                                push();
                                if (state.compareAndSet(PUSHING, IDLE))
                                    return;
                                else if (state.compareAndSet(AGAIN, PUSHING))
                                    continue;
                            }
                        }
                    } else if (s == PUSHING) {
                        if (state.compareAndSet(PUSHING, AGAIN))
                            return;
                    } else if (s == AGAIN){
                        // do nothing, the pusher will already rerun
                        return;
                    } else {
                        throw new AssertionError("Unknown state:" + s);
                    }
                }
            }

            private void push() {
                long prev;
                while ((prev = demand.get()) > 0) {
                    if (!demand.compareAndSet(prev, prev -1))
                        continue;

                    int index = position;
                    if (index < BODY.length) {
                        position++;
                        subscriber.onNext(ByteBuffer.wrap(BODY[index].getBytes(US_ASCII)));
                    }
                }

                if (position == BODY.length && !cancelled.get()) {
                    cancelled.set(true);
                    subscriber.onComplete();  // NOTE: onComplete without demand
                }
            }

            @Override
            public void cancel() {
                if (cancelled.compareAndExchange(false, true))
                    return;  // already cancelled
            }
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

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpServer.create(sa, 0);
        httpTestServer.createContext("/http1/echo", new Http1EchoHandler());
        httpURI = "http://" + serverAuthority(httpTestServer) + "/http1/echo";

        httpsTestServer = HttpsServer.create(sa, 0);
        httpsTestServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer.createContext("/https1/echo", new Http1EchoHandler());
        httpsURI = "https://" + serverAuthority(httpsTestServer) + "/https1/echo";

        http2TestServer = new Http2TestServer("localhost", false, 0);
        http2TestServer.addHandler(new Http2EchoHandler(), "/http2/echo");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/echo";

        https2TestServer = new Http2TestServer("localhost", true, sslContext);
        https2TestServer.addHandler(new Http2EchoHandler(), "/https2/echo");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/echo";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop(0);
        httpsTestServer.stop(0);
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static class Http1EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    static class Http2EchoHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }
}
