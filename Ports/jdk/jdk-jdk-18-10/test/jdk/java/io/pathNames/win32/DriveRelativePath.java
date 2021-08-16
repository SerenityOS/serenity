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
   @bug 4070044 4164823
   @summary Check getCanonicalPath's treatment of drive-relative paths (win32)
 */

import java.io.*;


public class DriveRelativePath {

    static void fail(String s) {
        throw new RuntimeException(s);
    }

    public static void main(String[] args) throws IOException {

        if (File.separatorChar != '\\') {
            /* This test is only valid on win32 systems */
            return;
        }

        File f = new File("foo");
        String c = f.getCanonicalPath();
        System.err.println(c);

        int di = c.indexOf(':');
        if (di == -1) fail("No drive in canonical path");
        String drive = c.substring(0, di + 1);
        File f2 = new File(drive + "foo");
        System.err.println(f2);
        String c2 = f2.getCanonicalPath();
        System.err.println(c2);
        if (!c2.equals(c)) fail("Canonical path mismatch: \""
                                + f2 + "\" maps to \""
                                + c2 + "\"; it should map to \""
                                + c + "\"");

    }

}
