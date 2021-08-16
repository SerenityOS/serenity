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

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.internal.net.http.common.HttpHeadersBuilder;

import java.net.InetAddress;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient.Version;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Defines an adaptation layers so that a test server handlers and filters
 * can be implemented independently of the underlying server version.
 * <p>
 * For instance:
 * <pre>{@code
 *
 *  URI http1URI, http2URI;
 *
 *  InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
 *  HttpTestServer server1 = HttpTestServer.of(HttpServer.create(sa, 0));
 *  HttpTestContext context = server.addHandler(new HttpTestEchoHandler(), "/http1/echo");
 *  http2URI = "http://localhost:" + server1.getAddress().getPort() + "/http1/echo";
 *
 *  Http2TestServer http2TestServer = new Http2TestServer("localhost", false, 0);
 *  HttpTestServer server2 = HttpTestServer.of(http2TestServer);
 *  server2.addHandler(new HttpTestEchoHandler(), "/http2/echo");
 *  http1URI = "http://localhost:" + server2.getAddress().getPort() + "/http2/echo";
 *
 *  }</pre>
 */
public interface HttpServerAdapters {

    static final boolean PRINTSTACK =
            Boolean.getBoolean("jdk.internal.httpclient.debug");

    static void uncheckedWrite(ByteArrayOutputStream baos, byte[] ba) {
        try {
            baos.write(ba);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void printBytes(PrintStream out, String prefix, byte[] bytes) {
        int padding = 4 + 4 - (bytes.length % 4);
        padding = padding > 4 ? padding - 4 : 4;
        byte[] bigbytes = new byte[bytes.length + padding];
        System.arraycopy(bytes, 0, bigbytes, padding, bytes.length);
        out.println(prefix + bytes.length + " "
                    + new BigInteger(bigbytes).toString(16));
    }

    /**
     * A version agnostic adapter class for HTTP request Headers.
     */
    public static abstract class HttpTestRequestHeaders {
        public abstract Optional<String> firstValue(String name);
        public abstract Set<String> keySet();
        public abstract Set<Map.Entry<String, List<String>>> entrySet();
        public abstract List<String> get(String name);
        public abstract boolean containsKey(String name);

        public static HttpTestRequestHeaders of(Headers headers) {
            return new Http1TestRequestHeaders(headers);
        }

        public static HttpTestRequestHeaders of(HttpHeaders headers) {
            return new Http2TestRequestHeaders(headers);
        }

        private static final class Http1TestRequestHeaders extends HttpTestRequestHeaders {
            private final Headers headers;
            Http1TestRequestHeaders(Headers h) { this.headers = h; }
            @Override
            public Optional<String> firstValue(String name) {
                if (headers.containsKey(name)) {
                    return Optional.ofNullable(headers.getFirst(name));
                }
                return Optional.empty();
            }
            @Override
            public Set<String> keySet() { return headers.keySet(); }
            @Override
            public Set<Map.Entry<String, List<String>>> entrySet() {
                return headers.entrySet();
            }
            @Override
            public List<String> get(String name) {
                return headers.get(name);
            }
            @Override
            public boolean containsKey(String name) {
                return headers.containsKey(name);
            }
        }
        private static final class Http2TestRequestHeaders extends HttpTestRequestHeaders {
            private final HttpHeaders headers;
            Http2TestRequestHeaders(HttpHeaders h) { this.headers = h; }
            @Override
            public Optional<String> firstValue(String name) {
                return headers.firstValue(name);
            }
            @Override
            public Set<String> keySet() { return headers.map().keySet(); }
            @Override
            public Set<Map.Entry<String, List<String>>> entrySet() {
                return headers.map().entrySet();
            }
            @Override
            public List<String> get(String name) {
                return headers.allValues(name);
            }
            @Override
            public boolean containsKey(String name) {
                return headers.firstValue(name).isPresent();
            }
        }
    }

    /**
     * A version agnostic adapter class for HTTP response Headers.
     */
    public static abstract class HttpTestResponseHeaders {
        public abstract void addHeader(String name, String value);

        public static HttpTestResponseHeaders of(Headers headers) {
            return new Http1TestResponseHeaders(headers);
        }
        public static HttpTestResponseHeaders of(HttpHeadersBuilder headersBuilder) {
            return new Http2TestResponseHeaders(headersBuilder);
        }

        private final static class Http1TestResponseHeaders extends HttpTestResponseHeaders {
            private final Headers headers;
            Http1TestResponseHeaders(Headers h) { this.headers = h; }
            @Override
            public void addHeader(String name, String value) {
                headers.add(name, value);
            }
        }
        private final static class Http2TestResponseHeaders extends HttpTestResponseHeaders {
            private final HttpHeadersBuilder headersBuilder;
            Http2TestResponseHeaders(HttpHeadersBuilder hb) { this.headersBuilder = hb; }
            @Override
            public void addHeader(String name, String value) {
                headersBuilder.addHeader(name, value);
            }
        }
    }

    /**
     * A version agnostic adapter class for HTTP Server Exchange.
     */
    public static abstract class HttpTestExchange implements AutoCloseable {
        public abstract Version getServerVersion();
        public abstract Version getExchangeVersion();
        public abstract InputStream   getRequestBody();
        public abstract OutputStream  getResponseBody();
        public abstract HttpTestRequestHeaders getRequestHeaders();
        public abstract HttpTestResponseHeaders getResponseHeaders();
        public abstract void sendResponseHeaders(int code, int contentLength) throws IOException;
        public abstract URI getRequestURI();
        public abstract String getRequestMethod();
        public abstract void close();
        public void serverPush(URI uri, HttpHeaders headers, byte[] body) {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            serverPush(uri, headers, bais);
        }
        public void serverPush(URI uri, HttpHeaders headers, InputStream body) {
            throw new UnsupportedOperationException("serverPush with " + getExchangeVersion());
        }
        public boolean serverPushAllowed() {
            return false;
        }
        public static HttpTestExchange of(HttpExchange exchange) {
            return new Http1TestExchange(exchange);
        }
        public static HttpTestExchange of(Http2TestExchange exchange) {
            return new Http2TestExchangeImpl(exchange);
        }

        abstract void doFilter(Filter.Chain chain) throws IOException;

        // implementations...
        private static final class Http1TestExchange extends HttpTestExchange {
            private final HttpExchange exchange;
            Http1TestExchange(HttpExchange exch) {
                this.exchange = exch;
            }
            @Override
            public Version getServerVersion() { return Version.HTTP_1_1; }
            @Override
            public Version getExchangeVersion() { return Version.HTTP_1_1; }
            @Override
            public InputStream getRequestBody() {
                return exchange.getRequestBody();
            }
            @Override
            public OutputStream getResponseBody() {
                return exchange.getResponseBody();
            }
            @Override
            public HttpTestRequestHeaders getRequestHeaders() {
                return HttpTestRequestHeaders.of(exchange.getRequestHeaders());
            }
            @Override
            public HttpTestResponseHeaders getResponseHeaders() {
                return HttpTestResponseHeaders.of(exchange.getResponseHeaders());
            }
            @Override
            public void sendResponseHeaders(int code, int contentLength) throws IOException {
                if (contentLength == 0) contentLength = -1;
                else if (contentLength < 0) contentLength = 0;
                exchange.sendResponseHeaders(code, contentLength);
            }
            @Override
            void doFilter(Filter.Chain chain) throws IOException {
                chain.doFilter(exchange);
            }
            @Override
            public void close() { exchange.close(); }
            @Override
            public URI getRequestURI() { return exchange.getRequestURI(); }
            @Override
            public String getRequestMethod() { return exchange.getRequestMethod(); }
            @Override
            public String toString() {
                return this.getClass().getSimpleName() + ": " + exchange.toString();
            }
        }

        private static final class Http2TestExchangeImpl extends HttpTestExchange {
            private final Http2TestExchange exchange;
            Http2TestExchangeImpl(Http2TestExchange exch) {
                this.exchange = exch;
            }
            @Override
            public Version getServerVersion() { return Version.HTTP_2; }
            @Override
            public Version getExchangeVersion() { return Version.HTTP_2; }
            @Override
            public InputStream getRequestBody() {
                return exchange.getRequestBody();
            }
            @Override
            public OutputStream getResponseBody() {
                return exchange.getResponseBody();
            }
            @Override
            public HttpTestRequestHeaders getRequestHeaders() {
                return HttpTestRequestHeaders.of(exchange.getRequestHeaders());
            }

            @Override
            public HttpTestResponseHeaders getResponseHeaders() {
                return HttpTestResponseHeaders.of(exchange.getResponseHeaders());
            }
            @Override
            public void sendResponseHeaders(int code, int contentLength) throws IOException {
                if (contentLength == 0) contentLength = -1;
                else if (contentLength < 0) contentLength = 0;
                exchange.sendResponseHeaders(code, contentLength);
            }
            @Override
            public boolean serverPushAllowed() {
                return exchange.serverPushAllowed();
            }
            @Override
            public void serverPush(URI uri, HttpHeaders headers, InputStream body) {
                exchange.serverPush(uri, headers, body);
            }
            void doFilter(Filter.Chain filter) throws IOException {
                throw new IOException("cannot use HTTP/1.1 filter with HTTP/2 server");
            }
            @Override
            public void close() { exchange.close();}
            @Override
            public URI getRequestURI() { return exchange.getRequestURI(); }
            @Override
            public String getRequestMethod() { return exchange.getRequestMethod(); }
            @Override
            public String toString() {
                return this.getClass().getSimpleName() + ": " + exchange.toString();
            }
        }

    }


    /**
     * A version agnostic adapter class for HTTP Server Handlers.
     */
    public interface HttpTestHandler {
        void handle(HttpTestExchange t) throws IOException;

        default HttpHandler toHttpHandler() {
            return (t) -> doHandle(HttpTestExchange.of(t));
        }
        default Http2Handler toHttp2Handler() {
            return (t) -> doHandle(HttpTestExchange.of(t));
        }
        private void doHandle(HttpTestExchange t) throws IOException {
            try {
                handle(t);
            } catch (Throwable x) {
                System.out.println("WARNING: exception caught in HttpTestHandler::handle " + x);
                System.err.println("WARNING: exception caught in HttpTestHandler::handle " + x);
                if (PRINTSTACK && !expectException(t)) x.printStackTrace(System.out);
                throw x;
            }
        }
    }


    public static class HttpTestEchoHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                printBytes(System.out,"Echo server got "
                        + t.getExchangeVersion() + " bytes: ", bytes);
                if (t.getRequestHeaders().firstValue("Content-type").isPresent()) {
                    t.getResponseHeaders().addHeader("Content-type",
                            t.getRequestHeaders().firstValue("Content-type").get());
                }
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    public static boolean expectException(HttpTestExchange e) {
        HttpTestRequestHeaders h = e.getRequestHeaders();
        Optional<String> expectException = h.firstValue("X-expect-exception");
        if (expectException.isPresent()) {
            return expectException.get().equalsIgnoreCase("true");
        }
        return false;
    }

    /**
     * A version agnostic adapter class for HTTP Server Filter Chains.
     */
    public abstract class HttpChain {

        public abstract void doFilter(HttpTestExchange exchange) throws IOException;
        public static HttpChain of(Filter.Chain chain) {
            return new Http1Chain(chain);
        }

        public static HttpChain of(List<HttpTestFilter> filters, HttpTestHandler handler) {
            return new Http2Chain(filters, handler);
        }

        private static class Http1Chain extends HttpChain {
            final Filter.Chain chain;
            Http1Chain(Filter.Chain chain) {
                this.chain = chain;
            }
            @Override
            public void doFilter(HttpTestExchange exchange) throws IOException {
                try {
                    exchange.doFilter(chain);
                } catch (Throwable t) {
                    System.out.println("WARNING: exception caught in Http1Chain::doFilter " + t);
                    System.err.println("WARNING: exception caught in Http1Chain::doFilter " + t);
                    if (PRINTSTACK && !expectException(exchange)) t.printStackTrace(System.out);
                    throw t;
                }
            }
        }

        private static class Http2Chain extends HttpChain {
            ListIterator<HttpTestFilter> iter;
            HttpTestHandler handler;
            Http2Chain(List<HttpTestFilter> filters, HttpTestHandler handler) {
                this.iter = filters.listIterator();
                this.handler = handler;
            }
            @Override
            public void doFilter(HttpTestExchange exchange) throws IOException {
                try {
                    if (iter.hasNext()) {
                        iter.next().doFilter(exchange, this);
                    } else {
                        handler.handle(exchange);
                    }
                } catch (Throwable t) {
                    System.out.println("WARNING: exception caught in Http2Chain::doFilter " + t);
                    System.err.println("WARNING: exception caught in Http2Chain::doFilter " + t);
                    if (PRINTSTACK && !expectException(exchange)) t.printStackTrace(System.out);
                    throw t;
                }
            }
        }

    }

    /**
     * A version agnostic adapter class for HTTP Server Filters.
     */
    public abstract class HttpTestFilter {

        public abstract String description();

        public abstract void doFilter(HttpTestExchange exchange, HttpChain chain) throws IOException;

        public Filter toFilter() {
            return new Filter() {
                @Override
                public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                    HttpTestFilter.this.doFilter(HttpTestExchange.of(exchange), HttpChain.of(chain));
                }
                @Override
                public String description() {
                    return HttpTestFilter.this.description();
                }
            };
        }
    }

    /**
     * A version agnostic adapter class for HTTP Server Context.
     */
    public static abstract class HttpTestContext {
        public abstract String getPath();
        public abstract void addFilter(HttpTestFilter filter);
        public abstract Version getVersion();

        // will throw UOE if the server is HTTP/2
        public abstract void setAuthenticator(com.sun.net.httpserver.Authenticator authenticator);
    }

    /**
     * A version agnostic adapter class for HTTP Servers.
     */
    public static abstract class HttpTestServer {
        private static final class ServerLogging {
            private static final Logger logger = Logger.getLogger("com.sun.net.httpserver");
            static void enableLogging() {
                logger.setLevel(Level.FINE);
                Stream.of(Logger.getLogger("").getHandlers())
                        .forEach(h -> h.setLevel(Level.ALL));
            }
        }

        public abstract void start();
        public abstract void stop();
        public abstract HttpTestContext addHandler(HttpTestHandler handler, String root);
        public abstract InetSocketAddress getAddress();
        public abstract Version getVersion();

        public String serverAuthority() {
            return InetAddress.getLoopbackAddress().getHostName() + ":"
                    + getAddress().getPort();
        }

        public static HttpTestServer of(HttpServer server) {
            return new Http1TestServer(server);
        }

        public static HttpTestServer of(HttpServer server, ExecutorService executor) {
            return new Http1TestServer(server, executor);
        }

        public static HttpTestServer of(Http2TestServer server) {
            return new Http2TestServerImpl(server);
        }

        private static class Http1TestServer extends  HttpTestServer {
            private final HttpServer impl;
            private final ExecutorService executor;
            Http1TestServer(HttpServer server) {
                this(server, null);
            }
            Http1TestServer(HttpServer server, ExecutorService executor) {
                if (executor != null) server.setExecutor(executor);
                this.executor = executor;
                this.impl = server;
            }
            @Override
            public void start() {
                System.out.println("Http1TestServer: start");
                impl.start();
            }
            @Override
            public void stop() {
                System.out.println("Http1TestServer: stop");
                try {
                    impl.stop(0);
                } finally {
                    if (executor != null) {
                        executor.shutdownNow();
                    }
                }
            }
            @Override
            public HttpTestContext addHandler(HttpTestHandler handler, String path) {
                System.out.println("Http1TestServer[" + getAddress()
                        + "]::addHandler " + handler + ", " + path);
                return new Http1TestContext(impl.createContext(path, handler.toHttpHandler()));
            }
            @Override
            public InetSocketAddress getAddress() {
                return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                        impl.getAddress().getPort());
            }
            public Version getVersion() { return Version.HTTP_1_1; }
        }

        private static class Http1TestContext extends HttpTestContext {
            private final HttpContext context;
            Http1TestContext(HttpContext ctxt) {
                this.context = ctxt;
            }
            @Override public String getPath() {
                return context.getPath();
            }
            @Override
            public void addFilter(HttpTestFilter filter) {
                System.out.println("Http1TestContext::addFilter " + filter.description());
                context.getFilters().add(filter.toFilter());
            }
            @Override
            public void setAuthenticator(com.sun.net.httpserver.Authenticator authenticator) {
                context.setAuthenticator(authenticator);
            }
            @Override public Version getVersion() { return Version.HTTP_1_1; }
        }

        private static class Http2TestServerImpl extends  HttpTestServer {
            private final Http2TestServer impl;
            Http2TestServerImpl(Http2TestServer server) {
                this.impl = server;
            }
            @Override
            public void start() {
                System.out.println("Http2TestServerImpl: start");
                impl.start();
            }
            @Override
            public void stop() {
                System.out.println("Http2TestServerImpl: stop");
                impl.stop();
            }
            @Override
            public HttpTestContext addHandler(HttpTestHandler handler, String path) {
                System.out.println("Http2TestServerImpl[" + getAddress()
                                   + "]::addHandler " + handler + ", " + path);
                Http2TestContext context = new Http2TestContext(handler, path);
                impl.addHandler(context.toHttp2Handler(), path);
                return context;
            }
            @Override
            public InetSocketAddress getAddress() {
                return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                        impl.getAddress().getPort());
            }
            public Version getVersion() { return Version.HTTP_2; }
        }

