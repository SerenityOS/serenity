/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222968
 * @summary ByteArrayPublisher is not thread-safe resulting in broken re-use of HttpRequests
 * @run main/othervm ByteArrayPublishers
 */

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static java.net.http.HttpRequest.BodyPublisher;
import static java.net.http.HttpRequest.BodyPublishers;

public class ByteArrayPublishers {
    private static final BodyPublisher BODY_PUBLISHER =
        BodyPublishers.ofByteArray("abcdefghijklmnopqrstuvwxyz".getBytes());

    static int LOOPS = 100;

    public static void main(String[] args) throws Exception {
        HttpServer server = null;
        try {
            InetAddress loopBack = InetAddress.getLoopbackAddress();
            String lpBackStr = loopBack.getHostAddress();
            InetSocketAddress serverAddr = new InetSocketAddress(loopBack, 0);
            server = HttpServer.create(serverAddr, 500);
            server.createContext("/", (HttpExchange e) -> {
                    e.getRequestBody().readAllBytes();
                    String response = "Hello world";
                    e.sendResponseHeaders(200, response.length());
                    e.getResponseBody().write(response.getBytes(StandardCharsets.ISO_8859_1));
                    e.close();
            });
            server.start();
            var address = server.getAddress();
            URI dest = new URI("http://" + lpBackStr + ":"
                + Integer.toString(address.getPort()) + "/");

            HttpClient client = createClient();

            ArrayList<CompletableFuture<HttpResponse<Void>>> futures = new ArrayList<>(LOOPS);
            LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
            for (int i=0;i<LOOPS;i++) {
                futures.add(
                    client.sendAsync(createRequest(dest), HttpResponse.BodyHandlers.discarding())
                          .handle((v, t) -> {
                                if (t != null)
                                    results.add(t);
                                else
                                    results.add(v);
                                return null;
                          }));
            }

            for (int i=0; i<LOOPS; i++) {
                Object o = results.take();
                if (o instanceof Exception) {
                    throw new RuntimeException((Exception)o);
                }
            }
        } finally {
            server.stop(1);
        }
    }

    private static HttpRequest createRequest(URI uri) throws URISyntaxException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .method("POST", BODY_PUBLISHER)
                .version(HttpClient.Version.HTTP_1_1);
        builder.header("content-type", "text/plain");
        return builder.build();
    }

    private static HttpClient createClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

    }
}
