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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/*
 * @test
 * @bug 8138990
 * @summary Tests for HTTP Digest auth
 *          The impl maintains a cache for auth info,
 *          the testcases run in a separate JVM to avoid cache hits
 * @modules jdk.httpserver
 * @run main/othervm DigestAuth good
 * @run main/othervm DigestAuth only_nonce
 * @run main/othervm DigestAuth sha1
 * @run main/othervm DigestAuth no_header
 * @run main/othervm DigestAuth no_nonce
 * @run main/othervm DigestAuth no_qop
 * @run main/othervm DigestAuth invalid_alg
 * @run main/othervm DigestAuth validate_server
 * @run main/othervm DigestAuth validate_server_no_qop
 */
public class DigestAuth {

    static final String EXPECT_FAILURE = null;
    static final String EXPECT_DIGEST = "Digest";
    static final String REALM = "testrealm@host.com";
    static final String NEXT_NONCE = "40f2e879449675f288476d772627370a";

    static final String GOOD_WWW_AUTH_HEADER = "Digest "
            + "realm=\"testrealm@host.com\", "
            + "qop=\"auth,auth-int\", "
            + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", "
            + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";

    static final String GOOD_WWW_AUTH_HEADER_NO_QOP = "Digest "
            + "realm=\"testrealm@host.com\", "
            + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", "
            + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";

    static final String WWW_AUTH_HEADER_NO_NONCE = "Digest "
            + "realm=\"testrealm@host.com\", "
            + "qop=\"auth,auth-int\", "
            + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";

    static final String WWW_AUTH_HEADER_NO_QOP = "Digest "
            + "realm=\"testrealm@host.com\", "
            + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", "
            + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";

    static final String WWW_AUTH_HEADER_ONLY_NONCE = "Digest "
            + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\"";

    static final String WWW_AUTH_HEADER_SHA1 = "Digest "
            + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", "
            + "algorithm=\"SHA1\"";

    static final String WWW_AUTH_HEADER_INVALID_ALGORITHM = "Digest "
            + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", "
            + "algorithm=\"SHA123\"";

    static final String AUTH_INFO_HEADER_NO_QOP_FIRST =
              "nextnonce=\"" + NEXT_NONCE + "\", "
            + "rspauth=\"ee85bc4315d8b18757809f1a8b9382d8\"";

    static final String AUTH_INFO_HEADER_NO_QOP_SECOND =
              "rspauth=\"12f2fa12841b3775b6054576722446b2\"";

