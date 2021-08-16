/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4938372 6541641
 * @summary Flushing dirty pages prior to unmap can cause Cleaner thread to
 *          abort VM if memory system has pages locked
 * @run main/othervm ExpandingMap
 */
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * Test case provided by submitter of 4938372.
 */

public class ExpandingMap {

    public static void main(String[] args) throws Exception {

        int initialSize = 20480*1024;
        int maximumMapSize = 16*1024*1024;
        int maximumFileSize = 300000000;

        File file = File.createTempFile("exp", "tmp");
        file.deleteOnExit();
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        f.setLength(initialSize);

        FileChannel fc = f.getChannel();

        ByteBuffer[] buffers = new ByteBuffer[128];

        System.out.format("map %d -> %d\n", 0, initialSize);
        buffers[0] = fc.map(FileChannel.MapMode.READ_WRITE, 0, initialSize);

        int currentBuffer = 0;
        int currentSize = initialSize;
        int currentPosition = 0;

        ArrayList<String> junk = new ArrayList<String>();

        while (currentPosition+currentSize < maximumFileSize) {
            int inc = Math.max(1000*1024, (currentPosition+currentSize)/8);

            int size = currentPosition+currentSize+inc;
            f.setLength(size);

            while (currentSize+inc > maximumMapSize) {
                if (currentSize < maximumMapSize) {
                    System.out.format("map %d -> %d\n", currentPosition,
                        (currentPosition + maximumMapSize));
                    buffers[currentBuffer] = fc.map(FileChannel.MapMode.READ_WRITE,
                        currentPosition, maximumMapSize);
                    fillBuffer(buffers[currentBuffer], currentSize);
                }
                currentPosition += maximumMapSize;
                inc = currentSize+inc-maximumMapSize;
                currentSize = 0;
                currentBuffer++;
                if (currentBuffer == buffers.length) {
                    ByteBuffer[] old = buffers;
                    buffers = new ByteBuffer[currentBuffer+currentBuffer/2];
                    System.arraycopy(old, 0, buffers, 0, currentBuffer);                                        }
            }
            currentSize += inc;
            if (currentSize > 0) {
                System.out.format("map %d -> %d\n", currentPosition,
                    (currentPosition + currentSize));
                buffers[currentBuffer] = fc.map(FileChannel.MapMode.READ_WRITE,
                     currentPosition, currentSize);
                fillBuffer(buffers[currentBuffer], currentSize-inc);
            }

            // busy loop needed to reproduce issue
            long t = System.currentTimeMillis();
            while (System.currentTimeMillis() < t+500) {
                junk.add(String.valueOf(t));
                if (junk.size() > 100000) junk.clear();
            }
        }

        fc.close();
        // cleanup the ref to mapped buffers so they can be GCed
        for (int i = 0; i < buffers.length; i++)
            buffers[i] = null;
        System.gc();
        // Take a nap to wait for the Cleaner to cleanup those unrefed maps
        Thread.sleep(1000);
        System.out.println("TEST PASSED");
    }

    static void fillBuffer(ByteBuffer buf, int from) {
        int limit = buf.limit();
        for (int i=from; i<limit; i++) {
            buf.put(i, (byte)i);
        }
    }
}
