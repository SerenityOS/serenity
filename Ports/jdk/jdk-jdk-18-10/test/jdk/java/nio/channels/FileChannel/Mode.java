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
   @bug 4462298
 * @summary Test FileChannel maps with different accesses
 * @run main/othervm Mode
 */

import java.nio.channels.*;
import java.nio.MappedByteBuffer;
import java.io.*;


public class Mode {
   private static File testFile;

   public static void main(String[] args) throws Exception {
        testFile = File.createTempFile("testFile", null);
        testFile.deleteOnExit();
        testReadable();
        testWritable();
   }

    private static void testReadable() throws IOException {
        FileInputStream is = new FileInputStream(testFile);
        FileChannel channel = is.getChannel();
        try {
            MappedByteBuffer buff = channel.map(FileChannel.MapMode.READ_WRITE,
                                                0, 8);
            throw new RuntimeException("Exception expected, none thrown");
        } catch (NonWritableChannelException e) {
            // correct result
        }
        is.close();
    }

    private static void testWritable() throws IOException {
        FileOutputStream is = new FileOutputStream(testFile);
        FileChannel channel = is.getChannel();
        try {
            MappedByteBuffer buff = channel.map(FileChannel.MapMode.READ_ONLY,
                                                0, 8);
            throw new RuntimeException("Exception expected, none thrown");
        } catch (NonReadableChannelException e) {
            // correct result
        }
        is.close();
    }

}
