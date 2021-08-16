/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import sun.net.www.protocol.http.ntlm.NTLMAuthenticationCallback;

/*
 * @test
 * @bug 8137174
 * @modules java.base/sun.net.www.protocol.http.ntlm
 *          jdk.httpserver
 * @summary Checks if NTLM auth works fine if security manager set
 * @run main/othervm/java.security.policy=NTLMAuthWithSM.policy NTLMAuthWithSM
 */
public class NTLMAuthWithSM {

    public static void main(String[] args) throws Exception {
        // security manager is required
        if (System.getSecurityManager() == null) {
            throw new RuntimeException("Security manager not specified");
        }

        if (System.getProperty("os.name").startsWith("Windows")) {
            // disable transparent NTLM authentication on Windows
            NTLMAuthenticationCallback.setNTLMAuthenticationCallback(
                    new NTLMAuthenticationCallbackImpl());
        }

        try (LocalHttpServer server = LocalHttpServer.startServer()) {
            // set authenticator
            Authenticator.setDefault(new AuthenticatorImpl());

            String url = String.format("http://%s/test/",
                    server.getAuthority());

            // load a document which is protected with NTML authentication
            System.out.println("load() called: " + url);
            URLConnection conn = new URL(url).openConnection();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {

                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("Couldn't read a response");
                }
                do {
                    System.out.println(line);
                } while ((line = reader.readLine()) != null);
            }
        }

        System.out.println("Test passed");
    }

    private static class AuthenticatorImpl extends Authenticator {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            System.out.println("getPasswordAuthentication() called, scheme: "
                    + getRequestingScheme());
            if (getRequestingScheme().equalsIgnoreCase("ntlm")) {
                return new PasswordAuthentication("test", "test".toCharArray());
            }
            return null;
        }
    }

    // local http server which pretends to support NTLM auth
    static class LocalHttpServer implements HttpHandler, AutoCloseable {

        private final HttpServer server;

        private LocalHttpServer(HttpServer server) {
            this.server = server;
        }

        static LocalHttpServer startServer() throws IOException {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            HttpServer httpServer = HttpServer.create(
                    new InetSocketAddress(loopback, 0), 0);
            LocalHttpServer localHttpServer = new LocalHttpServer(httpServer);
            localHttpServer.start();

            return localHttpServer;
        }

        void start() {
            server.createContext("/test", this);
            server.start();
            System.out.println("HttpServer: started on port " + getPort());
        }

        void stop() {
            server.stop(0);
            System.out.println("HttpServer: stopped");
        }

        String getAuthority() {
            InetAddress address = server.getAddress().getAddress();
            String hostaddr = address.isAnyLocalAddress()
                   ? "localhost" : address.getHostAddress();
            if (hostaddr.indexOf(':') > -1) hostaddr = "[" + hostaddr + "]";
            return hostaddr + ":" + getPort();
        }

        int getPort() {
            return server.getAddress().getPort();
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println("HttpServer: handle connection");

            // read a request
            try (InputStream is = t.getRequestBody()) {
                while (is.read() > 0);
            }

            try {
                List<String> headers = t.getRequestHeaders()
                        .get("Authorization");
                if (headers != null && !headers.isEmpty()
                        && headers.get(0).trim().contains("NTLM")) {
                    byte[] output = "hello".getBytes();
                    t.sendResponseHeaders(200, output.length);
                    t.getResponseBody().write(output);
                    System.out.println("HttpServer: return 200");
                } else {
                    t.getResponseHeaders().set("WWW-Authenticate", "NTLM");
                    byte[] output = "forbidden".getBytes();
                    t.sendResponseHeaders(401, output.length);
                    t.getResponseBody().write(output);
                    System.out.println("HttpServer: return 401");
                }
            } catch (IOException e) {
                System.out.println("HttpServer: exception: " + e);
                System.out.println("HttpServer: return 500");
                t.sendResponseHeaders(500, 0);
            } finally {
                t.close();
            }
        }

        @Override
        public void close() {
            stop();
        }
    }

    private static class NTLMAuthenticationCallbackImpl
            extends NTLMAuthenticationCallback {

        // don't trust any site, so that no transparent NTLM auth happens
        @Override
        public boolean isTrustedSite(URL url) {
            System.out.println(
                    "NTLMAuthenticationCallbackImpl.isTrustedSite() called: "
                        + "return false");
            return false;
        }
    }
}
