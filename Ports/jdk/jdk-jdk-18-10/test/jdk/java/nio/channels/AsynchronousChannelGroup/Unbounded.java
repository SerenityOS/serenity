/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4607272 6842687
 * @summary Unit test for AsynchronousChannelGroup
 */

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.concurrent.*;
import java.io.IOException;

public class Unbounded {
    // number of concurrent completion handlers
    static final int CONCURRENCY_COUNT = 256;

    // set to true if an I/O operation fails
    static volatile boolean failed;

    // set to true when the test is done
    static volatile boolean finished;

    public static void main(String[] args) throws Exception {
        // create listener to accept connections
        AsynchronousServerSocketChannel listener =
            AsynchronousServerSocketChannel.open()
                .bind(new InetSocketAddress(0));

        // establish connections

        AsynchronousSocketChannel[] clients = new AsynchronousSocketChannel[CONCURRENCY_COUNT];
        AsynchronousSocketChannel[] peers = new AsynchronousSocketChannel[CONCURRENCY_COUNT];

        int port = ((InetSocketAddress)(listener.getLocalAddress())).getPort();
        SocketAddress sa = new InetSocketAddress(InetAddress.getLocalHost(), port);

        for (int i=0; i<CONCURRENCY_COUNT; i++) {
            clients[i] = AsynchronousSocketChannel.open();
            Future<Void> result = clients[i].connect(sa);
            peers[i] = listener.accept().get();
            result.get();
        }
        System.out.println("All connection established.");

        // the barrier where all threads (plus the main thread) wait
        final CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY_COUNT+1);

        // initiate a read operation on each channel.
        for (AsynchronousSocketChannel client: clients) {
            ByteBuffer buf = ByteBuffer.allocateDirect(100);
            client.read(buf, client,
                new CompletionHandler<Integer,AsynchronousSocketChannel>() {
                    public void completed(Integer bytesRead, AsynchronousSocketChannel ch) {
                        try {
                            ch.close();
                            barrier.await();
                        } catch (Exception x) {
                            throw new AssertionError(x);
                        }
                    }
                    public void failed(Throwable exc, AsynchronousSocketChannel ch) {
                        failed = true;
                        System.err.println("read failed: " + exc);
                        completed(0, ch);
                    }
                });
        }
        System.out.println("All read operations outstanding.");

        // write data to each of the accepted connections
        for (AsynchronousSocketChannel peer: peers) {
            peer.write(ByteBuffer.wrap("welcome".getBytes())).get();
            peer.shutdownOutput();
            peer.close();
        }

        // wait for all threads to reach the barrier
        System.out.println("Waiting for all threads to reach barrier");
        barrier.await();

        // finish up
        finished = true;
        listener.close();
        if (failed)
            throw new RuntimeException("I/O operation failed, see log for details");
    }
}
