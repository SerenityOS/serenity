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

/* @test
   @bug 4156625
   @summary  Socket.setSoTimeout(T) can cause incorrect delay of T
             under green threads
   @author Tom Rodriguez
 */

/*
 * This program depends a bit on the particular behaviour of the green
 * threads scheduler to produce the problem, but given that the underlying
 * bug a green threads bug, I think that's OK.
 */

import java.net.*;

public class SoTimeout implements Runnable {
    static ServerSocket serverSocket;
    static long timeWritten;
    static InetAddress addr;
    static int port;

    public static void main(String[] args) throws Exception {
        addr = InetAddress.getLocalHost();
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(addr, 0));
        port = serverSocket.getLocalPort();

        byte[] b = new byte[12];
        Thread t = new Thread(new SoTimeout());
        t.start();

        Socket s = serverSocket.accept();
        serverSocket.close();

        // set a 5 second timeout on the socket
        s.setSoTimeout(5000);

        s.getInputStream().read(b, 0, b.length);
        s.close();

        long waited = System.currentTimeMillis() - timeWritten;

        // this sequence should complete fairly quickly and if it
        // takes something resembling the the SoTimeout value then
        // we are probably incorrectly blocking and not waking up
        if (waited > 2000) {
            throw new Exception("shouldn't take " + waited + " to complete");
        }
    }

    public void run() {
        try {
            byte[] b = new byte[12];
            Socket s = new Socket(addr, port);

            Thread.yield();
            timeWritten = System.currentTimeMillis();
            s.getOutputStream().write(b, 0, 12);
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
