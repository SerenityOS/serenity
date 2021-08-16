/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package sun.management.jdp;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Enumeration;

/**
 * JdpBroadcaster is responsible for sending pre-built JDP packet across a Net
 *
 * <p> Multicast group address, port number and ttl have to be chosen on upper
 * level and passed to broadcaster constructor. Also it's possible to specify
 * source address to broadcast from. </p>
 *
 * <p>JdpBradcaster doesn't perform any validation on a supplied {@code port} and {@code ttl} because
 * the allowed values depend on an operating system setup</p>
 *
 */
public final class JdpBroadcaster {

    private final InetAddress addr;
    private final int port;
    private final DatagramChannel channel;

    /**
     * Create a new broadcaster
     *
     * @param address - multicast group address
     * @param srcAddress - address of interface we should use to broadcast.
     * @param port - udp port to use
     * @param ttl - packet ttl
     * @throws IOException
     */
    public JdpBroadcaster(InetAddress address, InetAddress srcAddress, int port, int ttl)
            throws IOException, JdpException {
        this.addr = address;
        this.port = port;

        ProtocolFamily family = (address instanceof Inet6Address)
                ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;

        channel = DatagramChannel.open(family);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, ttl);

        // with srcAddress equal to null, this constructor do exactly the same as
        // if srcAddress is not passed
        if (srcAddress != null) {
            // User requests particular interface to bind to
            NetworkInterface interf = NetworkInterface.getByInetAddress(srcAddress);

            if (interf == null) {
                throw new JdpException("Unable to get network interface for " + srcAddress.toString());
            }

            if (!interf.isUp()) {
                throw new JdpException(interf.getName() + " is not up.");
            }

            if (!interf.supportsMulticast()) {
                throw new JdpException(interf.getName() + " does not support multicast.");
            }

            try {
                channel.bind(new InetSocketAddress(srcAddress, 0));
            } catch (UnsupportedAddressTypeException ex) {
                throw new JdpException("Unable to bind to source address");
            }
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
        }
    }

    /**
     * Create a new broadcaster
     *
     * @param address - multicast group address
     * @param port - udp port to use
     * @param ttl - packet ttl
     * @throws IOException
     */
    public JdpBroadcaster(InetAddress address, int port, int ttl)
            throws IOException, JdpException {
        this(address, null, port, ttl);
    }

    /**
     * Broadcast pre-built packet
     *
     * @param packet - instance of JdpPacket
     * @throws IOException
     */
    public void sendPacket(JdpPacket packet)
            throws IOException {
        byte[] data = packet.getPacketData();
        // Unlike allocate/put wrap don't need a flip afterward
        ByteBuffer b = ByteBuffer.wrap(data);
        channel.send(b, new InetSocketAddress(addr, port));
    }

    /**
     * Shutdown broadcaster and close underlying socket channel
     *
     * @throws IOException
     */
    public void shutdown() throws IOException {
        channel.close();
    }
}
