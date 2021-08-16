/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6380091
 */
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;

public class CloseAfterConnect {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(0));

        InetAddress lh = InetAddress.getLocalHost();
        final SocketChannel sc = SocketChannel.open();
        final InetSocketAddress isa =
            new InetSocketAddress(lh, ssc.socket().getLocalPort());

        // establish connection in another thread
        Runnable connector =
            new Runnable() {
                public void run() {
                    try {
                        sc.connect(isa);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            };
        Thread thr = new Thread(connector);
        thr.start();

        // wait for connect to be established and for thread to
        // terminate
        do {
            try {
                thr.join();
            } catch (InterruptedException x) { }
        } while (thr.isAlive());

        // check connection is established
        if (!sc.isConnected()) {
            throw new RuntimeException("SocketChannel not connected");
        }

        // close channel - this triggered the bug as it attempted to signal
        // a thread that no longer exists
        sc.close();

        // clean-up
        ssc.accept().close();
        ssc.close();
    }
}
