/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import sun.net.NetProperties;
import sun.net.www.HeaderParser;
import static java.lang.System.out;
import static java.lang.String.format;

/**
 * @test
 * @summary this test verifies that a client may provides authorization
 *          headers directly when connecting with a server.
 * @bug 8087112
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters DigestEchoServer
 *        ReferenceTracker DigestEchoClient
 * @modules java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @run main/othervm DigestEchoClient
 * @run main/othervm -Djdk.http.auth.proxying.disabledSchemes=
 *                   -Djdk.http.auth.tunneling.disabledSchemes=
 *                   DigestEchoClient
 */

public class DigestEchoClient {

    static final String data[] = {
        "Lorem ipsum",
        "dolor sit amet",
        "consectetur adipiscing elit, sed do eiusmod tempor",
        "quis nostrud exercitation ullamco",
        "laboris nisi",
        "ut",
        "aliquip ex ea commodo consequat." +
        "Duis aute irure dolor in reprehenderit in voluptate velit esse" +
        "cillum dolore eu fugiat nulla pariatur.",
        "Excepteur sint occaecat cupidatat non proident."
    };

    static final AtomicLong serverCount = new AtomicLong();
    static final class EchoServers {
        final DigestEchoServer.HttpAuthType authType;
        final DigestEchoServer.HttpAuthSchemeType authScheme;
        final String protocolScheme;
        final String key;
        final DigestEchoServer server;
        final Version serverVersion;

        private EchoServers(DigestEchoServer server,
                    Version version,
                    String protocolScheme,
                    DigestEchoServer.HttpAuthType authType,
                    DigestEchoServer.HttpAuthSchemeType authScheme) {
            this.authType = authType;
            this.authScheme = authScheme;
            this.protocolScheme = protocolScheme;
            this.key = key(version, protocolScheme, authType, authScheme);
            this.server = server;
            this.serverVersion = version;
        }

        static String key(Version version,
                          String protocolScheme,
                          DigestEchoServer.HttpAuthType authType,
                          DigestEchoServer.HttpAuthSchemeType authScheme) {
            return String.format("%s:%s:%s:%s", version, protocolScheme, authType, authScheme);
        }

        private static EchoServers create(Version version,
                                   String protocolScheme,
                                   DigestEchoServer.HttpAuthType authType,
                                   DigestEchoServer.HttpAuthSchemeType authScheme) {
            try {
                serverCount.incrementAndGet();
                DigestEchoServer server =
                    DigestEchoServer.create(version, protocolScheme, authType, authScheme);
                return new EchoServers(server, version, protocolScheme, authType, authScheme);
            } catch (IOException x) {
                throw new UncheckedIOException(x);
            }
        }

        public static DigestEchoServer of(Version version,
                                    String protocolScheme,
                                    DigestEchoServer.HttpAuthType authType,
                                    DigestEchoServer.HttpAuthSchemeType authScheme) {
            String key = key(version, protocolScheme, authType, authScheme);
            return servers.computeIfAbsent(key, (k) ->
                    create(version, protocolScheme, authType, authScheme)).server;
        }

        public static void stop() {
            for (EchoServers s : servers.values()) {
                s.server.stop();
            }
        }

        private static final ConcurrentMap<String, EchoServers> servers = new ConcurrentHashMap<>();
    }

    final static String PROXY_DISABLED = NetProperties.get("jdk.http.auth.proxying.disabledSchemes");
    final static String TUNNEL_DISABLED = NetProperties.get("jdk.http.auth.tunneling.disabledSchemes");
    static {
        System.out.println("jdk.http.auth.proxying.disabledSchemes=" + PROXY_DISABLED);
        System.out.println("jdk.http.auth.tunneling.disabledSchemes=" + TUNNEL_DISABLED);
    }



