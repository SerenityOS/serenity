/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4193422
 * @summary Make sure NullPointerExceptions are thrown when null
 * is passed into permission names
 */

import java.security.*;
import java.util.PropertyPermission;

public class NullName {

    public static void main(String[]args) throws Exception {
        int count = 0;

        try {

            try {
                PropertyPermission pp = new PropertyPermission(null, "read");
            } catch (NullPointerException e) {
                count++;
            }

            try {
                java.io.FilePermission fp =
                    new java.io.FilePermission(null, "read");
            } catch (NullPointerException e) {
                count++;
            }

            try {
                java.net.SocketPermission sp =
                    new java.net.SocketPermission(null, "connect");
            } catch (NullPointerException e) {
                count++;
            }

            // do one of the classes that extends BasicPermission
            try {
                RuntimePermission rp = new RuntimePermission(null);
            } catch (NullPointerException e) {
                count++;
            }

            try {
                UnresolvedPermission up = new UnresolvedPermission(null, "blah", "read", null);
            } catch (NullPointerException e) {
                count++;
            }


        } catch (Exception e) {
            throw new Exception("Test failed: Wrong exception thrown");
        }

        if (count != 5)
            throw new Exception("Test failed: didn't catch enough NullPointerExceptions");
    }
}
