/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8004502
 * @library /test/lib
 * @summary Sanity check that NTLM will not be selected by the http protocol
 *    handler when running on a profile that does not support NTLM
 * @modules java.base/sun.net.www
 *          java.base/sun.net.www.protocol.http:open
 * @run main/othervm NoNTLM
 * @run main/othervm -Djava.net.preferIPv6Addresses=true NoNTLM
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import jdk.test.lib.net.URIBuilder;
import sun.net.www.MessageHeader;

public class NoNTLM {

    static final String CRLF = "\r\n";

    static final String OKAY =
        "HTTP/1.1 200" + CRLF +
        "Content-Length: 0" + CRLF +
        "Connection: close" + CRLF +
        CRLF;

    static class Client implements Runnable {
        private final URL url;
        private volatile IOException ioe;
        private volatile int respCode;

        Client(int port) throws IOException {
            this.url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .path("/foo.html")
                .toURLUnchecked();
            System.out.println("Client URL: " + this.url);
        }

        public void run() {
            try {
                HttpURLConnection uc =
                    (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
                try {
                    uc.getInputStream();
                } catch (IOException x) {
                    respCode = uc.getResponseCode();
                    throw x;
                }
                uc.disconnect();
            } catch (IOException x) {
                if (respCode == 0)
                    respCode = -1;
                ioe = x;
            }
        }

        IOException ioException() {
            return ioe;
        }

        int respCode() {
            return respCode;
        }

        static void start(int port) throws IOException {
            Client client = new Client(port);
            new Thread(client).start();
        }
    }

    /**
     * Return the http response with WWW-Authenticate headers for the given
     * authentication schemes.
     */
    static String authReplyFor(String... schemes) {
        // construct the server reply
        String reply = "HTTP/1.1 401 Unauthorized" + CRLF +
                       "Content-Length: 0"+ CRLF +
                       "Connection: close" + CRLF;
        for (String s: schemes) {
            switch (s) {
                case "Basic" :
                    reply += "WWW-Authenticate: Basic realm=\"wallyworld\"" + CRLF;
                    break;
                case "Digest" :
                    reply += "WWW-Authenticate: Digest" +
                             " realm=\"wallyworld\"" +
                             " domain=/" +
                             " nonce=\"abcdefghijklmnopqrstuvwxyz\"" +
                             " qop=\"auth\"" + CRLF;
                    break;
                case "NTLM" :
                    reply += "WWW-Authenticate: NTLM" + CRLF;
                    break;
                default :
                    throw new RuntimeException("Should not get here");
            }
        }
        reply += CRLF;
        return reply;
    }

    /**
     * Test the http protocol handler with the given authentication schemes
     * in the WWW-Authenticate header.
     */
    static void test(String... schemes) throws IOException {

        // the authentication scheme that the client is expected to choose
        String expected = null;
        for (String s: schemes) {
            if (expected == null) {
                expected = s;
            } else if (s.equals("Digest")) {
                expected = s;
            }
        }

        // server reply
        String reply = authReplyFor(schemes);

        System.out.println("====================================");
        System.out.println("Expect client to choose: " + expected);
        System.out.println(reply);
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket ss = new ServerSocket(0, 0, loopback)) {
            Client.start(ss.getLocalPort());

            // client ---- GET ---> server
            // client <--- 401 ---- server
            try (Socket s = ss.accept()) {
                new MessageHeader().parseHeader(s.getInputStream());
                s.getOutputStream().write(reply.getBytes("US-ASCII"));
            }

            // client ---- GET ---> server
            // client <--- 200 ---- server
            String auth;
            try (Socket s = ss.accept()) {
                MessageHeader mh = new MessageHeader();
                mh.parseHeader(s.getInputStream());
                s.getOutputStream().write(OKAY.getBytes("US-ASCII"));
                auth = mh.findValue("Authorization");
            }

            // check Authorization header
            if (auth == null)
                throw new RuntimeException("Authorization header not found");
            System.out.println("Server received Authorization header: " + auth);
            String[] values = auth.split(" ");
            if (!values[0].equals(expected))
                throw new RuntimeException("Unexpected value");
        }
    }

    /**
     * Test the http protocol handler with one WWW-Authenticate header with
     * the value "NTLM".
     */
    static void testNTLM() throws Exception {
        // server reply
        String reply = authReplyFor("NTLM");

        System.out.println("====================================");
        System.out.println("Expect client to fail with 401 Unauthorized");
        System.out.println(reply);

        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket ss = new ServerSocket(0, 0, loopback)) {
            Client client = new Client(ss.getLocalPort());
            Thread thr = new Thread(client);
            thr.start();

            // client ---- GET ---> server
            // client <--- 401 ---- client
            try (Socket s = ss.accept()) {
                new MessageHeader().parseHeader(s.getInputStream());
                s.getOutputStream().write(reply.getBytes("US-ASCII"));
            }

            // the client should fail with 401
            System.out.println("Waiting for client to terminate");
            thr.join();
            IOException ioe = client.ioException();
            if (ioe != null)
                System.out.println("Client failed: " + ioe);
            int respCode = client.respCode();
            if (respCode != 0 && respCode != -1)
                System.out.println("Client received HTTP response code: " + respCode);
            if (respCode != HttpURLConnection.HTTP_UNAUTHORIZED)
                throw new RuntimeException("Unexpected response code");
        }
    }

    public static void main(String[] args) throws Exception {
        boolean ntlmSupported = false;
        try {
            Class<?> ntlmProxyClass = Class.forName("sun.net.www.protocol.http.NTLMAuthenticationProxy", true, NoNTLM.class.getClassLoader());
            Field ntlmSupportedField = ntlmProxyClass.getDeclaredField("supported");
            ntlmSupportedField.setAccessible(true);
            if (ntlmSupportedField.getBoolean(null)) {
                System.out.println("NTLM is supported.");
                ntlmSupported = true;
            }
        } catch (ClassNotFoundException okay) { }

        // setup Authenticator
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("user", "pass".toCharArray());
            }
        });

        // test combinations of authentication schemes
        test("Basic");
        test("Digest");
        test("Basic", "Digest");

        if (ntlmSupported) {
            System.out.println("====================================");
            System.out.println("NTLM is supported: client would select NTLM: skipping `test(\"Basic\", \"NTLM\")`..");
        } else {
            test("Basic", "NTLM");
        }

        test("Digest", "NTLM");
        test("Basic", "Digest", "NTLM");

        if (ntlmSupported) {
            System.out.println("====================================");
            System.out.println("NTLM is supported: client would select NTLM: skipping `testNTLM()`..");
        } else {
            // test NTLM only, this should fail with "401 Unauthorized"
            testNTLM();
        }

        System.out.println();
        System.out.println("TEST PASSED");
    }
}
