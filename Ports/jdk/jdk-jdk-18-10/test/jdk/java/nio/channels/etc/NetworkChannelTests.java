/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4640544
 * @summary Unit test for channels that implement NetworkChannel
 */

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import java.util.*;

public class NetworkChannelTests {

    static interface ChannelFactory {
        NetworkChannel open() throws IOException;
    }

    static class BogusSocketAddress extends SocketAddress {
    }

    /**
     * Exercise bind method.
     */
    static void bindTests(ChannelFactory factory) throws IOException {
        NetworkChannel ch;

        // AlreadyBoundException
        ch = factory.open().bind(new InetSocketAddress(0));
        try {
            ch.bind(new InetSocketAddress(0));
            throw new RuntimeException("AlreadyBoundException not thrown");
        } catch (AlreadyBoundException x) {
        }
        ch.close();

        // bind(null)
        ch = factory.open().bind(null);
        if (ch.getLocalAddress() == null)
            throw new RuntimeException("socket not found");
        ch.close();

        // UnsupportedAddressTypeException
        ch = factory.open();
        try {
            ch.bind(new BogusSocketAddress());
            throw new RuntimeException("UnsupportedAddressTypeException not thrown");
        } catch (UnsupportedAddressTypeException x) {
        }
        ch.close();

        // ClosedChannelException
        try {
            ch.bind(new InetSocketAddress(0));
            throw new RuntimeException("ClosedChannelException not thrown");
        } catch (ClosedChannelException x) {
        }
    }

    /**
     * Exercise getLocalAddress method.
     */
    static void localAddressTests(ChannelFactory factory) throws IOException {
        NetworkChannel ch;

        // not bound
        ch = factory.open();
        if (ch.getLocalAddress() != null) {
            throw new RuntimeException("Local address returned when not bound");
        }

        // bound
        InetSocketAddress local =
            (InetSocketAddress)(ch.bind(new InetSocketAddress(0)).getLocalAddress());
        if (!local.getAddress().isAnyLocalAddress()) {
            if (NetworkInterface.getByInetAddress(local.getAddress()) == null)
                throw new RuntimeException("not bound to local address");
        }
        if (local.getPort() <= 0)
            throw new RuntimeException("not bound to local port");

        // closed
        ch.close();
        try {
            ch.getLocalAddress();
            throw new RuntimeException("ClosedChannelException expected");
        } catch (ClosedChannelException e) { }
    }

    /**
     * Exercise getRemoteAddress method (SocketChannel only)
     */
    static void connectedAddressTests() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open()
            .bind(new InetSocketAddress(0));
        InetSocketAddress local = (InetSocketAddress)(ssc.getLocalAddress());
        int port = local.getPort();
        InetSocketAddress server = new InetSocketAddress(InetAddress.getLocalHost(), port);

        SocketChannel sc = SocketChannel.open();

        // not connected
        if (sc.getRemoteAddress() != null)
            throw new RuntimeException("getRemoteAddress returned address when not connected");

        // connected
        sc.connect(server);
        SocketAddress remote = sc.getRemoteAddress();
        if (!remote.equals(server))
            throw new RuntimeException("getRemoteAddress returned incorrect address");

        // closed
        sc.close();
        try {
            sc.getRemoteAddress();
            throw new RuntimeException("ClosedChannelException expected");
        } catch (ClosedChannelException e) { }

        ssc.close();
    }

    public static void main(String[] args) throws IOException {
        ChannelFactory factory;

        // -- SocketChannel --

        factory = new ChannelFactory() {
            public NetworkChannel open() throws IOException {
                return SocketChannel.open();
            }
        };

        bindTests(factory);
        localAddressTests(factory);
        connectedAddressTests();

        // -- ServerSocketChannel --

        factory = new ChannelFactory() {
            public NetworkChannel open() throws IOException {
                return ServerSocketChannel.open();
            }
        };

        bindTests(factory);
        localAddressTests(factory);

        // backlog values
        ServerSocketChannel.open()
            .bind(new InetSocketAddress(0), 100).close();
        ServerSocketChannel.open()
            .bind(new InetSocketAddress(0), 0).close();
        ServerSocketChannel.open()
            .bind(new InetSocketAddress(0), -1).close();

        // -- DatagramChannel --

        factory = new ChannelFactory() {
            public NetworkChannel open() throws IOException {
                return DatagramChannel.open();
            }
        };

        bindTests(factory);
        localAddressTests(factory);
    }

}
