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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import javax.net.ssl.SSLContext;
import static java.util.stream.Collectors.joining;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.net.http.HttpRequest.BodyPublishers.fromPublisher;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/*
 * @test
 * @summary Basic tests for Flow adapter Publishers
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm FlowAdapterPublisherTest
 */

public class FlowAdapterPublisherTest {

    SSLContext sslContext;
    HttpServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpsServer httpsTestServer;       // HTTPS/1.1
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
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
    public void testAPIExceptions() {
        assertThrows(NPE, () -> fromPublisher(null));
        assertThrows(NPE, () -> fromPublisher(null, 1));
        assertThrows(IAE, () -> fromPublisher(new BBPublisher(), 0));
        assertThrows(IAE, () -> fromPublisher(new BBPublisher(), -1));
        assertThrows(IAE, () -> fromPublisher(new BBPublisher(), Long.MIN_VALUE));

        Publisher publisher = fromPublisher(new BBPublisher());
        assertThrows(NPE, () -> publisher.subscribe(null));
    }

    //  Flow.Publisher<ByteBuffer>

    @Test(dataProvider = "uris")
    void testByteBufferPublisherUnknownLength(String url) {
        String[] body = new String[] { "You know ", "it's summer ", "in Ireland ",
                "when the ", "rain gets ", "warmer." };
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(fromPublisher(new BBPublisher(body))).build();

        HttpResponse<String> response = client.sendAsync(request, ofString(UTF_8)).join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, Arrays.stream(body).collect(joining()));
    }

    @Test(dataProvider = "uris")
    void testByteBufferPublisherFixedLength(String url) {
        String[] body = new String[] { "You know ", "it's summer ", "in Ireland ",
                "when the ", "rain gets ", "warmer." };
        int cl = Arrays.stream(body).mapToInt(String::length).sum();
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(fromPublisher(new BBPublisher(body), cl)).build();

        HttpResponse<String> response = client.sendAsync(request, ofString(UTF_8)).join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, Arrays.stream(body).collect(joining()));
    }

    // Flow.Publisher<MappedByteBuffer>

    @Test(dataProvider = "uris")
    void testMappedByteBufferPublisherUnknownLength(String url) {
        String[] body = new String[] { "God invented ", "whiskey to ", "keep the ",
                "Irish from ", "ruling the ", "world." };
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(fromPublisher(new MBBPublisher(body))).build();

        HttpResponse<String> response = client.sendAsync(request, ofString(UTF_8)).join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, Arrays.stream(body).collect(joining()));
    }

    @Test(dataProvider = "uris")
    void testMappedByteBufferPublisherFixedLength(String url) {
        String[] body = new String[] { "God invented ", "whiskey to ", "keep the ",
                "Irish from ", "ruling the ", "world." };
        int cl = Arrays.stream(body).mapToInt(String::length).sum();
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(fromPublisher(new MBBPublisher(body), cl)).build();

        HttpResponse<String> response = client.sendAsync(request, ofString(UTF_8)).join();
        String text = response.body();
        System.out.println(text);
        assertEquals(response.statusCode(), 200);
        assertEquals(text, Arrays.stream(body).collect(joining()));
    }

    // The following two tests depend on Exception detail messages, which is
    // not ideal, but necessary to discern correct behavior. They should be
    // updated if the exception message is updated.

    @Test(dataProvider = "uris")
    void testPublishTooFew(String url) throws InterruptedException {
        String[] body = new String[] { "You know ", "it's summer ", "in Ireland ",
                "when the ", "rain gets ", "warmer." };
        int cl = Arrays.stream(body).mapToInt(String::length).sum() + 1; // length + 1
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(fromPublisher(new BBPublisher(body), cl)).build();

        try {
            HttpResponse<String> response = client.send(request, ofString(UTF_8));
            fail("Unexpected response: " + response);
        } catch (IOException expected) {
            assertMessage(expected, "Too few bytes returned");
        }
    }

    @Test(dataProvider = "uris")
    void testPublishTooMany(String url) throws InterruptedException {
        String[] body = new String[] { "You know ", "it's summer ", "in Ireland ",
                "when the ", "rain gets ", "warmer." };
        int cl = Arrays.stream(body).mapToInt(String::length).sum() - 1; // length - 1
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(fromPublisher(new BBPublisher(body), cl)).build();

        try {
            HttpResponse<String> response = client.send(request, ofString(UTF_8));
            fail("Unexpected response: " + response);
        } catch (IOException expected) {
            assertMessage(expected, "Too many bytes in request body");
        }
    }

    private void assertMessage(Throwable t, String contains) {
        if (!t.getMessage().contains(contains)) {
            String error = "Exception message:[" + t.toString() + "] doesn't contain [" + contains + "]";
            throw new AssertionError(error, t);
        }
    }

    static class BBPublisher extends AbstractPublisher
        implements Flow.Publisher<ByteBuffer>
    {
        BBPublisher(String... bodyParts) { super(bodyParts); }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new InternalSubscription());
        }
    }

    static class MBBPublisher extends AbstractPublisher
        implements Flow.Publisher<MappedByteBuffer>
    {
        MBBPublisher(String... bodyParts) { super(bodyParts); }

        @Override
        public void subscribe(Flow.Subscriber<? super MappedByteBuffer> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new InternalSubscription());
        }
    }

    static abstract class AbstractPublisher {
        private final String[] bodyParts;
        protected volatile Flow.Subscriber subscriber;

        AbstractPublisher(String... bodyParts) {
            this.bodyParts = bodyParts;
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
                    if (index < bodyParts.length) {
                        position++;
                        subscriber.onNext(ByteBuffer.wrap(bodyParts[index].getBytes(UTF_8)));
                    }
                }

                if (position == bodyParts.length && !cancelled.get()) {
                    cancelled.set(true);
                    subscriber.onComplete();
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

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(),0);
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
