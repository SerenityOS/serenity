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

/*
 * @test
 * @summary Checks HTTP versions when interacting with an HTTP/2 server
 * @bug 8242044
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @build jdk.test.lib.Platform
 * @run testng/othervm HttpVersionsTest
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.String.format;
import static java.lang.System.out;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class HttpVersionsTest {

    SSLContext sslContext;
    Http2TestServer http2TestServer;
    Http2TestServer https2TestServer;
    String http2URI;
    String https2URI;

    static final int ITERATIONS = 3;
    static final String[] BODY = new String[] {
            "I'd like another drink I think",
            "Another drink to make me pink",
            "I think I'll drink until I stink",
            "I'll drink until I cannot blink"
    };
    int nextBodyId;

    @DataProvider(name = "scenarios")
    public Object[][] scenarios() {
        return new Object[][] {
                { http2URI,  true  },
                { https2URI, true  },
                { http2URI,  false },
                { https2URI, false },
        };
    }

    /** Checks that an HTTP/2 request receives an HTTP/2 response. */
    @Test(dataProvider = "scenarios")
    void testHttp2Get(String uri, boolean sameClient) throws Exception {
        out.println(format("\n--- testHttp2Get uri:%s, sameClient:%s", uri, sameClient));
        HttpClient client = null;
        for (int i=0; i<ITERATIONS; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                                   .sslContext(sslContext)
                                   .version(HTTP_2)
                                   .build();

            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .build();
            HttpResponse<String> response = client.send(request, ofString());
            out.println("Got response: " + response);
            out.println("Got body: " + response.body());

            assertEquals(response.statusCode(), 200);
            assertEquals(response.version(), HTTP_2);
            assertEquals(response.body(), "");
            if (uri.startsWith("https"))
                assertTrue(response.sslSession().isPresent());
        }
    }

    @Test(dataProvider = "scenarios")
    void testHttp2Post(String uri, boolean sameClient) throws Exception {
        out.println(format("\n--- testHttp2Post uri:%s, sameClient:%s", uri, sameClient));
        HttpClient client = null;
        for (int i=0; i<ITERATIONS; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                                   .sslContext(sslContext)
                                   .version(HTTP_2)
                                   .build();

            String msg = BODY[nextBodyId++%4];
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .POST(BodyPublishers.ofString(msg))
                    .build();
            HttpResponse<String> response = client.send(request, ofString());
            out.println("Got response: " + response);
            out.println("Got body: " + response.body());

            assertEquals(response.statusCode(), 200);
            assertEquals(response.version(), HTTP_2);
            assertEquals(response.body(), msg);
            if (uri.startsWith("https"))
                assertTrue(response.sslSession().isPresent());
        }
    }

    /** Checks that an HTTP/1.1 request receives an HTTP/1.1 response, from the HTTP/2 server. */
    @Test(dataProvider = "scenarios")
    void testHttp1dot1Get(String uri, boolean sameClient) throws Exception {
        out.println(format("\n--- testHttp1dot1Get uri:%s, sameClient:%s", uri, sameClient));
        HttpClient client = null;
        for (int i=0; i<ITERATIONS; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                                   .sslContext(sslContext)
                                   .version(HTTP_1_1)
                                   .build();

            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .build();
            HttpResponse<String> response = client.send(request, ofString());
            out.println("Got response: " + response);
            out.println("Got body: " + response.body());
            response.headers().firstValue("X-Received-Body").ifPresent(s -> out.println("X-Received-Body:" + s));

            assertEquals(response.statusCode(), 200);
            assertEquals(response.version(), HTTP_1_1);
            assertEquals(response.body(), "");
            assertEquals(response.headers().firstValue("X-Magic").get(),
                         "HTTP/1.1 request received by HTTP/2 server");
            assertEquals(response.headers().firstValue("X-Received-Body").get(), "");
            if (uri.startsWith("https"))
                assertTrue(response.sslSession().isPresent());
        }
    }

    @Test(dataProvider = "scenarios")
    void testHttp1dot1Post(String uri, boolean sameClient) throws Exception {
        out.println(format("\n--- testHttp1dot1Post uri:%s, sameClient:%s", uri, sameClient));
        HttpClient client = null;
        for (int i=0; i<ITERATIONS; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                                   .sslContext(sslContext)
                                   .version(HTTP_1_1)
                                   .build();
            String msg = BODY[nextBodyId++%4];
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .POST(BodyPublishers.ofString(msg))
                    .build();
            HttpResponse<String> response = client.send(request, ofString());
            out.println("Got response: " + response);
            out.println("Got body: " + response.body());
            response.headers().firstValue("X-Received-Body").ifPresent(s -> out.println("X-Received-Body:" + s));

            assertEquals(response.statusCode(), 200);
            assertEquals(response.version(), HTTP_1_1);
            assertEquals(response.body(), "");
            assertEquals(response.headers().firstValue("X-Magic").get(),
                         "HTTP/1.1 request received by HTTP/2 server");
            assertEquals(response.headers().firstValue("X-Received-Body").get(), msg);
            if (uri.startsWith("https"))
                assertTrue(response.sslSession().isPresent());
        }
    }

    // -- Infrastructure

    static final ExecutorService executor = Executors.newCachedThreadPool();

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        http2TestServer =  new Http2TestServer("localhost", false, 0, executor, 50, null, null, true);
        http2TestServer.addHandler(new Http2VerEchoHandler(), "/http2/vts");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/vts";

        https2TestServer =  new Http2TestServer("localhost", true, 0, executor, 50, null, sslContext, true);
        https2TestServer.addHandler(new Http2VerEchoHandler(), "/https2/vts");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/vts";

        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        http2TestServer.stop();
        https2TestServer.stop();
        executor.shutdown();
    }

    static class Http2VerEchoHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }
}
