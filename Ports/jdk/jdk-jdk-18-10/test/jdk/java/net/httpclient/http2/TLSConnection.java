/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Security;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

/*
 * @test
 * @bug 8150769 8157107
 * @library server
 * @summary Checks that SSL parameters can be set for HTTP/2 connection
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run main/othervm
 *       -Djdk.internal.httpclient.debug=true
 *       -Djdk.httpclient.HttpClient.log=all
 *       TLSConnection
 */
public class TLSConnection {

    private static final String KEYSTORE = System.getProperty("test.src")
            + File.separator + "keystore.p12";
    private static final String PASSWORD = "password";

    private static final SSLParameters USE_DEFAULT_SSL_PARAMETERS = new SSLParameters();

    // expect highest supported version we know about
    static String expectedTLSVersion(SSLContext ctx) throws Exception {
        if (ctx == null)
            ctx = SSLContext.getDefault();
        SSLParameters params = ctx.getSupportedSSLParameters();
        String[] protocols = params.getProtocols();
        for (String prot : protocols) {
            if (prot.equals("TLSv1.3"))
                return "TLSv1.3";
        }
        return "TLSv1.2";
    }

    public static void main(String[] args) throws Exception {
        // re-enable 3DES
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        // enable all logging
        System.setProperty("jdk.httpclient.HttpClient.log", "all,frames:all");

        // initialize JSSE
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE);
        System.setProperty("javax.net.ssl.keyStorePassword", PASSWORD);
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE);
        System.setProperty("javax.net.ssl.trustStorePassword", PASSWORD);

        Handler handler = new Handler();

        try (Http2TestServer server = new Http2TestServer("localhost", true, 0)) {
            server.addHandler(handler, "/");
            server.start();

            int port = server.getAddress().getPort();
            String uriString = "https://localhost:" + Integer.toString(port);

            // run test cases
            boolean success = true;

            SSLParameters parameters = null;
            success &= expectFailure(
                    "---\nTest #1: SSL parameters is null, expect NPE",
                    () -> connect(uriString, parameters),
                    NullPointerException.class);

            success &= expectSuccess(
                    "---\nTest #2: default SSL parameters, "
                            + "expect successful connection",
                    () -> connect(uriString, USE_DEFAULT_SSL_PARAMETERS));
            success &= checkProtocol(handler.getSSLSession(), expectedTLSVersion(null));

            // set SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA cipher suite
            // which has less priority in default cipher suite list
            success &= expectSuccess(
                    "---\nTest #3: SSL parameters with "
                            + "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA cipher suite, "
                            + "expect successful connection",
                    () -> connect(uriString, new SSLParameters(
                            new String[] { "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA" },
                            new String[] { "TLSv1.2" })));
            success &= checkProtocol(handler.getSSLSession(), "TLSv1.2");
            success &= checkCipherSuite(handler.getSSLSession(),
                    "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA");

            // set TLS_RSA_WITH_AES_128_CBC_SHA cipher suite
            // which has less priority in default cipher suite list
            // also set TLSv1.2 protocol
            success &= expectSuccess(
                    "---\nTest #4: SSL parameters with "
                            + "TLS_RSA_WITH_AES_128_CBC_SHA cipher suite,"
                            + " expect successful connection",
                    () -> connect(uriString, new SSLParameters(
                            new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA" },
                            new String[] { "TLSv1.2" })));
            success &= checkProtocol(handler.getSSLSession(), "TLSv1.2");
            success &= checkCipherSuite(handler.getSSLSession(),
                    "TLS_RSA_WITH_AES_128_CBC_SHA");

            if (success) {
                System.out.println("Test passed");
            } else {
                throw new RuntimeException("At least one test case failed");
            }
        }
    }

    private static interface Test {

        public void run() throws Exception;
    }

    private static class Handler implements Http2Handler {

        private static final byte[] BODY = "Test response".getBytes();

        private volatile SSLSession sslSession;

        @Override
        public void handle(Http2TestExchange t) throws IOException {
            System.out.println("Handler: received request to "
                    + t.getRequestURI());

            try (InputStream is = t.getRequestBody()) {
                byte[] body = is.readAllBytes();
                System.out.println("Handler: read " + body.length
                        + " bytes of body: ");
                System.out.println(new String(body));
            }

            try (OutputStream os = t.getResponseBody()) {
                t.sendResponseHeaders(200, BODY.length);
                os.write(BODY);
            }

            sslSession = t.getSSLSession();
        }

        SSLSession getSSLSession() {
            return sslSession;
        }
    }

    private static void connect(String uriString, SSLParameters sslParameters)
            throws URISyntaxException, IOException, InterruptedException
    {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2);
        if (sslParameters != USE_DEFAULT_SSL_PARAMETERS)
            builder.sslParameters(sslParameters);
        HttpClient client = builder.build();

        HttpRequest request = HttpRequest.newBuilder(new URI(uriString))
                .POST(BodyPublishers.ofString("body"))
                .build();
        String body = client.send(request, BodyHandlers.ofString()).body();

        System.out.println("Response: " + body);
    }

    private static boolean checkProtocol(SSLSession session, String protocol) {
        if (session == null) {
            System.out.println("Check protocol: no session provided");
            return false;
        }

        System.out.println("Check protocol: negotiated protocol: "
                + session.getProtocol());
        System.out.println("Check protocol: expected protocol: "
                + protocol);
        if (!protocol.equals(session.getProtocol())) {
            System.out.println("Check protocol: unexpected negotiated protocol");
            return false;
        }

        return true;
    }

    private static boolean checkCipherSuite(SSLSession session, String ciphersuite) {
        if (session == null) {
            System.out.println("Check protocol: no session provided");
            return false;
        }

        System.out.println("Check protocol: negotiated ciphersuite: "
                + session.getCipherSuite());
        System.out.println("Check protocol: expected ciphersuite: "
                + ciphersuite);
        if (!ciphersuite.equals(session.getCipherSuite())) {
            System.out.println("Check protocol: unexpected negotiated ciphersuite");
            return false;
        }

        return true;
    }

    private static boolean expectSuccess(String message, Test test) {
        System.out.println(message);
        try {
            test.run();
            System.out.println("Passed");
            return true;
        } catch (Exception e) {
            System.out.println("Failed: unexpected exception:");
            e.printStackTrace(System.out);
            return false;
        }
    }

    private static boolean expectFailure(String message, Test test,
                                         Class<? extends Throwable> expectedException) {

        System.out.println(message);
        try {
            test.run();
            System.out.println("Failed: unexpected successful connection");
            return false;
        } catch (Exception e) {
            System.out.println("Got an exception:");
            e.printStackTrace(System.out);
            if (expectedException != null
                    && !expectedException.isAssignableFrom(e.getClass())) {
                System.out.printf("Failed: expected %s, but got %s%n",
                        expectedException.getName(),
                        e.getClass().getName());
                return false;
            }
            System.out.println("Passed: expected exception");
            return true;
        }
    }
}
