/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4563125
 * @summary Test size method of FileChannel
 * @run main/othervm Size
 * @key randomness
 */

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.util.Random;


/**
 * Testing FileChannel's size method.
 */

public class Size {

    public static void main(String[] args) throws Exception {
        testSmallFile();
        testLargeFile();
    }

    private static void testSmallFile() throws Exception {
        File smallFile = new File("smallFileTest");
        Random generator = new Random();
        for(int i=0; i<100; i++) {
            long testSize = generator.nextInt(1000);
            initTestFile(smallFile, testSize);
            try (FileChannel c = new FileInputStream(smallFile).getChannel()) {
                if (c.size() != testSize) {
                    throw new RuntimeException("Size failed in testSmallFile. "
                                             + "Expect size " + testSize
                                             + ", actual size " + c.size());
                }
            }
        }
        smallFile.deleteOnExit();
    }

    // Test for bug 4563125
    private static void testLargeFile() throws Exception {
        File largeFile = new File("largeFileTest");
        long testSize = ((long)Integer.MAX_VALUE) * 2;
        initTestFile(largeFile, 10);
        try (FileChannel fc = new RandomAccessFile(largeFile, "rw").getChannel()) {
            fc.size();
            fc.map(FileChannel.MapMode.READ_WRITE, testSize, 10);
            if (fc.size() != testSize + 10) {
                throw new RuntimeException("Size failed in testLargeFile. "
                                         + "Expect size " + (testSize + 10)
                                         + ", actual size " + fc.size());
            }
        }
        largeFile.deleteOnExit();
    }

    /**
     * Create a file with the specified size in bytes.
     *
     */
    private static void initTestFile(File f, long size) throws Exception {
        try (BufferedWriter awriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(f), "8859_1")))
        {
            for(int i=0; i<size; i++) {
                awriter.write("e");
            }
        }
    }
}
