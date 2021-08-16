/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8216498
 * @summary Tests Exception detail message when too few response bytes are
 *          received before a socket exception or eof.
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm
 *       -Djdk.httpclient.HttpClient.log=headers,errors,channel
 *       ShortResponseBody
 */

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
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ShortResponseBody {

    Server closeImmediatelyServer;
    Server closeImmediatelyHttpsServer;
    Server variableLengthServer;
    Server variableLengthHttpsServer;
    Server fixedLengthServer;

    String httpURIClsImed;
    String httpsURIClsImed;
    String httpURIVarLen;
    String httpsURIVarLen;
    String httpURIFixLen;

    SSLContext sslContext;
    SSLParameters sslParameters;

    static final String EXPECTED_RESPONSE_BODY =
            "<html><body><h1>Heading</h1><p>Some Text</p></body></html>";

    final static AtomicLong ids = new AtomicLong();
    final ThreadFactory factory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r,  "HttpClient-Worker-" + ids.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    };
    final ExecutorService service = Executors.newCachedThreadPool(factory);

    final AtomicReference<SkipException> skiptests = new AtomicReference<>();
    void checkSkip() {
        var skip = skiptests.get();
        if (skip != null) throw skip;
    }
    static String name(ITestResult result) {
        var params = result.getParameters();
        return result.getName()
                + (params == null ? "()" : Arrays.toString(result.getParameters()));
    }

    @BeforeMethod
    void beforeMethod(ITestContext context) {
        if (context.getFailedTests().size() > 0) {
            if (skiptests.get() == null) {
                SkipException skip = new SkipException("some tests failed");
                skip.setStackTrace(new StackTraceElement[0]);
                skiptests.compareAndSet(null, skip);
            }
        }
    }

    @AfterClass
    static final void printFailedTests(ITestContext context) {
        out.println("\n=========================\n");
        try {
            var FAILURES = context.getFailedTests().getAllResults().stream()
                    .collect(Collectors.toMap(r -> name(r), ITestResult::getThrowable));

            if (FAILURES.isEmpty()) return;
            out.println("Failed tests: ");
            FAILURES.entrySet().forEach((e) -> {
                out.printf("\t%s: %s%n", e.getKey(), e.getValue());
                e.getValue().printStackTrace(out);
                e.getValue().printStackTrace();
            });
         } finally {
            out.println("\n=========================\n");
        }
    }

    @DataProvider(name = "sanity")
    public Object[][] sanity() {
        return new Object[][]{
            { httpURIVarLen  + "?length=all" },
            { httpsURIVarLen + "?length=all" },
            { httpURIFixLen  + "?length=all" },
        };
    }

    @Test(dataProvider = "sanity")
    void sanity(String url) throws Exception {
        HttpClient client = newHttpClient();
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
    public Object[][] variants(ITestContext context) {
        String[][] cases = new String[][] {
            // The length query string is the total number of bytes in the reply,
            // including headers, before the server closes the connection. The
            // second arg is a partial-expected-detail message in the exception.
            { httpURIVarLen + "?length=0",   "no bytes"     }, // EOF without receiving anything
            { httpURIVarLen + "?length=1",   "status line"  }, // EOF during status-line
            { httpURIVarLen + "?length=2",   "status line"  },
            { httpURIVarLen + "?length=10",  "status line"  },
            { httpURIVarLen + "?length=19",  "header"       }, // EOF during Content-Type header
            { httpURIVarLen + "?length=30",  "header"       },
            { httpURIVarLen + "?length=45",  "header"       },
            { httpURIVarLen + "?length=48",  "header"       },
            { httpURIVarLen + "?length=51",  "header"       },
            { httpURIVarLen + "?length=98",  "header"       }, // EOF during Connection header
            { httpURIVarLen + "?length=100", "header"       },
            { httpURIVarLen + "?length=101", "header"       },
            { httpURIVarLen + "?length=104", "header"       },
            { httpURIVarLen + "?length=106", "chunked transfer encoding" }, // EOF during chunk header ( length )
            { httpURIVarLen + "?length=110", "chunked transfer encoding" }, // EOF during chunk response body data

            { httpsURIVarLen + "?length=0",   "no bytes"    },
            { httpsURIVarLen + "?length=1",   "status line" },
            { httpsURIVarLen + "?length=2",   "status line" },
            { httpsURIVarLen + "?length=10",  "status line" },
            { httpsURIVarLen + "?length=19",  "header"      },
            { httpsURIVarLen + "?length=30",  "header"      },
            { httpsURIVarLen + "?length=45",  "header"      },
            { httpsURIVarLen + "?length=48",  "header"      },
            { httpsURIVarLen + "?length=51",  "header"      },
            { httpsURIVarLen + "?length=98",  "header"      },
            { httpsURIVarLen + "?length=100", "header"      },
            { httpsURIVarLen + "?length=101", "header"      },
            { httpsURIVarLen + "?length=104", "header"      },
            { httpsURIVarLen + "?length=106", "chunked transfer encoding" },
            { httpsURIVarLen + "?length=110", "chunked transfer encoding" },

            { httpURIFixLen + "?length=0",   "no bytes"    }, // EOF without receiving anything
            { httpURIFixLen + "?length=1",   "status line" }, // EOF during status-line
            { httpURIFixLen + "?length=2",   "status line" },
            { httpURIFixLen + "?length=10",  "status line" },
            { httpURIFixLen + "?length=19",  "header"      }, // EOF during Content-Type header
            { httpURIFixLen + "?length=30",  "header"      },
            { httpURIFixLen + "?length=45",  "header"      },
            { httpURIFixLen + "?length=48",  "header"      },
            { httpURIFixLen + "?length=51",  "header"      },
            { httpURIFixLen + "?length=78",  "header"      }, // EOF during Connection header
            { httpURIFixLen + "?length=79",  "header"      },
            { httpURIFixLen + "?length=86",  "header"      },
            { httpURIFixLen + "?length=104", "fixed content-length" }, // EOF during body
            { httpURIFixLen + "?length=106", "fixed content-length" },
            { httpURIFixLen + "?length=110", "fixed content-length" },

            // ## ADD https fixed

            { httpURIClsImed,  "no bytes"},
            { httpsURIClsImed, "no bytes"},
        };

        if (context.getFailedTests().size() > 0) {
            // Shorten the log output by preventing useless
            // skip traces to be printed for subsequent methods
            // if one of the previous @Test method has failed.
            return new Object[0][];
        }

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

    HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .proxy(NO_PROXY)
                .sslContext(sslContext)
                .sslParameters(sslParameters)
                .executor(service)
                .build();
    }

    @Test(dataProvider = "uris")
    void testSynchronousGET(String url, String expectedMsg, boolean sameClient)
        throws Exception
    {
        checkSkip();
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            try {
                HttpResponse<String> response = client.send(request, ofString());
                String body = response.body();
                out.println(response + ": " + body);
                fail("UNEXPECTED RESPONSE: " + response);
            } catch (IOException ioe) {
                out.println("Caught expected exception:" + ioe);
                assertExpectedMessage(request, ioe, expectedMsg);
                // synchronous API must have the send method on the stack
                assertSendMethodOnStack(ioe);
                assertNoConnectionExpiredException(ioe);
            }
        }
    }

    @Test(dataProvider = "uris")
    void testAsynchronousGET(String url, String expectedMsg, boolean sameClient)
        throws Exception
    {
        checkSkip();
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            try {
                HttpResponse<String> response = client.sendAsync(request, ofString()).get();
                String body = response.body();
                out.println(response + ": " + body);
                fail("UNEXPECTED RESPONSE: " + response);
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof IOException) {
                    IOException ioe = (IOException) ee.getCause();
                    out.println("Caught expected exception:" + ioe);
                    assertExpectedMessage(request, ioe, expectedMsg);
                    assertNoConnectionExpiredException(ioe);
                } else {
                    throw ee;
                }
            }
        }
    }

    // can be used to prolong request body publication
    static final class InfiniteInputStream extends InputStream {
        int count = 0;
        int k16 = 0;
        @Override
        public int read() throws IOException {
            if (++count == 1) {
                System.out.println("Start sending 1 byte");
            }
            if (count > 16 * 1024) {
                k16++;
                System.out.println("... 16K sent.");
                count = count % (16 * 1024);
            }
            if (k16 > 128) {
                System.out.println("WARNING: InfiniteInputStream: " +
                        "more than 128 16k buffers generated: returning EOF");
                return -1;
            }
            return 1;
        }

        @Override
        public int read(byte[] buf, int offset, int length) {
            //int count = offset;
            length = Math.max(0, Math.min(buf.length - offset, length));
            //for (; count < length; count++)
            //    buf[offset++] = 0x01;
            //return count;
            if (count == 0) {
                System.out.println("Start sending " + length);
            } else if (count > 16 * 1024) {
                k16++;
                System.out.println("... 16K sent.");
                count = count % (16 * 1024);
            }
            if (k16 > 128) {
                System.out.println("WARNING: InfiniteInputStream: " +
                        "more than 128 16k buffers generated: returning EOF");
                return -1;
            }
            count += length;
            return length;
        }
    }

    // POST tests are racy in what may be received before writing may cause a
    // broken pipe or reset exception, before all the received data can be read.
    // Any message up to, and including, the "expected" error message can occur.
    // Strictly ordered list, in order of possible occurrence.
    static final List<String> MSGS_ORDER =
            List.of("no bytes", "status line", "header");


    @Test(dataProvider = "uris")
    void testSynchronousPOST(String url, String expectedMsg, boolean sameClient)
        throws Exception
    {
        checkSkip();
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .POST(BodyPublishers.ofInputStream(() -> new InfiniteInputStream()))
                    .build();
            try {
                HttpResponse<String> response = client.send(request, ofString());
                String body = response.body();
                out.println(response + ": " + body);
                fail("UNEXPECTED RESPONSE: " + response);
            } catch (IOException ioe) {
                out.println("Caught expected exception:" + ioe);

                List<String> expectedMessages = new ArrayList<>();
                expectedMessages.add(expectedMsg);
                MSGS_ORDER.stream().takeWhile(s -> !s.equals(expectedMsg))
                                   .forEach(expectedMessages::add);

                assertExpectedMessage(request, ioe, expectedMessages);
                // synchronous API must have the send method on the stack
                assertSendMethodOnStack(ioe);
                assertNoConnectionExpiredException(ioe);
            }
        }
    }

    @Test(dataProvider = "uris")
    void testAsynchronousPOST(String url, String expectedMsg, boolean sameClient)
        throws Exception
    {
        checkSkip();
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .POST(BodyPublishers.ofInputStream(() -> new InfiniteInputStream()))
                    .build();
            try {
                HttpResponse<String> response = client.sendAsync(request, ofString()).get();
                String body = response.body();
                out.println(response + ": " + body);
                fail("UNEXPECTED RESPONSE: " + response);
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof IOException) {
                    IOException ioe = (IOException) ee.getCause();
                    out.println("Caught expected exception:" + ioe);

                    List<String> expectedMessages = new ArrayList<>();
                    expectedMessages.add(expectedMsg);
                    MSGS_ORDER.stream().takeWhile(s -> !s.equals(expectedMsg))
                            .forEach(expectedMessages::add);

                    assertExpectedMessage(request, ioe, expectedMessages);
                    assertNoConnectionExpiredException(ioe);
                } else {
                    throw ee;
                }
            }
        }
    }


    void assertExpectedMessage(HttpRequest request, Throwable t, String expected) {
        if (request.uri().getScheme().equalsIgnoreCase("https")
                && (t instanceof SSLHandshakeException)) {
            // OK
            out.println("Skipping expected " + t);
        } else {
            String msg = t.getMessage();
            assertTrue(msg.contains(expected),
                    "exception msg:[" + msg + "]");
        }
    }

    void assertExpectedMessage(HttpRequest request, Throwable t, List<String> expected) {
        if (request.uri().getScheme().equalsIgnoreCase("https")
                && (t instanceof SSLHandshakeException)) {
            // OK
            out.println("Skipping expected " + t);
        } else {
            String msg = t.getMessage();
            assertTrue(expected.stream().anyMatch(msg::contains),
                    "exception msg:[" + msg + "] not in " + Arrays.asList(expected));
        }
    }

    // Asserts that the "send" method appears in the stack of the given
    // exception. The synchronous API must contain the send method on the stack.
    static void assertSendMethodOnStack(IOException ioe) {
        final String cn = "jdk.internal.net.http.HttpClientImpl";
        List<StackTraceElement> list = Stream.of(ioe.getStackTrace())
                .filter(ste -> ste.getClassName().equals(cn)
                        && ste.getMethodName().equals("send"))
                .collect(toList());
        if (list.size() != 1) {
            ioe.printStackTrace(out);
            fail(cn + ".send method not found in stack.");
        }
    }

    // Asserts that the implementation-specific ConnectionExpiredException does
    // NOT appear anywhere in the exception or its causal chain.
    static void assertNoConnectionExpiredException(IOException ioe) {
        Throwable throwable = ioe;
        do {
            String cn = throwable.getClass().getSimpleName();
            if (cn.equals("ConnectionExpiredException")) {
                ioe.printStackTrace(out);
                fail("UNEXPECTED ConnectionExpiredException in:[" + ioe + "]");
            }
        } while ((throwable = throwable.getCause()) != null);
    }

    // -- infra

    /**
     * A server that, listens on a port, accepts new connections, and can be
     * closed.
     */
    static abstract class Server extends Thread implements AutoCloseable {
        protected final ServerSocket ss;
        protected volatile boolean closed;

        Server(String name) throws IOException {
            super(name);
            ss = newServerSocket();
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            this.start();
        }

        protected ServerSocket newServerSocket() throws IOException {
            return new ServerSocket();
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
                out.println("Unexpected exception while closing server: " + e);
                e.printStackTrace(out);
                throw new UncheckedIOException("Unexpected: ", e);
            }
        }
    }

    /**
     * A server that closes the connection immediately, without reading or writing.
     */
    static class PlainCloseImmediatelyServer extends Server {
        PlainCloseImmediatelyServer() throws IOException {
            super("PlainCloseImmediatelyServer");
        }

        protected PlainCloseImmediatelyServer(String name) throws IOException {
            super(name);
        }

        @Override
        public void run() {
            while (!closed) {
                try (Socket s = ss.accept()) {
                    if (s instanceof SSLSocket) {
                        ((SSLSocket)s).startHandshake();
                    }
                    out.println("Server: got connection, closing immediately ");
                } catch (Throwable e) {
                    if (!closed) {
                        out.println("Unexpected exception in server: " + e);
                        e.printStackTrace(out);
                        throw new RuntimeException("Unexpected: ", e);
                    }
                }
            }
        }
    }

    /**
     * A server that closes the connection immediately, without reading or writing,
     * after completing the SSL handshake.
     */
    static final class SSLCloseImmediatelyServer extends PlainCloseImmediatelyServer {
        SSLCloseImmediatelyServer() throws IOException {
            super("SSLCloseImmediatelyServer");
        }
        @Override
        public ServerSocket newServerSocket() throws IOException {
            return SSLServerSocketFactory.getDefault().createServerSocket();
        }
    }

    /**
     * A server that replies with headers and a, possibly partial, reply, before
     * closing the connection. The number of bytes of written ( header + body),
     * is controllable through the "length" query string param in the requested
     * URI.
     */
    static abstract class ReplyingServer extends Server {

        private final String name;

        ReplyingServer(String name) throws IOException {
            super(name);
            this.name = name;
        }

        abstract String response();

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
                    String headers = readRequestHeaders(is);

                    String query = uriPath.getRawQuery();
                    if (query == null) {
                        out.println("Request headers: [" + headers + "]");
                    }
                    assert query != null : "null query for uriPath: " + uriPath;
                    String qv = query.split("=")[1];
                    int len;
                    if (qv.equals("all")) {
                        len = response().getBytes(US_ASCII).length;
                    } else {
                        len = Integer.parseInt(query.split("=")[1]);
                    }

                    OutputStream os = s.getOutputStream();
                    out.println(name + ": writing " + len  + " bytes");
                    byte[] responseBytes = response().getBytes(US_ASCII);
                    for (int i = 0; i< len; i++) {
                        os.write(responseBytes[i]);
                        os.flush();
                    }
                } catch (Throwable e) {
                    if (!closed) {
                        out.println("Unexpected exception in server: " + e);
                        e.printStackTrace(out);
                        throw new RuntimeException("Unexpected: " + e, e);
                    }
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
        static String readRequestHeaders(InputStream is) throws IOException {
            int requestEndCount = 0, r;
            StringBuilder sb = new StringBuilder();
            while ((r = is.read()) != -1) {
                sb.append((char) r);
                if (r == requestEnd[requestEndCount]) {
                    requestEndCount++;
                    if (requestEndCount == 4) {
                        break;
                    }
                } else {
                    requestEndCount = 0;
                }
            }
            return sb.toString();
        }
    }

    /** A server that issues a, possibly-partial, chunked reply. */
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

        static final String RESPONSE = RESPONSE_HEADERS + CHUNKED_RESPONSE_BODY;

        PlainVariableLengthServer() throws IOException {
            super("PlainVariableLengthServer");
        }

        protected PlainVariableLengthServer(String name) throws IOException {
            super(name);
        }

        @Override
        String response( ) { return RESPONSE; }
    }

    /** A server that issues a, possibly-partial, chunked reply over SSL. */
    static final class SSLVariableLengthServer extends PlainVariableLengthServer {
        SSLVariableLengthServer() throws IOException {
            super("SSLVariableLengthServer");
        }
        @Override
        public ServerSocket newServerSocket() throws IOException {
            return SSLServerSocketFactory.getDefault().createServerSocket();
        }
    }

    /** A server that issues a fixed-length reply. */
    static final class FixedLengthServer extends ReplyingServer {

        static final String RESPONSE_BODY = EXPECTED_RESPONSE_BODY;

        static final String RESPONSE_HEADERS =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + RESPONSE_BODY.length() + "\r\n" +
                "Connection: close\r\n\r\n";

        static final String RESPONSE = RESPONSE_HEADERS + RESPONSE_BODY;

        FixedLengthServer() throws IOException {
            super("FixedLengthServer");
        }

        @Override
        String response( ) { return RESPONSE; }
    }

    static String serverAuthority(Server server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");
        SSLContext.setDefault(sslContext);

        sslParameters = new SSLParameters();

        closeImmediatelyServer = new PlainCloseImmediatelyServer();
        httpURIClsImed = "http://" + serverAuthority(closeImmediatelyServer)
                + "/http1/closeImmediately/foo";

        closeImmediatelyHttpsServer = new SSLCloseImmediatelyServer();
        httpsURIClsImed = "https://" + serverAuthority(closeImmediatelyHttpsServer)
                + "/https1/closeImmediately/foo";

        variableLengthServer = new PlainVariableLengthServer();
        httpURIVarLen = "http://" + serverAuthority(variableLengthServer)
                + "/http1/variable/bar";

        variableLengthHttpsServer = new SSLVariableLengthServer();
        httpsURIVarLen = "https://" + serverAuthority(variableLengthHttpsServer)
                + "/https1/variable/bar";

        fixedLengthServer = new FixedLengthServer();
        httpURIFixLen = "http://" + serverAuthority(fixedLengthServer)
                + "/http1/fixed/baz";
    }

    @AfterTest
    public void teardown() throws Exception {
        closeImmediatelyServer.close();
        closeImmediatelyHttpsServer.close();
        variableLengthServer.close();
        variableLengthHttpsServer.close();
        fixedLengthServer.close();
    }
}
