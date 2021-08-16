/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import jdk.test.lib.net.SimpleSSLContext;
import static java.net.Proxy.NO_PROXY;

/*
 * @test
 * @bug 8169415
 * @library /test/lib
 * @modules java.logging
 *          java.base/sun.net.www
 *          jdk.httpserver/sun.net.httpserver
 * @build jdk.test.lib.net.SimpleSSLContext HTTPTest HTTPTestServer HTTPTestClient
 * @summary A simple HTTP test that starts an echo server supporting Digest
 *          authentication, then starts a regular HTTP client to invoke it.
 *          The client first does a GET request on "/", then follows on
 *          with a POST request that sends "Hello World!" to the server.
 *          The client expects to receive "Hello World!" in return.
 *          The test supports several execution modes:
 *            SERVER: The server performs Digest Server authentication;
 *            PROXY:  The server pretends to be a proxy and performs
 *                    Digest Proxy authentication;
 *            SERVER307: The server redirects the client (307) to another
 *                    server that perform Digest authentication;
 *            PROXY305: The server attempts to redirect
 *                    the client to a proxy using 305 code;
 * @run main/othervm HTTPTest SERVER
 * @run main/othervm HTTPTest PROXY
 * @run main/othervm HTTPTest SERVER307
 * @run main/othervm HTTPTest PROXY305
 *
 * @author danielfuchs
 */
public class HTTPTest {

    public static final boolean DEBUG =
         Boolean.parseBoolean(System.getProperty("test.debug", "false"));
    public static enum HttpAuthType { SERVER, PROXY, SERVER307, PROXY305 };
    public static enum HttpProtocolType { HTTP, HTTPS };
    public static enum HttpSchemeType { NONE, BASICSERVER, BASIC, DIGEST };
    public static final HttpAuthType DEFAULT_HTTP_AUTH_TYPE = HttpAuthType.SERVER;
    public static final HttpProtocolType DEFAULT_PROTOCOL_TYPE = HttpProtocolType.HTTP;
    public static final HttpSchemeType DEFAULT_SCHEME_TYPE = HttpSchemeType.DIGEST;

    public static class HttpTestAuthenticator extends Authenticator {
        private final String realm;
        private final String username;
        // Used to prevent incrementation of 'count' when calling the
        // authenticator from the server side.
        private final ThreadLocal<Boolean> skipCount = new ThreadLocal<>();
        // count will be incremented every time getPasswordAuthentication()
        // is called from the client side.
        final AtomicInteger count = new AtomicInteger();
        private final String name;

