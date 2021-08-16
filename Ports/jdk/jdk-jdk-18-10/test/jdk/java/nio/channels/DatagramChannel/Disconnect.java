/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7132924
 * @library /test/lib
 * @key intermittent
 * @summary Test DatagramChannel.disconnect when DatagramChannel is connected to an IPv4 socket
 * @run main Disconnect
 * @run main/othervm -Djava.net.preferIPv4Stack=true Disconnect
 */

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import jdk.test.lib.net.IPSupport;

public class Disconnect {
    public static void main(String[] args) throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();

        // test with default protocol family
        try (DatagramChannel dc = DatagramChannel.open()) {
            test(dc);
            test(dc);
        }

        if (IPSupport.hasIPv4()) {
            // test with IPv4 only
            try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)) {
                test(dc);
                test(dc);
            }
        }

        if (IPSupport.hasIPv6()) {
            // test with IPv6 only
            try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET6)) {
                test(dc);
                test(dc);
            }
        }
    }

    /**
     * Connect DatagramChannel to a server, write a datagram and disconnect. Invoke
     * a second or subsequent time with the same DatagramChannel instance to check
     * that disconnect works as expected.
     */
    static void test(DatagramChannel dc) throws IOException {
        try (DatagramChannel server = DatagramChannel.open()) {
            server.bind(new InetSocketAddress(0));

            InetAddress lh = InetAddress.getLocalHost();
            dc.connect(new InetSocketAddress(lh, server.socket().getLocalPort()));

            dc.write(ByteBuffer.wrap("hello".getBytes()));

            ByteBuffer bb = ByteBuffer.allocate(100);
            server.receive(bb);

            dc.disconnect();

            try {
                dc.write(ByteBuffer.wrap("another message".getBytes()));
                throw new RuntimeException("write should fail, not connected");
            } catch (NotYetConnectedException expected) {
            }
        }
    }
}
