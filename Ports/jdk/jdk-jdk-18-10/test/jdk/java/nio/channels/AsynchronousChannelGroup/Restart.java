/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 */

import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.IOException;

/**
 * Exercise replacement of threads in the thread pool when completion handlers
 * terminate due to errors or runtime exceptions.
 */

public class Restart {
    static final Random rand = new Random();

    public static void main(String[] args) throws Exception {
        // thread group for thread pools
        final ThreadGroup tg = new ThreadGroup("test");

        // keep track of the number of threads that terminate
        final AtomicInteger exceptionCount = new AtomicInteger(0);
        final Thread.UncaughtExceptionHandler ueh =
            new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    exceptionCount.incrementAndGet();
                }
            };
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(tg, r);
                t.setUncaughtExceptionHandler(ueh);
                return t;
            }
        };

        // group with fixed thread pool
        int nThreads = 1 + rand.nextInt(4);
        AsynchronousChannelGroup group =
                AsynchronousChannelGroup.withFixedThreadPool(nThreads, factory);
        try {
            testRestart(group, 100);
        } finally {
            group.shutdown();
        }

        // group with cached thread pool
        ExecutorService pool = Executors.newCachedThreadPool(factory);
        group = AsynchronousChannelGroup.withCachedThreadPool(pool, rand.nextInt(5));
        try {
            testRestart(group, 100);
        } finally {
            group.shutdown();
        }

        // group with custom thread pool
        group = AsynchronousChannelGroup.withThreadPool(
                Executors.newFixedThreadPool(1+rand.nextInt(5), factory));
        try {
            testRestart(group, 100);
        } finally {
            group.shutdown();
        }

        // give time for threads to terminate
        Thread.sleep(3000);
        int actual = exceptionCount.get();
        if (actual != 300)
            throw new RuntimeException(actual + " exceptions, expected: " + 300);
    }

    static void testRestart(AsynchronousChannelGroup group, int count)
        throws Exception
    {
        try (AsynchronousServerSocketChannel listener =
                AsynchronousServerSocketChannel.open(group)) {

            listener.bind(new InetSocketAddress(0));
            for (int i=0; i<count; i++) {
                final CountDownLatch latch = new CountDownLatch(1);

                listener.accept((Void)null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
                    public void completed(AsynchronousSocketChannel ch, Void att) {
                        try {
                            ch.close();
                        } catch (IOException ignore) { }

                        latch.countDown();

                        // throw error or runtime exception
                        if (rand.nextBoolean()) {
                            throw new Error();
                        } else {
                            throw new RuntimeException();
                        }
                    }
                    public void failed(Throwable exc, Void att) {
                    }
                });

                // establish loopback connection which should cause completion
                // handler to be invoked.
                int port = ((InetSocketAddress)(listener.getLocalAddress())).getPort();
                try (AsynchronousSocketChannel ch = AsynchronousSocketChannel.open()) {
                    InetAddress lh = InetAddress.getLocalHost();
                    ch.connect(new InetSocketAddress(lh, port)).get();
                }

                // wait for handler to be invoked
                latch.await();
            }
        }
    }
}
