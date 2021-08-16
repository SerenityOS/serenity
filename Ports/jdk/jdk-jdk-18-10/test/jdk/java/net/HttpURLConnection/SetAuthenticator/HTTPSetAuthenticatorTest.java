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
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * @test
 * @bug 8169415
 * @library /test/lib
 * @modules java.logging
 *          java.base/sun.net.www
 *          java.base/sun.net.www.protocol.http
 *          jdk.httpserver/sun.net.httpserver
 * @build jdk.test.lib.net.SimpleSSLContext HTTPTest HTTPTestServer HTTPTestClient HTTPSetAuthenticatorTest
 * @summary A simple HTTP test that starts an echo server supporting the given
 *          authentication scheme, then starts a regular HTTP client to invoke it.
 *          The client first does a GET request on "/", then follows on
 *          with a POST request that sends "Hello World!" to the server.
 *          The client expects to receive "Hello World!" in return.
 *          The test supports several execution modes:
 *            SERVER: The server performs Server authentication;
 *            PROXY:  The server pretends to be a proxy and performs
 *                    Proxy authentication;
 *            SERVER307: The server redirects the client (307) to another
 *                    server that perform Server authentication;
 *            PROXY305: The server attempts to redirect
 *                    the client to a proxy using 305 code;
 *           This test runs the client several times, providing different
 *           authenticators to the HttpURLConnection and verifies that
 *           the authenticator is invoked as expected - validating that
 *           connections with different authenticators do not share each
 *           other's socket channel and authentication info.
 *           Note: BASICSERVER means that the server will let the underlying
 *                 com.sun.net.httpserver.HttpServer perform BASIC
 *                 authentication when in Server mode. There should be
 *                 no real difference between BASICSERVER and BASIC - it should
 *                 be transparent on the client side.
 * @run main/othervm HTTPSetAuthenticatorTest NONE SERVER PROXY SERVER307 PROXY305
 * @run main/othervm HTTPSetAuthenticatorTest DIGEST SERVER
 * @run main/othervm HTTPSetAuthenticatorTest DIGEST PROXY
 * @run main/othervm HTTPSetAuthenticatorTest DIGEST PROXY305
 * @run main/othervm HTTPSetAuthenticatorTest DIGEST SERVER307
 * @run main/othervm HTTPSetAuthenticatorTest BASIC  SERVER
 * @run main/othervm HTTPSetAuthenticatorTest BASIC  PROXY
 * @run main/othervm HTTPSetAuthenticatorTest BASIC  PROXY305
 * @run main/othervm HTTPSetAuthenticatorTest BASIC  SERVER307
 * @run main/othervm HTTPSetAuthenticatorTest BASICSERVER SERVER
 * @run main/othervm HTTPSetAuthenticatorTest BASICSERVER SERVER307
 *
 * @author danielfuchs
 */
public class HTTPSetAuthenticatorTest extends HTTPTest {

    public static void main(String[] args) throws Exception {
        String[] schemes;
        String[] params;
         if (args == null || args.length == 0) {
            schemes = Stream.of(HttpSchemeType.values())
                        .map(HttpSchemeType::name)
                        .collect(Collectors.toList())
                        .toArray(new String[0]);
            params = new String[0];
        } else {
            schemes = new String[] { args[0] };
            params = Arrays.copyOfRange(args, 1, args.length);
        }
        for (String scheme : schemes) {
            System.out.println("==== Testing with scheme=" + scheme + " ====\n");
            new HTTPSetAuthenticatorTest(HttpSchemeType.valueOf(scheme))
                .execute(params);
            System.out.println();
        }
    }

    final HttpSchemeType scheme;
    public HTTPSetAuthenticatorTest(HttpSchemeType scheme) {
        this.scheme = scheme;
    }

    @Override
    public HttpSchemeType getHttpSchemeType() {
        return scheme;
    }

