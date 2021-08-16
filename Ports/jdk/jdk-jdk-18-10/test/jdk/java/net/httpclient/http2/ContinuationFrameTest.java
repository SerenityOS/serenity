/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test for CONTINUATION frame handling
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @library /test/lib server
 * @compile ../ReferenceTracker.java
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm ContinuationFrameTest
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import jdk.internal.net.http.common.HttpHeadersBuilder;
import jdk.internal.net.http.frame.ContinuationFrame;
import jdk.internal.net.http.frame.HeaderFrame;
import jdk.internal.net.http.frame.HeadersFrame;
import jdk.internal.net.http.frame.Http2Frame;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.net.http.HttpClient.Version.HTTP_2;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ContinuationFrameTest {

    SSLContext sslContext;
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
    String http2URI;
    String https2URI;
    String noBodyhttp2URI;
    String noBodyhttps2URI;
    final ReferenceTracker TRACKER = ReferenceTracker.INSTANCE;

    /**
     * A function that returns a list of 1) a HEADERS frame ( with an empty
     * payload ), and 2) a CONTINUATION frame with the actual headers.
     */
    static BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> oneContinuation =
        (Integer streamid, List<ByteBuffer> encodedHeaders) -> {
            List<ByteBuffer> empty =  List.of(ByteBuffer.wrap(new byte[0]));
            HeadersFrame hf = new HeadersFrame(streamid, 0, empty);
            ContinuationFrame cf = new ContinuationFrame(streamid,
                                                         HeaderFrame.END_HEADERS,
                                                         encodedHeaders);
            return List.of(hf, cf);
        };

    /**
     * A function that returns a list of 1) a HEADERS frame with END_STREAM
     * ( and with an empty payload ), and 2) two CONTINUATION frames,the first
     * is empty and the second contains headers and the END_HEADERS flag
     */
    static BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> twoContinuation =
        (Integer streamid, List<ByteBuffer> encodedHeaders) -> {
            List<ByteBuffer> empty =  List.of(ByteBuffer.wrap(new byte[0]));
            HeadersFrame hf = new HeadersFrame(streamid, HeaderFrame.END_STREAM, empty);
            ContinuationFrame cf = new ContinuationFrame(streamid, 0,empty);
            ContinuationFrame cf1 = new ContinuationFrame(streamid,
                                                         HeaderFrame.END_HEADERS,
                                                         encodedHeaders);

                return List.of(hf, cf, cf1);
            };

    /**
     * A function that returns a list of a HEADERS frame followed by a number of
     * CONTINUATION frames. Each frame contains just a single byte of payload.
     */
    static BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> byteAtATime =
        (Integer streamid, List<ByteBuffer> encodedHeaders) -> {
            assert encodedHeaders.get(0).hasRemaining();
            List<Http2Frame> frames = new ArrayList<>();
            ByteBuffer hb = ByteBuffer.wrap(new byte[] {encodedHeaders.get(0).get()});
            HeadersFrame hf = new HeadersFrame(streamid, 0, hb);
            frames.add(hf);
            for (ByteBuffer bb : encodedHeaders) {
                while (bb.hasRemaining()) {
                    List<ByteBuffer> data = List.of(ByteBuffer.wrap(new byte[] {bb.get()}));
                    ContinuationFrame cf = new ContinuationFrame(streamid, 0, data);
                    frames.add(cf);
                }
            }
            frames.get(frames.size() - 1).setFlag(HeaderFrame.END_HEADERS);
            return frames;
        };

    @DataProvider(name = "variants")
    public Object[][] variants() {
        return new Object[][] {
                { http2URI,        false, oneContinuation },
                { https2URI,       false, oneContinuation },
                { http2URI,        true,  oneContinuation },
                { https2URI,       true,  oneContinuation },

                { noBodyhttp2URI,  false, twoContinuation },
                { noBodyhttp2URI,  true,  twoContinuation },
                { noBodyhttps2URI, false, twoContinuation },
                { noBodyhttps2URI, true,  twoContinuation },

                { http2URI,        false, byteAtATime },
                { https2URI,       false, byteAtATime },
                { http2URI,        true,  byteAtATime },
                { https2URI,       true,  byteAtATime },
        };
    }

    static final int ITERATION_COUNT = 20;

    @Test(dataProvider = "variants")
    void test(String uri,
              boolean sameClient,
              BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> headerFramesSupplier)
        throws Exception
    {
        CFTHttp2TestExchange.setHeaderFrameSupplier(headerFramesSupplier);

        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null) {
                client = HttpClient.newBuilder()
                         .proxy(HttpClient.Builder.NO_PROXY)
                         .sslContext(sslContext)
                         .build();
                TRACKER.track(client);
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                                             .POST(BodyPublishers.ofString("Hello there!"))
                                             .build();
            HttpResponse<String> resp;
            if (i % 2 == 0) {
                resp = client.send(request, BodyHandlers.ofString());
            } else {
                resp = client.sendAsync(request, BodyHandlers.ofString()).join();
            }

            if(uri.contains("nobody")) {
                out.println("Got response: " + resp);
                assertTrue(resp.statusCode() == 204,
                    "Expected 204, got:" + resp.statusCode());
                assertEquals(resp.version(), HTTP_2);
                continue;
            }
            out.println("Got response: " + resp);
            out.println("Got body: " + resp.body());
            assertTrue(resp.statusCode() == 200,
                       "Expected 200, got:" + resp.statusCode());
            assertEquals(resp.body(), "Hello there!");
            assertEquals(resp.version(), HTTP_2);
        }
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        http2TestServer = new Http2TestServer("localhost", false, 0);
        http2TestServer.addHandler(new Http2EchoHandler(), "/http2/echo");
        http2TestServer.addHandler(new Http2NoBodyHandler(), "/http2/nobody");
        int port = http2TestServer.getAddress().getPort();
        http2URI = "http://localhost:" + port + "/http2/echo";
        noBodyhttp2URI = "http://localhost:" + port + "/http2/nobody";

        https2TestServer = new Http2TestServer("localhost", true, sslContext);
        https2TestServer.addHandler(new Http2EchoHandler(), "/https2/echo");
        https2TestServer.addHandler(new Http2NoBodyHandler(), "/https2/nobody");
        port = https2TestServer.getAddress().getPort();
        https2URI = "https://localhost:" + port + "/https2/echo";
        noBodyhttps2URI = "https://localhost:" + port + "/https2/nobody";

        // Override the default exchange supplier with a custom one to enable
        // particular test scenarios
        http2TestServer.setExchangeSupplier(CFTHttp2TestExchange::new);
        https2TestServer.setExchangeSupplier(CFTHttp2TestExchange::new);

        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        AssertionError fail = TRACKER.check(500);
        try {
            http2TestServer.stop();
            https2TestServer.stop();
        } finally {
            if (fail != null) {
                throw fail;
            }
        }
    }

    static class Http2EchoHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                t.getResponseHeaders().addHeader("justSome", "Noise");
                t.getResponseHeaders().addHeader("toAdd", "payload in");
                t.getResponseHeaders().addHeader("theHeader", "Frames");
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    static class Http2NoBodyHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                t.sendResponseHeaders(204, -1);
            }
        }
    }

    // A custom Http2TestExchangeImpl that overrides sendResponseHeaders to
    // allow headers to be sent with a number of CONTINUATION frames.
    static class CFTHttp2TestExchange extends Http2TestExchangeImpl {
        static volatile BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> headerFrameSupplier;

        static void setHeaderFrameSupplier(BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> hfs) {
            headerFrameSupplier = hfs;
        }

        CFTHttp2TestExchange(int streamid, String method, HttpHeaders reqheaders,
                             HttpHeadersBuilder rspheadersBuilder, URI uri, InputStream is,
                             SSLSession sslSession, BodyOutputStream os,
                             Http2TestServerConnection conn, boolean pushAllowed) {
            super(streamid, method, reqheaders, rspheadersBuilder, uri, is, sslSession,
                  os, conn, pushAllowed);

        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
            this.responseLength = responseLength;
            if (responseLength != 0 && rCode != 204) {
                long clen = responseLength > 0 ? responseLength : 0;
                rspheadersBuilder.setHeader("Content-length", Long.toString(clen));
            }
            rspheadersBuilder.setHeader(":status", Integer.toString(rCode));
            HttpHeaders headers = rspheadersBuilder.build();

            List<ByteBuffer> encodeHeaders = conn.encodeHeaders(headers);
            List<Http2Frame> headerFrames = headerFrameSupplier.apply(streamid, encodeHeaders);
            assert headerFrames.size() > 0;  // there must always be at least 1

            if(headerFrames.get(0).getFlag(HeaderFrame.END_STREAM))
                os.closeInternal();

            for (Http2Frame f : headerFrames)
                conn.outputQ.put(f);

            os.goodToGo();
            System.err.println("Sent response headers " + rCode);
        }
    }
}
