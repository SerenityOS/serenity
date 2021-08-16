/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase runtime/jbe/hoist/hoist01.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.hoist.hoist01.hoist01
 */

package vm.compiler.jbe.hoist.hoist01;

// hoist01.java

/* -- Test hoist invariant code of double expression. This eliminates unnecessary recomputations of invariant expressions in loops.
     Example:

     double a[];
     double x, y, z;
     for (i = 0; i < 100; i++)
        a[i] = (x + y)*z;
 */

public class hoist01 {
    final static double PI=3.14159;
    int LEN = 5000;
    double a[] = new double[LEN];
    double aopt[] = new double[LEN];
    int i1=1, i2=2, i3=3, i4=4, i5=5, i6=6, i7=7, i8=8;
    int i9=9, i10=10, i11=11, i12=12, i13=13, i14=14, i15=15;

    public static void main(String args[]) {
        hoist01 hst = new hoist01();

        hst.f();
        hst.fopt();
        if (hst.eCheck()) {
            System.out.println("Test hoist01 Passed.");
        } else {
            throw new Error("Test hoist01 Failed.");
        }
    }

    void f() {
        // Non-optimized version: i1 through i15 are invariants
        for (int i = 1; i < a.length; i++) {
            a[0] = Math.sin(2 * PI * Math.pow(1/PI,3));
            a[i] = a[0] + (i1 + i2)*PI + i3 + i4 + i5/i6*i7 +
                i9%i8 + i10*(i11*i12) + i13 + i14 + i15;
        }
    }

    // Code fragment after the invariant expression is hoisted out of the loop.
    void fopt() {
        double t;

        aopt[0] = Math.sin(2 * PI * Math.pow(1/PI,3));
        t = aopt[0] + (i1 + i2)*PI + i3 + i4 + i5/i6*i7 +
            i9%i8 + i10*(i11*i12) + i13 + i14 + i15;

        for (int i = 1; i < aopt.length; i++) {
            aopt[i] = t;
        }
    }

    // Check Loop Hoisting results
    boolean eCheck() {
        for (int i = 0; i < a.length; i++)
            if (a[i] != aopt[i])
                return false;

        return true;
    }
}
