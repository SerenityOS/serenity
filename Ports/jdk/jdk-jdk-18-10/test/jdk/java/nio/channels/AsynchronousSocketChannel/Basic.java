/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4607272 6842687 6878369 6944810 7023403
 * @summary Unit test for AsynchronousSocketChannel(use -Dseed=X to set PRNG seed)
 * @library /test/lib
 * @modules jdk.net
 * @key randomness intermittent
 * @build jdk.test.lib.RandomFactory jdk.test.lib.Utils
 * @run main/othervm/timeout=600 Basic -skipSlowConnectTest
 */

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import static java.net.StandardSocketOptions.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import jdk.test.lib.RandomFactory;
import java.util.List;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPCOUNT;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPIDLE;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPINTERVAL;

public class Basic {
    private static final Random RAND = RandomFactory.getRandom();

    static boolean skipSlowConnectTest = false;

    public static void main(String[] args) throws Exception {
        for (String arg: args) {
            switch (arg) {
            case "-skipSlowConnectTest" :
                skipSlowConnectTest = true;
                break;
            default:
                throw new RuntimeException("Unrecognized argument: " + arg);
            }
        }

        testBind();
        testSocketOptions();
        testConnect();
        testCloseWhenPending();
        testCancel();
        testRead1();
        testRead2();
        testRead3();
        testWrite1();
        testWrite2();
        // skip timeout tests until 7052549 is fixed
        if (!System.getProperty("os.name").startsWith("Windows"))
            testTimeout();
        testShutdown();
    }

    static class Server implements Closeable {
        private final ServerSocketChannel ssc;
        private final InetSocketAddress address;

        Server() throws IOException {
            this(0);
        }

        Server(int recvBufSize) throws IOException {
            ssc = ServerSocketChannel.open();
            if (recvBufSize > 0) {
                ssc.setOption(SO_RCVBUF, recvBufSize);
            }
            ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            address = (InetSocketAddress)ssc.getLocalAddress();
        }

        InetSocketAddress address() {
            return address;
        }

        SocketChannel accept() throws IOException {
            return ssc.accept();
        }

        public void close() throws IOException {
            ssc.close();
        }

    }

    static void testBind() throws Exception {
        System.out.println("-- bind --");

        try (AsynchronousSocketChannel ch = AsynchronousSocketChannel.open()) {
            if (ch.getLocalAddress() != null)
                throw new RuntimeException("Local address should be 'null'");
            ch.bind(new InetSocketAddress(0));

            // check local address after binding
            InetSocketAddress local = (InetSocketAddress)ch.getLocalAddress();
            if (local.getPort() == 0)
                throw new RuntimeException("Unexpected port");
            if (!local.getAddress().isAnyLocalAddress())
                throw new RuntimeException("Not bound to a wildcard address");

            // try to re-bind
            try {
                ch.bind(new InetSocketAddress(0));
                throw new RuntimeException("AlreadyBoundException expected");
            } catch (AlreadyBoundException x) {
            }
        }

        // check ClosedChannelException
        AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
        ch.close();
        try {
            ch.bind(new InetSocketAddress(0));
            throw new RuntimeException("ClosedChannelException  expected");
        } catch (ClosedChannelException  x) {
        }
    }

