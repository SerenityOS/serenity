/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8267262
 * @summary Tests for Filter static factory methods
 * @run testng/othervm FilterTest
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import static org.testng.Assert.*;

public class FilterTest {

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IOException> IOE = IOException.class;

    static final InetAddress LOOPBACK_ADDR = InetAddress.getLoopbackAddress();
    static final boolean ENABLE_LOGGING = true;
    static final Logger logger = Logger.getLogger("com.sun.net.httpserver");

    @BeforeTest
    public void setup() {
        if (ENABLE_LOGGING) {
            ConsoleHandler ch = new ConsoleHandler();
            logger.setLevel(Level.ALL);
            ch.setLevel(Level.ALL);
            logger.addHandler(ch);
        }
    }

    @Test
    public void testNull() {
        expectThrows(NPE, () -> Filter.beforeHandler(null, e -> e.getResponseHeaders().set("X-Foo", "Bar")));
        expectThrows(NPE, () -> Filter.beforeHandler("Some description", null));

        expectThrows(NPE, () -> Filter.afterHandler("Some description", null));
        expectThrows(NPE, () -> Filter.afterHandler(null, HttpExchange::getResponseCode));
    }

    @Test
    public void testDescription() {
        var desc = "Some description";

        var beforeFilter = Filter.beforeHandler(desc, HttpExchange::getRequestBody);
        assertEquals(desc, beforeFilter.description());

        var afterFilter = Filter.afterHandler(desc, HttpExchange::getResponseCode);
        assertEquals(desc, afterFilter.description());
    }

    @DataProvider
    public static Object[][] throwingFilters() {
        return new Object[][] {
            {Filter.beforeHandler("before RE", e -> { throw new RuntimeException(); }), IOE},
            {Filter.beforeHandler("before AE", e -> { throw new AssertionError();   }), IOE},

            {Filter.afterHandler( "after RE",  e -> { throw new RuntimeException(); }), null},
            {Filter.afterHandler( "after AE",  e -> { throw new AssertionError();   }), null},
        };
    }

    @Test(dataProvider = "throwingFilters")
    public void testException(Filter filter, Class<Exception> exception)
            throws Exception
    {
        var handler = new EchoHandler();
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR,0), 10);
        server.createContext("/", handler).getFilters().add(filter);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            if (exception != null) {
                expectThrows(exception, () -> client.send(request, HttpResponse.BodyHandlers.ofString()));
            } else {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(response.statusCode(), 200);
                assertEquals(response.body(), "hello world");
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBeforeHandler() throws Exception {
        var handler = new EchoHandler();
        var filter = Filter.beforeHandler("Add x-foo response header",
                e -> e.getResponseHeaders().set("x-foo", "bar"));
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR,0), 10);
        server.createContext("/", handler).getFilters().add(filter);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            assertEquals(response.headers().map().size(), 3);
            assertEquals(response.headers().firstValue("x-foo").orElseThrow(), "bar");
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBeforeHandlerRepeated() throws Exception {
        var handler = new EchoHandler();
        var filter1 = Filter.beforeHandler("Add x-foo response header",
                e -> e.getResponseHeaders().set("x-foo", "bar"));
        var filter2 = Filter.beforeHandler("Update x-foo response header",
                e -> e.getResponseHeaders().set("x-foo", "barbar"));
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR, 0), 10);
        var context = server.createContext("/", handler);
        context.getFilters().add(filter1);
        context.getFilters().add(filter2);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            assertEquals(response.headers().map().size(), 3);
            assertEquals(response.headers().firstValue("x-foo").orElseThrow(), "barbar");
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBeforeHandlerSendResponse() throws Exception {
        var handler = new NoResponseHandler();
        var filter = Filter.beforeHandler("Add x-foo response header and send response",
                e -> {
                    try (InputStream is = e.getRequestBody();
                         OutputStream os = e.getResponseBody()) {
                        is.readAllBytes();
                        e.getResponseHeaders().set("x-foo", "bar");
                        var resp = "hello world".getBytes(StandardCharsets.UTF_8);
                        e.sendResponseHeaders(200, resp.length);
                        os.write(resp);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                        throw new UncheckedIOException(ioe);
                    }
                });
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR, 0), 10);
        server.createContext("/", handler).getFilters().add(filter);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            assertEquals(response.headers().map().size(), 3);
            assertEquals(response.headers().firstValue("x-foo").orElseThrow(), "bar");
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testAfterHandler() throws Exception {
        var handler = new EchoHandler();
        var respCode = new CompletableFuture<Integer>();
        var filter = Filter.afterHandler("Log response code",
                e -> respCode.complete(e.getResponseCode()));
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR, 0), 10);
        server.createContext("/", handler).getFilters().add(filter);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            assertEquals(response.statusCode(), (int)respCode.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testAfterHandlerRepeated() throws Exception {
        var handler = new EchoHandler();
        var attr = new CompletableFuture<String>();
        final var value = "some value";
        var filter1 = Filter.afterHandler("Set attribute",
                e -> e.setAttribute("test-attr", value));
        var filter2 = Filter.afterHandler("Read attribute",
                e -> attr.complete((String) e.getAttribute("test-attr")));
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR, 0), 10);
        var context = server.createContext("/", handler);
        context.getFilters().add(filter2);
        context.getFilters().add(filter1);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            assertEquals(attr.get(), value);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testAfterHandlerSendResponse() throws Exception {
        var handler = new NoResponseHandler();
        var respCode = new CompletableFuture<Integer>();
        var filter = Filter.afterHandler("Log response code and send response",
                e -> {
                    try (InputStream is = e.getRequestBody();
                         OutputStream os = e.getResponseBody()) {
                        is.readAllBytes();
                        var resp = "hello world".getBytes(StandardCharsets.UTF_8);
                        e.sendResponseHeaders(200, resp.length);
                        os.write(resp);
                        respCode.complete(e.getResponseCode());
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                        throw new UncheckedIOException(ioe);
                    }
                });
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR, 0), 10);
        server.createContext("/", handler).getFilters().add(filter);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            assertEquals(response.statusCode(), (int)respCode.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBeforeAndAfterHandler() throws Exception {
        var handler = new EchoHandler();
        var respCode = new CompletableFuture<Integer>();
        var beforeFilter = Filter.beforeHandler("Add x-foo response header",
                e -> e.getResponseHeaders().set("x-foo", "bar"));
        var afterFilter = Filter.afterHandler("Log response code",
                e -> respCode.complete(e.getResponseCode()));
        var server = HttpServer.create(new InetSocketAddress(LOOPBACK_ADDR, 0), 10);
        var context = server.createContext("/", handler);
        context.getFilters().add(beforeFilter);
        context.getFilters().add(afterFilter);
        server.start();
        try {
            var client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            var request = HttpRequest.newBuilder(uri(server, "")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);
            assertEquals(response.headers().map().size(), 3);
            assertEquals(response.headers().firstValue("x-foo").orElseThrow(), "bar");
            assertEquals(response.statusCode(), (int)respCode.get());
        } finally {
            server.stop(0);
        }
    }

    static URI uri(HttpServer server, String path) {
        return URI.create("http://localhost:%s/%s".formatted(server.getAddress().getPort(), path));
    }

    /**
     * A test handler that discards the request and sends a response
     */
    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody();
                 OutputStream os = exchange.getResponseBody()) {
                is.readAllBytes();
                var resp = "hello world".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, resp.length);
                os.write(resp);
            }
        }
    }

    /**
     * A test handler that does nothing
     */
    static class NoResponseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException { }
    }
}
