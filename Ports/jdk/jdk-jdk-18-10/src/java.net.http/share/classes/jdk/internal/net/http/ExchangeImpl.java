/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;

import static java.net.http.HttpClient.Version.HTTP_1_1;

/**
 * Splits request so that headers and body can be sent separately with optional
 * (multiple) responses in between (e.g. 100 Continue). Also request and
 * response always sent/received in different calls.
 *
 * Synchronous and asynchronous versions of each method are provided.
 *
 * Separate implementations of this class exist for HTTP/1.1 and HTTP/2
 *      Http1Exchange   (HTTP/1.1)
 *      Stream          (HTTP/2)
 *
 * These implementation classes are where work is allocated to threads.
 */
abstract class ExchangeImpl<T> {

    private static final Logger debug =
            Utils.getDebugLogger("ExchangeImpl"::toString, Utils.DEBUG);

    final Exchange<T> exchange;

    ExchangeImpl(Exchange<T> e) {
        // e == null means a http/2 pushed stream
        this.exchange = e;
    }

    final Exchange<T> getExchange() {
        return exchange;
    }

    HttpClient client() {
        return exchange.client();
    }

    /**
     * Returns the {@link HttpConnection} instance to which this exchange is
     * assigned.
     */
    abstract HttpConnection connection();

    /**
     * Initiates a new exchange and assigns it to a connection if one exists
     * already. connection usually null.
     */
    static <U> CompletableFuture<? extends ExchangeImpl<U>>
    get(Exchange<U> exchange, HttpConnection connection)
    {
        if (exchange.version() == HTTP_1_1) {
            if (debug.on())
                debug.log("get: HTTP/1.1: new Http1Exchange");
            return createHttp1Exchange(exchange, connection);
        } else {
            Http2ClientImpl c2 = exchange.client().client2(); // #### improve
            HttpRequestImpl request = exchange.request();
            CompletableFuture<Http2Connection> c2f = c2.getConnectionFor(request, exchange);
            if (debug.on())
                debug.log("get: Trying to get HTTP/2 connection");
            // local variable required here; see JDK-8223553
            CompletableFuture<CompletableFuture<? extends ExchangeImpl<U>>> fxi =
                c2f.handle((h2c, t) -> createExchangeImpl(h2c, t, exchange, connection));
            return fxi.thenCompose(x->x);
        }
    }

    private static <U> CompletableFuture<? extends ExchangeImpl<U>>
    createExchangeImpl(Http2Connection c,
                       Throwable t,
                       Exchange<U> exchange,
                       HttpConnection connection)
    {
        if (debug.on())
            debug.log("handling HTTP/2 connection creation result");
        boolean secure = exchange.request().secure();
        if (t != null) {
            if (debug.on())
                debug.log("handling HTTP/2 connection creation failed: %s",
                                 (Object)t);
            t = Utils.getCompletionCause(t);
            if (t instanceof Http2Connection.ALPNException) {
                Http2Connection.ALPNException ee = (Http2Connection.ALPNException)t;
                AbstractAsyncSSLConnection as = ee.getConnection();
                if (debug.on())
                    debug.log("downgrading to HTTP/1.1 with: %s", as);
                CompletableFuture<? extends ExchangeImpl<U>> ex =
                        createHttp1Exchange(exchange, as);
                return ex;
            } else {
                if (debug.on())
                    debug.log("HTTP/2 connection creation failed "
                                     + "with unexpected exception: %s", (Object)t);
                return MinimalFuture.failedFuture(t);
            }
        }
        if (secure && c== null) {
            if (debug.on())
                debug.log("downgrading to HTTP/1.1 ");
            CompletableFuture<? extends ExchangeImpl<U>> ex =
                    createHttp1Exchange(exchange, null);
            return ex;
        }
        if (c == null) {
            // no existing connection. Send request with HTTP 1 and then
            // upgrade if successful
            if (debug.on())
                debug.log("new Http1Exchange, try to upgrade");
            return createHttp1Exchange(exchange, connection)
                    .thenApply((e) -> {
                        exchange.h2Upgrade();
                        return e;
                    });
        } else {
            if (debug.on()) debug.log("creating HTTP/2 streams");
            Stream<U> s = c.createStream(exchange);
            CompletableFuture<? extends ExchangeImpl<U>> ex = MinimalFuture.completedFuture(s);
            return ex;
        }
    }

    private static <T> CompletableFuture<Http1Exchange<T>>
    createHttp1Exchange(Exchange<T> ex, HttpConnection as)
    {
        try {
            return MinimalFuture.completedFuture(new Http1Exchange<>(ex, as));
        } catch (Throwable e) {
            return MinimalFuture.failedFuture(e);
        }
    }

    // Called for 204 response - when no body is permitted
    void nullBody(HttpResponse<T> resp, Throwable t) {
        // Needed for HTTP/1.1 to close the connection or return it to the pool
        // Needed for HTTP/2 to subscribe a dummy subscriber and close the stream
    }

    /* The following methods have separate HTTP/1.1 and HTTP/2 implementations */

    abstract CompletableFuture<ExchangeImpl<T>> sendHeadersAsync();

    /** Sends a request body, after request headers have been sent. */
    abstract CompletableFuture<ExchangeImpl<T>> sendBodyAsync();

    abstract CompletableFuture<T> readBodyAsync(HttpResponse.BodyHandler<T> handler,
                                                boolean returnConnectionToPool,
                                                Executor executor);

    /**
     * Ignore/consume the body.
     */
    abstract CompletableFuture<Void> ignoreBody();


    /** Gets the response headers. Completes before body is read. */
    abstract CompletableFuture<Response> getResponseAsync(Executor executor);


    /** Cancels a request.  Not currently exposed through API. */
    abstract void cancel();

    /**
     * Cancels a request with a cause.  Not currently exposed through API.
     */
    abstract void cancel(IOException cause);

    /**
     * Called when the exchange is released, so that cleanup actions may be
     * performed - such as deregistering callbacks.
     * Typically released is called during upgrade, when an HTTP/2 stream
     * takes over from an Http1Exchange, or when a new exchange is created
     * during a multi exchange before the final response body was received.
     */
    abstract void released();

    /**
     * Called when the exchange is completed, so that cleanup actions may be
     * performed - such as deregistering callbacks.
     * Typically, completed is called at the end of the exchange, when the
     * final response body has been received (or an error has caused the
     * completion of the exchange).
     */
    abstract void completed();

    /**
     * Returns true if this exchange was canceled.
     * @return true if this exchange was canceled.
     */
    abstract boolean isCanceled();

    /**
     * Returns the cause for which this exchange was canceled, if available.
     * @return the cause for which this exchange was canceled, if available.
     */
    abstract Throwable getCancelCause();

    // Mark the exchange as upgraded
    // Needed to handle cancellation during the upgrade from
    // Http1Exchange to Stream
    void upgraded() { }
}
