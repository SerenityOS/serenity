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
 * @library /test/lib
 * @run main/othervm Bind
 */

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import jtreg.SkippedException;

/**
 * Check that all bind variations work
 */
public class Bind {

    static Path spath, cpath;

    static UnixDomainSocketAddress sAddr, cAddr, UNNAMED, nullAddr;
    static ServerSocketChannel server;
    static SocketChannel client, accept1;

    public static void main(String args[]) throws Exception {
        checkSupported();
        spath = Path.of("server.sock");
        cpath = Path.of("client.sock");
        sAddr = UnixDomainSocketAddress.of(spath);
        cAddr = UnixDomainSocketAddress.of(cpath);
        nullAddr = UnixDomainSocketAddress.of("");
        UNNAMED = nullAddr;
        runTests();
    }

    static void checkSupported() {
        try {
            SocketChannel.open(StandardProtocolFamily.UNIX).close();
        } catch (UnsupportedOperationException e) {
            throw new SkippedException("Unix domain channels not supported");
        } catch (Exception e) {
            // continue test to see what problem is
        }
    }

    static interface ThrowingRunnable {
        public void run() throws Exception;
    }

    static void init() throws IOException {
        Files.deleteIfExists(cpath);
        Files.deleteIfExists(spath);
        client = null; server = null; accept1 = null;
    }

    static void checkNormal(ThrowingRunnable r) {
        try {
            init();
            r.run();
            System.out.println("PASS:");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            cleanup();
        }
    }

    static void checkException(Class<? extends Exception> expected, ThrowingRunnable r) {
        try {
            init();
            r.run();
            throw new RuntimeException("Exception expected");
        } catch (Exception e) {
            if (!expected.isAssignableFrom(e.getClass())) {
                String msg = "Expected: " + expected + " Got: " + e.getClass();
                throw new RuntimeException(msg);
            }
            System.out.println("PASS: Got " + e);
        } finally {
            cleanup();
        }
    }

    static void cleanup() {
        try {
            if (server != null)
                server.close();
            if (client != null)
                client.close();
            if (accept1 != null)
                accept1.close();
        } catch (IOException e) {}
    }

    static void assertClientAddress(SocketAddress a) {
        assertAddress(a, cAddr, "client");
    }

    static void assertServerAddress(SocketAddress a) {
        assertAddress(a, sAddr, "server");
    }

    static void assertAddress(SocketAddress a, UnixDomainSocketAddress a1, String s) {
        if (!(a instanceof UnixDomainSocketAddress)) {
            throw new RuntimeException("wrong address type");
        }
        UnixDomainSocketAddress ua = (UnixDomainSocketAddress)a;
        if (!a.equals(a1))
            throw new RuntimeException("this is not the " + s + " address");
    }

    static void assertEquals(Object a, Object b) {
        if (!a.equals(b))
            throw new RuntimeException("identity check failed");
    }

