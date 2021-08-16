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
 * @bug 6648001
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main/othervm/timeout=20 -ea:sun.net.www.protocol.http.AuthenticationInfo -Dhttp.auth.serializeRequests=true Deadlock
 * @run main/othervm/timeout=20 -Djava.net.preferIPv6Addresses=true
 *                              -ea:sun.net.www.protocol.http.AuthenticationInfo -Dhttp.auth.serializeRequests=true Deadlock
 * @summary  cancelling HTTP authentication causes deadlock
 */

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;

public class Deadlock {

    public static void main (String[] args) throws Exception {
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress (loopback, 0);
        HttpServer server = HttpServer.create(addr, 0);
        HttpContext ctx = server.createContext("/test", handler);
        BasicAuthenticator a = new BasicAuthenticator("foobar@test.realm") {
            @Override
            public boolean checkCredentials (String username, String pw) {
                return "fred".equals(username) && pw.charAt(0) == 'x';
            }
        };

        ctx.setAuthenticator(a);
        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start ();
        java.net.Authenticator.setDefault(new MyAuthenticator());

        System.out.print("Deadlock: " );
        for (int i=0; i<2; i++) {
            Runner t = new Runner(server, i);
            t.start();
            t.join();
        }
        server.stop(2);
        executor.shutdown();
        if (error) {
            throw new RuntimeException("test failed error");
        }

        if (count != 2) {
            throw new RuntimeException("test failed count = " + count);
        }
        System.out.println("OK");

    }

    static class Runner extends Thread {
        HttpServer server;
        int i;
        Runner(HttpServer s, int i) {
            server = s;
            this.i = i;
        }

        @Override
        public void run() {
            URL url;
            HttpURLConnection urlc;
            try {
                url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(server.getAddress().getPort())
                    .path("/test/foo.html")
                    .toURLUnchecked();
                urlc = (HttpURLConnection)url.openConnection (Proxy.NO_PROXY);
            } catch (IOException e) {
                error = true;
                return;
            }
            InputStream is = null;
            try {
                is = urlc.getInputStream();
                while (is.read()!= -1) {}
            } catch (IOException e) {
                if (i == 1) error = true;
            } finally {
                if (is != null) try { is.close(); } catch (IOException e) {}
            }
        }
    }

    public static boolean error = false;
    public static int count = 0;

    static class MyAuthenticator extends java.net.Authenticator {
        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            PasswordAuthentication pw;
            if (!getRequestingPrompt().equals("foobar@test.realm")) {
                Deadlock.error = true;
            }
            if (count == 0) {
                pw = null;
            } else {
                pw = new PasswordAuthentication("fred", "xyz".toCharArray());
            }
            count++;
            return pw;
        }
    }

    static class Handler implements HttpHandler {
        int invocation = 1;

        @Override
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            while (is.read() != -1);
            is.close();
            t.sendResponseHeaders(200, -1);
            HttpPrincipal p = t.getPrincipal();
            if (!p.getUsername().equals("fred")) {
                error = true;
            }
            if (!p.getRealm().equals("foobar@test.realm")) {
                error = true;
            }
            t.close();
        }
    }
}
