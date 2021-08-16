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

/*
 * @test
 * @summary Basic test for Expect 100-Continue ( HTTP/1.1 only )
 * @modules java.net.http
 *          jdk.httpserver
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm ExpectContinue
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;

public class ExpectContinue {

    SSLContext sslContext;
    HttpServer httpTestServer;         // HTTP/1.1    [ 2 servers ]
    HttpsServer httpsTestServer;       // HTTPS/1.1
    String httpURI;
    String httpsURI;

    @DataProvider(name = "positive")
    public Object[][] positive() {
        return new Object[][] {
                { httpURI,  false, "Billy" },
                { httpURI,  false, "Bob"   },
                { httpURI,  true,  "Jimmy" },
                { httpsURI, true,  "Jack"  },
        };
    }

    @Test(dataProvider = "positive")
    void test(String uriString, boolean expectedContinue, String data)
        throws Exception
    {
        out.printf("test(%s, %s, %s): starting%n", uriString, expectedContinue, data);
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .expectContinue(expectedContinue)
                .POST(BodyPublishers.ofString(data))
                .build();

        HttpResponse<String> response = client.send(request,
                                                    BodyHandlers.ofString());
        System.out.println("First response: " + response);
        assertEquals(response.statusCode(), 200);
        assertEquals(response.body(), data);

        // again with the same request, to ensure no Expect header duplication
        response = client.send(request, BodyHandlers.ofString());
        System.out.println("Second response: " + response);
        assertEquals(response.statusCode(), 200);
        assertEquals(response.body(), data);
    }

    @Test(dataProvider = "positive")
    void testAsync(String uriString, boolean expectedContinue, String data) {
        out.printf("test(%s, %s, %s): starting%n", uriString, expectedContinue, data);
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .expectContinue(expectedContinue)
                .POST(BodyPublishers.ofString(data))
                .build();

        HttpResponse<String> response = client.sendAsync(request,
                BodyHandlers.ofString()).join();
        System.out.println("First response: " + response);
        assertEquals(response.statusCode(), 200);
        assertEquals(response.body(), data);

        // again with the same request, to ensure no Expect header duplication
        response = client.sendAsync(request, BodyHandlers.ofString()).join();
        System.out.println("Second response: " + response);
        assertEquals(response.statusCode(), 200);
        assertEquals(response.body(), data);
    }

    // -- Infrastructure

    static String serverAuthority(HttpServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getAddress().getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpServer.create(sa, 0);
        httpTestServer.createContext("/http1/ec", new Http1ExpectContinueHandler());
        httpURI = "http://" + serverAuthority(httpTestServer) + "/http1/ec";

        httpsTestServer = HttpsServer.create(sa, 0);
        httpsTestServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer.createContext("/https1/ec", new Http1ExpectContinueHandler());
        httpsURI = "https://" + serverAuthority(httpsTestServer) + "/https1/ec";

        httpTestServer.start();
        httpsTestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop(0);
        httpsTestServer.stop(0);
    }

    static class Http1ExpectContinueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();

                List<String> expect = t.getRequestHeaders().get("Expect");
                if (expect != null && expect.size() != 1) {
                    System.out.println("Server: Expect: " + expect);
                    Throwable ex = new AssertionError("Expect: " + expect);
                    ex.printStackTrace();
                    t.sendResponseHeaders(500, 0);
                } else {
                    t.sendResponseHeaders(200, bytes.length);
                    os.write(bytes);
                }
            }
        }
    }
}
