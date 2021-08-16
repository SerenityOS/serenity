/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.common.SequentialScheduler.DeferredCompleter;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Utils;
import static java.net.http.HttpClient.Version.HTTP_2;
import static jdk.internal.net.http.common.Utils.ProxyHeaders;

/**
 * Wraps socket channel layer and takes care of SSL also.
 *
 * Subtypes are:
 *      PlainHttpConnection: regular direct TCP connection to server
 *      PlainProxyConnection: plain text proxy connection
 *      PlainTunnelingConnection: opens plain text (CONNECT) tunnel to server
 *      AsyncSSLConnection: TLS channel direct to server
 *      AsyncSSLTunnelConnection: TLS channel via (CONNECT) proxy tunnel
 */
abstract class HttpConnection implements Closeable {

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
    final static Logger DEBUG_LOGGER = Utils.getDebugLogger(
            () -> "HttpConnection(SocketTube(?))", Utils.DEBUG);

    /** The address this connection is connected to. Could be a server or a proxy. */
    final InetSocketAddress address;
    private final HttpClientImpl client;
    private final TrailingOperations trailingOperations;

    HttpConnection(InetSocketAddress address, HttpClientImpl client) {
        this.address = address;
        this.client = client;
        trailingOperations = new TrailingOperations();
    }

    private static final class TrailingOperations {
        private final Map<CompletionStage<?>, Boolean> operations =
                new IdentityHashMap<>();
        void add(CompletionStage<?> cf) {
            synchronized(operations) {
                operations.put(cf, Boolean.TRUE);
                cf.whenComplete((r,t)-> remove(cf));
            }
        }
        boolean remove(CompletionStage<?> cf) {
            synchronized(operations) {
                return operations.remove(cf);
            }
        }
    }

    final void addTrailingOperation(CompletionStage<?> cf) {
        trailingOperations.add(cf);
    }

//    final void removeTrailingOperation(CompletableFuture<?> cf) {
//        trailingOperations.remove(cf);
//    }

    final HttpClientImpl client() {
        return client;
    }

    /**
     * Initiates the connect phase.
     *
     * Returns a CompletableFuture that completes when the underlying
     * TCP connection has been established or an error occurs.
     */
    public abstract CompletableFuture<Void> connectAsync(Exchange<?> exchange);

    /**
     * Finishes the connection phase.
     *
     * Returns a CompletableFuture that completes when any additional,
     * type specific, setup has been done. Must be called after connectAsync. */
    public abstract CompletableFuture<Void> finishConnect();

    /** Tells whether, or not, this connection is connected to its destination. */
    abstract boolean connected();

    /** Tells whether, or not, this connection is secure ( over SSL ) */
    abstract boolean isSecure();

    /**
     * Tells whether, or not, this connection is proxied.
     * Returns true for tunnel connections, or clear connection to
     * any host through proxy.
     */
    abstract boolean isProxied();

    /**
     * Returns the address of the proxy used by this connection.
     * Returns the proxy address for tunnel connections, or
     * clear connection to any host through proxy.
     * Returns {@code null} otherwise.
     */
    abstract InetSocketAddress proxy();

    /** Tells whether, or not, this connection is open. */
    final boolean isOpen() {
        return channel().isOpen() &&
                (connected() ? !getConnectionFlow().isFinished() : true);
    }

    /**
     * Forces a call to the native implementation of the
     * connection's channel to verify that this channel is still
     * open.
     * <p>
     * This method should only be called just after an HTTP/1.1
     * connection is retrieved from the HTTP/1.1 connection pool.
     * It is used to trigger an early detection of the channel state,
     * before handling the connection over to the HTTP stack.
     * It helps minimizing race conditions where the selector manager
     * thread hasn't woken up - or hasn't raised the event, before
     * the connection was retrieved from the pool. It helps reduce
     * the occurrence of "HTTP/1.1 parser received no bytes"
     * exception, when the server closes the connection while
     * it's being taken out of the pool.
     * <p>
     * This method attempts to read one byte from the underlying
     * channel. Because the connection was in the pool - there
     * should be nothing to read.
     * <p>
     * If {@code read} manages to read a byte off the connection, this is a
     * protocol error: the method closes the connection and returns false.
     * If {@code read} returns EOF, the method closes the connection and
     * returns false.
     * If {@code read} throws an exception, the method returns false.
     * Otherwise, {@code read} returns 0, the channel appears to be
     * still open, and the method returns true.
     * @return true if the channel appears to be still open.
     */
    final boolean checkOpen() {
        if (isOpen()) {
            try {
                // channel is non blocking
                int read = channel().read(ByteBuffer.allocate(1));
                if (read == 0) return true;
                close();
            } catch (IOException x) {
                debug.log("Pooled connection is no longer operational: %s",
                        x.toString());
                return false;
            }
        }
        return false;
    }

