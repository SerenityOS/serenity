/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237945
 * @summary Test that triggers the too strong assertion "just_allocated_object(alloc_ctl) == ptr" in LibraryCallKit::tightly_coupled_allocation()
 *
 * @run main/othervm -Xbatch compiler.arraycopy.TestTightlyCoupledAllocationAssert
 */

package compiler.arraycopy;

public class TestTightlyCoupledAllocationAssert {

    static A a;
    static byte[] bArr = new byte[8];

    public static void main(String[] strArr) {
        for (int i = 0; i < 10000; i++ ) {
            test(i % 20);
        }
    }

    public static void test(int i) {
        byte [] bArrLocal = new byte[8]; // (1)
        if (i < 16) {
            a = new A(); // (2)
            return;
        }
        // The assertion "just_allocated_object(alloc_ctl) == ptr" in LibraryCallKit::tightly_coupled_allocation() fails during parsing here since
        // the most recent allocation is (2) and not (1). The earlier bailout checks about the memory state (involving the variable "rawmem") pass
        // because the if branch returns and the memory state from (2) is not merged with the memory state from (1) for the array copy.
        System.arraycopy(bArr, 0, bArrLocal, 0, 8);
    }
}

class A { }
