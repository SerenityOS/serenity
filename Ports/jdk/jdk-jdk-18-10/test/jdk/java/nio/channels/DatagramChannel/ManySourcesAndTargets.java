/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234805 8235193
 * @summary Test DatagramChannel send/receive and that receive returns the expected
 *     sender address
 * @run main ManySourcesAndTargets
 * @run main/othervm -Djava.net.preferIPv4Stack=true ManySourcesAndTargets
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManySourcesAndTargets {
    public static void main(String[] args) throws Exception {

        // use addresses on interfaces that have the loopback and local host
        InetAddress lh = InetAddress.getLocalHost();
        InetAddress lb = InetAddress.getLoopbackAddress();
        List<InetAddress> addresses = Stream.of(lh, lb)
                .map(ManySourcesAndTargets::networkInterface)
                .flatMap(Optional::stream)
                .flatMap(NetworkInterface::inetAddresses)
                .filter(ia -> !ia.isAnyLocalAddress())
                .distinct()
                .collect(Collectors.toList());

        // Test DatagramChannel.send
        try (DatagramChannel reader = DatagramChannel.open()) {
            // bind reader to wildcard address so it can receive from any address
            reader.bind(new InetSocketAddress(0));
            for (InetAddress address : addresses) {
                System.out.format("%n-- %s --%n", address.getHostAddress());

                // send 3 datagrams from the given address to the reader
                testSend(3, address, reader);
            }
        }

        // Test DatagramChannel.receive
        try (DatagramChannel sender = DatagramChannel.open()) {
            // bind sender to wildcard address so it can send to any address
            sender.bind(new InetSocketAddress(0));
            for (InetAddress address : addresses) {
                System.out.format("%n-- %s --%n", address.getHostAddress());

                // send 3 datagrams to a datagram bound to the given address
                testReceive(3, sender, address);
            }
        }
    }

    /**
     * Creates a sender DatagramChannel bound to the given address and uses it to
     * sends datagrams to the given reader. The reader receives the datagrams and
     * checks the source/sender address.
     */
    static void testSend(int count, InetAddress address, DatagramChannel reader) throws Exception {
        int remotePort = reader.socket().getLocalPort();
        InetSocketAddress remote = new InetSocketAddress(address, remotePort);

        try (DatagramChannel sender = DatagramChannel.open()) {
            sender.bind(new InetSocketAddress(address, 0));

            SocketAddress local = sender.getLocalAddress();
            byte[] bytes = serialize(local);

            SocketAddress previousSource = null;
            for (int i = 0; i < count; i++) {
                System.out.format("send %s -> %s%n", local, remote);
                sender.send(ByteBuffer.wrap(bytes), remote);

                ByteBuffer bb = ByteBuffer.allocate(1000);
                SocketAddress source = reader.receive(bb);
                System.out.format("received datagram from %s%n", source);

                // check source address and payload
                SocketAddress payload = deserialize(bb.array());
                if (!source.equals(local))
                    throw new RuntimeException("source=" + source + ", expected=" + local);
                if (!payload.equals(local))
                    throw new RuntimeException("payload=" + payload + ", expected=" + local);

                // check that cached source was used
                if (previousSource == null) {
                    previousSource = source;
                } else if (source != previousSource) {
                    throw new RuntimeException("Cached SocketAddress not returned");
                }
            }
        }
    }

    /**
     * Creates a reader DatagramChannel bound to the given address uses the given
     * sender to send datagrams to that reader. The reader receives the datagrams.
     */
    static void testReceive(int count, DatagramChannel sender, InetAddress address) throws Exception {
        SocketAddress local = sender.getLocalAddress();

        try (DatagramChannel reader = DatagramChannel.open()) {
            // bind to the given address
            reader.bind(new InetSocketAddress(address, 0));

            SocketAddress remote = reader.getLocalAddress();

            for (int i = 0; i < count; i++) {
                System.out.format("send %s -> %s%n", local, remote);
                reader.send(ByteBuffer.allocate(32), remote);

                ByteBuffer bb = ByteBuffer.allocate(1000);
                SocketAddress source = reader.receive(bb);
                System.out.format("received datagram from %s%n", source);
            }
        }
    }

    private static byte[] serialize(SocketAddress address) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(address);
        oos.close();
        return baos.toByteArray();
    }

    private static SocketAddress deserialize(byte[] bytes) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (SocketAddress) ois.readObject();
    }

    private static Optional<NetworkInterface> networkInterface(InetAddress ia) {
        try {
            return Optional.ofNullable(NetworkInterface.getByInetAddress(ia));
        } catch (SocketException e) {
            return Optional.empty();
        }
    }
}