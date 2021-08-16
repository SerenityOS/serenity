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
 * @library /test/lib server
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm
 *       -Djdk.internal.httpclient.debug=true
 *       -Djdk.httpclient.HttpClient.log=errors,requests,responses
 *       ServerPushWithDiffTypes
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiPredicate;

import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class ServerPushWithDiffTypes {

    static Map<String,String> PUSH_PROMISES = Map.of(
            "/x/y/z/1", "the first push promise body",
            "/x/y/z/2", "the second push promise body",
            "/x/y/z/3", "the third push promise body",
            "/x/y/z/4", "the fourth push promise body",
            "/x/y/z/5", "the fifth push promise body",
            "/x/y/z/6", "the sixth push promise body",
            "/x/y/z/7", "the seventh push promise body",
            "/x/y/z/8", "the eighth push promise body",
            "/x/y/z/9", "the ninth push promise body"
    );

    @Test
    public static void test() throws Exception {
        Http2TestServer server = null;
        try {
            server = new Http2TestServer(false, 0);
            Http2Handler handler =
                    new ServerPushHandler("the main response body",
                                          PUSH_PROMISES);
            server.addHandler(handler, "/");
            server.start();
            int port = server.getAddress().getPort();
            System.err.println("Server listening on port " + port);

            HttpClient client = HttpClient.newHttpClient();
            // use multi-level path
            URI uri = new URI("http://localhost:" + port + "/foo/a/b/c");
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

            ConcurrentMap<HttpRequest,CompletableFuture<HttpResponse<BodyAndType<?>>>>
                    results = new ConcurrentHashMap<>();
            PushPromiseHandler<BodyAndType<?>> bh = PushPromiseHandler.of(
                    (pushRequest) -> new BodyAndTypeHandler(pushRequest), results);

            CompletableFuture<HttpResponse<BodyAndType<?>>> cf =
                    client.sendAsync(request, new BodyAndTypeHandler(request), bh);
            results.put(request, cf);
            cf.join();

            assertEquals(results.size(), PUSH_PROMISES.size() + 1);

            for (HttpRequest r : results.keySet()) {
                URI u = r.uri();
                BodyAndType<?> body = results.get(r).get().body();
                String result;
                // convert all body types to String for easier comparison
                if (body.type() == String.class) {
                    result = (String)body.getBody();
                } else if (body.type() == byte[].class) {
                    byte[] bytes = (byte[])body.getBody();
                    result = new String(bytes, UTF_8);
                } else if (Path.class.isAssignableFrom(body.type())) {
                    Path path = (Path)body.getBody();
                    result = new String(Files.readAllBytes(path), UTF_8);
                } else {
                    throw new AssertionError("Unknown:" + body.type());
                }

                System.err.printf("%s -> %s\n", u.toString(), result.toString());
                String expected = PUSH_PROMISES.get(r.uri().getPath());
                if (expected == null)
                    expected = "the main response body";
                assertEquals(result, expected);
            }
        } finally {
            server.stop();
        }
    }

    interface BodyAndType<T> {
        Class<T> type();
        T getBody();
    }

    static final Path WORK_DIR = Paths.get(".");

    static class BodyAndTypeHandler implements BodyHandler<BodyAndType<?>> {
        int count;
        final HttpRequest request;

        BodyAndTypeHandler(HttpRequest request) {
            this.request = request;
        }

        @Override
        public HttpResponse.BodySubscriber<BodyAndType<?>> apply(HttpResponse.ResponseInfo info) {
            int whichType = count++ % 3;  // real world may base this on the request metadata
            switch (whichType) {
                case 0: // String
                    return new BodyAndTypeSubscriber(BodySubscribers.ofString(UTF_8));
                case 1: // byte[]
                    return new BodyAndTypeSubscriber(BodySubscribers.ofByteArray());
                case 2: // Path
                    URI u = request.uri();
                    Path path = Paths.get(WORK_DIR.toString(), u.getPath());
                    try {
                        Files.createDirectories(path.getParent());
                    } catch (IOException ee) {
                        throw new UncheckedIOException(ee);
                    }
                    return new BodyAndTypeSubscriber(BodySubscribers.ofFile(path));
                default:
                    throw new AssertionError("Unexpected " + whichType);
            }
        }
    }

    static class BodyAndTypeSubscriber<T>
        implements HttpResponse.BodySubscriber<BodyAndType<T>>
    {
        private static class BodyAndTypeImpl<T> implements BodyAndType<T> {
            private final Class<T> type;
            private final T body;
            public BodyAndTypeImpl(Class<T> type, T body) { this.type = type; this.body = body; }
            @Override public Class<T> type() { return type; }
            @Override public T getBody() { return body; }
        }

        private final BodySubscriber<?> bodySubscriber;
        private final CompletableFuture<BodyAndType<T>> cf;

        BodyAndTypeSubscriber(BodySubscriber bodySubscriber) {
            this.bodySubscriber = bodySubscriber;
            cf = new CompletableFuture<>();
            bodySubscriber.getBody().whenComplete(
                    (r,t) -> cf.complete(new BodyAndTypeImpl(r.getClass(), r)));
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            bodySubscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            bodySubscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            bodySubscriber.onError(throwable);
            cf.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            bodySubscriber.onComplete();
        }

        @Override
        public CompletionStage<BodyAndType<T>> getBody() {
            return cf;
        }
    }

    // --- server push handler ---
    static class ServerPushHandler implements Http2Handler {

        private final String mainResponseBody;
        private final Map<String,String> promises;

        public ServerPushHandler(String mainResponseBody,
                                 Map<String,String> promises)
            throws Exception
        {
            Objects.requireNonNull(promises);
            this.mainResponseBody = mainResponseBody;
            this.promises = promises;
        }

        public void handle(Http2TestExchange exchange) throws IOException {
            System.err.println("Server: handle " + exchange);
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }

            if (exchange.serverPushAllowed()) {
                pushPromises(exchange);
            }

            // response data for the main response
            try (OutputStream os = exchange.getResponseBody()) {
                byte[] bytes = mainResponseBody.getBytes(UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }

        static final BiPredicate<String,String> ACCEPT_ALL = (x, y) -> true;

        private void pushPromises(Http2TestExchange exchange) throws IOException {
            URI requestURI = exchange.getRequestURI();
            for (Map.Entry<String,String> promise : promises.entrySet()) {
                URI uri = requestURI.resolve(promise.getKey());
                InputStream is = new ByteArrayInputStream(promise.getValue().getBytes(UTF_8));
                Map<String,List<String>> map = Map.of("X-Promise", List.of(promise.getKey()));
                HttpHeaders headers = HttpHeaders.of(map, ACCEPT_ALL);
                // TODO: add some check on headers, maybe
                exchange.serverPush(uri, headers, is);
            }
            System.err.println("Server: All pushes sent");
        }
    }
}
