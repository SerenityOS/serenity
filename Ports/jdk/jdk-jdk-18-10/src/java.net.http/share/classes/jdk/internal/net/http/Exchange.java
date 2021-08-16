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

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLPermission;
import java.security.AccessControlContext;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.common.Log;

import static jdk.internal.net.http.common.Utils.permissionForProxy;

/**
 * One request/response exchange (handles 100/101 intermediate response also).
 * depth field used to track number of times a new request is being sent
 * for a given API request. If limit exceeded exception is thrown.
 *
 * Security check is performed here:
 * - uses AccessControlContext captured at API level
 * - checks for appropriate URLPermission for request
 * - if permission allowed, grants equivalent SocketPermission to call
 * - in case of direct HTTP proxy, checks additionally for access to proxy
 *    (CONNECT proxying uses its own Exchange, so check done there)
 *
 */
final class Exchange<T> {

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    final HttpRequestImpl request;
    final HttpClientImpl client;
    volatile ExchangeImpl<T> exchImpl;
    volatile CompletableFuture<? extends ExchangeImpl<T>> exchangeCF;
    volatile CompletableFuture<Void> bodyIgnored;

    // used to record possible cancellation raised before the exchImpl
    // has been established.
    private volatile IOException failed;
    @SuppressWarnings("removal")
    final AccessControlContext acc;
    final MultiExchange<T> multi;
    final Executor parentExecutor;
    volatile boolean upgrading; // to HTTP/2
    volatile boolean upgraded;  // to HTTP/2
    final PushGroup<T> pushGroup;
    final String dbgTag;

    // Keeps track of the underlying connection when establishing an HTTP/2
    // exchange so that it can be aborted/timed out mid setup.
    final ConnectionAborter connectionAborter = new ConnectionAborter();

    Exchange(HttpRequestImpl request, MultiExchange<T> multi) {
        this.request = request;
        this.upgrading = false;
        this.client = multi.client();
        this.multi = multi;
        this.acc = multi.acc;
        this.parentExecutor = multi.executor;
        this.pushGroup = multi.pushGroup;
        this.dbgTag = "Exchange";
    }

    /* If different AccessControlContext to be used  */
    Exchange(HttpRequestImpl request,
             MultiExchange<T> multi,
             @SuppressWarnings("removal") AccessControlContext acc)
    {
        this.request = request;
        this.acc = acc;
        this.upgrading = false;
        this.client = multi.client();
        this.multi = multi;
        this.parentExecutor = multi.executor;
        this.pushGroup = multi.pushGroup;
        this.dbgTag = "Exchange";
    }

    PushGroup<T> getPushGroup() {
        return pushGroup;
    }

    Executor executor() {
        return parentExecutor;
    }

    public HttpRequestImpl request() {
        return request;
    }

    public Optional<Duration> remainingConnectTimeout() {
        return multi.remainingConnectTimeout();
    }

    HttpClientImpl client() {
        return client;
    }

    // Keeps track of the underlying connection when establishing an HTTP/2
    // exchange so that it can be aborted/timed out mid setup.
    static final class ConnectionAborter {
        private volatile HttpConnection connection;
        private volatile boolean closeRequested;

        void connection(HttpConnection connection) {
            this.connection = connection;
            if (closeRequested) closeConnection();
        }

        void closeConnection() {
            closeRequested = true;
            HttpConnection connection = this.connection;
            this.connection = null;
            if (connection != null) {
                try {
                    connection.close();
                } catch (Throwable t) {
                    // ignore
                }
            }
        }

        void disable() {
            connection = null;
            closeRequested = false;
        }
    }

    // Called for 204 response - when no body is permitted
    // This is actually only needed for HTTP/1.1 in order
    // to return the connection to the pool (or close it)
    void nullBody(HttpResponse<T> resp, Throwable t) {
        exchImpl.nullBody(resp, t);
    }

    public CompletableFuture<T> readBodyAsync(HttpResponse.BodyHandler<T> handler) {
        // If we received a 407 while establishing the exchange
        // there will be no body to read: bodyIgnored will be true,
        // and exchImpl will be null (if we were trying to establish
        // an HTTP/2 tunnel through an HTTP/1.1 proxy)
        if (bodyIgnored != null) return MinimalFuture.completedFuture(null);

        // The connection will not be returned to the pool in the case of WebSocket
        return exchImpl.readBodyAsync(handler, !request.isWebSocket(), parentExecutor)
                .whenComplete((r,t) -> exchImpl.completed());
    }

