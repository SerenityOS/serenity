/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6439651
 * @modules jdk.httpserver
 * @run main/othervm UserAuth
 * @summary Sending "Cookie" header with JRE 1.5.0_07 doesn't work anymore
 */

import java.net.*;
import com.sun.net.httpserver.*;
import java.util.*;
import java.io.*;
import static java.net.Proxy.NO_PROXY;

public class UserCookie
{
    com.sun.net.httpserver.HttpServer httpServer;

    public static void main(String[] args) {
        new UserCookie();
    }

    public UserCookie() {
        try {
            startHttpServer();
            doClient();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    void doClient() {
        try {
            // set default CookieHandler to accept only accepts cookies from original server.
            CookieHandler.setDefault(new CookieManager());

            InetSocketAddress address = httpServer.getAddress();

            URL url = new URL("http://" + address.getHostName() + ":" + address.getPort() + "/test/");
            HttpURLConnection uc = (HttpURLConnection)url.openConnection(NO_PROXY);
            uc.setRequestProperty("Cookie", "value=ValueDoesNotMatter");
            int resp = uc.getResponseCode();

            System.out.println("Response Code is " + resp);
            if (resp != 200)
                throw new RuntimeException("Failed: Cookie header was not retained");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpServer.stop(1);
        }
    }

     /**
     * Http Server
     */
    void startHttpServer() throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        if (!InetAddress.getByName(address.getHostName()).equals(address)) {
            // if this happens then we should possibly change the client
            // side to use the address literal in its URL instead of
            // the host name.
            throw new IOException(address.getHostName()
                                  + " resolves to "
                                  + InetAddress.getByName(address.getHostName())
                                  + " not to "
                                  + address + ": check host configuration.");
        }

        httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(address, 0), 0);

        // create HttpServer context
        HttpContext ctx = httpServer.createContext("/test/", new MyHandler());

        httpServer.start();
    }

    class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Headers reqHeaders = t.getRequestHeaders();

            List<String> cookie = reqHeaders.get("Cookie");

            if (cookie == null || !cookie.get(0).equals("value=ValueDoesNotMatter"))
                t.sendResponseHeaders(400, -1);

            t.sendResponseHeaders(200, -1);
            t.close();
        }
    }



}
