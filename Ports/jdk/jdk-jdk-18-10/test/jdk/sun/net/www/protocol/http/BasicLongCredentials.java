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
 * @bug 6947917
 * @modules jdk.httpserver
 * @summary  Error in basic authentication when user name and password are long
 * @library /test/lib
 * @run main BasicLongCredentials
 * @run main/othervm -Djava.net.preferIPv6Addresses=true BasicLongCredentials
 */

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;

import jdk.test.lib.net.URIBuilder;

public class BasicLongCredentials {

    static final String USERNAME = "ThisIsMyReallyReallyReallyReallyReallyReally" +
                                   "LongFirstNameDotLastNameAtCompanyEmailAddress";
    static final String PASSWORD = "AndThisIsALongLongLongLongLongLongLongLongLong" +
                                   "LongLongLongLongLongLongLongLongLongPassword";
    static final String REALM = "foobar@test.realm";

    public static void main (String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        try {
            Handler handler = new Handler();
            HttpContext ctx = server.createContext("/test", handler);

            BasicAuthenticator a = new BasicAuthenticator(REALM) {
                public boolean checkCredentials (String username, String pw) {
                    return USERNAME.equals(username) && PASSWORD.equals(pw);
                }
            };
            ctx.setAuthenticator(a);
            server.start();

            Authenticator.setDefault(new MyAuthenticator());

            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .host(server.getAddress().getAddress())
                      .port(server.getAddress().getPort())
                      .path("/test/")
                      .toURL();
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            InputStream is = urlc.getInputStream();
            int c = 0;
            while (is.read()!= -1) { c ++; }

            if (c != 0) { throw new RuntimeException("Test failed c = " + c); }
            if (error) { throw new RuntimeException("Test failed: error"); }

            System.out.println ("OK");
        } finally {
            server.stop(0);
        }
    }

    public static boolean error = false;

    static class MyAuthenticator extends java.net.Authenticator {
        @Override
        public PasswordAuthentication getPasswordAuthentication () {
            if (!getRequestingPrompt().equals(REALM)) {
                BasicLongCredentials.error = true;
            }
            return new PasswordAuthentication (USERNAME, PASSWORD.toCharArray());
        }
    }

    static class Handler implements HttpHandler {
        public void handle (HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            while (is.read () != -1) ;
            is.close();
            t.sendResponseHeaders(200, -1);
            HttpPrincipal p = t.getPrincipal();
            if (!p.getUsername().equals(USERNAME)) {
                error = true;
            }
            if (!p.getRealm().equals(REALM)) {
                error = true;
            }
            t.close();
        }
    }
}
