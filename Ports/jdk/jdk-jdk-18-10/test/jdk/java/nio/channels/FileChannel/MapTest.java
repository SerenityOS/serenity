/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429043 8002180
 * @summary Test file mapping with FileChannel
 * @run main/othervm/timeout=240 MapTest
 * @key randomness
 */

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.charset.StandardCharsets.*;
import java.util.Random;


/**
 * Testing FileChannel's mapping capabilities.
 */

public class MapTest {

    private static PrintStream out = System.out;
    private static PrintStream err = System.err;

    private static Random generator = new Random();

    private static int CHARS_PER_LINE = File.separatorChar == '/' ? 5 : 6;

    private static File blah;

    public static void main(String[] args) throws Exception {
        blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        initTestFile(blah);
        try {
            out.println("Test file " + blah + " initialized");
            testZero();
            out.println("Zero size: OK");
            testRead();
            out.println("Read: OK");
            testWrite();
            out.println("Write: OK");
            testHighOffset();
            out.println("High offset: OK");
            testForce();
            out.println("Force: OK");
            testExceptions();
            out.println("Exceptions: OK");
        } finally {
            blah.delete();
        }
    }

    /**
     * Creates file blah:
     * 0000
     * 0001
     * 0002
     * 0003
     * .
     * .
     * .
     * 3999
     *
     * Blah extends beyond a single page of memory so that the
     * ability to index into a file of multiple pages is tested.
     */
    private static void initTestFile(File blah) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(blah.toPath(), ISO_8859_1)) {
            for (int i=0; i<4000; i++) {
                String number = new Integer(i).toString();
                for (int h=0; h<4-number.length(); h++)
                    writer.write("0");
                writer.write(""+i);
                writer.newLine();
            }
        }
    }

    /**
     * Tests zero size file mapping
     */
    private static void testZero() throws Exception {
        try (FileInputStream fis = new FileInputStream(blah)) {
            FileChannel fc = fis.getChannel();
            MappedByteBuffer b = fc.map(MapMode.READ_ONLY, 0, 0);
        }
    }

    /**
     * Maps blah file with a random offset and checks to see if read
     * from the ByteBuffer gets the right line number
     */
    private static void testRead() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.setLength(4);

        for (int x=0; x<1000; x++) {
            try (FileInputStream fis = new FileInputStream(blah)) {
                FileChannel fc = fis.getChannel();

                long offset = generator.nextInt(10000);
                long expectedResult = offset / CHARS_PER_LINE;
                offset = expectedResult * CHARS_PER_LINE;

                MappedByteBuffer b = fc.map(MapMode.READ_ONLY,
                                            offset, 100);

                for (int i=0; i<4; i++) {
                    byte aByte = b.get(i);
                    sb.setCharAt(i, (char)aByte);
                }

                int result = Integer.parseInt(sb.toString());
                if (result != expectedResult) {
                    err.println("I expected "+expectedResult);
                    err.println("I got "+result);
                    throw new Exception("Read test failed");
                }
            }
        }
    }

    /**
     * Maps blah file with a random offset and checks to see if data
     * written out to the file can be read back in
     */
    private static void testWrite() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.setLength(4);

        for (int x=0; x<1000; x++) {
            try (RandomAccessFile raf = new RandomAccessFile(blah, "rw")) {
                FileChannel fc = raf.getChannel();

                long offset = generator.nextInt(1000);
                MappedByteBuffer b = fc.map(MapMode.READ_WRITE,
                                            offset, 100);

                for (int i=0; i<4; i++) {
                    b.put(i, (byte)('0' + i));
                }

                for (int i=0; i<4; i++) {
                    byte aByte = b.get(i);
                    sb.setCharAt(i, (char)aByte);
                }
                if (!sb.toString().equals("0123"))
                    throw new Exception("Write test failed");
            }
        }
    }

    private static void testHighOffset() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.setLength(4);

        for (int x=0; x<1000; x++) {
            try (RandomAccessFile raf = new RandomAccessFile(blah, "rw")) {
                FileChannel fc = raf.getChannel();
                long offset = 66000;
                MappedByteBuffer b = fc.map(MapMode.READ_WRITE,
                                            offset, 100);
            }
        }
    }

    /**
     * Maps blah file, writes some data and forcing writeback of
     * the data exercising various valid and invalid writeback ranges.
     */
    private static void testForce() throws Exception {
        for (int x=0; x<50; x++) {
            try (RandomAccessFile raf = new RandomAccessFile(blah, "rw")) {
                FileChannel fc = raf.getChannel();
                final int BLOCK_SIZE = 64;
                final int BLOCK_COUNT = (4096 * 2)/ BLOCK_SIZE;
                int offset = 0;
                MappedByteBuffer b = fc.map(MapMode.READ_WRITE,
                                            0, BLOCK_SIZE * (BLOCK_COUNT + 1));

                for (int blocks = 0; blocks < BLOCK_COUNT; blocks++) {
                    for (int i = 0; i < BLOCK_SIZE; i++) {
                        b.put(offset + i, (byte)('0' + i));
                    }
                    b.force(offset, BLOCK_SIZE);
                    offset += BLOCK_SIZE;
                }

                Exception exc = null;
                try {
                    // start and end are out of range
                    b.force(offset + BLOCK_SIZE, BLOCK_SIZE);
                } catch (IndexOutOfBoundsException e) {
                    exc = e;
                }
                if (exc == null) {
                    throw new RuntimeException("expected Exception for force beyond buffer extent");
                }

                exc = null;
                try {
                    // start is in range but end is out of range
                    b.force(offset, 2 * BLOCK_SIZE);
                } catch (IndexOutOfBoundsException e) {
                    exc = e;
                }
                if (exc == null) {
                    throw new RuntimeException("expected Exception for force beyond write limit");
                }
            }
        }
    }

    /**
     * Test exceptions specified by map method
     */
    private static void testExceptions() throws Exception {
        // check exceptions when channel opened for read access
        try (FileChannel fc = FileChannel.open(blah.toPath(), READ)) {
            testExceptions(fc);

            checkException(fc, MapMode.READ_WRITE, 0L, fc.size(),
                           NonWritableChannelException.class);

            checkException(fc, MapMode.READ_WRITE, -1L, fc.size(),
                           NonWritableChannelException.class, IllegalArgumentException.class);

            checkException(fc, MapMode.READ_WRITE, 0L, -1L,
                           NonWritableChannelException.class, IllegalArgumentException.class);

            checkException(fc, MapMode.PRIVATE, 0L, fc.size(),
                           NonWritableChannelException.class);

            checkException(fc, MapMode.PRIVATE, -1L, fc.size(),
                           NonWritableChannelException.class, IllegalArgumentException.class);

            checkException(fc, MapMode.PRIVATE, 0L, -1L,
                           NonWritableChannelException.class, IllegalArgumentException.class);
        }

        // check exceptions when channel opened for write access
        try (FileChannel fc = FileChannel.open(blah.toPath(), WRITE)) {
            testExceptions(fc);

            checkException(fc, MapMode.READ_ONLY, 0L, fc.size(),
                           NonReadableChannelException.class);

            checkException(fc, MapMode.READ_ONLY, -1L, fc.size(),
                           NonReadableChannelException.class, IllegalArgumentException.class);

            /*
             * implementation/spec mismatch, these tests disabled for now
             */
            //checkException(fc, MapMode.READ_WRITE, 0L, fc.size(),
            //               NonWritableChannelException.class);
            //checkException(fc, MapMode.PRIVATE, 0L, fc.size(),
            //               NonWritableChannelException.class);
        }

        // check exceptions when channel opened for read and write access
        try (FileChannel fc = FileChannel.open(blah.toPath(), READ, WRITE)) {
            testExceptions(fc);
        }
    }

    private static void testExceptions(FileChannel fc) throws IOException {
        checkException(fc, null, 0L, fc.size(),
                       NullPointerException.class);

        checkException(fc, MapMode.READ_ONLY, -1L, fc.size(),
                       IllegalArgumentException.class);

        checkException(fc, null, -1L, fc.size(),
                       IllegalArgumentException.class, NullPointerException.class);

        checkException(fc, MapMode.READ_ONLY, 0L, -1L,
                       IllegalArgumentException.class);

        checkException(fc, null, 0L, -1L,
                       IllegalArgumentException.class, NullPointerException.class);

        checkException(fc, MapMode.READ_ONLY, 0L, Integer.MAX_VALUE + 1L,
                       IllegalArgumentException.class);

        checkException(fc, null, 0L, Integer.MAX_VALUE + 1L,
                       IllegalArgumentException.class, NullPointerException.class);

        checkException(fc, MapMode.READ_ONLY, Long.MAX_VALUE, 1L,
                       IllegalArgumentException.class);

        checkException(fc, null, Long.MAX_VALUE, 1L,
                       IllegalArgumentException.class, NullPointerException.class);

    }

    /**
     * Checks that FileChannel map throws one of the expected exceptions
     * when invoked with the given inputs.
     */
    private static void checkException(FileChannel fc,
                                       MapMode mode,
                                       long position,
                                       long size,
                                       Class<?>... expected)
        throws IOException
    {
        Exception exc = null;
        try {
            fc.map(mode, position, size);
        } catch (Exception actual) {
            exc = actual;
        }
        if (exc != null) {
            for (Class<?> clazz: expected) {
                if (clazz.isInstance(exc)) {
                    return;
                }
            }
        }
        System.err.println("Expected one of");
        for (Class<?> clazz: expected) {
            System.out.println(clazz);
        }
        if (exc == null) {
            throw new RuntimeException("No expection thrown");
        } else {
            throw new RuntimeException("Unexpected exception thrown", exc);
        }
    }
}
