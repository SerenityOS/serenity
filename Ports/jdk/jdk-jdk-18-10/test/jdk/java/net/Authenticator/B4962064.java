/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4962064
 * @run main/othervm B4962064
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B4962064
 * @summary Extend Authenticator to provide access to request URI and server/proxy
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class B4962064 implements HttpHandler {

    static int count = 0;

    public void handle (HttpExchange req) {
        try {
            switch (count) {
              case 0:
                req.getResponseHeaders().set("Connection", "close");
                req.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"foo\"");
                req.sendResponseHeaders(401, -1);
                break;
              case 1:
              case 3:
                req.sendResponseHeaders(200, 0);
                try(PrintWriter pw = new PrintWriter(req.getResponseBody())) {
                    pw.print("Hello .");
                }
                break;
              case 2:
                req.getResponseHeaders().set("Connection", "close");
                req.getResponseHeaders().add("Proxy-Authenticate", "Basic realm=\"foo\"");
                req.sendResponseHeaders(407, -1);
                break;
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
    static URL urlsave;

    public static void main (String[] args) throws Exception {
        B4962064 b4962064 = new B4962064();
        try {
            InetAddress address = InetAddress.getLoopbackAddress();
            InetAddress resolved = InetAddress.getByName(address.getHostName());
            System.out.println("Lookup: " + address + " -> \""
                               + address.getHostName() + "\" -> "
                               + resolved);
            server = HttpServer.create(new InetSocketAddress(address, 0), 10);
            server.createContext("/", b4962064);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            int port = server.getAddress().getPort();
            String proxyHost = address.equals(resolved)
                ? address.getHostName()
                : address.getHostAddress();
            System.setProperty ("http.proxyHost", proxyHost);
            System.setProperty ("http.proxyPort", Integer.toString (port));
            MyAuthenticator auth = new MyAuthenticator ();
            Authenticator.setDefault (auth);
            System.out.println ("Server started: listening on port: " + port);
            String s = new String ("http://foo.com/d1/d2/d3/foo.html");
            urlsave = new URL (s);
            client (s);
            s = new String ("http://bar.com/dr/d3/foo.html");
            urlsave = new URL (s);
            client (s);
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
        int count = 0;
        MyAuthenticator () {
            super ();
        }

        public PasswordAuthentication getPasswordAuthentication () {
            URL url = getRequestingURL ();
            if (!url.equals (urlsave)) {
                except ("urls not equal");
            }
            Authenticator.RequestorType expected;
            if (count == 0) {
                expected = Authenticator.RequestorType.SERVER;
            } else {
                expected = Authenticator.RequestorType.PROXY;
            }
            if (getRequestorType() != expected) {
                except ("wrong authtype");
            }
            count ++;
            return (new PasswordAuthentication ("user", "passwordNotCheckedAnyway".toCharArray()));
        }

    }

}
