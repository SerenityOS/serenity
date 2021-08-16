/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313882 7183800
 * @summary Test DatagramChannel's send and receive methods
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

public class Connect {

    static PrintStream log = System.err;

    public static void main(String[] args) throws Exception {
        test();
    }

    static void test() throws Exception {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        try (Responder r = new Responder();
             Initiator a = new Initiator(r.getSocketAddress())
        ) {
            invoke(threadPool, a, r);
        } finally {
            threadPool.shutdown();
        }
    }

    static void invoke(ExecutorService e, Runnable reader, Runnable writer) throws CompletionException {
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(writer, e);
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(reader, e);
        wait(f1, f2);
    }


    // This method waits for either one of the given futures to complete exceptionally
    // or for all of the given futures to complete successfully.
    private static void wait(CompletableFuture<?>... futures) throws CompletionException {
        CompletableFuture<?> future = CompletableFuture.allOf(futures);
        Stream.of(futures)
                .forEach(f -> f.exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                }));
        future.join();
    }

    private static SocketAddress toConnectAddress(SocketAddress address) {
        if (address instanceof InetSocketAddress) {
            var inet = (InetSocketAddress) address;
            if (inet.getAddress().isAnyLocalAddress()) {
                // if the peer is bound to the wildcard address, use
                // the loopback address to connect.
                var loopback = InetAddress.getLoopbackAddress();
                return new InetSocketAddress(loopback, inet.getPort());
            }
        }
        return address;
    }

    public static class Initiator implements AutoCloseable, Runnable {
        final SocketAddress connectSocketAddress;
        final DatagramChannel dc;

        Initiator(SocketAddress peerSocketAddress) throws IOException {
            this.connectSocketAddress = toConnectAddress(peerSocketAddress);
            dc = DatagramChannel.open();
        }

        public void run() {
            try {
                ByteBuffer bb = ByteBuffer.allocateDirect(256);
                bb.put("hello".getBytes());
                bb.flip();
                log.println("Initiator connecting to " + connectSocketAddress);
                dc.connect(connectSocketAddress);

                // Send a message
                log.println("Initiator attempting to write to Responder at " + connectSocketAddress.toString());
                dc.write(bb);

                // Try to send to some other address
                try {
                    int port = dc.socket().getLocalPort();
                    InetAddress loopback = InetAddress.getLoopbackAddress();
                    InetSocketAddress otherAddress = new InetSocketAddress(loopback, (port == 3333 ? 3332 : 3333));
                    log.println("Testing if Initiator throws AlreadyConnectedException" + otherAddress.toString());
                    dc.send(bb, otherAddress);
                    throw new RuntimeException("Initiator allowed send to other address while already connected");
                } catch (AlreadyConnectedException ace) {
                    // Correct behavior
                }

                // Read a reply
                bb.flip();
                log.println("Initiator waiting to read");
                dc.read(bb);
                bb.flip();
                CharBuffer cb = StandardCharsets.US_ASCII.
                        newDecoder().decode(bb);
                log.println("Initiator received from Responder at " + connectSocketAddress + ": " + cb);
            } catch (Exception ex) {
                log.println("Initiator threw exception: " + ex);
                throw new RuntimeException(ex);
            } finally {
                log.println("Initiator finished");
            }
        }

        @Override
        public void close() throws IOException {
            dc.close();
        }
    }

    public static class Responder implements AutoCloseable, Runnable {
        final DatagramChannel dc;

        Responder() throws IOException {
            var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
            dc = DatagramChannel.open().bind(address);
        }

        SocketAddress getSocketAddress() throws IOException {
            return dc.getLocalAddress();
        }

        public void run() {
            try {
                // Listen for a message
                ByteBuffer bb = ByteBuffer.allocateDirect(100);
                log.println("Responder waiting to receive");
                SocketAddress sa = dc.receive(bb);
                bb.flip();
                CharBuffer cb = StandardCharsets.US_ASCII.
                        newDecoder().decode(bb);
                log.println("Responder received from Initiator at" + sa +  ": " + cb);

                // Reply to sender
                dc.connect(sa);
                bb.flip();
                log.println("Responder attempting to write: " + dc.getRemoteAddress().toString());
                dc.write(bb);
            } catch (Exception ex) {
                log.println("Responder threw exception: " + ex);
                throw new RuntimeException(ex);
            } finally {
                log.println("Responder finished");
            }
        }

        @Override
        public void close() throws IOException {
            dc.close();
        }
    }
}
