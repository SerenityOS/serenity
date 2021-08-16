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

/**
 * @test
 * @bug 8245194
 * @run main/othervm/java.security.policy=policy1 Security policy1
 * @run main/othervm/java.security.policy=policy2 Security policy2
 * @run main/othervm -Djava.security.manager=allow Security policy3
 * @summary Security test for Unix Domain socket and server socket channels
 */

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static java.net.StandardProtocolFamily.UNIX;

/**
 * Tests required all with security manager
 */

public class Security {

    static interface Command {
        public void run() throws Exception;
    }

    static <T extends Exception> void call(Command r, Class<? extends Exception> expectedException) {
        boolean threw = false;
        try {
            r.run();
        } catch (Throwable t) {
            if (expectedException == null) {
                t.printStackTrace();
                throw new RuntimeException("an exception was thrown but was not expected");
            }
            threw = true;
            if (!(expectedException.isAssignableFrom(t.getClass()))) {
                throw new RuntimeException("wrong exception type thrown " + t.toString());
            }
        }
        if (expectedException != null && !threw) {
            // should have thrown
            throw new RuntimeException("% was expected".formatted(expectedException.getName()));
        }
    }


    public static void main(String[] args) throws Exception {
        try {
           SocketChannel.open(UNIX);
        } catch (UnsupportedOperationException e) {
            System.out.println("Unix domain not supported");
            return;
        }

        String policy = args[0];
        switch (policy) {
            case "policy1":
                testPolicy1();
                break;
            case "policy2":
                testPolicy2();
                break;
            case "policy3":
                testPolicy3();
                break;
        }
    }

    static void setSecurityManager(String policy) {
        String testSrc = System.getProperty("test.src");
        // Three /// required for Windows below
        String policyURL = "file:///" + testSrc + File.separator + policy;
        System.out.println("POLICY: " + policyURL);
        System.setProperty("java.security.policy", policyURL);
        System.setSecurityManager(new SecurityManager());
    }

    static void close(NetworkChannel... channels) {

        for (NetworkChannel chan : channels) {
            try {
                chan.close();
            } catch (Exception e) {
            }
        }
    }

    private static final Class<SecurityException> SE = SecurityException.class;
    private static final Class<IOException> IOE = IOException.class;

    // No permission

    public static void testPolicy1() throws Exception {
        Path servername = Path.of("sock");
        Files.deleteIfExists(servername);
        // Permission exists to bind a ServerSocketChannel
        final UnixDomainSocketAddress saddr = UnixDomainSocketAddress.of(servername);
        try (final ServerSocketChannel server = ServerSocketChannel.open(UNIX)) {
            try (final SocketChannel client = SocketChannel.open(UNIX)) {
                call(() -> {
                    server.bind(saddr);
                }, SE);
                call(() -> {
                    client.connect(saddr);
                }, SE);
            }
        } finally {
            Files.deleteIfExists(servername);
        }
    }

    // All permissions

    public static void testPolicy2() throws Exception {
        Path servername = Path.of("sock");
        Files.deleteIfExists(servername);
        final UnixDomainSocketAddress saddr = UnixDomainSocketAddress.of(servername);
        try (final ServerSocketChannel server = ServerSocketChannel.open(UNIX)) {
            try (final SocketChannel client = SocketChannel.open(UNIX)) {
                call(() -> {
                    server.bind(saddr);
                }, null);
                call(() -> {
                    client.connect(saddr);
                }, null);
                try (final SocketChannel peer = server.accept()) {
                    // Should succeed
                }
            }
        } finally {
            Files.deleteIfExists(servername);
        }
    }

    public static void testPolicy3() throws Exception {
        Path sock1 = Path.of("sock3");
        Path sock2 = null;
        Files.deleteIfExists(sock1);
        final UnixDomainSocketAddress saddr = UnixDomainSocketAddress.of(sock1);
        try (var s1 = ServerSocketChannel.open(UNIX)) {
            s1.bind(saddr);
            try (var s2 = ServerSocketChannel.open(UNIX)) {
                s2.bind(null);
                var add2 = (UnixDomainSocketAddress)s2.getLocalAddress();
                sock2 = add2.getPath();

                // Now set security manager and check if we can see addresses

                setSecurityManager("policy3");

                if (((UnixDomainSocketAddress)s1
                            .getLocalAddress())
                            .getPath()
                            .toString()
                            .length() != 0)
                {
                    throw new RuntimeException("address should have been empty");
                }

                if (((UnixDomainSocketAddress)s2
                            .getLocalAddress())
                            .getPath()
                            .toString()
                            .length() != 0)
                {
                    throw new RuntimeException("address should have been empty");
                }
            }
        } finally {
            System.setSecurityManager(null);
            Files.deleteIfExists(sock1);
            Files.deleteIfExists(sock2);
        }
    }
}
