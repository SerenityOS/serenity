/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8204310
 * @summary General tests of the setLength method
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main SetLength
 * @key randomness
 */

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;

import jdk.test.lib.RandomFactory;

public class SetLength {

    static void checkState(RandomAccessFile f, long expectedFilePointer,
            long expectedLength)
        throws IOException
    {
        long filePointer = f.getFilePointer();
        long length = f.length();
        if (length != expectedLength) {
            throw new RuntimeException("File length " + length + " != expected "
                    + expectedLength);
        }
        if (filePointer != expectedFilePointer) {
            throw new RuntimeException("File pointer " + filePointer
                    + " != expected " + expectedFilePointer);
        }
    }

    static void test(RandomAccessFile f, long quarterLength)
        throws IOException
    {
        long halfLength = 2 * quarterLength;
        long threeQuarterLength = 3 * quarterLength;
        long fullLength = 4 * quarterLength;

        // initially, empty file
        checkState(f, 0, 0);

        // extending the file size
        f.setLength(halfLength);
        checkState(f, 0, halfLength);

        // writing from the begining
        f.write(new byte[(int)fullLength]);
        checkState(f, fullLength, fullLength);

        // setting to the same length
        f.setLength(fullLength);
        checkState(f, fullLength, fullLength);

        // truncating the file
        f.setLength(threeQuarterLength);
        checkState(f, threeQuarterLength, threeQuarterLength);

        // changing the file pointer
        f.seek(quarterLength);
        checkState(f, quarterLength, threeQuarterLength);

        // truncating the file again
        f.setLength(halfLength);
        checkState(f, quarterLength, halfLength);

        // writing from the middle with extending the file
        f.write(new byte[(int)halfLength]);
        checkState(f, threeQuarterLength, threeQuarterLength);

        // changing the file pointer
        f.seek(quarterLength);
        checkState(f, quarterLength, threeQuarterLength);

        // writing from the middle without extending the file
        f.write(new byte[(int)quarterLength]);
        checkState(f, halfLength, threeQuarterLength);

        // changing the file pointer to the end of file
        f.seek(threeQuarterLength);
        checkState(f, threeQuarterLength, threeQuarterLength);

        // writing to the end of file
        f.write(new byte[(int)quarterLength]);
        checkState(f, fullLength, fullLength);

        // truncating the file to zero
        f.setLength(0);
        checkState(f, 0, 0);

        // changing the file pointer beyond the end of file
        f.seek(threeQuarterLength);
        checkState(f, threeQuarterLength, 0);

        // writing beyont the end of file
        f.write(new byte[(int)quarterLength]);
        checkState(f, fullLength, fullLength);

        // negative file pointer
        try {
            f.seek(-1);
            throw new RuntimeException("IOE not thrown");
        } catch (IOException expected) {
        }
        checkState(f, fullLength, fullLength);

        // truncating the file after failed seek
        f.setLength(halfLength);
        checkState(f, halfLength, halfLength);

        // truncating after closing
        f.close();
        try {
            f.setLength(halfLength);
            throw new RuntimeException("IOE not thrown");
        } catch (IOException expected) {
        }
    }

    public static void main(String[] args) throws IOException {
        File f28b = new File("f28b");
        File f28K = new File("f28K");
        File frnd = new File("frnd");

        try (RandomAccessFile raf28b = new RandomAccessFile(f28b, "rw");
             RandomAccessFile raf28K = new RandomAccessFile(f28K, "rw");
             RandomAccessFile rafrnd = new RandomAccessFile(frnd, "rw")) {
            test(raf28b, 7);
            test(raf28K, 7 * 1024);
            test(rafrnd, 1 + RandomFactory.getRandom().nextInt(16000));
        }

        // truncating read-only file
        try (RandomAccessFile raf28b = new RandomAccessFile(f28b, "r")) {
            raf28b.setLength(42);
            throw new RuntimeException("IOE not thrown");
        } catch (IOException expected) {
        }
    }

}
