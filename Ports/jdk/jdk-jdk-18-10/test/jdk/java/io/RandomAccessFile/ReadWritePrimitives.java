/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4092350
   @summary Verify that reads and writes of primitives are correct
 */

// The bug mentioned is actually a performance bug that prompted
// changes in the methods to write primitives
import java.io.*;

import java.io.*;

public class ReadWritePrimitives {

    public static void main(String args[]) throws IOException {
        long start, finish;
        start = System.currentTimeMillis();
        testShort();
        finish = System.currentTimeMillis();
        // System.err.println("Time taken="+(finish-start));
        start = System.currentTimeMillis();
        testChar();
        finish = System.currentTimeMillis();
        // System.err.println("Time taken="+(finish-start));
        start = System.currentTimeMillis();
        testInt();
        finish = System.currentTimeMillis();
        // System.err.println("Time taken="+(finish-start));
        start = System.currentTimeMillis();
        testLong();
        finish = System.currentTimeMillis();
        // System.err.println("Time taken="+(finish-start));
    }

    private static void testShort() throws IOException {
        File fh = new File(System.getProperty("test.dir", "."),
                          "x.ReadWriteGenerated");
        RandomAccessFile f = new RandomAccessFile(fh,"rw");
        for(int i = 0; i < 10000; i++){
            f.writeShort((short)i);
        }
        f.writeShort((short)65535);
        f.close();
        f = new RandomAccessFile(fh,"r");
        for(int i = 0; i < 10000; i++) {
            short r = f.readShort();
            if (r != ((short)i)) {
                System.err.println("An error occurred. Read:" + r
                                   + " i:" + ((short)i));
                throw new IOException("Bad read from a writeShort");
            }
        }
        short rmax = f.readShort();
        if (rmax != ((short)65535)) {
            System.err.println("An error occurred. Read:" + rmax);
            throw new IOException("Bad read from a writeShort");
        }
        f.close();
    }

    private static void testChar() throws IOException {
        File fh = new File(System.getProperty("test.dir", "."),
                          "x.ReadWriteGenerated");
        RandomAccessFile f = new RandomAccessFile(fh,"rw");
        for(int i = 0; i < 10000; i++){
            f.writeChar((char)i);
        }
        f.close();
        f = new RandomAccessFile(fh,"r");
        for(int i = 0; i < 10000; i++) {
            char r = f.readChar();
            if (r != ((char)i)){
                System.err.println("An error occurred. Read:" + r
                                   + " i:" + ((char) i));
                throw new IOException("Bad read from a writeChar");
            }
        }
        f.close();
    }


    private static void testInt() throws IOException {
        File fh = new File(System.getProperty("test.dir", "."),
                          "x.ReadWriteGenerated");
        RandomAccessFile f = new RandomAccessFile(fh,"rw");
        for(int i = 0; i < 10000; i++){
            f.writeInt((short)i);
        }
        f.writeInt(Integer.MAX_VALUE);
        f.close();
        f = new RandomAccessFile(fh, "r");
        for(int i = 0; i < 10000; i++) {
            int r = f.readInt();
            if (r != i){
                System.err.println("An error occurred. Read:" + r
                                   + " i:" + i);
                throw new IOException("Bad read from a writeInt");
            }
        }
        int rmax = f.readInt();
        if (rmax != Integer.MAX_VALUE){
            System.err.println("An error occurred. Read:" + rmax);
            throw new IOException("Bad read from a writeInt");
        }
        f.close();
    }

    private static void testLong() throws IOException {
        File fh = new File(System.getProperty("test.dir", "."),
                          "x.ReadWriteGenerated");
        RandomAccessFile f = new RandomAccessFile(fh,"rw");
        for(int i = 0; i < 10000; i++){
            f.writeLong(123456789L * (long)i);
        }
        f.close();
        f = new RandomAccessFile(fh,"r");
        for(int i = 0; i < 10000; i++){
            long r = f.readLong();
            if (r != (((long) i) * 123456789L) ) {
                System.err.println("An error occurred. Read:" + r
                                   + " i" + ((long) i));

                throw new IOException("Bad read from a writeInt");
            }
        }
        f.close();

    }

}
