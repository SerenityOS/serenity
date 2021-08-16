/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4625907 8246729
 * @summary Testing force()
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main/othervm Force
 * @key randomness
 */

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.*;
import java.util.Random;
import jdk.test.lib.RandomFactory;

public class Force {
    public static void main(String[] args) throws Exception {
        test1();
        test2();
    }

    private static void test1() throws Exception {
        Random random = RandomFactory.getRandom();
        long filesize = random.nextInt(3*1024*1024);
        int cut = random.nextInt((int)filesize);
        File file = File.createTempFile("Blah", null);
        file.deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(filesize);
            FileChannel fc = raf.getChannel();
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, cut, filesize-cut);
            mbb.force();
        }

        // improve chance that mapped buffer will be unmapped
        System.gc();
        Thread.sleep(500);
    }

    private static void test2() throws Exception {
        var path = Files.createTempFile("test", "map");
        var channel = FileChannel.open(path, READ, WRITE);
        MappedByteBuffer buffer =
            channel.map(FileChannel.MapMode.READ_WRITE, 0, 1000);
        buffer.putInt(1234);
        buffer.limit(4);
        buffer.force();
    }
}
