/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4638365
 * @summary Test FileChannel.transferFrom and transferTo for 4GB files
 * @run testng/timeout=300 Transfer4GBFile
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileAlreadyExistsException;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

public class Transfer4GBFile {

    private static PrintStream err = System.err;
    private static PrintStream out = System.out;

    // Test transferTo with large file
    @Test
    public void xferTest04() throws Exception { // for bug 4638365
        File source = File.createTempFile("blah", null);
        source.deleteOnExit();
        long testSize = ((long)Integer.MAX_VALUE) * 2;
        initTestFile(source, 10);
        RandomAccessFile raf = new RandomAccessFile(source, "rw");
        FileChannel fc = raf.getChannel();
        out.println("  Writing large file...");
        long t0 = System.nanoTime();
        fc.write(ByteBuffer.wrap("Use the source!".getBytes()), testSize - 40);
        long t1 = System.nanoTime();
        out.printf("  Wrote large file in %d ns (%d ms) %n",
            t1 - t0, TimeUnit.NANOSECONDS.toMillis(t1 - t0));

        fc.close();
        raf.close();

        File sink = File.createTempFile("sink", null);
        sink.deleteOnExit();

        FileInputStream fis = new FileInputStream(source);
        FileChannel sourceChannel = fis.getChannel();

        raf = new RandomAccessFile(sink, "rw");
        FileChannel sinkChannel = raf.getChannel();

        long bytesWritten = sourceChannel.transferTo(testSize -40, 10,
                                                     sinkChannel);
        if (bytesWritten != 10) {
            throw new RuntimeException("Transfer test 4 failed " +
                                       bytesWritten);
        }
        sourceChannel.close();
        sinkChannel.close();

        source.delete();
        sink.delete();
    }

    // Test transferFrom with large file
    @Test
    public void xferTest05() throws Exception { // for bug 4638365
        // Create a source file & large sink file for the test
        File source = File.createTempFile("blech", null);
        source.deleteOnExit();
        initTestFile(source, 100);

        // Create the sink file as a sparse file if possible
        File sink = null;
        FileChannel fc = null;
        while (fc == null) {
            sink = File.createTempFile("sink", null);
            // re-create as a sparse file
            sink.delete();
            try {
                fc = FileChannel.open(sink.toPath(),
                                      StandardOpenOption.CREATE_NEW,
                                      StandardOpenOption.WRITE,
                                      StandardOpenOption.SPARSE);
            } catch (FileAlreadyExistsException ignore) {
                // someone else got it
            }
        }
        sink.deleteOnExit();

        long testSize = ((long)Integer.MAX_VALUE) * 2;
        try {
            out.println("  Writing large file...");
            long t0 = System.nanoTime();
            fc.write(ByteBuffer.wrap("Use the source!".getBytes()),
                     testSize - 40);
            long t1 = System.nanoTime();
            out.printf("  Wrote large file in %d ns (%d ms) %n",
            t1 - t0, TimeUnit.NANOSECONDS.toMillis(t1 - t0));
        } catch (IOException e) {
            // Can't set up the test, abort it
            err.println("xferTest05 was aborted.");
            return;
        } finally {
            fc.close();
        }

        // Get new channels for the source and sink and attempt transfer
        FileChannel sourceChannel = new FileInputStream(source).getChannel();
        try {
            FileChannel sinkChannel = new RandomAccessFile(sink, "rw").getChannel();
            try {
                long bytesWritten = sinkChannel.transferFrom(sourceChannel,
                                                             testSize - 40, 10);
                if (bytesWritten != 10) {
                    throw new RuntimeException("Transfer test 5 failed " +
                                               bytesWritten);
                }
            } finally {
                sinkChannel.close();
            }
        } finally {
            sourceChannel.close();
        }

        source.delete();
        sink.delete();
    }

    /**
     * Creates file blah of specified size in bytes.
     */
    private static void initTestFile(File blah, long size) throws Exception {
        if (blah.exists())
            blah.delete();
        FileOutputStream fos = new FileOutputStream(blah);
        BufferedWriter awriter
            = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"));

        for(int i=0; i<size; i++) {
            awriter.write("e");
        }
        awriter.flush();
        awriter.close();
    }
}
