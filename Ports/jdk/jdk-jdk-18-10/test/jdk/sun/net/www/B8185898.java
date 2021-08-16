/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8185898
 * @modules java.base/sun.net.www
 * @library /test/lib
 * @run main/othervm B8185898
 * @summary setRequestProperty(key, null) results in HTTP header without colon in request
 */

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import jdk.test.lib.net.URIBuilder;
import sun.net.www.MessageHeader;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/*
 * Test checks that MessageHeader with key != null and value == null is set correctly
 * and printed according to HTTP standard in the format <key>: <value>
 * */
public class B8185898 {

    static HttpServer server;
    static final String RESPONSE_BODY = "Test response body";
    static final String H1 = "X-header1";
    static final String H2 = "X-header2";
    static final String VALUE = "This test value should appear";
    static int port;
    static URL url;
    static volatile Map<String, List<String>> headers;

    static class Handler implements HttpHandler {

        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            InetSocketAddress rem = t.getRemoteAddress();
            headers = t.getRequestHeaders();    // Get request headers on the server side
            is.readAllBytes();
            is.close();

            OutputStream os = t.getResponseBody();
            t.sendResponseHeaders(200, RESPONSE_BODY.length());
            os.write(RESPONSE_BODY.getBytes(UTF_8));
            t.close();
        }
    }

    public static void main(String[] args) throws Exception {
        ExecutorService exec = Executors.newCachedThreadPool();
        InetAddress loopback = InetAddress.getLoopbackAddress();

        try {
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);
            server = HttpServer.create(addr, 100);
            HttpHandler handler = new Handler();
            HttpContext context = server.createContext("/", handler);
            server.setExecutor(exec);
            server.start();

            port = server.getAddress().getPort();
            System.out.println("Server on port: " + port);
            url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(port)
                    .path("/foo")
                    .toURLUnchecked();
            System.out.println("URL: " + url);
            testMessageHeader();
            testMessageHeaderMethods();
            testURLConnectionMethods();
        } finally {
            server.stop(0);
            System.out.println("After server shutdown");
            exec.shutdown();
        }
    }

    // Test message header with malformed message header and fake request line
    static void testMessageHeader() {
        final String badHeader = "This is not a request line for HTTP/1.1";
        final String fakeRequestLine = "This /is/a/fake/status/line HTTP/2.0";
        final String expectedHeaders = fakeRequestLine + "\r\n"
                + H1 + ": " + VALUE + "\r\n"
                + H2 + ": " + VALUE + "\r\n"
                + badHeader + ":\r\n\r\n";

        MessageHeader header = new MessageHeader();
        header.add(H1, VALUE);
        header.add(H2, VALUE);
        header.add(badHeader, null);
        header.prepend(fakeRequestLine, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        header.print(new PrintStream(out));

        if (!out.toString().equals(expectedHeaders)) {
            throw new AssertionError("FAILED: expected: "
                    + expectedHeaders + "\nReceived: " + out.toString());
        } else {
            System.out.println("PASSED: ::print returned correct "
                    + "status line and headers:\n" + out.toString());
        }
    }

    // Test MessageHeader::print, ::toString, implicitly testing that
    // MessageHeader::mergeHeader formats headers correctly for responses
    static void testMessageHeaderMethods() throws IOException {
        // {{inputString1, expectedToString1, expectedPrint1}, {...}}
        String[][] strings = {
                {"HTTP/1.1 200 OK\r\n"
                        + "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n"
                        + "Connection: keep-alive\r\n"
                        + "Host: 127.0.0.1:12345\r\n"
                        + "User-agent: Java/12\r\n\r\nfoooo",
                "pairs: {null: HTTP/1.1 200 OK}"
                        + "{Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2}"
                        + "{Connection: keep-alive}"
                        + "{Host: 127.0.0.1:12345}"
                        + "{User-agent: Java/12}",
                "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n"
                        + "Connection: keep-alive\r\n"
                        + "Host: 127.0.0.1:12345\r\n"
                        + "User-agent: Java/12\r\n\r\n"},
                {"HTTP/1.1 200 OK\r\n"
                        + "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n"
                        + "Connection: keep-alive\r\n"
                        + "Host: 127.0.0.1:12345\r\n"
                        + "User-agent: Java/12\r\n"
                        + "X-Header:\r\n\r\n",
                "pairs: {null: HTTP/1.1 200 OK}"
                        + "{Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2}"
                        + "{Connection: keep-alive}"
                        + "{Host: 127.0.0.1:12345}"
                        + "{User-agent: Java/12}"
                        + "{X-Header: }",
                "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n"
                        + "Connection: keep-alive\r\n"
                        + "Host: 127.0.0.1:12345\r\n"
                        + "User-agent: Java/12\r\n"
                        + "X-Header: \r\n\r\n"},
        };

        System.out.println("Test custom message headers");
        for (String[] s : strings) {
            // Test MessageHeader::toString
            MessageHeader header = new MessageHeader(
                    new ByteArrayInputStream(s[0].getBytes(ISO_8859_1)));
            if (!header.toString().endsWith(s[1])) {
                throw new AssertionError("FAILED: expected: "
                        + s[1] + "\nReceived: " + header);
            } else {
                System.out.println("PASSED: ::toString returned correct "
                        + "status line and headers:\n" + header);
            }

            // Test MessageHeader::print
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            header.print(new PrintStream(out));
            if (!out.toString().equals(s[2])) {
                throw new AssertionError("FAILED: expected: "
                        + s[2] + "\nReceived: " + out.toString());
            } else {
                System.out.println("PASSED: ::print returned correct "
                        + "status line and headers:\n" + out.toString());
            }
        }
    }

    // Test methods URLConnection::getRequestProperties,
    // ::getHeaderField, ::getHeaderFieldKey
    static void testURLConnectionMethods() throws IOException {
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        urlConn.setRequestProperty(H1, "");
        urlConn.setRequestProperty(H1, VALUE);
        urlConn.setRequestProperty(H2, null);    // Expected to contain ':' between key and value
        Map<String, List<String>> props = urlConn.getRequestProperties();
        Map<String, List<String>> expectedMap = Map.of(
                H1, List.of(VALUE),
                H2, Arrays.asList((String) null));

        // Test request properties
        System.out.println("Client request properties");
        StringBuilder sb = new StringBuilder();
        props.forEach((k, v) -> sb.append(k + ": "
                + v.stream().collect(Collectors.joining()) + "\n"));
        System.out.println(sb);

        if (!props.equals(expectedMap)) {
            throw new AssertionError("Unexpected properties returned: "
                    + props);
        } else {
            System.out.println("Properties returned as expected");
        }

        // Test header fields
        String headerField = urlConn.getHeaderField(0);
        if (!headerField.contains("200 OK")) {
            throw new AssertionError("Expected headerField[0]: status line. "
                    + "Received: " + headerField);
        } else {
            System.out.println("PASSED: headerField[0] contains status line: "
                    + headerField);
        }

        String headerFieldKey = urlConn.getHeaderFieldKey(0);
        if (headerFieldKey != null) {
            throw new AssertionError("Expected headerFieldKey[0]: null. "
                    + "Received: " + headerFieldKey);
        } else {
            System.out.println("PASSED: headerFieldKey[0] is null");
        }

        // Check that test request headers are included with correct format
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConn.getInputStream()))
        ) {
            if (!headers.keySet().contains(H1)) {
                throw new AssertionError("Expected key not found: "
                        + H1 + ": " + VALUE);
            } else if (!headers.get(H1).equals(List.of(VALUE))) {
                throw new AssertionError("Unexpected key-value pair: "
                        + H1 + ": " + headers.get(H1));
            } else {
                System.out.println("PASSED: " + H1 + " included in request headers");
            }

            if (!headers.keySet().contains(H2)) {
                throw new AssertionError("Expected key not found: "
                        + H2 + ": ");
                // Check that empty list is returned
            } else if (!headers.get(H2).equals(List.of(""))) {
                throw new AssertionError("Unexpected key-value pair: "
                        + H2 + ": " + headers.get(H2));
            } else {
                System.out.println("PASSED: " + H2 + " included in request headers");
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
            }
        }
    }
}
