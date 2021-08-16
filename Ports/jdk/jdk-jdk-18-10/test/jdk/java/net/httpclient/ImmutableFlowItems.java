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
 * @summary Tests response body subscribers's onNext's Lists are unmodifiable,
 *          and that the buffers are read-only
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm ImmutableFlowItems
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

public class ImmutableFlowItems {

    SSLContext sslContext;
    HttpServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpsServer httpsTestServer;       // HTTPS/1.1
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI_fixed;
    String httpURI_chunk;
    String httpsURI_fixed;
    String httpsURI_chunk;
    String http2URI_fixed;
    String http2URI_chunk;
    String https2URI_fixed;
    String https2URI_chunk;

    @DataProvider(name = "variants")
    public Object[][] variants() {
        return new Object[][]{
                { httpURI_fixed   },
                { httpURI_chunk   },
                { httpsURI_fixed  },
                { httpsURI_chunk  },
                { http2URI_fixed  },
                { http2URI_chunk  },
                { https2URI_fixed },
                { https2URI_chunk },
        };
    }

    static final String BODY = "You'll never plough a field by turning it over in your mind.";

    HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
    }

    @Test(dataProvider = "variants")
    public void testAsString(String uri) throws Exception {
        HttpClient client = newHttpClient();

        HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                .build();

        BodyHandler<String> handler = new CRSBodyHandler();
        client.sendAsync(req, handler)
                .thenApply(HttpResponse::body)
                .thenAccept(body -> assertEquals(body, BODY))
                .join();
    }

    static class CRSBodyHandler implements BodyHandler<String> {
        @Override
        public BodySubscriber<String> apply(HttpResponse.ResponseInfo rinfo) {
            assertEquals(rinfo.statusCode(), 200);
            return new CRSBodySubscriber();
        }
    }

    static class CRSBodySubscriber implements BodySubscriber<String> {
        private final BodySubscriber<String> ofString = BodySubscribers.ofString(UTF_8);

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            ofString.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            assertUnmodifiableList(item);
            long c = item.stream().filter(ByteBuffer::isReadOnly).count();
            assertEquals(c, item.size(), "Unexpected writable buffer in: " +item);
            ofString.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            ofString.onError(throwable);
        }

        @Override
        public void onComplete() {
            ofString.onComplete();
        }

        @Override
        public CompletionStage<String> getBody() {
            return ofString.getBody();
        }
    }

    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    static void assertUnmodifiableList(List<ByteBuffer> list) {
        assertNotNull(list);
        ByteBuffer b = list.get(0);
        assertNotNull(b);
        assertThrows(UOE, () -> list.add(ByteBuffer.wrap(new byte[0])));
        assertThrows(UOE, () -> list.remove(b));
    }

    static String serverAuthority(HttpServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getAddress().getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        // HTTP/1.1
        HttpHandler h1_fixedLengthHandler = new HTTP1_FixedLengthHandler();
        HttpHandler h1_chunkHandler = new HTTP1_ChunkedHandler();
        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpServer.create(sa, 0);
        httpTestServer.createContext("/http1/fixed", h1_fixedLengthHandler);
        httpTestServer.createContext("/http1/chunk", h1_chunkHandler);
        httpURI_fixed = "http://" + serverAuthority(httpTestServer) + "/http1/fixed";
        httpURI_chunk = "http://" + serverAuthority(httpTestServer) + "/http1/chunk";

        httpsTestServer = HttpsServer.create(sa, 0);
        httpsTestServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer.createContext("/https1/fixed", h1_fixedLengthHandler);
        httpsTestServer.createContext("/https1/chunk", h1_chunkHandler);
        httpsURI_fixed = "https://" + serverAuthority(httpsTestServer) + "/https1/fixed";
        httpsURI_chunk = "https://" + serverAuthority(httpsTestServer) + "/https1/chunk";

        // HTTP/2
        Http2Handler h2_fixedLengthHandler = new HTTP2_FixedLengthHandler();
        Http2Handler h2_chunkedHandler = new HTTP2_VariableHandler();

        http2TestServer = new Http2TestServer("localhost", false, 0);
        http2TestServer.addHandler(h2_fixedLengthHandler, "/http2/fixed");
        http2TestServer.addHandler(h2_chunkedHandler, "/http2/chunk");
        http2URI_fixed = "http://" + http2TestServer.serverAuthority() + "/http2/fixed";
        http2URI_chunk = "http://" + http2TestServer.serverAuthority() + "/http2/chunk";

        https2TestServer = new Http2TestServer("localhost", true, sslContext);
        https2TestServer.addHandler(h2_fixedLengthHandler, "/https2/fixed");
        https2TestServer.addHandler(h2_chunkedHandler, "/https2/chunk");
        https2URI_fixed = "https://" + https2TestServer.serverAuthority() + "/https2/fixed";
        https2URI_chunk = "https://" + https2TestServer.serverAuthority() + "/https2/chunk";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop(0);
        httpsTestServer.stop(0);
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static class HTTP1_FixedLengthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            out.println("HTTP1_FixedLengthHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = BODY.getBytes(UTF_8);
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    static class HTTP1_ChunkedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            out.println("HTTP1_ChunkedHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = BODY.getBytes(UTF_8);
                t.sendResponseHeaders(200, 0);
                os.write(bytes);
            }
        }
    }

    static class HTTP2_FixedLengthHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            out.println("HTTP2_FixedLengthHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = BODY.getBytes(UTF_8);
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    static class HTTP2_VariableHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            out.println("HTTP2_VariableHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                is.readAllBytes();
                byte[] bytes = BODY.getBytes(UTF_8);
                t.sendResponseHeaders(200, 0); // variable
                os.write(bytes);
            }
        }
    }
}
