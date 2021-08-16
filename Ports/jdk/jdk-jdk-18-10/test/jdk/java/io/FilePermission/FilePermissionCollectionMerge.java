/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168127
 * @summary FilePermissionCollection merges incorrectly
 * @modules java.base/sun.security.util
 * @library /test/lib
 * @build jdk.test.lib.Asserts
 * @run main FilePermissionCollectionMerge
 */

import sun.security.util.FilePermCompat;
import java.io.FilePermission;
import java.security.Permissions;
import jdk.test.lib.Asserts;

public class FilePermissionCollectionMerge {

    public static void main(String[] args) throws Exception {
        test("x");
        test("x/*");
        test("x/-");
        test("*");
        test("-");
        test("/x");
        test("/x/*");
        test("/x/-");
    }

    static void test(String arg) {

        FilePermission fp1 = new FilePermission(arg, "read");
        FilePermission fp2 = (FilePermission)
                FilePermCompat.newPermUsingAltPath(fp1);
        FilePermission fp3 = (FilePermission)
                FilePermCompat.newPermPlusAltPath(fp1);

        // All 3 are different
        Asserts.assertNE(fp1, fp2);
        Asserts.assertNE(fp1.hashCode(), fp2.hashCode());

        Asserts.assertNE(fp1, fp3);
        Asserts.assertNE(fp1.hashCode(), fp3.hashCode());

        Asserts.assertNE(fp2, fp3);
        Asserts.assertNE(fp2.hashCode(), fp3.hashCode());

        // The plus one implies the other 2
        Asserts.assertTrue(fp3.implies(fp1));
        Asserts.assertTrue(fp3.implies(fp2));

        // The using one different from original
        Asserts.assertFalse(fp2.implies(fp1));
        Asserts.assertFalse(fp1.implies(fp2));

        // FilePermssionCollection::implies always works
        testMerge(fp1);
        testMerge(fp2);
        testMerge(fp3);
        testMerge(fp1, fp2);
        testMerge(fp1, fp3);
        testMerge(fp2, fp1);
        testMerge(fp2, fp3);
        testMerge(fp3, fp1);
        testMerge(fp3, fp2);
        testMerge(fp1, fp2, fp3);
        testMerge(fp2, fp3, fp1);
        testMerge(fp3, fp1, fp2);
    }

    // Add all into a collection, and check if it implies the last one.
    static void testMerge(FilePermission... fps) {
        java.security.Permissions perms = new Permissions();
        FilePermission last = null;
        for (FilePermission fp : fps) {
            perms.add(fp);
            last = fp;
        }
        Asserts.assertTrue(perms.implies(last));
    }
}
