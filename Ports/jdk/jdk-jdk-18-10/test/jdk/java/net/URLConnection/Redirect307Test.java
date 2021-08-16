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
 * @bug 4380568 7095949
 * @library /test/lib
 * @summary  HttpURLConnection does not support 307 redirects
 */
import java.io.*;
import java.net.*;
import jdk.test.lib.net.URIBuilder;
import static java.net.Proxy.NO_PROXY;

class RedirServer extends Thread {

    static final int TIMEOUT = 10 * 1000;

    ServerSocket ss;
    int port;

    String reply1Part1 = "HTTP/1.1 307 Temporary Redirect\r\n" +
        "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
        "Server: Apache/1.3.14 (Unix)\r\n" +
        "Location: http://localhost:";
    String reply1Part2 = "/redirected.html\r\n" +
        "Connection: close\r\n" +
        "Content-Type: text/html; charset=iso-8859-1\r\n\r\n" +
        "<html>Hello</html>";

    RedirServer (ServerSocket ss) throws IOException {
        this.ss = ss;
        this.ss.setSoTimeout(TIMEOUT);
        port = this.ss.getLocalPort();
    }

    String reply2 = "HTTP/1.1 200 Ok\r\n" +
        "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
        "Server: Apache/1.3.14 (Unix)\r\n" +
        "Connection: close\r\n" +
        "Content-Type: text/html; charset=iso-8859-1\r\n\r\n" +
        "World";

    static final byte[] requestEnd = new byte[] {'\r', '\n', '\r', '\n' };

    // Read until the end of a HTTP request
    void readOneRequest(InputStream is) throws IOException {
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

    public void run () {
        try {
            try (Socket s = ss.accept()) {
                s.setSoTimeout(TIMEOUT);
                readOneRequest(s.getInputStream());
                String reply = reply1Part1 + port + reply1Part2;
                s.getOutputStream().write(reply.getBytes());
            }

            /* wait for redirected connection */
            try (Socket s = ss.accept()) {
                s.setSoTimeout(TIMEOUT);
                readOneRequest(s.getInputStream());
                s.getOutputStream().write(reply2.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { ss.close(); } catch (IOException unused) {}
        }
    }
};

public class Redirect307Test {
    public static void main(String[] args) throws Exception {
        ServerSocket sock = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        int port = sock.getLocalPort();
        RedirServer server = new RedirServer(sock);
        server.start();

        URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .toURL();
        URLConnection conURL =  url.openConnection(NO_PROXY);
        conURL.setDoInput(true);
        conURL.setAllowUserInteraction(false);
        conURL.setUseCaches(false);

        try (InputStream in = conURL.getInputStream()) {
            if ((in.read() != (int)'W') || (in.read()!=(int)'o')) {
                throw new RuntimeException ("Unexpected string read");
            }
        }
    }
}
