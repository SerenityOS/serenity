/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4742177
 * @library /test/lib
 * @summary Re-test IPv6 (and specifically MulticastSocket) with latest Linux & USAGI code
 */
import java.util.*;
import java.net.*;
import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

public class NoLoopbackPackets {
    private static String osname;

    static boolean isWindows() {
        if (osname == null)
            osname = System.getProperty("os.name");
        return osname.contains("Windows");
    }

    private static final String MESSAGE = "hello world (" + System.nanoTime() + ")";
    public static void main(String[] args) throws Exception {
        if (isWindows()) {
            System.out.println("The test only run on non-Windows OS. Bye.");
            return;
        }

        MulticastSocket msock = null;
        List<SocketAddress> failedGroups = new ArrayList<SocketAddress>();
        Sender sender = null;
        Thread senderThread = null;
        try {
            msock = new MulticastSocket();
            int port = msock.getLocalPort();

            // we will send packets to three multicast groups :-
            // 224.1.1.1, ::ffff:224.1.1.2, and ff02::1:1
            //
            List<SocketAddress> groups = new ArrayList<SocketAddress>();
            if (IPSupport.hasIPv4()) {
                groups.add(new InetSocketAddress(InetAddress.getByName("224.1.1.1"), port));
            }

            NetworkConfiguration nc = NetworkConfiguration.probe();
            if (IPSupport.hasIPv6() && nc.hasTestableIPv6Address()) {
                groups.add(new InetSocketAddress(InetAddress.getByName("::ffff:224.1.1.2"), port));
                groups.add(new InetSocketAddress(InetAddress.getByName("ff02::1:1"), port));
            }
            if (groups.isEmpty()) {
                System.err.println("Nothing to test: there are no network interfaces");
            }

            sender = new Sender(groups);
            senderThread = new Thread(sender);
            senderThread.start();

            // Now try to receive multicast packets. we should not see any of them
            // since we disable loopback mode.
            //
            msock.setSoTimeout(5000);       // 5 seconds

            byte[] buf = new byte[1024];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) 'z';
            }
            DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
            byte[] expected = MESSAGE.getBytes();
            assert expected.length <= buf.length;
            for (SocketAddress group : groups) {
                System.out.println("joining group: " + group);
                msock.joinGroup(group, null);

                try {
                    do {
                        for (int i = 0; i < buf.length; i++) {
                            buf[i] = (byte) 'a';
                        }
                        msock.receive(packet);
                        byte[] data = packet.getData();
                        int len = packet.getLength();

                        if (expected(data, len, expected)) {
                            failedGroups.add(group);
                            break;
                        } else {
                            System.err.println("WARNING: Unexpected packet received from "
                                               + group + ": "
                                               + len + " bytes");
                            System.err.println("\t as text: " + new String(data, 0, len));
                        }
                    } while (true);
                } catch (SocketTimeoutException e) {
                    // we expect this
                    System.out.println("Received expected exception from " + group + ": " + e);
                } finally {
                    msock.leaveGroup(group, null);
                }
            }
        } finally {
            if (msock != null) try { msock.close(); } catch (Exception e) {}
            if (sender != null) {
                sender.stop();
            }
        }
        try {
            if (failedGroups.size() > 0) {
                System.out.println("We should not receive anything from following groups, but we did:");
                for (SocketAddress group : failedGroups)
                    System.out.println(group);
                throw new RuntimeException("test failed.");
            }
        } finally {
            if (senderThread != null) {
                senderThread.join();
            }
        }
    }

    static boolean expected(byte[] data, int len, byte[] expected) {
        if (len != expected.length) return false;
        for (int i = 0; i < len; i++) {
            if (data[i] != expected[i]) return false;
        }
        return true;
    }

    static class Sender implements Runnable {
        private List<SocketAddress> sendToGroups;
        private volatile boolean stop;

        public Sender(List<SocketAddress> groups) {
            sendToGroups = groups;
        }

        public void run() {
            byte[] buf = MESSAGE.getBytes();
            List<DatagramPacket> packets = new ArrayList<DatagramPacket>();

            try (MulticastSocket msock = new MulticastSocket()) {
                for (SocketAddress group : sendToGroups) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, group);
                    packets.add(packet);
                }

                msock.setLoopbackMode(true);    // disable loopback mode
                while (!stop) {
                    for (DatagramPacket packet : packets) {
                        msock.send(packet);
                    }

                    Thread.sleep(1000);     // 1 second
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void stop() {
            stop = true;
        }
    }
}
