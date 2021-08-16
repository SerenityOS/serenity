/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4862411
 * @summary Test positional write method of FileChannel
 * @key randomness
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.channels.FileChannel;
import java.util.Random;


/**
 * Testing FileChannel's positional write method.
 */
public class Pwrite {

    private static Random generator = new Random();

    private static File blah;

    public static void main(String[] args) throws Exception {
        genericTest();
        testUnwritableChannel();
    }

    // This test for bug 4862411
    private static void testUnwritableChannel() throws Exception {
        File blah = File.createTempFile("blah2", null);
        blah.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(blah);
        fos.write(new byte[128]);
        fos.close();
        FileInputStream fis = new FileInputStream(blah);
        FileChannel fc = fis.getChannel();
        try {
            fc.write(ByteBuffer.allocate(256),1);
            throw new RuntimeException("Expected exception not thrown");
        } catch(NonWritableChannelException e) {
            // Correct result
        } finally {
            fc.close();
            blah.delete();
        }
    }

    private static void genericTest() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.setLength(4);

        blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        initTestFile(blah);

        RandomAccessFile raf = new RandomAccessFile(blah, "rw");
        FileChannel c = raf.getChannel();

        for (int x=0; x<100; x++) {
            long offset = generator.nextInt(1000);
            ByteBuffer bleck = ByteBuffer.allocateDirect(4);

            // Write known sequence out
            for (byte i=0; i<4; i++) {
                bleck.put(i);
            }
            bleck.flip();
            long originalPosition = c.position();
            int totalWritten = 0;
            while (totalWritten < 4) {
                int written = c.write(bleck, offset);
                if (written < 0)
                    throw new Exception("Read failed");
                totalWritten += written;
            }

            long newPosition = c.position();

            // Ensure that file pointer position has not changed
            if (originalPosition != newPosition)
                throw new Exception("File position modified");

            // Attempt to read sequence back in
            bleck = ByteBuffer.allocateDirect(4);
            originalPosition = c.position();
            int totalRead = 0;
            while (totalRead < 4) {
                int read = c.read(bleck, offset);
                if (read < 0)
                    throw new Exception("Read failed");
                totalRead += read;
            }
            newPosition = c.position();

            // Ensure that file pointer position has not changed
            if (originalPosition != newPosition)
                throw new Exception("File position modified");

            for (byte i=0; i<4; i++) {
                if (bleck.get(i) != i)
                    throw new Exception("Write test failed");
            }
        }
        c.close();
        raf.close();
        blah.delete();
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
        FileOutputStream fos = new FileOutputStream(blah);
        BufferedWriter awriter
            = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"));

        for(int i=0; i<4000; i++) {
            String number = new Integer(i).toString();
            for (int h=0; h<4-number.length(); h++)
                awriter.write("0");
            awriter.write(""+i);
            awriter.newLine();
        }
       awriter.flush();
       awriter.close();
    }
}
