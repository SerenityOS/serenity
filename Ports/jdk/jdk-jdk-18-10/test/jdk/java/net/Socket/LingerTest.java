/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4796166
 * @library /test/lib
 * @summary Linger interval delays usage of released file descriptor
 * @run main LingerTest
 * @run main/othervm -Djava.net.preferIPv4Stack=true LingerTest
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.IPSupport;

public class LingerTest {

    static class Sender implements Runnable {
        Socket s;

        public Sender(Socket s) {
            this.s = s;
        }

        public void run() {
            System.out.println ("Sender starts");
            try {
                s.getOutputStream().write(new byte[128*1024]);
            }
            catch (IOException ioe) {
            }
            System.out.println ("Sender ends");
        }
    }

    static class Closer implements Runnable {
        Socket s;

        public Closer(Socket s) {
            this.s = s;
        }

        public void run() {
            System.out.println ("Closer starts");
            try {
                s.close();
            }
            catch (IOException ioe) {
            }
            System.out.println ("Closer ends");
        }
    }

    static class Other implements Runnable {
        final InetAddress address;
        final int port;
        final long delay;
        boolean connected = false;

        public Other(InetAddress address, int port, long delay) {
            this.address = address;
            this.port = port;
            this.delay = delay;
        }

        public void run() {
            System.out.println ("Other starts: sleep " + delay);
            try {
                Thread.sleep(delay);
                System.out.println ("Other opening socket");
                Socket s = new Socket(address, port);
                synchronized (this) {
                    connected = true;
                }
                s.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
            System.out.println ("Other ends");
        }

        public synchronized boolean connected() {
            return connected;
        }
    }

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket(0, 50, loopback);

        Socket s1 = new Socket(loopback, ss.getLocalPort());
        Socket s2 = ss.accept();

        // setup conditions for untransmitted data and lengthy
        // linger interval
        s1.setSendBufferSize(128*1024);
        s1.setSoLinger(true, 30);
        s2.setReceiveBufferSize(1*1024);

        // start sender
        Thread senderThread = new Thread(new Sender(s1));
        senderThread.start();

        // other thread that will connect after 5 seconds.
        Other other = new Other(loopback, ss.getLocalPort(), 5000);
        Thread otherThread = new Thread(other);
        otherThread.start();

        // give sender time to queue the data
        System.out.println ("Main sleep 1000");
        Thread.sleep(1000);
        System.out.println ("Main continue");

        // close the socket asynchronously
        Thread closerThread = new Thread(new Closer(s1));
        closerThread.start();

        System.out.println ("Main sleep 15000");
        // give other time to run
        Thread.sleep(15000);
        System.out.println ("Main closing serversocket");

        ss.close();
        // check that other is done
        if (!other.connected()) {
            throw new RuntimeException("Other thread is blocked");
        }

        // await termination of all test related threads
        senderThread.join(60_000);
        otherThread.join(60_000);
        closerThread.join(60_000);

        System.out.println ("Main ends");
    }
}