    /**
     * Called after a redirect or similar kind of retry where a body might
     * be sent but we don't want it. Should send a RESET in h2. For http/1.1
     * we can consume small quantity of data, or close the connection in
     * other cases.
     */
    public CompletableFuture<Void> ignoreBody() {
        if (bodyIgnored != null) return bodyIgnored;
        return exchImpl.ignoreBody();
    }

    /**
     * Called when a new exchange is created to replace this exchange.
     * At this point it is guaranteed that readBody/readBodyAsync will
     * not be called.
     */
    public void released() {
        ExchangeImpl<?> impl = exchImpl;
        if (impl != null) impl.released();
        // Don't set exchImpl to null here. We need to keep
        // it alive until it's replaced by a Stream in wrapForUpgrade.
        // Setting it to null here might get it GC'ed too early, because
        // the Http1Response is now only weakly referenced by the Selector.
    }

    public void cancel() {
        // cancel can be called concurrently before or at the same time
        // that the exchange impl is being established.
        // In that case we won't be able to propagate the cancellation
        // right away
        if (exchImpl != null) {
            exchImpl.cancel();
        } else {
            // no impl - can't cancel impl yet.
            // call cancel(IOException) instead which takes care
            // of race conditions between impl/cancel.
            cancel(new IOException("Request cancelled"));
        }
    }

    public void cancel(IOException cause) {
        if (debug.on()) debug.log("cancel exchImpl: %s, with \"%s\"", exchImpl, cause);
        // If the impl is non null, propagate the exception right away.
        // Otherwise record it so that it can be propagated once the
        // exchange impl has been established.
        ExchangeImpl<?> impl = exchImpl;
        if (impl != null) {
            // propagate the exception to the impl
            if (debug.on()) debug.log("Cancelling exchImpl: %s", exchImpl);
            impl.cancel(cause);
        } else {
            // no impl yet. record the exception
            failed = cause;

            // abort/close the connection if setting up the exchange. This can
            // be important when setting up HTTP/2
            connectionAborter.closeConnection();

            // now call checkCancelled to recheck the impl.
            // if the failed state is set and the impl is not null, reset
            // the failed state and propagate the exception to the impl.
            checkCancelled();
        }
    }

    // This method will raise an exception if one was reported and if
    // it is possible to do so. If the exception can be raised, then
    // the failed state will be reset. Otherwise, the failed state
    // will persist until the exception can be raised and the failed state
    // can be cleared.
    // Takes care of possible race conditions.
    private void checkCancelled() {
        ExchangeImpl<?> impl = null;
        IOException cause = null;
        CompletableFuture<? extends ExchangeImpl<T>> cf = null;
        if (failed != null) {
            synchronized(this) {
                cause = failed;
                impl = exchImpl;
                cf = exchangeCF;
            }
        }
        if (cause == null) return;
        if (impl != null) {
            // The exception is raised by propagating it to the impl.
            if (debug.on()) debug.log("Cancelling exchImpl: %s", impl);
            impl.cancel(cause);
            failed = null;
        } else {
            Log.logTrace("Exchange: request [{0}/timeout={1}ms] no impl is set."
                         + "\n\tCan''t cancel yet with {2}",
                         request.uri(),
                         request.timeout().isPresent() ?
                         // calling duration.toMillis() can throw an exception.
                         // this is just debugging, we don't care if it overflows.
                         (request.timeout().get().getSeconds() * 1000
                          + request.timeout().get().getNano() / 1000000) : -1,
                         cause);
            if (cf != null) cf.completeExceptionally(cause);
        }
    }

    <T> CompletableFuture<T> checkCancelled(CompletableFuture<T> cf, HttpConnection connection) {
        return cf.handle((r,t) -> {
            if (t == null) {
                if (multi.requestCancelled()) {
                    // if upgraded, we don't close the connection.
                    // cancelling will be handled by the HTTP/2 exchange
                    // in its own time.
                    if (!upgraded) {
                        t = getCancelCause();
                        if (t == null) t = new IOException("Request cancelled");
                        if (debug.on()) debug.log("exchange cancelled during connect: " + t);
                        try {
                            connection.close();
                        } catch (Throwable x) {
                            if (debug.on()) debug.log("Failed to close connection", x);
                        }
                        return MinimalFuture.<T>failedFuture(t);
                    }
                }
            }
            return cf;
        }).thenCompose(Function.identity());
    }

    public void h2Upgrade() {
        upgrading = true;
        request.setH2Upgrade(client.client2());
    }

