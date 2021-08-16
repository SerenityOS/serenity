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

import java.io.File;
import java.io.FilePermission;
import java.util.Arrays;
import java.util.List;

/*
 * @test
 * @bug 8156054
 * @summary Test some of FilePermission methods when canonicalization property
 *          set and un-set.
 * @run main/othervm -Djdk.io.permissionsUseCanonicalPath=true
 *      FilePermissionTest truetruetruetruetruetrue
 * @run main/othervm -Djdk.io.permissionsUseCanonicalPath=false
 *      FilePermissionTest falsefalsefalsefalsefalsefalse
 * @run main FilePermissionTest falsefalsefalsefalsefalsefalse
 */
public class FilePermissionTest {

    public static void main(String[] args) throws Exception {

        final File realFile = new File("exist.file");
        try {
            if (!realFile.createNewFile()) {
                throw new RuntimeException("Unable to create a file.");
            }
            check(Arrays.asList(realFile.getName(), "notexist.file"), args[0]);
        } finally {
            if (realFile.exists()) {
                realFile.delete();
            }
        }
    }

    private static void check(List<String> files, String expected) {

        StringBuilder actual = new StringBuilder();
        files.forEach(f -> {
            StringBuilder result = new StringBuilder();
            FilePermission fp1 = new FilePermission(f, "read");
            FilePermission fp2 = new FilePermission(
                    new File(f).getAbsolutePath(), "read");
            result.append(fp1.equals(fp2));
            result.append(fp1.implies(fp2));
            result.append(fp1.hashCode() == fp2.hashCode());
            System.out.println(fp1 + " Vs. " + fp2 + " : Result: " + result);
            actual.append(result);
        });
        if (!expected.equals(actual.toString())) {
            throw new RuntimeException("Failed: " + expected + "/" + actual);
        }
    }
}
