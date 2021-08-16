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
 * @run main/othervm -Dsun.net.httpserver.idleInterval=4 Test3
 * @run main/othervm -Djava.net.preferIPv6Addresses=true
 *                   -Dsun.net.httpserver.idleInterval=4 Test3
 */

import com.sun.net.httpserver.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.regex.Pattern.*;
import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.ssl.*;

/**
 * Test pipe-lining over http
 */

public class Test3 extends Test {
    static int count = 1;
    public static void main (String[] args) throws Exception {
        System.out.print ("Test3: ");
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress(loopback, 0);
        HttpServer server = HttpServer.create (addr, 0);
        int port = server.getAddress().getPort();
        HttpContext c2 = server.createContext ("/test", handler);
        c2.getAttributes().put ("name", "This is the http handler");

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
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            int x = invocation ++;
            rmap.set ("XTest", Integer.toString (x));

            switch (x) {
            case 0:
                try {Thread.sleep (2000); } catch (Exception e) {}
                checkBody (is, body1);
                break;
            case 1:
                try {Thread.sleep (1000); } catch (Exception e) {}
                checkBody (is, body2);
                break;
            case 2:
                checkBody (is, body3);
                break;
            case 3:
                checkBody (is, body4);
                break;
            }
            t.sendResponseHeaders (200, -1);
            t.close();
        }
    }

    static void checkBody (InputStream is, String cmp) throws IOException {
        byte [] b = new byte [1024];
        int count = 0, c;
        while ((c=is.read(b, count, b.length-count)) != -1) {
            count+=c;
        }
        is.close();
        String s = new String (b, 0, count, "ISO8859_1");
        if (!s.equals (cmp)) {
            throw new RuntimeException ("strings not equal");
        }
    }

    static String body1 = "1234567890abcdefghij";
    static String body2 = "2234567890abcdefghij0123456789";
    static String body3 = "3wertyuiop";
    static String body4 = "4234567890";

    static String result =
        "HTTP/1.1 200 OK.*Xtest: 0.*"+
        "HTTP/1.1 200 OK.*Xtest: 1.*"+
        "HTTP/1.1 200 OK.*Xtest: 2.*"+
        "HTTP/1.1 200 OK.*Xtest: 3.*";

    public static void doClient (int port) throws Exception {
        String s = "GET /test/1.html HTTP/1.1\r\nContent-length: 20\r\n"+
        "\r\n" +body1 +
        "GET /test/2.html HTTP/1.1\r\nContent-length: 30\r\n"+
        "\r\n"+ body2 +
        "GET /test/3.html HTTP/1.1\r\nContent-length: 10\r\n"+
        "\r\n"+ body3 +
        "GET /test/4.html HTTP/1.1\r\nContent-length: 10\r\n"+
        "\r\n"+body4;

        Socket socket = new Socket (InetAddress.getLoopbackAddress(), port);
        OutputStream os = socket.getOutputStream();
        os.write (s.getBytes());
        InputStream is = socket.getInputStream();
        int c, count=0;
        byte[] b = new byte [1024];
        while ((c=is.read(b, count, b.length-count)) != -1) {
            count +=c;
        }
        is.close();
        socket.close();
        s = new String (b,0,count, "ISO8859_1");
        if (!compare (s, result)) {
            throw new RuntimeException ("wrong string result");
        }
    }

    static boolean compare (String s, String result) {
        Pattern pattern = Pattern.compile (result,
                Pattern.DOTALL|Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher (s);
        return matcher.matches();
    }
}
