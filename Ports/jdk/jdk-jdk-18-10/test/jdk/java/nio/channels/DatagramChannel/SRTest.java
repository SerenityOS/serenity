/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test DatagramChannel's send and receive methods
 * @run testng/othervm/timeout=20 SRTest
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import static java.nio.charset.StandardCharsets.US_ASCII;

import org.testng.annotations.*;

public class SRTest {

    ExecutorService executorService;
    static PrintStream log = System.err;

    static final String DATA_STRING = "hello";

    @BeforeClass
    public void beforeClass() {
        executorService = Executors.newCachedThreadPool();
    }

    @AfterClass
    public void afterClass() {
        executorService.shutdown();
    }

    @Test
    public void classicReaderClassicWriter() throws Exception {
        try (ClassicReader cr = new ClassicReader();
             ClassicWriter cw = new ClassicWriter(cr.port())) {
            invoke(executorService, cr, cw);
        }
        log.println("Classic RW: OK");
    }

    @Test
    public void classicReaderNioWriter() throws Exception {
        try (ClassicReader cr = new ClassicReader();
             NioWriter nw = new NioWriter(cr.port())) {
            invoke(executorService, cr, nw);
        }
        log.println("Classic R, Nio W: OK");
    }

    @Test
    public void nioReaderClassicWriter() throws Exception {
        try (NioReader nr = new NioReader();
             ClassicWriter cw = new ClassicWriter(nr.port())) {
            invoke(executorService, nr, cw);
        }
        log.println("Classic W, Nio R: OK");
    }

    @Test
    public void nioReaderNioWriter() throws Exception {
        try (NioReader nr = new NioReader();
             NioWriter nw = new NioWriter(nr.port())) {
            invoke(executorService, nr, nw);
        }
        log.println("Nio RW: OK");
    }

    private static void invoke(ExecutorService e, Runnable reader, Runnable writer) {
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(writer, e);
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(reader, e);
        wait(f1, f2);
    }

    // Exit with CompletionException if any passed futures complete exceptionally
    private static void wait(CompletableFuture<?>... futures) throws CompletionException {
        CompletableFuture<?> future = CompletableFuture.allOf(futures);
        Stream.of(futures)
                .forEach(f -> f.exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                }));
        future.join();
    }

    public static class ClassicWriter implements Runnable, AutoCloseable {
        final DatagramSocket ds;
        final int dstPort;

        ClassicWriter(int dstPort) throws SocketException {
            this.dstPort = dstPort;
            this.ds = new DatagramSocket();
        }

        public void run() {
            try {
                byte[] data = DATA_STRING.getBytes(US_ASCII);
                InetAddress address = InetAddress.getLoopbackAddress();
                DatagramPacket dp = new DatagramPacket(data, data.length,
                                                       address, dstPort);
                ds.send(dp);
            } catch (Exception e) {
                log.println("ClassicWriter [" + ds.getLocalAddress() + "]");
                throw new RuntimeException("ClassicWriter threw exception: " + e);
            } finally {
                log.println("ClassicWriter finished");
            }
        }

        @Override
        public void close() throws IOException {
            ds.close();
        }
    }

    public static class NioWriter implements Runnable, AutoCloseable {
        final DatagramChannel dc;
        final int dstPort;

        NioWriter(int dstPort) throws IOException {
            this.dc = DatagramChannel.open();
            this.dstPort = dstPort;
        }

        public void run() {
            try {
                ByteBuffer bb = ByteBuffer.allocateDirect(256);
                bb.put(DATA_STRING.getBytes(US_ASCII));
                bb.flip();
                InetAddress address = InetAddress.getLoopbackAddress();
                InetSocketAddress isa = new InetSocketAddress(address, dstPort);
                dc.send(bb, isa);
            } catch (Exception ex) {
                log.println("NioWriter [" + dc.socket().getLocalAddress() + "]");
                throw new RuntimeException("NioWriter threw exception: " + ex);
            } finally {
                log.println("NioWriter finished");
            }
        }

        @Override
        public void close() throws IOException {
            dc.close();
        }
    }

    public static class ClassicReader implements Runnable, AutoCloseable {
        final DatagramSocket ds;

        ClassicReader() throws IOException {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
            this.ds = new DatagramSocket(address);
        }

        int port() {
            return ds.getLocalPort();
        }

        public void run() {
            try {
                byte[] buf = new byte[256];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                ds.receive(dp);
                String received = new String(dp.getData(), dp.getOffset(), dp.getLength(), US_ASCII);
                log.println("ClassicReader received: " + received);
            } catch (Exception ex) {
                log.println("ClassicReader [" + ds.getLocalAddress() +"]");
                throw new RuntimeException("ClassicReader threw exception: " + ex);
            } finally {
                log.println("ClassicReader finished");
            }
        }

        @Override
        public void close() throws IOException {
            ds.close();
        }
    }

    public static class NioReader implements Runnable, AutoCloseable {
        final DatagramChannel dc;

        NioReader() throws IOException {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
            this.dc = DatagramChannel.open().bind(address);
        }

        int port() {
            return dc.socket().getLocalPort();
        }

        public void run() {
            try {
                ByteBuffer bb = ByteBuffer.allocateDirect(100);
                dc.receive(bb);
                bb.flip();
                CharBuffer cb = US_ASCII.newDecoder().decode(bb);
                log.println("NioReader received: " + cb);
            } catch (Exception ex) {
                log.println("NioReader [" + dc.socket().getLocalAddress() +"]");
                throw new RuntimeException("NioReader threw exception: " + ex);
            } finally {
                log.println("NioReader finished");
            }
        }

        @Override
        public void close() throws IOException {
            dc.close();
        }
    }
}
