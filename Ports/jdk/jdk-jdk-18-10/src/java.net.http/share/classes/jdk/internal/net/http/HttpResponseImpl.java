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
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.net.ssl.SSLSession;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jdk.internal.net.http.websocket.RawChannel;

/**
 * The implementation class for HttpResponse
 */
class HttpResponseImpl<T> implements HttpResponse<T>, RawChannel.Provider {

    final int responseCode;
    final HttpRequest initialRequest;
    final Optional<HttpResponse<T>> previousResponse;
    final HttpHeaders headers;
    final Optional<SSLSession> sslSession;
    final URI uri;
    final HttpClient.Version version;
    final RawChannelProvider rawChannelProvider;
    final T body;

    public HttpResponseImpl(HttpRequest initialRequest,
                            Response response,
                            HttpResponse<T> previousResponse,
                            T body,
                            Exchange<T> exch) {
        this.responseCode = response.statusCode();
        this.initialRequest = initialRequest;
        this.previousResponse = Optional.ofNullable(previousResponse);
        this.headers = response.headers();
        //this.trailers = trailers;
        this.sslSession = Optional.ofNullable(response.getSSLSession());
        this.uri = response.request().uri();
        this.version = response.version();
        this.rawChannelProvider = RawChannelProvider.create(response, exch);
        this.body = body;
    }

    @Override
    public int statusCode() {
        return responseCode;
    }

    @Override
    public HttpRequest request() {
        return initialRequest;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return previousResponse;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public T body() {
        return body;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return sslSession;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public HttpClient.Version version() {
        return version;
    }
    // keepalive flag determines whether connection is closed or kept alive
    // by reading/skipping data

    /**
     * Returns a RawChannel that may be used for WebSocket protocol.
     * @implNote This implementation does not support RawChannel over
     *           HTTP/2 connections.
     * @return a RawChannel that may be used for WebSocket protocol.
     * @throws UnsupportedOperationException if getting a RawChannel over
     *         this connection is not supported.
     * @throws IOException if an I/O exception occurs while retrieving
     *         the channel.
     */
    @Override
    public synchronized RawChannel rawChannel() throws IOException {
        if (rawChannelProvider == null) {
            throw new UnsupportedOperationException(
                    "RawChannel is only supported for WebSocket creation");
        }
        return rawChannelProvider.rawChannel();
    }

    /**
     * Closes the RawChannel that may have been used for WebSocket protocol.
     *
     * @apiNote This method should be called to close the connection
     * if an exception occurs during the websocket handshake, in cases where
     * {@link #rawChannel() rawChannel().close()} would have been called.
     * An unsuccessful handshake may prevent the creation of the RawChannel:
     * if a RawChannel has already been created, this method wil close it.
     * Otherwise, it will close the connection.
     *
     * @throws UnsupportedOperationException if getting a RawChannel over
     *         this connection is not supported.
     * @throws IOException if an I/O exception occurs while closing
     *         the channel.
     */
    @Override
    public synchronized void closeRawChannel() throws IOException {
        if (rawChannelProvider == null) {
            throw new UnsupportedOperationException(
                    "RawChannel is only supported for WebSocket creation");
        }
        rawChannelProvider.closeRawChannel();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String method = request().method();
        URI uri = request().uri();
        String uristring = uri == null ? "" : uri.toString();
        sb.append('(')
          .append(method)
          .append(" ")
          .append(uristring)
          .append(") ")
          .append(statusCode());
        return sb.toString();
    }

    /**
     * An auxiliary class used for RawChannel creation when creating a WebSocket.
     * This avoids keeping around references to connection/exchange in the
     * regular HttpResponse case. Only those responses corresponding to an
     * initial WebSocket request have a RawChannelProvider.
     */
    private static final class RawChannelProvider implements RawChannel.Provider {
        private final HttpConnection connection;
        private final Exchange<?> exchange;
        private RawChannel rawchan;
        RawChannelProvider(HttpConnection conn, Exchange<?> exch) {
            connection = conn;
            exchange = exch;
        }

        static RawChannelProvider create(Response resp, Exchange<?> exch) {
            if (resp.request().isWebSocket()) {
                return new RawChannelProvider(connection(resp, exch), exch);
            }
            return null;
        }

        @Override
        public synchronized RawChannel rawChannel() {
            if (rawchan == null) {
                ExchangeImpl<?> exchImpl = exchangeImpl();
                if (!(exchImpl instanceof Http1Exchange)) {
                    // RawChannel is only used for WebSocket - and WebSocket
                    // is not supported over HTTP/2 yet, so we should not come
                    // here. Getting a RawChannel over HTTP/2 might be supported
                    // in the future, but it would entail retrieving any left over
                    // bytes that might have been read but not consumed by the
                    // HTTP/2 connection.
                    throw new UnsupportedOperationException("RawChannel is not supported over HTTP/2");
                }
                // Http1Exchange may have some remaining bytes in its
                // internal buffer.
                Supplier<ByteBuffer> initial = ((Http1Exchange<?>) exchImpl)::drainLeftOverBytes;
                rawchan = new RawChannelTube(connection, initial);
            }
            return rawchan;
        }

        public synchronized void closeRawChannel() throws IOException {
            //  close the rawChannel, if created, or the
            // connection, if not.
            if (rawchan != null) rawchan.close();
            else connection.close();
        }

        private static HttpConnection connection(Response resp, Exchange<?> exch) {
            if (exch == null || exch.exchImpl == null) {
                assert resp.statusCode == 407;
                return null; // case of Proxy 407
            }
            return exch.exchImpl.connection();
        }

        private ExchangeImpl<?> exchangeImpl() {
            return exchange != null ? exchange.exchImpl : null;
        }

    }
}
