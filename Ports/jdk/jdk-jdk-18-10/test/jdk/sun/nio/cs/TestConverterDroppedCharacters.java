/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4122961
 * @summary Verify that converters don't drop characters on buffer boundaries

 * This is a slightly modified version of the attachment supplied with the
 * bug report.
 * @modules jdk.charsets
 */
import java.io.*;

public class TestConverterDroppedCharacters {
    public static void main(String args[])
        throws java.io.IOException, java.io.UnsupportedEncodingException,
               java.io.FileNotFoundException
    {
        /* Try misc. encodings, many are broken. */
        tryEncoding("Big5");
        tryEncoding("CNS11643");
        tryEncoding("Cp1006");
        tryEncoding("Cp1381");
        tryEncoding("Cp33722");
        tryEncoding("GB2312");
        tryEncoding("KSC5601");
        tryEncoding("SJIS");
        tryEncoding("UTF8");
    }

    static void tryEncoding(String encoding)
        throws java.io.IOException, java.io.UnsupportedEncodingException,
               java.io.FileNotFoundException
    {
        String filename = "OUTPUT";
        int goesBadAfter = 8193;
        int i;
        char data[] = new char[goesBadAfter+1];

        System.out.println("Testing " + encoding);

        /* Create some data */
        for(i = 0; i < goesBadAfter; i++) {
            data[i] = (char)((i % 0x7f) + 1);
        }

        /* Write the data out to a file. */
        FileOutputStream fout = new FileOutputStream(filename);
        OutputStreamWriter ow = new OutputStreamWriter(fout, encoding);
        BufferedWriter fd     = new BufferedWriter(ow);
        fd.write(data,0,goesBadAfter);
        fd.close();

        /* Now read it back with the same encoding. */
        char buf[] = new char[goesBadAfter+1];
        FileInputStream fin = new FileInputStream("OUTPUT");
        InputStreamReader ir = new InputStreamReader(fin, encoding);
        ir.read(buf,0,goesBadAfter);
        ir.close();

        /* And check to see if what we wrote is what we got back. */
        for(i = 0; i < goesBadAfter; i++) {
            if (data[i] != buf[i]) {
                System.out.println("ERROR with encoding " + encoding
                                   + ": Data wrong at position " + i + "   "
                                   + "in: " + (int)data[i] + "   "
                                   + "out: " + (int)buf[i]);
                throw new RuntimeException();
            }
        }
        System.out.println("Successfully tested " + encoding);
    }
}
