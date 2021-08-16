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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.WebSocket;
import jdk.internal.net.http.common.BufferSupplier;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.Pair;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.common.OperationTrackers.Trackable;
import jdk.internal.net.http.common.OperationTrackers.Tracker;
import jdk.internal.net.http.websocket.BuilderImpl;
import jdk.internal.misc.InnocuousThread;

/**
 * Client implementation. Contains all configuration information and also
 * the selector manager thread which allows async events to be registered
 * and delivered when they occur. See AsyncEvent.
 */
final class HttpClientImpl extends HttpClient implements Trackable {

    static final boolean DEBUGELAPSED = Utils.TESTING || Utils.DEBUG;  // dev flag
    static final boolean DEBUGTIMEOUT = false; // dev flag
    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
    final Logger debugelapsed = Utils.getDebugLogger(this::dbgString, DEBUGELAPSED);
    final Logger debugtimeout = Utils.getDebugLogger(this::dbgString, DEBUGTIMEOUT);
    static final AtomicLong CLIENT_IDS = new AtomicLong();

    // Define the default factory as a static inner class
    // that embeds all the necessary logic to avoid
    // the risk of using a lambda that might keep a reference on the
    // HttpClient instance from which it was created (helps with
    // heapdump analysis).
    private static final class DefaultThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger nextId = new AtomicInteger();

        DefaultThreadFactory(long clientID) {
            namePrefix = "HttpClient-" + clientID + "-Worker-";
        }

