/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng/othervm IOExchanges
 */

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.System.out;
import static java.net.StandardProtocolFamily.*;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class IOExchanges {
    static boolean unixDomainSupported = true;


    @BeforeTest()
    public void setup() {
        try {
            SocketChannel.open(UNIX);
        } catch (IOException | UnsupportedOperationException e) {
            unixDomainSupported = false;
            out.println("Unix domain channels not supported");
        }
    }

    static SocketChannel openSocketChannel(ProtocolFamily family)
            throws IOException {
        return family == UNIX ? SocketChannel.open(family)
                : SocketChannel.open();
    }

    static ServerSocketChannel openServerSocketChannel(ProtocolFamily family)
             throws IOException {
        return family == UNIX ? ServerSocketChannel.open(family)
                     : ServerSocketChannel.open();
    }

    public static void deleteFile(SocketAddress addr) throws Exception {
        if (addr instanceof UnixDomainSocketAddress) {
            Files.deleteIfExists(((UnixDomainSocketAddress) addr).getPath());
        }
    }

    /*
     The following, non-exhaustive set, of tests exercise different combinations
     of blocking and non-blocking accept/connect calls along with I/O
     operations, that exchange a single byte. The intent it to test a reasonable
     set of blocking and non-blocking scenarios.

     The individual test method names follow their test scenario.
        [BAccep|SELNBAccep|SPINNBAccep] - Accept either:
                         blocking, select-non-blocking, spinning-non-blocking
        [BConn|NBConn] - blocking connect / non-blocking connect
        [BIO|NBIO]     - blocking / non-blocking I/O operations (read/write)
        [WR|RW] - connecting thread write/accepting thread reads first, and vice-versa
        [Id]    - unique test Id

        BAccep_BConn_BIO_WR_1
        BAccep_BConn_BIO_RW_2
        SELNBAccep_BConn_BIO_WR_3
        SELNBAccep_BConn_BIO_RW_4
        SPINNBAccep_BConn_BIO_WR_5
        SPINNBAccep_BConn_BIO_RW_6
        BAccep_NBConn_BIO_WR_7
        BAccep_NBConn_BIO_RW_8
        SELNBAccep_NBConn_BIO_WR_9
        SELNBAccep_NBConn_BIO_RW_10
        SPINNBAccep_NBConn_BIO_WR_11
        SPINNBAccep_NBConn_BIO_RW_12

        BAccep_BConn_NBIO_WR_1a         // Non-Blocking I/O
        BAccep_BConn_NBIO_RW_2a
        SELNBAccep_BConn_NBIO_WR_3a
        SELNBAccep_BConn_NBIO_RW_4a
        SPINNBAccep_BConn_NBIO_WR_5a
        SPINNBAccep_BConn_NBIO_RW_6a
        BAccep_NBConn_NBIO_WR_7a
        BAccep_NBConn_NBIO_RW_8a
        SELNBAccep_NBConn_NBIO_WR_9a
        SELNBAccep_NBConn_NBIO_RW_10a
        SPINBAccep_NBConn_NBIO_WR_11a
        SPINBAccep_NBConn_NBIO_RW_12a
    */

    @DataProvider(name = "family")
    public Object[][] family() {
        return unixDomainSupported ?
                new Object[][] {
                    { UNIX },
                    { INET }}
                : new Object[][] {
                    { INET }
        };
    }

    @Test(dataProvider = "family")
    public void BAccep_BConn_BIO_WR_1(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t1", () -> {
                try (SocketChannel sc = openSocketChannel(family)) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x01).flip();
                    assertEquals(sc.write(bb), 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    assertEquals(sc.read(bb.clear()), -1);
                }
            });
            t.start();

            try (SocketChannel sc = ssc.accept()) {
                ByteBuffer bb = ByteBuffer.allocate(10);
                assertEquals(sc.read(bb), 1);
                out.printf("read:  0x%x%n", bb.get(0));
                assertEquals(bb.get(0), 0x01);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void BAccep_BConn_BIO_RW_2(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t2", () -> {
                try (SocketChannel sc = openSocketChannel(family)) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10);
                    assertEquals(sc.read(bb), 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x02);
                }
            });
            t.start();

            try (SocketChannel sc = ssc.accept()) {
                ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x02).flip();
                assertEquals(sc.write(bb), 1);
                out.printf("wrote: 0x%x%n", bb.get(0));
                assertEquals(sc.read(bb.clear()), -1);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_BConn_BIO_WR_3(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family);
             Selector selector = Selector.open()) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t3", () -> {
                try (SocketChannel sc = openSocketChannel(family)) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x03).flip();
                    assertEquals(sc.write(bb), 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    assertEquals(sc.read(bb.clear()), -1);
                }
            });
            t.start();

            ssc.configureBlocking(false).register(selector, OP_ACCEPT);
            assertEquals(selector.select(), 1);

            try (SocketChannel sc = ssc.accept()) {
                ByteBuffer bb = ByteBuffer.allocate(10);
                assertEquals(sc.read(bb), 1);
                out.printf("read:  0x%x%n", bb.get(0));
                assertEquals(bb.get(0), 0x03);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_BConn_BIO_RW_4(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family);
             Selector selector = Selector.open()) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t4", () -> {
                try (SocketChannel sc = openSocketChannel(family)) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10);
                    assertEquals(sc.read(bb), 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x04);
                }
            });
            t.start();

            ssc.configureBlocking(false).register(selector, OP_ACCEPT);
            assertEquals(selector.select(), 1);

            try (SocketChannel sc = ssc.accept()) {
                ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x04).flip();
                assertEquals(sc.write(bb), 1);
                out.printf("wrote: 0x%x%n", bb.get(0));
                assertEquals(sc.read(bb.clear()), -1);

            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINNBAccep_BConn_BIO_WR_5(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t5", () -> {
                try (SocketChannel sc = openSocketChannel(family)) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x05).flip();
                    assertEquals(sc.write(bb), 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    assertEquals(sc.read(bb.clear()), -1);
                }
            });
            t.start();

            SocketChannel accepted;
            for (; ; ) {
                accepted = ssc.accept();
                if (accepted != null) {
                    out.println("accepted new connection");
                    break;
                }
                Thread.onSpinWait();
            }

            try (SocketChannel sc = accepted) {
                ByteBuffer bb = ByteBuffer.allocate(10);
                assertEquals(sc.read(bb), 1);
                out.printf("read:  0x%x%n", bb.get(0));
                assertEquals(bb.get(0), 0x05);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINNBAccep_BConn_BIO_RW_6(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t6", () -> {
                try (SocketChannel sc = openSocketChannel(family)) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10);
                    assertEquals(sc.read(bb), 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x06);
                }
            });
            t.start();

            SocketChannel accepted;
            for (; ; ) {
                accepted = ssc.accept();
                if (accepted != null) {
                    out.println("accepted new connection");
                    break;
                }
                Thread.onSpinWait();
            }

            try (SocketChannel sc = accepted) {
                ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x06).flip();
                assertEquals(sc.write(bb), 1);
                out.printf("wrote: 0x%x%n", bb.get(0));
                assertEquals(sc.read(bb.clear()), -1);

            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    // Similar to the previous six scenarios, but with same-thread
    // non-blocking connect.

    @Test(dataProvider = "family")
    public void BAccep_NBConn_BIO_WR_7(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    sc.configureBlocking(true);
                    TestThread t = TestThread.of("t7", () -> {
                        ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x07).flip();
                        assertEquals(sc.write(bb), 1);
                        out.printf("wrote: 0x%x%n", bb.get(0));
                        assertEquals(sc.read(bb.clear()), -1);
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10);
                    assertEquals(sc2.read(bb), 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x07);
                    sc2.shutdownOutput();
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void BAccep_NBConn_BIO_RW_8(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    sc.configureBlocking(true);
                    TestThread t = TestThread.of("t8", () -> {
                        ByteBuffer bb = ByteBuffer.allocate(10);
                        assertEquals(sc.read(bb), 1);
                        out.printf("read:  0x%x%n", bb.get(0));
                        assertEquals(bb.get(0), 0x08);
                        sc.shutdownOutput();
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x08).flip();
                    assertEquals(sc2.write(bb), 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    assertEquals(sc2.read(bb.clear()), -1);
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_NBConn_BIO_WR_9(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family);
                 Selector selector = Selector.open()) {
                sc.configureBlocking(false);
                sc.connect(addr);

                ssc.configureBlocking(false).register(selector, OP_ACCEPT);
                assertEquals(selector.select(), 1);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    sc.configureBlocking(true);
                    TestThread t = TestThread.of("t9", () -> {
                        ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x09).flip();
                        assertEquals(sc.write(bb), 1);
                        out.printf("wrote: 0x%x%n", bb.get(0));
                        assertEquals(sc.read(bb.clear()), -1);
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10);
                    assertEquals(sc2.read(bb), 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x09);
                    sc2.shutdownOutput();
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_NBConn_BIO_RW_10(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family);
                 Selector selector = Selector.open()) {
                sc.configureBlocking(false);
                sc.connect(addr);

                ssc.configureBlocking(false).register(selector, OP_ACCEPT);
                assertEquals(selector.select(), 1);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    sc.configureBlocking(true);
                    TestThread t = TestThread.of("t10", () -> {
                        ByteBuffer bb = ByteBuffer.allocate(10);
                        assertEquals(sc.read(bb), 1);
                        out.printf("read:  0x%x%n", bb.get(0));
                        assertEquals(bb.get(0), 0x10);
                        sc.shutdownOutput();
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x10).flip();
                    assertEquals(sc2.write(bb), 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    assertEquals(sc2.read(bb.clear()), -1);
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINNBAccep_NBConn_BIO_WR_11(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                SocketChannel accepted;
                for (; ; ) {
                    accepted = ssc.accept();
                    if (accepted != null) {
                        out.println("accepted new connection");
                        break;
                    }
                    Thread.onSpinWait();
                }

                try (SocketChannel sc2 = accepted) {
                    assertTrue(sc.finishConnect());
                    sc.configureBlocking(true);
                    TestThread t = TestThread.of("t11", () -> {
                        ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x11).flip();
                        assertEquals(sc.write(bb), 1);
                        out.printf("wrote: 0x%x%n", bb.get(0));
                        assertEquals(sc.read(bb.clear()), -1);
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10);
                    assertEquals(sc2.read(bb), 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x11);
                    sc2.shutdownOutput();
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINNBAccep_NBConn_BIO_RW_12(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                SocketChannel accepted;
                for (; ; ) {
                    accepted = ssc.accept();
                    if (accepted != null) {
                        out.println("accepted new connection");
                        break;
                    }
                    Thread.onSpinWait();
                }

                try (SocketChannel sc2 = accepted) {
                    assertTrue(sc.finishConnect());
                    sc.configureBlocking(true);
                    TestThread t = TestThread.of("t12", () -> {
                        ByteBuffer bb = ByteBuffer.allocate(10);
                        assertEquals(sc.read(bb), 1);
                        out.printf("read:  0x%x%n", bb.get(0));
                        assertEquals(bb.get(0), 0x12);
                        sc.shutdownOutput();
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x12).flip();
                    assertEquals(sc2.write(bb), 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    assertEquals(sc2.read(bb.clear()), -1);
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    // ---
    // Similar to the previous twelve scenarios but with non-blocking IO
    // ---

    @Test(dataProvider = "family")
    public void BAccep_BConn_NBIO_WR_1a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t1a", () -> {
                try (SocketChannel sc = openSocketChannel(family);
                     Selector selector = Selector.open()) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x1A).flip();
                    sc.configureBlocking(false);
                    SelectionKey k = sc.register(selector, OP_WRITE);
                    selector.select();
                    int c;
                    while ((c = sc.write(bb)) < 1) ;
                    assertEquals(c, 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    k.interestOps(OP_READ);
                    selector.select();
                    bb.clear();
                    while ((c = sc.read(bb)) == 0) ;
                    assertEquals(c, -1);
                }
            });
            t.start();

            try (SocketChannel sc = ssc.accept();
                 Selector selector = Selector.open()) {
                ByteBuffer bb = ByteBuffer.allocate(10);
                sc.configureBlocking(false);
                sc.register(selector, OP_READ);
                selector.select();
                int c;
                while ((c = sc.read(bb)) == 0) ;
                assertEquals(c, 1);
                out.printf("read:  0x%x%n", bb.get(0));
                assertEquals(bb.get(0), 0x1A);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void BAccep_BConn_NBIO_RW_2a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t2a", () -> {
                try (SocketChannel sc = openSocketChannel(family);
                     Selector selector = Selector.open()) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10);
                    sc.configureBlocking(false);
                    sc.register(selector, OP_READ);
                    selector.select();
                    int c;
                    while ((c = sc.read(bb)) == 0) ;
                    assertEquals(c, 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x2A);
                }
            });
            t.start();

            try (SocketChannel sc = ssc.accept();
                 Selector selector = Selector.open()) {
                ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x2A).flip();
                sc.configureBlocking(false);
                SelectionKey k = sc.register(selector, OP_WRITE);
                selector.select();
                int c;
                while ((c = sc.write(bb)) < 1) ;
                assertEquals(c, 1);
                out.printf("wrote: 0x%x%n", bb.get(0));
                k.interestOps(OP_READ);
                selector.select();
                bb.clear();
                while ((c = sc.read(bb)) == 0) ;
                assertEquals(c, -1);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_BConn_NBIO_WR_3a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family);
             Selector aselector = Selector.open()) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t3a", () -> {
                try (SocketChannel sc = openSocketChannel(family);
                     Selector selector = Selector.open()) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x3A).flip();
                    sc.configureBlocking(false);
                    SelectionKey k = sc.register(selector, OP_WRITE);
                    selector.select();
                    int c;
                    while ((c = sc.write(bb)) < 1) ;
                    assertEquals(c, 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    k.interestOps(OP_READ);
                    selector.select();
                    bb.clear();
                    while ((c = sc.read(bb)) == 0) ;
                    assertEquals(c, -1);
                }
            });
            t.start();

            ssc.configureBlocking(false).register(aselector, OP_ACCEPT);
            assertEquals(aselector.select(), 1);

            try (SocketChannel sc = ssc.accept();
                 Selector selector = Selector.open()) {
                ByteBuffer bb = ByteBuffer.allocate(10);
                sc.configureBlocking(false);
                sc.register(selector, OP_READ);
                selector.select();
                int c;
                while ((c = sc.read(bb)) == 0) ;
                assertEquals(c, 1);
                out.printf("read:  0x%x%n", bb.get(0));
                assertEquals(bb.get(0), 0x3A);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_BConn_NBIO_RW_4a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family);
             Selector aselector = Selector.open()) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t4a", () -> {
                try (SocketChannel sc = openSocketChannel(family);
                     Selector selector = Selector.open()) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10);
                    sc.configureBlocking(false);
                    sc.register(selector, OP_READ);
                    selector.select();
                    int c;
                    while ((c = sc.read(bb)) == 0) ;
                    assertEquals(c, 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x4A);
                }
            });
            t.start();

            ssc.configureBlocking(false).register(aselector, OP_ACCEPT);
            assertEquals(aselector.select(), 1);

            try (SocketChannel sc = ssc.accept();
                 Selector selector = Selector.open()) {
                ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x4A).flip();
                sc.configureBlocking(false);
                SelectionKey k = sc.register(selector, OP_WRITE);
                selector.select();
                int c;
                while ((c = sc.write(bb)) < 1) ;
                assertEquals(c, 1);
                out.printf("wrote: 0x%x%n", bb.get(0));
                k.interestOps(OP_READ);
                selector.select();
                bb.clear();
                while ((c = sc.read(bb)) == 0) ;
                assertEquals(c, -1);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINNBAccep_BConn_NBIO_WR_5a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t5a", () -> {
                try (SocketChannel sc = openSocketChannel(family);
                     Selector selector = Selector.open()) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x5A).flip();
                    sc.configureBlocking(false);
                    SelectionKey k = sc.register(selector, OP_WRITE);
                    selector.select();
                    int c;
                    while ((c = sc.write(bb)) < 1) ;
                    assertEquals(c, 1);
                    out.printf("wrote: 0x%x%n", bb.get(0));
                    k.interestOps(OP_READ);
                    selector.select();
                    bb.clear();
                    while ((c = sc.read(bb)) == 0) ;
                    assertEquals(c, -1);
                }
            });
            t.start();

            SocketChannel accepted;
            for (; ; ) {
                accepted = ssc.accept();
                if (accepted != null) {
                    out.println("accepted new connection");
                    break;
                }
                Thread.onSpinWait();
            }

            try (SocketChannel sc = accepted;
                 Selector selector = Selector.open()) {
                ByteBuffer bb = ByteBuffer.allocate(10);
                sc.configureBlocking(false);
                sc.register(selector, OP_READ);
                selector.select();
                int c;
                while ((c = sc.read(bb)) == 0) ;
                assertEquals(c, 1);
                out.printf("read:  0x%x%n", bb.get(0));
                assertEquals(bb.get(0), 0x5A);
            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINNBAccep_BConn_NBIO_RW_6a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            TestThread t = TestThread.of("t6a", () -> {
                try (SocketChannel sc = openSocketChannel(family);
                     Selector selector = Selector.open()) {
                    assertTrue(sc.connect(addr));
                    ByteBuffer bb = ByteBuffer.allocate(10);
                    sc.configureBlocking(false);
                    sc.register(selector, OP_READ);
                    selector.select();
                    int c;
                    while ((c = sc.read(bb)) == 0) ;
                    assertEquals(c, 1);
                    out.printf("read:  0x%x%n", bb.get(0));
                    assertEquals(bb.get(0), 0x6A);
                }
            });
            t.start();

            SocketChannel accepted;
            for (; ; ) {
                accepted = ssc.accept();
                if (accepted != null) {
                    out.println("accepted new connection");
                    break;
                }
                Thread.onSpinWait();
            }

            try (SocketChannel sc = accepted;
                 Selector selector = Selector.open()) {
                ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x6A).flip();
                sc.configureBlocking(false);
                SelectionKey k = sc.register(selector, OP_WRITE);
                selector.select();
                int c;
                while ((c = sc.write(bb)) < 1) ;
                assertEquals(c, 1);
                out.printf("wrote: 0x%x%n", bb.get(0));
                k.interestOps(OP_READ);
                selector.select();
                bb.clear();
                while ((c = sc.read(bb)) == 0) ;
                assertEquals(c, -1);

            }
            t.awaitCompletion();
            deleteFile(addr);
        }
    }

    // Similar to the previous six scenarios but with same-thread
    // non-blocking connect.

    @Test(dataProvider = "family")
    public void BAccep_NBConn_NBIO_WR_7a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    TestThread t = TestThread.of("t7a", () -> {
                        try (Selector selector = Selector.open()) {
                            ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x7A).flip();
                            sc.configureBlocking(false);
                            SelectionKey k = sc.register(selector, OP_WRITE);
                            selector.select();
                            int c;
                            while ((c = sc.write(bb)) < 1) ;
                            assertEquals(c, 1);
                            out.printf("wrote: 0x%x%n", bb.get(0));
                            k.interestOps(OP_READ);
                            selector.select();
                            bb.clear();
                            while ((c = sc.read(bb)) == 0) ;
                            assertEquals(c, -1);
                        }
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10);
                    sc2.configureBlocking(false);
                    try (Selector selector = Selector.open()) {
                        sc2.register(selector, OP_READ);
                        selector.select();
                        int c;
                        while ((c = sc2.read(bb)) == 0) ;
                        assertEquals(c, 1);
                        out.printf("read:  0x%x%n", bb.get(0));
                        assertEquals(bb.get(0), 0x7A);
                        sc2.shutdownOutput();
                    }
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void BAccep_NBConn_NBIO_RW_8a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    TestThread t = TestThread.of("t8a", () -> {
                        try (Selector selector = Selector.open()) {
                            ByteBuffer bb = ByteBuffer.allocate(10);
                            sc.register(selector, OP_READ);
                            selector.select();
                            int c;
                            while ((c = sc.read(bb)) == 0) ;
                            assertEquals(c, 1);
                            out.printf("read:  0x%x%n", bb.get(0));
                            assertEquals(bb.get(0), (byte) 0x8A);
                            sc.shutdownOutput();
                        }
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x8A).flip();
                    sc2.configureBlocking(false);
                    try (Selector selector = Selector.open()) {
                        SelectionKey k = sc2.register(selector, OP_WRITE);
                        selector.select();
                        int c;
                        while ((c = sc2.write(bb)) < 1) ;
                        assertEquals(c, 1);
                        out.printf("wrote: 0x%x%n", bb.get(0));
                        k.interestOps(OP_READ);
                        selector.select();
                        bb.clear();
                        while ((c = sc2.read(bb)) == 0) ;
                        assertEquals(c, -1);
                    }
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_NBConn_NBIO_WR_9a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                Selector aselector = Selector.open();
                ssc.configureBlocking(false).register(aselector, OP_ACCEPT);
                assertEquals(aselector.select(), 1);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    TestThread t = TestThread.of("t9a", () -> {
                        try (Selector selector = Selector.open()) {
                            ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0x9A).flip();
                            sc.configureBlocking(false);
                            SelectionKey k = sc.register(selector, OP_WRITE);
                            selector.select();
                            int c;
                            while ((c = sc.write(bb)) < 1) ;
                            assertEquals(c, 1);
                            out.printf("wrote: 0x%x%n", bb.get(0));
                            k.interestOps(OP_READ);
                            selector.select();
                            bb.clear();
                            while ((c = sc.read(bb)) == 0) ;
                            assertEquals(c, -1);
                        }
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10);
                    sc2.configureBlocking(false);
                    try (Selector selector = Selector.open()) {
                        sc2.register(selector, OP_READ);
                        selector.select();
                        int c;
                        while ((c = sc2.read(bb)) == 0) ;
                        assertEquals(c, 1);
                        out.printf("read:  0x%x%n", bb.get(0));
                        assertEquals(bb.get(0), (byte) 0x9A);
                        sc2.shutdownOutput();
                    }
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SELNBAccep_NBConn_NBIO_RW_10a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                Selector aselector = Selector.open();
                ssc.configureBlocking(false).register(aselector, OP_ACCEPT);
                assertEquals(aselector.select(), 1);

                try (SocketChannel sc2 = ssc.accept()) {
                    assertTrue(sc.finishConnect());
                    TestThread t = TestThread.of("t10a", () -> {
                        try (Selector selector = Selector.open()) {
                            ByteBuffer bb = ByteBuffer.allocate(10);
                            sc.register(selector, OP_READ);
                            selector.select();
                            int c;
                            while ((c = sc.read(bb)) == 0) ;
                            assertEquals(c, 1);
                            out.printf("read:  0x%x%n", bb.get(0));
                            assertEquals(bb.get(0), (byte) 0xAA);
                            sc.shutdownOutput();
                        }
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0xAA).flip();
                    sc2.configureBlocking(false);
                    try (Selector selector = Selector.open()) {
                        SelectionKey k = sc2.register(selector, OP_WRITE);
                        selector.select();
                        int c;
                        while ((c = sc2.write(bb)) < 1) ;
                        assertEquals(c, 1);
                        out.printf("wrote: 0x%x%n", bb.get(0));
                        k.interestOps(OP_READ);
                        selector.select();
                        bb.clear();
                        while ((c = sc2.read(bb)) == 0) ;
                        assertEquals(c, -1);
                    }
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINBAccep_NBConn_NBIO_WR_11a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                SocketChannel accepted;
                for (; ; ) {
                    accepted = ssc.accept();
                    if (accepted != null) {
                        out.println("accepted new connection");
                        break;
                    }
                    Thread.onSpinWait();
                }

                try (SocketChannel sc2 = accepted) {
                    assertTrue(sc.finishConnect());
                    TestThread t = TestThread.of("t11a", () -> {
                        try (Selector selector = Selector.open()) {
                            ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0xBA).flip();
                            sc.configureBlocking(false);
                            SelectionKey k = sc.register(selector, OP_WRITE);
                            selector.select();
                            int c;
                            while ((c = sc.write(bb)) < 1) ;
                            assertEquals(c, 1);
                            out.printf("wrote: 0x%x%n", bb.get(0));
                            k.interestOps(OP_READ);
                            selector.select();
                            bb.clear();
                            while ((c = sc.read(bb)) == 0) ;
                            assertEquals(c, -1);
                        }
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10);
                    sc2.configureBlocking(false);
                    try (Selector selector = Selector.open()) {
                        sc2.register(selector, OP_READ);
                        selector.select();
                        int c;
                        while ((c = sc2.read(bb)) == 0) ;
                        assertEquals(c, 1);
                        out.printf("read:  0x%x%n", bb.get(0));
                        assertEquals(bb.get(0), (byte) 0xBA);
                        sc2.shutdownOutput();
                    }
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    @Test(dataProvider = "family")
    public void SPINBAccep_NBConn_NBIO_RW_12a(ProtocolFamily family)
            throws Throwable {
        try (ServerSocketChannel ssc = openServerSocketChannel(family)) {
            ssc.bind(null);
            SocketAddress addr = ssc.getLocalAddress();

            try (SocketChannel sc = openSocketChannel(family)) {
                sc.configureBlocking(false);
                sc.connect(addr);

                SocketChannel accepted;
                for (; ; ) {
                    accepted = ssc.accept();
                    if (accepted != null) {
                        out.println("accepted new connection");
                        break;
                    }
                    Thread.onSpinWait();
                }

                try (SocketChannel sc2 = accepted) {
                    assertTrue(sc.finishConnect());
                    TestThread t = TestThread.of("t10a", () -> {
                        try (Selector selector = Selector.open()) {
                            ByteBuffer bb = ByteBuffer.allocate(10);
                            sc.register(selector, OP_READ);
                            selector.select();
                            int c;
                            while ((c = sc.read(bb)) == 0) ;
                            assertEquals(c, 1);
                            out.printf("read:  0x%x%n", bb.get(0));
                            assertEquals(bb.get(0), (byte) 0xCA);
                            sc.shutdownOutput();
                        }
                    });
                    t.start();

                    ByteBuffer bb = ByteBuffer.allocate(10).put((byte) 0xCA).flip();
                    sc2.configureBlocking(false);
                    try (Selector selector = Selector.open()) {
                        SelectionKey k = sc2.register(selector, OP_WRITE);
                        selector.select();
                        int c;
                        while ((c = sc2.write(bb)) < 1) ;
                        assertEquals(c, 1);
                        out.printf("wrote: 0x%x%n", bb.get(0));
                        k.interestOps(OP_READ);
                        selector.select();
                        bb.clear();
                        while ((c = sc2.read(bb)) == 0) ;
                        assertEquals(c, -1);
                    }
                    t.awaitCompletion();
                }
            }
            deleteFile(addr);
        }
    }

    // --

    static class TestThread extends Thread {
        private final UncheckedRunnable runnable;
        private volatile Throwable throwable;

        TestThread(UncheckedRunnable runnable, String name) {
            super(name);
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Throwable t) {
                out.printf("[%s] caught unexpected: %s%n", getName(), t);
                throwable = t;
            }
        }

        interface UncheckedRunnable {
            void run() throws Throwable;
        }

        static TestThread of(String name, UncheckedRunnable runnable) {
            return new TestThread(runnable, name);
        }

        void awaitCompletion() throws Throwable {
            this.join();
            if (throwable != null)
                throw throwable;
        }
    }
}
