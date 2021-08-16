/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8129895
 * @summary Throw VerifyError when checking assignability of primitive arrays
 * that are not identical.  For example, [I is not assignable to [B.
 * @compile primArray.jasm
 * @compile primArray49.jasm
 * @run main/othervm -Xverify:all PrimIntArray
 */

// Test that an int[] is not assignable to byte[].
public class PrimIntArray {

    public static void main(String args[]) throws Throwable {
        System.out.println("Regression test for bug 8129895");

        try {
            Class newClass = Class.forName("primArray");
            throw new RuntimeException("Expected VerifyError exception not thrown with new verifier");
        } catch (java.lang.VerifyError e) {
            System.out.println("Test PrimIntArray passed with new verifier");
        }

        try {
            Class newClass = Class.forName("primArray49");
            throw new RuntimeException("Expected VerifyError exception not thrown by old verifier");
        } catch (java.lang.VerifyError e) {
            System.out.println("Test PrimIntArray passed with old verifier");
        }
    }
}
