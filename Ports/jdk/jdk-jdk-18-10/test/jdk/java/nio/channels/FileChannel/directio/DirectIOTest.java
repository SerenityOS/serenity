/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164900
 * @summary Test for ExtendedOpenOption.DIRECT flag
 * @requires (os.family == "linux" | os.family == "aix")
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main/native DirectIOTest
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import jdk.test.lib.Platform;
import java.nio.file.FileStore;
import java.nio.file.StandardOpenOption;
import com.sun.nio.file.ExtendedOpenOption;

public class DirectIOTest {

    private static final int BASE_SIZE = 4096;

    private static int testWrite(Path p, long blockSize) throws Exception {
        try (FileChannel fc = FileChannel.open(p, StandardOpenOption.WRITE,
             ExtendedOpenOption.DIRECT)) {
            int bs = (int)blockSize;
            int size = Math.max(BASE_SIZE, bs);
            int alignment = bs;
            ByteBuffer src = ByteBuffer.allocateDirect(size + alignment - 1)
                                       .alignedSlice(alignment);
            assert src.capacity() != 0;
            for (int j = 0; j < size; j++) {
                src.put((byte)0);
            }
            src.flip();
            fc.write(src);
            return size;
        }
    }

    private static int testRead(Path p, long blockSize) throws Exception {
        try (FileChannel fc = FileChannel.open(p, ExtendedOpenOption.DIRECT)) {
            int bs = (int)blockSize;
            int size = Math.max(BASE_SIZE, bs);
            int alignment = bs;
            ByteBuffer dest = ByteBuffer.allocateDirect(size + alignment - 1)
                                        .alignedSlice(alignment);
            assert dest.capacity() != 0;
            fc.read(dest);
            return size;
        }
    }

    public static Path createTempFile() throws IOException {
        return Files.createTempFile(
            Paths.get(System.getProperty("test.dir", ".")), "test", null);
    }

    private static boolean isFileInCache(int size, Path p) {
        String path = p.toString();
        return isFileInCache0(size, path);
    }

    private static native boolean isFileInCache0(int size, String path);

    public static void main(String[] args) throws Exception {
        Path p = createTempFile();
        long blockSize = Files.getFileStore(p).getBlockSize();

        System.loadLibrary("DirectIO");

        try {
            int size = testWrite(p, blockSize);
            if (isFileInCache(size, p)) {
                throw new RuntimeException("DirectIO is not working properly with "
                    + "write. File still exists in cache!");
            }
            size = testRead(p, blockSize);
            if (isFileInCache(size, p)) {
                throw new RuntimeException("DirectIO is not working properly with "
                    + "read. File still exists in cache!");
            }
        } finally {
            Files.delete(p);
        }
    }
}
