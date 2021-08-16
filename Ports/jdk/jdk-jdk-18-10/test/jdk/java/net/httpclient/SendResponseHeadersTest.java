/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8253005
 * @library /test/lib
 * @summary Check that sendResponseHeaders throws an IOException when headers
 *  have already been sent
 * @run testng/othervm SendResponseHeadersTest
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.http.HttpClient.Builder.NO_PROXY;
import static org.testng.Assert.expectThrows;
import static org.testng.Assert.fail;

public class SendResponseHeadersTest {
    URI uri;
    HttpServer server;
    ExecutorService executor;

    @BeforeTest
    public void setUp() throws IOException, URISyntaxException {
        var loopback = InetAddress.getLoopbackAddress();
        var addr = new InetSocketAddress(loopback, 0);
        server = HttpServer.create(addr, 0);
        server.createContext("/test", new TestHandler());
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();

        uri = URIBuilder.newBuilder()
                .scheme("http")
                .host(loopback)
                .port(server.getAddress().getPort())
                .path("/test/foo.html")
                .build();
    }

    @Test
    public void testSend() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .proxy(NO_PROXY)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        // verify empty response received, otherwise an error has occurred
        if (!response.body().isEmpty())
            fail(response.body());
    }

    @AfterTest
    public void tearDown() {
        server.stop(0);
        executor.shutdown();
    }

    static class TestHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody();
                 OutputStream os = exchange.getResponseBody()) {

                is.readAllBytes();
                exchange.sendResponseHeaders(200, 0);
                try {
                    IOException io = expectThrows(IOException.class,
                            () -> exchange.sendResponseHeaders(200, 0));
                    System.out.println("Got expected exception: " + io);
                } catch (Throwable t) {
                    // expectThrows triggered an assertion, return error message to the client
                    t.printStackTrace();
                    os.write(("Unexpected error: " + t).getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
}
