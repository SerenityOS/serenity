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
 * @bug 4462336 6799037
 * @summary Simple MappedByteBuffer tests
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class Basic {
    public static void main(String[] args) throws Exception {
        byte[] srcData = new byte[20];
        for (int i=0; i<20; i++)
            srcData[i] = 3;
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(blah);
        FileChannel fc = fos.getChannel();
        fc.write(ByteBuffer.wrap(srcData));
        fc.close();
        fos.close();

        FileInputStream fis = new FileInputStream(blah);
        fc = fis.getChannel();
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, 10);
        mbb.load();
        mbb.isLoaded();
        mbb.force();
        if (!mbb.isReadOnly())
            throw new RuntimeException("Incorrect isReadOnly");

        // repeat with unaligned position in file
        mbb = fc.map(FileChannel.MapMode.READ_ONLY, 1, 10);
        mbb.load();
        mbb.isLoaded();
        mbb.force();
        fc.close();
        fis.close();

        RandomAccessFile raf = new RandomAccessFile(blah, "r");
        fc = raf.getChannel();
        mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, 10);
        if (!mbb.isReadOnly())
            throw new RuntimeException("Incorrect isReadOnly");
        fc.close();
        raf.close();

        raf = new RandomAccessFile(blah, "rw");
        fc = raf.getChannel();
        mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 10);
        if (mbb.isReadOnly())
            throw new RuntimeException("Incorrect isReadOnly");
        fc.close();
        raf.close();

        // clean-up
        mbb = null;
        System.gc();
        Thread.sleep(500);
    }
}
