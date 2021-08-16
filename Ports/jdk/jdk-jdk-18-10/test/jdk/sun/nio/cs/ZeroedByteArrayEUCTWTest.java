/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4522270
   @summary Ensure that a zeroed byte array produces a valid String when EUC-TW  decoded

 */

public class ZeroedByteArrayEUCTWTest
{
    public static void main(String[] args) throws Exception {
        test("cns11643");
    }

    public static void test(String encoding) throws Exception {
        String result = null;
        byte[] data = new byte[16];

        for (int i = 0; i < 16; i++) {
            data[i] = 0;
        }

        result = new String(data, encoding);
        if (result.length() != 16)
            throw new Exception ("EUC_TW regression test bugID 4522270 failed");

        for (int i=0; i < 16; i++) {
            data[i] = (byte)( 32 + i);
        }
    }
}
