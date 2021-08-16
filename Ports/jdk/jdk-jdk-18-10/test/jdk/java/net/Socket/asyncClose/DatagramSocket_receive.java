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
 * Tests that a thread blocked in DatagramSocket.receive
 * throws a SocketException if the socket is asynchronously closed.
 */
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatagramSocket_receive extends AsyncCloseTest implements Runnable {
    private final DatagramSocket s;
    private final int timeout;
    private final CountDownLatch latch;
    private final AtomicBoolean readyToClose = new AtomicBoolean(false);

    public DatagramSocket_receive() throws SocketException {
        this(0);
    }

    public DatagramSocket_receive(int timeout) throws SocketException {
        this.timeout = timeout;
        latch = new CountDownLatch(1);
        s = new DatagramSocket(0, InetAddress.getLoopbackAddress());
    }

    public String description() {
        String s = "DatagramSocket.receive(DatagramPacket)";
        if (timeout > 0) {
            s += " (timeout specified)";
        }
        return s;
    }

    public void run() {
        try {
            byte b[] = new byte[1024];
            DatagramPacket p  = new DatagramPacket(b, b.length);
            if (timeout > 0) {
                s.setSoTimeout(timeout);
            }
            latch.countDown();
            do {
                // if readyToClose is still false it means some other
                // process on the system attempted to send datagram packet:
                // just ignore it, and go back to accept again.
                s.receive(p);
            } while (!readyToClose.get());
            failed("DatagramSocket.receive(DatagramPacket) returned unexpectedly!!" + " - " + p.getAddress());
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
            Thread thr = new Thread(this);
            thr.start();
            latch.await();
            Thread.sleep(5000); //sleep, so receive(DatagramPacket) can block
            readyToClose.set(true);
            s.close();
            thr.join();

            if (isClosed()) {
                return passed();
            } else {
                return failed("DatagramSocket.receive(DatagramPacket) wasn't preempted");
            }
        } catch (Exception x) {
            failed(x.getMessage());
            throw new RuntimeException(x);
        }
    }
}
