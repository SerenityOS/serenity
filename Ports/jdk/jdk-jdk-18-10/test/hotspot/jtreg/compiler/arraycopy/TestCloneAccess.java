/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8248791
 * @summary Test cloning with more than 8 (=ArrayCopyLoadStoreMaxElem) where loads are wrongly replaced by zero.
 * @requires vm.compiler2.enabled | vm.graal.enabled
 *
 * @run main/othervm -XX:-ReduceBulkZeroing
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestCloneAccess::*
 *                   compiler.arraycopy.TestCloneAccess
 * @run main/othervm -XX:-ReduceBulkZeroing -XX:-ReduceInitialCardMarks
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestCloneAccess::*
 *                   compiler.arraycopy.TestCloneAccess
 */
package compiler.arraycopy;

public class TestCloneAccess {
    static int test(E src) throws CloneNotSupportedException {
        // ArrayCopyNode for this clone is not inlined since there are more than 8 (=ArrayCopyLoadStoreMaxElem) fields
        src.i1 = 3;
        E dest = (E)src.clone();
        dontInline(dest.i1, dest.i2);

        // Both loads are wrongly optimized and replaced by a constant zero. LoadNode::Value() tries to find out if a load
        // is done from a freshly-allocated object. If that is the case, the load can be replaced by the default value zero.
        // However, in this case, the Allocation node belongs to an ArrayCopyNode which is responsible for initializing 'dest'.
        // If -XX:-ReduceBulkZeroing is set, the InitializationNode of the allocation does not bail out of this optimization
        // which results in a replacement of both loads by zero. This is addressed by this fix. If -XX:+ReduceBulkZeroing is
        // set, then we already bail out and perform the load correctly.
        return dest.i1 + dest.i2;
    }

    public static void main(String[] args) throws Exception {
        E e = new E();
        e.i2 = 4;
        int res = 0;
        for (int i = 0; i < 20000; i++) {
            res = test(e);
            if (res != 7 || e.i1 != 3 || e.i2 != 4) {
                throw new RuntimeException("Wrong result! Expected: res = 7, e.i1 = 3, e.i2 = 4 "
                                           + "but got: res = " + res + ", e.i1 = " + e.i1 + ", e.i2 = " + e.i2);
            }
        }
    }

    // Dont inline this method
    public static void dontInline(int i1, int i2) {
    }
}

class E implements Cloneable {
    /*
     * Need more than 8 (=ArrayCopyLoadStoreMaxElem) fields
     */
    int i1;
    int i2;
    int i3;
    int i4;
    int i5;
    int i6;
    int i7;
    int i8;
    int i9;

    E() {
        i1 = 0;
        i2 = 1;
        i3 = 2;
        i4 = 3;
        i5 = 4;
        i6 = 5;
        i7 = 6;
        i8 = 7;
        i9 = 8;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

