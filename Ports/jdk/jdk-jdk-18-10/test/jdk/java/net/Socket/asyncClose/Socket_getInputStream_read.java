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
 * Tests that a thread blocked in Socket.getInputStream().read()
 * throws a SocketException if the socket is asynchronously closed.
 */
import java.net.*;
import java.io.*;
import java.util.concurrent.CountDownLatch;

public class Socket_getInputStream_read extends AsyncCloseTest implements Runnable {
    private final Socket s;
    private final int timeout;
    private final CountDownLatch latch;

    public Socket_getInputStream_read() {
        this(0);
    }

    public Socket_getInputStream_read(int timeout) {
        this.timeout = timeout;
        latch = new CountDownLatch(1);
        s = new Socket();
    }

    public String description() {
        String s = "Socket.getInputStream().read()";
        if (timeout > 0) {
            s += " (with timeout)";
        }
        return s;
    }

    public void run() {
        try {
            InputStream in = s.getInputStream();
            if (timeout > 0) {
                s.setSoTimeout(timeout);
            }
            latch.countDown();
            int n = in.read();
            failed("Socket.getInputStream().read() returned unexpectedly!!");
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
            Thread.sleep(5000); //sleep, so Socket.getInputStream().read() can block
            s.close();
            thr.join();

            if (isClosed()) {
                return passed();
            } else {
                return failed("Socket.getInputStream().read() wasn't preempted");
            }
        } catch (Exception x) {
            failed(x.getMessage());
            throw new RuntimeException(x);
        }
    }
}
