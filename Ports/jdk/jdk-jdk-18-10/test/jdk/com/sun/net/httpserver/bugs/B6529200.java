/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6529200
 * @run main/othervm B6529200
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6529200
 * @summary  lightweight http server does not work with http1.0 clients
 */

import com.sun.net.httpserver.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

public class B6529200 {

    public static void main (String[] args) throws Exception {
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress (loopback, 0);
        HttpServer server = HttpServer.create (addr, 0);
        HttpContext ctx = server.createContext ("/test", handler);

        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor (executor);
        server.start ();

        /* test 1: keep-alive */

        Socket sock = new Socket (loopback, server.getAddress().getPort());
        OutputStream os = sock.getOutputStream();
        System.out.println ("GET /test/foo HTTP/1.0\r\nConnection: keep-alive\r\n\r\n");
        os.write ("GET /test/foo HTTP/1.0\r\nConnection: keep-alive\r\n\r\n".getBytes());
        os.flush();
        InputStream is = sock.getInputStream();
        StringBuffer s = new StringBuffer();
        boolean finished = false;

        sock.setSoTimeout (10 * 1000);
        try {
            while (!finished) {
                char c = (char) is.read();
                s.append (c);
                finished = s.indexOf ("\r\n\r\nhello") != -1;
                /* test will timeout otherwise */
            }
        } catch (SocketTimeoutException e) {
            server.stop (2);
            executor.shutdown ();
            throw new RuntimeException ("Test failed in test1");
        }

        System.out.println (new String (s));

        /* test 2: even though we request keep-alive, server must close
         * because it is sending unknown content length response */

        System.out.println("GET /test/foo HTTP/1.0\r\nConnection: keep-alive\r\n\r\n");
        os.write ("GET /test/foo HTTP/1.0\r\nConnection: keep-alive\r\n\r\n".getBytes());
        os.flush();
        int i=0,c;
        byte [] buf = new byte [8*1024];
        try {
            while ((c=is.read()) != -1) {
                buf[i++] = (byte)c;
            }
        } catch (SocketTimeoutException e) {
            server.stop (2);
            executor.shutdown ();
            throw new RuntimeException ("Test failed in test2");
        }

        String ss = new String (buf, "ISO-8859-1");
        if (ss.indexOf ("\r\n\r\nhello world") == -1) {
            server.stop (2);
            executor.shutdown ();
            throw new RuntimeException ("Test failed in test2: wrong string");
        }
        System.out.println (ss);
        is.close ();
        server.stop (2);
        executor.shutdown();
    }


    static class Handler implements HttpHandler {
        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is;
            OutputStream os;
            switch (invocation++) {
              case 1:
                is = t.getRequestBody();
                while (is.read() != -1) ;
                is.close();
                t.sendResponseHeaders (200, "hello".length());
                os = t.getResponseBody();
                os.write ("hello".getBytes());
                os.close();
                break;
              case 2:
                is = t.getRequestBody();
                while (is.read() != -1) ;
                is.close();
                t.sendResponseHeaders (200, 0);
                os = t.getResponseBody();
                os.write ("hello world".getBytes());
                os.close();
                break;
            }
        }
    }
}
