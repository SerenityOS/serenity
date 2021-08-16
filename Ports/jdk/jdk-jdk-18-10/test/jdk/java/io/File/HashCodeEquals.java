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
   @bug 4141318
   @summary Check that equal File instances have identical hash codes
 */

import java.io.File;


public class HashCodeEquals {

    static void test(String fn1, String fn2) throws Exception {
        File f1 = new File(fn1);
        File f2 = new File(fn2);
        if (!f1.equals(f2))
            throw new Exception("Instances with equal paths are not equal");
        int h1 = f1.hashCode();
        int h2 = f2.hashCode();
        if (h1 != h2)
            throw new Exception("Hashcodes of equal instances are not equal");
    }

    static void testWin32() throws Exception {
        test("a:/foo/bar/baz", "a:/foo/bar/baz");
        test("A:/Foo/Bar/BAZ", "a:/foo/bar/baz");
    }

    static void testUnix() throws Exception {
        test("foo/bar/baz", "foo/bar/baz");
    }

    public static void main(String[] args) throws Exception {
        if (File.separatorChar == '\\') testWin32();
        if (File.separatorChar == '/') testUnix();
    }

}