    synchronized IOException getCancelCause() {
        return failed;
    }

    // get/set the exchange impl, solving race condition issues with
    // potential concurrent calls to cancel() or cancel(IOException)
    private CompletableFuture<? extends ExchangeImpl<T>>
    establishExchange(HttpConnection connection) {
        if (debug.on()) {
            debug.log("establishing exchange for %s,%n\t proxy=%s",
                      request, request.proxy());
        }
        // check if we have been cancelled first.
        Throwable t = getCancelCause();
        checkCancelled();
        if (t != null) {
            if (debug.on()) {
                debug.log("exchange was cancelled: returned failed cf (%s)", String.valueOf(t));
            }
            return exchangeCF = MinimalFuture.failedFuture(t);
        }

        CompletableFuture<? extends ExchangeImpl<T>> cf, res;
        cf = ExchangeImpl.get(this, connection);
        // We should probably use a VarHandle to get/set exchangeCF
        // instead - as we need CAS semantics.
        synchronized (this) { exchangeCF = cf; };
        res = cf.whenComplete((r,x) -> {
            synchronized(Exchange.this) {
                if (exchangeCF == cf) exchangeCF = null;
            }
        });
        checkCancelled();
        return res.thenCompose((eimpl) -> {
                    // recheck for cancelled, in case of race conditions
                    exchImpl = eimpl;
                    IOException tt = getCancelCause();
                    checkCancelled();
                    if (tt != null) {
                        return MinimalFuture.failedFuture(tt);
                    } else {
                        // Now we're good to go. Because exchImpl is no longer
                        // null cancel() will be able to propagate directly to
                        // the impl after this point ( if needed ).
                        return MinimalFuture.completedFuture(eimpl);
                    } });
    }

    // Completed HttpResponse will be null if response succeeded
    // will be a non null responseAsync if expect continue returns an error

    public CompletableFuture<Response> responseAsync() {
        return responseAsyncImpl(null);
    }

    CompletableFuture<Response> responseAsyncImpl(HttpConnection connection) {
        SecurityException e = checkPermissions();
        if (e != null) {
            return MinimalFuture.failedFuture(e);
        } else {
            return responseAsyncImpl0(connection);
        }
    }

    // check whether the headersSentCF was completed exceptionally with
    // ProxyAuthorizationRequired. If so the Response embedded in the
    // exception is returned. Otherwise we proceed.
    private CompletableFuture<Response> checkFor407(ExchangeImpl<T> ex, Throwable t,
                                                    Function<ExchangeImpl<T>,CompletableFuture<Response>> andThen) {
        t = Utils.getCompletionCause(t);
        if (t instanceof ProxyAuthenticationRequired) {
            if (debug.on()) debug.log("checkFor407: ProxyAuthenticationRequired: building synthetic response");
            bodyIgnored = MinimalFuture.completedFuture(null);
            Response proxyResponse = ((ProxyAuthenticationRequired)t).proxyResponse;
            HttpConnection c = ex == null ? null : ex.connection();
            Response syntheticResponse = new Response(request, this,
                    proxyResponse.headers, c, proxyResponse.statusCode,
                    proxyResponse.version, true);
            return MinimalFuture.completedFuture(syntheticResponse);
        } else if (t != null) {
            if (debug.on()) debug.log("checkFor407: no response - %s", (Object)t);
            return MinimalFuture.failedFuture(t);
        } else {
            if (debug.on()) debug.log("checkFor407: all clear");
            return andThen.apply(ex);
        }
    }

    // After sending the request headers, if no ProxyAuthorizationRequired
    // was raised and the expectContinue flag is on, we need to wait
    // for the 100-Continue response
    private CompletableFuture<Response> expectContinue(ExchangeImpl<T> ex) {
        assert request.expectContinue();
        return ex.getResponseAsync(parentExecutor)
                .thenCompose((Response r1) -> {
            Log.logResponse(r1::toString);
            int rcode = r1.statusCode();
            if (rcode == 100) {
                Log.logTrace("Received 100-Continue: sending body");
                if (debug.on()) debug.log("Received 100-Continue for %s", r1);
                CompletableFuture<Response> cf =
                        exchImpl.sendBodyAsync()
                                .thenCompose(exIm -> exIm.getResponseAsync(parentExecutor));
                cf = wrapForUpgrade(cf);
                cf = wrapForLog(cf);
                return cf;
            } else {
                Log.logTrace("Expectation failed: Received {0}",
                        rcode);
                if (debug.on()) debug.log("Expect-Continue failed (%d) for: %s", rcode, r1);
                if (upgrading && rcode == 101) {
                    IOException failed = new IOException(
                            "Unable to handle 101 while waiting for 100");
                    return MinimalFuture.failedFuture(failed);
                }
                return exchImpl.readBodyAsync(this::ignoreBody, false, parentExecutor)
                        .thenApply(v ->  r1);
            }
        });
    }

