/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7014860
 * @library /test/lib
 * @summary Socket.getInputStream().available() not clear for
 *          case that connection is shutdown for reading
 * @run main ShutdownInput
 * @run main/othervm -Djava.net.preferIPv4Stack=true ShutdownInput
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import jdk.test.lib.net.IPSupport;

public class ShutdownInput {
    static boolean failed = false;

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        InetAddress iaddr = InetAddress.getLoopbackAddress();

        try (ServerSocket ss = new ServerSocket(0, 0, iaddr);
              Socket s1 = new Socket(iaddr, ss.getLocalPort());
              Socket s2 = ss.accept() ) {

            test(s1, s2, "Testing NET");
        }

        // check the NIO socket adapter
        InetSocketAddress socketAddress = new InetSocketAddress(iaddr, 0);
        try (ServerSocketChannel sc = ServerSocketChannel.open().bind(socketAddress);
             SocketChannel s1 = SocketChannel.open(
                     new InetSocketAddress(iaddr, sc.socket().getLocalPort()));
             SocketChannel s2 = sc.accept() ) {

            test(s1.socket(), s2.socket(), "Testing NIO");
        }

        if (failed) {
            throw new RuntimeException("Failed: check output");
        }
    }

    public static void test(Socket s1, Socket s2, String mesg) throws Exception {
        OutputStream os = s1.getOutputStream();
        os.write("This is a message".getBytes("US-ASCII"));

        InputStream in = s2.getInputStream();
        s2.shutdownInput();

        if (in.available() != 0) {
            failed = true;
            System.out.println(mesg + ":" + s2 + " in.available() should be 0, " +
                               "but returns "+ in.available());
        }

        byte[] ba = new byte[2];
        if (in.read() != -1 ||
            in.read(ba) != -1 ||
            in.read(ba, 0, ba.length) != -1) {

            failed = true;
            System.out.append(mesg + ":" + s2 + " in.read() should be -1");
        }
    }
}
