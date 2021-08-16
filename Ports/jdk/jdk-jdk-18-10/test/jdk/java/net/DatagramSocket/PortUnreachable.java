/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4361783
 * @key intermittent
 * @summary Test to see if ICMP Port Unreachable on non-connected
 *          DatagramSocket causes a SocketException "socket closed"
 *          exception on Windows 2000.
 * @run main/othervm PortUnreachable
 */

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class PortUnreachable {

    DatagramSocket clientSock;
    int serverPort;
    int clientPort;

    public void serverSend() throws Exception {
        InetAddress addr = InetAddress.getLocalHost();
        Thread.sleep(1000);
        // send a delayed packet which should mean a delayed icmp
        // port unreachable
        byte b[] = "A late msg".getBytes();
        DatagramPacket packet = new DatagramPacket(b, b.length, addr,
                serverPort);
        clientSock.send(packet);

        DatagramSocket sock = recreateServerSocket(serverPort);
        b = "Greetings from the server".getBytes();
        packet = new DatagramPacket(b, b.length, addr, clientPort);
        sock.send(packet);
        Thread.sleep(500);  // give time to the kernel to send packet
        sock.close();
    }

    DatagramSocket recreateServerSocket (int serverPort) throws Exception {
        DatagramSocket serverSocket = null;
        int retryCount = 0;
        long sleeptime = 0;
        System.out.println("Attempting to recreate server socket with port: " +
                serverPort);
        // it's possible that this method intermittently fails, if some other
        // process running on the machine grabs the port we want before us,
        // and doesn't release it before the 10 * 500 ms are elapsed...
        while (serverSocket == null) {
            try {
                serverSocket = new DatagramSocket(serverPort, InetAddress.getLocalHost());
            } catch (BindException bEx) {
                if (retryCount++ < 10) {
                    sleeptime += sleepAtLeast(500);
                } else {
                    System.out.println("Give up after 10 retries and " + sleeptime(sleeptime));
                    System.out.println("Has some other process grabbed port " + serverPort + "?");
                    throw bEx;
                }
            }
        }

        System.out.println("PortUnreachableTest.recreateServerSocket: returning socket == "
                + serverSocket.getLocalAddress() + ":" + serverSocket.getLocalPort()
                + " obtained at " + attempt(retryCount) + " attempt with " + sleeptime(sleeptime));
        return serverSocket;
    }

    long sleepAtLeast(long millis) throws Exception {
        long start = System.nanoTime();
        long ms = millis;
        while (ms > 0) {
            assert ms < Long.MAX_VALUE/1000_000L;
            Thread.sleep(ms);
            long elapsedms = (System.nanoTime() - start)/1000_000L;
            ms = millis - elapsedms;
        }
        return millis - ms;
    }

    String attempt(int retry) {
        switch (retry) {
            case 0: return "first";
            case 1: return "second";
            case 2: return "third";
            default: return retry + "th";
        }
    }

    String sleeptime(long millis) {
        if (millis == 0) return "no sleep";
        long sec = millis / 1000L;
        long ms =  millis % 1000L;
        String sleeptime = "";
        if (millis > 0) {
           if (sec > 0) {
               sleeptime = "" + sec + " s" +
                   (ms > 0 ? " " : "");
            }
            if (ms > 0 ) {
                sleeptime += ms + " ms";
            }
        } else sleeptime = millis + " ms"; // should not happen
        return sleeptime + " of sleep time";
    }

    PortUnreachable() throws Exception {
        clientSock = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        clientPort = clientSock.getLocalPort();

    }

    void execute () throws Exception{

        // pick a port for the server
        DatagramSocket sock2 = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        serverPort = sock2.getLocalPort();

        // send a burst of packets to the unbound port - we should get back
        // icmp port unreachable messages
        //
        InetAddress addr = InetAddress.getLocalHost();
        byte b[] = "Hello me".getBytes();
        DatagramPacket packet = new DatagramPacket(b, b.length, addr,
                                                   serverPort);
        //close just before sending
        sock2.close();
        for (int i=0; i<100; i++)
            clientSock.send(packet);

        serverSend();

        // try to receive
        b = new byte[25];
        packet = new DatagramPacket(b, b.length, addr, serverPort);
        clientSock.setSoTimeout(10000);
        clientSock.receive(packet);
        System.out.println("client received data packet " + new String(packet.getData()));

        // done
        clientSock.close();
    }

    public static void main(String[] args) throws Exception {
        // A BindException might be thrown intermittently. In that case retry
        // 3 times before propagating the exception to finish execution.
        int catchCount = 0;

        while (true) {
            try {
                PortUnreachable test = new PortUnreachable();
                test.execute();
                return;
            } catch (BindException bEx) {
                System.out.println("Failed to bind server: " + bEx);
                if (++catchCount > 3) {
                    System.out.printf("Max retry count exceeded (%d)%n", catchCount);
                    throw bEx;
                }
                System.out.printf("Retrying; retry count: %d%n", catchCount);
            }
        }
    }
}
