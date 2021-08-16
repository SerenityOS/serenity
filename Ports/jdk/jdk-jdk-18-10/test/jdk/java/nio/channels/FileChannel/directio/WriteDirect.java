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
 * @summary Test FileChannel write with DirectIO
 * @library .. /test/lib
 * @build DirectIOTest
 * @run main/othervm WriteDirect
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import com.sun.nio.file.ExtendedOpenOption;

public class WriteDirect {

    private static int charsPerGroup = -1;

    private static int alignment = -1;

    private static boolean initTests() throws Exception {
        Path p = DirectIOTest.createTempFile();
        try {
            FileStore fs = Files.getFileStore(p);
            alignment = (int)fs.getBlockSize();
            charsPerGroup = alignment;
        } finally {
            Files.delete(p);
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        if (initTests()) {
            testWithNotAlignedBuffer();
            testWithNotAlignedBufferOffset();
            testWithArrayOfBuffer();
        }
    }

    static void testWithNotAlignedBuffer() throws Exception {
        Path p = DirectIOTest.createTempFile();
        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE,
            ExtendedOpenOption.DIRECT)) {
            int bufferSize = charsPerGroup - 1;
            ByteBuffer src = ByteBuffer.allocate(bufferSize);
            try {
                fc.write(src);
                throw new RuntimeException("Expected exception not thrown");
            } catch (IOException e) {
                if (!e.getMessage().contains("Number of remaining bytes ("
                    + bufferSize + ") is not a multiple of the block size ("
                    + alignment + ")"))
                    throw new Exception("Write failure");
            }
        }
    }

    private static void testWithNotAlignedBufferOffset() throws Exception {
        int bufferSize = charsPerGroup * 2;
        int pos = alignment - 1;

        Path p = DirectIOTest.createTempFile();

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE,
            ExtendedOpenOption.DIRECT)) {
            ByteBuffer block = ByteBuffer.allocateDirect(bufferSize);
            block.position(pos);
            block.limit(bufferSize - 1);
            try {
                fc.write(block);
                throw new RuntimeException("Expected exception not thrown");
            } catch (IOException e) {
                if (!e.getMessage().contains("Current location of the bytebuffer "
                    +  "(" + pos + ") is not a multiple of the block size ("
                    + alignment + ")"))
                    throw new Exception("Write test failed");
            }
        }
    }

    static void testWithArrayOfBuffer() throws Exception {
        Path p = DirectIOTest.createTempFile();
        ByteBuffer[] srcs = new ByteBuffer[4];
        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.WRITE, ExtendedOpenOption.DIRECT)) {
            for (int i = 0; i < 4; i++) {
                srcs[i] = ByteBuffer.allocateDirect(charsPerGroup + alignment - 1)
                                    .alignedSlice(alignment);
                for (int j = 0; j < charsPerGroup; j++) {
                    srcs[i].put((byte)i);
                }
                srcs[i].flip();
            }

            fc.write(srcs, 1, 2);
        }

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE)) {
            ByteBuffer bb = ByteBuffer.allocateDirect(charsPerGroup * 2);
            fc.read(bb);
            bb.flip();
            for (int k = 0; k < charsPerGroup; k++) {
                if (bb.get() != 1)
                    throw new RuntimeException("Write failure");
            }
            for (int m = 0; m < charsPerGroup; m++) {
                if (bb.get() != 2)
                    throw new RuntimeException("Write failure");
            }
            try {
                bb.get();
                throw new RuntimeException("Write failure");
            } catch (BufferUnderflowException bufe) {
                // correct result
            }
        }
    }
}
