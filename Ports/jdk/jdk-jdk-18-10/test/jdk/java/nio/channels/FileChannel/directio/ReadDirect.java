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
 * @summary Test read method of FileChannel with DirectIO
 * (use -Dseed=X to set PRNG seed)
 * @library .. /test/lib
 * @build jdk.test.lib.RandomFactory
 *        DirectIOTest
 * @run main/othervm ReadDirect
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

public class ReadDirect {

    private static PrintStream err = System.err;

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

    private static void testWithSingleBuffer() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.setLength(2);

        Path p = DirectIOTest.createTempFile();

        initTestFile(p);
        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
            ExtendedOpenOption.DIRECT)) {
            ByteBuffer block = ByteBuffer.allocateDirect(charsPerGroup
                + alignment - 1).alignedSlice(alignment);
            for (int x = 0; x < 100; x++) {
                block.clear();
                long offset = x * charsPerGroup;
                long expectedResult = offset / charsPerGroup;
                fc.read(block);

                for (int i = 0; i < 2; i++) {
                    byte aByte = block.get(i);
                    sb.setCharAt(i, (char)aByte);
                }
                int result = Integer.parseInt(sb.toString());
                if (result != expectedResult) {
                    err.println("I expected " + expectedResult);
                    err.println("I got " + result);
                    throw new Exception("Read test failed");
                }
            }
        }
    }

    private static void testWithNotAlignedBufferSize() throws Exception {
        int bufferSize = charsPerGroup - 1;
        Path p = DirectIOTest.createTempFile();

        try (OutputStream fos = Files.newOutputStream(p)) {
            fos.write(new byte[bufferSize]);
        }

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
            ExtendedOpenOption.DIRECT)) {
            ByteBuffer block = ByteBuffer.allocate(bufferSize);
            try {
                fc.read(block);
                throw new RuntimeException("Expected exception not thrown");
            } catch (IOException e) {
                if (!e.getMessage().contains("Number of remaining bytes ("
                    + bufferSize + ") is not a multiple of the block size ("
                    + alignment + ")"))
                    throw new Exception("Read test failed");
            }
        }
    }

    private static void testWithNotAlignedBufferOffset() throws Exception {
        int bufferSize = charsPerGroup * 2;
        int pos = alignment - 1;

        Path p = DirectIOTest.createTempFile();

        try (OutputStream fos = Files.newOutputStream(p)) {
            fos.write(new byte[bufferSize]);
        }

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
            ExtendedOpenOption.DIRECT)) {
            ByteBuffer block = ByteBuffer.allocateDirect(bufferSize);
            block.position(pos);
            block.limit(bufferSize - 1);
            try {
                fc.read(block);
                throw new RuntimeException("Expected exception not thrown");
            } catch (IOException e) {
                if (!e.getMessage().contains("Current location of the bytebuffer "
                    +  "(" + pos + ") is not a multiple of the block size ("
                    + alignment + ")"))
                    throw new Exception("Read test failed");
            }
        }
    }

    private static void testWithArrayOfBuffer() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.setLength(2);
        ByteBuffer[] dests = new ByteBuffer[4];
        Path p = DirectIOTest.createTempFile();

        initTestFile(p);

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
            ExtendedOpenOption.DIRECT)) {
            int randomNumber = -1;

            for (int i = 0; i < 4; i++) {
                dests[i] = ByteBuffer.allocateDirect
                    (charsPerGroup + alignment - 1).alignedSlice(alignment);
                for (int j = 0; j < charsPerGroup; j++) {
                    dests[i].put(j, (byte)'a');
                }
            }

            // The size of the test FileChannel is 100*charsPerGroup.
            // As the channel bytes will be scattered into two buffers
            // each of size charsPerGroup, the offset cannot be greater
            // than 98*charsPerGroup, so the value of randomNumber must
            // be in the range [0,98], i.e., 0 <= randomNumber < 99.
            randomNumber = generator.nextInt(99);
            long offset =  randomNumber * charsPerGroup;
            fc.position(offset);
            fc.read(dests, 1, 2);

            for (int i = 0; i < 4; i++) {
                if (i == 1 || i == 2) {
                    for (int j = 0; j < 2; j++) {
                        byte aByte = dests[i].get(j);
                        sb.setCharAt(j, (char)aByte);
                    }
                    int result = Integer.parseInt(sb.toString());
                    int expectedResult = randomNumber + i - 1;
                    if (result != expectedResult) {
                        err.println("I expected " + expectedResult);
                        err.println("I got " + result);
                        throw new Exception("Read test failed");
                    }
                } else {
                    for (int k = 0; k < charsPerGroup; k++) {
                        if (dests[i].get(k) != (byte)'a')
                            throw new RuntimeException("Read test failed");
                    }
                }
            }
        }
    }

    public static void testOnEOF() throws Exception {
        int bufferSize = charsPerGroup / 2;
        Path p = DirectIOTest.createTempFile();

        try (OutputStream fos = Files.newOutputStream(p)) {
            byte[] writeBlock = new byte[bufferSize];
            for (int i = 0; i < bufferSize; i++) {
                writeBlock[i] = ((byte)'a');
            }
            fos.write(writeBlock);
        }

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
            ExtendedOpenOption.DIRECT)) {
            ByteBuffer block = ByteBuffer.allocateDirect(
                    (bufferSize / alignment + 1) * alignment + alignment - 1)
                    .alignedSlice(alignment);
            int result = fc.read(block);
            if (result != bufferSize) {
                err.println("Number of bytes to read " + bufferSize);
                err.println("I read " + result);
                throw new Exception("Read test failed");
            }
            for (int j = 0; j < bufferSize; j++) {
                if (block.get(j) != (byte)'a')
                    throw new RuntimeException("Read test failed");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (initTests()) {
            testWithSingleBuffer();
            testWithNotAlignedBufferSize();
            testWithNotAlignedBufferOffset();
            testWithArrayOfBuffer();
            testOnEOF();
        }
    }

    private static void initTestFile(Path p)
            throws Exception {
        try (OutputStream fos = Files.newOutputStream(p)) {
            try (BufferedWriter awriter
                 = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"))) {
                for (int i = 0; i < 100; i++) {
                    String number = new Integer(i).toString();
                    for (int h = 0; h < 2 - number.length(); h++)
                        awriter.write("0");
                    awriter.write("" + i);
                    for (int j = 0; j < (charsPerGroup - 2); j++)
                        awriter.write("0");
                }
                awriter.flush();
            }
        }
    }
}