    static final AtomicInteger NC = new AtomicInteger();
    static final Random random = new Random();
    static final SSLContext context;
    static {
        try {
            context = new SimpleSSLContext().get();
            SSLContext.setDefault(context);
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }
    static final List<Boolean> BOOLEANS = List.of(true, false);

    final boolean useSSL;
    final DigestEchoServer.HttpAuthSchemeType authScheme;
    final DigestEchoServer.HttpAuthType authType;
    DigestEchoClient(boolean useSSL,
                     DigestEchoServer.HttpAuthSchemeType authScheme,
                     DigestEchoServer.HttpAuthType authType)
            throws IOException {
        this.useSSL = useSSL;
        this.authScheme = authScheme;
        this.authType = authType;
    }

    static final AtomicLong clientCount = new AtomicLong();
    static final ReferenceTracker TRACKER = ReferenceTracker.INSTANCE;
    public HttpClient newHttpClient(DigestEchoServer server) {
        clientCount.incrementAndGet();
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder = builder.proxy(ProxySelector.of(null));
        if (useSSL) {
            builder.sslContext(context);
        }
        switch (authScheme) {
            case BASIC:
                builder = builder.authenticator(DigestEchoServer.AUTHENTICATOR);
                break;
            case BASICSERVER:
                // don't set the authenticator: we will handle the header ourselves.
                // builder = builder.authenticator(DigestEchoServer.AUTHENTICATOR);
                break;
            default:
                break;
        }
        switch (authType) {
            case PROXY:
                builder = builder.proxy(ProxySelector.of(server.getProxyAddress()));
                break;
            case PROXY305:
                builder = builder.proxy(ProxySelector.of(server.getProxyAddress()));
                builder = builder.followRedirects(HttpClient.Redirect.NORMAL);
                break;
            case SERVER307:
                builder = builder.followRedirects(HttpClient.Redirect.NORMAL);
                break;
            default:
                break;
        }
        return TRACKER.track(builder.build());
    }

    public static List<Version> serverVersions(Version clientVersion) {
        if (clientVersion == Version.HTTP_1_1) {
            return List.of(clientVersion);
        } else {
            return List.of(Version.values());
        }
    }

    public static List<Version> clientVersions() {
        return List.of(Version.values());
    }

    public static List<Boolean> expectContinue(Version serverVersion) {
        if (serverVersion == Version.HTTP_1_1) {
            return BOOLEANS;
        } else {
            // our test HTTP/2 server does not support Expect: 100-Continue
            return List.of(Boolean.FALSE);
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServerAdapters.enableServerLogging();
        boolean useSSL = false;
        EnumSet<DigestEchoServer.HttpAuthType> types =
                EnumSet.complementOf(EnumSet.of(DigestEchoServer.HttpAuthType.PROXY305));
        Throwable failed = null;
        if (args != null && args.length >= 1) {
            useSSL = "SSL".equals(args[0]);
            if (args.length > 1) {
                List<DigestEchoServer.HttpAuthType> httpAuthTypes =
                        Stream.of(Arrays.copyOfRange(args, 1, args.length))
                                .map(DigestEchoServer.HttpAuthType::valueOf)
                                .collect(Collectors.toList());
                types = EnumSet.copyOf(httpAuthTypes);
            }
        }
        try {
            for (DigestEchoServer.HttpAuthType authType : types) {
                // The test server does not support PROXY305 properly
                if (authType == DigestEchoServer.HttpAuthType.PROXY305) continue;
                EnumSet<DigestEchoServer.HttpAuthSchemeType> basics =
                        EnumSet.of(DigestEchoServer.HttpAuthSchemeType.BASICSERVER,
                                DigestEchoServer.HttpAuthSchemeType.BASIC);
                for (DigestEchoServer.HttpAuthSchemeType authScheme : basics) {
                    DigestEchoClient dec = new DigestEchoClient(useSSL,
                            authScheme,
                            authType);
                    for (Version clientVersion : clientVersions()) {
                        for (Version serverVersion : serverVersions(clientVersion)) {
                            for (boolean expectContinue : expectContinue(serverVersion)) {
                                for (boolean async : BOOLEANS) {
                                    for (boolean preemptive : BOOLEANS) {
                                        dec.testBasic(clientVersion,
                                                serverVersion, async,
                                                expectContinue, preemptive);
                                    }
                                }
                            }
                        }
                    }
                }
                EnumSet<DigestEchoServer.HttpAuthSchemeType> digests =
                        EnumSet.of(DigestEchoServer.HttpAuthSchemeType.DIGEST);
                for (DigestEchoServer.HttpAuthSchemeType authScheme : digests) {
                    DigestEchoClient dec = new DigestEchoClient(useSSL,
                            authScheme,
                            authType);
                    for (Version clientVersion : clientVersions()) {
                        for (Version serverVersion : serverVersions(clientVersion)) {
                            for (boolean expectContinue : expectContinue(serverVersion)) {
                                for (boolean async : BOOLEANS) {
                                    dec.testDigest(clientVersion, serverVersion,
                                            async, expectContinue);
                                }
                            }
                        }
                    }
                }
            }
        } catch(Throwable t) {
            out.println(DigestEchoServer.now()
                    + ": Unexpected exception: " + t);
            t.printStackTrace();
            failed = t;
            throw t;
        } finally {
            Thread.sleep(100);
            AssertionError trackFailed = TRACKER.check(500);
            EchoServers.stop();
            System.out.println(" ---------------------------------------------------------- ");
            System.out.println(String.format("DigestEchoClient %s %s", useSSL ? "SSL" : "CLEAR", types));
            System.out.println(String.format("Created %d clients and %d servers",
                    clientCount.get(), serverCount.get()));
            System.out.println(String.format("basics:  %d requests sent, %d ns / req",
                    basicCount.get(), basics.get()));
            System.out.println(String.format("digests: %d requests sent, %d ns / req",
                    digestCount.get(), digests.get()));
            System.out.println(" ---------------------------------------------------------- ");
            if (trackFailed != null) {
                if (failed != null) {
                    failed.addSuppressed(trackFailed);
                    if (failed instanceof Error) throw (Error) failed;
                    if (failed instanceof Exception) throw (Exception) failed;
                }
                throw trackFailed;
            }
        }
    }

    boolean isSchemeDisabled() {
        String disabledSchemes;
        if (isProxy(authType)) {
            disabledSchemes = useSSL
                    ? TUNNEL_DISABLED
                    : PROXY_DISABLED;
        } else return false;
        if (disabledSchemes == null
                || disabledSchemes.isEmpty()) {
            return false;
        }
        String scheme;
        switch (authScheme) {
            case DIGEST:
                scheme = "Digest";
                break;
            case BASIC:
                scheme = "Basic";
                break;
            case BASICSERVER:
                scheme = "Basic";
                break;
            case NONE:
                return false;
            default:
                throw new InternalError("Unknown auth scheme: " + authScheme);
        }
        return Stream.of(disabledSchemes.split(","))
                .map(String::trim)
                .filter(scheme::equalsIgnoreCase)
                .findAny()
                .isPresent();
    }

    final static AtomicLong basics = new AtomicLong();
    final static AtomicLong basicCount = new AtomicLong();
    // @Test
    void testBasic(Version clientVersion, Version serverVersion, boolean async,
                   boolean expectContinue, boolean preemptive)
        throws Exception
    {
        final boolean addHeaders = authScheme == DigestEchoServer.HttpAuthSchemeType.BASICSERVER;
        // !preemptive has no meaning if we don't handle the authorization
        // headers ourselves
        if (!preemptive && !addHeaders) return;

        out.println(format("*** testBasic: client: %s, server: %s, async: %s, useSSL: %s, " +
                        "authScheme: %s, authType: %s, expectContinue: %s preemptive: %s***",
                clientVersion, serverVersion, async, useSSL, authScheme, authType,
                expectContinue, preemptive));

        DigestEchoServer server = EchoServers.of(serverVersion,
                useSSL ? "https" : "http", authType, authScheme);
        URI uri = DigestEchoServer.uri(useSSL ? "https" : "http",
                server.getServerAddress(), "/foo/");

        HttpClient client = newHttpClient(server);
        HttpResponse<String> r;
        CompletableFuture<HttpResponse<String>> cf1;
        String auth = null;

        try {
            for (int i=0; i<data.length; i++) {
                out.println(DigestEchoServer.now() + " ----- iteration " + i + " -----");
                List<String> lines = List.of(Arrays.copyOfRange(data, 0, i+1));
                assert lines.size() == i + 1;
                String body = lines.stream().collect(Collectors.joining("\r\n"));
                BodyPublisher reqBody = BodyPublishers.ofString(body);
                HttpRequest.Builder builder = HttpRequest.newBuilder(uri).version(clientVersion)
                        .POST(reqBody).expectContinue(expectContinue);
                boolean isTunnel = isProxy(authType) && useSSL;
                if (addHeaders) {
                    // handle authentication ourselves
                    assert !client.authenticator().isPresent();
                    if (auth == null) auth = "Basic " + getBasicAuth("arthur");
                    try {
                        if ((i > 0 || preemptive)
                                && (!isTunnel || i == 0 || isSchemeDisabled())) {
                            // In case of a SSL tunnel through proxy then only the
                            // first request should require proxy authorization
                            // Though this might be invalidated if the server decides
                            // to close the connection...
                            out.println(String.format("%s adding %s: %s",
                                    DigestEchoServer.now(),
                                    authorizationKey(authType),
                                    auth));
                            builder = builder.header(authorizationKey(authType), auth);
                        }
                    } catch (IllegalArgumentException x) {
                        throw x;
                    }
                } else {
                    // let the stack do the authentication
                    assert client.authenticator().isPresent();
                }
                long start = System.nanoTime();
                HttpRequest request = builder.build();
                HttpResponse<Stream<String>> resp;
                try {
                    if (async) {
                        resp = client.sendAsync(request, BodyHandlers.ofLines()).join();
                    } else {
                        resp = client.send(request, BodyHandlers.ofLines());
                    }
                } catch (Throwable t) {
                    long stop = System.nanoTime();
                    synchronized (basicCount) {
                        long n = basicCount.getAndIncrement();
                        basics.set((basics.get() * n + (stop - start)) / (n + 1));
                    }
                    // unwrap CompletionException
                    if (t instanceof CompletionException) {
                        assert t.getCause() != null;
                        t = t.getCause();
                    }
                    out.println(DigestEchoServer.now()
                            + ": Unexpected exception: " + t);
                    throw new RuntimeException("Unexpected exception: " + t, t);
                }

                if (addHeaders && !preemptive && (i==0 || isSchemeDisabled())) {
                    assert resp.statusCode() == 401 || resp.statusCode() == 407;
                    Stream<String> respBody = resp.body();
                    if (respBody != null) {
                        System.out.printf("Response body (%s):\n", resp.statusCode());
                        respBody.forEach(System.out::println);
                    }
                    System.out.println(String.format("%s received: adding header %s: %s",
                            resp.statusCode(), authorizationKey(authType), auth));
                    request = HttpRequest.newBuilder(uri).version(clientVersion)
                            .POST(reqBody).header(authorizationKey(authType), auth).build();
                    if (async) {
                        resp = client.sendAsync(request, BodyHandlers.ofLines()).join();
                    } else {
                        resp = client.send(request, BodyHandlers.ofLines());
                    }
                }
                final List<String> respLines;
                try {
                    if (isSchemeDisabled()) {
                        if (resp.statusCode() != 407) {
                            throw new RuntimeException("expected 407 not received");
                        }
                        System.out.println("Scheme disabled for [" + authType
                                + ", " + authScheme
                                + ", " + (useSSL ? "HTTP" : "HTTPS")
                                + "]: Received expected " + resp.statusCode());
                        continue;
                    } else {
                        System.out.println("Scheme enabled for [" + authType
                                + ", " + authScheme
                                + ", " + (useSSL ? "HTTPS" : "HTTP")
                                + "]: Expecting 200, response is: " + resp);
                        assert resp.statusCode() == 200 : "200 expected, received " + resp;
                        respLines = resp.body().collect(Collectors.toList());
                    }
                } finally {
                    long stop = System.nanoTime();
                    synchronized (basicCount) {
                        long n = basicCount.getAndIncrement();
                        basics.set((basics.get() * n + (stop - start)) / (n + 1));
                    }
                }
                if (!lines.equals(respLines)) {
                    throw new RuntimeException("Unexpected response: " + respLines);
                }
            }
        } finally {
        }
        System.out.println("OK");
    }

    String getBasicAuth(String username) {
        StringBuilder builder = new StringBuilder(username);
        builder.append(':');
        for (char c : DigestEchoServer.AUTHENTICATOR.getPassword(username)) {
            builder.append(c);
        }
        return Base64.getEncoder().encodeToString(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    final static AtomicLong digests = new AtomicLong();
    final static AtomicLong digestCount = new AtomicLong();
    // @Test
    void testDigest(Version clientVersion, Version serverVersion,
                    boolean async, boolean expectContinue)
            throws Exception
    {
        String test = format("testDigest: client: %s, server: %s, async: %s, useSSL: %s, " +
                             "authScheme: %s, authType: %s, expectContinue: %s",
                              clientVersion, serverVersion, async, useSSL,
                              authScheme, authType, expectContinue);
        out.println("*** " + test + " ***");
        DigestEchoServer server = EchoServers.of(serverVersion,
                useSSL ? "https" : "http", authType, authScheme);

        URI uri = DigestEchoServer.uri(useSSL ? "https" : "http",
                server.getServerAddress(), "/foo/");

        HttpClient client = newHttpClient(server);
        HttpResponse<String> r;
        CompletableFuture<HttpResponse<String>> cf1;
        byte[] cnonce = new byte[16];
        String cnonceStr = null;
        DigestEchoServer.DigestResponse challenge = null;

        try {
            for (int i=0; i<data.length; i++) {
                out.println(DigestEchoServer.now() + "----- iteration " + i + " -----");
                List<String> lines = List.of(Arrays.copyOfRange(data, 0, i+1));
                assert lines.size() == i + 1;
                String body = lines.stream().collect(Collectors.joining("\r\n"));
                HttpRequest.BodyPublisher reqBody = HttpRequest.BodyPublishers.ofString(body);
                HttpRequest.Builder reqBuilder = HttpRequest
                        .newBuilder(uri).version(clientVersion).POST(reqBody)
                        .expectContinue(expectContinue);

                boolean isTunnel = isProxy(authType) && useSSL;
                String digestMethod = isTunnel ? "CONNECT" : "POST";

                // In case of a tunnel connection only the first request
                // which establishes the tunnel needs to authenticate with
                // the proxy.
                if (challenge != null && (!isTunnel || isSchemeDisabled())) {
                    assert cnonceStr != null;
                    String auth = digestResponse(uri, digestMethod, challenge, cnonceStr);
                    try {
                        reqBuilder = reqBuilder.header(authorizationKey(authType), auth);
                    } catch (IllegalArgumentException x) {
                        throw x;
                    }
                }

                long start = System.nanoTime();
                HttpRequest request = reqBuilder.build();
                HttpResponse<Stream<String>> resp;
                if (async) {
                    resp = client.sendAsync(request, BodyHandlers.ofLines()).join();
                } else {
                    resp = client.send(request, BodyHandlers.ofLines());
                }
                System.out.println(resp);
                assert challenge != null || resp.statusCode() == 401 || resp.statusCode() == 407
                        : "challenge=" + challenge + ", resp=" + resp + ", test=[" + test + "]";
                if (resp.statusCode() == 401 || resp.statusCode() == 407) {
                    // This assert may need to be relaxed if our server happened to
                    // decide to close the tunnel connection, in which case we would
                    // receive 407 again...
                    assert challenge == null || !isTunnel || isSchemeDisabled()
                            : "No proxy auth should be required after establishing an SSL tunnel";

                    System.out.println("Received " + resp.statusCode() + " answering challenge...");
                    random.nextBytes(cnonce);
                    cnonceStr = new BigInteger(1, cnonce).toString(16);
                    System.out.println("Response headers: " + resp.headers());
                    Optional<String> authenticateOpt = resp.headers().firstValue(authenticateKey(authType));
                    String authenticate = authenticateOpt.orElseThrow(
                            () -> new RuntimeException(authenticateKey(authType) + ": not found"));
                    assert authenticate.startsWith("Digest ");
                    HeaderParser hp = new HeaderParser(authenticate.substring("Digest ".length()));
                    String qop = hp.findValue("qop");
                    String nonce = hp.findValue("nonce");
                    if (qop == null && nonce == null) {
                        throw new RuntimeException("QOP and NONCE not found");
                    }
                    challenge = DigestEchoServer.DigestResponse
                            .create(authenticate.substring("Digest ".length()));
                    String auth = digestResponse(uri, digestMethod, challenge, cnonceStr);
                    try {
                        request = HttpRequest.newBuilder(uri).version(clientVersion)
                            .POST(reqBody).header(authorizationKey(authType), auth).build();
                    } catch (IllegalArgumentException x) {
                        throw x;
                    }

                    if (async) {
                        resp = client.sendAsync(request, BodyHandlers.ofLines()).join();
                    } else {
                        resp = client.send(request, BodyHandlers.ofLines());
                    }
                    System.out.println(resp);
                }
                final List<String> respLines;
                try {
                    if (isSchemeDisabled()) {
                        if (resp.statusCode() != 407) {
                            throw new RuntimeException("expected 407 not received");
                        }
                        System.out.println("Scheme disabled for [" + authType
                                + ", " + authScheme +
                                ", " + (useSSL ? "HTTP" : "HTTPS")
                                + "]: Received expected " + resp.statusCode());
                        continue;
                    } else {
                        assert resp.statusCode() == 200;
                        respLines = resp.body().collect(Collectors.toList());
                    }
                } finally {
                    long stop = System.nanoTime();
                    synchronized (digestCount) {
                        long n = digestCount.getAndIncrement();
                        digests.set((digests.get() * n + (stop - start)) / (n + 1));
                    }
                }
                if (!lines.equals(respLines)) {
                    throw new RuntimeException("Unexpected response: " + respLines);
                }
            }
        } finally {
        }
        System.out.println("OK");
    }

    // WARNING: This is not a full fledged implementation of DIGEST.
    // It does contain bugs and inaccuracy.
    static String digestResponse(URI uri, String method, DigestEchoServer.DigestResponse challenge, String cnonce)
            throws NoSuchAlgorithmException {
        int nc = NC.incrementAndGet();
        DigestEchoServer.DigestResponse response1 = new DigestEchoServer.DigestResponse("earth",
                "arthur", challenge.nonce, cnonce, String.valueOf(nc), uri.toASCIIString(),
                challenge.algorithm, challenge.qop, challenge.opaque, null);
        String response = DigestEchoServer.DigestResponse.computeDigest(true, method,
                DigestEchoServer.AUTHENTICATOR.getPassword("arthur"), response1);
        String auth = "Digest username=\"arthur\", realm=\"earth\""
                + ", response=\"" + response + "\", uri=\""+uri.toASCIIString()+"\""
                + ", qop=\"" + response1.qop + "\", cnonce=\"" + response1.cnonce
                + "\", nc=\"" + nc + "\", nonce=\"" + response1.nonce + "\"";
        if (response1.opaque != null) {
            auth = auth + ", opaque=\"" + response1.opaque + "\"";
        }
        return auth;
    }

    static String authenticateKey(DigestEchoServer.HttpAuthType authType) {
        switch (authType) {
            case SERVER: return "www-authenticate";
            case SERVER307: return "www-authenticate";
            case PROXY: return "proxy-authenticate";
            case PROXY305: return "proxy-authenticate";
            default: throw new InternalError("authType: " + authType);
        }
    }

    static String authorizationKey(DigestEchoServer.HttpAuthType authType) {
        switch (authType) {
            case SERVER: return "authorization";
            case SERVER307: return "Authorization";
            case PROXY: return "Proxy-Authorization";
            case PROXY305: return "proxy-Authorization";
            default: throw new InternalError("authType: " + authType);
        }
    }

    static boolean isProxy(DigestEchoServer.HttpAuthType authType) {
        switch (authType) {
            case SERVER: return false;
            case SERVER307: return false;
            case PROXY: return true;
            case PROXY305: return true;
            default: throw new InternalError("authType: " + authType);
        }
    }
}
