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

/*
 * @test
 * @bug 8221481
 * @compile/module=java.base java/net/PlatformSocketImpl.java
 * @run testng/othervm BadUsages
 * @summary Test the platform SocketImpl when used in unintended ways
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.SocketOptions;
import java.net.StandardSocketOptions;
import java.util.Set;

import java.net.PlatformSocketImpl;  // test helper

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * SocketImpl does not specify how the SocketImpl behaves when used in ways
 * that are not intended, e.g. invoking socket operations before the socket is
 * created or trying to establish a connection after the socket is connected or
 * closed.
 *
 * This test exercises the platform SocketImpl to test that it is reliable, and
 * throws reasonable exceptions, for these scenarios.
 */

@Test
public class BadUsages {

    /**
     * Test create when already created.
     */
    public void testCreate1() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            expectThrows(IOException.class, () -> impl.create(true));
        }
    }

    /**
     * Test create when closed.
     */
    public void testCreate2() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class, () -> impl.create(true));
    }

    /**
     * Test connect when not created.
     */
    public void testConnect1() throws IOException {
        try (var ss = new ServerSocket(0)) {
            var impl = new PlatformSocketImpl(false);
            var address = ss.getInetAddress();
            int port = ss.getLocalPort();
            expectThrows(IOException.class, () -> impl.connect(address, port));
        }
    }

    /**
     * Test connect with unsupported address type.
     */
    public void testConnect2() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            var remote = new SocketAddress() { };
            expectThrows(IOException.class, () -> impl.connect(remote, 0));
        }
    }

    /**
     * Test connect with an unresolved address.
     */
    public void testConnect3() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            var remote = new InetSocketAddress("blah-blah.blah-blah", 80);
            expectThrows(IOException.class, () -> impl.connect(remote, 0));
        }
    }

    /**
     * Test connect when already connected.
     */
    public void testConnect4() throws IOException {
        try (var ss = new ServerSocket();
             var impl = new PlatformSocketImpl(false)) {
            var loopback = InetAddress.getLoopbackAddress();
            ss.bind(new InetSocketAddress(loopback, 0));
            impl.create(true);
            int port = ss.getLocalPort();
            impl.connect(loopback, port);
            expectThrows(IOException.class, () -> impl.connect(loopback, port));
        }
    }

    /**
     * Test connect when closed.
     */
    public void testConnect5() throws IOException {
        try (var ss = new ServerSocket(0)) {
            var impl = new PlatformSocketImpl(false);
            impl.close();
            String host = ss.getInetAddress().getHostAddress();
            int port = ss.getLocalPort();
            expectThrows(IOException.class, () -> impl.connect(host, port));
        }
    }

    /**
     * Test bind when not created.
     */
    public void testBind1() throws IOException {
        var impl = new PlatformSocketImpl(false);
        var loopback = InetAddress.getLoopbackAddress();
        expectThrows(IOException.class, () -> impl.bind(loopback, 0));
    }

    /**
     * Test bind when already bound.
     */
    public void testBind2() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            var loopback = InetAddress.getLoopbackAddress();
            impl.bind(loopback, 0);
            expectThrows(IOException.class, () -> impl.bind(loopback, 0));
        }
    }

    /**
     * Test bind when connected.
     */
    public void testBind3() throws IOException {
        try (var ss = new ServerSocket();
             var impl = new PlatformSocketImpl(false)) {
            var loopback = InetAddress.getLoopbackAddress();
            ss.bind(new InetSocketAddress(loopback, 0));
            impl.create(true);
            impl.connect(ss.getLocalSocketAddress(), 0);
            expectThrows(IOException.class, () -> impl.bind(loopback, 0));
        }
    }

    /**
     * Test bind when closed.
     */
    public void testBind4() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        var loopback = InetAddress.getLoopbackAddress();
        expectThrows(IOException.class, () -> impl.bind(loopback, 0));
    }


    /**
     * Test listen when not created.
     */
    public void testListen1() {
        var impl = new PlatformSocketImpl(false);
        expectThrows(IOException.class, () -> impl.listen(16));
    }

    /**
     * Test listen when not bound.
     */
    public void testListen2() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            expectThrows(IOException.class, () -> impl.listen(16));
        }
    }

    /**
     * Test listen when closed.
     */
    public void testListen3() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class, () -> impl.listen(16));
    }

    /**
     * Test accept when not created.
     */
    public void testAccept1() throws IOException {
        var impl = new PlatformSocketImpl(true);
        var si = new PlatformSocketImpl(false);
        expectThrows(IOException.class, () -> impl.accept(si));
    }

    /**
     * Test accept when not bound.
     */
    public void testAccept2() throws IOException {
        try (var impl = new PlatformSocketImpl(true)) {
            impl.create(true);
            var si = new PlatformSocketImpl(false);
            expectThrows(IOException.class, () -> impl.accept(si));
        }
    }

    /**
     * Test accept when not a stream socket.
     */
    public void testAccept3() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(false);
            impl.bind(InetAddress.getLoopbackAddress(), 0);
            var si = new PlatformSocketImpl(false);
            expectThrows(IOException.class, () -> impl.accept(si));
        }
    }

    /**
     * Test accept when closed.
     */
    public void testAccept4() throws IOException {
        var impl = new PlatformSocketImpl(true);
        impl.close();
        var si = new PlatformSocketImpl(false);
        expectThrows(IOException.class, () -> impl.accept(si));
    }

    /**
     * Test accept with SocketImpl that is already created.
     */
    public void testAccept5() throws IOException {
        try (var impl = new PlatformSocketImpl(true);
             var si = new PlatformSocketImpl(false)) {
            impl.create(true);
            impl.bind(InetAddress.getLoopbackAddress(), 0);
            si.create(true);
            expectThrows(IOException.class, () -> impl.accept(si));
        }
    }

    /**
     * Test accept with SocketImpl that is closed.
     */
    public void testAccept6() throws IOException {
        try (var impl = new PlatformSocketImpl(true);
             var si = new PlatformSocketImpl(false)) {
            impl.create(true);
            impl.bind(InetAddress.getLoopbackAddress(), 0);
            si.create(true);
            si.close();
            expectThrows(IOException.class, () -> impl.accept(si));
        }
    }

    /**
     * Test available when not created.
     */
    public void testAvailable1() throws IOException {
        var impl = new PlatformSocketImpl(false);
        expectThrows(IOException.class, () -> impl.available());
    }

    /**
     * Test available when created but not connected.
     */
    public void testAvailable2() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            expectThrows(IOException.class, () -> impl.available());
        }
    }

    /**
     * Test available when closed.
     */
    public void testAvailable3() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class, () -> impl.available());
    }

    /**
     * Test setOption when not created.
     */
    public void testSetOption1() throws IOException {
        var impl = new PlatformSocketImpl(false);
        expectThrows(IOException.class,
                     () -> impl.setOption(StandardSocketOptions.SO_REUSEADDR, true));
        // legacy
        expectThrows(SocketException.class,
                     () -> impl.setOption(SocketOptions.SO_REUSEADDR, true));
    }

    /**
     * Test setOption when closed.
     */
    public void testSetOption2() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class,
                     () -> impl.setOption(StandardSocketOptions.SO_REUSEADDR, true));
        // legacy
        expectThrows(SocketException.class,
                     () -> impl.setOption(SocketOptions.SO_REUSEADDR, true));
    }

    /**
     * Test setOption with unsupported option.
     */
    public void testSetOption3() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            var opt = new SocketOption<String>() {
                @Override public String name() { return "birthday"; }
                @Override public Class<String> type() { return String.class; }
            };
            expectThrows(UnsupportedOperationException.class, () -> impl.setOption(opt, ""));
            // legacy
            expectThrows(SocketException.class, () -> impl.setOption(-1, ""));
        }
    }

    /**
     * Test setOption(int, Object) with invalid values.
     */
    public void testSetOption4() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            expectThrows(SocketException.class,
                         () -> impl.setOption(SocketOptions.SO_REUSEADDR, -1));
            expectThrows(SocketException.class,
                         () -> impl.setOption(SocketOptions.SO_TIMEOUT, -1));
            expectThrows(SocketException.class,
                         () -> impl.setOption(SocketOptions.SO_SNDBUF, -1));
            expectThrows(SocketException.class,
                         () -> impl.setOption(SocketOptions.SO_RCVBUF, -1));
        }
    }

    /**
     * Test getOption when not created.
     */
    public void testGetOption1() throws IOException {
        var impl = new PlatformSocketImpl(false);
        expectThrows(IOException.class,
                     () -> impl.getOption(StandardSocketOptions.SO_REUSEADDR));
        expectThrows(SocketException.class,
                     () -> impl.getOption(-1));
    }

    /**
     * Test getOption when closed.
     */
    public void testGetOption2() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class,
                     () -> impl.getOption(StandardSocketOptions.SO_REUSEADDR));
        expectThrows(SocketException.class,
                     () -> impl.getOption(SocketOptions.SO_REUSEADDR));
    }

    /**
     * Test getOption with unsupported option.
     */
    public void testGetOption3() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            var opt = new SocketOption<String>() {
                @Override public String name() { return "birthday"; }
                @Override public Class<String> type() { return String.class; }
            };
            expectThrows(UnsupportedOperationException.class, () -> impl.getOption(opt));
            expectThrows(SocketException.class, () -> impl.getOption(-1));
        }
    }

    /**
     * Test shutdownInput when not created.
     */
    public void testShutdownInput1() throws IOException {
        var impl = new PlatformSocketImpl(false);
        expectThrows(IOException.class, () -> impl.shutdownInput());
    }

    /**
     * Test shutdownInput when not connected.
     */
    public void testShutdownInput2() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            expectThrows(IOException.class, () -> impl.shutdownInput());
        }
    }

    /**
     * Test shutdownInput when closed.
     */
    public void testShutdownInput3() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class, () -> impl.shutdownInput());
    }

    /**
     * Test shutdownOutput when not created.
     */
    public void testShutdownOutput1() throws IOException {
        var impl = new PlatformSocketImpl(false);
        expectThrows(IOException.class, () -> impl.shutdownOutput());
    }

    /**
     * Test shutdownOutput when not connected.
     */
    public void testShutdownOutput2() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            expectThrows(IOException.class, () -> impl.shutdownOutput());
        }
    }

    /**
     * Test shutdownOutput when closed.
     */
    public void testShutdownOutput3() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class, () -> impl.shutdownOutput());
    }

    /**
     * Test sendUrgentData when not created.
     */
    public void testSendUrgentData1() throws IOException {
        var impl = new PlatformSocketImpl(false);
        expectThrows(IOException.class, () -> impl.sendUrgentData(0));
    }

    /**
     * Test sendUrgentData when not connected.
     */
    public void testSendUrgentData2() throws IOException {
        try (var impl = new PlatformSocketImpl(false)) {
            impl.create(true);
            expectThrows(IOException.class, () -> impl.sendUrgentData(0));
        }
    }

    /**
     * Test sendUrgentData when closed.
     */
    public void testSendUrgentData3() throws IOException {
        var impl = new PlatformSocketImpl(false);
        impl.close();
        expectThrows(IOException.class, () -> impl.sendUrgentData(0));
    }
}
