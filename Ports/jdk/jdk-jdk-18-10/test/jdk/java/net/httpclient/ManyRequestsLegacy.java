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

/*
 * @test
 * @modules java.net.http
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @compile ../../../com/sun/net/httpserver/LogFilter.java
 * @compile ../../../com/sun/net/httpserver/EchoHandler.java
 * @compile ../../../com/sun/net/httpserver/FileServerHandler.java
 * @run main/othervm/timeout=40 ManyRequestsLegacy
 * @run main/othervm/timeout=40 -Dtest.insertDelay=true ManyRequestsLegacy
 * @run main/othervm/timeout=40 -Dtest.chunkSize=64 ManyRequestsLegacy
 * @run main/othervm/timeout=40 -Dtest.insertDelay=true
 *                              -Dtest.chunkSize=64 ManyRequestsLegacy
 * @summary Send a large number of requests asynchronously using the legacy
 *          URL.openConnection(), to help sanitize results of the test
 *          ManyRequest.java.
 */

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;
import jdk.test.lib.net.SimpleSSLContext;
import static java.net.Proxy.NO_PROXY;

public class ManyRequestsLegacy {

    volatile static int counter = 0;

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger("com.sun.net.httpserver");
        logger.setLevel(Level.ALL);
        logger.info("TEST");
        System.out.println("Sending " + REQUESTS
                         + " requests; delay=" + INSERT_DELAY
                         + ", chunks=" + CHUNK_SIZE
                         + ", XFixed=" + XFIXED);
        SSLContext ctx = new SimpleSSLContext().get();
        SSLContext.setDefault(ctx);
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        HttpsServer server = HttpsServer.create(addr, 0);
        server.setHttpsConfigurator(new Configurator(ctx));

        LegacyHttpClient client = new LegacyHttpClient();

