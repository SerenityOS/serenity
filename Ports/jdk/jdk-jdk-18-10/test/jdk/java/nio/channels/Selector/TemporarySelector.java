/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6645197
 * @run main/othervm -Xmx16m TemporarySelector
 * @summary Timed read with socket adaptor throws ClosedSelectorException if temporary selector GC'ed.
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TemporarySelector {

    static volatile boolean done = false;

    public static void main(String[] args) throws Exception {

        Runnable r = new Runnable() {
            public void run() {
                while (!done) {
                    System.gc();
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            }
        };

        try {
            // Create a server socket that will open and accept on loopback connection
            ServerSocketChannel ssc =  ServerSocketChannel.open();
            final ServerSocket ss =  ssc.socket();
            ss.bind(new InetSocketAddress(0));
            int localPort = ss.getLocalPort();

            // Create a client socket that will connect and read
            System.out.println("Connecting to server socket");
            System.out.flush();
            SocketChannel channel = SocketChannel.open(new InetSocketAddress("localhost", localPort));
            System.out.println("Connected to server socket");
            System.out.flush();

            // Create a thread to try and cause the GC to run
            Thread t = new Thread(r);
            t.start();
            byte[] buffer = new byte[500];
            System.out.println("Reading from socket input stream");
            System.out.flush();
            Socket socket = channel.socket();
            socket.setSoTimeout(10000);  // The timeout must be set
            // to trigger this bug
            try {
                socket.getInputStream().read(buffer);
            } catch (java.net.SocketTimeoutException ste) {
                // no java.nio.channels.ClosedSelectorException
            }
        } finally {
            done = true;
        }
    }
}