    // After sending the request headers, if no ProxyAuthorizationRequired
    // was raised and the expectContinue flag is off, we can immediately
    // send the request body and proceed.
    private CompletableFuture<Response> sendRequestBody(ExchangeImpl<T> ex) {
        assert !request.expectContinue();
        if (debug.on()) debug.log("sendRequestBody");
        CompletableFuture<Response> cf = ex.sendBodyAsync()
                .thenCompose(exIm -> exIm.getResponseAsync(parentExecutor));
        cf = wrapForUpgrade(cf);
        cf = wrapForLog(cf);
        return cf;
    }

    CompletableFuture<Response> responseAsyncImpl0(HttpConnection connection) {
        Function<ExchangeImpl<T>, CompletableFuture<Response>> after407Check;
        bodyIgnored = null;
        if (request.expectContinue()) {
            request.addSystemHeader("Expect", "100-Continue");
            Log.logTrace("Sending Expect: 100-Continue");
            // wait for 100-Continue before sending body
            after407Check = this::expectContinue;
        } else {
            // send request body and proceed.
            after407Check = this::sendRequestBody;
        }
        // The ProxyAuthorizationRequired can be triggered either by
        // establishExchange (case of HTTP/2 SSL tunneling through HTTP/1.1 proxy
        // or by sendHeaderAsync (case of HTTP/1.1 SSL tunneling through HTTP/1.1 proxy
        // Therefore we handle it with a call to this checkFor407(...) after these
        // two places.
        Function<ExchangeImpl<T>, CompletableFuture<Response>> afterExch407Check =
                (ex) -> ex.sendHeadersAsync()
                        .handle((r,t) -> this.checkFor407(r, t, after407Check))
                        .thenCompose(Function.identity());
        return establishExchange(connection)
                .handle((r,t) -> this.checkFor407(r,t, afterExch407Check))
                .thenCompose(Function.identity());
    }

    private CompletableFuture<Response> wrapForUpgrade(CompletableFuture<Response> cf) {
        if (upgrading) {
            return cf.thenCompose(r -> checkForUpgradeAsync(r, exchImpl));
        }
        return cf;
    }

    private CompletableFuture<Response> wrapForLog(CompletableFuture<Response> cf) {
        if (Log.requests()) {
            return cf.thenApply(response -> {
                Log.logResponse(response::toString);
                return response;
            });
        }
        return cf;
    }

    HttpResponse.BodySubscriber<T> ignoreBody(HttpResponse.ResponseInfo hdrs) {
        return HttpResponse.BodySubscribers.replacing(null);
    }

    // if this response was received in reply to an upgrade
    // then create the Http2Connection from the HttpConnection
    // initialize it and wait for the real response on a newly created Stream

