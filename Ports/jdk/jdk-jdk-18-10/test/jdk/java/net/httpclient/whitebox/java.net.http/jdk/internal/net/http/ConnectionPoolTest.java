/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.stream.IntStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jdk.internal.net.http.common.FlowTube;

/**
 * @summary Verifies that the ConnectionPool correctly handle
 *          connection deadlines and purges the right connections
 *          from the cache.
 * @bug 8187044 8187111 8221395
 * @author danielfuchs
 */
public class ConnectionPoolTest {

    static long getActiveCleaners() throws ClassNotFoundException {
        // ConnectionPool.ACTIVE_CLEANER_COUNTER.get()
        // ConnectionPoolTest.class.getModule().addReads(
        //      Class.forName("java.lang.management.ManagementFactory").getModule());
        return java.util.stream.Stream.of(ManagementFactory.getThreadMXBean()
                .dumpAllThreads(false, false))
              .filter(t -> t.getThreadName().startsWith("HTTP-Cache-cleaner"))
              .count();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[] {"testCacheCleaners"};
        }
        for (String arg : args) {
            if ("testCacheCleaners".equals(arg)) {
                testCacheCleaners();
            } else if ("testPoolSize".equals(arg)) {
                assert args.length == 1 : "testPoolSize should be run in its own VM";
                testPoolSize();
            } else if ("testCloseOrReturnToPool".equals(arg)) {
                assert args.length == 1 : "testCloseOrReturnToPool should be run in its own VM";
                testCloseOrReturnToPool();
            } else throw new RuntimeException("unknown test case: " + arg);
        }
    }

    public static void testCacheCleaners() throws Exception {
        ConnectionPool pool = new ConnectionPool(666);
        HttpClient client = new HttpClientStub(pool);
        InetSocketAddress proxy = InetSocketAddress.createUnresolved("bar", 80);
        System.out.println("Adding 20 connections to pool");
        Random random = new Random();

        final int count = 20;
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        int[] keepAlives = new int[count];
        HttpConnectionStub[] connections = new HttpConnectionStub[count];
        long purge = pool.purgeExpiredConnectionsAndReturnNextDeadline(now);
        long expected = 0;
        if (purge != expected) {
            throw new RuntimeException("Bad purge delay: " + purge
                                        + ", expected " + expected);
        }
        expected = Long.MAX_VALUE;
        for (int i=0; i<count; i++) {
            InetSocketAddress addr = InetSocketAddress.createUnresolved("foo"+i, 80);
            keepAlives[i] = random.nextInt(10) * 10  + 10;
            connections[i] = new HttpConnectionStub(client, addr, proxy, true);
            System.out.println("Adding connection: " + now
                                + " keepAlive: " + keepAlives[i]
                                + " /" + connections[i]);
            pool.returnToPool(connections[i], now, keepAlives[i]);
            expected = Math.min(expected, keepAlives[i] * 1000);
            purge = pool.purgeExpiredConnectionsAndReturnNextDeadline(now);
            if (purge != expected) {
                throw new RuntimeException("Bad purge delay: " + purge
                                        + ", expected " + expected);
            }
        }
        int min = IntStream.of(keepAlives).min().getAsInt();
        int max = IntStream.of(keepAlives).max().getAsInt();
        int mean = (min + max)/2;
        System.out.println("min=" + min + ", max=" + max + ", mean=" + mean);
        purge = pool.purgeExpiredConnectionsAndReturnNextDeadline(now);
        System.out.println("first purge would be in " + purge + " ms");
        if (Math.abs(purge/1000 - min) > 0) {
            throw new RuntimeException("expected " + min + " got " + purge/1000);
        }
        long opened = java.util.stream.Stream.of(connections)
                     .filter(HttpConnectionStub::connected).count();
        if (opened != count) {
            throw new RuntimeException("Opened: expected "
                                       + count + " got " + opened);
        }
        purge = mean * 1000;
        System.out.println("start purging at " + purge + " ms");
        Instant next = now;
        do {
           System.out.println("next purge is in " + purge + " ms");
           next = next.plus(purge, ChronoUnit.MILLIS);
           purge = pool.purgeExpiredConnectionsAndReturnNextDeadline(next);
           long k = now.until(next, ChronoUnit.SECONDS);
           System.out.println("now is " + k + "s from start");
           for (int i=0; i<count; i++) {
               if (connections[i].connected() != (k < keepAlives[i])) {
                   throw new RuntimeException("Bad connection state for "
                             + i
                             + "\n\t connected=" + connections[i].connected()
                             + "\n\t keepAlive=" + keepAlives[i]
                             + "\n\t elapsed=" + k);
               }
           }
        } while (purge > 0);
        opened = java.util.stream.Stream.of(connections)
                     .filter(HttpConnectionStub::connected).count();
        if (opened != 0) {
           throw new RuntimeException("Closed: expected "
                                       + count + " got "
                                       + (count-opened));
        }
    }

    public static void testPoolSize() throws Exception {
        final int MAX_POOL_SIZE = 10;
        System.setProperty("jdk.httpclient.connectionPoolSize",
                String.valueOf(MAX_POOL_SIZE));
        ConnectionPool pool = new ConnectionPool(666);
        HttpClient client = new HttpClientStub(pool);
        InetSocketAddress proxy = InetSocketAddress.createUnresolved("bar", 80);
        System.out.println("Adding 20 connections to pool");
        Random random = new Random();

        final int count = 20;
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        int[] keepAlives = new int[count];
        HttpConnectionStub[] connections = new HttpConnectionStub[count];
        long purge = pool.purgeExpiredConnectionsAndReturnNextDeadline(now);
        long expected = 0;
        if (purge != expected) {
            throw new RuntimeException("Bad purge delay: " + purge
                    + ", expected " + expected);
        }
        expected = Long.MAX_VALUE;
        int previous = 0;
        for (int i=0; i<count; i++) {
            InetSocketAddress addr = InetSocketAddress.createUnresolved("foo"+i, 80);
            keepAlives[i] = random.nextInt(10) * 10  + 5 + previous;
            previous = keepAlives[i];
            connections[i] = new HttpConnectionStub(client, addr, proxy, true);
            System.out.println("Adding connection: " + now
                    + " keepAlive: " + keepAlives[i]
                    + " /" + connections[i]);
            pool.returnToPool(connections[i], now, keepAlives[i]);
            if (i < MAX_POOL_SIZE) {
                expected = Math.min(expected, keepAlives[i] * 1000);
            } else {
                expected = keepAlives[i-MAX_POOL_SIZE+1] * 1000;
                if (pool.contains(connections[i-MAX_POOL_SIZE])) {
                    throw new RuntimeException("Connection[" + i + "]/"
                            + connections[i] + " should have been removed");
                }
            }
            purge = pool.purgeExpiredConnectionsAndReturnNextDeadline(now);
            if (purge != expected) {
                throw new RuntimeException("Bad purge delay for " + i + ": "
                        + purge + ", expected " + expected);
            }
        }

        long opened = java.util.stream.Stream.of(connections)
                .filter(HttpConnectionStub::connected).count();
        if (opened != MAX_POOL_SIZE) {
            throw new RuntimeException("Opened: expected "
                    + count + " got " + opened);
        }
        for (int i=0 ; i<count; i++) {
            boolean closed = (i < count - MAX_POOL_SIZE);
            if (connections[i].closed != closed) {
                throw new RuntimeException("connection[" + i + "] should be "
                        + (closed ? "closed" : "opened"));
            }
            if (pool.contains(connections[i]) == closed) {
                throw new RuntimeException("Connection[" + i + "]/"
                        + connections[i] + " should "
                        + (closed ? "" : "not ")
                        + "have been removed");
            }
        }
    }

    public static void testCloseOrReturnToPool() throws Exception {
        HttpClientFacade facade = (HttpClientFacade)HttpClient.newHttpClient();
        HttpClientImpl client = facade.impl;
        ConnectionPool pool = client.connectionPool();
        InetSocketAddress proxy = InetSocketAddress.createUnresolved("bar", 80);

        InetSocketAddress addr = InetSocketAddress.createUnresolved("foo1", 80);
        HttpConnectionStub conn1 = new HttpConnectionStub(facade, client, addr, proxy, true);
        HttpHeaders hdrs = HttpHeaders.of(new HashMap<>(), (s1,s2) -> true);
        HttpConnection conn;

        conn1.reopen();
        if (!conn1.isOpen()) {
            throw new RuntimeException("conn1 finished");
        }

        conn1.closeOrReturnToCache(hdrs);

        // Check we can find conn1 in the pool
        if (conn1 != (conn = pool.getConnection(true, addr, proxy))) {
            throw new RuntimeException("conn1 not returned, got: " + conn);
        }
        System.out.println("Found connection in the pool: " + conn );

        // Try to return it with no headers: the connection should
        // be closed and not returned to the pool (EOF).
        conn.closeOrReturnToCache(null);
        if ((conn = pool.getConnection(true, addr, proxy)) != null) {
            throw new RuntimeException(conn + " found in the pool!");
        }
        if (!conn1.closed) {
            throw new RuntimeException("conn1 not closed!");
        }
        System.out.println("EOF connection successfully closed when returned to pool");

        // reopen the connection
        conn1.reopen();
        if (!conn1.isOpen()) {
            throw new RuntimeException("conn1 finished");
        }

        // Try to return it with empty headers: the connection should
        // be returned to the pool.
        conn1.closeOrReturnToCache(hdrs);
        if (conn1 != (conn = pool.getConnection(true, addr, proxy))) {
            throw new RuntimeException("conn1 not returned to pool, got: " + conn);
        }
        if (conn1.closed) {
            throw new RuntimeException("conn1 closed");
        }
        if (!conn1.isOpen()) {
            throw new RuntimeException("conn1 finished");
        }

        System.out.println("Keep alive connection successfully returned to pool");

        // Try to return it with connection: close headers: the connection should
        // not be returned to the pool, and should be closed.
        HttpHeaders hdrs2 = HttpHeaders.of(Map.of("connection", List.of("close")), (s1, s2) -> true);
        conn1.closeOrReturnToCache(hdrs2);
        if ((conn = pool.getConnection(true, addr, proxy)) != null) {
            throw new RuntimeException(conn + " found in the pool!");
        }
        if (!conn1.closed) {
            throw new RuntimeException("conn1 not closed!");
        }
        System.out.println("Close connection successfully closed when returned to pool");

        // reopen and finish the connection.
        conn1.reopen();
        conn1.finish(true);
        if (conn1.closed) {
            throw new RuntimeException("conn1 closed");
        }
        if (conn1.isOpen()) {
            throw new RuntimeException("conn1 is opened!");
        }
        conn1.closeOrReturnToCache(hdrs2);
        if ((conn = pool.getConnection(true, addr, proxy)) != null) {
            throw new RuntimeException(conn + " found in the pool!");
        }
        if (!conn1.closed) {
            throw new RuntimeException("conn1 not closed!");
        }
        System.out.println("Finished 'close' connection successfully closed when returned to pool");

        // reopen and finish the connection.
        conn1.reopen();
        conn1.finish(true);
        if (conn1.closed) {
            throw new RuntimeException("conn1 closed");
        }
        if (conn1.isOpen()) {
            throw new RuntimeException("conn1 is opened!");
        }
        conn1.closeOrReturnToCache(hdrs);
        if ((conn = pool.getConnection(true, addr, proxy)) != null) {
            throw new RuntimeException(conn + " found in the pool!");
        }
        if (!conn1.closed) {
            throw new RuntimeException("conn1 not closed!");
        }
        System.out.println("Finished keep-alive connection successfully closed when returned to pool");

        Reference.reachabilityFence(facade);
    }

    static <T> T error() {
        throw new InternalError("Should not reach here: wrong test assumptions!");
    }

    static class FlowTubeStub implements FlowTube {
        final HttpConnectionStub conn;
        FlowTubeStub(HttpConnectionStub conn) { this.conn = conn; }
        @Override
        public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onError(Throwable error) { error(); }
        @Override public void onComplete() { error(); }
        @Override public void onNext(List<ByteBuffer> item) { error();}
        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
        }
        @Override public boolean isFinished() { return conn.finished; }
    }

    static class SocketChannelStub extends SocketChannel {

        SocketChannelStub() { super(SelectorProvider.provider()); }

        @Override
        public SocketChannel bind(SocketAddress local) throws IOException {
            return error();
        }
        @Override
        public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
            return error();
        }
        @Override
        public SocketChannel shutdownInput() throws IOException {
            return error();
        }
        @Override
        public SocketChannel shutdownOutput() throws IOException {
            return error();
        }
        @Override
        public Socket socket() { return error(); }
        @Override
        public boolean isConnected() { return true; }
        @Override
        public boolean isConnectionPending() { return false; }
        @Override
        public boolean connect(SocketAddress remote) throws IOException {
            return error();
        }
        @Override
        public boolean finishConnect() throws IOException {
            return error();
        }
        @Override
        public SocketAddress getRemoteAddress() throws IOException {
            return error();
        }
        @Override
        public int read(ByteBuffer dst) throws IOException {
            return isConnected() ? 0 : -1;
        }
        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return error();
        }
        @Override
        public int write(ByteBuffer src) throws IOException {
            return error();
        }
        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return 0;
        }
        @Override
        public SocketAddress getLocalAddress() throws IOException {
            return error();
        }
        @Override
        public <T> T getOption(SocketOption<T> name) throws IOException {
            return error();
        }
        @Override
        public Set<SocketOption<?>> supportedOptions() {
            return error();
        }
        @Override
        protected void implCloseSelectableChannel() throws IOException {
            error();
        }
        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            error();
        }
    }

    // Emulates an HttpConnection that has a strong reference to its HttpClient.
    static class HttpConnectionStub extends HttpConnection {

        public HttpConnectionStub(
                HttpClient client,
                InetSocketAddress address,
                InetSocketAddress proxy,
                boolean secured) {
            this(client, null, address, proxy, secured);
        }
        public HttpConnectionStub(
                HttpClient client,
                HttpClientImpl impl,
                InetSocketAddress address,
                InetSocketAddress proxy,
                boolean secured) {
            super(address, impl);
            this.key = ConnectionPool.cacheKey(address, proxy);
            this.address = address;
            this.proxy = proxy;
            this.secured = secured;
            this.client = client;
            this.channel = new SocketChannelStub();
            this.flow = new FlowTubeStub(this);
        }

        final InetSocketAddress proxy;
        final InetSocketAddress address;
        final boolean secured;
        final ConnectionPool.CacheKey key;
        final HttpClient client;
        final FlowTubeStub flow;
        final SocketChannel channel;
        volatile boolean closed, finished;

        // Used for testing closeOrReturnToPool.
        void finish(boolean finished) { this.finished = finished; }
        void reopen() { closed = finished = false;}

        // All these return something
        @Override boolean connected() {return !closed;}
        @Override boolean isSecure() {return secured;}
        @Override boolean isProxied() {return proxy!=null;}
        @Override InetSocketAddress proxy() { return proxy; }
        @Override ConnectionPool.CacheKey cacheKey() {return key;}
        @Override FlowTube getConnectionFlow() {return flow;}
        @Override SocketChannel channel() {return channel;}
        @Override
        public void close() {
            closed=finished=true;
            System.out.println("closed: " + this);
        }
        @Override
        public String toString() {
            return "HttpConnectionStub: " + address + " proxy: " + proxy;
        }


        // All these throw errors
        @Override public HttpPublisher publisher() {return error();}
        @Override public CompletableFuture<Void> connectAsync(Exchange<?> e) {return error();}
        @Override public CompletableFuture<Void> finishConnect() {return error();}
    }
    // Emulates an HttpClient that has a strong reference to its connection pool.
    static class HttpClientStub extends HttpClient {
        public HttpClientStub(ConnectionPool pool) {
            this.pool = pool;
        }
        final ConnectionPool pool;
        @Override public Optional<CookieHandler> cookieHandler() {return error();}
        @Override public Optional<Duration> connectTimeout() {return error();}
        @Override public HttpClient.Redirect followRedirects() {return error();}
        @Override public Optional<ProxySelector> proxy() {return error();}
        @Override public SSLContext sslContext() {return error();}
        @Override public SSLParameters sslParameters() {return error();}
        @Override public Optional<Authenticator> authenticator() {return error();}
        @Override public HttpClient.Version version() {return HttpClient.Version.HTTP_1_1;}
        @Override public Optional<Executor> executor() {return error();}
        @Override
        public <T> HttpResponse<T> send(HttpRequest req,
                                        HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            return error();
        }
        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest req,
                HttpResponse.BodyHandler<T> responseBodyHandler) {
            return error();
        }
        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest req,
                HttpResponse.BodyHandler<T> bodyHandler,
                HttpResponse.PushPromiseHandler<T> multiHandler) {
            return error();
        }
    }

}