    public static void runTests() throws IOException {
        checkNormal(() -> {
            client = SocketChannel.open(StandardProtocolFamily.UNIX);
            client.bind(cAddr);
        });
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(sAddr);
        });
        // Repeat first two to make sure they are repeatable
        checkNormal(() -> {
            client = SocketChannel.open(StandardProtocolFamily.UNIX);
            client.bind(cAddr);
        });
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(sAddr);
        });
        // address with space should work
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            UnixDomainSocketAddress usa =  UnixDomainSocketAddress.of("with space"); // relative to CWD
            Files.deleteIfExists(usa.getPath());
            server.bind(usa);
            client = SocketChannel.open(usa);
            Files.delete(usa.getPath());
            assertAddress(client.getRemoteAddress(), usa, "address");
        });
        // client bind to null: allowed
        checkNormal(() -> {
            client = SocketChannel.open(StandardProtocolFamily.UNIX);
            client.bind(null);
            SocketAddress a = client.getLocalAddress();
            assertAddress(a, nullAddr, "null address");
            assertEquals(a, UNNAMED);
        });
        // client bind to UNNAMED: allowed
        checkNormal(() -> {
            client = SocketChannel.open(StandardProtocolFamily.UNIX);
            client.bind(UNNAMED);
            SocketAddress a = client.getLocalAddress();
            assertAddress(a, nullAddr, "null address");
            assertEquals(a, UNNAMED);
        });
        // server bind to null: should bind to a local address
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(null);
            UnixDomainSocketAddress usa = (UnixDomainSocketAddress)server.getLocalAddress();
            if (usa.getPath().toString().isEmpty())
                throw new RuntimeException("expected non zero address length");
            System.out.println("Null server address: " + server.getLocalAddress());
        });
        // server no bind : not allowed
        checkException(
            NotYetBoundException.class, () -> {
                server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
                server.accept();
            }
        );

        // client implicit bind and connect
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            client = SocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(sAddr);
            client.connect(sAddr);
            SocketAddress cAddr = client.getLocalAddress();
            assertAddress(cAddr, nullAddr, "null address");
            assertEquals(cAddr, UNNAMED);
            assertServerAddress(server.getLocalAddress());
        });
        // client null bind and connect (check all addresses)
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            client = SocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(sAddr);
            client.bind(null);
            client.connect(sAddr);
            assertAddress(client.getLocalAddress(), UNNAMED, "unnamed address");
            assertServerAddress(server.getLocalAddress());
        });
        // client explicit bind and connect (check all addresses)
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            client = SocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(sAddr);
            client.bind(cAddr);
            client.connect(sAddr);
            accept1 = server.accept();
            assertClientAddress(client.getLocalAddress());
            assertServerAddress(server.getLocalAddress());
            assertAddress(client.getRemoteAddress(), sAddr, "client's remote server address");
            assertAddress(accept1.getLocalAddress(), sAddr, "accepted local address (server)");
            assertAddress(accept1.getRemoteAddress(), cAddr, "accepted remote address (client)");
        });
        // server multiple bind : not allowed
        checkException(
            AlreadyBoundException.class, () -> {
                server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
                server.bind(sAddr);
                server.bind(sAddr);
            }
        );
        // client multiple bind : not allowed
        checkException(
            AlreadyBoundException.class, () -> {
                client = SocketChannel.open(StandardProtocolFamily.UNIX);
                client.bind(cAddr);
                client.bind(cAddr);
            }
        );
        // client multiple bind to different addresses: not allowed
        checkException(
            AlreadyBoundException.class, () -> {
                client = SocketChannel.open(StandardProtocolFamily.UNIX);
                client.bind(cAddr);
                client.bind(sAddr);
            }
        );
        // client multiple bind to differnt addresses, incl null: not allowed
        checkException(
            AlreadyBoundException.class, () -> {
                client = SocketChannel.open(StandardProtocolFamily.UNIX);
                client.bind(null);
                client.bind(cAddr);
            }
        );

        // server bind to existing name: not allowed

        checkException(
            BindException.class, () -> {
                var path = Files.createFile(Path.of("moo.sock"));
                var addr = UnixDomainSocketAddress.of(path);
                server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
                try {
                    server.bind(addr);
                } finally {
                    Files.deleteIfExists(path);
                }
            }
        );


        // client bind to existing name: not allowed
        checkException(
            BindException.class, () -> {
                var path = Path.of("temp.sock");
                Files.deleteIfExists(path);
                Files.createFile(path);
                var addr = UnixDomainSocketAddress.of(path);
                client = SocketChannel.open(StandardProtocolFamily.UNIX);
                try {
                    client.bind(addr);
                } finally {
                    Files.deleteIfExists(path);
                }
            }
        );

        // bind and connect to name of close to max size
        checkNormal(() -> {
            int len = 100;
            char[] chars = new char[len];
            Arrays.fill(chars, 'x');
            String name = new String(chars);
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(name);
            ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(address);
            SocketChannel client = SocketChannel.open(address);
            assertAddress(server.getLocalAddress(), address, "server");
            assertAddress(client.getRemoteAddress(), address, "client");
            Files.delete(address.getPath());
        });

        // implicit server bind
        checkNormal(() -> {
            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            server.bind(null);
            UnixDomainSocketAddress usa = (UnixDomainSocketAddress)server.getLocalAddress();
            client = SocketChannel.open(usa);
            accept1 = server.accept();
            assertAddress(client.getRemoteAddress(), usa, "server");
            Files.delete(usa.getPath());
        });
    }
}
