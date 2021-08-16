/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6421581
 * @summary NPE while closing HttpExchange.getResonseBody()
 */

import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class B6421581 {

    static boolean error = false;
    static int iter = 0;

    public static void main(String[] args) throws Exception {
                once();
    }

    public static void once() throws Exception {
        InetSocketAddress inetAddress = new InetSocketAddress(
            "localhost", 0);
        HttpServer server = HttpServer.create(inetAddress, 5);
        int port = server.getAddress().getPort();
        ExecutorService e = (Executors.newFixedThreadPool(5));
        server.setExecutor(e);
        HttpContext context = server.createContext("/hello");
        server.start();
        context.setHandler(new HttpHandler() {
            public void handle(HttpExchange msg) {
                iter ++;
                System.out.println("Got request");
                switch (iter) {
                case 1:
                    /* close output stream without opening inpustream */
                    /* chunked encoding */
                    try {
                        msg.sendResponseHeaders(200, 0);
                        OutputStream out = msg.getResponseBody();
                        out.write("hello".getBytes());
                        out.close();
                    } catch(Exception e) {
                        error = true;
                    } finally {
                        msg.close();
                    }
                    break;
                case 2:
                    /* close output stream without opening inpustream */
                    /* fixed encoding */
                    try {
                        msg.sendResponseHeaders(200, 5);
                        OutputStream out = msg.getResponseBody();
                        out.write("hello".getBytes());
                        out.close();
                    } catch(Exception e) {
                        error = true;
                    } finally {
                        msg.close();
                    }
                    break;
                case 3:
                    /* close exchange without opening any stream */
                    try {
                        msg.sendResponseHeaders(200, -1);
                        msg.close();
                    } catch(Exception e) {
                        error = true;
                    }
                    break;
                }
            }
        });

        URL url = new URL("http://localhost:"+port+"/hello/url.text");
        doURL(url);
        doURL(url);
        doURL(url);
        e.shutdown();
        e.awaitTermination(4, TimeUnit.SECONDS);
        server.stop(0);
        if (error) {
            throw new RuntimeException ("test failed");
        }
    }

    static void doURL (URL url) throws Exception {
        InputStream is = url.openStream();
        while (is.read() != -1) ;
        is.close();
    }
}
