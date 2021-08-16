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
 * @bug 4433616
 * @summary  Test that packet discarding is done at impl level
 */
import java.net.*;

public class CheckDiscard {

    CheckDiscard() throws Exception {

        DatagramSocket s = new DatagramSocket();

        /*
         * Create two sender threads
         */
        Sender s1 = new Sender( s.getLocalPort() );
        Sender s2 = new Sender( s.getLocalPort() );

        /*
         * "connect" to sender 1
         */
        InetAddress ia = InetAddress.getLocalHost();
        s.connect( ia, s1.getLocalPort() );

        /*
         * Kick off the senders
         */
        (new Thread(s1)).start();
        (new Thread(s2)).start();

        /*
         * Receive packets and verify that they came from the
         * right sender
         */
        byte b[] = new byte[512];
        DatagramPacket p = new DatagramPacket(b, b.length);
        s.setSoTimeout(4000);
        try {
            for (int i=0; i<20; i++) {
                s.receive(p);
                if ((p.getPort() != s1.getLocalPort()) ||
                    (!p.getAddress().equals(ia))) {
                    throw new Exception("Received packet from wrong sender");
                }
            }
        } catch (SocketTimeoutException e) {
        }

        /*
         * Finally check if either sender threw an exception
         */
        Exception e;
        e = s1.getException();
        if (e != null) throw e;
        e = s2.getException();
        if (e != null) throw e;
    }

    public static void main(String args[]) throws Exception {
        new CheckDiscard();
    }



    public class Sender implements Runnable {

        Exception exc = null;
        DatagramSocket s;
        int port;

        Sender(int port) throws Exception {
            s = new DatagramSocket();
            this.port = port;
        }

        public int getLocalPort() {
            return s.getLocalPort();
        }

        public void setException(Exception e) {
            exc = e;
        }

        public Exception getException() {
            return exc;
        }

        /*
         * Send 10 packets to the receiver
         */
        public void run() {
            try {

                byte b[] = "Hello".getBytes();
                DatagramPacket p = new DatagramPacket(b, b.length);
                p.setAddress( InetAddress.getLocalHost() );
                p.setPort( port );

                for (int i=0; i<10; i++) {
                    s.send(p);
                    Thread.currentThread().sleep(1000);
                }

            } catch (Exception e) {
                setException(e);
            }

            s.close();
        }
    }

}
