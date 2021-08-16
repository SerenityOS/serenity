/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4413768
 * @summary Checking that PortUnreachableException is thrown when
 *          ICMP Port Unreachable is received.
 */
import java.net.*;
import java.util.Properties;

public class Test {

    /*
     * Return an available port
     */
    int getPort() throws Exception {
        DatagramSocket s = new DatagramSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }

    /*
     * Perform test by sending to remote_host:port
     * sendOnly => send datagram to host and expect PUE on subsequent
     *             send
     * !sendOnly => send datagram to host and expect PUE on subsequent
     *              send or receive.
     */
    void doTest(String remote_host, int port, boolean sendOnly) throws Exception {

        System.out.println("***");
        System.out.println("Test Description:");
        System.out.println("    DatagramSocket.connect");
        System.out.println("    Loop: DatagramSocket.send");
        if (!sendOnly) {
            System.out.println("          DatagramSocket.receive");
        }
        System.out.println("");
        System.out.println("Test Run:");

        InetAddress ia = InetAddress.getByName(remote_host);
        DatagramSocket s = new DatagramSocket(0);
        s.setSoTimeout(1000);
        s.connect(ia, port);

        byte[] b = "Hello".getBytes();
        DatagramPacket p1 = new DatagramPacket(b, b.length, ia, port);

        DatagramPacket p2 = new DatagramPacket(b, b.length);

        int i = 0;
        boolean gotPUE = false;

        do {

            System.out.println("Sending datagram to unreachable port...");
            try {
                s.send(p1);
            } catch (PortUnreachableException e) {
                System.out.println("DatagramSocket.send threw PUE");
                gotPUE = true;
            }

            if (!gotPUE) {
                Thread.currentThread().sleep(1000);
            }

            if (!sendOnly && !gotPUE) {
                System.out.println("DatagramSocket.receive...");
                try {
                    s.receive(p2);
                } catch (PortUnreachableException e) {
                    System.out.println("DatagramSocket.receive threw PUE");
                    gotPUE = true;
                } catch (SocketTimeoutException e) {
                    System.out.println("DatagramSocket.receive timed out - no PUE");
                }
            }

            i++;
        } while (i < 10 && !gotPUE);

        if (!gotPUE) {
            System.out.println("DatagramSocket.{send,receive} didn't throw " +
                "PortUnreachableException - passing anyway!");
        } else {
            System.out.println("    Test passed.");
        }
        System.out.println("");
    }

    /*
     * Perform tests via remote_host.
     */
    Test(String remote_host) throws Exception {

        int port = getPort();

        doTest(remote_host, port, true);
        doTest(remote_host, port, false);
    }

    public static void main(String args[]) throws Exception {

        String remote_host = "localhost";
        if (args.length > 0) {
            remote_host = args[0];
        }

        new Test(remote_host);
    }
}
