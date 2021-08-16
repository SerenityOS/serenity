/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test asynchronous close during a blocking write
 * @key randomness
 */

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Random;

public class CloseDuringWrite {

    static final Random rand = new Random();

    /**
     * A task that closes a Closeable
     */
    static class Closer implements Callable<Void> {
        final Closeable c;
        Closer(Closeable c) {
            this.c = c;
        }
        public Void call() throws IOException {
            c.close();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
        try {
            try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
                ssc.bind(new InetSocketAddress(0));
                InetAddress lh = InetAddress.getLocalHost();
                int port = ssc.socket().getLocalPort();
                SocketAddress sa = new InetSocketAddress(lh, port);

                ByteBuffer bb = ByteBuffer.allocate(2*1024*1024);

                for (int i=0; i<20; i++) {
                    try (SocketChannel source = SocketChannel.open(sa);
                         SocketChannel sink = ssc.accept())
                    {
                        // schedule channel to be closed
                        Closer c = new Closer(source);
                        int when = 1000 + rand.nextInt(2000);
                        Future<Void> result = pool.schedule(c, when, TimeUnit.MILLISECONDS);

                        // the write should either succeed or else throw a
                        // ClosedChannelException (more likely an
                        // AsynchronousCloseException)
                        try {
                            for (;;) {
                                int limit = rand.nextInt(bb.capacity());
                                bb.position(0);
                                bb.limit(limit);
                                int n = source.write(bb);
                                System.out.format("wrote %d, expected %d%n", n, limit);
                            }
                        } catch (ClosedChannelException expected) {
                            System.out.println(expected + " (expected)");
                        } finally {
                            result.get();
                        }
                    }
                }
            }
        } finally {
            pool.shutdown();
        }
    }
}
