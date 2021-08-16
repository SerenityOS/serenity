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
 * @bug 6341616
 * @library /test/lib
 * @run main B6341616
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6341616
 * @summary  Server doesnt send response if there is a RuntimeException in validate of BasicAuthFilter
 */

import com.sun.net.httpserver.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import jdk.test.lib.net.URIBuilder;

public class B6341616 {

    public static void main (String[] args) throws Exception {
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress (loopback, 0);
        HttpServer server = HttpServer.create (addr, 0);
        HttpContext ctx = server.createContext ("/test", handler);
        BasicAuthenticator filter = new BasicAuthenticator ("foobar@test.realm") {
            public boolean checkCredentials (String username, String pw) {
                throw new RuntimeException ("");
            }
        };

        ctx.setAuthenticator (filter);
        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor (executor);
        server.start ();
        java.net.Authenticator.setDefault (new MyAuthenticator());

        URL url = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(server.getAddress().getPort())
            .path("/test/foo.html")
            .toURL();
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
        try {
            InputStream is = urlc.getInputStream();
            int c = 0;
            while (is.read()!= -1) {
                c ++;
            }
        } catch (IOException e) {
            server.stop(2);
            executor.shutdown();
            System.out.println ("OK");
        }
    }

    public static boolean error = false;

    static class MyAuthenticator extends java.net.Authenticator {
        public PasswordAuthentication getPasswordAuthentication () {
            PasswordAuthentication pw;
            if (!getRequestingPrompt().equals ("foobar@test.realm")) {
                B6341616.error = true;
            }
            pw = new PasswordAuthentication ("fred", "xyz".toCharArray());
            return pw;
        }
    }

    static class Handler implements HttpHandler {
        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            while (is.read () != -1) ;
            is.close();
            t.sendResponseHeaders (200, -1);
            t.close();
        }
    }
}
