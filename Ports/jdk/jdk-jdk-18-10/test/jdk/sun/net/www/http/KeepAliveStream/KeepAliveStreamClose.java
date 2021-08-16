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

/*
 * @test
 * @bug 4392195
 * @summary Infinite loop in sun.net.www.http.KeepAliveStream [due to skip()]
 * @library /test/lib
 * @run main/othervm/timeout=30 KeepAliveStreamClose
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;

public class KeepAliveStreamClose {
    static class XServer extends Thread {
        ServerSocket srv;
        Socket s;
        InputStream is;
        OutputStream os;

        XServer (ServerSocket s) {
            srv = s;
        }

        Socket getSocket () {
            return (s);
        }

        // simulated HTTP server response
        static String response = "HTTP/1.1 200 OK\nDate: Thu, 07 Dec 2000 11:32:28 GMT\n"+
            "Server: Apache/1.3.6 (Unix)\nKeep-Alive: timeout=15, max=100\nConnection: Keep-Alive\n"+
            "Content-length: 255\nContent-Type: text/html\n\n";

        public void run() {
            try {
                s = srv.accept ();
                is = s.getInputStream ();
                os = s.getOutputStream ();
                // read the first ten bytes
                for (int i=0; i<10; i++) {
                    is.read();
                }
                os.write (response.getBytes());
                for (int i=0; i<255; i++) {
                    os.write ("X".getBytes());
                    Thread.sleep (1000);
                }
            } catch (Exception e) {
            }
        }
    }

    /*
     * If the server closes the connection at the same time as the client
     * does, then the client will hang (jdk1.3) because KeepAliveStream.skip
     * is returning zero and the calling close() stays in a loop
     */

    public static void main (String[] args) {
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            ServerSocket serversocket = new ServerSocket (0, 50, loopback);
            int port = serversocket.getLocalPort ();
            XServer server = new XServer (serversocket);
            server.start ();
            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .toURL();
            URLConnection urlc = url.openConnection ();
            InputStream is = urlc.getInputStream ();
            int i=0, c;
            while ((c=is.read())!= -1) {
                i++;
                if (i == 5) {
                    server.getSocket().close ();
                    is.close();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException ("Unexpected exception");
        }
    }
}
