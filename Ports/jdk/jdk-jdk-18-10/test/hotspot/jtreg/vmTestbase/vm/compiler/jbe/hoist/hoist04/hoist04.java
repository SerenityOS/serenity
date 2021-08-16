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
 * @summary converted from VM Testbase runtime/jbe/hoist/hoist04.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.hoist.hoist04.hoist04
 */

package vm.compiler.jbe.hoist.hoist04;

// hoist04.java

/* -- Test hoist invariant code in conversion of integer to double in a do..while loop.
     Example:

     double a[];
     int x, y, z;
     for (i = 0; i < 100; i++)
        a[i] = (double)(x + y)*z;

     x, y, and z are loop invariants and can be hoisted out of the loop and calculated only once.
 */

public class hoist04 {
    int LEN = 5000;
    double a[] = new double[LEN];
    double aopt[] = new double[LEN];
    boolean bool_val = true;
    int i1 = 1, i2 = 2, i3 = 3, i4 = 4, i5 = 5, i6 = 6, i7 = 7, i8 = 8, i9 = 9;

    public static void main(String args[]) {
        hoist04 hst = new hoist04();

        hst.f();
        hst.fopt();
        if (hst.eCheck()) {
            System.out.println("Test hoist04 Passed.");
        } else {
            throw new Error("Test hoist04 Failed.");
        }
    }

    // Non-optimized version: i1 through i9 are the invariants
    void f() {
        int i = 0;

        do
            a[i++] = bool_val ? (double)(i1+i2+i3+i4+i5+i6+i7+i8+i9) : (double)(i1*i2*i3*i4*i5*i6*i7*i8*i9);
        while (i < a.length);
    }

    // Code fragment after the invariant expression is hoisted out of the loop.
    void fopt() {
        int i = 0;
        int t =  bool_val ? (i1+i2+i3+i4+i5+i6+i7+i8+i9) : (i1*i2*i3*i4*i5*i6*i7*i8*i9);

        do
            aopt[i++] = (double)t;
        while (i < aopt.length);
    }

    // Check Loop Hoisting results
    boolean eCheck() {
        for (int i = 0; i < a.length; i++)
            if (a[i] != aopt[i]) {
                System.out.println("a["+i+"]="+a[i]+"; aopt["+i+"]="+aopt[i]);
                return false;
            }

        return true;
    }
}
