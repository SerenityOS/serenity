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
 * Tests that a thread blocked in Socket.getOutputStream().write()
 * throws a SocketException if the socket is asynchronously closed.
 */
import java.net.*;
import java.io.*;
import java.util.concurrent.CountDownLatch;

public class Socket_getOutputStream_write extends AsyncCloseTest implements Runnable {
    private final Socket s;
    private final CountDownLatch latch;

    public Socket_getOutputStream_write() {
        latch = new CountDownLatch(1);
        s = new Socket();
    }

    public String description() {
        return "Socket.getOutputStream().write()";
    }

    public void run() {
        try {
            OutputStream out = s.getOutputStream();
            byte b[] = new byte[8192];
            latch.countDown();
            for (;;) {
                out.write(b);
            }
        } catch (SocketException se) {
            if (latch.getCount() != 1) {
                closed();
            }
        } catch (Exception e) {
            failed(e.getMessage());
        } finally {
            if (latch.getCount() == 1) {
                latch.countDown();
            }
        }
    }

    public AsyncCloseTest go() {
        try {
            InetAddress lh = InetAddress.getLocalHost();
            ServerSocket ss = new ServerSocket(0, 0, lh);
            s.connect( new InetSocketAddress(lh, ss.getLocalPort()) );
            Socket s2 = ss.accept();
            Thread thr = new Thread(this);
            thr.start();
            latch.await();
            Thread.sleep(1000);
            s.close();
            thr.join();

            if (isClosed()) {
                return passed();
            } else {
                return failed("Socket.getOutputStream().write() wasn't preempted");
            }
        } catch (Exception x) {
            failed(x.getMessage());
            throw new RuntimeException(x);
        }
    }
}
