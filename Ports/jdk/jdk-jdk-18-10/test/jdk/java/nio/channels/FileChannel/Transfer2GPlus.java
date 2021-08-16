/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8271308
 * @summary Verify that transferTo() copies more than Integer.MAX_VALUE bytes
 * @library .. /test/lib
 * @build jdk.test.lib.Platform
 * @run main Transfer2GPlus
 */

import java.io.File;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import jdk.test.lib.Platform;

public class Transfer2GPlus {
    private static final long BASE   = (long)Integer.MAX_VALUE;
    private static final int  EXTRA  = 1024;
    private static final long LENGTH = BASE + EXTRA;

    public static void main(String[] args) throws IOException {
        Path src = Files.createTempFile("src", ".dat");
        src.toFile().deleteOnExit();
        byte[] b = createSrcFile(src);
        testToFileChannel(src, b);
        testToWritableByteChannel(src, b);
    }

    // Create a file of size LENGTH with EXTRA random bytes at offset BASE.
    private static byte[] createSrcFile(Path src)
        throws IOException {
        RandomAccessFile raf = new RandomAccessFile(src.toString(), "rw");
        raf.setLength(LENGTH);
        raf.seek(BASE);
        Random r = new Random(System.nanoTime());
        byte[] b = new byte[EXTRA];
        r.nextBytes(b);
        raf.write(b);
        return b;
    }

    // Exercises transferToDirectly() on Linux and transferToTrustedChannel()
    // on macOS and Windows.
    private static void testToFileChannel(Path src, byte[] expected)
        throws IOException {
        Path dst = Files.createTempFile("dst", ".dat");
        dst.toFile().deleteOnExit();
        try (FileChannel srcCh = FileChannel.open(src)) {
            try (FileChannel dstCh = FileChannel.open(dst,
                 StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                long total = 0L;
                if ((total = srcCh.transferTo(0, LENGTH, dstCh)) < LENGTH) {
                    if (!Platform.isLinux())
                        throw new RuntimeException("Transfer too small: " + total);

                    // If this point is reached we're on Linux which cannot
                    // transfer all LENGTH bytes in one call to sendfile(2),
                    // so loop to get the rest.
                    do {
                        long n = srcCh.transferTo(total, LENGTH, dstCh);
                        if (n == 0)
                            break;
                        total += n;
                    } while (total < LENGTH);
                }

                if (dstCh.size() < LENGTH)
                    throw new RuntimeException("Target file too small: " +
                        dstCh.size() + " < " + LENGTH);

                System.out.println("Transferred " + total + " bytes");

                dstCh.position(BASE);
                ByteBuffer bb = ByteBuffer.allocate(EXTRA);
                dstCh.read(bb);
                if (!Arrays.equals(bb.array(), expected))
                    throw new RuntimeException("Unexpected values");
            }
        }
    }

    // Exercises transferToArbitraryChannel() on all platforms.
    private static void testToWritableByteChannel(Path src, byte[] expected)
        throws IOException {
        File file = File.createTempFile("dst", ".dat");
        file.deleteOnExit();
        try (FileChannel srcCh = FileChannel.open(src)) {
            // The FileOutputStream is wrapped so that newChannel() does not
            // return a FileChannelImpl and so make a faster path be taken.
            try (DataOutputStream stream =
                new DataOutputStream(new FileOutputStream(file))) {
                try (WritableByteChannel wbc = Channels.newChannel(stream)) {
                    long n;
                    if ((n = srcCh.transferTo(0, LENGTH, wbc)) < LENGTH)
                        throw new RuntimeException("Too few bytes transferred: " +
                            n + " < " + LENGTH);

                    System.out.println("Transferred " + n + " bytes");

                    RandomAccessFile raf = new RandomAccessFile(file, "r");
                    raf.seek(BASE);
                    byte[] b = new byte[EXTRA];
                    raf.read(b);
                    if (!Arrays.equals(b, expected))
                        throw new RuntimeException("Unexpected values");
                }
            }
        }
    }
}
