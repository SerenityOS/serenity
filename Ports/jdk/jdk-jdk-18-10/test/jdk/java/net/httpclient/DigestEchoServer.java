/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import sun.net.www.HeaderParser;
import java.net.http.HttpClient.Version;

/**
 * A simple HTTP server that supports Basic or Digest authentication.
 * By default this server will echo back whatever is present
 * in the request body. Note that the Digest authentication is
 * a test implementation implemented only for tests purposes.
 * @author danielfuchs
 */
public abstract class DigestEchoServer implements HttpServerAdapters {

    public static final boolean DEBUG =
            Boolean.parseBoolean(System.getProperty("test.debug", "false"));
    public static final boolean NO_LINGER =
            Boolean.parseBoolean(System.getProperty("test.nolinger", "false"));
    public static final boolean TUNNEL_REQUIRES_HOST =
            Boolean.parseBoolean(System.getProperty("test.requiresHost", "false"));
    public enum HttpAuthType {
        SERVER, PROXY, SERVER307, PROXY305
        /* add PROXY_AND_SERVER and SERVER_PROXY_NONE */
    };
    public enum HttpAuthSchemeType { NONE, BASICSERVER, BASIC, DIGEST };
    public static final HttpAuthType DEFAULT_HTTP_AUTH_TYPE = HttpAuthType.SERVER;
    public static final String DEFAULT_PROTOCOL_TYPE = "https";
    public static final HttpAuthSchemeType DEFAULT_SCHEME_TYPE = HttpAuthSchemeType.DIGEST;

    public static class HttpTestAuthenticator extends Authenticator {
        private final String realm;
        private final String username;
        // Used to prevent incrementation of 'count' when calling the
        // authenticator from the server side.
        private final ThreadLocal<Boolean> skipCount = new ThreadLocal<>();
        // count will be incremented every time getPasswordAuthentication()
        // is called from the client side.
        final AtomicInteger count = new AtomicInteger();

