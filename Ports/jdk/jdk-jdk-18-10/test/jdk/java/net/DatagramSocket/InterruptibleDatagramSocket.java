/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

/*
 * @test
 * @summary Check interrupt mechanism for DatagramSocket,
 *      MulticastSocket, and DatagramSocketAdaptor
 * @run main InterruptibleDatagramSocket
 */

public class InterruptibleDatagramSocket {
    private static void test0(DatagramSocket s) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Thread testThread = Thread.currentThread();

        Thread coordinator = new Thread(() -> {
            try {
                latch.await();
                sleep(500);
                testThread.interrupt();
            } catch (InterruptedException e) {
            }
        });
        byte[] data = {0, 1, 2};
        DatagramPacket p = new DatagramPacket(data, data.length,
                s.getLocalAddress(), s.getLocalPort());
        s.setSoTimeout(2000);
        coordinator.start();
        latch.countDown();
        try {
            s.receive(p);
        } finally {
            try {
                coordinator.join();
            } catch (InterruptedException e) {
            }
        }
    }

    static void test(DatagramSocket s, boolean interruptible) throws Exception {
        try {
            test0(s);
            throw new RuntimeException("Receive shouldn't have succeeded");
        } catch (SocketTimeoutException e) {
            if (interruptible)
                throw e;
            System.out.println("Got expected SocketTimeoutException: " + e);
        } catch (SocketException e) {
            if ((e.getCause() instanceof ClosedByInterruptException) && interruptible) {
                System.out.println("Got expected ClosedByInterruptException: " + e);
            } else {
                throw e;
            }
        } catch (ClosedByInterruptException e) {
            if (!interruptible)
                throw e;
            System.out.println("Got expected ClosedByInterruptException: " + e);
        }
        if (s.isClosed() && !interruptible)
            throw new RuntimeException("DatagramSocket should not be closed");
        else if (!s.isClosed() && interruptible)
            throw new RuntimeException("DatagramSocket should be closed");
    }

    public static void main(String[] args) throws Exception {
        try (DatagramSocket s = new DatagramSocket()) {
            test(s, false);
        }
        try (DatagramSocket s = new MulticastSocket()) {
            test(s, false);
        }
        try (DatagramSocket s = DatagramChannel.open().socket()) {
            test(s, true);
        }
    }
}