    interface HttpPublisher extends FlowTube.TubePublisher {
        void enqueue(List<ByteBuffer> buffers) throws IOException;
        void enqueueUnordered(List<ByteBuffer> buffers) throws IOException;
        void signalEnqueued() throws IOException;
    }

    /**
     * Returns the HTTP publisher associated with this connection.  May be null
     * if invoked before connecting.
     */
    abstract HttpPublisher publisher();

    // HTTP/2 MUST use TLS version 1.2 or higher for HTTP/2 over TLS
    private static final Predicate<String> testRequiredHTTP2TLSVersion = proto ->
            proto.equals("TLSv1.2") || proto.equals("TLSv1.3");

   /**
    * Returns true if the given client's SSL parameter protocols contains at
    * least one TLS version that HTTP/2 requires.
    */
   private static final boolean hasRequiredHTTP2TLSVersion(HttpClient client) {
       String[] protos = client.sslParameters().getProtocols();
       if (protos != null) {
           return Arrays.stream(protos).filter(testRequiredHTTP2TLSVersion).findAny().isPresent();
       } else {
           return false;
       }
   }

    /**
     * Factory for retrieving HttpConnections. A connection can be retrieved
     * from the connection pool, or a new one created if none available.
     *
     * The given {@code addr} is the ultimate destination. Any proxies,
     * etc, are determined from the request. Returns a concrete instance which
     * is one of the following:
     *      {@link PlainHttpConnection}
     *      {@link PlainTunnelingConnection}
     *
     * The returned connection, if not from the connection pool, must have its,
     * connect() or connectAsync() method invoked, which ( when it completes
     * successfully ) renders the connection usable for requests.
     */
    public static HttpConnection getConnection(InetSocketAddress addr,
                                               HttpClientImpl client,
                                               HttpRequestImpl request,
                                               Version version) {
        // The default proxy selector may select a proxy whose  address is
        // unresolved. We must resolve the address before connecting to it.
        InetSocketAddress proxy = Utils.resolveAddress(request.proxy());
        HttpConnection c = null;
        boolean secure = request.secure();
        ConnectionPool pool = client.connectionPool();

        if (!secure) {
            c = pool.getConnection(false, addr, proxy);
            if (c != null && c.checkOpen() /* may have been eof/closed when in the pool */) {
                final HttpConnection conn = c;
                if (DEBUG_LOGGER.on())
                    DEBUG_LOGGER.log(conn.getConnectionFlow()
                                     + ": plain connection retrieved from HTTP/1.1 pool");
                return c;
            } else {
                return getPlainConnection(addr, proxy, request, client);
            }
        } else {  // secure
            if (version != HTTP_2) { // only HTTP/1.1 connections are in the pool
                c = pool.getConnection(true, addr, proxy);
            }
            if (c != null && c.isOpen()) {
                final HttpConnection conn = c;
                if (DEBUG_LOGGER.on())
                    DEBUG_LOGGER.log(conn.getConnectionFlow()
                                     + ": SSL connection retrieved from HTTP/1.1 pool");
                return c;
            } else {
                String[] alpn = null;
                if (version == HTTP_2 && hasRequiredHTTP2TLSVersion(client)) {
                    alpn = new String[] { "h2", "http/1.1" };
                }
                return getSSLConnection(addr, proxy, alpn, request, client);
            }
        }
    }

    private static HttpConnection getSSLConnection(InetSocketAddress addr,
                                                   InetSocketAddress proxy,
                                                   String[] alpn,
                                                   HttpRequestImpl request,
                                                   HttpClientImpl client) {
        if (proxy != null)
            return new AsyncSSLTunnelConnection(addr, client, alpn, proxy,
                                                proxyTunnelHeaders(request));
        else
            return new AsyncSSLConnection(addr, client, alpn);
    }

