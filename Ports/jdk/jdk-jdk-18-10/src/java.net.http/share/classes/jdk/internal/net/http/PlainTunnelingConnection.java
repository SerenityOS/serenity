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
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.net.http.HttpHeaders;
import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.MinimalFuture;
import static java.net.http.HttpResponse.BodyHandlers.discarding;
import static jdk.internal.net.http.common.Utils.ProxyHeaders;

/**
 * A plain text socket tunnel through a proxy. Uses "CONNECT" but does not
 * encrypt. Used by WebSocket, as well as HTTP over SSL + Proxy.
 * Wrapped in SSLTunnelConnection or AsyncSSLTunnelConnection for encryption.
 */
final class PlainTunnelingConnection extends HttpConnection {

    final PlainHttpConnection delegate;
    final ProxyHeaders proxyHeaders;
    final InetSocketAddress proxyAddr;
    private volatile boolean connected;

    protected PlainTunnelingConnection(InetSocketAddress addr,
                                       InetSocketAddress proxy,
                                       HttpClientImpl client,
                                       ProxyHeaders proxyHeaders) {
        super(addr, client);
        this.proxyAddr = proxy;
        this.proxyHeaders = proxyHeaders;
        delegate = new PlainHttpConnection(proxy, client);
    }

    @Override
    public CompletableFuture<Void> connectAsync(Exchange<?> exchange) {
        if (debug.on()) debug.log("Connecting plain connection");
        return delegate.connectAsync(exchange)
            .thenCompose(unused -> delegate.finishConnect())
            .thenCompose((Void v) -> {
                if (debug.on()) debug.log("sending HTTP/1.1 CONNECT");
                HttpClientImpl client = client();
                assert client != null;
                HttpRequestImpl req = new HttpRequestImpl("CONNECT", address, proxyHeaders);
                MultiExchange<Void> mulEx = new MultiExchange<>(null, req,
                        client, discarding(), null, null);
                Exchange<Void> connectExchange = mulEx.getExchange();

                return connectExchange
                        .responseAsyncImpl(delegate)
                        .thenCompose((Response resp) -> {
                            CompletableFuture<Void> cf = new MinimalFuture<>();
                            if (debug.on()) debug.log("got response: %d", resp.statusCode());
                            if (resp.statusCode() == 407) {
                                return connectExchange.ignoreBody().handle((r,t) -> {
                                    // close delegate after reading body: we won't
                                    // be reusing that connection anyway.
                                    delegate.close();
                                    ProxyAuthenticationRequired authenticationRequired =
                                            new ProxyAuthenticationRequired(resp);
                                    cf.completeExceptionally(authenticationRequired);
                                    return cf;
                                }).thenCompose(Function.identity());
                            } else if (resp.statusCode() != 200) {
                                delegate.close();
                                cf.completeExceptionally(new IOException(
                                        "Tunnel failed, got: "+ resp.statusCode()));
                            } else {
                                // get the initial/remaining bytes
                                ByteBuffer b = ((Http1Exchange<?>)connectExchange.exchImpl).drainLeftOverBytes();
                                int remaining = b.remaining();
                                assert remaining == 0: "Unexpected remaining: " + remaining;
                                cf.complete(null);
                            }
                            return cf;
                        })
                        .handle((result, ex) -> {
                            if (ex == null) {
                                return MinimalFuture.completedFuture(result);
                            } else {
                                if (debug.on())
                                    debug.log("tunnel failed with \"%s\"", ex.toString());
                                Throwable t = ex;
                                if (t instanceof CompletionException)
                                    t = t.getCause();
                                if (t instanceof HttpTimeoutException) {
                                    String msg = "proxy tunneling CONNECT request timed out";
                                    t = new HttpTimeoutException(msg);
                                    t.initCause(ex);
                                }
                                return MinimalFuture.<Void>failedFuture(t);
                            }
                        })
                        .thenCompose(Function.identity());
            });
    }

    public CompletableFuture<Void> finishConnect() {
        connected = true;
        return MinimalFuture.completedFuture(null);
    }

    @Override
    boolean isTunnel() { return true; }

    @Override
    HttpPublisher publisher() { return delegate.publisher(); }

    @Override
    boolean connected() {
        return connected;
    }

    @Override
    SocketChannel channel() {
        return delegate.channel();
    }

    @Override
    FlowTube getConnectionFlow() {
        return delegate.getConnectionFlow();
    }

    @Override
    ConnectionPool.CacheKey cacheKey() {
        return new ConnectionPool.CacheKey(null, proxyAddr);
    }

    @Override
    public void close() {
        delegate.close();
        connected = false;
    }

    @Override
    boolean isSecure() {
        return false;
    }

    @Override
    boolean isProxied() {
        return true;
    }

    @Override
    InetSocketAddress proxy() {
        return proxyAddr;
    }
}
