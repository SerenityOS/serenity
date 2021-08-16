/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211420
 * @library /test/lib
 * @run main/othervm B8211420
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B8211420
 * @summary
 */

import com.sun.net.httpserver.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.io.*;
import java.net.*;

import jdk.test.lib.net.URIBuilder;

public class B8211420 {

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger("com.sun.net.httpserver");
        ConsoleHandler c = new ConsoleHandler();
        c.setLevel(Level.WARNING);
        logger.addHandler(c);
        logger.setLevel(Level.WARNING);
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress(loopback, 0);
        HttpServer server = HttpServer.create(addr, 0);
        HttpContext ctx = server.createContext("/test", handler);
        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();

        URL url = URIBuilder.newBuilder()
                            .scheme("http")
                            .host(server.getAddress().getAddress())
                            .port(server.getAddress().getPort())
                            .path("/test/foo.html")
                            .toURL();
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
        try {
            InputStream is = urlc.getInputStream();
            while (is.read()!= -1) ;
            is.close ();
            String prop = urlc.getHeaderField("Content-length");
                System.out.println("Content-length = " + prop + " should be null");
            if (prop != null)
                throw new RuntimeException("Content-length was present");

            urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            is = urlc.getInputStream();
            while (is.read()!= -1) ;
            is.close();
            if (urlc.getResponseCode() != 304) // expected for 2nd test
                throw new RuntimeException("wrong response code");
            String clen = urlc.getHeaderField("Content-length");
            System.out.println("Content-length = " + clen + " should be 99");
            System.out.println("len = " + clen.length());
            if (clen == null || !clen.equals("99"))
                throw new RuntimeException("Content-length not present or has wrong value");
            System.out.println("OK");
        } finally {
            server.stop(2);
            executor.shutdown();
        }
    }

    public static boolean error = false;

    static class Handler implements HttpHandler {
        volatile int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            while (is.read() != -1) ;
            is.close();
            if (invocation++ == 1) {
                // send a 204 response with no body
                t.sendResponseHeaders(204, -1);
                t.close();
            } else {
                // send a 304 response with no body but with content - length
                rmap.add("Content-length", "99");
                t.sendResponseHeaders(304, -1);
                t.close();
            }
        }
    }
}
