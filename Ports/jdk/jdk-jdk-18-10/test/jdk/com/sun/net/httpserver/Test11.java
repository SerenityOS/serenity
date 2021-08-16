/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6270015
 * @summary  Light weight HTTP server
 * @library /test/lib
 * @run main Test11
 * @run main/othervm -Djava.net.preferIPv6Addresses=true Test11
 */

import java.net.*;
import java.util.concurrent.*;
import java.io.*;
import com.sun.net.httpserver.*;
import jdk.test.lib.net.URIBuilder;

public class Test11 {
    static class Handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            read (t.getRequestBody());
            String response = "response";
            t.sendResponseHeaders (200, response.length());
            OutputStream os = t.getResponseBody();
            os.write (response.getBytes ("ISO8859_1"));
            t.close();
        }

        void read (InputStream is ) throws IOException {
            byte[] b = new byte [8096];
            while (is.read (b) != -1) {}
        }
    }

    public static void main (String[] args) throws Exception {
        System.out.print ("Test 11: ");
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        ExecutorService s = Executors.newCachedThreadPool();
        try {
            HttpContext ctx = server.createContext (
                "/foo/bar/", new Handler ()
            );
            s =  Executors.newCachedThreadPool();
            server.start ();
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .loopback()
                      .port(server.getAddress().getPort())
                      .path("/Foo/bar/test.html")
                      .toURL();
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            int r = urlc.getResponseCode();
            if (r == 200) {
                throw new RuntimeException ("wrong response received");
            }
            System.out.println ("OK");
        } finally {
            s.shutdown();
            server.stop(2);
        }
    }
}
