/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4260284 8219196 8223254
 * @summary Test if DataOutputStream will overcount written field.
 * @requires (sun.arch.data.model == "64" & os.maxMemory >= 4g)
 * @run testng/othervm -Xmx4g WriteUTF
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import org.testng.annotations.Test;

public class WriteUTF {
    @Test
    public static void overcountWrittenField() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF("Hello, World!");  // 15
        dos.flush();
        if  (baos.size() != dos.size())
            throw new RuntimeException("Miscounted bytes in DataOutputStream.");
    }

    private static void writeUTF(int size) throws IOException {
        // this character gives 3 bytes when encoded using UTF-8
        String s = "\u0800".repeat(size);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF(s);
    }

    @Test(expectedExceptions = UTFDataFormatException.class)
    public void utfDataFormatException() throws IOException {
        writeUTF(1 << 16);
    }

    // Without 8219196 fix, throws ArrayIndexOutOfBoundsException instead of
    // expected UTFDataFormatException. Requires 4GB of heap (-Xmx4g) to run
    // without throwing an OutOfMemoryError.
    @Test(expectedExceptions = UTFDataFormatException.class)
    public void arrayIndexOutOfBoundsException() throws IOException {
        writeUTF(Integer.MAX_VALUE / 3 + 1);
    }
}
