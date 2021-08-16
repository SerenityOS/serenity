/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.net.StandardProtocolFamily.UNIX;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/*
 * Make sure that System.inheritedChannel returns the correct type
 */

public class UnixDomainChannelTest {
    private static final UnixDomainSocketAddress SOCK_ADDR =
            UnixDomainSocketAddress.of(Path.of("foo.socket"));

    private static boolean passed = true;

    public static class Child {
        public static void main(String[] args) throws Exception {
            // we want to make sure that System.inheritedChannel either
            // returns a ServerSocketChannel or a SocketChannel
            Channel channel = System.inheritedChannel();
            String result = channel == null ? "N" : "Y";
            if (args[0].equals("test1") || args[0].equals("test2")) {
                if (channel instanceof SocketChannel) {
                    SocketChannel sc = (SocketChannel) channel;
                    ByteBuffer buf = ByteBuffer.wrap(result.getBytes(ISO_8859_1));
                    sc.write(buf);
                }
            } else { // test3
                if (channel instanceof ServerSocketChannel) {
                    ServerSocketChannel server = (ServerSocketChannel) channel;
                    SocketChannel sc = server.accept();
                    ByteBuffer buf = ByteBuffer.wrap(result.getBytes(ISO_8859_1));
                    sc.write(buf);
                }
            }
        }
    }

    public static void main(String args[]) throws Exception {
        test1();
        test2();
        test3();
        if (!passed)
            throw new RuntimeException();
    }

    // Test with a named connected socket
    private static void test1() throws Exception {
        ServerSocketChannel listener = ServerSocketChannel.open(UNIX);
        listener.bind(SOCK_ADDR);
        SocketChannel sock1 = SocketChannel.open(SOCK_ADDR);
        SocketChannel sock2 = listener.accept();
        System.out.println("test1: launching child");
        Launcher.launchWithSocketChannel(
                "UnixDomainChannelTest$Child", sock2, null, "test1");
        ByteBuffer bb = ByteBuffer.allocate(10);
        int c = sock1.read(bb);
        if (c != 1) {
            System.err.printf("test1: failed " +
                    "- unexpected number of bytes read %d d\n", c);
            passed = false;
        }
        byte b = bb.get(0);
        if (b != 'Y') {
            System.err.printf("test1: failed " +
                    "- unexpected byte read %d d\n", b);
            passed = false;
        }
        closeAll(listener, sock1, sock2);
        Files.deleteIfExists(SOCK_ADDR.getPath());
    }

    // Test with unnamed socketpair
    private static void test2() throws Exception {
        ServerSocketChannel listener = ServerSocketChannel.open(UNIX);
        SocketAddress addr = listener.bind(null).getLocalAddress();
        SocketChannel sock1 = SocketChannel.open(addr);
        SocketChannel sock2 = listener.accept();
        System.out.println("test2: launching child");
        Launcher.launchWithSocketChannel(
                "UnixDomainChannelTest$Child", sock2, null, "test2");
        ByteBuffer bb = ByteBuffer.allocate(10);
        int c = sock1.read(bb);
        if (c != 1) {
            System.err.printf("test3: failed " +
                    "- unexpected number of bytes read %d d\n", c);
            passed = false;
        }
        byte b = bb.get(0);
        if (b != 'Y') {
            System.err.printf("test3: failed " +
                    "- unexpected byte read %d d\n", b);
            passed = false;
        }
        closeAll(listener, sock1, sock2);
        Files.deleteIfExists(((UnixDomainSocketAddress)addr).getPath());
    }

    // Test with a named listener
    private static void test3() throws Exception {
        ServerSocketChannel listener = ServerSocketChannel.open(UNIX);
        listener.bind(SOCK_ADDR);
        SocketChannel sock1 = SocketChannel.open(UNIX);
        System.out.println("test3: launching child");
        Launcher.launchWithServerSocketChannel(
                "UnixDomainChannelTest$Child", listener, null, "test3");
        sock1.connect(SOCK_ADDR);
        ByteBuffer bb = ByteBuffer.allocate(10);
        int c = sock1.read(bb);
        if (c != 1) {
            System.err.printf("test3: failed " +
                    "- unexpected number of bytes read %d d\n", c);
            passed = false;
        }
        byte b = bb.get(0);
        if (b != 'Y') {
            System.err.printf("test3: failed " +
                    "- unexpected byte read %d d\n", b);
            passed = false;
        }
        closeAll(listener, sock1);
        Files.deleteIfExists(SOCK_ADDR.getPath());
    }

    private static void closeAll(Channel... channels) {
        for (Channel c : channels) {
            try {
                if (c != null)
                    c.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not close channel " + c);
            }
        }
    }
}
