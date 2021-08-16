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
 * @summary Ensure that the POST method is retied when the property is set.
 * @run testng/othervm -Djdk.httpclient.enableAllMethodRetry RetryPost
 * @run testng/othervm -Djdk.httpclient.enableAllMethodRetry=true RetryPost
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
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.testng.Assert.assertEquals;

public class RetryPost {

    FixedLengthServer fixedLengthServer;
    String httpURIFixLen;

    static final String RESPONSE_BODY =
            "You use a glass mirror to see your face: you use works of art to see your soul.";

    @DataProvider(name = "uris")
    public Object[][] variants() {
        return new Object[][] {
                { httpURIFixLen, true  },
                { httpURIFixLen, false },
        };
    }

    static final int ITERATION_COUNT = 3;

    static final String REQUEST_BODY = "Body";

    @Test(dataProvider = "uris")
    void testSynchronousPOST(String url, boolean sameClient) throws Exception  {
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .POST(BodyPublishers.ofString(REQUEST_BODY))
                    .build();
            HttpResponse<String> response = client.send(request, ofString());
            String body = response.body();
            out.println(response + ": " + body);
            assertEquals(response.statusCode(), 200);
            assertEquals(body, RESPONSE_BODY);
        }
    }

    @Test(dataProvider = "uris")
    void testAsynchronousPOST(String url, boolean sameClient) {
        out.print("---\n");
        HttpClient client = null;
        for (int i=0; i< ITERATION_COUNT; i++) {
            if (!sameClient || client == null)
                client = HttpClient.newBuilder().proxy(NO_PROXY).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .POST(BodyPublishers.ofString(REQUEST_BODY))
                    .build();
            client.sendAsync(request, ofString())
                    .thenApply(r -> { out.println(r + ": " + r.body()); return r; })
                    .thenApply(r -> { assertEquals(r.statusCode(), 200); return r; })
                    .thenApply(HttpResponse::body)
                    .thenAccept(b -> assertEquals(b, RESPONSE_BODY))
                    .join();
        }
    }


    /**
     * A server that issues a valid fixed-length reply on even requests, and
     * immediately closes the connection on odd requests ( tick-tock ).
     */
    static class FixedLengthServer extends Thread implements AutoCloseable {

        static final String RESPONSE_HEADERS =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + RESPONSE_BODY.length() + "\r\n" +
                "Connection: close\r\n\r\n";

        static final String RESPONSE = RESPONSE_HEADERS + RESPONSE_BODY;

        private final ServerSocket ss;
        private volatile boolean closed;
        private int invocationTimes;

        FixedLengthServer() throws IOException {
            super("FixedLengthServer");
            ss = new ServerSocket();
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            this.start();
        }

        public int getPort() { return ss.getLocalPort(); }

        @Override
        public void run() {
            while (!closed) {
                try (Socket s = ss.accept()) {
                    invocationTimes++;
                    out.print("FixedLengthServer: got connection ");
                    if ((invocationTimes & 0x1) == 0x1) {
                        out.println(" closing immediately");
                        s.close();
                        continue;
                    }
                    InputStream is = s.getInputStream();
                    URI requestMethod = readRequestMethod(is);
                    out.print(requestMethod + " ");
                    URI uriPath = readRequestPath(is);
                    out.println(uriPath);
                    readRequestHeaders(is);
                    byte[] body = is.readNBytes(4);
                    assert body.length == REQUEST_BODY.length() :
                            "Unexpected request body " + body.length;

                    OutputStream os = s.getOutputStream();
                    out.println("Server: writing response bytes");
                    byte[] responseBytes = RESPONSE.getBytes(US_ASCII);
                    os.write(responseBytes);
                } catch (IOException e) {
                    if (!closed)
                        throw new UncheckedIOException("Unexpected", e);
                }
            }
        }

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
    }

    static String serverAuthority(FixedLengthServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        fixedLengthServer = new FixedLengthServer();
        httpURIFixLen = "http://" + serverAuthority(fixedLengthServer)
                + "/http1/fixed/baz";
    }

    @AfterTest
    public void teardown() throws Exception {
        fixedLengthServer.close();
    }
}