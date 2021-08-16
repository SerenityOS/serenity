/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4027914
   @summary Check getParent's handling of root directories
 */

import java.io.File;


public class GetParent {

    static void check(String path, String[] parents) throws Exception {
        File f = new File(path);
        String p;
        System.err.print(path + ":");
        for (int i = 0; i < parents.length; i++) {
            p = f.getParent();
            System.err.print(" (" + p + ")");
            if (p == null)
                throw new Exception("No parent for " + f);
            if (! p.equals(parents[i]))
                throw new Exception("Wrong parent for " + f
                                    + ": Expected " + parents[i]
                                    + ", got " + p);
            f = new File(p);
        }
        if (f.getParent() != null)
            throw new Exception("Too many parents for " + path);
        System.err.println();
    }

    static void testUnix() throws Exception {
        check("foo", new String[] { });
        check("./foo", new String[] { "." });
        check("foo/bar/baz", new String[] { "foo/bar", "foo" });
        check("../../foo", new String[] { "../..", ".." });
        check("foo//bar", new String[] { "foo" });
        check("/foo/bar/baz.gorp",
              new String[] { "/foo/bar", "/foo", "/" });
    }

    static void testWin32() throws Exception {
        System.err.println("Win32");
        check("foo", new String[] { });
        check(".\\foo", new String[] { "." });
        check("foo\\bar\\baz", new String[] { "foo\\bar", "foo" });
        check("..\\..\\foo", new String[] { "..\\..", ".." });
        check("c:\\foo\\bar\\baz.xxx",
              new String[] { "c:\\foo\\bar", "c:\\foo", "c:\\" });
    }

    public static void main(String[] args) throws Exception {
        if (File.separatorChar == '/') testUnix();
        if (File.separatorChar == '\\') testWin32();
    }

}
