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
 * @summary Exercise a streaming subscriber ( InputStream ) without holding a
 *          strong (or any ) reference to the client.
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm
 *       -Djdk.httpclient.HttpClient.log=trace,headers,requests
 *       StreamingBody
 */

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
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static org.testng.Assert.assertEquals;

public class StreamingBody implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;        // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;       // HTTPS/1.1
    HttpTestServer http2TestServer;       // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;      // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    static final String MESSAGE = "StreamingBody message body";
    static final int ITERATIONS = 100;

    @DataProvider(name = "positive")
    public Object[][] positive() {
        return new Object[][] {
                { httpURI,    },
                { httpsURI,   },
                { http2URI,   },
                { https2URI,  },
        };
    }

    @Test(dataProvider = "positive")
    void test(String uriString) throws Exception {
        out.printf("%n---- starting (%s) ----%n", uriString);
        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri).build();

        for (int i=0; i< ITERATIONS; i++) {
            out.println("iteration: " + i);
            HttpResponse<InputStream> response = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .proxy(NO_PROXY)
                    .build()
                    .sendAsync(request, BodyHandlers.ofInputStream())
                    .join();

            String body = new String(response.body().readAllBytes(), UTF_8);
            out.println("Got response: " + response);
            out.println("Got body Path: " + body);

            assertEquals(response.statusCode(), 200);
            assertEquals(body, MESSAGE);
        }
    }


    // -- Infrastructure

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        httpTestServer.addHandler(new MessageHandler(), "/http1/streamingbody/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/streamingbody/w";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new MessageHandler(),"/https1/streamingbody/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/streamingbody/x";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new MessageHandler(), "/http2/streamingbody/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/streamingbody/y";
        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new MessageHandler(), "/https2/streamingbody/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/streamingbody/z";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static class MessageHandler implements HttpTestHandler {
        @Override
        public void handle(HttpTestExchange t) throws IOException {
            System.out.println("MessageHandler for: " + t.getRequestURI());
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = MESSAGE.getBytes(UTF_8);
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }
}
