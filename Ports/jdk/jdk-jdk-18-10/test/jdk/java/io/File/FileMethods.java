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
   @bug 4107063 4131237
   @summary Basic test for new File-returning methods
 */

import java.io.*;


public class FileMethods {

    private static void ck(String op, File got, File ans) throws Exception {
        if (!got.equals(ans))
            throw new Exception(op + " incorrect");
    }

    private static void ck(String op, File f, String[] ls, File[] lf)
        throws Exception
    {
        System.err.println("--- " + op);
        int n = lf.length;
        if (ls.length != n)
            throw new Exception("listFiles returned incorrect count");
        for (int i = 0; i < n; i++) {
            if (ls[i].equals(lf[i].getName())
                && lf[i].getParentFile().equals(f)) {
                System.err.println(ls[i] + " ==> " + lf[i]);
            } else {
                throw new Exception("list mismatch: " + ls[i] + ", " + lf[i]);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        File f;
        f = new File("foo/bar");
        ck("getParentFile", f.getParentFile(), new File(f.getParent()));

        f = new File(".");
        ck("getAbsoluteFile",
           f.getAbsoluteFile(), new File(f.getAbsolutePath()));

        ck("getCanonicalFile",
           f.getCanonicalFile(), new File(f.getCanonicalPath()));

        f = f.getCanonicalFile();
        ck("listFiles", f, f.list(), f.listFiles());

        FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".class");
            }};
        ck("listFiles/filtered", f, f.list(ff), f.listFiles(ff));

        FileFilter ff2 = new FileFilter() {
            public boolean accept(File f) {
                return f.getPath().endsWith(".class");
            }};
        ck("listFiles/filtered2", f, f.list(ff), f.listFiles(ff2));

    }

}
