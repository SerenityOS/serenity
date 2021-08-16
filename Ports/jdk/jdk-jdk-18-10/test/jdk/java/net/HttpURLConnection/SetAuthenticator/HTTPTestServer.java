/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import sun.net.www.HeaderParser;

/**
 * A simple HTTP server that supports Digest authentication.
 * By default this server will echo back whatever is present
 * in the request body.
 * @author danielfuchs
 */
public class HTTPTestServer extends HTTPTest {

    final HttpServer      serverImpl; // this server endpoint
    final HTTPTestServer  redirect;   // the target server where to redirect 3xx
    final HttpHandler     delegate;   // unused

    private HTTPTestServer(HttpServer server, HTTPTestServer target,
                           HttpHandler delegate) {
        this.serverImpl = server;
        this.redirect = target;
        this.delegate = delegate;
    }

    public static void main(String[] args)
            throws IOException {

           HTTPTestServer server = create(HTTPTest.DEFAULT_PROTOCOL_TYPE,
                                          HTTPTest.DEFAULT_HTTP_AUTH_TYPE,
                                          HTTPTest.AUTHENTICATOR,
                                          HTTPTest.DEFAULT_SCHEME_TYPE);
           try {
               System.out.println("Server created at " + server.getAddress());
               System.out.println("Strike <Return> to exit");
               System.in.read();
           } finally {
               System.out.println("stopping server");
               server.stop();
           }
    }

