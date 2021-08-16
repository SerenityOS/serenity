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

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.nio.charset.StandardCharsets;
import jdk.test.lib.net.URIBuilder;

/**
 * @test
 * @bug 8199849 8235976
 * @summary
 * @library /test/lib
 * @run main/othervm ParamTest
 * @run main/othervm -Djava.net.preferIPv6Addresses=true ParamTest
 */

public class ParamTest {

    static final String[] variants = {
        " ,charset=utf-8",
        " ,charset=UtF-8",
        " ,charset=\"utF-8\"",
        " ,charset=\"UtF-8\""
    };

    static final int LOOPS = variants.length;

    volatile static boolean error = false;

    static class BasicServer extends Thread {

        final ServerSocket server;

        Socket s;
        InputStream is;
        OutputStream os;

        static final String realm = "wallyworld";

        String reply1 = "HTTP/1.1 401 Unauthorized\r\n"+
            "WWW-Authenticate: Basic realm=\""+realm+"\"\r\n";

        String reply2 = "HTTP/1.1 200 OK\r\n"+
            "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
            "Server: Apache/1.3.14 (Unix)\r\n" +
            "Connection: close\r\n" +
            "Content-Type: text/html; charset=iso-8859-1\r\n" +
            "Content-Length: 10\r\n\r\n";

        BasicServer(ServerSocket s) {
            server = s;
        }

        String readHeaders(Socket sock) throws IOException {
            InputStream is = sock.getInputStream();
            String s = "";
            byte[] buf = new byte[1024];
            while (!s.endsWith("\r\n\r\n")) {
                int c = is.read(buf);
                if (c == -1)
                    return s;
                String f = new String(buf, 0, c, StandardCharsets.ISO_8859_1);
                s = s + f;
            }
            return s;
        }

        void check(String s, int iteration) {
            if (s.indexOf(encodedAuthString) == -1) {
                System.err.printf("On iteration %d, wrong auth string received %s\n", iteration, s);
                error = true;
            } else {
                System.err.println("check: correct auth string received");
            }
        }

        public void run() {
            try {
                for (int j = 0; j < 2; j++)
                    for (int i = 0; i < LOOPS; i++) {
                        System.out.println("Server 1: accept");
                        s = server.accept();
                        readHeaders(s);
                        System.out.println("accepted");
                        os = s.getOutputStream();
                        String str = reply1 + variants[i] + "\r\n\r\n";
                        os.write(str.getBytes());

                        System.out.println("Server 2: accept");
                        Socket s1 = server.accept();
                        String request = readHeaders(s1);
                        check(request, i);
                        System.out.println("accepted");
                        os = s1.getOutputStream();
                        os.write((reply2 + "HelloWorld").getBytes());
                        os.flush();
                        s.close();
                        s1.close();
                        finished();
                    }
            } catch (Exception e) {
                System.out.println(e);
                error = true;
            }
        }

        public synchronized void finished() {
            notifyAll();
        }

    }

    static final String password = "Selam D\u00fcnya.";

    // "user : <password above>" encoded in UTF-8 and converted to Base 64

    static final String encodedAuthString = "dXNlcjpTZWxhbSBEw7xueWEu";

    static class MyAuthenticator extends Authenticator {
        MyAuthenticator() {
            super();
        }

        public PasswordAuthentication getPasswordAuthentication()
            {
            System.out.println("Auth called");
            return (new PasswordAuthentication ("user", password.toCharArray()));
        }
    }


    static void read(InputStream is) throws IOException {
        int c;
        System.out.println("reading");
        while ((c=is.read()) != -1) {
            System.out.write(c);
        }
        System.out.println("");
        System.out.println("finished reading");
    }

    public static void main(String args[]) throws Exception {
        MyAuthenticator auth = new MyAuthenticator();
        Authenticator.setDefault(auth);
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        int port = ss.getLocalPort();
        BasicServer server = new BasicServer(ss);
        synchronized (server) {
            server.start();
            System.out.println("client 1");
            String base = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .path("/")
                .build()
                .toString();
            URL url = new URL(base + "d1/d2/d3/foo.html");

            for (int i = 0; i < LOOPS; i++) {
                URLConnection urlc = url.openConnection(Proxy.NO_PROXY);
                InputStream is = urlc.getInputStream();
                read(is);
                System.out.println("Client: waiting for notify");
                server.wait();
                System.out.println("Client: continue");
                // check if authenticator was called once (ok) or twice (not)
                if (error) {
                    System.err.println("Error old client iteration " + i);
                }
            }

            URI uri = url.toURI();
            HttpClient client = HttpClient.newBuilder()
                .authenticator(auth)
                .proxy(ProxySelector.of(null))
                .build();

            HttpRequest request = HttpRequest
                .newBuilder(uri)
                .GET()
                .build();

            for (int i = 0; i < LOOPS; i++) {
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                if (status != 200) {
                    System.err.printf("Error new client (%d) iteration ",
                                status, i);
                    error = true;
                } else
                    System.err.println("New client ok iteration " + i);
                System.out.println("New Client: waiting for notify");
                server.wait();
                System.out.println("New Client: continue");
            }

            if (error) {
                throw new RuntimeException("Test failed");
            }
        }
    }
}
