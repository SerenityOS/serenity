/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4526350
   @summary Verify IOUtil.java reads to buffer limits
 */
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class ReadToLimit {
    public static void main(String[] args) throws Exception {
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        initTestFile(blah);

        ByteBuffer[] dstBuffers = new ByteBuffer[2];
        for(int i=0; i<2; i++) {
            dstBuffers[i] = ByteBuffer.allocateDirect(10);
            dstBuffers[i].limit(5);
        }
        FileInputStream fis = new FileInputStream(blah);
        FileChannel fc = fis.getChannel();
        long bytesRead = fc.read(dstBuffers);
        for(int i=0; i<2; i++)
            if (dstBuffers[i].position() != 5)
                throw new Exception("Test failed");
        fc.close();
        fis.close();
        blah.delete();
    }

    /**
     * Creates file blah:
     * 0000
     * 0001
     * 0002
     * 0003
     */
    private static void initTestFile(File blah) throws Exception {
        FileOutputStream fos = new FileOutputStream(blah);
        BufferedWriter awriter
            = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"));

        for(int i=0; i<4; i++) {
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
