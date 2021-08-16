/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8245194
 * @run main/othervm SocketOptions
 */

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import jdk.net.UnixDomainPrincipal;
import static jdk.net.ExtendedSocketOptions.SO_PEERCRED;

/**
 * Check that all supported options can actually be set and got
 */
public class SocketOptions {

    public static void main(String args[]) throws Exception {
        if (!supported()) {
            System.out.println("Unix domain channels not supported");
            return;
        }
        test(ServerSocketChannel.open(StandardProtocolFamily.UNIX));
        test(SocketChannel.open(StandardProtocolFamily.UNIX));
        testPeerCred();
    }

    static void testPeerCred() throws Exception {
        UnixDomainSocketAddress addr = null;
        UnixDomainPrincipal p;
        try (ServerSocketChannel s = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            s.bind(null);
            addr = (UnixDomainSocketAddress)s.getLocalAddress();
            try (SocketChannel c = SocketChannel.open(addr)) {
                if (!c.supportedOptions().contains(SO_PEERCRED)) {
                    return;
                }
                Files.deleteIfExists(addr.getPath());
                p = c.getOption(SO_PEERCRED);
                String s1 = p.user().getName();
                System.out.println(s1);
                System.out.println(p.group().getName());
                String s2 = System.getProperty("user.name");

                // Check returned user name

                if (!s1.equals(s2)) {
                    throw new RuntimeException("wrong username");
                }

                // Try setting the option: Read only

                try {
                    c.setOption(SO_PEERCRED, p);
                    throw new RuntimeException("should have thrown SocketException");
                } catch (SocketException e) {}
            }
        } finally {
            if (addr != null)
                Files.deleteIfExists(addr.getPath());
        }

        // Try getting from unconnected socket

        try (var c = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            try {
                p = c.getOption(SO_PEERCRED);
                System.out.println(p.user());
                throw new RuntimeException("should have thrown SocketException");
            } catch (SocketException e) {}
        }

        // Try getting from ServerSocketChannel

        try (var server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            try {
                p = server.getOption(SO_PEERCRED);
                System.out.println(p.user());
                throw new RuntimeException("should have thrown USE");
            } catch (UnsupportedOperationException e) {}
        }
    }

    static boolean supported() {
        try {
            SocketChannel.open(StandardProtocolFamily.UNIX).close();
        } catch (UnsupportedOperationException e) {
            return false;
        } catch (Exception e) {
            return true; // continue test to see what problem is
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void test(NetworkChannel chan) throws IOException {
        System.out.println("Checking: " + chan.getClass());
        Set<SocketOption<?>> supported = chan.supportedOptions();
        for (SocketOption<?> option : supported) {
            String name = option.name();
            System.out.println("Checking option " + name);
            if (option.type() == Boolean.class) {
                chan.setOption((SocketOption<Boolean>)option, true);
                chan.setOption((SocketOption<Boolean>)option, false);
                chan.getOption(option);
            } else if (option.type() == Integer.class) {
                chan.setOption((SocketOption<Integer>)option, 10);
                chan.getOption(option);
            }
        }
    }
}