    private CompletableFuture<Response>
    checkForUpgradeAsync(Response resp,
                         ExchangeImpl<T> ex) {

        int rcode = resp.statusCode();
        if (upgrading && (rcode == 101)) {
            Http1Exchange<T> e = (Http1Exchange<T>)ex;
            // check for 101 switching protocols
            // 101 responses are not supposed to contain a body.
            //    => should we fail if there is one?
            if (debug.on()) debug.log("Upgrading async %s", e.connection());
            return e.readBodyAsync(this::ignoreBody, false, parentExecutor)
                .thenCompose((T v) -> {// v is null
                    debug.log("Ignored body");
                    // we pass e::getBuffer to allow the ByteBuffers to accumulate
                    // while we build the Http2Connection
                    ex.upgraded();
                    upgraded = true;
                    return Http2Connection.createAsync(e.connection(),
                                                 client.client2(),
                                                 this, e::drainLeftOverBytes)
                        .thenCompose((Http2Connection c) -> {
                            boolean cached = c.offerConnection();
                            if (cached) connectionAborter.disable();
                            Stream<T> s = c.getStream(1);

                            if (s == null) {
                                // s can be null if an exception occurred
                                // asynchronously while sending the preface.
                                Throwable t = c.getRecordedCause();
                                IOException ioe;
                                if (t != null) {
                                    if (!cached)
                                        c.close();
                                    ioe = new IOException("Can't get stream 1: " + t, t);
                                } else {
                                    ioe = new IOException("Can't get stream 1");
                                }
                                return MinimalFuture.failedFuture(ioe);
                            }
                            exchImpl.released();
                            Throwable t;
                            // There's a race condition window where an external
                            // thread (SelectorManager) might complete the
                            // exchange in timeout at the same time where we're
                            // trying to switch the exchange impl.
                            // 'failed' will be reset to null after
                            // exchImpl.cancel() has completed, so either we
                            // will observe failed != null here, or we will
                            // observe e.getCancelCause() != null, or the
                            // timeout exception will be routed to 's'.
                            // Either way, we need to relay it to s.
                            synchronized (this) {
                                exchImpl = s;
                                t = failed;
                            }
                            // Check whether the HTTP/1.1 was cancelled.
                            if (t == null) t = e.getCancelCause();
                            // if HTTP/1.1 exchange was timed out, or the request
                            // was cancelled don't try to go further.
                            if (t instanceof HttpTimeoutException || multi.requestCancelled()) {
                                if (t == null) t = new IOException("Request cancelled");
                                s.cancelImpl(t);
                                return MinimalFuture.failedFuture(t);
                            }
                            if (debug.on())
                                debug.log("Getting response async %s", s);
                            return s.getResponseAsync(null);
                        });}
                );
        }
        return MinimalFuture.completedFuture(resp);
    }

    private URI getURIForSecurityCheck() {
        URI u;
        String method = request.method();
        InetSocketAddress authority = request.authority();
        URI uri = request.uri();

        // CONNECT should be restricted at API level
        if (method.equalsIgnoreCase("CONNECT")) {
            try {
                u = new URI("socket",
                             null,
                             authority.getHostString(),
                             authority.getPort(),
                             null,
                             null,
                             null);
            } catch (URISyntaxException e) {
                throw new InternalError(e); // shouldn't happen
            }
        } else {
            u = uri;
        }
        return u;
    }

    /**
     * Returns the security permission required for the given details.
     * If method is CONNECT, then uri must be of form "scheme://host:port"
     */
    private static URLPermission permissionForServer(URI uri,
                                                     String method,
                                                     Map<String, List<String>> headers) {
        if (method.equals("CONNECT")) {
            return new URLPermission(uri.toString(), "CONNECT");
        } else {
            return Utils.permissionForServer(uri, method, headers.keySet().stream());
        }
    }

    /**
     * Performs the necessary security permission checks required to retrieve
     * the response. Returns a security exception representing the denied
     * permission, or null if all checks pass or there is no security manager.
     */
    private SecurityException checkPermissions() {
        String method = request.method();
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || method.equals("CONNECT")) {
            // tunneling will have a null acc, which is fine. The proxy
            // permission check will have already been preformed.
            return null;
        }

        HttpHeaders userHeaders = request.getUserHeaders();
        URI u = getURIForSecurityCheck();
        URLPermission p = permissionForServer(u, method, userHeaders.map());

        try {
            assert acc != null;
            sm.checkPermission(p, acc);
        } catch (SecurityException e) {
            return e;
        }
        String hostHeader = userHeaders.firstValue("Host").orElse(null);
        if (hostHeader != null && !hostHeader.equalsIgnoreCase(u.getHost())) {
            // user has set a Host header different to request URI
            // must check that for URLPermission also
            URI u1 = replaceHostInURI(u, hostHeader);
            URLPermission p1 = permissionForServer(u1, method, userHeaders.map());
            try {
                assert acc != null;
                sm.checkPermission(p1, acc);
            } catch (SecurityException e) {
                return e;
            }
        }
        ProxySelector ps = client.proxySelector();
        if (ps != null) {
            if (!method.equals("CONNECT")) {
                // a non-tunneling HTTP proxy. Need to check access
                URLPermission proxyPerm = permissionForProxy(request.proxy());
                if (proxyPerm != null) {
                    try {
                        sm.checkPermission(proxyPerm, acc);
                    } catch (SecurityException e) {
                        return e;
                    }
                }
            }
        }
        return null;
    }

    private static URI replaceHostInURI(URI u, String hostPort) {
        StringBuilder sb = new StringBuilder();
        sb.append(u.getScheme())
                .append("://")
                .append(hostPort)
                .append(u.getRawPath());
        return URI.create(sb.toString());
    }

    HttpClient.Version version() {
        return multi.version();
    }

    String dbgString() {
        return dbgTag;
    }
}
