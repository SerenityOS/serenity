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

/* @test
   @bug 4074388
   @summary Check for correct implementation of RandomAccessFile.writeBytes
   and writeChars.
   */

import java.io.*;

public class WriteBytesChars {

    public static void main(String args[]) throws Exception {
        String towrite;
        char[] buf = new char[80];
        byte[] b = new byte[80];
        File fn = new File("x.WriteBytesChars");

        RandomAccessFile raf = new RandomAccessFile(fn , "rw");;
        try {
            for (int i = 0; i < 80; i++) {
                buf[i] = 'a';
            }
            towrite = new String(buf);

            raf.writeBytes(towrite);
            raf.seek(0);
            raf.read(b);

            System.out.println("RandomAccessFile.writeBytes");
            if (towrite.equals(new String(b))) {
                System.err.println("Test succeeded.");
            } else {
                throw new
                    RuntimeException("RandomAccessFile.writeBytes, wrong result");
            }

            raf.seek(0);
            raf.writeChars(towrite);
            raf.seek(0);
            for (int i = 0; i < 80; i++) {
                buf[i] = raf.readChar();
            }

            System.out.println("RandomAccessFile.writeChars");
            if (towrite.equals(new String(buf))) {
                System.err.println("Test succeeded.");
            } else {
                throw new
                    RuntimeException("RandomAccessFile.writeChars, wrong result");
            }
        } finally {
            raf.close();
            fn.delete();
        }
    }
}
