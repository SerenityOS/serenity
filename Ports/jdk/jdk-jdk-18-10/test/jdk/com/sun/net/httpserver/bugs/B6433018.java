/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6433018
 * @summary  HTTP server sometimes sends bad request for browsers javascript
 * @run main B6433018
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6433018
 */

import com.sun.net.httpserver.*;

import java.util.concurrent.*;
import java.io.*;
import java.net.*;

public class B6433018 {

    static final String CRLF = "\r\n";

    /* invalid HTTP POST with extra CRLF at end */
    /* This checks that the server is able to handle it
     * and recognise the second request */

    static final String cmd =
        "POST /test/item HTTP/1.1"+CRLF+
        "Keep-Alive: 300"+CRLF+
        "Proxy-Connection: keep-alive"+CRLF+
        "Content-Type: text/xml"+CRLF+
        "Content-Length: 22"+CRLF+
        "Pragma: no-cache"+CRLF+
        "Cache-Control: no-cache"+CRLF+ CRLF+
        "<item desc=\"excuse\" />"+CRLF+
        "GET /test/items HTTP/1.1"+CRLF+
        "Host: araku:9999"+CRLF+
        "Accept-Language: en-us,en;q=0.5"+CRLF+
        "Accept-Encoding: gzip,deflate"+CRLF+
        "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7"+CRLF+
        "Keep-Alive: 300"+CRLF+
        "Proxy-Connection: keep-alive"+CRLF+
        "Pragma: no-cache"+CRLF+
        "Cache-Control: no-cache"+CRLF+CRLF;

    public static void main(String[] args) throws Exception {
        CountDownLatch finished = new CountDownLatch(2);
        Handler handler = new Handler(finished);
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress(loopback, 0);
        HttpServer server = HttpServer.create(addr, 0);
        HttpContext ctx = server.createContext("/test", handler);

        server.start();
        int port = server.getAddress().getPort();
        try (Socket s = new Socket(loopback, port);
             OutputStream os = s.getOutputStream()) {
            os.write(cmd.getBytes());
            finished.await(30, TimeUnit.SECONDS);
        } finally {
            server.stop(2);
        }

        if (finished.getCount() != 0)
            throw new RuntimeException("did not receive the 2 requests");

        System.out.println("OK");
    }

    static class Handler implements HttpHandler {
        private final CountDownLatch finished;

        Handler(CountDownLatch finished) {
            this.finished = finished;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try (InputStream is = t.getRequestBody()) {
                Headers map = t.getRequestHeaders();
                Headers rmap = t.getResponseHeaders();
                while (is.read() != -1);
            }
            t.sendResponseHeaders(200, -1);
            t.close();
            finished.countDown();
        }
    }
}
