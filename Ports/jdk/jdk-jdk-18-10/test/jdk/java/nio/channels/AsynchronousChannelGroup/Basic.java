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
 * @bug 4607272
 * @summary Unit test for AsynchronousChannelGroup
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;

public class Basic {
    static final Random rand = new Random();
    static final ThreadFactory threadFactory = (Runnable r) -> {
        return new Thread(r);
    };

    public static void main(String[] args) throws Exception {
        shutdownTests();
        shutdownNowTests();
        afterShutdownTests();
        miscTests();
    }

    static void awaitTermination(AsynchronousChannelGroup group) throws InterruptedException {
        boolean terminated = group.awaitTermination(20, TimeUnit.SECONDS);
        if (!terminated)
            throw new RuntimeException("Group should have terminated");
    }

    static void testShutdownWithNoChannels(ExecutorService pool,
                                           AsynchronousChannelGroup group)
        throws Exception
    {
        group.shutdown();
        if (!group.isShutdown())
            throw new RuntimeException("Group should be shutdown");
        // group should terminate quickly
        awaitTermination(group);
        if (pool != null && !pool.isTerminated())
            throw new RuntimeException("Executor should have terminated");
    }

    static void testShutdownWithChannels(ExecutorService pool,
                                         AsynchronousChannelGroup group)
        throws Exception
    {

        // create channel that is bound to group
        AsynchronousChannel ch;
        switch (rand.nextInt(2)) {
            case 0 : ch = AsynchronousSocketChannel.open(group); break;
            case 1 : ch = AsynchronousServerSocketChannel.open(group); break;
            default : throw new AssertionError();
        }
        group.shutdown();
        if (!group.isShutdown())
            throw new RuntimeException("Group should be shutdown");

        // last channel so should terminate after this channel is closed
        ch.close();

        // group should terminate quickly
        awaitTermination(group);
        if (pool != null && !pool.isTerminated())
            throw new RuntimeException("Executor should have terminated");
    }

    static void shutdownTests() throws Exception {
        System.out.println("-- test shutdown --");

        // test shutdown with no channels in groups
        for (int i = 0; i < 100; i++) {
            ExecutorService pool = Executors.newCachedThreadPool();
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withCachedThreadPool(pool, rand.nextInt(5));
            testShutdownWithNoChannels(pool, group);
        }
        for (int i = 0; i < 100; i++) {
            int nThreads = 1 + rand.nextInt(8);
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withFixedThreadPool(nThreads, threadFactory);
            testShutdownWithNoChannels(null, group);
        }
        for (int i = 0; i < 100; i++) {
            ExecutorService pool = Executors.newCachedThreadPool();
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withThreadPool(pool);
            testShutdownWithNoChannels(pool, group);
        }

        // test shutdown with channel in group
        for (int i = 0; i < 100; i++) {
            ExecutorService pool = Executors.newCachedThreadPool();
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withCachedThreadPool(pool, rand.nextInt(10));
            try {
                testShutdownWithChannels(pool, group);
            } finally {
                group.shutdown();
            }
        }
        for (int i = 0; i < 100; i++) {
            int nThreads = 1 + rand.nextInt(8);
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withFixedThreadPool(nThreads, threadFactory);
            try {
                testShutdownWithChannels(null, group);
            } finally {
                group.shutdown();
            }
        }
        for (int i = 0; i < 100; i++) {
            ExecutorService pool = Executors.newCachedThreadPool();
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withThreadPool(pool);
            try {
                testShutdownWithChannels(pool, group);
            } finally {
                group.shutdown();
            }
        }
    }

    static void testShutdownNow(ExecutorService pool,
                                AsynchronousChannelGroup group)
        throws Exception
    {
        // I/O in progress
        AsynchronousServerSocketChannel ch = AsynchronousServerSocketChannel
                .open(group).bind(new InetSocketAddress(0));
        ch.accept();

        // forceful shutdown
        group.shutdownNow();

        // shutdownNow is required to close all channels
        if (ch.isOpen())
            throw new RuntimeException("Channel should be closed");

        awaitTermination(group);

        if (pool != null && !pool.isTerminated())
            throw new RuntimeException("Executor should have terminated");
    }

