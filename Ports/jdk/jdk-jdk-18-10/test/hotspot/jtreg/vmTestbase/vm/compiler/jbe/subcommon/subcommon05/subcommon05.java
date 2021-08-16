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
 * @summary converted from VM Testbase runtime/jbe/subcommon/subcommon05.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.subcommon.subcommon05.subcommon05
 */

package vm.compiler.jbe.subcommon.subcommon05;

/* -- Common subexpression elimination testing
   This is a test for simple CSE elimination within a basic block.
 */

public class subcommon05 {
    int LEN = 1000;
    double i1, i2, i3, i4, i5, i6;
    double i7, i8, i9, i10, i11, i12;
    double a[]= new double[LEN];
    double aopt[] = new double[LEN];

    public static void main(String args[]) {
        subcommon05 sce = new subcommon05();

        sce.un_optimized();
        sce.hand_optimized();
        if (sce.eCheck()) {
            System.out.println("Test subcommon05 Passed.");
        } else {
            throw new Error("Test subcommon05 Failed.");
        }
    }

    void un_optimized() {
        i1 = 1.0;
        i2 = 2.0;
        i3 = 3.0;
        i4 = 4.0;
        i5 = 5.0;
        i6 = 6.0;
        i7 = 7.0;
        i8 = 8.0;
        i9 = 9.0;
        i10 = 10.0;
        i11 = 11.0;
        i12 = 12.0;

        for (int k = 0; k < 150; k++)
            a[k] = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12;
        for (int k = 150; k < 300; k++)
            a[k] = i1 - i2 - i3 - i4 - i5 - i6 - i7 - i8 - i9 - i10 - i11 - i12;
        for (int k = 300; k < 450; k++)
            a[k] = i1 * i2 * i3 * i4 * i5 * i6 * i7 * i8 * i9 * i10 * i11 * i12;
        for (int k = 450; k < 600; k++)
            a[k] = i1 / i2 / i3 / i4 / i5 / i6 / i7 / i8 / i9 / i10 / i11 / i12;
        for (int k = 600; k < 750; k++)
            a[k] = i1 % i2 % i3 % i4 % i5 % i6 % i7 % i8 % i9 % i10 % i11 % i12;
        for (int k = 750; k < 1000; k++)
            a[k] = (((i1 + i2 - i3) * i4 / i5) % ((i6 + i7 - i8) * i9 / i10) % i11) % i12;
   }

    void hand_optimized() {
        double rega, regs, regm, regd, regr, regc;
        i1 = 1.0;
        i2 = 2.0;
        i3 = 3.0;
        i4 = 4.0;
        i5 = 5.0;
        i6 = 6.0;
        i7 = 7.0;
        i8 = 8.0;
        i9 = 9.0;
        i10 = 10.0;
        i11 = 11.0;
        i12 = 12.0;

        rega = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12;
        regs = i1 - i2 - i3 - i4 - i5 - i6 - i7 - i8 - i9 - i10 - i11 - i12;
        regm = i1 * i2 * i3 * i4 * i5 * i6 * i7 * i8 * i9 * i10 * i11 * i12;
        regd = i1 / i2 / i3 / i4 / i5 / i6 / i7 / i8 / i9 / i10 / i11 / i12;
        regr = i1 % i2 % i3 % i4 % i5 % i6 % i7 % i8 % i9 % i10 % i11 % i12;
        regc = (((i1 + i2 - i3) * i4 / i5) % ((i6 + i7 - i8) * i9 / i10) % i11) % i12;

        for (int k = 0; k < 150; k++)
            aopt[k] = rega;
        for (int k = 150; k < 300; k++)
            aopt[k] = regs;
        for (int k = 300; k < 450; k++)
            aopt[k] = regm;
        for (int k = 450; k < 600; k++)
            aopt[k] = regd;
        for (int k = 600; k < 750; k++)
            aopt[k] = regr;
        for (int k = 750; k < 1000; k++)
            aopt[k] = regc;
    }

    // Compare non-optimized and hand-optimized results
    boolean eCheck() {
        boolean r = true;

        for (int i = 0; i < LEN; i++) {
                if (a[i] != aopt[i]) {
                    System.out.println("Bad result: a["+i+"]="+a[i]+"; aopt["+i+"]="+aopt[i]);
                    r = false;
                }
        }
        return r;
    }
}
