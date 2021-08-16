/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7129083
 * @library /test/lib
 * @summary Cookiemanager does not store cookies if url is read
 *          before setting cookiemanager
 */

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;

import jdk.test.lib.net.URIBuilder;

public class CookieHttpClientTest implements Runnable {
    final ServerSocket ss;
    static final int TIMEOUT = 10 * 1000;

    static final String replyString = "HTTP/1.1 200 OK\r\n" +
            "Set-Cookie: name=test\r\n" +
            "Content-Length: 10\r\n\r\n" +
            "1234567890";

    // HTTP server, reply with Set-Cookie
    @Override
    public void run() {
        Socket s = null;
        try {
            s = ss.accept();
            s.setSoTimeout(TIMEOUT);
            readOneRequest(s.getInputStream());
            s.getOutputStream().write(replyString.getBytes());

            readOneRequest(s.getInputStream());
            s.getOutputStream().write(replyString.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (s != null) { s.close(); } ss.close(); }
            catch (IOException unused) {  /* gulp!burp! */   }
        }
    }

    static final byte[] requestEnd = new byte[] {'\r', '\n', '\r', '\n' };

    // Read until the end of a HTTP request
    static void readOneRequest(InputStream is) throws IOException {
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

    CookieHttpClientTest() throws Exception {
        /* start the server */
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        (new Thread(this)).start();

        URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(ss.getLocalPort())
                .path("/").toURL();

        // Run without a CookieHandler first
        InputStream in = url.openConnection().getInputStream();
        while (in.read() != -1);  // read response body so connection can be reused

        // Set a CookeHandler and retest using the HttpClient from the KAC
        CookieManager manager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);

        in = url.openConnection().getInputStream();
        while (in.read() != -1);

        if (manager.getCookieStore().getCookies().isEmpty()) {
            throw new RuntimeException("Failed: No cookies in the cookie Handler.");
        }
    }

    public static void main(String args[]) throws Exception {
        new CookieHttpClientTest();
    }
}
