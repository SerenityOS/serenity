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

/*
 * @test
 * @bug 4431020
 * @summary On Windows 2000 we observed behaviour that reflects the underlying
 * implementation :-
 *   1. PortUnreachableException throw per underlying reset
 *   2. No PUE throw for DatagramSocket.send
 */
import java.net.*;

public class Concurrent implements Runnable {

    DatagramSocket s;

    public void run() {
        try {
            byte b[] = new byte[512];
            DatagramPacket p = new DatagramPacket(b, b.length);

            int pue_count = 0;
            while (true) {
                try {
                    System.out.println("receive...");
                    s.receive(p);
                } catch (PortUnreachableException pue) {
                    System.out.println("receive threw PortUnreachableException");
                    pue_count++;
                }
                System.out.println("receiver sleeping");
                Thread.currentThread().sleep(100*pue_count);
            }

        } catch (Exception e) { }
    }

    Concurrent(InetAddress ia, int port) throws Exception {

        System.out.println("");
        System.out.println("***");
        System.out.println("Test Description:");
        System.out.println("    - Block reader thread on receive");
        System.out.println("    - Send datagrams to bad destination with wee pauses");
        System.out.println("    - Observe which thread gets the PUE");
        System.out.println("");

        /*
         * Create the datagram and connect it to destination
         */
        s = new DatagramSocket();
        s.connect(ia, port);
        s.setSoTimeout(60000);

        /*
         * Start the reader thread
         */
        Thread thr = new Thread(this);
        thr.start();
        Thread.currentThread().sleep(2000);

        byte b[] = new byte[512];
        DatagramPacket p = new DatagramPacket(b, b.length);

        /*
         * Send a bunch of packets to the destination
         */
        for (int i=0; i<10; i++) {
            try {
                System.out.println("Sending...");
                s.send(p);
            } catch (PortUnreachableException e) {
                System.out.println("send threw PortUnreachableException");
            }
            Thread.currentThread().sleep(100);
        }

        /*
         * Give time for ICMP port unreachables to return
         */
        Thread.currentThread().sleep(5000);

        s.close();
    }


    public static void main(String args[]) throws Exception {

        InetAddress ia;
        int port;
        if (args.length >= 2) {
            ia = InetAddress.getByName(args[0]);
            port = Integer.parseInt(args[1]);
        } else {
            ia = InetAddress.getLocalHost();
            DatagramSocket s1 = new DatagramSocket();
            port = s1.getLocalPort();
            s1.close();
        }

        new Concurrent(ia, port);
    }

}
