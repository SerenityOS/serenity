/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4607272 6822643 6830721 6842687
 * @summary Unit test for AsynchronousFileChannel
 * @key randomness
 */

import java.nio.file.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static java.nio.file.StandardOpenOption.*;

public class Basic {

    private static final Random rand = new Random();

    public static void main(String[] args) throws IOException {
        // create temporary file
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();

        AsynchronousFileChannel ch = AsynchronousFileChannel
            .open(blah.toPath(), READ, WRITE);
        try {
            // run tests
            testUsingCompletionHandlers(ch);
            testUsingWaitOnResult(ch);
            testInterruptHandlerThread(ch);
        } finally {
            ch.close();
        }

        // run test that expects channel to be closed
        testClosedChannel(ch);

        // these tests open the file themselves
        testLocking(blah.toPath());
        testCustomThreadPool(blah.toPath());
        testAsynchronousClose(blah.toPath());
        testCancel(blah.toPath());
        testTruncate(blah.toPath());

        // eagerly clean-up
        blah.delete();
    }

    /*
     * Generate buffer with random contents
     * Writes buffer to file using a CompletionHandler to consume the result
     *    of each write operation
     * Reads file to EOF to a new buffer using a CompletionHandler to consume
     *    the result of each read operation
     * Compares buffer contents
     */
    static void testUsingCompletionHandlers(AsynchronousFileChannel ch)
        throws IOException
    {
        System.out.println("testUsingCompletionHandlers");

        ch.truncate(0L);

        // generate buffer with random elements and write it to file
        ByteBuffer src = genBuffer();
        writeFully(ch, src, 0L);

        // read to EOF or buffer is full
        ByteBuffer dst = (rand.nextBoolean()) ?
            ByteBuffer.allocateDirect(src.capacity()) :
            ByteBuffer.allocate(src.capacity());
        readAll(ch, dst, 0L);

        // check buffers are the same
        src.flip();
        dst.flip();
        if (!src.equals(dst)) {
            throw new RuntimeException("Contents differ");
        }
    }

