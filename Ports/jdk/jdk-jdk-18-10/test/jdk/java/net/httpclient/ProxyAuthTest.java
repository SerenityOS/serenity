/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163561
 * @modules java.base/sun.net.www
 *          java.net.http
 * @summary Verify that Proxy-Authenticate header is correctly handled
 * @run main/othervm ProxyAuthTest
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.List;
import sun.net.www.MessageHeader;

public class ProxyAuthTest {
    private static final String AUTH_USER = "user";
    private static final String AUTH_PASSWORD = "password";

    public static void main(String[] args) throws Exception {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            int port = ss.getLocalPort();
            MyProxy proxy = new MyProxy(ss);
            (new Thread(proxy)).start();
            System.out.println("Proxy listening port " + port);

            Auth auth = new Auth();
            InetSocketAddress paddr = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);

            URI uri = new URI("http://www.google.ie/");
            CountingProxySelector ps = CountingProxySelector.of(paddr);
            HttpClient client = HttpClient.newBuilder()
                                          .proxy(ps)
                                          .authenticator(auth)
                                          .build();
            HttpRequest req = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<?> resp = client.sendAsync(req, BodyHandlers.discarding()).get();
            if (resp.statusCode() != 404) {
                throw new RuntimeException("Unexpected status code: " + resp.statusCode());
            }

            if (auth.isCalled) {
                System.out.println("Authenticator is called");
            } else {
                throw new RuntimeException("Authenticator is not called");
            }

            if (!proxy.matched) {
                throw new RuntimeException("Proxy authentication failed");
            }
            if (ps.count() > 1) {
                throw new RuntimeException("CountingProxySelector. Expected 1, got " + ps.count());
            }
        }
    }

    static class Auth extends Authenticator {
        private volatile boolean isCalled;

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            System.out.println("scheme: " + this.getRequestingScheme());
            isCalled = true;
            return new PasswordAuthentication(AUTH_USER,
                    AUTH_PASSWORD.toCharArray());
        }
    }

    /**
     * A Proxy Selector that wraps a ProxySelector.of(), and counts the number
     * of times its select method has been invoked. This can be used to ensure
     * that the Proxy Selector is invoked only once per HttpClient.sendXXX
     * invocation.
     */
    static class CountingProxySelector extends ProxySelector {
        private final ProxySelector proxySelector;
        private volatile int count; // 0
        private CountingProxySelector(InetSocketAddress proxyAddress) {
            proxySelector = ProxySelector.of(proxyAddress);
        }

        public static CountingProxySelector of(InetSocketAddress proxyAddress) {
            return new CountingProxySelector(proxyAddress);
        }

        int count() { return count; }

        @Override
        public List<Proxy> select(URI uri) {
            count++;
            return proxySelector.select(uri);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            proxySelector.connectFailed(uri, sa, ioe);
        }
    }

    static class MyProxy implements Runnable {
        final ServerSocket ss;
        private volatile boolean matched;

        MyProxy(ServerSocket ss) {
            this.ss = ss;
        }

        public void run() {
            for (int i = 0; i < 2; i++) {
                try (Socket s = ss.accept();
                     InputStream in = s.getInputStream();
                     OutputStream os = s.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(os));
                     PrintWriter out = new PrintWriter(writer);) {
                    MessageHeader headers = new MessageHeader(in);
                    System.out.println("Proxy: received " + headers);

                    String authInfo = headers.findValue("Proxy-Authorization");
                    if (authInfo != null) {
                        authenticate(authInfo);
                        out.print("HTTP/1.1 404 Not found\r\n");
                        out.print("\r\n");
                        System.out.println("Proxy: 404");
                        out.flush();
                    } else {
                        out.print("HTTP/1.1 407 Proxy Authorization Required\r\n");
                        out.print(
                                "Proxy-Authenticate: Basic realm=\"a fake realm\"\r\n");
                        out.print("\r\n");
                        System.out.println("Proxy: Authorization required");
                        out.flush();
                    }
                } catch (IOException x) {
                    System.err.println("Unexpected IOException from proxy.");
                    x.printStackTrace();
                    break;
                }
            }
        }

        private void authenticate(String authInfo) throws IOException {
            try {
                authInfo.trim();
                int ind = authInfo.indexOf(' ');
                String recvdUserPlusPass = authInfo.substring(ind + 1).trim();
                // extract encoded username:passwd
                String value = new String(
                        Base64.getMimeDecoder().decode(recvdUserPlusPass));
                String userPlusPassword = AUTH_USER + ":" + AUTH_PASSWORD;
                if (userPlusPassword.equals(value)) {
                    matched = true;
                    System.out.println("Proxy: client authentication successful");
                } else {
                    System.err.println(
                            "Proxy: client authentication failed, expected ["
                                    + userPlusPassword + "], actual [" + value
                                    + "]");
                }
            } catch (Exception e) {
                throw new IOException(
                        "Proxy received invalid Proxy-Authorization value: "
                                + authInfo);
            }
        }
    }

}
