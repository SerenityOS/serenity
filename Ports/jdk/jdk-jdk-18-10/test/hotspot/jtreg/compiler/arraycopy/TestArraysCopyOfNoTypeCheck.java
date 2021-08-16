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
 * @test
 * @bug 8055910
 * @summary Arrays.copyOf doesn't perform subtype check
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.arraycopy.TestArraysCopyOfNoTypeCheck
 */

package compiler.arraycopy;

import java.util.Arrays;

public class TestArraysCopyOfNoTypeCheck {

    static class A {
    }

    static class B extends A {
    }

    static B[] test(A[] arr) {
        return Arrays.copyOf(arr, 10, B[].class);
    }

    static public void main(String[] args) {
        A[] arr = new A[20];
        for (int i = 0; i < 20000; i++) {
            test(arr);
        }
        A[] arr2 = new A[20];
        arr2[0] = new A();
        boolean exception = false;
        try {
            test(arr2);
        } catch (ArrayStoreException ase) {
            exception = true;
        }
        if (!exception) {
            throw new RuntimeException("TEST FAILED: ArrayStoreException not thrown");
        }
    }
}
