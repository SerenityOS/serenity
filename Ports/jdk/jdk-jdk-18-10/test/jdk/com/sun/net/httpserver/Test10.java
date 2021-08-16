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
 * @bug 7005016
 * @summary  pit jdk7 b121  sqe test jhttp/HttpServer150013 failing
 * @run main/othervm -Dsun.net.httpserver.clockTick=1000 -Dsun.net.httpserver.idleInterval=3 Test10
 * @run main/othervm -Dsun.net.httpserver.clockTick=1000 -Dsun.net.httpserver.idleInterval=3
 *                   -Djava.net.preferIPv6Addresses Test10
 */

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/*
 * Test handling of empty Http headers
 */

public class Test10 extends Test {
    public static void main (String[] args) throws Exception {
        System.out.print ("Test10: ");
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress(loopback, 0);
        HttpServer server = HttpServer.create (addr, 0);
        int port = server.getAddress().getPort();
        HttpContext c2 = server.createContext ("/test", handler);

        ExecutorService exec = Executors.newCachedThreadPool();
        server.setExecutor (exec);
        try {
            server.start ();
            doClient(port);
            System.out.println ("OK");
        } finally {
            delay();
            if (server != null)
                server.stop(2);
            if (exec != null)
                exec.shutdown();
        }
    }

    static class Handler implements HttpHandler {
        volatile int invocation = 0;
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            while (is.read() != -1);
            Headers map = t.getRequestHeaders();
            t.sendResponseHeaders (200, -1);
            t.close();
        }
    }

    public static void doClient (int port) throws Exception {
        String s = "GET /test/1.html HTTP/1.1\r\n\r\n";

        Socket socket = new Socket (InetAddress.getLoopbackAddress(), port);
        OutputStream os = socket.getOutputStream();
        os.write (s.getBytes());
        socket.setSoTimeout (10 * 1000);
        InputStream is = socket.getInputStream();
        int c;
        byte[] b = new byte [1024];
        while ((c=is.read(b)) != -1) ;
        is.close();
        socket.close();
    }
}