        try {
            test(server, client);
            System.out.println("OK");
        } finally {
            server.stop(0);
        }
    }

    //static final int REQUESTS = 1000;
    static final int REQUESTS = 20;
    static final boolean INSERT_DELAY = Boolean.getBoolean("test.insertDelay");
    static final int CHUNK_SIZE = Math.max(0,
           Integer.parseInt(System.getProperty("test.chunkSize", "0")));
    static final boolean XFIXED = Boolean.getBoolean("test.XFixed");

    static class LegacyHttpClient {
        static final class LegacyHttpResponse implements HttpResponse<byte[]> {
            final HttpRequest request;
            final byte[] response;
            final int statusCode;
            public LegacyHttpResponse(HttpRequest request, int statusCode, byte[] response) {
                this.request = request;
                this.statusCode = statusCode;
                this.response = response;
            }
            private <T> T error() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override
            public int statusCode() { return statusCode;}
            @Override
            public HttpRequest request() {return request;}
            @Override
            public Optional<HttpResponse<byte[]>> previousResponse() {return Optional.empty();}
            @Override
            public HttpHeaders headers() { return error(); }
            @Override
            public byte[] body() {return response;}
            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty(); // for now
            }
            @Override
            public URI uri() { return request.uri();}
            @Override
            public HttpClient.Version version() { return Version.HTTP_1_1;}
        }

        private void debugCompleted(String tag, long startNanos, HttpRequest req) {
            System.err.println(tag + " elapsed "
                    + (System.nanoTime() - startNanos)/1000_000L
                    + " millis for " + req.method()
                    + " to " + req.uri());
        }

        CompletableFuture<? extends HttpResponse<byte[]>> sendAsync(HttpRequest r, byte[] buf) {
            long start = System.nanoTime();
            try {
                CompletableFuture<LegacyHttpResponse> cf = new CompletableFuture<>();
                URLConnection urlc = r.uri().toURL().openConnection(NO_PROXY);
                HttpURLConnection httpc = (HttpURLConnection)urlc;
                httpc.setRequestMethod(r.method());
                for (String s : r.headers().map().keySet()) {
                    httpc.setRequestProperty(s, r.headers().allValues(s)
                        .stream().collect(Collectors.joining(",")));
                }
                httpc.setDoInput(true);
                if (buf != null) httpc.setDoOutput(true);
                Thread t = new Thread(() -> {
                    try {
                        if (buf != null) {
                            try (OutputStream os = httpc.getOutputStream()) {
                                os.write(buf);
                                os.flush();
                            }
                        }
                        LegacyHttpResponse response = new LegacyHttpResponse(r,
                                httpc.getResponseCode(),httpc.getInputStream().readAllBytes());
                        cf.complete(response);
                    } catch(Throwable x) {
                        cf.completeExceptionally(x);
                    }
                });
                t.start();
                return cf.whenComplete((b,x) -> debugCompleted("ClientImpl (async)", start, r));
            } catch(Throwable t) {
                debugCompleted("ClientImpl (async)", start, r);
                return CompletableFuture.failedFuture(t);
            }
        }
    }

    static class TestEchoHandler extends EchoHandler {
        final Random rand = new Random();
        @Override
        public void handle(HttpExchange e) throws IOException {
            System.out.println("Server: received " + e.getRequestURI());
            super.handle(e);
        }
        @Override
        protected void close(HttpExchange t, OutputStream os) throws IOException {
            if (INSERT_DELAY) {
                try { Thread.sleep(rand.nextInt(200)); }
                catch (InterruptedException e) {}
            }
            System.out.println("Server: close outbound: " + t.getRequestURI());
            os.close();
        }
        @Override
        protected void close(HttpExchange t, InputStream is) throws IOException {
            if (INSERT_DELAY) {
                try { Thread.sleep(rand.nextInt(200)); }
                catch (InterruptedException e) {}
            }
            System.out.println("Server: close inbound: " + t.getRequestURI());
            is.close();
        }
    }

    static void test(HttpsServer server, LegacyHttpClient client) throws Exception {
        int port = server.getAddress().getPort();
        URI baseURI = new URI("https://localhost:" + port + "/foo/x");
        server.createContext("/foo", new TestEchoHandler());
        server.start();

        RequestLimiter limiter = new RequestLimiter(40);
        Random rand = new Random();
        CompletableFuture<?>[] results = new CompletableFuture<?>[REQUESTS];
        HashMap<HttpRequest,byte[]> bodies = new HashMap<>();

        for (int i=0; i<REQUESTS; i++) {
            byte[] buf = new byte[(i+1)*CHUNK_SIZE+i+1];  // different size bodies
            rand.nextBytes(buf);
            URI uri = new URI(baseURI.toString() + String.valueOf(i+1));
            HttpRequest r = HttpRequest.newBuilder(uri)
                                       .header("XFixed", "true")
                                       .POST(BodyPublishers.ofByteArray(buf))
                                       .build();
            bodies.put(r, buf);

            results[i] =
                limiter.whenOkToSend()
                       .thenCompose((v) -> {
                           System.out.println("Client: sendAsync: " + r.uri());
                           return client.sendAsync(r, buf);
                       })
                       .thenCompose((resp) -> {
                           limiter.requestComplete();
                           if (resp.statusCode() != 200) {
                               String s = "Expected 200, got: " + resp.statusCode();
                               System.out.println(s + " from "
                                                  + resp.request().uri().getPath());
                               return completedWithIOException(s);
                           } else {
                               counter++;
                               System.out.println("Result (" + counter + ") from "
                                                   + resp.request().uri().getPath());
                           }
                           return CompletableFuture.completedStage(resp.body())
                                      .thenApply((b) -> new Pair<>(resp, b));
                       })
                      .thenAccept((pair) -> {
                          HttpRequest request = pair.t.request();
                          byte[] requestBody = bodies.get(request);
                          check(Arrays.equals(requestBody, pair.u),
                                "bodies not equal:[" + bytesToHexString(requestBody)
                                + "] [" + bytesToHexString(pair.u) + "]");

                      });
        }

        // wait for them all to complete and throw exception in case of error
        CompletableFuture.allOf(results).join();
    }

    static <T> CompletableFuture<T> completedWithIOException(String message) {
        return CompletableFuture.failedFuture(new IOException(message));
    }

    static String bytesToHexString(byte[] bytes) {
        if (bytes == null)
            return "null";

        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return sb.toString();
    }

    static final class Pair<T,U> {
        Pair(T t, U u) {
            this.t = t; this.u = u;
        }
        T t;
        U u;
    }

    /**
     * A simple limiter for controlling the number of requests to be run in
     * parallel whenOkToSend() is called which returns a CF<Void> that allows
     * each individual request to proceed, or block temporarily (blocking occurs
     * on the waiters list here. As each request actually completes
     * requestComplete() is called to notify this object, and allow some
     * requests to continue.
     */
    static class RequestLimiter {

        static final CompletableFuture<Void> COMPLETED_FUTURE =
                CompletableFuture.completedFuture(null);

        final int maxnumber;
        final LinkedList<CompletableFuture<Void>> waiters;
        int number;
        boolean blocked;

        RequestLimiter(int maximum) {
            waiters = new LinkedList<>();
            maxnumber = maximum;
        }

        synchronized void requestComplete() {
            number--;
            // don't unblock until number of requests has halved.
            if ((blocked && number <= maxnumber / 2) ||
                        (!blocked && waiters.size() > 0)) {
                int toRelease = Math.min(maxnumber - number, waiters.size());
                for (int i=0; i<toRelease; i++) {
                    CompletableFuture<Void> f = waiters.remove();
                    number ++;
                    f.complete(null);
                }
                blocked = number >= maxnumber;
            }
        }

        synchronized CompletableFuture<Void> whenOkToSend() {
            if (blocked || number + 1 >= maxnumber) {
                blocked = true;
                CompletableFuture<Void> r = new CompletableFuture<>();
                waiters.add(r);
                return r;
            } else {
                number++;
                return COMPLETED_FUTURE;
            }
        }
    }

    static void check(boolean cond, Object... msg) {
        if (cond)
            return;
        StringBuilder sb = new StringBuilder();
        for (Object o : msg)
            sb.append(o);
        throw new RuntimeException(sb.toString());
    }

    static class Configurator extends HttpsConfigurator {
        public Configurator(SSLContext ctx) {
            super(ctx);
        }

        public void configure(HttpsParameters params) {
            params.setSSLParameters(getSSLContext().getSupportedSSLParameters());
        }
    }
}
