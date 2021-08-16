/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4130498 4391178 6198547
   @summary Basic test for createNewFile method
 */

import java.io.*;


public class CreateNewFile {

    public static void main(String[] args) throws Exception {

        File f = new File(System.getProperty("test.dir", "."),
                          "x.CreateNewFile");
        if (f.exists() && !f.delete())
            throw new Exception("Cannot delete test file " + f);
        if (!f.createNewFile())
            throw new Exception("Cannot create new file " + f);
        if (!f.exists())
            throw new Exception("Did not create new file " + f);
        if (f.createNewFile())
            throw new Exception("Created existing file " + f);

        try {
            f = new File("/");
            if (f.createNewFile())
                throw new Exception("Created root directory!");
        } catch (IOException e) {
            // Exception expected
        }

        testCreateExistingDir();
    }

    // Test JDK-6198547
    private static void testCreateExistingDir() throws IOException {
        File tmpFile = new File("hugo");
        if (tmpFile.exists() && !tmpFile.delete())
            throw new RuntimeException("Cannot delete " + tmpFile);
        if (!tmpFile.mkdir())
            throw new RuntimeException("Cannot create dir " + tmpFile);
        if (!tmpFile.exists())
            throw new RuntimeException("Cannot see created dir " + tmpFile);
        if (tmpFile.createNewFile())
            throw new RuntimeException("Should fail to create file " + tmpFile);
    }
}