    private static String toString(Headers headers) {
        return headers.entrySet().stream()
                .map((e) -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    public static HTTPTestServer create(HttpProtocolType protocol,
                                        HttpAuthType authType,
                                        HttpTestAuthenticator auth,
                                        HttpSchemeType schemeType)
            throws IOException {
        return create(protocol, authType, auth, schemeType, null);
    }

    public static HTTPTestServer create(HttpProtocolType protocol,
                                        HttpAuthType authType,
                                        HttpTestAuthenticator auth,
                                        HttpSchemeType schemeType,
                                        HttpHandler delegate)
            throws IOException {
        Objects.requireNonNull(authType);
        Objects.requireNonNull(auth);
        switch(authType) {
            // A server that performs Server Digest authentication.
            case SERVER: return createServer(protocol, authType, auth,
                                             schemeType, delegate, "/");
            // A server that pretends to be a Proxy and performs
            // Proxy Digest authentication. If protocol is HTTPS,
            // then this will create a HttpsProxyTunnel that will
            // handle the CONNECT request for tunneling.
            case PROXY: return createProxy(protocol, authType, auth,
                                           schemeType, delegate, "/");
            // A server that sends 307 redirect to a server that performs
            // Digest authentication.
            // Note: 301 doesn't work here because it transforms POST into GET.
            case SERVER307: return createServerAndRedirect(protocol,
                                                        HttpAuthType.SERVER,
                                                        auth, schemeType,
                                                        delegate, 307);
            // A server that sends 305 redirect to a proxy that performs
            // Digest authentication.
            case PROXY305:  return createServerAndRedirect(protocol,
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
     * get reused by a subsequent test in the same VM. This is to avoid having the
     * AuthCache reuse credentials from previous tests - which would invalidate the
     * assumptions made by the current test on when the default authenticator should
     * be called.
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
                    SocketAddress address = getAddress(bindable);
                    String key = toString(address);
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

        private static String toString(SocketAddress address) {
            // We don't rely on address.toString(): sometimes it can be
            // "/127.0.0.1:port", sometimes it can be "localhost/127.0.0.1:port"
            // Instead we compose our own string representation:
            InetSocketAddress candidate = (InetSocketAddress) address;
            String hostAddr = candidate.getAddress().getHostAddress();
            if (hostAddr.contains(":")) hostAddr = "[" + hostAddr + "]";
            return hostAddr + ":" + candidate.getPort();
        }

        protected abstract B createBindable() throws IOException;

        protected abstract SocketAddress getAddress(B bindable);

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
            InetAddress address = InetAddress.getLoopbackAddress();
            return new ServerSocket(0, 0, address);
        }

        @Override
        protected SocketAddress getAddress(ServerSocket socket) {
            return socket.getLocalSocketAddress();
        }

        @Override
        protected void close(ServerSocket socket) throws IOException {
            socket.close();
        }
    }

    /*
     * Used to create HttpServer for a NTLMTestServer.
     */
    private static abstract class WebServerFactory<S extends HttpServer>
            extends SocketBindableFactory<S> {
        @Override
        protected S createBindable() throws IOException {
            S server = newHttpServer();
            InetAddress address = InetAddress.getLoopbackAddress();
            server.bind(new InetSocketAddress(address, 0), 0);
            return server;
        }

        @Override
        protected SocketAddress getAddress(S server) {
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

    private static final class HttpServerFactory extends WebServerFactory<HttpServer> {
        private static final HttpServerFactory instance = new HttpServerFactory();

        static HttpServer create() throws IOException {
            return instance.createInternal();
        }

        @Override
        protected HttpServer newHttpServer() throws IOException {
            return HttpServer.create();
        }
    }

    private static final class HttpsServerFactory extends WebServerFactory<HttpsServer> {
        private static final HttpsServerFactory instance = new HttpsServerFactory();

        static HttpsServer create() throws IOException {
            return instance.createInternal();
        }

        @Override
        protected HttpsServer newHttpServer() throws IOException {
            return HttpsServer.create();
        }
    }

    static HttpServer createHttpServer(HttpProtocolType protocol) throws IOException {
        switch (protocol) {
            case HTTP:  return HttpServerFactory.create();
            case HTTPS: return configure(HttpsServerFactory.create());
            default: throw new InternalError("Unsupported protocol " + protocol);
        }
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


    static void setContextAuthenticator(HttpContext ctxt,
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

    public static HTTPTestServer createServer(HttpProtocolType protocol,
                                        HttpAuthType authType,
                                        HttpTestAuthenticator auth,
                                        HttpSchemeType schemeType,
                                        HttpHandler delegate,
                                        String path)
            throws IOException {
        Objects.requireNonNull(authType);
        Objects.requireNonNull(auth);

        HttpServer impl = createHttpServer(protocol);
        final HTTPTestServer server = new HTTPTestServer(impl, null, delegate);
        final HttpHandler hh = server.createHandler(schemeType, auth, authType);
        HttpContext ctxt = impl.createContext(path, hh);
        server.configureAuthentication(ctxt, schemeType, auth, authType);
        impl.start();
        return server;
    }

    public static HTTPTestServer createProxy(HttpProtocolType protocol,
                                        HttpAuthType authType,
                                        HttpTestAuthenticator auth,
                                        HttpSchemeType schemeType,
                                        HttpHandler delegate,
                                        String path)
            throws IOException {
        Objects.requireNonNull(authType);
        Objects.requireNonNull(auth);

        HttpServer impl = createHttpServer(protocol);
        final HTTPTestServer server = protocol == HttpProtocolType.HTTPS
                ? new HttpsProxyTunnel(impl, null, delegate)
                : new HTTPTestServer(impl, null, delegate);
        final HttpHandler hh = server.createHandler(schemeType, auth, authType);
        HttpContext ctxt = impl.createContext(path, hh);
        server.configureAuthentication(ctxt, schemeType, auth, authType);
        impl.start();

        return server;
    }

    public static HTTPTestServer createServerAndRedirect(
                                        HttpProtocolType protocol,
                                        HttpAuthType targetAuthType,
                                        HttpTestAuthenticator auth,
                                        HttpSchemeType schemeType,
                                        HttpHandler targetDelegate,
                                        int code300)
            throws IOException {
        Objects.requireNonNull(targetAuthType);
        Objects.requireNonNull(auth);

        // The connection between client and proxy can only
        // be a plain connection: SSL connection to proxy
        // is not supported by our client connection.
        HttpProtocolType targetProtocol = targetAuthType == HttpAuthType.PROXY
                                          ? HttpProtocolType.HTTP
                                          : protocol;
        HTTPTestServer redirectTarget =
                (targetAuthType == HttpAuthType.PROXY)
                ? createProxy(protocol, targetAuthType,
                              auth, schemeType, targetDelegate, "/")
                : createServer(targetProtocol, targetAuthType,
                               auth, schemeType, targetDelegate, "/");
        HttpServer impl = createHttpServer(protocol);
        final HTTPTestServer redirectingServer =
                 new HTTPTestServer(impl, redirectTarget, null);
        InetSocketAddress redirectAddr = redirectTarget.getAddress();
        URL locationURL = url(targetProtocol, redirectAddr, "/");
        final HttpHandler hh = redirectingServer.create300Handler(locationURL,
                                             HttpAuthType.SERVER, code300);
        impl.createContext("/", hh);
        impl.start();
        return redirectingServer;
    }

    public InetSocketAddress getAddress() {
        return serverImpl.getAddress();
    }

    public InetSocketAddress getProxyAddress() {
        return serverImpl.getAddress();
    }

    public void stop() {
        serverImpl.stop(0);
        if (redirect != null) {
            redirect.stop();
        }
    }

    protected void writeResponse(HttpExchange he) throws IOException {
        if (delegate == null) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            he.getResponseBody().write(he.getRequestBody().readAllBytes());
        } else {
            delegate.handle(he);
        }
    }

    private HttpHandler createHandler(HttpSchemeType schemeType,
                                      HttpTestAuthenticator auth,
                                      HttpAuthType authType) {
        return new HttpNoAuthHandler(authType);
    }

    private void configureAuthentication(HttpContext ctxt,
                            HttpSchemeType schemeType,
                            HttpTestAuthenticator auth,
                            HttpAuthType authType) {
        switch(schemeType) {
            case DIGEST:
                // DIGEST authentication is handled by the handler.
                ctxt.getFilters().add(new HttpDigestFilter(auth, authType));
                break;
            case BASIC:
                // BASIC authentication is handled by the filter.
                ctxt.getFilters().add(new HttpBasicFilter(auth, authType));
                break;
            case BASICSERVER:
                switch(authType) {
                    case PROXY: case PROXY305:
                        // HttpServer can't support Proxy-type authentication
                        // => we do as if BASIC had been specified, and we will
                        //    handle authentication in the handler.
                        ctxt.getFilters().add(new HttpBasicFilter(auth, authType));
                        break;
                    case SERVER: case SERVER307:
                        // Basic authentication is handled by HttpServer
                        // directly => the filter should not perform
                        // authentication again.
                        setContextAuthenticator(ctxt, auth);
                        ctxt.getFilters().add(new HttpNoAuthFilter(authType));
                        break;
                    default:
                        throw new InternalError("Invalid combination scheme="
                             + schemeType + " authType=" + authType);
                }
            case NONE:
                // No authentication at all.
                ctxt.getFilters().add(new HttpNoAuthFilter(authType));
                break;
            default:
                throw new InternalError("No such scheme: " + schemeType);
        }
    }

    private HttpHandler create300Handler(URL proxyURL,
        HttpAuthType type, int code300) throws MalformedURLException {
        return new Http3xxHandler(proxyURL, type, code300);
    }

    // Abstract HTTP filter class.
    private abstract static class AbstractHttpFilter extends Filter {

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
        protected abstract boolean isAuthentified(HttpExchange he) throws IOException;
        protected abstract void requestAuthentication(HttpExchange he) throws IOException;
        protected void accept(HttpExchange he, Chain chain) throws IOException {
            chain.doFilter(he);
        }

        @Override
        public String description() {
            return "Filter for " + type;
        }
        @Override
        public void doFilter(HttpExchange he, Chain chain) throws IOException {
            try {
                System.out.println(type + ": Got " + he.getRequestMethod()
                    + ": " + he.getRequestURI()
                    + "\n" + HTTPTestServer.toString(he.getRequestHeaders()));
                if (!isAuthentified(he)) {
                    try {
                        requestAuthentication(he);
                        he.sendResponseHeaders(getUnauthorizedCode(), 0);
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

    private final static class DigestResponse {
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

    private class HttpNoAuthFilter extends AbstractHttpFilter {

        public HttpNoAuthFilter(HttpAuthType authType) {
            super(authType, authType == HttpAuthType.SERVER
                            ? "NoAuth Server" : "NoAuth Proxy");
        }

        @Override
        protected boolean isAuthentified(HttpExchange he) throws IOException {
            return true;
        }

        @Override
        protected void requestAuthentication(HttpExchange he) throws IOException {
            throw new InternalError("Should not com here");
        }

        @Override
        public String description() {
            return "Passthrough Filter";
        }

    }

    // An HTTP Filter that performs Basic authentication
    private class HttpBasicFilter extends AbstractHttpFilter {

        private final HttpTestAuthenticator auth;
        public HttpBasicFilter(HttpTestAuthenticator auth, HttpAuthType authType) {
            super(authType, authType == HttpAuthType.SERVER
                            ? "Basic Server" : "Basic Proxy");
            this.auth = auth;
        }

        @Override
        protected void requestAuthentication(HttpExchange he)
            throws IOException {
            he.getResponseHeaders().add(getAuthenticate(),
                 "Basic realm=\"" + auth.getRealm() + "\"");
            System.out.println(type + ": Requesting Basic Authentication "
                 + he.getResponseHeaders().getFirst(getAuthenticate()));
        }

        @Override
        protected boolean isAuthentified(HttpExchange he) {
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
            return "Filter for " + type;
        }

    }


    // An HTTP Filter that performs Digest authentication
    private class HttpDigestFilter extends AbstractHttpFilter {

        // This is a very basic DIGEST - used only for the purpose of testing
        // the client implementation. Therefore we can get away with never
        // updating the server nonce as it makes the implementation of the
        // server side digest simpler.
        private final HttpTestAuthenticator auth;
        private final byte[] nonce;
        private final String ns;
        public HttpDigestFilter(HttpTestAuthenticator auth, HttpAuthType authType) {
            super(authType, authType == HttpAuthType.SERVER
                            ? "Digest Server" : "Digest Proxy");
            this.auth = auth;
            nonce = new byte[16];
            new Random(Instant.now().toEpochMilli()).nextBytes(nonce);
            ns = new BigInteger(1, nonce).toString(16);
        }

        @Override
        protected void requestAuthentication(HttpExchange he)
            throws IOException {
            he.getResponseHeaders().add(getAuthenticate(),
                 "Digest realm=\"" + auth.getRealm() + "\","
                 + "\r\n    qop=\"auth\","
                 + "\r\n    nonce=\"" + ns +"\"");
            System.out.println(type + ": Requesting Digest Authentication "
                 + he.getResponseHeaders().getFirst(getAuthenticate()));
        }

        @Override
        protected boolean isAuthentified(HttpExchange he) {
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
                    return validate(he.getRequestMethod(), dgr);
                }
                return false;
            }
            return false;
        }

        boolean validate(String reqMethod, DigestResponse dg) {
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
                return verify(reqMethod, dg, pa);
            } catch(IllegalArgumentException | SecurityException
                    | NoSuchAlgorithmException e) {
                System.out.println(type + ": " + e.getMessage());
                return false;
            }
        }

        boolean verify(String reqMethod, DigestResponse dg, char[] pw)
            throws NoSuchAlgorithmException {
            String response = DigestResponse.computeDigest(true, reqMethod, pw, dg);
            if (!dg.response.equals(response)) {
                System.out.println(type + ": bad response returned by client: "
                                    + dg.response + " expected " + response);
                return false;
            } else {
                System.out.println(type + ": verified response " + response);
            }
            return true;
        }

        @Override
        public String description() {
            return "Filter for DIGEST authentication";
        }
    }

    // Abstract HTTP handler class.
    private abstract static class AbstractHttpHandler implements HttpHandler {

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
        public void handle(HttpExchange he) throws IOException {
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

        protected abstract void sendResponse(HttpExchange he) throws IOException;

    }

    private class HttpNoAuthHandler extends AbstractHttpHandler {

        public HttpNoAuthHandler(HttpAuthType authType) {
            super(authType, authType == HttpAuthType.SERVER
                            ? "NoAuth Server" : "NoAuth Proxy");
        }

        @Override
        protected void sendResponse(HttpExchange he) throws IOException {
            HTTPTestServer.this.writeResponse(he);
        }

    }

    // A dummy HTTP Handler that redirects all incoming requests
    // by sending a back 3xx response code (301, 305, 307 etc..)
    private class Http3xxHandler extends AbstractHttpHandler {

        private final URL redirectTargetURL;
        private final int code3XX;
        public Http3xxHandler(URL proxyURL, HttpAuthType authType, int code300) {
            super(authType, "Server" + code300);
            this.redirectTargetURL = proxyURL;
            this.code3XX = code300;
        }

        int get3XX() {
            return code3XX;
        }

        @Override
        public void sendResponse(HttpExchange he) throws IOException {
            System.out.println(type + ": Got " + he.getRequestMethod()
                    + ": " + he.getRequestURI()
                    + "\n" + HTTPTestServer.toString(he.getRequestHeaders()));
            System.out.println(type + ": Redirecting to "
                               + (authType == HttpAuthType.PROXY305
                                    ? "proxy" : "server"));
            he.getResponseHeaders().add(getLocation(),
                redirectTargetURL.toExternalForm().toString());
            he.sendResponseHeaders(get3XX(), 0);
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

    // This is a bit hacky: HttpsProxyTunnel is an HTTPTestServer hidden
    // behind a fake proxy that only understands CONNECT requests.
    // The fake proxy is just a server socket that intercept the
    // CONNECT and then redirect streams to the real server.
    static class HttpsProxyTunnel extends HTTPTestServer
            implements Runnable {

        final ServerSocket ss;
        private volatile boolean stop;

        public HttpsProxyTunnel(HttpServer server, HTTPTestServer target,
                               HttpHandler delegate)
                throws IOException {
            super(server, target, delegate);
            System.out.flush();
            System.err.println("WARNING: HttpsProxyTunnel is an experimental test class");
            ss = ServerSocketFactory.create();
            start();
        }

        final void start() throws IOException {
            Thread t = new Thread(this, "ProxyThread");
            t.setDaemon(true);
            t.start();
        }

        @Override
        public void stop() {
            try (var toClose = ss) {
                stop = true;
                System.out.println("Server " + ss + " stop requested");
                super.stop();
            } catch (IOException ex) {
                if (DEBUG) ex.printStackTrace(System.out);
            }
        }

        // Pipe the input stream to the output stream.
        private synchronized Thread pipe(InputStream is, OutputStream os, char tag) {
            return new Thread("TunnelPipe("+tag+")") {
                @Override
                public void run() {
                    try {
                        try {
                            int c;
                            while ((c = is.read()) != -1) {
                                os.write(c);
                                os.flush();
                                // if DEBUG prints a + or a - for each transferred
                                // character.
                                if (DEBUG) System.out.print(tag);
                            }
                            is.close();
                        } finally {
                            os.close();
                        }
                    } catch (IOException ex) {
                        if (DEBUG) ex.printStackTrace(System.out);
                    }
                }
            };
        }

        @Override
        public InetSocketAddress getProxyAddress() {
            return new InetSocketAddress(ss.getInetAddress(), ss.getLocalPort());
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
            if (b.length() == 0) {
                return "";
            }
            if (b.codePointAt(b.length() -1) == '\r') {
                b.delete(b.length() -1, b.length());
            }
            return b.toString();
        }

        @Override
        public void run() {
            Socket clientConnection = null;
            while (!stop) {
                System.out.println("Tunnel: Waiting for client at: " + ss);
                final Socket previous = clientConnection;
                try {
                    clientConnection = ss.accept();
                } catch (IOException io) {
                    try {
                        ss.close();
                    } catch (IOException ex) {
                        if (DEBUG) {
                            ex.printStackTrace(System.out);
                        }
                    }
                    // log the reason that caused the server to stop accepting connections
                    if (!stop) {
                        System.err.println("Server will stop accepting connections due to an exception:");
                        io.printStackTrace();
                    }
                    break;
                } finally {
                    // close the previous connection
                    if (previous != null) {
                        try {
                            previous.close();
                        } catch (IOException e) {
                            // ignore
                            if (DEBUG) {
                                System.out.println("Ignoring exception that happened while closing " +
                                        "an older connection:");
                                e.printStackTrace(System.out);
                            }
                        }
                    }
                }
                System.out.println("Tunnel: Client accepted");
                try {
                    // We have only 1 client... process the current client
                    // request and wait until it has finished before
                    // accepting a new connection request.
                    processRequestAndWaitToComplete(clientConnection);
                } catch (IOException ioe) {
                    // close the client connection
                    try {
                        clientConnection.close();
                    } catch (IOException io) {
                        // ignore
                        if (DEBUG) {
                            System.out.println("Ignoring exception that happened during client" +
                                    " connection close:");
                            io.printStackTrace(System.out);
                        }
                    } finally {
                        clientConnection = null;
                    }
                } catch (Throwable t) {
                    // don't close the client connection for non-IOExceptions, instead
                    // just log it and move on to accept next connection
                    if (!stop) {
                        t.printStackTrace();
                    }
                }
            }
        }

        private void processRequestAndWaitToComplete(final Socket clientConnection)
                throws IOException, InterruptedException {
            final Socket targetConnection;
            InputStream  ccis = clientConnection.getInputStream();
            OutputStream ccos = clientConnection.getOutputStream();
            Writer w = new OutputStreamWriter(
                    clientConnection.getOutputStream(), "UTF-8");
            PrintWriter pw = new PrintWriter(w);
            System.out.println("Tunnel: Reading request line");
            String requestLine = readLine(ccis);
            System.out.println("Tunnel: Request line: " + requestLine);
            if (requestLine.startsWith("CONNECT ")) {
                // We should probably check that the next word following
                // CONNECT is the host:port of our HTTPS serverImpl.
                // Some improvement for a followup!

                // Read all headers until we find the empty line that
                // signals the end of all headers.
                while(!requestLine.equals("")) {
                    System.out.println("Tunnel: Reading header: "
                            + (requestLine = readLine(ccis)));
                }

                targetConnection = new Socket(
                        serverImpl.getAddress().getAddress(),
                        serverImpl.getAddress().getPort());

                // Then send the 200 OK response to the client
                System.out.println("Tunnel: Sending "
                        + "HTTP/1.1 200 OK\r\n\r\n");
                pw.print("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
                pw.flush();
            } else {
                // This should not happen. If it does then consider it a
                // client error and throw an IOException
                System.out.println("Tunnel: Throwing an IOException due to unexpected" +
                        " request line: " + requestLine);
                throw new IOException("Client request error - Unexpected request line");
            }

            // Pipe the input stream of the client connection to the
            // output stream of the target connection and conversely.
            // Now the client and target will just talk to each other.
            System.out.println("Tunnel: Starting tunnel pipes");
            Thread t1 = pipe(ccis, targetConnection.getOutputStream(), '+');
            Thread t2 = pipe(targetConnection.getInputStream(), ccos, '-');
            t1.start();
            t2.start();
            // wait for the request to complete
            t1.join();
            t2.join();
        }
    }
}
