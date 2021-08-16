/*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4101707
   @summary Test if fill() will behave correctly at EOF to
            allow file to grow.
*/

import java.io.*;

public class GrowAfterEOF {
    public static void main(String[] args) throws Exception {
        File input = new File(".", "TestEOFInput.txt");
        RandomAccessFile rf = new RandomAccessFile(input, "rw");
        try {
            BufferedReader r = new BufferedReader
                (new InputStreamReader(new FileInputStream(input)));
            try {
                // write something
                rf.writeBytes("a line");

                // read till the end of file
                while (r.readLine() != null);

                // append to the end of the file
                rf.seek(rf.length());
                rf.writeBytes("new line");

                // now try to read again
                boolean readMore = false;
                while (r.readLine() != null) {
                    readMore = true;
                }
                if (!readMore) {
                    input.delete();
                    throw new Exception("Failed test: unable to read!");
                } else {
                    input.delete();
                }
            } finally {
                r.close();
            }
        } finally {
            rf.close();
        }
    }
}
