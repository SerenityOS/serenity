/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Check that appends are atomic
 * @key randomness
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.*;

public class AtomicAppend {
    static final Random rand = new Random();

    // Open file for appending, returning FileChannel
    static FileChannel newFileChannel(File file) throws IOException {
        if (rand.nextBoolean()) {
            return new FileOutputStream(file, true).getChannel();
        } else {
            return FileChannel.open(file.toPath(), APPEND);
        }
    }

    // Open file for append, returning OutputStream
    static OutputStream newOutputStream(File file) throws IOException {
        if (rand.nextBoolean()) {
            return new FileOutputStream(file, true);
        } else {
            return Files.newOutputStream(file.toPath(), APPEND);
        }
    }

    // write a byte to the given channel
    static void write(FileChannel fc, int b) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put((byte)b);
        buf.flip();
        if (rand.nextBoolean()) {
            ByteBuffer[] bufs = new ByteBuffer[1];
            bufs[0] = buf;
            fc.write(bufs);
        } else {
            fc.write(buf);
        }
    }

    public static void main(String[] args) throws Throwable {
        final int nThreads = 16;
        final int writes = 1000;
        final File file = File.createTempFile("foo", null);
        try {
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < nThreads; i++)
                pool.execute(new Runnable() { public void run() {
                    try {
                        // randomly choose FileChannel or OutputStream
                        if (rand.nextBoolean()) {
                            try (FileChannel fc = newFileChannel(file)) {
                                for (int j=0; j<writes; j++) write(fc, 'x');
                            }
                        } else {
                            try (OutputStream out = newOutputStream(file)) {
                                for (int j = 0; j<writes; j++) out.write('x');
                            }
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }});
            pool.shutdown();
            pool.awaitTermination(1L, TimeUnit.MINUTES);
            if (file.length() != (long) (nThreads * writes))
                throw new RuntimeException("File not expected length");
        } finally {
            file.delete();
        }
    }
}
