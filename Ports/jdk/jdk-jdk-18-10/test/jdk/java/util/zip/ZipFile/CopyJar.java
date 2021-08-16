/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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

/* @test 1.1 99/06/01
   @bug 4239446
   @summary Make sure the ZipEntry fields are correct.
 */

import java.io.*;
import java.util.zip.*;

public class CopyJar {
    public static void main(String args[]) throws Exception {
        try (ZipFile zf = new ZipFile(new File(System.getProperty("test.src", "."),
                                               "input.jar"))) {
            ZipEntry ze = zf.getEntry("ReleaseInflater.java");
            ZipOutputStream zos = new ZipOutputStream(new ByteArrayOutputStream());
            InputStream in = zf.getInputStream(ze);
            byte[] b = new byte[128];
            int n;
            zos.putNextEntry(ze);
            while((n = in.read(b)) != -1) {
                zos.write(b, 0, n);
            }
            zos.close();
        }
    }
}