    static void shutdownNowTests() throws Exception {
        System.out.println("-- test shutdownNow --");

        for (int i = 0; i < 10; i++) {
            ExecutorService pool = pool = Executors.newCachedThreadPool();
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withCachedThreadPool(pool, rand.nextInt(5));
            try {
                testShutdownNow(pool, group);
            } finally {
                group.shutdown();
            }
        }
        for (int i = 0; i < 10; i++) {
            int nThreads = 1 + rand.nextInt(8);
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withFixedThreadPool(nThreads, threadFactory);
            try {
                testShutdownNow(null, group);
            } finally {
                group.shutdown();
            }
        }
        for (int i = 0; i < 10; i++) {
            ExecutorService pool = Executors.newCachedThreadPool();
            AsynchronousChannelGroup group = AsynchronousChannelGroup
                    .withThreadPool(pool);
            try {
                testShutdownNow(pool, group);
            } finally {
                group.shutdown();
            }
        }
    }

    // test creating channels in group after group is shutdown
    static void afterShutdownTests() throws Exception {
        System.out.println("-- test operations after group is shutdown  --");
        AsynchronousChannelGroup group =
            AsynchronousChannelGroup.withFixedThreadPool(1, threadFactory);

        try (AsynchronousSocketChannel ch = AsynchronousSocketChannel.open(group);
                AsynchronousServerSocketChannel listener =
                    AsynchronousServerSocketChannel.open(group)) {

            // initiate accept
            listener.bind(new InetSocketAddress(0));
            Future<AsynchronousSocketChannel> result = listener.accept();

            // shutdown group
            group.shutdown();
            if (!group.isShutdown())
                throw new RuntimeException("Group should be shutdown");

            // attempt to create another channel
            try {
                AsynchronousSocketChannel.open(group);
                throw new RuntimeException("ShutdownChannelGroupException expected");
            } catch (ShutdownChannelGroupException x) {
            }
            try {
                AsynchronousServerSocketChannel.open(group);
                throw new RuntimeException("ShutdownChannelGroupException expected");
            } catch (ShutdownChannelGroupException x) {
            }

            // attempt to create another channel by connecting. This should cause
            // the accept operation to fail.
            InetAddress lh = InetAddress.getLocalHost();
            int port = ((InetSocketAddress)listener.getLocalAddress()).getPort();
            InetSocketAddress isa = new InetSocketAddress(lh, port);
            ch.connect(isa).get();
            try {
                result.get();
                throw new RuntimeException("Connection was accepted");
            } catch (ExecutionException x) {
                Throwable cause = x.getCause();
                if (!(cause instanceof IOException))
                    throw new RuntimeException("Cause should be IOException");
                cause = cause.getCause();
                if (!(cause instanceof ShutdownChannelGroupException))
                    throw new RuntimeException("IOException cause should be ShutdownChannelGroupException");
            }

            // initiate another accept even though channel group is shutdown.
            Future<AsynchronousSocketChannel> res = listener.accept();
            try {
                res.get(3, TimeUnit.SECONDS);
                throw new RuntimeException("TimeoutException expected");
            } catch (TimeoutException x) {
            }
            // connect to the listener which should cause the accept to complete
            AsynchronousSocketChannel.open().connect(isa);
            try {
                res.get();
                throw new RuntimeException("Connection was accepted");
            } catch (ExecutionException x) {
                Throwable cause = x.getCause();
                if (!(cause instanceof IOException))
                    throw new RuntimeException("Cause should be IOException");
                cause = cause.getCause();
                if (!(cause instanceof ShutdownChannelGroupException))
                    throw new RuntimeException("IOException cause should be ShutdownChannelGroupException");
            }

            // group should *not* terminate as channels are open
            boolean terminated = group.awaitTermination(3, TimeUnit.SECONDS);
            if (terminated) {
                throw new RuntimeException("Group should not have terminated");
            }
        } finally {
            group.shutdown();
        }
    }

    static void miscTests() throws Exception {
        System.out.println("-- miscellenous tests --");
        try {
            AsynchronousChannelGroup.withFixedThreadPool(1, null);
            throw new RuntimeException("NPE expected");
        } catch (NullPointerException x) {
        }
        try {
            AsynchronousChannelGroup.withFixedThreadPool(0, threadFactory);
            throw new RuntimeException("IAE expected");
        } catch (IllegalArgumentException e) {
        }
        try {
            AsynchronousChannelGroup.withCachedThreadPool(null, 0);
            throw new RuntimeException("NPE expected");
        } catch (NullPointerException x) {
        }
        try {
            AsynchronousChannelGroup.withThreadPool(null);
            throw new RuntimeException("NPE expected");
        } catch (NullPointerException e) {
        }
    }
}
