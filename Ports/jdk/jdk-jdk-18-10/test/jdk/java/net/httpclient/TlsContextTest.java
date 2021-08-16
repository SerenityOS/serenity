/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.net.http.HttpClient.Version;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.testng.Assert.assertEquals;
import jdk.test.lib.security.SecurityUtils;

/*
 * @test
 * @bug 8239594
 * @summary This test verifies that the TLS version handshake respects ssl context
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters TlsContextTest
 * @modules java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @run testng/othervm -Dtest.requiresHost=true
 *                   -Djdk.httpclient.HttpClient.log=headers
 *                   -Djdk.internal.httpclient.disableHostnameVerification
 *                   -Djdk.internal.httpclient.debug=false
 *                    TlsContextTest
 */

public class TlsContextTest implements HttpServerAdapters {

    static HttpTestServer https2Server;
    static String https2URI;
    SSLContext server;
    final static Integer ITERATIONS = 3;

    @BeforeTest
    public void setUp() throws Exception {
        // Re-enable TLSv1 and TLSv1.1 since test depends on them
        SecurityUtils.removeFromDisabledTlsAlgs("TLSv1", "TLSv1.1");

        server = SimpleSSLContext.getContext("TLS");
        final ExecutorService executor = Executors.newCachedThreadPool();
        https2Server = HttpTestServer.of(
                new Http2TestServer("localhost", true, 0, executor, 50, null, server, true));
        https2Server.addHandler(new TlsVersionTestHandler("https", https2Server),
                "/server/");
        https2Server.start();
        https2URI = "https://" + https2Server.serverAuthority() + "/server/";
    }

    @DataProvider(name = "scenarios")
    public Object[][] scenarios() throws Exception {
        return new Object[][]{
                { SimpleSSLContext.getContext("TLS"),     HTTP_2,   "TLSv1.3" },
                { SimpleSSLContext.getContext("TLSv1.2"), HTTP_2,   "TLSv1.2" },
                { SimpleSSLContext.getContext("TLSv1.1"), HTTP_1_1, "TLSv1.1" },
                { SimpleSSLContext.getContext("TLSv1.1"), HTTP_2,   "TLSv1.1" },
        };
    }

    /**
     * Tests various scenarios between client and server tls handshake with valid http
     */
    @Test(dataProvider = "scenarios")
    public void testVersionProtocols(SSLContext context,
                                     Version version,
                                     String expectedProtocol) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                                      .sslContext(context)
                                      .version(version)
                                      .build();
        HttpRequest request = HttpRequest.newBuilder(new URI(https2URI))
                                         .GET()
                                         .build();
        for (int i = 0; i < ITERATIONS; i++) {
            HttpResponse<String> response = client.send(request, ofString());
            testAllProtocols(response, expectedProtocol);
        }
    }

    private void testAllProtocols(HttpResponse<String> response,
                                  String expectedProtocol) throws Exception {
        String protocol = response.sslSession().get().getProtocol();
        int statusCode = response.statusCode();
        Version version = response.version();
        out.println("Got Body " + response.body());
        out.println("The protocol negotiated is :" + protocol);
        assertEquals(statusCode, 200);
        assertEquals(protocol, expectedProtocol);
        assertEquals(version, expectedProtocol.equals("TLSv1.1") ? HTTP_1_1 : HTTP_2);
    }

    @AfterTest
    public void teardown() throws Exception {
        https2Server.stop();
    }

    static class TlsVersionTestHandler implements HttpTestHandler {
        final String scheme;
        final HttpTestServer server;

        TlsVersionTestHandler(String scheme, HttpTestServer server) {
            this.scheme = scheme;
            this.server = server;
        }

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                t.sendResponseHeaders(200, 10);
                os.write(bytes);
            }
        }
    }
}
