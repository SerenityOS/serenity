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
 * @bug 4771562
 * @summary Verify that if ObjectInputStream.read(byte[], int, int) is called
 *          with a null byte array and invalid offset/length values, a
 *          NullPointerException is thrown rather than an
 *          IndexOutOfBoundsException.
 */

import java.io.*;

public class Test {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        byte[] b = new byte[10];
        int[][] badBounds =
            { { -1, -1}, { -1, 5 }, { 5, -1 }, { 100, 5 }, { 5, 100 } };

        for (int i = 0; i < badBounds.length; i++) {
            try {
                oout.write(null, badBounds[i][0], badBounds[i][1]);
                throw new Error();
            } catch (NullPointerException e) {
            }
        }
        for (int i = 0; i < badBounds.length; i++) {
            try {
                oout.write(b, badBounds[i][0], badBounds[i][1]);
                throw new Error();
            } catch (IndexOutOfBoundsException e) {
            }
        }
        oout.write(b);
        oout.flush();

        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        for (int i = 0; i < badBounds.length; i++) {
            try {
                oin.read(null, badBounds[i][0], badBounds[i][1]);
                throw new Error();
            } catch (NullPointerException e) {
            }
        }
        for (int i = 0; i < badBounds.length; i++) {
            try {
                oin.read(b, badBounds[i][0], badBounds[i][1]);
                throw new Error();
            } catch (IndexOutOfBoundsException e) {
            }
        }
        oin.read(b);
    }
}
