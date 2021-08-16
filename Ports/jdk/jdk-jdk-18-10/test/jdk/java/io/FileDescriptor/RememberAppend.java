/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8023173
 * @summary FileDescriptor should respect append flag
 */

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;

public class RememberAppend {
    private static final byte[] bytes = "ABC ".getBytes();

    public static void main(String[] args) throws Throwable {
        File f = File.createTempFile("tmp.file", null);
        f.deleteOnExit();

        try (FileOutputStream fos1 = new FileOutputStream(f.getPath(), true)) {
            fos1.write(bytes);
        }

        try (FileOutputStream fos1 = new FileOutputStream(f.getPath(), true);
             FileOutputStream fos2 = new FileOutputStream(fos1.getFD())) {
            fos2.write(bytes);
        }

        if (f.length() != 2 * bytes.length) {
            throw new RuntimeException("Append flag ignored");
        }
    }
}
