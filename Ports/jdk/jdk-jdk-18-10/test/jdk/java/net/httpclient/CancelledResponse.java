/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jdk.test.lib.net.SimpleSSLContext;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;

import static java.lang.String.format;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * @test
 * @bug 8087112
 * @library /test/lib
 * @modules java.net.http/jdk.internal.net.http.common
 * @build jdk.test.lib.net.SimpleSSLContext
 * @build MockServer ReferenceTracker
 * @run main/othervm  CancelledResponse
 * @run main/othervm  CancelledResponse SSL
 */

/**
 * Similar test to SplitResponse except that the client will cancel the response
 * before receiving it fully.
 */
public class CancelledResponse {

    static String response(String body, boolean serverKeepalive) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n");
        if (!serverKeepalive)
            sb.append("Connection: Close\r\n");

        sb.append("Content-length: ")
                .append(body.getBytes(ISO_8859_1).length)
                .append("\r\n");
        sb.append("\r\n");
        sb.append(body);
        return sb.toString();
    }

    static final String responses[] = {
        "Lorem ipsum dolor sit amet consectetur adipiscing elit,",
        "sed do eiusmod tempor quis nostrud exercitation ullamco laboris nisi ut",
        "aliquip ex ea commodo consequat."
    };

    static final ReferenceTracker TRACKER = ReferenceTracker.INSTANCE;
    final ServerSocketFactory factory;
    final SSLContext context;
    final boolean useSSL;
    CancelledResponse(boolean useSSL) throws IOException {
        this.useSSL = useSSL;
        context = new SimpleSSLContext().get();
        SSLContext.setDefault(context);
        factory = useSSL ? SSLServerSocketFactory.getDefault()
                         : ServerSocketFactory.getDefault();
    }

    public HttpClient newHttpClient() {
        HttpClient client;
        if (useSSL) {
            client = HttpClient.newBuilder()
                               .sslContext(context)
                               .build();
        } else {
            client = HttpClient.newHttpClient();
        }
        return TRACKER.track(client);
    }

    public static void main(String[] args) throws Exception {
        boolean useSSL = false;
        if (args != null && args.length == 1) {
            useSSL = "SSL".equals(args[0]);
        }
        CancelledResponse sp = new CancelledResponse(useSSL);

        Throwable failed = null;
        try {
            for (Version version : Version.values()) {
                for (boolean serverKeepalive : new boolean[]{true, false}) {
                    // Note: the mock server doesn't support Keep-Alive, but
                    // pretending that it might exercises code paths in and out of
                    // the connection pool, and retry logic
                    for (boolean async : new boolean[]{true, false}) {
                        sp.test(version, serverKeepalive, async);
                    }
                }
            }
        } catch (Exception | Error t) {
            failed = t;
            throw t;
        } finally {
            Thread.sleep(100);
            AssertionError trackFailed = TRACKER.check(500);
            if (trackFailed != null) {
                if (failed != null) {
                    failed.addSuppressed(trackFailed);
                    if (failed instanceof Error) throw (Error) failed;
                    if (failed instanceof Exception) throw (Exception) failed;
                }
                throw trackFailed;
            }
        }
    }

    static class CancelException extends IOException {
    }

    // @Test
    void test(Version version, boolean serverKeepalive, boolean async)
        throws Exception
    {
        out.println(format("*** version %s, serverKeepAlive: %s, async: %s ***",
                           version, serverKeepalive, async));
        MockServer server = new MockServer(0, factory);
        URI uri = new URI(server.getURL());
        out.println("server is: " + uri);
        server.start();

        HttpClient client = newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).version(version).build();
        try {
            for (int i = 0; i < responses.length; i++) {
                HttpResponse<String> r = null;
                CompletableFuture<HttpResponse<String>> cf1;
                CancelException expected = null;
                AtomicBoolean cancelled = new AtomicBoolean();

                out.println("----- iteration " + i + " -----");
                String body = responses[i];
                Thread t = sendSplitResponse(response(body, serverKeepalive), server, cancelled);

                try {
                    if (async) {
                        out.println("send async: " + request);
                        cf1 = client.sendAsync(request, ofString(body, cancelled));
                        r = cf1.get();
                    } else { // sync
                        out.println("send sync: " + request);
                        r = client.send(request, ofString(body, cancelled));
                    }
                } catch (CancelException c1) {
                    System.out.println("Got expected exception: " + c1);
                    expected = c1;
                } catch (IOException | ExecutionException | CompletionException c2) {
                    Throwable c = c2;
                    while (c != null && !(c instanceof CancelException)) {
                        c = c.getCause();
                    }
                    if (c instanceof CancelException) {
                        System.out.println("Got expected exception: " + c);
                        expected = (CancelException)c;
                    } else throw c2;
                }
                if (r != null) {
                    if (r.statusCode() != 200)
                        throw new RuntimeException("Failed");

                    String rxbody = r.body();
                    out.println("received " + rxbody);
                    if (!rxbody.equals(body))
                        throw new RuntimeException(format("Expected:%s, got:%s", body, rxbody));
                }
                t.join();
                conn.close();
                if (expected == null) {
                    throw new RuntimeException("Expected exception not raised for "
                            + i + " cancelled=" + cancelled.get());
                }
            }
        } finally {
            server.close();
        }
        System.out.println("OK");
    }

    static class CancellingSubscriber implements BodySubscriber<String> {
        private final String expected;
        private final CompletableFuture<String> result;
        private Flow.Subscription subscription;
        final AtomicInteger index = new AtomicInteger();
        final AtomicBoolean cancelled;
        CancellingSubscriber(String expected, AtomicBoolean cancelled) {
            this.cancelled = cancelled;
            this.expected = expected;
            result = new CompletableFuture<>();
        }

        @Override
        public CompletionStage<String> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            //if (result.isDone())
            for (ByteBuffer b : item) {
                while (b.hasRemaining() && !result.isDone()) {
                    int i = index.getAndIncrement();
                    char at = expected.charAt(i);
                    byte[] data = new byte[b.remaining()];
                    b.get(data); // we know that the server writes 1 char
                    String s = new String(data);
                    char c = s.charAt(0);
                    if (c != at) {
                        Throwable x = new IllegalStateException("char at "
                                + i + " is '" + c + "' expected '"
                                + at + "' for \"" + expected +"\"");
                        out.println("unexpected char received, cancelling");
                        subscription.cancel();
                        result.completeExceptionally(x);
                        return;
                    }
                }
            }
            if (index.get() > 0 && !result.isDone()) {
                // we should complete the result here, but let's
                // see if we get something back...
                out.println("Cancelling subscription after reading " + index.get());
                cancelled.set(true);
                subscription.cancel();
                result.completeExceptionally(new CancelException());
                return;
            }
            if (!result.isDone()) {
                out.println("requesting 1 more");
                subscription.request(1);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            int len = index.get();
            if (len == expected.length()) {
                result.complete(expected);
            } else {
                Throwable x = new IllegalStateException("received only "
                        + len + " chars, expected " + expected.length()
                        + " for \"" + expected +"\"");
                result.completeExceptionally(x);
            }
        }
    }

    static class CancellingHandler implements BodyHandler<String> {
        final String expected;
        final AtomicBoolean cancelled;
        CancellingHandler(String expected, AtomicBoolean cancelled) {
            this.expected = expected;
            this.cancelled = cancelled;
        }
        @Override
        public BodySubscriber<String> apply(HttpResponse.ResponseInfo rinfo) {
            assert !cancelled.get();
            return new CancellingSubscriber(expected, cancelled);
        }
    }

    BodyHandler<String> ofString(String expected, AtomicBoolean cancelled) {
        return new CancellingHandler(expected, cancelled);
    }

    // required for cleanup
    volatile MockServer.Connection conn;

    // Sends the response, mostly, one byte at a time with a small delay
    // between bytes, to encourage that each byte is read in a separate read
    Thread sendSplitResponse(String s, MockServer server, AtomicBoolean cancelled) {
        System.out.println("Sending: ");
        Thread t = new Thread(() -> {
            System.out.println("Waiting for server to receive headers");
            conn = server.activity();
            System.out.println("Start sending response");
            int sent = 0;
            try {
                int len = s.length();
                out.println("sending " + s);
                for (int i = 0; i < len; i++) {
                    String onechar = s.substring(i, i + 1);
                    conn.send(onechar);
                    sent++;
                    Thread.sleep(10);
                }
                out.println("sent " + s);
            } catch (SSLException | SocketException | RuntimeException x) {
                // if SSL then we might get a "Broken Pipe", or a
                // RuntimeException wrapping an InvalidAlgorithmParameterException
                // (probably if the channel is closed during the handshake),
                // otherwise we get a "Socket closed".
                boolean expected = cancelled.get();
                if (sent > 0 && expected) {
                    System.out.println("Connection closed by peer as expected: " + x);
                    return;
                } else {
                    System.out.println("Unexpected exception (sent="
                            + sent + ", cancelled=" + expected + "): " + x);
                    if (x instanceof RuntimeException) throw (RuntimeException) x;
                    throw new RuntimeException(x);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}
