/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171377
 * @summary Basic test for Unsafe::invokeCleaner
 * @modules jdk.unsupported
 * @run testng/othervm InvokeCleaner
 */

import java.io.Closeable;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import sun.misc.Unsafe;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

public class InvokeCleaner {

    static Unsafe UNSAFE;
    static Path bob = Paths.get("bob");
    static List<Closeable> closeables = new ArrayList<>();

    @BeforeClass
    static void init() throws Exception {
        UNSAFE = getUnsafe();

        byte[] srcData = new byte[20];
        for (int i=0; i<20; i++)
            srcData[i] = (byte)i;
        Files.write(bob, srcData);
    }

    @DataProvider(name = "badBuffers")
    static Object[][] createBadBuffers() throws Exception {
        FileChannel fc = FileChannel.open(bob);
        closeables.add(fc);
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, 10);

        return new Object[][] {
                { ByteBuffer.allocate(0) },
                { ByteBuffer.allocate(10) },
                { ByteBuffer.allocate(10).duplicate() },
                { ByteBuffer.allocate(10).slice() },
                { ByteBuffer.allocateDirect(10).duplicate() },
                { ByteBuffer.allocateDirect(10).slice() },
                { ByteBuffer.allocateDirect(0).duplicate() },
                { ByteBuffer.allocateDirect(0).slice() },
                { mbb.duplicate() },
                { mbb.slice() }
        };
    }

    @Test(dataProvider="badBuffers",
          expectedExceptions = IllegalArgumentException.class)
    public void badBuffers(ByteBuffer buffer) throws Exception {
        UNSAFE.invokeCleaner(buffer);
    }

    @DataProvider(name = "goodBuffers")
    static Object[][] createGoodBuffers() throws Exception {
        FileChannel fc = FileChannel.open(bob);
        closeables.add(fc);
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, 10);
        mbb.load();

        return new Object[][] {
                { ByteBuffer.allocateDirect(0) },
                { ByteBuffer.allocateDirect(10) },
                { mbb },
                { fc.map(FileChannel.MapMode.READ_ONLY, 1, 11) }
        };
    }

    @Test(dataProvider="goodBuffers")
    public void goodBuffers(ByteBuffer buffer) throws Exception {
        UNSAFE.invokeCleaner(buffer);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void npe() throws Exception {
        UNSAFE.invokeCleaner(null);
    }

    static Unsafe getUnsafe() throws ReflectiveOperationException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe)f.get(null);
    }

    @AfterClass
    public void cleanup() throws Exception {
        for(Closeable fc : closeables)
            fc.close();
    }
}
