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
 * @bug 4533243
 * @summary Closing a keep alive stream gives NullPointerException
 * @library /test/lib
 * @run main/othervm/timeout=30 KeepAliveStreamCloseWithWrongContentLength
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;

public class KeepAliveStreamCloseWithWrongContentLength {

    static class XServer extends Thread {
        ServerSocket srv;
        Socket s;
        InputStream is;
        OutputStream os;

        XServer (ServerSocket s) {
            srv = s;
        }

        public void run() {
            try {
                s = srv.accept ();
                // read HTTP request from client
                InputStream is = s.getInputStream();
                // read the first ten bytes
                for (int i=0; i<10; i++) {
                    is.read();
                }
                OutputStreamWriter ow =
                    new OutputStreamWriter((os = s.getOutputStream()));
                ow.write("HTTP/1.0 200 OK\n");

                // Note: The client expects 10 bytes.
                ow.write("Content-Length: 10\n");
                ow.write("Content-Type: text/html\n");

                // Note: If this line is missing, everything works fine.
                ow.write("Connection: Keep-Alive\n");
                ow.write("\n");

                // Note: The (buggy) server only sends 9 bytes.
                ow.write("123456789");
                ow.flush();
            } catch (Exception e) {
            } finally {
                try {if (os != null) { os.close(); }} catch (IOException e) {}
            }
        }
    }

    public static void main (String[] args) throws Exception {
        final InetAddress loopback = InetAddress.getLoopbackAddress();
        final ServerSocket serversocket = new ServerSocket();
        serversocket.bind(new InetSocketAddress(loopback, 0));

        try {
            int port = serversocket.getLocalPort ();
            XServer server = new XServer (serversocket);
            server.start ();
            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .toURL();
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            InputStream is = urlc.getInputStream ();
            int c = 0;
            while (c != -1) {
                try {
                    c=is.read();
                } catch (IOException ioe) {
                    is.read ();
                    break;
                }
            }
            is.close();
        } catch (IOException e) {
            return;
        } catch (NullPointerException e) {
            throw new RuntimeException (e);
        } finally {
            serversocket.close();
        }
    }
}