        @SuppressWarnings("removal")
        @Override
        public Thread newThread(Runnable r) {
            String name = namePrefix + nextId.getAndIncrement();
            Thread t;
            if (System.getSecurityManager() == null) {
                t = new Thread(null, r, name, 0, false);
            } else {
                t = InnocuousThread.newThread(name, r);
            }
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * A DelegatingExecutor is an executor that delegates tasks to
     * a wrapped executor when it detects that the current thread
     * is the SelectorManager thread. If the current thread is not
     * the selector manager thread the given task is executed inline.
     */
    final static class DelegatingExecutor implements Executor {
        private final BooleanSupplier isInSelectorThread;
        private final Executor delegate;
        DelegatingExecutor(BooleanSupplier isInSelectorThread, Executor delegate) {
            this.isInSelectorThread = isInSelectorThread;
            this.delegate = delegate;
        }

        Executor delegate() {
            return delegate;
        }

        @Override
        public void execute(Runnable command) {
            if (isInSelectorThread.getAsBoolean()) {
                delegate.execute(command);
            } else {
                command.run();
            }
        }

        @SuppressWarnings("removal")
        private void shutdown() {
            if (delegate instanceof ExecutorService service) {
                PrivilegedAction<?> action = () -> {
                    service.shutdown();
                    return null;
                };
                AccessController.doPrivileged(action, null,
                        new RuntimePermission("modifyThread"));
            }
        }

    }

    private final CookieHandler cookieHandler;
    private final Duration connectTimeout;
    private final Redirect followRedirects;
    private final ProxySelector userProxySelector;
    private final ProxySelector proxySelector;
    private final Authenticator authenticator;
    private final Version version;
    private final ConnectionPool connections;
    private final DelegatingExecutor delegatingExecutor;
    private final boolean isDefaultExecutor;
    // Security parameters
    private final SSLContext sslContext;
    private final SSLParameters sslParams;
    private final SelectorManager selmgr;
    private final FilterFactory filters;
    private final Http2ClientImpl client2;
    private final long id;
    private final String dbgTag;

    // The SSL DirectBuffer Supplier provides the ability to recycle
    // buffers used between the socket reader and the SSLEngine, or
    // more precisely between the SocketTube publisher and the
    // SSLFlowDelegate reader.
    private final SSLDirectBufferSupplier sslBufferSupplier
            = new SSLDirectBufferSupplier(this);

    // This reference is used to keep track of the facade HttpClient
    // that was returned to the application code.
    // It makes it possible to know when the application no longer
    // holds any reference to the HttpClient.
    // Unfortunately, this information is not enough to know when
    // to exit the SelectorManager thread. Because of the asynchronous
    // nature of the API, we also need to wait until all pending operations
    // have completed.
    private final WeakReference<HttpClientFacade> facadeRef;

    // This counter keeps track of the number of operations pending
    // on the HttpClient. The SelectorManager thread will wait
    // until there are no longer any pending operations and the
    // facadeRef is cleared before exiting.
    //
    // The pendingOperationCount is incremented every time a send/sendAsync
    // operation is invoked on the HttpClient, and is decremented when
    // the HttpResponse<T> object is returned to the user.
    // However, at this point, the body may not have been fully read yet.
    // This is the case when the response T is implemented as a streaming
    // subscriber (such as an InputStream).
    //
    // To take care of this issue the pendingOperationCount will additionally
    // be incremented/decremented in the following cases:
    //
    // 1. For HTTP/2  it is incremented when a stream is added to the
    //    Http2Connection streams map, and decreased when the stream is removed
    //    from the map. This should also take care of push promises.
    // 2. For WebSocket the count is increased when creating a
    //    DetachedConnectionChannel for the socket, and decreased
    //    when the channel is closed.
    //    In addition, the HttpClient facade is passed to the WebSocket builder,
    //    (instead of the client implementation delegate).
    // 3. For HTTP/1.1 the count is incremented before starting to parse the body
    //    response, and decremented when the parser has reached the end of the
    //    response body flow.
    //
    // This should ensure that the selector manager thread remains alive until
    // the response has been fully received or the web socket is closed.
    private final AtomicLong pendingOperationCount = new AtomicLong();
    private final AtomicLong pendingWebSocketCount = new AtomicLong();
    private final AtomicLong pendingHttpRequestCount = new AtomicLong();
    private final AtomicLong pendingHttp2StreamCount = new AtomicLong();

    /** A Set of, deadline first, ordered timeout events. */
    private final TreeSet<TimeoutEvent> timeouts;

    /**
     * This is a bit tricky:
     * 1. an HttpClientFacade has a final HttpClientImpl field.
     * 2. an HttpClientImpl has a final WeakReference<HttpClientFacade> field,
     *    where the referent is the facade created for that instance.
     * 3. We cannot just create the HttpClientFacade in the HttpClientImpl
     *    constructor, because it would be only weakly referenced and could
     *    be GC'ed before we can return it.
     * The solution is to use an instance of SingleFacadeFactory which will
     * allow the caller of new HttpClientImpl(...) to retrieve the facade
     * after the HttpClientImpl has been created.
     */
    private static final class SingleFacadeFactory {
        HttpClientFacade facade;
        HttpClientFacade createFacade(HttpClientImpl impl) {
            assert facade == null;
            return (facade = new HttpClientFacade(impl));
        }
    }

    static HttpClientFacade create(HttpClientBuilderImpl builder) {
        SingleFacadeFactory facadeFactory = new SingleFacadeFactory();
        HttpClientImpl impl = new HttpClientImpl(builder, facadeFactory);
        impl.start();
        assert facadeFactory.facade != null;
        assert impl.facadeRef.get() == facadeFactory.facade;
        return facadeFactory.facade;
    }

    private HttpClientImpl(HttpClientBuilderImpl builder,
                           SingleFacadeFactory facadeFactory) {
        id = CLIENT_IDS.incrementAndGet();
        dbgTag = "HttpClientImpl(" + id +")";
        if (builder.sslContext == null) {
            try {
                sslContext = SSLContext.getDefault();
            } catch (NoSuchAlgorithmException ex) {
                throw new UncheckedIOException(new IOException(ex));
            }
        } else {
            sslContext = builder.sslContext;
        }
        Executor ex = builder.executor;
        if (ex == null) {
            ex = Executors.newCachedThreadPool(new DefaultThreadFactory(id));
            isDefaultExecutor = true;
        } else {
            isDefaultExecutor = false;
        }
        delegatingExecutor = new DelegatingExecutor(this::isSelectorThread, ex);
        facadeRef = new WeakReference<>(facadeFactory.createFacade(this));
        client2 = new Http2ClientImpl(this);
        cookieHandler = builder.cookieHandler;
        connectTimeout = builder.connectTimeout;
        followRedirects = builder.followRedirects == null ?
                Redirect.NEVER : builder.followRedirects;
        this.userProxySelector = builder.proxy;
        this.proxySelector = Optional.ofNullable(userProxySelector)
                .orElseGet(HttpClientImpl::getDefaultProxySelector);
        if (debug.on())
            debug.log("proxySelector is %s (user-supplied=%s)",
                      this.proxySelector, userProxySelector != null);
        authenticator = builder.authenticator;
        if (builder.version == null) {
            version = HttpClient.Version.HTTP_2;
        } else {
            version = builder.version;
        }
        if (builder.sslParams == null) {
            sslParams = getDefaultParams(sslContext);
        } else {
            sslParams = builder.sslParams;
        }
        connections = new ConnectionPool(id);
        connections.start();
        timeouts = new TreeSet<>();
        try {
            selmgr = new SelectorManager(this);
        } catch (IOException e) {
            // unlikely
            throw new UncheckedIOException(e);
        }
        selmgr.setDaemon(true);
        filters = new FilterFactory();
        initFilters();
        assert facadeRef.get() != null;
    }

    private void start() {
        selmgr.start();
    }

    // Called from the SelectorManager thread, just before exiting.
    // Clears the HTTP/1.1 and HTTP/2 cache, ensuring that the connections
    // that may be still lingering there are properly closed (and their
    // possibly still opened SocketChannel released).
    private void stop() {
        // Clears HTTP/1.1 cache and close its connections
        connections.stop();
        // Clears HTTP/2 cache and close its connections.
        client2.stop();
        // shutdown the executor if needed
        if (isDefaultExecutor) delegatingExecutor.shutdown();
    }

    private static SSLParameters getDefaultParams(SSLContext ctx) {
        SSLParameters params = ctx.getDefaultSSLParameters();
        return params;
    }

    @SuppressWarnings("removal")
    private static ProxySelector getDefaultProxySelector() {
        PrivilegedAction<ProxySelector> action = ProxySelector::getDefault;
        return AccessController.doPrivileged(action);
    }

    // Returns the facade that was returned to the application code.
    // May be null if that facade is no longer referenced.
    final HttpClientFacade facade() {
        return facadeRef.get();
    }

    // Increments the pendingOperationCount.
    final long reference() {
        pendingHttpRequestCount.incrementAndGet();
        return pendingOperationCount.incrementAndGet();
    }

    // Decrements the pendingOperationCount.
    final long unreference() {
        final long count = pendingOperationCount.decrementAndGet();
        final long httpCount = pendingHttpRequestCount.decrementAndGet();
        final long http2Count = pendingHttp2StreamCount.get();
        final long webSocketCount = pendingWebSocketCount.get();
        if (count == 0 && facade() == null) {
            selmgr.wakeupSelector();
        }
        assert httpCount >= 0 : "count of HTTP/1.1 operations < 0";
        assert http2Count >= 0 : "count of HTTP/2 operations < 0";
        assert webSocketCount >= 0 : "count of WS operations < 0";
        assert count >= 0 : "count of pending operations < 0";
        return count;
    }

    // Increments the pendingOperationCount.
    final long streamReference() {
        pendingHttp2StreamCount.incrementAndGet();
        return pendingOperationCount.incrementAndGet();
    }

    // Decrements the pendingOperationCount.
    final long streamUnreference() {
        final long count = pendingOperationCount.decrementAndGet();
        final long http2Count = pendingHttp2StreamCount.decrementAndGet();
        final long httpCount = pendingHttpRequestCount.get();
        final long webSocketCount = pendingWebSocketCount.get();
        if (count == 0 && facade() == null) {
            selmgr.wakeupSelector();
        }
        assert httpCount >= 0 : "count of HTTP/1.1 operations < 0";
        assert http2Count >= 0 : "count of HTTP/2 operations < 0";
        assert webSocketCount >= 0 : "count of WS operations < 0";
        assert count >= 0 : "count of pending operations < 0";
        return count;
    }

    // Increments the pendingOperationCount.
    final long webSocketOpen() {
        pendingWebSocketCount.incrementAndGet();
        return pendingOperationCount.incrementAndGet();
    }

    // Decrements the pendingOperationCount.
    final long webSocketClose() {
        final long count = pendingOperationCount.decrementAndGet();
        final long webSocketCount = pendingWebSocketCount.decrementAndGet();
        final long httpCount = pendingHttpRequestCount.get();
        final long http2Count = pendingHttp2StreamCount.get();
        if (count == 0 && facade() == null) {
            selmgr.wakeupSelector();
        }
        assert httpCount >= 0 : "count of HTTP/1.1 operations < 0";
        assert http2Count >= 0 : "count of HTTP/2 operations < 0";
        assert webSocketCount >= 0 : "count of WS operations < 0";
        assert count >= 0 : "count of pending operations < 0";
        return count;
    }

    // Returns the pendingOperationCount.
    final long referenceCount() {
        return pendingOperationCount.get();
    }

    final static class HttpClientTracker implements Tracker {
        final AtomicLong httpCount;
        final AtomicLong http2Count;
        final AtomicLong websocketCount;
        final AtomicLong operationsCount;
        final Reference<?> reference;
        final String name;
        HttpClientTracker(AtomicLong http,
                          AtomicLong http2,
                          AtomicLong ws,
                          AtomicLong ops,
                          Reference<?> ref,
                          String name) {
            this.httpCount = http;
            this.http2Count = http2;
            this.websocketCount = ws;
            this.operationsCount = ops;
            this.reference = ref;
            this.name = name;
        }
        @Override
        public long getOutstandingOperations() {
            return operationsCount.get();
        }
        @Override
        public long getOutstandingHttpOperations() {
            return httpCount.get();
        }
        @Override
        public long getOutstandingHttp2Streams() { return http2Count.get(); }
        @Override
        public long getOutstandingWebSocketOperations() {
            return websocketCount.get();
        }
        @Override
        public boolean isFacadeReferenced() {
            return reference.get() != null;
        }
        @Override
        public String getName() {
            return name;
        }
    }

    public Tracker getOperationsTracker() {
        return new HttpClientTracker(pendingHttpRequestCount,
                pendingHttp2StreamCount,
                pendingWebSocketCount,
                pendingOperationCount,
                facadeRef,
                dbgTag);
    }

    // Called by the SelectorManager thread to figure out whether it's time
    // to terminate.
    final boolean isReferenced() {
        HttpClient facade = facade();
        return facade != null || referenceCount() > 0;
    }

    /**
     * Wait for activity on given exchange.
     * The following occurs in the SelectorManager thread.
     *
     *  1) add to selector
     *  2) If selector fires for this exchange then
     *     call AsyncEvent.handle()
     *
     * If exchange needs to change interest ops, then call registerEvent() again.
     */
    void registerEvent(AsyncEvent exchange) throws IOException {
        selmgr.register(exchange);
    }

    /**
     * Allows an AsyncEvent to modify its interestOps.
     * @param event The modified event.
     */
    void eventUpdated(AsyncEvent event) throws ClosedChannelException {
        assert !(event instanceof AsyncTriggerEvent);
        selmgr.eventUpdated(event);
    }

    boolean isSelectorThread() {
        return Thread.currentThread() == selmgr;
    }

    Http2ClientImpl client2() {
        return client2;
    }

    private void debugCompleted(String tag, long startNanos, HttpRequest req) {
        if (debugelapsed.on()) {
            debugelapsed.log(tag + " elapsed "
                    + (System.nanoTime() - startNanos)/1000_000L
                    + " millis for " + req.method()
                    + " to " + req.uri());
        }
    }

    @Override
    public <T> HttpResponse<T>
    send(HttpRequest req, BodyHandler<T> responseHandler)
        throws IOException, InterruptedException
    {
        CompletableFuture<HttpResponse<T>> cf = null;

        // if the thread is already interrupted no need to go further.
        // cf.get() would throw anyway.
        if (Thread.interrupted()) throw new InterruptedException();
        try {
            cf = sendAsync(req, responseHandler, null, null);
            return cf.get();
        } catch (InterruptedException ie) {
            if (cf != null )
                cf.cancel(true);
            throw ie;
        } catch (ExecutionException e) {
            final Throwable throwable = e.getCause();
            final String msg = throwable.getMessage();

            if (throwable instanceof IllegalArgumentException) {
                throw new IllegalArgumentException(msg, throwable);
            } else if (throwable instanceof SecurityException) {
                throw new SecurityException(msg, throwable);
            } else if (throwable instanceof HttpConnectTimeoutException) {
                HttpConnectTimeoutException hcte = new HttpConnectTimeoutException(msg);
                hcte.initCause(throwable);
                throw hcte;
            } else if (throwable instanceof HttpTimeoutException) {
                throw new HttpTimeoutException(msg);
            } else if (throwable instanceof ConnectException) {
                ConnectException ce = new ConnectException(msg);
                ce.initCause(throwable);
                throw ce;
            } else if (throwable instanceof SSLHandshakeException) {
                // special case for SSLHandshakeException
                SSLHandshakeException he = new SSLHandshakeException(msg);
                he.initCause(throwable);
                throw he;
            } else if (throwable instanceof SSLException) {
                // any other SSLException is wrapped in a plain
                // SSLException
                throw new SSLException(msg, throwable);
            } else if (throwable instanceof IOException) {
                throw new IOException(msg, throwable);
            } else {
                throw new IOException(msg, throwable);
            }
        }
    }

    private static final Executor ASYNC_POOL = new CompletableFuture<Void>().defaultExecutor();

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest, BodyHandler<T> responseHandler)
    {
        return sendAsync(userRequest, responseHandler, null);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest,
              BodyHandler<T> responseHandler,
              PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(userRequest, responseHandler, pushPromiseHandler, delegatingExecutor.delegate);
    }

