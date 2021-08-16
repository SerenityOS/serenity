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
 * @bug 8202252
 * @run testng CompletionHandlerRelease
 * @summary Verify that reference to CompletionHandler is cleared after use
 */

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import static java.net.StandardSocketOptions.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CompletionHandlerRelease {
    @Test
    public void testConnect() throws Exception {
        try (Server server = new Server()) {
            try (AsynchronousSocketChannel ch =
                 AsynchronousSocketChannel.open(GROUP)) {
                CountDownLatch latch = new CountDownLatch(1);
                Handler<Void,Object> handler =
                    new Handler<Void,Object>("connect", latch);
                ReferenceQueue queue = new ReferenceQueue<WeakReference>();
                WeakReference<Object> ref =
                    new WeakReference<Object>(handler, queue);

                ch.connect(server.address(), null, handler);

                try { latch.await(); } catch (InterruptedException ignore) { }

                handler = null;
                waitForRefToClear(ref, queue);

                server.accept().get().close();
            }
        }
    }

    @Test
    public void testWrite() throws Exception {
        try (Server server = new Server();
             AsynchronousSocketChannel ch =
                 AsynchronousSocketChannel.open(GROUP)) {
            ch.connect(server.address()).get();

            try (AsynchronousSocketChannel sc = server.accept().get()) {
                ByteBuffer src = ByteBuffer.wrap("hello".getBytes("UTF-8"));
                sc.setOption(SO_SNDBUF, src.remaining());

                CountDownLatch latch = new CountDownLatch(1);
                Handler<Integer,Object> handler =
                    new Handler<Integer,Object>("write", latch);
                ReferenceQueue queue = new ReferenceQueue<WeakReference>();
                WeakReference<Object> ref =
                    new WeakReference<Object>(handler, queue);

                sc.write(src, null, handler);

                try { latch.await(); } catch (InterruptedException ignore) { }

                handler = null;
                waitForRefToClear(ref, queue);
            }
        }
    }

    @Test
    public void testRead() throws Exception {
        try (Server server = new Server();
             AsynchronousSocketChannel ch =
                 AsynchronousSocketChannel.open(GROUP)) {
            ch.connect(server.address()).get();

            try (AsynchronousSocketChannel sc = server.accept().get()) {
                ByteBuffer src = ByteBuffer.wrap("hello".getBytes("UTF-8"));
                sc.setOption(SO_SNDBUF, src.remaining());
                sc.write(src).get();

                CountDownLatch latch = new CountDownLatch(1);
                Handler<Integer,Object> handler =
                    new Handler<Integer,Object>("read", latch);
                ReferenceQueue queue = new ReferenceQueue<WeakReference>();
                WeakReference<Object> ref =
                    new WeakReference<Object>(handler, queue);

                ByteBuffer dst = ByteBuffer.allocate(64);
                ch.read(dst, null, handler);

                try { latch.await(); } catch (InterruptedException ignore) { }

                handler = null;
                waitForRefToClear(ref, queue);
            }
        }
    }

    private AsynchronousChannelGroup GROUP;

    @BeforeTest
    void setup() throws IOException {
        GROUP = AsynchronousChannelGroup.withFixedThreadPool(2,
            Executors.defaultThreadFactory());
    }

    @AfterTest
    void cleanup() throws IOException {
        GROUP.shutdownNow();
    }

    class Server implements Closeable {
        private final AsynchronousServerSocketChannel ssc;
        private final InetSocketAddress address;

        Server() throws IOException {
            this(0);
        }

        Server(int recvBufSize) throws IOException {
            ssc = AsynchronousServerSocketChannel.open(GROUP);
            if (recvBufSize > 0) {
                ssc.setOption(SO_RCVBUF, recvBufSize);
            }
            ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(),
                0));
            address = (InetSocketAddress)ssc.getLocalAddress();
        }

        InetSocketAddress address() {
            return address;
        }

        Future<AsynchronousSocketChannel> accept() throws IOException {
            return ssc.accept();
        }

        public void close() throws IOException {
            ssc.close();
        }
    }

    static class Handler<V,A> implements CompletionHandler<V,A> {
        private final String name;
        private final CountDownLatch latch;

        Handler(String name, CountDownLatch latch) {
            this.name = name;
            this.latch = latch;
        }

        public void completed(V result, A attachment) {
            System.out.format("%s completed(%s, %s)%n",
                name, result, attachment);
            latch.countDown();
        }

        public void failed(Throwable exc, A attachment) {
            System.out.format("%s failed(%s, %s)%n",
                name, exc, attachment);
            exc.printStackTrace();
            latch.countDown();
        }
    }

    private void waitForRefToClear(Reference ref, ReferenceQueue queue)
        throws InterruptedException {
        Reference r;
        while ((r = queue.remove(20)) == null) {
            System.gc();
        }
        assertEquals(r, ref);
        assertNull(r.get());
    }
}
