/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7184932 8232673
 * @summary Test asynchronous close and interrupt of timed socket adapter methods
 * @key randomness intermittent
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.net.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Random;


public class AdaptorCloseAndInterrupt {
    private static final ScheduledExecutorService pool =
        Executors.newScheduledThreadPool(1);
    final ServerSocketChannel listener;
    final DatagramChannel peer;
    final int port;

    final AtomicBoolean isClosed = new AtomicBoolean();
    final AtomicBoolean isInterrupted = new AtomicBoolean();

    public AdaptorCloseAndInterrupt() {
        listener = null;
        peer = null;
        port = -1;
    }

    public AdaptorCloseAndInterrupt(ServerSocketChannel listener) {
        this.listener = listener;
        this.port = listener.socket().getLocalPort();
        this.peer = null;
    }

    public AdaptorCloseAndInterrupt(DatagramChannel listener) {
        this.peer = listener;
        this.port = peer.socket().getLocalPort();
        this.listener = null;
    }

    public static void main(String args[]) throws Exception {
        try {
            try (ServerSocketChannel listener = ServerSocketChannel.open()) {
                listener.socket().bind(null);
                new AdaptorCloseAndInterrupt(listener).scReadAsyncClose();
                new AdaptorCloseAndInterrupt(listener).scReadAsyncInterrupt();
            }

            try (DatagramChannel peer = DatagramChannel.open()) {
                peer.socket().bind(null);
                new AdaptorCloseAndInterrupt(peer).dcReceiveAsyncClose(0);
                new AdaptorCloseAndInterrupt(peer).dcReceiveAsyncClose(30_000);
                new AdaptorCloseAndInterrupt(peer).dcReceiveAsyncInterrupt(0);
                new AdaptorCloseAndInterrupt(peer).dcReceiveAsyncInterrupt(30_000);
            }

            new AdaptorCloseAndInterrupt().ssAcceptAsyncClose();
            new AdaptorCloseAndInterrupt().ssAcceptAsyncInterrupt();
        } finally {
            pool.shutdown();
        }
        System.out.println("Test Passed");
    }

    void scReadAsyncClose() throws IOException {
        try {
            SocketChannel sc = SocketChannel.open(new InetSocketAddress(
                InetAddress.getLoopbackAddress(), port));
            sc.socket().setSoTimeout(30*1000);

            doAsyncClose(sc);

            try {
                sc.socket().getInputStream().read(new byte[100]);
                System.err.format("close() was invoked: %s%n", isClosed.get());
                throw new RuntimeException("read should not have completed");
            } catch (ClosedChannelException expected) {}

            if (!sc.socket().isClosed())
                throw new RuntimeException("socket is not closed");
        } finally {
            // accept connection and close it.
            listener.accept().close();
        }
    }

    void scReadAsyncInterrupt() throws IOException {
        try {
            final SocketChannel sc = SocketChannel.open(new InetSocketAddress(
                InetAddress.getLoopbackAddress(), port));
            sc.socket().setSoTimeout(30*1000);

            doAsyncInterrupt();

            try {
                sc.socket().getInputStream().read(new byte[100]);
                throw new RuntimeException("read should not have completed");
            } catch (ClosedByInterruptException expected) {
                System.out.format("interrupt() was invoked: %s%n",
                    isInterrupted.get());
                System.out.format("scReadAsyncInterrupt was interrupted: %s%n",
                    Thread.currentThread().interrupted());
            }

            if (!sc.socket().isClosed())
                throw new RuntimeException("socket is not closed");
        } finally {
            // accept connection and close it.
            listener.accept().close();
        }
    }

    void dcReceiveAsyncClose(int timeout) throws IOException {
        DatagramChannel dc = DatagramChannel.open();
        dc.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        dc.socket().setSoTimeout(timeout);

        doAsyncClose(dc);

        try {
            dc.socket().receive(new DatagramPacket(new byte[100], 100));
            System.err.format("close() was invoked: %s%n", isClosed.get());
            throw new RuntimeException("receive should not have completed");
        } catch (SocketException expected) { }

        if (!dc.socket().isClosed())
            throw new RuntimeException("socket is not closed");
    }

    void dcReceiveAsyncInterrupt(int timeout) throws IOException {
        DatagramChannel dc = DatagramChannel.open();
        dc.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        dc.socket().setSoTimeout(timeout);

        doAsyncInterrupt();

        try {
            dc.socket().receive(new DatagramPacket(new byte[100], 100));
            throw new RuntimeException("receive should not have completed");
        } catch (SocketException expected) {
            System.out.format("interrupt() was invoked: %s%n",
                isInterrupted.get());
            System.out.format("dcReceiveAsyncInterrupt was interrupted: %s%n",
                Thread.currentThread().interrupted());
        } catch (SocketTimeoutException unexpected) {
            System.err.format("Receive thread interrupt invoked: %s%n",
                isInterrupted.get());
            System.err.format("Receive thread was interrupted: %s%n",
                Thread.currentThread().isInterrupted());
            throw unexpected;
        }

        if (!dc.socket().isClosed())
            throw new RuntimeException("socket is not closed");
    }

    void ssAcceptAsyncClose() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(null);
        ssc.socket().setSoTimeout(30*1000);

        doAsyncClose(ssc);

        try {
            ssc.socket().accept();
            System.err.format("close() was invoked: %s%n", isClosed.get());
            throw new RuntimeException("accept should not have completed");
        } catch (ClosedChannelException expected) {}

        if (!ssc.socket().isClosed())
            throw new RuntimeException("socket is not closed");
    }

    void ssAcceptAsyncInterrupt() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(null);
        ssc.socket().setSoTimeout(30*1000);

        doAsyncInterrupt();

        try {
            ssc.socket().accept();
            throw new RuntimeException("accept should not have completed");
        } catch (ClosedByInterruptException expected) {
            System.out.format("interrupt() was invoked: %s%n",
                isInterrupted.get());
            System.out.format("ssAcceptAsyncInterrupt was interrupted: %s%n",
                Thread.currentThread().interrupted());
        }

        if (!ssc.socket().isClosed())
            throw new RuntimeException("socket is not closed");
    }

    void doAsyncClose(final AbstractSelectableChannel sc) {
        AdaptorCloseAndInterrupt.pool.schedule(new Callable<Void>() {
            public Void call() throws Exception {
                sc.close();
                isClosed.set(true);
                return null;
            }
        }, new Random().nextInt(1000), TimeUnit.MILLISECONDS);
    }

    void doAsyncInterrupt() {
        final Thread current = Thread.currentThread();
        AdaptorCloseAndInterrupt.pool.schedule(new Callable<Void>() {
            public Void call() throws Exception {
                current.interrupt();
                isInterrupted.set(true);
                return null;
            }
        }, new Random().nextInt(1000), TimeUnit.MILLISECONDS);
    }

}
