/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @test
 * @bug 8167646
 * @summary Better invalid FilePermission
 * @library /test/lib
 * @build jdk.test.lib.Asserts
 * @run main Invalid
 */

import jdk.test.lib.Asserts;

import java.io.FilePermission;

public class Invalid {

    public static void main(String args[]) throws Exception {

        // Allmighty
        FilePermission af = new FilePermission("<<ALL FILES>>", "read");

        // Normal
        FilePermission fp = new FilePermission("a", "read");

        // Invalid
        FilePermission fp1 = new FilePermission("a\000", "read");
        FilePermission fp2 = new FilePermission("a\000", "read");
        FilePermission fp3 = new FilePermission("b\000", "read");

        // Invalid equals to itself
        Asserts.assertEQ(fp1, fp1);

        // and not equals to anything else, including other invalid ones
        Asserts.assertNE(fp, fp1);
        Asserts.assertNE(fp1, fp);
        Asserts.assertNE(fp1, fp2);
        Asserts.assertNE(fp1, fp3);

        // Invalid implies itself
        Asserts.assertTrue(fp1.implies(fp1));

        // <<ALL FILES>> implies invalid
        Asserts.assertTrue(af.implies(fp1));

        // and not implies or implied by anything else, including other
        // invalid ones
        Asserts.assertFalse(fp.implies(fp1));
        Asserts.assertFalse(fp1.implies(fp));
        Asserts.assertFalse(fp1.implies(fp2));
        Asserts.assertFalse(fp1.implies(fp3));
    }
}
