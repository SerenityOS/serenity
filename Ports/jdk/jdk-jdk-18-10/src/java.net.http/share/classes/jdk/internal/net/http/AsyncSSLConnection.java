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

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.SSLTube;
import jdk.internal.net.http.common.Utils;

/**
 * Asynchronous version of SSLConnection.
 */
class AsyncSSLConnection extends AbstractAsyncSSLConnection {

    final PlainHttpConnection plainConnection;
    final PlainHttpPublisher writePublisher;
    private volatile SSLTube flow;

    AsyncSSLConnection(InetSocketAddress addr,
                       HttpClientImpl client,
                       String[] alpn) {
        super(addr, client, Utils.getServerName(addr), addr.getPort(), alpn);
        plainConnection = new PlainHttpConnection(addr, client);
        writePublisher = new PlainHttpPublisher();
    }

    @Override
    public CompletableFuture<Void> connectAsync(Exchange<?> exchange) {
        return plainConnection
                .connectAsync(exchange)
                .thenApply( unused -> {
                    // create the SSLTube wrapping the SocketTube, with the given engine
                    flow = new SSLTube(engine,
                                       client().theExecutor(),
                                       client().getSSLBufferSupplier()::recycle,
                                       plainConnection.getConnectionFlow());
                    return null; } );
    }

    @Override
    public CompletableFuture<Void> finishConnect() {
        // The actual ALPN value, which may be the empty string, is not
        // interesting at this point, only that the handshake has completed.
        return getALPN()
                .handle((String unused, Throwable ex) -> {
                    if (ex == null) {
                        return plainConnection.finishConnect();
                    } else {
                        plainConnection.close();
                        return MinimalFuture.<Void>failedFuture(ex);
                    } })
                .thenCompose(Function.identity());
    }

    @Override
    boolean connected() {
        return plainConnection.connected();
    }

    @Override
    HttpPublisher publisher() { return writePublisher; }

    @Override
    boolean isProxied() {
        return false;
    }

    @Override
    InetSocketAddress proxy() {
        return null;
    }

    @Override
    SocketChannel channel() {
        return plainConnection.channel();
    }

    @Override
    ConnectionPool.CacheKey cacheKey() {
        return ConnectionPool.cacheKey(address, null);
    }

    @Override
    public void close() {
        plainConnection.close();
    }

    @Override
    SSLTube getConnectionFlow() {
       return flow;
   }
}
