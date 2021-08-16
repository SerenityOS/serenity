/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6550798
 * @library /test/lib
 * @summary Using InputStream.skip with ResponseCache will cause partial data to be cached
 * @modules jdk.httpserver
 * @run main/othervm test
 */

import java.net.*;
import com.sun.net.httpserver.*;
import java.io.*;

import jdk.test.lib.net.URIBuilder;

public class test {

    final static int LEN = 16 * 1024;

    public static void main(String[] args)  throws Exception {

        TestCache.reset();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer s = HttpServer.create(new InetSocketAddress(loopback, 0), 10);
        s.createContext("/", new HttpHandler() {
            public void handle(HttpExchange e) {
                try {
                    byte[] buf = new byte [LEN];
                    OutputStream o = e.getResponseBody();
                    e.sendResponseHeaders(200, LEN);
                    o.write(buf);
                    e.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    TestCache.fail = true;
                }
            }
        });
        s.start();

        System.out.println("http request with cache hander");
        URL u = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(s.getAddress().getPort())
            .path("/f")
            .toURL();
        System.out.println("URL: " + u);
        URLConnection conn = u.openConnection();

        InputStream is = null;
        try {
            // this calls into TestCache.get
            byte[] buf = new byte[8192];
            is = new BufferedInputStream(conn.getInputStream());

            is.skip(1000);

            while (is.read(buf) != -1) {
            }
        } finally {
            if (is != null) {
                // this calls into TestCache.put
                // TestCache.put will check if the resource
                // should be cached
                is.close();
            }
            s.stop(0);
        }

        if (TestCache.fail) {
            System.out.println("TEST FAILED");
            throw new RuntimeException();
        } else {
            System.out.println("TEST OK");
        }
    }
}
