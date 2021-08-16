/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6913877
 * @summary Stress AsynchronousFileChannel.write
 * @key randomness
 */

import java.io.*;
import java.nio.ByteBuffer;
import static java.nio.file.StandardOpenOption.*;
import java.nio.channels.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class LotsOfWrites {
    static final Random rand = new Random();

    /**
     * Asynchronously writes a known pattern to a file up to a given size,
     * counting down a latch to release waiters when done.
     */
    static class Writer implements CompletionHandler<Integer,ByteBuffer> {
        private final File file;
        private final long size;
        private final CountDownLatch latch;
        private final AsynchronousFileChannel channel;

        private volatile long position;
        private volatile byte nextByte;

        private long updatePosition(long nwrote) {
            position += nwrote;
            return position;
        }

        private ByteBuffer genNextBuffer() {
            int n = Math.min(8192 + rand.nextInt(8192), (int)(size - position));
            ByteBuffer buf = ByteBuffer.allocate(n);
            for (int i=0; i<n; i++) {
                buf.put(nextByte++);
            }
            buf.flip();
            return buf;
        }

        // close channel and release any waiters
        private void done() {
            try {
                channel.close();
            } catch (IOException ignore) { }
            latch.countDown();
        }

        Writer(File file, long size, CountDownLatch latch) throws IOException {
            this.file = file;
            this.size = size;
            this.latch = latch;
            this.channel = AsynchronousFileChannel.open(file.toPath(), WRITE);
        }

        File file() {
            return file;
        }

        long size() {
            return size;
        }

        // initiate first write
        void start() {
            ByteBuffer buf = genNextBuffer();
            channel.write(buf, 0L, buf, this);
        }

        @Override
        public void completed(Integer nwrote, ByteBuffer buf) {
            long pos = updatePosition(nwrote);
            if (!buf.hasRemaining()) {
                // buffer has been completely written; decide if we need to
                // write more
                if (position >= size) {
                    done();
                    return;
                }
                buf = genNextBuffer();
            }
            channel.write(buf, pos, buf, this);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer buf) {
            exc.printStackTrace();
            done();
        }
    }

    public static void main(String[] args) throws Exception {
        // random number of writers
        int count = 20 + rand.nextInt(16);
        Writer[] writers = new Writer[count];
        CountDownLatch latch = new CountDownLatch(count);

        // initiate writing to each file
        for (int i=0; i<count; i++) {
            long size = 512*1024 + rand.nextInt(512*1024);
            File blah = File.createTempFile("blah", null);
            blah.deleteOnExit();
            Writer writer = new Writer(blah, size, latch);
            writers[i] = writer;
            writer.start();
        }

        // wait for writing to complete
        latch.await();

        // verify content of each file
        boolean failed = false;
        byte[] buf = new byte[8192];
        for (int i=0; i<count ;i++) {
            Writer writer = writers[i];
            FileInputStream in = new FileInputStream(writer.file());
            try {
                long size = 0L;
                byte expected = 0;
                int nread = in.read(buf);
                while (nread > 0) {
                    for (int j=0; j<nread; j++) {
                        if (buf[j] != expected) {
                            System.err.println("Unexpected contents");
                            failed = true;
                            break;
                        }
                        expected++;
                    }
                    if (failed)
                        break;
                    size += nread;
                    nread = in.read(buf);
                }
                if (!failed && size != writer.size()) {
                    System.err.println("Unexpected size");
                    failed = true;
                }
                if (failed)
                    break;
            } finally {
                in.close();
            }
        }

        // clean-up
        for (int i=0; i<count; i++) {
            writers[i].file().delete();
        }

        if (failed)
            throw new RuntimeException("Test failed");
    }
}
