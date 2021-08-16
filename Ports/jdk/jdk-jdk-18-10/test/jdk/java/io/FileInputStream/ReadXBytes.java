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
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main ReadXBytes
 * @bug 8264777
 * @summary Test read{All,N}Bytes overrides (use -Dseed=X to set PRNG seed)
 * @key randomness
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;
import jdk.test.lib.RandomFactory;

public class ReadXBytes {
    private static final int ITERATIONS = 10;
    private static final int MAX_FILE_SIZE = 1_000_000;
    private static final Random RND = RandomFactory.getRandom();

    public static void main(String args[]) throws IOException {
        File dir = new File(System.getProperty("test.src", "."));
        dir.deleteOnExit();

        File empty = File.createTempFile("foo", "bar", dir);
        empty.deleteOnExit();
        try (FileInputStream fis = new FileInputStream(empty)) {
            try {
                fis.readNBytes(-1);
                throw new RuntimeException("IllegalArgumentException expected");
            } catch (IllegalArgumentException expected) {
            }
            byte[] nbytes = fis.readNBytes(0);
            if (nbytes.length != 0)
                throw new RuntimeException("readNBytes() zero length for empty");

            byte[] b = fis.readNBytes(1);
            if (b.length != 0)
                throw new RuntimeException("readNBytes: zero-length byte[] expected");

            b = fis.readAllBytes();
            if (b.length != 0)
                throw new RuntimeException("readAllBytes: zero-length byte[] expected");
        }

        for (int i = 0; i < ITERATIONS; i++) {
            File file = File.createTempFile("foo", "bar", dir);
            file.deleteOnExit();

            int size = 1 + RND.nextInt(MAX_FILE_SIZE);
            System.out.printf("size %d%n", size);
            byte[] bytes = new byte[size];
            RND.nextBytes(bytes);
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                raf.write(bytes);
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                int pos = RND.nextInt(size);
                int len = RND.nextInt(size - pos);
                fis.getChannel().position(pos);
                byte[] nbytes = fis.readNBytes(0);
                if (nbytes.length != 0)
                    throw new RuntimeException("readNBytes() zero length");
                nbytes = fis.readNBytes(len);
                if (nbytes.length != len)
                    throw new RuntimeException("readNBytes() length");
                if (!Arrays.equals(nbytes, 0, len, bytes, pos, pos + len))
                    throw new RuntimeException("readNBytes() content");
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                int pos = RND.nextInt(size);
                fis.getChannel().position(pos);
                byte[] allbytes = fis.readAllBytes();
                if (allbytes.length != size - pos)
                    throw new RuntimeException("readAllBytes() length");
                if (!Arrays.equals(allbytes, 0, allbytes.length,
                                   bytes, pos, pos + allbytes.length))
                    throw new RuntimeException("readAllBytes() content");
            }

            file.delete();
        }
        dir.delete();
    }
}
