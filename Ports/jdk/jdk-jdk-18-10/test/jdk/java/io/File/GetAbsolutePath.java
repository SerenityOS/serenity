/*
 * Copyright (c) 1998, 2001, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4131169 4109131
   @summary Basic test for getAbsolutePath method
 */

import java.io.*;


public class GetAbsolutePath {

    private static boolean ignoreCase = false;

    private static void ck(String path, String ans) throws Exception {
        File f = new File(path);
        String p = f.getAbsolutePath();
        if ((ignoreCase && p.equalsIgnoreCase(ans)) || p.equals(ans))
            System.err.println(path + " ==> " + p);
        else
            throw new Exception(path + ": expected " + ans + ", got " + p);
    }

    private static void testWin32() throws Exception {
        String wd = System.getProperty("user.dir");
        char d;
        if ((wd.length() > 2) && (wd.charAt(1) == ':')
            && (wd.charAt(2) == '\\'))
            d = wd.charAt(0);
        else
            throw new Exception("Current directory has no drive");
        ck("/foo/bar", d + ":\\foo\\bar");
        ck("\\foo\\bar", d + ":\\foo\\bar");
        ck("c:\\foo\\bar", "c:\\foo\\bar");
        ck("c:/foo/bar", "c:\\foo\\bar");
        ck("\\\\foo\\bar", "\\\\foo\\bar");

        /* Tricky directory-relative case */
        d = Character.toLowerCase(d);
        char z = 0;
        if (d != 'c') z = 'c';
        else if (d != 'd') z = 'd';
        if (z != 0) {
            File f = new File(z + ":.");
            if (f.exists()) {
                String zwd = f.getCanonicalPath();
                ck(z + ":foo", zwd + "\\foo");
            }
        }

        /* Empty path */
        ck("", wd);
    }

    private static void testUnix() throws Exception {
        String wd = System.getProperty("user.dir");
        ck("foo", wd + "/foo");
        ck("foo/bar", wd + "/foo/bar");
        ck("/foo", "/foo");
        ck("/foo/bar", "/foo/bar");

        /* Empty path */
        ck("", wd);
    }

    public static void main(String[] args) throws Exception {
        if (File.separatorChar == '\\') {
            ignoreCase = true;
            testWin32();
        }
        if (File.separatorChar == '/') testUnix();
    }

}