        private static class Http2TestContext
                extends HttpTestContext implements HttpTestHandler {
            private final HttpTestHandler handler;
            private final String path;
            private final List<HttpTestFilter> filters = new CopyOnWriteArrayList<>();
            Http2TestContext(HttpTestHandler hdl, String path) {
                this.handler = hdl;
                this.path = path;
            }
            @Override
            public String getPath() { return path; }
            @Override
            public void addFilter(HttpTestFilter filter) {
                System.out.println("Http2TestContext::addFilter " + filter.description());
                filters.add(filter);
            }
            @Override
            public void handle(HttpTestExchange exchange) throws IOException {
                System.out.println("Http2TestContext::handle " + exchange);
                HttpChain.of(filters, handler).doFilter(exchange);
            }
            @Override
            public void setAuthenticator(com.sun.net.httpserver.Authenticator authenticator) {
                throw new UnsupportedOperationException("Can't set HTTP/1.1 authenticator on HTTP/2 context");
            }
            @Override public Version getVersion() { return Version.HTTP_2; }
        }
    }

    public static void enableServerLogging() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%4$s [%1$tb %1$td, %1$tl:%1$tM:%1$tS.%1$tN] %2$s: %5$s%6$s%n");
        HttpTestServer.ServerLogging.enableLogging();
    }

}