    /**
     * This method is used to build a filter that will accept or
     * veto (header-name, value) tuple for transmission on the
     * wire.
     * The filter is applied to the headers when sending the headers
     * to the remote party.
     * Which tuple is accepted/vetoed depends on:
     * <pre>
     *    - whether the connection is a tunnel connection
     *      [talking to a server through a proxy tunnel]
     *    - whether the method is CONNECT
     *      [establishing a CONNECT tunnel through a proxy]
     *    - whether the request is using a proxy
     *      (and the connection is not a tunnel)
     *      [talking to a server through a proxy]
     *    - whether the request is a direct connection to
     *      a server (no tunnel, no proxy).
     * </pre>
     * @param request
     * @return
     */
    BiPredicate<String,String> headerFilter(HttpRequestImpl request) {
        if (isTunnel()) {
            // talking to a server through a proxy tunnel
            // don't send proxy-* headers to a plain server
            assert !request.isConnect();
            return Utils.NO_PROXY_HEADERS_FILTER;
        } else if (request.isConnect()) {
            // establishing a proxy tunnel
            // check for proxy tunnel disabled schemes
            // assert !this.isTunnel();
            assert request.proxy() == null;
            return Utils.PROXY_TUNNEL_FILTER;
        } else if (request.proxy() != null) {
            // talking to a server through a proxy (no tunnel)
            // check for proxy disabled schemes
            // assert !isTunnel() && !request.isConnect();
            return Utils.PROXY_FILTER;
        } else {
            // talking to a server directly (no tunnel, no proxy)
            // don't send proxy-* headers to a plain server
            // assert request.proxy() == null && !request.isConnect();
            return Utils.NO_PROXY_HEADERS_FILTER;
        }
    }

    BiPredicate<String,String> contextRestricted(HttpRequestImpl request, HttpClient client) {
        if (!isTunnel() && request.isConnect()) {
            // establishing a proxy tunnel
            assert request.proxy() == null;
            return Utils.PROXY_TUNNEL_RESTRICTED(client);
        } else {
            return Utils.CONTEXT_RESTRICTED(client);
        }
    }

    // Composes a new immutable HttpHeaders that combines the
    // user and system header but only keeps those headers that
    // start with "proxy-"
    private static ProxyHeaders proxyTunnelHeaders(HttpRequestImpl request) {
        HttpHeaders userHeaders = HttpHeaders.of(request.headers().map(), Utils.PROXY_TUNNEL_FILTER);
        HttpHeaders systemHeaders = HttpHeaders.of(request.getSystemHeadersBuilder().map(), Utils.PROXY_TUNNEL_FILTER);
        return new ProxyHeaders(userHeaders, systemHeaders);
    }

    /* Returns either a plain HTTP connection or a plain tunnelling connection
     * for proxied WebSocket */
    private static HttpConnection getPlainConnection(InetSocketAddress addr,
                                                     InetSocketAddress proxy,
                                                     HttpRequestImpl request,
                                                     HttpClientImpl client) {
        if (request.isWebSocket() && proxy != null)
            return new PlainTunnelingConnection(addr, proxy, client,
                                                proxyTunnelHeaders(request));

        if (proxy == null)
            return new PlainHttpConnection(addr, client);
        else
            return new PlainProxyConnection(proxy, client);
    }

    void closeOrReturnToCache(HttpHeaders hdrs) {
        if (hdrs == null) {
            // the connection was closed by server, eof
            Log.logTrace("Cannot return connection to pool: closing {0}", this);
            close();
            return;
        }
        HttpClientImpl client = client();
        if (client == null) {
            Log.logTrace("Client released: closing {0}", this);
            close();
            return;
        }
        ConnectionPool pool = client.connectionPool();
        boolean keepAlive = hdrs.firstValue("Connection")
                .map((s) -> !s.equalsIgnoreCase("close"))
                .orElse(true);

        if (keepAlive && checkOpen()) {
            Log.logTrace("Returning connection to the pool: {0}", this);
            pool.returnToPool(this);
        } else {
            Log.logTrace("Closing connection (keepAlive={0}, isOpen={1}): {2}",
                    keepAlive, isOpen(), this);
            close();
        }
    }

