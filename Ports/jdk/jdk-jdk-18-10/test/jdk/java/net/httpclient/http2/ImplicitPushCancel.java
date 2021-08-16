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
 *      -Djdk.internal.httpclient.debug=true
 *      -Djdk.httpclient.HttpClient.log=errors,requests,responses,trace
 *      ImplicitPushCancel
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class ImplicitPushCancel {

    static Map<String,String> PUSH_PROMISES = Map.of(
            "/x/y/z/1", "the first push promise body",
            "/x/y/z/2", "the second push promise body",
            "/x/y/z/3", "the third push promise body",
            "/x/y/z/4", "the fourth push promise body",
            "/x/y/z/5", "the fifth push promise body",
            "/x/y/z/6", "the sixth push promise body",
            "/x/y/z/7", "the seventh push promise body",
            "/x/y/z/8", "the eight push promise body",
            "/x/y/z/9", "the ninth push promise body"
    );
    static final String MAIN_RESPONSE_BODY = "the main response body";

    Http2TestServer server;
    URI uri;

    @BeforeTest
    public void setup() throws Exception {
        server = new Http2TestServer(false, 0);
        Http2Handler handler = new ServerPushHandler(MAIN_RESPONSE_BODY,
                                                     PUSH_PROMISES);
        server.addHandler(handler, "/");
        server.start();
        int port = server.getAddress().getPort();
        System.err.println("Server listening on port " + port);
        uri = new URI("http://localhost:" + port + "/foo/a/b/c");
    }

    @AfterTest
    public void teardown() {
        server.stop();
    }

    static final <T> HttpResponse<T> assert200ResponseCode(HttpResponse<T> response) {
        assertEquals(response.statusCode(), 200);
        return response;
    }

    /*
     * With a handler not capable of accepting push promises, then all push
     * promises should be rejected / cancelled, without interfering with the
     * main response.
     */
    @Test
    public void test() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        client.sendAsync(HttpRequest.newBuilder(uri).build(), BodyHandlers.ofString())
                .thenApply(ImplicitPushCancel::assert200ResponseCode)
                .thenApply(HttpResponse::body)
                .thenAccept(body -> body.equals(MAIN_RESPONSE_BODY))
                .join();

        ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<String>>> promises
                = new ConcurrentHashMap<>();
        PushPromiseHandler<String> pph = PushPromiseHandler
                .of((r) -> BodyHandlers.ofString(), promises);
        HttpResponse<String> main = client.sendAsync(
                HttpRequest.newBuilder(uri).build(),
                BodyHandlers.ofString(),
                pph)
                .join();

        promises.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ":" + e.getValue().join().body()));

        promises.putIfAbsent(main.request(), CompletableFuture.completedFuture(main));
        promises.entrySet().stream().forEach(entry -> {
            HttpRequest request = entry.getKey();
            HttpResponse<String> response = entry.getValue().join();
            assertEquals(response.statusCode(), 200);
            if (PUSH_PROMISES.containsKey(request.uri().getPath())) {
                assertEquals(response.body(), PUSH_PROMISES.get(request.uri().getPath()));
            } else {
                assertEquals(response.body(), MAIN_RESPONSE_BODY);
            }

        } );
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

        private void pushPromises(Http2TestExchange exchange) throws IOException {
            URI requestURI = exchange.getRequestURI();
            for (Map.Entry<String,String> promise : promises.entrySet()) {
                URI uri = requestURI.resolve(promise.getKey());
                InputStream is = new ByteArrayInputStream(promise.getValue().getBytes(UTF_8));
                HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (x, y) -> true);
                exchange.serverPush(uri, headers, is);
            }
            System.err.println("Server: All pushes sent");
        }
    }
}

