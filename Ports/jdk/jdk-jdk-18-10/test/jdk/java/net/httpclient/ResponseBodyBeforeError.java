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
 * @summary Tests that all response body is delivered to the BodySubscriber
 *          before an abortive error terminates the flow
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm ResponseBodyBeforeError
 */

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ResponseBodyBeforeError {

    ReplyingServer variableLengthServer;
    ReplyingServer variableLengthHttpsServer;
    ReplyingServer fixedLengthServer;
    ReplyingServer fixedLengthHttpsServer;

    String httpURIVarLen;
    String httpsURIVarLen;
    String httpURIFixLen;
    String httpsURIFixLen;

    SSLContext sslContext;

    static final String EXPECTED_RESPONSE_BODY =
            "<html><body><h1>Heading</h1><p>Some Text</p></body></html>";

    @DataProvider(name = "sanity")
    public Object[][] sanity() {
        return new Object[][]{
                { httpURIVarLen   + "?length=all" },
                { httpsURIVarLen  + "?length=all" },
                { httpURIFixLen   + "?length=all" },
                { httpsURIFixLen  + "?length=all" },
        };
    }

    @Test(dataProvider = "sanity")
    void sanity(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .proxy(NO_PROXY)
                .sslContext(sslContext)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, ofString());
        String body = response.body();
        assertEquals(body, EXPECTED_RESPONSE_BODY);
        client.sendAsync(request, ofString())
                .thenApply(resp -> resp.body())
                .thenAccept(b -> assertEquals(b, EXPECTED_RESPONSE_BODY))
                .join();
    }

    @DataProvider(name = "uris")
    public Object[][] variants() {
        Object[][] cases = new Object[][] {
            // The length query string is the total number of response body
            // bytes in the reply, before the server closes the connection. The
            // second arg is a partial-expected-body that the body subscriber
            // should receive before onError is invoked.

            { httpURIFixLen + "?length=0",   ""               },
            { httpURIFixLen + "?length=1",   "<"              },
            { httpURIFixLen + "?length=2",   "<h"             },
            { httpURIFixLen + "?length=10",  "<html><bod"     },
            { httpURIFixLen + "?length=19",  "<html><body><h1>Hea"             },
            { httpURIFixLen + "?length=31",  "<html><body><h1>Heading</h1><p>" },

            { httpsURIFixLen + "?length=0",   ""              },
            { httpsURIFixLen + "?length=1",   "<"             },
            { httpsURIFixLen + "?length=2",   "<h"            },
            { httpsURIFixLen + "?length=10",  "<html><bod"    },
            { httpsURIFixLen + "?length=19",  "<html><body><h1>Hea"             },
            { httpsURIFixLen + "?length=31",  "<html><body><h1>Heading</h1><p>" },

            // accounts for chunk framing
            { httpURIVarLen + "?length=0",   ""               },
            { httpURIVarLen + "?length=1",   ""               },
            { httpURIVarLen + "?length=2",   ""               },
            { httpURIVarLen + "?length=4",   "<"              },
            { httpURIVarLen + "?length=5",   "<h"             },
            { httpURIVarLen + "?length=18",  "<html><bod"     },
            { httpURIVarLen + "?length=20",  "<html><body>"   },
            { httpURIVarLen + "?length=21",  "<html><body>"   }, // boundary around chunk framing
            { httpURIVarLen + "?length=22",  "<html><body>"   },
            { httpURIVarLen + "?length=23",  "<html><body>"   },
            { httpURIVarLen + "?length=24",  "<html><body>"   },
            { httpURIVarLen + "?length=25",  "<html><body>"   },
            { httpURIVarLen + "?length=26",  "<html><body>"   },
            { httpURIVarLen + "?length=27",  "<html><body><"  },
            { httpURIVarLen + "?length=51",  "<html><body><h1>Heading</h1><p>" },

            { httpsURIVarLen + "?length=0",   ""              },
            { httpsURIVarLen + "?length=1",   ""              },
            { httpsURIVarLen + "?length=2",   ""              },
            { httpsURIVarLen + "?length=4",   "<"             },
            { httpsURIVarLen + "?length=5",   "<h"            },
            { httpsURIVarLen + "?length=18",  "<html><bod"    },
            { httpsURIVarLen + "?length=20",  "<html><body>"  },
            { httpsURIVarLen + "?length=21",  "<html><body>"  },
            { httpsURIVarLen + "?length=22",  "<html><body>"  },
            { httpsURIVarLen + "?length=23",  "<html><body>"  },
            { httpsURIVarLen + "?length=24",  "<html><body>"  },
            { httpsURIVarLen + "?length=25",  "<html><body>"  },
            { httpsURIVarLen + "?length=26",  "<html><body>"  },
            { httpsURIVarLen + "?length=27",  "<html><body><" },
            { httpsURIVarLen + "?length=51",  "<html><body><h1>Heading</h1><p>" },
        };

        List<Object[]> list = new ArrayList<>();
        Arrays.asList(cases).stream()
                .map(e -> new Object[] {e[0], e[1], true})  // reuse client
                .forEach(list::add);
        Arrays.asList(cases).stream()
                .map(e -> new Object[] {e[0], e[1], false}) // do not reuse client
                .forEach(list::add);
        return list.stream().toArray(Object[][]::new);
    }

    static final int ITERATION_COUNT = 3;

    @Test(dataProvider = "uris")
    void testSynchronousAllRequestBody(String url,
                                       String expectedPatrialBody,
                                       boolean sameClient)
        throws Exception
    {
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                        .proxy(NO_PROXY)
                        .sslContext(sslContext)
                        .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            CustomBodySubscriber bs = new CustomBodySubscriber();
            try {
                HttpResponse<String> response = client.send(request, r -> bs);
                String body = response.body();
                out.println(response + ": " + body);
                fail("UNEXPECTED RESPONSE: " + response);
            } catch (IOException expected) {
                String pm = bs.receivedAsString();
                out.println("partial body received: " + pm);
                assertEquals(pm, expectedPatrialBody);
            }
        }
    }

    @Test(dataProvider = "uris")
    void testAsynchronousAllRequestBody(String url,
                                        String expectedPatrialBody,
                                        boolean sameClient)
        throws Exception
    {
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder()
                        .proxy(NO_PROXY)
                        .sslContext(sslContext)
                        .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            CustomBodySubscriber bs = new CustomBodySubscriber();
            try {
                HttpResponse<String> response = client.sendAsync(request, r -> bs).get();
                String body = response.body();
                out.println(response + ": " + body);
                fail("UNEXPECTED RESPONSE: " + response);
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof IOException) {
                    String pm = bs.receivedAsString();
                    out.println("partial body received: " + pm);
                    assertEquals(pm, expectedPatrialBody);
                } else {
                    throw ee;
                }
            }
        }
    }

    static final class CustomBodySubscriber implements BodySubscriber<String> {

        Flow.Subscription subscription;
        private final List<ByteBuffer> received = new ArrayList<>();
        private final CompletableFuture<String> cf = new CompletableFuture<>();

        @Override
        public CompletionStage<String> getBody() {
            return cf;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            out.println("CustomBodySubscriber got onSubscribe: ");
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            out.println("CustomBodySubscriber got onNext: " + items);
            received.addAll(items);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable expected) {
            out.println("CustomBodySubscriber got expected: " + expected);
            cf.completeExceptionally(expected);
        }

        String receivedAsString() {
            int size = received.stream().mapToInt(ByteBuffer::remaining).sum();
            byte[] res = new byte[size];
            int from = 0;
            for (ByteBuffer b : received) {
                int l = b.remaining();
                b.get(res, from, l);
                from += l;
            }
            return new String(res, UTF_8);
        }

        @Override
        public void onComplete() {
            out.println("CustomBodySubscriber got complete: ");
            assert false : "Unexpected onComplete";
        }
    }

    // -- infra

    /**
     * A server that replies with headers and a, possibly partial, reply, before
     * closing the connection. The number of body bytes of written, is
     * controllable through the "length" query string param in the requested
     * URI.
     */
    static abstract class ReplyingServer extends Thread implements Closeable {

        private final String name;
        private final ServerSocket ss;
        private volatile boolean closed;

        private ReplyingServer(String name) throws IOException {
            super(name);
            this.name = name;
            ss = newServerSocket();
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            this.start();
        }

        protected ServerSocket newServerSocket() throws IOException {
            return new ServerSocket();
        }

        abstract String responseHeaders();

        abstract String responseBody();

        @Override
        public void run() {
            while (!closed) {
                try (Socket s = ss.accept()) {
                    out.print(name + ": got connection ");
                    InputStream is = s.getInputStream();
                    URI requestMethod = readRequestMethod(is);
                    out.print(requestMethod + " ");
                    URI uriPath = readRequestPath(is);
                    out.println(uriPath);
                    readRequestHeaders(is);

                    String query = uriPath.getRawQuery();
                    assert query != null;
                    String qv = query.split("=")[1];
                    int len;
                    if (qv.equals("all")) {
                        len = responseBody().getBytes(US_ASCII).length;
                    } else {
                        len = Integer.parseInt(query.split("=")[1]);
                    }

                    OutputStream os = s.getOutputStream();
                    os.write(responseHeaders().getBytes(US_ASCII));
                    out.println(name  + ": headers written, writing " + len  + " body bytes");
                    byte[] responseBytes = responseBody().getBytes(US_ASCII);
                    for (int i = 0; i< len; i++) {
                        os.write(responseBytes[i]);
                        os.flush();
                    }
                } catch (IOException e) {
                    if (!closed)
                        throw new UncheckedIOException("Unexpected", e);
                }
            }
        }

        static final byte[] requestEnd = new byte[] { '\r', '\n', '\r', '\n' };

        // Read the request method
        static URI readRequestMethod(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            int r;
            while ((r = is.read()) != -1 && r != 0x20) {
                sb.append((char)r);
            }
            return URI.create(sb.toString());
        }

        // Read the request URI path
        static URI readRequestPath(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            int r;
            while ((r = is.read()) != -1 && r != 0x20) {
                sb.append((char)r);
            }
            return URI.create(sb.toString());
        }

        // Read until the end of a HTTP request headers
        static void readRequestHeaders(InputStream is) throws IOException {
            int requestEndCount = 0, r;
            while ((r = is.read()) != -1) {
                if (r == requestEnd[requestEndCount]) {
                    requestEndCount++;
                    if (requestEndCount == 4) {
                        break;
                    }
                } else {
                    requestEndCount = 0;
                }
            }
        }

        public int getPort() { return ss.getLocalPort(); }

        @Override
        public void close() {
            if (closed)
                return;
            closed = true;
            try {
                ss.close();
            } catch (IOException e) {
                throw new UncheckedIOException("Unexpected", e);
            }
        }
    }

    /** A server that issues a possibly-partial chunked reply. */
    static class PlainVariableLengthServer extends ReplyingServer {

        static final String CHUNKED_RESPONSE_BODY =
                "6\r\n"+ "<html>\r\n" +
                "6\r\n"+ "<body>\r\n" +
                "10\r\n"+ "<h1>Heading</h1>\r\n" +
                "10\r\n"+ "<p>Some Text</p>\r\n" +
                "7\r\n"+ "</body>\r\n" +
                "7\r\n"+ "</html>\r\n" +
                "0\r\n"+ "\r\n";

        static final String RESPONSE_HEADERS =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "Connection: close\r\n\r\n";


        PlainVariableLengthServer() throws IOException {
            super("PlainVariableLengthServer");
        }

        protected PlainVariableLengthServer(String name) throws IOException {
            super(name);
        }

        @Override
        String responseHeaders( ) { return RESPONSE_HEADERS; }

        @Override
        String responseBody( ) { return CHUNKED_RESPONSE_BODY; }
    }

    /** A server that issues a, possibly-partial, chunked reply over SSL */
    static final class SSLVariableLengthServer extends PlainVariableLengthServer {
        SSLVariableLengthServer() throws IOException {
            super("SSLVariableLengthServer");
        }
        @Override
        public ServerSocket newServerSocket() throws IOException {
            return SSLServerSocketFactory.getDefault().createServerSocket();
        }
    }

    /** A server that issues a, possibly-partial, fixed-length reply. */
    static class PlainFixedLengthServer extends ReplyingServer {

        static final String RESPONSE_BODY = EXPECTED_RESPONSE_BODY;

        static final String RESPONSE_HEADERS =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + RESPONSE_BODY.length() + "\r\n" +
                "Connection: close\r\n\r\n";

        PlainFixedLengthServer() throws IOException {
            super("PlainFixedLengthServer");
        }

        protected PlainFixedLengthServer(String name) throws IOException {
            super(name);
        }

        @Override
        String responseHeaders( ) { return RESPONSE_HEADERS; }

        @Override
        String responseBody( ) { return RESPONSE_BODY; }
    }

    /** A server that issues a,  possibly-partial, fixed-length reply over SSL */
    static final class SSLFixedLengthServer extends PlainFixedLengthServer {
        SSLFixedLengthServer() throws IOException {
            super("SSLFixedLengthServer");
        }
        @Override
        public ServerSocket newServerSocket() throws IOException {
            return SSLServerSocketFactory.getDefault().createServerSocket();
        }
    }

    static String serverAuthority(ReplyingServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");
        SSLContext.setDefault(sslContext);

        variableLengthServer = new PlainVariableLengthServer();
        httpURIVarLen = "http://" + serverAuthority(variableLengthServer)
                + "/http1/variable/foo";

        variableLengthHttpsServer = new SSLVariableLengthServer();
        httpsURIVarLen = "https://" + serverAuthority(variableLengthHttpsServer)
                + "/https1/variable/bar";

        fixedLengthServer = new PlainFixedLengthServer();
        httpURIFixLen = "http://" + serverAuthority(fixedLengthServer)
                + "/http1/fixed/baz";

        fixedLengthHttpsServer = new SSLFixedLengthServer();
        httpsURIFixLen = "https://" + serverAuthority(fixedLengthHttpsServer)
                + "/https1/fixed/foz";
    }

    @AfterTest
    public void teardown() throws Exception {
        variableLengthServer.close();
        variableLengthHttpsServer.close();
        fixedLengthServer.close();
        fixedLengthHttpsServer.close();
    }
}
