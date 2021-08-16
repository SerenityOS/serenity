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

/* @test
   @bug 4452020 4629048 4638365 4869859
 * @summary Test FileChannel scattering reads
 * @run main/othervm ScatteringRead
 */

import java.nio.channels.*;
import java.nio.*;
import java.io.*;

public class ScatteringRead {

    private static final int NUM_BUFFERS = 3;

    private static final int BUFFER_CAP = 3;

    private static final int BIG_BUFFER_CAP = Integer.MAX_VALUE / 3 + 10;

    public static void main(String[] args) throws Exception {
        test1(); // for bug 4452020
        test2(); // for bug 4629048
        System.gc();
    }

    private static void test1() throws Exception {
        ByteBuffer dstBuffers[] = new ByteBuffer[NUM_BUFFERS];
        for (int i=0; i<NUM_BUFFERS; i++)
            dstBuffers[i] = ByteBuffer.allocateDirect(BUFFER_CAP);
        File blah = File.createTempFile("blah1", null);
        blah.deleteOnExit();
        createTestFile(blah);

        FileInputStream fis = new FileInputStream(blah);
        FileChannel fc = fis.getChannel();

        byte expectedResult = -128;
        for (int k=0; k<20; k++) {
            long bytesRead = fc.read(dstBuffers);
            for (int i=0; i<NUM_BUFFERS; i++) {
                for (int j=0; j<BUFFER_CAP; j++) {
                    byte b = dstBuffers[i].get(j);
                    if (b != expectedResult++)
                        throw new RuntimeException("Test failed");
                }
                dstBuffers[i].flip();
            }
        }
        fis.close();
    }

    private static void createTestFile(File blah) throws Exception {
        FileOutputStream fos = new FileOutputStream(blah);
        for(int i=-128; i<128; i++)
            fos.write((byte)i);
        fos.flush();
        fos.close();
    }

    private static void test2() throws Exception {
        ByteBuffer dstBuffers[] = new ByteBuffer[2];
        for (int i=0; i<2; i++)
            dstBuffers[i] = ByteBuffer.allocateDirect(10);
        File blah = File.createTempFile("blah2", null);
        blah.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(blah);
        for(int i=0; i<15; i++)
            fos.write((byte)92);
        fos.flush();
        fos.close();

        FileInputStream fis = new FileInputStream(blah);
        FileChannel fc = fis.getChannel();

        long bytesRead = fc.read(dstBuffers);
        if (dstBuffers[1].limit() != 10)
            throw new Exception("Scattering read changed buf limit.");
        fis.close();
    }
}
