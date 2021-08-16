/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @test
 * @bug 8164705
 * @modules java.base/java.io:open
 * @summary Remove pathname canonicalization from FilePermission
 */

import java.io.FilePermission;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Correctness {

    static boolean err = false;
    static Method containsMethod;
    static boolean isWindows =
            System.getProperty("os.name").contains("Windows");
    public static void main(String args[]) throws Exception {
        check("/", "/");
        checkNo("/", "/x");
        checkNo("/", "/../x");

        checkNo("/", "x");

        check("/-", "/*");
        checkNo("/*", "/-");

        check("/*", "/x");
        check("/-", "/x");
        check("/-", "/x/*");
        check("/-", "/x/-");
        check("/-", "/x/y");
        checkNo("/*", "/x/y");
        check("/x/*", "/x/x");
        checkNo("/x/-", "/x");
        checkNo("/x/*", "/x");
        check("/x/-", "/x/x");
        check("/x/-", "/x/x/y");
        checkNo("/x/*", "/x/x/y");
        checkNo("/x/*", "/x");

        check("*", "x");
        checkNo("", "x");
        check("-", "x");
        check("-", "*");
        check("-", "a/-");
        check("-", "a/*");
        checkNo("*", "a/b");
        check("a/*", "a/b");
        check("a/-", "a/*");
        check("a/-", "a/b/c");
        checkNo("a/*", "a/b/c");

        check("../", "../");
        check("../-", "../*");
        check("../../*", "../../a");

        // If we allow .. and abs/rel checks
        check("../-", "a");
        check("../../-", "-");
        checkNo("../../*", "a");
        //check("/-", "a");
        //checkNo("/*", "a");
        //check("/-", "-");

        try {
            containsMethod = FilePermission.class.getDeclaredMethod(
                    "containsPath", Path.class, Path.class);
            containsMethod.setAccessible(true);
            System.out.println();

            // The 1st 2 args of contains() must be normalized paths.
            // When FilePermission::containsPath is called by implies,
            // paths have already been normalized.
            contains("x", "x", 0);
            contains("x", "x/y", 1);
            contains("x", "x/y/z", 2);
            contains("x", "y", -1);
            contains("x", "", -1);
            contains("", "", 0);
            contains("", "x", 1);
            contains("", "x/y", 2);
            contains("/", "/", 0);
            contains("/", "/x", 1);
            contains("/", "/x/y", 2);
            contains("/x", "/x/y", 1);
            contains("/x", "/y", -1);
            //contains("/", "..", Integer.MAX_VALUE);
            //contains("/", "x", Integer.MAX_VALUE);
            //contains("/", "x/y", Integer.MAX_VALUE);
            //contains("/", "../x", Integer.MAX_VALUE);
            contains("/x", "y", -1);
            contains("x", "/y", -1);

            contains("", "..", -1);
            contains("", "../x", -1);
            contains("..", "", 1);
            contains("..", "x", 2);
            contains("..", "x/y", 3);
            contains("../x", "x", -1);
            contains("../x", "y", -1);
            contains("../x", "../x/y", 1);
            contains("../../x", "../../x/y", 1);
            contains("../../../x", "../../../x/y", 1);
            contains("../x", "../y", -1);
        } catch (NoSuchMethodException e) {
            // Ignored
        }
        if (err) throw new Exception("Failed.");
    }

    // Checks if s2 is inside s1 and depth is expected.
    static void contains(String s1, String s2, int expected) throws Exception {
        contains0(s1, s2, expected);
        if (isWindows) {
            contains0("C:" + s1, s2, -1);
            contains0(s1, "C:" + s2, -1);
            contains0("C:" + s1, "D:" + s2, -1);
            contains0("C:" + s1, "C:" + s2, expected);
        }
    }

    static void contains0(String s1, String s2, int expected) throws Exception {
        Path p1 = Paths.get(s1);
        Path p2 = Paths.get(s2);
        int d = (int)containsMethod.invoke(null, p1, p2);
        Path p;
        try {
            p = p2.relativize(p1);
        } catch (Exception e) {
            p = null;
        }
        System.out.printf("%-20s -> %-20s: %20s %5d %5d %s\n", s1, s2, p,
                d, expected, d==expected?"":" WRONG");
        if (d != expected) {
            err = true;
        }
    }

    static void check0(String s1, String s2, boolean expected) {
        FilePermission fp1 = new FilePermission(s1, "read");
        FilePermission fp2 = new FilePermission(s2, "read");
        boolean b = fp1.implies(fp2);
        System.out.printf("%-30s -> %-30s: %5b %s\n",
                s1, s2, b, b==expected?"":" WRONG");
        if (b != expected) {
            err = true;
            System.out.println(fp1);
            System.out.println(fp2);
        }
    }

    static void check(String s1, String s2, boolean expected) {
        check0(s1, s2, expected);
        if (isWindows) {
            check0("C:" + s1, s2, false);
            check0(s1, "C:" + s2, false);
            check0("C:" + s1, "D:" + s2, false);
            check0("C:" + s1, "C:" + s2, expected);
        }
    }

    static void check(String s1, String s2) {
        check(s1, s2, true);
    }

    static void checkNo(String s1, String s2) {
        check(s1, s2, false);
    }
}