    /* Tells whether or not this connection is a tunnel through a proxy */
    boolean isTunnel() { return false; }

    abstract SocketChannel channel();

    final InetSocketAddress address() {
        return address;
    }

    abstract ConnectionPool.CacheKey cacheKey();

    /**
     * Closes this connection, by returning the socket to its connection pool.
     */
    @Override
    public abstract void close();

    abstract FlowTube getConnectionFlow();

    /**
     * A publisher that makes it possible to publish (write) ordered (normal
     * priority) and unordered (high priority) buffers downstream.
     */
    final class PlainHttpPublisher implements HttpPublisher {
        final Object reading;
        PlainHttpPublisher() {
            this(new Object());
        }
        PlainHttpPublisher(Object readingLock) {
            this.reading = readingLock;
        }
        final ConcurrentLinkedDeque<List<ByteBuffer>> queue = new ConcurrentLinkedDeque<>();
        final ConcurrentLinkedDeque<List<ByteBuffer>> priority = new ConcurrentLinkedDeque<>();
        volatile Flow.Subscriber<? super List<ByteBuffer>> subscriber;
        volatile HttpWriteSubscription subscription;
        final SequentialScheduler writeScheduler =
                    new SequentialScheduler(this::flushTask);
        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            synchronized (reading) {
                //assert this.subscription == null;
                //assert this.subscriber == null;
                if (subscription == null) {
                    subscription = new HttpWriteSubscription();
                }
                this.subscriber = subscriber;
            }
            // TODO: should we do this in the flow?
            subscriber.onSubscribe(subscription);
            signal();
        }

        void flushTask(DeferredCompleter completer) {
            try {
                HttpWriteSubscription sub = subscription;
                if (sub != null) sub.flush();
            } finally {
                completer.complete();
            }
        }

        void signal() {
            writeScheduler.runOrSchedule();
        }

        final class HttpWriteSubscription implements Flow.Subscription {
            final Demand demand = new Demand();

            @Override
            public void request(long n) {
                if (n <= 0) throw new IllegalArgumentException("non-positive request");
                demand.increase(n);
                if (debug.on())
                    debug.log("HttpPublisher: got request of "  + n + " from "
                               + getConnectionFlow());
                writeScheduler.runOrSchedule();
            }

            @Override
            public void cancel() {
                if (debug.on())
                    debug.log("HttpPublisher: cancelled by " + getConnectionFlow());
            }

            private boolean isEmpty() {
                return queue.isEmpty() && priority.isEmpty();
            }

            private List<ByteBuffer> poll() {
                List<ByteBuffer> elem = priority.poll();
                return elem == null ? queue.poll() : elem;
            }

            void flush() {
                while (!isEmpty() && demand.tryDecrement()) {
                    List<ByteBuffer> elem = poll();
                    if (debug.on())
                        debug.log("HttpPublisher: sending "
                                    + Utils.remaining(elem) + " bytes ("
                                    + elem.size() + " buffers) to "
                                    + getConnectionFlow());
                    subscriber.onNext(elem);
                }
            }
        }

        @Override
        public void enqueue(List<ByteBuffer> buffers) throws IOException {
            queue.add(buffers);
            int bytes = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
            debug.log("added %d bytes to the write queue", bytes);
        }

        @Override
        public void enqueueUnordered(List<ByteBuffer> buffers) throws IOException {
            // Unordered frames are sent before existing frames.
            int bytes = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
            priority.add(buffers);
            debug.log("added %d bytes in the priority write queue", bytes);
        }

        @Override
        public void signalEnqueued() throws IOException {
            debug.log("signalling the publisher of the write queue");
            signal();
        }
    }

    String dbgTag;
    final String dbgString() {
        FlowTube flow = getConnectionFlow();
        String tag = dbgTag;
        if (tag == null && flow != null) {
            dbgTag = tag = this.getClass().getSimpleName() + "(" + flow + ")";
        } else if (tag == null) {
            tag = this.getClass().getSimpleName() + "(?)";
        }
        return tag;
    }

    @Override
    public String toString() {
        return "HttpConnection: " + channel().toString();
    }
}