    static final String AUTH_INFO_HEADER_WRONG_DIGEST =
              "nextnonce=\"" + NEXT_NONCE + "\", "
            + "rspauth=\"7327570c586207eca2afae94fc20903d\", "
            + "cnonce=\"0a4f113b\", "
            + "nc=00000001, "
            + "qop=auth";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("No testcase specified");
        }
        String testcase = args[0];

        // start a local HTTP server
        try (LocalHttpServer server = LocalHttpServer.startServer()) {

            // set authenticator
            AuthenticatorImpl auth = new AuthenticatorImpl();
            Authenticator.setDefault(auth);

            String url = String.format("http://%s/test/", server.getAuthority());

            boolean success = true;
            switch (testcase) {
                case "good":
                    // server returns a good WWW-Authenticate header
                    server.setWWWAuthHeader(GOOD_WWW_AUTH_HEADER);
                    success = testAuth(url, auth, EXPECT_DIGEST);
                    if (auth.lastRequestedPrompt == null ||
                            !auth.lastRequestedPrompt.equals(REALM)) {
                        System.out.println("Unexpected realm: "
                                + auth.lastRequestedPrompt);
                        success = false;
                    }
                    break;
                case "validate_server":
                    // enable processing Authentication-Info headers
                    System.setProperty("http.auth.digest.validateServer",
                            "true");

                    /* Server returns good WWW-Authenticate
                     * and Authentication-Info headers with wrong digest
                     */
                    server.setWWWAuthHeader(GOOD_WWW_AUTH_HEADER);
                    server.setAuthInfoHeader(AUTH_INFO_HEADER_WRONG_DIGEST);
                    success = testAuth(url, auth, EXPECT_FAILURE);
                    if (auth.lastRequestedPrompt == null ||
                            !auth.lastRequestedPrompt.equals(REALM)) {
                        System.out.println("Unexpected realm: "
                                + auth.lastRequestedPrompt);
                        success = false;
                    }
                    break;
                case "validate_server_no_qop":
                    // enable processing Authentication-Info headers
                    System.setProperty("http.auth.digest.validateServer",
                            "true");

                    /* Server returns good both WWW-Authenticate
                     * and Authentication-Info headers without any qop field,
                     * so that client-nonce should not be taked into account,
                     * and connection should succeed.
                     */
                    server.setWWWAuthHeader(GOOD_WWW_AUTH_HEADER_NO_QOP);
                    server.setAuthInfoHeader(AUTH_INFO_HEADER_NO_QOP_FIRST);
                    success = testAuth(url, auth, EXPECT_DIGEST);
                    if (auth.lastRequestedPrompt == null ||
                            !auth.lastRequestedPrompt.equals(REALM)) {
                        System.out.println("Unexpected realm: "
                                + auth.lastRequestedPrompt);
                        success = false;
                    }

                    // connect again and check if nextnonce was used
                    server.setAuthInfoHeader(AUTH_INFO_HEADER_NO_QOP_SECOND);
                    success &= testAuth(url, auth, EXPECT_DIGEST);
                    if (!NEXT_NONCE.equals(server.lastRequestedNonce)) {
                        System.out.println("Unexpected next nonce: "
                                + server.lastRequestedNonce);
                        success = false;
                    }
                    break;
                case "only_nonce":
                    /* Server returns a good WWW-Authenticate header
                     * which contains only nonce (no realm set).
                     *
                     * Realm from  WWW-Authenticate header is passed to
                     * authenticator which can use it as a prompt
                     * when it asks a user for credentials.
                     *
                     * It's fine if an HTTP client doesn't fail if no realm set,
                     * and delegates making a decision to authenticator/user.
                     */
                    server.setWWWAuthHeader(WWW_AUTH_HEADER_ONLY_NONCE);
                    success = testAuth(url, auth, EXPECT_DIGEST);
                    if (auth.lastRequestedPrompt != null &&
                            !auth.lastRequestedPrompt.trim().isEmpty()) {
                        System.out.println("Unexpected realm: "
                                + auth.lastRequestedPrompt);
                        success = false;
                    }
                    break;
                case "sha1":
                    // server returns a good WWW-Authenticate header with SHA-1
                    server.setWWWAuthHeader(WWW_AUTH_HEADER_SHA1);
                    success = testAuth(url, auth, EXPECT_DIGEST);
                    break;
                case "no_header":
                    // server returns no WWW-Authenticate header
                    success = testAuth(url, auth, EXPECT_FAILURE);
                    if (auth.lastRequestedScheme != null) {
                        System.out.println("Unexpected scheme: "
                                + auth.lastRequestedScheme);
                        success = false;
                    }
                    break;
                case "no_nonce":
                    // server returns a wrong WWW-Authenticate header (no nonce)
                    server.setWWWAuthHeader(WWW_AUTH_HEADER_NO_NONCE);
                    success = testAuth(url, auth, EXPECT_FAILURE);
                    break;
                case "invalid_alg":
                    // server returns a wrong WWW-Authenticate header
                    // (invalid hash algorithm)
                    server.setWWWAuthHeader(WWW_AUTH_HEADER_INVALID_ALGORITHM);
                    success = testAuth(url, auth, EXPECT_FAILURE);
                    break;
                case "no_qop":
                    // server returns a good WWW-Authenticate header
                    // without QOPs
                    server.setWWWAuthHeader(WWW_AUTH_HEADER_NO_QOP);
                    success = testAuth(url, auth, EXPECT_DIGEST);
                    break;
                default:
                    throw new RuntimeException("Unexpected testcase: "
                            + testcase);
            }

            if (!success) {
                throw new RuntimeException("Test failed");
            }
        }

        System.out.println("Test passed");
    }

    static boolean testAuth(String url, AuthenticatorImpl auth,
            String expectedScheme) {

        try {
            System.out.printf("Connect to %s, expected auth scheme is '%s'%n",
                    url, expectedScheme);
            load(url);

            if (expectedScheme == null) {
                System.out.println("Unexpected successful connection");
                return false;
            }

            System.out.printf("Actual auth scheme is '%s'%n",
                    auth.lastRequestedScheme);
            if (!expectedScheme.equalsIgnoreCase(auth.lastRequestedScheme)) {
                System.out.println("Unexpected auth scheme");
                return false;
            }
        } catch (IOException e) {
            if (expectedScheme != null) {
                System.out.println("Unexpected exception: " + e);
                e.printStackTrace(System.out);
                return false;
            }
            System.out.println("Expected exception: " + e);
        }

        return true;
    }

    static void load(String url) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        conn.setUseCaches(false);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {

            String line = reader.readLine();
            if (line == null) {
                throw new IOException("Couldn't read response");
            }
            do {
                System.out.println(line);
            } while ((line = reader.readLine()) != null);
        }
    }

    private static class AuthenticatorImpl extends Authenticator {

        private String lastRequestedScheme;
        private String lastRequestedPrompt;

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            lastRequestedScheme = getRequestingScheme();
            lastRequestedPrompt = getRequestingPrompt();
            System.out.println("AuthenticatorImpl: requested "
                    + lastRequestedScheme);

            return new PasswordAuthentication("Mufasa",
                    "Circle Of Life".toCharArray());
        }
    }

    // local HTTP server which pretends to support HTTP Digest auth
    static class LocalHttpServer implements HttpHandler, AutoCloseable {

        private final HttpServer server;
        private volatile String wwwAuthHeader = null;
        private volatile String authInfoHeader = null;
        private volatile String lastRequestedNonce;

        private LocalHttpServer(HttpServer server) {
            this.server = server;
        }

        public String getAuthority() {
            InetAddress address = server.getAddress().getAddress();
            String hostaddr = address.isAnyLocalAddress()
                ? "localhost" : address.getHostAddress();
            if (hostaddr.indexOf(':') > -1) {
                hostaddr = "[" + hostaddr + "]";
            }
            return hostaddr + ":" + getPort();
        }

        void setWWWAuthHeader(String wwwAuthHeader) {
            this.wwwAuthHeader = wwwAuthHeader;
        }

        void setAuthInfoHeader(String authInfoHeader) {
            this.authInfoHeader = authInfoHeader;
        }

        static LocalHttpServer startServer() throws IOException {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            HttpServer httpServer = HttpServer.create(
                    new InetSocketAddress(loopback, 0), 0);
            LocalHttpServer localHttpServer = new LocalHttpServer(httpServer);
            localHttpServer.start();

            return localHttpServer;
        }

        void start() {
            server.createContext("/test", this);
            server.start();
            System.out.println("HttpServer: started on port " + getAuthority());
        }

        void stop() {
            server.stop(0);
            System.out.println("HttpServer: stopped");
        }

        int getPort() {
            return server.getAddress().getPort();
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println("HttpServer: handle connection");

            // read a request
            try (InputStream is = t.getRequestBody()) {
                while (is.read() > 0);
            }

            try {
                List<String> headers = t.getRequestHeaders()
                        .get("Authorization");
                String header = "";
                if (headers != null && !headers.isEmpty()) {
                    header = headers.get(0).trim().toLowerCase();
                }
                if (header.startsWith("digest")) {
                    if (authInfoHeader != null) {
                        t.getResponseHeaders().add("Authentication-Info",
                                authInfoHeader);
                    }
                    lastRequestedNonce = findParameter(header, "nonce");
                    byte[] output = "hello".getBytes();
                    t.sendResponseHeaders(200, output.length);
                    t.getResponseBody().write(output);
                    System.out.println("HttpServer: return 200");
                } else {
                    if (wwwAuthHeader != null) {
                        t.getResponseHeaders().add(
                                "WWW-Authenticate", wwwAuthHeader);
                    }
                    byte[] output = "forbidden".getBytes();
                    t.sendResponseHeaders(401, output.length);
                    t.getResponseBody().write(output);
                    System.out.println("HttpServer: return 401");
                }
            } catch (IOException e) {
                System.out.println("HttpServer: exception: " + e);
                System.out.println("HttpServer: return 500");
                t.sendResponseHeaders(500, 0);
            } finally {
                t.close();
            }
        }

        private static String findParameter(String header, String name) {
            name = name.toLowerCase();
            if (header != null) {
                String[] params = header.split("\\s");
                for (String param : params) {
                    param = param.trim().toLowerCase();
                    if (param.startsWith(name)) {
                        String[] parts = param.split("=");
                        if (parts.length > 1) {
                            return parts[1]
                                    .replaceAll("\"", "").replaceAll(",", "");
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public void close() {
            stop();
        }
    }
}
