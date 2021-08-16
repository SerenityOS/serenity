/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8012019
 * @summary Tests interruption of threads doing position-based read methods in
 *   an attempt to provoke a deadlock between position sensitive and position
 *   insensitive methods
 */
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;

public class InterruptDeadlock {

    /**
     * A thread that continuously reads from a FileChannel with
     * read(ByteBuffer,long). The thread terminates when interrupted and/or
     * the FileChannel is closed.
     */
    static class Reader extends Thread {
        final FileChannel fc;
        volatile Exception exception;

        Reader(FileChannel fc) {
            this.fc = fc;
        }

        @Override
        public void run() {
            ByteBuffer bb = ByteBuffer.allocate(1024);
            try {
                long pos = 0L;
                for (;;) {
                    bb.clear();
                    int n = fc.read(bb, pos);
                    if (n > 0)
                        pos += n;
                    // fc.size is important here as it is position sensitive
                    if (pos >= fc.size())
                        pos = 0L;
                }
            } catch (ClosedChannelException x) {
                System.out.println(x.getClass() + " (expected)");
            } catch (Exception unexpected) {
                this.exception = unexpected;
            }
        }

        Exception exception() {
            return exception;
        }

        static Reader startReader(FileChannel fc) {
            Reader r = new Reader(fc);
            r.start();
            return r;
        }
    }

    // the number of reader threads to start
    private static final int READER_COUNT = 4;

    public static void main(String[] args) throws Exception {
        Path file = Paths.get("data.txt");
        try (FileChannel fc = FileChannel.open(file, CREATE, TRUNCATE_EXISTING, WRITE)) {
            fc.position(1024L * 1024L);
            fc.write(ByteBuffer.wrap(new byte[1]));
        }

        Reader[] readers = new Reader[READER_COUNT];

        for (int i=1; i<=20; i++) {
            System.out.format("Iteration: %s%n", i);

            try (FileChannel fc = FileChannel.open(file)) {
                boolean failed = false;

                // start reader threads
                for (int j=0; j<READER_COUNT; j++) {
                    readers[j] = Reader.startReader(fc);
                }

                // give readers a bit of time to get started (not strictly required)
                Thread.sleep(100);

                // interrupt and wait for the readers to terminate
                for (Reader r: readers) {
                    r.interrupt();
                }
                for (Reader r: readers) {
                    try {
                        r.join(10000);
                        Exception e = r.exception();
                        if (e != null) {
                            System.err.println("Reader thread failed with: " + e);
                            failed = true;
                        }
                    } catch (InterruptedException x) {
                        System.err.println("Reader thread did not terminte");
                        failed = true;
                    }
                }

                // the channel should not be open at this point
                if (fc.isOpen()) {
                    System.err.println("FileChannel was not closed");
                    failed = true;
                }

                if (failed)
                    throw new RuntimeException("Test failed - see log for details");
            }
        }
    }
}
