/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8187898
 * @summary Test of writeBytes(byte[])
 */

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class WriteBytes {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream out = new BufferedOutputStream(baos, 512);
        PrintStream ps = new PrintStream(out, false);

        byte[] buf = new byte[128];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte)i;
        }

        ps.writeBytes(buf);
        assertTrue(baos.size() == 0, "Buffer should not have been flushed");
        ps.close();
        assertTrue(baos.size() == buf.length, "Stream size " + baos.size() +
            " but expected " + buf.length);

        ps = new PrintStream(out, true);
        ps.writeBytes(buf);
        assertTrue(baos.size() == 2*buf.length, "Stream size " + baos.size() +
            " but expected " + 2*buf.length);

        byte[] arr = baos.toByteArray();
        assertTrue(arr.length == 2*buf.length, "Array length " + arr.length +
            " but expected " + 2*buf.length);
        assertTrue(Arrays.equals(buf, 0, buf.length, arr, 0, buf.length),
            "First write not equal");
        assertTrue(Arrays.equals(buf, 0, buf.length, arr, buf.length,
            2*buf.length), "Second write not equal");

        ps.close();
        ps.writeBytes(buf);
        assertTrue(ps.checkError(), "Error condition should be true");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition)
            throw new RuntimeException(msg);
    }
}
