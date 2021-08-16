/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8012625
 * @library /test/lib
 * @modules jdk.httpserver
 * @run main B8012625
 */

import java.net.*;
import java.io.*;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

import jdk.test.lib.net.URIBuilder;

import com.sun.net.httpserver.*;

public class B8012625 implements HttpHandler {

    public static void main (String[] args) throws Exception {
        B8012625 test = new B8012625();
        test.run();
    }

    public void run() throws Exception {
        URL url = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(port)
            .path("/foo")
            .toURL();
        System.out.println("URL: " + url);
        HttpURLConnection uc = (HttpURLConnection)url.openConnection();
        uc.setDoOutput(true);
        uc.setRequestMethod("POST");
        uc.addRequestProperty("Expect", "100-Continue");
        //uc.setFixedLengthStreamingMode(256);
        System.out.println ("Client: getting outputstream");
        long before = System.currentTimeMillis();
        OutputStream os = uc.getOutputStream();
        long after = System.currentTimeMillis();
        System.out.println ("Client: writing to outputstream");
        byte[] buf = new byte[256];
        os.write(buf);
        System.out.println ("Client: done writing ");
        int r = uc.getResponseCode();
        System.out.println ("Client: received response code " + r);
        server.stop(1);
        ex.shutdownNow();
        if (after - before >= 5000) {
            throw new RuntimeException("Error: 5 second delay seen");
        }
    }

    int port;
    HttpServer server;
    ExecutorService ex;

    public B8012625 () throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        server = HttpServer.create(new InetSocketAddress(loopback, 0), 10);
        HttpContext ctx = server.createContext("/", this);
        ex = Executors.newFixedThreadPool(5);
        server.setExecutor(ex);
        server.start();
        port = server.getAddress().getPort();
   }

    public void handle(HttpExchange ex) throws IOException {
        String s = ex.getRequestMethod();
        if (!s.equals("POST")) {
            ex.getResponseHeaders().set("Allow", "POST");
            ex.sendResponseHeaders(500, -1);
            ex.close();
            return;
        }
        System.out.println ("Server: reading request body");
        InputStream is = ex.getRequestBody();
        // read request
        byte[] buf = new byte [1024];
        while (is.read(buf) != -1) ;
        is.close();
        ex.sendResponseHeaders(200, -1);
        ex.close();
   }
}
