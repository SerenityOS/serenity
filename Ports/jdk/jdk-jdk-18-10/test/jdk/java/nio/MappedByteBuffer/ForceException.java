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

/* @test
 * @bug 6539707
 * @summary Test behavior of force() with respect to throwing exceptions
 * @run main ForceException
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ForceException {
    public static void main(String[] args) throws IOException {
        int blockSize = 2048 * 1024;
        int numberOfBlocks = 200;
        int fileLength = numberOfBlocks * blockSize;

        File file = new File(System.getProperty("test.src", "."), "test.dat");
        file.deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(fileLength);

            int pos = (numberOfBlocks - 1) * blockSize;
            int size = (int)Math.min(blockSize, fileLength - pos);
            MappedByteBuffer mbb =
                raf.getChannel().map(FileChannel.MapMode.READ_WRITE, pos, size);

            System.out.printf("Write region 0x%s..0x%s%n",
                Long.toHexString(pos), Long.toHexString(size));
            for (int k = 0; k < mbb.limit(); k++) {
                mbb.put(k, (byte)65);
            }

            // Catch and process UncheckedIOException; other Throwables fail
            try {
                System.out.println("Force");
                mbb.force();
            } catch (UncheckedIOException legal) {
                System.out.printf("Caught legal exception %s%n", legal);
                IOException cause = legal.getCause(); // can't be null
                // Throw the cause if flush failed (should be only on Windows)
                if (cause.getMessage().startsWith("Flush failed")) {
                    throw cause;
                }
            }

            System.out.println("OK");
        }
    }
}
