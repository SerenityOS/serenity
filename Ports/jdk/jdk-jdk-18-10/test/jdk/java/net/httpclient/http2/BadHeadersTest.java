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
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @library /test/lib server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm -Djdk.internal.httpclient.debug=true BadHeadersTest
 */

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
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import static java.util.List.of;
import static java.util.Map.entry;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

// Code copied from ContinuationFrameTest
public class BadHeadersTest {

    private static final List<List<Entry<String, String>>> BAD_HEADERS = of(
        of(entry(":status", "200"),  entry(":hello", "GET")),                      // Unknown pseudo-header
        of(entry(":status", "200"),  entry("hell o", "value")),                    // Space in the name
        of(entry(":status", "200"),  entry("hello", "line1\r\n  line2\r\n")),      // Multiline value
        of(entry(":status", "200"),  entry("hello", "DE" + ((char) 0x7F) + "L")),  // Bad byte in value
        of(entry("hello", "world!"), entry(":status", "200"))                      // Pseudo header is not the first one
    );

    SSLContext sslContext;
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
    String http2URI;
    String https2URI;

    /**
     * A function that returns a list of 1) a HEADERS frame ( with an empty
     * payload ), and 2) a CONTINUATION frame with the actual headers.
     */
    static BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> oneContinuation =
            (Integer streamid, List<ByteBuffer> encodedHeaders) -> {
                List<ByteBuffer> empty =  of(ByteBuffer.wrap(new byte[0]));
                HeadersFrame hf = new HeadersFrame(streamid, 0, empty);
                ContinuationFrame cf = new ContinuationFrame(streamid,
                                                             HeaderFrame.END_HEADERS,
                                                             encodedHeaders);
                return of(hf, cf);
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
                        List<ByteBuffer> data = of(ByteBuffer.wrap(new byte[] {bb.get()}));
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
                { http2URI,  false, oneContinuation },
                { https2URI, false, oneContinuation },
                { http2URI,  true,  oneContinuation },
                { https2URI, true,  oneContinuation },

                { http2URI,  false, byteAtATime },
                { https2URI, false, byteAtATime },
                { http2URI,  true,  byteAtATime },
                { https2URI, true,  byteAtATime },
        };
    }


    @Test(dataProvider = "variants")
    void test(String uri,
              boolean sameClient,
              BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> headerFramesSupplier)
        throws Exception
    {
        CFTHttp2TestExchange.setHeaderFrameSupplier(headerFramesSupplier);

        HttpClient client = null;
        for (int i=0; i< BAD_HEADERS.size(); i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder().sslContext(sslContext).build();

            URI uriWithQuery = URI.create(uri +  "?BAD_HEADERS=" + i);
            HttpRequest request = HttpRequest.newBuilder(uriWithQuery)
                    .POST(BodyPublishers.ofString("Hello there!"))
                    .build();
            System.out.println("\nSending request:" + uriWithQuery);
            final HttpClient cc = client;
            try {
                HttpResponse<String> response = cc.send(request, BodyHandlers.ofString());
                fail("Expected exception, got :" + response + ", " + response.body());
            } catch (IOException ioe) {
                System.out.println("Got EXPECTED: " + ioe);
                assertDetailMessage(ioe, i);
            }
        }
    }

    @Test(dataProvider = "variants")
    void testAsync(String uri,
                   boolean sameClient,
                   BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> headerFramesSupplier)
    {
        CFTHttp2TestExchange.setHeaderFrameSupplier(headerFramesSupplier);

        HttpClient client = null;
        for (int i=0; i< BAD_HEADERS.size(); i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder().sslContext(sslContext).build();

            URI uriWithQuery = URI.create(uri +  "?BAD_HEADERS=" + i);
            HttpRequest request = HttpRequest.newBuilder(uriWithQuery)
                    .POST(BodyPublishers.ofString("Hello there!"))
                    .build();
            System.out.println("\nSending request:" + uriWithQuery);
            final HttpClient cc = client;

            Throwable t = null;
            try {
                HttpResponse<String> response = cc.sendAsync(request, BodyHandlers.ofString()).get();
                fail("Expected exception, got :" + response + ", " + response.body());
            } catch (Throwable t0) {
                System.out.println("Got EXPECTED: " + t0);
                if (t0 instanceof ExecutionException) {
                    t0 = t0.getCause();
                }
                t = t0;
            }
            assertDetailMessage(t, i);
        }
    }

    // Assertions based on implementation specific detail messages. Keep in
    // sync with implementation.
    static void assertDetailMessage(Throwable throwable, int iterationIndex) {
        assertTrue(throwable instanceof IOException,
                   "Expected IOException, got, " + throwable);
        assertTrue(throwable.getMessage().contains("protocol error"),
                "Expected \"protocol error\" in: " + throwable.getMessage());

        if (iterationIndex == 0) { // unknown
            assertTrue(throwable.getMessage().contains("Unknown pseudo-header"),
                    "Expected \"Unknown pseudo-header\" in: " + throwable.getMessage());
        } else if (iterationIndex == 4) { // unexpected
            assertTrue(throwable.getMessage().contains(" Unexpected pseudo-header"),
                    "Expected \" Unexpected pseudo-header\" in: " + throwable.getMessage());
        } else {
            assertTrue(throwable.getMessage().contains("Bad header"),
                    "Expected \"Bad header\" in: " + throwable.getMessage());
        }
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        http2TestServer = new Http2TestServer("localhost", false, 0);
        http2TestServer.addHandler(new Http2EchoHandler(), "/http2/echo");
        int port = http2TestServer.getAddress().getPort();
        http2URI = "http://localhost:" + port + "/http2/echo";

        https2TestServer = new Http2TestServer("localhost", true, sslContext);
        https2TestServer.addHandler(new Http2EchoHandler(), "/https2/echo");
        port = https2TestServer.getAddress().getPort();
        https2URI = "https://localhost:" + port + "/https2/echo";

        // Override the default exchange supplier with a custom one to enable
        // particular test scenarios
        http2TestServer.setExchangeSupplier(CFTHttp2TestExchange::new);
        https2TestServer.setExchangeSupplier(CFTHttp2TestExchange::new);

        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        http2TestServer.stop();
        https2TestServer.stop();
    }

    static class Http2EchoHandler implements Http2Handler {

        @Override
        public void handle(Http2TestExchange t) throws IOException {
            try (InputStream is = t.getRequestBody();
                 OutputStream os = t.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                // Note: strictly ordered response headers will be added within
                // the custom sendResponseHeaders implementation, based upon the
                // query parameter
                t.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    // A custom Http2TestExchangeImpl that overrides sendResponseHeaders to
    // allow headers to be sent with a number of CONTINUATION frames.
    static class CFTHttp2TestExchange extends Http2TestExchangeImpl {
        static volatile BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> headerFrameSupplier;
        volatile int badHeadersIndex = -1;

        static void setHeaderFrameSupplier(BiFunction<Integer,List<ByteBuffer>,List<Http2Frame>> hfs) {
            headerFrameSupplier = hfs;
        }

        CFTHttp2TestExchange(int streamid, String method, HttpHeaders reqheaders,
                             HttpHeadersBuilder rspheadersBuilder, URI uri, InputStream is,
                             SSLSession sslSession, BodyOutputStream os,
                             Http2TestServerConnection conn, boolean pushAllowed) {
            super(streamid, method, reqheaders, rspheadersBuilder, uri, is, sslSession,
                  os, conn, pushAllowed);
            String query = uri.getQuery();
            badHeadersIndex = Integer.parseInt(query.substring(query.indexOf("=") + 1));
            assert badHeadersIndex >= 0 && badHeadersIndex < BAD_HEADERS.size() :
                    "Unexpected badHeadersIndex value: " + badHeadersIndex;
        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
            assert rspheadersBuilder.build().map().size() == 0;
            assert badHeadersIndex >= 0 && badHeadersIndex < BAD_HEADERS.size() :
                    "Unexpected badHeadersIndex value: " + badHeadersIndex;

            List<Entry<String,String>> headers = BAD_HEADERS.get(badHeadersIndex);
            System.out.println("Server replying with bad headers: " + headers);
            List<ByteBuffer> encodeHeaders = conn.encodeHeadersOrdered(headers);

            List<Http2Frame> headerFrames = headerFrameSupplier.apply(streamid, encodeHeaders);
            assert headerFrames.size() > 0;  // there must always be at least 1

            if (responseLength < 0) {
                headerFrames.get(headerFrames.size() -1).setFlag(HeadersFrame.END_STREAM);
                os.closeInternal();
            }

            for (Http2Frame f : headerFrames)
                conn.outputQ.put(f);

            os.goodToGo();
            System.err.println("Sent response headers " + rCode);
        }
    }
}
