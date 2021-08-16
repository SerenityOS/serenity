/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4258697
 * @library /test/lib
 * @summary Make sure that http CONTINUE status followed by invalid
 * response doesn't cause HttpClient to recursively loop and
 * eventually StackOverflow.
 *
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;
import jdk.test.lib.net.URIBuilder;
import static java.net.Proxy.NO_PROXY;

public class HttpContinueStackOverflow {

    static class Server implements Runnable {
        ServerSocket serverSock ;

        Server() throws IOException {
            serverSock = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        }

        int getLocalPort() {
            return serverSock.getLocalPort();
        }

        public void run() {
            Socket sock = null;
            try {
                serverSock.setSoTimeout(10000);
                sock = serverSock.accept();

                /* setup streams and read http request */
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
                PrintStream out = new PrintStream( sock.getOutputStream() );
                in.readLine();

                /* send continue followed by invalid response */
                out.println("HTTP/1.1 100 Continue\r");
                out.println("\r");
                out.println("junk junk junk");
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { serverSock.close(); } catch (IOException unused) {}
                try { sock.close(); } catch (IOException unused) {}
            }
        }
    }

    HttpContinueStackOverflow() throws Exception {
        /* create the server */
        Server s = new Server();
        (new Thread(s)).start();

        /* connect to server and get response code */
        URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(s.getLocalPort())
                .path("/anything.html")
                .toURL();

        HttpURLConnection conn = (HttpURLConnection)url.openConnection(NO_PROXY);
        conn.getResponseCode();
        System.out.println("TEST PASSED");
    }

    public static void main(String args[]) throws Exception {
        System.out.println("Testing 100-Continue");
        new HttpContinueStackOverflow();
    }
}
