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
 * @bug 6431193
 * @library /test/lib
 * @summary  The new HTTP server exits immediately
 * @run main B6431193
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6431193
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import jdk.test.lib.net.URIBuilder;

import com.sun.net.httpserver.*;

public class B6431193 {

    static boolean error = false;

    public static void read (InputStream i) throws IOException {
        while (i.read() != -1);
        i.close();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        class MyHandler implements HttpHandler {
            public void handle(HttpExchange t) throws IOException {
                InputStream is = t.getRequestBody();
                read(is);
                // .. read the request body
                    String response = "This is the response";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                error = Thread.currentThread().isDaemon();
            }
        }


        HttpServer server;
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            server = HttpServer.create(new InetSocketAddress(loopback, 0), 10);

            server.createContext("/apps", new MyHandler());
            server.setExecutor(null);
            // creates a default executor
                server.start();
            int port = server.getAddress().getPort();
            String s = "http://localhost:"+port+"/apps/foo";
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .loopback()
                      .port(port)
                      .path("/apps/foo")
                      .toURL();
            InputStream is = url.openConnection(Proxy.NO_PROXY).getInputStream();
            read (is);
            server.stop (1);
            if (error) {
                throw new RuntimeException ("error in test");
            }

        }
        catch (Exception e) {
            throw new AssertionError("Unexpected exception: " + e, e);
        }
    }
}
