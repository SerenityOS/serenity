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

/* @test
 * @summary Fixed a potential NullPointerException when setting a ResponseCache that returns a null CacheRequest
 * @bug 4837267
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main Test
 * @run main/othervm -Djava.net.preferIPv6Addresses=true Test
 * @author Michael McMahon
 */

import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.util.*;

import jdk.test.lib.net.URIBuilder;

public class Test
{

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            byte[] b = new byte[1024];
            int r = 0;
            InputStream is = t.getRequestBody();
            while (is.read(b) != -1) ;
            String response = "This is the response";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public static void main(String args[])  throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        server.createContext("/", new MyHandler());
        server.start();
        ResponseCache bak = ResponseCache.getDefault();

        try {
            ResponseCache.setDefault(new ResponseCache() {
                public CacheResponse get(URI uri, String rqstMethod, Map<String,List<String>> rqstHeaders)
                    throws IOException {
                    return null;
                }
                public CacheRequest put(URI uri, URLConnection conn)  throws IOException
                {
                    return null;
                }
            });

            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .host(server.getAddress().getAddress())
                      .port(server.getAddress().getPort())
                      .path("/")
                      .toURL();
            URLConnection urlc = url.openConnection(Proxy.NO_PROXY);
            InputStream is = urlc.getInputStream();
            while (is.read() != -1) ;
            is.close();
        } finally {
            ResponseCache.setDefault(bak);
            server.stop(0);
        }
    }
}
