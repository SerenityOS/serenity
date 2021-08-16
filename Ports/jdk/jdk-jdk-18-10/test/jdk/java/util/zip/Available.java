/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4028605 4109069 4234207 4401122
   @summary Make sure ZipInputStream/InflaterInputStream.available() will
            return 0 after EOF has reached and 1 otherwise.
   */


import java.io.*;
import java.util.zip.*;

public class Available {

    public static void main(String[] args) throws Exception {
        // 4028605 4109069 4234207
        test1();
        // test 4401122
        test2();
    }

    private static void test1() throws Exception {
        File f = new File(System.getProperty("test.src", "."), "input.jar");

        // test ZipInputStream
        try (FileInputStream fis = new FileInputStream(f);
             ZipInputStream z = new ZipInputStream(fis))
        {
            z.getNextEntry();
            tryAvail(z);
        }

        // test InflaterInputStream
        try (ZipFile zfile = new ZipFile(f)) {
            tryAvail(zfile.getInputStream(zfile.getEntry("Available.java")));
        }
    }

    static void tryAvail(InputStream in) throws Exception {
        byte[] buf = new byte[1024];
        int n;

        while ((n = in.read(buf)) != -1);
        if (in.available() != 0) {
            throw new Exception("available should return 0 after EOF");
        }
    }

    // To reproduce 4401122
    private static void test2() throws Exception {
        File f = new File(System.getProperty("test.src", "."), "input.jar");
        try (ZipFile zf = new ZipFile(f)) {
            InputStream in = zf.getInputStream(zf.getEntry("Available.java"));

            int initialAvailable = in.available();
            in.read();
            if (in.available() != initialAvailable - 1)
                throw new RuntimeException("Available not decremented.");
            for(int j=0; j<initialAvailable-1; j++)
                in.read();
            if (in.available() != 0)
                throw new RuntimeException();
            in.close();
            if (in.available() != 0)
                throw new RuntimeException();
        }
    }

}
