/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8034170
 * @summary Digest authentication interop issue
 * @library /test/lib
 * @run main/othervm B8034170 unquoted
 * @run main/othervm -Dhttp.auth.digest.quoteParameters=true B8034170 quoted
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B8034170 unquoted
 */

public class B8034170 {

    static boolean expectQuotes;

    static class BasicServer extends Thread {

        ServerSocket server;

        Socket s;
        InputStream is;
        OutputStream os;

        static final String realm = "wallyworld";

        String reply1 = "HTTP/1.1 401 Unauthorized\r\n"+
            "WWW-Authenticate: Digest realm=\""+realm+"\", qop=\"auth\"" +
            ", nonce=\"8989de95ea2402b64d73cecdb15da255\"" +
            ", opaque=\"bbfb4c9ee92ddccc73521c3e6e841ba2\"\r\n\r\n";

        String OKreply = "HTTP/1.1 200 OK\r\n"+
            "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
            "Server: Apache/1.3.14 (Unix)\r\n" +
            "Connection: close\r\n" +
            "Content-Type: text/plain; charset=iso-8859-1\r\n" +
            "Content-Length: 10\r\n\r\n";

        String ERRreply = "HTTP/1.1 500 Internal server error\r\n"+
            "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
            "Server: Apache/1.3.14 (Unix)\r\n" +
            "Connection: close\r\n" +
            "Content-Length: 0\r\n\r\n";

        BasicServer (ServerSocket s) {
            server = s;
        }

        int readAll (Socket s, byte[] buf) throws IOException {
            int pos = 0;
            InputStream is = s.getInputStream ();
            // wait two seconds for request, as client doesn't close
            // the connection
            s.setSoTimeout(2000);
            try {
                int n;
                while ((n=is.read(buf, pos, buf.length-pos)) > 0)
                    pos +=n;
            } catch (SocketTimeoutException x) { }
            return pos;
        }

        public void run () {
            byte[] buf = new byte[5000];
            try {
                System.out.println ("Server 1: accept");
                s = server.accept ();
                System.out.println ("accepted");
                os = s.getOutputStream();
                os.write (reply1.getBytes());
                readAll (s, buf);
                s.close ();

                System.out.println ("Server 2: accept");
                s = server.accept ();
                System.out.println ("accepted");
                os = s.getOutputStream();
                int count = readAll (s, buf);
                String reply = new String(buf, 0, count);

                boolean error;

                if (expectQuotes) {
                    error = false;
                    if (!reply.contains("qop=\"auth\"")) {
                        System.out.println ("Expecting quoted qop. Not found");
                        error = true;
                    }
                    if (!reply.contains("algorithm=\"MD5\"")) {
                        System.out.println ("Expecting quoted algorithm. Not found");
                        error = true;
                    }
                } else {
                    error = false;
                    if (!reply.contains("qop=auth")) {
                        System.out.println ("Expecting unquoted qop. Not found");
                        error = true;
                    }
                    if (!reply.contains("algorithm=MD5")) {
                        System.out.println ("Expecting unquoted algorithm. Not found");
                        error = true;
                    }
                }
                if (error) {
                    os.write(ERRreply.getBytes());
                    os.flush();
                    s.close();
                } else {
                    os.write((OKreply+"HelloWorld").getBytes());
                    os.flush();
                    s.close();
                }
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
        expectQuotes = args[0].equals("quoted");

        MyAuthenticator3 auth = new MyAuthenticator3 ();
        Authenticator.setDefault (auth);
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        int port = ss.getLocalPort ();
        BasicServer server = new BasicServer (ss);
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
        }
    }
}
