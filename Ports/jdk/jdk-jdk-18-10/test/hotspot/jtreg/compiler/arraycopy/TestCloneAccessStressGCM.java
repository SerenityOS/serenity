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
 * @key stress randomness
 * @bug 8235332 8248226
 * @summary Test cloning with more than 8 (=ArrayCopyLoadStoreMaxElem) fields with StressGCM
 * @library /
 * @requires vm.compiler2.enabled | vm.graal.enabled
 *
 * @run main/othervm -Xbatch
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestCloneAccessStressGCM::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:-ReduceInitialCardMarks
 *                   compiler.arraycopy.TestCloneAccessStressGCM
 * @run main/othervm -Xbatch
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestCloneAccessStressGCM::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:-ReduceInitialCardMarks
 *                   -XX:-ReduceBulkZeroing
 *                   compiler.arraycopy.TestCloneAccessStressGCM
 */

package compiler.arraycopy;

public class TestCloneAccessStressGCM {

    static int test(E src) throws CloneNotSupportedException {
        // ArrayCopyNode for this clone is not inlined since there are more than 8 (=ArrayCopyLoadStoreMaxElem) fields
        E dest = (E)src.clone();

        // The ArrayCopyNode initialization for the clone is executed after the LoadI nodes for 'dest' due to a
        // memory input from the uninitialized new object instead of the ArrayCopyNode. As a result, uninitialized
        // memory is read for each field and added together.
        return dest.i1 + dest.i2 + dest.i3 + dest.i4 + dest.i5 +
            dest.i6 + dest.i7 + dest.i8 + dest.i9;
    }

    public static void main(String[] args) throws Exception {
        TestCloneAccessStressGCM test = new TestCloneAccessStressGCM();
        int result = 0;
        E e = new E();
        for (int i = 0; i < 20000; i++) {
            result = test(e);
            if (result != 36) {
              throw new RuntimeException("Return value not 36. Got: " + result + "; additional check:  " + e.sum());
            }
        }

        if (result != 36) {
            throw new RuntimeException("Return value not 36. Got: " + result + "; additional check:  " + e.sum());
        }
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

    public int sum() {
        return i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9;
    }
}

