/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8199849
 * @library /test/lib
 * @summary Checks that unicode bytes are being handled correctly
 * @run main/othervm -Dfile.encoding=UTF_8 TestHttpUnicode
 */

import com.sun.net.httpserver.*;
import jdk.test.lib.net.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class TestHttpUnicode {

    private static final String TEST_USER = "Selam D\u00fcnya. Ho\u015f\u00e7akal D\u00fcnya";
    private static final String TEST_PW = "Selam D\u00fcnya. Ho\u015f\u00e7akal D\u00fcnya";

    static class ClientAuthenticator extends java.net.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(TEST_USER, TEST_PW.toCharArray());
        }
    }

    static class Handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            while (is.read() != -1) ;
            is.close();

            HttpPrincipal p = t.getPrincipal();
            if (p.getUsername().equals(TEST_USER)) {
                t.sendResponseHeaders(200, -1);
            }
            t.close();
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServer testHttpServer = null;
        try {
            InetSocketAddress loopbackAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
            testHttpServer = HttpServer.create(loopbackAddress, 0);
            HttpContext context = testHttpServer.createContext("/test", new Handler());
            System.setProperty("http.maxRedirects", "3");

            BasicAuthenticator serverAuthenticator = new BasicAuthenticator("authCharacterSet@test.realm") {
                public boolean checkCredentials(String username, String pw) {
                    return username.equals(TEST_USER) && pw.equals(TEST_PW);
                }
            };
            context.setAuthenticator(serverAuthenticator);
            java.net.Authenticator.setDefault(new ClientAuthenticator());

            testHttpServer.start();
            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(testHttpServer.getAddress().getPort())
                .path("/test/authCharacterSet.html")
                .toURL();
            HttpURLConnection testConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

            // Authenication CHECK
            if (testConnection.getResponseCode() == 401) {
                throw new RuntimeException("Test Authentication failed with HTTP Status 401.");
            }

            InputStream is = testConnection.getInputStream();
            while (is.read() != -1) ;
        } finally {
            testHttpServer.stop(2);
        }
    }
}
