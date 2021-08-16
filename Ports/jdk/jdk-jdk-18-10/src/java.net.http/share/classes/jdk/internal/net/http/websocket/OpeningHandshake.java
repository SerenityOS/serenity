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

package jdk.internal.net.http.websocket;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocketHandshakeException;

import jdk.internal.net.http.HttpRequestBuilderImpl;
import jdk.internal.net.http.HttpRequestImpl;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Pair;
import jdk.internal.net.http.common.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLPermission;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static jdk.internal.net.http.common.Utils.isValidName;
import static jdk.internal.net.http.common.Utils.permissionForProxy;
import static jdk.internal.net.http.common.Utils.stringOf;

public class OpeningHandshake {

    private static final String HEADER_CONNECTION = "Connection";
    private static final String HEADER_UPGRADE    = "Upgrade";
    private static final String HEADER_ACCEPT     = "Sec-WebSocket-Accept";
    private static final String HEADER_EXTENSIONS = "Sec-WebSocket-Extensions";
    private static final String HEADER_KEY        = "Sec-WebSocket-Key";
    private static final String HEADER_PROTOCOL   = "Sec-WebSocket-Protocol";
    private static final String HEADER_VERSION    = "Sec-WebSocket-Version";
    private static final String VERSION           = "13";  // WebSocket's lucky number

    private static final Set<String> ILLEGAL_HEADERS;

    static {
        ILLEGAL_HEADERS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ILLEGAL_HEADERS.addAll(List.of(HEADER_ACCEPT,
                                       HEADER_EXTENSIONS,
                                       HEADER_KEY,
                                       HEADER_PROTOCOL,
                                       HEADER_VERSION));
    }

    private static final SecureRandom random = new SecureRandom();

    private final MessageDigest sha1;
    private final HttpClient client;

