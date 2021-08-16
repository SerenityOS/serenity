/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6842687
 * @summary Unit test for AsynchronousSocketChannel/AsynchronousServerSocketChannel
 */
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Initiates I/O operation on a thread that terminates before the I/O completes.
 */

public class DieBeforeComplete {

    public static void main(String[] args) throws Exception {
        final AsynchronousServerSocketChannel listener =
                AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(0));

        InetAddress lh = InetAddress.getLocalHost();
        int port = ((InetSocketAddress) (listener.getLocalAddress())).getPort();
        final SocketAddress sa = new InetSocketAddress(lh, port);

        // -- accept --

        // initiate accept in a thread that dies before connection is established
        Future<AsynchronousSocketChannel> r1 =
                initiateAndDie(new Task<AsynchronousSocketChannel>() {
            public Future<AsynchronousSocketChannel> run() {
                return listener.accept();
            }});

        // establish and accept connection
        SocketChannel peer = SocketChannel.open(sa);
        final AsynchronousSocketChannel channel = r1.get();

        // --- read --

        // initiate read in a thread that dies befores bytes are available
        final ByteBuffer dst = ByteBuffer.allocate(100);
        Future<Integer> r2 = initiateAndDie(new Task<Integer>() {
            public Future<Integer> run() {
                return channel.read(dst);
            }});

        // send bytes
        peer.write(ByteBuffer.wrap("hello".getBytes()));
        int nread = r2.get();
        if (nread <= 0)
            throw new RuntimeException("Should have read at least one byte");

        // -- write --

        // initiate writes in threads that dies
        boolean completedImmediately;
        Future<Integer> r3;
        do {
            final ByteBuffer src = ByteBuffer.wrap(new byte[10000]);
            r3 = initiateAndDie(new Task<Integer>() {
                public Future<Integer> run() {
                    return channel.write(src);
                }});
            try {
                int nsent = r3.get(5, TimeUnit.SECONDS);
                if (nsent <= 0)
                    throw new RuntimeException("Should have wrote at least one byte");
                completedImmediately = true;
            } catch (TimeoutException x) {
                completedImmediately = false;
            }
        } while (completedImmediately);

        // drain connection
        peer.configureBlocking(false);
        ByteBuffer src = ByteBuffer.allocateDirect(10000);
        do {
            src.clear();
            nread = peer.read(src);
            if (nread == 0) {
                Thread.sleep(100);
                nread = peer.read(src);
            }
        } while (nread > 0);

        // write should complete now
        int nsent = r3.get();
        if (nsent <= 0)
            throw new RuntimeException("Should have wrote at least one byte");
    }

    static interface Task<T> {
        Future<T> run();
    }

    static <T> Future<T> initiateAndDie(final Task<T> task) {
        final AtomicReference<Future<T>> result = new AtomicReference<Future<T>>();
        Runnable r = new Runnable() {
            public void run() {
                result.set(task.run());
            }
        };
        Thread t = new Thread(r);
        t.start();
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException x) {
            }
        }
        return result.get();
    }
}
