/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * Used in conjunction to EchoService to test System.inheritedChannel().
 *
 * The first test is the TCP echo test. A service is launched with a TCP
 * socket and a TCP message is sent to the service. The test checks that
 * the message is correctly echoed.
 *
 * The second test is a UDP echo test. A service is launched with a UDP
 * socket and a UDP packet is sent to the service. The test checks that
 * the packet is correctly echoed.
 *
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;

import jdk.test.lib.Utils;

public class EchoTest {

    private static int failures = 0;

    private static String ECHO_SERVICE = "EchoService";

    /*
     * Sends a message with random bytes to the service, and then waits for
     * a reply (with timeout). Once the reply is received it is checked to ensure
     * that it matches the original message.
     */
    private static void TCPEchoTest() throws IOException {
        SocketChannel sc = Launcher.launchWithInetSocketChannel(ECHO_SERVICE, null);

        String msg = "Where's that damn torpedo?";
        int repeat = 100;
        int size = msg.length() * repeat;

        // generate bytes into a buffer and send it to the service

        ByteBuffer bb1 = ByteBuffer.allocate(size);
        Random gen = new Random();
        for (int i=0; i<repeat; i++) {
            bb1.put(msg.getBytes("UTF-8"));
        }
        bb1.flip();
        sc.write(bb1);

        // now we put the channel into non-blocking mode and we read the
        // reply from the service into a second buffer.

        ByteBuffer bb2 = ByteBuffer.allocate(size+100);
        sc.configureBlocking(false);
        Selector sel = sc.provider().openSelector();
        SelectionKey sk = sc.register(sel, SelectionKey.OP_READ);
        int nread = 0;
        long to = Utils.adjustTimeout(5000);
        while (nread < size) {
            long st = System.currentTimeMillis();
            sel.select(to);
            if (sk.isReadable()) {
                int n = sc.read(bb2);
                if (n > 0) {
                    nread += n;
                }
                if (n < 0) {
                    break;              // EOF
                }
            }
            sel.selectedKeys().remove(sk);
            to -= System.currentTimeMillis() - st;
            if (to <= 0) {
                break;
            }
        }
        sc.close();

        // and compare the response

        boolean err = false;

        if (nread != size) {
            err = true;
        } else {
            bb1.flip();
            bb2.flip();
            while (bb1.hasRemaining()) {
                if (bb1.get() != bb2.get()) {
                    err = true;
                }
            }
        }

        // if error print out the response from the service (could be a stack trace)
        if (err) {
            System.err.println("Bad response or premature EOF, bytes read: ");
            bb2.flip();
            while (bb2.hasRemaining()) {
                char c = (char)bb2.get();
                System.out.print(c);
            }
            throw new RuntimeException("Bad response or premature EOF from service");
        }
    }

    /*
     * Send a UDP packet to the service, wait for a reply (with timeout). Finally
     * check that the packet is the same length as the original.
     */
    private static void UDPEchoTest() throws IOException {
        DatagramChannel dc = Launcher.launchWithDatagramChannel(ECHO_SERVICE, null);

        String msg = "I was out saving the galaxy when your grandfather was in diapers";

        ByteBuffer bb = ByteBuffer.wrap(msg.getBytes("UTF-8"));
        dc.write(bb);

        // and receive the echo
        byte b[] = new byte[msg.length() + 100];
        DatagramPacket pkt2 = new DatagramPacket(b, b.length);
        dc.socket().setSoTimeout((int)Utils.adjustTimeout(5000));
        dc.socket().receive(pkt2);

        if (pkt2.getLength() != msg.length()) {
            throw new RuntimeException("Received packet of incorrect length");
        }

        dc.close();
    }

    public static void main(String args[]) throws IOException {

        // TCP echo
        try {
            TCPEchoTest();
            System.out.println("TCP echo test passed.");
        } catch (Exception x) {
            System.err.println(x);
            failures++;
        }

        // UDP echo
        try {
            UDPEchoTest();
            System.out.println("UDP echo test passed.");
        } catch (Exception x) {
            x.printStackTrace();
            System.err.println(x);
            failures++;
        }

        if (failures > 0) {
            throw new RuntimeException("Test failed - see log for details");
        }
    }

}
