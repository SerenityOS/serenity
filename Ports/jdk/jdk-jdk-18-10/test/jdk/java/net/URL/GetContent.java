/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4145315
 * @library /test/lib
 * @summary Test a read from nonexistant URL
 * @run main GetContent
 * @run main/othervm -Djava.net.preferIPv6Addresses GetContent
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;

public class GetContent implements Runnable {

     ServerSocket ss;

     public void run() {
        try (Socket s = ss.accept()) {
            s.setTcpNoDelay(true);

            PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    s.getOutputStream() ));

            out.print("HTTP/1.1 404 Not Found\r\n");
            out.print("Connection: close\r\n");
            out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
            out.print("\r\n");
            out.flush();
            out.print("<HTML><BODY>Sorry, page not found</BODY></HTML>");
            out.flush();

            // wait for client to read response - otherwise http
            // client get error and re-establish connection
            Thread.sleep(2000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { ss.close(); } catch (IOException unused) {}
        }
     }

     GetContent() throws Exception {
         InetAddress loopback = InetAddress.getLoopbackAddress();
         ss = new ServerSocket();
         ss.bind(new InetSocketAddress(loopback, 0));

         Thread thr = new Thread(this);
         thr.start();

         boolean error = true;
         try {
             java.net.URL url = URIBuilder.newBuilder()
                 .scheme("http")
                 .host(ss.getInetAddress())
                 .port(ss.getLocalPort())
                 .path("/no-such-name")
                 .toURL();
             Object obj = url.openConnection(Proxy.NO_PROXY)
                          .getContent();
             InputStream in = (InputStream) obj;
             byte buff[] = new byte[200];
             int len = in.read(buff);
         } catch (IOException ex) {
             error = false;
         }

         if (error)
             throw new RuntimeException("No IOException generated.");
     }

     public static void main(String args[]) throws Exception {
        new GetContent();
     }
}
