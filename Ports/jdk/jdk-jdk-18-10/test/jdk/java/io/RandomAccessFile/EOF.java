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
   @bug 4017497
   @summary Check that read returns -1 on EOF, as specified
 */

import java.io.*;

public class EOF {

    public static void main(String[] args) throws IOException {
        byte buf[] = new byte[100];
        int n;
        String dir = System.getProperty("test.src", ".");
        RandomAccessFile raf = new RandomAccessFile(new File(dir, "EOF.java"), "r");
        try {
            for (;;) {
                n = raf.read(buf, 0, buf.length);
                if (n <= 0) break;
            }
            if (n != -1)
                throw new RuntimeException("Expected -1 for EOF, got " + n);
        } finally {
            raf.close();
        }
    }

}
