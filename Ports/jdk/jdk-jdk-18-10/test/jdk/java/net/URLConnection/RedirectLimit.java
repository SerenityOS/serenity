/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4458085 7095949
 * @library /test/lib
 * @summary  Redirects Limited to 5
 */

/*
 * Simulate a server that redirects ( to a different URL) 9 times
 * and see if the client correctly follows the trail
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;

import jdk.test.lib.net.URIBuilder;

class RedirLimitServer extends Thread {
    static final int TIMEOUT = 20 * 1000;
    static final int NUM_REDIRECTS = 9;

    static final String reply1 = "HTTP/1.1 307 Temporary Redirect\r\n" +
        "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
        "Server: Apache/1.3.14 (Unix)\r\n" +
        "Location: http://localhost:";
    static final String reply2 = ".html\r\n" +
        "Connection: close\r\n" +
        "Content-Type: text/html; charset=iso-8859-1\r\n\r\n" +
        "<html>Hello</html>";
    static final String reply3 = "HTTP/1.1 200 Ok\r\n" +
        "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
        "Server: Apache/1.3.14 (Unix)\r\n" +
        "Connection: close\r\n" +
        "Content-Type: text/html; charset=iso-8859-1\r\n\r\n" +
        "World";

    final ServerSocket ss;
    final int port;
    final CountDownLatch readyToStart = new CountDownLatch(1);

    RedirLimitServer(ServerSocket ss) throws IOException {
        this.ss = ss;
        port = this.ss.getLocalPort();
        this.ss.setSoTimeout(TIMEOUT);
    }

    static final byte[] requestEnd = new byte[] {'\r', '\n', '\r', '\n' };

    // Read until the end of a HTTP request
    void readOneRequest(InputStream is) throws IOException {
        int requestEndCount = 0, r;
        StringBuilder sb = new StringBuilder();
        while ((r = is.read()) != -1) {
            sb.append((char)r);
            if (r == requestEnd[requestEndCount]) {
                requestEndCount++;
                if (requestEndCount == 4) {
                    break;
                }
            } else {
                requestEndCount = 0;
            }
        }
        System.out.println("Server got request: " + sb.toString());
    }

    public void run() {
        try {
            readyToStart.countDown();
            for (int i=0; i<NUM_REDIRECTS; i++) {
                try (Socket s = ss.accept()) {
                    System.out.println("Server accepted socket: " + s);
                    s.setSoTimeout(TIMEOUT);
                    readOneRequest(s.getInputStream());
                    System.out.println("Redirecting to: /redirect" + i);
                    String reply = reply1 + port + "/redirect" + i + reply2;
                    s.getOutputStream().write(reply.getBytes());
                }
            }
            try (Socket s = ss.accept()) {
                System.out.println("Server accepted socket: " + s);
                s.setSoTimeout(TIMEOUT);
                readOneRequest(s.getInputStream());
                System.out.println("Replying...");
                s.getOutputStream().write(reply3.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
};

public class RedirectLimit {
    public static void main(String[] args) throws Exception {
        try (ServerSocket ss = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            int port = ss.getLocalPort();
            RedirLimitServer server = new RedirLimitServer(ss);
            server.start();
            server.readyToStart.await();
            URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(port)
                    .toURL();
            URLConnection conURL = url.openConnection(Proxy.NO_PROXY);

            conURL.setDoInput(true);
            conURL.setAllowUserInteraction(false);
            conURL.setUseCaches(false);

            try (InputStream in = conURL.getInputStream()) {
                if ((in.read() != (int) 'W') || (in.read() != (int) 'o')) {
                    throw new RuntimeException("Unexpected string read");
                }
            }
        }
    }
}
