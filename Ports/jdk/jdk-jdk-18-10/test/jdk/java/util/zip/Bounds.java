/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4811913 6894950
 * @summary Test bounds checking in zip package
 */

import java.util.zip.*;

public class Bounds {
    public static void main(String[] args) throws Exception {
        byte[] b = new byte[0];
        int offset = 4;
        int length =  Integer.MAX_VALUE - 3;

        try {
            (new CRC32()).update(b, offset, length);
            throw new RuntimeException("Expected exception not thrown");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // Correct result
        }
        try {
            (new Deflater()).setDictionary(b, offset, length);
            throw new RuntimeException("Expected exception not thrown");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // Correct result
        }
        try {
            (new Adler32()).update(b, offset, length);
            throw new RuntimeException("Expected exception not thrown");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // Correct result
        }
        try {
            (new Deflater()).deflate(b, offset, length);
            throw new RuntimeException("Expected exception not thrown");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // Correct result
        }
        try {
            (new Inflater()).inflate(b, offset, length);
            throw new RuntimeException("Expected exception not thrown");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // Correct result
        }
    }
}
