/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198928
 * @library /test/lib
 * @build jdk.test.lib.Utils
 * @run main CloseDuringConnect
 * @summary Attempt to cause a deadlock by closing a SocketChannel in one thread
 *     where another thread is closing the channel after a connect fail
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import jdk.test.lib.Utils;

public class CloseDuringConnect {

    // number of test iterations, needs to be 5-10 at least
    static final int ITERATIONS = 50;

    // maximum delay before closing SocketChannel, in milliseconds
    static final int MAX_DELAY_BEFORE_CLOSE = 20;

    /**
     * Invoked by a task in the thread pool to connect to a remote address.
     * The connection should never be established.
     */
    static Void connect(SocketChannel sc, SocketAddress remote) {
        try {
            if (!sc.connect(remote)) {
                while (!sc.finishConnect()) {
                    Thread.yield();
                }
            }
            throw new RuntimeException("Connected, should not happen");
        } catch (IOException expected) { }
        if (sc.isConnected())
            throw new RuntimeException("isConnected return true, should not happen");
        return null;
    }

    /**
     * Invoked by a task in the thread pool to close a socket channel.
     */
    static Void close(SocketChannel sc) {
        try {
            sc.close();
        } catch (IOException e) {
            throw new UncheckedIOException("close failed", e);
        }
        return null;
    }

    /**
     * Test for deadlock by submitting a task to connect to the given address
     * while another task closes the socket channel.
     * @param pool the thread pool to submit or schedule tasks
     * @param remote the remote address, does not accept connections
     * @param blocking socket channel blocking mode
     * @param delay the delay, in millis, before closing the channel
     */
    static void test(ScheduledExecutorService pool,
                     SocketAddress remote,
                     boolean blocking,
                     long delay) {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(blocking);
            Future<Void> r1 = pool.submit(() -> connect(sc, remote));
            Future<Void> r2 = pool.schedule(() -> close(sc), delay, MILLISECONDS);
            r1.get();
            r2.get();
        } catch (Throwable t) {
            throw new RuntimeException("Test failed", t);
        }
    }

    public static void main(String[] args) throws Exception {
        SocketAddress refusing = Utils.refusingEndpoint();
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
        try {
            IntStream.range(0, ITERATIONS).forEach(i -> {
                System.out.format("Iteration %d ...%n", (i + 1));

                // Execute the test for varying delays up to MAX_DELAY_BEFORE_CLOSE,
                // for socket channels configured both blocking and non-blocking
                IntStream.range(0, MAX_DELAY_BEFORE_CLOSE).forEach(delay -> {
                    test(pool, refusing, /*blocking mode*/true, delay);
                    test(pool, refusing, /*blocking mode*/false, delay);
                });
            });
        } finally {
            pool.shutdown();
        }
    }
}
