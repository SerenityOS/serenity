/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198753
 * @summary Test DatagramChannel connect exceptions
 * @library ..
 * @run testng ConnectExceptions
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConnectExceptions {
    static DatagramChannel sndChannel;
    static DatagramChannel rcvChannel;
    static InetSocketAddress sender;
    static InetSocketAddress receiver;

    @BeforeTest
    public static void setup() throws Exception {
        sndChannel = DatagramChannel.open();
        sndChannel.bind(null);
        InetAddress address = InetAddress.getLocalHost();
        if (address.isLoopbackAddress()) {
            address = InetAddress.getLoopbackAddress();
        }
        sender = new InetSocketAddress(address,
            sndChannel.socket().getLocalPort());

        rcvChannel = DatagramChannel.open();
        rcvChannel.bind(null);
        receiver = new InetSocketAddress(address,
            rcvChannel.socket().getLocalPort());
    }

    @Test(expectedExceptions = UnsupportedAddressTypeException.class)
    public static void unsupportedAddressTypeException() throws Exception {
        rcvChannel.connect(sender);
        sndChannel.connect(new SocketAddress() {});
    }

    @Test(expectedExceptions = UnresolvedAddressException.class)
    public static void unresolvedAddressException() throws Exception {
        String host = TestUtil.UNRESOLVABLE_HOST;
        InetSocketAddress unresolvable = new InetSocketAddress (host, 37);
        sndChannel.connect(unresolvable);
    }

    @Test(expectedExceptions = AlreadyConnectedException.class)
    public static void alreadyConnectedException() throws Exception {
        sndChannel.connect(receiver);
        InetSocketAddress random = new InetSocketAddress(0);
        sndChannel.connect(random);
    }

    @AfterTest
    public static void cleanup() throws Exception {
        rcvChannel.close();
        sndChannel.close();
    }
}
