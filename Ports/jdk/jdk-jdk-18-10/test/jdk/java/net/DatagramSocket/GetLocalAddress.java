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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.channels.DatagramChannel;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @test
 * @run main/othervm GetLocalAddress
 * @summary Check that getLocalAddress() and getLocalSocketAddress()
 *          work as specified
 */

public class GetLocalAddress {
    static final Map<DatagramSocket, InetAddress> addressMap = new LinkedHashMap<>();
    static final Set<DatagramSocket> toClose = new LinkedHashSet<>();

    public static void main(String[] args) throws Exception {
        try {
            testAllSockets();
        } finally {
            cleanup();
        }
    }

    static void testAllSockets() throws IOException {
        var address = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        bindAndAddToMap(new DatagramSocket(null), address);
        bindAndAddToMap(new MulticastSocket(null), address);
        bindAndAddToMap(DatagramChannel.open().socket(), address);
        addToMap(new DatagramSocket(address));
        addToMap(new MulticastSocket(address));
        addToMap(DatagramChannel.open().bind(address).socket());
        testSocket();
        testAfterClose();
    }

    static void bindAndAddToMap(DatagramSocket socket, InetSocketAddress address)
            throws IOException
    {
        toClose.add(socket);
        testNullAddress(socket, socket.getLocalSocketAddress(), "before bind");
        testWildcardAddress(socket, socket.getLocalAddress(), "before bind");
        socket.bind(address);
        addressMap.put(socket, ((InetSocketAddress)socket.getLocalSocketAddress())
                .getAddress());
    }

    static void addToMap(DatagramSocket socket) throws IOException {
        toClose.add(socket);
        addressMap.put(socket, ((InetSocketAddress)socket.getLocalSocketAddress())
                .getAddress());
    }

    static void testSocket() throws IOException {
        for (var entry : addressMap.entrySet()) {
            var socket = entry.getKey();
            checkAddresses(socket, entry.getValue(),
                    ((InetSocketAddress)entry.getKey().getLocalSocketAddress())
                                                      .getAddress());
            checkAddresses(socket, entry.getValue(),
                    entry.getKey().getLocalAddress());
        }
    }

    static void testAfterClose() throws IOException {
        for (var entry : addressMap.entrySet()) {
            var socket = entry.getKey();
            socket.close();
            toClose.remove(socket);
            testNullAddress(socket, socket.getLocalSocketAddress(), "after close");
            testNullAddress(socket, socket.getLocalAddress(), "after close");
        }
    }

    static void checkAddresses(DatagramSocket socket, InetAddress a1, InetAddress a2) {
        System.out.println(socket.getClass() + ": Address1: "
                + a1.toString() + " Address2: " + a2.toString());
        if (!a1.getHostAddress().equals(a2.getHostAddress()))
        {
            throw new RuntimeException("Local address don't match for " + socket.getClass());
        }
    }

    static void testNullAddress(DatagramSocket socket, Object address, String when) {
        System.out.println(socket.getClass() + ": Checking address " + when);
        if (address != null) {
            throw new RuntimeException("Expected null address " + when + ", got: "
                    + address +  " for " + socket.getClass());
        }
    }

    static void testWildcardAddress(DatagramSocket socket, InetAddress address, String when) {
        System.out.println(socket.getClass() + ": Checking address " + when);
        if (address == null || !address.isAnyLocalAddress()) {
            throw new RuntimeException("Expected wildcard address " + when + ", got: "
                    + address +  " for " + socket.getClass());
        }
    }

    static void cleanup() {
        // Some socket might not have been closed if
        // the test failed in exception.
        // forcing cleanup here.
        toClose.forEach(GetLocalAddress::close);
        toClose.clear();
        addressMap.clear();
    }

    static void close(DatagramSocket socket) {
        try {
            socket.close();
        } catch (Throwable ignore) { }
    }

}