    @Override
    public int run(HTTPTestServer server,
                   HttpProtocolType protocol,
                   HttpAuthType mode)
            throws IOException
    {
        HttpTestAuthenticator authOne = new HttpTestAuthenticator("authOne", "dublin", "foox");
        HttpTestAuthenticator authTwo = new HttpTestAuthenticator("authTwo", "dublin", "foox");
        int expectedIncrement = scheme == HttpSchemeType.NONE
                                ? 0 : EXPECTED_AUTH_CALLS_PER_TEST;
        int count;
        int defaultCount = AUTHENTICATOR.count.get();

        // Connect to the server with a GET request, then with a
        // POST that contains "Hello World!"
        // Uses authenticator #1
        System.out.println("\nClient: Using authenticator #1: "
            + toString(authOne));
        HTTPTestClient.connect(protocol, server, mode, authOne);
        count = authOne.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #1 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }

        // Connect to the server with a GET request, then with a
        // POST that contains "Hello World!"
        // Uses authenticator #2
        System.out.println("\nClient: Using authenticator #2: "
            + toString(authTwo));
        HTTPTestClient.connect(protocol, server, mode, authTwo);
        count = authTwo.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #2 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }

        // Connect to the server with a GET request, then with a
        // POST that contains "Hello World!"
        // Uses authenticator #1
        System.out.println("\nClient: Using authenticator #1 again: "
            + toString(authOne));
        HTTPTestClient.connect(protocol, server, mode, authOne);
        count = authOne.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #1 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }
        count = authTwo.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #2 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }
        count =  AUTHENTICATOR.count.get();
        if (count != defaultCount) {
            throw new AssertionError("Default Authenticator called " + count(count)
                + " expected it to be called " + expected(defaultCount));
        }

        // Now tries with the default authenticator: it should be invoked.
        System.out.println("\nClient: Using the default authenticator: "
            + toString(null));
        HTTPTestClient.connect(protocol, server, mode, null);
        count = authOne.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #1 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }
        count = authTwo.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #2 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }
        count =  AUTHENTICATOR.count.get();
        if (count != defaultCount + expectedIncrement) {
            throw new AssertionError("Default Authenticator called " + count(count)
                + " expected it to be called " + expected(defaultCount + expectedIncrement));
        }

        // Now tries with explicitly setting the default authenticator: it should
        // be invoked again.
        // Uncomment the code below when 8169068 is available.
//        System.out.println("\nClient: Explicitly setting the default authenticator: "
//            + toString(Authenticator.getDefault()));
//        HTTPTestClient.connect(protocol, server, mode, Authenticator.getDefault());
//        count = authOne.count.get();
//        if (count != expectedIncrement) {
//            throw new AssertionError("Authenticator #1 called " + count(count)
//                + " expected it to be called " + expected(expectedIncrement));
//        }
//        count = authTwo.count.get();
//        if (count != expectedIncrement) {
//            throw new AssertionError("Authenticator #2 called " + count(count)
//                + " expected it to be called " + expected(expectedIncrement));
//        }
//        count =  AUTHENTICATOR.count.get();
//        if (count != defaultCount + 2 * expectedIncrement) {
//            throw new AssertionError("Default Authenticator called " + count(count)
//                + " expected it to be called "
//                + expected(defaultCount + 2 * expectedIncrement));
//        }

        // Now tries to set an authenticator on a connected connection.
        URL url = url(protocol,  server.getAddress(), "/");
        Proxy proxy = proxy(server, mode);
        HttpURLConnection conn = openConnection(url, mode, proxy);
        try {
            conn.setAuthenticator(null);
            throw new RuntimeException("Expected NullPointerException"
                    + " trying to set a null authenticator"
                    + " not raised.");
        } catch (NullPointerException npe) {
            System.out.println("Client: caught expected NPE"
                    + " trying to set a null authenticator: "
                    + npe);
        }
        conn.connect();
        try {
            try {
                conn.setAuthenticator(authOne);
                throw new RuntimeException("Expected IllegalStateException"
                        + " trying to set an authenticator after connect"
                        + " not raised.");
            } catch (IllegalStateException ise) {
                System.out.println("Client: caught expected ISE"
                        + " trying to set an authenticator after connect: "
                        + ise);
            }
            // Uncomment the code below when 8169068 is available.
//            try {
//                conn.setAuthenticator(Authenticator.getDefault());
//                throw new RuntimeException("Expected IllegalStateException"
//                        + " trying to set an authenticator after connect"
//                        + " not raised.");
//            } catch (IllegalStateException ise) {
//                System.out.println("Client: caught expected ISE"
//                        + " trying to set an authenticator after connect: "
//                        + ise);
//            }
            try {
                conn.setAuthenticator(null);
                throw new RuntimeException("Expected"
                        + " IllegalStateException or NullPointerException"
                        + " trying to set a null authenticator after connect"
                        + " not raised.");
            } catch (IllegalStateException | NullPointerException xxe) {
                System.out.println("Client: caught expected "
                        + xxe.getClass().getSimpleName()
                        + " trying to set a null authenticator after connect: "
                        + xxe);
            }
        } finally {
            conn.disconnect();
        }

        // double check that authOne and authTwo haven't been invoked.
        count = authOne.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #1 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }
        count = authTwo.count.get();
        if (count != expectedIncrement) {
            throw new AssertionError("Authenticator #2 called " + count(count)
                + " expected it to be called " + expected(expectedIncrement));
        }

        // All good!
        // return the number of times the default authenticator is supposed
        // to have been called.
        return scheme == HttpSchemeType.NONE ? 0 : 1 * EXPECTED_AUTH_CALLS_PER_TEST;
    }

    static String toString(Authenticator a) {
        return sun.net.www.protocol.http.AuthenticatorKeys.getKey(a);
    }

}
