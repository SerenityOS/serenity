/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4921848
 * @library /test/lib
 * @run main/othervm -Dhttp.auth.preference=basic B4921848
 * @run main/othervm -Djava.net.preferIPv6Addresses=true
 *                   -Dhttp.auth.preference=basic B4921848
 * @summary Allow user control over authentication schemes
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;

public class B4921848 implements HttpHandler {

    static int count = 0;

    public void handle (HttpExchange req) {
        try {
            if (count == 0 ) {
                req.getResponseHeaders().set("Connection", "close");
                req.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"foo\"");
                req.getResponseHeaders().add("WWW-Authenticate", "Digest realm=\"bar\" domain=/biz nonce=\"hereisanonce\"");
                req.sendResponseHeaders(401, -1);
            } else {
                String authheader = req.getRequestHeaders().get("Authorization").get(0);
                if (authheader.startsWith ("Basic")) {
                    req.sendResponseHeaders(200, 0);
                    try(PrintWriter pw = new PrintWriter(req.getResponseBody())) {
                        pw.print("Hello .");
                    }
                } else {
                    req.sendResponseHeaders(400, -1);
                }
            }
            count ++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void read (InputStream is) throws IOException {
        int c;
        System.out.println ("reading");
        while ((c=is.read()) != -1) {
            System.out.write (c);
        }
        System.out.println ("");
        System.out.println ("finished reading");
    }


    static void client (String u) throws Exception {
        URL url = new URL (u);
        System.out.println ("client opening connection to: " + u);
        URLConnection urlc = url.openConnection ();
        InputStream is = urlc.getInputStream ();
        read (is);
        is.close();
    }

    static HttpServer server;

    public static void main (String[] args) throws Exception {
        B4921848 b4921848 = new B4921848();
        MyAuthenticator auth = new MyAuthenticator ();
        Authenticator.setDefault (auth);
        ProxySelector.setDefault(ProxySelector.of(null)); // no proxy
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            server = HttpServer.create(new InetSocketAddress(loopback, 0), 10);
            server.createContext("/", b4921848);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            String serverURL = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(server.getAddress().getPort())
                .path("/")
                .build()
                .toString();
            System.out.println("Server: listening at: " + serverURL);
            client(serverURL + "d1/d2/d3/foo.html");
        } catch (Exception e) {
            if (server != null) {
                server.stop(1);
            }
            throw e;
        }
        server.stop(1);
    }

    public static void except (String s) {
        server.stop(1);
        throw new RuntimeException (s);
    }

    static class MyAuthenticator extends Authenticator {
        MyAuthenticator () {
            super ();
        }

        public PasswordAuthentication getPasswordAuthentication () {
            return (new PasswordAuthentication ("user", "passwordNotCheckedAnyway".toCharArray()));
        }

    }

}