    /*
     * Generate buffer with random contents
     * Writes buffer to file, invoking the Future's get method to wait for
     *    each write operation to complete
     * Reads file to EOF to a new buffer, invoking the Future's get method to
     *    wait for each write operation to complete
     * Compares buffer contents
     */
    static void testUsingWaitOnResult(AsynchronousFileChannel ch)
        throws IOException
    {
        System.out.println("testUsingWaitOnResult");

        ch.truncate(0L);

        // generate buffer
        ByteBuffer src = genBuffer();

        // write buffer completely to file
        long position = 0L;
        while (src.hasRemaining()) {
            Future<Integer> result = ch.write(src, position);
            try {
                int n = result.get();
                // update position
                position += n;
            } catch (ExecutionException x) {
                throw new RuntimeException(x.getCause());
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        }

        // read file into new buffer
        ByteBuffer dst = (rand.nextBoolean()) ?
            ByteBuffer.allocateDirect(src.capacity()) :
            ByteBuffer.allocate(src.capacity());
        position = 0L;
        int n;
        do {
            Future<Integer> result = ch.read(dst, position);
            try {
                n = result.get();

                // update position
                if (n > 0) position += n;
            } catch (ExecutionException x) {
                throw new RuntimeException(x.getCause());
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        } while (n > 0);

        // check buffers are the same
        src.flip();
        dst.flip();
        if (!src.equals(dst)) {
            throw new RuntimeException("Contents differ");
        }
    }

    // exercise lock methods
    static void testLocking(Path file) throws IOException {
        System.out.println("testLocking");

        AsynchronousFileChannel ch = AsynchronousFileChannel
            .open(file, READ, WRITE);
        FileLock fl;
        try {
            // test 1 - acquire lock and check that tryLock throws
            // OverlappingFileLockException
            try {
                fl = ch.lock().get();
            } catch (ExecutionException x) {
                throw new RuntimeException(x);
            } catch (InterruptedException x) {
                throw new RuntimeException("Should not be interrupted");
            }
            if (!fl.acquiredBy().equals(ch))
                throw new RuntimeException("FileLock#acquiredBy returned incorrect channel");
            try {
                ch.tryLock();
                throw new RuntimeException("OverlappingFileLockException expected");
            } catch (OverlappingFileLockException x) {
            }
            fl.release();

            // test 2 - acquire try and check that lock throws OverlappingFileLockException
            fl = ch.tryLock();
            if (fl == null)
                throw new RuntimeException("Unable to acquire lock");
            try {
                ch.lock((Void)null, new CompletionHandler<FileLock,Void> () {
                    public void completed(FileLock result, Void att) {
                    }
                    public void failed(Throwable exc, Void att) {
                    }
                });
                throw new RuntimeException("OverlappingFileLockException expected");
            } catch (OverlappingFileLockException x) {
            }
        } finally {
            ch.close();
        }

        // test 3 - channel is closed so FileLock should no longer be valid
        if (fl.isValid())
            throw new RuntimeException("FileLock expected to be invalid");
    }

    // interrupt should not close channel
    static void testInterruptHandlerThread(final AsynchronousFileChannel ch) {
        System.out.println("testInterruptHandlerThread");

        ByteBuffer buf = ByteBuffer.allocateDirect(100);
        final CountDownLatch latch = new CountDownLatch(1);

        ch.read(buf, 0L, (Void)null, new CompletionHandler<Integer,Void>() {
            public void completed(Integer result, Void att) {
                try {
                    Thread.currentThread().interrupt();
                    long size = ch.size();
                    latch.countDown();
                } catch (IOException x) {
                    x.printStackTrace();
                }
            }
            public void failed(Throwable exc, Void att) {
            }
        });

        // wait for handler to complete
        await(latch);
    }

    // invoke method on closed channel
    static void testClosedChannel(AsynchronousFileChannel ch) {
        System.out.println("testClosedChannel");

        if (ch.isOpen())
            throw new RuntimeException("Channel should be closed");

        ByteBuffer buf = ByteBuffer.allocateDirect(100);

        // check read fails with ClosedChannelException
        try {
            ch.read(buf, 0L).get();
            throw new RuntimeException("ExecutionException expected");
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ClosedChannelException))
                throw new RuntimeException("Cause of ClosedChannelException expected");
        } catch (InterruptedException x) {
        }

        // check write fails with ClosedChannelException
        try {
            ch.write(buf, 0L).get();
            throw new RuntimeException("ExecutionException expected");
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ClosedChannelException))
                throw new RuntimeException("Cause of ClosedChannelException expected");
        } catch (InterruptedException x) {
        }

        // check lock fails with ClosedChannelException
        try {
            ch.lock().get();
            throw new RuntimeException("ExecutionException expected");
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ClosedChannelException))
                throw new RuntimeException("Cause of ClosedChannelException expected");
        } catch (InterruptedException x) {
        }
    }


    // exercise custom thread pool
    static void testCustomThreadPool(Path file) throws IOException {
        System.out.println("testCustomThreadPool");

        // records threads that are created
        final List<Thread> threads = new ArrayList<Thread>();

        ThreadFactory threadFactory = new ThreadFactory() {
             @Override
             public Thread newThread(Runnable r) {
                 Thread t = new Thread(r);
                 t.setDaemon(true);
                 synchronized (threads) {
                     threads.add(t);
                 }
                 return t;
             }
        };

        // exercise tests with varied number of threads
        for (int nThreads=1; nThreads<=5; nThreads++) {
            synchronized (threads) {
                threads.clear();
            }
            ExecutorService executor = Executors.newFixedThreadPool(nThreads, threadFactory);
            Set<StandardOpenOption> opts = EnumSet.of(WRITE);
            AsynchronousFileChannel ch = AsynchronousFileChannel.open(file, opts, executor);
            try {
                for (int i=0; i<10; i++) {
                    // do I/O operation to see which thread invokes the completion handler
                    final AtomicReference<Thread> invoker = new AtomicReference<Thread>();
                    final CountDownLatch latch = new CountDownLatch(1);

                    ch.write(genBuffer(), 0L, (Void)null, new CompletionHandler<Integer,Void>() {
                        public void completed(Integer result, Void att) {
                            invoker.set(Thread.currentThread());
                            latch.countDown();
                        }
                        public void failed(Throwable exc, Void att) {
                        }
                    });
                    await(latch);

                    // check invoker
                    boolean found = false;
                    synchronized (threads) {
                        for (Thread t: threads) {
                            if (t == invoker.get()) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found)
                        throw new RuntimeException("Invoker thread not found");
                }
            } finally {
                ch.close();
                executor.shutdown();
            }
        }


        // test sharing a thread pool between many channels
        ExecutorService executor = Executors
            .newFixedThreadPool(1+rand.nextInt(10), threadFactory);
        final int n = 50 + rand.nextInt(50);
        AsynchronousFileChannel[] channels = new AsynchronousFileChannel[n];
        try {
            for (int i=0; i<n; i++) {
                Set<StandardOpenOption> opts = EnumSet.of(WRITE);
                channels[i] = AsynchronousFileChannel.open(file, opts, executor);
                final CountDownLatch latch = new CountDownLatch(1);
                channels[i].write(genBuffer(), 0L, (Void)null, new CompletionHandler<Integer,Void>() {
                    public void completed(Integer result, Void att) {
                        latch.countDown();
                    }
                    public void failed(Throwable exc, Void att) {
                    }
                });
                await(latch);

                // close ~half the channels
                if (rand.nextBoolean())
                    channels[i].close();
            }
        } finally {
            // close remaining channels
            for (int i=0; i<n; i++) {
                if (channels[i] != null) channels[i].close();
            }
            executor.shutdown();
        }
    }

    // exercise asynchronous close
    static void testAsynchronousClose(Path file) throws IOException {
        System.out.println("testAsynchronousClose");

        // create file
        AsynchronousFileChannel ch = AsynchronousFileChannel
            .open(file, WRITE, TRUNCATE_EXISTING);
        long size = 0L;
        do {
            ByteBuffer buf = genBuffer();
            int n = buf.remaining();
            writeFully(ch, buf, size);
            size += n;
        } while (size < (50L * 1024L * 1024L));

        ch.close();

        ch = AsynchronousFileChannel.open(file, WRITE, SYNC);

        // randomize number of writers, buffer size, and positions

        int nwriters = 1 + rand.nextInt(8);
        ByteBuffer[] buf = new ByteBuffer[nwriters];
        long[] position = new long[nwriters];
        for (int i=0; i<nwriters; i++) {
            buf[i] = genBuffer();
            position[i] = rand.nextInt((int)size);
        }

        // initiate I/O
        Future[] result = new Future[nwriters];
        for (int i=0; i<nwriters; i++) {
            result[i] = ch.write(buf[i], position[i]);
        }

        // close file
        ch.close();

        // write operations should complete or fail with AsynchronousCloseException
        for (int i=0; i<nwriters; i++) {
            try {
                result[i].get();
            } catch (ExecutionException x) {
                Throwable cause = x.getCause();
                if (!(cause instanceof AsynchronousCloseException))
                    throw new RuntimeException(cause);
            } catch (CancellationException  x) {
                throw new RuntimeException(x);   // should not happen
            } catch (InterruptedException x) {
                throw new RuntimeException(x);   // should not happen
            }
        }
    }

    // exercise cancel method
    static void testCancel(Path file) throws IOException {
        System.out.println("testCancel");

        for (int i=0; i<2; i++) {
            boolean mayInterruptIfRunning = (i == 0) ? false : true;

            // open with SYNC option to improve chances that write will not
            // complete immediately
            AsynchronousFileChannel ch = AsynchronousFileChannel
                .open(file, WRITE, SYNC);

            // start write operation
            Future<Integer> res = ch.write(genBuffer(), 0L);

            // cancel operation
            boolean cancelled = res.cancel(mayInterruptIfRunning);

            // check post-conditions
            if (!res.isDone())
                throw new RuntimeException("isDone should return true");
            if (res.isCancelled() != cancelled)
                throw new RuntimeException("isCancelled not consistent");
            try {
                res.get();
                if (cancelled)
                    throw new RuntimeException("CancellationException expected");
            } catch (CancellationException x) {
                if (!cancelled)
                    throw new RuntimeException("CancellationException not expected");
            } catch (ExecutionException x) {
                throw new RuntimeException(x);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
            try {
                res.get(1, TimeUnit.SECONDS);
                if (cancelled)
                    throw new RuntimeException("CancellationException expected");
            } catch (CancellationException x) {
                if (!cancelled)
                    throw new RuntimeException("CancellationException not expected");
            } catch (ExecutionException x) {
                throw new RuntimeException(x);
            } catch (TimeoutException x) {
                throw new RuntimeException(x);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }

            ch.close();
        }
    }

    // exercise truncate method
    static void testTruncate(Path file) throws IOException {
        System.out.println("testTruncate");

        // basic tests
        AsynchronousFileChannel ch = AsynchronousFileChannel
            .open(file, CREATE, WRITE, TRUNCATE_EXISTING);
        try {
            writeFully(ch, genBuffer(), 0L);
            long size = ch.size();

            // attempt to truncate to a size greater than the current size
            if (ch.truncate(size + 1L).size() != size)
                throw new RuntimeException("Unexpected size after truncation");

            // truncate file
            if (ch.truncate(size - 1L).size() != (size - 1L))
                throw new RuntimeException("Unexpected size after truncation");

            // invalid size
            try {
                ch.truncate(-1L);
                throw new RuntimeException("IllegalArgumentException expected");
            } catch (IllegalArgumentException e) { }

        } finally {
            ch.close();
        }

        // channel is closed
        try {
            ch.truncate(0L);
            throw new RuntimeException("ClosedChannelException expected");
        } catch (ClosedChannelException  e) { }

        // channel is read-only
        ch = AsynchronousFileChannel.open(file, READ);
        try {
            try {
            ch.truncate(0L);
                throw new RuntimeException("NonWritableChannelException expected");
            } catch (NonWritableChannelException  e) { }
        } finally {
            ch.close();
        }
    }

    // returns ByteBuffer with random bytes
    static ByteBuffer genBuffer() {
        int size = 1024 + rand.nextInt(16000);
        byte[] buf = new byte[size];
        boolean useDirect = rand.nextBoolean();
        if (useDirect) {
            ByteBuffer bb = ByteBuffer.allocateDirect(buf.length);
            bb.put(buf);
            bb.flip();
            return bb;
        } else {
            return ByteBuffer.wrap(buf);
        }
    }

    // writes all remaining bytes in the buffer to the given channel at the
    // given position
    static void writeFully(final AsynchronousFileChannel ch,
                           final ByteBuffer src,
                           long position)
    {
        final CountDownLatch latch = new CountDownLatch(1);

        // use position as attachment
        ch.write(src, position, position, new CompletionHandler<Integer,Long>() {
            public void completed(Integer result, Long position) {
                int n = result;
                if (src.hasRemaining()) {
                    long p = position + n;
                    ch.write(src, p, p, this);
                } else {
                    latch.countDown();
                }
            }
            public void failed(Throwable exc, Long position) {
            }
        });

        // wait for writes to complete
        await(latch);
    }

    static void readAll(final AsynchronousFileChannel ch,
                        final ByteBuffer dst,
                       long position)
    {
        final CountDownLatch latch = new CountDownLatch(1);

        // use position as attachment
        ch.read(dst, position, position, new CompletionHandler<Integer,Long>() {
            public void completed(Integer result, Long position) {
                int n = result;
                if (n > 0) {
                    long p = position + n;
                    ch.read(dst, p, p, this);
                } else {
                    latch.countDown();
                }
            }
            public void failed(Throwable exc, Long position) {
            }
        });

        // wait for reads to complete
        await(latch);
    }

    static void await(CountDownLatch latch) {
        // wait until done
        boolean done = false;
        while (!done) {
            try {
                latch.await();
                done = true;
            } catch (InterruptedException x) { }
        }
    }
}