    static void testSocketOptions() throws Exception {
        System.out.println("-- socket options --");

        try (AsynchronousSocketChannel ch = AsynchronousSocketChannel.open()) {
            ch.setOption(SO_RCVBUF, 128*1024)
              .setOption(SO_SNDBUF, 128*1024)
              .setOption(SO_REUSEADDR, true);

            // check SO_SNDBUF/SO_RCVBUF limits
            int before, after;
            before = ch.getOption(SO_SNDBUF);
            after = ch.setOption(SO_SNDBUF, Integer.MAX_VALUE).getOption(SO_SNDBUF);
            if (after < before)
                throw new RuntimeException("setOption caused SO_SNDBUF to decrease");
            before = ch.getOption(SO_RCVBUF);
            after = ch.setOption(SO_RCVBUF, Integer.MAX_VALUE).getOption(SO_RCVBUF);
            if (after < before)
                throw new RuntimeException("setOption caused SO_RCVBUF to decrease");

            ch.bind(new InetSocketAddress(0));

            // default values
            if (ch.getOption(SO_KEEPALIVE))
                throw new RuntimeException("Default of SO_KEEPALIVE should be 'false'");
            if (ch.getOption(TCP_NODELAY))
                throw new RuntimeException("Default of TCP_NODELAY should be 'false'");

            // set and check
            if (!ch.setOption(SO_KEEPALIVE, true).getOption(SO_KEEPALIVE))
                throw new RuntimeException("SO_KEEPALIVE did not change");
            if (!ch.setOption(TCP_NODELAY, true).getOption(TCP_NODELAY))
                throw new RuntimeException("SO_KEEPALIVE did not change");

            // read others (can't check as actual value is implementation dependent)
            ch.getOption(SO_RCVBUF);
            ch.getOption(SO_SNDBUF);

            Set<SocketOption<?>> options = ch.supportedOptions();
            boolean reuseport = options.contains(SO_REUSEPORT);
            if (reuseport) {
                if (ch.getOption(SO_REUSEPORT))
                    throw new RuntimeException("Default of SO_REUSEPORT should be 'false'");
                if (!ch.setOption(SO_REUSEPORT, true).getOption(SO_REUSEPORT))
                    throw new RuntimeException("SO_REUSEPORT did not change");
            }
            List<? extends SocketOption> extOptions = List.of(TCP_KEEPCOUNT,
                    TCP_KEEPIDLE, TCP_KEEPINTERVAL);
            if (options.containsAll(extOptions)) {
                ch.setOption(TCP_KEEPIDLE, 1234);
                checkOption(ch, TCP_KEEPIDLE, 1234);
                ch.setOption(TCP_KEEPINTERVAL, 123);
                checkOption(ch, TCP_KEEPINTERVAL, 123);
                ch.setOption(TCP_KEEPCOUNT, 7);
                checkOption(ch, TCP_KEEPCOUNT, 7);
            }
        }
    }

