/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4290060
   @summary Check if the zip file is closed before access any
            elements in the Enumeration.
 */

import java.io.*;
import java.util.zip.*;
import java.util.Enumeration;

public class EnumAfterClose {
    public static void main(String args[]) throws Exception {
        Enumeration e;
        try (ZipFile zf = new ZipFile(new File(System.getProperty("test.src", "."),
                                               "input.zip"))) {
            e = zf.entries();
        }
        // ensure that the ZipFile is closed before checking the Enumeration
        try {
            if (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry)e.nextElement();
            }
        } catch (IllegalStateException ie) {
        }
    }
}