    {
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // Shouldn't happen: SHA-1 must be available in every Java platform
            // implementation
            throw new InternalError("Minimum requirements", e);
        }
    }

    private final HttpRequestImpl request;
    private final Collection<String> subprotocols;
    private final String nonce;

    public OpeningHandshake(BuilderImpl b) {
        checkURI(b.getUri());
        Proxy proxy = proxyFor(b.getProxySelector(), b.getUri());
        checkPermissions(b, proxy);
        this.client = b.getClient();
        URI httpURI = createRequestURI(b.getUri());
        HttpRequestBuilderImpl requestBuilder = new HttpRequestBuilderImpl(httpURI);
        Duration connectTimeout = b.getConnectTimeout();
        if (connectTimeout != null) {
            requestBuilder.timeout(connectTimeout);
        }
        for (Pair<String, String> p : b.getHeaders()) {
            if (ILLEGAL_HEADERS.contains(p.first)) {
                throw illegal("Illegal header: " + p.first);
            }
            requestBuilder.header(p.first, p.second);
        }
        this.subprotocols = createRequestSubprotocols(b.getSubprotocols());
        if (!this.subprotocols.isEmpty()) {
            String p = String.join(", ", this.subprotocols);
            requestBuilder.header(HEADER_PROTOCOL, p);
        }
        requestBuilder.header(HEADER_VERSION, VERSION);
        this.nonce = createNonce();
        requestBuilder.header(HEADER_KEY, this.nonce);
        // Setting request version to HTTP/1.1 forcibly, since it's not possible
        // to upgrade from HTTP/2 to WebSocket (as of August 2016):
        //
        //     https://tools.ietf.org/html/draft-hirano-httpbis-websocket-over-http2-00
        requestBuilder.version(Version.HTTP_1_1).GET();
        request = requestBuilder.buildForWebSocket();
        request.isWebSocket(true);
        Utils.setWebSocketUpgradeHeaders(request);
        request.setProxy(proxy);
    }

    private static Collection<String> createRequestSubprotocols(
            Collection<String> subprotocols)
    {
        LinkedHashSet<String> sp = new LinkedHashSet<>(subprotocols.size(), 1);
        for (String s : subprotocols) {
            if (s.trim().isEmpty() || !isValidName(s)) {
                throw illegal("Bad subprotocol syntax: " + s);
            }
            if (!sp.add(s)) {
                throw illegal("Duplicating subprotocol: " + s);
            }
        }
        return Collections.unmodifiableCollection(sp);
    }

    /*
     * Checks the given URI for being a WebSocket URI and translates it into a
     * target HTTP URI for the Opening Handshake.
     *
     * https://tools.ietf.org/html/rfc6455#section-3
     */
    static URI createRequestURI(URI uri) {
        String s = uri.getScheme();
        assert "ws".equalsIgnoreCase(s) || "wss".equalsIgnoreCase(s);
        String newUri = uri.toString();
        if (s.equalsIgnoreCase("ws")) {
            newUri = "http" + newUri.substring(2);
        }
        else {
            newUri = "https" + newUri.substring(3);
        }

        try {
            return new URI(newUri);
        } catch (URISyntaxException e) {
            // Shouldn't happen: URI invariant
            throw new InternalError(e);
        }
    }

    @SuppressWarnings("removal")
    public CompletableFuture<Result> send() {
        PrivilegedAction<CompletableFuture<Result>> pa = () ->
                client.sendAsync(this.request, BodyHandlers.ofString())
                      .thenCompose(this::resultFrom);
        return AccessController.doPrivileged(pa);
    }

    /*
     * The result of the opening handshake.
     */
    static final class Result {

        final String subprotocol;
        final TransportFactory transport;

        private Result(String subprotocol, TransportFactory transport) {
            this.subprotocol = subprotocol;
            this.transport = transport;
        }
    }

    private CompletableFuture<Result> resultFrom(HttpResponse<?> response) {
        // Do we need a special treatment for SSLHandshakeException?
        // Namely, invoking
        //
        //     Listener.onClose(StatusCodes.TLS_HANDSHAKE_FAILURE, "")
        //
        // See https://tools.ietf.org/html/rfc6455#section-7.4.1
        Result result = null;
        Throwable exception = null;
        try {
            result = handleResponse(response);
        } catch (IOException e) {
            exception = e;
        } catch (Exception e) {
            exception = new WebSocketHandshakeException(response).initCause(e);
        } catch (Error e) {
            // We should attempt to close the connection and relay
            // the error through the completable future even in this
            // case.
            exception = e;
        }
        if (exception == null) {
            return MinimalFuture.completedFuture(result);
        }
        try {
            // calling this method will close the rawChannel, if created,
            // or the connection, if not.
            ((RawChannel.Provider) response).closeRawChannel();
        } catch (IOException e) {
            exception.addSuppressed(e);
        }
        return MinimalFuture.failedFuture(exception);
    }

    private Result handleResponse(HttpResponse<?> response) throws IOException {
        // By this point all redirects, authentications, etc. (if any) MUST have
        // been done by the HttpClient used by the WebSocket; so only 101 is
        // expected
        int c = response.statusCode();
        if (c != 101) {
            throw checkFailed("Unexpected HTTP response status code " + c);
        }
        HttpHeaders headers = response.headers();
        String upgrade = requireSingle(headers, HEADER_UPGRADE);
        if (!upgrade.equalsIgnoreCase("websocket")) {
            throw checkFailed("Bad response field: " + HEADER_UPGRADE);
        }
        String connection = requireSingle(headers, HEADER_CONNECTION);
        if (!connection.equalsIgnoreCase("Upgrade")) {
            throw checkFailed("Bad response field: " + HEADER_CONNECTION);
        }
        Optional<String> version = requireAtMostOne(headers, HEADER_VERSION);
        if (version.isPresent() && !version.get().equals(VERSION)) {
            throw checkFailed("Bad response field: " + HEADER_VERSION);
        }
        requireAbsent(headers, HEADER_EXTENSIONS);
        String x = this.nonce + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        this.sha1.update(x.getBytes(StandardCharsets.ISO_8859_1));
        String expected = Base64.getEncoder().encodeToString(this.sha1.digest());
        String actual = requireSingle(headers, HEADER_ACCEPT);
        if (!actual.trim().equals(expected)) {
            throw checkFailed("Bad " + HEADER_ACCEPT);
        }
        String subprotocol = checkAndReturnSubprotocol(headers);
        RawChannel channel = ((RawChannel.Provider) response).rawChannel();
        return new Result(subprotocol, new TransportFactoryImpl(channel));
    }

    private String checkAndReturnSubprotocol(HttpHeaders responseHeaders)
            throws CheckFailedException
    {
        Optional<String> opt = responseHeaders.firstValue(HEADER_PROTOCOL);
        if (!opt.isPresent()) {
            // If there is no such header in the response, then the server
            // doesn't want to use any subprotocol
            return "";
        }
        String s = requireSingle(responseHeaders, HEADER_PROTOCOL);
        // An empty string as a subprotocol's name is not allowed by the spec
        // and the check below will detect such responses too
        if (this.subprotocols.contains(s)) {
            return s;
        } else {
            throw checkFailed("Unexpected subprotocol: " + s);
        }
    }

    private static void requireAbsent(HttpHeaders responseHeaders,
                                      String headerName)
    {
        List<String> values = responseHeaders.allValues(headerName);
        if (!values.isEmpty()) {
            throw checkFailed(format("Response field '%s' present: %s",
                                     headerName,
                                     stringOf(values)));
        }
    }

    private static Optional<String> requireAtMostOne(HttpHeaders responseHeaders,
                                                     String headerName)
    {
        List<String> values = responseHeaders.allValues(headerName);
        if (values.size() > 1) {
            throw checkFailed(format("Response field '%s' multivalued: %s",
                                     headerName,
                                     stringOf(values)));
        }
        return values.stream().findFirst();
    }

    private static String requireSingle(HttpHeaders responseHeaders,
                                        String headerName)
    {
        List<String> values = responseHeaders.allValues(headerName);
        if (values.isEmpty()) {
            throw checkFailed("Response field missing: " + headerName);
        } else if (values.size() > 1) {
            throw checkFailed(format("Response field '%s' multivalued: %s",
                                     headerName,
                                     stringOf(values)));
        }
        return values.get(0);
    }

    private static String createNonce() {
        byte[] bytes = new byte[16];
        OpeningHandshake.random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static CheckFailedException checkFailed(String message) {
        throw new CheckFailedException(message);
    }

    private static URI checkURI(URI uri) {
        String scheme = uri.getScheme();
        if (!("ws".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)))
            throw illegal("invalid URI scheme: " + scheme);
        if (uri.getHost() == null)
            throw illegal("URI must contain a host: " + uri);
        if (uri.getFragment() != null)
            throw illegal("URI must not contain a fragment: " + uri);
        return uri;
    }

    private static IllegalArgumentException illegal(String message) {
        return new IllegalArgumentException(message);
    }

    /**
     * Returns the proxy for the given URI when sent through the given client,
     * or {@code null} if none is required or applicable.
     */
    private static Proxy proxyFor(Optional<ProxySelector> selector, URI uri) {
        if (!selector.isPresent()) {
            return null;
        }
        URI requestURI = createRequestURI(uri); // Based on the HTTP scheme
        List<Proxy> pl = selector.get().select(requestURI);
        if (pl.isEmpty()) {
            return null;
        }
        Proxy proxy = pl.get(0);
        if (proxy.type() != Proxy.Type.HTTP) {
            return null;
        }
        return proxy;
    }

    /**
     * Performs the necessary security permissions checks to connect ( possibly
     * through a proxy ) to the builders WebSocket URI.
     *
     * @throws SecurityException if the security manager denies access
     */
    static void checkPermissions(BuilderImpl b, Proxy proxy) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return;
        }
        Stream<String> headers = b.getHeaders().stream().map(p -> p.first).distinct();
        URLPermission perm1 = Utils.permissionForServer(b.getUri(), "", headers);
        sm.checkPermission(perm1);
        if (proxy == null) {
            return;
        }
        URLPermission perm2 = permissionForProxy((InetSocketAddress) proxy.address());
        if (perm2 != null) {
            sm.checkPermission(perm2);
        }
    }
}
