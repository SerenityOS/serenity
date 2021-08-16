/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import static org.testng.Assert.expectThrows;

/*
 * @test
 * @bug 8236105
 * @summary Check that DatagramSocket, MulticastSocket,
 *          DatagramSocketAdaptor and DatagramChannel all
 *          throw expected Execption when passed a DatagramPacket
 *          with invalid details
 * @run testng SendCheck
 */

public class SendCheck {
    private InetAddress loopbackAddr, wildcardAddr;
    static final Class<IOException> IOE = IOException.class;
    static final Class<SocketException> SE = SocketException.class;

    static final byte[] buf = {0, 1, 2};
    static DatagramSocket socket;

    @BeforeTest
    public void setUp() {
        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterTest
    public void closeDown() {
        socket.close();
    }

    static final class Packet {
        private Packet(String description, DatagramPacket packet) {
            this.description = description;
            this.packet = packet;
        }

        final String description;
        final DatagramPacket packet;

        public String toString() {
            return description;
        }

        public static Packet of(DatagramPacket packet) {
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            String description;
            if (address == null) {
                description = "<null>:" + port;
            } else if (port < 0) {
                description = packet.getAddress().toString() + ":" + port;
            } else {
                description = packet.getSocketAddress().toString();
            }
            return new Packet(description, packet);
        }
    }

    @DataProvider(name = "packets")
    Object[][] providerIO() throws IOException {
        loopbackAddr = InetAddress.getLoopbackAddress();
        wildcardAddr = new InetSocketAddress(0).getAddress();

        // loopback addr with no port set
        var pkt1 = new DatagramPacket(buf, 0, buf.length);
        pkt1.setAddress(loopbackAddr);

        // wildcard addr with no port set
        var pkt2 = new DatagramPacket(buf, 0, buf.length);
        pkt2.setAddress(wildcardAddr);

        /*
        Commented until JDK-8236807 is fixed

        // wildcard addr w/valid port
        var pkt3 = new DatagramPacket(buf, 0, buf.length);
        pkt3.setAddress(wildcardAddr);
        pkt3.setPort(socket.getLocalPort());
        */

        List<Packet> Packets = List.of(Packet.of(pkt1), Packet.of(pkt2));

        List<Sender> senders = List.of(
                Sender.of(new DatagramSocket(null)),
                Sender.of(new MulticastSocket(null), (byte) 0),
                Sender.of(DatagramChannel.open()),
                Sender.of(DatagramChannel.open().socket()),
                Sender.of((MulticastSocket)
                        DatagramChannel.open().socket(), (byte) 0)
        );

        List<Object[]> testcases = new ArrayList<>();
        for (var packet : Packets) {
            addTestCaseFor(testcases, senders, packet);
        }

        return testcases.toArray(new Object[0][0]);
    }

    static void addTestCaseFor(List<Object[]> testcases,
                               List<Sender> senders, Packet p) {
        for (var s : senders) {
            Object[] testcase = new Object[]{s, p, s.expectedException()};
            testcases.add(testcase);
        }
    }

    @Test(dataProvider = "packets")
    public static void sendCheck(Sender<IOException> sender,
                                        Packet packet,
                                        Class<? extends Throwable> exception) {
        DatagramPacket pkt = packet.packet;
        if (exception != null) {
            Throwable t = expectThrows(exception, () -> sender.send(pkt));
            System.out.printf("%s got expected exception %s%n",
                    packet.toString(), t);
        } else {
            try {
                sender.send(pkt);
            } catch (IOException e) {
                throw new AssertionError("Unexpected exception for "
                        + sender + " / " + packet, e);
            }
        }
    }

    interface Sender<E extends Exception> extends AutoCloseable {
        void send(DatagramPacket p) throws E;

        void close() throws E;

        Class<? extends E> expectedException();

        static Sender<IOException> of(DatagramSocket socket) {
            return new SenderImpl<>(socket, socket::send, socket::close, SE);
        }

        static Sender<IOException> of(MulticastSocket socket, byte ttl) {
            SenderImpl.Send<IOException> send =
                    (pkt) -> socket.send(pkt, ttl);
            return new SenderImpl<>(socket, send, socket::close, SE);
        }

        static Sender<IOException> of(DatagramChannel socket) {
            SenderImpl.Send<IOException> send =
                    (pkt) -> socket.send(ByteBuffer.wrap(pkt.getData()),
                            pkt.getSocketAddress());
            return new SenderImpl<>(socket, send, socket::close, SE);
        }
    }

    static final class SenderImpl<E extends Exception> implements Sender<E> {
        @FunctionalInterface
        interface Send<E extends Exception> {
            void send(DatagramPacket p) throws E;
        }

        @FunctionalInterface
        interface Closer<E extends Exception> {
            void close() throws E;
        }

        private final Send<E> send;
        private final Closer<E> closer;
        private final Object socket;
        private final Class<? extends E> expectedException;

        public SenderImpl(Object socket, Send<E> send, Closer<E> closer,
                          Class<? extends E> expectedException) {
            this.socket = socket;
            this.send = send;
            this.closer = closer;
            this.expectedException = expectedException;
        }

        @Override
        public void send(DatagramPacket p) throws E {
            send.send(p);
        }

        @Override
        public void close() throws E {
            closer.close();
        }

        @Override
        public Class<? extends E> expectedException() {
            return expectedException;
        }

        @Override
        public String toString() {
            return socket.getClass().getSimpleName();
        }
    }
}
