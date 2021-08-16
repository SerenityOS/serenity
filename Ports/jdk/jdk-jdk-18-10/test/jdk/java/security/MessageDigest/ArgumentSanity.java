/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4100227
 * @summary Do some sanity checks on the input arguments
 */

import java.security.*;

public class ArgumentSanity {

    public static void main(String[]args) throws Exception {

        byte[] data = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                        0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };
        byte[] out = new byte[16];
        MessageDigest dig = null;

        try {
            dig = MessageDigest.getInstance("md5");

            try {
                dig.update(null, 5, 20);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            }

            try {
                dig.update(data, 5, 20);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            }

            try {
                dig.digest(null, 5, 20);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            }

            try {
                dig.digest(out, 5, 20);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            }

            System.out.println("Test succeeded");

        } catch (Exception e) {
            throw new Exception("Test failed: Wrong exception thrown");
        }
    }
}