    @SuppressWarnings("removal")
    private <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest,
              BodyHandler<T> responseHandler,
              PushPromiseHandler<T> pushPromiseHandler,
              Executor exchangeExecutor)    {

        Objects.requireNonNull(userRequest);
        Objects.requireNonNull(responseHandler);

        AccessControlContext acc = null;
        if (System.getSecurityManager() != null)
            acc = AccessController.getContext();

        // Clone the, possibly untrusted, HttpRequest
        HttpRequestImpl requestImpl = new HttpRequestImpl(userRequest, proxySelector);
        if (requestImpl.method().equals("CONNECT"))
            throw new IllegalArgumentException("Unsupported method CONNECT");

        long start = DEBUGELAPSED ? System.nanoTime() : 0;
        reference();
        try {
            if (debugelapsed.on())
                debugelapsed.log("ClientImpl (async) send %s", userRequest);

            // When using sendAsync(...) we explicitly pass the
            // executor's delegate as exchange executor to force
            // asynchronous scheduling of the exchange.
            // When using send(...) we don't specify any executor
            // and default to using the client's delegating executor
            // which only spawns asynchronous tasks if it detects
            // that the current thread is the selector manager
            // thread. This will cause everything to execute inline
            // until we need to schedule some event with the selector.
            Executor executor = exchangeExecutor == null
                    ? this.delegatingExecutor : exchangeExecutor;

            MultiExchange<T> mex = new MultiExchange<>(userRequest,
                                                            requestImpl,
                                                            this,
                                                            responseHandler,
                                                            pushPromiseHandler,
                                                            acc);
            CompletableFuture<HttpResponse<T>> res =
                    mex.responseAsync(executor).whenComplete((b,t) -> unreference());
            if (DEBUGELAPSED) {
                res = res.whenComplete(
                        (b,t) -> debugCompleted("ClientImpl (async)", start, userRequest));
            }

            // makes sure that any dependent actions happen in the CF default
            // executor. This is only needed for sendAsync(...), when
            // exchangeExecutor is non-null.
            if (exchangeExecutor != null) {
                res = res.whenCompleteAsync((r, t) -> { /* do nothing */}, ASYNC_POOL);
            }
            return res;
        } catch(Throwable t) {
            unreference();
            debugCompleted("ClientImpl (async)", start, userRequest);
            throw t;
        }
    }

    // Main loop for this client's selector
    private final static class SelectorManager extends Thread {

        // For testing purposes we have an internal System property that
        // can control the frequency at which the selector manager will wake
        // up when there are no pending operations.
        // Increasing the frequency (shorter delays) might allow the selector
        // to observe that the facade is no longer referenced and might allow
        // the selector thread to terminate more timely - for when nothing is
        // ongoing it will only check for that condition every NODEADLINE ms.
        // To avoid misuse of the property, the delay that can be specified
        // is comprised between [MIN_NODEADLINE, MAX_NODEADLINE], and its default
        // value if unspecified (or <= 0) is DEF_NODEADLINE = 3000ms
        // The property is -Djdk.internal.httpclient.selectorTimeout=<millis>
        private static final int MIN_NODEADLINE = 1000; // ms
        private static final int MAX_NODEADLINE = 1000 * 1200; // ms
        private static final int DEF_NODEADLINE = 3000; // ms
        private static final long NODEADLINE; // default is DEF_NODEADLINE ms
        static {
            // ensure NODEADLINE is initialized with some valid value.
            long deadline =  Utils.getIntegerProperty(
                "jdk.internal.httpclient.selectorTimeout",
                DEF_NODEADLINE); // millis
            if (deadline <= 0) deadline = DEF_NODEADLINE;
            deadline = Math.max(deadline, MIN_NODEADLINE);
            NODEADLINE = Math.min(deadline, MAX_NODEADLINE);
        }

        private final Selector selector;
        private volatile boolean closed;
        private final List<AsyncEvent> registrations;
        private final List<AsyncTriggerEvent> deregistrations;
        private final Logger debug;
        private final Logger debugtimeout;
        HttpClientImpl owner;
        ConnectionPool pool;

        SelectorManager(HttpClientImpl ref) throws IOException {
            super(null, null,
                  "HttpClient-" + ref.id + "-SelectorManager",
                  0, false);
            owner = ref;
            debug = ref.debug;
            debugtimeout = ref.debugtimeout;
            pool = ref.connectionPool();
            registrations = new ArrayList<>();
            deregistrations = new ArrayList<>();
            selector = Selector.open();
        }

        void eventUpdated(AsyncEvent e) throws ClosedChannelException {
            if (Thread.currentThread() == this) {
                SelectionKey key = e.channel().keyFor(selector);
                if (key != null && key.isValid()) {
                    SelectorAttachment sa = (SelectorAttachment) key.attachment();
                    sa.register(e);
                } else if (e.interestOps() != 0){
                    // We don't care about paused events.
                    // These are actually handled by
                    // SelectorAttachment::resetInterestOps later on.
                    // But if we reach here when trying to resume an
                    // event then it's better to fail fast.
                    if (debug.on()) debug.log("No key for channel");
                    e.abort(new IOException("No key for channel"));
                }
            } else {
                register(e);
            }
        }

        // This returns immediately. So caller not allowed to send/receive
        // on connection.
        synchronized void register(AsyncEvent e) {
            registrations.add(e);
            selector.wakeup();
        }

        synchronized void cancel(SocketChannel e) {
            SelectionKey key = e.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            selector.wakeup();
        }

        void wakeupSelector() {
            selector.wakeup();
        }

        synchronized void shutdown() {
            Log.logTrace("{0}: shutting down", getName());
            if (debug.on()) debug.log("SelectorManager shutting down");
            closed = true;
            try {
                selector.close();
            } catch (IOException ignored) {
            } finally {
                owner.stop();
            }
        }

        @Override
        public void run() {
            List<Pair<AsyncEvent,IOException>> errorList = new ArrayList<>();
            List<AsyncEvent> readyList = new ArrayList<>();
            List<Runnable> resetList = new ArrayList<>();
            try {
                if (Log.channel()) Log.logChannel(getName() + ": starting");
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (this) {
                        assert errorList.isEmpty();
                        assert readyList.isEmpty();
                        assert resetList.isEmpty();
                        for (AsyncTriggerEvent event : deregistrations) {
                            event.handle();
                        }
                        deregistrations.clear();
                        for (AsyncEvent event : registrations) {
                            if (event instanceof AsyncTriggerEvent) {
                                readyList.add(event);
                                continue;
                            }
                            SelectableChannel chan = event.channel();
                            SelectionKey key = null;
                            try {
                                key = chan.keyFor(selector);
                                SelectorAttachment sa;
                                if (key == null || !key.isValid()) {
                                    if (key != null) {
                                        // key is canceled.
                                        // invoke selectNow() to purge it
                                        // before registering the new event.
                                        selector.selectNow();
                                    }
                                    sa = new SelectorAttachment(chan, selector);
                                } else {
                                    sa = (SelectorAttachment) key.attachment();
                                }
                                // may throw IOE if channel closed: that's OK
                                sa.register(event);
                                if (!chan.isOpen()) {
                                    throw new IOException("Channel closed");
                                }
                            } catch (IOException e) {
                                Log.logTrace("{0}: {1}", getName(), e);
                                if (debug.on())
                                    debug.log("Got " + e.getClass().getName()
                                              + " while handling registration events");
                                chan.close();
                                // let the event abort deal with it
                                errorList.add(new Pair<>(event, e));
                                if (key != null) {
                                    key.cancel();
                                    selector.selectNow();
                                }
                            }
                        }
                        registrations.clear();
                        selector.selectedKeys().clear();
                    }

                    for (AsyncEvent event : readyList) {
                        assert event instanceof AsyncTriggerEvent;
                        event.handle();
                    }
                    readyList.clear();

                    for (Pair<AsyncEvent,IOException> error : errorList) {
                        // an IOException was raised and the channel closed.
                        handleEvent(error.first, error.second);
                    }
                    errorList.clear();

                    // Check whether client is still alive, and if not,
                    // gracefully stop this thread
                    if (!owner.isReferenced()) {
                        Log.logTrace("{0}: {1}",
                                getName(),
                                "HttpClient no longer referenced. Exiting...");
                        return;
                    }

                    // Timeouts will have milliseconds granularity. It is important
                    // to handle them in a timely fashion.
                    long nextTimeout = owner.purgeTimeoutsAndReturnNextDeadline();
                    if (debugtimeout.on())
                        debugtimeout.log("next timeout: %d", nextTimeout);

                    // Keep-alive have seconds granularity. It's not really an
                    // issue if we keep connections linger a bit more in the keep
                    // alive cache.
                    long nextExpiry = pool.purgeExpiredConnectionsAndReturnNextDeadline();
                    if (debugtimeout.on())
                        debugtimeout.log("next expired: %d", nextExpiry);

                    assert nextTimeout >= 0;
                    assert nextExpiry >= 0;

                    // Don't wait for ever as it might prevent the thread to
                    // stop gracefully. millis will be 0 if no deadline was found.
                    if (nextTimeout <= 0) nextTimeout = NODEADLINE;

                    // Clip nextExpiry at NODEADLINE limit. The default
                    // keep alive is 1200 seconds (half an hour) - we don't
                    // want to wait that long.
                    if (nextExpiry <= 0) nextExpiry = NODEADLINE;
                    else nextExpiry = Math.min(NODEADLINE, nextExpiry);

                    // takes the least of the two.
                    long millis = Math.min(nextExpiry, nextTimeout);

                    if (debugtimeout.on())
                        debugtimeout.log("Next deadline is %d",
                                         (millis == 0 ? NODEADLINE : millis));
                    //debugPrint(selector);
                    int n = selector.select(millis == 0 ? NODEADLINE : millis);
                    if (n == 0) {
                        // Check whether client is still alive, and if not,
                        // gracefully stop this thread
                        if (!owner.isReferenced()) {
                            Log.logTrace("{0}: {1}",
                                    getName(),
                                    "HttpClient no longer referenced. Exiting...");
                            return;
                        }
                        owner.purgeTimeoutsAndReturnNextDeadline();
                        continue;
                    }

                    Set<SelectionKey> keys = selector.selectedKeys();
                    assert errorList.isEmpty();

                    for (SelectionKey key : keys) {
                        SelectorAttachment sa = (SelectorAttachment) key.attachment();
                        if (!key.isValid()) {
                            IOException ex = sa.chan.isOpen()
                                    ? new IOException("Invalid key")
                                    : new ClosedChannelException();
                            sa.pending.forEach(e -> errorList.add(new Pair<>(e,ex)));
                            sa.pending.clear();
                            continue;
                        }

                        int eventsOccurred;
                        try {
                            eventsOccurred = key.readyOps();
                        } catch (CancelledKeyException ex) {
                            IOException io = Utils.getIOException(ex);
                            sa.pending.forEach(e -> errorList.add(new Pair<>(e,io)));
                            sa.pending.clear();
                            continue;
                        }
                        sa.events(eventsOccurred).forEach(readyList::add);
                        resetList.add(() -> sa.resetInterestOps(eventsOccurred));
                    }

                    selector.selectNow(); // complete cancellation
                    selector.selectedKeys().clear();

                    // handle selected events
                    readyList.forEach((e) -> handleEvent(e, null));
                    readyList.clear();

                    // handle errors (closed channels etc...)
                    errorList.forEach((p) -> handleEvent(p.first, p.second));
                    errorList.clear();

                    // reset interest ops for selected channels
                    resetList.forEach(r -> r.run());
                    resetList.clear();

                }
            } catch (Throwable e) {
                if (!closed) {
                    // This terminates thread. So, better just print stack trace
                    String err = Utils.stackTrace(e);
                    Log.logError("{0}: {1}: {2}", getName(),
                            "HttpClientImpl shutting down due to fatal error", err);
                }
                if (debug.on()) debug.log("shutting down", e);
                if (Utils.ASSERTIONSENABLED && !debug.on()) {
                    e.printStackTrace(System.err); // always print the stack
                }
            } finally {
                if (Log.channel()) Log.logChannel(getName() + ": stopping");
                shutdown();
            }
        }

