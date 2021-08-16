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
 * @summary Test positional read method of FileChannel with DirectIO
 * (use -Dseed=X to set PRNG seed)
 * @library .. /test/lib
 * @build jdk.test.lib.RandomFactory
 *        DirectIOTest
 * @run main/othervm PreadDirect
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
 * Testing FileChannel's positional read method.
 */

public class PreadDirect {

    private static PrintStream err = System.err;

    private static Random generator = RandomFactory.getRandom();

    private static int charsPerGroup = -1;

    private static int alignment = -1;

    public static void main(String[] args) throws Exception {
        if (initTests()) {
            genericTest();
            testNotAlignedChannelPosition();
            testNegativeChannelPosition();
        }
    }

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

    private static void testNegativeChannelPosition() throws Exception {
        Path p = DirectIOTest.createTempFile();

        try (OutputStream fos = Files.newOutputStream(p)) {
            fos.write(new byte[charsPerGroup]);
        }

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.DELETE_ON_CLOSE, ExtendedOpenOption.DIRECT)) {
            try {
                fc.read(ByteBuffer.allocate(charsPerGroup), -1L);
                throw new RuntimeException("Expected exception not thrown");
            } catch(IllegalArgumentException e) {
                // Correct result
            }
        }
    }

    private static void testNotAlignedChannelPosition() throws Exception {
        Path p = DirectIOTest.createTempFile();

        try (OutputStream fos = Files.newOutputStream(p)) {
            fos.write(new byte[charsPerGroup]);
        }

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.DELETE_ON_CLOSE, ExtendedOpenOption.DIRECT)) {
            long pos = charsPerGroup - 1;
            try {
                fc.read(ByteBuffer.allocate(charsPerGroup), pos);
                throw new RuntimeException("Expected exception not thrown");
            } catch(IOException e) {
                if (!e.getMessage().contains("Channel position (" + pos
                    + ") is not a multiple of the block size (" + alignment + ")"))
                    throw new RuntimeException("Read test failed");
            }
        }
    }

    private static void genericTest() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.setLength(2);

        Path p = DirectIOTest.createTempFile();

        initTestFile(p);

        try (FileChannel fc = FileChannel.open(p,
            StandardOpenOption.DELETE_ON_CLOSE, ExtendedOpenOption.DIRECT)) {
            ByteBuffer block =
                ByteBuffer.allocateDirect(charsPerGroup + alignment - 1)
                          .alignedSlice(alignment);
            for (int x = 0; x < 100; x++) {
                block.clear();
                long offset = generator.nextInt(100) * charsPerGroup;
                long expectedResult = offset / charsPerGroup;
                offset = expectedResult * charsPerGroup;

                long originalPosition = fc.position();

                int read = fc.read(block, offset);
                if (read != charsPerGroup)
                    throw new Exception("Read failed");

                long newPosition = fc.position();

                for (int i = 0; i < 2; i++) {
                    byte aByte = block.get(i);
                    sb.setCharAt(i, (char)aByte);
                }
                int result = Integer.parseInt(sb.toString());
                if (result != expectedResult) {
                    err.println("I expected "+ expectedResult);
                    err.println("I got "+ result);
                    throw new Exception("Read test failed");
                }

                // Ensure that file pointer position has not changed
                if (originalPosition != newPosition)
                    throw new Exception("File position modified");
            }
        }
    }

    private static void initTestFile(Path p) throws Exception {
        try (OutputStream fos = Files.newOutputStream(p)) {
            try (BufferedWriter awriter
                 = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"))) {

                for (int i = 0; i < 100; i++) {
                    String number = new Integer(i).toString();
                    for (int h = 0; h < 2 - number.length(); h++)
                        awriter.write("0");
                    awriter.write(""+i);
                    for (int j = 0; j < (charsPerGroup - 2); j++)
                        awriter.write("0");
                }
                awriter.flush();
            }
        }
    }
}