    static void checkOption(AsynchronousSocketChannel sc, SocketOption name, Object expectedValue)
            throws IOException {
        Object value = sc.getOption(name);
        if (!value.equals(expectedValue)) {
            throw new RuntimeException("value not as expected");
        }
    }
    static void testConnect() throws Exception {
        System.out.println("-- connect --");

        SocketAddress address;

        try (Server server = new Server()) {
            address = server.address();

            // connect to server and check local/remote addresses
            try (AsynchronousSocketChannel ch = AsynchronousSocketChannel.open()) {
                ch.connect(address).get();
                // check local address
                if (ch.getLocalAddress() == null)
                    throw new RuntimeException("Not bound to local address");

                // check remote address
                InetSocketAddress remote = (InetSocketAddress)ch.getRemoteAddress();
                if (remote.getPort() != server.address().getPort())
                    throw new RuntimeException("Connected to unexpected port");
                if (!remote.getAddress().equals(server.address().getAddress()))
                    throw new RuntimeException("Connected to unexpected address");

                // try to connect again
                try {
                    ch.connect(server.address()).get();
                    throw new RuntimeException("AlreadyConnectedException expected");
                } catch (AlreadyConnectedException x) {
                }

                // clean-up
                server.accept().close();
            }

            // check that connect fails with ClosedChannelException
            AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.close();
            try {
                ch.connect(server.address()).get();
                throw new RuntimeException("ExecutionException expected");
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof ClosedChannelException))
                    throw new RuntimeException("Cause of ClosedChannelException expected",
                            x.getCause());
            }
            final AtomicReference<Throwable> connectException = new AtomicReference<>();
            ch.connect(server.address(), (Void)null, new CompletionHandler<Void,Void>() {
                public void completed(Void result, Void att) {
                }
                public void failed(Throwable exc, Void att) {
                    connectException.set(exc);
                }
            });
            while (connectException.get() == null) {
                Thread.sleep(100);
            }
            if (!(connectException.get() instanceof ClosedChannelException))
                throw new RuntimeException("ClosedChannelException expected",
                        connectException.get());
        }

        // test that failure to connect closes the channel
        try (AsynchronousSocketChannel ch = AsynchronousSocketChannel.open()) {
            try {
                ch.connect(address).get();
            } catch (ExecutionException x) {
                // failed to establish connection
                if (ch.isOpen())
                    throw new RuntimeException("Channel should be closed");
            }
        }

        // repeat test by connecting to a (probably) non-existent host. This
        // improves the chance that the connect will not fail immediately.
        if (!skipSlowConnectTest) {
            try (AsynchronousSocketChannel ch = AsynchronousSocketChannel.open()) {
                try {
                    ch.connect(genSocketAddress()).get();
                } catch (ExecutionException x) {
                    // failed to establish connection
                    if (ch.isOpen())
                        throw new RuntimeException("Channel should be closed");
                }
            }
        }
    }

    static void testCloseWhenPending() throws Exception {
        System.out.println("-- asynchronous close when connecting --");

        AsynchronousSocketChannel ch;

        // asynchronous close while connecting
        ch = AsynchronousSocketChannel.open();
        Future<Void> connectResult = ch.connect(genSocketAddress());

        // give time to initiate the connect (SYN)
        Thread.sleep(50);

        // close
        ch.close();

        // check that exception is thrown in timely manner
        try {
            connectResult.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException x) {
            throw new RuntimeException("AsynchronousCloseException not thrown");
        } catch (ExecutionException x) {
            // expected
        }

        System.out.println("-- asynchronous close when reading --");

        try (Server server = new Server(1)) {
            ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();

            ByteBuffer dst = ByteBuffer.allocateDirect(100);
            Future<Integer> result = ch.read(dst);

            // attempt a second read - should fail with ReadPendingException
            ByteBuffer buf = ByteBuffer.allocateDirect(100);
            try {
                ch.read(buf);
                throw new RuntimeException("ReadPendingException expected");
            } catch (ReadPendingException x) {
            }

            // close channel (should cause initial read to complete)
            SocketChannel peer = server.accept();
            ch.close();
            peer.close();

            // check that AsynchronousCloseException is thrown
            try {
                result.get();
                throw new RuntimeException("Should not read");
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof AsynchronousCloseException))
                    throw new RuntimeException(x);
            }

            System.out.println("-- asynchronous close when writing --");

            ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();
            peer = server.accept();
            peer.setOption(SO_RCVBUF, 1);

            final AtomicReference<Throwable> writeException =
                new AtomicReference<Throwable>();

            // write bytes to fill socket buffer
            final AtomicInteger numCompleted = new AtomicInteger();
            ch.write(genBuffer(), ch, new CompletionHandler<Integer,AsynchronousSocketChannel>() {
                public void completed(Integer result, AsynchronousSocketChannel ch) {
                    System.out.println("completed write to async channel: " + result);
                    numCompleted.incrementAndGet();
                    ch.write(genBuffer(), ch, this);
                    System.out.println("started another write to async channel: " + result);
                }
                public void failed(Throwable x, AsynchronousSocketChannel ch) {
                    System.out.println("failed write to async channel");
                    writeException.set(x);
                }
            });

            // give time for socket buffer to fill up -
            // take pauses until the handler is no longer being invoked
            // because all writes are being pended which guarantees that
            // the internal channel state indicates it is writing
            int prevNumCompleted = numCompleted.get();
            do {
                Thread.sleep((long)(1000 * jdk.test.lib.Utils.TIMEOUT_FACTOR));
                System.out.println("check if buffer is filled up");
                if (numCompleted.get() == prevNumCompleted) {
                    break;
                }
                prevNumCompleted = numCompleted.get();
            } while (true);

            // attempt a concurrent write -
            // should fail with WritePendingException
            try {
                System.out.println("concurrent write to async channel");
                ch.write(genBuffer());
                System.out.format("prevNumCompleted: %d, numCompleted: %d%n",
                                  prevNumCompleted, numCompleted.get());
                throw new RuntimeException("WritePendingException expected");
            } catch (WritePendingException x) {
            }

            // close channel - should cause initial write to complete
            System.out.println("closing async channel...");
            ch.close();
            System.out.println("closed async channel");
            peer.close();

            // wait for exception
            while (writeException.get() == null) {
                Thread.sleep(100);
            }
            if (!(writeException.get() instanceof AsynchronousCloseException))
                throw new RuntimeException("AsynchronousCloseException expected",
                        writeException.get());
        }
    }

    static void testCancel() throws Exception {
        System.out.println("-- cancel --");

        try (Server server = new Server()) {
            for (int i=0; i<2; i++) {
                boolean mayInterruptIfRunning = (i == 0) ? false : true;

                // establish loopback connection
                AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
                ch.connect(server.address()).get();
                SocketChannel peer = server.accept();

                // start read operation
                ByteBuffer buf = ByteBuffer.allocate(1);
                Future<Integer> res = ch.read(buf);

                // cancel operation
                boolean cancelled = res.cancel(mayInterruptIfRunning);

                // check post-conditions
                if (!res.isDone())
                    throw new RuntimeException("isDone should return true");
                if (res.isCancelled() != cancelled)
                    throw new RuntimeException("isCancelled not consistent");
                try {
                    res.get();
                    throw new RuntimeException("CancellationException expected");
                } catch (CancellationException x) {
                }
                try {
                    res.get(1, TimeUnit.SECONDS);
                    throw new RuntimeException("CancellationException expected");
                } catch (CancellationException x) {
                }

                // check that the cancel doesn't impact writing to the channel
                if (!mayInterruptIfRunning) {
                    buf = ByteBuffer.wrap("a".getBytes());
                    ch.write(buf).get();
                }

                ch.close();
                peer.close();
            }
        }
    }

    static void testRead1() throws Exception {
        System.out.println("-- read (1) --");

        try (Server server = new Server()) {
            final AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();

            // read with 0 bytes remaining should complete immediately
            ByteBuffer buf = ByteBuffer.allocate(1);
            buf.put((byte)0);
            int n = ch.read(buf).get();
            if (n != 0)
                throw new RuntimeException("0 expected");

            // write bytes and close connection
            ByteBuffer src = genBuffer();
            try (SocketChannel sc = server.accept()) {
                sc.setOption(SO_SNDBUF, src.remaining());
                while (src.hasRemaining())
                    sc.write(src);
            }

            // reads should complete immediately
            final ByteBuffer dst = ByteBuffer.allocateDirect(src.capacity() + 100);
            final CountDownLatch latch = new CountDownLatch(1);
            ch.read(dst, (Void)null, new CompletionHandler<Integer,Void>() {
                public void completed(Integer result, Void att) {
                    int n = result;
                    if (n > 0) {
                        ch.read(dst, (Void)null, this);
                    } else {
                        latch.countDown();
                    }
                }
                public void failed(Throwable exc, Void att) {
                }
            });

            latch.await();

            // check buffers
            src.flip();
            dst.flip();
            if (!src.equals(dst)) {
                throw new RuntimeException("Contents differ");
            }

            // close channel
            ch.close();

            // check read fails with ClosedChannelException
            try {
                ch.read(dst).get();
                throw new RuntimeException("ExecutionException expected");
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof ClosedChannelException))
                    throw new RuntimeException("Cause of ClosedChannelException expected",
                            x.getCause());
            }
        }
    }

    static void testRead2() throws Exception {
        System.out.println("-- read (2) --");

        try (Server server = new Server()) {
            final AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();
            SocketChannel sc = server.accept();

            ByteBuffer src = genBuffer();

            // read until the buffer is full
            final ByteBuffer dst = ByteBuffer.allocateDirect(src.capacity());
            final CountDownLatch latch = new CountDownLatch(1);
            ch.read(dst, (Void)null, new CompletionHandler<Integer,Void>() {
                public void completed(Integer result, Void att) {
                    if (dst.hasRemaining()) {
                        ch.read(dst, (Void)null, this);
                    } else {
                        latch.countDown();
                    }
                }
                public void failed(Throwable exc, Void att) {
                }
            });

            // trickle the writing
            do {
                int rem = src.remaining();
                int size = (rem <= 100) ? rem : 50 + RAND.nextInt(rem - 100);
                ByteBuffer buf = ByteBuffer.allocate(size);
                for (int i=0; i<size; i++)
                    buf.put(src.get());
                buf.flip();
                Thread.sleep(50 + RAND.nextInt(1500));
                while (buf.hasRemaining())
                    sc.write(buf);
            } while (src.hasRemaining());

            // wait until ascynrhonous reading has completed
            latch.await();

            // check buffers
            src.flip();
            dst.flip();
            if (!src.equals(dst)) {
               throw new RuntimeException("Contents differ");
            }

            sc.close();
            ch.close();
        }
    }

    // exercise scattering read
    static void testRead3() throws Exception {
        System.out.println("-- read (3) --");

        try (Server server = new Server()) {
            final AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();
            SocketChannel sc = server.accept();

            ByteBuffer[] dsts = new ByteBuffer[3];
            for (int i=0; i<dsts.length; i++) {
                dsts[i] = ByteBuffer.allocateDirect(100);
            }

            // scattering read that completes ascynhronously
            final CountDownLatch l1 = new CountDownLatch(1);
            ch.read(dsts, 0, dsts.length, 0L, TimeUnit.SECONDS, (Void)null,
                new CompletionHandler<Long,Void>() {
                    public void completed(Long result, Void att) {
                        long n = result;
                        if (n <= 0)
                            throw new RuntimeException("No bytes read");
                        l1.countDown();
                    }
                    public void failed(Throwable exc, Void att) {
                    }
            });

            // write some bytes
            sc.write(genBuffer());

            // read should now complete
            l1.await();

            // write more bytes
            sc.write(genBuffer());

            // read should complete immediately
            for (int i=0; i<dsts.length; i++) {
                dsts[i].rewind();
            }

            final CountDownLatch l2 = new CountDownLatch(1);
            ch.read(dsts, 0, dsts.length, 0L, TimeUnit.SECONDS, (Void)null,
                new CompletionHandler<Long,Void>() {
                    public void completed(Long result, Void att) {
                        long n = result;
                        if (n <= 0)
                            throw new RuntimeException("No bytes read");
                        l2.countDown();
                    }
                    public void failed(Throwable exc, Void att) {
                    }
            });
            l2.await();

            ch.close();
            sc.close();
        }
    }

    static void testWrite1() throws Exception {
        System.out.println("-- write (1) --");

        try (Server server = new Server()) {
            final AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();
            SocketChannel sc = server.accept();

            // write with 0 bytes remaining should complete immediately
            ByteBuffer buf = ByteBuffer.allocate(1);
            buf.put((byte)0);
            int n = ch.write(buf).get();
            if (n != 0)
                throw new RuntimeException("0 expected");

            // write all bytes and close connection when done
            final ByteBuffer src = genBuffer();
            ch.write(src, (Void)null, new CompletionHandler<Integer,Void>() {
                public void completed(Integer result, Void att) {
                    if (src.hasRemaining()) {
                        ch.write(src, (Void)null, this);
                    } else {
                        try {
                            ch.close();
                        } catch (IOException ignore) { }
                    }
                }
                public void failed(Throwable exc, Void att) {
                }
            });

            // read to EOF or buffer full
            ByteBuffer dst = ByteBuffer.allocateDirect(src.capacity() + 100);
            do {
                n = sc.read(dst);
            } while (n > 0);
            sc.close();

            // check buffers
            src.flip();
            dst.flip();
            if (!src.equals(dst)) {
                throw new RuntimeException("Contents differ");
            }

            // check write fails with ClosedChannelException
            try {
                ch.read(dst).get();
                throw new RuntimeException("ExecutionException expected");
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof ClosedChannelException))
                    throw new RuntimeException("Cause of ClosedChannelException expected",
                            x.getCause());
            }
        }
    }

    // exercise gathering write
    static void testWrite2() throws Exception {
        System.out.println("-- write (2) --");

        try (Server server = new Server()) {
            final AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();
            SocketChannel sc = server.accept();

            // number of bytes written
            final AtomicLong bytesWritten = new AtomicLong(0);

            // write buffers (should complete immediately)
            ByteBuffer[] srcs = genBuffers(1);
            final CountDownLatch l1 = new CountDownLatch(1);
            ch.write(srcs, 0, srcs.length, 0L, TimeUnit.SECONDS, (Void)null,
                new CompletionHandler<Long,Void>() {
                    public void completed(Long result, Void att) {
                        long n = result;
                        if (n <= 0)
                            throw new RuntimeException("No bytes read");
                        bytesWritten.addAndGet(n);
                        l1.countDown();
                    }
                    public void failed(Throwable exc, Void att) {
                    }
            });
            l1.await();

            // set to true to signal that no more buffers should be written
            final AtomicBoolean continueWriting = new AtomicBoolean(true);

            // write until socket buffer is full so as to create the conditions
            // for when a write does not complete immediately
            srcs = genBuffers(1);
            ch.write(srcs, 0, srcs.length, 0L, TimeUnit.SECONDS, (Void)null,
                new CompletionHandler<Long,Void>() {
                    public void completed(Long result, Void att) {
                        long n = result;
                        if (n <= 0)
                            throw new RuntimeException("No bytes written");
                        bytesWritten.addAndGet(n);
                        if (continueWriting.get()) {
                            ByteBuffer[] srcs = genBuffers(8);
                            ch.write(srcs, 0, srcs.length, 0L, TimeUnit.SECONDS,
                                (Void)null, this);
                        }
                    }
                    public void failed(Throwable exc, Void att) {
                    }
            });

            // give time for socket buffer to fill up.
            Thread.sleep(5*1000);

            // signal handler to stop further writing
            continueWriting.set(false);

            // read until done
            ByteBuffer buf = ByteBuffer.allocateDirect(4096);
            long total = 0L;
            do {
                int n = sc.read(buf);
                if (n <= 0)
                    throw new RuntimeException("No bytes read");
                buf.rewind();
                total += n;
            } while (total < bytesWritten.get());

            ch.close();
            sc.close();
        }
    }

    static void testShutdown() throws Exception {
        System.out.println("-- shutdown --");

        try (Server server = new Server();
             AsynchronousSocketChannel ch = AsynchronousSocketChannel.open())
        {
            ch.connect(server.address()).get();
            try (SocketChannel peer = server.accept()) {
                ByteBuffer buf = ByteBuffer.allocateDirect(1000);
                int n;

                // check read
                ch.shutdownInput();
                n = ch.read(buf).get();
                if (n != -1)
                    throw new RuntimeException("-1 expected");
                // check full with full buffer
                buf.put(new byte[100]);
                n = ch.read(buf).get();
                if (n != -1)
                    throw new RuntimeException("-1 expected");

                // check write
                ch.shutdownOutput();
                try {
                    ch.write(buf).get();
                    throw new RuntimeException("ClosedChannelException expected");
                } catch (ExecutionException x) {
                    if (!(x.getCause() instanceof ClosedChannelException))
                        throw new RuntimeException("ClosedChannelException expected",
                                x.getCause());
                }
            }
        }
    }

    static void testTimeout() throws Exception {
        System.out.println("-- timeouts --");
        testTimeout(Integer.MIN_VALUE, TimeUnit.SECONDS);
        testTimeout(-1L, TimeUnit.SECONDS);
        testTimeout(0L, TimeUnit.SECONDS);
        testTimeout(2L, TimeUnit.SECONDS);
    }

    static void testTimeout(final long timeout, final TimeUnit unit) throws Exception {
        System.out.printf("---- timeout: %d ms%n", unit.toMillis(timeout));
        try (Server server = new Server()) {
            AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.connect(server.address()).get();

            ByteBuffer dst = ByteBuffer.allocate(512);

            final AtomicReference<Throwable> readException = new AtomicReference<Throwable>();

            // this read should timeout if value is > 0
            ch.read(dst, timeout, unit, null, new CompletionHandler<Integer,Void>() {
                public void completed(Integer result, Void att) {
                    readException.set(new RuntimeException("Should not complete"));
                }
                public void failed(Throwable exc, Void att) {
                    readException.set(exc);
                }
            });
            if (timeout > 0L) {
                // wait for exception
                while (readException.get() == null) {
                    Thread.sleep(100);
                }
                if (!(readException.get() instanceof InterruptedByTimeoutException))
                    throw new RuntimeException("InterruptedByTimeoutException expected",
                            readException.get());

                // after a timeout then further reading should throw unspecified runtime exception
                boolean exceptionThrown = false;
                try {
                    ch.read(dst);
                } catch (RuntimeException x) {
                    exceptionThrown = true;
                }
                if (!exceptionThrown)
                    throw new RuntimeException("RuntimeException expected after timeout.");
            } else {
                Thread.sleep(1000);
                Throwable exc = readException.get();
                if (exc != null)
                    throw new RuntimeException(exc);
            }

            final AtomicReference<Throwable> writeException = new AtomicReference<Throwable>();

            // write bytes to fill socket buffer
            ch.write(genBuffer(), timeout, unit, ch,
                new CompletionHandler<Integer,AsynchronousSocketChannel>()
            {
                public void completed(Integer result, AsynchronousSocketChannel ch) {
                    ch.write(genBuffer(), timeout, unit, ch, this);
                }
                public void failed(Throwable exc, AsynchronousSocketChannel ch) {
                    writeException.set(exc);
                }
            });
            if (timeout > 0) {
                // wait for exception
                while (writeException.get() == null) {
                    Thread.sleep(100);
                }
                if (!(writeException.get() instanceof InterruptedByTimeoutException))
                    throw new RuntimeException("InterruptedByTimeoutException expected",
                            writeException.get());

                // after a timeout then further writing should throw unspecified runtime exception
                boolean exceptionThrown = false;
                try {
                    ch.write(genBuffer());
                } catch (RuntimeException x) {
                    exceptionThrown = true;
                }
                if (!exceptionThrown)
                    throw new RuntimeException("RuntimeException expected after timeout.");
            } else {
                Thread.sleep(1000);
                Throwable exc = writeException.get();
                if (exc != null)
                    throw new RuntimeException(exc);
            }

            // clean-up
            server.accept().close();
            ch.close();
        }
    }

    // returns ByteBuffer with random bytes
    static ByteBuffer genBuffer() {
        int size = 1024 + RAND.nextInt(16000);
        byte[] buf = new byte[size];
        RAND.nextBytes(buf);
        boolean useDirect = RAND.nextBoolean();
        if (useDirect) {
            ByteBuffer bb = ByteBuffer.allocateDirect(buf.length);
            bb.put(buf);
            bb.flip();
            return bb;
        } else {
            return ByteBuffer.wrap(buf);
        }
    }

    // return ByteBuffer[] with random bytes
    static ByteBuffer[] genBuffers(int max) {
        int len = 1;
        if (max > 1)
            len += RAND.nextInt(max);
        ByteBuffer[] bufs = new ByteBuffer[len];
        for (int i=0; i<len; i++)
            bufs[i] = genBuffer();
        return bufs;
    }

    // return random SocketAddress
    static SocketAddress genSocketAddress() {
        StringBuilder sb = new StringBuilder("10.");
        sb.append(RAND.nextInt(256));
        sb.append('.');
        sb.append(RAND.nextInt(256));
        sb.append('.');
        sb.append(RAND.nextInt(256));
        InetAddress rh;
        try {
            rh = InetAddress.getByName(sb.toString());
        } catch (UnknownHostException x) {
            throw new InternalError("Should not happen");
        }
        return new InetSocketAddress(rh, RAND.nextInt(65535)+1);
    }
}
