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
 * @bug 8199851
 * @summary Test for multiple vs single cookie header for HTTP/2 vs HTTP/1.1
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
 *       -Djdk.tls.acknowledgeCloseNotify=true
 *       -Djdk.httpclient.HttpClient.log=trace,headers,requests
 *       CookieHeaderTest
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CookieHeaderTest implements HttpServerAdapters {

    SSLContext sslContext;
    HttpTestServer httpTestServer;        // HTTP/1.1    [ 6 servers ]
    HttpTestServer httpsTestServer;       // HTTPS/1.1
    HttpTestServer http2TestServer;       // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;      // HTTP/2 ( h2  )
    DummyServer httpDummyServer;
    DummyServer httpsDummyServer;
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;
    String httpDummy;
    String httpsDummy;

    static final String MESSAGE = "Basic CookieHeaderTest message body";
    static final int ITERATIONS = 3;
    static final long start = System.nanoTime();
    public static String now() {
        long now = System.nanoTime() - start;
        long secs = now / 1000_000_000;
        long mill = (now % 1000_000_000) / 1000_000;
        long nan = now % 1000_000;
        return String.format("[%d s, %d ms, %d ns] ", secs, mill, nan);
    }

    @DataProvider(name = "positive")
    public Object[][] positive() {
        return new Object[][] {
                { httpURI, HttpClient.Version.HTTP_1_1  },
                { httpsURI, HttpClient.Version.HTTP_1_1  },
                { httpDummy, HttpClient.Version.HTTP_1_1 },
                { httpsDummy, HttpClient.Version.HTTP_1_1 },
                { httpURI, HttpClient.Version.HTTP_2  },
                { httpsURI, HttpClient.Version.HTTP_2  },
                { httpDummy, HttpClient.Version.HTTP_2 },
                { httpsDummy, HttpClient.Version.HTTP_2 },
                { http2URI, null  },
                { https2URI, null },
        };
    }

    static final AtomicLong requestCounter = new AtomicLong();

    @Test(dataProvider = "positive")
    void test(String uriString, HttpClient.Version version) throws Exception {
        out.printf("%n---- starting (%s) ----%n", uriString);
        ConcurrentHashMap<String, List<String>> cookieHeaders
                = new ConcurrentHashMap<>();
        CookieHandler cookieManager = new TestCookieHandler(cookieHeaders);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(Redirect.ALWAYS)
                .cookieHandler(cookieManager)
                .sslContext(sslContext)
                .build();
        assert client.cookieHandler().isPresent();

        URI uri = URI.create(uriString);
        List<String> cookies = new ArrayList<>();
        cookies.add("CUSTOMER=ARTHUR_DENT");
        cookies.add("LOCATION=TR\u0100IN_STATION");
        cookies.add("LOC\u0100TION=TRAIN_STATION");
        cookies.add("ORDER=BISCUITS");
        cookieHeaders.put("Cookie", cookies);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .header("X-uuid", "uuid-" + requestCounter.incrementAndGet());
        if (version != null) {
            requestBuilder.version(version);
        }
        HttpRequest request = requestBuilder.build();
        out.println("Initial request: " + request.uri());

        for (int i=0; i< ITERATIONS; i++) {
            out.println("iteration: " + i);
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            out.println("  Got response: " + response);
            out.println("  Got body Path: " + response.body());

            assertEquals(response.statusCode(), 200);
            assertEquals(response.body(), MESSAGE);
            assertEquals(response.headers().allValues("X-Request-Cookie"),
                    cookies.stream()
                            .filter(s -> !s.startsWith("LOC"))
                            .collect(Collectors.toList()));
            requestBuilder = HttpRequest.newBuilder(uri)
                    .header("X-uuid", "uuid-" + requestCounter.incrementAndGet());
            if (version != null) {
                requestBuilder.version(version);
            }
            request = requestBuilder.build();
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
        httpTestServer.addHandler(new CookieValidationHandler(), "/http1/cookie/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/cookie/retry";
        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        httpsTestServer.addHandler(new CookieValidationHandler(),"/https1/cookie/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/cookie/retry";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2TestServer.addHandler(new CookieValidationHandler(), "/http2/cookie/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/cookie/retry";
        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        https2TestServer.addHandler(new CookieValidationHandler(), "/https2/cookie/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/cookie/retry";


        // DummyServer
        httpDummyServer = DummyServer.create(sa);
        httpsDummyServer = DummyServer.create(sa, sslContext);
        httpDummy = "http://" + httpDummyServer.serverAuthority() + "/http1/dummy/x";
        httpsDummy = "https://" + httpsDummyServer.serverAuthority() + "/https1/dummy/x";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
        httpDummyServer.start();
        httpsDummyServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
        httpsDummyServer.stopServer();
        httpsDummyServer.stopServer();
    }

    static class TestCookieHandler extends CookieHandler {

        final ConcurrentHashMap<String, List<String>> cookies;
        TestCookieHandler(ConcurrentHashMap<String, List<String>> map) {
            this.cookies = map;
        }

        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders)
                throws IOException
        {
            return cookies;
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders)
                throws IOException
        {
            // do nothing
        }
    }

    static class CookieValidationHandler implements HttpTestHandler {
        ConcurrentHashMap<String,String> closedRequests = new ConcurrentHashMap<>();

        @Override
        public void handle(HttpTestExchange t) throws IOException {
            System.out.println("CookieValidationHandler for: " + t.getRequestURI());

            List<String> uuids = t.getRequestHeaders().get("X-uuid");
            if (uuids == null || uuids.size() != 1) {
                readAllRequestData(t);
                try (OutputStream os = t.getResponseBody()) {
                    String msg = "Incorrect uuid header values:[" + uuids + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                }
                return;
            }

            String uuid = uuids.get(0);
            // retrying
            if (closedRequests.putIfAbsent(uuid, t.getRequestURI().toString()) == null) {
                if (t.getExchangeVersion() == HttpClient.Version.HTTP_1_1) {
                    // Throwing an exception here only causes a retry
                    // with HTTP_1_1 - where it forces the server to close
                    // the connection.
                    // For HTTP/2 then throwing an IOE would cause the server
                    // to close the stream, and throwing anything else would
                    // cause it to close the connection, but neither would
                    // cause the client to retry.
                    // So we simply do not try to retry with HTTP/2 and just verify
                    // we have received the expected cookie
                    throw new IOException("Closing on first request");
                }
            }

            // Check whether this request was upgraded.
            // An upgraded request will have a version of HTTP_2 and
            // an Upgrade: h2c header
            HttpClient.Version version = t.getExchangeVersion();
            List<String> upgrade = t.getRequestHeaders().get("Upgrade");
            if (upgrade == null) upgrade = List.of();
            boolean upgraded = version == HttpClient.Version.HTTP_2
                    && upgrade.stream().anyMatch("h2c"::equalsIgnoreCase);

            // not retrying
            readAllRequestData(t);
            try (OutputStream os = t.getResponseBody()) {
                List<String> cookie = t.getRequestHeaders().get("Cookie");
                if (cookie != null) {
                    if (version == HttpClient.Version.HTTP_1_1 || upgraded) {
                        if (cookie.size() == 1) {
                            cookie = List.of(cookie.get(0).split("; "));
                        } else if (cookie.size() > 1) {
                            String msg = "Found multiple 'Cookie:' lines for version=%s (upgraded=%s): %s";
                            msg = String.format(msg, version, upgraded, cookie);
                            (new RuntimeException(msg)).printStackTrace();
                            t.sendResponseHeaders(500, -1);
                            os.write(msg.getBytes(UTF_8));
                            return;
                        }
                    }
                    Collections.sort(cookie = new ArrayList<String>(cookie));
                }
                if (cookie == null || cookie.size() == 0) {
                    String msg = "No cookie header present";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else if (!cookie.get(0).equals("CUSTOMER=ARTHUR_DENT")) {
                    String msg = "Incorrect cookie header value:[" + cookie.get(0) + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else if (cookie.size() == 2 && !cookie.get(1).equals("ORDER=BISCUITS")) {
                    String msg = "Incorrect cookie header value:[" + cookie.get(0) + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else if (cookie.size() != 2) {
                    String msg = "Incorrect cookie header values:[" + cookie + "]";
                    (new RuntimeException(msg)).printStackTrace();
                    t.sendResponseHeaders(500, -1);
                    os.write(msg.getBytes(UTF_8));
                } else {
                    assert cookie.get(0).equals("CUSTOMER=ARTHUR_DENT");
                    byte[] bytes = MESSAGE.getBytes(UTF_8);
                    for (String value : cookie) {
                        t.getResponseHeaders().addHeader("X-Request-Cookie", value);
                    }
                    t.sendResponseHeaders(200, bytes.length);
                    os.write(bytes);
                }
            } finally {
                closedRequests.remove(uuid);
            }

        }
    }

    static void readAllRequestData(HttpTestExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            is.readAllBytes();
        }
    }

    static class DummyServer extends Thread {
        final ServerSocket ss;
        final boolean secure;
        ConcurrentLinkedQueue<Socket> connections = new ConcurrentLinkedQueue<>();
        volatile boolean stopped;
        DummyServer(ServerSocket ss, boolean secure) {
            super("DummyServer[" + ss.getLocalPort()+"]");
            this.secure = secure;
            this.ss = ss;
        }

        // This is a bit shaky. It doesn't handle continuation
        // lines, but our client shouldn't send any.
        // Read a line from the input stream, swallowing the final
        // \r\n sequence. Stops at the first \n, doesn't complain
        // if it wasn't preceded by '\r'.
        //
        String readLine(InputStream r) throws IOException {
            StringBuilder b = new StringBuilder();
            int c;
            while ((c = r.read()) != -1) {
                if (c == '\n') break;
                b.appendCodePoint(c);
            }
            if (b.codePointAt(b.length() -1) == '\r') {
                b.delete(b.length() -1, b.length());
            }
            return b.toString();
        }

        @Override
        public void run() {
            try {
                while(!stopped) {
                    Socket clientConnection = ss.accept();
                    connections.add(clientConnection);
                    System.out.println(now() + getName() + ": Client accepted");
                    StringBuilder headers = new StringBuilder();
                    Socket targetConnection = null;
                    InputStream  ccis = clientConnection.getInputStream();
                    OutputStream ccos = clientConnection.getOutputStream();
                    Writer w = new OutputStreamWriter(
                            clientConnection.getOutputStream(), "UTF-8");
                    PrintWriter pw = new PrintWriter(w);
                    System.out.println(now() + getName() + ": Reading request line");
                    String requestLine = readLine(ccis);
                    System.out.println(now() + getName() + ": Request line: " + requestLine);

                    StringTokenizer tokenizer = new StringTokenizer(requestLine);
                    String method = tokenizer.nextToken();
                    assert method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("GET");
                    String path = tokenizer.nextToken();
                    URI uri;
                    try {
                        String hostport = serverAuthority();
                        uri = new URI((secure ? "https" : "http") +"://" + hostport + path);
                    } catch (Throwable x) {
                        System.err.printf("Bad target address: \"%s\" in \"%s\"%n",
                                path, requestLine);
                        clientConnection.close();
                        continue;
                    }

                    // Read all headers until we find the empty line that
                    // signals the end of all headers.
                    String line = requestLine;
                    String cookies = null;
                    while (!line.equals("")) {
                        System.out.println(now() + getName() + ": Reading header: "
                                + (line = readLine(ccis)));
                        if (line.startsWith("Cookie:")) {
                            if (cookies == null) cookies = line;
                            else cookies = cookies + "\n" + line;
                        }
                        headers.append(line).append("\r\n");
                    }

                    StringBuilder response = new StringBuilder();
                    StringBuilder xheaders = new StringBuilder();

                    int index = headers.toString()
                            .toLowerCase(Locale.US)
                            .indexOf("content-length: ");
                    if (index >= 0) {
                        index = index + "content-length: ".length();
                        String cl = headers.toString().substring(index);
                        StringTokenizer tk = new StringTokenizer(cl);
                        int len = Integer.parseInt(tk.nextToken());
                        System.out.println(now() + getName()
                                + ": received body: "
                                + new String(ccis.readNBytes(len), UTF_8));
                    }
                    String resp = MESSAGE;
                    String status = "200 OK";
                    if (cookies == null) {
                        resp = "No cookies found in headers";
                        status = "500 Internal Server Error";
                    } else if (cookies.contains("\n")) {
                        resp = "More than one 'Cookie:' line found: "
                                + Arrays.asList(cookies.split("\n"));
                        status = "500 Internal Server Error";
                    } else {
                        List<String> values =
                                Stream.of(cookies.substring("Cookie:".length()).trim().split("; "))
                                        .map(String::trim)
                                        .collect(Collectors.toList());
                        Collections.sort(values);
                        if (values.size() != 2) {
                            resp = "Bad cookie list: " + values;
                            status = "500 Internal Server Error";
                        } else if (!values.get(0).equals("CUSTOMER=ARTHUR_DENT")) {
                            resp = "Unexpected cookie: " + values.get(0) + " in " + values;
                            status = "500 Internal Server Error";
                        } else if (!values.get(1).equals("ORDER=BISCUITS")) {
                            resp = "Unexpected cookie: " + values.get(1) + " in " + values;
                            status = "500 Internal Server Error";
                        } else {
                            for (String cookie : values) {
                                xheaders.append("X-Request-Cookie: ")
                                        .append(cookie)
                                        .append("\r\n");
                            }
                        }
                    }
                    byte[] b = resp.getBytes(UTF_8);
                    System.out.println(now()
                            + getName() + ": sending back " + uri);

                    response.append("HTTP/1.1 ")
                            .append(status)
                            .append("\r\nContent-Length: ")
                            .append(b.length)
                            .append("\r\n")
                            .append(xheaders)
                            .append("\r\n");

                    // Then send the 200 OK response to the client
                    System.out.println(now() + getName() + ": Sending "
                            + response);
                    pw.print(response);
                    pw.flush();
                    ccos.write(b);
                    ccos.flush();
                    ccos.close();
                    connections.remove(clientConnection);
                    clientConnection.close();
                }
            } catch (Throwable t) {
                if (!stopped) {
                    System.out.println(now() + getName() + ": failed: " + t);
                    t.printStackTrace();
                    try {
                        stopServer();
                    } catch (Throwable e) {

                    }
                }
            } finally {
                System.out.println(now() + getName() + ": exiting");
            }
        }

        void close(Socket s) {
            try {
                s.close();
            } catch(Throwable t) {

            }
        }
        public void stopServer() throws IOException {
            stopped = true;
            ss.close();
            connections.forEach(this::close);
        }

        public String serverAuthority() {
            return InetAddress.getLoopbackAddress().getHostName() + ":"
                    + ss.getLocalPort();
        }

        static DummyServer create(InetSocketAddress sa) throws IOException {
            ServerSocket ss = ServerSocketFactory.getDefault()
                    .createServerSocket(sa.getPort(), -1, sa.getAddress());
            return  new DummyServer(ss, false);
        }

        static DummyServer create(InetSocketAddress sa, SSLContext sslContext) throws IOException {
            ServerSocket ss = sslContext.getServerSocketFactory()
                    .createServerSocket(sa.getPort(), -1, sa.getAddress());
            return new DummyServer(ss, true);
        }


    }
}
