/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import jdk.test.lib.net.URIBuilder;

/**
 * @test
 * @bug 4513440
 * @summary BasicAuthentication is zeroing out the given password
 * @library /test/lib
 * @run main/othervm BasicTest3
 * @run main/othervm -Djava.net.preferIPv6Addresses=true BasicTest3
 */

public class BasicTest3 {

    static class BasicServer3 extends Thread {

        ServerSocket server;

        Socket s;
        InputStream is;
        OutputStream os;

        static final String realm = "wallyworld";

        String reply1 = "HTTP/1.1 401 Unauthorized\r\n"+
            "WWW-Authenticate: Basic realm=\""+realm+"\"\r\n\r\n";

        String reply2 = "HTTP/1.1 200 OK\r\n"+
            "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
            "Server: Apache/1.3.14 (Unix)\r\n" +
            "Connection: close\r\n" +
            "Content-Type: text/html; charset=iso-8859-1\r\n" +
            "Content-Length: 10\r\n\r\n";

        BasicServer3 (ServerSocket s) {
            server = s;
        }

        void readAll (Socket s) throws IOException {
            byte[] buf = new byte [128];
            InputStream is = s.getInputStream ();
            s.setSoTimeout(1000);
            try {
                while (is.read(buf) > 0) ;
            } catch (SocketTimeoutException x) { }
        }

        public void run () {
            try {
                System.out.println ("Server 1: accept");
                s = server.accept ();
                System.out.println ("accepted");
                os = s.getOutputStream();
                os.write (reply1.getBytes());
                readAll (s);
                s.close ();

                System.out.println ("Server 2: accept");
                s = server.accept ();
                System.out.println ("accepted");
                os = s.getOutputStream();
                readAll (s);
                os.write ((reply2+"HelloWorld").getBytes());

            }
            catch (Exception e) {
                System.out.println (e);
            }
            finished ();
        }

        public synchronized void finished () {
            notifyAll();
        }

    }

    static class MyAuthenticator3 extends Authenticator {
        PasswordAuthentication pw;
        MyAuthenticator3 () {
            super ();
            pw = new PasswordAuthentication ("user", "passwordNotCheckedAnyway".toCharArray());
        }

        public PasswordAuthentication getPasswordAuthentication ()
            {
            System.out.println ("Auth called");
            return pw;
        }

        public void checkPW () {
            if (!new String (pw.getPassword()).equals ("passwordNotCheckedAnyway")) {
                throw new RuntimeException ("Password was \"" + new String (pw.getPassword()) + "\"");
            }
        }
    }


    static void read (InputStream is) throws IOException {
        int c;
        System.out.println ("reading");
        while ((c=is.read()) != -1) {
            System.out.write (c);
        }
        System.out.println ("");
        System.out.println ("finished reading");
    }

    public static void main (String args[]) throws Exception {
        MyAuthenticator3 auth = new MyAuthenticator3 ();
        Authenticator.setDefault (auth);
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        int port = ss.getLocalPort ();
        BasicServer3 server = new BasicServer3 (ss);
        synchronized (server) {
            server.start();
            System.out.println ("client 1");
            URL url =  URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .path("/d1/d2/d3/foo.html")
                .toURL();
            URLConnection urlc = url.openConnection(Proxy.NO_PROXY);
            InputStream is = urlc.getInputStream ();
            read (is);
            is.close ();
            auth.checkPW ();
        }
    }
}
