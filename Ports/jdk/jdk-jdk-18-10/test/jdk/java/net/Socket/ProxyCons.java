/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4097826
 * @library /test/lib
 * @summary SOCKS support inadequate
 * @run main/timeout=40/othervm -DsocksProxyHost=nonexistant ProxyCons
 * @run main/timeout=40/othervm -DsocksProxyHost=nonexistant -Djava.net.preferIPv4Stack=true ProxyCons
 */

import java.net.*;
import jdk.test.lib.net.IPSupport;

public class ProxyCons {
    class Server extends Thread {
        ServerSocket server;
        Server (ServerSocket server) {
            super ();
            this.server = server;
        }
        public void run () {
            try {
                Socket s = server.accept ();
                s.close();
                while (!finished ()) {
                    Thread.sleep (500);
                }
            } catch (Exception e) {
            }
        }
        boolean isFinished = false;

        synchronized boolean finished () {
            return (isFinished);
        }
        synchronized void done () {
            isFinished = true;
        }
    }

    public ProxyCons() {
    }

    void test() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(localHost, 0));
        try {
            Server s = new Server(ss);
            s.start();
            Socket sock = new Socket(Proxy.NO_PROXY);
            sock.connect(new InetSocketAddress(localHost, ss.getLocalPort()));
            s.done();
            sock.close();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } finally {
            ss.close();
        }
    }

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        ProxyCons c = new ProxyCons();
        c.test();
    }
}
