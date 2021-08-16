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

/*
 * @test
 * @library /test/lib
 * @bug 4701299
 * @summary Keep-Alive-Timer thread management in KeepAliveCache causes memory leak
 * @run main KeepAliveTimerThread
 * @run main/othervm -Djava.net.preferIPv6Addresses=true KeepAliveTimerThread
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;
import static java.net.Proxy.NO_PROXY;

public class KeepAliveTimerThread {
    static class Fetcher implements Runnable {
        URL url;

        Fetcher(URL url) {
            this.url = url;
        }

        public void run() {
            try {
                InputStream in = url.openConnection(NO_PROXY).getInputStream();
                byte b[] = new byte[128];
                int n;
                do {
                    n = in.read(b);
                } while (n > 0);
                in.close();
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
    }

    static class Server extends Thread {
        ServerSocket server;
        Server (ServerSocket server) {
            super ();
            this.server = server;
        }
        void readAll (Socket s) throws IOException {
            byte[] buf = new byte [128];
            InputStream is = s.getInputStream ();
            s.setSoTimeout(1000);
            try {
                while (is.read(buf) > 0) ;
            } catch (SocketTimeoutException x) { }
        }
        /*
         * Our "http" server to return a 404
         */
        public void run() {
            try {
                Socket s = server.accept();
                readAll(s);

                PrintStream out = new PrintStream(
                                                  new BufferedOutputStream(
                                                                           s.getOutputStream() ));

                /* send the header */
                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
                out.print("Content-Length: 78\r\n");
                out.print("\r\n");
                out.print("<HTML>");
                out.print("<HEAD><TITLE>File Content</TITLE></HEAD>");
                out.print("<BODY>A dummy body.</BODY>");
                out.print("</HTML>");
                out.flush();

                s.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { server.close(); } catch (IOException unused) {}
            }
        }
    }


    public static void main(String args[]) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        Server s = new Server(ss);
        s.start();

        URL url = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(ss.getLocalPort())
            .toURL();
        System.out.println("URL: " + url);

        // start fetch in its own thread group
        ThreadGroup grp = new ThreadGroup("MyGroup");

        // http request in another thread group
        Thread thr = new Thread(grp, new Fetcher(url));
        thr.start();
        thr.join();

        // fetcher is done - the group should now be empty
        if (grp.activeCount() > 0) {
            throw new RuntimeException("Keep-alive thread started in wrong thread group");
        }

        grp.destroy();
    }

}
