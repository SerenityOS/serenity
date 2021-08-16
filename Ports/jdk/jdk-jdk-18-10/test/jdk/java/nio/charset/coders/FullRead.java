/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Ensure that InputStreamReader reads as many characters as possible
 */

// InputStreamReader is not required by its spec to read as many characters as
// possible upon each invocation of read(char[], int, int), but many programs
// (e.g., javac) depend upon this behavior.

import java.io.*;


public class FullRead {

    static int MAX_LEN = 1 << 16;

    static void test(File f, int len) throws Exception {
        FileOutputStream fo = new FileOutputStream(f);
        for (int i = 0; i < len; i++)
            fo.write('x');
        fo.close();

        FileInputStream fi = new FileInputStream(f);
        Reader rd = new InputStreamReader(fi, "US-ASCII");
        char[] cb = new char[MAX_LEN + 100];
        int n = rd.read(cb, 0, cb.length);
        System.out.println(len + " : " + n);
        if (len != n)
            throw new Exception("Expected " + len + ", read " + n);
    }

    public static void main(String[] args) throws Exception {
        File f = File.createTempFile("foo", "bar");
        f.deleteOnExit();
        System.out.println(f);

        for (int i = 4; i <= MAX_LEN; i <<= 1)
            test(f, i);
    }

}
