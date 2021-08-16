/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 *
 * @bug 4095393
 * @test main SendSize
 * @summary this tests a regression in 1.2, beta 2 and earlier where
 * the DatagramPackets sent the entire buffer, not the buffer length
 * of the packet.
 *
 * @author Benjamin Renaud
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SendSize {
    static final int bufferLength = 512;
    static final int packetLength = 256;

    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(
                new InetSocketAddress(InetAddress.getLocalHost(), 0)
        );
        Thread server = new ServerThread(serverSocket);
        server.start();
        Thread client = new ClientThread(serverSocket.getLocalPort());
        client.start();
        server.join();
        client.join();
    }

    static class ServerThread extends Thread {
        DatagramSocket server;

        ServerThread(DatagramSocket server) {
            this.server = server;
        }

        public void run() {
            try {
                System.err.println("started server thread: " + server);
                byte[] buf = new byte[1024];
                for (int i = 0; i < 10; i++) {
                    DatagramPacket receivePacket = new DatagramPacket(buf,
                            buf.length);
                    server.receive(receivePacket);
                    int len = receivePacket.getLength();
                    switch (len) {
                        case packetLength:
                            System.out.println("receive length is: " + len);
                            break;
                        default:
                            throw new RuntimeException(
                                    "receive length is: " + len +
                                            ", send length: " + packetLength +
                                            ", buffer length: " + buf.length);
                    }
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("caught: " + e);
            } finally {
                if (server != null) { server.close(); }
            }
        }
    }

    static class ClientThread extends Thread {

        int serverPort;
        DatagramSocket client;
        InetAddress host;

        ClientThread(int serverPort)throws IOException {
            this.serverPort = serverPort;
            this.host = InetAddress.getLocalHost();
            this.client = new DatagramSocket();
        }

        public void run() {
            try {
                System.err.println("started client thread: " + client);
                byte[] buf = new byte[bufferLength];
                DatagramPacket sendPacket =
                    new DatagramPacket(buf, packetLength,
                                       host, serverPort);
                for (int i = 0; i < 10; i++) {
                    client.send(sendPacket);
                }
                System.err.println("sent 10 packets");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("caught: " + e);
            } finally {
                if (client != null) { client.close(); }
            }
        }
    }
}