//        void debugPrint(Selector selector) {
//            System.err.println("Selector: debugprint start");
//            Set<SelectionKey> keys = selector.keys();
//            for (SelectionKey key : keys) {
//                SelectableChannel c = key.channel();
//                int ops = key.interestOps();
//                System.err.printf("selector chan:%s ops:%d\n", c, ops);
//            }
//            System.err.println("Selector: debugprint end");
//        }

        /** Handles the given event. The given ioe may be null. */
        void handleEvent(AsyncEvent event, IOException ioe) {
            if (closed || ioe != null) {
                event.abort(ioe);
            } else {
                event.handle();
            }
        }
    }

    final String debugInterestOps(SelectableChannel channel) {
        try {
            SelectionKey key = channel.keyFor(selmgr.selector);
            if (key == null) return "channel not registered with selector";
            String keyInterestOps = key.isValid()
                    ? "key.interestOps=" + key.interestOps() : "invalid key";
            return String.format("channel registered with selector, %s, sa.interestOps=%s",
                                 keyInterestOps,
                                 ((SelectorAttachment)key.attachment()).interestOps);
        } catch (Throwable t) {
            return String.valueOf(t);
        }
    }

    /**
     * Tracks multiple user level registrations associated with one NIO
     * registration (SelectionKey). In this implementation, registrations
     * are one-off and when an event is posted the registration is cancelled
     * until explicitly registered again.
     *
     * <p> No external synchronization required as this class is only used
     * by the SelectorManager thread. One of these objects required per
     * connection.
     */
    private static class SelectorAttachment {
        private final SelectableChannel chan;
        private final Selector selector;
        private final Set<AsyncEvent> pending;
        private final static Logger debug =
                Utils.getDebugLogger("SelectorAttachment"::toString, Utils.DEBUG);
        private int interestOps;

        SelectorAttachment(SelectableChannel chan, Selector selector) {
            this.pending = new HashSet<>();
            this.chan = chan;
            this.selector = selector;
        }

        void register(AsyncEvent e) throws ClosedChannelException {
            int newOps = e.interestOps();
            // re register interest if we are not already interested
            // in the event. If the event is paused, then the pause will
            // be taken into account later when resetInterestOps is called.
            boolean reRegister = (interestOps & newOps) != newOps;
            interestOps |= newOps;
            pending.add(e);
            if (debug.on())
                debug.log("Registering %s for %d (%s)", e, newOps, reRegister);
            if (reRegister) {
                // first time registration happens here also
                try {
                    chan.register(selector, interestOps, this);
                } catch (Throwable x) {
                    abortPending(x);
                }
            } else if (!chan.isOpen()) {
                abortPending(new ClosedChannelException());
            }
        }

        /**
         * Returns a Stream<AsyncEvents> containing only events that are
         * registered with the given {@code interestOps}.
         */
        Stream<AsyncEvent> events(int interestOps) {
            return pending.stream()
                    .filter(ev -> (ev.interestOps() & interestOps) != 0);
        }

        /**
         * Removes any events with the given {@code interestOps}, and if no
         * events remaining, cancels the associated SelectionKey.
         */
        void resetInterestOps(int interestOps) {
            int newOps = 0;

            Iterator<AsyncEvent> itr = pending.iterator();
            while (itr.hasNext()) {
                AsyncEvent event = itr.next();
                int evops = event.interestOps();
                if (event.repeating()) {
                    newOps |= evops;
                    continue;
                }
                if ((evops & interestOps) != 0) {
                    itr.remove();
                } else {
                    newOps |= evops;
                }
            }

            this.interestOps = newOps;
            SelectionKey key = chan.keyFor(selector);
            if (newOps == 0 && key != null && pending.isEmpty()) {
                key.cancel();
            } else {
                try {
                    if (key == null || !key.isValid()) {
                        throw new CancelledKeyException();
                    }
                    key.interestOps(newOps);
                    // double check after
                    if (!chan.isOpen()) {
                        abortPending(new ClosedChannelException());
                        return;
                    }
                    assert key.interestOps() == newOps;
                } catch (CancelledKeyException x) {
                    // channel may have been closed
                    if (debug.on()) debug.log("key cancelled for " + chan);
                    abortPending(x);
                }
            }
        }

        void abortPending(Throwable x) {
            if (!pending.isEmpty()) {
                AsyncEvent[] evts = pending.toArray(new AsyncEvent[0]);
                pending.clear();
                IOException io = Utils.getIOException(x);
                for (AsyncEvent event : evts) {
                    event.abort(io);
                }
            }
        }
    }

    /*package-private*/ SSLContext theSSLContext() {
        return sslContext;
    }

    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    @Override
    public SSLParameters sslParameters() {
        return Utils.copySSLParameters(sslParams);
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.ofNullable(authenticator);
    }

    /*package-private*/ final DelegatingExecutor theExecutor() {
        return delegatingExecutor;
    }

    @Override
    public final Optional<Executor> executor() {
        return isDefaultExecutor
                ? Optional.empty()
                : Optional.of(delegatingExecutor.delegate());
    }

    ConnectionPool connectionPool() {
        return connections;
    }

    @Override
    public Redirect followRedirects() {
        return followRedirects;
    }


    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.ofNullable(cookieHandler);
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.ofNullable(connectTimeout);
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.ofNullable(userProxySelector);
    }

    // Return the effective proxy that this client uses.
    ProxySelector proxySelector() {
        return proxySelector;
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        // Make sure to pass the HttpClientFacade to the WebSocket builder.
        // This will ensure that the facade is not released before the
        // WebSocket has been created, at which point the pendingOperationCount
        // will have been incremented by the RawChannelTube.
        // See RawChannelTube.
        return new BuilderImpl(this.facade(), proxySelector);
    }

    @Override
    public Version version() {
        return version;
    }

    String dbgString() {
        return dbgTag;
    }

    @Override
    public String toString() {
        // Used by tests to get the client's id and compute the
        // name of the SelectorManager thread.
        return super.toString() + ("(" + id + ")");
    }

    private void initFilters() {
        addFilter(AuthenticationFilter.class);
        addFilter(RedirectFilter.class);
        if (this.cookieHandler != null) {
            addFilter(CookieFilter.class);
        }
    }

    private void addFilter(Class<? extends HeaderFilter> f) {
        filters.addFilter(f);
    }

    final LinkedList<HeaderFilter> filterChain() {
        return filters.getFilterChain();
    }

    // Timer controls.
    // Timers are implemented through timed Selector.select() calls.

    synchronized void registerTimer(TimeoutEvent event) {
        Log.logTrace("Registering timer {0}", event);
        timeouts.add(event);
        selmgr.wakeupSelector();
    }

    synchronized void cancelTimer(TimeoutEvent event) {
        Log.logTrace("Canceling timer {0}", event);
        timeouts.remove(event);
    }

    /**
     * Purges ( handles ) timer events that have passed their deadline, and
     * returns the amount of time, in milliseconds, until the next earliest
     * event. A return value of 0 means that there are no events.
     */
    private long purgeTimeoutsAndReturnNextDeadline() {
        long diff = 0L;
        List<TimeoutEvent> toHandle = null;
        int remaining = 0;
        // enter critical section to retrieve the timeout event to handle
        synchronized(this) {
            if (timeouts.isEmpty()) return 0L;

            Instant now = Instant.now();
            Iterator<TimeoutEvent> itr = timeouts.iterator();
            while (itr.hasNext()) {
                TimeoutEvent event = itr.next();
                diff = now.until(event.deadline(), ChronoUnit.MILLIS);
                if (diff <= 0) {
                    itr.remove();
                    toHandle = (toHandle == null) ? new ArrayList<>() : toHandle;
                    toHandle.add(event);
                } else {
                    break;
                }
            }
            remaining = timeouts.size();
        }

        // can be useful for debugging
        if (toHandle != null && Log.trace()) {
            Log.logTrace("purgeTimeoutsAndReturnNextDeadline: handling "
                    +  toHandle.size() + " events, "
                    + "remaining " + remaining
                    + ", next deadline: " + (diff < 0 ? 0L : diff));
        }

        // handle timeout events out of critical section
        if (toHandle != null) {
            Throwable failed = null;
            for (TimeoutEvent event : toHandle) {
                try {
                   Log.logTrace("Firing timer {0}", event);
                   event.handle();
                } catch (Error | RuntimeException e) {
                    // Not expected. Handle remaining events then throw...
                    // If e is an OOME or SOE it might simply trigger a new
                    // error from here - but in this case there's not much we
                    // could do anyway. Just let it flow...
                    if (failed == null) failed = e;
                    else failed.addSuppressed(e);
                    Log.logTrace("Failed to handle event {0}: {1}", event, e);
                }
            }
            if (failed instanceof Error) throw (Error) failed;
            if (failed instanceof RuntimeException) throw (RuntimeException) failed;
        }

        // return time to wait until next event. 0L if there's no more events.
        return diff < 0 ? 0L : diff;
    }

    // used for the connection window
    int getReceiveBufferSize() {
        return Utils.getIntegerNetProperty(
                "jdk.httpclient.receiveBufferSize",
                0 // only set the size if > 0
        );
    }

    // used for testing
    int getSendBufferSize() {
        return Utils.getIntegerNetProperty(
                "jdk.httpclient.sendBufferSize",
                0 // only set the size if > 0
        );
    }

    // Optimization for reading SSL encrypted data
    // --------------------------------------------

    // Returns a BufferSupplier that can be used for reading
    // encrypted bytes of the channel. These buffers can then
    // be recycled by the SSLFlowDelegate::Reader after their
    // content has been copied in the SSLFlowDelegate::Reader
    // readBuf.
    // Because allocating, reading, copying, and recycling
    // all happen in the SelectorManager thread,
    // then this BufferSupplier can be shared between all
    // the SSL connections managed by this client.
    BufferSupplier getSSLBufferSupplier() {
        return sslBufferSupplier;
    }

    // An implementation of BufferSupplier that manages a pool of
    // maximum 3 direct byte buffers (SocketTube.MAX_BUFFERS) that
    // are used for reading encrypted bytes off the channel before
    // copying and subsequent unwrapping.
    private static final class SSLDirectBufferSupplier implements BufferSupplier {
        private static final int POOL_SIZE = SocketTube.MAX_BUFFERS;
        private final ByteBuffer[] pool = new ByteBuffer[POOL_SIZE];
        private final HttpClientImpl client;
        private final Logger debug;
        private int tail, count; // no need for volatile: only accessed in SM thread.

        SSLDirectBufferSupplier(HttpClientImpl client) {
            this.client = Objects.requireNonNull(client);
            this.debug = client.debug;
        }

        // Gets a buffer from the pool, or allocates a new one if needed.
        @Override
        public ByteBuffer get() {
            assert client.isSelectorThread();
            assert tail <= POOL_SIZE : "allocate tail is " + tail;
            ByteBuffer buf;
            if (tail == 0) {
                if (debug.on()) {
                    // should not appear more than SocketTube.MAX_BUFFERS
                    debug.log("ByteBuffer.allocateDirect(%d)", Utils.BUFSIZE);
                }
                assert count++ < POOL_SIZE : "trying to allocate more than "
                            + POOL_SIZE + " buffers";
                buf = ByteBuffer.allocateDirect(Utils.BUFSIZE);
            } else {
                assert tail > 0 : "non positive tail value: " + tail;
                tail--;
                buf = pool[tail];
                pool[tail] = null;
            }
            assert buf.isDirect();
            assert buf.position() == 0;
            assert buf.hasRemaining();
            assert buf.limit() == Utils.BUFSIZE;
            assert tail < POOL_SIZE;
            assert tail >= 0;
            return buf;
        }

        // Returns the given buffer to the pool.
        @Override
        public void recycle(ByteBuffer buffer) {
            assert client.isSelectorThread();
            assert buffer.isDirect();
            assert !buffer.hasRemaining();
            assert tail < POOL_SIZE : "recycle tail is " + tail;
            assert tail >= 0;
            buffer.position(0);
            buffer.limit(buffer.capacity());
            // don't fail if assertions are off. we have asserted above.
            if (tail < POOL_SIZE) {
                pool[tail] = buffer;
                tail++;
            }
            assert tail <= POOL_SIZE;
            assert tail > 0;
        }
    }

}
