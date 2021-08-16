/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6979009
 * @summary Ensure ClosedByInterruptException is thrown when I/O operation
 *     interrupted by Thread.interrupt
 * @key randomness
 */

import java.io.*;
import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class ClosedByInterrupt {

    static final int K = 1024;
    static final Random rand = new Random();

    static volatile boolean failed;

    public static void main(String[] args) throws Exception {
        File f = File.createTempFile("blah", null);
        f.deleteOnExit();

        // create 1MB file.
        byte[] b = new byte[K*K];
        rand.nextBytes(b);
        ByteBuffer bb = ByteBuffer.wrap(b);
        try (FileChannel fc = new FileOutputStream(f).getChannel()) {
            while (bb.hasRemaining())
                fc.write(bb);
        }

        // test with 1-16 concurrent threads
        for (int i=1; i<=16; i++) {
            System.out.format("%d thread(s)%n", i);
            test(f, i);
            if (failed)
                break;
        }

        if (failed)
            throw new RuntimeException("Test failed");
    }

    /**
     * Starts "nThreads" that do I/O on the given file concurrently. Continuously
     * interrupts one of the threads to cause the file to be closed and
     * ClosedByInterruptException to be thrown. The other threads should "fail" with
     * ClosedChannelException (or the more specific AsynchronousCloseException).
     */
    static void test(File f, int nThreads) throws Exception {
        try (FileChannel fc = new RandomAccessFile(f, "rwd").getChannel()) {
            Thread[] threads = new Thread[nThreads];

            // start threads
            for (int i=0; i<nThreads; i++) {
                boolean interruptible = (i==0);
                ReaderWriter task = new ReaderWriter(fc, interruptible);
                Thread t = new Thread(task);
                t.start();
                threads[i] = t;
            }

            // give time for threads to start
            Thread.sleep(500 + rand.nextInt(1000));

            // interrupt thread until channel is closed
            while (fc.isOpen()) {
                threads[0].interrupt();
                Thread.sleep(rand.nextInt(50));
            }

            // wait for test to finish
            for (int i=0; i<nThreads; i++) {
                threads[i].join();
            }
        }
    }

    /**
     * A task that continuously reads or writes to random areas of a file
     * until the channel is closed. An "interruptible" task expects the
     * channel to be closed by an interupt, a "non-interruptible" thread
     * does not.
     */
    static class ReaderWriter implements Runnable {
        final FileChannel fc;
        final boolean interruptible;
        final boolean writer;

        ReaderWriter(FileChannel fc, boolean interruptible) {
            this.fc = fc;
            this.interruptible = interruptible;
            this.writer = rand.nextBoolean();
        }

        public void run() {
            ByteBuffer bb = ByteBuffer.allocate(K);
            if (writer)
                rand.nextBytes(bb.array());

            try {
                for (;;) {
                    long position = rand.nextInt(K*K - bb.capacity());
                    if (writer) {
                        bb.position(0).limit(bb.capacity());
                        fc.write(bb, position);
                    } else {
                        bb.clear();
                        fc.read(bb, position);
                    }
                    if (!interruptible) {
                        // give the interruptible thread a chance
                        try {
                            Thread.sleep(rand.nextInt(50));
                        } catch (InterruptedException e) {
                            unexpected(e);
                        }
                    }
                }
            } catch (ClosedByInterruptException e) {
                if (interruptible) {
                    if (Thread.interrupted()) {
                        expected(e + " thrown and interrupt status set");
                    } else {
                        unexpected(e + " thrown but interrupt status not set");
                    }
                } else {
                    unexpected(e);
                }
            } catch (ClosedChannelException e) {
                if (interruptible) {
                    unexpected(e);
                } else {
                    expected(e);
                }
            } catch (Exception e) {
                unexpected(e);
            }
        }
    }

    static void expected(Exception e) {
        System.out.format("%s (expected)%n", e);
    }

    static void expected(String msg) {
        System.out.format("%s (expected)%n", msg);
    }

    static void unexpected(Exception e) {
        System.err.format("%s (not expected)%n", e);
        failed = true;
    }

    static void unexpected(String msg) {
        System.err.println(msg);
        failed = true;
    }
}
