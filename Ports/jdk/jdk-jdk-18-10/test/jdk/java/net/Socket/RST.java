/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4468997
 * @library /test/lib
 * @summary SO_LINGER is ignored on Windows with Winsock 2
 * @run main RST
 * @run main/othervm -Djava.net.preferIPv4Stack=true RST
 */
import java.net.*;
import java.io.*;
import jdk.test.lib.net.IPSupport;

public class RST implements Runnable {

    Socket client;

    public void run() {
        try {
            client.setSoLinger(true, 0);        // hard reset
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    RST() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        client = new Socket(loopback, ss.getLocalPort());
        Socket server = ss.accept();

        Thread thr = new Thread(this);
        thr.start();

        SocketException exc = null;
        try {
            InputStream in = server.getInputStream();

            /*
             * This read should throw a SocketException indicating a
             * connection reset.
             */
            int n = in.read();
        } catch (SocketException se) {
            exc = se;
        }

        server.close();
        ss.close();

        if (exc == null) {
            throw new Exception("Expected SocketException not thrown");
        }
        if (exc.getMessage().toLowerCase().indexOf("reset") == -1) {
            throw new Exception("SocketException thrown but not expected \"connection reset\"");
        }
    }


    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        new RST();
    }
}
