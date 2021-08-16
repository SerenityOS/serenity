/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8225425
 * @summary Verifies that transparent NTLM (on Windows) is not used by default,
 *          and is used only when the relevant property is set.
 * @requires os.family == "windows"
 * @library /test/lib
 * @run testng/othervm
 *      -Dtest.auth.succeed=false
 *      TestTransparentNTLM
 * @run testng/othervm
 *      -Djdk.http.ntlm.transparentAuth=allHosts
 *      -Dtest.auth.succeed=true
 *      TestTransparentNTLM
 * @run testng/othervm
 *      -Djdk.http.ntlm.transparentAuth=blahblah
 *      -Dtest.auth.succeed=false
 *      TestTransparentNTLM
 * @run testng/othervm
 *      -Djdk.http.ntlm.transparentAuth=trustedHosts
 *      -Dtest.auth.succeed=false
 *      TestTransparentNTLM
 */

// Run with `trustedHosts` to exercise the native code, nothing more.

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import jdk.test.lib.net.URIBuilder;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.SkipException;
import static java.lang.System.out;
import static java.net.Proxy.NO_PROXY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TestTransparentNTLM {

    boolean succeed;  // true if authentication is expected to succeed
    Server server;
    URL url;

    @Test
    public void testNTLM() throws IOException {
        out.println("connecting to url: " + url);
        HttpURLConnection uc = (HttpURLConnection)url.openConnection(NO_PROXY);
        int respCode = uc.getResponseCode();
        out.println("received: " + respCode);

        if (succeed) {
            assertEquals(respCode, HttpURLConnection.HTTP_OK);
            String body = new String(uc.getInputStream().readAllBytes(), UTF_8);
            out.println("received body: " + body);
        } else {
            assertEquals(respCode, HttpURLConnection.HTTP_UNAUTHORIZED);
        }
    }

    static class Server extends Thread implements  Closeable {

        static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();
        final ServerSocket serverSocket;
        final boolean expectAuthToSucceed;

        Server(boolean expectAuthToSucceed) throws IOException {
            super("TestTransparentNTLM-Server");
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(LOOPBACK, 0));
            this.expectAuthToSucceed = expectAuthToSucceed;
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        static final String AUTH_REQUIRED =
                "HTTP/1.1 401 Unauthorized\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "WWW-Authenticate: NTLM\r\n\r\n";

        static final String AUTH_STAGE_TWO =
                "HTTP/1.1 401 Unauthorized\r\n" +
                "Content-Length: 0\r\n" +
                "WWW-Authenticate: NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==\r\n\r\n";

        static final String AUTH_SUCCESSFUL =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 11\r\n\r\n" +
                "Hello world";

        @Override
        public void run() {
            try {
                try (Socket s = serverSocket.accept()) {
                    out.println("Server accepted connection - 1");
                    readRequestHeaders(s.getInputStream());
                    s.getOutputStream().write(AUTH_REQUIRED.getBytes(UTF_8));
                }

                if (expectAuthToSucceed) {
                    // await the second follow up connection
                    try (Socket s = serverSocket.accept()) {
                        out.println("Server accepted connection - 2");
                        readRequestHeaders(s.getInputStream());
                        s.getOutputStream().write(AUTH_STAGE_TWO.getBytes(UTF_8));
                        readRequestHeaders(s.getInputStream());
                        s.getOutputStream().write(AUTH_SUCCESSFUL.getBytes(UTF_8));
                    }
                }
            } catch (IOException e) {
                fail("Unexpected exception", e);
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }

        static final byte[] REQUEST_END = new byte[] {'\r', '\n', '\r', '\n'};

        // Read until the end of the HTTP request headers
        static void readRequestHeaders(InputStream is) throws IOException {
            int requestEndCount = 0, r;
            while ((r = is.read()) != -1) {
                if (r == REQUEST_END[requestEndCount]) {
                    requestEndCount++;
                    if (requestEndCount == 4) {
                        break;
                    }
                } else {
                    requestEndCount = 0;
                }
            }
        }
    }

    @BeforeTest
    public void setup() throws Exception {
        succeed = System.getProperty("test.auth.succeed").equals("true");
        if (succeed)
            out.println("Expect client to succeed, with 200 Ok");
        else
            out.println("Expect client to fail, with 401 Unauthorized");

        server = new Server(succeed);
        server.start();
        url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(server.port())
                .path("/xxyyzz")
                .toURL();
    }

    @AfterTest
    public void teardown() throws Exception {
        server.close();
        server.join();
    }
}
