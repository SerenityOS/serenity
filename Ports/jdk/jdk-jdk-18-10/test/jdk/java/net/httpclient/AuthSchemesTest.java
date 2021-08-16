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
 * @bug 8217237
 * @library /test/lib
 * @modules java.net.http
 * @run main/othervm AuthSchemesTest
 * @summary HttpClient does not deal well with multi-valued WWW-Authenticate challenge headers
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.Authenticator;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jdk.test.lib.net.URIBuilder;

public class AuthSchemesTest {
    static class BasicServer extends Thread {

        ServerSocket server;

        Socket s;
        InputStream is;
        OutputStream os;
        static final String RESPONSE = "Hello world";
        static final String respLength = Integer.toString(RESPONSE.length());
        static final String realm = "wally world";

        String reply1 = "HTTP/1.1 401 Unauthorized\r\n"+
                "WWW-Authenticate: BarScheme\r\n" +
                "WWW-Authenticate: FooScheme realm=\""+realm+"\"\r\n" +
                "WWW-Authenticate: Basic realm=\""+realm+"\"\r\n" +
                "WWW-Authenticate: WoofScheme\r\n\r\n";

        String reply2 = "HTTP/1.1 200 OK\r\n"+
                "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
                "Server: Apache/1.3.14 (Unix)\r\n" +
                "Connection: close\r\n" +
                "Content-Type: text/html; charset=iso-8859-1\r\n" +
                "Content-Length: " + respLength + "\r\n\r\n";

        BasicServer(ServerSocket s) {
            server = s;
        }

        String response() {
            return RESPONSE;
        }

        void readAll(Socket s) throws IOException {
            byte[] buf = new byte [128];
            InputStream is = s.getInputStream();
            s.setSoTimeout(1000);
            try {
                while (is.read(buf) > 0) ;
            } catch (SocketTimeoutException x) { }
        }

        public void run() {
            try {
                System.out.println("Server 1: accept");
                s = server.accept();
                System.out.println("accepted");
                os = s.getOutputStream();
                os.write(reply1.getBytes());
                readAll(s);
                s.close();

                System.out.println("Server 2: accept");
                s = server.accept();
                System.out.println("accepted");
                os = s.getOutputStream();
                os.write((reply2+RESPONSE).getBytes());
                readAll(s);
                s.close();

            }
            catch (Exception e) {
                System.out.println(e);
            }
            finished();
        }

        boolean isfinished = false;

        public synchronized void finished() {
            isfinished = true;
            notifyAll();
        }

        public synchronized void waitforfinish() {
            while (!isfinished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class Auth extends Authenticator {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("user", new char[] {'a','b','c'});
        }
    }

    public static void main(String[] args) throws Exception {
        ServerSocket serversocket = null;
        BasicServer server = null;
        Auth authenticator = new Auth();

        serversocket = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        int port = serversocket.getLocalPort();
        server = new BasicServer(serversocket);

        HttpClient client = HttpClient.newBuilder()
                .authenticator(authenticator)
                .build();
        server.start();
        URI uri = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(port)
            .path("/foo")
            .build();
        System.out.println("URI: " + uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || !response.body().equals(server.response())) {
            System.out.println("Status code = " + response.statusCode());
            serversocket.close();
            throw new RuntimeException("Test failed");
        }
        serversocket.close();
        server.waitforfinish();
        System.out.println("OK");
    }
}
