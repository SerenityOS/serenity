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
 * @bug 4712609
 * @summary Socket(String host, int port) throws NullPointerException if host is null
 */

import java.net.*;
import java.io.IOException;

public class NullHost {
    class Server extends Thread {
        private ServerSocket svr;

        public Server() throws IOException {
            svr = new ServerSocket();
            // The client side calls Socket((String) null, ...) which
            // resolves to InetAddress.getByName((String)null) which in
            // turns will resolve to the loopback address
            svr.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        }

        public int getPort() {
            return svr.getLocalPort();
        }

        volatile boolean done;
        public void shutdown() {
            try {
                done = true;
                svr.close();
            } catch (IOException e) {
            }
        }

        public void run() {
            Socket s;
            try {
                while (!done) {
                    s = svr.accept();
                    s.close();
                }
            } catch (IOException e) {
                if (!done) e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        NullHost n = new NullHost();
    }

    public NullHost () throws IOException {
        Server s = new Server();
        int port = s.getPort();
        s.start();
        try {
            try (var sock = new Socket((String)null, port)) {}
            try (var sock = new Socket((String)null, port, true)) {}
            try (var sock = new Socket((String)null, port, null, 0)) {}
        } catch (NullPointerException e) {
            throw new RuntimeException("Got a NPE");
        } finally {
            s.shutdown();
        }
    }
}
