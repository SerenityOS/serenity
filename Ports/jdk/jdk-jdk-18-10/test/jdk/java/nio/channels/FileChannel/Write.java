/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4475533 4698138 4638365 4796221
 * @summary Test FileChannel write
 * @run main/othervm Write
 */

import java.nio.channels.*;
import java.nio.*;
import java.io.*;

public class Write {

   public static void main(String[] args) throws Exception {
       test1(); // for bug 4475533
       test2();
       test3(); // for bug 4698138
   }

    // Test to see that offset > length does not throw exception
    static void test1() throws Exception {
        ByteBuffer[] dsts = new ByteBuffer[4];
        for (int i=0; i<4; i++)
            dsts[i] = ByteBuffer.allocateDirect(10);

        File testFile = File.createTempFile("test1", null);
        try {
            FileOutputStream fos = new FileOutputStream(testFile);
            FileChannel fc = fos.getChannel();
            fc.write(dsts, 2, 1);
            fos.close();
        } finally {
            testFile.delete();
        }
    }

    // Test to see that the appropriate buffers are updated
    static void test2() throws Exception {
        File testFile = File.createTempFile("test2", null);
        testFile.delete();
        ByteBuffer[] srcs = new ByteBuffer[4];
        for (int i=0; i<4; i++)
            srcs[i] = ByteBuffer.allocateDirect(10);

        srcs[0].put((byte)1); srcs[0].flip();
        srcs[1].put((byte)2); srcs[1].flip();
        srcs[2].put((byte)3); srcs[2].flip();
        srcs[3].put((byte)4); srcs[3].flip();

        FileOutputStream fos = new FileOutputStream(testFile);
        FileChannel fc = fos.getChannel();
        try {
            fc.write(srcs, 1, 2);
        } finally {
            fc.close();
        }

        FileInputStream fis = new FileInputStream(testFile);
        fc = fis.getChannel();
        try {
            ByteBuffer bb = ByteBuffer.allocateDirect(10);
            fc.read(bb);
            bb.flip();
            if (bb.get() != 2)
                throw new RuntimeException("Write failure");
            if (bb.get() != 3)
                throw new RuntimeException("Write failure");
            try {
                bb.get();
                throw new RuntimeException("Write failure");
            } catch (BufferUnderflowException bufe) {
                // correct result
            }
        } finally {
            fc.close();
        }

        // eagerly clean-up
        testFile.delete();
    }

    // Test write to a negative position (bug 4698138).
    static void test3() throws Exception {
        File testFile = File.createTempFile("test1", null);
        testFile.deleteOnExit();
        ByteBuffer dst = ByteBuffer.allocate(10);
        FileOutputStream fos = new FileOutputStream(testFile);
        FileChannel fc = fos.getChannel();
        try {
            fc.write(dst, -1);
            throw new RuntimeException("Expected IAE not thrown");
        } catch (IllegalArgumentException iae) {
            // Correct result
        } finally {
            fos.close();
        }
    }
}
