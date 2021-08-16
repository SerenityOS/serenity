/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6890349
 * @library /test/lib
 * @run main/othervm B6890349
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6890349
 * @summary  Light weight HTTP server
 */

import java.net.*;
import java.io.*;

public class B6890349 extends Thread {
    public static final void main(String[] args) throws Exception {

        try {
            ServerSocket server = new ServerSocket();
            InetAddress loopback = InetAddress.getLoopbackAddress();
            InetSocketAddress address = new InetSocketAddress(loopback, 0);
            server.bind(address);

            int port = server.getLocalPort();
            System.out.println ("listening on "  + port);
            B6890349 t = new B6890349 (server);
            t.start();
            URL u = new URL("http",
                InetAddress.getLoopbackAddress().getHostAddress(),
                port,
                "/foo\nbar");
            System.out.println("URL: " + u);
            HttpURLConnection urlc = (HttpURLConnection)u.openConnection(Proxy.NO_PROXY);
            InputStream is = urlc.getInputStream();
            throw new RuntimeException ("Test failed");
        } catch (IOException e) {
            System.out.println ("Caught expected exception: " + e);
        }
    }

    ServerSocket server;

    B6890349 (ServerSocket server) {
        this.server = server;
    }

    String resp = "HTTP/1.1 200 Ok\r\nContent-length: 0\r\n\r\n";

    public void run () {
        try {
            Socket s = server.accept ();
            OutputStream os = s.getOutputStream();
            os.write (resp.getBytes());
        } catch (IOException e) {
            System.out.println (e);
        }
    }
}
