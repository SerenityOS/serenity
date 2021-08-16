/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4017728 4079849 6788196
 * @summary Check for correct Array Bounds check in read of FileInputStream and
 * RandomAccessFile
 */

import java.io.*;

/*
 * The test calls the read(byte buf[] , int off , int len) of
 * FileInputStream with different values of off and len to see if the
 * IndexOutOfBoundsException is thrown.  The read(...) method calls
 * readBytes(...) in native code(io_util.c).  The read(...) method in
 * RandomAccessFile also calls the same native method.  So one should
 * see similar results.
 */

public class ReadBytesBounds {

    static final FileInputStream fis;
    static final RandomAccessFile raf;
    static final byte[] b = new byte[32];

    static {
        try {
            String dir = System.getProperty("test.src", ".");
            File testFile = new File(dir, "input.txt");
            fis = new FileInputStream(testFile);
            raf = new RandomAccessFile(testFile , "r");
        } catch (Throwable t) {
            throw new Error(t);
        }
    }

    public static void main(String argv[]) throws Throwable {
        try {
            testRead(-1, -1, false);
            testRead(-1,  0, false);
            testRead( 0, -1, false);
            testRead( 0, 33, false);
            testRead(33,  0, false);
            testRead(33,  4, false);
            testRead( 0, 32, true);
            testRead(32,  0, true);
            testRead(32,  4, false);
            testRead( 4, 16, true);
            testRead( 1, 31, true);
            testRead( 0,  0, true);
            testRead(31,  Integer.MAX_VALUE, false);
            testRead( 0,  Integer.MAX_VALUE, false);
            testRead(-1,  Integer.MAX_VALUE, false);
            testRead(-4,  Integer.MIN_VALUE, false);
            testRead( 0,  Integer.MIN_VALUE, false);
        } finally {
            fis.close();
            raf.close();
        }
    }

    static void testRead(int off, int len, boolean expected) throws Throwable {
        System.err.printf("off=%d len=%d expected=%b%n", off, len, expected);
        boolean result;
        try {
            fis.read(b, off, len);
            raf.read(b, off, len);
            result = true;
        } catch (IndexOutOfBoundsException e) {
            result = false;
        }

        if (result != expected) {
            throw new RuntimeException
                (String.format("Unexpected result off=%d len=%d expected=%b",
                               off, len, expected));
        }
    }
}