        public HttpTestAuthenticator(String name, String realm, String username) {
            this.name = name;
            this.realm = realm;
            this.username = username;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (skipCount.get() == null || skipCount.get().booleanValue() == false) {
                System.out.println("Authenticator " + name + " called: " + count.incrementAndGet());
            }
            return new PasswordAuthentication(getUserName(),
                    new char[] {'b','a','r'});
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

        @Override
        public String toString() {
            return super.toString() + "[name=\"" + name + "\"]";
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
        AUTHENTICATOR = new HttpTestAuthenticator("AUTHENTICATOR","dublin", "foox");
        Authenticator.setDefault(AUTHENTICATOR);
    }

    static {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext.setDefault(new SimpleSSLContext().get());
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static final Logger logger = Logger.getLogger ("com.sun.net.httpserver");
    static {
        if (DEBUG) logger.setLevel(Level.ALL);
        Stream.of(Logger.getLogger("").getHandlers())
              .forEach(h -> h.setLevel(Level.ALL));
    }

    static final int EXPECTED_AUTH_CALLS_PER_TEST = 1;

    public static void main(String[] args) throws Exception {
        // new HTTPTest().execute(HttpAuthType.SERVER.name());
        new HTTPTest().execute(args);
    }

    public void execute(String... args) throws Exception {
        Stream<HttpAuthType> modes;
        if (args == null || args.length == 0) {
            modes = Stream.of(HttpAuthType.values());
        } else {
            modes = Stream.of(args).map(HttpAuthType::valueOf);
        }
        modes.forEach(this::test);
        System.out.println("Test PASSED - Authenticator called: "
                 + expected(AUTHENTICATOR.count.get()));
    }

    public void test(HttpAuthType mode) {
        for (HttpProtocolType type: HttpProtocolType.values()) {
            test(type, mode);
        }
    }

    public HttpSchemeType getHttpSchemeType() {
        return DEFAULT_SCHEME_TYPE;
    }

    public void test(HttpProtocolType protocol, HttpAuthType mode) {
        if (mode == HttpAuthType.PROXY305 && protocol == HttpProtocolType.HTTPS ) {
            // silently skip unsupported test combination
            return;
        }
        System.out.println("\n**** Testing " + protocol + " "
                           + mode + " mode ****\n");
        int authCount = AUTHENTICATOR.count.get();
        int expectedIncrement = 0;
        try {
            // Creates an HTTP server that echoes back whatever is in the
            // request body.
            HTTPTestServer server =
                    HTTPTestServer.create(protocol,
                                          mode,
                                          AUTHENTICATOR,
                                          getHttpSchemeType());
            try {
                expectedIncrement += run(server, protocol, mode);
            } finally {
                server.stop();
            }
        }  catch (IOException ex) {
            ex.printStackTrace(System.err);
            throw new UncheckedIOException(ex);
        }
        int count = AUTHENTICATOR.count.get();
        if (count != authCount + expectedIncrement) {
            throw new AssertionError("Authenticator called " + count(count)
                        + " expected it to be called "
                        + expected(authCount + expectedIncrement));
        }
    }

    /**
     * Runs the test with the given parameters.
     * @param server    The server
     * @param protocol  The protocol (HTTP/HTTPS)
     * @param mode      The mode (PROXY, SERVER, SERVER307...)
     * @return The number of times the default authenticator should have been
     *         called.
     * @throws IOException in case of connection or protocol issues
     */
    public int run(HTTPTestServer server,
                   HttpProtocolType protocol,
                   HttpAuthType mode)
            throws IOException
    {
        // Connect to the server with a GET request, then with a
        // POST that contains "Hello World!"
        HTTPTestClient.connect(protocol, server, mode, null);
        // return the number of times the default authenticator is supposed
        // to have been called.
        return EXPECTED_AUTH_CALLS_PER_TEST;
    }

    public static String count(int count) {
        switch(count) {
            case 0: return "not even once";
            case 1: return "once";
            case 2: return "twice";
            default: return String.valueOf(count) + " times";
        }
    }

    public static String expected(int count) {
        switch(count) {
            default: return count(count);
        }
    }
    public static String protocol(HttpProtocolType type) {
        return type.name().toLowerCase(Locale.US);
    }

    public static URL url(HttpProtocolType protocol, InetSocketAddress address,
                          String path) throws MalformedURLException {
        return new URL(protocol(protocol),
                       address.getAddress().getHostAddress(),
                       address.getPort(), path);
    }

    public static Proxy proxy(HTTPTestServer server, HttpAuthType authType) {
        if (authType != HttpAuthType.PROXY) return null;

        InetSocketAddress proxyAddress = server.getProxyAddress();
        if (!proxyAddress.isUnresolved()) {
            // Forces the proxy to use an unresolved address created
            // from the actual IP address to avoid using the proxy
            // address hostname which would result in resolving to
            // a posibly different address. For instance we want to
            // avoid cases such as:
            //    ::1 => "localhost" => 127.0.0.1
            proxyAddress = InetSocketAddress.
                createUnresolved(proxyAddress.getAddress().getHostAddress(),
                                 proxyAddress.getPort());
        }

        return new Proxy(Proxy.Type.HTTP, proxyAddress);
    }

    public static HttpURLConnection openConnection(URL url,
                                                   HttpAuthType authType,
                                                   Proxy proxy)
                                    throws IOException {

        HttpURLConnection conn = (HttpURLConnection)
                (authType == HttpAuthType.PROXY
                    ? url.openConnection(proxy)
                    : url.openConnection(NO_PROXY));
        return conn;
    }
}
