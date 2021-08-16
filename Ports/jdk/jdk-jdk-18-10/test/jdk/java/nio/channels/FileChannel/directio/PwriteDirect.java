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

/* @test
 * @bug 8164900
 * @summary Test positional write method of FileChannel with DirectIO
 * (use -Dseed=X to set PRNG seed)
 * @library .. /test/lib
 * @build jdk.test.lib.RandomFactory
 *        DirectIOTest
 * @run main/othervm PwriteDirect
 * @key randomness
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import com.sun.nio.file.ExtendedOpenOption;

import jdk.test.lib.RandomFactory;

/**
 * Testing FileChannel's positional write method.
 */
public class PwriteDirect {

    private static Random generator = RandomFactory.getRandom();

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
            genericTest();
            TestWithNotAlignedChannelPosition();
            testUnwritableChannel();
        }
    }

    private static void testUnwritableChannel() throws Exception {
        Path p = DirectIOTest.createTempFile();

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.DELETE_ON_CLOSE, ExtendedOpenOption.DIRECT)) {
            try {
                fc.write(ByteBuffer.allocate(charsPerGroup), 0);
                throw new RuntimeException("Expected exception not thrown");
            } catch(NonWritableChannelException e) {
                // Correct result
            }
        }
    }

    private static void TestWithNotAlignedChannelPosition() throws Exception {
        Path p = DirectIOTest.createTempFile();

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE, ExtendedOpenOption.DIRECT)) {
            int bufferSize = charsPerGroup;
            long position = charsPerGroup - 1;
            try {
                fc.write(ByteBuffer.allocate(bufferSize), position);
                throw new RuntimeException("Expected exception not thrown");
            } catch(IOException e) {
                if (!e.getMessage().contains("Channel position (" + position + ")"
                    + " is not a multiple of the block size (" + alignment + ")"))
                    throw new RuntimeException("Write test failed");
            }
        }
    }

    private static void genericTest() throws Exception {
        Path p = DirectIOTest.createTempFile();

        initTestFile(p);

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.READ, StandardOpenOption.WRITE,
            StandardOpenOption.DELETE_ON_CLOSE, ExtendedOpenOption.DIRECT)) {
            ByteBuffer block =
                ByteBuffer.allocateDirect(charsPerGroup + alignment - 1)
                          .alignedSlice(alignment);
            for (int x = 0; x < 100; x++) {
                block.clear();
                long offset = generator.nextInt(100) * charsPerGroup;

                // Write known sequence out
                for (int i = 0; i < charsPerGroup; i++) {
                    block.put(i, (byte)'a');
                }
                long originalPosition = fc.position();

                int written = fc.write(block, offset);
                if (written < 0)
                    throw new Exception("Write failed");

                long newPosition = fc.position();

                // Ensure that file pointer position has not changed
                if (originalPosition != newPosition)
                    throw new Exception("File position modified");

                // Attempt to read sequence back in
                originalPosition = fc.position();

                block.rewind();
                int read = fc.read(block, offset);
                if (read != charsPerGroup)
                    throw new Exception("Read failed");

                newPosition = fc.position();

                // Ensure that file pointer position has not changed
                if (originalPosition != newPosition)
                    throw new Exception("File position modified");

                for (int j = 0; j < charsPerGroup; j++) {
                    if (block.get(j) != (byte)'a')
                        throw new Exception("Write test failed");
                }
            }
        }
    }

    private static void initTestFile(Path p) throws Exception {
        try (OutputStream fos = Files.newOutputStream(p)) {
            try (BufferedWriter awriter
                = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"))) {
                for (int i = 0; i < 100; i++) {
                    String number = new Integer(i).toString();
                    for (int h = 0; h < 4 - number.length(); h++)
                        awriter.write("0");
                    awriter.write("" + i);
                    for (int j = 0; j < 4092; j++)
                        awriter.write("0");
                }
                awriter.flush();
            }
        }
    }
}
