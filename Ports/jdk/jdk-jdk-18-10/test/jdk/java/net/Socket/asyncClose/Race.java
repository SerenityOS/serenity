/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006395 8012244
 * @summary Tests racing code that reads and closes a Socket
 */

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.concurrent.Phaser;

// Racey test, will not always fail, but if it does then we have a problem.

public class Race {
    final static int THREADS = 100;

    public static void main(String[] args) throws Exception {
        try (ServerSocket ss = new ServerSocket()) {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            ss.bind(new InetSocketAddress(loopback, 0));
            final int port = ss.getLocalPort();
            final Phaser phaser = new Phaser(THREADS + 1);
            for (int i=0; i<100; i++) {
                try {
                    final Socket s = new Socket(loopback, port);
                    s.setSoLinger(false, 0);
                    try (Socket sa = ss.accept()) {
                        sa.setSoLinger(false, 0);
                        final InputStream is = s.getInputStream();
                        Thread[] threads = new Thread[THREADS];
                        for (int j=0; j<THREADS; j++) {
                            threads[j] = new Thread() {
                            public void run() {
                                try {
                                    phaser.arriveAndAwaitAdvance();
                                    while (is.read() != -1)
                                        Thread.sleep(50);
                                } catch (Exception x) {
                                    if (!(x instanceof SocketException
                                          && x.getMessage().equalsIgnoreCase("socket closed")))
                                        x.printStackTrace();
                                    // ok, expect Socket closed
                                }
                            }};
                        }
                        for (int j=0; j<100; j++)
                            threads[j].start();
                        phaser.arriveAndAwaitAdvance();
                        s.close();
                        for (int j=0; j<100; j++)
                            threads[j].join();
                    }
                } catch (ConnectException e) {
                    System.err.println("Exception " + e + " Port: " + port);
                }
            }
        }
    }
}
