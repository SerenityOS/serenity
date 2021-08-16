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
 * @test TestRangeCheck
 * @bug 8054883
 * @summary Tests that range check is not skipped
 * @run main compiler.osr.TestRangeCheck
 */

package compiler.osr;

public class TestRangeCheck {
    public static void main(String args[]) {
        try {
            test();
            throw new AssertionError("Expected ArrayIndexOutOfBoundsException was not thrown");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Expected ArrayIndexOutOfBoundsException was thrown");
        }
    }

    private static void test() {
        int arr[] = new int[1];
        int result = 1;

        // provoke OSR compilation
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
        }

        if (result > 0 && arr[~result] > 0) {
            arr[~result] = 0;
        }
    }
}
