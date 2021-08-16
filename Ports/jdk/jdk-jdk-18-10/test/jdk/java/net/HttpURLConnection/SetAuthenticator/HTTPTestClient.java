/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/**
 * A simple Http client that connects to the HTTPTestServer.
 * @author danielfuchs
 */
public class HTTPTestClient extends HTTPTest {

    public static void connect(HttpProtocolType protocol,
                               HTTPTestServer server,
                               HttpAuthType authType,
                               Authenticator auth)
            throws IOException {

        InetSocketAddress address = server.getAddress();
        final URL url = url(protocol,  address, "/");
        final Proxy proxy = proxy(server, authType);

        System.out.println("Client: FIRST request: " + url + " GET");
        HttpURLConnection conn = openConnection(url, authType, proxy);
        configure(conn, auth);
        System.out.println("Response code: " + conn.getResponseCode());
        String result = new String(conn.getInputStream().readAllBytes(), "UTF-8");
        System.out.println("Response body: " + result);
        if (!result.isEmpty()) {
            throw new RuntimeException("Unexpected response to GET: " + result);
        }
        System.out.println("\nClient: NEXT request: " + url + " POST");
        conn = openConnection(url, authType, proxy);
        configure(conn, auth);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.getOutputStream().write("Hello World!".getBytes("UTF-8"));
        System.out.println("Response code: " + conn.getResponseCode());
        result = new String(conn.getInputStream().readAllBytes(), "UTF-8");
        System.out.println("Response body: " + result);
        if ("Hello World!".equals(result)) {
            System.out.println("Test passed!");
        } else {
            throw new RuntimeException("Unexpected response to POST: " + result);
        }
    }

    private static void configure(HttpURLConnection conn, Authenticator auth)
        throws IOException {
        if (auth != null) {
            conn.setAuthenticator(auth);
        }
        if (conn instanceof HttpsURLConnection) {
            System.out.println("Client: configuring SSL connection");
            // We have set a default SSLContext so we don't need to do
            // anything here. Otherwise it could look like:
            //     HttpsURLConnection httpsConn = (HttpsURLConnection)conn;
            //     httpsConn.setSSLSocketFactory(
            //               new SimpleSSLContext().get().getSocketFactory());
        }
    }

}