        public HttpTestAuthenticator(String realm, String username) {
            this.realm = realm;
            this.username = username;
        }
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (skipCount.get() == null || skipCount.get().booleanValue() == false) {
                System.out.println("Authenticator called: " + count.incrementAndGet());
            }
            return new PasswordAuthentication(getUserName(),
                    new char[] {'d','e','n', 't'});
        }
        // Called by the server side to get the password of the user
        // being authentified.
        public final char[] getPassword(String user) {
            if (user.equals(username)) {
                skipCount.set(Boolean.TRUE);
                try {
                    return getPasswordAuthentication().getPassword();
                } finally {
                    skipCount.set(Boolean.FALSE);
                }
            }
            throw new SecurityException("User unknown: " + user);
        }
        public final String getUserName() {
            return username;
        }
        public final String getRealm() {
            return realm;
        }
    }

    public static final HttpTestAuthenticator AUTHENTICATOR;
    static {
        AUTHENTICATOR = new HttpTestAuthenticator("earth", "arthur");
    }


    final HttpTestServer       serverImpl; // this server endpoint
    final DigestEchoServer     redirect;   // the target server where to redirect 3xx
    final HttpTestHandler      delegate;   // unused
    final String               key;

    DigestEchoServer(String key,
                             HttpTestServer server,
                             DigestEchoServer target,
                             HttpTestHandler delegate) {
        this.key = key;
        this.serverImpl = server;
        this.redirect = target;
        this.delegate = delegate;
    }

    public static void main(String[] args)
            throws IOException {

        DigestEchoServer server = create(Version.HTTP_1_1,
                DEFAULT_PROTOCOL_TYPE,
                DEFAULT_HTTP_AUTH_TYPE,
                AUTHENTICATOR,
                DEFAULT_SCHEME_TYPE);
        try {
            System.out.println("Server created at " + server.getAddress());
            System.out.println("Strike <Return> to exit");
            System.in.read();
        } finally {
            System.out.println("stopping server");
            server.stop();
        }
    }

    private static String toString(HttpTestRequestHeaders headers) {
        return headers.entrySet().stream()
                .map((e) -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    public static DigestEchoServer create(Version version,
                                          String protocol,
                                          HttpAuthType authType,
                                          HttpAuthSchemeType schemeType)
            throws IOException {
        return create(version, protocol, authType, AUTHENTICATOR, schemeType);
    }

    public static DigestEchoServer create(Version version,
                                          String protocol,
                                          HttpAuthType authType,
                                          HttpTestAuthenticator auth,
                                          HttpAuthSchemeType schemeType)
            throws IOException {
        return create(version, protocol, authType, auth, schemeType, null);
    }

    public static DigestEchoServer create(Version version,
                                        String protocol,
                                        HttpAuthType authType,
                                        HttpTestAuthenticator auth,
                                        HttpAuthSchemeType schemeType,
                                        HttpTestHandler delegate)
            throws IOException {
        Objects.requireNonNull(authType);
        Objects.requireNonNull(auth);
        switch(authType) {
            // A server that performs Server Digest authentication.
            case SERVER: return createServer(version, protocol, authType, auth,
                                             schemeType, delegate, "/");
            // A server that pretends to be a Proxy and performs
            // Proxy Digest authentication. If protocol is HTTPS,
            // then this will create a HttpsProxyTunnel that will
            // handle the CONNECT request for tunneling.
            case PROXY: return createProxy(version, protocol, authType, auth,
                                           schemeType, delegate, "/");
            // A server that sends 307 redirect to a server that performs
            // Digest authentication.
            // Note: 301 doesn't work here because it transforms POST into GET.
            case SERVER307: return createServerAndRedirect(version,
                                                        protocol,
                                                        HttpAuthType.SERVER,
                                                        auth, schemeType,
                                                        delegate, 307);
            // A server that sends 305 redirect to a proxy that performs
            // Digest authentication.
            // Note: this is not correctly stubbed/implemented in this test.
            case PROXY305:  return createServerAndRedirect(version,
                                                        protocol,
                                                        HttpAuthType.PROXY,
                                                        auth, schemeType,
                                                        delegate, 305);
            default:
                throw new InternalError("Unknown server type: " + authType);
        }
    }


    /**
     * The SocketBindableFactory ensures that the local port used by an HttpServer
     * or a proxy ServerSocket previously created by the current test/VM will not
     * get reused by a subsequent test in the same VM.
     * This is to avoid having the test client trying to reuse cached connections.
     */
    private static abstract class SocketBindableFactory<B> {
        private static final int MAX = 10;
        private static final CopyOnWriteArrayList<String> addresses =
                new CopyOnWriteArrayList<>();
        protected B createInternal() throws IOException {
            final int max = addresses.size() + MAX;
            final List<B> toClose = new ArrayList<>();
            try {
                for (int i = 1; i <= max; i++) {
                    B bindable = createBindable();
                    InetSocketAddress address = getAddress(bindable);
                    String key = "localhost:" + address.getPort();
                    if (addresses.addIfAbsent(key)) {
                        System.out.println("Socket bound to: " + key
                                + " after " + i + " attempt(s)");
                        return bindable;
                    }
                    System.out.println("warning: address " + key
                            + " already used. Retrying bind.");
                    // keep the port bound until we get a port that we haven't
                    // used already
                    toClose.add(bindable);
                }
            } finally {
                // if we had to retry, then close the socket we're not
                // going to use.
                for (B b : toClose) {
                    try { close(b); } catch (Exception x) { /* ignore */ }
                }
            }
            throw new IOException("Couldn't bind socket after " + max + " attempts: "
                    + "addresses used before: " + addresses);
        }

        protected abstract B createBindable() throws IOException;

        protected abstract InetSocketAddress getAddress(B bindable);

        protected abstract void close(B bindable) throws IOException;
    }

    /*
     * Used to create ServerSocket for a proxy.
     */
    private static final class ServerSocketFactory
    extends SocketBindableFactory<ServerSocket> {
        private static final ServerSocketFactory instance = new ServerSocketFactory();

        static ServerSocket create() throws IOException {
            return instance.createInternal();
        }

        @Override
        protected ServerSocket createBindable() throws IOException {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            return ss;
        }

        @Override
        protected InetSocketAddress getAddress(ServerSocket socket) {
            return new InetSocketAddress(socket.getInetAddress(), socket.getLocalPort());
        }

        @Override
        protected void close(ServerSocket socket) throws IOException {
            socket.close();
        }
    }

    /*
     * Used to create HttpServer
     */
    private static abstract class H1ServerFactory<S extends HttpServer>
            extends SocketBindableFactory<S> {
        @Override
        protected S createBindable() throws IOException {
            S server = newHttpServer();
            server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            return server;
        }

        @Override
        protected InetSocketAddress getAddress(S server) {
            return server.getAddress();
        }

        @Override
        protected void close(S server) throws IOException {
            server.stop(1);
        }

        /*
         * Returns a HttpServer or a HttpsServer in different subclasses.
         */
        protected abstract S newHttpServer() throws IOException;
    }

    /*
     * Used to create Http2TestServer
     */
    private static abstract class H2ServerFactory<S extends Http2TestServer>
            extends SocketBindableFactory<S> {
        @Override
        protected S createBindable() throws IOException {
            final S server;
            try {
                server = newHttpServer();
            } catch (IOException io) {
                throw io;
            } catch (Exception x) {
                throw new IOException(x);
            }
            return server;
        }

        @Override
        protected InetSocketAddress getAddress(S server) {
            return server.getAddress();
        }

        @Override
        protected void close(S server) throws IOException {
            server.stop();
        }

        /*
         * Returns a HttpServer or a HttpsServer in different subclasses.
         */
        protected abstract S newHttpServer() throws Exception;
    }

    private static final class Http2ServerFactory extends H2ServerFactory<Http2TestServer> {
        private static final Http2ServerFactory instance = new Http2ServerFactory();

        static Http2TestServer create() throws IOException {
            return instance.createInternal();
        }

        @Override
        protected Http2TestServer newHttpServer() throws Exception {
            return new Http2TestServer("localhost", false, 0);
        }
    }

    private static final class Https2ServerFactory extends H2ServerFactory<Http2TestServer> {
        private static final Https2ServerFactory instance = new Https2ServerFactory();

        static Http2TestServer create() throws IOException {
            return instance.createInternal();
        }

        @Override
        protected Http2TestServer newHttpServer() throws Exception {
            return new Http2TestServer("localhost", true, 0);
        }
    }

    private static final class Http1ServerFactory extends H1ServerFactory<HttpServer> {
        private static final Http1ServerFactory instance = new Http1ServerFactory();

        static HttpServer create() throws IOException {
            return instance.createInternal();
        }

        @Override
        protected HttpServer newHttpServer() throws IOException {
            return HttpServer.create();
        }
    }

    private static final class Https1ServerFactory extends H1ServerFactory<HttpsServer> {
        private static final Https1ServerFactory instance = new Https1ServerFactory();

        static HttpsServer create() throws IOException {
            return instance.createInternal();
        }

        @Override
        protected HttpsServer newHttpServer() throws IOException {
            return HttpsServer.create();
        }
    }

    static Http2TestServer createHttp2Server(String protocol) throws IOException {
        final Http2TestServer server;
        if ("http".equalsIgnoreCase(protocol)) {
            server = Http2ServerFactory.create();
        } else if ("https".equalsIgnoreCase(protocol)) {
            server = Https2ServerFactory.create();
        } else {
            throw new InternalError("unsupported protocol: " + protocol);
        }
        return server;
    }

    static HttpTestServer createHttpServer(Version version, String protocol)
            throws IOException
    {
        switch(version) {
            case HTTP_1_1:
                return HttpTestServer.of(createHttp1Server(protocol));
            case HTTP_2:
                return HttpTestServer.of(createHttp2Server(protocol));
            default:
                throw new InternalError("Unexpected version: " + version);
        }
    }

    static HttpServer createHttp1Server(String protocol) throws IOException {
        final HttpServer server;
        if ("http".equalsIgnoreCase(protocol)) {
            server = Http1ServerFactory.create();
        } else if ("https".equalsIgnoreCase(protocol)) {
            server = configure(Https1ServerFactory.create());
        } else {
            throw new InternalError("unsupported protocol: " + protocol);
        }
        return server;
    }

    static HttpsServer configure(HttpsServer server) throws IOException {
        try {
            SSLContext ctx = SSLContext.getDefault();
            server.setHttpsConfigurator(new Configurator(ctx));
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
        return server;
    }


    static void setContextAuthenticator(HttpTestContext ctxt,
                                        HttpTestAuthenticator auth) {
        final String realm = auth.getRealm();
        com.sun.net.httpserver.Authenticator authenticator =
            new BasicAuthenticator(realm) {
                @Override
                public boolean checkCredentials(String username, String pwd) {
                    return auth.getUserName().equals(username)
                           && new String(auth.getPassword(username)).equals(pwd);
                }
        };
        ctxt.setAuthenticator(authenticator);
    }

    public static DigestEchoServer createServer(Version version,
                                        String protocol,
                                        HttpAuthType authType,
                                        HttpTestAuthenticator auth,
                                        HttpAuthSchemeType schemeType,
                                        HttpTestHandler delegate,
                                        String path)
            throws IOException {
        Objects.requireNonNull(authType);
        Objects.requireNonNull(auth);

        HttpTestServer impl = createHttpServer(version, protocol);
        String key = String.format("DigestEchoServer[PID=%s,PORT=%s]:%s:%s:%s:%s",
                ProcessHandle.current().pid(),
                impl.getAddress().getPort(),
                version, protocol, authType, schemeType);
        final DigestEchoServer server = new DigestEchoServerImpl(key, impl, null, delegate);
        final HttpTestHandler handler =
                server.createHandler(schemeType, auth, authType, false);
        HttpTestContext context = impl.addHandler(handler, path);
        server.configureAuthentication(context, schemeType, auth, authType);
        impl.start();
        return server;
    }

    public static DigestEchoServer createProxy(Version version,
                                        String protocol,
                                        HttpAuthType authType,
                                        HttpTestAuthenticator auth,
                                        HttpAuthSchemeType schemeType,
                                        HttpTestHandler delegate,
                                        String path)
            throws IOException {
        Objects.requireNonNull(authType);
        Objects.requireNonNull(auth);

        if (version == Version.HTTP_2 && protocol.equalsIgnoreCase("http")) {
            System.out.println("WARNING: can't use HTTP/1.1 proxy with unsecure HTTP/2 server");
            version = Version.HTTP_1_1;
        }
        HttpTestServer impl = createHttpServer(version, protocol);
        String key = String.format("DigestEchoServer[PID=%s,PORT=%s]:%s:%s:%s:%s",
                ProcessHandle.current().pid(),
                impl.getAddress().getPort(),
                version, protocol, authType, schemeType);
        final DigestEchoServer server = "https".equalsIgnoreCase(protocol)
                ? new HttpsProxyTunnel(key, impl, null, delegate)
                : new DigestEchoServerImpl(key, impl, null, delegate);

        final HttpTestHandler hh = server.createHandler(HttpAuthSchemeType.NONE,
                                         null, HttpAuthType.SERVER,
                                         server instanceof HttpsProxyTunnel);
        HttpTestContext ctxt = impl.addHandler(hh, path);
        server.configureAuthentication(ctxt, schemeType, auth, authType);
        impl.start();

        return server;
    }

    public static DigestEchoServer createServerAndRedirect(
                                        Version version,
                                        String protocol,
                                        HttpAuthType targetAuthType,
                                        HttpTestAuthenticator auth,
                                        HttpAuthSchemeType schemeType,
                                        HttpTestHandler targetDelegate,
                                        int code300)
            throws IOException {
        Objects.requireNonNull(targetAuthType);
        Objects.requireNonNull(auth);

        // The connection between client and proxy can only
        // be a plain connection: SSL connection to proxy
        // is not supported by our client connection.
        String targetProtocol = targetAuthType == HttpAuthType.PROXY
                                          ? "http"
                                          : protocol;
        DigestEchoServer redirectTarget =
                (targetAuthType == HttpAuthType.PROXY)
                ? createProxy(version, protocol, targetAuthType,
                              auth, schemeType, targetDelegate, "/")
                : createServer(version, targetProtocol, targetAuthType,
                               auth, schemeType, targetDelegate, "/");
        HttpTestServer impl = createHttpServer(version, protocol);
        String key = String.format("RedirectingServer[PID=%s,PORT=%s]:%s:%s:%s:%s",
                ProcessHandle.current().pid(),
                impl.getAddress().getPort(),
                version, protocol,
                HttpAuthType.SERVER, code300)
                + "->" + redirectTarget.key;
        final DigestEchoServer redirectingServer =
                 new DigestEchoServerImpl(key, impl, redirectTarget, null);
        InetSocketAddress redirectAddr = redirectTarget.getAddress();
        URL locationURL = url(targetProtocol, redirectAddr, "/");
        final HttpTestHandler hh = redirectingServer.create300Handler(key, locationURL,
                                             HttpAuthType.SERVER, code300);
        impl.addHandler(hh,"/");
        impl.start();
        return redirectingServer;
    }

    public abstract InetSocketAddress getServerAddress();
    public abstract InetSocketAddress getProxyAddress();
    public abstract InetSocketAddress getAddress();
    public abstract void stop();
    public abstract Version getServerVersion();

    private static class DigestEchoServerImpl extends DigestEchoServer {
        DigestEchoServerImpl(String key,
                             HttpTestServer server,
                             DigestEchoServer target,
                             HttpTestHandler delegate) {
            super(key, Objects.requireNonNull(server), target, delegate);
        }

        public InetSocketAddress getAddress() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    serverImpl.getAddress().getPort());
        }

        public InetSocketAddress getServerAddress() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    serverImpl.getAddress().getPort());
        }

        public InetSocketAddress getProxyAddress() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    serverImpl.getAddress().getPort());
        }

        public Version getServerVersion() {
            return serverImpl.getVersion();
        }

        public void stop() {
            serverImpl.stop();
            if (redirect != null) {
                redirect.stop();
            }
        }
    }

    protected void writeResponse(HttpTestExchange he) throws IOException {
        if (delegate == null) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
            he.getResponseBody().write(he.getRequestBody().readAllBytes());
        } else {
            delegate.handle(he);
        }
    }

    private HttpTestHandler createHandler(HttpAuthSchemeType schemeType,
                                      HttpTestAuthenticator auth,
                                      HttpAuthType authType,
                                      boolean tunelled) {
        return new HttpNoAuthHandler(key, authType, tunelled);
    }

    void configureAuthentication(HttpTestContext ctxt,
                                 HttpAuthSchemeType schemeType,
                                 HttpTestAuthenticator auth,
                                 HttpAuthType authType) {
        switch(schemeType) {
            case DIGEST:
                // DIGEST authentication is handled by the handler.
                ctxt.addFilter(new HttpDigestFilter(key, auth, authType));
                break;
            case BASIC:
                // BASIC authentication is handled by the filter.
                ctxt.addFilter(new HttpBasicFilter(key, auth, authType));
                break;
            case BASICSERVER:
                switch(authType) {
                    case PROXY: case PROXY305:
                        // HttpServer can't support Proxy-type authentication
                        // => we do as if BASIC had been specified, and we will
                        //    handle authentication in the handler.
                        ctxt.addFilter(new HttpBasicFilter(key, auth, authType));
                        break;
                    case SERVER: case SERVER307:
                        if (ctxt.getVersion() == Version.HTTP_1_1) {
                            // Basic authentication is handled by HttpServer
                            // directly => the filter should not perform
                            // authentication again.
                            setContextAuthenticator(ctxt, auth);
                            ctxt.addFilter(new HttpNoAuthFilter(key, authType));
                        } else {
                            ctxt.addFilter(new HttpBasicFilter(key, auth, authType));
                        }
                        break;
                    default:
                        throw new InternalError(key + ": Invalid combination scheme="
                             + schemeType + " authType=" + authType);
                }
            case NONE:
                // No authentication at all.
                ctxt.addFilter(new HttpNoAuthFilter(key, authType));
                break;
            default:
                throw new InternalError(key + ": No such scheme: " + schemeType);
        }
    }

    private HttpTestHandler create300Handler(String key, URL proxyURL,
                                             HttpAuthType type, int code300)
            throws MalformedURLException
    {
        return new Http3xxHandler(key, proxyURL, type, code300);
    }

    // Abstract HTTP filter class.
    private abstract static class AbstractHttpFilter extends HttpTestFilter {

        final HttpAuthType authType;
        final String type;
        public AbstractHttpFilter(HttpAuthType authType, String type) {
            this.authType = authType;
            this.type = type;
        }

        String getLocation() {
            return "Location";
        }
        String getAuthenticate() {
            return authType == HttpAuthType.PROXY
                    ? "Proxy-Authenticate" : "WWW-Authenticate";
        }
        String getAuthorization() {
            return authType == HttpAuthType.PROXY
                    ? "Proxy-Authorization" : "Authorization";
        }
        int getUnauthorizedCode() {
            return authType == HttpAuthType.PROXY
                    ? HttpURLConnection.HTTP_PROXY_AUTH
                    : HttpURLConnection.HTTP_UNAUTHORIZED;
        }
        String getKeepAlive() {
            return "keep-alive";
        }
        String getConnection() {
            return authType == HttpAuthType.PROXY
                    ? "Proxy-Connection" : "Connection";
        }
        protected abstract boolean isAuthentified(HttpTestExchange he) throws IOException;
        protected abstract void requestAuthentication(HttpTestExchange he) throws IOException;
        protected void accept(HttpTestExchange he, HttpChain chain) throws IOException {
            chain.doFilter(he);
        }

        @Override
        public String description() {
            return "Filter for " + type;
        }
        @Override
        public void doFilter(HttpTestExchange he, HttpChain chain) throws IOException {
            try {
                System.out.println(type + ": Got " + he.getRequestMethod()
                    + ": " + he.getRequestURI()
                    + "\n" + DigestEchoServer.toString(he.getRequestHeaders()));

                // Assert only a single value for Expect. Not directly related
                // to digest authentication, but verifies good client behaviour.
                List<String> expectValues = he.getRequestHeaders().get("Expect");
                if (expectValues != null && expectValues.size() > 1) {
                    throw new IOException("Expect:  " + expectValues);
                }

                if (!isAuthentified(he)) {
                    try {
                        requestAuthentication(he);
                        he.sendResponseHeaders(getUnauthorizedCode(), -1);
                        System.out.println(type
                            + ": Sent back " + getUnauthorizedCode());
                    } finally {
                        he.close();
                    }
                } else {
                    accept(he, chain);
                }
            } catch (RuntimeException | Error | IOException t) {
               System.err.println(type
                    + ": Unexpected exception while handling request: " + t);
               t.printStackTrace(System.err);
               he.close();
               throw t;
            }
        }

    }

    // WARNING: This is not a full fledged implementation of DIGEST.
    // It does contain bugs and inaccuracy.
    final static class DigestResponse {
        final String realm;
        final String username;
        final String nonce;
        final String cnonce;
        final String nc;
        final String uri;
        final String algorithm;
        final String response;
        final String qop;
        final String opaque;

        public DigestResponse(String realm, String username, String nonce,
                              String cnonce, String nc, String uri,
                              String algorithm, String qop, String opaque,
                              String response) {
            this.realm = realm;
            this.username = username;
            this.nonce = nonce;
            this.cnonce = cnonce;
            this.nc = nc;
            this.uri = uri;
            this.algorithm = algorithm;
            this.qop = qop;
            this.opaque = opaque;
            this.response = response;
        }

        String getAlgorithm(String defval) {
            return algorithm == null ? defval : algorithm;
        }
        String getQoP(String defval) {
            return qop == null ? defval : qop;
        }

        // Code stolen from DigestAuthentication:

        private static String encode(String src, char[] passwd, MessageDigest md) {
            try {
                md.update(src.getBytes("ISO-8859-1"));
            } catch (java.io.UnsupportedEncodingException uee) {
                assert false;
            }
            if (passwd != null) {
                byte[] passwdBytes = new byte[passwd.length];
                for (int i=0; i<passwd.length; i++)
                    passwdBytes[i] = (byte)passwd[i];
                md.update(passwdBytes);
                Arrays.fill(passwdBytes, (byte)0x00);
            }
            byte[] digest = md.digest();
            return HexFormat.of().formatHex(digest);
        }

        public static String computeDigest(boolean isRequest,
                                           String reqMethod,
                                           char[] password,
                                           DigestResponse params)
            throws NoSuchAlgorithmException
        {

            String A1, HashA1;
            String algorithm = params.getAlgorithm("MD5");
            boolean md5sess = algorithm.equalsIgnoreCase ("MD5-sess");

            MessageDigest md = MessageDigest.getInstance(md5sess?"MD5":algorithm);

            if (params.username == null) {
                throw new IllegalArgumentException("missing username");
            }
            if (params.realm == null) {
                throw new IllegalArgumentException("missing realm");
            }
            if (params.uri == null) {
                throw new IllegalArgumentException("missing uri");
            }
            if (params.nonce == null) {
                throw new IllegalArgumentException("missing nonce");
            }

            A1 = params.username + ":" + params.realm + ":";
            HashA1 = encode(A1, password, md);

            String A2;
            if (isRequest) {
                A2 = reqMethod + ":" + params.uri;
            } else {
                A2 = ":" + params.uri;
            }
            String HashA2 = encode(A2, null, md);
            String combo, finalHash;

            if ("auth".equals(params.qop)) { /* RRC2617 when qop=auth */
                if (params.cnonce == null) {
                    throw new IllegalArgumentException("missing nonce");
                }
                if (params.nc == null) {
                    throw new IllegalArgumentException("missing nonce");
                }
                combo = HashA1+ ":" + params.nonce + ":" + params.nc + ":" +
                            params.cnonce + ":auth:" +HashA2;

            } else { /* for compatibility with RFC2069 */
                combo = HashA1 + ":" +
                           params.nonce + ":" +
                           HashA2;
            }
            finalHash = encode(combo, null, md);
            return finalHash;
        }

        public static DigestResponse create(String raw) {
            String username, realm, nonce, nc, uri, response, cnonce,
                   algorithm, qop, opaque;
            HeaderParser parser = new HeaderParser(raw);
            username = parser.findValue("username");
            realm = parser.findValue("realm");
            nonce = parser.findValue("nonce");
            nc = parser.findValue("nc");
            uri = parser.findValue("uri");
            cnonce = parser.findValue("cnonce");
            response = parser.findValue("response");
            algorithm = parser.findValue("algorithm");
            qop = parser.findValue("qop");
            opaque = parser.findValue("opaque");
            return new DigestResponse(realm, username, nonce, cnonce, nc, uri,
                                      algorithm, qop, opaque, response);
        }

    }

    private static class HttpNoAuthFilter extends AbstractHttpFilter {

        static String type(String key, HttpAuthType authType) {
            String type = authType == HttpAuthType.SERVER
                    ? "NoAuth Server Filter" : "NoAuth Proxy Filter";
            return "["+type+"]:"+key;
        }

        public HttpNoAuthFilter(String key, HttpAuthType authType) {
            super(authType, type(key, authType));
        }

        @Override
        protected boolean isAuthentified(HttpTestExchange he) throws IOException {
            return true;
        }

        @Override
        protected void requestAuthentication(HttpTestExchange he) throws IOException {
            throw new InternalError("Should not com here");
        }

        @Override
        public String description() {
            return "Passthrough Filter";
        }

    }

    // An HTTP Filter that performs Basic authentication
    private static class HttpBasicFilter extends AbstractHttpFilter {

        static String type(String key, HttpAuthType authType) {
            String type = authType == HttpAuthType.SERVER
                    ? "Basic Server Filter" : "Basic Proxy Filter";
            return "["+type+"]:"+key;
        }

        private final HttpTestAuthenticator auth;
        public HttpBasicFilter(String key, HttpTestAuthenticator auth,
                               HttpAuthType authType) {
            super(authType, type(key, authType));
            this.auth = auth;
        }

        @Override
        protected void requestAuthentication(HttpTestExchange he)
            throws IOException
        {
            String headerName = getAuthenticate();
            String headerValue = "Basic realm=\"" + auth.getRealm() + "\"";
            he.getResponseHeaders().addHeader(headerName, headerValue);
            System.out.println(type + ": Requesting Basic Authentication, "
                               + headerName + " : "+ headerValue);
        }

        @Override
        protected boolean isAuthentified(HttpTestExchange he) {
            if (he.getRequestHeaders().containsKey(getAuthorization())) {
                List<String> authorization =
                    he.getRequestHeaders().get(getAuthorization());
                for (String a : authorization) {
                    System.out.println(type + ": processing " + a);
                    int sp = a.indexOf(' ');
                    if (sp < 0) return false;
                    String scheme = a.substring(0, sp);
                    if (!"Basic".equalsIgnoreCase(scheme)) {
                        System.out.println(type + ": Unsupported scheme '"
                                           + scheme +"'");
                        return false;
                    }
                    if (a.length() <= sp+1) {
                        System.out.println(type + ": value too short for '"
                                            + scheme +"'");
                        return false;
                    }
                    a = a.substring(sp+1);
                    return validate(a);
                }
                return false;
            }
            return false;
        }

        boolean validate(String a) {
            byte[] b = Base64.getDecoder().decode(a);
            String userpass = new String (b);
            int colon = userpass.indexOf (':');
            String uname = userpass.substring (0, colon);
            String pass = userpass.substring (colon+1);
            return auth.getUserName().equals(uname) &&
                   new String(auth.getPassword(uname)).equals(pass);
        }

        @Override
        public String description() {
            return "Filter for BASIC authentication: " + type;
        }

    }


    // An HTTP Filter that performs Digest authentication
    // WARNING: This is not a full fledged implementation of DIGEST.
    // It does contain bugs and inaccuracy.
    private static class HttpDigestFilter extends AbstractHttpFilter {

        static String type(String key, HttpAuthType authType) {
            String type = authType == HttpAuthType.SERVER
                    ? "Digest Server Filter" : "Digest Proxy Filter";
            return "["+type+"]:"+key;
        }

        // This is a very basic DIGEST - used only for the purpose of testing
        // the client implementation. Therefore we can get away with never
        // updating the server nonce as it makes the implementation of the
        // server side digest simpler.
        private final HttpTestAuthenticator auth;
        private final byte[] nonce;
        private final String ns;
        public HttpDigestFilter(String key, HttpTestAuthenticator auth, HttpAuthType authType) {
            super(authType, type(key, authType));
            this.auth = auth;
            nonce = new byte[16];
            new Random(Instant.now().toEpochMilli()).nextBytes(nonce);
            ns = new BigInteger(1, nonce).toString(16);
        }

        @Override
        protected void requestAuthentication(HttpTestExchange he)
                throws IOException {
            String separator;
            Version v = he.getExchangeVersion();
            if (v == Version.HTTP_1_1) {
                separator = "\r\n    ";
            } else if (v == Version.HTTP_2) {
                separator = " ";
            } else {
                throw new InternalError(String.valueOf(v));
            }
            String headerName = getAuthenticate();
            String headerValue = "Digest realm=\"" + auth.getRealm() + "\","
                    + separator + "qop=\"auth\","
                    + separator + "nonce=\"" + ns +"\"";
            he.getResponseHeaders().addHeader(headerName, headerValue);
            System.out.println(type + ": Requesting Digest Authentication, "
                               + headerName + " : " + headerValue);
        }

        @Override
        protected boolean isAuthentified(HttpTestExchange he) {
            if (he.getRequestHeaders().containsKey(getAuthorization())) {
                List<String> authorization = he.getRequestHeaders().get(getAuthorization());
                for (String a : authorization) {
                    System.out.println(type + ": processing " + a);
                    int sp = a.indexOf(' ');
                    if (sp < 0) return false;
                    String scheme = a.substring(0, sp);
                    if (!"Digest".equalsIgnoreCase(scheme)) {
                        System.out.println(type + ": Unsupported scheme '" + scheme +"'");
                        return false;
                    }
                    if (a.length() <= sp+1) {
                        System.out.println(type + ": value too short for '" + scheme +"'");
                        return false;
                    }
                    a = a.substring(sp+1);
                    DigestResponse dgr = DigestResponse.create(a);
                    return validate(he.getRequestURI(), he.getRequestMethod(), dgr);
                }
                return false;
            }
            return false;
        }

        boolean validate(URI uri, String reqMethod, DigestResponse dg) {
            if (!"MD5".equalsIgnoreCase(dg.getAlgorithm("MD5"))) {
                System.out.println(type + ": Unsupported algorithm "
                                   + dg.algorithm);
                return false;
            }
            if (!"auth".equalsIgnoreCase(dg.getQoP("auth"))) {
                System.out.println(type + ": Unsupported qop "
                                   + dg.qop);
                return false;
            }
            try {
                if (!dg.nonce.equals(ns)) {
                    System.out.println(type + ": bad nonce returned by client: "
                                    + nonce + " expected " + ns);
                    return false;
                }
                if (dg.response == null) {
                    System.out.println(type + ": missing digest response.");
                    return false;
                }
                char[] pa = auth.getPassword(dg.username);
                return verify(uri, reqMethod, dg, pa);
            } catch(IllegalArgumentException | SecurityException
                    | NoSuchAlgorithmException e) {
                System.out.println(type + ": " + e.getMessage());
                return false;
            }
        }


        boolean verify(URI uri, String reqMethod, DigestResponse dg, char[] pw)
            throws NoSuchAlgorithmException {
            String response = DigestResponse.computeDigest(true, reqMethod, pw, dg);
            if (!dg.response.equals(response)) {
                System.out.println(type + ": bad response returned by client: "
                                    + dg.response + " expected " + response);
                return false;
            } else {
                // A real server would also verify the uri=<request-uri>
                // parameter - but this is just a test...
                System.out.println(type + ": verified response " + response);
            }
            return true;
        }


        @Override
        public String description() {
            return "Filter for DIGEST authentication: " + type;
        }
    }

    // Abstract HTTP handler class.
    private abstract static class AbstractHttpHandler implements HttpTestHandler {

        final HttpAuthType authType;
        final String type;
        public AbstractHttpHandler(HttpAuthType authType, String type) {
            this.authType = authType;
            this.type = type;
        }

        String getLocation() {
            return "Location";
        }

        @Override
        public void handle(HttpTestExchange he) throws IOException {
            try {
                sendResponse(he);
            } catch (RuntimeException | Error | IOException t) {
               System.err.println(type
                    + ": Unexpected exception while handling request: " + t);
               t.printStackTrace(System.err);
               throw t;
            } finally {
                he.close();
            }
        }

        protected abstract void sendResponse(HttpTestExchange he) throws IOException;

    }

    static String stype(String type, String key, HttpAuthType authType, boolean tunnelled) {
        type = type + (authType == HttpAuthType.SERVER
                       ? " Server" : " Proxy")
                + (tunnelled ? " Tunnelled" : "");
        return "["+type+"]:"+key;
    }

    private class HttpNoAuthHandler extends AbstractHttpHandler {

        // true if this server is behind a proxy tunnel.
        final boolean tunnelled;
        public HttpNoAuthHandler(String key, HttpAuthType authType, boolean tunnelled) {
            super(authType, stype("NoAuth", key, authType, tunnelled));
            this.tunnelled = tunnelled;
        }

        @Override
        protected void sendResponse(HttpTestExchange he) throws IOException {
            if (DEBUG) {
                System.out.println(type + ": headers are: "
                        + DigestEchoServer.toString(he.getRequestHeaders()));
            }
            if (authType == HttpAuthType.SERVER && tunnelled) {
                // Verify that the client doesn't send us proxy-* headers
                // used to establish the proxy tunnel
                Optional<String> proxyAuth = he.getRequestHeaders()
                        .keySet().stream()
                        .filter("proxy-authorization"::equalsIgnoreCase)
                        .findAny();
                if (proxyAuth.isPresent()) {
                    System.out.println(type + " found "
                            + proxyAuth.get() + ": failing!");
                    throw new IOException(proxyAuth.get()
                            + " found by " + type + " for "
                            + he.getRequestURI());
                }
            }
            DigestEchoServer.this.writeResponse(he);
        }

    }

    // A dummy HTTP Handler that redirects all incoming requests
    // by sending a back 3xx response code (301, 305, 307 etc..)
    private class Http3xxHandler extends AbstractHttpHandler {

        private final URL redirectTargetURL;
        private final int code3XX;
        public Http3xxHandler(String key, URL proxyURL, HttpAuthType authType, int code300) {
            super(authType, stype("Server" + code300, key, authType, false));
            this.redirectTargetURL = proxyURL;
            this.code3XX = code300;
        }

        int get3XX() {
            return code3XX;
        }

        @Override
        public void sendResponse(HttpTestExchange he) throws IOException {
            System.out.println(type + ": Got " + he.getRequestMethod()
                    + ": " + he.getRequestURI()
                    + "\n" + DigestEchoServer.toString(he.getRequestHeaders()));
            System.out.println(type + ": Redirecting to "
                               + (authType == HttpAuthType.PROXY305
                                    ? "proxy" : "server"));
            he.getResponseHeaders().addHeader(getLocation(),
                redirectTargetURL.toExternalForm().toString());
            he.sendResponseHeaders(get3XX(), -1);
            System.out.println(type + ": Sent back " + get3XX() + " "
                 + getLocation() + ": " + redirectTargetURL.toExternalForm().toString());
        }
    }

    static class Configurator extends HttpsConfigurator {
        public Configurator(SSLContext ctx) {
            super(ctx);
        }

        @Override
        public void configure (HttpsParameters params) {
            params.setSSLParameters (getSSLContext().getSupportedSSLParameters());
        }
    }

    static final long start = System.nanoTime();
    public static String now() {
        long now = System.nanoTime() - start;
        long secs = now / 1000_000_000;
        long mill = (now % 1000_000_000) / 1000_000;
        long nan = now % 1000_000;
        return String.format("[%d s, %d ms, %d ns] ", secs, mill, nan);
    }

    static class  ProxyAuthorization {
        final HttpAuthSchemeType schemeType;
        final HttpTestAuthenticator authenticator;
        private final byte[] nonce;
        private final String ns;
        private final String key;

        ProxyAuthorization(String key, HttpAuthSchemeType schemeType, HttpTestAuthenticator auth) {
            this.key = key;
            this.schemeType = schemeType;
            this.authenticator = auth;
            nonce = new byte[16];
            new Random(Instant.now().toEpochMilli()).nextBytes(nonce);
            ns = new BigInteger(1, nonce).toString(16);
        }

        String doBasic(Optional<String> authorization) {
            String offset = "proxy-authorization: basic ";
            String authstring = authorization.orElse("");
            if (!authstring.toLowerCase(Locale.US).startsWith(offset)) {
                return "Proxy-Authenticate: BASIC " + "realm=\""
                        + authenticator.getRealm() +"\"";
            }
            authstring = authstring
                    .substring(offset.length())
                    .trim();
            byte[] base64 = Base64.getDecoder().decode(authstring);
            String up = new String(base64, StandardCharsets.UTF_8);
            int colon = up.indexOf(':');
            if (colon < 1) {
                return "Proxy-Authenticate: BASIC " + "realm=\""
                        + authenticator.getRealm() +"\"";
            }
            String u = up.substring(0, colon);
            String p = up.substring(colon+1);
            char[] pw = authenticator.getPassword(u);
            if (!p.equals(new String(pw))) {
                return "Proxy-Authenticate: BASIC " + "realm=\""
                        + authenticator.getRealm() +"\"";
            }
            System.out.println(now() + key + " Proxy basic authentication success");
            return null;
        }

        String doDigest(Optional<String> authorization) {
            String offset = "proxy-authorization: digest ";
            String authstring = authorization.orElse("");
            if (!authstring.toLowerCase(Locale.US).startsWith(offset)) {
                return "Proxy-Authenticate: " +
                        "Digest realm=\"" + authenticator.getRealm() + "\","
                        + "\r\n    qop=\"auth\","
                        + "\r\n    nonce=\"" + ns +"\"";
            }
            authstring = authstring
                    .substring(offset.length())
                    .trim();
            boolean validated = false;
            try {
                DigestResponse dgr = DigestResponse.create(authstring);
                validated = validate("CONNECT", dgr);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (!validated) {
                return "Proxy-Authenticate: " +
                        "Digest realm=\"" + authenticator.getRealm() + "\","
                        + "\r\n    qop=\"auth\","
                        + "\r\n    nonce=\"" + ns +"\"";
            }
            return null;
        }




        boolean validate(String reqMethod, DigestResponse dg) {
            String type = now() + this.getClass().getSimpleName() + ":" + key;
            if (!"MD5".equalsIgnoreCase(dg.getAlgorithm("MD5"))) {
                System.out.println(type + ": Unsupported algorithm "
                        + dg.algorithm);
                return false;
            }
            if (!"auth".equalsIgnoreCase(dg.getQoP("auth"))) {
                System.out.println(type + ": Unsupported qop "
                        + dg.qop);
                return false;
            }
            try {
                if (!dg.nonce.equals(ns)) {
                    System.out.println(type + ": bad nonce returned by client: "
                            + nonce + " expected " + ns);
                    return false;
                }
                if (dg.response == null) {
                    System.out.println(type + ": missing digest response.");
                    return false;
                }
                char[] pa = authenticator.getPassword(dg.username);
                return verify(type, reqMethod, dg, pa);
            } catch(IllegalArgumentException | SecurityException
                    | NoSuchAlgorithmException e) {
                System.out.println(type + ": " + e.getMessage());
                return false;
            }
        }


        boolean verify(String type, String reqMethod, DigestResponse dg, char[] pw)
                throws NoSuchAlgorithmException {
            String response = DigestResponse.computeDigest(true, reqMethod, pw, dg);
            if (!dg.response.equals(response)) {
                System.out.println(type + ": bad response returned by client: "
                        + dg.response + " expected " + response);
                return false;
            } else {
                // A real server would also verify the uri=<request-uri>
                // parameter - but this is just a test...
                System.out.println(type + ": verified response " + response);
            }
            return true;
        }

        public boolean authorize(StringBuilder response, String requestLine, String headers) {
            String message = "<html><body><p>Authorization Failed%s</p></body></html>\r\n";
            if (authenticator == null && schemeType != HttpAuthSchemeType.NONE) {
                message = String.format(message, " No Authenticator Set");
                response.append("HTTP/1.1 407 Proxy Authentication Failed\r\n");
                response.append("Content-Length: ")
                        .append(message.getBytes(StandardCharsets.UTF_8).length)
                        .append("\r\n\r\n");
                response.append(message);
                return false;
            }
            Optional<String> authorization = Stream.of(headers.split("\r\n"))
                    .filter((k) -> k.toLowerCase(Locale.US).startsWith("proxy-authorization:"))
                    .findFirst();
            String authenticate = null;
            switch(schemeType) {
                case BASIC:
                case BASICSERVER:
                    authenticate = doBasic(authorization);
                    break;
                case DIGEST:
                    authenticate = doDigest(authorization);
                    break;
                case NONE:
                    response.append("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
                    return true;
                default:
                    throw new InternalError("Unknown scheme type: " + schemeType);
            }
            if (authenticate != null) {
                message = String.format(message, "");
                response.append("HTTP/1.1 407 Proxy Authentication Required\r\n");
                response.append("Content-Length: ")
                        .append(message.getBytes(StandardCharsets.UTF_8).length)
                        .append("\r\n")
                        .append(authenticate)
                        .append("\r\n\r\n");
                response.append(message);
                return false;
            }
            response.append("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
            return true;
        }
    }

    public interface TunnelingProxy {
        InetSocketAddress getProxyAddress();
        void stop();
    }

    // This is a bit hacky: HttpsProxyTunnel is an HTTPTestServer hidden
    // behind a fake proxy that only understands CONNECT requests.
    // The fake proxy is just a server socket that intercept the
    // CONNECT and then redirect streams to the real server.
    static class HttpsProxyTunnel extends DigestEchoServer
            implements Runnable, TunnelingProxy {

        final ServerSocket ss;
        final CopyOnWriteArrayList<CompletableFuture<Void>> connectionCFs
                = new CopyOnWriteArrayList<>();
        volatile ProxyAuthorization authorization;
        volatile boolean stopped;
        public HttpsProxyTunnel(String key, HttpTestServer server, DigestEchoServer target,
                                HttpTestHandler delegate)
                throws IOException {
            this(key, server, target, delegate, ServerSocketFactory.create());
        }
        private HttpsProxyTunnel(String key, HttpTestServer server, DigestEchoServer target,
                                HttpTestHandler delegate, ServerSocket ss)
                throws IOException {
            super("HttpsProxyTunnel:" + ss.getLocalPort() + ":" + key,
                    server, target, delegate);
            System.out.flush();
            System.err.println("WARNING: HttpsProxyTunnel is an experimental test class");
            this.ss = ss;
            start();
        }

        final void start() throws IOException {
            Thread t = new Thread(this, "ProxyThread");
            t.setDaemon(true);
            t.start();
        }

        @Override
        public Version getServerVersion() {
            // serverImpl is not null when this proxy
            // serves a single server. It will be null
            // if this proxy can serve multiple servers.
            if (serverImpl != null) return serverImpl.getVersion();
            return null;
        }

        @Override
        public void stop() {
            stopped = true;
            if (serverImpl != null) {
                serverImpl.stop();
            }
            if (redirect != null) {
                redirect.stop();
            }
            try {
                ss.close();
            } catch (IOException ex) {
                if (DEBUG) ex.printStackTrace(System.out);
            }
        }


        @Override
        void configureAuthentication(HttpTestContext ctxt,
                                     HttpAuthSchemeType schemeType,
                                     HttpTestAuthenticator auth,
                                     HttpAuthType authType) {
            if (authType == HttpAuthType.PROXY || authType == HttpAuthType.PROXY305) {
                authorization = new ProxyAuthorization(key, schemeType, auth);
            } else {
                super.configureAuthentication(ctxt, schemeType, auth, authType);
            }
        }

        boolean badRequest(StringBuilder response, String hostport, List<String> hosts) {
            String message = null;
            if (hosts.isEmpty()) {
                message = "No host header provided\r\n";
            } else if (hosts.size() > 1) {
                message = "Multiple host headers provided\r\n";
                for (String h : hosts) {
                    message = message + "host: " + h + "\r\n";
                }
            } else {
                String h = hosts.get(0);
                if (!hostport.equalsIgnoreCase(h)
                        && !hostport.equalsIgnoreCase(h + ":80")
                        && !hostport.equalsIgnoreCase(h + ":443")) {
                    message = "Bad host provided: [" + h
                            + "] doesnot match [" + hostport + "]\r\n";
                }
            }
            if (message != null) {
                int length = message.getBytes(StandardCharsets.UTF_8).length;
                response.append("HTTP/1.1 400 BadRequest\r\n")
                        .append("Content-Length: " + length)
                        .append("\r\n\r\n")
                        .append(message);
                return true;
            }

            return false;
        }

        boolean authorize(StringBuilder response, String requestLine, String headers) {
            if (authorization != null) {
                return authorization.authorize(response, requestLine, headers);
            }
            response.append("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
            return true;
        }

        // Pipe the input stream to the output stream.
        private synchronized Thread pipe(InputStream is, OutputStream os, char tag, CompletableFuture<Void> end) {
            return new Thread("TunnelPipe("+tag+")") {
                @Override
                public void run() {
                    try {
                        int c = 0;
                        try {
                            while ((c = is.read()) != -1) {
                                os.write(c);
                                os.flush();
                                // if DEBUG prints a + or a - for each transferred
                                // character.
                                if (DEBUG) System.out.print(tag);
                            }
                            is.close();
                        } catch (IOException ex) {
                            if (DEBUG || !stopped && c >  -1)
                                ex.printStackTrace(System.out);
                            end.completeExceptionally(ex);
                        } finally {
                            try {os.close();} catch (Throwable t) {}
                        }
                    } finally {
                        end.complete(null);
                    }
                }
            };
        }

        @Override
        public InetSocketAddress getAddress() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    ss.getLocalPort());
        }
        @Override
        public InetSocketAddress getProxyAddress() {
            return getAddress();
        }
        @Override
        public InetSocketAddress getServerAddress() {
            // serverImpl can be null if this proxy can serve
            // multiple servers.
            if (serverImpl != null) {
                return serverImpl.getAddress();
            }
            return null;
        }


        // This is a bit shaky. It doesn't handle continuation
        // lines, but our client shouldn't send any.
        // Read a line from the input stream, swallowing the final
        // \r\n sequence. Stops at the first \n, doesn't complain
        // if it wasn't preceded by '\r'.
        //
        String readLine(InputStream r) throws IOException {
            StringBuilder b = new StringBuilder();
            int c;
            while ((c = r.read()) != -1) {
                if (c == '\n') break;
                b.appendCodePoint(c);
            }
            if (b.codePointAt(b.length() -1) == '\r') {
                b.delete(b.length() -1, b.length());
            }
            return b.toString();
        }

        @Override
        public void run() {
            Socket clientConnection = null;
            Socket targetConnection = null;
            try {
                while (!stopped) {
                    System.out.println(now() + "Tunnel: Waiting for client");
                    Socket toClose;
                    targetConnection = clientConnection = null;
                    try {
                        toClose = clientConnection = ss.accept();
                        if (NO_LINGER) {
                            // can be useful to trigger "Connection reset by peer"
                            // errors on the client side.
                            clientConnection.setOption(StandardSocketOptions.SO_LINGER, 0);
                        }
                    } catch (IOException io) {
                        if (DEBUG || !stopped) io.printStackTrace(System.out);
                        break;
                    }
                    System.out.println(now() + "Tunnel: Client accepted");
                    StringBuilder headers = new StringBuilder();
                    InputStream  ccis = clientConnection.getInputStream();
                    OutputStream ccos = clientConnection.getOutputStream();
                    Writer w = new OutputStreamWriter(
                                   clientConnection.getOutputStream(), "UTF-8");
                    PrintWriter pw = new PrintWriter(w);
                    System.out.println(now() + "Tunnel: Reading request line");
                    String requestLine = readLine(ccis);
                    System.out.println(now() + "Tunnel: Request line: " + requestLine);
                    if (requestLine.startsWith("CONNECT ")) {
                        // We should probably check that the next word following
                        // CONNECT is the host:port of our HTTPS serverImpl.
                        // Some improvement for a followup!
                        StringTokenizer tokenizer = new StringTokenizer(requestLine);
                        String connect = tokenizer.nextToken();
                        assert connect.equalsIgnoreCase("connect");
                        String hostport = tokenizer.nextToken();
                        InetSocketAddress targetAddress;
                        List<String> hosts = new ArrayList<>();
                        try {
                            URI uri = new URI("https", hostport, "/", null, null);
                            int port = uri.getPort();
                            port = port == -1 ? 443 : port;
                            targetAddress = new InetSocketAddress(uri.getHost(), port);
                            if (serverImpl != null) {
                                assert targetAddress.getHostString()
                                        .equalsIgnoreCase(serverImpl.getAddress().getHostString());
                                assert targetAddress.getPort() == serverImpl.getAddress().getPort();
                            }
                        } catch (Throwable x) {
                            System.err.printf("Bad target address: \"%s\" in \"%s\"%n",
                                    hostport, requestLine);
                            toClose.close();
                            continue;
                        }

                        // Read all headers until we find the empty line that
                        // signals the end of all headers.
                        String line = requestLine;
                        while(!line.equals("")) {
                            System.out.println(now() + "Tunnel: Reading header: "
                                               + (line = readLine(ccis)));
                            headers.append(line).append("\r\n");
                            int index = line.indexOf(':');
                            if (index >= 0) {
                                String key = line.substring(0, index).trim();
                                if (key.equalsIgnoreCase("host")) {
                                    hosts.add(line.substring(index+1).trim());
                                }
                            }
                        }
                        StringBuilder response = new StringBuilder();
                        if (TUNNEL_REQUIRES_HOST) {
                            if (badRequest(response, hostport, hosts)) {
                                System.out.println(now() + "Tunnel: Sending " + response);
                                // send the 400 response
                                pw.print(response.toString());
                                pw.flush();
                                toClose.close();
                                continue;
                            } else {
                                assert hosts.size() == 1;
                                System.out.println(now()
                                        + "Tunnel: Host header verified " + hosts);
                            }
                        }

                        final boolean authorize = authorize(response, requestLine, headers.toString());
                        if (!authorize) {
                            System.out.println(now() + "Tunnel: Sending "
                                    + response);
                            // send the 407 response
                            pw.print(response.toString());
                            pw.flush();
                            toClose.close();
                            continue;
                        }
                        System.out.println(now()
                                + "Tunnel connecting to target server at "
                                + targetAddress.getAddress() + ":" + targetAddress.getPort());
                        targetConnection = new Socket(
                                targetAddress.getAddress(),
                                targetAddress.getPort());

                        // Then send the 200 OK response to the client
                        System.out.println(now() + "Tunnel: Sending "
                                           + response);
                        pw.print(response);
                        pw.flush();
                    } else {
                        // This should not happen. If it does then just print an
                        // error - both on out and err, and close the accepted
                        // socket
                        System.out.println("WARNING: Tunnel: Unexpected status line: "
                                + requestLine + " received by "
                                + ss.getLocalSocketAddress()
                                + " from "
                                + toClose.getRemoteSocketAddress()
                                + " - closing accepted socket");
                        // Print on err
                        System.err.println("WARNING: Tunnel: Unexpected status line: "
                                             + requestLine + " received by "
                                           + ss.getLocalSocketAddress()
                                           + " from "
                                           + toClose.getRemoteSocketAddress());
                        // close accepted socket.
                        toClose.close();
                        System.err.println("Tunnel: accepted socket closed.");
                        continue;
                    }

                    // Pipe the input stream of the client connection to the
                    // output stream of the target connection and conversely.
                    // Now the client and target will just talk to each other.
                    System.out.println(now() + "Tunnel: Starting tunnel pipes");
                    CompletableFuture<Void> end, end1, end2;
                    Thread t1 = pipe(ccis, targetConnection.getOutputStream(), '+',
                            end1 = new CompletableFuture<>());
                    Thread t2 = pipe(targetConnection.getInputStream(), ccos, '-',
                            end2 = new CompletableFuture<>());
                    var end11 = end1.whenComplete((r, t) -> exceptionally(end2, t));
                    var end22 = end2.whenComplete((r, t) ->  exceptionally(end1, t));
                    end = CompletableFuture.allOf(end11, end22);
                    Socket tc = targetConnection;
                    end.whenComplete(
                            (r,t) -> {
                                try { toClose.close(); } catch (IOException x) { }
                                try { tc.close(); } catch (IOException x) { }
                                finally {connectionCFs.remove(end);}
                            });
                    connectionCFs.add(end);
                    targetConnection = clientConnection = null;
                    t1.start();
                    t2.start();
                }
            } catch (Throwable ex) {
                close(clientConnection, ex);
                close(targetConnection, ex);
                close(ss, ex);
                ex.printStackTrace(System.err);
            } finally {
                System.out.println(now() + "Tunnel: exiting (stopped=" + stopped + ")");
                connectionCFs.forEach(cf -> cf.complete(null));
            }
        }

        void exceptionally(CompletableFuture<?> cf, Throwable t) {
            if (t != null) cf.completeExceptionally(t);
        }

        void close(Closeable c, Throwable e) {
            if (c == null) return;
            try {
                c.close();
            } catch (IOException x) {
                e.addSuppressed(x);
            }
        }
    }

    /**
     * Creates a TunnelingProxy that can serve multiple servers.
     * The server address is extracted from the CONNECT request line.
     * @param authScheme The authentication scheme supported by the proxy.
     *                   Typically one of DIGEST, BASIC, NONE.
     * @return A new TunnelingProxy able to serve multiple servers.
     * @throws IOException If the proxy could not be created.
     */
    public static TunnelingProxy createHttpsProxyTunnel(HttpAuthSchemeType authScheme)
            throws IOException {
        HttpsProxyTunnel result = new HttpsProxyTunnel("", null, null, null);
        if (authScheme != HttpAuthSchemeType.NONE) {
            result.configureAuthentication(null,
                                           authScheme,
                                           AUTHENTICATOR,
                                           HttpAuthType.PROXY);
        }
        return result;
    }

    private static String protocol(String protocol) {
        if ("http".equalsIgnoreCase(protocol)) return "http";
        else if ("https".equalsIgnoreCase(protocol)) return "https";
        else throw new InternalError("Unsupported protocol: " + protocol);
    }

    public static URL url(String protocol, InetSocketAddress address,
                          String path) throws MalformedURLException {
        return new URL(protocol(protocol),
                address.getHostString(),
                address.getPort(), path);
    }

    public static URI uri(String protocol, InetSocketAddress address,
                          String path) throws URISyntaxException {
        return new URI(protocol(protocol) + "://" +
                address.getHostString() + ":" +
                address.getPort() + path);
    }
}
